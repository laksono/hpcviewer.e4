 
package edu.rice.cs.hpcviewer.ui.parts.editor;

import javax.inject.Inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.swt.widgets.Composite;

import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.data.experiment.source.FileSystemSourceFile;
import edu.rice.cs.hpcviewer.ui.util.Utilities;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.SWT;



public class Editor implements ICodeEditor
{
	static final public String ID = "edu.rice.cs.hpcviewer.ui.part.editor";
	
	private SourceViewer textViewer;
	
	@Inject IEventBroker broker;
	@Inject MPart part;

	@Inject
	public Editor() {
		
	}
	
	@PostConstruct
	public void postConstruct(Composite parent) {

		textViewer = new SourceViewer(parent, null, SWT.BORDER| SWT.MULTI | SWT.V_SCROLL);
		textViewer.setEditable(false);
		
		CompositeRuler ruler 	   = new CompositeRuler();
		LineNumberRulerColumn lnrc = new LineNumberRulerColumn();
		ruler.addDecorator(0,lnrc);
		
		StyledText styledText = textViewer.getTextWidget();		
		styledText.setFont(JFaceResources.getTextFont());
		
		GridDataFactory.fillDefaults().grab(true, true).applyTo(styledText);
		
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(parent);
		
		Object obj = part.getObject();
		if (obj != null)
			setData(obj);
	}
	
	@PreDestroy
	public void preDestroy() {
	}

	@Override
	public void setData(Object obj) {
		if (obj != null && obj instanceof Scope) {
			Scope scope = (Scope) obj;

			if (!Utilities.isFileReadable(scope))
				return;
			
			FileSystemSourceFile file = (FileSystemSourceFile) scope.getSourceFile();
			
			String filename = file.getCompleteFilename();
			int lineNumber  = scope.getFirstLineNumber();

			IDocument document = new Document();
			
			String text = readLineByLineJava8(filename);
			
			document.set(text);
			
			textViewer.setDocument(document);
			textViewer.getControl().setFocus();
			try {
				int offset = document.getLineOffset(lineNumber);
				textViewer.setMark(offset);
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
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

	@Override
	public void setTitle(String title) {}

	@Override
	public void setMarker(int lineNumber) {}
}