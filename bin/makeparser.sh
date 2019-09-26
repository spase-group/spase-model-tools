# Make the parser files for a version of the SPASE data model.
# Designed for the SPASE website envronment.
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

# Make directory for build
mkdir $homepath/tools/parser$group/build

# Make the parser files
mkdir $homepath/tools/parser$group/build/parser$group"-"$verpack
./runjava.sh org.spase.model.util.MakeParser -d $dbname".db" -m $version -p $homepath/data -o $homepath/tools/parser$group/build/parser$group"-"$verpack

# Now compile
pushd $homepath/tools/parser$group/build/parser$group"-"$verpack
javac -extdirs ../../../../WEB-INF/lib -d .. *.java

# Build documentation
echo "Building documentation"
javadoc -extdirs ../../../../WEB-INF/lib -d ../api/parser$group"-"$verpack *.java

# Build JAR file
echo "Building jar file"
cd ..
jar cf parser$group"-"$verpack.jar parser$group"-"$verpack spase api/parser$group"-"$verpack

# Distribute
mv -f parser$group"-"$verpack.jar $homepath/tools/parser$group
rm -r -f $homepath/tools/parser$group/api-$verpack
mv -f api/parser$group"-"$verpack $homepath/tools/parser$group/api-$verpack
popd

# Clean-up
/bin/rm -R -f $homepath/tools/parser$group/build

