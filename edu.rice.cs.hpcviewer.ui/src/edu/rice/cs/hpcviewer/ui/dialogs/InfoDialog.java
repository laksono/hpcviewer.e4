package edu.rice.cs.hpcviewer.ui.dialogs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;

import edu.rice.cs.hpclog.LogProperty;
import edu.rice.cs.hpcsetting.preferences.ViewerPreferenceManager;
import edu.rice.cs.hpcviewer.ui.util.ApplicationProperty;


/************************************************************************
 * 
 * Dialog to show collected information on Eclipse, Java and the system
 * <ul>
 * <li>Export to a file if needed
 * </ul>
 *
 ************************************************************************/
public class InfoDialog extends Dialog 
{
	private TableViewer tableViewer;
	
	public InfoDialog(Shell parentShell) {
		super(parentShell);
	}
	
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);

		tableViewer  = new TableViewer(container, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		
		TableViewerColumn colKeyViewer = new TableViewerColumn(tableViewer, SWT.LEFT_TO_RIGHT);
		colKeyViewer.getColumn().setText("Key");
		colKeyViewer.getColumn().setWidth(200);
		colKeyViewer.getColumn().setResizable(true);
		
		TableViewerColumn colValViewer = new TableViewerColumn(tableViewer, SWT.LEFT_TO_RIGHT);
		colValViewer.getColumn().setText("Value");
		colValViewer.getColumn().setWidth(1000);
		colValViewer.getColumn().setResizable(true);
		
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		
		tableViewer.setContentProvider(new IStructuredContentProvider() {
			
			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement == null)
					return null;
				
				@SuppressWarnings("unchecked")
				Map<String, String> map = (Map<String, String>) inputElement;

				return map.entrySet().toArray();
			}
		});
		
		colKeyViewer.setLabelProvider(new ColumnLabelProvider() {
			
			@Override
			public String getText(Object element) {
				if (element == null)
					return null;
				
				@SuppressWarnings("unchecked")
				Map.Entry<String, String> entry = (Entry<String, String>) element;
				return entry.getKey();
			}
		});
		
		colValViewer.setLabelProvider(new ColumnLabelProvider()  {
			
			@Override
			public String getText(Object element) {
				if (element == null)
					return null;
				
				@SuppressWarnings("unchecked")
				Map.Entry<String, String> entry = (Entry<String, String>) element;
				return entry.getValue();
			}
		} );
		
		FillLayout fill = new FillLayout();
		container.setLayout(fill);		
		
		tableViewer.setInput(getInfo());
		
		return container;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.HELP_ID, "Export", false);
		createButton(parent, IDialogConstants.DETAILS_ID, "Show log files", false);
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.HELP_ID) {
			export();
		} else if (buttonId == IDialogConstants.DETAILS_ID) {
			InfoLogDialog logDialog = new InfoLogDialog(getShell());
			logDialog.open();
			
		} else {
			super.buttonPressed(buttonId);
		}
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Information");
	}

	
	@Override
	protected Point getInitialSize() {
		return new Point(600, 400);
	}
	
	
	/***
	 * export the content of the table to a file
	 */
	private void export() {
		FileDialog fileDlg = new FileDialog(getShell(), SWT.SAVE);
		fileDlg.setText("Export to file ...");
		fileDlg.setFilterExtensions(new String[] {"*.txt", "*.csv"});
		fileDlg.setFileName("settings.txt");
		fileDlg.setOverwrite(true);
		String filename = fileDlg.open();
		
		if (filename != null) {
			File file = new File(filename);
			try {
				PrintWriter pw = new PrintWriter(file);
				TableItem [] items = tableViewer.getTable().getItems();
				for(TableItem item: items) {
					String key = item.getText(0);
					String val = item.getText(1);
					pw.format("%s : \t%s\n", key, val == null? "" : val);
				}
				pw.close();
				
			} catch (FileNotFoundException e) {
				MessageDialog.openError(getShell(), "Error", e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	
	
	/***
	 * Retrieve the list of infos
	 * @return Map<String, String> info
	 */
	private Map<String, String> getInfo() {

		Map<String, String>  mapTableItems = new LinkedHashMap<String, String>();

		try {
			String version = ApplicationProperty.getVersion();
			mapTableItems.put("Version: ", version.trim());

			String locHome  = Platform.getUserLocation().toString();
			String location = ViewerPreferenceManager.INSTANCE.getPreferenceStoreLocation();
			String locInstall = Platform.getInstallLocation().getURL().getFile();
			String locInstance = Platform.getInstanceLocation().getURL().getFile();
			String locUser   = Platform.getLogFileLocation().toOSString(); //.getUserLocation().getURL().getFile();
			String locConfig = Platform.getConfigurationLocation().toString();

			List<String> logUser = LogProperty.getLogFile();

			mapTableItems.put("hpcviewer Properties", null);
			mapTableItems.put("User location", 		locHome);
			mapTableItems.put("Install directory",  locInstall);
			mapTableItems.put("Instance directory", locInstance);
			mapTableItems.put("User log files",     logUser.toString());
			mapTableItems.put("Eclipse log user",   locUser);
			mapTableItems.put("Preference file",    location);
			mapTableItems.put("Configuration dir",  locConfig);

		} catch (Exception e) {
			e.printStackTrace();
		}
		mapTableItems.put("OS", 			Platform.getOS());
		mapTableItems.put("Arch", 			Platform.getOSArch());
		mapTableItems.put("Args", 			Arrays.toString(Platform.getCommandLineArgs()));
		mapTableItems.put("Window system",  Platform.getWS());

		mapTableItems.put("Dark theme", Display.isSystemDarkTheme() ? "Yes" : "No");
		
		int procs = Runtime.getRuntime().availableProcessors();
		mapTableItems.put("Number of processors", String.valueOf(procs));

		long memMax = Runtime.getRuntime().maxMemory();
		long memFree = Runtime.getRuntime().freeMemory();
		long memTot  = Runtime.getRuntime().totalMemory();
		
		mapTableItems.put("Max JVM memory (bytes)",   String.valueOf(memMax));
		mapTableItems.put("Free memory (bytes)",  String.valueOf(memFree));
		mapTableItems.put("Total available memory (bytes)", String.valueOf(memTot));
		
		mapTableItems.put("Java Properties", null);
		Properties properties = System.getProperties();
		properties.forEach((key, val) -> {
			mapTableItems.put(key.toString(), val.toString());
		});
		/*
		mapTableItems.put("System Properties", null);
		Map<String, String> mapSystem = System.getenv();
		mapTableItems.putAll(mapSystem);
		*/
		return mapTableItems;
	}
	
	
	/***
	 * Unit test
	 * @param args
	 */
	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		
		InfoDialog dlg = new InfoDialog(shell);
		dlg.open();
	}
}
