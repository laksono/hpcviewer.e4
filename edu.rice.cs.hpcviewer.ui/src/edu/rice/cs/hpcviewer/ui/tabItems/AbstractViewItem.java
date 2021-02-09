package edu.rice.cs.hpcviewer.ui.tabItems;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.services.EMenuService;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import edu.rice.cs.hpc.data.experiment.BaseExperiment;
import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.IMetricManager;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScopeType;
import edu.rice.cs.hpc.filter.service.FilterStateProvider;
import edu.rice.cs.hpcbase.BaseConstants;
import edu.rice.cs.hpcbase.ViewerDataEvent;
import edu.rice.cs.hpcviewer.ui.ProfilePart;
import edu.rice.cs.hpcviewer.ui.addon.DatabaseCollection;
import edu.rice.cs.hpcviewer.ui.base.IViewBuilder;
import edu.rice.cs.hpcviewer.ui.internal.ScopeTreeViewer;


/*******************************************************************************************
 * 
 * Abstract class to manage basic tab item such as:
 * <ul>
 * <li>Standard events</li>
 * <li>Table viewer</li>
 * <li>Standard actions</li>
 * <li>and so on</li>
 * </ul>
 *
 *******************************************************************************************/
public abstract class AbstractViewItem extends AbstractBaseViewItem implements EventHandler
{

	protected EPartService  partService;
	protected EModelService modelService;
	protected MApplication  app;
	protected IEventBroker  eventBroker;
	protected EMenuService  menuService;
	
	protected DatabaseCollection databaseAddOn;
	protected ProfilePart   profilePart;

	private IViewBuilder contentViewer;
	
	/** Each view needs to store the experiment database.
	 * In case it needs to populate the table, we know which database 
	 * to be loaded. */
	private BaseExperiment  experiment;
	
	/** This variable is a flag whether a table is already populated or not.
	 * If the root is null, it isn't populated
	 */
	private RootScope       root;

	public AbstractViewItem(CTabFolder parent, int style) {
		super(parent, style);
	}

	
	@Override
	public void setService(EPartService partService, 
			IEventBroker broker,
			DatabaseCollection database,
			ProfilePart   profilePart,
			EMenuService  menuService) {
		
		this.partService = partService;
		this.eventBroker = broker;
		this.databaseAddOn = database;
		this.profilePart = profilePart;
		this.menuService = menuService;
	}
	
	
	@Override
	public void createContent(Composite parent) {
		contentViewer = setContentViewer(parent, menuService);
    	contentViewer.createContent(profilePart, parent, menuService);

		// subscribe to user action events
		eventBroker.subscribe(BaseConstants.TOPIC_HPC_REMOVE_DATABASE, this);
		eventBroker.subscribe(ViewerDataEvent.TOPIC_HIDE_SHOW_COLUMN,    this);
		eventBroker.subscribe(ViewerDataEvent.TOPIC_HPC_ADD_NEW_METRIC,  this);
		eventBroker.subscribe(ViewerDataEvent.TOPIC_HPC_METRIC_UPDATE,   this);
		
		// subscribe to filter events
		eventBroker.subscribe(FilterStateProvider.FILTER_REFRESH_PROVIDER, this);
	}
	
	@Override
	public void setInput(Object input) {
		
		if (!(input instanceof BaseExperiment))
			return;
					
		// important: needs to store the experiment database for further usage
		// when the view is becoming visible
		this.experiment = (BaseExperiment) input;
				
		// TODO: this process takes time
		root = createRoot(experiment);
		contentViewer.setData(root);
	}

	
	/****
	 * Retrieve the current input of this view
	 * @return
	 */
	@Override
	public Object getInput() {
		return experiment;
	}
	
	
	@Override
	public void handleEvent(Event event) {
		ScopeTreeViewer treeViewer = contentViewer.getTreeViewer();
		if (treeViewer.getTree().isDisposed())
			return;

		Object obj = event.getProperty(IEventBroker.DATA);
		if (obj == null || experiment == null || root == null)
			return;
		
		if (!(obj instanceof ViewerDataEvent)) {

			if (event.getTopic().equals(FilterStateProvider.FILTER_REFRESH_PROVIDER)) {
				FilterStateProvider.filterExperiment((Experiment) experiment);
				
				// TODO: this process takes time
				root = createRoot(experiment);
				contentViewer.setData(root);
			}
			return;
		}
		
		ViewerDataEvent eventInfo = (ViewerDataEvent) obj;
		if (experiment != eventInfo.experiment) 
			return;
		
		String topic = event.getTopic();
		if (topic.equals(ViewerDataEvent.TOPIC_HIDE_SHOW_COLUMN)) {
			IMetricManager mgr = eventInfo.experiment;
			treeViewer.setColumnsStatus(mgr, (boolean[]) eventInfo.data);
			
		} else if (topic.equals(ViewerDataEvent.TOPIC_HPC_ADD_NEW_METRIC)) {
			treeViewer.addUserMetricColumn((BaseMetric) eventInfo.data);

		} else if (topic.equals(BaseConstants.TOPIC_HPC_REMOVE_DATABASE)) {
			// mark that this part will be destroyed
			experiment.dispose();
			experiment = null;

		} else if (topic.equals(ViewerDataEvent.TOPIC_HPC_METRIC_UPDATE)) {
			treeViewer.refreshColumnTitle();
		}
	}
	
	public boolean focus () {
		ScopeTreeViewer viewer = contentViewer.getTreeViewer();
		return viewer.getTree().forceFocus();
	}

	protected abstract RootScope 	  createRoot(BaseExperiment experiment);
	protected abstract IViewBuilder   setContentViewer(Composite parent, EMenuService menuService);
	protected abstract RootScopeType  getRootType();

}
