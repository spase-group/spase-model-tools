package org.spase.model.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

/**
 * Convert an ontology expressed in Turtle to an enumeration.
 * <p>
 * Usage:<blockquote> ConvertTurtle start file.ttl </blockquote>
 * 
 * @author Todd King
 * @author UCLA/IGPP
 * @version 1.0, 12/04/2013
 * @since 1.0
 */

public class ConvertTurtle {
	private String mVersion = "1.0.0";

	boolean verbose = false;
	
	HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
	HashMap<String, String> equiv = new HashMap<String, String>();

	String delim = ".";
	String output = null;
	
	PrintStream out = System.out;

	// create the Options
	Options mOptions = new Options();

	public ConvertTurtle() {
		mOptions.addOption( "h", "help", false, "Display this text" );
		mOptions.addOption( "v", "verbose", false, "Verbose. Show status at each step" );
		mOptions.addOption( "o", "output", true, "Output filename.");		
		mOptions.addOption( "d", "delimiter", true, "Delimiter in enumeration output.");		
		mOptions.addOption( "n", "node", true, "Class name for the start node of the enumeration.");		
	}
	
	public static void main(String[] args) {
		ConvertTurtle me = new ConvertTurtle();
		
		String startNode = "";
		
		if (args.length < 2) {
			System.err.println("Version: " + me.mVersion);
			System.err.println("Usage: " + me.getClass().getName()
					+ " start file.ttl");
			System.exit(1);
		}
		
		// create the command line parser
		CommandLineParser parser = new PosixParser();
		try { // parse the command line arguments
			CommandLine line = parser.parse(me.mOptions, args);

			if(line.hasOption("h")) me.showHelp();
			if(line.hasOption("v")) me.verbose = true;
			if(line.hasOption("o")) me.output = line.getOptionValue("o");
			if(line.hasOption("d")) me.delim = line.getOptionValue("d");
			if(line.hasOption("n")) startNode = line.getOptionValue("n");

			if(me.output != null) {
				me.out = new PrintStream(me.output);
			}
			if(me.verbose) {
				System.out.print("Output written to: " + me.output);
			}

			if(line.getArgs().length < 1) {
				System.out.println("Insufficient arguments. Missing input file name.");
				return;
			}
			String source = line.getArgs()[0];
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(source)));
			String buffer;			
			String name = null;
			
			while((buffer = reader.readLine()) != null) {
				if(buffer.startsWith(":")) { 
					if(buffer.split(" ").length == 1) { 
						name = buffer.trim(); 
						if(me.verbose) System.out.println("Class: " + name); 
					}
				}
				if(buffer.contains("rdfs:subClassOf")) {
					buffer = buffer.trim();
					String[] part = buffer.split(" ");
					if(part.length > 1 && name != null) {
						me.push(part[1], name);
					}					
				}
				if(buffer.contains("owl:equivalentClass")) {
					buffer = buffer.trim();
					String[] part = buffer.split(" ");
					if(part.length > 1 && name != null) {
						me.equiv.put(name, part[1]);
					}					
				}
				if(buffer.contains("owl:someValuesFrom")) {
					buffer = buffer.trim();
					String[] part = buffer.split(" ");
					if(part.length > 1 && name != null) {
						me.push(name, part[1]);
					}
				}
				if(buffer.endsWith(".")) {
					if(name != null) {
						if( ! me.map.containsKey(name) && ! me.equiv.containsKey(name)) {
							me.map.put(name,  new ArrayList<String>());
							if(me.verbose) System.out.println("Map: " + name + " [" + me.map.get(name).size() + "]");
						}
					}
					name = null;
				}
			}
			
			reader.close();
			
			if(me.verbose) {
				System.out.println("Map size: " + me.map.size());
			 	for(String item : me.map.keySet()) {
					System.out.println(item);				
				}
		}
			
			me.printEnum(startNode.replace(":",  ""), me.delim, startNode);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * Display help information.
    *
	 * @since		1.0
	 **/
	public void showHelp()
	{
		System.out.println("");
		System.out.println(getClass().getName() + "; Version: " + mVersion);
		System.out.println("Converts an ontology in Turtle format to string enumerations.");
		System.out.println("");
		
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java " + getClass().getName() + " [options]\n", mOptions );

		System.out.println("");
		System.out.println("Development funded by NASA's SPASE and VMO projects at UCLA.");
		System.out.println("");
	}
	
	void push(String key, String item) {
		if(map.containsKey(key)) {
			ArrayList<String> list = map.get(key);
			list.add(item);
		} else {
			ArrayList<String> list = new ArrayList<String>();
			list.add(item);
			map.put(key,  list);
		}
	}
	void printEnum(String prefix, String delim, String item)
	{
		if(verbose) System.out.println("Looking for: " + item);
		
		if( ! map.containsKey(item)) {	// Try equiv
			if(equiv.containsKey(item)) {
				item = equiv.get(item);
			}
		}

		ArrayList<String> list = map.get(item);
		if(list == null) { out.println(prefix); return; }
		if(list.size() == 0) out.println(prefix);
		if(verbose) System.out.println("[" + list.size() + "]");
		
		for(String line : list) {
			printEnum(prefix+line.replace(":",  delim), delim, line);
		}
	}
}
