# Make the JAXB generated java classes to support a version of i
# the SPASE data model.
# Designed for the SPASE website envronment.
#
# Author: Todd King
#
version=${1:-2.2.1}
homepath=${2:-/c/projects/spase/webapp/root}
vername=`echo $version | sed 's/\./_/g'`
verpack=`echo $version | sed 's/\.//g'`

# Path to JAXB
export JAXB_HOME=c:/java/jaxb

# Make directory for build
mkdir $homepath/tools/parser/build

# Make the parser files
pushd $homepath/tools/parser/build

/bin/rm -R -f parser$verpack
/bin/rm -R -f META-INF
mkdir META-INF
mkdir parser$verpack

cd parser$verpack

$JAXB_HOME/bin/xjc.sh -episode ../META-INF/episode.xml -p org.spase.parser$verpack $homepath/data/schema/spase-$vername.xsd -d .  

# Now compile
javac -Djava.ext.dirs=$homepath/WEB-INF/lib:$CLASSPATH:$homepath/WEB-INF/tools/jaxb/lib -d .. org/spase/parser$verpack/*.java

# Build documentation
javadoc -extdirs $homepath/WEB-INF/lib:$CLASSPATH:$homepath/WEB-INF/tools/jaxb/lib -d ../api/parser$verpack org/spase/parser$verpack/*.java

# Build JAR file
cd ..
jar cf spase-jaxb-parser$verpack.jar META-INF parser$verpack org/spase/parser$verpack api/parser$verpack

# Distribute
cp spase-jaxb-parser$verpack.jar $homepath/tools/parser
rm -r -f $homepath/tools/parser/jaxb/api$verpack
mv api/parser$verpack $homepath/tools/parser/jaxb/api$verpack

popd

# Clean-up
/bin/rm -R -f $homepath/tools/parser/build

