#!/bin/bash

which mvn > /dev/null
if [ $? -eq 1 ]; then
    echo "Please install apache maven to proceed"
    exit 1
fi

# rebuild dependencies (Annotations and the Processor)
cd ./Annotation/
echo "Building dependency $(pwd)"
mvn -q clean install
cd ../Processor
echo "Building dependency $(pwd)"
mvn -q clean install

echo
# build the demo program without processor and run it
echo "TestClass with default java dispatch:"
cd ../Demo
mvn -q -Punmodified clean compile
cd ./target/classes
echo "----------------------------------------------------------------------------"
java si.kisek.annotationdispatch.TestClass
echo "----------------------------------------------------------------------------"

echo
#build the demo program with annotation processing and run it
echo "TestClass with multiple dispatch:"
cd ../../
mvn -q clean compile
cd ./target/classes
echo "----------------------------------------------------------------------------"
java si.kisek.annotationdispatch.TestClass
echo "----------------------------------------------------------------------------"


