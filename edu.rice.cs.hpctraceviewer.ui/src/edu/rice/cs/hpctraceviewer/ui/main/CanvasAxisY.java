package edu.rice.cs.hpctraceviewer.ui.main;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;

import edu.rice.cs.hpc.data.experiment.extdata.IBaseData;
import edu.rice.cs.hpctraceviewer.data.SpaceTimeDataController;
import edu.rice.cs.hpctraceviewer.data.ImageTraceAttributes;
import edu.rice.cs.hpctraceviewer.data.timeline.ProcessTimeline;
import edu.rice.cs.hpctraceviewer.data.timeline.ProcessTimelineService;
import edu.rice.cs.hpctraceviewer.ui.base.ITraceCanvas;
import edu.rice.cs.hpctraceviewer.ui.base.ITracePart;
import edu.rice.cs.hpctraceviewer.ui.base.ITraceCanvas.MouseState;


/*********************
 * 
 * Canvas to draw vertical axis of the main view
 *
 *********************/
public class CanvasAxisY extends AbstractAxisCanvas 
{
	static final private int COLUMN_WIDTH = 5;
	
	private final int []listColorSWT = {
											SWT.COLOR_CYAN,   SWT.COLOR_DARK_BLUE,
											SWT.COLOR_YELLOW, SWT.COLOR_DARK_MAGENTA,
											SWT.COLOR_GRAY,   SWT.COLOR_DARK_GREEN,
											SWT.COLOR_WHITE,  SWT.COLOR_DARK_RED
										};
	private final Color []listColorObjects;
	private final Color bgColor;
	private final ProcessTimelineService timeLine;

	/** Relates to the condition that the mouse is in.*/
	private ITraceCanvas.MouseState mouseState;
	
	private AxisToolTip tooltip = null;

	
	/****
	 * Constructor CanvasAxisY
	 * 
	 * @param tracePart ITracePart the parent of the view
	 * @param timeLine ProcessTimelineService
	 * @param parent Composite
	 * @param style int (see {@code SWT} constants for canvas)
	 */
	public CanvasAxisY(ITracePart tracePart, ProcessTimelineService timeLine, Composite parent, int style) {
		super(tracePart, parent, style);
		
		bgColor = parent.getBackground();
		this.timeLine = timeLine;
		
		listColorObjects = new Color[listColorSWT.length];
		for(int i=0; i<listColorSWT.length; i++) {
			listColorObjects[i] = getDisplay().getSystemColor(listColorSWT[i]);
		}
		
		mouseState = MouseState.ST_MOUSE_INIT;
	}
	
	@Override
	public void setData(Object data) {
		super.setData(data);
		
		if (mouseState == MouseState.ST_MOUSE_INIT) {
			
			tooltip = new AxisToolTip(this);
			
			mouseState = MouseState.ST_MOUSE_NONE;
		}
		tooltip.setData((SpaceTimeDataController) data);
	}
	

	
	@Override
	public void paintControl(PaintEvent e) {

		if (getData() == null)
			return;
				
		final SpaceTimeDataController data   = (SpaceTimeDataController) getData();
        final IBaseData traceData 		     = data.getBaseData();

        if (traceData == null)
        	return;

		final ImageTraceAttributes attribute = data.getAttributes();
		
        final String processes[] = traceData.getListOfRanks();
        
        if (processes == null || processes.length==0)
        	return;
        
        int numLevels = traceData.getNumLevels();
        boolean isHybridProgram  = numLevels > 1;
		
		// --------------------------------------------------------------------------
        // Manually fill the client area with the default background color
        // Some platforms don't paint the background properly 
		// --------------------------------------------------------------------------
        
		e.gc.setBackground(bgColor);
		e.gc.fillRectangle(getClientArea());

		// -----------------------------------------------------
		// collect the position and the length of each process
		// -----------------------------------------------------
		List<Integer> listProcPosition   = new ArrayList<Integer>();
		List<Integer> listThreadPosition = new ArrayList<Integer>();

		listProcPosition.  add(0);
		listThreadPosition.add(0);
		
		int oldRank   = 0;
		int oldThread = 0;
		
		for (int i=0; i<getNumProcessTimeline(); i++) {
			ProcessTimeline procTimeline = getProcessTimeline(i);
			if (procTimeline == null)
				continue;
			
			final int procNumber  = procTimeline.getProcessNum();
			if (procNumber >= processes.length)
				// inconsistency between the list of processes and the current timeline
				// probably hpctraceviewer is in the middle of rebuffering
				return;
			
			final String procName = processes[procNumber]; 
			final int position    = attribute.convertRankToPixel(procNumber);
	
			int rank = 0;
			int thread = 0;

			if (!isHybridProgram) {
				
				// either pure MPI or pure OpenMP threads
				
				rank = Integer.valueOf(procName);
				
				if (oldRank != rank) {
					listProcPosition.add(position);
					oldRank = rank;
				}
				continue;
			}
			
			// hybrid application
			
			int dotIndex = procName.indexOf('.');
			if (dotIndex >= 0) {
				String strRank   = procName.substring(0, dotIndex);
				String strThread = procName.substring(dotIndex+1);
				
				rank   = Integer.valueOf(strRank);
				thread = Integer.valueOf(strThread);
			}
			
			if (oldRank != rank) {
				listProcPosition.add(position);
				oldRank = rank;
			}
			if (oldThread != thread) {
				listThreadPosition.add(position);
				oldThread = thread;
			}
		}
		listProcPosition.  add(getClientArea().height);
		listThreadPosition.add(getClientArea().height);
		
		// -----------------------------------------------------
		// draw MPI column
		// -----------------------------------------------------
		int currentColor = 0;
		int limitColor   = 1;
		int x_end = isHybridProgram ? COLUMN_WIDTH : COLUMN_WIDTH * 2;
		
		for (int i=0; i<listProcPosition.size()-1; i++) {
			currentColor = limitColor - ((i+1) % 2);

			e.gc.setBackground(listColorObjects[currentColor]);
			
			Integer procPosition = listProcPosition.get(i);
			Integer nextPosition = listProcPosition.get(i+1);
			
			e.gc.fillRectangle(0, procPosition, x_end, nextPosition);
		}
		if (!isHybridProgram)
			return;
		
		// -----------------------------------------------------
		// draw thread column
		// only for hybrid application
		// -----------------------------------------------------
		
		limitColor += 2;
		int xEnd = 2 * COLUMN_WIDTH;
		
		for (int i=0; i<listThreadPosition.size()-1; i++) {
			
			currentColor = limitColor - ((i+1) % 2);
			e.gc.setBackground(listColorObjects[currentColor]);
			
			Integer threadPosition = listThreadPosition.get(i);
			Integer nextPosition   = listThreadPosition.get(i+1);
			
			e.gc.fillRectangle(COLUMN_WIDTH+1, threadPosition, xEnd, nextPosition);
		}
	}
	
	
	@Override
	public void dispose() {
		if (tooltip != null) {
			tooltip.deactivate();
			tooltip = null;
		}
		
		super.dispose();
	}
	
	
	protected int getNumProcessTimeline() {
		return timeLine.getNumProcessTimeline();
	}
	
	
	protected ProcessTimeline getProcessTimeline(int i) {
		return timeLine.getProcessTimeline(i);
	}

	
	/********************************************************
	 * 
	 * Tooltip class to show the rank and/or the thread of the 
	 *  current position.
	 *  
	 *  Caller needs to set data every time there is a new data
	 *
	 ********************************************************/
	static private class AxisToolTip extends DefaultToolTip
	{
		private SpaceTimeDataController data;

		public AxisToolTip(Control control) {
			super(control);
		}
		
		void setData(SpaceTimeDataController data) {
			this.data = data;
		}
	
		@Override
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.window.DefaultToolTip#getText(org.eclipse.swt.widgets.Event)
		 */
		protected String getText(Event event) {
			
	        final IBaseData traceData = data.getBaseData();

	        if (traceData == null)
	        	return null;

			final ImageTraceAttributes attribute = data.getAttributes();
			int process = attribute.convertPixelToRank(event.y);
			
			String procNames[] = traceData.getListOfRanks();
			
			if (process < 0 && process >= procNames.length)
				return null;
			
	        String text = procNames[process];
	        
	        if (traceData.isHybridRank()) {
	        	int indexDot = text.indexOf('.');
	        	String rank = text.substring(0, indexDot);
	        	
	        	if (event.x <= COLUMN_WIDTH) {
	        		text = "Rank " + rank;
	        	} else {
		        	text = text + " (rank: " + rank + ", thread: " + text.substring(indexDot+1) + ")";
	        	}
	        	return text;
	        } else {
	        	text = "Rank " + text;
	        }
			
			return text;
		}
		
		@Override
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.window.ToolTip#getLocation(org.eclipse.swt.graphics.Point, org.eclipse.swt.widgets.Event)
		 */
		public Point getLocation(Point tipSize, Event event) {
			Object obj = getToolTipArea(event);
			Control control = (Control) obj;
			
			return control.toDisplay(event.x+5, event.y-15);
		}
	}
}