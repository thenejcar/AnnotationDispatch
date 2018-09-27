#!/usr/bin/env bash

cd ./Annotation
mvn clean install
cd ../Processor
mvn clean install
cd ../Demo
echo

# for different versions probably just need to change the -processor flag
/Users/nejc/source_installs/langtools-1ff9d5118aae/dist/bootstrap/bin/javac -d /Users/nejc/git_repos/AnnotationDispatch/Demo/target/classes -classpath /Users/nejc/git_repos/AnnotationDispatch/Demo/target/classes:/Users/nejc/.m2/repository/si/kisek/annotationdispatch/annotation/1.0/annotation-1.0.jar:/Users/nejc/.m2/repository/si/kisek/annotationdispatch/processor/1.0/processor-1.0.jar:/Users/nejc/source_installs/langtools-1ff9d5118aae/dist/lib/classes.jar:/Users/nejc/source_installs/langtools-1ff9d5118aae/dist/bootstrap/lib/javac.jar: -sourcepath /Users/nejc/git_repos/AnnotationDispatch/Demo/src/main/java:/Users/nejc/git_repos/AnnotationDispatch/Demo/target/generated-sources/annotations: /Users/nejc/git_repos/AnnotationDispatch/Demo/src/main/java/si/kisek/annotationdispatchdemo/models/Reptile.java /Users/nejc/git_repos/AnnotationDispatch/Demo/src/main/java/si/kisek/annotationdispatchdemo/models/Pine.java /Users/nejc/git_repos/AnnotationDispatch/Demo/src/main/java/si/kisek/annotationdispatchdemo/models/Mammal.java /Users/nejc/git_repos/AnnotationDispatch/Demo/src/main/java/si/kisek/annotationdispatchdemo/models/Cat.java /Users/nejc/git_repos/AnnotationDispatch/Demo/src/main/java/si/kisek/annotationdispatchdemo/AnotherClass.java /Users/nejc/git_repos/AnnotationDispatch/Demo/src/main/java/si/kisek/annotationdispatchdemo/models/Animal.java  /Users/nejc/git_repos/AnnotationDispatch/Demo/src/main/java/si/kisek/annotationdispatchdemo/models/Tree.java /Users/nejc/git_repos/AnnotationDispatch/Demo/src/main/java/si/kisek/annotationdispatchdemo/models/Dog.java /Users/nejc/git_repos/AnnotationDispatch/Demo/src/main/java/si/kisek/annotationdispatchdemo/models/Lizard.java /Users/nejc/git_repos/AnnotationDispatch/Demo/src/main/java/si/kisek/annotationdispatchdemo/models/Oak.java /Users/nejc/git_repos/AnnotationDispatch/Demo/src/main/java/si/kisek/annotationdispatchdemo/models/Apple.java /Users/nejc/git_repos/AnnotationDispatch/Demo/src/main/java/si/kisek/annotationdispatchdemo/TestClass.java  -s /Users/nejc/git_repos/AnnotationDispatch/Demo/target/generated-sources/annotations -processor si.kisek.annotationdispatch.ProcessorVisitor -g -verbose

cd ..
