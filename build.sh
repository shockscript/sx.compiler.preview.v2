#!usr/env/bin bash
. build-modules.sh &&
rm -rf src/application/target &&
cd src/application && mvn install &&
cat target/sxc-1.0-SNAPSHOT.jar > lib/sxc.jar &&
cd ../..