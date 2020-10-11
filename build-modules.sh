#!usr/env/bin bash
rm -rf src/semantics/target &&
rm -rf src/parser/target &&
rm -rf src/verifier/target &&
cd src/semantics && mvn install &&
cd ../parser && mvn install &&
cd ../verifier && mvn install &&
cd ../..