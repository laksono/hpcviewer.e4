 
package edu.rice.cs.hpctraceviewer.ui;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import edu.rice.cs.hpc.data.db.IdTupleType;
import edu.rice.cs.hpc.data.experiment.BaseExperiment;
import edu.rice.cs.hpc.data.experiment.extdata.IBaseData;
import edu.rice.cs.hpcbase.BaseConstants;
import edu.rice.cs.hpctraceviewer.data.AbstractDBOpener;
import edu.rice.cs.hpctraceviewer.data.SpaceTimeDataController;
import edu.rice.cs.hpctraceviewer.data.local.LocalDBOpener;
import edu.rice.cs.hpctraceviewer.ui.base.AbstractBaseItem;
import edu.rice.cs.hpctraceviewer.ui.base.IPixelAnalysis;
import edu.rice.cs.hpctraceviewer.ui.base.ITracePart;
import edu.rice.cs.hpctraceviewer.ui.base.ITraceViewAction;
import edu.rice.cs.hpctraceviewer.ui.blamestat.CpuBlameAnalysis;
import edu.rice.cs.hpctraceviewer.ui.blamestat.HPCBlameView;
import edu.rice.cs.hpctraceviewer.ui.callstack.HPCCallStackView;
import edu.rice.cs.hpctraceviewer.ui.context.BaseTraceContext;
import edu.rice.cs.hpctraceviewer.ui.depth.HPCDepthView;
import edu.rice.cs.hpctraceviewer.ui.internal.TraceEventData;
import edu.rice.cs.hpctraceviewer.ui.main.HPCTraceView;
import edu.rice.cs.hpctraceviewer.ui.minimap.SpaceTimeMiniCanvas;
import edu.rice.cs.hpctraceviewer.ui.preferences.TracePreferenceManager;
import edu.rice.cs.hpctraceviewer.ui.statistic.HPCStatView;
import edu.rice.cs.hpctraceviewer.ui.summary.HPCSummaryView;
import edu.rice.cs.hpctraceviewer.ui.util.IConstants;

import javax.annotation.PreDestroy;

import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.IPartListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;


/***************************************************************
 * 
 * The main class for trace viewer
 *
 ***************************************************************/
public class TracePart implements ITracePart, IPartListener, IPropertyChangeListener, EventHandler
{
	public static final String ID = "edu.rice.cs.hpctraceviewer.ui.partdescriptor.trace";

	private final HashMap<String, IUndoContext> mapLabelToContext;
	
	private BaseExperiment experiment;
	
	private CTabFolder tabFolderTopLeft;
	private CTabFolder tabFolderBottomLeft;
	
	private HPCTraceView     tbtmTraceView;
	private HPCDepthView     tbtmDepthView;
	private HPCCallStackView tbtmCallStack;
	private HPCSummaryView   tbtmSummaryView;
	
	private HPCStatView tbtmStatView;
	private HPCBlameView tbtmBlameView;
	
	private SpaceTimeMiniCanvas miniCanvas;
	
	private IEclipseContext context;
	private SpaceTimeDataController stdc;
	private EPartService partService;
	private IEventBroker eventBroker;
	
	@Inject
	public TracePart() {
		mapLabelToContext = new HashMap<String, IUndoContext>(8);
	}
	
	@PostConstruct
	public void postConstruct(Composite parent, 
							  MWindow window, 
							  EPartService partService, 
							  IEventBroker eventBroker,
							  @Named(IServiceConstants.ACTIVE_PART) MPart part) {
		
		this.partService = partService;
		this.eventBroker = eventBroker;
		
		SashForm sashFormMain  = new SashForm(parent, SWT.NONE);
		SashForm sashFormLeft  = new SashForm(sashFormMain, SWT.VERTICAL);
		SashForm sashFormRight = new SashForm(sashFormMain, SWT.VERTICAL);

		context = part.getContext();
		
		// ---------------
		// main view
		// ---------------
		
		tabFolderTopLeft = new CTabFolder(sashFormLeft, SWT.BORDER);
		
		tbtmTraceView = new HPCTraceView(tabFolderTopLeft, SWT.NONE);
		createTabItem(tbtmTraceView, "Main view", tabFolderTopLeft, eventBroker);
		
		// ---------------
		// depth view
		// ---------------
		
		tabFolderBottomLeft = new CTabFolder(sashFormLeft, SWT.BORDER);
		
		tbtmDepthView = new HPCDepthView(tabFolderBottomLeft, SWT.NONE);
		createTabItem(tbtmDepthView, "Depth view", tabFolderBottomLeft, eventBroker);
		
		tbtmSummaryView = new HPCSummaryView(tabFolderBottomLeft, SWT.NONE);
		createTabItem(tbtmSummaryView, "Summary view", tabFolderBottomLeft, eventBroker);
		
		// ---------------
		// call stack
		// ---------------

		CTabFolder tabFolderRight = new CTabFolder(sashFormRight, SWT.BORDER);		

		tbtmCallStack = new HPCCallStackView(tabFolderRight, SWT.NONE);
		createTabItem(tbtmCallStack, "Call stack", tabFolderRight, eventBroker);

		tbtmStatView = new HPCStatView(tabFolderRight, SWT.NONE);
		createTabItem(tbtmStatView, "Statistics", tabFolderRight, eventBroker);
		
		tbtmBlameView = new HPCBlameView(tabFolderRight, SWT.NONE);
		createTabItem(tbtmBlameView, "GPU Idleness Blame", tabFolderRight, eventBroker);
		
		tabFolderRight.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {

				if (e.item == tbtmStatView || e.item == tbtmBlameView) {
					int numItems = tbtmStatView.getItemCount();
					if (numItems < 1) {
						activateStatisticItem();
					}
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		// ---------------
		// mini map
		// ---------------
		
		final Composite miniArea = new Composite(sashFormRight, SWT.BORDER_DASH);
		
		GridDataFactory.fillDefaults().grab(true, true).applyTo(miniArea);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(miniArea);

		Label lblMiniMap = new Label(miniArea, SWT.BORDER);
		lblMiniMap.setText("Mini map");
		lblMiniMap.setToolTipText("The view to show the portion of the execution shown by the Trace View," +
				  				  "relative to process/time dimensions");
		
		GridDataFactory.fillDefaults().indent(5, 4).align(SWT.LEFT, SWT.TOP).grab(true, false).applyTo(lblMiniMap);
		
		miniCanvas = new SpaceTimeMiniCanvas(this, miniArea);

		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(miniCanvas);
		GridDataFactory.swtDefaults().grab(true, true).align(SWT.CENTER, SWT.CENTER).hint(160, 100).applyTo(miniCanvas);
		
		// ---------------
		// sash settings
		// ---------------
		
		sashFormLeft .setWeights(new int[] {8000, 2000});		
		sashFormRight.setWeights(new int[] {8000, 2000});		
		sashFormMain .setWeights(new int[] {8000, 2000});

		// ---------------
		// finalization
		// ---------------
		
		tabFolderBottomLeft.setSelection(tbtmDepthView);
		tabFolderRight.setSelection(tbtmCallStack);
		tabFolderTopLeft.setSelection(tbtmTraceView);
		tabFolderTopLeft.setFocus();
		
		partService.addPartListener(this);
		TracePreferenceManager.INSTANCE.getPreferenceStore().addPropertyChangeListener(this);
		
		eventBroker.subscribe(BaseConstants.TOPIC_HPC_REMOVE_DATABASE, this);
	}
	
	
	private void createTabItem(AbstractBaseItem item, String title, CTabFolder parent, IEventBroker eventBroker) {
		item.setText(title);

		Composite tabArea = new Composite(parent, SWT.NONE);
		FillLayout tabLayout = new FillLayout();
		tabLayout.type = SWT.VERTICAL;
		tabArea.setLayout(tabLayout);
		
		item.createContent(this, context, eventBroker, tabArea);
		item.setControl(tabArea);
	}
	
	
	public ITraceViewAction getActions() {
		if (tbtmTraceView != null)
			return tbtmTraceView.getActions();
		
		return null;
	}
	
	
	@PreDestroy
	public void preDestroy() {
		if (partService != null)
			partService.removePartListener(this);
		
		if (eventBroker != null)
			eventBroker.unsubscribe(this);
		
		PreferenceStore pref = TracePreferenceManager.INSTANCE.getPreferenceStore();
		if (pref != null)
			pref.removePropertyChangeListener(this);
	}
	
	
	@Focus
	public void onFocus() {
		if (tabFolderTopLeft != null && !tabFolderTopLeft.isDisposed())
			tabFolderTopLeft.setFocus();
	}

	
	@Override
	public BaseExperiment getExperiment() {
		return experiment;
	}

	@Override
	public void setInput(MPart part, Object input) {
		this.experiment = (BaseExperiment) input;
		part.setLabel("Trace: " + experiment.getName());
		part.setTooltip(experiment.getDefaultDirectory().getAbsolutePath());
	}

	@Override
	public void partActivated(MPart part) {}

	@Override
	public void partBroughtToTop(MPart part) {}

	@Override
	public void partDeactivated(MPart part) {}

	@Override
	public void partHidden(MPart part) {}

	@Override
	public void partVisible(MPart part) {
		// if we already create the database and the views, we don't need to recreate again
		if (part.getObject() != this || stdc != null)
			return;

		// if we need to close the part, we shouldn't continue
		if (eventBroker == null || partService == null)
			return;
		
		if (experiment.getRootScope() == null)
			return;
		
		try {
			AbstractDBOpener dbOpener = new LocalDBOpener(context, experiment);
			stdc = dbOpener.openDBAndCreateSTDC(null);

			// TODO: make sure all the tabs other than trace view has the stdc first
			tbtmDepthView.setInput(stdc);
			tbtmCallStack.setInput(stdc);
			miniCanvas.   updateView(stdc);
			tbtmStatView .setInput(stdc);
			
			// TODO: summary view has to be set AFTER the stat view 
			//       since the stat view requires info from summary view 
			tbtmSummaryView.setInput(stdc);

			// this has to be the last tab item to be set
			// start reading the database and draw it
			tbtmTraceView.setInput(stdc);
			
			IBaseData data = stdc.getBaseData();
			List<Short> listIdTupleTypes = data.getIdTupleTypes();
			boolean hasGPU = false; 
			
			// check whether this database has gpu profile or not
			for (Short type: listIdTupleTypes) {
				if (IdTupleType.KIND_GPU_CONTEXT == type) {
					tbtmBlameView.setInput(stdc);
					hasGPU = true;
					
					break;
				}
			}
			if (!hasGPU) {
				tbtmBlameView.dispose();
				tbtmSummaryView.setAnalysisTool(IPixelAnalysis.EMPTY);
			} else {
				tbtmSummaryView.setAnalysisTool(new CpuBlameAnalysis(eventBroker));
			}
			
		} catch (Exception e) {
			Shell shell = Display.getDefault().getActiveShell();
			MessageDialog.openError(shell, "Error in opening the database", e.getClass() + ":" + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void activateStatisticItem() {
		tabFolderBottomLeft.setSelection(tbtmSummaryView);
	}

	@Override
	public IUndoContext getContext(final String label) {
		IUndoContext context = mapLabelToContext.get(label);
		if (context != null)
			return context;

		context = new BaseTraceContext(label);
		
		mapLabelToContext.put(label, context);
		
		return context;
	}
	
	
	@Override
	public IOperationHistory getOperationHistory() {
		return OperationHistoryFactory.getOperationHistory();
	}


	@Override
	public Object getInput() {
		return stdc;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		TraceEventData data = new TraceEventData(stdc, this, stdc);
		eventBroker.post(IConstants.TOPIC_COLOR_MAPPING, data);
	}

	@Override
	public void handleEvent(Event event) {
		if (event.getTopic().equals(BaseConstants.TOPIC_HPC_REMOVE_DATABASE)) {
			// mark that this part will be close soon. Do not do any tasks
			partService = null;
			eventBroker = null;
			
			// need to dispose resources
			if (stdc != null)
				stdc.dispose();
		}
	}
}