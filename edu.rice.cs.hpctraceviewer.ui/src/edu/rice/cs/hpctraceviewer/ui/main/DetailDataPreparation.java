package edu.rice.cs.hpctraceviewer.ui.main;

import org.eclipse.swt.graphics.Color;

import edu.rice.cs.hpctraceviewer.data.DataLinePainting;
import edu.rice.cs.hpctraceviewer.data.DataPreparation;
import edu.rice.cs.hpctraceviewer.data.TimelineDataSet;

public class DetailDataPreparation extends DataPreparation {

	private TimelineDataSet dataset;
	
	/*****
	 * Constructor for preparing data to paint on the space-time canvas
	 * 
	 * @param data a DataLinePainting object
	 */
	public DetailDataPreparation(DataLinePainting data) 
	{
		super(data);
		dataset = new TimelineDataSet( data.ptl.line(), data.ptl.size(), data.height);
	}

	@Override
	public void finishLine(String proc, int currSampleMidpoint, int succSampleMidpoint,
			int currDepth, Color color, int sampleCount) {

		final DetailDataVisualization data = new DetailDataVisualization(currSampleMidpoint, 
				succSampleMidpoint, currDepth, color, sampleCount);
		
		dataset.add(data);
	}

	/*****
	 * retrieve the list of data to paint
	 * @return
	 */
	public TimelineDataSet getList() {
		
		return dataset;
	}
}
