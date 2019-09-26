# Run PHP to create the PDF document. 
# Strip first 5 lines to remove HTTP header.
# Output is to stdout, must redirect into desired file
#
# Designed for the SPASE website environment.
#
# Author: Todd King
#

version=${1:-2.2.1}
dbname=${2:-spase-model}
homepath=${3:-/c/projects/spase/webapp/website/site/ROOT}
# php makedoc.php "&version=$version" | sed '1,5d'
php makedoc.php $version $dbname $homepath
