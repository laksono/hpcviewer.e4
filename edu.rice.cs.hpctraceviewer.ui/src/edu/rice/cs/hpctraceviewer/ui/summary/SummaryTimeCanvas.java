package edu.rice.cs.hpctraceviewer.ui.summary;

import java.util.Map.Entry;
import java.util.TreeMap;


import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.widgets.Composite;

import edu.rice.cs.hpctraceviewer.ui.base.IPixelAnalysis;
import edu.rice.cs.hpctraceviewer.ui.base.ITracePart;
import edu.rice.cs.hpctraceviewer.ui.context.BaseTraceContext;
import edu.rice.cs.hpctraceviewer.ui.internal.AbstractTimeCanvas;
import edu.rice.cs.hpctraceviewer.ui.internal.TraceEventData;
import edu.rice.cs.hpctraceviewer.ui.operation.AbstractTraceOperation;
import edu.rice.cs.hpctraceviewer.ui.operation.BufferRefreshOperation;
import edu.rice.cs.hpctraceviewer.ui.operation.ZoomOperation;
import edu.rice.cs.hpctraceviewer.ui.util.IConstants;
import edu.rice.cs.hpctraceviewer.data.SpaceTimeDataController;
import edu.rice.cs.hpctraceviewer.data.timeline.ProcessTimelineService;
import edu.rice.cs.hpctraceviewer.data.Frame;
import edu.rice.cs.hpctraceviewer.data.ImageTraceAttributes;
import edu.rice.cs.hpctraceviewer.data.Position;
import edu.rice.cs.hpctraceviewer.data.ColorTable;
import edu.rice.cs.hpctraceviewer.data.util.Constants;

/******************************************************************
 * 
 * Canvas class for summary view
 *
 ******************************************************************/
public class SummaryTimeCanvas extends AbstractTimeCanvas implements IOperationHistoryListener 
{
	private final IEventBroker eventBroker;
	private final ITracePart tracePart;

	private final int zoomFactor;
	private final ProcessTimelineService ptlService;
	
	private SpaceTimeDataController dataTraces = null;
	private TreeMap<Integer /* pixel */, Integer /* percent */> mapPixelToPercent;	
	
	private int totPixels;
	private ImageData detailData;

	private IPixelAnalysis analysisTool;
	
	/**********************************
	 * Construct a summary canvas without background nor scrollbar
	 * 
	 * @param composite
	 **********************************/
	public SummaryTimeCanvas(ITracePart tracePart, 
							 Composite composite, 
							 IEclipseContext context, 
							 IEventBroker eventBroker) {
		super(composite, SWT.NO_BACKGROUND);

		this.eventBroker = eventBroker;
		this.tracePart   = tracePart;
		analysisTool     = IPixelAnalysis.EMPTY;
		
		mapPixelToPercent = new TreeMap<Integer, Integer>();		
		
		ptlService = (ProcessTimelineService) context.get(Constants.CONTEXT_TIMELINE);

		// It is critical to reconstruct the image data according to Device zoom
		// On Mac with retina display, the hardware pixel has 4x pixels than
		// the swt level. Retrieving pixel without adapting with device zoom
		// will return incorrect pixel.
		
		int deviceZoom = DPIUtil.getDeviceZoom();
		zoomFactor     = deviceZoom / 100;

		tracePart.getOperationHistory().addOperationHistoryListener(this);
	}

	
	public void setAnalysisTool(IPixelAnalysis analysisTool) {
		this.analysisTool = analysisTool;
	}
	
	
	@Override
	public void paintControl(PaintEvent event) {
		if (dataTraces == null)
			return;

		super.paintControl(event);
		refreshWithCondition();
	}

	@Override
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.
	 * DisposeEvent)
	 */
	public void widgetDisposed(DisposeEvent e) {
		tracePart.getOperationHistory().removeOperationHistoryListener(this);
	}

	private void refreshWithCondition() {
		if (getBuffer() == null) {
			// ------------------------------------------------------------------------
			// ------------------------------------------------------------------------
			rebuffer(detailData);
			return;
		}

		// ------------------------------------------------------------------------
		// we need to avoid repainting if the size of the image buffer is not the same
		// as the image of the canvas. This case happens when the view is resize while
		// it's in hidden state, and then it turns visible.
		// this will cause misalignment in the view
		// ------------------------------------------------------------------------
		final Rectangle r1 = getBuffer().getBounds();
		final Rectangle r2 = getClientArea();

		if (!(r1.height == r2.height && r1.width == r2.width)) {
			rebuffer(detailData);
			return;
		}
	}

	
	/*****
	 * rebuffers the data in the summary time canvas and then asks receiver to paint
	 * it again
	 *****/
	private void rebuffer(ImageData detailData) {
		// store the original image data for further usage such as when the painting is
		// needed.
		this.detailData = detailData;

		if (detailData == null)
			return;

		// ------------------------------------------------------------------------------------------
		// let use GC instead of ImageData since GC allows us to draw lines and
		// rectangles
		// ------------------------------------------------------------------------------------------
		initBuffer();

		final int viewWidth = getBounds().width;
		final int viewHeight = getBounds().height;

		if (viewWidth == 0 || viewHeight == 0)
			return;

		final Image imageBuffer = new Image(getDisplay(), viewWidth, viewHeight);
		setBuffer(imageBuffer);

		GC buffer = new GC(imageBuffer);
		buffer.setBackground(Constants.COLOR_WHITE);
		buffer.fillRectangle(0, 0, viewWidth, viewHeight);

		float width  = detailData.width / zoomFactor;
		float height = detailData.height / zoomFactor;

		float yScale = (float) viewHeight / height;
		float xScale = ((float) viewWidth / width);

		int xOffset = 0;
		
		mapPixelToPercent.clear();

		// ----------------------------------
		// plugin initialization
		// ----------------------------------
		analysisTool.analysisInit(dataTraces, getColorTable(), ptlService);
		
		// ---------------------------------------------------------------------------
		// needs to be optimized:
		// for every pixel along the width, check the pixel, group them based on color,
		// count the amount of each group, and draw the pixel
		// ---------------------------------------------------------------------------
		
		for (int x = 0; x < width; ++x) {
			// ---------------------------------------------------------------------------
			// use tree map to sort the key of color map
			// without sort, it can be confusing
			// ---------------------------------------------------------------------------
			TreeMap<Integer, Integer> mapPixelToCount  = new TreeMap<Integer, Integer>();
			
			// ------------------------------------------------------------------------
			// Analysis plugin				
			// ------------------------------------------------------------------------
			analysisTool.analysisPixelInit(x);

			for (int y = 0; y < height; ++y) { // One iter per trace line

				int pixelValue = detailData.getPixel(x*zoomFactor, y*zoomFactor);
				
				Integer old = mapPixelToCount.get(pixelValue);
				if (old != null) {
					old++;
					mapPixelToCount.put(pixelValue, old);
				} else {
					mapPixelToCount.put(pixelValue, 1);
				}
				
				// ------------------------------------------------------------------------
				// Analysis plugin				
				// ------------------------------------------------------------------------
				analysisTool.analysisPixelXY(detailData, x, y, pixelValue);
			}
			
			// ---------------------------------------------------------------------------
			// draw the line of a specific color with a specific length from bottom to the
			// top
			// note: the coordinates 0,0 starts from the top-left corner !
			// ---------------------------------------------------------------------------

			int yOffset = viewHeight;
			int h = 0;
			int i = 1;
			int size = mapPixelToCount.size();
			
			for (Entry<Integer, Integer> entry: mapPixelToCount.entrySet()) {
				final Integer pixel = entry.getKey();
				final Integer count = entry.getValue();

				final RGB rgb = detailData.palette.getRGB(pixel);
				final Color c = new Color(getDisplay(), rgb);

				final int yLength = (int) Math.ceil(count * yScale);

				buffer.setBackground(c);

				// if this is the last color, we should draw from the current position to the end
				// this may not be the best solution, but the round-up in height variable may give
				// empty spaces if the number of colors are not a height's divisor.

				if (i==size) {
					buffer.fillRectangle(xOffset, yOffset - yLength, (int) Math.max(1, xScale), yLength);
				} else {
					buffer.fillRectangle(xOffset, 0, (int) Math.max(1, xScale), viewHeight - h);
				}
				i++;
				yOffset -= yLength;
				c.dispose();

				// accumulate the statistics of this pixel
				Integer val = mapPixelToPercent.get(pixel);
				Integer acc = (val == null ? count : val + count);
				mapPixelToPercent.put(pixel, acc);
				
				h += yLength;

				// ----------------------------------------------------------------------------
				// Recap analysis plugin:
				// ----------------------------------------------------------------------------
				analysisTool.analysisPixelFinal(pixel);
			}
			xOffset = Math.round(xOffset + xScale);
		}
		totPixels = (int) (width * height);

		buffer.dispose();

		redraw();

		SummaryData data = new SummaryData(detailData.palette, mapPixelToPercent, getColorTable(), totPixels);
		TraceEventData eventData = new TraceEventData(dataTraces, this, data);
		
		eventBroker.post(IConstants.TOPIC_STATISTICS, eventData);
		
		// ----------------------------------------------------------------------------
		// Finalize external plugin
		// ----------------------------------------------------------------------------
		analysisTool.analysisFinal(detailData);
	}

	
	
	/****
	 * main method to decide whether we want to create a new buffer or just to
	 * redraw the canvas
	 * 
	 * @param _detailData : new data
	 */
	private void refresh(ImageData detailData) {
		super.init();
		rebuffer(detailData);
	}

	/********
	 * set the new database
	 * 
	 * @param data
	 ********/
	public void updateData(SpaceTimeDataController data) {
		dataTraces = data;
		setVisible(true);
		redraw();
	}

	/*****
	 * get the number of pixel per time unit
	 * 
	 * @return
	 */
	private double getScalePixelsPerTime() {
		final int viewWidth = getClientArea().width;

		return (double) viewWidth / (double) getNumTimeDisplayed();
	}

	/******
	 * get the time interval displayed on the canvas
	 * 
	 * @return
	 */
	private long getNumTimeDisplayed() {
		return (dataTraces.getAttributes().getTimeInterval());
	}


	// ---------------------------------------------------------------------------------------
	// Override methods
	// ---------------------------------------------------------------------------------------

	@Override
	public void historyNotification(final OperationHistoryEvent event) {

		if (isDisposed())
			return;

		// we are not interested with other operation contexts
		IUndoContext context = tracePart.getContext(BaseTraceContext.CONTEXT_OPERATION_BUFFER);
		if (!event.getOperation().hasContext(context)) 
			return;
		
		if (event.getEventType() == OperationHistoryEvent.DONE) {

			final IUndoableOperation operation = event.getOperation();
			if (!(operation instanceof AbstractTraceOperation)) {
				return;
			}
			final AbstractTraceOperation op = (AbstractTraceOperation) operation;
			if (op.getData() != dataTraces)
				return;

			getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					BufferRefreshOperation operation = (BufferRefreshOperation) op;
					refresh(operation.getImageData());
				}
			});
		}
	}

	@Override
	protected void changePosition(Point point) {}
	

	@Override
	protected void changeRegion(Rectangle region) {
		final ImageTraceAttributes attributes = dataTraces.getAttributes();

		long timeBegin = attributes.getTimeBegin();
		int left  = region.x;
		int right = region.width + region.x;

		long topLeftTime     = timeBegin + (long) (left  / getScalePixelsPerTime());
		long bottomRightTime = timeBegin + (long) (right / getScalePixelsPerTime());

		final Position position = attributes.getPosition();

		final Frame frame = new Frame(topLeftTime, bottomRightTime, attributes.getProcessBegin(),
				attributes.getProcessEnd(), attributes.getDepth(), position.time, position.process);

		IUndoContext context = tracePart.getContext(BaseTraceContext.CONTEXT_OPERATION_TRACE);
		try {
			tracePart.getOperationHistory().execute(new ZoomOperation(dataTraces, "Time zoom in", frame, context), null,
					null);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

	}

	@Override
	protected String tooltipText(int pixel, RGB rgb) {

		// ------------------------------------------------
		// get the number of counts of this pixel
		// ------------------------------------------------
		Integer stat = mapPixelToPercent.get(pixel);

		if (stat != null) {
			// ------------------------------------------------
			// compute the percentage
			// ------------------------------------------------
			float percent = (float) 100.0 * ((float) stat / (float) totPixels);

			if (percent > 0) {
				final String percent_str = String.format("%.2f %%\n", percent);
				return percent_str;
			}
		}
		return null;
	}

	@Override
	protected ColorTable getColorTable() {
		return dataTraces.getColorTable();
	}
}