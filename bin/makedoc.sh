# Run PHP to create the PDF document. 
# Strip first 5 lines to remove HTTP header.
# Output is to stdout, must redirect into desired file
#
# Designed for the SPASE website envronment.
#
# Author: Todd King
#

version=${1:-2.2.1}
homepath=${2:-/c/projects/spase/webapp/ROOT}
# php makedoc.php "&version=$version" | sed '1,5d'
php makedoc.php $version $homepath
