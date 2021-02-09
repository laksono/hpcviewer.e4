package edu.rice.cs.hpcviewer.ui.actions;

import java.util.Stack;

import edu.rice.cs.hpc.data.experiment.scope.CallSiteScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpcviewer.ui.internal.ScopeTreeViewer;

public class FlatScopeAction 
{
	
	private Stack<Scope> 	   stackFlatNodes;
	private Stack<Object[]>    stackExpandedNodes;
	
	final private ScopeTreeViewer   treeViewer;
	
	//-----------------------------------------------------------------------
	// 					METHODS
	//-----------------------------------------------------------------------

	/**
	 * @param treeViewer the tree viewer to flatten/unflatten
	 */
	public FlatScopeAction(ScopeTreeViewer treeViewer) {

		this.treeViewer = treeViewer;
		stackFlatNodes  = new Stack<Scope>();
	}


	//-----------------------------------------------------------------------
	// 					FLATTEN: PUBLIC INTERFACES
	//-----------------------------------------------------------------------
	

	/**
	 * Flatten the tree one level more
	 * @return true if the action is successful, false otherwise
	 */
	public boolean flatten() {
		// save the current root scope
		Scope objParentNode = (Scope) treeViewer.getInput();
		Scope objParentChildNode = (Scope) objParentNode.getChildAt(0);
		
		// -------------------------------------------------------------------
		// copy the "root" of the current input
		// -------------------------------------------------------------------
		Scope objFlattenedNode = (objParentChildNode.duplicate());
		Scope root = objFlattenedNode.createRoot();

		objParentChildNode.copyMetrics(objFlattenedNode, 0);
		
		boolean hasKids = false;

		// create the list of flattened node
		for (int i=0;i<objParentChildNode.getChildCount();i++) {
			Scope node =  (Scope) objParentChildNode.getChildAt(i);
			if(node.getChildCount()>0) {
				
				// this node has children, add the children
				for (Object child: node.getChildren()) {
					Scope childNode = (Scope) child;
					if (!(childNode instanceof CallSiteScope)) {
						objFlattenedNode.add(childNode);
					}
				}
				//addChildren(node, objFlattenedNode);
				hasKids = true;
			} else {
				// no children: add the node itself !
				objFlattenedNode.add(node);
			}
		}
		if(hasKids) {
			if (objFlattenedNode.hasChildren()) {
				//pushElementStates();
				
				stackFlatNodes.push(objParentNode);

				treeViewer.getTree().setRedraw(false);
				// we update the data of the table
				treeViewer.setInput(root);
				// refreshing the table to take into account a new data
				treeViewer.refresh();
				
				// expand the tree until reaching level 2
				treeViewer.expandToLevel(2, true);

				treeViewer.getTree().setRedraw(true);
			}
		}
		return (hasKids && objFlattenedNode.hasChildren());
	}

	
	/**
	 * Unflatten flattened tree (tree has to be flattened before)
	 * @return true if the action is successful, false otherwise
	 */
	public boolean unflatten() {
		if (stackFlatNodes.isEmpty())
			return false;
		
		Scope objParentNode = stackFlatNodes.pop();
		if(objParentNode != null) {
			this.treeViewer.setInput(objParentNode);
			
			//popElementStates();
			
			objParentNode.setParent(null);
		}
		return true;
	}

	public boolean canUnflatten() {
		return (!stackFlatNodes.isEmpty());
	}
	

	
	//-----------------------------------------------------------------------
	// 					FLATTEN: PRIVATE METHODS
	//-----------------------------------------------------------------------

	/**
	 * store the current expanded elements into a stack
	 */
	private void pushElementStates() {
		Object []arrNodes = this.treeViewer.getExpandedElements();
		if (stackExpandedNodes == null)
			stackExpandedNodes = new java.util.Stack<Object[]>();

		stackExpandedNodes.push(arrNodes);
	}
	
	/**
	 * recover the latest expanded element into stack
	 */
	private void popElementStates() {
		Object []arrNodes = stackExpandedNodes.pop();
		this.treeViewer.setExpandedElements(arrNodes);
	}	
}
