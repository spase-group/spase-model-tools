# Make the UML (xmi) files for a version of the SPASE data model.
# Designed for the SPASE website envronment.
#
# Author: Todd King
#
version=${1:-2.2.1}
dbname=${2:-spase-base-model}
homepath=${3:-/c/projects/work/spase/website/site/ROOT}

vername=`echo $version | sed 's/\./_/g'`
verpack=`echo $version | sed 's/\.//g'`

# "spase-model" becomes "spase". All others left as is
base=$dbname
if [ $base = "spase-model" ]; then
   base="spase"
fi

# Make the UML xmi file
./runjava.sh org.spase.model.util.MakeXMI -a -d $dbname".db" -m $version -p $homepath/data > $homepath/data/xmi/$base-$vername.xmi
cp $homepath/data/xmi/$base-$vername.xmi $homepath/docs/xmi