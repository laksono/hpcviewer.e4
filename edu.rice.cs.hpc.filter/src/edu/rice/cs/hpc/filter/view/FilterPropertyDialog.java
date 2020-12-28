package edu.rice.cs.hpc.filter.view;

import java.util.Iterator;
import java.util.Map.Entry;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.jface.viewers.TableViewerColumn;

import edu.rice.cs.hpc.data.filter.FilterAttribute;
import edu.rice.cs.hpc.filter.service.FilterMap;
import edu.rice.cs.hpc.filter.service.FilterStateProvider;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.window.Window;

/*********************************************************
 * 
 * Window for displaying the filters' property (FilterMap  object) to users,
 *  and modify them (edit or delete).
 *  <p> 
 *  By default, the window searches for filter map in workspace directory or user home directory.
 * However, caller can provide its own FilterMap object by calling {@code setInput()} method
 * after the initialization and before calling {@code open()} method.
 *********************************************************/
public class FilterPropertyDialog extends TitleAreaDialog implements IDoubleClickListener, ICheckStateListener
{
	private static final int INITIAL_SIZE_X = 450;
	private static final int INITIAL_SIZE_Y = 500;
	
	private final FilterStateProvider filterService;
	
	private Button btnEdit, btnDelete;
	private CheckboxTableViewer checkboxTableViewer;
	
	private FilterMap filterMap;
	
	
	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public FilterPropertyDialog(Shell parentShell, FilterStateProvider filterService) {
		super(parentShell);
		filterMap = new FilterMap();
		this.filterService = filterService;
	}

	
	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle("Filter properties");
		setMessage("A pattern to be matched and a type how the filter to be applied." +
					" If a row is checked, then the filter is enabled. Otherwise it's disabled.");
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		GridData parentGD = new GridData(GridData.FILL_BOTH);
		
		checkboxTableViewer = CheckboxTableViewer.newCheckList(container, 
															   SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		ColumnViewerToolTipSupport.enableFor(checkboxTableViewer, ToolTip.NO_RECREATE);
		
		Table table = checkboxTableViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		table.setLayoutData(gd);
		
		container.setLayoutData(parentGD);

		
		TableViewerColumn columnPattern = new TableViewerColumn(checkboxTableViewer, SWT.NONE);
		columnPattern.setLabelProvider(new PatternLabelProvider());
		TableColumn tblclmnPattern = columnPattern.getColumn();
		tblclmnPattern.setToolTipText("Select a filter to delete or check/uncheck a filter pattern to enable/disable.");
		tblclmnPattern.setWidth(149);
		tblclmnPattern.setText("Pattern");
		
		TableViewerColumn columnType = new TableViewerColumn(checkboxTableViewer, SWT.NONE);
		columnType.setLabelProvider(new TypeLabelProvider());
		TableColumn tblclmnType = columnType.getColumn();
		tblclmnType.setToolTipText("Select a type of filtering: 'Self only' means only the filtered scope is elided, 'Children only' means only the children of the matched node are elided, while 'Self and children' means the filtered scope and its descendants are elided.");
		tblclmnType.setWidth(100);
		tblclmnType.setText("Type");
		
		checkboxTableViewer.setContentProvider	 (new ArrayContentProvider());
		checkboxTableViewer.setCheckStateProvider(new CheckStateProvider());
		checkboxTableViewer.setComparator		 (new PatternViewerComparator());
		
		checkboxTableViewer.addCheckStateListener (this);
		checkboxTableViewer.addDoubleClickListener(this);
		
		Group grpActions = new Group(container, SWT.NONE);
		grpActions.setLayout(new FillLayout(SWT.VERTICAL));
		grpActions.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true, 1, 1));
		
		Button btnAdd = new Button(grpActions, SWT.NONE);
		btnAdd.setToolTipText("Add a new pattern filter");
		btnAdd.setText("Add");
		btnAdd.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				add();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		btnEdit = new Button(grpActions, SWT.NONE);
		btnEdit.setText("Edit");
		btnEdit.setEnabled(false);
		btnEdit.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				edit();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		btnDelete = new Button(grpActions, SWT.NONE);
		btnDelete.setText("Delete");
		btnDelete.setEnabled(false);
		btnDelete.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				delete();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		Button []buttons = new Button[] {btnEdit, btnDelete};
		checkboxTableViewer.addSelectionChangedListener(new SelectionChangedListener(buttons));

		// set the input of the table
	    setInput(filterMap);

		getShell().setText("Filter");

		return area;
	}

	
	/***
	 * Change the input data of the table
	 * 
	 * @param map the new filter map
	 */
	public void setInput(FilterMap map) {
		this.filterMap = map;
		checkboxTableViewer.setInput(map.getEntrySet());
	}
	
	
	/***
	 * Retrieve the input data 
	 * @return FilterMap the input data
	 */
	public FilterMap getInput() {
		return filterMap;
	}
	
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	
	@Override
	protected void okPressed() {
		apply();
		super.okPressed();
	}
	
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.HELP_ID) {
			apply();
			return;
		}
		super.buttonPressed(buttonId);
	}
	
	
	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,     IDialogConstants.OK_LABEL,     true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		createButton(parent, IDialogConstants.HELP_ID,   "Apply",                       false);
	}

	
	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(INITIAL_SIZE_X, INITIAL_SIZE_Y);
	}
		
	
	/**
	 * Apply the change and broadcast to everyone if necessary
	 */
	private void apply() {
		// 1. check if there's any change compared to the original filter
		FilterMap newMap = FilterMap.getInstance();
		newMap.iterator(); // TODO: forcing to load data
		
		// check if they have the same size
		// if they do, check the content
		boolean same = newMap.size() == filterMap.size();
		
		if (same) {
			// check the content
			same = newMap.equals(filterMap);
		}
		
		// 2. save the filter, and broadcast it
		if (!same) {
			filterMap.save();
			if (filterService != null) {
				filterService.broadcast(filterMap);
			}
		}
	}
	
	/***
	 * edit the current selected row
	 * 
	 * @param filterMap : filter map
	 * @param pattern : the pattern to be modified
	 * @param attribute : attribute
	 */
	private void edit(FilterMap 	  filterMap, 
					  String 		  pattern, 
					  FilterAttribute attribute) {
		
		Shell shell = getShell();
		final FilterInputDialog dialog = new FilterInputDialog(shell, "Editing a filter", pattern, attribute);
		if (dialog.open() == Window.OK)
		{
			FilterAttribute newattribute = dialog.getAttribute();
			if (filterMap.update(pattern, dialog.getValue(), newattribute))
			{
				setInput(filterMap);
			} else {
				MessageDialog.openWarning(shell, "Unable to update", "Failed to update the pattern.");
			}
		}
	}
	
	
	@Override
	public void doubleClick(DoubleClickEvent event) {
		final ISelection selection = event.getSelection();
		final StructuredSelection select = (StructuredSelection) selection;
		
		if (select != null && !select.isEmpty()) {
			@SuppressWarnings("unchecked")
			final Entry<String, FilterAttribute> item = (Entry<String, FilterAttribute>) select.getFirstElement();
			
			if (item != null) {
				edit(filterMap, item.getKey(), item.getValue());
			}
		}
	}
	
	
	/***
	 * Add a new filter item
	 * @return
	 */
	private boolean add() {
		
		final Shell shell = getShell(); 
		final FilterInputDialog dialog = new FilterInputDialog(shell, "Add a pattern", "", null);
		
		if (dialog.open() == IDialogConstants.OK_ID) {
			FilterAttribute attribute = dialog.getAttribute();
			final String key = dialog.getValue();
			
			if (filterMap.get(key) == null) {
				// save the new pattern to the registry
				filterMap.put(key, attribute);
				updateView(filterMap);
				return true;
			} else {
				MessageDialog.openError(shell, "Unable to add a new filter", 
						"The pattern already exists: " + key);
			}
		}
		return false;
	}

	
	/****
	 * Edit the selection item in the viewer
	 * @return
	 */
	private boolean edit() {
		ISelection selection = checkboxTableViewer.getSelection();
		if (selection != null) {
			final StructuredSelection select = (StructuredSelection) selection;
			@SuppressWarnings("unchecked")
			final Entry<String, FilterAttribute> item= (Entry<String, FilterAttribute>) select.getFirstElement();
			if (item == null)
				return false;

			final Shell shell = getShell();
			final FilterInputDialog dialog = new FilterInputDialog(shell, "Editing a filter", item.getKey(), item.getValue());
			
			if (dialog.open() == Window.OK)
			{
				//final FilterMap filterMap = FilterMap.getInstance();
				FilterAttribute attribute = dialog.getAttribute();
				if (filterMap.update(item.getKey(), dialog.getValue(), attribute))
				{
					updateView(filterMap);
					return true;
				} else {
					MessageDialog.openWarning(shell, "Unable to update", "Failed to update the pattern.");
				}
			}
		}
		return false;
	}

	
	/***
	 * Delete the selected item in the viewer
	 * @return
	 */
	private boolean delete() {
		ISelection selection = checkboxTableViewer.getSelection();
		if (selection != null) {
			final StructuredSelection select = (StructuredSelection) selection;
			int size = select.size();
			String message = null;
			if (size == 1) {
				@SuppressWarnings("unchecked")
				final Entry<String, FilterAttribute> item= (Entry<String, FilterAttribute>) select.getFirstElement();
				message = "Are you sure to delete '" + item.getKey() + "' pattern?";
			} else if (size > 1) {
				message = "Are you sure to delete " + size + " pattern?" ;
			}
			if (message != null) {
				
				final Shell shell = getShell();
				if (MessageDialog.openConfirm(shell, "Deleting a pattern", message)) {

					@SuppressWarnings("unchecked")
					Iterator<Entry<String, FilterAttribute>> iterator = select.iterator();
					while(iterator.hasNext()) {
						Entry<String, FilterAttribute> item = iterator.next();
						filterMap.remove(item.getKey());
					}
					updateView(filterMap);
				}
			}
		}
		return false;
	}
	
	
	@Override
	public void checkStateChanged(CheckStateChangedEvent event) {
		@SuppressWarnings("unchecked")
		final Entry<String, FilterAttribute> element = (Entry<String, FilterAttribute>) event.getElement();
		final String key = element.getKey();
		
		// get the original attribute to be modified
		FilterAttribute attribute = element.getValue();
		attribute.enable = event.getChecked();
		
		// change the filter.
		// now, it's up to caller to save to the registry
		filterMap.put(key, attribute);
	}	    	


	/****
	 * Update the checkbox viewer
	 * 
	 * @param filterMap
	 */
	private void updateView(final FilterMap filterMap) {
		setInput(filterMap);
	}

	
	///////////////////////////////////////////////////////////////////////
	// Helper classes
	///////////////////////////////////////////////////////////////////////
	
	/**
	 * 
	 * Label for pattern column
	 *
	 */
	static private class PatternLabelProvider extends CellLabelProvider
	{
		@Override
		public void update(ViewerCell cell) {
			@SuppressWarnings("unchecked")
			Entry<String, FilterAttribute> item = (Entry<String, FilterAttribute>) cell.getElement();
			cell.setText(item.getKey());
		}
		
		@Override
		public String getToolTipText(Object element) {
			if (element != null && element instanceof Entry<?, ?>) {
				@SuppressWarnings("unchecked")
				Entry<String, FilterAttribute> item = (Entry<String, FilterAttribute>) element;
				FilterAttribute attr = item.getValue();
				String enable = attr.enable ? " enabled " : " disabled ";
				StringBuffer sb = new StringBuffer();
				sb.append("Filter '");
				sb.append(item.getKey());
				sb.append("' is ");
				sb.append(enable);
				return sb.toString();
				
			}
			
			return element.toString();
		}
	}

	
	/******************************************************************
	 * 
	 * a label provider class for the filter table
	 *
	 *******************************************************************/
	static private class TypeLabelProvider extends CellLabelProvider
	{
		@Override
		public void update(ViewerCell cell) {
			@SuppressWarnings("unchecked")
			Entry<String, FilterAttribute> item = (Entry<String, FilterAttribute>) cell.getElement();
			cell.setText(item.getValue().getFilterType());
		}
		
		@Override
		public String getToolTipText(Object element) {
			if (element != null && element instanceof Entry<?, ?>) {
				@SuppressWarnings("unchecked")
				final Entry<String, FilterAttribute> item = (Entry<String, FilterAttribute>) element;
				final FilterAttribute attr = item.getValue();
				final String text = attr.getDescription();
				return text;
			}
			return element.toString();
		}
	}
	
	
	/******************************************************************
	 * 
	 * class to fill the value of a check box
	 *
	 *******************************************************************/
	static private class CheckStateProvider implements ICheckStateProvider
	{
		
		@Override
		public boolean isGrayed(Object element) {
			return false;
		}
		
		@Override
		public boolean isChecked(Object element) {
			if (element instanceof Entry<?,?>) 
			{
				@SuppressWarnings("unchecked")
				FilterAttribute value = ((Entry<String, FilterAttribute>)element).getValue();
				return value.enable.booleanValue();
			}
			return false;
		}
	}
	

	
	/*******************************************************************
	 * 
	 * class for a row comparison  
	 *
	 ********************************************************************/
	static private class PatternViewerComparator extends ViewerComparator
	{
    	@SuppressWarnings("unchecked")
		@Override
    	public int compare(Viewer viewer, Object e1, Object e2) 
    	{
			Entry<String, FilterAttribute> item1 = (Entry<String, FilterAttribute>) e1;
    		Entry<String, FilterAttribute> item2 = (Entry<String, FilterAttribute>) e2;
    		return (item1.getKey().compareTo(item2.getKey()));
    	}
	}
	
	/********************************************************************
	 * 
	 * class for a selection event on the table
	 *
	 ********************************************************************/
	private static class SelectionChangedListener implements ISelectionChangedListener
	{
		final private Button []btnSelections;
		
		public SelectionChangedListener(Button []buttons) {
			btnSelections = buttons;
		}
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			ISelection selection = event.getSelection();
			boolean enable = (selection != null) && (!selection.isEmpty());
			for(Button btn : btnSelections) {
				btn.setEnabled(enable);
			}
		}	
	}
	

	
	////////////////////////////////////////////////////////////////////////////////////////////////
	// 
	// unit test
	//
	////////////////////////////////////////////////////////////////////////////////////////////////
	
	static public void main(String []argv) {
		
		final Shell shell = new Shell();
		
		FilterPropertyDialog dlg = new FilterPropertyDialog(shell, null);
		dlg.open();
		FilterMap map = dlg.getInput();
		
		Iterator<Entry<String, FilterAttribute>> iterator = map.iterator();
		while(iterator.hasNext()) {
			Entry<String, FilterAttribute> entry = iterator.next();
			System.out.println(entry.getKey() + " : " + entry.getValue());
		}
	}
}
