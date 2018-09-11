package si.kisek.annotationdispatch;


import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import si.kisek.annotationdispatch.models.MethodInstance;
import si.kisek.annotationdispatch.models.MethodModel;
import si.kisek.annotationdispatch.models.MethodSwitcher;
import si.kisek.annotationdispatch.utils.CodeGeneratorSwitch;
import si.kisek.annotationdispatch.utils.Utils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.*;

import static si.kisek.annotationdispatch.utils.Utils.javacList;


/*
 * Processor that implements multiple dispatch with an 'if instanceof' switch
 * */
@SupportedAnnotationTypes({
        "si.kisek.annotationdispatch.ExampleAnnotation",
        "si.kisek.annotationdispatch.MultiDispatch",
        "si.kisek.annotationdispatch.MultiDispatchClass",
        "si.kisek.annotationdispatch.MultiDispatchVisitable"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ProcessorTree extends MultidispatchProcessor {

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

        // create the new methods
        generateNewMethods(roundEnv);

        // replace all calls with calls to our methods
        replaceMethodCalls(roundEnv);

        // add the new methods into classes
        addNewMethods();

        return true;
    }

    /*
     * generate new methods that implement multiple dispatch
     * */
    private void generateNewMethods(RoundEnvironment roundEnv) {
        if (originalMethods == null) {
            throw new RuntimeException("Cannot start the second pass if methods map is null");
        }

        generatedMethods = new HashMap<>();

        // generate the switcher
        for (MethodModel model : originalMethods.keySet()) {

            JCTree.JCMethodDecl generatedMethod = model.generateDispatchMethod(tm, elements);
            Set<MethodInstance> instances = originalMethods.get(model);

            MethodSwitcher methodSwitcher = new MethodSwitcher(super.types, model, instances);

            CodeGeneratorSwitch codeGenerator = new CodeGeneratorSwitch(tm, elements, generatedMethod);
            List<JCTree.JCStatement> statements = new ArrayList<>();
            statements.add(codeGenerator.generateIfInstanceOf(methodSwitcher.getRoot()));

            // throw exception if none of the branches worked
            statements.add(
                    // throw new RuntimeException("No method definition for runtime argument of type " + arg1.getClass())
                    codeGenerator.generateDefaultThrowStat(generatedMethod)
            );

            generatedMethod.body = tm.Block(0, javacList(statements));

            generatedMethods.put(model, generatedMethod);

            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Method " + generatedMethod.name.toString() + " generated");

        }
    }

    private void replaceMethodCalls(RoundEnvironment roundEnv) {
        for (MethodModel mm : originalMethods.keySet()) {
            for (Element e : roundEnv.getElementsAnnotatedWith(MultiDispatchClass.class)) {
                JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) trees.getPath(e).getLeaf();
                if (classDecl == mm.getParentClass()) {
                    super.replaceMethodsInClass(mm, generatedMethods.get(mm), classDecl);
                }
            }
        }
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

            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Method " + generatedMethod.name + " added to " + classDecl.name);
        }
    }
}