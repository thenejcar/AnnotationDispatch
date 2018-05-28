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
import si.kisek.annotationdispatch.utils.ReplaceMethodsVisitor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import java.util.*;


@SupportedAnnotationTypes({
        "si.kisek.annotationdispatch.ExampleAnnotation",
        "si.kisek.annotationdispatch.MultiDispatch",
        "si.kisek.annotationdispatch.MultiDispatchClass",
        "si.kisek.annotationdispatch.MultiDispatchVisitable",
        "si.kisek.annotationdispatch.MultiDispatchVisitableBase"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public abstract class MultidispatchProcessor extends AbstractProcessor {

    Trees trees;  // compiler's AST
    TreeMaker tm;  // used to add subtrees to compiler's AST
    JavacElements elements;  // utility methods for operating with program elements
    JavacTypes types;  // utility methods for operating with types
    Symtab symtab;  // javac's sym table

    Map<MethodModel, Set<MethodInstance>> originalMethods = new HashMap<>();

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

    /*
     * get all methods annotated with multidispatch annotation into a hashmap
     * */
    protected Map<MethodModel, Set<MethodInstance>> processAnnotatedMethods(RoundEnvironment roundEnv) {
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

                MethodModel model = new MethodModel(name, parameterTypes.size(), declaration.restype, declaration.mods,
                        (JCTree.JCClassDecl) parent.getLeaf(), e, elements.getPackageOf(e).getQualifiedName().toString());
                map.putIfAbsent(model, new HashSet<>());
                map.get(model).add(new MethodInstance(model, parameterTypes));

            } else {
                throw new RuntimeException("@MultiDispatch_Demo annotations should be on method declarations only");
            }
        }
        return map;
    }

    public void replaceMethodsInClass(MethodModel toReplace, JCTree.JCMethodDecl newMethod, JCTree.JCClassDecl targetClass) {

        for (MethodInstance oldMethod : originalMethods.get(toReplace)) {
            ReplaceMethodsVisitor visitor = new ReplaceMethodsVisitor(oldMethod, newMethod.getName());
            visitor.visitClassDef(targetClass);
        }

        System.out.println("Calls to " + toReplace.getName() + " in " + targetClass.name + " replaced with calls to " + newMethod.getName());

    }
}
