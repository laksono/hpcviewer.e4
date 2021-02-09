package edu.rice.cs.hpctraceviewer.ui.depth;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import edu.rice.cs.hpctraceviewer.ui.base.ISpaceTimeCanvas;
import edu.rice.cs.hpctraceviewer.ui.base.ITracePart;
import edu.rice.cs.hpctraceviewer.ui.context.BaseTraceContext;
import edu.rice.cs.hpctraceviewer.ui.internal.AbstractTimeCanvas;
import edu.rice.cs.hpctraceviewer.ui.internal.BaseViewPaint;
import edu.rice.cs.hpctraceviewer.ui.operation.AbstractTraceOperation;
import edu.rice.cs.hpctraceviewer.ui.operation.PositionOperation;
import edu.rice.cs.hpctraceviewer.ui.operation.ZoomOperation;
import edu.rice.cs.hpctraceviewer.data.SpaceTimeDataController;
import edu.rice.cs.hpctraceviewer.data.Frame;
import edu.rice.cs.hpctraceviewer.data.ImageTraceAttributes;
import edu.rice.cs.hpctraceviewer.data.Position;
import edu.rice.cs.hpctraceviewer.data.ColorTable;
import edu.rice.cs.hpctraceviewer.data.util.Constants;



/**********************************************************
 * 
 * A canvas for displaying the depth view.
 *
 **********************************************************/
public class DepthTimeCanvas extends AbstractTimeCanvas 
	implements IOperationHistoryListener, ISpaceTimeCanvas
{	
	private final static float FRACTION_ZOOM_DEPTH = 2.0f;
	private final static int   DEPTH_MIN = 1;
	
	private final ITracePart tracePart;

	private SpaceTimeDataController stData;
	private int currentProcess = Integer.MIN_VALUE;
	private Rectangle bound;
	private int visibleDepths;

	/********************
	 * constructor to create this canvas
	 * 
	 * @param composite : the parent composite
	 */
	public DepthTimeCanvas(ITracePart tracePart, Composite composite)
    {
		super(composite, SWT.NONE);
		
		this.tracePart = tracePart;
	}
	
	/****
	 * new data update
	 * @param _stData
	 */
	public void updateView(SpaceTimeDataController stData)
	{
		super.init();
		setVisible(true);
		
		if (this.stData == null) {
			// just initialize once
			tracePart.getOperationHistory().addOperationHistoryListener(this);
		}
		this.stData = stData;
		visibleDepths = stData.getMaxDepth();
	}
	
	@Override
	public void widgetDisposed(DisposeEvent e) {

		removeDisposeListener(this);
		tracePart.getOperationHistory().removeOperationHistoryListener(this);
		super.widgetDisposed(e);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.events.PaintListener#paintControl(org.eclipse.swt.events.PaintEvent)
	 */
	@Override
	public void paintControl(PaintEvent event)
	{
		bound = getClientArea();

		if (stData == null )
			return;
		
		super.paintControl(event);
		
		final long topLeftPixelX = Math.round(stData.getAttributes().getTimeBegin()*getScalePixelsPerTime());
		final int viewHeight 	 = bound.height;

		//--------------------
		//draws cross hairs
		//--------------------
		
		event.gc.setForeground(Constants.COLOR_WHITE);
		//event.gc.setAlpha(240);
		
		long selectedTime = stData.getAttributes().getFrame().position.time;
		
		int topPixelCrossHairX = (int)(Math.round(selectedTime*getScalePixelsPerTime())-2-topLeftPixelX);
		event.gc.fillRectangle(topPixelCrossHairX,0,4,viewHeight);
		
		final int depth    = stData.getAttributes().getDepth();
		final int maxDepth = stData.getMaxDepth();
		final int minDepth = TimelineDepthThread.getMinDepth(depth, visibleDepths, maxDepth);
		final int cdepth   = depth-minDepth;
		final int width    = cdepth*viewHeight/visibleDepths + viewHeight/(2*visibleDepths);
		event.gc.fillRectangle(topPixelCrossHairX-8,width-1,20,4);
	}
	
	
    /***
     * force to refresh the content of the canvas. 
     */
    public void refresh() 
    {
		rebuffer();
    }
    
    
    @Override
    public double getScalePixelsPerTime()
	{
		if (bound == null) {
			final Display display = Display.getDefault();
			display.syncExec(new Runnable() {
				
				@Override
				public void run() {
					bound = getClientArea();
				}
			});
		}
		final int viewWidth = bound.width;

		return (double)viewWidth / (double)getNumTimeDisplayed();
	}

    @Override
	public double getScalePixelsPerRank() {
		return Math.max(bound.height/(double)visibleDepths, 1);
	}

	
	/****
	 * Zoom out the depth: increase the depth so users can see more 
	 */
	public void zoomOut() {
		visibleDepths  = (int) Math.min(stData.getMaxDepth(), visibleDepths + FRACTION_ZOOM_DEPTH);
		
		rebuffer();
	}
	
	
	/****
	 * Zoom in the depth: decrease the depth so user can see more pixels
	 */
	public void zoomIn() {
		visibleDepths  = (int) Math.max(DEPTH_MIN, visibleDepths - FRACTION_ZOOM_DEPTH);
		 
		rebuffer();
	}
	
	
	/****
	 * check if we can zoom out
	 * @return true if it's feasible
	 */
	public boolean canZoomOut() {
		return visibleDepths < stData.getMaxDepth();
	}
	
	
	/****
	 * check if can zoom in
	 * @return true if it's possible
	 */
	public boolean canZoomIn() {
		return visibleDepths > DEPTH_MIN;
	}
	
	
	//---------------------------------------------------------------------------------------
	// PRIVATE METHODS
	//---------------------------------------------------------------------------------------

	private long getNumTimeDisplayed()
	{
		return (stData.getAttributes().getTimeInterval());
	}
	
	
    /************
     * method to repaint the canvas
     * this method can be costly, please do not call this unless the data has changed
     * 
     */
	private void rebuffer()
	{
		if (stData == null )
			return;

		final ImageTraceAttributes attributes = stData.getAttributes();
		final Frame frame = attributes.getFrame();

		// store the current process so that we don't need to rebuffer every time
		// we change the position within the same process
		currentProcess = frame.position.process;

		final Display display = Display.getDefault();
		display.syncExec( new Runnable() {
			
			@Override
			public void run() {
				final Rectangle rb = getBounds();
				
				final int viewWidth  = rb.width;
				final int viewHeight = rb.height;

				if (viewWidth <= 0 && viewHeight<= 0) {
					return;
				}
				//paints the current screen

				final Image imageBuffer = new Image(getDisplay(), viewWidth, viewHeight);
				setBuffer(imageBuffer);

				final GC bufferGC = new GC(getBuffer());
				bufferGC.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
				bufferGC.fillRectangle(0,0,viewWidth,viewHeight);
				
				attributes.setDepthPixelVertical(viewHeight);
								
				BaseViewPaint depthPaint = new DepthViewPaint( 	bufferGC, 
																stData, 
																attributes, 
																true, 
																DepthTimeCanvas.this,
																visibleDepths);
				
				depthPaint.addJobChangeListener(new DepthJobListener());
				depthPaint.schedule();
			}
		});
	}



	@Override
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.operations.IOperationHistoryListener#historyNotification(org.eclipse.core.commands.operations.OperationHistoryEvent)
	 */
	public void historyNotification(final OperationHistoryEvent event) {
		
		if (event.getEventType() == OperationHistoryEvent.DONE) 
		{
			final IUndoableOperation operation = event.getOperation();
			if (!(operation instanceof AbstractTraceOperation)) {
				return;
			}
			AbstractTraceOperation op = (AbstractTraceOperation) operation;
			if (op.getData() != stData) 
				return;

			IUndoContext bufferContext   = tracePart.getContext(BaseTraceContext.CONTEXT_OPERATION_BUFFER);
			IUndoContext positionContext = tracePart.getContext(BaseTraceContext.CONTEXT_OPERATION_POSITION);
			
			if (operation.hasContext(bufferContext)) {
				// this event includes if there's a change of colors definition, so everyone needs
				// to refresh the content
				try {
					super.init();
					rebuffer();
				} catch (java.lang.NullPointerException e) {
					// ignore exception when there's (possibly) multiple thread access  
				}
				
			} else if (operation.hasContext(positionContext)) {
				PositionOperation opPos = (PositionOperation) operation;
				Position position = opPos.getPosition();
				if (position.process == currentProcess)
				{
					// changing cursor position within the same process
					redraw();
				} else {
					// different process, we need to repaint every thing
					rebuffer();
				}
			}
		}
	}

	@Override
	protected void changePosition(Point point) {
    	long closeTime = stData.getAttributes().getTimeBegin() + (long)(point.x / getScalePixelsPerTime());
    	
    	Position currentPosition = stData.getAttributes().getPosition();
    	Position newPosition = new Position(closeTime, currentPosition.process);
    	IUndoContext context = tracePart.getContext(BaseTraceContext.CONTEXT_OPERATION_POSITION);
    		
    	try {
			tracePart.getOperationHistory().execute(
					new PositionOperation(stData, newPosition, context), 
					null, null);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	protected void changeRegion(Rectangle region) 
	{
		final ImageTraceAttributes attributes = stData.getAttributes();

		long topLeftTime 	 = attributes.getTimeBegin() + (long)(region.x / getScalePixelsPerTime());
		long bottomRightTime = attributes.getTimeBegin() + (long)((region.width+region.x) / getScalePixelsPerTime());
		
		if (bottomRightTime-topLeftTime<=1) {
			// should send error message that the length is too small
			return;
		}
		IUndoContext context = tracePart.getContext(BaseTraceContext.CONTEXT_OPERATION_TRACE);
		
		final Frame oldFrame 	= attributes.getFrame();
		final Position position = oldFrame.position;
		
		Frame frame = new Frame(topLeftTime, bottomRightTime,
				attributes.getProcessBegin(), attributes.getProcessEnd(),
				attributes.getDepth(), position.time, position.process);
		try {
			tracePart.getOperationHistory().execute(
					new ZoomOperation(stData, "Time zoom out", frame, context), 
					null, null);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
	
	private class DepthJobListener implements IJobChangeListener
	{
		
		@Override
		public void sleeping(IJobChangeEvent event) {}
		
		@Override
		public void scheduled(IJobChangeEvent event) {}
		
		@Override
		public void running(IJobChangeEvent event) {}
		
		@Override
		public void done(IJobChangeEvent event) {

			Display display = Display.getDefault();
			display.asyncExec(new Runnable() {
				
				@Override
				public void run() {
					redraw();
				}
			} );
		}
		
		@Override
		public void awake(IJobChangeEvent event) {}
		
		@Override
		public void aboutToRun(IJobChangeEvent event) {}

	}

	@Override
	public void setMessage(String message) {
		
	}

	@Override
	protected String tooltipText(int pixel, RGB rgb) {
		return null;
	}

	@Override
	protected ColorTable getColorTable() {
		return stData.getColorTable();
	}
}
