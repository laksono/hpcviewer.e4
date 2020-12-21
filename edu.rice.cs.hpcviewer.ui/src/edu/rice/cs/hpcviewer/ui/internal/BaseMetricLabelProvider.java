package edu.rice.cs.hpcviewer.ui.internal;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpcsetting.fonts.FontManager;
import edu.rice.cs.hpcviewer.ui.base.IMetricLabelProvider;
import edu.rice.cs.hpcviewer.ui.resources.ColorManager;


/****
 * 
 * Class to provide basic label provider for metric columns
 *
 */
public class BaseMetricLabelProvider extends ColumnLabelProvider implements IMetricLabelProvider 
{
	private final TreeViewer treeViewer;
	protected Scope scope = null;
	protected BaseMetric metric = null;


	public BaseMetricLabelProvider(TreeViewer treeViewer, BaseMetric metricNew) {
		this.treeViewer = treeViewer;
		this.metric = metricNew;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.viewer.metric.IMetricLabelProvider#isEnabled()
	 */
	public boolean isEnabled() {
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.viewer.metric.IMetricLabelProvider#setScope(java.lang.Object)
	 */
	public void setScope(Object scope) {
		if (scope instanceof Scope) {
			this.scope = (Scope)scope;
		}
		return;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.viewer.metric.IMetricLabelProvider#setMetric(java.lang.Object)
	 */
	public void setMetric(Object metric) {
		if (metric instanceof BaseMetric) {
			this.metric = (BaseMetric)metric;
		}
		return;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ColumnLabelProvider#getFont(java.lang.Object)
	 */
	public Font getFont(Object element) {
		return FontManager.getMetricFont();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ColumnLabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		String text = null; 

		if ((metric != null) && (element instanceof Scope)) {
			Scope node = (Scope) element;
			text = metric.getMetricTextValue(node);
		}
		return text;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ColumnLabelProvider#getBackground(java.lang.Object)
	 */
	public Color getBackground(final Object element) {
		if (treeViewer != null) {
			Object input = treeViewer.getInput();
			if (input != null && input instanceof Scope) {
				if (((Scope)input).getChildAt(0) == element) {
					return ColorManager.getColorTopRow(treeViewer.getControl());
				}
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ColumnLabelProvider#getForeground(java.lang.Object)
	 */
	public Color getForeground(final Object element) {
		return null;
	}
	
	protected TreeViewer getViewer() {
		return treeViewer;
	}
}
