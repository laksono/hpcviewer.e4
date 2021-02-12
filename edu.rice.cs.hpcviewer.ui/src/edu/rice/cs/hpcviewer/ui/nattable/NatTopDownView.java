package edu.rice.cs.hpcviewer.ui.nattable;

import java.io.Serializable;
import java.util.Collection;
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
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
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
import org.eclipse.nebula.widgets.nattable.painter.layer.NatGridLayerPainter;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionModel;
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
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpcviewer.ui.ProfilePart;
import edu.rice.cs.hpcviewer.ui.addon.DatabaseCollection;
import edu.rice.cs.hpcviewer.ui.tabItems.AbstractBaseViewItem;

public class NatTopDownView extends AbstractBaseViewItem 
{
	private final static String TITLE_TOP_DOWN = "Top-down view";
	
	private NatTable natTable;
	private Composite container;
	private RootScope root;
	
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
	}

	@Override
	public void createContent(Composite parent) {
		
		parent.setLayout(new GridLayout(1, false));
	}

	@Override
	public void setInput(Object input) {
		root = (RootScope) input;
		
		List<BaseMetric> metrics   = ((Experiment)root.getExperiment()).getVisibleMetrics();

		BodyLayerTopDown bodyLayer = new BodyLayerTopDown(root, metrics);
		
        // build the column header layer
        IDataProvider columnHeaderDataProvider = new ColumnDataProvider(metrics);
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
		
		natTable = new NatTable(container, SWT.NO_REDRAW_RESIZE | SWT.DOUBLE_BUFFERED | SWT.BORDER, gridLayer, false);

        natTable.setLayerPainter(
                new NatGridLayerPainter(natTable, DataLayer.DEFAULT_ROW_HEIGHT));		

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
		return root;
	}
	
	

	
	static private class BodyLayerTopDown extends AbstractLayerTransform 
	{
        private final SelectionLayer selectionLayer;
        private final TreeList<Scope> treeList;
        private final TreeLayer treeLayer;

		public BodyLayerTopDown(RootScope root, List<BaseMetric> metrics) {
			
            // wrapping of the list to show into GlazedLists
            // see http://publicobject.com/glazedlists/ for further information
			BaseExperiment experiment = root.getExperiment();
			Collection<Scope> listOfScopes = experiment.getListOfScopes();
            EventList<Scope> eventList = GlazedLists.eventList(listOfScopes);

            TransformedList<Scope, Scope> rowObjectsGlazedList = GlazedLists.threadSafeList(eventList);

            // use the SortedList constructor with 'null' for the Comparator
            // because the Comparator will be set by configuration
            SortedList<Scope> sortedList = new SortedList<>(rowObjectsGlazedList, null);
            TreeList.Format<Scope> treeFormat = new TreeScopeFormat(root);
            
            // wrap the SortedList with the TreeList
            treeList = new TreeList<Scope>(sortedList, treeFormat, new ScopeExpansionModel());

            IRowDataProvider<Scope> bodyDataProvider = new ListDataProvider<Scope>(this.treeList, 
            									new TreeColumnAccessor(metrics));
            DataLayer bodyDataLayer = new DataLayer(bodyDataProvider);

            // layer for event handling of GlazedLists and PropertyChanges
            GlazedListsEventLayer<Scope> glazedListsEventLayer = new GlazedListsEventLayer<>(bodyDataLayer, this.treeList);

            GlazedListTreeData<Scope> treeData = new GlazedListTreeData<>(this.treeList);
            ITreeRowModel<Scope> treeRowModel = new GlazedListTreeRowModel<>(treeData);

            selectionLayer = new SelectionLayer( glazedListsEventLayer );
            selectionLayer.setSelectionModel(new RowSelectionModel<Scope>( getSelectionLayer(), bodyDataProvider,  new IRowIdAccessor<Scope>() {

				@Override
				public Serializable getRowId(Scope rowObject) {
					return rowObject.getCCTIndex();
				}
			}));
            
            treeLayer = new TreeLayer(this.selectionLayer, treeRowModel);
            ViewportLayer viewportLayer = new ViewportLayer(this.treeLayer);

            setUnderlyingLayer(viewportLayer);
		}

        public SelectionLayer getSelectionLayer() {
            return this.selectionLayer;
        }
	}	
}
