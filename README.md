# AnnotationDispatch

Implementation of multiple dispatch in Java using annotation processing mechanisms. Developed as a part of master's thesis at University of Ljubljana.

### Requirements

 - Java SE 8 JDK
 - Maven (version 3.5 or higher)

### Demo

The `./demo.bash` script will compile both dependencies (Annotation and Processor) and use them during compilation of the Demo project which contains two demo programs. They will first be compiled without the annotation processing to show the regular Java dispatching behavior and then compiled again with all three implemented annotation processors, showing how the programs behave with multiple dispatch.

### Debugging the annotation processor

 - put a breakpoint at the desired line in one of the files in the Processor project
 - run `debug-____.bash` script (builds the processor and annotation, then starts the compilation of the Demo project)
 - connect to port 8000 with the debugger (for example, in IntelliJ IDEA use "remote" run configuration)
 - optionally, change the commented *verbose* tags in `Demo/pom.xml` to true, to get more compiler messages

### Using the library

Directory **Annotations/** contains the annotations that need to be added to the source code:

 - `@MultiDispatch` should be placed on all methods that need to be dispatched using multiple dispatch. The system will automatically group methods with the same name and number of parameters together.
 - `@MultiDispatchClass` should be places on all classes where the annotated methods will be used. If the methods are used in a class that is not annotated with this annotation, they will behave in a regular Java way.
 - `@MultiDispatchVisitable` should be placed on all classes that are used as parameters in dispatched methods, if you want to use the *visitor* dispatching mechanism. When using the other two mechanisms, this annotation does not need to be used (for example if you do not have access to all the used parameter classes).
 
Code annotated in this way needs to be compiled with one of the annotation processors from the *Processor* project: `ProcessorTree`, `ProcessorReflection` or `ProcessorVisitor` that use three different dispatch mechanism. This can be done by adding the `-processor` flag to the javac command or via maven compiler plugin (see `Demo/pom.xml` for an example).


 ### Limitations
 
 The current version of the library requires JDK 8, because the implementation is compiler specific. Additionally, using simple types (int, double, ...) as parameters in methods is not supported -- instead use their equivalent classes (Integer, Double, ...) or, if you wish to use the *visitor* dispatch, wrap them in custom classes. 
