# Make the SQLite database for a version of the SPASE data model.
# Designed for the SPASE website envronment.
#
# Author: Todd King
#
version=${1:-2.2.3}
homepath=${2:-/c/projects/spase/webapp/root}
vername=`echo $version | sed 's/\./_/g'`
verpack=`echo $version | sed 's/\.//g'`

# Make the UML xmi file
pushd /c/projects/spase/data-model/draft-2_2_X/sqlite
./clear-version.sh $version
./load-tables.sh
cp spase-model.db $homepath/data/
popd