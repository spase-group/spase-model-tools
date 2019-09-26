# Make the SQLite database for a version of the SPASE data model.
# Designed for the SPASE website environment.
#
# Author: Todd King
#

version=${1:-2.2.3}
dbname=${2:-spase-model}
homepath=${3:-/c/projects/spase/webapp/website/site/ROOT}

vername=`echo $version | sed 's/\./_/g'`
verpack=`echo $version | sed 's/\.//g'`
# Change n.n.n to n_n_X
verset=`echo $version | sed -e 's/\.[^.]*$/.X/' -e 's/\./_/g'`

# Make the UML xmi file
echo  /c/projects/spase/data-model/$dbname-$verset/sqlite
pushd /c/projects/spase/data-model/$dbname-$verset/sqlite
./clear-version.sh $version $dbname
./load-tables.sh $dbname
cp $dbname".db" $homepath/data/
cp $dbname".db" $homepath/data/model
popd