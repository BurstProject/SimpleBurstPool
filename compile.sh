CP=conf/:classes/:lib/*:lib/akka/*
SP=src/

/bin/mkdir -p classes/

scalac -sourcepath $SP -classpath $CP -d classes/ src/scala/*.scala src/scala/simple_burst_pool/*.scala src/scala/simple_burst_pool/model/*.scala src/java/fr/cryptohash/*.java src/java/nxt/*/*.java || exit 1

javac -sourcepath $SP -classpath $CP -d classes/ src/java/fr/cryptohash/*.java src/java/nxt/*/*.java || exit 1

/bin/rm -f simplepool.jar 
jar cf simplepool.jar -C classes . || exit 1
/bin/rm -rf classes

echo "simplepool.jar generated successfully"
