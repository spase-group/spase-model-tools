# Change a document file names for a version to draft names.
# Designed for the SPASE website environment.
#
# Author: Todd King
#
version=${1:-2.2.1}
dbname=${2:-spase-model}
homepath=${3:-/c/projects/spase/webapp/root/site}
vmpath=~/projects-vm/spase/website/site/ROOT

vername=`echo $version | sed 's/\./_/g'`
verpack=`echo $version | sed 's/\.//g'`

# "spase-model" becomes "". All others become suffix
temp=(${dbname//-/ })
group="-"${temp[1]}
if [ $group = "-model" ]; then
   group=""
fi

echo cp $homepath/data/spase-model.db  $vmpath/data/model/spase-model.db
echo cp $homepath/data/spase-model.db  $vmpath/data/spase-model.db

echo cp $homepath/docs/dictionary/spase$group-$vername.pdf $vmpath/docs/dictionary/spase$group-$vername.pdf

echo cp $homepath/data/schema/spase$group-$vername.xsd $vmpath/data/schema/spase$group-$vername.xsd 
echo cp $homepath/data/schema/spase$group-$vername.xsd $vmpath/data/model/schema/spase$group-$vername.xsd 

echo cp $homepath/data/model/spase$group-$vername.pdf  $vmpath/data/model/spase$group-$vername.pdf
echo cp -r -f $homepath/data/reference/spase$group-$vername $vmpath/data/model

