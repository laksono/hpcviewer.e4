package edu.rice.cs.hpctraceviewer.ui.internal;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import edu.rice.cs.hpc.data.util.OSValidator;
import edu.rice.cs.hpctraceviewer.data.SpaceTimeDataController;
import edu.rice.cs.hpctraceviewer.data.TimelineDataSet;
import edu.rice.cs.hpctraceviewer.ui.base.ISpaceTimeCanvas;
import edu.rice.cs.hpctraceviewer.ui.preferences.TracePreferenceManager;
import edu.rice.cs.hpctraceviewer.ui.util.Utility;




/******************************************************
 * 
 * Abstract class to paint depth view and detail view
 * The instance of the children of the class needs to 
 * implement the start and the end method of the painting
 * 
 *
 *******************************************************/
public abstract class BaseViewPaint extends Job
{

	final protected ISpaceTimeCanvas canvas;
	private Map<Future<Integer>, BaseTimelineThread> mapFutureToTask;

	protected boolean changedBounds;
		
	protected SpaceTimeDataController controller;

	/**
	 * Constructor to paint a view (trace and depth view)
	 * 
	 * @param title name of this view (job title purpose)
	 * @param _data global data of the traces
	 * @param _attributes the attribute of the trace view
	 * @param _changeBound true or false if it requires changes of bound
	 * @param threadExecutor executor
	 */

	public BaseViewPaint(String title, SpaceTimeDataController _data, boolean _changeBound, 
						 ISpaceTimeCanvas canvas) 
	{
		super(title);
		
		changedBounds = _changeBound;
		controller    = _data;
		this.canvas   = canvas;
	}
	

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus run(IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;
		
		// TODO: this is a hack. Somehow, the value of monitor is canceled, and we cannot paint anything
		//       this happens on Mac. So to make sure we can paint, we have to set manually the cancel to true

		monitor.setCanceled(false);
		
		if (!paint( monitor))
		{
			status = Status.CANCEL_STATUS;
		}
		
		return status;
	}	

	/**********************************************************************************
	 *	Paints the specified time units and processes at the specified depth
	 *	on the SpaceTimeCanvas using the SpaceTimeSamplePainter given. Also paints
	 *	the sample's max depth before becoming overDepth on samples that have gone over depth.
	 *
	 *	@param canvas   		 The SpaceTimeDetailCanvas that will be painted on.
	 *  @return boolean true of the pain is successful, false otherwise
	 ***********************************************************************************/
	public boolean paint(IProgressMonitor monitor)
	{	
		// depending upon how zoomed out you are, the iteration you will be
		// making will be either the number of pixels or the processors
		int linesToPaint = getNumberOfLines();

		// -------------------------------------------------------------------
		// hack fix: if the number of horizontal pixels is less than 1 we
		// return immediately, otherwise it throws an exception
		// -------------------------------------------------------------------
		if (controller.getPixelHorizontal() <= 0)
			return false;
		
		final int max_threads = TracePreferenceManager.getMaxThreads();
		int num_threads = Utility.getNumThreads(max_threads);
		
		// -------------------------------------------------------------------
		// initialize the painting (to be implemented by the instance)
		// -------------------------------------------------------------------

		if (!startPainting(linesToPaint, num_threads, changedBounds))
			return false;

		monitor.beginTask(getName(), linesToPaint);

		// -------------------------------------------------------------------
		// Create multiple threads to collect data
		// -------------------------------------------------------------------
		
		// decompression can be done with multiple threads without accessing gtk (on linux)
		// It looks like there's no major performance effect though

		try {
			launchDataGettingThreads(changedBounds, num_threads);
			
		} catch (Exception e) {
			e.printStackTrace();
			
			// shutdown the monitor to end the progress bar
			monitor.done();
			return false;
		}
		
		// -------------------------------------------------------------------
		// instantiate queue based on whether we need multi-threading or not.
		// in case of multithreading, we want a thread-safe queue
		// -------------------------------------------------------------------
		final Queue<TimelineDataSet> queue = new ConcurrentLinkedQueue<TimelineDataSet>();

		// -------------------------------------------------------------------
		// case where everything works fine, and all the data has been read,
		//	we paint the canvas using multiple threads
		// -------------------------------------------------------------------
				
		final double xscale = canvas.getScalePixelsPerTime();
		final double yscale = Math.max(canvas.getScalePixelsPerRank(), 1);
		
		mapFutureToTask = new HashMap<Future<Integer>, BaseTimelineThread>(num_threads);
		
		ExecutorService threadExecutor = Executors.newCachedThreadPool();
		ExecutorCompletionService<Integer> ecs = new ExecutorCompletionService<Integer>(threadExecutor);
						
		for (int i=0; i< num_threads; i++ ) {
			final BaseTimelineThread thread = getTimelineThread(canvas, xscale, yscale, queue, monitor);
			
			try {
				// when hpctraceviewer exits while a job is still running,
				// there will be a sporadic of error messages.
				// we should just warning the user instead of scaring them with
				// error message and backtrace

				Future<Integer> f = ecs.submit(thread);
				mapFutureToTask.put(f, thread);
			
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		
		// -------------------------------------------------------------------
		// draw to the canvas
		// -------------------------------------------------------------------
		
		int num_paint_threads = OSValidator.isUnix() ? 1 : num_threads;
		if (OSValidator.isUnix()) {
			// linux need UI thread to paint
			Display.getDefault().syncExec(() -> {
				executePaint( ecs, 
						  	  num_threads, 
						  	  num_paint_threads, 
						  	  queue, 
						  	  linesToPaint, 
						  	  monitor, 
						  	  threadExecutor);
			});
		} else {
			executePaint( ecs, 
					  	  num_threads, 
					  	  num_paint_threads, 
					  	  queue, 
					  	  linesToPaint, 
					  	  monitor, 
					  	  threadExecutor);
		}
		monitor.done();
		threadExecutor.shutdown();
		
		changedBounds = false;

		return !monitor.isCanceled();
	}
	
	
	/****
	 * run jobs for collecting data and painting the image
	 * 
	 * @param ecs : completion service from data collection job
	 * @param num_threads : number of threads for collecting data (see ecs)
	 * @param num_paint_threads : number of threads for paiting
	 * @param queue : the data to be collected
	 * @param linesToPaint : number of lines to paint
	 * @param timelineDone : atomic integer for number of lines collected
	 * @param monitor : UI progress monitor 
	 */
	private void executePaint(ExecutorCompletionService<Integer> ecs,
			int num_threads, int num_paint_threads, Queue<TimelineDataSet> queue, 
			int linesToPaint, IProgressMonitor monitor, ExecutorService threadExecutor) 
	{
		final List<Future<List<ImagePosition>>> threadsPaint = new ArrayList<Future<List<ImagePosition>>>();
		
		// for threads as many as the number of paint threads (specified by the caller)
		for (int threadNum=0; threadNum < num_paint_threads; threadNum++) 
		{

			final BasePaintThread paintThread = getPaintThread(queue, linesToPaint,
					controller.getPixelHorizontal(), monitor);

			if (paintThread != null) {
				final Future<List<ImagePosition>> submit = threadExecutor.submit( paintThread );
				threadsPaint.add(submit);
			}
		}
		// -------------------------------------------------------------------
		// Finalize the painting (to be implemented by the instance)
		// -------------------------------------------------------------------
		ArrayList<Integer> result = new ArrayList<Integer>();
		if (waitDataPreparationThreads(ecs, result, num_threads, monitor))
		{
			endPainting(threadsPaint, monitor);
		} else {
			
			// threads to collect data are shutdown, perhaps due to cancellation
			// it is very important to cancel threads to paint the canvas
			// without this cancellation, in some platforms,  the painter threads 
			// are NOT notified, and still thinking that the progress monitor is still
			// valid and continue to wait data from data collector threads
			// 
			// Due to bug in Eclipse' ProgressMonitor, the monitor's isCanceled() method
			// always returns true if the parent exits.
			
			for(Future<List<ImagePosition>> p: threadsPaint) {
				p.cancel(true);
			}
			
			monitor.setCanceled(true);
			// whatever the result, we need to clear the 
			monitor.setTaskName("");
		}
	}
	
	/******
	 * Wait for any thread who finishes first, and then add the result to the list
	 * 
	 * @param ecs : executor service
	 * @param result : the list of the result
	 * @param launch_threads : number of launched threads
	 */
	private boolean waitDataPreparationThreads(ExecutorCompletionService<Integer> ecs, 
			ArrayList<Integer> result, int launch_threads, IProgressMonitor monitor)
	{
		int num_invalid_samples = 0;		
		
		for (int i=0; i<launch_threads; i++)
		{
			if (monitor.isCanceled())
				return false;
			
			try {
				Future<Integer> f = ecs.take();
				Integer linenum = f.get();
				if (linenum == null)
					return false;
				
				num_invalid_samples += linenum.intValue();				
				result.add(linenum);

				BaseTimelineThread t = mapFutureToTask.get(f);

				endPreparationThread(t, linenum.intValue());
				
			} catch (Exception e) {
				// we don't need to show exception message everywhere unless if we are in develop mode
				return false;
			}
		}
		endDataPreparation(num_invalid_samples);
		return true;
	}

	/******
	 * finalize the data collection, and put all images into a canvas
	 * 
	 * @param canvas
	 * @param listOfImageThreads
	 */
	private boolean endPainting(List<Future<List<ImagePosition>>> listOfImageThreads,
			IProgressMonitor monitor)
	{
		for( Future<List<ImagePosition>> listFutures : listOfImageThreads ) 
		{
			if (monitor.isCanceled())
				return false;
			
			try {
				List<ImagePosition> listImages = listFutures.get();
				if (listImages == null)
					continue;

				for (ImagePosition image : listImages) 
				{
					if (monitor.isCanceled())
						return false;

					drawPainting(canvas, image);
				}				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// notify user that we have finished painting
		endPainting(monitor.isCanceled());
		
		return true;
	}
		

	//------------------------------------------------------------------------------------------------
	// abstract methods 
	//------------------------------------------------------------------------------------------------
	
	/**
	 * Initialize the paint, before creating the threads to paint.
	 * The method return false to exit the paint, true to paint.
	 * 
	 * The implementer is also responsible to reset its counter (yikes!)
	 * 
	 * @param linesToPaint
	 * @param changedBounds
	 * @return false will exit the painting
	 */
	abstract protected boolean startPainting(int linesToPaint, int numThreads, boolean changedBounds);
	
	/*****
	 * notification for the termination of painting
	 * 
	 * @param isCanceled : flag if the process has been canceled or not
	 */
	abstract protected void endPainting(boolean isCanceled);
	
	/***
	 * start painting an image to the canvas
	 * 
	 * @param canvas: canvas to be painted
	 * @param imagePosition : a pair of image and position
	 */
	abstract protected void drawPainting(ISpaceTimeCanvas canvas, ImagePosition imagePosition);
	
	/**
	 * Retrieve the number of lines to paint 
	 * @return
	 */
	abstract protected int getNumberOfLines();
	
	/****
	 * launching threads for remote communication
	 * 
	 * @param changedBounds
	 * @param numThreads
	 * @throws IOException
	 */
	abstract protected void launchDataGettingThreads(boolean changedBounds, int numThreads) 
			throws IOException;

	/****
	 * get a thread for collecting timeline data
	 * @param canvas
	 * @param xscale
	 * @param yscale
	 * @param queue
	 * @param timelineDone
	 * @return
	 */
	abstract protected BaseTimelineThread  getTimelineThread(ISpaceTimeCanvas canvas, double xscale, double yscale,
			Queue<TimelineDataSet> queue, IProgressMonitor monitor);
	
	/***
	 * get a thread for painting a number of lines
	 * @param queue
	 * @param numLines
	 * @param timelineDone
	 * @param device
	 * @param width
	 * @return
	 */
	abstract protected BasePaintThread getPaintThread( Queue<TimelineDataSet> queue, int numLines, 
													   int width, IProgressMonitor monitor);

	abstract protected void endPreparationThread(BaseTimelineThread thread, int result);
	
	abstract protected void endDataPreparation(int numInvalidData);
}
