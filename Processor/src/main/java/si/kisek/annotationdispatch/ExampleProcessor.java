package si.kisek.annotationdispatch;


import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Name;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

@SupportedAnnotationTypes({"si.kisek.annotationdispatch.ExampleAnnotation", "si.kisek.annotationdispatch.MultiDispatch_Demo", "si.kisek.annotationdispatch.MultiDispatchClass_Demo"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ExampleProcessor extends AbstractProcessor {

    private Trees trees;  // compiler's AST
    private TreeMaker tm;  // used to add subtrees to compiler's AST
    private JavacElements elements;  // utility methods for op[erating with program elements
    private Symtab symtab;  // javac's sym table

    private Map<Name, List<MethodInstance>> methods = new HashMap<>();
    private Map<Name, JCTree.JCMethodDecl> generatedMethods = new HashMap<>();


    //get the compiler trees
    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        trees = Trees.instance(processingEnv);

        if (processingEnv instanceof JavacProcessingEnvironment) {
            tm = TreeMaker.instance(((JavacProcessingEnvironment) processingEnv).getContext());
            elements = (JavacElements) processingEnv.getElementUtils();
            symtab = Symtab.instance(((JavacProcessingEnvironment) processingEnv).getContext());
        } else {
            System.out.println("You are not using a javac processing environment, throwing an exception");
            throw new RuntimeException("Annotation needs to be processed using javac");
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        System.out.println("Processor started");

        // processing for the ExampleAnnotation
        for (Element e : roundEnv.getElementsAnnotatedWith(ExampleAnnotation.class)) {
            System.out.println("processing element: " + e);
            System.out.print("Annotation content: ");
            String annotationContent = ((ExampleAnnotation) e.getAnnotation(ExampleAnnotation.class)).changeTo();
            System.out.println(annotationContent);
            // example is used on a String and changes the initialization value to the value in the annotation
            Tree tree = trees.getPath(e).getLeaf();

            if (tree instanceof JCTree.JCVariableDecl) {
                // TODO: reliably determine the type (probably something inside varDecl.varType .type or .sym)

                JCTree.JCVariableDecl varDecl = (JCTree.JCVariableDecl) tree;
                System.out.println("Overwriting the String declaration \"" + ((JCTree.JCLiteral) varDecl.init).getValue() + "\" with \"" + annotationContent + "\"");
                varDecl.init = tm.Literal(annotationContent);

            } else {
                System.out.println("Something went wrong / annotated value is not a String declaration");
            }
        }

        if (methods.size() > 0) {
            // already processed, skip
            return false;
        }

        // first pass
        methods = processAnnotatedMethods(roundEnv);
        if (methods.size() == 0) {
            // no annotated methods, skip
            return true;
        }


        generateNewMethods(roundEnv);

        // second pass
        replaceMethods(roundEnv);


        return true;
    }

    /*
     * get all methods annotated with multidispatch annotation into a hashmap
     * */
    private Map<Name, List<MethodInstance>> processAnnotatedMethods(RoundEnvironment roundEnv) {
        Map<Name, List<MethodInstance>> map = new HashMap<Name, List<MethodInstance>>();

        for (Element e : roundEnv.getElementsAnnotatedWith(MultiDispatch_Demo.class)) {
            Tree tree = trees.getPath(e).getLeaf();
            if (tree instanceof JCTree.JCMethodDecl) {
                JCTree.JCMethodDecl declaration = (JCTree.JCMethodDecl) tree;

                Name name = declaration.getName();

                // get parameter types in a list
                List<Type> parameterTypes = new ArrayList<>();
                for (JCTree.JCVariableDecl parameterDecl : declaration.params) {
                    parameterTypes.add(parameterDecl.vartype.type);
                }

                // get parent class
                TreePath parent = trees.getPath(e);
                while (!(parent.getLeaf() instanceof JCTree.JCClassDecl) && parent.getParentPath() != null) {
                    parent = parent.getParentPath(); // move up until you hit a class declaration or null
                }

                map.putIfAbsent(name, new ArrayList<>());
                map.get(name).add(new MethodInstance(name, parameterTypes, declaration.restype, declaration.mods, (JCTree.JCClassDecl) parent.getLeaf()));

            } else {
                throw new RuntimeException("@MultiDispatch_Demo annotations should be on method declarations only");
            }
        }
        return map;
    }

    /*
     * generate new methods that implement multiple dispatch
     * */
    private void generateNewMethods(RoundEnvironment roundEnv) {
        if (methods == null) {
            throw new RuntimeException("Cannot start the second pass if methods map is null");
        }

        generatedMethods = new HashMap<>();

        // generate the switcher (in this example it's just hardcoded)
        for (Name methodName : methods.keySet()) {
            List<MethodInstance> instances = methods.get(methodName);
            // TODO: check if methods with same name have the same return types, modifiers and parent class
            JCTree.JCClassDecl classDecl = instances.get(0).getParentClass();
            String random = UUID.randomUUID().toString().substring(0,8);
            Name generatedName = elements.getName(methodName.toString() + "_" + random);  // generate a random name

            JCTree.JCMethodDecl method = tm.MethodDef(
                    instances.get(0).getModifiers(),            // modifiers from original method
                    generatedName,
                    instances.get(0).getReturnValue(),          // return type from original method
                    javacList(new JCTree.JCTypeParameter[0]), // no type parameters
                    javacList(new JCTree.JCVariableDecl[0]),  // arguments are added below
                    javacList(new JCTree.JCExpression[0]),    // no exception throwing
                    null,                               // body is added below
                    null                           // no default value
            );

            // create the one parameter and add it to the method
            Name arg1 = elements.getName("arg1");
            Type arg1Type = new Type.ClassType(
                    new Type.JCNoType(),
                    com.sun.tools.javac.util.List.from(new Type[0]),
                    elements.getTypeElement("java.lang.Object")
            );
            JCTree.JCVariableDecl parameter1 = tm.Param(arg1, arg1Type, method.sym);
            method.params = javacList(Collections.singletonList(parameter1));


            // TODO: sort the possible classes in correct order and store them in a nice structure (tree?)
            // hardcoded for now
            MethodInstance methodNekej = null;
            MethodInstance methodNekajDrugega = null;
            for (MethodInstance mi : instances) {
                if (mi.getParameters().get(0).tsym.name.toString().equals("Nekej")) {
                    methodNekej = mi;
                } else {
                    methodNekajDrugega = mi;
                }
            }

            if (methodNekej == null || methodNekajDrugega == null) {
                throw new RuntimeException("Some methods were not found, please keep the example as it is (method parameter types are hardcoded in the processor)");
            }

            // create the body of the method
            List<JCTree.JCStatement> statements = new ArrayList<>();
            statements.add(
                    tm.If(
                            // if (arg1 instanceof Nekej)
                            tm.Parens(tm.TypeTest(tm.Ident(arg1), tm.Ident(elements.getName("Nekej")))),
                            // then
                            tm.Block(0,
                                    javacList(Collections.singletonList(tm.If(
                                            // if (arg1 instanceof NekajDrugega)
                                            tm.Parens(tm.TypeTest(tm.Ident(arg1),
                                                    tm.Ident(elements.getName("NekajDrugega")))),
                                            // then return methodNekajDrugega((NekajDrugega) arg1)
                                            tm.Return(tm.Apply(
                                                    javacList(new JCTree.JCExpression[0]),
                                                    tm.Ident(methodNekajDrugega.getName()),
                                                    javacList(Collections.singletonList(
                                                            tm.TypeCast(
                                                                    methodNekajDrugega.getParameters().get(0).tsym.type,
                                                                    tm.Ident(arg1)
                                                            ))
                                                    ))),
                                            // else return methodNekej((Nekej) arg1)
                                            tm.Return(tm.Apply(
                                                    javacList(new JCTree.JCExpression[0]),
                                                    tm.Ident(methodNekej.getName()),
                                                    javacList(Collections.singletonList(
                                                            tm.TypeCast(
                                                                    methodNekej.getParameters().get(0).tsym.type,
                                                                    tm.Ident(arg1)
                                                            ))
                                                    )))
                                            )
                                    ))
                            ),
                            // else
                            null
                    )
            );
            statements.add(
                    // throw new RuntimeException("No method definition for runtime argument of type " + arg1.getClass())
                    tm.Throw(tm.NewClass(
                            null,
                            javacList(new JCTree.JCExpression[0]),
                            tm.Ident(elements.getName("java.lang.RuntimeException")),
                            javacList(Collections.singletonList(tm.Binary(
                                    JCTree.Tag.PLUS,
                                    tm.Literal("No method definition for runtime argument of type "),
                                    tm.Apply(
                                            javacList(new JCTree.JCExpression[0]),
                                            tm.Select(tm.Ident(arg1), elements.getName("getClass")),
                                            javacList(new JCTree.JCExpression[0])
                                    )
                            ))),
                            null
                            )
                    )
            );

            method.body = tm.Block(0, javacList(statements));

            generatedMethods.put(methodName, method);

            classDecl.defs.append(method);  // add the method to the class

            List<Type> paramTypes = new ArrayList<>();
            for (JCTree.JCVariableDecl varDecl : method.params) {
                paramTypes.add(varDecl.type);
            }
            Symbol.MethodSymbol methodSymbol = new Symbol.MethodSymbol(
                    method.mods.flags,
                    methodName,
                    new Type.MethodType(javacList(paramTypes), method.getReturnType().type, javacList(new Type[0]), symtab.methodClass),
                    classDecl.sym
            );

            // use reflection to add the generated method symbol to the parent class

            try {
                Field field = Symbol.ClassSymbol.class.getField("members_field");
                Method enterSym = field.getType().getMethod("enter", Symbol.class);

                Scope scope = (Scope) field.get(classDecl.sym); // get the class members field
                enterSym.invoke(scope, methodSymbol);  // enter the symbol
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Unable to inject method symbol into the class, you are probably using the wrong compiler version");
            }

        }
    }

    /*
     * replace calls to original methods with the generated ones
     * */
    private void replaceMethods(RoundEnvironment roundEnv) {
        //TODO: check if we really need the class annotation, maybe we can find all places where annotated methods were called?


        // iterate over the whole class and replace method calls with the new method
        for (Name methodName : methods.keySet()) {
            for (Element e : roundEnv.getElementsAnnotatedWith(MultiDispatchClass_Demo.class)) {
                // TODO: in the real implementation we'll probably have a visitor of some sort

                JCTree.JCClassDecl classTree = (JCTree.JCClassDecl) trees.getPath(e).getLeaf();
                for (JCTree subtree : classTree.defs) {
                    // only the methods are supported for now
                    if (subtree instanceof JCTree.JCMethodDecl) {
                        for (JCTree.JCStatement statement : ((JCTree.JCMethodDecl) subtree).body.stats) {
                            // the visior would visit everything, this code supports just the simple call from main method
                            if (statement instanceof JCTree.JCExpressionStatement && ((JCTree.JCExpressionStatement) statement).getExpression() instanceof JCTree.JCMethodInvocation) {
                                JCTree.JCMethodInvocation invocation = (JCTree.JCMethodInvocation) ((JCTree.JCExpressionStatement) statement).getExpression();
                                if (invocation.meth instanceof JCTree.JCIdent && ((JCTree.JCIdent) invocation.meth).name.equals(methodName)) {
                                    //replace with the generated method
                                    ((JCTree.JCIdent) invocation.meth).name = generatedMethods.get(methodName).name;
                                }
                            }
                        }
                    }
                }

            }
        }
    }


    // da niso predolge vrstice
    private static <T> com.sun.tools.javac.util.List<T> javacList(Iterable<T> normalIterable) {
        return com.sun.tools.javac.util.List.from(normalIterable);
    }

    private static <T> com.sun.tools.javac.util.List<T> javacList(T[] array) {
        return com.sun.tools.javac.util.List.from(array);
    }
}
