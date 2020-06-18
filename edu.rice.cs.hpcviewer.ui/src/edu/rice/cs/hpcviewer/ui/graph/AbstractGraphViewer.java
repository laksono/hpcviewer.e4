 
package edu.rice.cs.hpcviewer.ui.graph;

import javax.inject.Inject;

import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.annotation.PostConstruct;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.swtchart.Chart;
import org.swtchart.IAxisSet;
import org.swtchart.IAxisTick;

import edu.rice.cs.hpc.data.experiment.BaseExperiment;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpcviewer.ui.parts.editor.IUpperPart;
import edu.rice.cs.hpcviewer.ui.util.ElementIdManager;

import javax.annotation.PreDestroy;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

public abstract class AbstractGraphViewer implements IUpperPart
{
	static public final int PLOT_OK          = 0;
	static public final int PLOT_ERR_IO 	 = -1;
	static public final int PLOT_ERR_UNKNOWN = -2;
	
	static public final int MAX_TITLE_CHARS = 100; // maximum charaters for a title
	
    private Chart chart;
    private GraphEditorInput input;
    private Composite parent;

	@Inject
	public AbstractGraphViewer() {		
	}
	
	@PostConstruct
	public void postConstruct(Composite parent) {
		
		this.parent = parent;
	}
	
	
	@PreDestroy
	public void preDestroy() {
	}
	
	
	@Focus
	public void onFocus() {
		chart.setFocus();
	}

	@Override
	public BaseExperiment getExperiment() {
		return input.getScope().getExperiment();
	}



	@Override
	public void setMarker(int lineNumber) {
	}

	@Override
	public void setInput(MPart part, Object obj) {

		if (obj == null) return;

		// we shouldn't create another plot if we already have plotted
		if (this.input != null) return;
		
		// Important: First thing: we need to set the value of input here 
		// subclasses may need the input value for setting the title
		
		input = (GraphEditorInput) obj;
		
		part.setTooltip(getTitle());
		
		//----------------------------------------------
		// chart creation
		//----------------------------------------------
		chart = new GraphChart(parent, SWT.NONE);

		//----------------------------------------------
		// formatting axis
		//----------------------------------------------
		IAxisSet axisSet = chart.getAxisSet();
		IAxisTick yTick = axisSet.getYAxis(0).getTick();
		yTick.setFormat(new DecimalFormat("0.0##E0##"));
		
		//----------------------------------------------
		// tidy-up the chart
		//----------------------------------------------

		chart.getLegend().setVisible(false);
		
		final String title = getTitle();
		chart.getTitle().setText(title);
		
		part.setLabel(title);
		part.setTooltip(title);
		
		//----------------------------------------------
		// main part: ask the subclass to plot the graph
		//----------------------------------------------

		plotData(input);
		
		// -----------------------------------------------------------------
		// Due to SWT Chart bug, we need to adjust the range once the create-part-control
		// 	finishes its layout.
		// -----------------------------------------------------------------

		chart.getAxisSet().adjustRange();
	}
	
	protected GraphEditorInput getInput() {
		return input;
	}
	
	
	@Override
	public String getTitle() {
		
		Scope scope = input.getScope();
		BaseMetric metric = input.getMetric();
		
		String scopeName = scope.getName();
		if (scopeName.length() > MAX_TITLE_CHARS) {
			scopeName = scope.getName().substring(0, MAX_TITLE_CHARS) + "...";
		}
		String type  = getGraphTypeLabel();
		String title = "[" + type + "] " + scopeName +": " + metric.getDisplayName();
		
		return title;
	}
	

	static public String getID(String descID, Scope scope, BaseMetric metric) {
		
		String dbId  = ElementIdManager.getElementId(scope.getExperiment());
		int scopeId  = scope.getCCTIndex();
		int metricId = metric.getIndex();
		int graphId  = descID.hashCode();
		
		return dbId 					+ ElementIdManager.ELEMENT_SEPARATOR + 
			   String.valueOf(scopeId)  + ElementIdManager.ELEMENT_SEPARATOR +
			   String.valueOf(metricId) + ElementIdManager.ELEMENT_SEPARATOR +
			   String.valueOf(graphId);
	}
	
	protected Chart getChart() {
		return chart;
	}
	
	/**
	 * method to plot a graph of a specific scope and metric of an experiment
	 * 
	 * @param scope: the scope to plot
	 * @param metric: the raw metric to plot
	 * 
	 * @return PLOT_OK if everything works fine. Negative integer otherwise 
	 */
	protected abstract int plotData(GraphEditorInput input);
	
	/****
	 * Translate a set of thread-index selections into the original set of
	 * thread-index selection.<br/>
	 * It is possible that the child class change the index of x-axis. This
	 * method will then translate from the current selected index to the original
	 * index so that it can be displayed properly by {@link ThreadView}. 
	 *  
	 * @param selections : a set of selected index (usually only one item)
	 * @return the translated set of indexes
	 */
	protected abstract ArrayList<Integer> translateUserSelection(ArrayList<Integer> selections); 


	protected abstract String getGraphTypeLabel();
}