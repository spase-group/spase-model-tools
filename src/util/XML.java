package org.spase.model.util;

import java.io.PrintStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.StringReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Comment;
import org.w3c.dom.Text;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

// Container for a label 
public class XML {
	String		mVersion = "0.0.0";
	Document	mDocument;
	String		mPathName = "";
	
 	/** Creates an instance of a XML */
 	public XML() {
 	}
 	
 	/** 
     * Returns a string with the release information for this compilation.
     *
     * @return	a string contining the release information for this compilation.
     * @since           1.0
     */
	public String version() {
		return mVersion;
 	}
 	
 	/**
 	 * Entry point for testing
 	 **/
    public static void main(String args[]) 
    {
    	int		output = 0;	// Default is Dump
    	
    	if(args.length == 0) {
    		System.out.println("Proper usage: spase.XML pathname [dump|xml]");
    		return;
    	}
    	
    	XML doc = new XML();
    	try {
			doc.parseXML(args[0]);
    	
	    	if(args.length > 1) {
	    		if(args[1].compareToIgnoreCase("dump") == 0) output = 0;	// Dump
	    		if(args[1].compareToIgnoreCase("xml") == 0) output = 1;	// XML
	    	}
	    	
	    	System.out.println("--- Data Dump ---");
	    	doc.dumpData(System.out);
	    	
	    	System.out.println("--- XML ---");
	    	doc.printXML(System.out);
	    	
	    	switch(output) {
			case 1:	// XML
				// doc.printXML(System.out);
				break;
			case 0:	// Dump as label
			default:
				// doc.print();
				break;
	    	}
    	} catch(Exception e) {
    		System.out.println(e.getMessage());
    		// e.printStackTrace(System.out);
    		return;
    	}
    }
    
    /** 
     * Parses a file containing XML into its constitute elments.
	 * The path and name of the file are passed to the method which is
	 * opened and parsed.
	 *
     * @param pathName  the fully qualified path and name of the file to parse.
     *
     * @return          <code>true</code> if the file could be opened;
     *                  <code>false</code> otherwise.
     * @since           1.0
     */
 	public boolean parseXML(String pathName) 
 		throws Exception 
 	{
		FileInputStream file;
		boolean			status;
		
		mPathName = pathName;
		
		file = new FileInputStream(mPathName);
		status = parseXML(file);
		file.close();
		
		return status;
 	}
 	
    /** 
     * Parses a file containing XML into its constitute elments.
	 * The file to parse must be previously opened and a InputStream
	 * pointing to the file is passed.
	 *
     * @param reader		a connection to a pre-opened file.
     *
     * @return          <code>true</code> if the file could be read;
     *                  <code>false</code> otherwise.
     * @since           1.0
     */
 	public boolean parseXML(InputStream stream) 	
 		throws Exception
 	{
        DocumentBuilderFactory factory =
            DocumentBuilderFactory.newInstance();
        
        // Configure parser
        factory.setValidating(false);   
        factory.setNamespaceAware(false);
        factory.setIgnoringComments(false);
        
		DocumentBuilder builder = factory.newDocumentBuilder();
		mDocument = builder.parse(stream);
        
        return true;
 	}

 	/** 
     * Generates an XML representation of the label and stream it to the 
     * print stream.
	 * 
     * @param out    	the stream to print the element to.
     *
     * @since           1.0
     */
	public void printXML(PrintStream out)	
		throws Exception
	{
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
		try {	// Need for Java 1.5 to work properly
			transfac.setAttribute("indent-number", new Integer(4));
		} catch(Exception ie) {
		}
        Transformer trans = transfac.newTransformer(getDefaultStyleSheet());
        
        // Set up desired output format
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StreamResult result = new StreamResult(out);
        DOMSource source = new DOMSource(mDocument);
        
        trans.transform(source, result);
	}
	
 	/** 
     * Dump the CData sections of the XML
	 * 
     * @param out    	the stream to print the element to.
     *
     * @since           1.0
     */
	public void dumpData(PrintStream out)
		throws Exception
	{
        Node node;
        DOMSource	source = new DOMSource(mDocument);
        
        node = source.getNode();
        dumpNode(out, node);
	}

 	/** 
     * Dump the CData sections of the XML
	 * 
     * @param out    	the stream to print the element to.
     *
     * @since           1.0
     */
	public void dumpNode(PrintStream out, Node node)
	{
		Node	child;
		Node	sibling;
		out.println(node.getNodeType() + ": " + node.getNodeName() + ": " + node.getNodeValue());
		if(node.hasChildNodes()) {	// Output all children
			child = node.getFirstChild();
			sibling = child;
			while(sibling != null) {
				dumpNode(out, sibling);
				sibling = sibling.getNextSibling();
			}
		}
	}
	
	/** 
     * Obtain a StreamSource to the default XML Style Sheet.
	 * 
     * @return          a {@link StreamSource} which can be used to read the default
     *					style sheet.
     *
     * @since           1.0
     */
	public StreamSource getDefaultStyleSheet()
	{
		StringReader reader = new StringReader(
			  "<!DOCTYPE stylesheet ["
			+ "   <!ENTITY cr \"<xsl:text> </xsl:text>\">"
			+ "]>"
 			+ "<xsl:stylesheet"
    		+ "   xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\""
    		+ "   xmlns:xalan=\"http://xml.apache.org/xslt\""
    		+ "   version=\"1.0\""
    		+ ">"
    		+ ""
    		+ "<xsl:output method=\"xml\" indent=\"yes\" xalan:indent-amount=\"4\"/>"
    		+ ""
      		+ "<!-- copy out the xml -->"
    		+ "<xsl:template match=\"* | @*\">"
        	+ "   <xsl:copy><xsl:copy-of select=\"@*\"/><xsl:apply-templates/></xsl:copy>"
    		+ "</xsl:template>"
			+ "</xsl:stylesheet>"
			);
			
			return new StreamSource(reader);
	}

}
	