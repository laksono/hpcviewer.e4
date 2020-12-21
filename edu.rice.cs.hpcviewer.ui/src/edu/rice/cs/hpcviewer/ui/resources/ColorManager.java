package edu.rice.cs.hpcviewer.ui.resources;

import javax.inject.Singleton;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;


/********************************************************
 * 
 * Add-on class to manage colors used in the application
 *
 ********************************************************/
@Creatable
@Singleton
public class ColorManager 
{
	static final private String COLOR_TOP = "hpcviewer.COLOR_TOP";
	static final private int CONTRAST_COLOR = 40;
	
	static public Color getColorTopRow(Control parent) {
		ColorRegistry registry = JFaceResources.getColorRegistry();
		if (registry == null)
			return null;
		
		Color clrDesc = registry.get(COLOR_TOP);
		if (clrDesc != null) {
			return clrDesc;
		}
		Color background = parent.getBackground();
		RGB rgbBackground = background.getRGB();
		RGB rgb = new RGB(rgbBackground.red, 
						  rgbBackground.green, 
						  Math.abs(rgbBackground.blue  - CONTRAST_COLOR));
		Color topColor = new Color(rgb);
		registry.put(COLOR_TOP, rgb);
		
		return topColor;
	}
}
