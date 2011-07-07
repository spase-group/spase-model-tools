# Build all the type of documents related to a version of the SPASE model.
# Designed for the SPASE website envronment.
#
# Author: Todd King
#
version=${1:-2.2.1}
homepath=${2:-/c/projects/spase/webapp/ROOT}
vername=`echo $version | sed 's/\./_/g'`
verpack=`echo $version | sed 's/\.//g'`

# Make the document
echo $homefolder
echo "./makedoc.sh $version $homepath > $homepath/data/dictionary/spase-$vername.pdf" 
./makedoc.sh $version $homepath > $homepath/data/dictionary/spase-$vername.pdf 
cp $homepath/data/dictionary/spase-$vername.pdf $homepath/docs/dictionary

# Make the schema
./makexsd.sh $version $homepath

# Make the XMI model
./makexmi.sh $version $homepath

# Make the editor files
 ./makeeditor.sh $version $homepath

# Make the registry files
# ./makeregistry.sh $version $homepath

# Make XSL files
./makexsl.sh $version $homepath

echo
echo "*** TO DO ***"
# Make the parser files (classic and JAXB)
./makeparser.sh $version $homepath
./makejaxb.sh $version $homepath

# Make schema documentation (using Oxygen)
echo Run the command: makeschema.bat $vername $homepath

# Copy documents to SPASE School
echo Then run the command: ./makeschool.sh $version $homepath
echo To copy generated documents to the SPASE school.