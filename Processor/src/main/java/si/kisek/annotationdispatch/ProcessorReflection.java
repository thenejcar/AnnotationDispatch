package si.kisek.annotationdispatch;


import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import si.kisek.annotationdispatch.models.MethodModel;
import si.kisek.annotationdispatch.utils.CodeGeneratorReflection;
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

    private JCTree.JCVariableDecl methodMapDecl;
    private JCTree.JCMethodDecl initMethodDecl;
    private Map<MethodModel, JCTree.JCMethodDecl> generatedMethods;

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
        // generate all the code
        CodeGeneratorReflection generator = new CodeGeneratorReflection(tm, elements, types, symtab, originalMethods);
        methodMapDecl = generator.generateMethodMapDecl();
        initMethodDecl = generator.generateInitMethod();
        generatedMethods = generator.generateDispatchers();

        // use generated methods instead of the original ones
        replaceMethodCalls(roundEnv);

        // add the init logic to the constructors
        addInitLogicToClass();

        // fill the symtables
        addNewMethods();

        return true;
    }


    /*
     * Replace calls to the orignal methods with the calls to generated disatcher
     * */
    private void replaceMethodCalls(RoundEnvironment roundEnv) {
        for (MethodModel model : originalMethods.keySet()) {
            for (Element e : roundEnv.getElementsAnnotatedWith(MultiDispatchClass.class)) {
                super.replaceMethodsInClass(model, generatedMethods.get(model), (JCTree.JCClassDecl) trees.getPath(e).getLeaf());
            }
        }
    }

    /*
     * Add the var decl to class and call init method in all constructors
     * */
    private void addInitLogicToClass() {

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
