package edu.rice.cs.hpcviewer.ui.nattable;

import org.eclipse.nebula.widgets.nattable.data.IDataProvider;

import edu.rice.cs.hpc.data.experiment.Experiment;

public class TreeRowHeaderDataProvider implements IDataProvider 
{
	private final Experiment experiment;
	
	public TreeRowHeaderDataProvider(Experiment experiment) {
		this.experiment = experiment;
	}
	@Override
	public Object getDataValue(int columnIndex, int rowIndex) {
		return Integer.valueOf(rowIndex+1);
	}

	@Override
	public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
	}

	@Override
	public int getColumnCount() {
		return 0;
	}

	@Override
	public int getRowCount() {
		return experiment.getListOfScopes().size();
	}
}
