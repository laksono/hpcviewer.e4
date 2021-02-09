package edu.rice.cs.hpctraceviewer.data.local;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;

import edu.rice.cs.hpc.data.experiment.BaseExperiment;
import edu.rice.cs.hpc.data.experiment.InvalExperimentException;
import edu.rice.cs.hpc.data.experiment.extdata.FileDB2;
import edu.rice.cs.hpc.data.experiment.extdata.IFileDB;
import edu.rice.cs.hpc.data.util.Constants;
import edu.rice.cs.hpc.data.util.Util;
import edu.rice.cs.hpctraceviewer.data.AbstractDBOpener;
import edu.rice.cs.hpctraceviewer.data.DatabaseAccessInfo;
import edu.rice.cs.hpctraceviewer.data.SpaceTimeDataController;
import edu.rice.cs.hpctraceviewer.data.version3.FileDB3;


/****************************************************
 * 
 * Class to open local trace database
 *
 ****************************************************/
public class LocalDBOpener extends AbstractDBOpener 
{
	private String directory;
	private int version;
	private BaseExperiment experiment;
	private final IEclipseContext context;
	
	/*******
	 * prepare opening a database 
	 * 
	 * @param IEclipseContext context
	 * @param DatabaseAccessInfo info
	 * 
	 * @throws Exception 
	 */
	public LocalDBOpener(IEclipseContext context, DatabaseAccessInfo info) throws Exception
	{
		this(context, info.getDatabasePath());
	}

	
	/*****
	 * Prepare opening a database
	 * @param IEclipseContext context
	 * @param experiment
	 * @throws Exception 
	 */
	public LocalDBOpener(IEclipseContext context, BaseExperiment experiment) throws Exception {
		this(context, experiment.getDefaultDirectory().getAbsolutePath());
		this.experiment = experiment;
		version = experiment.getMajorVersion();
	}
	
	
	/*****
	 * Prepare opening a database.
	 * @param IEclipseContext context
	 * @param directory absolute path to the directory
	 */
	public LocalDBOpener(IEclipseContext context, String directory)  throws Exception {
		
		this.context   = context;
		this.directory = directory;
		version = LocalDBOpener.directoryHasTraceData(directory);
		if (version<=0) {
			throw new Exception("The directory does not contain hpctoolkit database with trace data:"
					+ directory);
		}
	}

	@Override
	public int getVersion() {
		return version;
	}

	
	/****
	 * Create an instance of {@code IFileDB} depending on the database version 
	 * @return IFileDB
	 * @throws InvalExperimentException
	 */
	private IFileDB getFileDB() throws InvalExperimentException {
		IFileDB fileDB = null;
		switch (version)
		{
		case 1:
		case Constants.EXPERIMENT_DENSED_VERSION:
			fileDB = new FileDB2();
			break;
		case 3:
		case Constants.EXPERIMENT_SPARSE_VERSION:
			fileDB = new FileDB3();
			break;
		default:
			throw new InvalExperimentException("Trace data version is not unknown: " + version);
		}
		return fileDB;
	}
	
	
	@Override
	public SpaceTimeDataController openDBAndCreateSTDC(IProgressMonitor statusMgr)
			throws IOException, InvalExperimentException, Exception {

		// ---------------------------------------------------------------------
		// Try to open the database and refresh the data
		// ---------------------------------------------------------------------
		if (statusMgr != null)
			statusMgr.setTaskName("Opening trace data...");

		IFileDB fileDB = getFileDB();
		
		// prepare the xml experiment and all extended data
		if (experiment == null)
			return new SpaceTimeDataControllerLocal(context, statusMgr, directory, fileDB);
		else 
			return new SpaceTimeDataControllerLocal(context, statusMgr, experiment, fileDB);
	}

	
	@Override
	public void end() {}


	/**********************
	 * static method to check if a directory contains hpctoolkit's trace data
	 * 
	 * @param directory : a database directory
	 * @return int version of the database if the database is correct and valid
	 * 			   return negative number otherwise
	 */
	static private int directoryHasTraceData(String directory)
	{
		File file = new File(directory);
		String database_directory;
		if (file.isFile()) {
			// if the argument is a file, then we'll look for its parent directory
			file = file.getParentFile();
			database_directory = file.getAbsolutePath();
		} else {
			database_directory = directory;
		}
		// checking for version 4.0
		String file_path = database_directory + File.separatorChar + "trace.db";
		File tmp_file 	 = new File(file_path);
		if (tmp_file.canRead()) {
			return Constants.EXPERIMENT_SPARSE_VERSION;
		}
		
		// checking for version 2.0
		file_path = database_directory + File.separatorChar + "experiment.mt";
		tmp_file  = new File(file_path);
		if (tmp_file.canRead()) {
			return Constants.EXPERIMENT_DENSED_VERSION;
		}
		
		// checking for version 2.0 with old format files
		tmp_file  = new File(database_directory);
		File[] file_hpctraces = tmp_file.listFiles( new Util.FileThreadsMetricFilter("*.hpctrace") );
		if (file_hpctraces != null && file_hpctraces.length>0) {
			return 1;
		}
		return -1;
	}
}
