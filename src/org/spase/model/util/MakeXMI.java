/**
 * Creates an XMI document to describe a version of the SPASE data model.
 * Queries the data model database to build the schema.
 *
 * @author Todd King
 * @version 1.00 2007 09 10
 * @copyright 2007 Regents University of California. All Rights Reserved
 */

package org.spase.model.util;

// import igpp.*
import igpp.servlet.MultiPrinter;
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
public class MakeXMI extends Query {
	private String mVersion = "1.0.1";

	private String mModelVersion = null;
	private String mHomepath = "";
	private boolean mAnnotate = true;

	// Database access variables
	private String mHost = "";
	private String mDatabase = "spase-model.db";
	private String mUsername = "";
	private String mPassword = "";

	private JspWriter mWriter = null;
	private PrintStream mStream = null;
	private ServletOutputStream mServlet = null;

	ArrayList<String> mElemList = new ArrayList<String>();
	ArrayList<String> mElemLeaf = new ArrayList<String>();
	ArrayList<String> mEnumList = new ArrayList<String>();

	private String mOverview = "Creates an XMI document based on an information model definition.\n"
			 ;
	private String mAcknowledge = "Development funded by NASA's VMO project at UCLA.";


	private int mAssociationCount = 0;
	private boolean mVerbose= false;
	
	// create the Options
	Options mAppOptions = new org.apache.commons.cli.Options();

	public MakeXMI() {
		mAppOptions.addOption( "h", "help", false, "Dispay this text" );
		mAppOptions.addOption( "v", "verbose", false, "Verbose. Show status at each step." );
		mAppOptions.addOption( "a", "annotate", false, "annotation. Annotate the generated file." );
		mAppOptions.addOption( "p", "homepath", true, "Homepath. The path to tools and database. Default: current folder" );
		mAppOptions.addOption( "d", "database", true, "Database. The name of the SQLite database file. Default: " + mDatabase );
		mAppOptions.addOption( "m", "model", true, "Model. The version number of the model to generate." );
	}

	public static void main(String args[]) {
		MakeXMI me = new MakeXMI();

		if (args.length < 1) {
	   		me.showHelp();
    		return;
		}

		CommandLineParser parser = new BasicParser();
        try {
            CommandLine line = parser.parse(me.mAppOptions, args);

  			if(line.hasOption("h")) me.showHelp();
   			if(line.hasOption("v")) me.mVerbose = true;
   			if(line.hasOption("a")) { me.mAnnotate = true; }
   			if(line.hasOption("m")) { me.mModelVersion = line.getOptionValue("m"); }
   			if(line.hasOption("p")) { me.mHomepath = line.getOptionValue("p"); }
   			if(line.hasOption("d")) { me.mDatabase = line.getOptionValue("d"); }
			
			// Final fix-up
   			if(me.mHomepath.isEmpty()) me.mHomepath = new File(".").getAbsolutePath();
			if ( ! me.mHomepath.endsWith("/")) me.mHomepath += "/";
			
			me.setDatabaseDriver("SQLite");
			me.setDatabase(me.mHost, me.mHomepath + me.mDatabase);
			me.setUserLogin(me.mUsername, me.mPassword);
			me.useUser();

			me.setWriter(System.out);
			me.makeXMI();
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
				"attachment; filename='spase-"
						+ mModelVersion.replace(".", "_") + ".xmi'");

		setWriter(out);
		makeXMI();
	}

	public void makeXMI() throws Exception {
		String today = igpp.util.Date.now();

		String packageName = "SPASE_" + mModelVersion.replace(".", "_");

		printLine("<?xml version='1.0' encoding='UTF-8'?>");
		printLine("<!-- Automatically created based on the dictionary stored at http://www.spase-group.org -->");
		printLine("<!-- Version: " + mModelVersion + " -->");
		printLine("<!-- Generated: " + today + " -->");
		printLine("<xmi:XMI xmi:version='2.1' xmlns:uml='http://schema.omg.org/spec/UML/2.0' xmlns:xmi='http://schema.omg.org/spec/XMI/2.1'>");
		printLine(1,
				"<xmi:Documentation xmi:Exporter='SPASE MakeXMI' xmi:ExporterVersion='"
						+ mVersion + "' />");
		printLine(1,
				"<uml:Model name='SPASE_MODEL' xmi:id='SPASE_MODEL' visibility='public'>");

		printLine(1, "<packagedElement xmi:type='uml:Package' xmi:id='"
				+ packageName + "' name='" + packageName
				+ "' visibility='public'>");

		makeTree("Spase", "");
		makeTypes();
		makeEnum();

		printLine(1, "</packagedElement>");
		printLine(1, "</uml:Model>");
		printLine(1, "</xmi:XMI>");
	}

	/**
	 * Generate XML schema description of non-standard data types
	 **/
	public void makeTypes() throws Exception {
		printLine("<!-- ================================");
		printLine("      Types");
		printLine("     ================================ -->");

		printLine(
				1,
				"<ownedMember xmi:type='uml:DataType' name='int' visibility='public' xmi:id='Count'/>");
		printLine(
				1,
				"<ownedMember xmi:type='uml:DataType' name='DateTime' visibility='public' xmi:id='DateTime'/>");
		printLine(
				1,
				"<ownedMember xmi:type='uml:DataType' name='Duration' visibility='public' xmi:id='Duration'/>");
		printLine(
				1,
				"<ownedMember xmi:type='uml:DataType' name='double' visibility='public' xmi:id='Numeric'/>");
		printLine(
				1,
				"<ownedMember xmi:type='uml:DataType' name='Sequence' visibility='public' xmi:id='Sequence'/>");
		printLine(
				1,
				"<ownedMember xmi:type='uml:DataType' name='String' visibility='public' xmi:id='String'/>");
		printLine(
				1,
				"<ownedMember xmi:type='uml:DataType' name='URL' visibility='public' xmi:id='URL'/>");

		// Older types ???
		printLine(
				1,
				"<ownedMember xmi:type='uml:DataType' name='DataExtent' visibility='public' xmi:id='DataExtent'/>");
		printLine(
				1,
				"<ownedMember xmi:type='uml:DataType' name='TimeSpan' visibility='public' xmi:id='TimeSpan'/>");
	}

	/**
	 * Generate XML schema description for enumerations
	 **/
	public void makeEnum() throws Exception {
		String query;
		Statement statement;
		ResultSet resultSet;

		String name = "";
		String enumName = "";

		printLine("<!-- ================================");
		printLine("      Enumerations");
		printLine("     ================================ -->");

		query = "select distinct" + " member.List" + " from member"
				+ " where member.Version='" + mModelVersion + "'"
				+ " Order By member.List";

		// Now query for results
		statement = this.beginQuery();
		resultSet = this.select(statement, query);

		do {
			name = resultSet.getString("List");
			enumName = getXMIName(name);
			printLine(1, "<packagedElement xmi:type='uml:Enumeration' xmi:id='"
					+ enumName + "' name='" + enumName
					+ "' visibility='public'>");
			printLine(
					2,
					"<ownedComment xmi:type='uml:Comment' xmi:id='" + enumName
							+ "_doc' body='"
							+ igpp.util.Encode.htmlEncode(getElementDesc(name))
							+ "' >");
			printLine(3, "<annotatedElement xmi:idref='" + enumName + "'/>");
			printLine(4, "</ownedComment>");
			makeEnumValues(getXMIName(name), "", name);
			printLine(1, "</packagedElement>");
		} while (resultSet.next());

		this.endQuery(statement, resultSet);
	}

	/**
	 * Generate XML schema description for enumerations
	 * 
	 * @param prefix
	 *            context of the list.
	 * @param list
	 *            name of the list
	 **/
	public void makeEnumValues(String parent, String prefix, String list)
			throws Exception {
		String query;
		Statement statement;
		ResultSet resultSet;

		String listName = getXMIName(list);
		String value = "";
		String enumValue = "";
		String id;

		query = "select" + " member.Term" + " from member"
				+ " where member.List = '" + sqlEncode(list) + "'"
				+ " and member.Version='" + mModelVersion + "'"
				+ " Order By member.Term";

		statement = this.beginQuery();
		resultSet = this.select(statement, query);

		if (prefix.length() > 0)
			prefix += ".";

		do {
			value = resultSet.getString("Term");
			enumValue = getXMIName(value);
			id = parent + "." + prefix + enumValue;
			printLine(2,
					"<ownedLiteral xmi:type='uml:EnumerationLiteral' xmi:id='"
							+ id + "' name='" + prefix + enumValue
							+ "' visibility='public'>");
			printLine(
					3,
					"<ownedComment xmi:type='uml:Comment' xmi:id='"
							+ id
							+ "_doc' body='"
							+ igpp.util.Encode
									.htmlEncode(getElementDesc(value)) + "' >");
			printLine(4, "<annotatedElement xmi:idref='" + id + "'/>");
			printLine(3, "</ownedComment>");
			printLine(2, "</ownedLiteral>");
			if (isEnumeration(value)) {
				makeEnumValues(parent, prefix + enumValue, value);
			}
		} while (resultSet.next());

		this.endQuery(statement, resultSet);
	}

	public void makeTree(String term, String group) throws Exception {
		String query;
		Statement statement;
		ResultSet resultSet;
		int rows = 0;
		String[] list;

		String className;
		String elemName;
		String name;
		String id;
		String id_b;
		String assocID;
		String lower;
		String upper;
		String aggtype;

		ArrayList elemList = new ArrayList();
		ArrayList assocList = new ArrayList();

		if (isElemOutput(term))
			return;

		query = "select" + " ontology.*" + " from ontology"
				+ " where ontology.Object = '" + sqlEncode(term) + "'"
				+ " and ontology.Version='" + mModelVersion + "'"
				+ " Order By ontology.Pointer";

		// Determine the number of rows
		// We do htis the hard way since sqlite is a "FORWARD Only" database

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
			isElemLeaf(term);
		} else { // Not a leaf
			className = getXMIName(term);

			printLine(2, "<packagedElement" + " xmi:type='uml:Class'"
					+ " name='" + className + "'" + " xmi:id='" + className
					+ "'" + " visibility='public' "
					+ " isAbstract='false' isActive='false' isLeaf='false'>");
			printLine(
					4,
					"<ownedComment xmi:type='uml:Comment' xmi:id='" + className
							+ "_doc' body='"
							+ igpp.util.Encode.htmlEncode(getElementDesc(term))
							+ "' >");
			printLine(5, "<annotatedElement xmi:idref='" + className + "'/>");
			printLine(4, "</ownedComment>");
			do { // We're at the first record coming in to part of the code
				name = null;
				id = null;
				elemList.add(new String[] { resultSet.getString("Element"),
						group });
				elemName = resultSet.getString("Element");
				name = getXMIName(elemName);
				id = getXMIName(term) + "_" + name;
				lower = getXMILowerValue(resultSet.getString("Occurence"));
				upper = getXMIUpperValue(resultSet.getString("Occurence"));
				aggtype = getXMIAggregation(elemName);
				if (aggtype.compareTo("composite") == 0) {
					id = getAssociationName(className, elemName) + "_a";
				}
				id_b = getAssociationName(className, elemName) + "_b";

				printLine(3, "<ownedAttribute  xmi:type='uml:Property' name='"
						+ name + "' xmi:id='" + id + "' type='"
						+ getXMIType(elemName) + "' aggregation='" + aggtype
						+ "' " + getAssociation(className, elemName)
						+ " ownerScope='instance' visibility='public'>");
				printLine(
						4,
						"<ownedComment xmi:type='uml:Comment' xmi:id='"
								+ id
								+ "_doc' body='"
								+ igpp.util.Encode
										.htmlEncode(getElementDesc(elemName))
								+ "' >");
				printLine(5, "<annotatedElement xmi:idref='" + id + "'/>");
				printLine(4, "</ownedComment>");
				printLine(4, "<lowerValue xmi:id='" + id
						+ "_lower_cardinality' value='" + lower
						+ "' xmi:type='uml:LiteralInteger'/>");
				if (upper.compareTo("*") == 0) {
					printLine(4, "<upperValue xmi:id='" + id
							+ "_upper_cardinality' value='" + upper
							+ "' xmi:type='uml:LiteralUnlimitedNatural' />");
				} else {
					printLine(4, "<upperValue xmi:id='" + id
							+ "_upper_cardinality' value='" + upper
							+ "' xmi:type='uml:LiteralInteger'/>");
				}
				printLine(3, "</ownedAttribute>");

				if (isContainer(elemName)) { // Draw association
					assocList
							.add(new String[] { className, name, lower, upper });
				}
			} while (resultSet.next());
			printLine(2, "</packagedElement>");
		}
		this.endQuery(statement, resultSet);

		for (int i = 0; i < assocList.size(); i++) {
			list = (String[]) assocList.get(i);
			className = list[0];
			elemName = list[1];
			lower = list[2];
			upper = list[3];
			assocID = "assoc_" + getXMIName(term) + "_" + elemName;
			printLine(2, "<packagedElement xmi:type='uml:Association' xmi:id='"
					+ assocID + "'>");
			printLine(3, "<memberEnd xmi:idref='" + assocID + "_a" + "'/>");
			printLine(3, "<memberEnd xmi:idref='" + assocID + "_b" + "'/>");
			printLine(3, "<ownedEnd xmi:type='uml:Property'  xmi:id='"
					+ assocID + "_b" + "' association='" + assocID
					+ "' isNavigable='true' type='" + getXMIName(className)
					+ "' visibility='public'>");
			printLine(4, "<lowerValue value='" + lower + "' xmi:id='" + assocID
					+ "_b_lower_cardinality' xmi:type='uml:LiteralInteger'/>");
			if (upper.compareTo("*") == 0) {
				printLine(
						4,
						"<upperValue value='1' xmi:id='"
								+ assocID
								+ "_b_upper_cardinality' xmi:type='uml:LiteralInteger'/>");
			} else {
				printLine(
						4,
						"<upperValue value='1' xmi:id='"
								+ assocID
								+ "_b_upper_cardinality' xmi:type='uml:LiteralInteger'/>");
			}
			printLine(3, "</ownedEnd>");
			printLine(2, "</packagedElement>");
		}

		// Extract description of each element
		for (int i = 0; i < elemList.size(); i++) {
			list = (String[]) elemList.get(i);
			makeTree(list[0], list[1]);
		}
	}

	/* construct the association name */
	public String getAssociationName(String className, String term)
			throws Exception {
		String buffer = getElementType(term);
		if (igpp.util.Text.isMatch(buffer, "container")) {
			return "assoc_" + getXMIName(className) + "_" + getXMIName(term);
		}

		return "";
	}

	/* Determine the association attribute */
	public String getAssociation(String className, String term)
			throws Exception {
		String buffer = getElementType(term);
		if (igpp.util.Text.isMatch(buffer, "container")) {
			return "association='" + getAssociationName(className, term)
					+ "_a' ";
		}

		return "";
	}

	/* Check if a term is in the output list. Add it if not. */
	public boolean isElemOutput(String term) {
		if (igpp.util.Text.isInList(term, mElemList))
			return true;

		/* Add to list */
		mElemList.add(term);

		return false;
	}

	/* Check if a term is of container type. */
	public boolean isContainer(String term) {
		String buffer = "";
		try {
			buffer = getElementType(term);
		} catch (Exception e) {
		}

		if (igpp.util.Text.isMatch(buffer, "Container"))
			return true;

		return false;
	}

	/* Check if a term is of enumeration type. */
	public boolean isEnumeration(String term) {
		String buffer = "";
		try {
			buffer = getElementType(term);
		} catch (Exception e) {
		}

		if (igpp.util.Text.isMatch(buffer, "Enumeration"))
			return true;

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

	public String getXMIName(String term) {
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

	public String getXMIType(String term) throws Exception {
		String buffer = getElementType(term);
		if (igpp.util.Text.isMatch(buffer, "container")) {
			buffer = term; // Its a data type
		}
		if (igpp.util.Text.isMatch(buffer, "enumeration")) {
			buffer = getElementEnum(term); // Its a data type
		}
		if (igpp.util.Text.isMatch(buffer, "Text")) { // Change - conflicts with
														// "Text" enumeration
			buffer = "String"; // Its a data type
		}

		return getXMIName(buffer);
	}

	public String getXMIAggregation(String term) throws Exception {
		String buffer = getElementType(term);
		if (igpp.util.Text.isMatch(buffer, "container")) {
			return "composite";
		}

		return "none";
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

	public String getXMITypeID(String type) {
		if (igpp.util.Text.isMatch(type, "Count"))
			return "integer_id";
		if (igpp.util.Text.isMatch(type, "DateTime"))
			return "string_id";
		if (igpp.util.Text.isMatch(type, "Duration"))
			return "string_id";
		if (igpp.util.Text.isMatch(type, "Numeric"))
			return "double_id";
		if (igpp.util.Text.isMatch(type, "Sequence"))
			return "string_id";
		if (igpp.util.Text.isMatch(type, "Text"))
			return "string_id";
		if (igpp.util.Text.isMatch(type, "Item"))
			return "string_id";

		// Obsolete data types as of version 1.2.0
		if (igpp.util.Text.isMatch(type, "Date"))
			return "string_id";
		if (igpp.util.Text.isMatch(type, "Time"))
			return "string_id";

		return type;
	}

	public String getXMILowerValue(String occur) {
		occur = occur.trim();
		if (igpp.util.Text.isMatch(occur, "0"))
			return "0"; // Optional
		if (igpp.util.Text.isMatch(occur, "1"))
			return "1"; // One only
		if (igpp.util.Text.isMatch(occur, "+"))
			return "1"; // At least one, perhaps many
		if (igpp.util.Text.isMatch(occur, "*"))
			return "0"; // Any number

		return ""; // Default
	}

	public String getXMIUpperValue(String occur) {
		occur = occur.trim();
		if (igpp.util.Text.isMatch(occur, "0"))
			return "1"; // Optional
		if (igpp.util.Text.isMatch(occur, "1"))
			return "1"; // One only
		if (igpp.util.Text.isMatch(occur, "+"))
			return "*"; // At least one, perhaps many
		if (igpp.util.Text.isMatch(occur, "*"))
			return "*"; // Any number

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

	public void setVersion(boolean value) {
		mAnnotate = value;
	}

	public boolean getAnnotate() {
		return mAnnotate;
	}
}