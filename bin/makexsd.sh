# Make the schema files for a version of the SPASE data model.
# Designed for the SPASE website envronment.
#
# Author: Todd King
#
version=${1:-2.2.1}
homepath=${2:-/c/projects/spase/webapp/root}
vername=`echo $version | sed 's/\./_/g'`
verpack=`echo $version | sed 's/\.//g'`

# Make the schema
./runjava.sh org.spase.model.util.MakeXSD $version $homepath"/data" > $homepath/data/schema/spase-$vername.xsd
cp $homepath/data/schema/spase-$vername.xsd $homepath/docs/schema
