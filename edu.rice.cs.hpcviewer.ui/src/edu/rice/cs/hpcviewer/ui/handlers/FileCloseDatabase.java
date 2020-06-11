package edu.rice.cs.hpcviewer.ui.handlers;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

import edu.rice.cs.hpc.data.experiment.BaseExperiment;
import edu.rice.cs.hpcviewer.ui.experiment.DatabaseCollection;
import edu.rice.cs.hpcviewer.ui.parts.IBasePart;

public class FileCloseDatabase 
{
	@Inject DatabaseCollection database;
	@Inject EPartService       partService;
	
	@Execute
	public void execute() {

		if (database == null || database.isEmpty())
			return;
		
		if (database.getNumDatabase() == 1) {
			BaseExperiment experiment = database.getLast();
			removeDatabase(experiment);
			return;
		}
		MPart part = partService.getActivePart();
		Object obj = part.getObject();
		
		if (obj instanceof IBasePart) {
			removeDatabase(((IBasePart)obj).getExperiment());
		}
	}

	private void removeDatabase(BaseExperiment experiment) {
		database.removeDatabase(experiment);
	}
}
