package edu.rice.cs.hpctraceviewer.data.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class Constants {
	
	public final static Color COLOR_WHITE = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);
    public final static Color COLOR_BLACK = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
	
	/**The min number of time units you can zoom in*/
	public final static int MIN_TIME_UNITS_DISP = 1;
	
	public static final int dataIdxNULL = -1;
	public static final int dataIdxNoData = 0; // corresponds to "no data"
	public static final String dataStrNULL = null;
	
	public static final int DONE = 0x444F4E45;
	public static final int OPEN = 0x4F50454E;
	public static final int INFO = 0x494E464F;
	public static final int XML_HEADER = 0x45584D4C;
	public static final int DATABASE_NOT_FOUND = 0x4E4F4442;
	public static final int DB_OK = 0x44424F4B;

	public static final String CONTEXT_TIMELINE = "hpctraceviewer.timeline";
	
	
	public static final String  PROC_NO_ACTIVITY   = "<no activity>";
	public static final String  PROC_NO_THREAD     = "<no thread>";

	/**A null function*/
	public static final String NULL_FUNCTION = "-Outside Timeline-";
}
