/**
 * Creates an XML style sheet to represent each container of metadata.
 * Each stylesheet is placed in the current directory with the basename
 * of the container.
 * Queries the data model database to build the schema.
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
import java.util.Iterator;

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
public class MakeXSL extends Query {
	private String mVersion = "1.0.0";
	private boolean mVerbose= false;

	private String mModelVersion = null;
	private String mHomepath = "";
	private String mOutpath = "";

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

	// Enumeration of Type
	private final int TypeContainer = 0;
	private final int TypeCount = 1;
	private final int TypeDate = 2;
	private final int TypeEnumeration = 3;
	private final int TypeItem = 4;
	private final int TypeNumeric = 5;
	private final int TypeText = 6;
	private final int TypeTime = 7;

	private String mOverview = "Creates a set of XSL documents that can be used to display and edit documents.\n"
			 ;
	private String mAcknowledge = "Development funded by NASA's VMO project at UCLA.";



	// create the Options
	Options mAppOptions = new org.apache.commons.cli.Options();

	public MakeXSL() {
		mAppOptions.addOption( "h", "help", false, "Dispay this text" );
		mAppOptions.addOption( "v", "verbose", false, "Verbose. Show status at each step." );
		mAppOptions.addOption( "e", "edit", false, "Edit. Create XSL for use in an editor.  Default is create for display." );
		mAppOptions.addOption( "p", "homepath", true, "Homepath. The path to tools and database. Default: current folder" );
		mAppOptions.addOption( "d", "database", true, "Database. The name of the SQLite database file. Default: " + mDatabase );
		mAppOptions.addOption( "m", "model", true, "Model. The version number of the model to generate." );
		mAppOptions.addOption( "o", "outpath", true, "Output. The folder name to output the generated file." );
	}

	public static void main(String args[]) {
		boolean forEdit = false;
		MakeXSL me = new MakeXSL();


		if (args.length < 1) {
	   		me.showHelp();
    		return;
		}

		CommandLineParser parser = new BasicParser();
        try {
            CommandLine line = parser.parse(me.mAppOptions, args);

  			if(line.hasOption("h")) me.showHelp();
   			if(line.hasOption("v")) me.mVerbose = true;
   			if(line.hasOption("e")) forEdit = true;
   			if(line.hasOption("m")) { me.mModelVersion = line.getOptionValue("m"); }
   			if(line.hasOption("p")) { me.mHomepath = line.getOptionValue("p"); }
   			if(line.hasOption("d")) { me.mDatabase = line.getOptionValue("d"); }
   			if(line.hasOption("o")) { me.mOutpath = line.getOptionValue("o"); }

			// Fix paths to end with delimiter
   			if ( ! me.mHomepath.endsWith("/")) me.mHomepath += "/";
			if ( ! me.mOutpath.endsWith("/")) me.mOutpath += "/";
			
			System.err.println("Homepath: " + me.mHomepath);
			me.setDatabaseDriver("SQLite");
			me.setDatabase(me.mHost, me.mHomepath + me.mDatabase);
			me.setUserLogin(me.mUsername, me.mPassword);
			me.useUser();

			me.setWriter(System.out);
			me.makeAll(forEdit);
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
		setDatabase(mHost, mHomepath + mDatabase);
		setUserLogin(mUsername, mPassword);
		useUser();

		if(mModelVersion == null) mModelVersion = getModelVersion();
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
		response.setHeader("Content-Disposition",
				"attachment; filename=\"spase-" + mModelVersion.replace(".", "_")
						+ ".xsd\"");

		setWriter(out);
		makeAll();
	}

	/**
	 * Scan through list of items and return a list of all "container" items.
	 * 
	 * @param list
	 *            the list of element names to scan.
	 * 
	 * @return list of container items found in list.
	 **/
	public ArrayList getImportList(ArrayList list) throws Exception {
		ArrayList matchList = new ArrayList();
		String buffer;
		String name;

		for (Iterator i = list.iterator(); i.hasNext();) {
			buffer = (String) i.next(); // Occurrence token prepended to name
			name = buffer.substring(1);

			// Create form entry based on element type
			switch (getElementTypeToken(name)) {
			case TypeContainer:
				matchList.add(name);
				break;
			}
		}

		return matchList;
	}

	/**
	 * Scan through list of items and return a list parameters. An item with an
	 * occurance of 0, 1, or - is a parameter.
	 * 
	 * @param list
	 *            the list of element names to scan.
	 * 
	 * @return list of parameter items found in list.
	 **/
	public ArrayList getParamList(ArrayList list) throws Exception {
		ArrayList matchList = new ArrayList();

		for (Iterator i = list.iterator(); i.hasNext();) {
			String buffer = (String) i.next(); // Occurrence token prepended to
												// name
			char occur = buffer.charAt(0);
			String elemName = buffer.substring(1);

			switch (occur) {
			case '0':
			case '1':
			case '-':
				matchList.add(elemName);
				break;
			}
		}

		return matchList;
	}

	public void writeHeader(String name, String object, boolean isTop,
			boolean forEdit, ArrayList importList, ArrayList paramList)
			throws Exception {
		if (forEdit)
			writeEditHeader(name, object, isTop, importList, paramList);
		else
			writeDisplayHeader(name, isTop, importList, paramList);
	}

	public void writeFooter(String name, boolean isTop, boolean forEdit)
			throws Exception {
		if (forEdit)
			writeEditFooter(name, isTop);
		else
			writeDisplayFooter(name, isTop);
	}

	public void writeForm(String object, ArrayList list, boolean forEdit)
			throws Exception {
		if (forEdit) {
			writeEditForm(object, list);
			return;
		}
		writeDisplayForm(object, list);
	}

	public void writeItem(String object, String name, boolean forEdit)
			throws Exception {
		if (forEdit)
			writeEditItem(object, name);
		else
			writeDisplayItem(object, name);
	}

	public void writeEditHeader(String name, String object, boolean isTop,
			ArrayList list, ArrayList param) throws Exception {
		String today = igpp.util.Date.now();

		printLine("<?xml version=\"1.0\"?>");
		printLine("<!--");
		printLine("Transform " + name + " resource into an editable form.");
		printLine("");
		printLine("Author: Todd King");
		printLine("Since: " + today);
		printLine("-->");
		printLine("<!DOCTYPE stylesheet [");
		printLine("<!ENTITY nbsp  \"&#160;\" ><!-- non-breaking space -->");
		printLine("]>");
		printLine("<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">");
		printLine("");
		writeTemplateList(list);
		printLine("");
		printLine("<xsl:template name=\"" + name + "\" match=\"" + name + "\">");
		printLine("   <xsl:param name=\"elemPath\"><xsl:value-of select=\"ElementPath\"/></xsl:param>");
		printLine("   <xsl:param name=\"position\">-1</xsl:param>");
		printLine("   <xsl:param name=\"multiple\">0</xsl:param>");
		printLine("");
		writeParamList(object, param);
		printLine("");
		if (isTop) {
			printLine("<form id=\"resource\" method=\"post\">");
			printLine("<table>");
			printLine("<tr><td><b>"
					+ name
					+ "</b></td><td width=\"100%\" align=\"right\"><input type=\"submit\" name=\"action\" Value=\"Update\" /></td></tr>");
			printLine("</table>");
			printLine("<hr/>");
			printLine("<table>");
		} else {
			printLine("<tr><td valign=\"top\"><div class=\"tab\">" + name
					+ "</div></td><td>");
			printLine("	<xsl:if test=\"$multiple\">");
			printLine("		<xsl:if test=\"$position!=-1\">");
			printLine("			<xsl:if test=\"$position=1\">&nbsp;<a href=\"?new={$elemPath}\">new</a></xsl:if>");
			printLine("			<xsl:if test=\"$position!=1\">&nbsp;<a href=\"?remove={$elemPath}\">remove</a></xsl:if>");
			printLine("		</xsl:if>");
			printLine("	</xsl:if>");
			printLine("<img alt=\"Hide\" onclick=\"flipImage(this); showHide('{$elemPath}')\" src=\"open.gif\" border=\"0\" />");
			printLine("<img valign=\"top\" alt=\"Info\" onclick=\"showDictionary('"
					+ object + "');\" src=\"info.gif\" />");
			printLine(" </td></tr>");
			printLine("<tr><td colspan=\"2\">");
			printLine("<div id=\"{$elemPath}\" class=\"indent\">");
			printLine("<table>");
		}
	}

	public void writeEditFooter(String name, boolean isTop) throws Exception {
		if (isTop) {
			printLine("<tr><td colspan=\"2\" align=\"right\"><input align=\"right\" type=\"submit\" name=\"action\" Value=\"Update\" /></td></tr>");
			printLine("</table>");
			printLine("</form>");
		} else {
			printLine("</table>");
			printLine("</div>");
			printLine("</td>");
			printLine("</tr>");
		}

		printLine("</xsl:template>");
		printLine("</xsl:stylesheet>");
	}

	public void writeEditForm(String object, ArrayList list) throws Exception {
		String elemName;
		String paramName;
		String name;
		String value;
		String option;
		boolean multiple = false;
		char occur;
		String buffer;
		String required = "<font color=\"red\">*</font>";
		String filler = "";
		String options = "";
		ArrayList enumList;

		printLine("");

		for (Iterator i = list.iterator(); i.hasNext();) {
			buffer = (String) i.next(); // Occurrence token prepended to name
			occur = buffer.charAt(0);
			name = buffer.substring(1);
			elemName = name.replace(" ", "");
			paramName = object.replace(" ", "") + elemName;

			// Determine visual attributes based on occurrence
			filler = "";
			options = "";
			multiple = false;
			value = "";

			switch (occur) {
			case '0':
				filler = "";
				options = "";
				multiple = false;
				value = "$" + paramName + "";
				break;
			case '-':
				filler = "";
				options = "";
				multiple = false;
				value = "$" + paramName + "";
				break;
			case '1':
				filler = required;
				options = "";
				multiple = false;
				value = "$" + paramName + "";
				break;
			case '*':
				filler = "";
				options = makeAddRemove(elemName);
				multiple = true;
				value = "current()";
				break;
			case '+':
				filler = required;
				options = makeAddRemove(elemName);
				multiple = true;
				value = "current()";
				break;
			}

			// Add dictionary lookup
			options += "<img valign=\"top\" alt=\"Info\" onclick=\"showDictionary('"
					+ name + "');\" src=\"info.gif\" />";

			if (multiple) {
				printLine("   <xsl:for-each select=\"" + elemName + "\">");
			}

			// Create form entry based on element type
			switch (getElementTypeToken(name)) {
			case TypeContainer:
				if (multiple) {
					printLine("   <xsl:call-template name=\"" + elemName
							+ "\">");
					printLine("      <xsl:with-param name=\"position\"><xsl:value-of select=\"position()\"/></xsl:with-param>");
					printLine("      <xsl:with-param name=\"multiple\">1</xsl:with-param>");
					printLine("   </xsl:call-template>");
				} else {
					printLine("   <xsl:apply-templates select=\"" + elemName
							+ "\" />");
				}
				break;
			case TypeCount:
			case TypeDate:
			case TypeItem:
			case TypeNumeric:
			case TypeText:
			case TypeTime:
				printLine("   <tr><td valign=\"top\">");
				if (multiple)
					printLine("  <xsl:if test=\"position()=1\">" + name
							+ filler + ":</xsl:if>");
				else
					printLine(name + filler + ":");
				printLine("</td><td><input type=\"text\" name=\"{$elemPath}/"
						+ elemName + "\" size=\"30\" value=\"{" + value
						+ "}\" />" + options + "</td></tr>");
				break;
			case TypeEnumeration:
				enumList = makeEnum("", getEnumeration(name));
				printLine("   <tr><td valign=\"top\">");
				if (multiple)
					printLine("  <xsl:if test=\"position()=1\">" + name
							+ filler + ":</xsl:if>");
				else
					printLine(name + filler + ":");
				printLine("</td><td><select name=\"{$elemPath}/" + elemName
						+ "\">");
				if (occur == '0' || occur == '*') { // Allow selection of
													// nothing
					printLine("      <xsl:if test=\""
							+ value
							+ "=''\"><option selected=\"yes\"></option></xsl:if>");
					printLine("      <xsl:if test=\"" + value
							+ "!=''\"><option></option></xsl:if>");
				}
				for (Iterator n = enumList.iterator(); n.hasNext();) {
					option = (String) n.next();
					printLine("      <xsl:if test=\"" + value + "='" + option
							+ "'\"><option selected=\"yes\">" + option
							+ "</option></xsl:if>");
					printLine("      <xsl:if test=\"" + value + "!='" + option
							+ "'\"><option>" + option + "</option></xsl:if>");
				}
				printLine("   </select>" + options + "</td></tr>");
				break;
			}

			if (multiple) {
				printLine("   </xsl:for-each>");
			}
		}

		return;
	}

	public void writeParamList(String object, ArrayList list) {
		// Output referenced templates
		String name;

		for (Iterator i = list.iterator(); i.hasNext();) {
			String elemName = (String) i.next();
			elemName = elemName.replace(" ", "");
			String paramName = object.replace(" ", "") + elemName;
			printLine("   <xsl:param name=\"" + paramName
					+ "\"><xsl:value-of select=\"" + elemName
					+ "\"/></xsl:param>");
		}
	}

	public void writeEditItem(String object, String name) throws Exception {
		String elemName = name.replace(" ", "");
		String paramName = object.replace(" ", "") + elemName;

		printLine("   <xsl:param name=\"" + paramName
				+ "\"><xsl:value-of select=\"" + elemName + "\"/></xsl:param>");
	}

	public void writeDisplayHeader(String name, boolean isTop, ArrayList list,
			ArrayList param) throws Exception {
		String today = igpp.util.Date.now();

		printLine("<?xml version=\"1.0\"?>");
		printLine("<!--");
		printLine("Transform " + name
				+ " resource into a form suitable to display.");
		printLine("");
		printLine("Author: Todd King");
		printLine("Since: " + today);
		printLine("-->");
		printLine("<!DOCTYPE stylesheet [");
		printLine("<!ENTITY nbsp  \"&#160;\" ><!-- non-breaking space -->");
		printLine("]>");
		printLine("<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">");
		printLine("");
		writeTemplateList(list);
		printLine("");
		printLine("<xsl:template name=\"" + name + "\" match=\"" + name + "\">");
		if (isTop)
			printLine("<hr/>");
		printLine("<table width=\"100%\">");
		if (igpp.util.Text.isMatch(name, "Person")) {
			printLine("<tr><td width=\"50%\"><xsl:value-of select=\"PersonName\"/></td><td width=\"50%\" align=\"right\"><xsl:value-of select=\"ResourceID\"/></td></tr>");
		} else {
			if (isTop) {
				printLine("<tr><td><xsl:value-of select=\"ResourceID\"/></td><td width=\"50%\" align=\"right\"><xsl:value-of select=\""
						+ name + "\"/></td></tr>");
			} else {
				printLine("<tr><td>" + name + "</td><td></td></tr>");
			}
		}
		printLine("</table>");
		printLine("<div class=\"indent\">");
		printLine("<table>");
		printLine("   <xsl:if test=\"count(child::*)=0\"><tr><td colspan=\"2\"></td></tr></xsl:if>"); // Safegaurd
																										// from
																										// empty
																										// tags
	}

	public void writeDisplayFooter(String name, boolean isTop) throws Exception {
		printLine("</table>");
		printLine("</div>");

		printLine("</xsl:template>");
		printLine("");
		printLine("</xsl:stylesheet>");
	}

	public void writeDisplayForm(String object, ArrayList list)
			throws Exception {
		String elemName;
		String paramName;
		String name;
		String value;
		String option;
		boolean multiple = false;
		char occur;
		String buffer;
		String filler = "";
		String options = "";
		String linkOpen = "";
		String linkClose = "";
		ArrayList enumList;

		printLine("");

		for (Iterator i = list.iterator(); i.hasNext();) {
			buffer = (String) i.next(); // Occurrence token prepended to name
			occur = buffer.charAt(0);
			name = buffer.substring(1);
			elemName = name.replace(" ", "");
			paramName = elemName;

			if (igpp.util.Text.isMatch(paramName, "ResourceID"))
				continue; // Handled in header

			// Determine visual attributes based on occurrence
			filler = "";
			options = "";
			multiple = false;
			value = "";

			switch (occur) {
			case '0':
				multiple = false;
				value = paramName;
				break;
			case '1':
				multiple = false;
				value = paramName;
				break;
			case '*':
				multiple = true;
				value = ".";
				break;
			case '+':
				multiple = true;
				value = ".";
				break;
			}

			linkOpen = "";
			linkClose = "";

			// Special cases
			if (igpp.util.Text.isMatch(paramName, "URL")) {
				multiple = true;
				value = ".";
				linkOpen = "<a href=\"{string(.)}\">";
				linkClose = "</a>";
			}
			if (paramName.endsWith("ID")) {
				multiple = true;
				value = ".";
				linkOpen = "<a href=\"{$ResourceURL}{string(.)}\">";
				linkClose = "</a>";
			}

			if (multiple) {
				printLine("   <xsl:for-each select=\"" + elemName + "\">");
			}

			// Create form entry based on element type
			switch (getElementTypeToken(name)) {
			case TypeContainer:
				if (multiple) {
					printLine("   <tr><td colspan=\"2\"><xsl:call-template name=\""
							+ elemName + "\" /></td></tr>");
				} else {
					printLine("   <tr><td colspan=\"2\"><xsl:apply-templates select=\""
							+ elemName + "\" /></td></tr>");
				}
				break;
			case TypeCount:
			case TypeDate:
			case TypeItem:
			case TypeNumeric:
			case TypeText:
			case TypeTime:
			case TypeEnumeration:
				print("   <xsl:if test=\"string-length(" + value
						+ ") &gt; 0\">"); // Show if it has content
				print("   <tr><td valign=\"top\">");
				if (multiple)
					print("  <xsl:if test=\"position()=1\">");
				print(name + ":");
				if (multiple)
					print("</xsl:if>");
				print("   </td>");
				print("<td>");
				print(linkOpen);
				print("<xsl:value-of select=\"" + value + "\"/>");
				print(linkClose);
				print("</td></tr>");
				printLine("</xsl:if>");
				break;
			}

			if (multiple) {
				printLine("   </xsl:for-each>");
			}
		}

		return;
	}

	public void writeDisplayItem(String object, String name) throws Exception {
		// Do nothing
	}

	public void writeTemplateList(ArrayList list) {
		// Output referenced templates
		String name;

		for (Iterator i = list.iterator(); i.hasNext();) {
			name = (String) i.next();
			name = name.replace(" ", "");
			printLine("   <xsl:import href=\"" + name + ".xsl\" />");
		}
	}

	public String makeAddRemove(String name) {
		return ("\n"
				+ "      <xsl:if test=\"position()=1\">&nbsp;<a href=\"?new={$elemPath}/"
				+ name
				+ "\">new</a></xsl:if>\n"
				+ "      <xsl:if test=\"position()!=1\">&nbsp;<a href=\"?remove={$elemPath}/"
				+ name + "[{position()-1}]\">remove</a></xsl:if>\n" + "   ");
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
	public ArrayList makeEnum(String prefix, String list) throws Exception {
		String query;
		Statement statement;
		ResultSet resultSet;

		String term;
		String buffer;
		String enumName;
		ArrayList enumList = new ArrayList();

		query = "select" + " member.*" + " from member"
				+ " where member.Version='" + mModelVersion + "'"
				+ " and member.List ='" + list + "'";

		statement = this.beginQuery();
		resultSet = this.select(statement, query);

		enumList.clear();
		while (resultSet.next()) {
			term = resultSet.getString("Term");
			buffer = "";
			if (prefix.length() > 0)
				buffer += prefix + ".";
			buffer += getXSLName(term);
			enumList.add(buffer);
			enumName = getEnumeration(term);
			if (enumName.length() != 0)
				enumList.addAll(makeEnum(buffer, enumName));
		}
		// Clean-up
		this.endQuery(statement, resultSet);

		return enumList;
	}

	public void makeAll() throws Exception {
		makeAll(true);
	}

	public void makeAll(boolean forEdit) throws Exception {
		ArrayList topLevel = getTopLevelElements();

		for (Iterator i = topLevel.iterator(); i.hasNext();) {
			makeObject((String) i.next(), true, forEdit);
		}
	}

	public void makeObject(String object, boolean isTop, boolean forEdit)
			throws Exception {
		String query;
		Statement statement;
		ResultSet resultSet;

		PrintStream out;
		String lastObject = "";
		String elem = "";
		String occur = "";
		String xmlName = "";
		boolean needFooter = false;
		ArrayList list = new ArrayList();
		ArrayList importList = new ArrayList();
		ArrayList paramList = new ArrayList();
		ArrayList temp;

		query = "select" + " ontology.*" + " from ontology"
				+ " where ontology.Version='" + mModelVersion + "'"
				+ " and ontology.Object='" + object + "'"
				+ " Order By ontology.Object, ontology.Pointer";

		statement = this.beginQuery();
		resultSet = this.select(statement, query);

		xmlName = object.replace(" ", "");
		out = new PrintStream(mOutpath + xmlName + ".xsl");
		setWriter(out);
		System.out.println(object);

		// Build up list of elements
		list.clear();
		while (resultSet.next()) {
			elem = resultSet.getString("Element");
			occur = resultSet.getString("Occurence");
			list.add(occur + elem);
		}
		importList = getImportList(list);
		paramList = getParamList(list);
		writeHeader(xmlName, object, isTop, forEdit, importList, paramList);

		// Write out each item
		lastObject = "";
		while (resultSet.next()) {
			elem = resultSet.getString("Element");
			occur = resultSet.getString("Occurence");
			writeItem(object, elem, forEdit);
		}
		this.endQuery(statement, resultSet);

		writeForm(object, list, forEdit);
		writeFooter(object, isTop, forEdit);

		closeWriter();

		// Make all sub-objects
		for (Iterator i = list.iterator(); i.hasNext();) {
			String buffer = (String) i.next();
			String name = buffer.substring(1);
			makeObject(name, false, forEdit);
		}
	}

	public String getXSLName(String term) {
		// Strip spaces, dashes and single quotes
		String buffer;

		buffer = term.replace("-", "");
		buffer = buffer.replace("\'", "");
		buffer = buffer.replace(" ", "");

		return buffer;
	}

	public ArrayList getTopLevelElements() throws Exception {
		String query;
		Statement statement;
		ResultSet resultSet;

		ArrayList list = new ArrayList();

		query = "select" + " ontology.*" + " from ontology"
				+ " where ontology.Version='" + mModelVersion + "'"
				+ " and ontology.Object='Spase'";

		statement = this.beginQuery();
		resultSet = this.select(statement, query);

		list.clear();
		while (resultSet.next()) {
			list.add(resultSet.getString("Element"));
		}

		this.endQuery(statement, resultSet);

		return list;
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

	public int getElementTypeToken(String term) throws Exception {
		String type = getElementType(term);

		if (isMatch(type, "Container"))
			return TypeContainer;
		if (isMatch(type, "Count"))
			return TypeCount;
		if (isMatch(type, "DateTime"))
			return TypeDate;
		if (isMatch(type, "Duration"))
			return TypeTime;
		if (isMatch(type, "Enumeration"))
			return TypeEnumeration;
		if (isMatch(type, "Item"))
			return TypeItem;
		if (isMatch(type, "Numeric"))
			return TypeNumeric;
		if (isMatch(type, "Text"))
			return TypeText;

		// Obsolete as of version 1.2.0
		if (isMatch(type, "Date"))
			return TypeDate;
		if (isMatch(type, "Time"))
			return TypeTime;

		return TypeText;
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

	public void closeWriter() throws Exception {
		if (mWriter != null)
			mWriter.close();
		if (mStream != null)
			mStream.close();
		if (mServlet != null)
			mServlet.close();
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

	public void printIndent(int indent) {
		for (int i = 0; i <= indent; i++)
			print("   ");
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

	public boolean isMatch(String base, String text) {
		if (base.compareToIgnoreCase(text) == 0) {
			return true;
		}
		return false;
	}

	public boolean isInList(ArrayList list, String text) {
		String base;

		for (Iterator i = list.iterator(); i.hasNext();) {
			base = (String) i.next();
			if (base.compareToIgnoreCase(text) == 0) {
				return true;
			}
		}
		return false;
	}

	// Argument passing when a bean
	public void setVersion(String value) {
		mModelVersion = value;
	}

	public String getVersion() {
		return mModelVersion;
	}
}