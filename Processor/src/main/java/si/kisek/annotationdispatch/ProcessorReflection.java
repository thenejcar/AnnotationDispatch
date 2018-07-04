package si.kisek.annotationdispatch;


import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import si.kisek.annotationdispatch.models.MethodModel;
import si.kisek.annotationdispatch.utils.Utils;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.*;

import static si.kisek.annotationdispatch.utils.Utils.javacList;


/*
 * Processor that implements multiple dispatch with an 'if instanceof' switch
 * */
@SupportedAnnotationTypes({
        "si.kisek.annotationdispatch.ExampleAnnotation",
        "si.kisek.annotationdispatch.MultiDispatch",
        "si.kisek.annotationdispatch.MultiDispatchClass",
        "si.kisek.annotationdispatch.MultiDispatchVisitable",
        "si.kisek.annotationdispatch.MultiDispatchVisitableBase"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ProcessorReflection extends MultidispatchProcessor {

    private Map<MethodModel, JCTree.JCMethodDecl> generatedMethods = new HashMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (originalMethods.size() > 0) {
            // already processed, skip
            return false;
        }

        originalMethods = super.processAnnotatedMethods(roundEnv);
        if (originalMethods.size() == 0) {
            // no annotated methods, skip
            return true;
        }
        JCTree.JCMethodDecl initMethod = generateInitCalls(roundEnv);

        generateDispatchers(roundEnv);

        replaceMethodCalls(roundEnv);

        modifyConstructor();
        addNewMethods();

        return true;
    }


    /*
    * Generate the code that creates the map of mehods in the class, that will be placed in the constructor
    * */
    private JCTree.JCMethodDecl generateInitCalls(RoundEnvironment roundEnv) {

        return null;
    }

    /*
     * Generate the code that accepts method name and list of params and dispatches to correct original method
     * */
    private void generateDispatchers(RoundEnvironment roundEnv) {

    }

    /*
    * Replace calls to the orignal methods with the calls to generated disatcher
    * */
    private void replaceMethodCalls(RoundEnvironment roundEnv) {
        //TODO: check if we really need the class annotation, maybe we can find all places where annotated methods were called?

        for (MethodModel model : originalMethods.keySet()) {
            for (Element e : roundEnv.getElementsAnnotatedWith(MultiDispatchClass.class)) {
                super.replaceMethodsInClass(model, generatedMethods.get(model), (JCTree.JCClassDecl) trees.getPath(e).getLeaf());
            }
        }
    }

    /*
    * Add a call to init method to the constructor
    * */
    private void modifyConstructor() {

    }



    /*
     * Inject generated method in the parent class of the original ones
     * */
    private void addNewMethods() {

        for (MethodModel model : generatedMethods.keySet()) {
            JCTree.JCMethodDecl generatedMethod = generatedMethods.get(model);

            JCTree.JCClassDecl classDecl = model.getParentClass();

            classDecl.defs = classDecl.defs.append(generatedMethod);  // add the method to the class

            List<Type> paramTypes = new ArrayList<>();
            for (JCTree.JCVariableDecl varDecl : generatedMethod.params) {
                paramTypes.add(varDecl.type);
            }
            Symbol.MethodSymbol methodSymbol = new Symbol.MethodSymbol(
                    generatedMethod.mods.flags,
                    generatedMethod.name,
                    new Type.MethodType(javacList(paramTypes), generatedMethod.getReturnType().type, javacList(new Type[0]), symtab.methodClass),
                    classDecl.sym
            );

            // use reflection to add the generated method symbol to the parent class
            Utils.addSymbolToClass(classDecl, methodSymbol);

            System.out.println("Method " + generatedMethod.name + " added to " + classDecl.name);
        }
    }
}
