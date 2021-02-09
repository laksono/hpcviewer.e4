package edu.rice.cs.hpcviewer.ui.internal;

import java.io.IOException;
import java.util.List;
import java.util.Stack;

import javax.annotation.PreDestroy;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.services.EMenuService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import edu.rice.cs.hpc.data.experiment.BaseExperiment;
import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.IMetricManager;
import edu.rice.cs.hpc.data.experiment.metric.MetricValue;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpcbase.ViewerDataEvent;
import edu.rice.cs.hpcsetting.fonts.FontManager;
import edu.rice.cs.hpcsetting.preferences.PreferenceConstants;
import edu.rice.cs.hpcviewer.ui.ProfilePart;
import edu.rice.cs.hpcviewer.ui.actions.ExportTable;
import edu.rice.cs.hpcviewer.ui.actions.HotCallPath;
import edu.rice.cs.hpcviewer.ui.actions.MetricColumnHideShowAction;
import edu.rice.cs.hpcviewer.ui.actions.ZoomAction;
import edu.rice.cs.hpcviewer.ui.addon.DatabaseCollection;
import edu.rice.cs.hpcviewer.ui.base.IViewBuilder;
import edu.rice.cs.hpcviewer.ui.actions.UserDerivedMetric;
import edu.rice.cs.hpcviewer.ui.resources.IconManager;


/****
 * 
 * Base class to manage the content of the view part
 * 
 * A part has to call {@link IViewBuilder.createContent} to create the content of the view
 * (like toolbar and table tree).
 * 
 * For further customization, the caller (or part) has to subclass this class and implement
 * {@link beginToolbar} and {@link endToolbar}
 */
public abstract class AbstractViewBuilder implements IViewBuilder, ISelectionChangedListener
{
	static protected enum ViewerType {
		/** the viewer is independent to others. No need to update the status from others. */
		INDIVIDUAL,  
		/** the viewer depends the actions of others. Required status updates from others.*/
		COLLECTIVE   
	};
	
	final private int TREE_COLUMN_WIDTH  = 350;

	final private int ACTION_ZOOM_IN      = 0;
	final private int ACTION_ZOOM_OUT     = 1;
	final private int ACTION_HOTPATH      = 3;
	final private int ACTION_ADD_METRIC   = 4;
	final private int ACTION_EXPORT_DATA  = 5;
	final private int ACTION_COLUMN_HIDE  = 6; 
	final private int ACTION_FONT_BIGGER  = 8;
	final private int ACTION_FONT_SMALLER = 9;

	final private ActionType []actionTypes = {
			new ActionType(IconManager.Image_ZoomIn ,      "Zoom-in the selected node"),
			new ActionType(IconManager.Image_ZoomOut,      "Zoom-out from the current scope"),
			ActionType.ActionTypeNull,
			new ActionType(IconManager.Image_FlameIcon,    "Expand the hot path below the selected node"),
			new ActionType(IconManager.Image_FnMetric,     "Add a new derived metric"),
			new ActionType(IconManager.Image_SaveCSV,      "Export displayed data into a CSV format file"),
			new ActionType(IconManager.Image_CheckColumns, "Hide/show columns"),
			ActionType.ActionTypeNull,
			new ActionType(IconManager.Image_FontBigger,   "Increase font size"),
			new ActionType(IconManager.Image_FontSmaller,  "Decrease font size")
	};
	
	final private EPartService  partService;
	final private IEventBroker  eventBroker;
	final private DatabaseCollection database;
	private EMenuService menuService;
	
	private ScopeTreeViewer treeViewer = null;
	
	private ToolItem     toolItem[];
	private LabelMessage lblMessage;
	
	private Listener mouseDownListener = null;
	private StyledScopeLabelProvider labelProvider;
	
	private ZoomAction  zoomAction    = null;
	private HotCallPath hotPathAction = null;
	private ExportTable   exportCSV     = null;
	
	private MetricColumnHideShowAction metricAction = null;
	private UserDerivedMetric derivedMetricAction   = null;
	
	protected Stack<Object> stackActions; 
	
	/***
	 * Constructor for abstract content viewer
	 * <p>Initialize the class to get the app service like EPartService and EModelService
	 * since we don't have access to this injected variables</p>
	 * 
	 * @param partService
	 * @param eventBroker
	 * @param database
	 * @param partFactory
	 */
	public AbstractViewBuilder(
			EPartService  partService, 
			IEventBroker  eventBroker,
			DatabaseCollection database,
			ProfilePart   profilePart) {
		
		this.partService  = partService;
		this.eventBroker  = eventBroker;
		this.database     = database;
		stackActions = new Stack<Object>();
	}
	
	@Override
	public void createContent(ProfilePart profilePart, Composite parent, EMenuService menuService) {
		
		this.menuService = menuService;
				
		parent.setLayout(new GridLayout(1, false));
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		GridData gd_composite = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_composite.widthHint = 506;
		composite.setLayoutData(gd_composite);
				
		CoolBar coolBar = new CoolBar(composite, SWT.FLAT);

		ToolBar toolBar = new ToolBar(coolBar, SWT.FLAT | SWT.RIGHT);

		// -------------------------------------------
		// add the beginning of toolbar
		// -------------------------------------------
		beginToolbar(coolBar, toolBar);
		
		// -------------------------------------------
		// default tool bar
		// -------------------------------------------
		toolItem = new ToolItem[actionTypes.length];
		
		for(int i=0; i<actionTypes.length; i++) {
			toolItem[i] = createToolItem(toolBar, actionTypes[i].imageFileName, actionTypes[i].tooltip);
		}
		
		// -------------------------------------------
		// add the end of toolbar
		// -------------------------------------------
		endToolbar(coolBar, toolBar);

		createCoolItem(coolBar, toolBar);
		
		Point p = coolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		p.x += 20;
		
		coolBar.setSize(p);
		
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(false, false).applyTo(coolBar);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(coolBar);

		// -------------------------------------------
		// message label
		// -------------------------------------------

		lblMessage = new LabelMessage(composite, SWT.NONE);

		
		// -------------------------------------------
		// table creation
		// -------------------------------------------
		
		treeViewer = new ScopeTreeViewer(parent, SWT.NONE);
		Tree tree = treeViewer.getTree();
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        tree.setHeaderVisible(true);
        tree.setLinesVisible(true);

		treeViewer.setContentProvider( getContentProvider(treeViewer));
		
		mouseDownListener = new ScopeMouseListener(treeViewer, profilePart);
		treeViewer.getTree().addListener(SWT.MouseDown, mouseDownListener);
		treeViewer.addSelectionChangedListener(this);

		// initialize tool item handler at the end
		// because we need access to tree viewer :-( 
		setToolItemHandlers();

		final ExportTable export = new ExportTable(treeViewer, lblMessage);
		final Action a = new Action("Copy") {
			@Override
			public void run() {
				StringBuffer sb = export.getSelectedRows();
				
				Clipboard clipboard = new Clipboard(treeViewer.getTree().getDisplay());
			    TextTransfer [] textTransfer = {TextTransfer.getInstance()};
			    clipboard.setContents(new Object [] {sb.toString()}, textTransfer);
			    clipboard.dispose();
			}
        };
        
        final MenuManager mgr = new MenuManager();
        mgr.setRemoveAllWhenShown(true);

        mgr.addMenuListener(manager -> {
                IStructuredSelection selection = treeViewer.getStructuredSelection();
                if (!selection.isEmpty()) {
                        a.setText("Copy to clipboard");
                        mgr.add(a);
                }
        });
        treeViewer.getControl().setMenu(mgr.createContextMenu(treeViewer.getControl()));
	}

	
	@PreDestroy
	public void dispose() {
		treeViewer.removeSelectionChangedListener(this);
		((ScopeMouseListener) mouseDownListener).dispose();
		
	}
	
	@Override
	public void setData(RootScope root) {
		
		removeColumns();
		
		createScopeColumn(getViewer());
		
		Experiment experiment = (Experiment) root.getExperiment();
		
		// add metric columns only if the metric is not empty
		boolean bSorted = true;
		List<BaseMetric> metrics = experiment.getVisibleMetrics();
		
		for(BaseMetric metric : metrics) {
			if (root.getMetricValue(metric) == MetricValue.NONE)
				continue;
			
			treeViewer.addTreeColumn(metric, bSorted);
			
			// only the first visible column is sorted
			bSorted = false;
		}
		Scope rootTable = root.createRoot();
		
		// TOOO: populate the table: this can take really long time !
		treeViewer.setInput(rootTable);

		// synchronize hide/show columns with other views that already visible
		// since this view is just created, we need to ensure the columns hide/show
		// are the same.
		
		ViewerDataEvent dataEvent = database.getColumnStatus(experiment);
		
		if (dataEvent != null && dataEvent.data != null) {
			boolean []status = (boolean[]) dataEvent.data;
			treeViewer.setColumnsStatus(getMetricManager(), status);
		}
		
		treeViewer.initSelection(0);

		// enable/disable action buttons
		// this has to be in the last statement
		treeViewer.getTree().getDisplay().asyncExec(()-> {
			updateStatus();
		});
	}
	

	
	@Override
    public void selectionChanged(SelectionChangedEvent event) {
		updateStatus();
	}
	
	@Override
	public ScopeTreeViewer getTreeViewer() {
		return treeViewer;
	}
	
    
    /***
     * generic method to create column scope tree
     * Called by children to have uniform way to create a scope tree.
     * 
     * @param treeViewer
     * @return
     */
    protected TreeViewerColumn createScopeColumn(ScopeTreeViewer treeViewer) {

        //----------------- create the column tree
        final TreeViewerColumn colTree = new TreeViewerColumn(treeViewer,SWT.LEFT, 0);
        colTree.getColumn().setText("Scope");
        colTree.getColumn().setWidth(TREE_COLUMN_WIDTH);
        
        if (labelProvider == null) {
        	labelProvider = new StyledScopeLabelProvider(treeViewer);
        }
        colTree.setLabelProvider( labelProvider ); 

        // set sorting facility for this column
        // it is not common for users to sort based on scope, but it may happen :-(
        
        ScopeSelectionAdapter selectionAdapter = new ScopeSelectionAdapter(treeViewer, colTree);
		colTree.getColumn().addSelectionListener(selectionAdapter);
		
		return colTree;
    }

	
    /****
     * Dispose all the columns (it doesn't really remove it)
     * @return
     */
	private int removeColumns() {
		TreeColumn columns[] = treeViewer.getTree().getColumns();
		for (TreeColumn col : columns) {
			col.dispose();
		}
		return columns.length;
	}
	
	
	/**
	 * finalize tool item and add it to the current toolbar
	 * 
	 * @param toolbar parent toolbar
	 * @param toolbarStyle style of toolitem ex: {@link SWT.PUSH}
	 * @param name the name of the image file
	 * @param tooltip the tooltip text
	 * 
	 * @return {@code ToolItem} created tool item
	 */
	protected ToolItem createToolItem(ToolBar toolbar, int toolbarStyle, String name, String tooltip) {
		
		IconManager iconManager = IconManager.getInstance();
		ToolItem toolitem = new ToolItem(toolbar, toolbarStyle);
		
		toolitem.setImage(iconManager.getImage(name));
		toolitem.setToolTipText(tooltip);
		toolitem.setEnabled(false);
		
		return toolitem;
	}
	
	/**
	 * finalize tool item and add it to the current toolbar with {@link SWT.PUSH} as the style
	 * 
	 * @param toolbar parent toolbar
	 * @param name the name of the image file
	 * @param tooltip the tooltip text
	 * @return {@code ToolItem}
	 */
	protected ToolItem createToolItem(ToolBar toolbar, String name, String tooltip) {
		if (name == null) {
			return new ToolItem(toolbar, SWT.SEPARATOR);
		}
		return createToolItem(toolbar, SWT.PUSH, name, tooltip);
	}
	
    /***
     * create cool item based on given toolbar
     * 
     * @param coolBar parent cool bar
     * @param toolBar toolbar to be added in the cool item
     */
	protected void createCoolItem(CoolBar coolBar, ToolBar toolBar) {

		CoolItem coolItem = new CoolItem(coolBar, SWT.NONE);
		coolItem.setControl(toolBar);
		Point size = toolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		size.x += 20;
		coolItem.setSize(size);
    	
    }
	
	/**
	 * Enable/disable tool items.
	 * Subclasses must call this method once an operation is executed. 
	 */
	protected void updateStatus() {

		BaseExperiment exp = treeViewer.getExperiment();
		if (exp == null) {
			// disable everything
			for(ToolItem ti : toolItem) {
				ti.setEnabled(false);
			}
			return;
		}
		toolItem[ACTION_FONT_BIGGER] .setEnabled(true);
		toolItem[ACTION_FONT_SMALLER].setEnabled(true);
		toolItem[ACTION_ADD_METRIC]  .setEnabled(true);
		toolItem[ACTION_EXPORT_DATA] .setEnabled(true);
		
		IMetricManager mgr = (IMetricManager) exp;
		toolItem[ACTION_COLUMN_HIDE].setEnabled(mgr.getMetricCount() > 0);
		
		// --------------------------------------------------------------------------
		// tool items that depend once the selected node item
		// --------------------------------------------------------------------------
		
		IStructuredSelection selection = treeViewer.getStructuredSelection();
		
		// notify subclasses to update the status
		selectionChanged(selection);
		
		if (selection != null) {
			Object item = selection.getFirstElement();
			if (item instanceof Scope && ((Scope)item).hasChildren()) {
				
				Scope node = (Scope) item;
				boolean enabled = zoomAction.canZoomIn((Scope) node);
				toolItem[ACTION_ZOOM_IN].setEnabled(enabled);
				toolItem[ACTION_HOTPATH].setEnabled(true);
			} else {
				toolItem[ACTION_ZOOM_IN].setEnabled(false);
				toolItem[ACTION_HOTPATH].setEnabled(false);
			}
			
		} else {
			// no selection: disable some
			toolItem[ACTION_ZOOM_IN].setEnabled(false);
			toolItem[ACTION_HOTPATH].setEnabled(false);
		}
		if (zoomAction == null)
			return;
		
		boolean canZoomOut = zoomAction.canZoomOut() && 
							(!stackActions.isEmpty() && stackActions.peek()==zoomAction);
		
		toolItem[ACTION_ZOOM_OUT].setEnabled(canZoomOut);
    }
	
	/***
	 * Retrieve the viewer
	 * @return
	 */
	protected ScopeTreeViewer getViewer() {
		return treeViewer;
	}
	

    
	/**
	 * show the hot path below the selected node in the tree
	 */
	protected void showHotCallPath() {
		if (hotPathAction == null) {
			hotPathAction = new HotCallPath(getViewer(), lblMessage);
		}
		hotPathAction.showHotCallPath();
	}

    

	private void setToolItemHandlers() {
		toolItem[ACTION_HOTPATH].addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				showHotCallPath();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		zoomAction = new ZoomAction(getViewer());

		toolItem[ACTION_ZOOM_IN].addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {

				ScopeTreeViewer viewer = getViewer();
				ISelection selection   = viewer.getSelection();
				
				if (selection == null)
					return;
				
				IStructuredSelection structSelect = (IStructuredSelection) selection;
				Object data = structSelect.getFirstElement();
				if (data != null && data instanceof Scope) {
					zoomAction.zoomIn((Scope) data);
					stackActions.push(zoomAction);
					
					updateStatus();
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		toolItem[ACTION_ZOOM_OUT].addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {

				zoomAction.zoomOut();
				Object obj = stackActions.pop();
				
				if (obj != zoomAction) {
					System.err.println("Invalid undoed action: " + obj);
				}
				
				updateStatus();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		toolItem[ACTION_COLUMN_HIDE].addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (metricAction == null) {
					boolean affectOthers = getViewerType() == ViewerType.COLLECTIVE;
					metricAction = new MetricColumnHideShowAction(eventBroker, getMetricManager(), affectOthers);
				}
				
				metricAction.showColumnsProperties(treeViewer, database);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		toolItem[ACTION_ADD_METRIC].addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (derivedMetricAction == null) {
					derivedMetricAction = new UserDerivedMetric(getViewer().getRootScope(), getMetricManager(), eventBroker);
				}
				derivedMetricAction.addNewMeric();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
			
		});
		
		toolItem[ACTION_EXPORT_DATA].addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (exportCSV == null) {
					exportCSV = new ExportTable(treeViewer, lblMessage);
				}
				exportCSV.export();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		toolItem[ACTION_FONT_BIGGER].addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				changeFontHeight(PreferenceConstants.ID_FONT_GENERIC, 1);
				changeFontHeight(PreferenceConstants.ID_FONT_METRIC,  1);
				changeFontHeight(PreferenceConstants.ID_FONT_TEXT,    1);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		toolItem[ACTION_FONT_SMALLER].addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				changeFontHeight(PreferenceConstants.ID_FONT_GENERIC, -1);
				changeFontHeight(PreferenceConstants.ID_FONT_METRIC,  -1);
				changeFontHeight(PreferenceConstants.ID_FONT_TEXT,    -1);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
	}
	
	
	/****
	 * Change the height of the font for a given font id from the {@link PreferenceConstants}
	 * @param id the font id from {@link PreferenceConstants}
	 * @param deltaHeight the number of increase/decrease
	 */
	private void changeFontHeight(String id, int deltaHeight) {
		FontData []oldfd = FontManager.getFontDataPreference(id);
		FontData []newFd = FontDescriptor.copy(oldfd);
		int height = newFd[0].getHeight();
		int heightNew = height+deltaHeight;
		newFd[0].setHeight(heightNew);
		try {
			FontManager.setFontPreference(id, newFd);
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		treeViewer.computeIdealCellBound();		
	}
	
	
	/***
	 * retrieve the current part service.
	 * @return
	 */
	protected EPartService getPartService() {
		return partService;
	}
    
	protected DatabaseCollection getDatabaseCollection() {
		return database;
	}
	
	protected EMenuService getMenuService() {
		return menuService;
	}
	
    /////////////////////////////////////////////////////////
    ///
    ///  Abstract methods
    ///
    /////////////////////////////////////////////////////////
    
    protected abstract void beginToolbar(CoolBar coolbar, ToolBar toolbar);
    protected abstract void endToolbar  (CoolBar coolbar, ToolBar toolbar);
    protected abstract void selectionChanged(IStructuredSelection selection);
    
    protected abstract AbstractContentProvider getContentProvider(ScopeTreeViewer treeViewer);
    protected abstract IMetricManager 		   getMetricManager();
    protected abstract ViewerType              getViewerType();
    
    /////////////////////////////////////////////////////////
    ///
    ///  classes
    ///
    /////////////////////////////////////////////////////////
    
    static protected class ActionType
    {
    	public static final ActionType ActionTypeNull = new ActionType(null, null);
    	public String   imageFileName;
    	public String   tooltip;
    	
    	ActionType(String imageFileName, String tooltip) {
    		this.imageFileName = imageFileName;
    		this.tooltip = tooltip;
    	}
    }
}
