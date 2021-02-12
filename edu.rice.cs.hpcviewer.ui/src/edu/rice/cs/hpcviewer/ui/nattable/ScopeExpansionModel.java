package edu.rice.cs.hpcviewer.ui.nattable;

import java.util.List;

import ca.odell.glazedlists.TreeList;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;

public class ScopeExpansionModel implements TreeList.ExpansionModel<Scope>
{

	@Override
	public boolean isExpanded(Scope element, List<Scope> path) {
		return (element instanceof RootScope);
	}

	@Override
	public void setExpanded(Scope element, List<Scope> path, boolean expanded) {}
}
