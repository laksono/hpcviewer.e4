 
package edu.rice.cs.hpctraceviewer.ui.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

import edu.rice.cs.hpctraceviewer.ui.TracePart;
import edu.rice.cs.hpctraceviewer.ui.base.ITraceViewAction;

public class Home 
{
	@Execute
	public void execute(MWindow window, EPartService partService, EModelService modelService, MPart part) {

		if (part == null) {
			return;
		}
		TracePart traceView   = (TracePart) part.getObject();
		ITraceViewAction actions = traceView.getActions();
		if (actions != null)
			actions.home();
	}	

	@CanExecute
	public boolean canExecute() {
		
		return true;
	}
		
}