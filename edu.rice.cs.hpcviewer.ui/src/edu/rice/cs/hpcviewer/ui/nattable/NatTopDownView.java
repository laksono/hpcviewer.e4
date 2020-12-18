package edu.rice.cs.hpcviewer.ui.nattable;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.services.EMenuService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.DefaultNatTableStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.tree.GlazedListTreeData;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.tree.GlazedListTreeRowModel;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultColumnHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultRowHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.layer.AbstractLayerTransform;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.IUniqueIndexLayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.tree.ITreeRowModel;
import org.eclipse.nebula.widgets.nattable.tree.TreeLayer;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.TreeList;
import edu.rice.cs.hpc.data.experiment.BaseExperiment;
import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.MetricValue;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScopeType;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.data.util.ScopeComparator;
import edu.rice.cs.hpcviewer.ui.ProfilePart;
import edu.rice.cs.hpcviewer.ui.addon.DatabaseCollection;
import edu.rice.cs.hpcviewer.ui.tabItems.AbstractBaseViewItem;

public class NatTopDownView extends AbstractBaseViewItem 
{
	private final static String TITLE_TOP_DOWN = "Top-down view";
	
	private EPartService  partService;	
	private IEventBroker  eventBroker;
	private EMenuService  menuService;
	private DatabaseCollection database;
	private ProfilePart   profilePart;
	
	private NatTable natTable;
	private Composite container;

	
	public NatTopDownView(CTabFolder parent, int style) {
		super(parent, style);

		this.container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(this.container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(this.container);
		
		setText(TITLE_TOP_DOWN);
		setToolTipText("A view to display the calling context tree (CCT) of the profile data");
	}

	@Override
	public void setService(EPartService partService, IEventBroker broker, DatabaseCollection database,
			ProfilePart profilePart, EMenuService menuService) {
		
		this.partService = partService;
		this.eventBroker = broker;
		this.database    = database;
		this.profilePart = profilePart;
		this.menuService = menuService;
	}

	@Override
	public void createContent(Composite parent) {
		
		parent.setLayout(new GridLayout(1, false));
	}

	@Override
	public void setInput(Object input) {
		RootScope root = (RootScope) input;
		BodyLayerTopDown bodyLayer = new BodyLayerTopDown(root);
		
        // build the column header layer
        IDataProvider columnHeaderDataProvider = new ColumnDataProvider((Experiment) root.getExperiment());
        DataLayer columnDataHeaderLayer = new DefaultColumnHeaderDataLayer(columnHeaderDataProvider);
        ILayer columnHeaderLayer     = new ColumnHeaderLayer(columnDataHeaderLayer, bodyLayer, bodyLayer.getSelectionLayer());
		
        // build the row header layer
        IDataProvider rowHeaderDataProvider = new TreeRowHeaderDataProvider((Experiment) root.getExperiment());
        DataLayer rowDataHeaderLayer = new DefaultRowHeaderDataLayer(rowHeaderDataProvider);
        ILayer rowHeaderLayer = new RowHeaderLayer(rowDataHeaderLayer, bodyLayer, bodyLayer.getSelectionLayer());

        // build the corner layer
        IDataProvider cornerDataProvider = new DefaultCornerDataProvider(columnHeaderDataProvider, rowHeaderDataProvider);
        DataLayer cornerDataLayer = new DataLayer(cornerDataProvider);
        ILayer cornerLayer = new CornerLayer(cornerDataLayer, rowHeaderLayer, columnHeaderLayer);

		GridLayer gridLayer = new GridLayer(bodyLayer, columnHeaderLayer, rowHeaderLayer, cornerLayer);
		
		natTable = new NatTable(container, gridLayer, false);
		

        // create a new ConfigRegistry which will be needed for GlazedLists
        // handling
        ConfigRegistry configRegistry = new ConfigRegistry();

        // as the autoconfiguration of the NatTable is turned off, we have to
        // add the DefaultNatTableStyleConfiguration and the ConfigRegistry
        // manually
        natTable.setConfigRegistry(configRegistry);
        natTable.addConfiguration(new DefaultNatTableStyleConfiguration());

        natTable.configure();

        GridDataFactory.fillDefaults().grab(true, true).applyTo(natTable);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(natTable);
        
        setControl(container);
	}

	@Override
	public Object getInput() {
		return null;
	}
	
	
	static private class ScopeExpansionModel implements TreeList.ExpansionModel<Scope>
	{

		@Override
		public boolean isExpanded(Scope element, List<Scope> path) {
			if (element instanceof RootScope)
				return true;
			
			return false;
		}

		@Override
		public void setExpanded(Scope element, List<Scope> path, boolean expanded) {}
	}
	
	static private class BodyLayerTopDown extends AbstractLayerTransform 
	{
        private final SelectionLayer selectionLayer;
        private final IDataProvider dataProvider;
        private final TreeList<Scope> treeList;
        private final TreeLayer treeLayer;

		public BodyLayerTopDown(RootScope root) {
			
            // wrapping of the list to show into GlazedLists
            // see http://publicobject.com/glazedlists/ for further information
			BaseExperiment experiment = root.getExperiment();
			Collection<Scope> listOfScopes = experiment.getListOfScopes();
            EventList<Scope> eventList = GlazedLists.eventList(listOfScopes);

            TransformedList<Scope, Scope> rowObjectsGlazedList = GlazedLists.threadSafeList(eventList);

            // use the SortedList constructor with 'null' for the Comparator
            // because the Comparator will be set by configuration
            SortedList<Scope> sortedList = new SortedList<>(rowObjectsGlazedList, null);
            TreeList.Format<Scope> treeFormat = new TreeScopeFormat();
            
            // wrap the SortedList with the TreeList
            treeList = new TreeList<Scope>(sortedList, treeFormat, new ScopeExpansionModel());

			dataProvider = new TreeDataProvider(root);
            selectionLayer = new SelectionLayer( new DataLayer(dataProvider) );

            GlazedListTreeData<Scope> treeData = new GlazedListTreeData<>(this.treeList);
            ITreeRowModel<Scope> treeRowModel = new GlazedListTreeRowModel<>(treeData);
            treeLayer = new TreeLayer(this.selectionLayer, treeRowModel);
            
            ViewportLayer viewportLayer = new ViewportLayer(this.treeLayer);

            setUnderlyingLayer(viewportLayer);
		}

        public IUniqueIndexLayer getTreeLayer() {
			return treeLayer;
		}

		public SelectionLayer getSelectionLayer() {
            return this.selectionLayer;
        }

        public IDataProvider getBodyDataProvider() {
            return this.dataProvider;
        }

        public TreeList<Scope> getTreeList() {
            return this.treeList;
        }
	}
	
	
	static private class TreeScopeFormat implements TreeList.Format<Scope>
	{
		private ScopeComparator comparator;

		public void setMetric(BaseMetric metric) {
			comparator.setMetric(metric);
		}
		
		@Override
		public void getPath(List<Scope> path, Scope element) {
			if (element == null)
				return;

			path.add(element);
			
			Scope current = element;
			while(current != null && current.getParentScope() != null) {
				Scope parent = current.getParentScope();
				if (parent instanceof RootScope) {
					if (((RootScope)parent).getType() == RootScopeType.Invisible) {
						current = parent;
						continue;
					}
				}
				path.add(parent);
				current = parent;
			}
            Collections.reverse(path);
		}

		@Override
		public boolean allowsChildren(Scope element) {
			return element.hasChildren();
		}

		@Override
		public Comparator<? super Scope> getComparator(int depth) {
			if (comparator == null) {
				comparator = new ScopeComparator();
			}
			return null;
		}
		
	}
	
	static private class TreeDataProvider implements IDataProvider
	{
		private final RootScope root;
		private final List<BaseMetric> metrics;
		
		public TreeDataProvider(RootScope root) {
			this.root = root;
			metrics = ((Experiment)root.getExperiment()).getVisibleMetrics();
		}
		
		@Override
		public Object getDataValue(int columnIndex, int rowIndex) {
			Experiment experiment = (Experiment) root.getExperiment();
			Scope scope = experiment.getListOfScopes().get(rowIndex);
			if (columnIndex == 0)
				return scope;
			if (scope == null)
				return null;
			BaseMetric metric = metrics.get(columnIndex-1);
			MetricValue mv = scope.getMetricValue(metric);
			return mv;
		}

		@Override
		public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
			if (columnIndex == 0)
				return;
			Experiment experiment = (Experiment) root.getExperiment();
			Scope scope = experiment.getListOfScopes().get(rowIndex);
			if (scope == null)
				return;
			
			BaseMetric metric = metrics.get(columnIndex-1);
			scope.setMetricValue(metric.getIndex(), (MetricValue)newValue);
		}

		@Override
		public int getColumnCount() {
			return 1+metrics.size();
		}

		@Override
		public int getRowCount() {
			Experiment experiment = (Experiment) root.getExperiment();
			return experiment.getListOfScopes().size();
		}
		
	}
	
	/******
	 * 
	 * Data provider for table columns, which consists of:
	 * 0: tree column
	 * 1-N: metric columns, where N is number of metrics.
	 *
	 */
	static private class ColumnDataProvider implements IDataProvider
	{
		private final List<BaseMetric> metrics;
		
		public ColumnDataProvider(Experiment experiment) {
			metrics = experiment.getVisibleMetrics();
		}

		@Override
		public Object getDataValue(int columnIndex, int rowIndex) {

			if (columnIndex < 0 && columnIndex > getColumnCount())
				return null;
			
			if (columnIndex == 0)
				return "Scope";

			if (metrics == null)
				return null;

			return metrics.get(columnIndex-1);
		}

		@Override
		public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
			if (columnIndex>metrics.size()) {
				metrics.add((BaseMetric) newValue);
			}
		}

		@Override
		public int getColumnCount() {
			return metrics.size()+1;
		}

		@Override
		public int getRowCount() {
			return 1;
		}
	}
	
	static private class TreeRowHeaderDataProvider implements IDataProvider
	{
		private final Experiment experiment;
		
		public TreeRowHeaderDataProvider(Experiment experiment) {
			this.experiment = experiment;
		}
		@Override
		public Object getDataValue(int columnIndex, int rowIndex) {
			return Integer.valueOf(rowIndex+1);
		}

		@Override
		public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
		}

		@Override
		public int getColumnCount() {
			return 1;
		}

		@Override
		public int getRowCount() {
			return experiment.getListOfScopes().size();
		}
	}
}
