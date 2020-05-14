package edu.rice.cs.hpcviewer.ui.resources;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class ColorManager 
{
	static final private String COLOR_TOP = "hpcviewer.COLOR_TOP";
	
	static public Color getColorTopRow() {
		ColorRegistry registry = JFaceResources.getColorRegistry();
		if (registry == null)
			return null;
		
		Display display = Display.getDefault();
		
		Color clrDesc = registry.get(COLOR_TOP);
		if (clrDesc != null) {
			return clrDesc;
		}
		
		Color topColor = new Color(display, 255, 255, 204);
		registry.put(COLOR_TOP, topColor.getRGB());
		
		return topColor;
	}
}
