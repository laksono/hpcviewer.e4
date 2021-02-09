package edu.rice.cs.hpctraceviewer.data;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;

import edu.rice.cs.hpc.data.experiment.InvalExperimentException;


/**
 * An interface for the DBOpeners. Specifically, it is implemented by
 * {@link RemoteDBOpener} and {@link LocalDBOpener}. Its main purpose is to
 * create a {@link SpaceTimeDataController} from the connection to the database
 * (be it local or remote), but it also partially handles closing that connection.
 * 
 * @author Philip Taffet
 * 
 */
public abstract class AbstractDBOpener {

	

	/**
	 * This prepares the database for retrieving data and creates a
	 * SpaceTimeDataController from that data. The local implementation
	 * (LocalDBOpener) should return a SpaceTimeDataControllerLocal while the
	 * remote implementation (RemoteDBOpener) should return a
	 * SpaceTimeDataControllerRemote.
	 * 
	 * @param statusMgr progress monitor
	 * @return
	 * @throws IOException 
	 * @throws Exception 
	 * @throws InvalExperimentException 
	 */
	public abstract SpaceTimeDataController openDBAndCreateSTDC(IProgressMonitor statusMgr)
			throws IOException, InvalExperimentException, Exception;

	
	/****
	 * Return the version of trace database. 
	 * @return the version or 0 if the database doesn't exist
	 */
	public abstract int getVersion();
	
	// Our current policy on closing: Except for back-to-back connections to the
	// same server, we should close the server when we are making a new
	// connection, local or remote.

	
	/*****
	 * closing the database.
	 * The caller is responsible to call this method to terminate the connection (in case of remote database)
	 * or closing local file (local database)
	 */
	public abstract void end();
}
