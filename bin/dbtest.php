<?php
/**
 * Simple example of extending the SQLite3 class and changing the __construct
 * parameters, then using the open method to initialize the DB.
 */
print "Startup\n";
$Version = "2.2.1";
$DatabaseConn = new SQLite3('/Projects/spase/webapp/ROOT/html/data/spase-model.db');

function ShowTree2($term, $indent, $occur, $group, $pointer)
{
	global $Version;
	global $DatabaseConn;
	
	print "ShowTree2()\n";
  $rowList = array();
  
	   $query = "select" 
	          . " *"
	          . " from ontology"
	          . " where ontology.Object = '" . $term  . "'"
	          . " and ontology.Version='" . $Version  . "'"
	          . " Order By ontology.Pointer"
	          ;
	
	   print "Query: $query\n";
	   $result = $DatabaseConn->query($query);
	   if(!$result) {
	      print "Error in query: " . $query;
	      done();
	   }
	   
	   // Store results 
	   $count = 0;
	   $showName = 1;
	   $i = 0;
	   while($row = $result->fetchArray()) {
	   	  print $row['Object'] . "\n";
	   	  
	   	  $rowList[] = $row;
	   }
	   
	   print count($rowList);
}

function PrintTerm($term, $indent, $occur, $group, $pointer, $hasElements)
{
   print "Term: $term\n";
}

print "Calling ShowTree2\n";
ShowTree2('Spase', 0, 1, "", 0);

?>
