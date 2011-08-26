# Copy files from main web site to school
#
# Author: Todd King
#
version=${1:-2.2.1}
homepath=${2:-/c/projects/spase/webapp/root}
vername=`echo $version | sed 's/\./_/g'`
verpack=`echo $version | sed 's/\.//g'`

cp $homepath/docs/dictionary/spase-$vername.pdf $homepath/../school/html/docs/dictionary
cp $homepath/docs/model/spase-$vername.pdf $homepath/../school/html/docs/model
cp $homepath/docs/schema/spase-$vername.xsd $homepath/../school/html/docs/schema
cp $homepath/docs/xmi/spase-$vername.xmi $homepath/../school/html/docs/xmi
