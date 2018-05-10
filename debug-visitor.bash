#!/bin/bash

cd ./Annotation
mvn clean install
cd ../Processor
mvn clean install
cd ../Demo
echo
echo
echo "Starting the compilation in debug mode (annotation-processing-visitor profile)"
echo "get the debugger ready..."
echo
mvnDebug -Pannotation-processing-visitor -e -X clean compile

echo
echo "Compilation done, running the compiled TestClass"

cd target/classes/
java si.kisek.annotationdispatch.TestClass

