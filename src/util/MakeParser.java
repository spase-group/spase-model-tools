/**
 * Creates an XML parser for each container of metadata.
 * The parer classes convert XML documents into program 
 * accessible class members.
 * The generated source code is placed in the current directory.
 * Queries the data model database to build the the classes.
 * <p>
 * Usage:
 *   MakeParser version
 * 
 * @author Todd King
 * @version 1.00 2006 11 27
 * @copyright 2006 Regents University of California. All Rights Reserved
 */

package org.spase.model.util;

// import igpp.*
import igpp.servlet.MultiPrinter;
import igpp.database.Query;

// import javax.sql.*;
import java.sql.ResultSet;
import java.sql.Statement;

import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Date;

import java.text.SimpleDateFormat;

import javax.servlet.jsp.JspWriter;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MakeParser extends Query
{
	private String	mVersion = "1.0.0";
	
	private String	mModelVersion = null;
	
	// Database access variables
	private String mHost = "localhost";
	private String mDatabase = "spase";
	private String mUsername = "spase-user";
	private String mPassword = "my123";

	// Output 
	private JspWriter	mWriter = null;
	private PrintStream	mStream = null;
	private ServletOutputStream	mServlet = null;

	ArrayList<String> mTopLevelElements = null;;

	ArrayList<String>	mFileList = new ArrayList<String>();
	
	// Enumeration of Type
	private final int TypeContainer		= 0;
	private final int TypeCount			= 1;
	private final int TypeDate				= 2;
	private final int TypeEnumeration	= 3;
	private final int TypeItem				= 4;
	private final int TypeNumeric			= 5;
	private final int TypeText				= 6;
	private final int TypeTime				= 7;
	
    /** 
	 * Build an XML Schema document based on the SPASE data model specification
	 * in the data model database.
	 *<p>
	 * Usage:<blockquote>
	 *     MakeXSD version
	 * </blockquote>
	 *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @version     1.0, 11/23/06
	 * @since		1.0
	 */
	public static void main(String args[])
   {
		MakeParser me = new MakeParser();
		   
		if (args.length < 1) {
			System.err.println("Version: " + me.mVersion);
			System.err.println("Usage: " + me.getClass().getName() + " version");
			System.exit(1);
		}
		
		try {
			me.mModelVersion = args[0];
			me.mModelVersion = me.getModelVersion();
	
			me.setDatabase(me.mHost, me.mDatabase);
			me.setUserLogin(me.mUsername, me.mPassword);
			me.useUser();
			
			me.setWriter(System.out);
			me.makeAll();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
	public void init() 
			throws Exception 
	{
		setDatabase(mHost, mDatabase);
		setUserLogin(mUsername, mPassword);
		useUser();
		
		mModelVersion = getModelVersion();
	}
	
	public void destroy() 
	{
	}

    public void doPost(HttpServletRequest request, HttpServletResponse response)
   		throws Exception
    {
    	doGet(request, response);
    }
    
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	     throws Exception 
	{
   	ServletOutputStream out = response.getOutputStream();
   	
   	getModelVersion();
   	
		response.setContentType("application/data");
		response.setHeader("Content-Disposition", "attachment; filename=\"spase-" + mModelVersion.replace(".", "_") + ".xsd\"");
		
		setWriter(out);
		makeAll();
	}
	
	public void writeHeader(String name, String version, boolean isResource)
		throws Exception
	{
		String	today = new SimpleDateFormat("yyyy-MMM-d").format(new Date());
		String	ver = version.replace(".", "");
		
		printLine("package spase.parser%param%;", ver);
		printLine("");
		printLine("import org.w3c.dom.Node;");
		printLine("import java.util.ArrayList;");
		printLine("import java.util.Iterator;");
		printLine("");
		printLine("/**");
		printLine(" * A container of %param% information.", name);
		printLine(" *");
		printLine(" * @author Todd King - Generated automatically");
		printLine(" * @author UCLA/IGPP");
		printLine(" * @version     %param%", version);
		printLine(" **/");
		printLine("");
		if(isResource) {
			printLine("public class %param% extends Resource", name);
		} else {
			printLine("public class %param% extends XMLParser", name);
		}
		printLine("{");
	}
	
	public void writeConstructor(String name)
	{
		printLine("");
		printLine("   static public void main(String args[])", name);
		printLine("   {");
		printLine("      %param% me = new %param%();", name);
		printLine("      System.out.println(me.getXMLDocument(0));");
		printLine("   }");
		printLine("");
		printLine("   public %param%()", name);
		printLine("   {");
		printLine("      setClassName(\"%param%\");", name);
		printLine("      setRequired();");
		printLine("   }");
		printLine("");
		printLine("   public %param%(boolean forEdit)", name);
		printLine("   {");
		printLine("      setClassName(\"%param%\");", name);
		printLine("      setRequired();");
		printLine("      if(forEdit) makeEditNodes();");
		printLine("   }");
		printLine("");
		printLine("   public %param%(Node node)", name);
		printLine("   	throws Exception");
		printLine("   {");
		printLine("      setClassName(\"%param%\");", name);
		printLine("      setRequired();");
		printLine("      processNode(node);");
		printLine("   }");
	}

	public void writeFooter(String name)
		throws Exception
	{
		printLine("}");
	}
	
	public void writeVariables(ArrayList list)
		throws Exception
	{
		String	elemName;
		String	buffer;
		
		printLine("");
							
		for(Iterator i = list.iterator(); i.hasNext(); ) {
			buffer = (String) i.next();	// Occurrence token prepended to name
			elemName = getElemName(buffer);
			
			if(isMultiple(buffer)) {
				if(isContainer(getTermName(buffer))) {
					printLine("   private ArrayList<%param%> m%param% = new ArrayList<%param%>();", elemName); 
				} else {
					printLine("   private ArrayList<String> m%param% = new ArrayList<String>();", elemName); 
				}
			}	else { 
				if(isContainer(getTermName(buffer))) {
					printLine("   private %param% m%param% = new %param%();", elemName); 
				} else {
					printLine("   private String m%param% = \"\";", elemName); 
				}
			}
		}	
	}

	public void writeNodeMethods(String objectName, ArrayList list)
		throws Exception
	{
		String	tokenName;
		String	elemName;
		
		printLine("");
		printLine("   public void makeEditNodes()"); 
		printLine("   {");
		for(Iterator i = list.iterator(); i.hasNext(); ) {
			tokenName = (String) i.next();
			elemName = getElemName(tokenName);
			if(isContainer(getTermName(tokenName)) ) {
				if(isMultiple(tokenName)) {
					printLine("      m%param%.add(new %param%(true));", elemName);
				} else {
					printLine("      m%param%.makeEditNodes();", elemName);
				}
			}
		}
		printLine("   }");
		printLine("");
		printLine("   public void makeSkeletonNodes()"); 
		printLine("   {");
		for(Iterator i = list.iterator(); i.hasNext(); ) {
			tokenName = (String) i.next();
			elemName = getElemName(tokenName);
			if(isContainer(getTermName(tokenName)) ) {
				if(isMultiple(tokenName)) {
					printLine("      for(Iterator i = m%param%.iterator(); i.hasNext();) { ((%param%) i.next()).makeSkeletonNodes(); }", elemName);
				} else {
					printLine("      m%param%.makeSkeletonNodes();", elemName);
				}
			} else {
				if(isMultiple(tokenName)) {
					printLine("      if(m%param%.isEmpty()) m%param%.add(\"\");", elemName); 
				}
			}
		}
		printLine("   }");
		printLine("");
		printLine("   public XMLParser getMemberNode(String name)"); 
		printLine("   {");
		printLine("      String	nodeName = getMemberNodeName(name);");
		printLine("      int		nodeIndex = getMemberNodeIndex(name);");
		printLine("");
		if(hasResourceHeader(objectName)) {
				printLine("      if(isMatch(nodeName, \"ResourceHeader\")) return mResourceHeader;");
		}
		for(Iterator i = list.iterator(); i.hasNext(); ) {
			tokenName = (String) i.next();
			elemName = getElemName(tokenName);
			if(isContainer(getTermName(tokenName)) ) {
				if(isMultiple(tokenName)) {
					printLine("      if(isMatch(nodeName, \"%param%\")) return m%param%.get(nodeIndex);", elemName);
				} else {
					printLine("      if(isMatch(nodeName, \"%param%\")) return m%param%;", elemName);
				}
			}
		}
		printLine("");
		printLine("      return null;");
		printLine("   }");
		printLine("");
		printLine("   public void makeNewMember(String item)");
		printLine("   {");
		printLine("      if(item == null) return;");
		for(Iterator i = list.iterator(); i.hasNext(); ) {
			tokenName = (String) i.next();
			elemName = getElemName(tokenName);
			if(isMultiple(tokenName)) {
				if(isContainer(getTermName(tokenName)) ) {
					printLine("      if(isMatch(item, \"%param%\")) m%param%.add(new %param%());", elemName);
				} else {
					printLine("      if(isMatch(item, \"%param%\")) m%param%.add(\"\");", elemName);
				}
			}
		}
		printLine("   }");
		printLine("");
		
		printLine("   public void removeMember(String item, int index)");
		printLine("   {");
		printLine("      if(index < 0) return;");
		printLine("");
		for(Iterator i = list.iterator(); i.hasNext(); ) {
			tokenName = (String) i.next();
			elemName = getElemName(tokenName);
			if(isMultiple(tokenName)) {
				printLine("      if(isInList(item, \"%param%\", m%param%, index)) m%param%.remove(index);", elemName);
			}
		}
		printLine("   }");
	}

	public void writeXMLMethods(ArrayList list)
	{
		String	tokenName;
		String	elemName;
		
		printLine("");	
		printLine("   public String getXMLDocument(int n)");
		printLine("   {");
		printLine("      return getXMLDocument(n, null, -1, false);");
		printLine("   }");
		printLine("");	
		printLine("   public String getXMLDocument(int n, boolean inUseOnly)");
		printLine("   {");
		printLine("      return getXMLDocument(n, null, -1, inUseOnly);");
		printLine("   }");
		printLine("");	
		printLine("   public String getXMLDocument(int n, String path, int key)");
		printLine("   {");
		printLine("      return getXMLDocument(n, path, key, false);");
		printLine("   }");
		printLine("");	
		printLine("   public String getXMLDocument(int n, String path, int key, boolean inUseOnly)");
		printLine("   {");
		printLine("   	Iterator i;");
		printLine("   	XMLParser parser;");
		printLine("   	int		j;");
		printLine("   	String buffer = \"\";");
		printLine("   	String	elemPath = getElementPath(path, key);");
		printLine("");	
		printLine("   	if(path != null) buffer += getTaggedValue(n+1, \"ElementPath\", elemPath, inUseOnly);");
		for(Iterator i = list.iterator(); i.hasNext(); ) {
			tokenName = (String) i.next();
			elemName = getElemName(tokenName);
			if(isMultiple(tokenName)) {
				if(isContainer(getTermName(tokenName))) {
					printLine("");
					printLine("   	j = 0;");
					printLine("   	i = m%param%.iterator();", elemName);
					printLine("   	while(i.hasNext()) {");
					printLine("   		parser = (XMLParser) i.next();");
					printLine("   		buffer += parser.getXMLDocument(n+1, elemPath, j, inUseOnly);");
					printLine("   		j++;");
					printLine("   	}");
					printLine("");
				} else {
				   printLine("   	buffer += getTaggedList(n+1, \"%param%\", m%param%, inUseOnly);", elemName);
				}
			} else {
				if(isContainer(getTermName(tokenName))) {
					printLine("   	buffer += m%param%.getXMLDocument(n+1, elemPath, -1, inUseOnly);", elemName);
				} else {
				   printLine("   	buffer += getTaggedValue(n+1, \"%param%\", m%param%, inUseOnly);", elemName);
				}
			}
		}
		printLine("      if(inUseOnly && buffer.length() == 0) return \"\"; // Empty");	
		printLine("");	
		printLine("      return getTagOpen(n, getClassName()) + buffer + getTagClose(n, getClassName());");
		printLine("   }");
	}
	
	public void writeSetGet(ArrayList list)
		throws Exception
	{
		String	tokenName;
		String	elemName;
		
		for(Iterator i = list.iterator(); i.hasNext(); ) {
			tokenName = (String) i.next();
			elemName = getElemName(tokenName);
			if(isMultiple(tokenName)) {
				printLine("");
				if(isContainer(getTermName(tokenName))) {
					printLine("   public void set%param%(Node node) throws Exception { %param% i = new %param%(); i.processNode(node); m%param%.add(i);}", elemName);
					printLine("   public ArrayList<%param%> get%param%() { return m%param%; }", elemName);
				} else {
					printLine("   public void set%param%(String value) { m%param%.add(value); }", elemName);
					printLine("   public void set%param%(String[] value) { m%param%.clear(); for(int i = 0; i < value.length; i++) { m%param%.add(value[i]); } }", elemName);
					printLine("   public ArrayList<String> get%param%() { return m%param%; }", elemName);
				}
			} else {
				printLine("");
				if(isContainer(getTermName(tokenName))) {
					printLine("   public void set%param%(Node node) throws Exception { m%param%.processNode(node);} ", elemName);
					printLine("   public %param% get%param%() { return m%param%; }", elemName);
				} else {
					printLine("   public void set%param%(String value) { m%param% = value; }", elemName);
					printLine("   public void set%param%(String[] value) { m%param% = value[0]; }", elemName);
					printLine("   public String get%param%() { return m%param%; }", elemName);
				}
			}
		}
	}
	
	public void writeReset(ArrayList list)
	{
		String	tokenName;
		String	elemName;
		
		printLine("");
		printLine("   public void reset()");
		printLine("   {");
		for(Iterator i = list.iterator(); i.hasNext(); ) {
			tokenName = (String) i.next();
			elemName = getElemName(tokenName);
			if(isMultiple(tokenName)) {
				if(isContainer(getTermName(tokenName))) {
					printLine("		for(int i = 0; i < m%param%.size(); i++) m%param%.get(i).reset();", elemName);
				} else {
					printLine("		for(int i = 0; i < m%param%.size(); i++) m%param%.set(i, \"\");", elemName);
				}
			} else {
				if(isContainer(getTermName(tokenName))) {
					printLine("      m%param%.reset();", elemName);
				} else {
					printLine("      m%param% = \"\";", elemName);
				}
			}
		}
		printLine("   }");
	}
	
	public void writeRequired(ArrayList list)
	{
		String	tokenName;
		String	elemName;
		
		printLine("");
		printLine("   public void setRequired()");
		printLine("   {");
		for(Iterator i = list.iterator(); i.hasNext(); ) {
			tokenName = (String) i.next();
			if(isRequired(tokenName)) {
				elemName = getElemName(tokenName);
				printLine("      addRequired(\"" + elemName + "\");");
			}
		}
		printLine("   }");
	}
	
	public void writeXPathPairs(String object, ArrayList list)
	{
		String	tokenName;
		String	elemName;
		
		printLine("");
		printLine("   public ArrayList<Pair> getXPathPairs(String prefix, int index)");
		printLine("   {");
		printLine("      ArrayList<Pair> list = new ArrayList<Pair>();");
		printLine("      String path = \"\";");
		printLine("");
		printLine("      path = prefix + \"/\" + getClassName();");
		printLine("      if(index > 0) path += \"[\" + index + \"]\";");
		printLine("");
		for(Iterator i = list.iterator(); i.hasNext(); ) {
			tokenName = (String) i.next();
			elemName = getElemName(tokenName);
			if(isMultiple(tokenName)) {
				if(isContainer(getTermName(tokenName))) {
      			printLine("      list.addAll(%param%.getXPathPairs(path, m%param%));", elemName);
				} else {
      			printLine("      list.addAll(XMLParser.getXPathList(path + \"/%param%\", m%param%));", elemName);
				}
			} else {
				if(isContainer(getTermName(tokenName))) {
			      printLine("      list.addAll(m%param%.getXPathPairs(path, 0));", elemName);
				} else {
					printLine("      list.add(new Pair<String, String>(path + \"/%param%\", m%param%));", elemName);
				}
			}
		}
		printLine("      return list;");
		printLine("   }");
		
		printLine("");
		printLine("	public static ArrayList<Pair> getXPathPairs(String prefix, ArrayList<%param%> list)", object);
		printLine("	{");
		printLine("   	ArrayList<Pair> pairList = new ArrayList<Pair>();");
		printLine("");
		printLine("      for(int i = 0; i < list.size(); i++) {");
		printLine("	   	pairList.addAll(list.get(i).getXPathPairs(prefix, i+1));");
		printLine("      }");
		printLine("      return pairList;");
		printLine("	}");
	}
	
	public void writeDescriptionClass(ArrayList list, String version)
		throws Exception
	{
		String	today = new SimpleDateFormat("yyyy-MMM-d").format(new Date());
		String	ver = version.replace(".", "");
		String	verXSD = version.replace(".", "_");
		String	tokenName;
		String	elemName;
		
		String	fileName = "Description.java";
		PrintStream out = new PrintStream(fileName);
		mFileList.add(fileName);
		setWriter(out);
	   System.out.println(fileName);

	   printLine("package spase.parser%param%;", ver);
	   printLine("");
	   printLine("import java.util.ArrayList;");
	   printLine("import java.util.Iterator;");
	   printLine("");
	   // printLine("import org.w3c.dom.Document;");
	   // printLine("import org.w3c.dom.Node;");
	   // printLine("");
	   printLine("import javax.xml.transform.dom.DOMSource;");
	   printLine("");
	   printLine("/**");
	   printLine(" * A container of resource description.");
	   printLine(" *");
	   printLine(" * @author Todd King");
	   printLine(" * @author UCLA/IGPP");
	   printLine(" * @version     %param%", version);
	   printLine(" **/");
	   printLine("public class Description ");
	   printLine("{");
	   printLine("   private static String mVersion = \"%param%\";", version);
	   printLine("");
	   printLine("   private String mSpaseVersion = \"%param%\";", version);
	   printLine("   private ArrayList<Resource> mResource = new ArrayList<Resource>();");
	   printLine("");
	   printLine("   /**");
	   printLine("    * Entry point for testing.");
	   printLine("    *");
	   printLine("    * @author Todd King");
	   printLine("    * @author UCLA/IGPP");
	   printLine("    * @since     1.0");
	   printLine("    **/");
	   printLine("   public static void main(String args[]) ");
	   printLine("   {");
	   printLine("      int      output = 0; // Default is Dump");
	   printLine("      Description me = new Description();");
	   printLine("");
	   printLine("      System.out.println(\"Version: \" + me.mVersion);");
	   printLine("      if(args.length == 0) {");
	   printLine("         System.out.println(\"Proper usage: \" + me.getClass().getName() + \" xmlfile\");");
	   printLine("         return;");
	   printLine("      }");
	   printLine("");
	   printLine("      XMLParser parser = new XMLParser();");
	   printLine("");
	   printLine("");
	   printLine("      try {");
	   printLine("         me.parse(args[0]);");
	   printLine("         System.out.println(\"Parsed version: \" + me.mSpaseVersion);");
	   printLine("         me.dump();");
	   printLine("");
	   printLine("      } catch(Exception e) {");
	   printLine("         e.printStackTrace(System.out);");
	   printLine("      }     ");
	   printLine("   }");
	   printLine("    ");
	   printLine("   /** ");
	   printLine("    * Clear the contents.");
	   printLine("    * ");
	   printLine("    * @author Todd King");
	   printLine("    * @author UCLA/IGPP");
	   printLine("    * @since           1.0");
	   printLine("    **/");
	   printLine("   public void clear()");
	   printLine("       throws Exception");
	   printLine("   {");
	   printLine("      mSpaseVersion = \"\";");
	   printLine("      mResource = new ArrayList<Resource>();");
	   printLine("      System.gc();   // Force garbage collection");
	   printLine("   }");
	   printLine("    ");
	   printLine("   /** ");
	   printLine("    * Parse a description.");
	   printLine("    * ");
	   printLine("    * @author Todd King");
	   printLine("    * @author UCLA/IGPP");
	   printLine("    * @since           1.0");
	   printLine("    **/");
	   printLine("   public void parse(String pathname)");
	   printLine("       throws Exception");
	   printLine("   {");
	   printLine("      XMLParser parser = new XMLParser();");
	   printLine("");
	   printLine("      parser.parseXML(pathname);");
	   printLine("      populate(parser.getDocument());");
	   printLine("   }");
	   printLine("");
	   printLine("   /** ");
	   printLine("    * Walk the nodes of the Document and populate the resource classes.");
	   printLine("    * ");
	   printLine("    * @param document      the {@link Document} containing a parsed XML file.");
	   printLine("    *");
	   printLine("    * @author Todd King");
	   printLine("    * @author UCLA/IGPP");
	   printLine("    * @since           1.0");
	   printLine("    **/");
	   printLine("   public void populate(org.w3c.dom.Document document)");
	   printLine("       throws Exception");
	   printLine("   {");
	   printLine("       DOMSource  source = new DOMSource(document);");
	   printLine("");
	   printLine("       processNode(source.getNode());");
	   printLine("   }");
	   printLine("");
	   printLine("   /** ");
	   printLine("    * Walk the nodes of the Document and populate the resource classes.");
	   printLine("    * ");
	   printLine("    * @param node       the {@link Node}.");
	   printLine("    *");
	   printLine("    * @author Todd King");
	   printLine("    * @author UCLA/IGPP");
	   printLine("    * @since           1.0");
	   printLine("    **/");
	   printLine("   public void processNode(org.w3c.dom.Node node)");
	   printLine("       throws Exception");
	   printLine("   {");
	   printLine("       String   name;");
	   printLine("       org.w3c.dom.Node  child;");
	   printLine("       org.w3c.dom.Node  sibling;");
	   printLine("");
	   printLine("       if(node == null) return;");
	   printLine("       name = node.getNodeName();");
	   printLine("       if(name == null) return;");
	   printLine("");
	   printLine("       // Process all children");
	   printLine("       if(node.hasChildNodes()) { // Output all children");
	   printLine("          child = node.getFirstChild();");
	   printLine("          sibling = child;");
	   printLine("          while(sibling != null) {");
	   printLine("             name = sibling.getNodeName();");
	   printLine("             if(name == null) continue;");
	   printLine("");
	   printLine("             // Objects in this object");
	   printLine("             if(Util.isMatch(name, \"spase\")) processSpase(sibling);");
	   printLine(" ");
	   printLine("             // Next node");
	   printLine("             sibling = sibling.getNextSibling();");
	   printLine("          }");
	   printLine("       }");
	   printLine("   }");
	   printLine(" ");
	   printLine("   /** ");
	   printLine("    * Walk the nodes of the \"Spase\" object in a Document and populate the resource classes.");
	   printLine("    *");
	   printLine("    * @param node       the {@link Node}.");
	   printLine("    *");
	   printLine("    * @author Todd King");
	   printLine("    * @author UCLA/IGPP");
	   printLine("    * @since           1.0");
	   printLine("    **/");
	   printLine("   public void processSpase(org.w3c.dom.Node node)");
	   printLine("       throws Exception");
	   printLine("   {");
	   printLine("       String   name;");
	   printLine("       org.w3c.dom.Node  child;");
	   printLine("       org.w3c.dom.Node  sibling;");
	   printLine("");
	   printLine("       if(node == null) return;");
	   printLine("");
	   printLine("       // Process all children");
	   printLine("       if(node.hasChildNodes()) { // Output all children");
	   printLine("          child = node.getFirstChild();");
	   printLine("          sibling = child;");
	   printLine("          while(sibling != null) {");
	   printLine("             name = sibling.getNodeName();");
	   printLine("             if(name == null) continue;");
	   printLine("");
	   printLine("             // Elements in this object");
	   printLine("             if(Util.isMatch(name, \"version\")) mSpaseVersion = Util.getNodeText(sibling);");
	   printLine("");

	   printLine("             // Objects in this object ");
		for(Iterator i = list.iterator(); i.hasNext(); ) {
			elemName = getXSLName((String) i.next());
			printLine("             if(Util.isMatch(name, \"%param%\")) mResource.add(new %param%(sibling));", elemName);
		}
		
	   printLine("");
	   printLine("             // Next node");
	   printLine("             sibling = sibling.getNextSibling();");
	   printLine("          }");
	   printLine("       }");
	   printLine("   }");
	   printLine("");
	   printLine("   /** ");
	   printLine("    * Return a string with an XML representation of all resources");
	   printLine("    *");
	   printLine("    * @author Todd King");
	   printLine("    * @author UCLA/IGPP");
	   printLine("    * @since           1.0");
	   printLine("    **/");
	   printLine("   public String getXMLDocument(int n)");
	   printLine("   {");
	   printLine("      return getXMLDocument(n, null, -1);");
	   printLine("   }");
	   printLine("");
	   printLine("   /** ");
	   printLine("    * Return a string with an XML representation of all resources");
	   printLine("    * If inUseOnly is true than return only those elements which are");
	   printLine("    * required or set.");
	   printLine("    *");
	   printLine("    * @author Todd King");
	   printLine("    * @author UCLA/IGPP");
	   printLine("    * @since           1.0");
	   printLine("    **/");
	   printLine("   public String getXMLDocument(int n, boolean inUseOnly)");
	   printLine("   {");
	   printLine("      return getXMLDocument(n, null, -1, inUseOnly);");
	   printLine("   }");
	   printLine("");
	   printLine("   /** ");
	   printLine("    * Return a string with an XML representation of all resources");
	   printLine("    *");
	   printLine("    * @author Todd King");
	   printLine("    * @author UCLA/IGPP");
	   printLine("    * @since           1.0");
	   printLine("    **/");
	   printLine("   public String getXMLDocument(int n, String path, int key)");
	   printLine("   {");
	   printLine("      return getXMLDocument(n, path, key, false);");
	   printLine("   }");
	   printLine("");
	   printLine("   /** ");
	   printLine("    * Return a string with an XML representation of all resources.");
	   printLine("    * If inUseOnly is true than return only those elements which are");
	   printLine("    * required or set.");
	   printLine("    *");
	   printLine("    * @author Todd King");
	   printLine("    * @author UCLA/IGPP");
	   printLine("    * @since           1.0");
	   printLine("    **/");
	   printLine("   public String getXMLDocument(int n, String path, int key, boolean inUseOnly)");
	   printLine("   {");
	   printLine("       String   buffer = \"<?xml version=\\\"1.0\\\"?>\\n\";");
	   printLine("       Resource resource;");
	   printLine("       int      cnt = 0;");
	   printLine("       buffer += \"<Spase xmlns=\\\"http://www.spase-group.org/data/schema\\\">\\n\";");
	   printLine("       buffer += \"<Version>\" + mVersion + \"</Version>\\n\";");
	   printLine("       for(Iterator i = mResource.iterator(); i.hasNext(); ) {");
	   printLine("          resource = (Resource) i.next();");
	   printLine("          buffer += resource.getXMLDocument(n, path, getIndexOf(resource.getClassName(), cnt), inUseOnly);");
	   printLine("          cnt++;");
	   printLine("       }");
	   printLine("          buffer += \"</Spase>\\n\";");
	   printLine("");
	   printLine("   return buffer;");
	   printLine("   }");
	   printLine("");
	   printLine("   /** ");
	   printLine("    * Return a string with an XML representation of all resources");
	   printLine("    *");
	   printLine("    * @author Todd King");
	   printLine("    * @author UCLA/IGPP");
	   printLine("    * @since           1.0");
	   printLine("    **/");
	   printLine("   public int getIndexOf(String className, int stopAt)");
	   printLine("   {");
	   printLine("       Resource resource;");
	   printLine("       int n = -1;");
	   printLine("       int   cnt = 0;");
	   printLine("       ");
	   printLine("       for(Iterator i = mResource.iterator(); i.hasNext(); ) {");
	   printLine("          resource = (Resource) i.next();");
	   printLine("          if(className.compareTo(resource.getClassName()) == 0) n++;");
	   printLine("          if(cnt >= stopAt) break;");
	   printLine("          cnt++;");
	   printLine("       }");
	   printLine("       ");
	   printLine("       return n;");
	   printLine("   }");
	   printLine("");
	   printLine("   /** ");
	   printLine("    * Add a resource to the description");
	   printLine("    *");
	   printLine("    * @author Todd King");
	   printLine("    * @author UCLA/IGPP");
	   printLine("    * @since           1.0");
	   printLine("    **/");
	   printLine("   public void addResource(Resource resource) { mResource.add(resource); }");
	   printLine("");
	   printLine("   /** ");
	   printLine("    * Remove a resource from the description");
	   printLine("    *");
	   printLine("    * @author Todd King");
	   printLine("    * @author UCLA/IGPP");
	   printLine("    * @since           1.0");
	   printLine("    **/");
	   printLine("   public void removeResource(int index) { if(index < mResource.size() && index >= 0) mResource.remove(index); }");
	   printLine("");
	   printLine("   /** ");
	   printLine("    * Get the {@link ArrayList} of resource descriptions.");
	   printLine("    *");
	   printLine("    * @author Todd King");
	   printLine("    * @author UCLA/IGPP");
	   printLine("    * @since           1.0");
	   printLine("    **/");
	   printLine("   public ArrayList<Resource> getResources() { return mResource; }");
	   printLine("");
	   printLine("   /** ");
	   printLine("    * Get the version of the SPASE data model declared in the description.");
	   printLine("    *");
	   printLine("    * @author Todd King");
	   printLine("    * @author UCLA/IGPP");
	   printLine("    * @since           1.0");
	   printLine("    **/");
	   printLine("   public String getVersion() { return mSpaseVersion; }");
	   printLine("");
	   printLine("   /** ");
	   printLine("    * Get the the number of resources in this description.");
	   printLine("    *");
	   printLine("    * @author Todd King");
	   printLine("    * @author UCLA/IGPP");
	   printLine("    * @since           1.0");
	   printLine("    **/");
	   printLine("   public int size() { return mResource.size(); }");
	   printLine("    ");
	   printLine("");
	   printLine("   /** ");
	   printLine("    * Create skeleton nodes for all resources.");
	   printLine("    *");
	   printLine("    * @author Todd King");
	   printLine("    * @author UCLA/IGPP");
	   printLine("    * @since           1.0");
	   printLine("    **/");
	   printLine("   public void makeSkeletonNodes() { for(Iterator i = mResource.iterator(); i.hasNext();) { ((Resource) i.next()).makeSkeletonNodes(); } }");
	   printLine("");
	   printLine("   /** ");
	   printLine("    * Print the contents of the description.");
	   printLine("    *");
	   printLine("    * @author Todd King");
	   printLine("    * @author UCLA/IGPP");
	   printLine("    * @since           1.0");
	   printLine("    **/");
	   printLine("");
	   printLine("   public void dump()");
	   printLine("   {");
	   printLine("       System.out.println(\"Version: \" + mSpaseVersion);");
	   printLine("       for(int i = 0; i < mResource.size(); i++) {");
	   printLine("          mResource.get(i).dump();");
	   printLine("       }");
	   printLine("   }");
	   printLine("}");
	   
	   closeWriter();
	}

	public void writePairClass(String version)
		throws Exception
	{
		String	fileName = "Pair.java";
		PrintStream out = new PrintStream(fileName);
		String	ver = version.replace(".", "");
		
		mFileList.add(fileName);
		setWriter(out);
	   System.out.println(fileName);

		printLine("package spase.parser%param%;", ver);
      printLine("");
      printLine("/**");
      printLine(" * An simple class to maintain pairs of objects.");
      printLine(" * Adapted form a web post.");
      printLine(" *");
      printLine(" * @author Todd King");
      printLine(" * @author UCLA/IGPP");
      printLine(" * @version     1.0.0");
      printLine(" * @since     1.0.0");
      printLine(" **/");
      printLine("public class Pair<L, R> {");
      printLine(" ");
      printLine("	private final L left;");
      printLine("	private final R right;");
      printLine(" ");
      printLine("	/** ");
      printLine("	* Creates an instance of pair with a \"left\" value and a \"right\" value. ");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public Pair(final L left, final R right) {");
      printLine("	  this.left = left;");
      printLine("	  this.right = right;");
      printLine("	}");
      printLine("    ");
      printLine("	/** ");
      printLine("	* Creates an instance of pair with a \"left\" value and a \"right\" value. ");
      printLine("	* The type of each value can be defined.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public static <A, B> Pair<A, B> create(A left, B right) {");
      printLine("	  return new Pair<A, B>(left, right);");
      printLine("	}");
      printLine(" ");
      printLine("	/** ");
      printLine("	* Returns the \"right\" element of the pair. ");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public R getRight() {");
      printLine("	  return right;");
      printLine("	}");
      printLine(" ");
      printLine("	/** ");
      printLine("	* Returns the \"right\" element of the pair. ");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public L getLeft() {");
      printLine("	  return left;");
      printLine("	}");
      printLine("	");
      printLine("	/** ");
      printLine("	* Determines if one object equals another. ");
      printLine("	* This checks if the content of each object");
      printLine("	* is the same. ");
      printLine("	*");
      printLine("   * @return          <code>true</code> if the content is the same;");
      printLine("   *                  <code>false</code> otherwise.");
      printLine("   *");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public final boolean equals(Object o) {");
      printLine("	  if (!(o instanceof Pair))");
      printLine("	      return false;");
      printLine("	");
      printLine("	  final Pair<?, ?> other = (Pair) o;");
      printLine("	  return equal(getLeft(), other.getLeft()) && equal(getRight(), other.getRight());");
      printLine("	}");
      printLine("	");
      printLine("	/** ");
      printLine("	* Determines if one object equals another. ");
      printLine("	* This checks if the content of each object");
      printLine("	* is the same. ");
      printLine("	*");
      printLine("   * @return          <code>true</code> if the content is the same;");
      printLine("   *                  <code>false</code> otherwise.");
      printLine("   *");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public static final boolean equal(Object o1, Object o2) {");
      printLine("	  if (o1 == null) {");
      printLine("	      return o2 == null;");
      printLine("	  }");
      printLine("	  return o1.equals(o2);");
      printLine("	}");
      printLine("	");
      printLine("	/** ");
      printLine("	* Returns the hash code. ");
      printLine("	* This checks if the content of each object");
      printLine("	* is the same. ");
      printLine("	*");
      printLine("   * @return          the hash code value.");
      printLine("   *");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public int hashCode() {");
      printLine("	  int hLeft = getLeft() == null ? 0 : getLeft().hashCode();");
      printLine("	  int hRight = getRight() == null ? 0 : getRight().hashCode();");
      printLine("	");
      printLine("	  return hLeft + (57 * hRight);");
      printLine("	}");
      printLine("}");
      printLine("");
		
		closeWriter();
	}
	
	public void writeUtilClass(String version)
		throws Exception
	{
		String	fileName = "Util.java";
		PrintStream out = new PrintStream(fileName);
		String	ver = version.replace(".", "");
		
		mFileList.add(fileName);
		setWriter(out);
	   System.out.println(fileName);

		printLine("package spase.parser%param%;", ver);
      printLine("");
      printLine("import org.w3c.dom.Node;");
      printLine("import java.util.ArrayList;");
      printLine("");
      printLine("/**");
      printLine(" * A container of utility methods for SPASE description.");
      printLine(" *");
      printLine(" * @author Todd King");
      printLine(" * @author UCLA/IGPP");
      printLine(" * @version     1.0");
      printLine(" * @since     1.0");
      printLine(" **/");
      printLine("public class Util");
      printLine("{");
      printLine("   /** ");
      printLine("    * Determines if two strings match, ignoring case.");
      printLine("    * ");
      printLine("    * @param src       the source string.");
      printLine("    * @param comp      the string to compare to the source string ");
      printLine("    *");
      printLine("    * @author Todd King");
      printLine("    * @author UCLA/IGPP");
      printLine("    * @since           1.0");
      printLine("    **/");
      printLine("   static public boolean isMatch(String src, String comp)");
      printLine("   {");
      printLine("   	if(src == null && comp == null) return true;");
      printLine("   	if(src == null) return false;");
      printLine("   	if(comp == null) return false;");
      printLine("   	");
      printLine("      if(src.compareToIgnoreCase(comp) == 0) return true;");
      printLine("");
      printLine("      return false;");
      printLine("   }");
      printLine("");
      printLine("   /** ");
      printLine("    * Concatenate all the text under a node.");
      printLine("    * ");
      printLine("    * @param node      the {@link Node}.");
      printLine("    *");
      printLine("    * @author Todd King");
      printLine("    * @author UCLA/IGPP");
      printLine("    * @since           1.0");
      printLine("    **/");
      printLine("   static public String getNodeText(Node node)");
      printLine("   {");
      printLine("      Node  child;");
      printLine("      Node  sibling;");
      printLine("      String   buffer = \"\";");
      printLine("");
      printLine("      if(node.hasChildNodes()) { // Output all children");
      printLine("         child = node.getFirstChild();");
      printLine("         sibling = child;");
      printLine("         while(sibling != null) {");
      printLine("            if(sibling.getNodeType() == Node.TEXT_NODE) {");
      printLine("               buffer += sibling.getNodeValue();");
      printLine("            }");
      printLine("            sibling = sibling.getNextSibling();");
      printLine("         }");
      printLine("      }");
      printLine("      ");
      printLine("      return buffer;");
      printLine("   }");
      printLine("");
      printLine("   /** ");
      printLine("    * Print a list of values.");
      printLine("    * ");
      printLine("    * @param node      the {@link Node}.");
      printLine("    *");
      printLine("    * @author Todd King");
      printLine("    * @author UCLA/IGPP");
      printLine("    * @since           1.0");
      printLine("    **/");
      printLine("   static public void dumpList(String label, ArrayList<String> list)");
      printLine("   {");
      printLine("      for(int i = 0; i < list.size(); i++) {");
      printLine("         System.out.println(label + list.get(i));");
      printLine("      }");
      printLine("   }");
      printLine("}");

		
		closeWriter();
	}
	
	public void writeResourceClass(String version)
		throws Exception
	{
		String	fileName = "Resource.java";
		PrintStream out = new PrintStream(fileName);
		String	ver = version.replace(".", "");
	
		mFileList.add(fileName);
		setWriter(out);
	   System.out.println(fileName);
	
		printLine("package spase.parser%param%;", ver);
      printLine("");
      printLine("import org.w3c.dom.Node;");
      printLine("import java.lang.reflect.Method;");
      printLine("import java.util.ArrayList;");
      printLine("");
      printLine("/**");
      printLine(" * A container of resource description.");
      printLine(" *");
      printLine(" * @author Todd King");
      printLine(" * @author UCLA/IGPP");
      printLine(" * @version     120");
      printLine(" **/");
      printLine("public class Resource extends XMLParser");
      printLine("{");
      printLine(" private String mVersion = \"1.0.0\";");
      printLine(" ");
      printLine(" public String mResourceID = \"\";");
      printLine(" public ResourceHeader mResourceHeader = new ResourceHeader();");
      printLine(" ");
      printLine("    /** ");
      printLine("     * Creates an instance of a Resource ");
      printLine("  *");
      printLine("  * @author Todd King");
      printLine("  * @author UCLA/IGPP");
      printLine("  * @version     1.1.0, 06/09/05");
      printLine("     **/");
      printLine("    public Resource() {");
      printLine("    }");
      printLine("    ");
      printLine("    /** ");
      printLine("     * Creates an instance of a Resource and populates it with the contents");
      printLine("     * of a parsed XML file starting at the passed Node.");
      printLine("  *");
      printLine("  * @author Todd King");
      printLine("  * @author UCLA/IGPP");
      printLine("  * @version     1.0.0");
      printLine("     **/");
      printLine("    public Resource(Node node) ");
      printLine("       throws Exception");
      printLine("    {");
      printLine("       processNode(node);");
      printLine("       System.out.println(\"Resource ID: \" + mResourceID);");
      printLine("    }");
      printLine("    ");
      printLine(" ");
      printLine(" /**");
      printLine("  * Entry point for testing.");
      printLine("     **/");
      printLine("    public static void main(String args[]) ");
      printLine("    {");
      printLine("       Resource me = new Resource();");
      printLine("       ");
      printLine("       if(args.length == 0) {");
      printLine("       System.out.println(\"Version: \" + me.mVersion);");
      printLine("          System.out.println(\"Proper usage: \" + me.getClass().getName() + \" xmlfile\");");
      printLine("          return;");
      printLine("       }");
      printLine("    }");
      printLine("    ");
      printLine("");
      printLine(" public void reset()");
      printLine(" {");
      printLine("    mResourceID = \"\";");
      printLine("    mResourceHeader.reset();");
      printLine(" }");
      printLine(" ");
      printLine(" /** ");
      printLine("  * Make an instance of every node for use in an editor");
      printLine("  **/");
      printLine(" public void makeEditNodes()");
      printLine(" {");
      printLine("    mResourceHeader.makeEditNodes();");
      printLine(" }");
	   printLine("");
	   printLine("   /** ");
	   printLine("    * Create skeleton nodes for all resources.");
	   printLine("    *");
	   printLine("    * @author Todd King");
	   printLine("    * @author UCLA/IGPP");
	   printLine("    * @since           1.0");
	   printLine("    **/");
	   printLine("   public void makeSkeletonNodes() { mResourceHeader.makeSkeletonNodes(); }");
      printLine("");
      printLine(" /** ");
      printLine("  * Retrieve the code nodes in the class.");
      printLine("  * Derived classes must implement this method");
      printLine("  * to retrieve added member nodes.");
      printLine("  **/");
      printLine(" public XMLParser getMemberNode(String name) ");
      printLine(" {");
      printLine("    String   nodeName = getMemberNodeName(name);");
      printLine("    int      nodeIndex = getMemberNodeIndex(name);");
      printLine("    ");
      printLine("    if(isMatch(nodeName, \"ResourceHeader\")) return mResourceHeader;");
      printLine("    ");
      printLine("    return null;");
      printLine(" }");
      printLine(" /**");
      printLine("  * Create an XML representation of the resource.");
      printLine("  * The dervied class should full implement this method.");
      printLine("  **/");
      printLine(" public String getXMLDocument(int level) { return \"\"; }");
      printLine(" ");
      printLine(" /**");
      printLine("  * Create an XML representation of the resource.");
      printLine("  * The dervied class should full implement this method.");
      printLine("  **/");
      printLine(" public String getXMLDocument(int level, String path, int key) { return \"\"; }");
      printLine(" ");
      printLine(" /**");
      printLine("  * Create a new item in the resource.");
      printLine("  * The dervied class should full implement this method.");
      printLine("  **/");
      printLine(" public void makeNew(String item) { return; }");
      printLine(" ");
      printLine(" public void setResourceID(String value) { mResourceID = value; }");
      printLine(" public void setResourceID(String[] value) { mResourceID = value[0]; }");
      printLine(" public String getResourceID() { return mResourceID; }");
      printLine(" ");
      printLine(" public void setResourceHeader(Node node) throws Exception { mResourceHeader.processNode(node); }");
      printLine(" public ResourceHeader getResourceHeader() { return mResourceHeader; }");
      printLine("");
      printLine(" }");
      printLine("");

		closeWriter();
	}	
		
	public void writeXMLParserClass(String version)
		throws Exception
	{
		String	fileName = "XMLParser.java";
		PrintStream out = new PrintStream(fileName);
		String	ver = version.replace(".", "");

		mFileList.add(fileName);
		setWriter(out);
	   System.out.println(fileName);

		printLine("package spase.parser%param%;", ver);
      printLine("");
      printLine("import java.io.PrintStream;");
      printLine("import java.io.InputStream;");
      printLine("import java.io.File;");
      printLine("import java.io.FileInputStream;");
      printLine("import java.io.StringReader;");
      printLine("import java.io.StringWriter;");
      printLine("import java.io.ByteArrayInputStream;");
      printLine("");
      printLine("import java.util.ArrayList;");
      printLine("import java.util.Iterator;");
      printLine("import java.util.AbstractSet;");
      printLine("");
      printLine("import java.lang.StringBuffer;");
      printLine("");
      printLine("import org.w3c.dom.Document;");
      printLine("import org.w3c.dom.Element;");
      printLine("import org.w3c.dom.Comment;");
      printLine("import org.w3c.dom.Text;");
      printLine("import org.w3c.dom.Node;");
      printLine("import org.w3c.dom.NodeList;");
      printLine("");
      printLine("import javax.xml.parsers.DocumentBuilderFactory;");
      printLine("import javax.xml.parsers.DocumentBuilder;");
      printLine("import javax.xml.transform.TransformerFactory;");
      printLine("import javax.xml.transform.Transformer;");
      printLine("import javax.xml.transform.OutputKeys;");
      printLine("import javax.xml.transform.stream.StreamResult;");
      printLine("import javax.xml.transform.dom.DOMSource;");
      printLine("import javax.xml.transform.stream.StreamSource;");
      printLine("");
      printLine("import java.lang.reflect.Method;");
      printLine("import java.lang.reflect.Field;");
      printLine("");
      printLine("/**");
      printLine(" * An example XML parser.");
      printLine(" *");
      printLine(" * @author Todd King");
      printLine(" * @author UCLA/IGPP");
      printLine(" * @version     1.0.0");
      printLine(" * @since     1.0.0");
      printLine(" **/");
      printLine("public class XMLParser {");
      printLine("	private String    mVersion = \"1.0.0\";");
      printLine("	private String    mPathName = \"\";");
      printLine("	private String    mClassName = null;");
      printLine("	private org.w3c.dom.Document  mDocument;");
      printLine("	");
      printLine("	private ArrayList<String>	mRequired = new ArrayList<String>();");
      printLine("	");
      printLine("	/** ");
      printLine("	* Creates an instance of a XML ");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public XMLParser() ");
      printLine("	{");
      printLine("	}");
      printLine("");
      printLine("	/**");
      printLine("	* Entry point for testing");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public static void main(String args[]) ");
      printLine("	{");
      printLine("		int      output = 1; // Default is XML");
      printLine("		XMLParser parser = new XMLParser();");
      printLine("		");
      printLine("		if(args.length == 0) {");
      printLine("			System.out.println(\"Version: \" + parser.mVersion);");
      printLine("			System.out.println(\"Proper usage: \" + parser.getClass().getName() + \" pathname [dump|xml]\");");
      printLine("			return;");
      printLine("		}");
      printLine("		");
      printLine("		try {");
      printLine("			parser.parseXML(args[0]);");
      printLine("			");
      printLine("			for(int i = 1; i < args.length; i++) {");
      printLine("				if(args[i].compareToIgnoreCase(\"dump\") == 0) output = 0;  // Dump");
      printLine("				if(args[i].compareToIgnoreCase(\"xml\") == 0) output = 1;   // XML");
      printLine("			}");
      printLine("			");
      printLine("			switch(output) {");
      printLine("			case 1:  // XML");
      printLine("				System.out.println(\"--- XML ---\");");
      printLine("				parser.printXML(System.out);");
      printLine("				// doc.printXML(System.out);");
      printLine("				break;");
      printLine("			case 0:  // Dump as label");
      printLine("			default:");
      printLine("				// doc.print();");
      printLine("				break;");
      printLine("			}");
      printLine("		} catch(Exception e) {");
      printLine("			System.out.println(e.getMessage());");
      printLine("			// e.printStackTrace(System.out);");
      printLine("			return;");
      printLine("		}");
      printLine("	}");
      printLine("    ");
      printLine("    /** ");
      printLine("     * Parses a file containing XML into its constitute elments and");
      printLine("     * sets internal variables with the contents of the file.");
      printLine("     * The path and name of the file are passed to the method which is");
      printLine("     * opened and parsed.");
      printLine("     *");
      printLine("     * @param pathName  the fully qualified path and name of the file to parse.");
      printLine("     *");
      printLine("     * @return          <code>true</code> if the file could be opened;");
      printLine("     *                  <code>false</code> otherwise.");
      printLine("     *");
      printLine("     * @author Todd King");
      printLine("     * @author UCLA/IGPP");
      printLine("     * @since           1.0");
      printLine("     **/");
      printLine("    public void load(String pathName) ");
      printLine("       throws Exception ");
      printLine("    {");
      printLine("    	 load(pathName, getClassName());");
      printLine("    }");
      printLine("    ");
      printLine("    /** ");
      printLine("     * Parses a file containing XML into its constitute elments and");
      printLine("     * sets internal variables with the contents of the file.");
      printLine("     * The path and name of the file are passed to the method which is");
      printLine("     * opened and parsed.");
      printLine("     *");
      printLine("     * @param pathName  the fully qualified path and name of the file to parse.");
      printLine("     * @param root	the name of the tag containg elements to process.");
      printLine("     *");
      printLine("     * @return          <code>true</code> if the file could be opened;");
      printLine("     *                  <code>false</code> otherwise.");
      printLine("     *");
      printLine("     * @author Todd King");
      printLine("     * @author UCLA/IGPP");
      printLine("     * @since           1.0");
      printLine("     **/");
      printLine("    public void load(String pathName, String root) ");
      printLine("       throws Exception ");
      printLine("    {");
      printLine("       parseXML(pathName);");
      printLine("       load(getDocument(), root);");
      printLine("    }");
      printLine("    ");
      printLine("    /** ");
      printLine("     * Parses a {@link Document} which contains a parsed XML file");
      printLine("     * and set internal variables with the contents of the file.");
      printLine("     *");
      printLine("     * @param document  the {@link Document} representation of a parsed XML file.");
      printLine("     * @param root	the name of the tag containg elements to process.");
      printLine("     *");
      printLine("     * @return          <code>true</code> if the file could be opened;");
      printLine("     *                  <code>false</code> otherwise.");
      printLine("     *");
      printLine("     * @author Todd King");
      printLine("     * @author UCLA/IGPP");
      printLine("     * @since           1.0");
      printLine("     **/");
      printLine("    public void load(org.w3c.dom.Document document, String root) ");
      printLine("       throws Exception ");
      printLine("    {");
      printLine("       DOMSource source = new DOMSource(getDocument());");
      printLine("       source.getNode().normalize();  // Clean-up DOM structure");
      printLine("       ");
      printLine("       Node node = source.getNode();");
      printLine("       if(root != null) node = findNode(node, root);");
      printLine("       ");
      printLine("       processNode(node);");
      printLine("    }");
      printLine("    ");
      printLine("    /** ");
      printLine("     * Parses a file containing XML into its constitute elments.");
      printLine("     * The path and name of the file are passed to the method which is");
      printLine("     * opened and parsed.");
      printLine("     *");
      printLine("     * @param pathName  the fully qualified path and name of the file to parse.");
      printLine("     *");
      printLine("     * @return          <code>true</code> if the file could be opened;");
      printLine("     *                  <code>false</code> otherwise.");
      printLine("     *");
      printLine("     * @author Todd King");
      printLine("     * @author UCLA/IGPP");
      printLine("     * @since           1.0");
      printLine("     **/");
      printLine("    public boolean parseXML(String pathName) ");
      printLine("       throws Exception ");
      printLine("    {");
      printLine("       FileInputStream file;");
      printLine("       boolean        status;");
      printLine("");
      printLine("       mPathName = pathName;");
      printLine("");
      printLine("       file = new FileInputStream(mPathName);");
      printLine("       status = parseXML(file);");
      printLine("       file.close();");
      printLine("");
      printLine("       return status;");
      printLine("    }");
      printLine("");
      printLine("    /** ");
      printLine("     * Parses a string containing XML into its constitute elments.");
      printLine("     *");
      printLine("     * @param text	the String conting the XML text.");
      printLine("     *");
      printLine("     * @return          <code>true</code> if the file could be opened;");
      printLine("     *                  <code>false</code> otherwise.");
      printLine("     *");
      printLine("     * @author Todd King");
      printLine("     * @author UCLA/IGPP");
      printLine("     * @since           1.0");
      printLine("     **/");
      printLine("    public boolean parseXMLString(String text) ");
      printLine("       throws Exception ");
      printLine("    {");
      printLine("    	 ByteArrayInputStream stream = new ByteArrayInputStream(text.getBytes());");
      printLine("       return parseXML(stream);");
      printLine("    }");
      printLine("");
      printLine("    /** ");
      printLine("     * Parses a file containing XML into its constitute elments.");
      printLine("     * The file to parse must be previously opened and a InputStream");
      printLine("     * pointing to the file is passed.");
      printLine("     *");
      printLine("     * @param stream     a connection to a pre-opened file.");
      printLine("     *");
      printLine("     * @return          <code>true</code> if the file could be read;");
      printLine("     *                  <code>false</code> otherwise.");
      printLine("     *");
      printLine("     * @author Todd King");
      printLine("     * @author UCLA/IGPP");
      printLine("     * @since           1.0");
      printLine("     **/");
      printLine("    public boolean parseXML(InputStream stream)  ");
      printLine("       throws Exception");
      printLine("    {");
      printLine("        DocumentBuilderFactory factory =");
      printLine("            DocumentBuilderFactory.newInstance();");
      printLine("");
      printLine("        // Configure parser");
      printLine("        factory.setValidating(false);   ");
      printLine("        factory.setNamespaceAware(false);");
      printLine("        factory.setIgnoringComments(false);");
      printLine("        factory.setCoalescing(true);");
      printLine("");
      printLine("        DocumentBuilder builder = factory.newDocumentBuilder();");
      printLine("        mDocument = builder.parse(stream);");
      printLine("");
      printLine("        return true;");
      printLine("    }");
      printLine("");
      printLine("    /** ");
      printLine("     * Generates an XML representation of the label and stream it to the ");
      printLine("     * print stream.");
      printLine("     * ");
      printLine("     * @param out     the stream to print the element to.");
      printLine("     *");
      printLine("     * @author Todd King");
      printLine("     * @author UCLA/IGPP");
      printLine("     * @since           1.0");
      printLine("     **/");
      printLine("    public void printXML(PrintStream out)  ");
      printLine("       throws Exception");
      printLine("    {");
      printLine("        //set up a transformer");
      printLine("        TransformerFactory transfac = TransformerFactory.newInstance();");
      printLine("        try { // Need for Java 1.5 to work properly");
      printLine("           transfac.setAttribute(\"indent-number\", new Integer(4));");
      printLine("        } catch(Exception ie) {");
      printLine("        }");
      printLine("        Transformer trans = transfac.newTransformer(getDefaultStyleSheet());");
      printLine("");
      printLine("        // Set up desired output format");
      printLine("        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, \"no\");");
      printLine("        trans.setOutputProperty(OutputKeys.INDENT, \"yes\");");
      printLine("");
      printLine("        // create string from xml tree");
      printLine("        StreamResult result = new StreamResult(out);");
      printLine("        DOMSource source = new DOMSource(mDocument);");
      printLine("");
      printLine("        trans.transform(source, result);");
      printLine("   }");
      printLine("");
      printLine("    /** ");
      printLine("     * Dump the CData sections of the XML");
      printLine("     * ");
      printLine("     * @param out     the stream to print the element to.");
      printLine("     *");
      printLine("     * @since           1.0");
      printLine("     **/");
      printLine("    public void dumpData(PrintStream out)");
      printLine("       throws Exception");
      printLine("    {");
      printLine("        Node node;");
      printLine("        DOMSource  source = new DOMSource(mDocument);");
      printLine("        ");
      printLine("        node = source.getNode();");
      printLine("        dumpNode(out, node);");
      printLine("    }");
      printLine("");
      printLine("    /** ");
      printLine("     * Dump the nodes of the XML");
      printLine("     * ");
      printLine("     * @param out     the stream to print the element to.");
      printLine("     *");
      printLine("     * @author Todd King");
      printLine("     * @author UCLA/IGPP");
      printLine("     * @since           1.0");
      printLine("     **/");
      printLine("    public void dumpNode(PrintStream out, Node node)");
      printLine("    {");
      printLine("       Node  child;");
      printLine("       Node  sibling;");
      printLine("       out.println(node.getNodeType() + \": \" + node.getNodeName() + \": \" + node.getNodeValue());");
      printLine("       if(node.hasChildNodes()) { // Output all children");
      printLine("          child = node.getFirstChild();");
      printLine("          sibling = child;");
      printLine("          while(sibling != null) {");
      printLine("             dumpNode(out, sibling);");
      printLine("             sibling = sibling.getNextSibling();");
      printLine("          }");
      printLine("       }");
      printLine("    }");
      printLine("");
      printLine("   /** ");
      printLine("    * Find the next node with the given name.");
      printLine("    *");
      printLine("    * @param node       the {@link Node} to start at.");
      printLine("    * @param name       the name of the node to look for.");
      printLine("    *");
      printLine("    * @author Todd King");
      printLine("    * @author UCLA/IGPP");
      printLine("    * @since           1.0");
      printLine("    **/");
      printLine("   public Node findNode(Node node, String name)");
      printLine("       throws Exception");
      printLine("   {");
      printLine("       String   buffer;");
      printLine("       Node  child;");
      printLine("       Node  sibling;");
      printLine("");
      printLine("       if(node == null) return null;	// Nothing to search");
      printLine("");
      printLine("       // Process all children");
      printLine("       if(node.hasChildNodes()) { // Output all children");
      printLine("          child = node.getFirstChild();");
      printLine("          sibling = child;");
      printLine("          while(sibling != null) {");
      printLine("             buffer = sibling.getNodeName();");
      printLine("             if(buffer == null) continue;");
      printLine("             // Elements in this object");
      printLine("             if(isMatch(name, buffer)) return sibling;");
      printLine("");
      printLine("             // Next node");
      printLine("             sibling = sibling.getNextSibling();");
      printLine("          }");
      printLine("       }");
      printLine("       return null;	// Not found");
      printLine("   }");
      printLine("");
      printLine("	 /** ");
      printLine("     * Obtain a StreamSource to the default XML Style Sheet.");
      printLine("     * ");
      printLine("     * @return          a {@link StreamSource} which can be used to read the default");
      printLine("     *             style sheet.");
      printLine("     *");
      printLine("     * @since           1.0");
      printLine("     **/");
      printLine("    static public StreamSource getDefaultStyleSheet()");
      printLine("    {");
      printLine("       StringReader reader = new StringReader(");
      printLine("         \"<!DOCTYPE stylesheet [\"");
      printLine("          + \"   <!ENTITY cr \\\"<xsl:text> </xsl:text>\\\">\"");
      printLine("          + \"]>\"");
      printLine("          + \"<xsl:stylesheet\"");
      printLine("          + \"   xmlns:xsl=\\\"http://www.w3.org/1999/XSL/Transform\\\"\"");
      printLine("          + \"   xmlns:xalan=\\\"http://xml.apache.org/xslt\\\"\"");
      printLine("          + \"   version=\\\"1.0\\\"\"");
      printLine("          + \">\"");
      printLine("          + \"\"");
      printLine("          + \"<xsl:output method=\\\"xml\\\" indent=\\\"yes\\\" xalan:indent-amount=\\\"4\\\"/>\"");
      printLine("          + \"\"");
      printLine("          + \"<!-- copy out the xml -->\"");
      printLine("          + \"<xsl:template match=\\\"* | @*\\\">\"");
      printLine("          + \"   <xsl:copy><xsl:copy-of select=\\\"@*\\\"/><xsl:apply-templates/></xsl:copy>\"");
      printLine("          + \"</xsl:template>\"");
      printLine("          + \"</xsl:stylesheet>\"");
      printLine("       );");
      printLine("       ");
      printLine("       return new StreamSource(reader);");
      printLine("    }");
      printLine("");
      printLine("    /** ");
      printLine("     * Obtain the Document containing the representation of the parsed XML files.");
      printLine("     * ");
      printLine("     * @return          a {@link Document} containing the representation of the parsed XML file.");
      printLine("     *");
      printLine("     * @author Todd King");
      printLine("     * @author UCLA/IGPP");
      printLine("     * @since           1.0");
      printLine("     **/");
      printLine("    public org.w3c.dom.Document getDocument()");
      printLine("    {");
      printLine("       return mDocument;");
      printLine("    }");
      printLine("");
      printLine("    /** ");
      printLine("     * Walk the nodes of the Document and populate the resource classes.");
      printLine("     * The name of each element encountered is used to find a method");
      printLine("     * to store the value. The method search for has the element name");
      printLine("     * with the prefix \"set\" and an parameter of type \"{@link String}\". ");
      printLine("     * If such a method is found the text contained by the");
      printLine("     * element tags is passed to the method. Then a method with the prefix of \"set\" is");
      printLine("     * with a parameter of of type \"{@link Node}\" is attempted. If such a method is found ");
      printLine("     * then method is called with the {@link Node} representing the element is passed as the argument. ");
      printLine("     * ");
      printLine("     * @param node       the {@link Node}.");
      printLine("     *");
      printLine("     * @author Todd King");
      printLine("     * @author UCLA/IGPP");
      printLine("     * @since           1.0");
      printLine("     **/");
      printLine("    public void processNode(Node node)");
      printLine("       throws Exception");
      printLine("    {");
      printLine("       String   name;");
      printLine("       Node  child;");
      printLine("       Node  sibling;");
      printLine("       Method   method;");
      printLine("");
      printLine("       if(node == null) return;");
      printLine("");
      printLine("       // Process all children");
      printLine("       if(node.hasChildNodes()) { // Output all children");
      printLine("          child = node.getFirstChild();");
      printLine("          sibling = child;");
      printLine("          while(sibling != null) {");
      printLine("             name = sibling.getNodeName();");
      printLine("             name = igpp.util.Text.toProperCase(name);");
      printLine("             if(name == null) continue;");
      printLine("");
      printLine("             switch(sibling.getNodeType()) {");
      printLine("             case Node.ELEMENT_NODE:");
      printLine("                // Check if element is supported.");
      printLine("                setMember(name, getNodeText(sibling));");
      printLine("                setMember(name, sibling);");
      printLine("             }");
      printLine("");
      printLine("             // Next node      ");
      printLine("             sibling = sibling.getNextSibling();");
      printLine("          }");
      printLine("       }");
      printLine("    }");
      printLine("");
      printLine("    /** ");
      printLine("     * Call the set() method with a given name suffix and a {@link String}");
      printLine("     * as an argument.");
      printLine("     *");
      printLine("     * @param value      the {@link String} value to set.");
      printLine("     *");
      printLine("     * @author Todd King");
      printLine("     * @author UCLA/IGPP");
      printLine("     * @since           1.0");
      printLine("     **/");
      printLine("    public String setMember(String name, String value)");
      printLine("    {");
      printLine("       String      member;");
      printLine("       String      methodName = \"\";");
      printLine("       Object[] passParam = new Object[1];");
      printLine("       Method   method;");
      printLine("       XMLParser   parent;");
      printLine("");
      printLine("       parent = getMemberParent(name);");
      printLine("       member = getMemberName(name);");
      printLine("       if(parent == null) return \"No parent\";");
      printLine("");
      printLine("       try {");
      printLine("          // Signature and parameters for \"set\" methods");
      printLine("          Class[]  argSig = new Class[1];");
      printLine("          argSig[0] = Class.forName(\"java.lang.String\");");
      printLine("");
      printLine("          methodName = \"set\" + igpp.util.Text.toProperCase(member);");
      printLine("             method = parent.getClass().getMethod(methodName, argSig);");
      printLine("          passParam[0] = value;            ");
      printLine("             method.invoke(parent, passParam);");
      printLine("       } catch(Exception e) {");
      printLine("          // Ignore that the method doesn't exist");
      printLine("          return \"error using class: \" + parent.getClassName() + \" and method name \" + methodName;");
      printLine("       }        ");
      printLine("       return parent.getClassName();");
      printLine("    }");
      printLine("");
      printLine("	/** ");
      printLine("	* Call the set() method with a given name suffix and an array of {@link String}");
      printLine("	* values as an argument.");
      printLine("	*");
      printLine("	* @param value      the {@link String} value to set.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public void setMember(String name, String[] value)");
      printLine("	{");
      printLine("		String      member;");
      printLine("		String      methodName;");
      printLine("		Object[] passParam = new Object[1];");
      printLine("		Method      method;");
      printLine("		XMLParser   parent;");
      printLine("		java.lang.String[]   base = new java.lang.String[1];");
      printLine("		");
      printLine("		parent = getMemberParent(name);");
      printLine("		member = getMemberName(name);");
      printLine("		if(parent == null) return;");
      printLine("		");
      printLine("		try {");
      printLine("			// Signature and parameters for \"set\" methods");
      printLine("			Class[]  argSig = new Class[1];");
      printLine("			argSig[0] = base.getClass();");
      printLine("			");
      printLine("			methodName = \"set\" +  igpp.util.Text.toProperCase(member);");
      printLine("			method = parent.getClass().getMethod(methodName, argSig);");
      printLine("			passParam[0] = value;            ");
      printLine("			method.invoke(parent, passParam);");
      printLine("		} catch(Exception e) {");
      printLine("			e.printStackTrace();");
      printLine("			// Ignore that the method doesn't exist");
      printLine("		}        ");
      printLine("    }");
      printLine("     ");
      printLine("	/** ");
      printLine("	* Call the set() method with a given name suffix and a DOM {@link Node}");
      printLine("	* as an argument.");
      printLine("	*");
      printLine("	* @param name       the name of the member (suffix for setXXX() method).");
      printLine("	* @param value      the {@link String} value to set.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("   public void setMember(String name, Node value)");
      printLine("   {");
      printLine("		String      member;");
      printLine("		String      methodName;");
      printLine("		Object[] passParam = new Object[1];");
      printLine("		Method   method;");
      printLine("		XMLParser   parent;");
      printLine("		");
      printLine("		parent = getMemberParent(name);");
      printLine("		member = getMemberName(name);");
      printLine("		if(parent == null) return;");
      printLine("		");
      printLine("		// Signature and parameters for \"set\" methods");
      printLine("		try {");
      printLine("			Class[]  argSig = new Class[1];");
      printLine("			argSig[0] = Class.forName(\"org.w3c.dom.Node\");");
      printLine("			");
      printLine("			methodName = \"set\" + igpp.util.Text.toProperCase(member);");
      printLine("			method = parent.getClass().getMethod(methodName, argSig);");
      printLine("			passParam[0] = value;            ");
      printLine("			method.invoke(parent, passParam);");
      printLine("		} catch(Exception e) {");
      printLine("			// Ignore that the method doesn't exist");
      printLine("		}        ");
      printLine("	}");
      printLine("");
      printLine("	/** ");
      printLine("	* Find the parent class for a member.");
      printLine("	* The syntax is [parent/]member");
      printLine("	* where \"parent/\" can have multiple levels.");
      printLine("	* If \"parent\" is blank, then its self-referential.");
      printLine("	* ");
      printLine("	* ");
      printLine("	* @param node       the {@link Node}.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public XMLParser getMemberParent(String name)");
      printLine("	{");
      printLine("		XMLParser parent = this;");
      printLine("		String[] part = name.split(\"/\", 2);");
      printLine("		if(part.length > 1 && part[0].length() > 0) {");
      printLine("			parent = getMemberNode(part[0]);");
      printLine("			if(parent != null) parent = parent.getMemberParent(part[1]);");
      printLine("		}");
      printLine("		return parent;");
      printLine("	}");
      printLine("      ");
      printLine(" /** ");
      printLine("	* Find the top level parent class for a member.");
      printLine("	* The syntax is [parent/]member");
      printLine("	* where \"parent/\" can have multiple levels.");
      printLine("	* If \"parent\" is blank, then its self-referential.");
      printLine("	* ");
      printLine("	* @param name       the XPath name for for the member.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public XMLParser getTopParent(String name)");
      printLine("	{");
      printLine("		XMLParser parent = this;");
      printLine("		String[] part = name.split(\"/\", 2);");
      printLine("		if(part.length > 1 && part[0].length() > 0) {");
      printLine("			parent = getMemberNode(part[0]);");
      printLine("		}");
      printLine("		return parent;");
      printLine("	}");
      printLine("      ");
      printLine("	/** ");
      printLine("	* Extract the path to a member from an XPath string.");
      printLine("	* The path is the text preceeding the last");
      printLine("	* node delimiter.");
      printLine("	* The syntax is [parent/]member");
      printLine("	* ");
      printLine("	* @param name	The full path name of the node.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public String getMemberPath(String name)");
      printLine("	{");
      printLine("		int n;");
      printLine("		");
      printLine("		n = name.lastIndexOf(\"/\");");
      printLine("		if(n == -1) return \"\";");
      printLine("		");
      printLine("		return name.substring(0, n);");
      printLine("	}");
      printLine("      ");
      printLine("	/** ");
      printLine("	* Extract the member name from an XPath string.");
      printLine("	* The member name is the text following the last");
      printLine("	* node delimiter.");
      printLine("	* The syntax is [parent.]member");
      printLine("	* ");
      printLine("	* ");
      printLine("	* @param node       the {@link Node}.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public String getMemberName(String name)");
      printLine("	{");
      printLine("		int n;");
      printLine("		");
      printLine("		n = name.lastIndexOf(\"/\");");
      printLine("		if(n == -1) return name;");
      printLine("		");
      printLine("		return name.substring(n+1);");
      printLine("	}");
      printLine("      ");
      printLine("	/** ");
      printLine("	* Locates a node in a structure based on the the \"name\" associated ");
      printLine("	* with the member. A \"node\" is a member of a class which also has");
      printLine("	* members. To be implemented in each derived class.");
      printLine("	* A name may have an option index. The full syntax is:");
      printLine("	* \"name[index]\"");
      printLine("	*");
      printLine("	* @param name       the name associated with a member.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public XMLParser getMemberNode(String name)");
      printLine("	{");
      printLine("		return null;");
      printLine("	}");
      printLine("      ");
      printLine("	/** ");
      printLine("	* Extracts the name portion of node reference.");
      printLine("	* A node reference has the syntax \"name[index]\".");
      printLine("	* The index (\"[index]\") is optional.");
      printLine("	*");
      printLine("	* @param name       the name associated with a member.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public String getMemberNodeName(String name)");
      printLine("	{");
      printLine("		int n;");
      printLine("		");
      printLine("		n = name.lastIndexOf(\"[\");");
      printLine("		if(n == -1) return name;");
      printLine("		");
      printLine("		return name.substring(0, n);");
      printLine("	}");
      printLine("      ");
      printLine("	/** ");
      printLine("	* Extracts the index portion of node reference.");
      printLine("	* A node reference has the syntax \"name[index]\".");
      printLine("	* The index (\"[index]\") is optional. The default ");
      printLine("	* index is zero.");
      printLine("	*");
      printLine("	* @param name       the name associated with a member.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public int getMemberNodeIndex(String name)");
      printLine("	{");
      printLine("		int n;");
      printLine("		int   j;");
      printLine("		");
      printLine("		n = name.indexOf(\"[\");");
      printLine("		if(n == -1) return 0;");
      printLine("		");
      printLine("		j = name.indexOf(\"]\");");
      printLine("		if(j == -1) j = name.length();");
      printLine("		");
      printLine("		return Integer.parseInt(name.substring(n+1, j));");
      printLine("	}");
      printLine("      ");
      printLine("	/** ");
      printLine("	* Concatenate all the text under a node.");
      printLine("	* ");
      printLine("	* @param node       the {@link Node}.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	static public String getNodeText(Node node)");
      printLine("	{");
      printLine("		Node  child;");
      printLine("		Node  sibling;");
      printLine("		String   buffer = \"\";");
      printLine("		");
      printLine("		if(node.hasChildNodes()) { // Output all children");
      printLine("			child = node.getFirstChild();");
      printLine("			sibling = child;");
      printLine("			while(sibling != null) {");
      printLine("				if(sibling.getNodeType() == Node.TEXT_NODE");
      printLine("				|| sibling.getNodeType() == Node.CDATA_SECTION_NODE) {");
      printLine("					 buffer += sibling.getNodeValue();");
      printLine("				}");
      printLine("			sibling = sibling.getNextSibling();");
      printLine("			}");
      printLine("		}");
      printLine("		");
      printLine("		return entityEncode(buffer);");
      printLine("	}");
      printLine(" ");
      printLine(" ");
      printLine("	/** ");
      printLine("	* Concatenate all the text under a node.");
      printLine("	* ");
      printLine("	* @param node       the {@link Node}.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	static public String getBranchText(Node node)");
      printLine("	throws Exception");
      printLine("	{");
      printLine("		Node  child;");
      printLine("		StringBuffer   buffer;");
      printLine("		int      n;");
      printLine("		");
      printLine("		if(!node.hasChildNodes()) return \"\";");
      printLine("		");
      printLine("		child = node.getFirstChild();");
      printLine("		StringWriter   writer = new StringWriter();");
      printLine("		");
      printLine("		TransformerFactory transfac = TransformerFactory.newInstance();");
      printLine("		// Needed for Java 1.5 to work properly");
      printLine("		transfac.setAttribute(\"indent-number\", new Integer(4));");
      printLine("		Transformer trans = transfac.newTransformer(getDefaultStyleSheet());");
      printLine("		");
      printLine("		// Set up desired output format");
      printLine("		trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, \"yes\");");
      printLine("		trans.setOutputProperty(OutputKeys.INDENT, \"yes\");");
      printLine("		");
      printLine("		// create string from xml tree");
      printLine("		StreamResult result = new StreamResult(writer);");
      printLine("		DOMSource source = new DOMSource(node);");
      printLine("		");
      printLine("		trans.transform(source, result);");
      printLine("		");
      printLine("		// Strip node tags - we want just content");
      printLine("		buffer = writer.getBuffer();");
      printLine("		n = buffer.indexOf(\">\");");
      printLine("		if(n != -1) buffer = buffer.delete(0, n+1);");
      printLine("		");
      printLine("		n = buffer.lastIndexOf(\"<\");");
      printLine("		if(n != -1) buffer = buffer.delete(n, buffer.length());");
      printLine("		");
      printLine("		return buffer.toString();");
      printLine("	}");
      printLine(" ");
      printLine("	/**");
      printLine("	* Return the pathname of the parsed document");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public String getPathName() { return mPathName; }");
      printLine("     ");
      printLine("	/**");
      printLine("	* Return the path to the parsed document");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public String getPath() ");
      printLine("	{ ");
      printLine("		if(mPathName.length() == 0) return \"\";");
      printLine("		");
      printLine("		try {");
      printLine("			File file = new File(mPathName); ");
      printLine("			return file.getParent();");
      printLine("		} catch(Exception e) {");
      printLine("		}");
      printLine("		return \"\";");
      printLine("	}");
      printLine("     ");
      printLine("	/**");
      printLine("	* Return a string with content enclosed in tags with the passed name.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public String makeTagContent(String tag, String content) { return \"<\" + tag + \">\" + content + \"</\" + tag + \">\\n\"; }");
      printLine("    ");
      printLine("	/**");
      printLine("	 * Return a string with spaces to format to indicated indentation level.");
      printLine("	 *");
      printLine("	 * @author Todd King");
      printLine("	 * @author UCLA/IGPP");
      printLine("	 * @since           1.0");
      printLine("	**/");
      printLine("	static public String indent(int level) ");
      printLine("	{ ");
      printLine("		String buffer = \"\"; ");
      printLine("		for(int i = 0; i < level; i++) buffer += \"   \"; ");
      printLine("		return buffer; ");
      printLine("	}");
      printLine("    ");
      printLine("	/**");
      printLine(" 	 * Prints the XML document to the currently define System.out.");
      printLine(" 	 * Each node is labeled with its XPath.");
      printLine("	 *");
      printLine("	 * @author Todd King");
      printLine("	 * @author UCLA/IGPP");
      printLine("	 * @since           1.0");
      printLine("	**/");
      printLine("	public void dump() ");
      printLine("	{ ");
      printLine("		ArrayList<Pair> list = getXPathPairs();");
      printLine("		Pair<String, String> pair;");
      printLine("		");
      printLine("		if(list == null) return;");
      printLine("		");
      printLine("		for(int i = 0; i < list.size(); i++) {");
      printLine("			pair = list.get(i);");
      printLine("			System.out.println(pair.getLeft() + \": \" + pair.getRight());");
      printLine("		}");
      printLine("	}");
      printLine("");
      printLine("	/**");
      printLine(" 	 * Return an ArrayList of string Pairs which contains for");
      printLine(" 	 * each node in the XML document an XPath as the \"left\" value");
      printLine(" 	 * of the pair and the value as the \"right\" value of pair.");
      printLine("	 *");
      printLine("	 * @author Todd King");
      printLine("	 * @author UCLA/IGPP");
      printLine("	 * @since           1.0");
      printLine("	**/");
      printLine("	public ArrayList<Pair> getXPathPairs() { return getXPathPairs(\"\", 0); }");
      printLine("	");
      printLine("	/**");
      printLine(" 	 * Return an ArrayList of string Pairs which contains for");
      printLine(" 	 * each node in the XML document an XPath as the \"left\" value");
      printLine(" 	 * of the pair and the value as the \"right\" value of pair.");
      printLine(" 	 * The Prefix will be added to each path. If index is other than");
      printLine(" 	 * zero then the prefix will be modified to include the index.");
      printLine("	 *");
      printLine("	 * @param prefix     the leading path to add to each XPath.");
      printLine("	 * @param index		the index of the item if part of an array.");
      printLine("	 *                   Indexes start a 1, a value of 0 indicates it is");
      printLine("	 *                   not part of an array. ");
      printLine("	 *");
      printLine("	 * @author Todd King");
      printLine("	 * @author UCLA/IGPP");
      printLine("	 * @since           1.0");
      printLine("	**/");
      printLine("	public ArrayList<Pair> getXPathPairs(String prefix, int index) { return null; }");
      printLine("	");
      printLine("	/**");
      printLine(" 	 * Return a string containing an XML representation of this instance.");
      printLine("	 * To be implemented in each derived class.");
      printLine("	 *");
      printLine("	 * @param n       the number of levels to indent the document.");
      printLine("	 *");
      printLine("	 * @author Todd King");
      printLine("	 * @author UCLA/IGPP");
      printLine("	 * @since           1.0");
      printLine("	**/");
      printLine("	public String getXMLDocument(int n) { return getXMLDocument(n, null, 0, false); }");
      printLine("");
      printLine("	/**");
      printLine(" 	 * Return a string containing an XML representation of this instance.");
      printLine("	 * To be implemented in each derived class.");
      printLine("	 *");
      printLine("	 * @param n       the number of levels to indent the document.");
      printLine("	 * @param inUseOnly	indicates whether to include only those elements currently in use.");
      printLine("	 *");
      printLine("	 * @author Todd King");
      printLine("	 * @author UCLA/IGPP");
      printLine("	 * @since           1.0");
      printLine("	**/");
      printLine("	public String getXMLDocument(int n, boolean inUseOnly) { return getXMLDocument(n, null, 0, inUseOnly); }");
      printLine("");
      printLine("	/**");
      printLine("	* Return a string containing an XML representation of this isntance.");
      printLine("	* To be implemented in each derived class.");
      printLine("	*");
      printLine("	* @param n       the number of levels to indent the document.");
      printLine("	* @param path    the path to the element.");
      printLine("	* @param key     the index (key) of the element at the path.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public String getXMLDocument(int n, String path, int key) { return getXMLDocument(n, path, key, false); }");
      printLine("");
      printLine("	/**");
      printLine("	* Return a string containing an XML representation of this isntance.");
      printLine("	* To be implemented in each derived class.");
      printLine("	*");
      printLine("	* @param n       the number of levels to indent the document.");
      printLine("	* @param path    the path to the element.");
      printLine("	* @param key     the index (key) of the element at the path.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public String getXMLDocument(int n, String path, int key, boolean inUseOnly) { return \"\"; }");
      printLine("");
      printLine("	/**");
      printLine("	* Create a new member. ");
      printLine("	* To be implemented in each derived class.");
      printLine("	*");
      printLine("	* @param name     the name of the member to create.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public void makeNewMember(String name)  { return; }");
      printLine("    ");
      printLine("	/**");
      printLine("	* Create an instance of all nodes for use when editing. ");
      printLine("	* To be implemented in each derived class.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public void makeEditNodes()  { return; }");
      printLine("    ");
      printLine("    ");
      printLine("	/**");
      printLine("	* Create all nodes for use when editing which currently");
      printLine(" * do not exist. ");
      printLine("	* To be implemented in each derived class.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public void makeSkeletonNodes()  { return; }");
      printLine("    ");
      printLine("	/**");
      printLine("	* Remove an elment of a member. ");
      printLine("	* To be implemented in each derived class.");
      printLine("	*");
      printLine("	* @param name       the name of the member to create.");
      printLine("	* @param index      the index of the element to remove.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public void removeMember(String name, int index)  { return; }");
      printLine("    ");
      printLine("	/**");
      printLine("	* Return the list of nodes of a member given the XPath like reference to the member.");
      printLine("	* For example, if you want a list of \"AccessURL\" nodes in the");
      printLine("	* \"ResourceHeader\" then the path would be \"ResourceHeader.AccessURL\"");
      printLine("	* <p>");
      printLine("	* Only returns nodes for items that are nodes.");
      printLine("	* <p>");
      printLine("	* Always returns a list with at least one entry. ");
      printLine("	* If no nodes are found then the returned entry is blank.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public ArrayList<XMLParser> getNodes(String path)");
      printLine("	{");
      printLine("		return getNodes(path, true);");
      printLine("	}");
      printLine(" ");
      printLine("	/**");
      printLine("	* Return the list of nodes of a member given the XPath like reference to the member.");
      printLine("	* For example, if you want a list of \"AccessURL\" nodes in the");
      printLine("	* \"ResourceHeader\" then the path would be \"ResourceHeader.AccessURL\"");
      printLine("	* <p>");
      printLine("	* Only returns nodes for items that are nodes.");
      printLine("	* <p>");
      printLine("	* If no nodes are found then the returned list is empty");
      printLine("	* unless alwaysOne is set to true.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public ArrayList<XMLParser> getNodes(String path, boolean alwaysOne)");
      printLine("	{");
      printLine("		String      member;");
      printLine("		String      name;");
      printLine("		XMLParser   parent;");
      printLine("		ArrayList<XMLParser> list;");
      printLine("		");
      printLine("		parent = getMemberParent(path);");
      printLine("		member = getMemberName(path);");
      printLine("		name = getMemberNodeName(member);");
      printLine("		");
      printLine("		list = getNodes(parent, name);");
      printLine("		");
      printLine("		if(alwaysOne) {");
      printLine("			try {");
      printLine("				if(list.size() == 0) list.add(parent.getClass().newInstance());");
      printLine("			} catch(Exception e) {");
      printLine("				// Ignore errors");
      printLine("			}");
      printLine("		}");
      printLine("		");
      printLine("		return list;");
      printLine("	}");
      printLine("");
      printLine("	/**");
      printLine("	* Return the list of nodes given the name of the member.");
      printLine("	* <p>");
      printLine("	* Only returns nodes for items that are nodes.");
      printLine("	* <p>");
      printLine("	* If no nodes are found then the returned list is empty.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public ArrayList<XMLParser> getNodes(XMLParser parent, String name)");
      printLine("	{");
      printLine("		String   methodName = \"\";");
      printLine("		Method   method;");
      printLine("		Object   response;");
      printLine("		ArrayList value;");
      printLine("		ArrayList<XMLParser> list = new ArrayList<XMLParser>();");
      printLine("		XMLParser	item;");
      printLine("		");
      printLine("		if(parent == null) return list;");
      printLine("	");
      printLine("		try {");
      printLine("			// Signature and parameters for \"get\" method");
      printLine("			methodName = \"get\" + igpp.util.Text.toProperCase(name);");
      printLine("			method = parent.getClass().getMethod(methodName);");
      printLine("			response = method.invoke(parent);");
      printLine("			if(response instanceof XMLParser) { ");
      printLine("				item = (XMLParser) response; ");
      printLine("				list.add(item);");
      printLine("			}");
      printLine("			if(response instanceof ArrayList) { ");
      printLine("				value = (ArrayList) response;");
      printLine("				if(value.size() > 0 && value.get(0) instanceof XMLParser) {");
      printLine("					for(int i = 0; i < value.size(); i++) {");
      printLine("						item = (XMLParser) value.get(i);");
      printLine("						list.add(item);");
      printLine("					}");
      printLine("				}");
      printLine("			}");
      printLine("		} catch(Exception e) {");
      printLine("			// Ignore that the method doesn't exist");
      printLine("			// return \"error using class: \" + parent.getClassName() + \" and method name \" + methodName;");
      printLine("		}");
      printLine("		return list;");
      printLine("	}");
      printLine("     ");
      printLine(" /**");
      printLine("  * Return the list of values of a member given the XPath like reference to the member.");
      printLine("  * The XPath is a canonical path which includes node names only (no array indexes)");
      printLine("  * For example, if you want all instances of the \"URL\" in \"AccessURL\" under");
      printLine("  * \"ResourceHeader\" then the path would be \"ResourceHeader/AccessURL/URL\"");
      printLine("  * <p>");
      printLine("  * Only returns values for items that return {@link String} values.");
      printLine("  * <p>");
      printLine("  * Always returns a list with at least one entry. ");
      printLine("  * If no nodes are found then the returned entry is blank.");
      printLine("  *");
      printLine("  * @author Todd King");
      printLine("  * @author UCLA/IGPP");
      printLine("  * @since           1.0");
      printLine("  **/");
      printLine(" public ArrayList<String> getAllValues(String path)");
      printLine(" {");
      printLine("    XMLParser   item;");
      printLine("    ArrayList<XMLParser>   parent;");
      printLine("    ArrayList<String> value = new ArrayList<String>();");
      printLine("    ");
      printLine("    String[] part = path.split(\"/\", 2);");
      printLine("    if(part.length > 1 && part[0].length() > 0) {	// If a parent and its a node, then run on decendent");
      printLine("       parent = getNodes(part[0], false);");
      printLine("       if(parent.size() != 0) {	// Process each node");
      printLine("	       for(int i = 0; i < parent.size(); i++) {");
      printLine("	       	item = parent.get(i);");
      printLine("	       	value.addAll(item.getAllValues(part[1]));");
      printLine("	       }");
      printLine("	       return value;");
      printLine("       }");
      printLine("    }");
      printLine("    ");
      printLine("    // If we reach here then we have a member to process");
      printLine("    value.addAll(getValues(path, false));");
      printLine("    return value;");
      printLine("  }");
      printLine("");
      printLine(" /**");
      printLine("  * Return the list of values of a member given the XPath like reference to the member.");
      printLine("  * For example, if you want the \"URL\" in the second \"AccessURL\" item in the");
      printLine("  * \"ResourceHeader\" then the path would be \"ResourceHeader/AccessURL[1]/URL\"");
      printLine("  * <p>");
      printLine("  * Only returns values for items that return {@link String} values.");
      printLine("  * <p>");
      printLine("  * Always returns a list with at least one entry. ");
      printLine("  * If no nodes are found then the returned entry is blank.");
      printLine("  *");
      printLine("  * @author Todd King");
      printLine("  * @author UCLA/IGPP");
      printLine("  * @since           1.0");
      printLine("  **/");
      printLine(" public ArrayList<String> getValues(String path)");
      printLine(" {");
      printLine(" 	return getValues(path, true);");
      printLine(" }");
      printLine(" ");
      printLine(" /**");
      printLine("  * Return the list of values of a member given the XPath like reference to the member.");
      printLine("  * For example, if you want the \"URL\" in the second \"AccessURL\" item in the");
      printLine("  * \"ResourceHeader\" then the path would be \"ResourceHeader/AccessURL[1]/URL\"");
      printLine("  * <p>");
      printLine("  * Only returns values for items that return {@link String} values.");
      printLine("  * <p>");
      printLine("  * If no nodes are found then the returned list is empty");
      printLine("  * unless alwaysOne is set to true.");
      printLine("  *");
      printLine("  * @author Todd King");
      printLine("  * @author UCLA/IGPP");
      printLine("  * @since           1.0");
      printLine("  **/");
      printLine(" public ArrayList<String> getValues(String path, boolean alwaysOne)");
      printLine(" {");
      printLine("    String      member;");
      printLine("    String      methodName = \"\";");
      printLine("    Object   response;");
      printLine("    Method   method;");
      printLine("    XMLParser   parent;");
      printLine("    ArrayList<String> value = new ArrayList<String>();");
      printLine("    ArrayList   list;");
      printLine("    String      buffer;");
      printLine("    ");
      printLine("     parent = getMemberParent(path);");
      printLine("     member = getMemberName(path);");
      printLine("     if(parent == null) { value.add(\"\"); return value; }");
      printLine("    ");
      printLine("     try {");
      printLine("        // Signature and parameters for \"get\" method");
      printLine("        methodName = \"get\" + igpp.util.Text.toProperCase(member);");
      printLine("        method = parent.getClass().getMethod(methodName);");
      printLine("        response = method.invoke(parent);");
      printLine("        if(response instanceof String) { ");
      printLine("          buffer = (String) response; ");
      printLine("          value.add(buffer);");
      printLine("        }");
      printLine("        if(response instanceof ArrayList) { ");
      printLine("          list = (ArrayList) response;");
      printLine("          if(list.size() > 0 && list.get(0) instanceof String) {");
      printLine("             for(int i = 0; i < list.size(); i++) {");
      printLine("                buffer = (String) list.get(i);");
      printLine("                value.add(buffer);");
      printLine("             }");
      printLine("          } ");
      printLine("        }");
      printLine("     } catch(Exception e) {");
      printLine("        // Ignore that the method doesn't exist");
      printLine("        // return \"error using class: \" + parent.getClassName() + \" and method name \" + methodName;");
      printLine("     }");
      printLine("     if(alwaysOne && value.size() == 0) value.add(\"\");");
      printLine("");
      printLine("     return value;");
      printLine("  }");
      printLine("");
      printLine("");
      printLine("	/**");
      printLine("	* Return a {@link StringReader} for an XML representation of the document.");
      printLine("	* Commonly used to pass the document through an XML stylesheet transformation");
      printLine("	* using {@link Transform}.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public StringReader getStringReader()");
      printLine("	{");
      printLine("		return new StringReader(getXMLDocument(0));");
      printLine("	}");
      printLine("	");
      printLine("	/**");
      printLine("	* Return a value enclosed in a tag and properly indented.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public String getTaggedValue(int level, String tagName, String value)");
      printLine("	{");
      printLine("	 	return indent(level) + \"<\" + tagName + \">\" + value + \"</\" + tagName + \">\\n\";");
      printLine("	}");
      printLine("       ");
      printLine("	/**");
      printLine("	* Return a value enclosed in a tag and properly indented. If inUseOnly is true then");
      printLine("	* only those elements that are required or have values defined are returned.");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("	public String getTaggedValue(int level, String tagName, String value, boolean inUseOnly)");
      printLine("	{");
      printLine("		if(!inUseOnly) return getTaggedValue(level, tagName, value);");
      printLine("		");
      printLine("		if(isInUse(tagName, value)) {");
      printLine("		 	return getTaggedValue(level, tagName, value);");
      printLine("		}");
      printLine("		");
      printLine("		return \"\";");
      printLine("	}");
      printLine("       ");
      printLine("	/**");
      printLine("	 * Return an open tag with the proper indentation.");
      printLine("	 *");
      printLine("	 * @author Todd King");
      printLine("	 * @author UCLA/IGPP");
      printLine("	 * @since           1.0");
      printLine("	**/");
      printLine("	public String getTagOpen(int level, String tagName)");
      printLine("	{");
      printLine("		return indent(level) + \"<\" + tagName + \">\\n\";");
      printLine("	}");
      printLine("       ");
      printLine("	/**");
      printLine("	 * Return an close tag with the proper indentation.");
      printLine("	 *");
      printLine("	 * @author Todd King");
      printLine("	 * @author UCLA/IGPP");
      printLine("	 * @since           1.0");
      printLine("	**/");
      printLine("	public String getTagClose(int level, String tagName)");
      printLine("	{");
      printLine("		return indent(level) + \"</\" + tagName + \">\\n\";");
      printLine("	}");
      printLine("       ");
      printLine("	/**");
      printLine("	 * Return a list of tagged values, properly indented.");
      printLine("	 *");
      printLine("	 * @author Todd King");
      printLine("	 * @author UCLA/IGPP");
      printLine("	 * @since           1.0");
      printLine("	**/");
      printLine("	public String getTaggedList(int level, String tagName, ArrayList<String> list)");
      printLine("	{");
      printLine("		Iterator i;");
      printLine("		String   buffer = \"\";");
      printLine("		");
      printLine("		i = list.iterator();");
      printLine("		while(i.hasNext()) {");
      printLine("			buffer += getTaggedValue(level, tagName, (String) i.next());");
      printLine("		}");
      printLine("		return buffer;");
      printLine("	}");
      printLine(" ");
      printLine("	/**");
      printLine("	 * Return a list of tagged values, properly indented. If inUseOnly is true then");
      printLine("	 * only those elements that are required or have values defined are returned.");
      printLine("	 *");
      printLine("	 * @author Todd King");
      printLine("	 * @author UCLA/IGPP");
      printLine("	 * @since           1.0");
      printLine("	**/");
      printLine("	public String getTaggedList(int level, String tagName, ArrayList<String> list, boolean inUseOnly)");
      printLine("	{");
      printLine("		Iterator i;");
      printLine("		String   buffer = \"\";");
      printLine("		");
      printLine("		i = list.iterator();");
      printLine("		while(i.hasNext()) {");
      printLine("			buffer += getTaggedValue(level, tagName, (String) i.next(), inUseOnly);");
      printLine("		}");
      printLine("		return buffer;");
      printLine("	}");
      printLine(" ");
      printLine(" /**");
      printLine("  * Return a list of tagged values, properly indented.");
      printLine("  *");
      printLine("  * @author Todd King");
      printLine("  * @author UCLA/IGPP");
      printLine("     * @since           1.0");
      printLine("     **/");
      printLine("    public boolean isMatch(String base, String value) ");
      printLine("    {");
      printLine("       if(base == null) return false;");
      printLine("       ");
      printLine("       if(base.compareToIgnoreCase(value) == 0) return true;");
      printLine("       ");
      printLine("       return false;");
      printLine("    }");
      printLine("    ");
      printLine("	/**");
      printLine("	* Construct an abbreviated XPath for a a class node. A path has the form:");
      printLine("	*<br>");
      printLine("	*  parent/className[key]");
      printLine("	*<br>");
      printLine("	*where \"parent/\" and \"[key]\" is optional");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	**/");
      printLine("    public String getElementPath(String base, int key) ");
      printLine("    {");
      printLine("       String   path = \"\";");
      printLine("       String   delim = \"\";");
      printLine("       ");
      printLine("       if(key == -2) return base; // Don't alter base");
      printLine("       ");
      printLine("       if(base == null) return null;");
      printLine("       path = base;");
      printLine("       ");
      printLine("       if(base.length() > 0) delim = \"/\";");
      printLine("       // path += delim + toImproperCase(getClassName());");
      printLine("       path += delim + getClassName();");
      printLine("       ");
      printLine("       if(key != -1) path += \"[\" + key + \"]\";");
      printLine("       ");
      printLine("       return path;");
      printLine("    }");
      printLine("    ");
      printLine(" /**");
      printLine("  * Determine if a list is of the proper type and the ");
      printLine("  * index is within the range of the list.");
      printLine("  *");
      printLine("  * @author Todd King");
      printLine("  * @author UCLA/IGPP");
      printLine("  * @since           1.0");
      printLine("    **/");
      printLine(" public boolean isInList(String base, String value, ArrayList list, int index) ");
      printLine(" {");
      printLine("    if(index < 0) return false;");
      printLine("    if(base == null) return false;");
      printLine("    ");
      printLine("    if(base.compareToIgnoreCase(value) == 0 && index < list.size()) return true;");
      printLine("    ");
      printLine("    return false;");
      printLine(" }");
      printLine("");
      printLine(" /**");
      printLine("  * Determine if an item is in a string ArrayList. ");
      printLine("  *");
      printLine("  * @author Todd King");
      printLine("  * @author UCLA/IGPP");
      printLine("  * @since           1.0");
      printLine("    **/");
      printLine(" public boolean isInList(String value, ArrayList<String> list) ");
      printLine(" {");
      printLine("    if(value == null) return false;");
      printLine("    if(list == null) return false;");
      printLine("    ");
      printLine("    for(int i = 0; i < list.size(); i++) {");
      printLine("	    if(value.compareToIgnoreCase(list.get(i)) == 0) return true;");
      printLine("    }");
      printLine("    ");
      printLine("    return false;");
      printLine(" }");
      printLine("");
      printLine(" /**");
      printLine("  * Determine if any or all items in a String ArrayList ");
      printLine("  * is in a string ArrayList. ");
      printLine("  *");
      printLine("  * @param all		if true then all words in value must be in list.");
      printLine("  *					otherwise if any word is in list then true is returned.");
      printLine("  *");
      printLine("  * @author Todd King");
      printLine("  * @author UCLA/IGPP");
      printLine("  * @since           1.0");
      printLine("    **/");
      printLine(" public boolean isInList(ArrayList<String> value, ArrayList<String> list, boolean all) ");
      printLine(" {");
      printLine(" 	 boolean match = false;");
      printLine("    if(value == null) return false;");
      printLine("    if(list == null) return false;");
      printLine("    if(value.size() == 0) return true;");
      printLine("    ");
      printLine("    for(int i = 0; i < value.size(); i++) {");
      printLine("    	 match = isInList(value.get(i), list);");
      printLine("	    if(!match && all) return false;");
      printLine("	    if(match && !all) return true;");
      printLine("    }");
      printLine("    ");
      printLine("    return match;");
      printLine(" }");
      printLine("");
      printLine("	/**");
      printLine("	* Determine if a word is a common word. ");
      printLine("	*");
      printLine("	* @author Todd King");
      printLine("	* @author UCLA/IGPP");
      printLine("	* @since           1.0");
      printLine("	 **/");
      printLine("	public boolean isCommonWord(String value) ");
      printLine("	{");
      printLine("		if(value.length() < 2) return true;	// Any one-letter word is \"common\"");
      printLine("		if(Character.isDigit(value.charAt(0))) return true;	// Starts with a digit");
      printLine("		");
      printLine("		if(value.compareToIgnoreCase(\"an\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"and\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"are\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"at\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"but\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"by\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"for\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"he\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"in\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"is\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"it\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"our\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"own\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"of\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"on\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"or\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"she\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"the\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"that\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"to\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"we\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"was\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"you\") == 0) return true;");
      printLine("		if(value.compareToIgnoreCase(\"your\") == 0) return true;");
      printLine("		");
      printLine("		return false;");
      printLine("	}");
      printLine("");
      printLine(" 	/**");
      printLine("     * Convert a string to \"improper\" case (make first letter lower case).");
      printLine("	  **/");
      printLine("	 static public String toImproperCase(String value) ");
      printLine("	 {");
      printLine("	    String   buffer;");
      printLine("	    ");
      printLine("	    if(value == null) return value;");
      printLine("	    if(value.length() == 0) return value;");
      printLine("	    if(value.length() == 1) return value.toUpperCase();");
      printLine("	    ");
      printLine("	    buffer = value.substring(0, 1).toLowerCase() + value.substring(1);");
      printLine("	    ");
      printLine("	    return buffer;");
      printLine("	 }");
      printLine("	 ");
      printLine("   /** ");
      printLine("    * Parse a string into words and return a list of unique words.");
      printLine("    * Common words, such as \"a\", \"the\", etc., are not included in the list.");
      printLine("    * ");
      printLine("    * @author Todd King");
      printLine("    * @author UCLA/IGPP");
      printLine("    * @since           1.0");
      printLine("    **/");
      printLine("   public ArrayList<String> parseWords(String text)");
      printLine("   {");
      printLine("		String	delimiters = \"[ \\\\t\\\\.,;:?!@#$%(){}'\\\"/_]\";");
      printLine("		ArrayList<String>	words = new ArrayList<String>();");
      printLine("		String[]	split;");
      printLine("		");
      printLine("		String[]	part = text.split(delimiters);");
      printLine("		");
      printLine("		for(int i = 0; i < part.length; i++) {");
      printLine("			split = splitMixed(part[i]);	// Split Mixed case words");
      printLine("			for(int j = 0; j < split.length; j++) {");
      printLine("				if(isCommonWord(part[i])) continue;");
      printLine("				if(! isInList(part[i], words)) words.add(part[i]);");
      printLine("			}");
      printLine("		}");
      printLine("		");
      printLine("		return words;");
      printLine("   }");
      printLine("   ");
      printLine("   /**");
      printLine("    * Divide a string on capital letters that follow lowercase letters");
      printLine("    *	");
      printLine("    * @author Todd King");
      printLine("    * @author UCLA/IGPP");
      printLine("    * @since           1.0");
      printLine("    **/");
      printLine("   public String[] splitMixed(String text)");
      printLine("   {");
      printLine("   	boolean isLower = false;");
      printLine("   	int	n = 0;");
      printLine("   	char	c;");
      printLine("   	ArrayList<String> list = new ArrayList<String>();");
      printLine("   	");
      printLine("   	for(int i = 0; i < text.length(); i++) {");
      printLine("   		c = text.charAt(i);");
      printLine("   		if(isLower && Character.isUpperCase(c)) {");
      printLine("   			list.add(text.substring(n, i));");
      printLine("   			n = i;");
      printLine("   		}");
      printLine("   		if(Character.isLowerCase(c)) isLower = true;");
      printLine("   		else isLower = false;");
      printLine("   	}");
      printLine("   	if(n < text.length()) list.add(text.substring(n));");
      printLine("   	");
      printLine("   	return list.toArray(new String[0]);");
      printLine("   }");
      printLine("   ");
      printLine("   /** ");
      printLine("    * Walk the internals of a class and collect a unique list");
      printLine("    * of words from all String fields. By convention the first letter");
      printLine("    * of each member field starts with \"m\" so we drop the first letter");
      printLine("    * when using the get() utility methods (i.e., getValues())");
      printLine("    * We also exclude anything that ends with \"ID\" since there are");
      printLine("    * identifiers and have no language relevance.");
      printLine("    * ");
      printLine("    * @author Todd King");
      printLine("    * @author UCLA/IGPP");
      printLine("    * @since           1.0");
      printLine("    **/");
      printLine("   public ArrayList<String> getWords()");
      printLine("   {");
      printLine("		String	buffer = \"\";");
      printLine("		String	baseName = \"\";");
      printLine("		ArrayList<String> value;");
      printLine("		ArrayList<XMLParser> node;");
      printLine("		XMLParser	xmlParserType = new XMLParser();");
      printLine("		String 		stringType = \"\";");
      printLine("		ArrayList	arrayListType = new ArrayList();");
      printLine("		ArrayList<String>	words = new ArrayList<String>();");
      printLine("		ArrayList<String>	newWords;");
      printLine("		ArrayList<String>	part;");
      printLine("		");
      printLine("		Field[] field = this.getClass().getDeclaredFields();");
      printLine("		for(int i = 0; i < field.length; i++) {");
      printLine("	      baseName = field[i].getName().substring(1);	// Drop first letter (always \"m\")");
      printLine("	      if(baseName.endsWith(\"ID\")) continue;	// Skip items that end with \"ID\"");
      printLine("			if(field[i].getType().isInstance(stringType)) {	// String values");
      printLine("				value = this.getValues(baseName, false);");
      printLine("				for(int j = 0; j < value.size(); j++) {");
      printLine("					buffer = value.get(j).toUpperCase();");
      printLine("					part = parseWords(buffer);");
      printLine("					for(int n = 0; n < part.size(); n++) {");
      printLine("						if(! isInList(part.get(n), words)) words.add(part.get(n));");
      printLine("					}");
      printLine("				}");
      printLine("			} else if(field[i].getType().isInstance(arrayListType)) {	// Arrays");
      printLine("				// Try as array of strings");
      printLine("				value = this.getAllValues(baseName);");
      printLine("				for(int j = 0; j < value.size(); j++) {");
      printLine("					buffer = value.get(j).toUpperCase();");
      printLine("					part = parseWords(buffer);");
      printLine("					for(int n = 0; n < part.size(); n++) {");
      printLine("						if(! isInList(part.get(n), words)) words.add(part.get(n));");
      printLine("					}");
      printLine("				}");
      printLine("				// Try as array of nodes");
      printLine("				node = this.getNodes(baseName, false);");
      printLine("				for(int j = 0; j < node.size(); j++) {");
      printLine("					newWords = node.get(j).getWords();");
      printLine("					for(int n = 0; n < newWords.size(); n++) {");
      printLine("						if(! isInList(newWords.get(n), words)) words.add(newWords.get(n));");
      printLine("					}");
      printLine("				}");
      printLine("			} else if(field[i].getType().getSuperclass().isInstance(xmlParserType)) {	// XMLParser");
      printLine("				node = this.getNodes(baseName, false);");
      printLine("				for(int j = 0; j < node.size(); j++) {");
      printLine("					newWords = node.get(j).getWords();");
      printLine("					for(int n = 0; n < newWords.size(); n++) {");
      printLine("						if(! isInList(newWords.get(n), words)) words.add(newWords.get(n));");
      printLine("					}");
      printLine("				}");
      printLine("			}");
      printLine("		}");
      printLine("		return words;");
      printLine("	}	 ");
      printLine("		");
      printLine("   /** ");
      printLine("    * Determine if an element is in use. An element is in use if the value in");
      printLine("    * not empty or the element is required.");
      printLine("    * ");
      printLine("    * @param name      the name of the element.");
      printLine("    * @param value     the value associated with the element.");
      printLine("    *");
      printLine("    * @author Todd King");
      printLine("    * @author UCLA/IGPP");
      printLine("    * @since           1.0");
      printLine("    **/");
      printLine("   public boolean isInUse(String name, String value)");
      printLine("   {");
      printLine("   	if(value.length() > 0) return true;");
      printLine("   	return isRequired(name);");
      printLine("   }");
      printLine("   ");
      printLine("   /** ");
      printLine("    * Determine if an element is required. An element is required is it is in");
      printLine("    * the list of required elements (see setRequired())");
      printLine("    * ");
      printLine("    * @param name      the name of the element.");
      printLine("    *");
      printLine("    * @author Todd King");
      printLine("    * @author UCLA/IGPP");
      printLine("    * @since           1.0");
      printLine("    **/");
      printLine("   public boolean isRequired(String name)");
      printLine("   {");
      printLine("   	return isInList(name, mRequired);");
      printLine("   }");
      printLine("   ");
      printLine("   /** ");
      printLine("    * Get a list of values labeled with an XPath.");
      printLine("    * With XPath the first item has index of 1.");
      printLine("    * ");
      printLine("    * @param node      the {@link Node}.");
      printLine("    *");
      printLine("    * @author Todd King");
      printLine("    * @author UCLA/IGPP");
      printLine("    * @since           1.0");
      printLine("    **/");
      printLine("   static public ArrayList<Pair> getXPathList(String prefix, ArrayList<String> list)");
      printLine("   {");
      printLine("   	ArrayList<Pair> pairList = new ArrayList<Pair>();");
      printLine("");
      printLine("      for(int i = 0; i < list.size(); i++) {");
      printLine("	   	Pair<String, String> pair = new Pair<String, String>(prefix + \"[\" + (i+1) + \"]\", list.get(i));");
      printLine("	   	pairList.add(pair);");
      printLine("      }");
      printLine("      return pairList;");
      printLine("   }");
      printLine("");
      printLine("	/**");
      printLine("	 * Convert special characters in a string for use in an");
      printLine("	 * HTML document. Converts only the special ASCII characters of &, \", ', < and > ");
      printLine("	 * to the corresponding HTML entities .");
      printLine("	 *");
      printLine("	 * @author Todd King");
      printLine("	 * @version 1.00 05/09/14");
      printLine("	 **/");
      printLine("	public static String entityEncode(String text)");
      printLine("	{");
      printLine("		String	buffer;");
      printLine("		");
      printLine("		if(text == null) return text;");
      printLine("		");
      printLine("		buffer = text;");
      printLine("		buffer = buffer.replace(\"&\", \"&amp;\");");
      printLine("		buffer = buffer.replace(\"\\\"\", \"&quot;\");");
      printLine("		buffer = buffer.replace(\"'\", \"&#39;\");");
      printLine("		buffer = buffer.replace(\"<\", \"&lt;\");");
      printLine("		buffer = buffer.replace(\">\", \"&gt;\");");
      printLine("		");
      printLine("		return buffer;");
      printLine("	}");
      printLine("");
      printLine("	public void setClassName(String name) { mClassName = name; }");
      printLine("	public String getClassName() { return mClassName; }");
      printLine("    ");
      printLine("	public void addRequired(String value) { mRequired.add(value); }");
      printLine("	public ArrayList getRequired() { return mRequired; }");
      printLine("}");
      
		closeWriter();
	}
	
	public void writeProjectFile(String version)
		throws Exception
	{
		String	buffer;
		String	fileName = "parser.jcp";
		PrintStream out = new PrintStream(fileName);
		String	ver = version.replace(".", "");
		
		setWriter(out);
	   System.out.println(fileName);

		printLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		printLine("");
		printLine("<project>");
		printLine("	<settings>");
		printLine("		<label>parser%param%</label>", ver);
		printLine("		<root_path>\\</root_path>");
		printLine("		<output_path>.\\</output_path>");
		printLine("		<source_path>\\</source_path>");
		printLine("		<jdkprofile_ref>java</jdkprofile_ref>");
		printLine("		<compiler_ref>&lt;Default&gt;</compiler_ref>");
		printLine("		<ant_ref/>");
		printLine("	</settings>");
		printLine("	<ant_events/>");
		printLine("	<run_items/>");
		printLine("	<runtimes>");
		printLine("		<runtime>");
		printLine("			<label>&lt;Default&gt;</label>");
		printLine("			<application_ref/>");
		printLine("			<applet_ref/>");
		printLine("			<debugger_ref/>");
		printLine("			<param/>");
		printLine("			<run/>");
		printLine("		</runtime>");
		printLine("	</runtimes>");
		printLine("	<libraries/>");
		printLine("	<files>");
		printLine("		<include>");
	   
		for(Iterator i = mFileList.iterator(); i.hasNext(); ) {
			buffer = (String) i.next();
			printLine("			<fileitem>");
			printLine("				<path>%param%</path>", buffer);
			printLine("			</fileitem>");
		}
		
		printLine("		</include>");
		printLine("	</files>");
		printLine("	<tasks/>");
		printLine("</project>");
			
	   closeWriter();
	}
	
	/**
	 * Determine if term is a container
	 **/
	public boolean isContainer(String term)
	{
		try {
			if(getElementTypeToken(term) == TypeContainer) return true;
		} catch(Exception e) {
		}
			
		return false;
	}
	
	/**
	 * Determine if term is a container
	 **/
	public boolean hasResourceHeader(String term)
	{
		String	buffer;
		boolean	has = false;
		
		// Exceptions
		if(isMatch(term, "Person")) return false;
		if(isMatch(term, "Granule")) return false;
		
		for(Iterator i = mTopLevelElements.iterator(); i.hasNext(); ) {
			buffer = (String) i.next();
			if(isMatch(term, buffer)) has = true;
		}
		return has;
	}
	
	/**
	 * Determine if token is multiple occurence
	 **/
	public boolean isMultiple(String token)
	{
		boolean multiple = false;
		char	occur = token.charAt(0);
			
		// Determine visual attributes based on occurrence
		multiple = false;
		
		switch(occur) {
		case '0':
			multiple = false;
			break;
		case '1':
			multiple = false;
			break;
		case '*':
			multiple = true;
			break;
		case '+':
			multiple = true;
			break;
		}
		
		return multiple;
	}
	
	/**
	 * Determine if token is required
	 **/
	public boolean isRequired(String token)
	{
		boolean required = false;
		char	occur = token.charAt(0);
			
		// Determine visual attributes based on occurrence
		required = false;
		
		switch(occur) {
		case '0':
			required = false;
			break;
		case '1':
			required = true;
			break;
		case '*':
			required = true;
			break;
		case '+':
			required = true;
			break;
		}
		
		return required;
	}
	
	/** 
	 * Get exportable name from encoded token
	 **/
	public String getElemName(String token)
	{
		String name = token.substring(1);
		String elemName = getXSLName(name);
		
		return elemName;
	}
	
	/** 
	 * Get term name from encoded token
	 **/
	public String getTermName(String token)
	{
		String name = token.substring(1);
		return name;
	}
	
	public void makeAll()
		throws Exception
	{
		mTopLevelElements = getTopLevelElements();
		
		for(Iterator i = mTopLevelElements.iterator(); i.hasNext(); ) {
			makeObject((String) i.next(), true);
		}
	   
	   writeDescriptionClass(mTopLevelElements, mModelVersion);
	   writeUtilClass(mModelVersion);
	   writeResourceClass(mModelVersion);
	   writePairClass(mModelVersion);
	   writeXMLParserClass(mModelVersion);
	   writeProjectFile(mModelVersion);
	}

	public void makeObject(String object, boolean isResource)
		throws Exception
	{
		String		query;
		Statement	statement;
		ResultSet	resultSet;

		PrintStream	out;		
		String	lastObject = "";
		String	elem = "";
		String	occur = "";
		String	xmlName = "";
		boolean	needFooter = false;
		ArrayList<String>	objectList = new ArrayList<String>();
		ArrayList<String>	list = new ArrayList<String>();
		
	   query = "select" 
	          + " ontology.*"
	          + " from ontology"
	          + " where ontology.Version='" + mModelVersion  + "'"
	          + " and ontology.Object='" + object + "'"
	          + " Order By ontology.Object, ontology.Pointer"
	          ;
	
		statement = this.beginQuery();
		resultSet = this.select(statement, query);
		
		xmlName = object.replace(" ", "");
		out = new PrintStream(xmlName + ".java");
		mFileList.add(xmlName + ".java");
		setWriter(out);
	   System.out.println(xmlName);
	   
		// Get list of elements in object
		list.clear();
	   lastObject = "";
		while(resultSet.next())	{
			elem = resultSet.getString("Element");
			occur = resultSet.getString("Occurence");
			list.add(occur + elem);
			if(isContainer(elem)) objectList.add(elem);
	   }
	   this.endQuery(statement, resultSet);
	   
		writeHeader(xmlName, mModelVersion, isResource);
	   writeVariables(list);
	   writeConstructor(xmlName);
	   writeReset(list);
	   writeRequired(list);
	   writeNodeMethods(object, list);
	   writeXMLMethods(list);
	   writeSetGet(list);
	   writeXPathPairs(xmlName, list);
	   writeFooter(xmlName);
	   
	   closeWriter();
	   
	   // Make parsers for all sub objects
	   for(Iterator i = objectList.iterator(); i.hasNext(); ) {
	   	makeObject((String) i.next(), false);
	   }
	}

	public String getXSLName(String term)
	{
		// Strip spaces, dashes and single quotes
		String	buffer;
		
		buffer = term.replace("-", "");
		buffer = buffer.replace("\'", "");
		buffer = buffer.replace(" ", "");
		
		return buffer;
	}

	public ArrayList getTopLevelElements()
		throws Exception
	{
		String		query;
		Statement	statement;
		ResultSet	resultSet;
		
		ArrayList	list = new ArrayList();
		
	   query = "select" 
	          + " ontology.*"
	          + " from ontology"
	          + " where ontology.Version='" + mModelVersion  + "'"
	          + " and ontology.Object='Spase'"
	          ;
	
		statement = this.beginQuery();
		resultSet = this.select(statement, query);
		
		list.clear();
		while(resultSet.next())	{
			if(isMatch(resultSet.getString("Element"), "Version")) continue;	// Not a resource
			list.add(resultSet.getString("Element"));
		}
		
		this.endQuery(statement, resultSet);
		
		return list;
	}

	public String getElementType(String term)
		throws Exception
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;
		String		buffer;
		
	   query = "select" 
	          + " dictionary.*"
	          + " from dictionary"
	          + " where dictionary.Term = '" + sqlEncode(term) + "'"
	          + " and dictionary.Version='" + mModelVersion   + "'"
	          ;

		statement = this.beginQuery();
		resultSet = this.select(statement, query);
		
	   buffer = "";
		while(resultSet.next())	{
   		buffer = resultSet.getString("Type");
   	}
		// Clean-up
		this.endQuery(statement, resultSet);
   
	   return buffer;
	}

	public int getElementTypeToken(String term)
		throws Exception
	{
		String type = getElementType(term);
		
		if(isMatch(type, "Container"))	return TypeContainer;
		if(isMatch(type, "Count"))			return TypeCount;
		if(isMatch(type, "DateTime"))		return TypeDate;
		if(isMatch(type, "Duration"))		return TypeTime;
		if(isMatch(type, "Enumeration"))	return TypeEnumeration;
		if(isMatch(type, "Item"))			return TypeItem;
		if(isMatch(type, "Numeric"))		return TypeNumeric;
		if(isMatch(type, "Text"))			return TypeText;
		
		// Obsolete as of version 1.2.0
		if(isMatch(type, "Date"))			return TypeDate;
		if(isMatch(type, "Time"))			return TypeTime;
		
		return TypeText;
	}
	
	public String getElementList(String term)
		throws Exception
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;
		String	buffer;
		
	   query = "select" 
	          + " dictionary.*"
	          + " from dictionary"
	          + " where dictionary.Term = '" + sqlEncode(term) + "'"
	          + " and dictionary.Version='" + mModelVersion   + "'"
	          ;

		statement = this.beginQuery();
		resultSet = this.select(statement, query);
		
	   buffer = "";
		while(resultSet.next())	{
   		buffer = resultSet.getString("List");
	   }
		// Clean-up
		this.endQuery(statement, resultSet);
	   
	   return buffer;
	}

	public String getElementEnum(String term)
		throws Exception
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;
		String	buffer;
		
	   query = "select" 
	          + " dictionary.*"
	          + " from dictionary"
	          + " where dictionary.Term = '" + sqlEncode(term) + "'"
	          + " and dictionary.Version='" + mModelVersion  + "'"
	          ;
	
		statement = this.beginQuery();
		resultSet = this.select(statement, query);
		
	   buffer = "";
		while(resultSet.next())	{
	   	buffer = resultSet.getString("List");
	   }
		// Clean-up
	   this.endQuery(statement, resultSet);
	   
	   return buffer;
	}

   //===========================================================
   // Utlity functions
   //===========================================================
   public void setWriter(PrintStream stream) 
   {
    	mWriter = null;
    	mStream = stream;
    	mServlet = null;
   }
    
   public void setWriter(javax.servlet.jsp.JspWriter writer) 
   {
    	mWriter = writer;
    	mStream = null;
    	mServlet = null;
   }
    
   public void setWriter(ServletOutputStream stream) 
   {
    	mWriter = null;
    	mStream = null;
    	mServlet = stream;
   }
    
   public void closeWriter() 
    	throws Exception
   {
    	if(mWriter != null) mWriter.close();
    	if(mStream != null) mStream.close();
    	if(mServlet != null) mServlet.close();
   }
    
	public void print(String text)
	{
    	try {
	    	if(mWriter != null) mWriter.print(text);
	    	if(mStream != null) mStream.print(text);
	    	if(mServlet != null) mServlet.print(text);
    	} catch(Exception e) {
    	}
	}
	
	public void printLine(String text)
	{
    	try {
	    	if(mWriter != null) mWriter.println(text);
	    	if(mStream != null) mStream.println(text);
	    	if(mServlet != null) mServlet.println(text);
    	} catch(Exception e) {
    	}
	}
	
	public void printLine(String text, String variable)
	{
		String buffer = text.replaceAll("%param%", variable);
		printLine(buffer);
	}
	
	public void printIndent(int indent)
	{
		for(int i = 0; i <= indent; i++) print("   ");
	}

	// Query database for most recent version
	public String getModelVersion()
		throws Exception
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;

		String	version = mModelVersion ;
		
		if(version == null) {
			query = "select"
		   		+ " * "
		   		+ " from history"
		   		+ " where"
		   		+ " history.ID = (Select max(history.ID) from history)"
			      ;
		
			statement = this.beginQuery();
			resultSet = this.select(statement, query);
			
			while(resultSet.next())	{
		      version = resultSet.getString("Version");
		   }
		   
		   this.endQuery(statement, resultSet);
		}
		return version;
	}
	
	public String sqlEncode(String text) { return igpp.util.Encode.sqlEncode(text); }
	public boolean isMatch(String base, String text) { if(base.compareToIgnoreCase(text) == 0) { return true; } return false; }
	public boolean isInList(ArrayList list, String text) 
	{ 
		String	base;
		
		for(Iterator i = list.iterator(); i.hasNext(); ) {
			base = (String) i.next();
			if(base.compareToIgnoreCase(text) == 0) { return true; } 
		}
		return false; 
	}
	
	// Argument passing when a bean
	public void setVersion(String value) { mModelVersion  = value; }
	public String getVersion() { return mModelVersion; }
}	
