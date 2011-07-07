# Make the XML stylesheet (XSL) files for a version of the SPASE data model.
# Designed for the SPASE website envronment.
#
# Author: Todd King
#
version=${1:-2.2.1}
homepath=${2:-/c/projects/spase/webapp/ROOT}
vername=`echo $version | sed 's/\./_/g'`
verpack=`echo $version | sed 's/\.//g'`

# Make XSL directory for build
mkdir $homepath/tools/stylesheet/html/xsl

# Make the editor XSL
mkdir $homepath/tools/stylesheet/html/xsl/editor$verpack
./runjava.sh org.spase.model.util.MakeXSL $version edit $homepath/data $homepath/tools/stylesheet/html/xsl/editor$verpack

# Make the display XSL
mkdir $homepath/tools/stylesheet/html/xsl/display$verpack
./runjava.sh org.spase.model.util.MakeXSL $version display $homepath/data $homepath/tools/stylesheet/html/xsl/display$verpack

# Package XSL files
cd $homepath/tools/stylesheet/html/
zip -r spase-xsl-$verpack.zip xsl/editor$verpack xsl/display$verpack

# Clean-up
rm -r -f xsl
