# Make the schema files for a version of the SPASE data model.
# Designed for the SPASE website environment.
#
# Author: Todd King
#
version=${1:-2.2.1}
dbname=${2:-spase-base-model}
extend=${3:-.}
rootelem=${4:-.}
homepath=${5:-/c/projects/work/spase/website/site/ROOT}

vername=`echo $version | sed 's/\./_/g'`
verpack=`echo $version | sed 's/\.//g'`

# Change base name as needed. "spase-base-model" becomes "spase", "spase-sim-model" becomes "spase-sim". All others remain as is.
base=$dbname
if [ $base = "spase-base-model" ]; then
   base="spase"
   dbname="spase-model"
fi
if [ $base = "spase-sim-model" ]; then
   base="spase-sim"
   dbname="spase-sim"
fi

# Make the schema
./runjava.sh org.spase.model.util.MakeXSD -d $dbname".db" -m $version -x $extend -r $rootelem -p $homepath"/data" > $homepath/data/schema/$base-$vername.xsd
cp $homepath/data/schema/$base-$vername.xsd $homepath/data/model/schema
