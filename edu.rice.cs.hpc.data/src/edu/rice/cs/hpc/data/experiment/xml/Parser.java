//////////////////////////////////////////////////////////////////////////
//									//
//	Parser.java							//
//									//
//	experiment.xml.Parser -- fast callback-based XML parser		//
//	Last edited: January 16, 2002 at 11:27 am			//
//									//
//	(c) Copyright 2002 Rice University. All rights reserved.	//
//									//
//////////////////////////////////////////////////////////////////////////




package edu.rice.cs.hpc.data.experiment.xml;

import java.io.*;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;




//////////////////////////////////////////////////////////////////////////
//	CLASS PARSER							//
//////////////////////////////////////////////////////////////////////////

 /**
 *
 *	A fast callback-based (SAX2) XML parser.
 *
 *	This parser is fast in part because it is lightweight. It does not
 *	validate the input file, and therefore it also need not read a DTD
 *	for the file. It has no knowledge of the semantics of what it parses
 *	and therefore does no semantic error checking or representation building.
 *	<p>
 *	Semantic checking and building are performed by a separate object of
 *	class <code>Builder</code>, which procedurally encodes knowledge of the
 *	document type being parsed, the document's semantic constraints, and
 *	the representation to be built for a document. hpcviewer uses a subclass
 *	of <code>Builder</code> to encode the experiment file format.
 *	<p>
 *	Note that we and SAX both define a class named <code>Parser</code>.
 *	Here the SAX class is referred to as <code>org.xml.sax.Parser</code>
 *
 *	@see edu.rice.cs.hpc.data.experiment.xml.Builder#Builder
 *	@see edu.rice.cs.hpc.data.experiment.xml.Builder#ExperimentBuilder
 *
 */


public class Parser extends Object
{


/** The name of the input to parse. */
protected String inputName;

/** A stream giving access to the input to parse. */
protected InputStream inputStream;

/** A reader which keeps track of the input line number. */
protected LineNumberReader lineNumberReader;

/** The builder to use while parsing. */
protected Builder builder;

/** The last reported parse position in the input file. */
protected Locator locator;




/** The SAX parser package to use. */
final String parserClass = "com.jclark.xml.sax.Driver";

final String xmlReaderClass = "org.apache.xerces.parsers.SAXParser";





//////////////////////////////////////////////////////////////////////////
//	INITIALIZATION														//
//////////////////////////////////////////////////////////////////////////




/*************************************************************************
 *	Creates a Parser.
 ************************************************************************/
	
public Parser(String inputName, InputStream inputStream, Builder builder)
{
	// creation arguments
	this.inputName = inputName;
	this.inputStream = inputStream;
	this.builder = builder;
	this.builder.setParser(this);

	// input line number, for error messages
	this.lineNumberReader = new LineNumberReader(new InputStreamReader(this.inputStream));
}




//////////////////////////////////////////////////////////////////////////
//	PARSING								//
//////////////////////////////////////////////////////////////////////////




/*************************************************************************
 *	Parses the XML file sending parse actions to the builder.
 * @throws Exception 
 ************************************************************************/
	
public void parse()
throws
	Exception
{
	// initialize building
	this.builder.begin();

	// parse the file
	try
	{
		XMLReader parser = new org.apache.xerces.parsers.SAXParser();
		ContentHandler handler = new Handler();
		parser.setContentHandler(handler);
		parser.parse(new InputSource(this.lineNumberReader));
	}
	catch( SAXException e )
	{
		// turn these into parse errors
		this.builder.error(this.getLineNumber());
		throw e;
	}
	catch( IOException e )
	{
		// let caller handle these, getting the line number from the builder
		this.builder.error(this.getLineNumber());
		throw e;
	}
	catch( Exception e )
	{
		if(e instanceof OldXMLFormatException) {
			//System.err.println("Error found: old XML file");
			throw e;
		} else if(e.getCause() instanceof OldXMLFormatException) {
			//System.err.println("Cause of error: old XML file");
			throw new OldXMLFormatException();
		} else {
			e.printStackTrace();
		}
		// TEMPORARY: do better inside 'builder'
		// turn these into parse errors
		this.builder.error(this.getLineNumber());
		return;
/**************************************************
		// treat everything else as fatal
		int lineNumber = this.getLineNumber();
		String what = this.inputName + ", line " + lineNumber;
		Dialogs.fatalException(Strings.XML_PARSE_EXCEPTION, what, e);
***************************************************/
	}

	// finalize building
	this.builder.end();
}




/*************************************************************************
 *	Returns the 1-based line number of the most recently read input line.
 *
 *	<p>
 *	We'd like this to be the line at which a syntax error was detected,
 *	but the lean parser we're using does not keep track of that. If it
 *	did this method should return <code>this.locator.getLineNumber()</code>.
 *
 ************************************************************************/
	
public int getLineNumber()
{
	return this.lineNumberReader.getLineNumber();
}




//////////////////////////////////////////////////////////////////////////
//	CLASS HANDLER														//
//////////////////////////////////////////////////////////////////////////

 /**
 *
 *	An auxiliary class required by SAX parsers.
 *
 *	Our handler class is an inner class of our <code>Parser</code> class.
 *	It accepts the SAX actions that occur during parsing and passes them to
 *	the parser's <code>Builder</code> instance.
 *
 */


	public class Handler extends DefaultHandler
	{

	/** Takes note of the current parsing position within the input. */
	public void setDocumentLocator(Locator locator)
	{
		Parser.this.locator = locator;
	}


	/** Tells the builder an element is starting. */
	public void startElement(String uri, String localName, String qualifiedName, Attributes attrs)
	{
		// Note: AttributeList.getType() is discarded here

		final int count = attrs.getLength();
		String[] attributeNames  = new String[count];
		String[] attributeValues = new String[count];
		for( int k = 0;  k < count;  k++ )
		{
			attributeNames [k] = attrs.getLocalName (k); // johnmc - was getName, perhaps getLocalName?
			attributeValues[k] = attrs.getValue(k);
		}

		Parser.this.builder.beginElement(localName, attributeNames, attributeValues);
	}


	/** Gives the builder content characters within current element. */
	public void characters(char[] chars, int start, int length)
	{
		String s = new String(chars, start, length).trim();
		if( s.length() > 0 )
		Parser.this.builder.content(s);
	}


	/** Tells the builder the current element is ending. */
	public void endElement(String uri, String localName, String qualifiedName)
	{
		Parser.this.builder.endElement(localName);
	}



}




}








