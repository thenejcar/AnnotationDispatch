package si.kisek.annotationdispatch;


import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;
import si.kisek.annotationdispatch.models.MethodInstance;
import si.kisek.annotationdispatch.models.MethodModel;
import si.kisek.annotationdispatch.models.MethodSwitcher;
import si.kisek.annotationdispatch.utils.CodeGeneratorSwitch;
import si.kisek.annotationdispatch.utils.ReplaceMethodsVisitor;

import javax.annotation.processing.*;
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
public class ProcessorSwitch extends MultidispatchProcessor {

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

        generateNewMethods(roundEnv);
        replaceMethodCalls(roundEnv);

        super.addNewMethods();

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

            MethodSwitcher methodSwitcher = new MethodSwitcher(types, model, instances);

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

            System.out.println("Method " + generatedMethod.name.toString() + " generated");

        }
    }

    protected void replaceMethodCalls(RoundEnvironment roundEnv) {
        //TODO: check if we really need the class annotation, maybe we can find all places where annotated methods were called?

        for (MethodModel model : originalMethods.keySet()) {
            Name newMethodName = generatedMethods.get(model).name;

            for (Element e : roundEnv.getElementsAnnotatedWith(MultiDispatchClass.class)) {
                JCTree.JCClassDecl classTree = (JCTree.JCClassDecl) trees.getPath(e).getLeaf();

                for (MethodInstance oldMethod : originalMethods.get(model)) {
                    ReplaceMethodsVisitor visitor = new ReplaceMethodsVisitor(oldMethod, newMethodName);
                    visitor.visitClassDef(classTree);
                }

                System.out.println("Calls to " + model.getName() + " in " + classTree.name + " replaced with calls to " + newMethodName);
            }
        }
    }
}
