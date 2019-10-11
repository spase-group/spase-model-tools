# Run PHP to create the PDF document. 
# Strip first 5 lines to remove HTTP header.
# Output is to stdout, must redirect into desired file
#
# Designed for the SPASE website environment.
#
# Author: Todd King
#

version=${1:-2.2.1}
dbname=${2:-spase-base-model}
homepath=${3:-/c/projects/work/spase/website/site/ROOT}

# Change base name as needed. "spase-base-model" becomes "spase", "spase-sim-model" becomes "spase-sim". All others remain as is.
base=$dbname
if [ $base = "spase-base-model" ]; then
   base="spase"
   dbname="spase-model"
fi
if [ $base = "spase-sim-model" ]; then
   base="spase-sim"
   dbname="spase-sim"
fi

# php makedoc.php "&version=$version" | sed '1,5d'
/c/tools/php/php.exe makedoc.php $version $dbname $homepath
