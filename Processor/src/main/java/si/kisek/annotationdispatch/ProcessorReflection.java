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

        if (originalMethods.size() > 0) {
            // already processed, skip
            return false;
        }

        originalMethods = super.processAnnotatedMethods(roundEnv);
        if (originalMethods.size() == 0) {
            // no annotated methods, skip
            return true;
        }

        Map<JCTree.JCClassDecl, Map<MethodModel, Set<MethodInstance>>> splitMethodModels = new HashMap<>();
        for (MethodModel mm : originalMethods.keySet()) {
            JCTree.JCClassDecl parent = mm.getParentClass();
            Map<MethodModel, Set<MethodInstance>> partialMap = splitMethodModels.getOrDefault(parent, new HashMap<>());
            partialMap.put(mm, new HashSet<>(originalMethods.get(mm)));
            splitMethodModels.put(parent, partialMap);
        }

        Map<MethodModel, JCTree.JCMethodDecl> generatedMethods = new HashMap<>();

        // generate dispatching code for each parent class
        for (JCTree.JCClassDecl parent : splitMethodModels.keySet()) {
            Map<MethodModel, Set<MethodInstance>> partialMap = splitMethodModels.get(parent);
            CodeGeneratorReflection generator = new CodeGeneratorReflection(tm, elements, types, symtab, partialMap);

            JCTree.JCMethodDecl initMethod = generator.generateInitMethod();
            JCTree.JCVariableDecl methodMap = generator.generateMethodMapDecl();
            Map<MethodModel, JCTree.JCMethodDecl> dispatchers = generator.generateDispatchers();
            generatedMethods.putAll(dispatchers);

//            // add the init logic to the constructors
//            addInitLogicToClass(parent, initMethod);

            // fill the symtables
            addNewMethod(parent, initMethod);
            dispatchers.values().forEach(dispatcher -> addNewMethod(parent, dispatcher));
            addNewField(parent, methodMap);

            super.addImports(parent, Arrays.asList(
                    tm.Import(tm.Select(tm.Select(tm.Ident(elements.getName("java")), elements.getName("util")), elements.getName("*")), false),
                    tm.Import(tm.Select(tm.Select(tm.Select(tm.Ident(elements.getName("java")), elements.getName("lang")), elements.getName("reflect")), elements.getName("*")), false)
            ));
        }

        // replace all calls with calls to the dispatcher
        replaceMethodCalls(roundEnv, generatedMethods);

        return true;
    }


    /*
     * Replace calls to the orignal methods with the calls to generated dispatchers
     * */
    private void replaceMethodCalls(RoundEnvironment roundEnv, Map<MethodModel, JCTree.JCMethodDecl> generatedMethods) {
        for (MethodModel model : originalMethods.keySet()) {
            for (Element e : roundEnv.getElementsAnnotatedWith(MultiDispatchClass.class)) {
                super.replaceMethodsInClass(model, generatedMethods.get(model), (JCTree.JCClassDecl) trees.getPath(e).getLeaf());
            }
        }
    }

//    /*
//     * Add the var decl to class and call init method in all constructors
//     * */
//    private void addInitLogicToClass(JCTree.JCClassDecl parentClass, JCTree.JCMethodDecl initMethod) {
//        for (JCTree def : parentClass.defs) {
//            if (def instanceof JCTree.JCMethodDecl) {
//                JCTree.JCMethodDecl constructor = (JCTree.JCMethodDecl) def;
//                if ("<init>".equals(((JCTree.JCMethodDecl) def).getName().toString()))
//                    // add the method call to end of the constructor
//                    constructor.body.stats = constructor.body.stats.append(
//                            tm.Exec(tm.Apply(emptyExpr(), tm.Ident(initMethod.getName()), emptyExpr()))
//                    );
//            }
//        }
//    }


    /*
     * Inject generated method in the parent class of the original ones
     * */
    private void addNewMethod(JCTree.JCClassDecl classDecl, JCTree.JCMethodDecl generatedMethod) {

        classDecl.defs = classDecl.defs.append(generatedMethod);  // add the method to the class

        List<Type> paramTypes = new ArrayList<>();
        for (JCTree.JCVariableDecl varDecl : generatedMethod.params) {
            paramTypes.add(varDecl.type == null ? varDecl.vartype.type : varDecl.type);
        }

        Type returnType = generatedMethod.getReturnType() == null ? new Type.JCVoidType() : generatedMethod.getReturnType().type;

        Symbol.MethodSymbol methodSymbol = new Symbol.MethodSymbol(
                generatedMethod.mods.flags,
                generatedMethod.name,
                new Type.MethodType(javacList(paramTypes), returnType, javacList(new Type[0]), symtab.methodClass),
                classDecl.sym
        );

        // use reflection to add the generated method symbol to the parent class
        Utils.addSymbolToClass(classDecl, methodSymbol);

        System.out.println("Method " + generatedMethod.name + " added to " + classDecl.name);
    }

    private void addNewField(JCTree.JCClassDecl classDecl, JCTree.JCVariableDecl generatedVar) {
        classDecl.defs = classDecl.defs.append(generatedVar);  // add the method to the class

        Symbol.VarSymbol varSymbol = new Symbol.VarSymbol(
                generatedVar.mods.flags,
                generatedVar.name,
                new Type.TypeVar(generatedVar.name, generatedVar.sym, generatedVar.vartype.type),
                classDecl.sym
        );

        // use reflection to add the generated method symbol to the parent class
        Utils.addSymbolToClass(classDecl, varSymbol);

        System.out.println("Variable " + generatedVar.name + " added to " + classDecl.name);
    }

}
