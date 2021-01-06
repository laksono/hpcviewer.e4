package edu.rice.cs.hpcviewer.ui.resources;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Image;
import edu.rice.cs.hpcbase.ui.BaseIconManager;

/**
 * Singleton class containing global variables for icons 
 * @author laksono
 *
 */
public class IconManager  extends BaseIconManager
{
	final static public String Image_CallFrom = "CallFrom.gif";
	final static public String Image_CallTo = "CallTo.gif";
	final static public String Image_CallFromDisabled = "CallFromDisabled.gif";
	final static public String Image_CallToDisabled = "CallToDisabled.gif";
	
	final static public String Image_InlineFrom = "CallFromInline.gif";
	final static public String Image_InlineTo = "CallToInline.gif";
	final static public String Image_InlineFromDisabled = "CallFromInlineDisabled.gif";
	final static public String Image_InlineToDisabled = "CallToInlineDisabled.gif";
	
	final static public String Image_MetricAggregate  = "MetricAggregate.png";
	
	final static public String Image_ZoomIn = "ZoomIn.gif";
	final static public String Image_ZoomOut = "ZoomOut.gif";
	final static public String Image_Flatten = "Flatten.png";
	final static public String Image_Unflatten = "Unflatten.png";
	
	final static public String Image_CheckColumns = "checkColumns.gif";
	final static public String Image_FlameIcon = "flameIcon.gif";
	
	final static public String Image_FnMetric = "FnMetric.png";
	final static public String Image_FontBigger = "FontBigger.png";
	final static public String Image_FontSmaller = "FontSmaller.png";
	final static public String Image_SaveCSV = "savecsv.png";
	final static public String Image_Graph = "Graph.png";
	final static public String Image_ThreadView = "cct-thread.png";
	final static public String Image_ThreadMap = "cct-thread-map.png";
	
	final static public String Image_Viewer_256 = "platform:/plugin/edu.rice.cs.hpcviewer.ui/resources/hpcviewerEclipse256.gif";
	final static public String Image_Viewer_128 = "platform:/plugin/edu.rice.cs.hpcviewer.ui/resources/hpcviewerEclipse128.gif";
	final static public String Image_Viewer_64  = "platform:/plugin/edu.rice.cs.hpcviewer.ui/resources/hpcviewerEclipse64.gif";
	final static public String Image_Viewer_48  = "platform:/plugin/edu.rice.cs.hpcviewer.ui/resources/hpcviewerEclipse48.gif";
	final static public String Image_Viewer_32  = "platform:/plugin/edu.rice.cs.hpcviewer.ui/resources/hpcviewerEclipse32.gif";
	final static public String Image_Viewer_16  = "platform:/plugin/edu.rice.cs.hpcviewer.ui/resources/hpcviewerEclipse16.gif";
	
	final static public String []Image_Viewer = {
			Image_Viewer_16,  Image_Viewer_32,
			Image_Viewer_48,  Image_Viewer_64,
			Image_Viewer_128, Image_Viewer_256
	};
	
	static private IconManager __singleton=null;
	static private final AtomicBoolean isInitialized = new AtomicBoolean(false);
	
	/********
	 * get the instance of the current icons class
	 * We just want to make sure only ONE initialization for 
	 * image creation and registration for each class.
	 * 
	 * Although initialization for each object doesn't harm, but
	 * it's useless and time consuming 
	 * 
	 * @return
	 ********/
	static public IconManager getInstance() {
		if (IconManager.__singleton == null) {
			IconManager.__singleton = new IconManager();
		}
		return IconManager.__singleton;
	}	
	
	/*************
	 * initialize images. The method only needs to be called once for the whole
	 * window life span. Although calling this multiple times is theoretically
	 * harmless (never tried).
	 * 
	 * @param registry
	 *************/
	public void init() {
		
		if (isInitialized.compareAndSet(false,true)) {
			ImageRegistry registry = JFaceResources.getImageRegistry();
			
			registerImage(registry, getClass(), Image_CallFrom); 
			registerImage(registry, getClass(), Image_CallTo); 
			registerImage(registry, getClass(), Image_CallFromDisabled); 
			registerImage(registry, getClass(), Image_CallToDisabled);

			registerImage(registry, getClass(), Image_InlineFrom);
			registerImage(registry, getClass(), Image_InlineTo); 
			registerImage(registry, getClass(), Image_InlineFromDisabled); 
			registerImage(registry, getClass(), Image_InlineToDisabled);

			registerImage(registry, getClass(), Image_MetricAggregate);

			registerImage(registry, getClass(), Image_ZoomIn); 
			registerImage(registry, getClass(), Image_ZoomOut); 
			registerImage(registry, getClass(), Image_Flatten); 
			registerImage(registry, getClass(), Image_Unflatten);

			registerImage(registry, getClass(), Image_CheckColumns);
			registerImage(registry, getClass(), Image_FlameIcon);

			registerImage(registry, getClass(), Image_FnMetric); 
			registerImage(registry, getClass(), Image_FontBigger); 
			registerImage(registry, getClass(), Image_FontSmaller); 
			registerImage(registry, getClass(), Image_SaveCSV);
			registerImage(registry, getClass(), Image_Graph); 
			registerImage(registry, getClass(), Image_ThreadView); 
			registerImage(registry, getClass(), Image_ThreadMap);
		}
	}
	
	
	
	public Image getImage(final String desc) { 
		ImageRegistry registry = JFaceResources.getImageRegistry();

		return registry.get(desc); 
	}
	 
}
