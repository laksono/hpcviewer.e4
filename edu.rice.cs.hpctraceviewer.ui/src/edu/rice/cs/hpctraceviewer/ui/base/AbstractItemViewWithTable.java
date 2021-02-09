package edu.rice.cs.hpctraceviewer.ui.base;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Drawable;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import edu.rice.cs.hpc.data.util.OSValidator;
import edu.rice.cs.hpc.data.util.string.StringUtil;
import edu.rice.cs.hpcsetting.fonts.FontManager;
import edu.rice.cs.hpcsetting.preferences.PreferenceConstants;
import edu.rice.cs.hpcsetting.preferences.ViewerPreferenceManager;
import edu.rice.cs.hpctraceviewer.data.ColorTable;
import edu.rice.cs.hpctraceviewer.ui.internal.TraceEventData;
import edu.rice.cs.hpctraceviewer.ui.preferences.TracePreferenceManager;
import edu.rice.cs.hpctraceviewer.ui.util.IConstants;



public abstract class AbstractItemViewWithTable extends AbstractBaseItem
		implements EventHandler, Listener, IPropertyChangeListener, DisposeListener 
{
	private static final int   COLUMN_PROC_WEIGHT    = 800;
	private static final int   COLUMN_COLOR_WIDTH    = 4;
	private static final int   COLUMN_PROC_MIN_WIDTH = 60;
	private static final String TEXT_PERCENT_PATTERN = "888x88x%";
	
	private TableViewer tableViewer;
	private ColumnProcedureLabelProvider lblProcProvider;
	private ColumnColorLabelProvider     lblColorProvider;
	private TableStatComparator comparator;
	
	private Object input;
	
	private IEventBroker broker;
	
	public AbstractItemViewWithTable(CTabFolder parent, int style) {
		super(parent, style);
	}


	@Override
	public void createContent(ITracePart parentPart, IEclipseContext context, IEventBroker broker,
			Composite parent) {
		
		final Composite tableComposite = new Composite(parent, SWT.NONE);
		TableColumnLayout layout = new TableColumnLayout();
		tableComposite.setLayout(layout);
		
		tableViewer = new AbstractBaseTableViewer(tableComposite, SWT.BORDER   | SWT.VIRTUAL   | 
													  SWT.RESIZE   | SWT.READ_ONLY |
													  SWT.H_SCROLL | SWT.V_SCROLL) {

			@Override
			protected Point computeCellBounds(GC gc, Point extent) {
				return extent;
			}
		};
		
		final Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		// column for colors
		final TableViewerColumn colColor  = new TableViewerColumn(tableViewer, SWT.LEFT, 0);
		lblColorProvider = new ColumnColorLabelProvider();
		colColor.setLabelProvider(lblColorProvider);
		
		TableColumn col = colColor.getColumn();
		col.setText(" ");
		col.setWidth(COLUMN_COLOR_WIDTH);
		col.setResizable(false);
		layout.setColumnData(col, new ColumnPixelData(IConstants.COLUMN_COLOR_WIDTH_PIXELS, false));
		
		// column for procedure name
		final TableViewerColumn colProc  = new TableViewerColumn(tableViewer, SWT.LEFT, 1);
		lblProcProvider = new ColumnProcedureLabelProvider();
		colProc.setLabelProvider(lblProcProvider);
		
		TableColumn column = colProc.getColumn();
		column.setText("Procedure");
		layout.setColumnData(column, new ColumnWeightData(COLUMN_PROC_WEIGHT, COLUMN_PROC_MIN_WIDTH, true));
		column.addSelectionListener(getSelectionAdapter(column, 0));
		
		// column for the percentage
		final TableViewerColumn colCount = new TableViewerColumn(tableViewer, SWT.LEFT, 2);
		colCount.setLabelProvider(new ColumnStatLabelProvider());
		
		column = colCount.getColumn();
		
		column.setText("%");
		column.setAlignment(SWT.RIGHT);
		column.addSelectionListener(getSelectionAdapter(column, 1));
		setColumnWidth(layout, column);

		// setup the table viewer
		tableViewer.setContentProvider(new StatisticContentProvider());		
		
		comparator = new TableStatComparator();
		tableViewer.setComparator(comparator);
		
		ColumnViewerToolTipSupport.enableFor(tableViewer, ToolTip.NO_RECREATE);
		
		this.broker = broker;
		addListener(SWT.Show, this);
	}
	
	
	/*****
	 * Create and return a new selection adapter for a given column
	 * 
	 * @param column
	 * @param index
	 * @return
	 */
	private SelectionAdapter getSelectionAdapter(final TableColumn column,
										 		 final int index) {
		SelectionAdapter adapter = new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				comparator.setColumn(index);
				int dir = comparator.getDirection();
				tableViewer.getTable().setSortDirection(dir);
				tableViewer.getTable().setSortColumn(column);
				tableViewer.refresh();
			}
		};
		return adapter;
	}
	
	
	@Override
	public void setInput(Object input) {
		this.input = input;
		
		broker.subscribe(getTopicEvent(), this);
		ViewerPreferenceManager.INSTANCE.getPreferenceStore().addPropertyChangeListener(this);
		
		addDisposeListener(this);
	}	


	@Override
	public void widgetDisposed(DisposeEvent e) {

		broker.unsubscribe(this);
		ViewerPreferenceManager.INSTANCE.getPreferenceStore().removePropertyChangeListener(this);
		removeDisposeListener(this);
	}


	@Override
	public void handleEvent(Event event) {
		if (event.getTopic().equals(getTopicEvent())) {
			Object obj = event.getProperty(IEventBroker.DATA);
			
			if (obj == null)
				return;
			
			assert(obj instanceof TraceEventData);
			
			TraceEventData eventData = (TraceEventData) obj;
			
			if (eventData.data != input)
				return;
			
			List<StatisticItem> list = getListItems(eventData.value);
			tableViewer.setInput(list);
		}
	}


	@Override
	public void handleEvent(org.eclipse.swt.widgets.Event event) {}


	@Override
	public void propertyChange(PropertyChangeEvent event) {
		final String property = event.getProperty();
		if (property.equals(PreferenceConstants.ID_FONT_GENERIC) || 
			property.equals(PreferenceConstants.ID_FONT_METRIC)) {
			
			tableViewer.refresh();
		}
	}

	/**
	 * TODO: hack version to get the current number of items.
	 * Used by the parent to check if the table has been initialized or not.
	 * 
	 * @return number of items
	 */
	public int getItemCount() {
		if (tableViewer == null) return 0;
		return tableViewer.getTable().getItemCount();
	}

	
	private void setColumnWidth(TableColumnLayout layout, TableColumn column) {
		Drawable parent = column.getDisplay();
		GC gc = new GC(parent);
		gc.setFont(FontManager.getMetricFont());

		String text = TEXT_PERCENT_PATTERN;
		if (OSValidator.isWindows()) {
			
			// FIXME: ugly hack to add some spaces for Windows
			// Somehow, Windows 10 doesn't allow to squeeze the text inside the table
			// we have to give them some spaces (2 spaces in my case).
			// A temporary fix for issue #37
			text += "xx";
		}

		Point extent = gc.textExtent(text);
		int width = (int) (extent.x);
		column.setWidth(width);
		layout.setColumnData(column, new ColumnPixelData(width, true));

		gc.dispose();
	}

	/*************************************************************
	 * 
	 * Content provider for the table in statistic view
	 *
	 *************************************************************/
	static private class StatisticContentProvider 
	implements IStructuredContentProvider
	{
		StatisticContentProvider() {}

		@SuppressWarnings("unchecked")
		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement == null)
				return null;
			
			ArrayList<StatisticItem> list = (ArrayList<StatisticItem>) inputElement;
			int size = list.size();
			StatisticItem []items = new StatisticItem[size];
			
			list.toArray(items);
			
			return items;
		}
		
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
	}

	
	/*************************************************************
	 * 
	 * Color of the procedure
	 *
	 *************************************************************/
	static private class ColumnColorLabelProvider extends ColorColumnLabelProvider 
	{
		@Override
		protected Color getColor(org.eclipse.swt.widgets.Event event, Object element) {
			if (element != null && element instanceof StatisticItem) {
				StatisticItem item = (StatisticItem) element;
				return item.color;
			}
			return event.display.getSystemColor(SWT.COLOR_WHITE);
		}
	}
	
	/*************************************************************
	 * 
	 * Class to manage label of procedure name
	 *
	 *************************************************************/
	static private class ColumnProcedureLabelProvider extends ColumnLabelProvider
	{		

		@Override
		public Font getFont(Object element) {
			return FontManager.getFontGeneric();
		}

		
		@Override
		public String getText(Object element) {
			if (element == null || !(element instanceof StatisticItem))
				return null;
			
			StatisticItem item = (StatisticItem) element;
			return item.procedureName;
		}
		
		@Override
		public String getToolTipText(Object element) {
    		final String originalText = getText(element);
    		return StringUtil.wrapScopeName(originalText, 100);
		}
		
		@Override
		public int getToolTipDisplayDelayTime(Object object) {
    		return TracePreferenceManager.getTooltipDelay();
		}
	}

	
	/*************************************************************
	 * 
	 * Class to manage label of statistic numnber
	 *
	 *************************************************************/
	private class ColumnStatLabelProvider extends ColumnLabelProvider
	{

		@Override
		public Font getFont(Object element) {
			return FontManager.getMetricFont();
		}

		@Override
		public String getText(Object element) {
			if (element == null || !(element instanceof StatisticItem))
				return null;
			
			StatisticItem item = (StatisticItem) element;
			return String.format("%.2f %%", item.percent);
		}
	}

	/***
	 * Return the name of the event topic to be handled.
	 * Every time the event arrives, the table will be refreshed.
	 * @return name of the event topic
	 */
	abstract protected String getTopicEvent();
	
	/***
	 * Get the color table mapping
	 * @return
	 */
	abstract protected ColorTable getColorTable();
	
	/***
	 * Get the list of items to be displayed in the table based on input from summary view.
	 * The list has to be the type of {@code List<StatisticItem>}
	 * @param input
	 * @return
	 */
	abstract protected List<StatisticItem> getListItems(Object input);
}
