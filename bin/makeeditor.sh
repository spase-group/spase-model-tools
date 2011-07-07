# Make the Editor XML stylesheet (XSL) files for a version of the SPASE data model.
# Designed for the SPASE website envronment.
#
# Author: Todd King
#
version=${1:-2.2.1}
homepath=${2:-/c/projects/spase/webapp/ROOT}
vername=`echo $version | sed 's/\./_/g'`
verpack=`echo $version | sed 's/\.//g'`

# Make the editor files
./runjava.sh org.spase.model.util.MakeXSL $version edit $homepath/data ../tools/editor
