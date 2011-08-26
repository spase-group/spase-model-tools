# Make the UML (xmi) files for a version of the SPASE data model.
# Designed for the SPASE website envronment.
#
# Author: Todd King
#
version=${1:-2.2.1}
homepath=${2:-/c/projects/spase/webapp/root}
vername=`echo $version | sed 's/\./_/g'`
verpack=`echo $version | sed 's/\.//g'`

# Make the UML xmi file
./runjava.sh org.spase.model.util.MakeXMI $version $homepath/data > $homepath/data/xmi/spase-$vername.xmi
cp $homepath/data/xmi/spase-$vername.xmi $homepath/docs/xmi