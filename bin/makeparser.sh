# Make the parser files for a version of the SPASE data model.
# Designed for the SPASE website envronment.
#
# Author: Todd King
#
version=${1:-2.2.1}
homepath=${2:-/c/projects/spase/webapp/ROOT}
vername=`echo $version | sed 's/\./_/g'`
verpack=`echo $version | sed 's/\.//g'`

# Make directory for build
mkdir $homepath/tools/parser/build

# Make the parser files
mkdir $homepath/tools/parser/build/parser$verpack
./runjava.sh org.spase.model.util.MakeParser $version $homepath/data $homepath/tools/parser/build/parser$verpack

# Now compile
pushd $homepath/tools/parser/build/parser$verpack
javac -d .. *.java

# Build documentation
javadoc -d ../api/parser$verpack *.java

# Build JAR file
cd ..
jar cf parser$verpack.jar parser$verpack spase/parser$verpack api/parser$verpack

# Distribute
mv -f parser$verpack.jar $homepath/tools/parser
rm -r -f $homepath/tools/parser/api$verpack
mv -f api/parser$verpack $homepath/tools/parser/api$verpack
popd

# Clean-up
/bin/rm -R -f $homepath/tools/parser/build

