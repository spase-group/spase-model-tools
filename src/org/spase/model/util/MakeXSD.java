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
import igpp.database.Query;

// import javax.sql.*;
import java.sql.ResultSet;
import java.sql.Statement;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;

import javax.servlet.jsp.JspWriter;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

/**
 * Build an XML Schema document based on the SPASE data model specification in
 * the data model database.
 * <p>
 * Usage:<blockquote> MakeXSD version </blockquote>
 * 
 * @author Todd King
 * @author UCLA/IGPP
 * @version 1.0, 11/23/06
 * @since 1.0
 */
public class MakeXSD extends Query {
	private String mVersion = "1.0.2";

	private String mModelVersion = null;
	private String mHomepath = "";
	private String mExtend = "";
	
	// Configuration
	private String mNamespace = "spase";
	private String mTarget = "http://www.spase-group.org/data/schema";

	// Database access variables
	private String mHost = "";
	private String mDatabase = "spase-model.db";
	private String mUsername = "";
	private String mPassword = "";
	private String mRootElem = "Spase";

	private ArrayList<String> mRootList = new ArrayList<String>();
	
	private JspWriter mWriter = null;
	private PrintStream mStream = null;
	private ServletOutputStream mServlet = null;

	ArrayList<String> mElemList = new ArrayList<String>();
	ArrayList<String> mListList = new ArrayList<String>();
	ArrayList<String> mElemLeaf = new ArrayList<String>();

	private String mOverview = "Creates XML schema document based on an information model definition.\n"
			 ;
	private String mAcknowledge = "Development funded by NASA's VMO project at UCLA.";

	private boolean mVerbose= false;
	
	// create the Options
	Options mAppOptions = new org.apache.commons.cli.Options();

	public MakeXSD() {
		mAppOptions.addOption( "h", "help", false, "Dispay this text" );
		mAppOptions.addOption( "v", "verbose", false, "Verbose. Show status at each step." );
		mAppOptions.addOption( "x", "extend", true, "extend. Generate as an extension of another schema." );
		mAppOptions.addOption( "p", "homepath", true, "Homepath. The path to tools and database. Default: current folder" );
		mAppOptions.addOption( "d", "database", true, "Database. The name of the SQLite database file. Default: " + mDatabase );
		mAppOptions.addOption( "m", "model", true, "Model. The version number of the model to generate." );
		mAppOptions.addOption( "r", "root", true, "Root. The root elements. If extending another schema the elements will override elements in the base schema. May be comma separated list. Default: " + mRootElem );
	}

	public static void main(String args[]) {
		MakeXSD me = new MakeXSD();

		if (args.length < 1) {
	   		me.showHelp();
    		return;
		}

		CommandLineParser parser = new BasicParser();
        try {
            CommandLine line = parser.parse(me.mAppOptions, args);

  			if(line.hasOption("h")) me.showHelp();
   			if(line.hasOption("v")) me.mVerbose = true;
   			if(line.hasOption("x")) { me.mExtend = line.getOptionValue("x"); }
   			if(line.hasOption("m")) { me.mModelVersion = line.getOptionValue("m"); }
   			if(line.hasOption("p")) { me.mHomepath = line.getOptionValue("p"); }
   			if(line.hasOption("d")) { me.mDatabase = line.getOptionValue("d"); }
   			if(line.hasOption("r")) { me.mRootElem = line.getOptionValue("r"); }
   			
			// Final fix-up
   			if(me.mHomepath.isEmpty()) me.mHomepath = new File(".").getAbsolutePath();
			if ( ! me.mHomepath.endsWith("/")) me.mHomepath += "/";
			if(me.mExtend.equals(".")) me.mExtend = "";
			if(me.mRootElem.equals(".")) me.mRootElem = "Spase";	// default
			String[] parts = me.mRootElem.split(",");
			for(String part : parts) {
				me.mRootList.add(part.trim());
			}
			
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

	/**
	 * Display help information.
	 **/
	public void showHelp()
	{
		System.out.println("");
		System.out.println(getClass().getName() + "; Version: " + mVersion);
		System.out.println(mOverview);
		System.out.println("");
		System.out.println("Usage: java " + getClass().getName() + " [options] [file...]");
		System.out.println("");
		System.out.println("Options:");

		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(getClass().getName(), mAppOptions);

		System.out.println("");
		System.out.println("Acknowledgements:");
		System.out.println(mAcknowledge);
		System.out.println("");
	}

	public void init() throws Exception {
		setDatabaseDriver("SQLite");
		setDatabase(mHost, mDatabase);
		setUserLogin(mUsername, mPassword);
		useUser();

		mModelVersion = getModelVersion();
	}

	public void destroy() {
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		doGet(request, response);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		ServletOutputStream out = response.getOutputStream();

		getModelVersion();

		response.setContentType("application/data");
		response.setHeader(
				"Content-Disposition",
				"attachment; filename=\"spase-"
						+ mModelVersion.replace(".", "_") + ".xsd\"");

		setWriter(out);
		makeXSD();
	}

	public void makeXSD() throws Exception {
		String today = igpp.util.Date.now();

		printLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		printLine("<!-- Automatically created based on the dictionary stored at http://www.spase-group.org -->");
		if( ! mExtend.isEmpty()) {	// Indicate what it extends
			printLine("<!-- Extends the schema contained in \"" + mExtend + "\" -->");			
		}
		printLine("<!-- Version: " + mModelVersion + " -->");
		printLine("<!-- Generated: " + today + " -->");
		printLine("<xsd:schema");
		printLine("	      targetNamespace=\"" + mTarget + "\"");
		printLine("	      xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"");
		printLine("	      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		printLine("	      xmlns:vc=\"http://www.w3.org/2007/XMLSchema-versioning\"");
		printLine("	      xmlns:rights=\"http://www.spase-group.org/data/schema\"");
		printLine("	      rights=\"Creative Commons Zero v1.0 Universal\"");
		printLine("	      xmlns:" + mNamespace + "=\"" + mTarget + "\"");
		printLine("	      elementFormDefault=\"qualified\"");
		printLine("	      attributeFormDefault=\"unqualified\"");
		printLine("	      vc:minVersion=\"1.1\"");
		printLine("	      version=\"" + mModelVersion + "\"");
		printLine(">");
		printLine("");
		
		if(mExtend.isEmpty()) {	// Only include in a base schema
			printLine(1, "<xsd:element name=\"Spase\" type=\"spase:Spase\" />");
		} 
		
		makeTree("Spase", "", true);
		makeGroup();
		makeDictionary();
		makeLists();
		if(mExtend.isEmpty()) {   // Only include in a base schema
			makeTypes();
		}

		printLine("</xsd:schema>");
	}

	/**
	 * Generate XML schema description of non-standard data types
	 **/
	public void makeTypes() throws Exception {
		printLine("<!-- ================================");
		printLine("      Types");
		printLine("     ================================ -->");

		printLine("");
		printLine(1, "<xsd:simpleType name=\"typeSequence\">");
		printLine(2, "<xsd:annotation>");
		printLine(3, "<xsd:documentation xml:lang=\"en\">");
		printAnnotation(4, getTypeDescription("Sequence"));
		printLine(3, "</xsd:documentation>");
		printLine(2, "</xsd:annotation>");
		printLine(2, "<xsd:list itemType=\"xsd:integer\"/>");
		printLine(1, "</xsd:simpleType>");

		printLine("");
		printLine(1, "<xsd:simpleType name=\"typeStringSequence\">");
		printLine(2, "<xsd:annotation>");
		printLine(3, "<xsd:documentation xml:lang=\"en\">");
		printAnnotation(4, getTypeDescription("StringSequence"));
		printLine(3, "</xsd:documentation>");
		printLine(2, "</xsd:annotation>");
		printLine(2, "<xsd:list itemType=\"xsd:string\"/>");
		printLine(1, "</xsd:simpleType>");

		printLine("");
		printLine(1, "<xsd:simpleType name=\"typeFloatSequence\">");
		printLine(2, "<xsd:annotation>");
		printLine(3, "<xsd:documentation xml:lang=\"en\">");
		printAnnotation(4, getTypeDescription("FloatSequence"));
		printLine(3, "</xsd:documentation>");
		printLine(2, "</xsd:annotation>");
		printLine(2, "<xsd:list itemType=\"xsd:float\"/>");
		printLine(1, "</xsd:simpleType>");

		printLine("");
		printLine(1, "<xsd:simpleType name=\"typeID\">");
		printLine(2, "<xsd:annotation>");
		printLine(3, "<xsd:documentation xml:lang=\"en\">");
		printAnnotation(4, getTypeDescription("ID"));
		printLine(3, "</xsd:documentation>");
		printLine(2, "</xsd:annotation>");
		printLine(2, "<xsd:restriction base=\"xsd:string\">");
		printLine(3, "<xsd:pattern value=\"[^:]+://[^/]+/.+\"/>");
		printLine(2, "</xsd:restriction>");
		printLine(1, "</xsd:simpleType>");

		printLine("");
		printLine(1, "<xsd:complexType name=\"typeValue\">");
		printLine(2, "<xsd:annotation>");
		printLine(3, "<xsd:documentation xml:lang=\"en\">");
		printAnnotation(4, getTypeDescription("Value"));
		printLine(3, "</xsd:documentation>");
		printLine(2, "</xsd:annotation>");
		printLine(2, "<xsd:simpleContent>");
		printLine(3, "<xsd:extension base=\"xsd:double\">");
		printLine(4, "<xsd:attribute name=\"Units\" type=\"xsd:string\">");
		printLine(5, "<xsd:annotation>");
		printLine(6, "<xsd:documentation xml:lang=\"en\">");
		printAnnotation(7, getTermDefinition("Units"));
		printLine(6, "</xsd:documentation>");
		printLine(5, "</xsd:annotation>");
		printLine(4, "</xsd:attribute>");
		printLine(4, "<xsd:attribute name=\"UnitsConversion\" type=\"xsd:string\">");
		printLine(5, "<xsd:annotation>");
		printLine(6, "<xsd:documentation xml:lang=\"en\">");
		printAnnotation(7, getTermDefinition("UnitsConversion"));
		printLine(6, "</xsd:documentation>");
		printLine(5, "</xsd:annotation>");
		printLine(4, "</xsd:attribute>");
		printLine(3, "</xsd:extension>");
		printLine(2, "</xsd:simpleContent>");
		printLine(1, "</xsd:complexType>");
		
		// Complex types defined with ontology
		// This should be for all dictionary items with a type that begins with a "+"
		defineType("ElementBoundary");
	}
	
	/**
	 * Generate XML schema complextType for a type defined with an ontology
	 **/
	public void defineType(String name) throws Exception {
		String query;
		Statement statement;
		ResultSet resultSet;

		query = "select" + " ontology.*" + " from ontology" 
				+ " where ontology.Version='" + mModelVersion + "'"
				+ " and ontology.Object='" + name + "'"
				;

		statement = this.beginQuery();
		resultSet = this.select(statement, query);
		printLine("");
		printLine(1, "<xsd:complexType name=\"type" + name + "\">");
		printLine(2, "<xsd:annotation>");
        printLine(3, "<xsd:documentation xml:lang=\"en\">");
        printLine(4, getTypeDescription(name));
        printLine(3, "</xsd:documentation>");
        printLine(2, "</xsd:annotation>");
        printLine(2, "<xsd:sequence>");
		// Generate types for each list
		while (resultSet.next()) {
			String elem = getXSLName(resultSet.getString("Element"));
			String type = getXSLName(resultSet.getString("Type"));
			if(type.isEmpty()) type =  mNamespace + ":" + elem ;
	        printLine(3, "<xsd:element name=\"" + elem + "\" type=\"" + type + "\" " + getXSLOccurance(resultSet.getString("Occurence")) + " />" );
		}
        printLine(2, "</xsd:sequence>");
		printLine(1, "</xsd:complexType>");

		// Clean-up
		this.endQuery(statement, resultSet);		
	}
	


	public String getTypeDescription(String name)
			throws Exception 
	{
		String desc = "";
		String query = "";
		
		query = "select" + " type.*" + " from type" + " where"
				+ " type.Version='"	+ mModelVersion + "'"
				+ " and type.Name='" + name + "'"
				;

		Statement statement = this.beginQuery();
		ResultSet resultSet = this.select(statement, query);
		while (resultSet.next()) {
			desc = resultSet.getString("Description");
		}
		
		// Clean-up
		this.endQuery(statement, resultSet);
		
		return desc;
	}

	public boolean isTermLocalType(String term)
			throws Exception 
	{
		boolean localType = false;
		String query = "";
		
		query = "select" + " dictionary.*" + " from dictionary" + " where"
				+ " dictionary.Version='"	+ mModelVersion + "'"
				+ " and dictionary.Term='" + term + "'"
				;

		Statement statement = this.beginQuery();
		ResultSet resultSet = this.select(statement, query);
		while (resultSet.next()) {
			if(resultSet.getString("Type").startsWith("+")) {	// Special case - use another class
				localType = true;
				break;
			}
		}
		
		// Clean-up
		this.endQuery(statement, resultSet);
		
		return localType;
	}
	

	public String getTermType(String term)
			throws Exception 
	{
		String type = "";
		String query = "";
		
		query = "select" + " dictionary.*" + " from dictionary" + " where"
				+ " dictionary.Version='"	+ mModelVersion + "'"
				+ " and dictionary.Term='" + term + "'"
				;

		Statement statement = this.beginQuery();
		ResultSet resultSet = this.select(statement, query);
		while (resultSet.next()) {
			type = term;
			if(resultSet.getString("Type").equals("Enumeration")) {	// Special case - use enumeration list name
				type = resultSet.getString("List");
			}
			if(resultSet.getString("Type").startsWith("+")) {	// Special case - use another class
				type = resultSet.getString("Type").substring(1);
			}
		}
		
		// Clean-up
		this.endQuery(statement, resultSet);
		
		return type;
	}
	
	public String getTermDefinition(String term)
			throws Exception 
	{
		String desc = "";
		String query = "";
		
		query = "select" + " dictionary.*" + " from dictionary" + " where"
				+ " dictionary.Version='"	+ mModelVersion + "'"
				+ " and dictionary.Term='" + term + "'"
				;

		Statement statement = this.beginQuery();
		ResultSet resultSet = this.select(statement, query);
		while (resultSet.next()) {
			desc = resultSet.getString("Definition");
		}
		
		// Clean-up
		this.endQuery(statement, resultSet);
		
		return desc;
	}

	/**
	 * Generate XML schema description of every list item
	 **/
	public void makeLists() throws Exception {
		String query;
		Statement statement;
		ResultSet resultSet;
		String desc;
		String listName;
		String buffer;

		query = "select" + " list.*" + " from list" + " where list.Version='"
				+ mModelVersion + "'";

		statement = this.beginQuery();
		resultSet = this.select(statement, query);

		printLine("<!-- ================================");
		printLine("      Lists");
		printLine("     ================================ -->");

		if(mExtend.isEmpty()) {	// Only include in a base schema
			// Generate enumeration for version
			printLine("<!-- ==========================");
			printLine("Version");
			printLine("========================== -->");
	
			printLine(1, "<xsd:simpleType name=\"Version\">");
			printAnnotation(2, "Version number.");
			printLine(1, "<xsd:restriction base=\"xsd:string\">");
			printLine(2, "<xsd:enumeration value=\"" + mModelVersion + "\" />");
			printLine(1, "</xsd:restriction>");
			printLine("</xsd:simpleType>");
		}
		
		// Generate types for each list
		while (resultSet.next()) {
			buffer = resultSet.getString("Name");
			// listName = "enum" + getXSLName(buffer);
			listName = getXSLName(buffer);
			if (igpp.util.Text.isMatch(resultSet.getString("Type"), "Open")) {
				desc = "Open List. See: " + resultSet.getString("Reference");
			} else {
				desc = resultSet.getString("Description");
			}

			printLine("<!-- ==========================");
			printLine(resultSet.getString("Name"));
			printLine("");
			printLine(igpp.util.Text.wordWrap(desc, 40, ""));
			printLine("========================== -->");

			if (igpp.util.Text.isMatch(resultSet.getString("Type"), "Open")) {
				printLine(1, "<xsd:element name=\"" + listName
						+ "\" type=\"xsd:string\">");
				printAnnotation(2, desc);
				printLine(1, "</xsd:element>");
			} else if (igpp.util.Text.isMatch(resultSet.getString("Type"), "Union")) {
				printLine(1, "<xsd:simpleType name=\"" + listName + "\">");
				printAnnotation(2, desc);
				printLine(1, "<xsd:union");
				// makeEnumUnion("enum", resultSet.getString("Reference"));
				makeEnumUnion(mNamespace + ":", resultSet.getString("Reference"));
				printLine(1, "/>");
				printLine("</xsd:simpleType>");
			} else { // Closed
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
	 * Determine if a dictionary term is an enumeration return the name of the
	 * enumeration list or "" if not an enumeration
	 **/
	public String getEnumeration(String term) throws Exception {
		String query;
		Statement statement;
		ResultSet resultSet;

		String itIs = "";
		String buffer;

		query = "select" + " dictionary.*" + " from dictionary"
				+ " where dictionary.Term = '" + sqlEncode(term) + "'"
				+ " and dictionary.Version='" + mModelVersion + "'";

		statement = this.beginQuery();
		resultSet = this.select(statement, query);

		itIs = "";
		while (resultSet.next()) {
			buffer = resultSet.getString("Type");
			buffer = buffer.trim();
			if (igpp.util.Text.isMatch(buffer, "Enumeration"))
				itIs = resultSet.getString("List");
		}

		// Clean-up
		this.endQuery(statement, resultSet);

		return itIs;
	}

	/**
	 * Create an enumeration list.
	 **/
	public void makeEnum(String prefix, String list) throws Exception {
		String query;
		Statement statement;
		ResultSet resultSet;

		String term;
		String buffer;
		String enumList;

		query = "select" + " member.*" + " from member"
				+ " where member.Version='" + mModelVersion + "'"
				+ " and member.List ='" + list + "'";

		statement = this.beginQuery();
		resultSet = this.select(statement, query);

		while (resultSet.next()) {
			term = resultSet.getString("Term");
			buffer = "";
			if (prefix.length() > 0)
				buffer += prefix + ".";
			buffer += getXSLName(term);
			printLine(2, "<xsd:enumeration value=\"" + buffer + "\">");
			printAnnotation(3, getElementDesc(term));
			printLine(2, "</xsd:enumeration>");
			enumList = getEnumeration(term);
			if (enumList.length() != 0)
				makeEnum(buffer, enumList);
		}
		// Clean-up
		this.endQuery(statement, resultSet);
	}
	
	/**
	 * Create an enumeration list.
	 **/
	public void makeEnumUnion(String prefix, String list) throws Exception {
		String enumList;
		String delim = "";
		
		String[] part = list.split(",");

		enumList = "memberTypes=\"";
		
		for(String p : part) {
			if(p.contains(":")) enumList += delim + p.trim();	// Namespace already defined
			else enumList += delim + prefix + p.trim();
			delim = " ";
		}
		enumList += "\"";
		printLine(enumList);
	}
	
	public void makeDictionary() throws Exception {
		String term;
		String type;
		String desc;

		String query;
		Statement statement;
		ResultSet resultSet;

		query = "select" + " dictionary.*" + " from dictionary"
				+ " where dictionary.Version='" + mModelVersion + "'"
				;

		statement = this.beginQuery();
		resultSet = this.select(statement, query);

		printLine("<!-- ================================");
		printLine("      Dictionary Terms");
		printLine("     ================================ -->");
		
		while (resultSet.next()) {
			term = resultSet.getString("Term");
			type = resultSet.getString("Type");
			desc = resultSet.getString("Definition");

			// Version and Extension are special instances and handled elsewhere.
			if(term.equals("Version")) continue;
			if(term.equals("Extension")) continue;

			if (type.equals("Item")) {	// An item appears in enumerations
				// Do nothing
			} else if (type.equals("Enumeration")) {	// Handled separately
				// Do nothing
			} else if (type.equals("Container")) {	// A set of elements as defined in the ontology
				// Do nothing
			} else 	if(type.equals("Boundary")) {	// Complex content
				printLine(1, "<xsd:complexType name=\"" + getXSLName(term) + "\">");
				printAnnotation(2, desc);
				printLine(2, "<xsd:complexContent>");					
				printLine(3, "<xsd:restriction base=\""	+ getXSLType(type) + "\""
						+ " />");
				printLine(1, "</xsd:complexContent>");					
				printLine(1, "</xsd:complexType>");
			} else 	if(type.equals("Value")) {	// Simple content
				printLine(1, "<xsd:complexType name=\"" + getXSLName(term) + "\">");
				printAnnotation(2, desc);
				printLine(2, "<xsd:simpleContent>");					
				printLine(3, "<xsd:restriction base=\""	+ getXSLType(type) + "\""
						+ " />");
				printLine(1, "</xsd:simpleContent>");					
				printLine(1, "</xsd:complexType>");
			} else {	// Simple base type
				if(isTermLocalType(term)) {
					// don't write
				} else {
					printLine(1, "<xsd:simpleType name=\"" + getXSLName(term) + "\">");
					printAnnotation(2, desc);
					printLine(2, "<xsd:restriction base=\""	+ getXSLType(type) + "\""
						+ " />");
					printLine(1, "</xsd:simpleType>");
				}
			}
		}
		this.endQuery(statement, resultSet);
	}

	public void makeGroup()
			throws Exception {
		String query;
		Statement statement;
		ResultSet resultSet;
		String group = "";
		String currentGroup = "";

		query = "select" + " ontology.*" + " from ontology"
				+ " where ontology.Version='" + mModelVersion + "'"
				+ " Order By ontology.Pointer";

		statement = this.beginQuery();
		resultSet = this.select(statement, query);
		while (resultSet.next()) {
			group = resultSet.getString("Group");
			if( ! group.startsWith(">")) continue;	// Not a group spec.
			if (currentGroup.length() > 0) {	// In a choice
				if( ! group.equals(currentGroup)) {	// Close group
					printLine(2, "</xsd:sequence>");
					printLine(1, "</xsd:group>");
				}
			}
			if (group.startsWith(">")) {	// Indicates member of group - text after ">" is group name
				if( ! group.equals(currentGroup)) {	// Start a new choice
					printLine(1, "<xsd:group name=\"" + group.substring(1) + "\">");
					printLine(2, "<xsd:sequence>");
				}
			}
			currentGroup = group;
			if(group.startsWith(">")) {
				String term = resultSet.getString("Element");
				printLine(3, "<xsd:element name=\"" + getXSLName(term) + "\""
						+ " type=\"" + mNamespace + ":" + getXSLName(term)	+ "\""
						+ " " + getXSLOccurance(resultSet.getString("Occurence"))
						+ " />");				
			}
			
		}
		this.endQuery(statement, resultSet);
		
		// If group was started - finish it
		if (currentGroup.startsWith(">")) {	// Indicates member of group - text after ">" is group name
				printLine(2, "</xsd:sequence>");
				printLine(1, "</xsd:group>");
		}

	}
	
	public void makeTree(String term, String group, boolean addLang) 
		throws Exception {

		ArrayList<String[]> elemList = new ArrayList<String[]>();
				
		printLine(0, "");
		if( ! mExtend.isEmpty()) { // Override if not in a base schema
			printLine(1, "<!-- \"override\" does an implicit \"include\" of the referenced schema, then redfines the element -->");
			printLine(1, "<xsd:override schemaLocation=\"" + mExtend + "\">");
		}
		makeRoot(group, addLang, elemList);
		if( ! mExtend.isEmpty()) {	// Override if not in a base schema
			printLine(1, "</xsd:override>");
			printLine(0, "");
		}
				
		// Extract description of each element
		for (int i = 0; i < elemList.size(); i++) {
			String[] pair = (String[]) elemList.get(i);
			makeBranch(pair[0], pair[1], false);
		}
		
		makeBranch(term, group, addLang);
	}
	
	public void makeRoot(String group, boolean addLang, ArrayList<String[]> elemList)
			throws Exception {

		// Process terms
		// Look for parts of the ontology
		for(String term : mRootList) {
			String query;
			Statement statement;
			ResultSet resultSet;
			int rows = 0;
			String desc;
			String currentGroup = "";
			String occur = "";
			int inc = 0;

			if (isElemOutput(term))
				continue;

			query = "select" + " ontology.*" + " from ontology"
					+ " where ontology.Object = '" + sqlEncode(term) + "'"
					+ " and ontology.Version='" + mModelVersion + "'"
					+ " Order By ontology.Pointer";
	
			// Determine the number of rows
			// We do this the hard way since sqlite is a "FORWARD Only" database
	
			statement = this.beginQuery();
			resultSet = this.select(statement, query);
			rows = 0;
			while (resultSet.next()) {
				rows++;
			}
			this.endQuery(statement, resultSet);
	
			// Now query for results
			statement = this.beginQuery();
			resultSet = this.select(statement, query);
	
			if (rows == 0) { // A leaf
			} else { // Not a leaf
				desc = getElementDesc(term);
	
				// printLine(1, "<xsd:element name=\"" + getXSLName(term)
				// 		+ "\" type=\"" + getXSLName(term) + "\"" + subGroup + "/>");
				printLine(1, "<xsd:complexType name=\"" + getXSLName(term) + "\">");
				printAnnotation(2, desc);
				printLine(2, "<xsd:sequence>");
				while (resultSet.next()) { // We're at the first record coming in to
											// part of the code
					group = resultSet.getString("Group");
					if(group.startsWith(">")) continue;	// Skip - part of a group
					
					if (currentGroup.length() > 0) {	// In a choice
						if( ! group.equals(currentGroup)) {	// Close group
							printLine(3, "</xsd:choice>");
							inc--;
						}
					}
					if (group.length() > 0) {	// part of a choice
						if( ! group.equals(currentGroup)) {	// Start a new choice
							printLine(3, "<xsd:choice "
											+ getXSLOccurance(resultSet.getString("Occurence"))
											+ ">");
							inc++;
						}
					}
					currentGroup = group;
					// Element
					elemList.add(new String[] { resultSet.getString("Element"),	"" });
					occur = getXSLOccurance(resultSet.getString("Occurence"));
					if(currentGroup.length() > 0) occur = ""; 
					String eType = "";
					String comment = "";
					try {
						eType = resultSet.getString("Type");
						if( ! eType.isEmpty()) { comment = "   <!-- defined in " + mExtend + " -->"; }
					} catch(Exception e) {	// Throws exception if field does not exist.
						// Do nothing
					}
	
					if(group.startsWith("[")) {	// Part of a group
						eType = resultSet.getString("Element");
						if(eType.isEmpty()) eType = term;	// If not in local dictionary - use element name
	
						printLine(
								3 + inc,
								// "<xsd:element ref=\""
								//		+ getXSLName(resultSet.getString("Element"))
								//		+ "\" "
								"<xsd:group ref=\"" + mNamespace + ":" + getXSLName(eType) + "\" />" + comment
								);
						
					} else {
						eType = getTermType(resultSet.getString("Element"));
						if(eType.isEmpty()) eType = resultSet.getString("Element");	// If not in local dictionary - use element name
						printLine(
								3 + inc,
								// "<xsd:element ref=\""
								//		+ getXSLName(resultSet.getString("Element"))
								//		+ "\" "
								"<xsd:element name=\""	+ getXSLName(resultSet.getString("Element")) + "\""
										   + " type=\""	+ mNamespace + ":" + getXSLName(eType) + "\""									 
										   + " " + occur 
										   + " />" + comment
								);
					}
				}
				if (currentGroup.length() > 0) {	// In a choice
					printLine(3, "</xsd:choice>");
				}
				printLine(2, "</xsd:sequence>");
				if (addLang) {
					printLine(3,
							"<xsd:attribute name=\"lang\" type=\"xsd:string\" default=\"en\"/>");
				}
				printLine(1, "</xsd:complexType>");
			}
			this.endQuery(statement, resultSet);
		}

		// Look for lists
		for(String name : mRootList) {
			String query;
			Statement statement;
			ResultSet resultSet;
			int rows = 0;

			if (isListOutput(name))
				continue;

			query = "select" + " member.*" + " from member"
					+ " where member.Version='" + mModelVersion + "'"
					+ " and member.List ='" + name + "'";

			statement = this.beginQuery();
			resultSet = this.select(statement, query);

			rows = 0;
			while (resultSet.next()) {
				rows++;
			}
			this.endQuery(statement, resultSet);
			
			if(rows > 0) {
				makeEnum("", name);
			}
		}
	}
	
	public void makeBranch(String term, String group, boolean addLang)
			throws Exception {
		String query;
		Statement statement;
		ResultSet resultSet;
		int rows = 0;
		String desc;
		String[] pair;
		String currentGroup = "";
		String occur = "";
		int inc = 0;

		ArrayList<String[]> elemList = new ArrayList<String[]>();
		if (isElemOutput(term))
			return;

		query = "select" + " ontology.*" + " from ontology"
				+ " where ontology.Object = '" + sqlEncode(term) + "'"
				+ " and ontology.Version='" + mModelVersion + "'"
				+ " Order By ontology.Pointer";

		// Determine the number of rows
		// We do this the hard way since sqlite is a "FORWARD Only" database

		statement = this.beginQuery();
		resultSet = this.select(statement, query);
		rows = 0;
		while (resultSet.next()) {
			rows++;
		}
		this.endQuery(statement, resultSet);

		// Now query for results
		statement = this.beginQuery();
		resultSet = this.select(statement, query);


		if (rows == 0) { // A leaf
			if (term.compareTo("Extension") == 0) { // "Extension" is an exception
				if(mExtend.isEmpty()) {	// Only include in a base schema
					printLine(0, "");
					// printLine(1, "<xsd:element name=\"" + getXSLName(term)
					//		+ "\" type=\"" + getXSLName(term) + "\" />");
					printLine(1, "<xsd:complexType name=\"" + getXSLName(term)
							+ "\">");
					printAnnotation(2, getElementDesc(term));
					printLine(2, "<xsd:sequence>");
					printLine(3, "<xsd:any minOccurs=\"0\" maxOccurs=\"unbounded\" processContents=\"lax\" namespace=\"##other\" />");
					printLine(2, "</xsd:sequence>");
					if (addLang)
						printLine(3,
								"<xsd:attribute name=\"lang\" type=\"xsd:string\" default=\"en\"/>");
					printLine(1, "</xsd:complexType>");
				}
			} else {
				isElemLeaf(term);
			}
		} else { // Not a leaf
			desc = getElementDesc(term);

			if(igpp.util.Text.isInList(term, mRootList)) {	// Just add to elemList - root items written separately
				elemList.add(new String[] { resultSet.getString("Element"),	"" });
			} else {
	
				printLine(0, "");
				/*
				if(igpp.util.Text.isInList(term, mRootList) && ! mExtend.isEmpty()) { // Override if not in a base schema
					printLine(1, "<!-- \"override\" does an implicit \"include\" of the referenced schema, then redfines the element -->");
					printLine(1, "<xsd:override schemaLocation=\"" + mExtend + "\">");
				}
				*/
				// printLine(1, "<xsd:element name=\"" + getXSLName(term)
				// 		+ "\" type=\"" + getXSLName(term) + "\"" + subGroup + "/>");
				printLine(1, "<xsd:complexType name=\"" + getXSLName(term) + "\">");
				printAnnotation(2, desc);
				printLine(2, "<xsd:sequence>");
				while (resultSet.next()) { // We're at the first record coming in to
											// part of the code
					group = resultSet.getString("Group");
					if(group.startsWith(">")) continue;	// Skip - part of a group
					
					if (currentGroup.length() > 0) {	// In a choice
						if( ! group.equals(currentGroup)) {	// Close group
							printLine(3, "</xsd:choice>");
							inc--;
						}
					}
					if (group.length() > 0) {	// part of a choice
						if( ! group.equals(currentGroup)) {	// Start a new choice
							printLine(3, "<xsd:choice "
											+ getXSLOccurance(resultSet.getString("Occurence"))
											+ ">");
							inc++;
						}
					}
					currentGroup = group;
					// Element
					if(isTermLocalType(resultSet.getString("Element"))) {	// Local type is pushed - not actual element
						elemList.add(new String[] { getTermType(resultSet.getString("Element")),	"" });
					} else {
						elemList.add(new String[] { resultSet.getString("Element"),	"" });
					}
					occur = getXSLOccurance(resultSet.getString("Occurence"));
					if(currentGroup.length() > 0) occur = ""; 
					String eType = "";
					String comment = "";
					try {
						eType = resultSet.getString("Type");
						if( ! eType.isEmpty()) { comment = "   <!-- defined in " + mExtend + " -->"; }
					} catch(Exception e) {	// Throws exception if field does not exist.
						// Do nothing
					}
	
					if(group.startsWith("[")) {
						eType = resultSet.getString("Element");
						if(eType.isEmpty()) eType = term;	// If not in local dictionary - use element name
	
						printLine(
								3 + inc,
								// "<xsd:element ref=\""
								//		+ getXSLName(resultSet.getString("Element"))
								//		+ "\" "
								"<xsd:group ref=\"" + mNamespace + ":" + getXSLName(eType) + "\" />" + comment
								);
						
					} else {
						eType = getTermType(resultSet.getString("Element"));
						if(eType.isEmpty()) eType = resultSet.getString("Element");	// If not in local dictionary - use element name
						printLine(
								3 + inc,
								// "<xsd:element ref=\""
								//		+ getXSLName(resultSet.getString("Element"))
								//		+ "\" "
								"<xsd:element name=\""	+ getXSLName(resultSet.getString("Element")) + "\""
										   + " type=\""	+ mNamespace + ":" + getXSLName(eType) + "\""									 
										   + " " + occur 
										   + " />" + comment
								);
					}
				}
				if (currentGroup.length() > 0) {	// In a choice
					printLine(3, "</xsd:choice>");
				}
				printLine(2, "</xsd:sequence>");
				if (addLang) {
					printLine(3,
							"<xsd:attribute name=\"lang\" type=\"xsd:string\" default=\"en\"/>");
				}
				printLine(1, "</xsd:complexType>");
				/*
				if(igpp.util.Text.isInList(term, mRootList) && ! mExtend.isEmpty()) {	// Override is not in a base schema
					printLine(1, "</xsd:override>");
					printLine(0, "");
				}
				*/
			}
		}
		this.endQuery(statement, resultSet);

		// Extract description of each element
		for (int i = 0; i < elemList.size(); i++) {
			pair = (String[]) elemList.get(i);
			makeBranch(pair[0], pair[1], false);
		}
	}

	/* Check if a term is in the output list. Add it if not. */
	public boolean isElemOutput(String term) {
		if (igpp.util.Text.isInList(term, mElemList))
			return true;

		/* Add to list */
		mElemList.add(term);

		return false;
	}
	
	/* Check if a list is in the output list. Add it if not. */
	public boolean isListOutput(String name) {
		if (igpp.util.Text.isInList(name, mListList))
			return true;

		/* Add to list */
		mListList.add(name);

		return false;
	}

	/* Check if a term is in the leaf list. Add it if not. */
	public boolean isElemLeaf(String term) {
		if (igpp.util.Text.isInList(term, mElemLeaf))
			return true;

		/* Add to list */
		mElemLeaf.add(term);

		return false;
	}

	public String getXSLName(String term) {
		// Strip spaces, dashes and single quotes
		String buffer;

		buffer = term.replace("-", "");
		buffer = buffer.replace("\'", "");
		buffer = buffer.replace(" ", "");

		return buffer;
	}

	public String getElementGroup(String term) throws Exception {
		String query;
		Statement statement;
		ResultSet resultSet;
		String buffer;

		query = "select" + " ontology.*" + " from ontology"
				+ " where ontology.Element = '" + sqlEncode(term) + "'"
				+ " and ontology.Version='" + mModelVersion + "'"
				+ " Order By ontology.Pointer";

		statement = this.beginQuery();
		resultSet = this.select(statement, query);

		buffer = "";
		while (resultSet.next()) {
			if (buffer.length() == 0)
				buffer = resultSet.getString("Group");
		}
		// Clean-up
		this.endQuery(statement, resultSet);

		return buffer;
	}

	public String getElementDesc(String term) throws Exception {
		String query;
		Statement statement;
		ResultSet resultSet;
		String buffer;

		query = "select" + " dictionary.*" + " from dictionary"
				+ " where dictionary.Term = '" + sqlEncode(term) + "'"
				+ " and dictionary.Version='" + mModelVersion + "'";

		statement = this.beginQuery();
		resultSet = this.select(statement, query);

		buffer = "";
		while (resultSet.next()) {
			buffer = resultSet.getString("Definition");
		}
		// Clean-up
		this.endQuery(statement, resultSet);

		return buffer;
	}

	public String getElementType(String term) throws Exception {
		String query;
		Statement statement;
		ResultSet resultSet;
		String buffer;

		query = "select" + " dictionary.*" + " from dictionary"
				+ " where dictionary.Term = '" + sqlEncode(term) + "'"
				+ " and dictionary.Version='" + mModelVersion + "'";

		statement = this.beginQuery();
		resultSet = this.select(statement, query);

		buffer = "";
		while (resultSet.next()) {
			buffer = resultSet.getString("Type");
		}
		// Clean-up
		this.endQuery(statement, resultSet);

		return buffer;
	}

	public String getElementList(String term) throws Exception {
		String query;
		Statement statement;
		ResultSet resultSet;
		String buffer;

		query = "select" + " dictionary.*" + " from dictionary"
				+ " where dictionary.Term = '" + sqlEncode(term) + "'"
				+ " and dictionary.Version='" + mModelVersion + "'";

		statement = this.beginQuery();
		resultSet = this.select(statement, query);

		buffer = "";
		while (resultSet.next()) {
			buffer = resultSet.getString("List");
		}
		// Clean-up
		this.endQuery(statement, resultSet);

		return buffer;
	}

	public String getElementEnum(String term) throws Exception {
		String query;
		Statement statement;
		ResultSet resultSet;
		String buffer;

		query = "select" + " dictionary.*" + " from dictionary"
				+ " where dictionary.Term = '" + sqlEncode(term) + "'"
				+ " and dictionary.Version='" + mModelVersion + "'";

		statement = this.beginQuery();
		resultSet = this.select(statement, query);

		buffer = "";
		while (resultSet.next()) {
			buffer = resultSet.getString("List");
		}
		// Clean-up
		this.endQuery(statement, resultSet);

		return buffer;
	}

	// ===========================================================
	// Utlity functions
	// ===========================================================
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

	public void print(String text) {
		try {
			if (mWriter != null)
				mWriter.print(text);
			if (mStream != null)
				mStream.print(text);
			if (mServlet != null)
				mServlet.print(text);
		} catch (Exception e) {
		}
	}

	public void print(int indent, String text) {
		printIndent(indent);
		printLine(text);
	}

	public void printLine(String text) {
		try {
			if (mWriter != null)
				mWriter.println(text);
			if (mStream != null)
				mStream.println(text);
			if (mServlet != null)
				mServlet.println(text);
		} catch (Exception e) {
		}
	}

	public void printLine(int indent, String text) {
		printIndent(indent);
		printLine(text);
	}

	public void printIndent(int indent) {
		for (int i = 0; i <= indent; i++)
			print("   ");
	}

	public String getIndent(int indent) {
		String buffer = "";

		for (int i = 0; i <= indent; i++)
			buffer += "   ";

		return buffer;
	}

	public void printAnnotation(int indent, String desc) throws Exception {
		desc = igpp.util.Text.wordWrap(desc, 40, getIndent(indent + 2));
		desc = igpp.util.Encode.htmlEncode(desc);
		printLine(indent, "<xsd:annotation>");
		printLine(indent + 1, "<xsd:documentation xml:lang=\"en\">");
		printLine(desc);
		printLine(indent + 1, "</xsd:documentation>");
		printLine(indent, "</xsd:annotation>");
	}

	public boolean printTerm(String term, int indent, int occur, String pointer)
			throws Exception {
		String query;
		Statement statement;
		ResultSet resultSet;

		boolean isContainer = false;

		query = "select" + " dictionary.*" + " from dictionary"
				+ " where dictionary.Term = '" + sqlEncode(term) + "'"
				+ " and dictionary.Version='" + mModelVersion + "'";

		statement = this.beginQuery();
		resultSet = this.select(statement, query);

		while (resultSet.next()) {
			print(indent, "<xsd:element name=\"" + term);
			if (igpp.util.Text
					.isMatch(resultSet.getString("Type"), "Container")) {
				printLine(">");
				printLine(indent + 1, "<xsd:complexType>");
				isContainer = true;
			} else if (igpp.util.Text.isMatch(resultSet.getString("Type"),
					"Item")) {
				printLine(">");
				printLine(indent + 1, "<xsd:complexType>");
				printLine(indent + 1, "</xsd:complexType>");
				printLine(indent, "</xsd:element>");
			} else {
				printLine("\" type=\""
						+ getXSLType(resultSet.getString("Type")) + "\" />");
			}
		}
		// Clean-up
		this.endQuery(statement, resultSet);

		return isContainer;
	}
	
	public String getXSLType(String type, String element)
	{
		if(type.isEmpty()) return mNamespace + ":" + element;
		
		if(type.contains(":")) return type;	// From another namespace
		
		return getXSLType(type);	// Internal types
	}

	public String getXSLType(String type) {
		// XML Schema built-in types
		if (igpp.util.Text.isMatch(type, "Count"))
			return "xsd:integer";
		if (igpp.util.Text.isMatch(type, "DateTime"))
			return "xsd:dateTime";
		if (igpp.util.Text.isMatch(type, "Duration"))
			return "xsd:duration";
		if (igpp.util.Text.isMatch(type, "Numeric"))
			return "xsd:double";
		if (igpp.util.Text.isMatch(type, "Text"))
			return "xsd:string";
		if (igpp.util.Text.isMatch(type, "URL"))
			return "xsd:anyURI";
		
		// Internally defined types
		if (igpp.util.Text.isMatch(type, "Boundary"))
			return "spase:typeBoundary";
		if (igpp.util.Text.isMatch(type, "Value"))
			return "spase:typeValue";
		if (igpp.util.Text.isMatch(type, "Sequence"))
			return "spase:typeSequence";
		if (igpp.util.Text.isMatch(type, "StringSequence"))
			return "spase:typeStringSequence";
		if (igpp.util.Text.isMatch(type, "FloatSequence"))
			return "spase:typeFloatSequence";
		if (igpp.util.Text.isMatch(type, "ID"))
			return "spase:typeID";

		// Defined differently prior to version 1.2.0
		// Date was a xsd:dateTime and Time was xsd:duration. 
		if (igpp.util.Text.isMatch(type, "Date"))
			return "xsd:date";
		if (igpp.util.Text.isMatch(type, "Time"))
			return "xsd:time";

		return "xsd:string";	// Default
	}

	public String getXSLOccurance(String occur) {
		occur = occur.trim();
		if (igpp.util.Text.isMatch(occur, "0"))
			return "minOccurs=\"0\" maxOccurs=\"1\""; // Optional
		if (igpp.util.Text.isMatch(occur, "1"))
			return "minOccurs=\"1\" maxOccurs=\"1\""; // One only
		if (igpp.util.Text.isMatch(occur, "+"))
			return "minOccurs=\"1\" maxOccurs=\"unbounded\""; // At least one,
																// perhaps many
		if (igpp.util.Text.isMatch(occur, "*"))
			return "minOccurs=\"0\" maxOccurs=\"unbounded\""; // Any number

		return ""; // Default
	}

	// Query database for most recent version
	public String getModelVersion() throws Exception {
		String query;
		Statement statement;
		ResultSet resultSet;

		String version = mModelVersion;

		if (version == null) {
			query = "select" + " * " + " from history" + " where"
					+ " history.ID = (Select max(history.ID) from history)";

			statement = this.beginQuery();
			resultSet = this.select(statement, query);

			while (resultSet.next()) {
				version = resultSet.getString("Version");
			}

			this.endQuery(statement, resultSet);
		}
		return version;
	}

	public String sqlEncode(String text) {
		return igpp.util.Encode.sqlEncode(text);
	}

	// Argument passing when a bean
	public void setVersion(String value) {
		mModelVersion = value;
	}

	public String getVersion() {
		return mModelVersion;
	}
}
