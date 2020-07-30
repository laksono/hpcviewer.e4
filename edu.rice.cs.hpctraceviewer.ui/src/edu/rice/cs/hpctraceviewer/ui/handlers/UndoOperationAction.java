package edu.rice.cs.hpctraceviewer.ui.handlers;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;

import edu.rice.cs.hpctraceviewer.ui.operation.TraceOperation;

/***********************************************************************
 * 
 * Class to manage undo operations such as region change, depth change
 * 	or processes pattern change
 * 
 * $Id: UndoOperationAction.java 1556 2013-01-02 21:11:12Z laksono $
 *
 ***********************************************************************/
public class UndoOperationAction
{
	protected IUndoableOperation[] getHistory() {
		return TraceOperation.getUndoHistory();
	}

	@Execute
	protected void execute() {
		IUndoableOperation[] operations = getHistory();
		final int len = operations.length;
		if (len<1)
			return;
		
		IUndoableOperation[] redos = TraceOperation.getRedoHistory();
		
		if (redos.length == 0) {
			// hack: when there's no redo, we need to remove the current
			// history into the redo's stack. To do this properly, we
			// should perform an extra undo before the real undo

			//doUndo();
			if (len-2>=0) {
				execute(operations[len-2]);
				return;
			}
		}
		doUndo();
	}
	
	
	@CanExecute
	protected boolean canExecute() {
		final IUndoableOperation []ops = getHistory(); 
		boolean status = (ops != null) && (ops.length>0);
		System.out.println("can undo: " + status +" , "  + ops);
		return status;
	}

	/***
	 * helper method to perform the default undo
	 */
	private void doUndo() {
		try {
			IStatus status = TraceOperation.getOperationHistory().
					undo(TraceOperation.undoableContext, null, null);
			if (!status.isOK()) {
				System.err.println("Cannot undo: " + status.getMessage());
			}
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
	

	protected void execute(IUndoableOperation operation) {
		try {
			TraceOperation.getOperationHistory().undoOperation(operation, null, null);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
}