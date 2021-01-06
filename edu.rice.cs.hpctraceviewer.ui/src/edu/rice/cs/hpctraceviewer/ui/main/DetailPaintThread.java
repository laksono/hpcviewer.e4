package edu.rice.cs.hpctraceviewer.ui.main;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Point;

import edu.rice.cs.hpcbase.BaseConstants;
import edu.rice.cs.hpctraceviewer.data.BaseDataVisualization;
import edu.rice.cs.hpctraceviewer.data.SpaceTimeDataController;
import edu.rice.cs.hpctraceviewer.data.TimelineDataSet;
import edu.rice.cs.hpctraceviewer.data.util.Constants;
import edu.rice.cs.hpctraceviewer.ui.internal.BasePaintThread;
import edu.rice.cs.hpctraceviewer.ui.internal.ImagePosition;


/*****************************************************************
 * 
 * Thread class to paint a set of data into a set of images
 * Once the object terminates, it will return the list of images to 
 * be visualized in a view
 *
 *****************************************************************/
public class DetailPaintThread 
	extends BasePaintThread
{
	final private boolean debugMode;

	final private Point  maxTextSize;
	final private Device device;
	
	private Image lineFinal;
	private Image lineOriginal;
	private GC gcFinal;
	private GC gcOriginal;
	
	/****
	 * constructor of the class, requiring a queue of list of data (per line) to be
	 * visualized on a set of images. The queue can be thread-safe (in case of multithreaded)
	 * or unsafe (case of single threaded) 
	 * 
	 * The class will return a list of images.
	 * 
	 * @param list : the queue of TimelineDataSet data
	 * @param numLines
	 * @param device : the display device used to create images. Cannot be null
	 * @param width : the width of the view
	 * @param maxTextSize : the maximum size of a letter for a given device
	 * @param debugMode : flag whether we need to show text information
	 */
	public DetailPaintThread(Device device, SpaceTimeDataController stData, Queue<TimelineDataSet> list, int numLines,
			AtomicInteger numDataCollected, AtomicInteger paintDone, int width, 
			Point maxTextSize, boolean debugMode,
			IProgressMonitor monitor) {
		
		super(stData, list, numLines, numDataCollected, paintDone, width, monitor);
		this.device      = device;
		this.maxTextSize = maxTextSize;
		this.debugMode   = debugMode;
	}
	
	private void paintText(GC gc, int odInitPixel, int odFinalPixel, int box_height, 
			int depth, Color color, int sampleCount) {
		if (!debugMode) {
			return;
		}
		final int box_width = odFinalPixel - odInitPixel;
		
		String decoration = String.valueOf(depth);
		
		String count = String.valueOf(sampleCount);
		if (sampleCount>DetailViewPaint.MAX_RECORDS_DISPLAY)
			count = DetailViewPaint.TOO_MANY_RECORDS;
		decoration +=  "(" + count + ")";

		// want 2 pixels on either side
		if((box_width - maxTextSize.x) >= 4) {

			// want 2 pixels on above and below
			if ((box_height - maxTextSize.y) >= 4) {

				gc.setBackground(color);

				// Pick the color of the text indicating sample depth. 
				// If the background is suffciently light, pick black, otherwise white
				if (color.getRed()+color.getBlue()+color.getGreen()>BaseConstants.DARKEST_COLOR_FOR_BLACK_TEXT)
					gc.setForeground(Constants.COLOR_BLACK);
				else
					gc.setForeground(Constants.COLOR_WHITE);
				
				Point textSize = gc.textExtent(decoration);
				gc.drawText(decoration, odInitPixel+((box_width - textSize.x)/2), ((box_height - textSize.y)/2));
			}
		}
	}

	@Override
	protected void initPaint(/*Device device, */int width, int height) {

		lineFinal = new Image(device, width, height);
		lineOriginal = new Image(device, width, 1);
		
		gcFinal = new GC(lineFinal);
		gcOriginal = new GC(lineOriginal);		
	}

	@Override
	protected void paint(int position, BaseDataVisualization data, int height) {

		// paint for the original image without text
		// this image will be needed for summary view to compute
		//	the number of colors-pixels
		paint(gcOriginal, data.x_start, data.x_end, 1, data.color);
		
		// paint the real image for detail view
		paint(gcFinal, data.x_start, data.x_end, height, data.color);
		
		// paint the text on the real image
		final DetailDataVisualization dataDetail = (DetailDataVisualization) data;
		paintText(gcFinal, data.x_start, data.x_end, height,
				data.depth, data.color, dataDetail.sample_counts);
	}

	@Override
	protected ImagePosition finalizePaint(int linenum) {
		
		final ImagePosition imgPos = new DetailImagePosition(linenum, lineFinal, lineOriginal);

		gcOriginal.dispose();
		gcFinal.dispose();

		return imgPos;
	}
}
