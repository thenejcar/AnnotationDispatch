#!/bin/bash

which mvn > /dev/null
if [ $? -eq 1 ]; then
    echo "Please install apache maven and make sure the command 'mvn' is on the classpath"
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
echo "Default java dispatch"
cd ../Demo
mvn -Punmodified clean compile
cd ./target/classes
echo "TestClass"
echo "----------------------------------------------------------------------------"
java si.kisek.annotationdispatchdemo.TestClass
echo "----------------------------------------------------------------------------"
echo "AnotherClass"
echo "----------------------------------------------------------------------------"
java si.kisek.annotationdispatchdemo.AnotherClass
echo "----------------------------------------------------------------------------"


echo
#build the demo program with annotation processing and run it
echo "Multiple dispatch, if-instanceof tree:"
cd ../../
mvn -Pannotation-processing-tree clean compile
cd ./target/classes
echo "TestClass"
echo "----------------------------------------------------------------------------"
java si.kisek.annotationdispatchdemo.TestClass
echo "----------------------------------------------------------------------------"
echo "AnotherClass"
echo "----------------------------------------------------------------------------"
java si.kisek.annotationdispatchdemo.AnotherClass
echo "----------------------------------------------------------------------------"

echo
echo
echo

echo "Multiple dispatch, visitor:"
cd ../../
mvn -Pannotation-processing-visitor clean compile
cd ./target/classes
echo "TestClass"
echo "----------------------------------------------------------------------------"
java si.kisek.annotationdispatchdemo.TestClass
echo "----------------------------------------------------------------------------"
echo "AnotherClass"
echo "----------------------------------------------------------------------------"
java si.kisek.annotationdispatchdemo.AnotherClass
echo "----------------------------------------------------------------------------"

echo
echo
echo

echo "Multiple dispatch, reflection:"
cd ../../
mvn -Pannotation-processing-reflection clean compile
cd ./target/classes
echo "TestClass"
echo "----------------------------------------------------------------------------"
java si.kisek.annotationdispatchdemo.TestClass
echo "----------------------------------------------------------------------------"
echo "AnotherClass"
echo "----------------------------------------------------------------------------"
java si.kisek.annotationdispatchdemo.AnotherClass
echo "----------------------------------------------------------------------------"

