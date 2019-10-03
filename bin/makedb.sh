# Make the SQLite database for a version of the SPASE data model.
# Designed for the SPASE website environment.
#
# Author: Todd King
#

version=${1:-2.2.3}
dbname=${2:-spase-base-model}
homepath=${3:-/c/projects/work/spase/website/site/ROOT}

vername=`echo $version | sed 's/\./_/g'`
verpack=`echo $version | sed 's/\.//g'`
# Change n.n.n to n_n_X
verset=`echo $version | sed -e 's/\.[^.]*$/.X/' -e 's/\./_/g'`

# Determine destination name
destname=$dbname
if [ $destname = "spase-base-model" ]; then
   destname="spase-model"
fi
if [ $destname = "spase-sim-model" ]; then
   destname="spase-sim"
fi

# Make the UML xmi file
echo  /c/projects/work/spase/data-model/$dbname/sqlite
pushd /c/projects/work/spase/data-model/$dbname/sqlite
./clear-version.sh $version $dbname
./load-tables.sh $dbname
cp $dbname".db" $homepath/data/$destname".db"
cp $dbname".db" $homepath/data/model/$destname".db"
popd