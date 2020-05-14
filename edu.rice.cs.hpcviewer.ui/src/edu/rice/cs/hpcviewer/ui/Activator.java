package edu.rice.cs.hpcviewer.ui;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import edu.rice.cs.hpcviewer.ui.resources.Icons;


/**
 * The activator class controls the plug-in life cycle
 */
public class Activator implements BundleActivator {

	// The plug-in ID
	public static final String PLUGIN_ID = "edu.rice.cs.hpcviewer.ui"; //$NON-NLS-1$


	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		Icons.getInstance().init();
	}

	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}

}
