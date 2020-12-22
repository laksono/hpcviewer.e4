 
package edu.rice.cs.hpcviewer.ui.parts.editor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import edu.rice.cs.hpc.data.experiment.BaseExperiment;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.data.experiment.source.FileSystemSourceFile;
import edu.rice.cs.hpcsetting.fonts.FontManager;
import edu.rice.cs.hpcsetting.preferences.PreferenceConstants;
import edu.rice.cs.hpcsetting.preferences.ViewerPreferenceManager;
import edu.rice.cs.hpcviewer.ui.base.IUpperPart;
import edu.rice.cs.hpcviewer.ui.graph.GraphEditorInput;
import edu.rice.cs.hpcviewer.ui.util.Utilities;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.SWT;


/**********************************************
 * 
 * Class to display the content of a file .
 * <p>
 * The main method to display the file is by using
 * {@link Editor.display()} <br/>
 * 
 * This method will try to find if the object to display
 * is already there or not. If yes, then it activates the
 * file viewer. Otherwise, it will create a new viewer.
 *
 **********************************************/
public class Editor extends CTabItem implements IUpperPart, IPropertyChangeListener
{
	
	static final private String PROPERTY_DATA = "hpceditor.data";

	
	private SourceViewer textViewer;
	private Object input;
	private int    searchOffset;
	private FindReplaceDocumentAdapter finder;
	

	public Editor(CTabFolder parent, int style) {
		super(parent, style);
		setShowClose(true);
	}
	
	
	public void setService(IEventBroker broker, MPart part) {
	}
	
	
	@PostConstruct
	public void postConstruct(Composite parent) {
		
		// add line number column to the source viewer
		CompositeRuler ruler 	   = new CompositeRuler();
		LineNumberRulerColumn lnrc = new LineNumberRulerColumn();

		Font font = FontManager.getTextEditorFont();

		lnrc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
		lnrc.setFont(font);
		ruler.addDecorator(0,lnrc);

		textViewer = new SourceViewer(parent, ruler, SWT.BORDER| SWT.MULTI | SWT.V_SCROLL);
		textViewer.setEditable(false);
		
		StyledText styledText = textViewer.getTextWidget();
		styledText.setFont(font);
		
		// make sure to set fill alignment and grab both horizontal and vertical space
		// without this, the source viewer will display only small fraction of composite
		
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		textViewer.getControl().setLayoutData(gd);
		
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(parent);
		
		ViewerPreferenceManager.INSTANCE.getPreferenceStore().addPropertyChangeListener(this);
	}
	
	
	@Override
	public void dispose() {
		ViewerPreferenceManager.INSTANCE.getPreferenceStore().removePropertyChangeListener(this);
		super.dispose();
	}
	
	public boolean hasEqualInput(Object input) {
		if (input == null) return false;
		
		File fileNew = getFileFromInput(input);
		File fileOld = getFileFromInput(textViewer.getData(PROPERTY_DATA));
		
		return fileNew == fileOld;
	}
	
	
	private File getFileFromInput(Object input) {
		if (input == null) return null;
		
		File file = null;
		
		if (input instanceof Scope) {
			file = ((Scope)input).getSourceFile().getFilename();
		} else if (input instanceof BaseExperiment) {
			file = ((BaseExperiment)input).getXMLExperimentFile();
		}
		return file;
	}
	
	
	private static String readLineByLineJava8(String filePath) 
	{
	    StringBuilder contentBuilder = new StringBuilder();
	    try (Stream<String> stream = Files.lines( Paths.get(filePath), StandardCharsets.UTF_8)) 
	    {
	        stream.forEach(s -> contentBuilder.append(s).append("\n"));
	    }
	    catch (IOException e) 
	    {
	        e.printStackTrace();
	    }
	    return contentBuilder.toString();
	}

	
	public boolean search(String text) {

		IRegion ir;
		
		try {
			if ((ir = finder.find(searchOffset, text, true, false, false, false)) != null) {
				
				setMarker(ir);
				searchOffset = ir.getOffset() + ir.getLength();
				return true;
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public void setMarker(int lineNumber) {
		
		IDocument document = textViewer.getDocument();
		
		int maxLines = document.getNumberOfLines()-1;
		lineNumber     = Math.max(0, Math.min(lineNumber, maxLines));

		try {
			int offset     = document.getLineOffset(lineNumber);
			int nextLine   = Math.min(lineNumber+1, maxLines);
			int nextOffset = document.getLineOffset(nextLine);
			int length     = Math.max(1, nextOffset - offset);
			
			document.addPosition(new Position(offset));
			
			TextSelection selection = new TextSelection(document, offset, length);
			textViewer.setSelection(selection, true);
			
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	
	public void setMarker(IRegion region) {
		IDocument document = textViewer.getDocument();
		try {
			document.addPosition(new Position(region.getOffset()));
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		TextSelection selection = new TextSelection(document, region.getOffset(), region.getLength());
		textViewer.setSelection(selection, true);
	}
	
	
	@Override
	public String getTitle() {
		return Editor.getTitle(input);
	}
	
	
	public static String getTitle(Object input) {
		String filename = null;
		
		if (input instanceof Scope) {
			filename = ((Scope)input).getSourceFile().getName(); 
		} else if (input instanceof BaseExperiment) {
			filename = ((BaseExperiment)input).getXMLExperimentFile().getName();
		} else if (input instanceof GraphEditorInput) {
			filename = ((GraphEditorInput)input).toString();
		}
		return filename;
	}


	@Override
	public void setInput(Object input) {
		
		this.input = input;
		setText(getTitle());
		
		if (input instanceof Scope) {
			Scope scope = (Scope) input;
			
			if (!Utilities.isFileReadable(scope))
				return;
			
			FileSystemSourceFile file = (FileSystemSourceFile) scope.getSourceFile();
			
			String filename = file.getCompleteFilename();
			int lineNumber  = scope.getFirstLineNumber();

			displayFile(scope, filename, lineNumber);
			
			setToolTipText(filename);
			
		} else if (input instanceof BaseExperiment) {
			
			BaseExperiment experiment = (BaseExperiment) input;
			String filename = experiment.getXMLExperimentFile().getAbsolutePath();
			
			displayFile(experiment, filename, 0);
			
			setToolTipText(filename);
		}
		// add more condition for different type of objects here
		// we should make this more flexible...
	}

	
	
	/***
	 * Display the content of a file, and highligjt a specified line number (generic version).
	 * 
	 * @param obj the object that identified this editor. It has to be either a scope or an experiment
	 * @param filename the complete path of the file name
	 * @param lineNumber the line number to be revealed
	 */
	private void displayFile(Object obj, String filename, int lineNumber) {
		IDocument document = new Document();
		
		AnnotationModel annModel = new AnnotationModel();
		annModel.connect(document);
		
		String text = readLineByLineJava8(filename);
		document.set(text);

		textViewer.setDocument(document, annModel);
		textViewer.setData(PROPERTY_DATA, obj);

		finder = new FindReplaceDocumentAdapter(document);

		setMarker(lineNumber);
	}


	@Override
	public void propertyChange(PropertyChangeEvent event) {
		final String property = event.getProperty();

		if (property.equals(PreferenceConstants.ID_FONT_TEXT)) {
			StyledText text = textViewer.getTextWidget();
			if (text == null)
				return;
			
			Font font = FontManager.getTextEditorFont();
			text.setFont(font);
			
			textViewer.refresh();
		}
	}
}