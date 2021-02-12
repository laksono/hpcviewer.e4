package edu.rice.cs.hpcviewer.ui.nattable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ca.odell.glazedlists.TreeList;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScopeType;
import edu.rice.cs.hpc.data.experiment.scope.Scope;


/******
 * 
 * Data provider for table columns, which consists of:
 * 0: tree column
 * 1-N: metric columns, where N is number of metrics.
 *
 */
public class TreeScopeFormat implements TreeList.Format<Scope>
{
	private Comparator<Scope> comparator;

	public TreeScopeFormat(RootScope root) {
	}

	@Override
	public void getPath(List<Scope> path, Scope element) {
		if (element == null)
			return;

		path.add(element);

		Scope parent = element.getParentScope();

		if (parent instanceof RootScope) {
			if ( ((RootScope)parent).getType() == RootScopeType.Invisible )
				return;
		}
		while(parent != null) {
			path.add(parent);
			parent = parent.getParentScope();
			if (parent instanceof RootScope) {
				if ( ((RootScope)parent).getType() == RootScopeType.Invisible )
					break;
			}
		}

		Collections.reverse(path);

	}

	@Override
	public boolean allowsChildren(Scope element) {
		return true;
	}

	@Override
	public Comparator<? super Scope> getComparator(int depth) {
		if (comparator == null) {
			comparator = new Comparator<Scope>() {

				@Override
				public int compare(Scope o1, Scope o2) {
					
					return o1.getName().compareTo(o2.getName());
				}
			};
		}
		return null;
	}		
}
