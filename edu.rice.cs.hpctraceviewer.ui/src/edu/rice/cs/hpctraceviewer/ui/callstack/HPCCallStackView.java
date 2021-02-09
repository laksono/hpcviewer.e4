package edu.rice.cs.hpctraceviewer.ui.callstack;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import edu.rice.cs.hpctraceviewer.data.SpaceTimeDataController;
import edu.rice.cs.hpctraceviewer.data.timeline.ProcessTimelineService;
import edu.rice.cs.hpctraceviewer.data.util.Constants;
import edu.rice.cs.hpctraceviewer.ui.base.AbstractBaseItem;
import edu.rice.cs.hpctraceviewer.ui.base.ITracePart;
import edu.rice.cs.hpctraceviewer.ui.internal.TraceEventData;
import edu.rice.cs.hpctraceviewer.ui.util.IConstants;
import edu.rice.cs.hpctraceviewer.ui.util.Utility;


/**A view for displaying the call path viewer and minimap.*/
//all the GUI setup for the call path and minimap are here//
public class HPCCallStackView extends AbstractBaseItem implements EventHandler
{
	private final static String ICON_MAX_DEPTH = "IconMaxDepth";
	private final static String MAX_DEPTH_FILE =  "platform:/plugin/edu.rice.cs.hpctraceviewer.ui/resources/max-depth16.png";

	private CallStackViewer csViewer;
	
	private Spinner depthEditor;
	
	private Button maxDepthButton;
	
	private boolean enableAction = false;
	private SpaceTimeDataController data;
	private IEventBroker broker;

	
	/***
	 * Constructor to initialize the tab item
	 * @param parent parent which is a CTabFolder
	 * @param style
	 */
	public HPCCallStackView(CTabFolder parent, int style) {
		super(parent, style);
	}

	@Override
	public void createContent(ITracePart parentPart, 
							  IEclipseContext context, 
							  IEventBroker broker,
							  Composite master) {
		
		this.broker = broker;
		setEnableAction(false);
		ProcessTimelineService ptlService = (ProcessTimelineService) context.get(Constants.CONTEXT_TIMELINE);

		/*************************************************************************
		 * Master Composite
		 ************************************************************************/
		
		master.setLayout(new GridLayout());
		master.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));
		
		/*************************************************************************
		 * Depth area. Consist of:
		 * - Depth View Spinner (the thing with the text box and little arrow buttons)
		 * - max depth (a shortcut to go to the maximum depth). See issue #64
		 ************************************************************************/
		
		Composite depthArea = new Composite(master, SWT.BORDER); 
		
		final Label lblDepth = new Label(depthArea, SWT.LEFT);
		lblDepth.setText("Depth: ");
		
		depthEditor = new Spinner(depthArea, SWT.BORDER);
		depthEditor.setMinimum(0);
		depthEditor.setPageIncrement(1);
		
		depthEditor.setLayout(new GridLayout());
		GridData depthData = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		depthData.widthHint = 50;
		depthEditor.setLayoutData(depthData);
		depthEditor.setVisible(false);
		
		maxDepthButton = new Button(depthArea, 0);		
		maxDepthButton.setEnabled(false);
		
		Image image = Utility.getImage(MAX_DEPTH_FILE, ICON_MAX_DEPTH);
		maxDepthButton.setImage(image);

		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(maxDepthButton);
		
		GridDataFactory.fillDefaults().grab(true, false).applyTo(depthArea);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(depthArea);
		
		maxDepthButton.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!getEnableAction())
					return;
				
				Integer depth = (Integer) maxDepthButton.getData();
				if (depth == null || depth.intValue() <= 0)
					return;

				depthEditor.setSelection(depth);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		depthEditor.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				if (!getEnableAction())
					return;
				
				String string = depthEditor.getText();
				int value = 0;
				if (string.length()<1) {
					// be careful: on linux/GTK, any change in the spinner will consists of two steps:
					//  1) empty the string
					//  2) set with the specified value
					// therefore, we consider any empty string to be illegal
					return;
				} else {
					try {
						value = Integer.valueOf(string);
					} catch (final NumberFormatException errorException) {
						e.display.asyncExec(new Runnable() {
							
							@Override
							public void run() {
								Color yellow = depthEditor.getDisplay().getSystemColor(SWT.COLOR_YELLOW);
								depthEditor.setBackground(yellow);
								depthEditor.setToolTipText("Incorrect number: " + errorException.getMessage());
							}
						});
						return;
					}
				}
				
				final int maximum = depthEditor.getMaximum();
				int minimum = 0;

				if (value > maximum) {
					value = maximum;
					
					e.display.asyncExec(new Runnable() {
						
						@Override
						public void run() {
							Shell shell = e.widget.getDisplay().getActiveShell();
							MessageDialog.openWarning(shell, "Value not allowed", 
									  "The value is higher than the maximum depth (" + maximum +").");
						}
					});
				}
				if (value < minimum) {
					value = minimum;
				}
				csViewer.setDepth(value);
			}
		});
		
		/*************************************************************************
		 * CallStackViewer
		 ************************************************************************/
		csViewer = new CallStackViewer(parentPart, master, this, ptlService, broker);
		
		setToolTipText("The view to show the depth and the actual call path for the point selected by the Trace View's crosshair");
	}
		
	
	/***
	 * Update the current view with the new database
	 * @param _stData the new database
	 */
	public void updateView(SpaceTimeDataController _stData) 
	{
		this.data = _stData;
		
		// guard : no action has to be taken at the moment;
		setEnableAction(false);
		
		// Fix bug #14: extra depth https://github.com/HPCToolkit/hpcviewer.e4/issues/14
		// By default hpcdata computes the depth based on the line scope, not procedure scope.
		// TODO: Ideally we need correct the max depth inside hpcdata, but unfortunately other classes
		//  like many in depth views, depend on the definition that max_depth = max_call_path + 1
		// Hence, it's simpler to correct the max depth here.
		
		final int maxDepth = _stData.getMaxDepth()-1;
		depthEditor.setSelection(0);
		depthEditor.setMaximum(maxDepth);		
		depthEditor.setVisible(true);
		depthEditor.setToolTipText("Change the current depth.\nMax depth is " + maxDepth);
		
		depthEditor.setSelection(data.getDefaultDepth());

		maxDepthButton.setToolTipText("Set to max depth: " + maxDepth);
		maxDepthButton.setData(Integer.valueOf(maxDepth));
		maxDepthButton.setEnabled(true);

		// instead of updating the content of the view, we just make the table
		// visible, and let other event to trigger the update content.
		// at this point, a data may not be ready to be processed
		csViewer.getTable().setVisible(true);

		// enable action
		setEnableAction(true);
		
		broker.subscribe(IConstants.TOPIC_DEPTH_UPDATE, this);
	}

	private void setEnableAction(boolean enabled) {
		enableAction = enabled;
	}
	
	private boolean getEnableAction() {
		return enableAction;
	}
	

	@Override
	public void setInput(Object input) {
		csViewer.setInput((SpaceTimeDataController)input);
		updateView((SpaceTimeDataController) input);
	}

	@Override
	public void handleEvent(Event event) {

		Object obj = event.getProperty(IEventBroker.DATA);
		if (obj == null) return;
		
		TraceEventData eventData = (TraceEventData) obj;
		if (eventData.source == this || eventData.data != this.data)
			return;
		
		if (event.getTopic().equals(IConstants.TOPIC_DEPTH_UPDATE)) {
			Integer depth = (Integer) eventData.value;
			
			setEnableAction(false);
			depthEditor.setSelection(depth.intValue());
			setEnableAction(true);
		}
	}
}
