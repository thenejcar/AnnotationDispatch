package si.kisek.annotationdispatch;


import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import si.kisek.annotationdispatch.models.AcceptMethod;
import si.kisek.annotationdispatch.models.MethodInstance;
import si.kisek.annotationdispatch.models.MethodModel;
import si.kisek.annotationdispatch.utils.CodeGeneratorVisitor;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

import static si.kisek.annotationdispatch.utils.Utils.emptyExpr;
import static si.kisek.annotationdispatch.utils.Utils.javacList;


/*
 * Processor that implements multiple dispatch with an extended Visitor pattern
 * */
@SupportedAnnotationTypes({
        "si.kisek.annotationdispatch.MultiDispatch",
        "si.kisek.annotationdispatch.MultiDispatchClass",
        "si.kisek.annotationdispatch.MultiDispatchVisitable"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ProcessorVisitor extends MultidispatchProcessor {

    private HashMap<MethodModel, HashMap<Type, Set<AcceptMethod>>> acceptMethods;
    private Set<Type> rootTypes = new HashSet<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (roundEnv.processingOver()) {
            msg.printMessage(Diagnostic.Kind.NOTE, "Processing done");
            return true;
        }

        msg.printMessage(Diagnostic.Kind.NOTE, "Processing round started");

        if (originalMethods.size() <= 0) {
            originalMethods = super.processAnnotatedMethods(roundEnv);
        }

        if (originalMethods.size() == 0) {
            // still no annotated methods, skip
            msg.printMessage(Diagnostic.Kind.ERROR, "No dispatchable methods found, exiting");
            return true;
        }

        // create the descriptions of accept methods that will be needed
        this.acceptMethods = generateAcceptMethods();

        for (MethodModel mm : super.originalMethods.keySet()) {

            CodeGeneratorVisitor generator = new CodeGeneratorVisitor(super.tm, super.elements, super.types, super.symtab, mm, this.acceptMethods.get(mm), this.originalMethods.get(mm));

            // generate the empty Visitor & Visitable classes
            generator.generateVisitableAndVisitor();

            // generate the visitor init method and replace the original methods calls with it
            JCTree.JCMethodDecl initMethod = generator.createVisitorInitMethod();
            for (Element e : roundEnv.getElementsAnnotatedWith(MultiDispatchClass.class)) {
                JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) trees.getPath(e).getLeaf();
                if (classDecl == mm.getParentClass()) {
                    super.replaceMethodsInClass(mm, initMethod, classDecl);
                }
            }

            // complete the visitor class and visitable interface
            generator.fillVisitableAndVisitor();
            msg.printMessage(Diagnostic.Kind.NOTE,"Generated visitable interface '" + generator.getVisitableInterfaceName() + "'.");

            // modify visitable classes (extend the interface, inplement the methods)
            for (Element e : roundEnv.getElementsAnnotatedWith(MultiDispatchVisitable.class)) {
                JCTree.JCClassDecl classDecl = ((JCTree.JCClassDecl) trees.getPath(e).getLeaf());
                // TODO: detect and complain when some accept types are missing the annotation
                // implement the Visitable interface (add the accept methods)
                generator.modifyVisitableClass(classDecl, rootTypes.contains(classDecl.sym.type), (JCTree.JCCompilationUnit)trees.getPath(e).getCompilationUnit());
                msg.printMessage(Diagnostic.Kind.NOTE, "Visitable class '" + e.getSimpleName() + "' filled with accept methods.");
            }
        }

        return true;
    }

    /*
    * Find all the accept methods that are needed in the multi visitor and remember which type they can resolve
    * Results are in 2-level hashmap, first maps by MM, the second maps by the type that a set of methods can resolve
    * Also, remember the root types (the ones without a superclass)
    * */
    private HashMap<MethodModel, HashMap<Type, Set<AcceptMethod>>> generateAcceptMethods() {

        HashMap<MethodModel, HashMap<Type, Set<AcceptMethod>>> result = new HashMap<>();

        // find all combinations of parameters that will be used in the accept methods
        for (MethodModel mm : originalMethods.keySet()) {
            Set<Type> possibleTypes = new HashSet<>();
            for (MethodInstance mi : originalMethods.get(mm)) {
                possibleTypes.addAll(mi.getParameters());
            }

            HashMap<Type, Set<AcceptMethod>> acceptMethods = new HashMap<>();

            // find roots - types with no visitable superclass
            for (Type t : possibleTypes) {
                boolean isRoot = true;
                for (Type candidate : possibleTypes) {
                    if (!t.equals(candidate) && super.types.isSubtype(t, candidate)) {
                        isRoot = false;
                        break;
                    }
                }
                if (isRoot)
                    rootTypes.add(t); // store in the set

                acceptMethods.put(t, new HashSet<>()); // initialise the map
            }

            // generate a Map of all possible accept method that the Visitable interface will contain
            // acceptMethods maps a type to a set of methods where it is relevant (it will be resolved by them)
            // each visitable class will implement the relevant methods, the rest are misses (call super or throw error)
            for (MethodInstance mi : originalMethods.get(mm)) {

                // for each method instance, generate as many methods as it has parameters
                // each method has i defined parameters and i - mm.getNumParameters() undefined parameters
                for (int i = 0; i < mm.getNumParameters(); i++) {
                    String name = "accept" + (i + 1);

                    JCTree.JCMethodDecl method = tm.MethodDef(
                            tm.Modifiers(0), // no modifiers
                            elements.getName(name),
                            mm.getReturnValue(),  // return type from original method
                            javacList(new JCTree.JCTypeParameter[0]),  // no type parameters
                            javacList(new JCTree.JCVariableDecl[0]),  // arguments are added below
                            emptyExpr(),  // no exception throwing
                            null,  // body is added below
                            null  // no default value
                    );

                    List<JCTree.JCVariableDecl> defined = new ArrayList<>();
                    for (int d = 0; d < i; d++) {

                        defined.add(tm.Param(
                                elements.getName("d" + d),
                                mi.getParameters().get(d),
                                method.sym
                        ));
                    }

                    List<JCTree.JCVariableDecl> undefined = new ArrayList<>();
                    for (int o = i + 1; o < mm.getNumParameters(); o++) {
                        undefined.add(tm.Param(
                                elements.getName("o" + o),
                                new Type.ClassType(
                                        new Type.JCNoType(),
                                        javacList(new Type[0]),
                                        elements.getTypeElement("java.lang.Object")  // Object is temporary, real type will be filled in the code generator
                                ),
                                method.sym
                        ));
                    }

                    // add the new method to the appropriate Set inside the Map
                    Type currType = mi.getParameters().get(i);
                    acceptMethods.get(currType).add(new AcceptMethod(name, mm, method.sym, i, defined, undefined));
                }
            }

            result.put(mm, acceptMethods);

        }
        return result;
    }


}
