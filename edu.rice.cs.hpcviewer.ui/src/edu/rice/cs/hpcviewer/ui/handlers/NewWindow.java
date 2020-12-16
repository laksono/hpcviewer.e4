 
package edu.rice.cs.hpcviewer.ui.handlers;

import javax.inject.Inject;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

import edu.rice.cs.hpcviewer.ui.addon.DatabaseCollection;
import edu.rice.cs.hpcviewer.ui.util.IConstants;

public class NewWindow 
{
	
	@Inject DatabaseCollection database;
	
	@Execute
	public void createWindow(EModelService modelService, MApplication app) {
		  MTrimmedWindow newWin = (MTrimmedWindow)modelService.cloneSnippet(app, IConstants.ID_SNIPPET_WINDOW, null);

		  app.getChildren().add(newWin);
	}
}