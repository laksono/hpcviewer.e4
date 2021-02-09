package edu.rice.cs.hpcviewer.ui.addon;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;

import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.rice.cs.hpc.data.experiment.BaseExperiment;
import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.InvalExperimentException;
import edu.rice.cs.hpc.data.trace.BaseTraceAttribute;
import edu.rice.cs.hpc.data.util.Constants;
import edu.rice.cs.hpc.filter.service.FilterMap;
import edu.rice.cs.hpcbase.BaseConstants;
import edu.rice.cs.hpcbase.ViewerDataEvent;
import edu.rice.cs.hpcbase.map.UserInputHistory;
import edu.rice.cs.hpcbase.ui.IMainPart;
import edu.rice.cs.hpctraceviewer.data.AbstractDBOpener;
import edu.rice.cs.hpctraceviewer.data.local.LocalDBOpener;
import edu.rice.cs.hpctraceviewer.ui.TracePart;
import edu.rice.cs.hpcviewer.ui.ProfilePart;
import edu.rice.cs.hpcviewer.ui.experiment.ExperimentManager;
import edu.rice.cs.hpcviewer.ui.handlers.RecentDatabase;
import edu.rice.cs.hpcviewer.ui.util.ElementIdManager;

/***
 * <b>
 * Class Database manager
 * </b>
 * <p>It manages multiple experiment databases, including:
 * <ul>
 *  <li>Adding a new database. It sends message DatabaseManager.EVENT_HPC_NEW_DATABASE to
 *      the application components.</li>
 *  <li>Removing an existing database</li>
 * </ul>
 *</p>
 */
@Creatable
@Singleton
public class DatabaseCollection 
{
	static private final String STACK_ID_BASE 	  = "edu.rice.cs.hpcviewer.ui.partstack.integrated";
	

	final private HashMap<MWindow, List<BaseExperiment>>   mapWindowToExperiments;
	final private HashMap<BaseExperiment, ViewerDataEvent> mapColumnStatus;
	
	private IEventBroker      eventBroker;
    private ExperimentManager experimentManager;
	
	private Logger    statusReporter;
	
	private @Inject UISynchronize sync;
	
	public DatabaseCollection() {

		mapColumnStatus = new HashMap<BaseExperiment, ViewerDataEvent>();		
		experimentManager = new ExperimentManager();
		
		mapWindowToExperiments = new HashMap<MWindow, List<BaseExperiment>>(1);
	}
	
	@Inject
	@Optional
	private void subscribeApplicationCompleted(
			@UIEventTopic(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE) 
			final MApplication   application,
			final EPartService   partService,
			final IEventBroker   broker,
			final EModelService  modelService, 
			@Named(IServiceConstants.ACTIVE_SHELL) Shell myShell) {
		
		this.eventBroker    = broker;

		this.statusReporter = LoggerFactory.getLogger(getClass());

		// handling the command line arguments:
		// if one of the arguments specify a file or a directory,
		// try to find the experiment.xml and load it.
		
		String args[] = Platform.getApplicationArgs();
		
		if (myShell == null) {
			myShell = new Shell(SWT.TOOL | SWT.NO_TRIM);
		}
		final Shell shell = myShell;
		
		String path = null;
		
		for (String arg: args) {
			if (arg.charAt(0) != '-')
				path = arg;
		}
		if (path == null || path.length() < 1) {
			String filename = checkExistance(application.getSelectedElement(), shell, null);
			if (filename == null)
				return; 
			path = filename;
		}
		final String filePath = path;
		
		// On Linux TWM window manager, the window may not be ready yet.
		sync.asyncExec(()-> {
			openDatabaseAndCreateViews(application, modelService, partService, shell, filePath);
		});
	}
	

	/****
	 * One-stop API to open and add a database. 
	 * This method shows a dialog box to pick a directory, check if the database already exists or not,
	 * create views and add to the list of the database collection.
	 * 
	 * @param shell the current shell
	 * @param application MApplication
	 * @param service EPartService
	 * @param modelService EModelService
	 * @param database directory
	 */
	public void addDatabase(Shell shell, 
			MApplication 	application, 
			EPartService    service,
			EModelService 	modelService,
			String          database) {
		
		String filename = checkExistance(application.getSelectedElement(), shell, database);
		if (filename == null)
			return;
		
		BaseExperiment exp = getExperimentObject(application.getSelectedElement(), filename);
		if (exp != null) {
			removeDatabase(application, modelService, service, exp);
		}
		
		openDatabaseAndCreateViews(application, modelService, service, shell, filename);
	}
	

	
	/****
	 * One-stop API to open a database. 
	 * This method shows a dialog box to pick a directory, check if the database already exists or not,
	 * create views and remove the existing databases before adding it to the list of the database collection.
	 * The removal is important to make sure there is only one database exist.
	 * 
	 * @param shell the current shell
	 * @param application MApplication
	 * @param service EPartService
	 * @param modelService EModelService
	 * @param database
	 */
	public void switchDatabase(Shell shell, 
			MApplication 	application, 
			EPartService    service,
			EModelService 	modelService,
			String          database) {
		
		String filename = checkExistance(application.getSelectedElement(), shell, database);
		if (filename == null)
			return;

		removeAll(application, modelService, service);
		
		openDatabaseAndCreateViews(application, modelService, service, shell, filename);
	}

	
	/****
	 * Add a new database into the collection.
	 * This database can be remove later on by calling {@code removeLast}
	 * or {@code removeAll}.
	 * 
	 * @param experiment cannot be null
	 * @param application the main application
	 * @param service EPartService to create parts
	 * @param modelService
	 * @param parentId the parent EPartStack of the parts. If it's null, the new parts will be assigned to the current active
	 */
	public void createViewsAndAddDatabase(BaseExperiment experiment, 
										  MApplication 	 application, 
										  EPartService   service,
										  EModelService  modelService) {
		
		if (experiment == null || service == null) {
			MessageDialog.openError( Display.getDefault().getActiveShell(), 
									 "Error in opening the file", 
									 "Database not found. " );
			return;
		}
		
		// Corner case for TWM window manager: sometimes the processing is faster
		// than the UI, and thus Eclipse doesn't provide any context or any child
		// at this stage. Maybe we should wait until it's ready?
		// 
		// using asyncExec we hope Eclipse will delay the processing until the UI 
		// is ready. This doesn't guarantee anything, but works in most cases :-(

		sync.asyncExec(()-> {
			showPart(experiment, application, modelService, service);
		});
	}
	

	/***
	 * Display a profile part 
	 * 
	 * @param experiment
	 * @param application
	 * @param service
	 * @param parentId
	 * 
	 * @return int
	 */
	private int showPart( BaseExperiment experiment, 
						   MApplication   application, 
						   EModelService  modelService,
						   EPartService   service) {

		//----------------------------------------------------------------
		// find an empty slot in the part stack
		// If no slot is available, we will try to create a new one.
		// However,creating a new part stack is tricky, and it's up to the
		// system where to locate the part stack.
		//----------------------------------------------------------------
		
		MWindow  window = application.getSelectedElement();
		if (window == null) {
			// window is not active yet
			
			// using asyncExec we hope Eclipse will delay the processing until the UI 
			// is ready. This doesn't guarantee anything, but works in most cases :-(
			sync.asyncExec(()-> {
				showPart(experiment, application, modelService, service);
			});
			return -1;
		}

		List<MStackElement> list = null;

		MPartStack stack  = (MPartStack)modelService.find(STACK_ID_BASE, window);
		if (stack != null)
			list = stack.getChildren();
		
		//----------------------------------------------------------------
		// create a new part stack if necessary
		// We don't want this, since it makes the layout weird.
		//----------------------------------------------------------------
		if (stack == null) {
			System.out.println("create a new part stack");
			
			stack = modelService.createModelElement(MPartStack.class);
			stack.setElementId(STACK_ID_BASE);
			stack.setToBeRendered(true);
			
			list = stack.getChildren();
		}
		
		final MPart part = service.createPart(ProfilePart.ID);
		if (list != null)
			list.add(part);
		
		service.showPart(part, PartState.VISIBLE);
		IMainPart view = null;

		int maxAttempt = 20;		
		while(maxAttempt>0) {
			view = (IMainPart) part.getObject();
			if (view != null)
				break;
			
			try {
				Thread.sleep(300);					
			} catch (Exception e) {
				
			}
			maxAttempt--;
		}
		if (view == null) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), 
									"Fail to get the view", 
									"hpcviewer is unable to retrieve the view. Please try again");
			return 0;
		}
		
		// has to set the element Id before populating the view
		// this is to avoid an issue where a stack cannot store parts of the same elementId
		// If there are two parts have the same elementID, when one is moved to the same stack.
		// it will remove the other part.
		
		String elementID = "P." + ElementIdManager.getElementId(experiment);
		part.setElementId(elementID);

		view.setInput(part, experiment);

		//----------------------------------------------------------------
		// part stack is ready, now we create all view parts and add it to the part stack
		// TODO: We assume adding to the part stack is always successful
		//----------------------------------------------------------------
		stack.setVisible(true);
		stack.setOnTop(true);
		
		List<BaseExperiment> listExperiments = getActiveListExperiments(application.getSelectedElement());
		if (listExperiments != null) {
			listExperiments.add(experiment);
		}

		//----------------------------------------------------------------
		// display the trace view if the information exists
		//----------------------------------------------------------------
		BaseTraceAttribute traceAtt = experiment.getTraceAttribute();
		if (traceAtt != null && traceAtt.dbTimeMax > 0) {
			try {
				AbstractDBOpener dbOpener = new LocalDBOpener(null, experiment);
				if (dbOpener.getVersion()<=0) {
					return 1;
				}
			} catch (Exception e) {
				return 0;
			}

			MPart tracePart  = service.createPart(TracePart.ID);
			MPart createPart = service.showPart(tracePart, PartState.CREATE);
			
			if (createPart != null) {
				// need to set the element id to avoid the same issue with the profile view
				// (see the comment at line 320-323)
				elementID = "T." + ElementIdManager.getElementId(experiment);
				createPart.setElementId(elementID);

				list.add(createPart);
				
				Object objTracePart = createPart.getObject();
				if (objTracePart != null) {
					((TracePart)objTracePart).setInput(createPart, experiment);
				}
			}
		}
		return 1;
	}
	
	
	
	
	/***
	 * Retrieve the iterator of the database collection from a given windo
	 * 
	 * @param window 
	 * @return Iterator for the list of the given window
	 */
	public Iterator<BaseExperiment> getIterator(MWindow window) {
		List<BaseExperiment> list = mapWindowToExperiments.get(window);
		if (list == null)
			return null;
		return list.iterator();
	}
	
		
	/***
	 * Retrieve the current registered databases
	 * @return
	 */
	public int getNumDatabase(MWindow window) {
		List<BaseExperiment> list = getActiveListExperiments(window);
		if (list == null)
			return 0;
		return list.size();
	}
	
	
	/***
	 * Check if the database is empty or not
	 * @return true if the database is empty
	 */
	public boolean isEmpty(MWindow window) {
		List<BaseExperiment> list = getActiveListExperiments(window);
		return list.isEmpty();
	}
	
	
	/***
	 * Remove the last registered database
	 * @return
	 */
	public BaseExperiment getLast(MWindow window) {
		List<BaseExperiment> list = getActiveListExperiments(window);
		return list.get(list.size()-1);
	}
	
	
	
	
	/***
	 * Retrieve the experiment object given a XML file path
	 * 
	 * @param pathXML the absolute path of the experiment XNK file path
	 * @return BaseExperiment object if the database exist, null otherwise.
	 */
	public BaseExperiment getExperimentObject(MWindow window, String pathXML) {
		List<BaseExperiment> list = getActiveListExperiments(window);
		
		if (list == null || list.isEmpty())
			return null;
		
		for (BaseExperiment exp: list) {
			String directory = exp.getXMLExperimentFile().getAbsolutePath();
			if (directory.equals(pathXML)) {

				return exp;
			}
		}
		return null;
	}
	
	/***
	 * Check if a database is good or already exist in the collection.
	 * <ul>
	 * <li>If it's empty: show a window to pick a database
	 * <li>If it's a directory, add the database filename
	 * <li>If it's a file: check if exist or not.
	 * </ul>

	 * @param shell
	 * @param pathXML the absolute path to XML file
	 * @return {@code DatabaseExistance}
	 */
	public String checkExistance(MWindow window, Shell shell, String fileOrDirectory) {
		
		// 1. convert the database path to experiment.xml file name
		
		String filename = fileOrDirectory;
		if (filename == null) {
			filename = experimentManager.openFileExperiment(shell);
			if (filename == null)
				return null;
		}else {
			int index = filename.lastIndexOf(Constants.DATABASE_FILENAME);
			if (index<0) {
				// it's a directory. Add the experiment.xml file
				filename += File.separator + Constants.DATABASE_FILENAME; 
			}
		}

		// 2. check if the file exists
		
		File file = new File(filename);
		if (!file.exists()) {
			MessageDialog.openError(shell, 
					"Fail to open a database", 
					filename + ": file not found");
			return null;
		}

		// 3. check if the database already opened or not
		
		BaseExperiment exp = getExperimentObject(window, filename);
		
		if (exp != null) {
			// we cannot have two exactly the same database in one window
			if (MessageDialog.openQuestion(shell, 
									   "Warning: database already exists", 
									   exp.getDefaultDirectory().getAbsolutePath() +
									   ": the database is already opened.\nDo you want to replace the existing one?" ) )
			
				// user decides to replace the database
				return filename;

			// we give up
			return null;
		}
		// database is valid, fresh and not already opened
		return filename;
	}
	
	
	/***
	 * Remove a database and all parts (views and editors) associated with it
	 * 
	 * @param experiment
	 */
	public void removeDatabase(MApplication application, 
							   EModelService modelService, 
							   EPartService partService, 
							   final BaseExperiment experiment) {
		
		if (experiment == null)
			return;
		
		// remove any database associated with this experiment
		// some parts may need to check the database if the experiment really exits or not.
		// If not, they will consider the experiment will be removed.
		List<BaseExperiment> list = getActiveListExperiments(application.getSelectedElement());
		list.remove(experiment);

		mapColumnStatus.remove(experiment);
		
		MWindow window = application.getSelectedElement();
		List<MPart> listParts = modelService.findElements(window, null, MPart.class);
		
		if (listParts == null)
			return;

		// first, notify all the parts that have experiment that they will be destroyed.
		
		ViewerDataEvent data = new ViewerDataEvent((Experiment) experiment, null);
		eventBroker.send(BaseConstants.TOPIC_HPC_REMOVE_DATABASE, data);
		
		// make sure the experiment's resources are disposed
		experiment.dispose();
		
		// destroy all the views and editors that belong to experiment
		// since Eclipse doesn't have "destroy" method, we hide them.
		
		for(MPart part: listParts) {
			Object obj = part.getObject();
			if (obj instanceof IMainPart) {
				IMainPart mpart = (IMainPart) obj;
				if (mpart.getExperiment() == experiment) {
					partService.hidePart(part, true);
				}
			}
		}
	}	
	
	
	/****
	 * Remove all databases
	 * @return
	 */
	public int removeAll(MApplication application, EModelService modelService, EPartService partService) {
		List<BaseExperiment> list = getActiveListExperiments(application.getSelectedElement());
		
		int size = list.size();
		
		// TODO: ugly solution to avoid concurrency (java will not allow to remove a list while iterating).
		// we need to copy the list to an array, and then remove the list
		
		BaseExperiment arrayExp[] = new BaseExperiment[size];
		list.toArray(arrayExp);
		
		for(BaseExperiment exp: arrayExp) {
			// inside this method, we remove the database AND hide the view parts
			removeDatabase(application, modelService, partService, exp);
		}
		list.clear();
		mapColumnStatus.clear();
		
		return size;
	}
	
	
	public void addColumnStatus(BaseExperiment experiment, ViewerDataEvent data) {
		mapColumnStatus.put(experiment, data);
	}
	
	
	public ViewerDataEvent getColumnStatus(BaseExperiment experiment) {
		return mapColumnStatus.get(experiment);
	}
	
	
	
	/***
	 * Retrieve the list of experiments of the current window.
	 * If Eclipse reports there is no active window, the list is null.
	 * 
	 * @return the list of experiments (if there's an active window). null otherwise.
	 * 
	 */
	private List<BaseExperiment> getActiveListExperiments(MWindow window) {

		if (window == null) {
			statusReporter.error("No active window");
			return null;
		}
		List<BaseExperiment> list = mapWindowToExperiments.get(window);
		
		if (list == null) {
			list = new ArrayList<BaseExperiment>();
			mapWindowToExperiments.put(window, list);
		}
		return list;
	}
	
	
	/****
	 * Find a database for a given path
	 * 
	 * @param shell the active shell
	 * @param expManager the experiment manager
	 * @param directoryOrXMLFile path to the database
	 * @return
	 * @throws Exception 
	 */
	private BaseExperiment openDatabase(Shell shell, String directoryOrXMLFile) throws Exception {
    	IFileStore fileStore;

		try {
			fileStore = EFS.getLocalFileSystem().getStore(new URI(directoryOrXMLFile));
			
		} catch (URISyntaxException e) {
			// somehow, URI may throw an exception for certain schemes. 
			// in this case, let's do it traditional way
			fileStore = EFS.getLocalFileSystem().getStore(new Path(directoryOrXMLFile));
			statusReporter.warn("Unable to locate " + directoryOrXMLFile, e);
		}
    	IFileInfo objFileInfo = fileStore.fetchInfo();

    	if (!objFileInfo.exists())
    		return null;

    	BaseExperiment experiment = null;
    	
    	if (objFileInfo.isDirectory()) {
    		experiment = experimentManager.openDatabaseFromDirectory(shell, directoryOrXMLFile);
    	} else {
			EFS.getLocalFileSystem().fromLocalFile(new File(directoryOrXMLFile));
			experiment = experimentManager.loadExperiment(directoryOrXMLFile);
    	}
    	return experiment;
	}
	

	/****
	 * Open a database using background job, and then create views using UI thread
	 *
	 * @param application
	 * @param modelService
	 * @param partService
	 * @param shell
	 * @param xmlFileOrDirectory
	 */
	public void openDatabaseAndCreateViews(final MApplication application,
											final EModelService modelService,
											final EPartService partService,
											final Shell shell, 
											final String xmlFileOrDirectory) {
		
		try {
			final BaseExperiment experiment = openDatabase(shell, xmlFileOrDirectory);
			if (experiment == null) {
				return;
			}
			
			// filter the tree if user has defined at least a filter
			
			FilterMap filterMap = FilterMap.getInstance();
			if (filterMap.isFilterEnabled()) {
				experiment.filter(filterMap);
			}
			
			// Everything works just fine: create views
			createViewsAndAddDatabase(experiment, application, partService, modelService);

		} catch (InvalExperimentException ei) {
			final String msg = "Invalid database " + xmlFileOrDirectory + "\nError at line " + ei.getLineNumber();
			statusReporter.error(msg, ei);

			MessageDialog.openError(shell, "Error " + ei.getClass(), msg);
			return;
			
		} catch (Exception e) {
			final String msg = "Error opening the database " + xmlFileOrDirectory + ":\n  " + e.getMessage();
			statusReporter.error(msg, e);
			MessageDialog.openError(shell, "Error " + e.getClass(), msg);
			return;
		}

		// store the current loaded database to history
		// we need to ensure we only store the directory, not the xml file
		
		String path = xmlFileOrDirectory;
		int index = path.lastIndexOf(Constants.DATABASE_FILENAME);

		if (index>0) {
			path = path.substring(0, index-1);
		}
		UserInputHistory history = new UserInputHistory(RecentDatabase.HISTORY_DATABASE_RECENT);
		history.addLine(path);
	}
}
