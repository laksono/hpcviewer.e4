package edu.rice.cs.hpcviewer.ui.actions;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.Dialog;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.metric.DerivedMetric;
import edu.rice.cs.hpc.data.experiment.metric.IMetricManager;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpcviewer.ui.dialogs.ExtDerivedMetricDlg;
import edu.rice.cs.hpcviewer.ui.internal.ViewerDataEvent;

/****************************************************
 * 
 * Class to handle adding a user derived metrics
 *
 ****************************************************/
public class UserDerivedMetric 
{
	final private RootScope 	 root;
	final private IEventBroker   eventBroker;
	final private IMetricManager metricMgr;

	/****
	 * Constructor to initialize a user derived metric action
	 * @param root RootScope
	 * @param metricMgr a IMetricManager object. Thread view has a special metric manager than others.
	 * @param eventBroker IEventBroker
	 */
	public UserDerivedMetric(RootScope root, IMetricManager metricMgr, IEventBroker eventBroker) {
		
		this.root 		 = root;
		this.eventBroker = eventBroker;
		this.metricMgr   = metricMgr;
	}
	
	
	/***
	 * Add a new metric.
	 * IF a user click the cancel button, there is no metric is added.
	 */
	public void addNewMeric() {
		
		ExtDerivedMetricDlg dialog = new ExtDerivedMetricDlg(null, metricMgr, root);
	
		if (dialog.open() == Dialog.OK) {
			
			final DerivedMetric metric = dialog.getMetric();
			metricMgr.addDerivedMetric(metric);
			
			ViewerDataEvent data = new ViewerDataEvent((Experiment) root.getExperiment(), metric);
			eventBroker.post(ViewerDataEvent.TOPIC_HPC_ADD_NEW_METRIC, data);
		}
	}
}
