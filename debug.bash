#!/bin/bash

cd ./Annotation
mvn clean install
cd ../Processor
mvn clean install
cd ../Demo
mvnDebug clean compile

echo
echo "Compilation done, running the compiled TextClass"
cd target/classes/
java si.kisek.annotationdispatch.TestClass

