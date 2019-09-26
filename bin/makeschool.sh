# Copy files from main web site to school
#
# Author: Todd King
#
version=${1:-2.2.3}
homepath=${2:-/c/projects/spase/webapp/website/site/ROOT}
vername=`echo $version | sed 's/\./_/g'`
verpack=`echo $version | sed 's/\.//g'`

cp $homepath/docs/dictionary/spase-$vername.pdf $homepath/../../../school/site/docs/dictionary
cp $homepath/data/model/spase-$vername.pdf $homepath/../../../school/site/docs/model
cp $homepath/data/schema/spase-$vername.xsd $homepath/../../../school/site/docs/schema
# cp $homepath/docs/xmi/spase-$vername.xmi $homepath/../../../school/site/docs/xmi
