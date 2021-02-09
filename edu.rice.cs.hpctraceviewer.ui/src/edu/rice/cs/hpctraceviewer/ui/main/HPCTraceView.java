 
package edu.rice.cs.hpctraceviewer.ui.main;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import edu.rice.cs.hpctraceviewer.data.SpaceTimeDataController;
import edu.rice.cs.hpctraceviewer.data.timeline.ProcessTimelineService;
import edu.rice.cs.hpctraceviewer.data.util.Constants;
import edu.rice.cs.hpctraceviewer.ui.base.AbstractBaseItem;
import edu.rice.cs.hpctraceviewer.ui.base.ITracePart;
import edu.rice.cs.hpctraceviewer.ui.base.ITraceViewAction;

public class HPCTraceView extends AbstractBaseItem
{
	public static final String ID_PERSPECTIVE = "edu.rice.cs.hpctraceviewer.ui.perspective.main";
	public static final String ID_PART        = "edu.rice.cs.hpctraceviewer.ui.part.main";
	
	public static final String ID_DATA = "hpctraceviewer.data";
	
	public static final int Y_AXIS_WIDTH  = 12;
	public static final int X_AXIS_HEIGHT = 20;
	
	private final ProcessTimelineService timelineService;
	
	private CanvasAxisX axisArea = null;
	private CanvasAxisY processCanvas = null;
	
	/** Paints and displays the detail view.*/
	private SpaceTimeDetailCanvas detailCanvas;
	
	@Inject
	public HPCTraceView(CTabFolder parent, int style) {
		super(parent, style);
		timelineService = new ProcessTimelineService();
	}
	
	@Override
	public void createContent(ITracePart parentPart, 
							  IEclipseContext context,
							  IEventBroker eventBroker,
							  Composite parent) {
		context.set(Constants.CONTEXT_TIMELINE, timelineService);
		
		/**************************************************************************
         * Process and Time dimension labels
         *************************************************************************/
		final Composite headerArea = new Composite(parent, SWT.NONE);
		
		Canvas headerCanvas = new Canvas(headerArea, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).
						hint(Y_AXIS_WIDTH, 20).applyTo(headerCanvas);
		
		final Composite labelGroup = new Composite(headerArea, SWT.NONE);

		GridLayoutFactory.fillDefaults().numColumns(2).generateLayout(headerArea);
		GridDataFactory.fillDefaults().grab(true, false).
						applyTo(headerArea);

		
		/*************************************************************************
		 * Detail View Canvas
		 ************************************************************************/

		Composite plotArea = new Composite(parent, SWT.NONE);
		
		processCanvas = new CanvasAxisY(parentPart, timelineService, plotArea, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).
						hint(Y_AXIS_WIDTH, 500).applyTo(processCanvas);

		
		detailCanvas = new SpaceTimeDetailCanvas(parentPart, context, eventBroker, plotArea); 

		detailCanvas.setLabels(labelGroup);
		
		GridDataFactory.fillDefaults().grab(true, true).applyTo(detailCanvas);

		detailCanvas.setVisible(true);
		
		/*************************************************************************
		 * Horizontal axis label 
		 *************************************************************************/

		Canvas footerCanvas = new Canvas(plotArea, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).
						hint(Y_AXIS_WIDTH, X_AXIS_HEIGHT).applyTo(footerCanvas);


		axisArea = new CanvasAxisX(parentPart, plotArea, SWT.NO_BACKGROUND);
		GridDataFactory.fillDefaults().grab(true, false).
						hint(500, X_AXIS_HEIGHT).applyTo(axisArea);

		GridLayoutFactory.fillDefaults().numColumns(2).generateLayout(plotArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(plotArea);
		
		/*************************************************************************
		 * Master layout 
		 *************************************************************************/
		
		GridLayoutFactory.fillDefaults().numColumns(1).generateLayout(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
	}
	
	
	public ITraceViewAction getActions() {
		return detailCanvas;
	}
	
	
	public void preDestroy() {		
	}
	

	@Override
	public void setInput(Object input) {
		SpaceTimeDataController stdc = (SpaceTimeDataController) input;
		
		detailCanvas. setData(stdc);
		axisArea.     setData(stdc);
		processCanvas.setData(stdc);
	}	
}