 
package edu.rice.cs.hpctraceviewer.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import edu.rice.cs.hpctraceviewer.ui.TracePart;
import edu.rice.cs.hpctraceviewer.ui.base.ITraceViewAction;

import org.eclipse.e4.core.di.annotations.CanExecute;

public class TimeZoomOut 
{
	@Execute
	public void execute(MPart part) {
		TracePart traceView   = (TracePart) part.getObject();
		ITraceViewAction actions = traceView.getActions();
		actions.timeZoomOut();
	}
	
	
	@CanExecute
	public boolean canExecute(MPart part) {
		if (part == null) {
			return false;
		}
		TracePart traceView   = (TracePart) part.getObject();
		ITraceViewAction actions = traceView.getActions();
		return (actions != null ? actions.canTimeZoomOut() : false);
	}
		
}