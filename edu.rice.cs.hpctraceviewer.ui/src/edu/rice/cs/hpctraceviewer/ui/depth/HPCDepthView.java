package edu.rice.cs.hpctraceviewer.ui.depth;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import edu.rice.cs.hpctraceviewer.data.SpaceTimeDataController;
import edu.rice.cs.hpctraceviewer.ui.base.AbstractBaseItem;
import edu.rice.cs.hpctraceviewer.ui.base.ITracePart;
import edu.rice.cs.hpctraceviewer.ui.main.HPCTraceView;

/*****************************************************
 * 
 * Depth view
 *
 *****************************************************/
public class HPCDepthView extends AbstractBaseItem
{
	private static final int VIEW_HEIGHT_HINT = 40;
	
	/** Paints and displays the detail view. */
	private DepthTimeCanvas depthCanvas;

	
	public HPCDepthView(CTabFolder parent, int style) {
		super(parent, style);
	}

		
	@Override
	public void createContent(ITracePart parentPart, 
							  IEclipseContext context,
							  IEventBroker broker,
							  Composite master) 
	{		
		final Composite plotArea = new Composite(master, SWT.NONE);
		
		/*************************************************************************
		 * Padding Canvas
		 *************************************************************************/
		
		final Canvas axisCanvas = new Canvas(plotArea, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).
						hint(HPCTraceView.Y_AXIS_WIDTH, VIEW_HEIGHT_HINT).applyTo(axisCanvas);
		
		/*************************************************************************
		 * Depth View Canvas
		 *************************************************************************/
		
		depthCanvas = new DepthTimeCanvas(parentPart, plotArea);
		GridDataFactory.fillDefaults().grab(true, true).
						hint(500, VIEW_HEIGHT_HINT).applyTo(depthCanvas);

		/*************************************************************************
		 * Master Composite
		 *************************************************************************/
		
		GridDataFactory.fillDefaults().grab(true, true).applyTo(plotArea);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(plotArea);
		
		setToolTipText("The view to show for a given process, its virtual time along the horizontal axis, and a call path" +
				" along the vertical axis, where `main' is at the top and leaves (samples) are at the bottom.");
	}


	@Override
	public void setInput(Object input) {
		depthCanvas.updateView((SpaceTimeDataController) input);
		depthCanvas.refresh();
	}
	
	
	
	/****
	 * Zoom out the depth: increase the depth so users can see more 
	 */
	public void zoomOut() {
		depthCanvas.zoomOut();
	}
	
	
	/****
	 * Zoom in the depth: decrease the depth so user can see more pixels
	 */
	public void zoomIn() {
		depthCanvas.zoomIn();
		 
	}
	
	
	/****
	 * check if we can zoom out
	 * @return true if it's feasible
	 */
	public boolean canZoomOut() {
		return depthCanvas.canZoomOut();
	}
	
	
	/****
	 * check if can zoom in
	 * @return true if it's possible
	 */
	public boolean canZoomIn() {
		return depthCanvas.canZoomIn();
	}

}
