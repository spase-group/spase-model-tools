# Make the XML stylesheet (XSL) files for a version of the SPASE data model.
# Designed for the SPASE website envronment.
#
# Author: Todd King
#
version=${1:-2.2.1}
dbname=${2:-spase-base-model}
homepath=${3:-/c/projects/work/spase/website/site/ROOT}
vername=`echo $version | sed 's/\./_/g'`
verpack=`echo $version | sed 's/\.//g'`
zip="\c\Program Files\7-Zip\7z.exe"

# "spase-model" becomes "spase". All others left as is
base=$dbname
if [ $base = "spase-base-model" ]; then
   base="spase"
fi

# "spase-model" becomes "". All others become suffix
temp=(${dbname//-/ })
group="-"${temp[1]}
if [ $group = "-model" ]; then
   group=""
fi

# Make XSL directory for build
mkdir $homepath/tools/stylesheet/html/xsl

# Make the editor XSL
mkdir $homepath/tools/stylesheet/html/xsl/"editor"$group"-"$verpack
#./runjava.sh org.spase.model.util.MakeXSL $version edit $homepath/data $homepath/tools/stylesheet/html/xsl/editor$verpack
./runjava.sh org.spase.model.util.MakeXSL -e -d $dbname".db" -m $version -p $homepath/data -o $homepath/tools/stylesheet/html/xsl/"editor"$group"-"$verpack

# Make the display XSL
mkdir $homepath/tools/stylesheet/html/xsl/"display"$group"-"$verpack
#./runjava.sh org.spase.model.util.MakeXSL $version display $homepath/data $homepath/tools/stylesheet/html/xsl/display$verpack
./runjava.sh org.spase.model.util.MakeXSL -d $dbname".db" -m $version -p $homepath/data -o $homepath/tools/stylesheet/html/xsl/"display"$group"-"$verpack

# Package XSL files
cd $homepath/tools/stylesheet/html/
"$zip" -r "spase"$group"-xsl-"$verpack".zip" xsl/"editor"$group"-"$verpack xsl/"display"$group"-"$verpack

# Clean-up
rm -r -f xsl
