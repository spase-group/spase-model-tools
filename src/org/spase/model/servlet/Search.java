/**
 *  Perform a data dictionary search and extraction.
 *
 * When used as a bean a call to init() or init(String) will establish the
 * connection to the database containing the model specification.
 * For the init() method, a JNDI resource with the reference name "jdbc/spase" must
 * be defined in the web.xml file. For the init(String) method the passed
 * String is name of the properties file (see igpp.database.Query).
 *
 * @author Todd King
 * @version 1.00 2006
 */
package org.spase.model.servlet;
 
// import igpp.*
import igpp.servlet.SmartHttpServlet;
import igpp.database.Query;
import igpp.util.Encode;


// import javax.sql.*;
import java.sql.Statement;
import java.sql.ResultSet;

//import java.io.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

// import java.util.*
import java.util.ArrayList;


import java.util.HashMap;

//import java.servlet.jsp.*;
import javax.servlet.jsp.JspWriter;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Search extends SmartHttpServlet
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2716930304317489091L;
	static final String mVersion = "1.0.3";
	
// Local variables
	String	host = "";
	String	database = "spase-model";
	String	username = "";
	String	password = "";
	String	driverName = "SQLite";

	boolean	doSearch = false;
	boolean	showLinks = false;
	boolean	showOccur = false;
	boolean	showAttrib = false;
	boolean	showValues = false;
	int	recurseDepth = 1;
	
	String	term = "";
	String	version = "";
	String	since = "";
	String	style = "entry";
	String	scope = "dictionary";
	int		generation = 2;
	String	submit = "";
	String	path = "";
	String  contextName = "Base";
	
	String realPath = "";
	
	String	scriptName = "search.html";
		
	// Query access = new Query();
	
	Query access = new Query();
	
	ArrayList<String> mGroupList = new ArrayList<String>();
	
	HashMap<String, String> contextToNameMap = new HashMap<String, String>();
	HashMap<String, String> typeToContextMap = new HashMap<String, String>();
	
	public static void main(String[] args) 
	{
		if(args.length < 0) {
			System.out.println("Version: " + mVersion);
			System.out.println("Usage: igpp.bean.Search");
			System.exit(1);
		}
		
		Search search = new Search();
		
		try {
			search.version = "1.0.0";
			search.init("test/data", "spase-sim"); // search.database);
			search.mOut.setOut(System.out);
			search.style = "xml";
			search.doTask(search.mOut);
		} catch(Exception e) {
			e.printStackTrace();			
		}
	}
	
	public void initMap()
	{
		// Map database name to model name
		contextToNameMap.put("spase-model", "Base");
		contextToNameMap.put("spase-sim", "Simulation Extensions");
		
		// Map type prefix to the context (database) name
		typeToContextMap.put("base", "spase-model");
		typeToContextMap.put("sim", "spase-sim");
	}
	

	public void reset()
	{
		this.doSearch = false;
		this.showLinks = false;
		this.showOccur = false;
		this.showAttrib = false;
		this.showValues = false;	
		this.term = "";
		this.version = "";
		this.style = "entry";
		this.scope = "dictionary";
		this.generation = 2;
		this.submit = "";
		this.since = "0.0.0";
		this.recurseDepth = 1;	
		this.path = "";
		this.contextName = "Base";
	}
	
	public void destroy()
	{
		try {
			this.access.closeConnection();
		} catch(Exception e) {
		}
	}
	
	/**
	 * Servlet initialization.
	 */
   public void init()
   	throws ServletException
   {
		try {
			ServletContext context = getServletContext();
			String realPath = context.getRealPath("/data");	// Should use context.getContextPath() but it's not available ???
			if(realPath == null) realPath = "";
			
			// setPath(path);
			setRealPath(realPath);

			System.out.println("From init() - Initalize with: spase-model.db; path: " + realPath);

			init(realPath, database);
		} catch(Exception e) {
			throw new ServletException(e);
		}
   }

	
	/**
	 * Servlet initialization.
	 */
  public void init(String path, String database)
  	throws ServletException
  {
		try {
			initMap();
			
			// Close connections - if established
			System.out.println("Closing connections...");
			
			// this.access.closeConnection();
			
			System.out.println("init: " + path + ":" + database + ".db");
			
			// Establish connections to database server
			/* MYSQL
			getAccess(this.database).setUserLogin(this.username, this.password);
			getAccess(this.database).setDatabase(this.host, this.database);
			getAccess(this.database).useUser();

			getAccess(this.database).useResource("jdbc/spase");
			*/
			
			// Try to close connection first
			try {
				this.access.closeConnection();
			} catch(Exception e) {
			}

			this.database = database;
			this.access.setDatabaseDriver("SQLite");
			this.access.setDatabase(path, database + ".db");
			this.access.connect();		

		} catch(Exception e) {
			throw new ServletException(e);
		}
  }

   /**
    * For programmatic initialization, for example, when used as a Bean
    * 
    * @param context	the servlet (application) context.
    * @param request	the servlet request.
    * @param database	name of the SQLite database containing the data dictionary.
    * @throws ServletException	if failure to connect to database.
    */
   public void init(ServletContext context, HttpServletRequest request, String database)
   	throws ServletException
   {
		try {
			String realPath = igpp.util.Text.getPath(context.getRealPath(request.getServletPath()));
			
			// setPath(path);
			setRealPath(realPath);
						
			System.out.println("Initalize with: " + database + "; path: " + realPath + "; context: " + contextName);
			
			init(realPath, database);
/*			
			getAccess(this.database).setDatabaseDriver("SQLite");
			getAccess(this.database).setDatabase(path, database + ".db");
			getAccess(this.database).connect();		
*/
		} catch(Exception e) {
			throw new ServletException(e);
		}
   }
	
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
	{
		try {
	    	doAction(request, response);
		} catch(IOException i) {
			throw i;
		} catch(Exception e) {
			throw new ServletException(e);
		}
	}
   
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
	{
		try {
			doAction(request, response);
		} catch(IOException i) {
			throw i;
		} catch(Exception e) {
			e.printStackTrace();
			throw new ServletException(e);
		}
	}
	                
	public void doAction(HttpServletRequest request, HttpServletResponse response)
		throws Exception
	{
		reset();	// Clear query parameters
		
		// Passed parameters
		setShowLinks(igpp.util.Text.getValue(request.getParameter("showLinks"), this.showLinks));
		setShowOccur(igpp.util.Text.getValue(request.getParameter("showOccur"), this.showOccur));
		setShowAttrib(igpp.util.Text.getValue(request.getParameter("showAttrib"), this.showAttrib));
		setShowValues(igpp.util.Text.getValue(request.getParameter("showValues"), this.showValues));
		setTerm(igpp.util.Text.getValue(request.getParameter("term"), this.term));
		setVersion(igpp.util.Text.getValue(request.getParameter("version"), this.version));
		setSince(igpp.util.Text.getValue(request.getParameter("since"), this.since));
		setStyle(igpp.util.Text.getValue(request.getParameter("style"), this.style));
		setScope(igpp.util.Text.getValue(request.getParameter("scope"), this.scope));
		setPath(igpp.util.Text.getValue(request.getParameter("path"), this.path));
		setContext(igpp.util.Text.getValue(request.getParameter("context"), this.database));
		
		// get ready to write response
		try { mOut.setOut(response.getWriter()); } catch(Exception e) { /* do nothing */ }

		setRealPath(igpp.util.Text.getPath(getServletContext().getRealPath(request.getServletPath())));

		if(this.style.compareTo("tree") == 0) { response.setContentType("text/html"); }
		else if(this.style.compareTo("mobile") == 0) { response.setContentType("text/html"); }
		else if(this.style.compareTo("version") == 0) { response.setContentType("text/xml"); }
		else if(this.style.compareTo("history") == 0) { response.setContentType("text/html"); }
		else if(this.style.compareTo("browse") == 0) { response.setContentType("text/xml"); }
		else if(this.style.compareTo("card") == 0) { response.setContentType("text/html"); }
		else if(this.style.compareTo("xml") == 0) { response.setContentType("text/xml");  }
		else if(this.style.compareTo("template") == 0) { response.setContentType("text/xml"); }
		else { response.setContentType("text/html"); }
			
		doTask(mOut);
	}
	
	public void doTask(JspWriter out)
	{
		try {
			contextName =  contextToNameMap.get(database);
			if(contextName == null) contextName = database;

			findVersion();
			
			// scriptName = "search.jsp";
			
			if(this.style.compareTo("tree") == 0) {  showTree(out); }	
			else if(this.style.compareTo("mobile") == 0) { scriptName = "search"; showMobile(out, this.term); } // Special case
			else if(this.style.compareTo("version") == 0) { showVersions(out); }	
			else if(this.style.compareTo("history") == 0) { showHistory(out); }	
			else if(this.style.compareTo("browse") == 0) {  showBrowseTree(out, this.term); } 	
			else if(this.style.compareTo("card") == 0) { showDictionary(out); }	
			else if(this.style.compareTo("xml") == 0) {   showXML(out); }	
			else if(this.style.compareTo("template") == 0) {  showTemplate(out); }	
			else {  showSearch(out); }	// Entry
			
			// this.access.disconnect();
		} catch(Exception e) {
			log(out, e.getMessage());
		}

	}

	public void showHistory(JspWriter out) 
		throws IOException, Exception
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;

		String curVer = "";
				
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.println("<html>");
    	
		query = "select"
	   		+ " * "
	   		+ " from history"
		      + " order by ID DESC"
		    	;
	
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		while(resultSet.next()) {
	    	String ver = resultSet.getString("Version");
	    	if(ver.length() == 0) continue;	// Nothing to report
	    	if(ver.charAt(0) == '0') continue;	// Nothing prior to version 1.0.0
	    	
			if(ver.compareTo(curVer) != 0) {	// New version
				if(curVer.length() > 0) out.println("</table>");
				out.println("Version: <b>" + ver + "</b>");
				out.println("<table class=\"history\">");
				out.println("<tr>");
				out.println("<th width=\"80px\">Updated</th>");
				out.println("<th>Description</th>");
				out.println("<th>Notes</th>");
				out.println("</tr>");
				curVer = ver;
			}

   		String color = "";
   		if(resultSet.getString("Description").compareTo("Released.") == 0) color=" class=\"release\"";
   	
	   	out.println(
	   		  "<tr" + color + ">"
	   		+ "<td>" + resultSet.getString("Updated") + "</td>"
	   		+ "<td>" + resultSet.getString("Description") + "</td>"
	   		+ "<td>" + resultSet.getString("Note") + "</td>"
	   		+ "</tr>"
	   		);
		}

		out.println("</table>");
	   
	   getAccess(this.database).endQuery(statement, resultSet);
	
		out.println("</html>");	
	}
	
	public void showVersions(JspWriter out) 
		throws IOException, Exception
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;

		String curVer = "";
				
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.println("<Response>");
    	
		query = "select"
	   		+ " * "
	   		+ " from history"
		      + " order by ID DESC"
		    	;
	
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		while(resultSet.next()) {
	    	String ver = resultSet.getString("Version");
	    	if(ver.length() == 0) continue;	// Nothing to report
	    	if(ver.charAt(0) == '0') continue;	// Nothing prior to version 1.0.0
	    	if(ver.compareTo(curVer) != 0) {	// Write entry
	    	   out.println("<Release>");
	    	   out.println("   <Version>" + resultSet.getString("Version") + "</Version>");
	    	   out.println("   <LastUpdate>" + resultSet.getString("Updated") + "</LastUpdate>");
	    	   if(resultSet.getString("Description").compareToIgnoreCase("Released.") == 0) {
		    	   out.println("   <Status>Released</Status>");
	    	   } else {
		    	   out.println("   <Status>Draft</Status>");
	    	   }
	    	   out.println("</Release>");
	    	   curVer = ver;
	    	}
	   }
	   
	   getAccess(this.database).endQuery(statement, resultSet);
	
		out.println("</Response>");	
	}
	
	public void showMobile(JspWriter out, String term)
			throws Exception
	{
		// For showMobile the path contains the xPath to the term
		String from = "";
		String fromLabel = "Home";
		String leadPath = "/";
		
		// Parse path - if defined
		if( ! igpp.util.Text.isEmpty(path)) {
			from = igpp.util.Text.getFile(path);
			leadPath = igpp.util.Text.getPath(path);
			fromLabel = from;
			if(fromLabel.isEmpty()) fromLabel = "Home";
			if(leadPath == null) leadPath = "/";
		}
		
		String query;
		Statement	statement;
		ResultSet	resultSet;

		out.println("<!DOCTYPE html>");
		out.println("<html>");
		out.println("<head>");
		out.println("   <meta charset=\"utf-8\" />");
		out.println("   <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />");
		out.println("   <title>SPASE Model</title>");
		out.println("   <link rel=\"stylesheet\" href=\"/_assets/css/themes/default/jquery.mobile-1.2.0.css\" />");
		out.println("   <link rel=\"stylesheet\" href=\"/_assets/css/layout.css\" />");
		out.println("   <script src=\"/_assets/js/jquery-1.8.2.min.js\"></script>");
		out.println("   <script src=\"/_assets/js/jquery.mobile-options.js\"></script>");
		out.println("   <script src=\"/_assets/js/jquery.mobile-1.2.0.js\"></script>");
		out.println("</head>");
		out.println("<body>");
		out.println("<div data-role=\"page\" >");

		out.println("<div data-role=\"header\" data-theme=\"b\" >");
		if(term.isEmpty()) {	// Show back button
			   out.println("      <h1>SPASE Data Model Reference</h1>");
		} else {	// Show bar with buttons
		   out.println("      <a href=\"" + this.scriptName + "?context=" + database + "&style=mobile&version=" + getVersion() + "&term=" + from.replaceAll(" ", "+") + "&path=" + leadPath.replaceAll(" ", "+")  + "\" data-icon=\"back\" data-direction=\"reverse\">" + fromLabel + "</a>");
		   out.println("      <h1>" + term + "</h1>");
		   out.println("      <a href=\"" + this.scriptName + "?context=" + database + "&style=mobile&version=" + getVersion() + "&term=\" data-icon=\"home\" data-iconpos=\"notext\" data-direction=\"reverse\">Home</a>");
		}
		out.println("</div><!-- /header -->");
		out.println("<div data-role=\"content\">");
		
		if(term.equals("")) {    // Root page
			out.println("<div class=\"content-primary\">");

			File info = new File(getRealPath() + "/info.htm");
			
			if(info.exists()) {
				BufferedReader reader = new BufferedReader(new FileReader(info));
				String buff;
				while((buff = reader.readLine()) != null) {
				out.println(buff);
			}
				reader.close();
			}
			out.println("</div> <!-- content-primary -->");
			  
			// Show versions
			String href =  this.scriptName 
					+ "?style=mobile"
					+ "&context=" + database
					+ "&term=Spase"
					+ "&path=/" 
					;

		   String where = "";
			if(database.equals("spase-model")) {	// Only certain versions
				where = " where Version LIKE '%.%.0'"
				   		+ " OR Version LIKE '1.2.%'"
				   		+ " OR Version LIKE '1.3.%'"
				   		+ " OR Version LIKE '2.%.%'"
				   		;
			}

			query = "select"
				   		+ " * "
				   		+ " from history"
				   		+ where
					    + " order by ID DESC"
				   		;
				
			out.println("<div class=\"content-secondary\">");
			out.println("<ul data-role=\"listview\" data-inset=\"true\" data-theme=\"c\" data-dividertheme=\"b\">");
			out.println("<li data-role=\"list-divider\">Version</li>");
			
			String curVer = "";
			boolean showDraft = true;
			boolean show = false;
			
			statement = getAccess(this.database).beginQuery();
			resultSet = getAccess(this.database).select(statement, query);
			while(resultSet.next()) {
		    	String ver = resultSet.getString("Version");
		    	if(ver.length() == 0) continue;	// Nothing to report
		    	if(ver.charAt(0) == '0') continue;	// Nothing prior to version 1.0.0
		    	if(ver.compareTo(curVer) != 0) {	// Write entry
		    	   String updated = " (" + resultSet.getString("Updated") + ")";
		    	   String status = "";
		    	   
		    	   show = false;
		    	   if(resultSet.getString("Description").compareToIgnoreCase("Released.") == 0) {
			    	   status = "Release";
			    	   show = true;
		    	   } else {
			    	   status = "Draft";
			    	   if(showDraft) { show = true; }
			    	   showDraft = false;	// Don't do it again - show first only
		    	   }
		    	   if(show) {
		    	      out.println("<li><a href=\"" + href +  "&version=" + ver + "\">" + ver + updated + "<span class=\"ui-li-count\">" + status + "</span></a></li>");
		    	   }
		    	   curVer = ver;
		    	}
		   }
			out.println("</ul> <!-- listview -->");
			out.println("</div> <!-- content-secondary -->");
		   
		   getAccess(this.database).endQuery(statement, resultSet);
	} else {	// Show Term

			boolean	isNew = false;
			
			if(getScope().equals("type")) {
				query = "select" 
						+ " type.*"
						+ " from type"
						+ " where type.Name='" + Encode.sqlEncode(term) + "'"
						+ " and type.Version='" + this.version + "'"
						;
					
					statement = getAccess(this.database).beginQuery();
					resultSet = getAccess(this.database).select(statement, query);
					
					while(resultSet.next()) {
						out.println(
								  "<!-- Definition -->"
								+ "<div class=\"title\">" + resultSet.getString("Name") + "</div>"
								+ "<div class=\"clear\"></div>"
								+ "<p>"	+ resultSet.getString("Description") + "</p>"
								+ "<p>" + "Since: " + resultSet.getString("Since") + "</p>"
								);
					}
					getAccess(this.database).endQuery(statement, resultSet);		
					
			} else {	// Dictionary
		
				System.out.println("Searching for: " + term + "; version: " + this.version + "; since: " + this.since + "; database: " + this.database);
				
				query = "select" 
					+ " dictionary.*"
					+ " from dictionary"
					+ " where dictionary.Term = '" + Encode.sqlEncode(term) + "'"
					+ " and dictionary.Version='" + this.version + "'"
					;
		
				isNew = false;
				statement = getAccess(this.database).beginQuery();
				resultSet = getAccess(this.database).select(statement, query);
				while(resultSet.next()) {
					isNew = igpp.util.Text.isMatch(resultSet.getString("Since"), this.version);
				}
				getAccess(this.database).endQuery(statement, resultSet);
				
				query = "select" 
					+ " ontology.*"
					+ " from ontology"
					+ " where ontology.Object = '" + Encode.sqlEncode(term) + "'"
					+ " and ontology.Version='" + this.version  + "'"
					+ " Order By ontology.Pointer"
				;
				
				statement = getAccess(this.database).beginQuery();
				resultSet = getAccess(this.database).select(statement, query);
			
				out.println("<div class=\"content-primary\">");
				
				TermDef termDef = getDictionaryTerm(term);
				
				showTerm3(out, termDef, path);
	
				out.println("</div> <!-- content-primary -->");
			  
				out.println("<div class=\"content-secondary\">");
				out.println("<ul data-role=\"listview\" data-inset=\"true\" data-theme=\"c\" data-dividertheme=\"b\">");
				
				boolean needHeader = true;
				
				while(resultSet.next()) {
				   isNew = igpp.util.Text.isMatch(resultSet.getString("Since"), this.version);
				   String typeVal = resultSet.getString("type");
				   String context = database;
				   String versionOpt = "&version=" + getVersion();
				   String[] parts = typeVal.split(":");
				   if(parts.length > 1) {
					   isNew = false;
					   versionOpt = "";	// Will default to latest
					   if(parts[0].equals("base")) { context = "spase-model"; }
					   else { context = parts[0]; }
				   }
				   String element = resultSet.getString("Element");
				   String occur = resultSet.getString("Occurence");
				   String href =  this.scriptName 
						+ "?context=" + context
						+ "&style=mobile"
						+ versionOpt	// "&version=x.x.x if needed 
						+ "&term=" + element.replaceAll(" ", "+")
						+ "&path=" + path + "/" + term.replaceAll(" ", "+");
				   String newText = "";
				   if(isNew) newText = " (new)";
					
				   if(needHeader) { out.println("<li data-role=\"list-divider\">Element</li>"); needHeader = false; }
				   
				   out.println("<li><a href=\"" + href + "\">" + element + newText + "<span class=\"ui-li-count\">" + occur + "</span></a></li>");
				}
				out.println("</ul> <!-- listview -->");
				out.println("</div> <!-- content-secondary -->");
				
				getAccess(this.database).endQuery(statement, resultSet);
			}
		}
		
	      // Finalize page
		out.println("</div><!-- /content -->");
		
		out.println("<div data-role=\"footer\" class=\"footer-docs\" data-theme=\"c\">");
		out.println("       <p class=\"right\"></p>");
		out.println("       <p>&copy; 2013 Regents University of California</p>");
		out.println("</div> <!-- /footer -->");
		
		out.println("</div> <!-- /page -->");
		out.println("</body>");
		out.println("</html>");
		
	}
		
	public void showBrowseTree(JspWriter out, String term)
	throws Exception
	{
		if(igpp.util.Text.isEmpty(term)) term = "Spase";
		
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.println("<tree>");
		showBrowseTree(out, this.database, term, "1", 0);
		out.println("</tree>");
	}
		
	public void showBrowseTree(JspWriter out, String context, String term, String occur, int depth)
		throws Exception
	{
		String query;
		Statement	statement;
		ResultSet	resultSet;

		String	buffer;		
		boolean	isNew = false;
		boolean	showNode = true;
		int		nRow = 0;
		
		if(depth > this.recurseDepth) return;
		depth++;
		
		query = "select" 
			+ " dictionary.*"
			+ " from dictionary"
			+ " where dictionary.Term = '" + Encode.sqlEncode(term) + "'"
			+ " and dictionary.Version='" + this.version + "'"
			;

		isNew = false;
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		while(resultSet.next()) {
			isNew = igpp.util.Text.isMatch(resultSet.getString("Since"), this.version);
		}
		getAccess(this.database).endQuery(statement, resultSet);
		
		query = "select" 
			+ " ontology.*"
			+ " from ontology"
			+ " where ontology.Object = '" + Encode.sqlEncode(term) + "'"
			+ " and ontology.Version='" + this.version  + "'"
			+ " Order By ontology.Pointer"
		;
		
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		
		showNode = true;
		nRow = 0;
		while(resultSet.next()) {
			nRow++;
			
			if(showNode && depth > 1) {
  			   isNew = igpp.util.Text.isMatch(resultSet.getString("Since"), this.version);
				buffer = " term=\"" + term + "\""
					    + " occur=\"" + occur + "\""
					    + " isnew=\"" + isNew + "\""
					    + " context=\"" + this.database + "\""
					    + " type=\"" + getValueType(term) + "\""
					    ;
				out.println("<node " + buffer + ">");
				showNode = false;	// Show once
			}

			// Show elements
			if(resultSet.getString("Type").startsWith("base:")) context = "spase-model";
			else context = this.database;
			
			showBrowseTree(out, context, resultSet.getString("Element"), resultSet.getString("Occurence"), depth);
		}
		getAccess(this.database).endQuery(statement, resultSet);
		
		if(nRow == 0) {	// If no sub-terms - print term
			buffer = " term=\"" + term + "\""
				    + " occur=\"" + occur + "\""
				    + " isnew=\"" + isNew + "\""
				    + " context=\"" + context + "\""
				    + " type=\"" + getValueType(term) + "\""
				    ;
			out.println("<leaf " + buffer + "/>");
		}
		if(!showNode)	out.println("</node>");	// We showed a node
	}

	public void showTree(JspWriter out)
		throws IOException, Exception
	{
		String	checked;
		
		findVersion();
		showVersion(out);
		
		out.println(
				"<form method=\"get\">"
			+ "<input type=\"hidden\" name=\"style\" value=\"tree\">"
			+ "Show: "
			);
			
		checked="";
		if(this.showLinks) checked="checked";
		out.println(
				"<input type=\"checkbox\" name=\"showLinks\" " + checked + ">Links"
			+ "&nbsp;|&nbsp;"
			);
		if(this.generation > 1) {
			checked="";
			if(this.showOccur) checked="checked";
			out.println(
				  "<input type=\"checkbox\" name=\"showOccur\" " + checked + ">Occurence"
				+ "&nbsp;|&nbsp;"
				);
		}
		
		checked="";
		if(this.showAttrib) checked="checked";
		out.println(
			"<input type=\"checkbox\" name=\"showAttrib\" " + checked + ">Attributes"
			+ "&nbsp;|&nbsp;"
			);
			
		checked = "";
		if(this.showValues) checked="checked";
		out.println(
			  "<input type=\"checkbox\" name=\"showValues\" " + checked + ">Possible Values"
			+ "&nbsp;|&nbsp;"
			);
			
		out.println("<br>");
		versionMenu(out, false);
		out.println("<br>");
		
		out.println("<input type=\"submit\" value=\"Refresh\"></form>");
		
		out.println("<a href=\"" + this.scriptName + "?context=" + this.database + "&version=" + this.version + "\">Search the Dictionary</a>");
		
		if(this.generation > 1) {
			out.println(
				  "<br><table class=\"plain\"><tr><td valign=top>Notes:</td><td>Elements marked with [ID] may be an embedded object with the given name or, "
				+ "with \"ID\" appended, a reference to a predefined object. A reference to any object is by its Resource ID.<p>"
				);
			if(this.showOccur) out.println("<p>Occurence specifications are in ( ): 0 = optional, 1 = required, * = zero or more, + = 1 or more"
				+ "; if an element is selected from a group it is indicated with \"of X\" where X is the group letter.");
			out.println("</td></tr></table>");
		}
		
		out.println("<table class=\"tree\">"); 
		if(this.generation > 1) {
			showTree2(out, "Spase", 0, "1", "", "", "");
		} else {
			showTree1(out, "Spase", 0, true, this.showLinks);
		}
		
		out.println("</table>");
	}

	public void showXML(JspWriter out) 
		throws IOException, Exception
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;
		
		TermDef	termDef = new TermDef();
		
		String pattern = Encode.sqlEncode(this.term);
		if(pattern.length() == 0) pattern = "%";	// Everything
		
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.println("<spaseDD xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
    	out.println("xsi:schemaLocation=\"http://www.igpp.ucla.edu/spase/data/schema http://www.igpp.ucla.edu/spase/data/schema/spasedd-1_0_0.xsd\">");
    	
		query = "select" 
		       + " dictionary.*"
		       + " from dictionary"
		       + " where dictionary.Term LIKE '" + pattern + "'"
		       + " and dictionary.Version='" + this.version  + "'"
		       + " order by dictionary.Term"
		       ;
		
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		
    	// Store results
		while(resultSet.next()) {
			termDef.term = resultSet.getString("Term");
			termDef.description = resultSet.getString("Definition");
			termDef.list = resultSet.getString("List");
			termDef.type = resultSet.getString("Type");
			termDef.attributes = resultSet.getString("Attributes");
			showXMLTerm(out, termDef);
		}
		getAccess(this.database).endQuery(statement, resultSet);
		
		out.println("</spaseDD>");
		
	}
	
	
	public void showTemplate(JspWriter out) 
		throws IOException, Exception
	{
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.println("<spaseDD xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
    	out.println("xsi:schemaLocation=\"http://www.igpp.ucla.edu/spase/data/xml http://www.igpp.ucla.edu/spase/data/xml/spasedd.xsd\">");
    	
    	showTemplate(out, "Spase", 0);
		
		out.println("</spaseDD>");
    }
    
	public void showTemplate(JspWriter out, String term, int indent) 
		throws IOException, Exception
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;
		boolean		showName;
		
		String pattern = Encode.sqlEncode(this.term);
		if(pattern.length() == 0) pattern = "%";	// Everything
		
	    query = "select" 
	          + " ontology.*"
	          + " from ontology"
	          + " where ontology.Object = '" + Encode.sqlEncode(term) + "'"
	          + " and ontology.Version='" + this.version + "'"
	          + " Order By ontology.Pointer"
	          ;
		
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		
    	// Process results
    	showName = true;
		while(resultSet.next()) {	// Process sub-elements 
			if(showName) {
				term = resultSet.getString("Object");
				if(term.compareToIgnoreCase("Spase") == 0) term = "SPASE"; 
				showTemplateTerm(out, term, indent, true, false);
				showName = false;
			}
			showTemplate(out, resultSet.getString("Element"), indent+1);
		}
		getAccess(this.database).endQuery(statement, resultSet);
		showTemplateTerm(out, term, indent, showName, true);
	}

	public void showSearch(JspWriter out)
		throws Exception
	{
		String	dirSel = "";
		String	listSel = "";
		String	typeSel = "";
		String	versionText = "";
		int		count = 0;
		String	pattern = "%";
		
		// Check if nothing specified - set default
		dirSel = "";
		if(this.scope.compareTo("dictionary") == 0) dirSel = "checked";
		if(this.scope.compareTo("list") == 0) listSel = "checked";
		if(this.scope.compareTo("type") == 0) typeSel = "checked";
		
		if(dirSel.length() == 0 && listSel.length() == 0 && typeSel.length() == 0) {
			dirSel = "checked";
			this.style = "entry";
			this.scope = "dictionary";
		}
		
		if(this.version.length() == 0) versionText = "Most current";
		else versionText = this.version;

		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.println("<html>");

		// Show search form
		out.println(
			  "<h1>Search the SPASE " + contextName + " dictionary</h1><br>"
			+ "Use the wildcard (*) for unconstrained portions of a search.<br>"
			+ "<form name=\"SearchForm\" method=\"get\" action=\"" + this.scriptName + "\">"
			+ "<input type=\"hidden\" name=\"style\" value=\"entry\" />"
			+ "<input type=\"hidden\" name=\"context\" value=\"" + database + "\" />"
			+ "<table class=\"form\">"
			+ "<tr>"
			+ "<td align=\"center\" colspan=\"2\">Search for: <input type=\"text\" name=\"term\" value=\"" + this.term + "\" /></td>"
			+ "</tr>"
			+ "<tr>"
			+ "<td align=\"center\" colspan=\"2\">"
			+ "In the:"
			+ "&nbsp;<input type=\"radio\" name=\"scope\" value=\"dictionary\" " + dirSel + "/>Dictionary"
			+ "&nbsp;<input type=\"radio\" name=\"scope\" value=\"list\"" + listSel + "/>List"
			+ "&nbsp;<input type=\"radio\" name=\"scope\" value=\"type\"" + typeSel + "/>Data Type"
			+ "</td>"
			+ "</tr>"
			);
			
		versionMenu(out, true);
		sinceMenu(out, true);

		out.println(
			  "<tr>"
			+ "<td align=\"center\" colspan=\"2\" >"
			+ "<input type=\"submit\" name=\"submit\" value=\"Search\" />"
			+ "</td>"
			+ "</tr>"
			+ "<tr>"
			+ "<td align=\"center\" colspan=\"2\"> Dictionary version: " + versionText + " [<a href=history.jsp>History</a>]</td>"
			+ "</tr>"
			+ "<tr>"
			+ "<td align=\"center\" colspan=\"2\" >"
			+ "<a href=\"" + this.scriptName + "?style=tree&version=" + this.version + "&showLinks=on&showOccur=on\">View dictionary as a entity tree</a>"
			+ "</td>"
			+ "</tr>"
			+ "</table>"
			+ "</form>"
			);
		
		// Search and show
		if(this.doSearch) {
			pattern = Encode.sqlEncode(this.term);
			if(pattern.length() == 0) pattern = "%";	// Everything
			
			out.println("<a target=_blank href=card.html?view=" + this.style + ">How to read an entry</a>");
		
			// Dictionary search     
			if(this.scope.compareTo("dictionary") == 0) count = showDictionary(out, pattern);
		
			// Data type search     
			if(this.scope.compareTo("list") == 0) count = showList(out, pattern);
		
			// Data type search     
			if(this.scope.compareTo("type") == 0) count = showType(out, pattern);
		
			if(count == 0) {
				out.println("<br>No matches found.<br>");
			}
		}
		out.println("</html>");
	}   

	public void versionMenu(JspWriter out, boolean inTable)
		throws Exception
	{
		String	checked;
		String	draft;
		String	ver;
		String	curVer = null;
		String	buffer;
		String  where = "";
		
		String	query;
		Statement	statement;
		ResultSet	resultSet;
		
		if(database.equals("spase-model")) {	// Only certain versions
			where = " where Version LIKE '%.%.0'"
			   		+ " OR Version LIKE '1.2.%'"
			   		+ " OR Version LIKE '1.3.%'"
			   		+ " OR Version LIKE '2.%.%'"
				    + " order by ID DESC"
			   		;
		}
		query = "select"
	   		+ " * "
	   		+ " from history"
	   		+ where
		    ;
		if(inTable) {
			out.println(
				  "<tr>"
				+ "<td align=\"center\" colspan=\"2\">"
				);
		}
		out.println(
			  "<table class=\"plain\"><tr><td>Version:</td><td>"
			);
			
    	draft = "(draft)";
    	curVer = "";
    	ver = "";
    	checked = "";
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		while(resultSet.next()) {
	    	ver = resultSet.getString("Version");
	    	if(ver.compareTo(curVer) != 0) {	// Write entry
				curVer = ver;
				checked = "";
		    	draft="(draft)";
		    	if(ver.compareTo(this.version) == 0) checked = "checked";
		    	buffer = resultSet.getString("Description");
		    	if(buffer == null) buffer = "";
		    	if(buffer.compareTo("Released.") == 0) draft = "";
				out.println("&nbsp;<span style=\"white-space:nowrap; display:inline-block\"><input type=\"radio\" name=\"version\" value=\"" + ver + "\" " + checked + ">" + ver + draft + "</span>");   		
	    	}
		}
		out.println(
			  "</td>"
			+ "</tr>"
			+ "</table>"
			);
		
		getAccess(this.database).endQuery(statement, resultSet);
	   
	   if(inTable) {
			out.println(
				  "</td>"
				+ "</tr>"
				);
	   }
		
	}
	
	public void sinceMenu(JspWriter out, boolean inTable)
		throws Exception
	{
		String	checked;
		String	ver;
		boolean	inception = true;
		
		String	query;
		Statement	statement;
		ResultSet	resultSet;
		
		query = "select"
	   		+ " distinct Version "
	   		+ " from history"
	   		+ " where Version LIKE '%.%.0'"
	   		+ " or Version LIKE '1.2.%'"
	   		+ " or Version LIKE '2.%.%'"
		    + " order by Version DESC"
		    ;
		
		if(inTable) {
			out.println(
				  "<tr>"
				+ "<td align=\"center\" colspan=\"2\">"
				);
		}
			
		out.println(
			  "Show Only Items Added Since: "
			);
			
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		while(resultSet.next()) {
	    	ver = resultSet.getString("Version");
	    	checked = "";
	    	if(ver.compareTo(this.since) == 0) { checked = "checked"; inception = false; }
	    	
			out.println("&nbsp;<span style=\"white-space:nowrap; display:inline-block\"><input type=\"radio\" name=\"since\" value=\"" + ver + "\" " + checked + ">" + ver + "</span>");
		}
		getAccess(this.database).endQuery(statement, resultSet);
	   
		checked = "";
		if(inception) checked = "checked";	   
		out.println(
			  "&nbsp;<span style=\"white-space:nowrap; display:inline-block\"><input type=\"radio\" name=\"since\" value=\"0.0.0\" " + checked + ">Inception</span>"
			);
		if(inTable) {
			out.println(
				  "</td>"
				+ "</tr>"
				);
		}
		
	}
	
	public void findVersion()
		throws Exception
	{
		// Query database for most recent version
		String	query;
		Statement	statement;
		ResultSet	resultSet;
		
		if(this.version.length() == 0) {
			query = "select"
		   		+ " max(Version) "
		   		+ " from history"
		   		+ " where Description = 'Released.'"
			    ;
		
			statement = getAccess(this.database).beginQuery();
			resultSet = getAccess(this.database).select(statement, query);
			while(resultSet.next()) {
		    	this.version = resultSet.getString("max(Version)");
		   }
		   getAccess(this.database).endQuery(statement, resultSet);
		}
		
		if(this.version == null) {	// No released versions - try any version
			query = "select"
			   		+ " max(Version) "
			   		+ " from history"
				    ;
			
				statement = getAccess(this.database).beginQuery();
				resultSet = getAccess(this.database).select(statement, query);
				while(resultSet.next()) {
			    	this.version = resultSet.getString("max(Version)");
			   }
			   getAccess(this.database).endQuery(statement, resultSet);			
		}
		if(this.version == null) this.version = "0.0.0";
		
		// Set generation
		this.generation = 2;	// Newest
		if(this.version.compareTo("0.99.1") == 0) this.generation = 1;
		if(this.version.compareTo("0.99.2") == 0) this.generation = 1;
		if(this.version.compareTo("0.99.3") == 0) this.generation = 1;
	}

	public void showVersion(JspWriter out)
		throws IOException
	{
	   out.println("<center>Dictionary version: " + this.version + "[<a href=history.jsp>History</a>]</center>");
	}

	public TermDef getDictionaryTerm(String term)
			throws Exception
		{
			String	query;
			Statement	statement;
			ResultSet	resultSet;
			TermDef termDef = new TermDef();
			
			System.out.println("getDictionaryTerm: " + term + "; version: " + this.version + "; since: " + this.since);
			
			query = "select" 
				+ " dictionary.*"
				+ " from dictionary"
				+ " where dictionary.Term='" + term + "'"
				+ " and dictionary.Version='" + this.version + "'"
				+ " and dictionary.Since>'" + this.since + "'"
				+ " order by dictionary.Term"
				;

			System.out.println("getDictionaryTerm query: " + query);

			statement = getAccess(this.database).beginQuery();
			resultSet = getAccess(this.database).select(statement, query);
			
			// Store results
			while(resultSet.next()) {
				termDef.term = resultSet.getString("Term");
				termDef.description = resultSet.getString("Definition");
				termDef.list = resultSet.getString("List");
				termDef.type = resultSet.getString("Type");
				termDef.attributes = resultSet.getString("Attributes");
				termDef.since = resultSet.getString("Since");
			}
			getAccess(this.database).endQuery(statement, resultSet);

			return termDef;
	}
	
	public int showDictionary(JspWriter out)
		throws Exception
	{
		return showDictionary(out, this.term);
	}
	
	public int showDictionary(JspWriter out, String pattern)
		throws Exception
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;
		int		count = 0;
		TermDef termDef = new TermDef();
		
		query = "select" 
			+ " dictionary.*"
			+ " from dictionary"
			+ " where dictionary.Term LIKE '" + pattern + "'"
			+ " and dictionary.Version='" + this.version + "'"
			+ " and dictionary.Since>'" + this.since + "'"
			+ " order by dictionary.Term"
			;
		
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		
		// Store results
		while(resultSet.next()) {
			termDef.term = resultSet.getString("Term");
			termDef.description = resultSet.getString("Definition");
			termDef.list = resultSet.getString("List");
			termDef.type = resultSet.getString("Type");
			termDef.attributes = resultSet.getString("Attributes");
			termDef.since = resultSet.getString("Since");
			count++;
			if(this.generation > 1) {
				showTerm2(out, termDef);
			} else {
				showTerm(out, termDef);
			}
		}
		getAccess(this.database).endQuery(statement, resultSet);

		return count;
}

	public void showTerm(JspWriter out, TermDef termDef)
		throws Exception
	{
		String	typeURL;
		String	description;
		String	attrib;
		String	url;
		String	label;
		String	item;
		String[] list;
		
		TermDef	termDef2 = new TermDef();
		
		String	query;
		Statement	statement;
		ResultSet	resultSet;
				
		typeURL = this.scriptName
			+ "?term=" + Encode.urlEncode(termDef.type) 
			+ "&style=" + this.style
			+ "&scope=type"
			+ "&version=" + this.version
			;
		description = Encode.htmlEncode(termDef.description);
		
		// Entry header and definition         
		out.println(
			  "<table width=\"600\" border=\"0\" cellspacing=\"0\">"
			+ "<tr>"
			+ "<td class=\"term\">" + termDef.term + "</td>"
			+ "<td class=\"type\">"
			+ "<a href=\"" + typeURL + "\">" + termDef.type + "</a></td>"
			+ "</tr>"
			+ "<tr>"
			+ "<td colspan=\"2\">"
			+ description
			+ "</td>"
			+ "</tr>"
			);
		
		// Show attributes - if any
		attrib = termDef.attributes.trim();
		if(attrib.length() > 0) {
			list = attrib.split(",");
			if(list.length > 0) {
				label = "Attributes:";
				out.println(
					  "<tr>"
					+ "<td colspan=\"2\">"
					+ "<table>"
					+ "<tr><td width=\"40\">&nbsp;</td>"
					+ "<td>"
					+ "<table>"
					);
					
			for(int i = 0; i < list.length; i++) {
				item = list[i];
				url = this.scriptName + "?term=" + Encode.urlEncode(item.trim())
					+ "&style=" + this.style
					+ "&scope=dictionary"
					+ "&version=" + this.version
					;
				out.println(
					  "<tr><td>" + label + "</td>"
					+ "<td><a href=\"" + url + "\">" + item + "</a></td>"
					+ "</tr>"
					);
				label = "";
			} 
			
			out.println(
				  "</table>"
				+ "</td>"
				+ "</tr>"
				+ "</table>"
				+ "</td>"
				+ "</tr>"
				);
			}
		}         
		
		// Show enumerated list - if one specified
		if(termDef.list.trim().length() > 0) {
			url = this.scriptName + "?term=" + Encode.urlEncode(termDef.list)
				+ "&style=" + this.style
				+ "&scope=list"
				+ "&version=" + this.version
				;
			query = "select" 
				+ " list.*"
				+ " from list"
				+ " where list.Name = '" + termDef.list + "'"
				+ " and list.Version='" + this.version + "'"
				+ " order by list.Name"
				;
		
			statement = getAccess(this.database).beginQuery();
			resultSet = getAccess(this.database).select(statement, query);
		
			out.println(
				  "<tr>"
				+ "<td colspan=\"2\">"
				+ "<table>"
				+ "<tr><td width=\"40\">&nbsp;</td>"
				+ "<td>"
				+ "<table>"
				);

			// Process results
			while(resultSet.next()) {
				termDef2.description = resultSet.getString("Description");
				termDef2.type = resultSet.getString("Type");
				termDef2.reference = resultSet.getString("Reference");
				description = Encode.htmlEncode(termDef2.description);
				label = "Allowed Values:";
				if(termDef2.type.compareTo("Open") == 0) {
					url = Encode.urlEncode(termDef2.reference);
					out.println(
						  "<tr>"
						+ "<td>" + label + "</td>"
						+ "<td>Open List</td>"
						+ "</tr>"
						+ "<tr>"
						+ "<td>&nbsp;</td>"
						+ "<td>For a current list see <a href=\"" + url + "\">" + termDef2.reference + "</a></td>"
						+ "</tr>"
						);
				} else {	// Closed
					Statement	statement2;
					ResultSet	resultSet2;
					query = "select *" 
						+ " from member"
						+ " where member.List = '" + termDef2.term + "'"
						+ " and member.Version='" + this.version + "'"
						+ " order by member.Term"
						;
				
					statement2 = getAccess(this.database).beginQuery();
					resultSet2 = getAccess(this.database).select(statement2, query);
		
					while(resultSet2.next()) {
						url = this.scriptName + "?term=" + Encode.urlEncode(resultSet2.getString("Term"))
							+ "&style=" + this.style
							+ "&scope=dictionary"
							+ "&version=" + this.version
							;
						out.println(
							  "<tr>"
							+ "<td>" + label + "</td>"
							+ "<td><a href=\"" + url + "\">" + resultSet2.getString("Term") + "</a></td>"
							+ "</tr>"
							);
						label = "";
					}
					getAccess(this.database).endQuery(statement2, resultSet2);
				}
			}
			getAccess(this.database).endQuery(statement, resultSet);
			
			out.println(
				  "</table>"
				+ "</td>"
				+ "</tr>"
				+ "</table>"
				+ "</td>"
				+ "</tr>"
				);
	   }

		// Show list of sub-elements     
		if(termDef.element.length() > 0) {
			list = termDef.element.split(",");
			if(list.length > 0) {
				label = "Sub-elements:";
				out.println(
					  "<tr> "
					+ "<td colspan=\"2\">"
					+ "<table>"
					+ "<tr><td width=\"40\">&nbsp;</td>"
					+ "<td>"
					+ "<table>"
					);
				for(int i = 0; i < list.length; i++) {
					item = list[i];
					url = this.scriptName + "?term=" + Encode.urlEncode(item.trim())
						+ "&style=" + this.style
						+ "&scope=dictionary"
						+ "&version=" + this.version
						;
					out.println(
						  "<tr><td>" + label + "</td>"
						+ "<td><a href=\"" + url + "\">" + item + "</a></td>"
						+ "</tr>"
						);
					label = "";
				}
				out.println(
					  "</table>"
					+ "</td>"
					+ "</tr>"
					+ "</table>"
					+ "</td>"
					+ "</tr>"
					);
			}
		}

		// Show membership in enumerated lists
		query = "select" 
			+ " distinct member.*"
			+ " from member"
			+ " where member.Item = '" + termDef.term + "'"
			+ " and member.Version='" + this.version + "'"
			+ " order by member.List"
			;
	
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		
		label ="Member of:";
		while(resultSet.next()) {
			if(label.length() != 0) {
				out.println(
					  "<tr>"
					+ "<td colspan=\"2\">"
					+ "<table>"
					+ "<tr><td width=\"40\">&nbsp;</td>"
					+ "<td>"
					+ "<table>"
					);			
			}
			item = resultSet.getString("List");
			url = this.scriptName + "?term=" + Encode.urlEncode(item)
				+ "&style=" + this.style
				+ "&scope=list"
				+ "&version=" + this.version
				;
			out.println(
				  "<tr>"
				+ "<td>" + label + "</td>"
				+ "<td><a href=\"" + url + "\">" + item + "</a></td>"
				+ "</tr>"
				);
				label = "";
		}
		getAccess(this.database).endQuery(statement, resultSet);
		
		if(label.length() == 0) {	// Finish table
			out.println(
				  "</table>"
				+ "</td>"
				+ "</tr>"
				+ "</table>"
				+ "</td>"
				+ "</tr>"
				);
		}


		// Finish 		
		out.println(
			  "</table>"
			+ "<br>"
			);
	}

	public void showTerm2(JspWriter out, TermDef termDef)
		throws Exception
	{
		String	typeURL;
		String[]	list;
		String		buffer;
		String		description;
		String		label;
		String		item;
		String		type;
		String		url;
		boolean		needHeader;
		
		String		query;
		Statement	statement;
		ResultSet	resultSet;
		
		TermDef termDef2 = new TermDef();
		
		typeURL = this.scriptName
			+ "?term=" + Encode.urlEncode(termDef.type) 
			+ "&style=" + this.style
			+ "&scope=type"
			+ "&version=" + this.version
			;
		description = Encode.htmlEncode(termDef.description);
					
		// Entry header and definition         
		out.println(
			  "<div class=\"definition\">"
			+ "<table class=\"definition\">"
			+ "<!-- Defnition start -->"
			+ "<tr>"
			+ "<td class=\"term\">" + termDef.term + "</td>"
			+ "<td class=\"type\">"
			+ "<a href=\"" + typeURL + "\">" + termDef.type + "</a></td>"
			+ "</tr>"
			+ "<tr>"
			+ "<td colspan=\"2\">"
			+ description
			+ "</td>"
			+ "</tr>"
			+ "<tr>"
			+ "<td colspan=\"2\">"
			+ "   <table>"
			+ "   <tr>"
			+ "   <td width=\"40\">&nbsp;</td>"
			+ "   <td class=\"nowrap\">" + "Since: " + termDef.since + "</td>"
			+ "   </tr>"
			+ "   </table>"
			+ "</td>"
			+ "</tr>"
			);
		
		// Show attributes - if any
		buffer = termDef.attributes.trim();
		if(buffer.length()> 0) {
			list = buffer.split(",");
			label = "Attributes:";
			out.println(
				  "<tr>"
				+ "<!-- Attributes Start -->"
				+ "<td colspan=\"2\">"
				+ "<table>"
				+ "<tr>"
				+ "<td width=\"40\">&nbsp;</td>"
				+ "<td>"
				+ "<table>"
				);
			for(int i = 0; i < list.length; i++) {
				item = list[i];
				url = this.scriptName + "?term=" + Encode.urlEncode(item.trim())
					+ "&style=" + this.style
					+ "&scope=dictionary"
					+ "&version=" + this.version
					;
				out.println(
					  "<tr>"
					+ "<td class=\"nowrap\">" + label + "</td>"
					+ "<td><a href=\"" + url + "\">" + item + "</a></td>"
					+ "</tr>"
					);
				label = "";
			} 
			out.println(
				  "</table>"
				+ "</td>"
				+ "</tr>"
				+ "</table>"
				+ "</td>"
				+ "<!-- Attributes End -->"
				+ "</tr>"
				);
		}
		
		// Show enumerated list - if one specified
		term = termDef.list.trim();
		if(termDef.type.compareTo("Enumeration") == 0) {
			query = "select" 
				+ " list.*"
				+ " from list"
				+ " where list.Name = '" + term + "'"
				+ " and list.Version='" + this.version + "'"
				+ " order by list.Name"
				;
		
			statement = getAccess(this.database).beginQuery();
			resultSet = getAccess(this.database).select(statement, query);
		
			// Display values
			needHeader = true;
			while(resultSet.next()) {
				termDef2.description = resultSet.getString("Description");
				termDef2.reference = resultSet.getString("Reference");
				termDef2.type = resultSet.getString("Type");
				
				needHeader = false;
				out.println(
					  "<tr>"
					+ "<!-- Values start -->"
					+ "<td colspan=\"2\">"
					+ "<table>"
					+ "<tr><td width=\"40\">&nbsp;</td>"
					+ "<td>"
					+ "<table>"
					);
	
				// Process results
				description = Encode.htmlEncode(termDef2.description);
				label = "Allowed Values:";
				type = termDef2.type;
				if(type.compareTo("Open") == 0) {
					url = Encode.urlEncode(termDef2.reference);
					out.println(
						  "<tr>"
						+ "<td class=\"nowrap\">" + label + "</td>"
						+ "<td>Open List</td>"
						+ "</tr>"
						+ "<tr>"
						+ "<td>&nbsp;</td>"
						+ "<td>For a current list see <a href=\"" + url + "\">" + termDef2.reference + "</a></td>"
						+ "</tr>"
						);
				} else {	// Closed
					showLinks = true;
					out.println(
						  "<tr>"
						+ "<td valign=top>" + label + "</td>"
						+ "<td>"
						);
					printEnumeration(out, "", termDef.term, 0, false);
					out.println(
						  "</td>"
						+ "</tr>"
						);
				}
			}
			getAccess(this.database).endQuery(statement, resultSet);
			
			if(! needHeader) {	// We need to close this section
				out.println(
					  "</table>"
					+ "</td>"
					+ "</tr>"
					+ "</table>"
					+ "</td>"
					+ "<!-- Values End -->"
					+ "</tr>"
					);
			}
		}
		
		
		// Show list of sub-elements     
		query = "select" 
			+ " ontology.*"
			+ " from ontology"
			+ " where ontology.Object = '" + Encode.sqlEncode(termDef.term) + "'"
			+ " and ontology.Version='" + this.version + "'"
			+ " order by ontology.Element"
			;
		
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		
		needHeader = true;
		label = "Sub-elements:";
		while(resultSet.next()) {
			if(needHeader) {
				needHeader = false;
				out.println(
					  "<tr>"
					+ "<!-- Sub-elements Start -->"
					+ "<td colspan=\"2\">"
					+ "<table>"
					+ "<tr>"
					+ "<td width=\"40\">&nbsp;</td>"
					+ "<td>"
					+ "<table>"
					);
			}
				
			url = this.scriptName + "?term=" + Encode.urlEncode(resultSet.getString("Element"))
				+ "&style=" + this.style
				+ "&scope=dictionary"
				;
			if(resultSet.getString("Type").startsWith("base:")) url += "&context=spase-model";
			else {  url += "&context=" + database + "&version=" + this.version; }
			item = "";
			out.println(
				  "<tr>"
				+ "<td class=\"nowrap\">" + label + "</td>"
				+ "<td><a href=\"" + url + "\">" + resultSet.getString("Element") + "</a>" + item + "</td>"
				+ "</tr>"
				);
			label = "";
		}
		getAccess(this.database).endQuery(statement, resultSet);
		
		if(! needHeader) {	// We printed a header
			out.println(
				  "</table>"
				+ "</td>"
				+ "</tr>"
				+ "</table>"
				+ "</td>"
				+ "<!-- Sub-elements End -->"
				+ "</tr>"
				);
		}
		
		// Show list of parent elements     
		query = "select" 
			+ " ontology.*"
			+ " from ontology"
			+ " where ontology.Element = '" + Encode.sqlEncode(termDef.term) + "'"
			+ " and ontology.Version='" + this.version + "'"
			+ " order by ontology.Object"
			;
		
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		
		needHeader = true;
		label = "Used by:";
		while(resultSet.next()) {
			if(needHeader) {
				needHeader = false;
				out.println(
					  "<tr>"
					+ "<!-- Parent Start -->"
					+ "<td colspan=\"2\">"
					+ "<table>"
					+ "<tr>"
					+ "<td width=\"40\">&nbsp;</td>"
					+ "<td>"
					+ "<table>"
					);
			}
			url = this.scriptName + "?term=" + Encode.urlEncode(resultSet.getString("Object"))
				+ "&style=" + this.style
				+ "&scope=dictionary"
				;
			if(resultSet.getString("Type").startsWith("base:")) url += "&context=spase-model";
			else {  url += "&context=" + database + "&version=" + this.version; }

			item = "";
			out.println(
				  "<tr>"
				+ "<td class=\"nowrap\">" + label + "</td>"
				+ "<td><a href=\"" + url + "\">" + resultSet.getString("Object") + "</a>" + item + "</td>"
				+ "</tr>"
				);
			label = "";
		}
		getAccess(this.database).endQuery(statement, resultSet);

		if(! needHeader) {	// We printed a header
			out.println(
				  "</table>"
				+ "</td>"
				+ "</tr>"
				+ "</table>"
				+ "</td>"
				+ "<!-- Parent End -->"
				+ "</tr>"
				);
		}
		// Show list of parent elements     
		query = "select" 
			+ " member.*"
			+ " from member"
			+ " where member.Term LIKE '" + Encode.sqlEncode(termDef.term) + "'"
			+ " and member.Version='" + this.version + "'"
			;
		
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);

		needHeader = true;
		label = "Member of:";		
		while(resultSet.next()) {
			if(needHeader) {
				needHeader = false;
				out.println(
					  "<tr>"
					+ "<!-- Parent Start -->"
					+ "<td colspan=\"2\">"
					+ "<table>"
					+ "<tr>"
					+ "<td width=\"40\">&nbsp;</td>"
					+ "<td>"
					+ "<table>"
					);
			}
			url = this.scriptName + "?term=" + Encode.urlEncode(resultSet.getString("List"))
				+ "&style=" + this.style
				+ "&scope=dictionary"
				+ "&version=" + this.version
				;
			item = "";
			out.println(
				  "<tr>"
				+ "<td class=\"nowrap\">" + label + "</td>"
				+ "<td><a href=\"" + url + "\">" + resultSet.getString("List") + "</a>" + item + "</td>"
				+ "</tr>"
				);
			label = "";
		}
		getAccess(this.database).endQuery(statement, resultSet);

		if(! needHeader) {	// We printed a header
			out.println(
				  "</table>"
				+ "</td>"
				+ "</tr>"
				+ "</table>"
				+ "</td>"
				+ "<!-- Parent End -->"
				+ "</tr>"
				);
		}
		
		out.println(
			  "<!-- Defnition End -->"
			+ "</table>"
			+ "</div>"
			+ "<br>"
			);
	}

	public void showTerm3(JspWriter out, TermDef termDef, String path)
			throws Exception
		{
			String	typeURL;
			String[]	list;
			String		buffer;
			String		description;
			String		label;
			String		item;
			String		type;
			String		url;
			boolean		needBreak;
			
			String		query;
			Statement	statement;
			ResultSet	resultSet;
			
			TermDef termDef2 = new TermDef();
			
			typeURL = this.scriptName
				+ "?term=" + Encode.urlEncode(termDef.type) 
				+ "&style=" + this.style
				+ "&scope=type"
				+ "&version=" + this.version
				+ "&path=" + path
				;
			description = Encode.htmlEncode(termDef.description);
						
			// Entry header and definition         
			out.println(
				  "<!-- Definition -->"
				+ "<div class=\"right\"><a href=\"" + typeURL + "\">" + termDef.type + "</a></div>"
				+ "<div class=\"title\">" + termDef.term + "</div>"
				+ "<div class=\"clear\"></div>"
				+ "<p>"	+ description + "</p>"
				+ "<p class=\"nowrap\">" + "Since: " + termDef.since + "</p>"
				);
			
			needBreak = false;
			out.println("<table>");
			
			// Show attributes - if any
			label = "Attributes:";
			buffer = termDef.attributes.trim();
			if(buffer.length()> 0) {
				list = buffer.split(",");
				out.println(
					  "<!-- Attributes Start -->"
					);
				for(int i = 0; i < list.length; i++) {
					item = list[i];
					url = this.scriptName + "?term=" + Encode.urlEncode(item.trim())
						+ "&style=" + this.style
						+ "&scope=dictionary"
						+ "&version=" + this.version
						+ "&path=" + path
						;
					out.println(
						  "<tr>"
						+ "<td class=\"nowrap\">" + label + "</td>"
						+ "<td><a href=\"" + url + "\">" + item + "</a></td>"
						+ "</tr>"
						);
					label = "";
				} 
			}
			if(label.length() == 0) {
				needBreak = true;
				out.println(
					"<!-- Attributes End -->"
					);
			}
			
			// Show enumerated list - if one specified
			term = termDef.list.trim();
			if(termDef.type.compareTo("Enumeration") == 0) {
				query = "select" 
					+ " list.*"
					+ " from list"
					+ " where list.Name = '" + term + "'"
					+ " and list.Version='" + this.version + "'"
					+ " order by list.Name"
					;
			
				statement = getAccess(this.database).beginQuery();
				resultSet = getAccess(this.database).select(statement, query);
			
				// Display values
				label = "Allowed Values:";
				while(resultSet.next()) {
					termDef2.description = resultSet.getString("Description");
					termDef2.reference = resultSet.getString("Reference");
					termDef2.type = resultSet.getString("Type");
					
					if(needBreak) { out.println("<tr><td>&nbsp;</td><td>&nbsp;</td></tr>"); needBreak = false; }
					out.println(
						  "<!-- Values start -->"
						);
		
					// Process results
					description = Encode.htmlEncode(termDef2.description);
					type = termDef2.type;
					if(type.compareTo("Open") == 0) {
						url = Encode.urlEncode(termDef2.reference);
						out.println(
							  "<tr>"
							+ "<td>" + label + "</td>"
							+ "<td>Open List</td>"
							+ "</tr>"
							+ "<tr>"
							+ "<td>&nbsp;</td>"
							+ "<td>For a current list see <a href=\"" + url + "\">" + termDef2.reference + "</a></td>"
							+ "</tr>"
							);
					} else {	// Closed
						showLinks = true;
						out.println(
							  "<tr>"
							+ "<td  class=\"nowrap\" valign=top>" + label + "</td>"
							+ "<td>"
							);
						printEnumeration(out, "", termDef.term, 0, false);
						out.println(
							  "</td>"
							+ "</tr>"
							);
					}
					label = "";
				}
				getAccess(this.database).endQuery(statement, resultSet);
				
				if(label.length() == 0) {
					needBreak = true;
					out.println(
							"<!-- Values End -->"
					);
				}
			}
			
			
			// Show list of parent elements     
			query = "select" 
				+ " ontology.*"
				+ " from ontology"
				+ " where ontology.Element = '" + Encode.sqlEncode(termDef.term) + "'"
				+ " and ontology.Version='" + this.version + "'"
				+ " order by ontology.Object"
				;
			
			statement = getAccess(this.database).beginQuery();
			resultSet = getAccess(this.database).select(statement, query);
			
			label = "Used by:";
			while(resultSet.next()) {
				if(needBreak) { out.println("<tr><td>&nbsp;</td><td>&nbsp;</td></tr>"); needBreak = false; }
				if(label.length() > 0) {
					out.println(
						  "<!-- Parent Start -->"
						);
				}
				url = this.scriptName + "?term=" + Encode.urlEncode(resultSet.getString("Object"))
					+ "&style=" + this.style
					+ "&scope=dictionary"
					+ "&path=" + path
					;
				if(resultSet.getString("Type").startsWith("base:")) url += "&context=spase-model";
				else {  url += "&context=" + database + "&version=" + this.version; }
				
				item = "";
				out.println(
					  "<tr>"
					+ "<td class=\"nowrap\">" + label + "</td>"
					+ "<td><a href=\"" + url + "\">" + resultSet.getString("Object") + "</a>" + item + "</td>"
					+ "</tr>"
					);
				label = "";
			}
			getAccess(this.database).endQuery(statement, resultSet);

			if(label.length() == 0) {
				needBreak = true;
				out.println(
					"<!-- Parent End -->"
				);
			}
			
			// Show list of reference from elements     
			query = "select" 
				+ " member.*"
				+ " from member"
				+ " where member.Term LIKE '" + Encode.sqlEncode(termDef.term) + "'"
				+ " and member.Version='" + this.version + "'"
				;
			
			statement = getAccess(this.database).beginQuery();
			resultSet = getAccess(this.database).select(statement, query);

			label = "Member of:";		
			while(resultSet.next()) {
				if(needBreak) { out.println("<tr><td>&nbsp;</td><td>&nbsp;</td></tr>"); needBreak = false; }
				if(label.length() > 0) {
					out.println(
						  "<!-- References Start -->"
						);
				}
				url = this.scriptName + "?term=" + Encode.urlEncode(resultSet.getString("List"))
					+ "&style=" + this.style
					+ "&scope=dictionary"
					+ "&version=" + this.version
					+ "&path=" + path
					;
				item = "";
				out.println(
					  "<tr>"
					+ "<td class=\"nowrap\">" + label + "</td>"
					+ "<td><a href=\"" + url + "\">" + resultSet.getString("List") + "</a>" + item + "</td>"
					+ "</tr>"
					);
				label = "";
			}
			getAccess(this.database).endQuery(statement, resultSet);

			if(label.length() == 0) {
				needBreak = true;
				out.println(
					  "<!-- References End -->"
				);
			}
			
			out.println("</table>");
		}
	
	public void showTemplateTerm(JspWriter out, String term, int indent, boolean openTerm, boolean endTerm)
		throws Exception
	{
		for(int i = 0; i < indent; i++) out.print("   ");
		if(openTerm) out.print("<" + XSLName(term) + ">");
		if(openTerm && endTerm) out.print(" ");
		if(endTerm) out.print("</" + XSLName(term) + ">");
		out.println();
	}

	public void showXMLTerm(JspWriter out, TermDef termDef)
		throws Exception
	{
		String	description;
		String[]	list;
		String		url;
		boolean		needHeader;
		
		TermDef	termDef2 = new TermDef();
		
		String	query;
		Statement	statement;
		ResultSet	resultSet;
		
		description = Encode.htmlEncode(termDef.description);
		
		// Entry header and definition         
		out.println("<entry>");
		out.println("<term>" + termDef.term	+ "</term>");
		out.println("<definition>" + description + "</definition>");
		out.println("<type>" + termDef.type + "</type>");
		
		// Show attributes - if any
		if(termDef.attributes.length() > 0) {
			list = termDef.attributes.split(",");
			for(int i = 0; i < list.length; i++) {
				out.println("<attribute>" + XSLName(list[i]) + "</attribute>");
		
			} 
		}
		
		// Show enumerated list - if one specified
		if(termDef.list.length() > 0) {
			url = this.scriptName + "?term=" + Encode.urlEncode(termDef.list)
				+ "&style=entry"
				+ "&scope=list"
				+ "&version=" + this.version
				;
			query = "select" 
				+ " list.*"
				+ " from list"
				+ " where list.Name = '" + Encode.sqlEncode(termDef.list) + "'"
				+ " and list.Version='" + this.version + "'"
				+ " order by list.Name"
				;
		
			statement = getAccess(this.database).beginQuery();
			resultSet = getAccess(this.database).select(statement, query);
			
			// Store results
			while(resultSet.next()) {
				termDef2.term = resultSet.getString("Name");
				termDef2.description = resultSet.getString("Description");
				termDef2.type = resultSet.getString("Type");
				termDef2.reference = resultSet.getString("Reference");
				out.println("<values>");
				description = Encode.htmlEncode(termDef2.description);
				if(termDef2.type.compareTo("Open") == 0) {
					url = Encode.urlEncode(termDef2.reference);
					out.println("<url>" + url + "</url>");
				} else {	// Closed
					printXMLEnumeration(out, "", termDef2.term, 0, false);
				}
				out.println("</values>");
			}
			getAccess(this.database).endQuery(statement, resultSet);
		}
		
		
		// Show list of sub-elements     
		query = "select" 
			+ " ontology.*"
			+ " from ontology"
			+ " where ontology.Object = '" + Encode.sqlEncode(termDef.term) + "'"
			+ " and ontology.Version='" + this.version + "'"
			+ " order by ontology.Element"
			;
		
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		
		needHeader = true;
		while(resultSet.next()) {
			if(needHeader) {
				needHeader = false;
				out.println("<elements>");
			}
			out.println("<element>" + resultSet.getString("Element") + "</element>");
		}
		getAccess(this.database).endQuery(statement, resultSet);
		
		if(! needHeader) {	// We printed a header
			out.println("</elements>");
		}
		
		// Show list of parent elements     
		query = "select" 
			+ " ontology.*"
			+ " from ontology"
			+ " where ontology.Element = '" + Encode.sqlEncode(termDef.term) + "'"
			+ " and ontology.Version='" + this.version + "'"
			+ " order by ontology.Object"
			;
		
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		
		needHeader = true;		        
		while(resultSet.next()) {
			if(needHeader) {
				needHeader = false;
				out.println("<usage>");
			}
			out.println("<usedby>" + resultSet.getString("Object") + "</usedby>");
		}
		getAccess(this.database).endQuery(statement, resultSet);
		
		if(! needHeader) {	// We printed a header
			out.println("</usage>");
		}
		
		out.println("</entry>");
	}

	public int showList(JspWriter out, String pattern)
		throws Exception
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;
		
		String	description;
		String	definition;
		String	url;
		int		count = 0;
		TermDef	termDef = new TermDef();
		
		query = "select" 
			+ " list.*"
			+ " from list"
			+ " where list.Name LIKE '" + pattern + "'"
			+ " and list.Version='" + this.version + "'"
			+ " and list.Since>'" + this.since + "'"
			+ " order by list.Name"
			;
			
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		while(resultSet.next()) {
			count++;
			termDef.term = resultSet.getString("Name");
			termDef.type = resultSet.getString("Type");
			termDef.reference = resultSet.getString("Reference");
			termDef.description = resultSet.getString("Description");
			description = Encode.htmlEncode(termDef.description);
			out.println(
				 "<table width=\"600\" border=\"0\" cellspacing=\"0\">"
				+ "<tr>"
				+ "<td width=\"50%\" bgcolor=\"#000000\" align=\"left\"><font color=\"#FFFFF\">" + termDef.term + "</font></td>"
				+ "<td width=\"50%\" bgcolor=\"#FFFFFF\" align=\"right\">" + termDef.type + "</td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td colspan=\"2\">"
				+ description
				+ "</td>"
				+ "</tr>"
				);

			if(termDef.type.compareTo("Open") == 0) {
				url = Encode.urlEncode(termDef.reference);
				out.println(
					  "<tr>"
					+ "<td colspan=\"2\">"
					+ "<table>"
					+ "<tr><td width=\"50\">&nbsp;</td>"
					+ "<td>For a current list see <a href=\"" + url + "\">" + termDef.reference + "</a></td>"
					+ "</table>"
					+ "</td>"
					+ "</tr>"
					);
			} else {	// Closed
				String	query2;
				Statement	statement2;
				ResultSet	resultSet2;
				int		n;
				
				query2 = "select" 
					+ " list.*, member.Term, dictionary.Definition"
					+ " from list, member, dictionary"
					+ " where list.Name = member.List"
					+ " and member.Term = dictionary.Term"
					+ " and list.Name = '" + termDef.term + "'"
					+ " and list.Version='" + this.version + "'"
					+ " and dictionary.Version='" + this.version + "'"
					+ " and member.Version='" + this.version + "'"
					+ " order by list.Name"
					;
				
				statement2 = getAccess(this.database).beginQuery();
				resultSet2 = getAccess(this.database).select(statement2, query2);
				out.println(
					  "<tr>"
					+ "<td colspan=\"2\">"
					+ "<table>"
					);
			
				n = 0;
				url="";
				while(resultSet2.next()) {
					n++;
					url = this.scriptName + "?term=" + Encode.urlEncode(resultSet2.getString("Term"))
						+ "&style=" + this.style
						+ "&scope=dictionary"
						+ "&version=" + this.version
					;
					definition = Encode.htmlEncode(resultSet2.getString("Definition"));
					term = resultSet2.getString("Term");
					term = term.replace(" ", "&nbsp;");
					out.println(
						"<tr><td width=\"50\">&nbsp;</td>"
						+ "<td  class=\"nowrap\" valign=top><a href=\"" + url + "\">" + term + "</a></td>"
						+ "<td>" + definition + "</td>"
						);
				}
				getAccess(this.database).endQuery(statement2, resultSet2);
				
				// If no terms found warn user
				if(n == 0) {
					out.println(
						  "<tr><td width=\"50\">&nbsp;</td>"
						+ "<td valign=top><a href=\"" + url + "\">" + termDef.term + "</a></td>"
						+ "<td><font color=\"red\">Definition not found</font></td>"
						);
				}
				out.println(
					  "</table>"
					+ "</td>"
					+ "</tr>"
					);
			}
			
			out.println(
				  "</table>"
				+ "<br>"
				);
		}
		getAccess(this.database).endQuery(statement, resultSet);
		
		return count;
	}

	public int showType(JspWriter out, String pattern)
		throws Exception
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;
		int		count = 0;
		
		query = "select" 
			+ " type.*"
			+ " from type"
			+ " where type.Name LIKE '" + pattern + "'"
			+ " and type.Version='" + this.version + "'"
			+ " and type.Since>'" + this.since + "'"
			+ " order by type.Name"
			;
		
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		
		while(resultSet.next()) {
			count++;
			out.println("<table width=\"600\" border=\"0\" cellspacing=\"0\">");
			out.println("<tr>");
			out.println("<td width=\"50%\" bgcolor=\"#000000\" align=\"left\"><font color=\"#FFFFF\">" + resultSet.getString("Name") + "</font></td>");
			out.println("<td width=\"50%\" bgcolor=\"#FFFFFF\" align=\"right\">&nbsp;</td>");
			out.println("</tr>");
			out.println("<tr>");
			out.println("<td colspan=\"2\">");
			out.println(resultSet.getString("Description"));
			out.println("</td>");
			out.println("</tr>");
			out.println("</table>");
			out.println("<br>");
		}
		getAccess(this.database).endQuery(statement, resultSet);
		
		return count;
	}

	public void showTree1(JspWriter out, String pattern, int indent, boolean dictionary, boolean showLinks)
		throws Exception
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;
		
		String	term;
		String	elements;
		String	list;
		String[]	part;
		boolean	endRow;
		
		if(dictionary) {	// Dictioanry term
			query = "select" 
				+ " dictionary.*"
				+ " from dictionary"
				+ " where dictionary.Term = '" + pattern + "'"
				+ " and dictionary.Version='" + this.version + "'"
				;
		} else {	// Members of a list
			query = "select" 
				+ " member.*"
				+ " from member"
				+ " where member.List = '" + pattern + "'"
				+ " and member.Version='" + this.version + "'"
				;
		}
		
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);

		while(resultSet.next()) {
			endRow = true;
			out.print("<tr>");
			printRowIndent(out, indent);
			term = resultSet.getString("Term");
			if(dictionary) {
				out.print("<td><b>");
				if(showLinks) out.print("<a href=\"" + this.scriptName + "?term=" + Encode.urlEncode(term) + "&style=entry&scope=dictionary&version=" + this.version + "\">");
				out.print(term.replace(" ", "&nbsp;"));
				if(showLinks) out.print("</a>");
				out.print("</b></td>");
				elements = resultSet.getString("Elements").trim();
				if(elements.length() > 0) {
					out.print("<td><font color=#0000FF>has&nbsp;elements</font></td>");
					out.print("</tr>"); endRow = false;
					part = elements.split(",");
					for(int i = 0; i < part.length; i++) {
						showTree1(out, part[i].trim(), indent+1, dictionary, showLinks);
					}
				}
				
				list = resultSet.getString("List").trim();
				if(list.length() > 0) {
					out.print("<td><font color=\"#FF0000\">is</font></td>");
					out.print("</tr>"); endRow = false;
					showTree1(out, list, indent+1, false, showLinks);
				}
				break;	// Only do first term
			} else {	// Member list
				out.print("<td>");
				if(showLinks) out.print("<a href=\"" + this.scriptName + "?term=" + Encode.urlEncode(term) + "&style=entry&scope=dictionary&version=" + this.version + "\">");
				out.print("<i>" + term.replace(" ", "&nbsp;") + "</i>");
				if(showLinks) out.print("</a>");
				out.print("</td>");
			}		
			if(endRow) out.println("</tr>");
		}
		getAccess(this.database).endQuery(statement, resultSet);
	}
	
	public void showTree2(JspWriter out, String term, int indent, String occur, String group, String type, String since)
		throws Exception
	{
		String query;
		Statement	statement;
		ResultSet	resultSet;
		
		boolean	showName;
		boolean	endRow;
		boolean	isNew = false;
		int		nRow = 0;
		TermDef	termDef = new TermDef();
		
		query = "select" 
			+ " dictionary.*"
			+ " from dictionary"
			+ " where dictionary.Term = '" + Encode.sqlEncode(term) + "'"
			+ " and dictionary.Version='" + this.version + "'"
			;

		isNew = false;
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		while(resultSet.next()) {
			isNew = igpp.util.Text.isMatch(resultSet.getString("Since"), this.version);
		}
		getAccess(this.database).endQuery(statement, resultSet);
		
		query = "select" 
			+ " ontology.*"
			+ " from ontology"
			+ " where ontology.Object = '" + Encode.sqlEncode(term) + "'"
			+ " and ontology.Version='" + this.version  + "'"
			+ " Order By ontology.Pointer"
		;
		
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		
		showName = true;
		out.println("<tr>");
		printRowIndent(out, indent);
		
		nRow = 0;
		while(resultSet.next()) {
			termDef.since = resultSet.getString("Since");
			termDef.term = resultSet.getString("Object");
			termDef.element = resultSet.getString("Element");
			termDef.occurence = resultSet.getString("Occurence");
			termDef.group = resultSet.getString("Group");
			termDef.type = resultSet.getString("Type");
		
			nRow++;
			endRow = true;
			
			// Print object name
			if(showName) {
				boolean temp = igpp.util.Text.isMatch(since, this.version);
				printTerm(out, term, occur, group, getContextFromType(type), temp);
				if(this.showAttrib) endRow = printAttrib(out, term, indent);
				if(! endRow) printRowIndent(out, indent+1);
				// Print element
				out.println("<td><font color=\"#0000FF\">has&nbsp;elements</font></td>");
				showName = false;
				out.println("</tr>");
			}	
			// Show elements
			showTree2(out, termDef.element, indent+1, termDef.occurence, termDef.group, termDef.type, termDef.since);
		}
		getAccess(this.database).endQuery(statement, resultSet);
		
		if(nRow == 0) {	// If no sub-terms - print term
			endRow = true;
			printTerm(out, term, occur, group, getContextFromType(type), isNew);
			if(this.showValues) endRow = printValues(out, term, indent);
			if(this.showAttrib) endRow = printAttrib(out, term, indent);
			if(endRow) out.println("</tr>");
		}
		
	}

// Determine if a dictionary term is an enumeration
	public String isEnumeration(JspWriter out, String term)
		throws Exception
	{
		String itIs;
		String	query;
		Statement	statement;
		ResultSet	resultSet;
		
		query = "select" 
			+ " dictionary.*"
			+ " from dictionary"
			+ " where dictionary.Term = '" + Encode.sqlEncode(term) + "'"
			+ " and dictionary.Type='Enumeration'"
			+ " and dictionary.Version='" + this.version + "'"
			;
		
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		
		itIs = "";
		while(resultSet.next()) {
			itIs = resultSet.getString("List");
			// buffer = resultSet.getString("Type").trim();
			// if(buffer.compareTo("Enumeration") == 0) itIs = resultSet.getString("List");
		}
		getAccess(this.database).endQuery(statement, resultSet);
		
		return itIs;
	}

	public String getValueType(String term)
		throws Exception, IOException
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;
		
		String valueType = "";
		
		query = "select" 
			+ " dictionary.*"
			+ " from dictionary"
			+ " where dictionary.Term = '" + Encode.sqlEncode(term) + "'"
			+ " and dictionary.Version='" + this.version + "'"
		;
		
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		
		while(resultSet.next()) {
			valueType = resultSet.getString("Type").trim();
		}
		getAccess(this.database).endQuery(statement, resultSet);
		
		// Clean-up
		return valueType;
	}


	public boolean printAttrib(JspWriter out, String term, int indent)
		throws Exception, IOException
	{
		boolean showHas;
		String	query;
		String	buffer;
		String	type;
		String[]	part;
		Statement	statement;
		ResultSet	resultSet;
		
		query = "select" 
			+ " dictionary.*"
			+ " from dictionary"
			+ " where dictionary.Term = '" + Encode.sqlEncode(term) + "'"
			+ " and dictionary.Version='" + this.version + "'"
		;
		
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		
		showHas = true;
		while(resultSet.next()) {
			buffer = resultSet.getString("Attributes").trim();
			if(buffer.length() == 0) continue;
			type = "";
			if(igpp.util.Text.isMatch(resultSet.getString("Since"), this.version)) type = " class=\"new\"";
			part = buffer.split(",");
			if(showHas && part.length > 0) {
				out.println("<td><font color=\"#0000FF\">has&nbsp;attributes</font></td>");
				out.println("</tr>");
				showHas = false;
			}
			for(int i = 0; i < part.length; i++) {
				printRowIndent(out, indent+1);
				out.print("<td" + type + ">");
				if(this.showLinks) out.print("<a" + type + " href=\"" + this.scriptName + "?term=" + Encode.urlEncode(part[i]) + "&style=" + this.style + "&scope=dictionary&version=" + this.version + "\">");
				out.print(part[i].replace(" ", "&nbsp;"));
				if(this.showLinks) out.print("</a>");
				out.print("</td>");
				out.println("</tr>");
			}
		}
		getAccess(this.database).endQuery(statement, resultSet);
		
		// Clean-up
		return showHas;
	}

	public boolean printValues(JspWriter out, String term, int indent)
		throws Exception
	{
		boolean showHas = true;
		String list = isEnumeration(out, term);
		
  		if(list.length() == 0) return showHas;	// no values
   
  		showHas = true;
		if(showHas) {
			out.println("<td><font color=\"#0000FF\">possible&nbsp;values</font></td>");
			out.println("</tr>");
			showHas = false;
		}
		printEnumeration(out, "", term, indent, true);

		return showHas;
	}

	public void printEnumeration(JspWriter out, String prefix, String term, int indent, boolean inTree)
		throws Exception
	{
		String	query;
		Statement	statement;
		ResultSet	resultSet;
		
		TermDef		termDef = new TermDef();;
		
		String	listName;
		String	buffer;
		String	newPrefix;
		String	type;
		
		listName = isEnumeration(out, term);
		if(listName.length() == 0) return;	// no values
		
		query = "select" 
			+ " member.*"
			+ " from member"
			+ " where member.List = '" + Encode.sqlEncode(listName) + "'"
			+ " and member.Version='" + this.version + "'"
			;
		
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);

		while(resultSet.next()) {
			termDef.term = resultSet.getString("Term");
			type = "";
			if(igpp.util.Text.isMatch(resultSet.getString("Since"), this.version)) type = " class=\"new\"";
			if(inTree) {
				printRowIndent(out, indent+1);
				out.println("<td" + type + ">");
			}
			buffer = "";
			if(this.showLinks) buffer += "<a" + type + " href=\"" + this.scriptName + "?term=" + Encode.urlEncode(termDef.term) + "&style=" + this.style + "&scope=dictionary&version=" + this.version + "&path=" + path + "\">";
			buffer += termDef.term;
			if(this.showLinks) buffer += "</a>";
			// Print main item
			if(prefix.length() > 0) out.print(prefix + ".");
			out.print(buffer);
			if(!inTree) out.print("<br>");
			if(isEnumeration(out, termDef.term).length() > 0) {	// Print enumeration of item
				newPrefix = prefix;
				if(newPrefix.length() != 0) newPrefix += ".";
				printEnumeration(out, newPrefix + buffer, termDef.term, indent, inTree);
			}
			if(inTree) {
				out.print("</td>");
				out.print("</tr>");
			}
		}
		getAccess(this.database).endQuery(statement, resultSet);
	}

	public void printXMLEnumeration(JspWriter out, String prefix, String term, int indent, boolean inTree)
		throws Exception
	{
		
		String	list;
		String	newPrefix;
		
		TermDef	termDef = new TermDef();
		
		String	query;
		Statement	statement;
		ResultSet	resultSet;
		
		list = isEnumeration(out, term);
		if(list.length() == 0) return;
		
		query = "select" 
			+ " member.*"
			+ " from member"
			+ " where member.List = '" + Encode.sqlEncode(list) + "'"
			+ " and member.Version='" + this.version + "'"
			;
		
		statement = getAccess(this.database).beginQuery();
		resultSet = getAccess(this.database).select(statement, query);
		
		// Store results
		while(resultSet.next()) {
			termDef.term = resultSet.getString("Term");
			// Print main item
			out.println("<value>");
			if(prefix.length() > 0) out.print(prefix + ".");
			out.println(termDef.term);
			out.println("</value>");
			if(isEnumeration(out, termDef.term).length() > 0) {	// Print enumeration of item
				newPrefix = prefix;
				if(newPrefix.length() > 0) newPrefix += ".";
				printXMLEnumeration(out, newPrefix + termDef.term, termDef.term, indent, inTree);
			}
		}
		getAccess(this.database).endQuery(statement, resultSet);
	}

	public void printTerm(JspWriter out, String term, String occur, String group, String context, boolean isNew)
		throws IOException
	{
		String	buffer = "";
		String	type = "";
		
		if(isNew) type = " class=\"new\"";
		
		// Determine group label - if appropriate
		if(group.length() > 0) {
			for(int i = 0; i < mGroupList.size(); i++) {
				if(group.compareToIgnoreCase((String) mGroupList.get(i)) == 0) {
				   buffer = "&nbsp;of&nbsp;" + (char)('A' + i);
					break;
				}
			}
			
			if(buffer.length() == 0) {	// Not in list
				buffer = "&nbsp;of&nbsp;" + (char)('A' + mGroupList.size());
				mGroupList.add(group);
			}
		}
		
		String versionParm = "";
		if(type.equals(getContext()))	{ // Same context use current version
			versionParm = "&version=" + this.version;
		}
		
		// Generate term definition
		out.println("<td" + type + "><b>");
		if(this.showLinks) out.print("<a" + type + " href=\"" + this.scriptName + "?context=" + Encode.urlEncode(context) + "&term=" + Encode.urlEncode(term) + "&style=entry&scope=dictionary" + versionParm + "\">");
		out.print(term.replace(" ", "&nbsp;"));
		if(this.showLinks) out.print("</a>");
		if(this.showOccur) {
			out.print("&nbsp;(" + occur + buffer + ")");
		}
		out.println("</b></td>");
	}

	public void printRowIndent(JspWriter out, int indent)
		throws IOException
	{
		out.println("<tr>");
		for(int i = 0; i < indent; i++) out.println("<td>&nbsp;</td>");
	}

	public void printIndent(JspWriter out, int indent)
		throws IOException
	{
		for(int i = 0; i < indent; i++) out.print(" ");
	}

	public String XSLName(String term)
	{
		// Strip spaces, dashes and single quotes
		term = term.replace(" ", "");
		term = term.replace("'", "");
		term = term.replace("-", "");
		return term;
	}

	/*
	 * Determine if web page trim is to added.
	 */
	public boolean needTrim()
	{
		if(this.style.compareToIgnoreCase("list") == 0 ) return true;
		if(this.style.compareToIgnoreCase("entry") == 0 ) return true;
		if(this.style.compareToIgnoreCase("tree") == 0 ) return true;
		return false;
	}
	
	public void log(JspWriter writer, String message)
	{
		try {
			writer.println(message + "<br>");
		} catch(Exception e) {
		}
	}
	
	public String getContextFromType(String type)
	{
		String[] part = type.split(":", 2);
		if(part.length > 1) {
			return typeToContextMap.get(part[0]);
		}
		
		return getContext();
	}
	
	public Query getAccess(String database) { return this.access; }

	// Passed parameters
	public boolean	getShowLinks() { return this.showLinks; }
	public void		setShowLinks(boolean value) { this.showLinks = value; }
	
	public boolean	getShowOccur() { return this.showOccur; }
	public void		setShowOccur(boolean value) { this.showOccur = value; }
	
	public boolean	getShowAttrib() { return this.showAttrib; }
	public void		setShowAttrib(boolean value) { this.showAttrib = value; }
	
	public boolean	getShowValues() { return this.showValues; }
	public void		setShowValues(boolean value) { this.showValues = value; }
	
	public int		getRecurseDepth() { return this.recurseDepth; }
	public void		setRecurseDepth(int value) { this.recurseDepth = value; }
	
	public String	getSubmit() { return this.submit; }
	public void		setSubmit(String value) { this.submit = value; this.doSearch = true; }
	
	public String	getTerm() { return this.term; }
	public void		setTerm(String value) { this.term = value; this.doSearch = true; }
	
	public String	getVersion() { return this.version; }
	public void		setVersion(String value) { this.version = value; }
	
	public String	getSince() { return this.since; }
	public void		setSince(String value) { this.since = value; }
	
	public String	getStyle() { return this.style; }
	public void		setStyle(String value) { this.style = value; }
	
	public String	getScope() { return this.scope; }
	public void		setScope(String value) { this.scope = value; }
	
	public String	getPath() { return this.path; }
	public void		setPath(String value) { this.path = value; }
	
	public String	getRealPath() { return this.realPath; }
	public void		setRealPath(String value) { this.realPath = value; }
	
	public String	getContext() { return this.database; }
	public void		setContext(String value) {
		if(value.equals(this.database)) return;	// No change
		this.database = value; 
		try {
			this.destroy();	// Close current database connections;

			this.init(this.realPath, this.database);	// Switch databases (context);
		} catch(Exception e) {
			// What to do when an error???
		}
	}
	
	// Inner class to store term definitions
	class TermDef {
		public String	term;
		public String	element;
		public String	reference;
		public String	type;
		public String	list;
		public String	description;
		public String	attributes;
		public String	occurence;
		public String	since;
		public String	group;
		
		TermDef() {
			term = "Undefined";
		}
	}

}