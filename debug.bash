#!/bin/bash

cd ./Annotation
mvn clean install
cd ../Processor
mvn clean install
cd ../Demo
echo
echo
echo "Starting the compilation in debug mode"
echo "get the debugger ready..."
echo
mvnDebug -e clean compile

echo
echo "Compilation done, running the compiled TestClass"

cd target/classes/
java si.kisek.annotationdispatch.TestClass

