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

public class MakeXML extends Query
{
	private String	mVersion = "1.0.0";

	private String	mModelVersion = null;

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
		MakeXML me = new MakeXML();

		if (args.length < 1) {
			System.err.println("Version: " + me.mVersion);
			System.err.println("Usage: " + me.getClass().getName() + " version");
			System.exit(1);
		}

		try {
			me.mModelVersion = args[0];
			me.mModelVersion = me.getModelVersion();

			me.setDatabaseDriver("SQLite");
			me.setDatabase(me.mHost, me.mDatabase);
			me.setUserLogin(me.mUsername, me.mPassword);
			me.useUser();

			me.setWriter(System.out);
			me.makeXML();
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
		response.setHeader("Content-Disposition", "attachment; filename=\"spase-" + mModelVersion.replace(".", "_") + ".xml\"");

		setWriter(out);
		makeXML();
	}

	public void makeXML()
		throws Exception
	{
		String today = igpp.util.Date.now();

		printLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		printLine("<!-- Automatically created based on the dictionary stored at http://www.spase-group.org -->");
		printLine("<!-- Version: " + mModelVersion + " -->");
		printLine("<!-- Generated: " + today + " -->");

		PrintXML("Spase", 0);
	}

	public void PrintXML(String term, int indent)
		throws Exception
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;

		int	count = 0;

		query = "select"
			+ " ontology.*"
			+ " from ontology"
			+ " where ontology.Object = '" + sqlEncode(term) + "'"
			+ " and ontology.Version='" + mVersion + "'"
			+ " Order By ontology.Pointer"
			;

		statement = this.beginQuery();
		resultSet = this.select(statement, query);

		PrintTerm(term, indent, true);
		count = 0;
		while(resultSet.next())	{	// Show elements
			if(count == 0) print("\n");
			count++;
			PrintXML(resultSet.getString("Element"), indent+1);
		}
		// Clean-up
	   this.endQuery(statement, resultSet);

		PrintTerm(term, (count == 0 ? 0 : indent), false);
	}

	public void PrintTerm(String term, int indent, boolean openTerm)
	{
		String buffer = "";

		printIndent(indent);

	   if(openTerm) print("<" + getXSLName(term) + ">");
	   if(!openTerm) print("</" + getXSLName(term) + ">\n");
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

	public void printLine(String text)
	{
    	try {
	    	if(mWriter != null) mWriter.println(text);
	    	if(mStream != null) mStream.println(text);
	    	if(mServlet != null) mServlet.println(text);
    	} catch(Exception e) {
    	}
	}

	public void printIndent(int indent)
	{
		for(int i = 0; i < indent; i++) print("   ");
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

	public String getXSLName(String term)
	{
		// Strip spaces, dashes and single quotes
		String	buffer;

		buffer = term.replace("-", "");
		buffer = buffer.replace("\'", "");
		buffer = buffer.replace(" ", "");

		return buffer;
	}

	public String sqlEncode(String text) { return igpp.util.Encode.sqlEncode(text); }

	// Argument passing when a bean
	public void setVersion(String value) { mModelVersion = value; }
	public String getVersion() { return mModelVersion; }
}