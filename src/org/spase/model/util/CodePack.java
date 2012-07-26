/**
 * Read a file and format it so that it can be used in
 * other source code generation applications to write out
 * the file. The preperation involves escaping every qoutation (")
 * and escape character (/), then outputing the line using printLine().
 *
 * @author Todd King
 * @version 1.00 2007 02 01
 * @copyright 2007 Regents University of California. All Rights Reserved
 */

package org.spase.model.util;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.PrintStream;

/** 
* Build Java code which can be used to write the contents of the file from
* a Java class.
*<p>
* Usage:<blockquote>
*     CodePack source output
* </blockquote>
*
* @author Todd King
* @author UCLA/IGPP
* @version 1.00 2007 02 01
* @since		1.0
*/
public class CodePack
{
	private String	mVersion = "1.0.0";
	
	public static void main(String args[])
	{
		PrintStream	out = System.out;
		BufferedReader	in;
		String	buffer;
		
		CodePack me = new CodePack();
		   
		if (args.length < 1) {
			System.err.println("Version: " + me.mVersion);
			System.err.println("Usage: " + me.getClass().getName() + " source [output]");
			System.exit(1);
		}

		try {
			// Open input file
			in = new BufferedReader(new FileReader(args[0]));
			// Open output file
			if(args.length > 1) {	// Open output file
				out = new PrintStream(new FileOutputStream(args[1]));
			}		
			
			while((buffer = in.readLine()) != null) {
				buffer = buffer.replace("\\", "\\\\");
				buffer = buffer.replace("\"", "\\\"");
				out.println("      printLine(\"" + buffer + "\");");
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}