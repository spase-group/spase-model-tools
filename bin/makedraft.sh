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

# mv $homepath/data/dictionary/spase$group-$vername.pdf $homepath/data/dictionary/spase$group-$vername-draft.pdf
mv $homepath/data/schema/spase$group-$vername.xsd $homepath/data/schema/spase$group-$vername-draft.xsd 
mv $homepath/data/model/schema/spase$group-$vername.pdf  $homepath/data/model/schema/spase$group-$vername-draft.pdf
mv $homepath/data/model/spase$group-$vername.pdf  $homepath/data/model/spase$group-$vername-draft.pdf
mv $homepath/data/model/spase$group-$vername  $homepath/data/model/spase$group-$vername-draft

# mv $homepath/data/xmi/spase$group-$vername.xmi $homepath/data/xmi/spase$group-$vername-draft.xmi

mv $homepath/docs/dictionary/spase$group-$vername.pdf $homepath/docs/dictionary/spase$group-$vername-draft.pdf
# mv $homepath/docs/schema/spase$group-$vername.xsd $homepath/docs/schema/spase$group-$vername-draft.xsd
# mv $homepath/docs/model/spase$group-$vername.pdf  $homepath/docs/model/spase$group-$vername-draft.pdf
# mv $homepath/docs/xmi/spase$group-$vername.xmi  $homepath/docs/xmi/spase$group-$vername-draft.xmi

# rm -r -f $homepath/data/reference/spase$group-$vername-draft
# mv $homepath/data/reference/spase$group-$vername  $homepath/data/reference/spase$group-$vername-draft