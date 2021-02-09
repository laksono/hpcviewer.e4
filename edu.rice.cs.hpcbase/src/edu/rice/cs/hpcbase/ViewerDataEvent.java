package edu.rice.cs.hpcbase;

import edu.rice.cs.hpc.data.experiment.Experiment;

/***************************************************************
 * 
 * Class to manage events.
 * 
 * Any parts of UI can share data based on Experiment. 
 * Each part is required to check if the experiment in this data is 
 *  exactly the same as their experiment or not. If not, they can skip the event. 
 *
 ***************************************************************/
public class ViewerDataEvent 
{	
	/** Event when metric columns have to be hidden or shown. See the data field. o*/
	static public final String TOPIC_HIDE_SHOW_COLUMN    = "hpcviewer/column_hide";
	
	/** Event when a new database has arrived. */
	static final public String TOPIC_HPC_NEW_DATABASE    = "hpcviewer/database_add";

	/** Event when a database has to be removed from the application */
	static final public String TOPIC_HPC_ADD_NEW_METRIC  = "hpcviewer/metric_add";

	static final public String TOPIC_HPC_METRIC_UPDATE   = "hpcviewer/metric_update";

	public Experiment experiment;
	public Object 	  data;
	
	
	/***
	 * Constructor to create a data event
	 * 
	 * @param experiment Experiment database
	 * @param data the data to be shared among different parts
	 */
	public ViewerDataEvent(Experiment experiment, Object data) {
		this.experiment = experiment;
		this.data		= data;
	}
}
