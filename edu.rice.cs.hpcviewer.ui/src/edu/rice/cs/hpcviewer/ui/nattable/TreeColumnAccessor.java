package edu.rice.cs.hpcviewer.ui.nattable;

import java.util.List;

import org.eclipse.nebula.widgets.nattable.data.IColumnAccessor;

import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.scope.Scope;

public class TreeColumnAccessor implements IColumnAccessor<Scope> 
{
	private final List<BaseMetric> metrics;
	
	public TreeColumnAccessor(List<BaseMetric> metrics) {
		this.metrics = metrics;
	}

	@Override
	public Object getDataValue(Scope rowObject, int columnIndex) {
		switch(columnIndex) {
		case 0:
			return rowObject.getName();
		default:
			if (columnIndex>0 && columnIndex <= metrics.size()) {
				BaseMetric metric = metrics.get(columnIndex-1);
				return metric.getMetricTextValue(rowObject);
			}
		}
		return null;
	}

	@Override
	public void setDataValue(Scope rowObject, int columnIndex, Object newValue) {}

	@Override
	public int getColumnCount() {
		return 1 + metrics.size();
	}
	
}
