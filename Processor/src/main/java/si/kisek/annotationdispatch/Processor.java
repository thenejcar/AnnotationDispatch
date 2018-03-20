package si.kisek.annotationdispatch;


import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Name;
import si.kisek.annotationdispatch.models.MethodInstance;
import si.kisek.annotationdispatch.models.MethodModel;
import si.kisek.annotationdispatch.models.MethodSwitcher;
import si.kisek.annotationdispatch.utils.CodeGenerator;
import si.kisek.annotationdispatch.utils.ReplaceMethodsVisitor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static si.kisek.annotationdispatch.utils.Utils.javacList;

@SupportedAnnotationTypes({"si.kisek.annotationdispatch.ExampleAnnotation", "si.kisek.annotationdispatch.MultiDispatch", "si.kisek.annotationdispatch.MultiDispatchClass"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class Processor extends AbstractProcessor {

    private Trees trees;  // compiler's AST
    private TreeMaker tm;  // used to add subtrees to compiler's AST
    private JavacElements elements;  // utility methods for operating with program elements
    private JavacTypes types;  // utility methods for operating with types
    private Symtab symtab;  // javac's sym table

    private Map<MethodModel, Set<MethodInstance>> originalMethods = new HashMap<>();
    private Map<MethodModel, JCTree.JCMethodDecl> generatedMethods = new HashMap<>();


    //get the compiler trees
    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        trees = Trees.instance(processingEnv);

        if (processingEnv instanceof JavacProcessingEnvironment) {
            tm = TreeMaker.instance(((JavacProcessingEnvironment) processingEnv).getContext());
            elements = (JavacElements) processingEnv.getElementUtils();
            types = (JavacTypes) processingEnv.getTypeUtils();
            symtab = Symtab.instance(((JavacProcessingEnvironment) processingEnv).getContext());
        } else {
            System.out.println("You are not using a javac processing environment, throwing an exception");
            throw new RuntimeException("Annotation needs to be processed using javac");
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (originalMethods.size() > 0) {
            // already processed, skip
            return false;
        }

        // first pass
        originalMethods = processAnnotatedMethods(roundEnv);
        if (originalMethods.size() == 0) {
            // no annotated methods, skip
            return true;
        }

        generateNewMethods(roundEnv);

        // replace method calls with the generated method
        replaceMethodCalls(roundEnv);

        addNewMethods();

        return true;
    }

    /*
     * get all methods annotated with multidispatch annotation into a hashmap
     * */
    private Map<MethodModel, Set<MethodInstance>> processAnnotatedMethods(RoundEnvironment roundEnv) {
        Map<MethodModel, Set<MethodInstance>> map = new HashMap<>();

        for (Element e : roundEnv.getElementsAnnotatedWith(MultiDispatch.class)) {
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

                MethodModel model = new MethodModel(name, parameterTypes.size(), declaration.restype, declaration.mods, (JCTree.JCClassDecl) parent.getLeaf());
                map.putIfAbsent(model, new HashSet<>());
                map.get(model).add(new MethodInstance(model, parameterTypes));

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
        if (originalMethods == null) {
            throw new RuntimeException("Cannot start the second pass if methods map is null");
        }

        generatedMethods = new HashMap<>();

        // generate the switcher (in this example it's just hardcoded)
        for (MethodModel model : originalMethods.keySet()) {

            JCTree.JCMethodDecl generatedMethod = model.generateDispatchMethod(tm, elements);
            Set<MethodInstance> instances = originalMethods.get(model);

            MethodSwitcher methodSwitcher = new MethodSwitcher(types, model, instances);

            CodeGenerator codeGenerator = new CodeGenerator(tm, elements, generatedMethod);
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

    /*
     * replace calls to original methods with the generated ones
     * */
    private void replaceMethodCalls(RoundEnvironment roundEnv) {
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
            Scope scope;
            try {
                Field field = Symbol.ClassSymbol.class.getField("members_field");
                Method enterSym = field.getType().getMethod("enter", Symbol.class);

                scope = (Scope) field.get(classDecl.sym); // get the class members field
                enterSym.invoke(scope, methodSymbol);  // enter the symbol
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Unable to inject method symbol into the class, you are probably using the wrong compiler version");
            }

            System.out.println("Method " + generatedMethod.name + " added to " + classDecl.name);
        }
    }
}
