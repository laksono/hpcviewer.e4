 
package edu.rice.cs.hpctraceviewer.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import edu.rice.cs.hpctraceviewer.ui.TracePart;
import edu.rice.cs.hpctraceviewer.ui.base.ITraceViewAction;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;

public class GoScroll 
{
	final String PARAM_ID = "edu.rice.cs.hpctraceviewer.ui.commandparameter.go";
	final String PARAM_VALUE_LEFT  = "left";
	final String PARAM_VALUE_RIGHT = "right";
	final String PARAM_VALUE_UP    = "up";
	final String PARAM_VALUE_DOWN  = "down";

	@Execute
	public void execute(MPart part, @Optional @Named(PARAM_ID) String param) {
		
		TracePart traceView = (TracePart) part.getObject();
		ITraceViewAction actions = traceView.getActions();
		
		if (param.equals(PARAM_VALUE_LEFT)) {
			actions.goLeft();
			
		} else if (param.equals(PARAM_VALUE_RIGHT)) {
			actions.goRight();
			
		} else if (param.equals(PARAM_VALUE_UP)) {
			actions.goUp();
			
		} else if (param.equals(PARAM_VALUE_DOWN)) {
			actions.goDown();
		}
	}
	
	
	@CanExecute
	public boolean canExecute(MPart part, @Optional @Named(PARAM_ID) String param) {
		if (part == null)
			return false;
		
		TracePart traceView = (TracePart) part.getObject();
		ITraceViewAction actions = traceView.getActions();
		if (actions == null)
			return false;
		
		if (param.equals(PARAM_VALUE_LEFT)) {
			return actions.canGoLeft();
			
		} else if (param.equals(PARAM_VALUE_RIGHT)) {
			return actions.canGoRight();
			
		} else if (param.equals(PARAM_VALUE_UP)) {
			return actions.canGoUp();
			
		} else if (param.equals(PARAM_VALUE_DOWN)) {
			return actions.canGoDown();
		}
		return true;
	}		
}