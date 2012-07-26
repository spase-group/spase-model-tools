# Change a document file names for a version to draft names.
# Designed for the SPASE website environment.
#
# Author: Todd King
#
version=${1:-2.2.1}
homepath=${2:-/c/projects/spase/webapp/root}
vername=`echo $version | sed 's/\./_/g'`
verpack=`echo $version | sed 's/\.//g'`

mv $homepath/data/dictionary/spase-$vername.pdf $homepath/data/dictionary/spase-$vername-draft.pdf
mv $homepath/data/schema/spase-$vername.xsd $homepath/data/schema/spase-$vername-draft.xsd 
mv $homepath/data/xmi/spase-$vername.xmi $homepath/data/xmi/spase-$vername-draft.xmi
mv $homepath/data/model/spase-$vername.pdf  $homepath/data/model/spase-$vername-draft.pdf

mv $homepath/docs/dictionary/spase-$vername.pdf $homepath/docs/dictionary/spase-$vername-draft.pdf
mv $homepath/docs/schema/spase-$vername.xsd $homepath/docs/schema/spase-$vername-draft.xsd
mv $homepath/docs/xmi/spase-$vername.xmi  $homepath/docs/xmi/spase-$vername-draft.xmi
mv $homepath/docs/model/spase-$vername.pdf  $homepath/docs/model/spase-$vername-draft.pdf

rm -r -f $homepath/data/reference/spase-$vername-draft
mv $homepath/data/reference/spase-$vername  $homepath/data/reference/spase-$vername-draft