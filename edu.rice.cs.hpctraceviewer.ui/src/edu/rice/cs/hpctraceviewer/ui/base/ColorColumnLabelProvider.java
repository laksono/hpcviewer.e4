package edu.rice.cs.hpctraceviewer.ui.base;

import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;

import edu.rice.cs.hpctraceviewer.ui.util.IConstants;

/********************************
 * 
 * Class to handle column which display a color
 *
 ********************************/
public abstract class ColorColumnLabelProvider extends OwnerDrawLabelProvider 
{
	@Override
	protected void measure(Event event, Object element) {}

	@Override
	protected void paint(Event event, Object element) {
		switch(event.index) {
		case 0:
			Color color = getColor(event, element);				
			event.gc.setBackground(color);
			
			Rectangle bound = event.getBounds();
			bound.width = IConstants.COLUMN_COLOR_WIDTH_PIXELS;
			
			event.gc.fillRectangle(bound);
			break;
		default:
			break;
		}
	}

	/**
	 * Retrieve the color for a given element.
	 * The return color cannot be null.
	 * 
	 * @param event
	 * @param element
	 * @return Color (cannot be null)
	 */
	abstract protected Color getColor(Event event, Object element);
}
