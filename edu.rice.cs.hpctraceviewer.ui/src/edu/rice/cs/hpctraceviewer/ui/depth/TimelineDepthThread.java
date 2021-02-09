package edu.rice.cs.hpctraceviewer.ui.depth;

import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;

import edu.rice.cs.hpctraceviewer.data.SpaceTimeDataController;
import edu.rice.cs.hpc.data.util.CallPath;
import edu.rice.cs.hpctraceviewer.data.DataLinePainting;
import edu.rice.cs.hpctraceviewer.data.DataPreparation;
import edu.rice.cs.hpctraceviewer.data.ImageTraceAttributes;
import edu.rice.cs.hpctraceviewer.data.TimelineDataSet;
import edu.rice.cs.hpctraceviewer.data.timeline.ProcessTimeline;
import edu.rice.cs.hpctraceviewer.ui.internal.BaseTimelineThread;


/*************************************************
 * 
 * Timeline thread for depth view
 *
 *************************************************/
public class TimelineDepthThread 
	extends BaseTimelineThread
{
	private final int visibleDepths;

	/*****
	 * Thread initialization
	 *  
	 * @param data : global data
	 * @param canvas : depth view canvas
	 * @param scaleX : The scale in the x-direction of pixels to time 
	 * @param scaleY : The scale in the y-direction of max depth
	 * @param width  : the width
	 */
	public TimelineDepthThread( SpaceTimeDataController data, 
								ImageTraceAttributes attributes,
								double scaleY, Queue<TimelineDataSet> queue, 
								AtomicInteger timelineDone, 
								IProgressMonitor monitor,
								int visibleDepths)
	{
		super(data, scaleY, queue, timelineDone, monitor);
		this.visibleDepths = visibleDepths;
	}


	@Override
	protected ProcessTimeline getNextTrace(AtomicInteger currentLine) {
		ProcessTimeline depthTrace = stData.getCurrentDepthTrace();
		if (depthTrace == null) {
			monitor.setCanceled(true);
			monitor.done(); // forcing to reset the title bar
			return null;
		}
		final ImageTraceAttributes attributes = stData.getAttributes();
		int currentDepthLineNum = currentLine.getAndIncrement();
		if (currentDepthLineNum < Math.min(attributes.getDepthPixelVertical(), visibleDepths)) {
			
			// I can't get the data from the ProcessTimeline directly, so create
			// a ProcessTimeline with data=null and then copy the actual data to
			// it.
			ProcessTimeline toDonate = new ProcessTimeline(currentDepthLineNum,
					(HashMap<Integer, CallPath>) stData.getScopeMap(), stData.getBaseData(), 
					stData.computeScaledProcess(), attributes.getPixelHorizontal(),
					attributes.getTimeInterval(), 
					stData.getMinBegTime() + attributes.getTimeBegin());

			toDonate.copyDataFrom(depthTrace);

			return toDonate;
		} else
			return null;
	}

	@Override
	protected boolean init(ProcessTimeline trace) {
		return true;
	}

	@Override
	protected void finalize() {
	}

	@Override
	protected DataPreparation getData( DataLinePainting data ) {
		int selectedDepth = stData.getAttributes().getDepth();
		int maxDepth = stData.getMaxDepth();
		int minDepth = getMinDepth(selectedDepth, visibleDepths, maxDepth);
		
		// the current depth is the current line to be painted		
		data.depth = minDepth + data.ptl.line();
		return new DepthDataPreparation(data, minDepth, visibleDepths);
	}
	
	
	/***
	 * Retrieve the first visible depth
	 * 
	 * @param currentDepth the current selected depth
	 * @param visibleDepths number of visible depths
	 * @param maxDepth maximum depths
	 * @return the first visible depth
	 */
	static int getMinDepth(int currentDepth, int visibleDepths, int maxDepth) {
		float mid = (float) (visibleDepths * 0.5);
		if (currentDepth>=0 && currentDepth<= mid)
			return 0;
		
		if (currentDepth+mid >= maxDepth)
			return (int) (maxDepth-visibleDepths);
		
		int mx = (int) (currentDepth + mid);
		return mx-visibleDepths;
	}
}
