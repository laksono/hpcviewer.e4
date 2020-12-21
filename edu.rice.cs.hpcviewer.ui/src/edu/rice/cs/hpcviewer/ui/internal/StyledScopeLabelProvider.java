package edu.rice.cs.hpcviewer.ui.internal;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Link;

import edu.rice.cs.hpc.data.experiment.scope.CallSiteScope;
import edu.rice.cs.hpc.data.experiment.scope.CallSiteScopeCallerView;
import edu.rice.cs.hpc.data.experiment.scope.ProcedureScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.data.util.string.StringUtil;
import edu.rice.cs.hpcsetting.fonts.FontManager;
import edu.rice.cs.hpcsetting.preferences.ViewerPreferenceManager;
import edu.rice.cs.hpcviewer.ui.resources.ColorManager;
import edu.rice.cs.hpcviewer.ui.resources.IconManager;
import edu.rice.cs.hpcviewer.ui.util.Utilities;


/***
 * 
 * Class to display label on the tree of views
 * 
 * A node of the tree contains three objects: [icon] [callsite] node_label
 * Every object has colors to indicate if they are clickable or not
 * - An object is clickable if they contain further information such as file source code
 * - Otherwise it is not clickable
 * 
 */
public class StyledScopeLabelProvider extends DelegatingStyledCellLabelProvider 
{	
	/**
	 * Initialization of the class: preparing the colors for each object
	 * 
	 * @param window
	 */
	public StyledScopeLabelProvider(TreeViewer treeViewer) {
		super( new ScopeLabelProvider(treeViewer));
	}
	
	
	@Override
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipText(java.lang.Object)
	 */
	public String getToolTipText(Object element)
	{
		if (element instanceof Scope) 
		{	
			final int minLengthForToolTip = 50;  
			final int toolTipDesiredLineLength = 80;
			
			String scopeName = ((Scope)element).getName();
			if (scopeName.length() > minLengthForToolTip) {
				return StringUtil.wrapScopeName(scopeName, toolTipDesiredLineLength);
			}
		}
		return null; // no tool tip for this cell
	}
	

	/***
	 * 
	 * Label provider to construct image, text and color of a tree item
	 *
	 */
	private static class ScopeLabelProvider extends ColumnLabelProvider implements IStyledLabelProvider
	{
		private final TreeViewer treeViewer;
		private final Styler STYLE_ACTIVE_LINK ;
		
		public ScopeLabelProvider(TreeViewer treeViewer) {
			this.treeViewer = treeViewer;
			
			final Link link = new Link(treeViewer.getTree(), SWT.NONE);
			final Color colorLinkActive = link.getLinkForeground();
			
			STYLE_ACTIVE_LINK = new StyledString.Styler() {

				@Override
				public void applyStyles(TextStyle textStyle) {
					textStyle.foreground = colorLinkActive;
					textStyle.font 		 = FontManager.getFontGeneric();
				}				
			};
		}
		
		@Override
		public Color getBackground(Object element) {
			Scope input = (Scope) treeViewer.getInput();
			if (input != null && input.getChildAt(0) == element)
				return ColorManager.getColorTopRow();
			return null;
		}

		@Override
		public StyledString getStyledText(Object element) {
			
			StyledString styledString= new StyledString();
			
			// ----------------------------------------------
			// special case for call sites :
			// - coloring the object for call site (if exists)
			// - show the icon if exists
			// ----------------------------------------------
			if (element instanceof CallSiteScope) {
				final CallSiteScope cs = (CallSiteScope) element;
				
				// the line number in XML is 0-based, while the editor is 1-based
				int line = 1+cs.getLineScope().getFirstLineNumber();
				boolean isReadable = Utilities.isFileReadable(cs.getLineScope());
				
				// show the line number
				if (line>0) {
					if (isReadable)
						styledString.append(String.valueOf(line)+": ", Utilities.STYLE_COUNTER);
					else 
						styledString.append(String.valueOf(line)+": ", Utilities.STYLE_DECORATIONS);
				}
			}
			
			Scope node = (Scope) element;
			final String text = getText(node);

			if(Utilities.isFileReadable(node)) {
				styledString.append( text, STYLE_ACTIVE_LINK );
			} else {
				styledString.append( text, Utilities.STYLE_INACTIVE_LINK );
			}
			return styledString;
		}
		
		@Override
		public Image getImage(Object element) {
			if (element instanceof CallSiteScope) {
				Scope node = (Scope) element;
				final Image image = Utilities.getScopeNavButton(node);
				
				return image;
			} else if (element instanceof RootScope) {
				IconManager iconManager = IconManager.getInstance();
				return iconManager.getImage(IconManager.Image_MetricAggregate);
			}
			return null;
		}
		
		/**
		 * Return the text of the scope tree. By default is the scope name.
		 */
		private String getText(Scope node) 
		{
			String text = "";
			ViewerPreferenceManager pref = ViewerPreferenceManager.INSTANCE;
			
			if (pref.getDebugCCT())  
			{
				//---------------------------------------------------------------
				// label for debugging purpose
				//---------------------------------------------------------------
				if (node instanceof CallSiteScope)
				{
					CallSiteScope caller = (CallSiteScope) node;
					Scope cct = caller.getLineScope();
					if (node instanceof CallSiteScopeCallerView) 
					{
						int numMerged = ((CallSiteScopeCallerView)caller).getNumMergedScopes();
						if (numMerged > 0) {
							int mult = numMerged + 1;
							text = mult + "*";
						}
						cct = ((CallSiteScopeCallerView)caller).getScopeCCT();
					}	
					text += " [c:" + caller.getCCTIndex() +"/" + cct.getCCTIndex()  + "] " ;
				} else
					text = "[c:" + node.getCCTIndex() + "] ";
			} 
			if (pref.getDebugFlat()) {
				text += "[f: " + node.getFlatIndex() ;
				if (node instanceof CallSiteScope) {
					ProcedureScope proc = ((CallSiteScope)node).getProcedureScope();
					text += "/" + proc.getFlatIndex();
				}
				text += "] ";
			} 
			
			text += node.getName();
			// add the name of the load module if the node is not readable
			// see issue #79: https://github.com/HPCToolkit/hpcviewer/issues/79

			if (needToAddLoadModuleSuffix(node)) {
				String lm = null;
				
				ProcedureScope proc = null;
				
				if (node instanceof CallSiteScope) {
					proc = ((CallSiteScope)node).getProcedureScope();
				} else {
					proc = (ProcedureScope) node;
				}
				if (!proc.isFalseProcedure()) {
					lm = proc.getLoadModule().getModuleName();
					int lastDot = 1+lm.lastIndexOf('/');
					
					text += " [" + lm.substring(lastDot) + "]";
				}
			}
			return text;
		}
	}
	
	static private boolean needToAddLoadModuleSuffix(Scope node) {
		if (node instanceof CallSiteScope ||
				(node instanceof ProcedureScope && ((ProcedureScope)node).isAlien())) {

			// we only add the load module suffix if:
			// - the file is NOT readable; and
			// - doesn't have load module suffix

			return (!Utilities.isFileReadable(node) && node.getName().lastIndexOf(']')<0);

		}
		return false;
	}
}
