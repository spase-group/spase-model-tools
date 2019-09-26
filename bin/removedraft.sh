# Change a document file names for a version to draft names.
# Designed for the SPASE website environment.
#
# Author: Todd King
#
version=${1:-2.2.1}
dbname=${2:-spase-model}
homepath=${3:-/c/projects/spase/webapp/website/site/ROOT}

vername=`echo $version | sed 's/\./_/g'`
verpack=`echo $version | sed 's/\.//g'`

# "spase-model" becomes "". All others become suffix
temp=(${dbname//-/ })
group="-"${temp[1]}
if [ $group = "-model" ]; then
   group=""
fi

rm -r -f $homepath/data/schema/spase$group-$vername-draft.xsd 
rm -r -f $homepath/data/model/spase$group-$vername-draft.pdf
rm -r -f $homepath/data/model/schema/spase$group-$vername-draft.pdf
rm -r -f $homepath/docs/dictionary/spase$group-$vername-draft.pdf
