package edu.rice.cs.hpctraceviewer.ui.main;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

import edu.rice.cs.hpc.data.db.IdTuple;
import edu.rice.cs.hpc.data.experiment.extdata.IBaseData;
import edu.rice.cs.hpc.data.experiment.extdata.IFileDB.IdTupleOption;
import edu.rice.cs.hpcsetting.preferences.PreferenceConstants;
import edu.rice.cs.hpcsetting.preferences.ViewerPreferenceManager;
import edu.rice.cs.hpctraceviewer.data.SpaceTimeDataController;
import edu.rice.cs.hpctraceviewer.data.TimelineDataSet;
import edu.rice.cs.hpctraceviewer.ui.base.ISpaceTimeCanvas;
import edu.rice.cs.hpctraceviewer.ui.internal.BasePaintThread;
import edu.rice.cs.hpctraceviewer.ui.internal.BaseTimelineThread;
import edu.rice.cs.hpctraceviewer.ui.internal.BaseViewPaint;
import edu.rice.cs.hpctraceviewer.ui.internal.ImagePosition;

/******************************************************
 * 
 * Painting class for detail view (space-time view)
 *
 ******************************************************/
public class DetailViewPaint extends BaseViewPaint 
{		
	/** maximum number of records to display **/
	static public final int MAX_RECORDS_DISPLAY = 99;
	/** text when we reach the maximum of records to display **/
	static public final String TOO_MANY_RECORDS = ">" + String.valueOf(MAX_RECORDS_DISPLAY) ;
	
	final private Point maxTextSize;

	private final GC masterGC;
	private final GC origGC;
	private final Device device;
	
	final private AtomicInteger currentLine, numDataCollected;
	final private int numLines;

	private boolean debug;
	private List<IdTuple> listIdTuples; 
	
	/***
	 * Create a job to paint the main canvas view
	 * @param device
	 * @param masterGC
	 * @param origGC
	 * @param data
	 * @param attributes
	 * @param numLines
	 * @param changeBound
	 * @param canvas
	 * @param threadExecutor
	 */
	public DetailViewPaint(final Device device, final GC masterGC, final GC origGC, SpaceTimeDataController data,
			int numLines, boolean changeBound,
			ISpaceTimeCanvas canvas) 
	{
		super("Main trace view", data, changeBound, canvas);
		this.masterGC = masterGC;
		this.origGC   = origGC;
		this.numLines = numLines;
		this.device   = device;
		
		// check if we need to print the text information on the canvas
		
		// initialize the size of maximum text
		//	the longest text should be: ">99(>99)"
		maxTextSize = masterGC.textExtent(TOO_MANY_RECORDS + "(" + TOO_MANY_RECORDS + ")");
		
		currentLine = new AtomicInteger(0);
		numDataCollected = new AtomicInteger(0);
		
		PreferenceStore pref = ViewerPreferenceManager.INSTANCE.getPreferenceStore();
		debug = pref.getBoolean(PreferenceConstants.ID_DEBUG_MODE);
	}

	@Override
	protected boolean startPainting(int linesToPaint, int numThreads, boolean changedBounds) {
		final IBaseData traceData = controller.getBaseData();
		listIdTuples = traceData.getListOfIdTuples(IdTupleOption.BRIEF);

		return true;
	}

	@Override
	protected int getNumberOfLines() {
		return numLines;
	}

	@Override
	protected BaseTimelineThread getTimelineThread(ISpaceTimeCanvas canvas, double xscale,
			double yscale, Queue<TimelineDataSet> queue, IProgressMonitor monitor) {

		return new TimelineThread(controller, changedBounds,   
				yscale, queue, numDataCollected, numLines, monitor);
	}

	@Override
	protected void launchDataGettingThreads(boolean changedBounds,
			int numThreads) throws IOException {
		controller.fillTracesWithData( changedBounds, numThreads);
	}

	@Override
	protected BasePaintThread getPaintThread(
			Queue<TimelineDataSet> queue, int numLines, 
			int width, IProgressMonitor monitor) {

		return new DetailPaintThread(device, controller, queue, numLines, 
				numDataCollected, currentLine, 
				width, maxTextSize, debug, monitor);
	}

	@Override
	protected void drawPainting(ISpaceTimeCanvas canvas,
			ImagePosition imagePosition) {

		final double pixelsPerRank = canvas.getScalePixelsPerRank();
		
		DetailImagePosition imgDetailLine = (DetailImagePosition)imagePosition;
		double yscale = Math.max(pixelsPerRank, 1);

		int yposition = (int) Math.round(imgDetailLine.position * yscale);

		// put the image onto the canvas
		masterGC.drawImage(imgDetailLine.image, 0, yposition);
		
		origGC.drawImage(imgDetailLine.imageOriginal, 0, imgDetailLine.position);

		imgDetailLine.image.dispose();
		imgDetailLine.imageOriginal.dispose();
	}

	@Override
	protected void endPainting(boolean isCanceled) {}

	@Override
	protected void endPreparationThread(BaseTimelineThread thread, int result) {
		if (!debug)
			return;
		
		Map<Integer, List<?>> m = thread.getInvalidData();
		if (m == null || m.size()==0)
			return;
		
		m.forEach((k,v) -> {
			IdTuple idt = listIdTuples.get(k);
			System.out.println(idt + " has invalid cpid: " + v);
		});

	}

	@Override
	protected void endDataPreparation(int numInvalidData) {

		if (numInvalidData > 0) {
			final String message = "Warning: " + numInvalidData + 
					" sample(s) have invalid call-path ID.";
			canvas.setMessage(message);
		}
	}
}
