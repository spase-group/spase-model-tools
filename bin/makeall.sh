# Build all the type of documents related to a version of the SPASE model.
# Designed for the SPASE website environment.
#
# Author: Todd King
#
version=${1:-.}
dbname=${2:-spase-model}
extend=${3:-.}
rootelem=${4:-.}
homepath=${5:-/c/projects/spase/webapp/website/site/ROOT}
vername=`echo $version | sed 's/\./_/g'`
verpack=`echo $version | sed 's/\.//g'`

if [ "$version" = "." ]; then 
  echo "Please provide a version number for the first argument."
  exit
fi

# Make database
echo "Create database..."
./makedb.sh $version $dbname $homepath

# Make the schema
echo "Create schema..."
./makexsd.sh $version $dbname $extend $rootelem $homepath

base=$dbname
if [ $base = "spase-model" ]; then
   base="spase"
fi


# Make the document
#if [ $base = "spase" ]; then
echo "Create document..."
   echo $homefolder
   echo "./makedoc.sh $version $dbname $homepath > $homepath/data/dictionary/$base-$vername.pdf" 
   ./makedoc.sh $version $dbname $homepath > $homepath/data/model/$base-$vername.pdf 
   cp $homepath/data/model/$base-$vername.pdf $homepath/docs/dictionary

   
   # Make the XMI model
#echo "Create XMI model..."
#   ./makexmi.sh $version $dbname $homepath

   # Make the editor files
#echo "Create editor files..."
#   ./makeeditor.sh $version $dbname $homepath

   # Make the registry files
   # ./makeregistry.sh $version $homepath

   # Make XSL files
echo "Create XSL files..."
   ./makexsl.sh $version $dbname $homepath
#fi

echo
echo "*** TO DO ***"
# Make the parser files (classic and JAXB)
echo "Create parser..."
./makeparser.sh $version $dbname $homepath
./makejaxb.sh $version $dbname $homepath

# Create DOS style homepath
dospath=`echo $homepath | sed -e 's.^/..' -e 's./.:\\\\.' -e 's./.\\\\.g'`

# Make schema documentation (using Oxygen)
echo Run the command: makeschema.bat $base-$vername $dospath
echo ""

# Copy documents to SPASE School
echo Then run the command: bash ./makeschool.sh $version $homepath
echo To copy generated documents to the SPASE school.
echo ""

# Change to draft version
echo If this is a draft version.
echo Run the command: bash ./makedraft.sh $version
echo ""

# Change to draft version
echo If a prior draft version was created and this is a release, remove the old draft.
echo Run the command: bash ./removedraft.sh $version
echo ""
# Change to draft version
# echo Copy all files to the VM.
# echo Run the command: bash ./copytovm.sh $version
