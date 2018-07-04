#!/bin/bash

cd ./Annotation
mvn clean install
cd ../Processor
mvn clean install
cd ../Demo
echo
echo
echo "Starting the compilation in debug mode (annotation-processing-reflection profile)"
echo "get the debugger ready..."
echo
mvnDebug -Pannotation-processing-reflection -e -X clean compile

echo
echo "Compilation done, running the compiled TestClass"

cd target/classes/

echo "Double dispatch example: "
java si.kisek.annotationdispatchdemo.AnotherClass

echo "'Quadruple' dispatch example: "
java si.kisek.annotationdispatchdemo.TestClass

