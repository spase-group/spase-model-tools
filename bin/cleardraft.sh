# Change a document file names for a version to draft names.
# Designed for the SPASE website environment.
#
# Author: Todd King
#
version=${1:-2.2.1}
dbname=${2:-spase-base-model}
homepath=${3:-/c/projects/work/spase/website/site/ROOT}

vername=`echo $version | sed 's/\./_/g'`
verpack=`echo $version | sed 's/\.//g'`

# "spase-model" becomes "". All others become suffix
temp=(${dbname//-/ })
group="-"${temp[1]}
if [ $group = "-model" ]; then
   group=""
fi

rm -f $homepath/data/dictionary/spase$group-$vername-draft.pdf
rm -f $homepath/data/schema/spase$group-$vername-draft.xsd 
rm -f $homepath/data/xmi/spase$group-$vername-draft.xmi
rm -f $homepath/data/model/spase$group-$vername-draft.pdf

rm -f $homepath/docs/dictionary/spase$group-$vername-draft.pdf
rm -f $homepath/docs/schema/spase$group-$vername-draft.xsd
rm -f $homepath/docs/xmi/spase$group-$vername-draft.xmi
rm -f $homepath/docs/model/spase$group-$vername-draft.pdf

rm -r -f $homepath/data/reference/spase$group-$vername-draft