package si.kisek.annotationdispatch;


import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import si.kisek.annotationdispatch.models.AcceptMethod;
import si.kisek.annotationdispatch.models.MethodInstance;
import si.kisek.annotationdispatch.models.MethodModel;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.annotation.processing.Filer;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static si.kisek.annotationdispatch.utils.Utils.javacList;


/*
 * Processor that implements multiple dispatch with an extended Visitor pattern
 * */
@SupportedAnnotationTypes({
        "si.kisek.annotationdispatch.ExampleAnnotation",
        "si.kisek.annotationdispatch.MultiDispatch",
        "si.kisek.annotationdispatch.MultiDispatchClass",
        "si.kisek.annotationdispatch.MultiDispatchVisitable",
        "si.kisek.annotationdispatch.MultiDispatchVisitableBase"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ProcessorVisitor extends MultidispatchProcessor {

    private boolean pass1Complete = false;
    private boolean pass2Complete = false;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (!pass1Complete) {
            pass1Complete = true;

            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Round 1");

            if (originalMethods.size() <= 0) {
                originalMethods = super.processAnnotatedMethods(roundEnv);
            }

            if (originalMethods.size() == 0) {
                // still no annotated methods, skip
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "No dispatchable methods found, exiting");
                return true;
            }
            generateAcceptMethods(roundEnv);
            PackageElement pck = (PackageElement) elements.getPackageOf(roundEnv.getElementsAnnotatedWith(MultiDispatchClass.class).iterator().next());
            generateVisitableBase(roundEnv, pck);
            modifyVisitableClasses(roundEnv);

            super.addNewMethods();

        } else if (!pass2Complete) {
            pass2Complete = true;

            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Round 2");
        }
        return true;

    }

    private void generateVisitableBase(RoundEnvironment roundEnv, PackageElement pck) {
        // TODO
        for (MethodModel mm : originalMethods.keySet()) {
            try {
                String filename = pck.getQualifiedName() + mm.getVisitableName();

                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Writing new file: " + filename);

                JavaFileObject jfo = processingEnv.getFiler().createSourceFile(filename, pck);
                BufferedWriter bw = new BufferedWriter(jfo.openWriter());
                bw.append("package ")
                        .append(String.valueOf(pck.getQualifiedName()))
                        .append(";");
                bw.newLine();
                bw.append("import si.kisek.annotationdispatch.MultiDispatchVisitableBase;");
                bw.newLine();
                bw.newLine();
                bw.append("@MultiDispatchVisitableBase");
                bw.newLine();
                bw.append("class Visitable")
                        .append(mm.getRandomness())
                        .append(" {");
                bw.newLine();
                bw.append("public static void someMethod(String arg1) { System.out.println(\"Method from generated class called: \" + arg1); }");
                bw.newLine();
                bw.append("}");
                bw.newLine();

                bw.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void modifyVisitableClasses(RoundEnvironment roundEnv) {
        // TODO: this is just a test
        for (MethodModel mm : originalMethods.keySet()) {
            for (Element e : roundEnv.getElementsAnnotatedWith(MultiDispatch.class)) {
                Tree tree = trees.getPath(e).getLeaf();
                if (tree instanceof JCTree.JCMethodDecl) {
                    JCTree.JCMethodDecl changedMethod = ((JCTree.JCMethodDecl) tree);
                    changedMethod.body.stats = changedMethod.body.stats.prepend(
                            tm.Exec(tm.Apply(
                                    javacList(new JCTree.JCExpression[0]),
                                    tm.Select(tm.Ident(elements.getName(mm.getVisitableName())), elements.getName("someMethod")),
                                    javacList(Collections.singletonList(tm.Literal("Literal String Parameter")))
                            ))
                    );
                }
            }
        }
    }


    private void generateAcceptMethods(RoundEnvironment roundEnv) {
        // fill the Visitable class with default methods
        for (MethodModel mm : originalMethods.keySet()) {
            Map<String, Type> possibleTypes = new HashMap<>();
            for (MethodInstance mi : originalMethods.get(mm)) {
                for (Type param : mi.getParameters()) {
                    possibleTypes.put(param.tsym.name.toString(), param);
                }
            }

            // generate a Map of all possible accept method
            // acceptMethods maps a type to a set of methods where it is relevant (it will be resolved by them)
            // Visitable class will implement all the methods with default body
            // each visitable class will implement the relevant methods
            HashMap<Type, Set<AcceptMethod>> acceptMethods = new HashMap<>();
            for (MethodInstance mi : originalMethods.get(mm)) {

                for (int i = 0; i < mm.getNumParameters(); i++) {
                    String name = mm.getRandomness() + "_accept" + (i + 1);

                    JCTree.JCMethodDecl method = tm.MethodDef(
                            tm.Modifiers(0), // no modifiers
                            elements.getName(name),
                            mm.getReturnValue(),  // return type from original method
                            com.sun.tools.javac.util.List.from(new JCTree.JCTypeParameter[0]),  // no type parameters (TODO: check this)
                            com.sun.tools.javac.util.List.from(new JCTree.JCVariableDecl[0]),  // arguments are added below
                            com.sun.tools.javac.util.List.from(new JCTree.JCExpression[0]),  // no exception throwing
                            null,  // body is added below
                            null  // no default value
                    );

                    List<JCTree.JCVariableDecl> defined = new ArrayList<>();
                    List<JCTree.JCVariableDecl> undefined = new ArrayList<>();
                    for (int d = 0; d < i; d++) {
                        defined.add(tm.Param(
                                elements.getName("d" + d),
                                new Type.ClassType(
                                        new Type.JCNoType(),
                                        com.sun.tools.javac.util.List.from(new Type[0]),
                                        elements.getTypeElement("java.lang.Object")
                                ),
                                method.sym
                        ));
                    }

                    for (int o = i + 1; o < mm.getNumParameters(); o++) {
                        undefined.add(tm.Param(
                                elements.getName("o" + o),
                                mi.getParameters().get(o),
                                method.sym
                        ));
                    }

                    // add the new method to the appropriate Set inside the Map
                    Type currType = mi.getParameters().get(i);
                    acceptMethods.putIfAbsent(currType, new HashSet<>());
                    acceptMethods.get(currType).add(new AcceptMethod(name, mm, method.sym, i, defined, undefined));
                }
            }

            for (Element e : roundEnv.getElementsAnnotatedWith(MultiDispatchVisitable.class)) {
                if (possibleTypes.containsKey(e.asType().toString())) {
                    // this Element can be a parameter in MM, generate accept methods for the visitor
                    for (Type t : acceptMethods.keySet()) {
                        if (!t.toString().equals(e.asType().toString())) {
                            continue;
                        }

                        for (AcceptMethod am : acceptMethods.get(t)) {
                            JCTree.JCMethodDecl method = tm.MethodDef(
                                    tm.Modifiers(0), // no modifiers
                                    elements.getName(am.getName()),
                                    mm.getReturnValue(),  // return type from original method
                                    com.sun.tools.javac.util.List.from(new JCTree.JCTypeParameter[0]),  // no type parameters (TODO: check this)
                                    com.sun.tools.javac.util.List.from(new JCTree.JCVariableDecl[0]),  // arguments are added below
                                    com.sun.tools.javac.util.List.from(new JCTree.JCExpression[0]),  // no exception throwing
                                    null,  // body is added below
                                    null  // no default value
                            );

                            //TODO: test if Exceptions are faster than return value wrapper
                        }
                        //TODO: inject the new methods in Element e

                    }
                }
            }
        }

    }
}
