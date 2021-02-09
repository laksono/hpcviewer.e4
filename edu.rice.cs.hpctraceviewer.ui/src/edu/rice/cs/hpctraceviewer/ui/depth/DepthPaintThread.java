package edu.rice.cs.hpctraceviewer.ui.depth;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import edu.rice.cs.hpctraceviewer.data.SpaceTimeDataController;
import edu.rice.cs.hpctraceviewer.data.BaseDataVisualization;
import edu.rice.cs.hpctraceviewer.data.TimelineDataSet;
import edu.rice.cs.hpctraceviewer.ui.internal.BasePaintThread;
import edu.rice.cs.hpctraceviewer.ui.internal.ImagePosition;


public class DepthPaintThread extends BasePaintThread {

	private Image image;
	private GC gc;

	public DepthPaintThread(SpaceTimeDataController stData, Queue<TimelineDataSet> list, int linesToPaint, 
			AtomicInteger numDataCollected, AtomicInteger paintDone,
			int width, IProgressMonitor monitor) {

		super(stData, list, linesToPaint, numDataCollected, paintDone, width, monitor);
	}

	@Override
	protected void initPaint(/*Device device,*/ int width, int height) {
		final Display device = Display.getDefault();
		image = new Image(device, width, height);
		gc    = new GC(image);
	}

	@Override
	protected void paint(int position, BaseDataVisualization data, int height) {
		// display only if the current thread line number is within the depth
		// note that the line number starts from zero while depth starts from 1 (I think)
		if (position < data.depth) {
			paint(gc, data.x_start, data.x_end, height, data.color);
		}
	}

	@Override
	protected ImagePosition finalizePaint(int linenum) {

		gc.dispose();
		return new ImagePosition(linenum, image);
	}
}
