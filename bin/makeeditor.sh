# Make the Editor XML stylesheet (XSL) files for a version of the SPASE data model.
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

# Make the editor files
./runjava.sh org.spase.model.util.MakeXSL -e -d $dbname".db" -m $version -p $homepath/data -o $homepath/tools/"editor"$group
