package edu.rice.cs.hpcviewer.ui.nattable;

import java.util.List;

import org.eclipse.nebula.widgets.nattable.data.IDataProvider;

import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;

public class ColumnDataProvider implements IDataProvider 
{
	private final static String TITLE_SCOPE_COLUMN = "Scope";
	private final List<BaseMetric> metrics;
	
	public ColumnDataProvider(List<BaseMetric> metrics) {
		this.metrics = metrics;
	}

	@Override
	public Object getDataValue(int columnIndex, int rowIndex) {

		if (columnIndex < 0 && columnIndex > getColumnCount())
			return null;
		
		if (columnIndex == 0)
			return TITLE_SCOPE_COLUMN;

		BaseMetric metric = metrics.get(columnIndex-1);
		return metric.getDisplayName();
	}

	@Override
	public void setDataValue(int columnIndex, int rowIndex, Object newValue) {}

	@Override
	public int getColumnCount() {
		return metrics.size()+1;
	}

	@Override
	public int getRowCount() {
		return 1;
	}
}
