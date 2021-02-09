package edu.rice.cs.hpc.data.experiment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.DerivedMetric;
import edu.rice.cs.hpc.data.experiment.metric.IMetricManager;
import edu.rice.cs.hpc.data.experiment.metric.MetricComparator;
import edu.rice.cs.hpc.data.experiment.metric.MetricType;
import edu.rice.cs.hpc.data.util.Constants;


/****************************************************************************
 * 
 * abstract base experiment that contains metrics. <br/>
 * This class just load metrics without generating callers view and flat view
 *
 ****************************************************************************/
public abstract class BaseExperimentWithMetrics extends BaseExperiment 
implements IMetricManager
{
	
	/** A list of metric descriptor   
	 ** the list is sorted based on the metric ID */
	protected List<BaseMetric> metrics;
	
	/** A list of metric descriptors sorted based on the hpcrun order
	 ** 
	 */
	protected ArrayList<BaseMetric> metricsWithOrder;
	
	/**
	 * map ID to the metric descriptor
	 */
	private Map<Integer, BaseMetric> mapIndexToMetric;
	
	private Map<String, BaseMetric> mapIdToMetric;
	
	//////////////////////////////////////////////////////////////////////////
	//ACCESS TO METRICS													    //
	//////////////////////////////////////////////////////////////////////////

	/*************************************************************************
	 *	Returns the array of metrics in the experiment.
	 ************************************************************************/

	/*****
	 * Set the list of metric descriptors. Ideally this method has to be called
	 * at most once for every creation of experiment
	 * 
	 * @param metricList : a list of metric descriptors
	 */
	public void setMetrics(List<BaseMetric> metricList) {
		metrics = metricList;
		mapIndexToMetric = new HashMap<Integer, BaseMetric>(metricList.size());
		mapIdToMetric    = new HashMap<>(metricList.size());
		metricsWithOrder = new ArrayList<BaseMetric>();
		
		for(BaseMetric metric:metrics) {
			if (metric.getOrder() >= 0) {
				metricsWithOrder.add(metric);
			}
			mapIndexToMetric.put(metric.getIndex(), metric);
			mapIdToMetric.put(metric.getShortName(), metric);
		}
		
		if (getMajorVersion() >= Constants.EXPERIMENT_SPARSE_VERSION) {
			// reorder the metric since hpcprof2 will output not in order fashion
			Collections.sort(metrics, new MetricComparator());
		}
	}


	/***
	 * Retrieve the list of metrics
	 * @return {@code List<BaseMetric>}
	 */
	public List<BaseMetric> getMetricList() 
	{
		return metrics;
	}
	
	/*****
	 * Return the list of "visible" metrics. <br/>
	 * Note: visible doesn't mean "SHOW". 
	 * Visible here means that the metrics can be displayed on metric property window
	 * or on list of metrics. 
	 * Perhaps we should change the term to be "displayable" metrics.
	 */
	public List<BaseMetric> getVisibleMetrics() 
	{
		ArrayList<BaseMetric> listMetrics = new ArrayList<>(metrics.size());
		
		for(BaseMetric metric : metrics) {
			switch (metric.getVisibility()) {
			
			case SHOW:
			case HIDE:
				listMetrics.add(metric);
				break;
				
			case SHOW_INCLUSIVE:
				if (metric.getMetricType() == MetricType.INCLUSIVE)
					listMetrics.add(metric);
				break;
				
			case SHOW_EXCLUSIVE:
				if (metric.getMetricType() == MetricType.EXCLUSIVE)
					listMetrics.add(metric);
				break;
			
			default:
				break;
			}
		}
		return listMetrics;
	}

	
	/*************************************************************************
	 *	Returns the number of metrics in the experiment.
	 ************************************************************************/
	public int getMetricCount()
	{
		return this.metrics.size();
	}

	/*************************************************************************
	 * Returns the metric with a given index.
	 * @param index the metric id (not the metric order)
	 ************************************************************************/
	public BaseMetric getMetric(int index)
	{		
		BaseMetric metric = mapIndexToMetric.get(index);
		if (metric != null)
			return metric;

		// not found
		throw new RuntimeException("Invalid metric index: " + index);
	}
	
	/*************************************************************************
	 *	Returns the metric with a given internal name.
	 *   
	 *  @param shortName the short name (ID) of the metric 
	 ************************************************************************/
	public BaseMetric getMetric(String shortName)
	{
		return mapIdToMetric.get(shortName);	
	}
	
	@Override
	public BaseMetric getMetricFromOrder(int order)
	{
		if (order >=0 && metricsWithOrder != null && order < metricsWithOrder.size()) {
			return metricsWithOrder.get(order);
		}
		return null;
	}
	
	//////////////////////////////////////////////////////////////////////////
	//Compute Derived Metrics												//
	//////////////////////////////////////////////////////////////////////////

	/**
	 * Create a derived metric based on formula expression
	 * @param objMetric : a new derived metric
	 * @return 
	 */
	@Override
	public void addDerivedMetric(DerivedMetric objMetric) {

		metrics.add(objMetric);
		mapIndexToMetric.put(objMetric.getIndex(), objMetric);
	}



	//////////////////////////////////////////////////////////////////////////
	//ACCESS TO SEARCH PATH												    //
	//////////////////////////////////////////////////////////////////////////

	/*************************************************************************
	 *	Returns the number of search paths in the experiment.
	 ************************************************************************/
	public int getSearchPathCount()
	{
		return this.configuration.getSearchPathCount();
	}

	/*************************************************************************
	 *	Returns the search path with a given index.
	 ************************************************************************/

	public File getSearchPath(int index)
	{
		return this.configuration.getSearchPath(index);
	}
}
