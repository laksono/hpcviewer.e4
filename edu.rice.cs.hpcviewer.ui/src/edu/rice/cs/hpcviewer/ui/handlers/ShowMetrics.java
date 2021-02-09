 
package edu.rice.cs.hpcviewer.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.rice.cs.hpc.data.experiment.BaseExperiment;
import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.DerivedMetric;
import edu.rice.cs.hpcbase.ViewerDataEvent;
import edu.rice.cs.hpcbase.ui.IBasePart;
import edu.rice.cs.hpcviewer.ui.addon.DatabaseCollection;
import edu.rice.cs.hpcviewer.ui.dialogs.MetricPropertyDialog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.CanExecute;

public class ShowMetrics 
{
	static final private String ID_MENU_URI = "bundleclass://edu.rice.cs.hpcviewer.ui/" + ShowMetrics.class.getName();
	
	@Inject EPartService partService;
	@Inject DatabaseCollection database;
	
	
	
	@AboutToShow
	public void aboutToShow( List<MMenuElement> items, 
							 EModelService modelService, 
							 MWindow window ) {
		if (!canExecute(window))
			return;
		
		Iterator<BaseExperiment> iterator = database.getIterator(window);

		while(iterator.hasNext()) {
			Experiment exp = (Experiment) iterator.next();
			
			String path    = exp.getDefaultDirectory().getAbsolutePath();
			String label   = path;
			
			if (exp.isMergedDatabase()) {
				label = "[Merged] " + label;
			}
			MDirectMenuItem menu = modelService.createModelElement(MDirectMenuItem.class);
			
			menu.setElementId(path);
			menu.setLabel(label);
			menu.setContributionURI(ID_MENU_URI);
			
			items.add(menu);
		}		
	}
	
	
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part, 
						@Active Shell shell, 
						MWindow window,
						IEventBroker eventBroker) {

		if (database == null || database.isEmpty(window))
			return;
			
		if (part == null)
			return;

		Object obj = part.getObject();
		if (obj == null || (!(obj instanceof IBasePart)))
			return;

		IBasePart basePart = (IBasePart) obj;
		Experiment exp = (Experiment) basePart.getExperiment();
		if (exp == null) {
			Logger logger = LoggerFactory.getLogger(getClass());
			logger.debug("Database not found");
			return;
		}
		
		// duplicate metrics before sending to the dialog box.
		// we don't know if the user will confirm the modification or not, so it's better to save them first.
		// if the user decides to cancel the modification, we can restore the metrics back.
		
		List<BaseMetric> metrics = exp.getMetricList();
		List<BaseMetric> copyMetrics = new ArrayList<BaseMetric>(metrics.size());
		
		for(BaseMetric metric: metrics) {
			copyMetrics.add(metric.duplicate());
		}
		
		MetricPropertyDialog dialog = new MetricPropertyDialog(shell, (Experiment) exp);
		
		int ret = dialog.open();
		
		if (ret == Dialog.OK) {
			ViewerDataEvent data = new ViewerDataEvent(exp, metrics);
			eventBroker.post(ViewerDataEvent.TOPIC_HPC_METRIC_UPDATE, data);
			
		} else {
			// in case there is modification, we need to restore the metrics back
			for(int i=0; i<metrics.size(); i++) {
				BaseMetric metric  = metrics.get(i);
				BaseMetric oMetric = copyMetrics.get(i);
				
				metric.setDisplayName(oMetric.getDisplayName());
				
				if (metric instanceof DerivedMetric) {
					DerivedMetric dm = (DerivedMetric) metric;
					DerivedMetric om = (DerivedMetric) oMetric;
					
					dm.setAnnotationType(om.getAnnotationType());
					dm.setDisplayFormat(om.getDisplayFormat());
					dm.setExpression(om.getFormula());
				}
			}
		}
	}
	
	
	@CanExecute
	public boolean canExecute(MWindow window) {		
		return database.getNumDatabase(window)>0;
	}
		
}