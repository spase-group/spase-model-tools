/**
 * Creates an XML Schema document to describe a version of the SPASE data model.
 * Queries the data model database to build the schema.
 *
 * @author Todd King
 * @version 1.00 2006 11 23
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

import javax.servlet.jsp.JspWriter;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
public class MakeXSD extends Query
{
	private String	mVersion = "1.0.1";

	private String	mModelVersion = null;
	private String	mHomepath = "";
	private boolean mAnnotate = true;

// Database access variables
	private String mHost = "";
	private String mDatabase = "spase-model.db";
	private String mUsername = "";
	private String mPassword = "";

	private JspWriter	mWriter = null;
	private PrintStream	mStream = null;
	private ServletOutputStream	mServlet = null;

	ArrayList<String>	mElemList = new ArrayList<String>();
	ArrayList<String>	mElemLeaf = new ArrayList<String>();

	public static void main(String args[])
   {
		MakeXSD me = new MakeXSD();

		if (args.length < 1) {
			System.err.println("Version: " + me.mVersion);
			System.err.println("Usage: " + me.getClass().getName() + " version [homepath] [annotate]");
			System.exit(1);
		}

		try {
			me.mModelVersion = args[0];
			me.mModelVersion = me.getModelVersion();
			if(args.length > 1) {
				me.mHomepath = args[1];
				if( ! me.mHomepath.endsWith("/")) me.mHomepath += "/";
			}

			if(args.length > 2) me.mAnnotate = false;

			me.setDatabaseDriver("SQLite");
			me.setDatabase(me.mHost, me.mHomepath + me.mDatabase);
			me.setUserLogin(me.mUsername, me.mPassword);
			me.useUser();

			me.setWriter(System.out);
			me.makeXSD();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	public void init()
			throws Exception
	{
		setDatabaseDriver("SQLite");
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
		makeXSD();
	}

	public void makeXSD()
		throws Exception
	{
		String today = igpp.util.Date.now();

		printLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		printLine("<!-- Automatically created based on the dictionary stored at http://www.spase-group.org -->");
		printLine("<!-- Version: " + mModelVersion + " -->");
		printLine("<!-- Generated: " + today + " -->");
		printLine("<xsd:schema");
		printLine("	      xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" ");
		printLine("	      targetNamespace=\"http://www.spase-group.org/data/schema\"");
		printLine("	      xmlns=\"http://www.spase-group.org/data/schema\"");
		printLine("	      elementFormDefault=\"qualified\">");

		makeTree("Spase", "", true);
		makeDictionary();
		makeLists();
		makeTypes();

		printLine("</xsd:schema>");
	}

	/**
	 * Generate XML schema description of non-standard data types
	**/
	public void makeTypes()
		throws Exception
	{
		printLine("<!-- ================================");
		printLine("      Types");
		printLine("     ================================ -->");

		printLine("    <xsd:simpleType name=\"typeSequence\">");
  		printLine("    <xsd:list itemType=\"xsd:integer\"/>");
		printLine("    </xsd:simpleType>");

	}

	/**
	 * Generate XML schema description of every list item
	**/
	public void makeLists()
		throws Exception
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;
		String	desc;
		String	listName;
		String	buffer;

		query = "select"
		       + " list.*"
		       + " from list"
		       + " where list.Version='" + mModelVersion + "'"
		       ;

		statement = this.beginQuery();
		resultSet = this.select(statement, query);

		printLine("<!-- ================================");
		printLine("      Lists");
		printLine("     ================================ -->");

		// Generate enumeration for version
   	printLine("<!-- ==========================");
      printLine("Version");
      printLine("========================== -->");

   	printLine(1, "<xsd:simpleType name=\"enumVersion\">");
   	printAnnotation(2, "Version number.");
   	printLine(1, "<xsd:restriction base=\"xsd:string\">");
		printLine(2, "<xsd:enumeration value=\"" + mModelVersion + "\" />");
		printLine(1, "</xsd:restriction>");
		printLine("</xsd:simpleType>");

		// Generate types for each list
		while(resultSet.next())	{
			buffer = resultSet.getString("Name");
			listName = "enum" + getXSLName(buffer);
			if(igpp.util.Text.isMatch(resultSet.getString("Type"), "Open")) {
				desc = "Open List. See: " + resultSet.getString("Reference");
			} else {
				desc = resultSet.getString("Description");
			}

	   	printLine("<!-- ==========================");
	      printLine(resultSet.getString("Name"));
	      printLine("");
	      printLine(igpp.util.Text.wordWrap(desc, 40, ""));
	      printLine("========================== -->");

			if(igpp.util.Text.isMatch(resultSet.getString("Type"), "Open")) {
				printLine(1, "<xsd:element name=\"" + listName + "\" type=\"xsd:string\">");
		   	printAnnotation(2, desc);
		   	printLine(1, "</xsd:element>");
		   } else {	// Closed
		   	printLine(1, "<xsd:simpleType name=\"" + listName + "\">");
		   	printAnnotation(2, desc);
		   	printLine(1, "<xsd:restriction base=\"xsd:string\">");
				makeEnum("", resultSet.getString("Name"));
				printLine(1, "</xsd:restriction>");
				printLine("</xsd:simpleType>");
			}
		}

		// Clean-up
	   this.endQuery(statement, resultSet);
	}

	/**
	 * Determine if a dictionary term is an enumeration
	 * return the name of the enumeration list or "" if not an enumeration
	**/
	public String getEnumeration(String term)
		throws Exception
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;

		String	itIs = "";
		String	buffer;

	   query = "select"
	          + " dictionary.*"
	          + " from dictionary"
	          + " where dictionary.Term = '" + sqlEncode(term) + "'"
	          + " and dictionary.Version='" + mModelVersion + "'"
	          ;

		statement = this.beginQuery();
		resultSet = this.select(statement, query);

	   itIs = "";
		while(resultSet.next())	{
	   	buffer = resultSet.getString("Type");
	   	buffer = buffer.trim();
	   	if(igpp.util.Text.isMatch(buffer, "Enumeration")) itIs = resultSet.getString("List");
	   }

		// Clean-up
	   this.endQuery(statement, resultSet);

	   return itIs;
	}

	/**
	 * Create an enumeration list.
	 **/
	public void makeEnum(String prefix, String list)
		throws Exception
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;

		String	term;
		String	buffer;
		String	enumList;
		String	closeTag;

		query = "select"
		       + " member.*"
		       + " from member"
		       + " where member.Version='" + mModelVersion + "'"
		       + " and member.List ='" + list + "'"
		       ;

		statement = this.beginQuery();
		resultSet = this.select(statement, query);

		while(resultSet.next())	{
			term = resultSet.getString("Term");
			buffer = "";
			if(prefix.length()> 0) buffer += prefix + ".";
			buffer += getXSLName(term);
			closeTag = " /";
			if(mAnnotate) closeTag = "";
			printLine("<xsd:enumeration value=\"" + buffer + "\"" + closeTag + ">");
			if(mAnnotate) {
				printAnnotation(1, getElementDesc(term));
				printLine("</xsd:enumeration>");
			}
			enumList = getEnumeration(term);
			if(enumList.length() != 0) makeEnum(buffer, enumList);
		}
		// Clean-up
	   this.endQuery(statement, resultSet);
	}

	public void makeDictionary()
		throws Exception
	{
		String	term;
		String	type;
		String	list;
		String	desc;
		String	group;
		String	subGroup;

		if(mElemLeaf.isEmpty()) return;

		Object[] elemLeaf = mElemLeaf.toArray();

		java.util.Arrays.sort(elemLeaf);
		printLine("<!-- ================================");
		printLine("      Dictionary Terms");
		printLine("     ================================ -->");
		for(int i = 0; i < elemLeaf.length; i++) {
			term = (String) elemLeaf[i];
	   	type = getElementType(term);
	   	list = getElementList(term);
	   	desc = getElementDesc(term);
	   	group = getElementGroup(term);
	   	subGroup = "";
	   	if(group.length() > 0) subGroup = " substitutionGroup=\"" + group + "\"";

	   	// Version is a phantom enueration with the current version number as the only member.
	   	// The current version number is added in the enumeration definition.
	   	if(igpp.util.Text.isMatch(term, "Version")) {
	   		type = "Enumeration";
	   		list = "Version";
	   	}
	   	/*
			if(igpp.util.Text.isMatch(term, "Extension")) {	// Special case
				printLine(1, "<xsd:element name=\"Extension\" substitutionGroup=\"ResourceEntity\">");
	  		   printAnnotation(2, desc);
				printLine(2, "<xsd:complexType>");
				printLine(3, "<xsd:sequence>");
				printLine(4, "<xsd:any namespace=\"##other\" processContents=\"lax\" minOccurs=\"0\" maxOccurs=\"unbounded\" />");
				printLine(3, "</xsd:sequence>");
				printLine(2, "</xsd:complexType>");
				printLine(1, "</xsd:element>");
			} else {
		*/
		   	if(igpp.util.Text.isMatch(type, "Item")) {
		  		   printLine(1, "<xsd:element name=\"" + getXSLName(term) + "\" type=\"xsd:boolean\" default=\"true\"" + subGroup + ">");
		  		   printAnnotation(2, desc);
		  		} else if(igpp.util.Text.isMatch(type, "Enumeration")) {
			   	printLine(1, "<xsd:element name=\"" + getXSLName(term) + "\" type=\"enum" + getXSLName(list) + "\"" + subGroup + ">");
			   	printAnnotation(2, desc);
			 	} else {
			   	printLine(1, "<xsd:element name=\"" + getXSLName(term) + "\" type=\"" + getXSLType(type) + "\"" + subGroup + ">");
			   	printAnnotation(2, desc);
			   }
				printLine(1, "</xsd:element>");
		// }
	  }
	}

	public void makeTree(String term, String group, boolean addLang)
		throws Exception
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;
		String		itIs = "";
		int			rows = 0;
		String		desc;
		String		subGroup;
		String[]		pair;

		ArrayList elemList = new ArrayList();
		ArrayList groupList = new ArrayList();

	   if(isElemOutput(term)) return;

	   query = "select"
	          + " ontology.*"
	          + " from ontology"
	          + " where ontology.Object = '" + sqlEncode(term) + "'"
	          + " and ontology.Version='" + mModelVersion  + "'"
	          + " Order By ontology.Pointer"
	          ;

		// Determine the number of rows
		// We do this the hard way since sqlite is a "FORWARD Only" database

	   statement = this.beginQuery();
	   resultSet = this.select(statement, query);
	   rows = 0;
	   while(resultSet.next()) {
	   	  rows++;
	   }
	   this.endQuery(statement, resultSet);

	   // Now query for results
	   statement = this.beginQuery();
	   resultSet = this.select(statement, query);

	   itIs = "";

	   if(rows == 0) {	// A leaf
		   if(term.compareTo("Extension") == 0) {	// "Extension" is an exception
			   	printLine(0, "");
				printLine(1, "<xsd:element name=\"" + getXSLName(term) + "\" type=\"" + getXSLName(term) + "\" />");
			   	printLine(1, "<xsd:complexType name=\"" + getXSLName(term) +"\">");
			   	printAnnotation(2, getElementDesc(term));
			   	printLine(2, "<xsd:sequence>");
			   	printLine(3, "<xsd:any minOccurs=\"0\" />");
			   	printLine(2, "</xsd:sequence>");
			   	if(addLang) printLine(3, "<xsd:attribute name=\"lang\" type=\"xsd:string\" default=\"en\"/>");
			   	printLine(1, "</xsd:complexType>");
		   } else {
			   isElemLeaf(term);
		   }
	   } else { // Not a leaf
	   	desc = getElementDesc(term);
	   	// group = resultSet.getString("Group");
	   	subGroup = "";
	   	if(group.length() > 0) subGroup = " substitutionGroup=\"" + group + "\"";

	   	printLine(0, "");
		printLine(1, "<xsd:element name=\"" + getXSLName(term) + "\" type=\"" + getXSLName(term) + "\"" + subGroup + "/>");
	   	printLine(1, "<xsd:complexType name=\"" + getXSLName(term) +"\">");
		   printAnnotation(2, desc);
			printLine(2, "<xsd:sequence>");
			while(resultSet.next()) {	// We're at the first record coming in to part of the code
				group = resultSet.getString("Group");
				if(group.length() > 0) {
					elemList.add(new String[] {resultSet.getString("Element"), group});
					if(! igpp.util.Text.isInList(group, groupList)) {
						groupList.add(group);
		    			printLine(3, "<xsd:element ref=\"" + getXSLName(group) + "\" " + getXSLOccurance(resultSet.getString("Occurence")) + " /> ");
					}
				} else {	// Element
					elemList.add(new String[] {resultSet.getString("Element"), ""});
		    		printLine(3, "<xsd:element ref=\"" + getXSLName(resultSet.getString("Element")) + "\" " + getXSLOccurance(resultSet.getString("Occurence")) + " /> ");
				}
			}
			printLine(2, "</xsd:sequence>");
			if(addLang) printLine(3, "<xsd:attribute name=\"lang\" type=\"xsd:string\" default=\"en\"/>");
			printLine(1, "</xsd:complexType>");
		}
	   this.endQuery(statement, resultSet);

	   // Extract description of each group
	   for(int i = 0; i < groupList.size(); i++) {
	   	printLine(0, "");
    		printLine(1, "<xsd:element name=\"" + ((String) groupList.get(i)) + "\" abstract=\"true\" /> ");
    		// elemList.add(groupList.get(i));
	   }

	   // Extract description of each element
	   for(int i = 0; i < elemList.size(); i++) {
	   	pair = (String[]) elemList.get(i);
	   	makeTree(pair[0], pair[1], false);
	   }
	}

	/* Check if a term is in the output list. Add it if not. */
	public boolean isElemOutput(String term)
	{
		if(igpp.util.Text.isInList(term, mElemList)) return true;

		/* Add to list */
		mElemList.add(term);

		return false;
	}

	/* Check if a term is in the leaf list. Add it if not. */
	public boolean isElemLeaf(String term)
	{
		if(igpp.util.Text.isInList(term, mElemLeaf)) return true;

		/* Add to list */
		mElemLeaf.add(term);

		return false;
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

	public String getElementGroup(String term)
		throws Exception
	{
		String		query;
		Statement	statement;
		ResultSet	resultSet;
		String		buffer;

	   query = "select"
	          + " ontology.*"
	          + " from ontology"
	          + " where ontology.Element = '" + sqlEncode(term) + "'"
	          + " and ontology.Version='" + mModelVersion  + "'"
	          + " Order By ontology.Pointer"
	          ;


		statement = this.beginQuery();
		resultSet = this.select(statement, query);

	   buffer = "";
		while(resultSet.next())	{
			if(buffer.length() == 0) buffer = resultSet.getString("Group");
	   }
		// Clean-up
		this.endQuery(statement, resultSet);

	   return buffer;
	}


	public String getElementDesc(String term)
		throws Exception
	{
		String		query;
		Statement	statement;
		ResultSet	resultSet;
		String		buffer;

	   query = "select"
	          + " dictionary.*"
	          + " from dictionary"
	          + " where dictionary.Term = '" + sqlEncode(term) + "'"
	          + " and dictionary.Version='" + mModelVersion + "'"
	          ;

		statement = this.beginQuery();
		resultSet = this.select(statement, query);

	   buffer = "";
		while(resultSet.next())	{
	   	buffer = resultSet.getString("Definition");
	   }
		// Clean-up
		this.endQuery(statement, resultSet);

	   return buffer;
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
	          + " and dictionary.Version='" + mModelVersion + "'"
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
	          + " and dictionary.Version='" + mModelVersion + "'"
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
	          + " and dictionary.Version='" + mModelVersion + "'"
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
    public void setWriter(PrintStream stream) {
    	mWriter = null;
    	mStream = stream;
    	mServlet = null;
    }

    public void setWriter(javax.servlet.jsp.JspWriter writer) {
    	mWriter = writer;
    	mStream = null;
    	mServlet = null;
    }

    public void setWriter(ServletOutputStream stream) {
    	mWriter = null;
    	mStream = null;
    	mServlet = stream;
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

	public void print(int indent, String text)
	{
		printIndent(indent);
		printLine(text);
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

	public void printLine(int indent, String text)
	{
		printIndent(indent);
		printLine(text);
	}

	public void printIndent(int indent)
	{
		for(int i = 0; i <= indent; i++) print("   ");
	}

	public String getIndent(int indent)
	{
		String	buffer = "";

		for(int i = 0; i <= indent; i++) buffer += "   ";

		return buffer;
	}

	public void printAnnotation(int indent, String desc)
		throws Exception
	{
		desc = igpp.util.Text.wordWrap(desc, 40, getIndent(indent+2));
		desc = igpp.util.Encode.htmlEncode(desc);
		printLine(indent, "<xsd:annotation>");
		printLine(indent+1, "<xsd:documentation xml:lang=\"en\">");
		printLine(desc);
		printLine(indent+1, "</xsd:documentation>");
		printLine(indent, "</xsd:annotation>");
	}

	public boolean printTerm(String term, int indent, int occur, String pointer)
		throws Exception
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;

		boolean isContainer = false;

	   query = "select"
	          + " dictionary.*"
	          + " from dictionary"
	          + " where dictionary.Term = '" + sqlEncode(term) + "'"
	          + " and dictionary.Version='" + mModelVersion + "'"
	          ;

		statement = this.beginQuery();
		resultSet = this.select(statement, query);

		while(resultSet.next())	{
			print(indent, "<xsd:element name=\"" + term);
			if(igpp.util.Text.isMatch(resultSet.getString("Type"), "Container")) {
				printLine(">");
				printLine(indent+1, "<xsd:complexType>");
				isContainer = true;
			} else if(igpp.util.Text.isMatch(resultSet.getString("Type"), "Item")) {
				printLine(">");
				printLine(indent+1, "<xsd:complexType>");
	    		printLine(indent+1, "</xsd:complexType>");
				printLine(indent, "</xsd:element>");
			} else {
			 	printLine("\" type=\"" + getXSLType(resultSet.getString("Type")) + "\" />");
			}
	   }
		// Clean-up
		this.endQuery(statement, resultSet);

	   return isContainer;
	}


	public String getXSLType(String type)
	{
		if(igpp.util.Text.isMatch(type, "Count")) return "xsd:integer";
		if(igpp.util.Text.isMatch(type, "DateTime")) return "xsd:dateTime";
		if(igpp.util.Text.isMatch(type, "Duration")) return "xsd:duration";
		if(igpp.util.Text.isMatch(type, "Numeric")) return "xsd:double";
		if(igpp.util.Text.isMatch(type, "Sequence")) return "typeSequence";
		if(igpp.util.Text.isMatch(type, "Text")) return "xsd:string";

		// Obsolete data types as of version 1.2.0
		if(igpp.util.Text.isMatch(type, "Date")) return "xsd:dateTime";
		if(igpp.util.Text.isMatch(type, "Time")) return "xsd:duration";

		return "xsd:string";
	}

	public String getXSLOccurance(String occur)
	{
		occur = occur.trim();
		if(igpp.util.Text.isMatch(occur, "0")) return "minOccurs=\"0\" maxOccurs=\"1\"";	// Optional
		if(igpp.util.Text.isMatch(occur, "1")) return "minOccurs=\"1\" maxOccurs=\"1\"";	// One only
		if(igpp.util.Text.isMatch(occur, "+")) return "minOccurs=\"1\" maxOccurs=\"unbounded\"";	// At least one, perhaps many
		if(igpp.util.Text.isMatch(occur, "*")) return "minOccurs=\"0\" maxOccurs=\"unbounded\"";	// Any number

		return "" ;	// Default
	}

	// Query database for most recent version
	public String getModelVersion()
		throws Exception
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;

		String	version = mModelVersion;

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

	// Argument passing when a bean
	public void setVersion(String value) { mModelVersion = value; }
	public String getVersion() { return mModelVersion; }

	public void setVersion(boolean value) { mAnnotate = value; }
	public boolean getAnnotate() { return mAnnotate; }
}