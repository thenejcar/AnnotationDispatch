package si.kisek.annotationdispatch;


import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import si.kisek.annotationdispatch.models.MethodInstance;
import si.kisek.annotationdispatch.models.MethodModel;
import si.kisek.annotationdispatch.utils.CodeGeneratorReflection;
import si.kisek.annotationdispatch.utils.Utils;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.*;

import static si.kisek.annotationdispatch.utils.Utils.emptyExpr;
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


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (roundEnv.processingOver()) {
            msg.printMessage(Diagnostic.Kind.NOTE, "Processing done");
            return true;
        }

        msg.printMessage(Diagnostic.Kind.NOTE, "Processing round started");

        originalMethods = super.processAnnotatedMethods(roundEnv);
        if (originalMethods.size() == 0) {
            // no annotated methods, skip
            return true;
        }

        // split the originamMethods into multiple maps, according to which class the method definition is in
        // and find the JCComplicationUnit for each of the parent classes
        Map<JCTree.JCClassDecl, Map<MethodModel, Set<MethodInstance>>> splitMethodModels = new HashMap<>();
        Map<JCTree.JCClassDecl, JCTree.JCCompilationUnit> elements = new HashMap<>();
        for (MethodModel mm : originalMethods.keySet()) {
            JCTree.JCClassDecl parent = mm.getParentClass();
            Map<MethodModel, Set<MethodInstance>> partialMap = splitMethodModels.getOrDefault(parent, new HashMap<>());
            partialMap.put(mm, new HashSet<>(originalMethods.get(mm)));
            splitMethodModels.put(parent, partialMap);
            elements.putIfAbsent(parent, (JCTree.JCCompilationUnit) trees.getPath(mm.getParentElement()).getCompilationUnit());
        }

        Map<MethodModel, JCTree.JCMethodDecl> generatedMethods = new HashMap<>();

        // generate dispatching code for each parent class
        for (JCTree.JCClassDecl parent : splitMethodModels.keySet()) {
            Map<MethodModel, Set<MethodInstance>> partialMap = splitMethodModels.get(parent);
            CodeGeneratorReflection generator = new CodeGeneratorReflection(tm, this.elements, types, symtab, partialMap);

            // generate the init method, the map where method references will be stored and a dispatcher for each methodModel
            JCTree.JCMethodDecl initMethod = generator.generateInitMethod();
            JCTree.JCVariableDecl methodMap = generator.generateMethodMapDecl();
            Map<MethodModel, JCTree.JCMethodDecl> dispatchers = generator.generateDispatchers();
            generatedMethods.putAll(dispatchers);

            // add the statements to class and fill the symtables
            Utils.addNewField(parent, methodMap, symtab, msg);
            Utils.addNewMethod(parent, initMethod, symtab, msg);
            dispatchers.values().forEach(dispatcher -> Utils.addNewMethod(parent, dispatcher, symtab, msg));

            // import java.util.*;
            // import java.lang.reflect.*;
            Utils.addImports(elements.get(parent), Arrays.asList(
                    tm.Import(tm.Select(tm.Select(tm.Ident(this.elements.getName("java")), this.elements.getName("util")), this.elements.getName("*")), false),
                    tm.Import(tm.Select(tm.Select(tm.Select(tm.Ident(this.elements.getName("java")), this.elements.getName("lang")), this.elements.getName("reflect")), this.elements.getName("*")), false)
            ));

            msg.printMessage(Diagnostic.Kind.NOTE, "Inserted new code into " + parent.name.toString());
        }

        // replace all calls with calls to the dispatcher
        replaceMethodCalls(roundEnv, generatedMethods);

        return true;
    }


    /*
     * Replace calls to the orignal methods with the calls to generated dispatchers
     * */
    private void replaceMethodCalls(RoundEnvironment roundEnv, Map<MethodModel, JCTree.JCMethodDecl> generatedMethods) {
        for (MethodModel mm : originalMethods.keySet()) {
            for (Element e : roundEnv.getElementsAnnotatedWith(MultiDispatchClass.class)) {
                JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) trees.getPath(e).getLeaf();
                super.replaceMethodsInClass(mm, generatedMethods.get(mm), classDecl);
            }
        }
    }

}
