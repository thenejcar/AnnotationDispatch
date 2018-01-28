package si.kisek.annotationdispatch;


import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.SharedNameTable;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes("si.kisek.annotationdispatch.ExampleAnnotation")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ExampleProcessor extends AbstractProcessor {

    private Trees trees; // compiler's AST
    private TreeMaker treeMaker;


    //get the compiler trees
    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        trees = Trees.instance(processingEnv);

        if (processingEnv instanceof JavacProcessingEnvironment) {
            treeMaker = TreeMaker.instance(((JavacProcessingEnvironment)processingEnv).getContext());
        } else {
            System.out.println("You are not using a javac processing environment, throwing an exception");
            throw new RuntimeException("Annotation needs to be processed using javac");
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        System.out.println("Processor started");
        for (Element e : roundEnv.getElementsAnnotatedWith(ExampleAnnotation.class)) {
            System.out.println(e);
            System.out.print("Annotation content: ");
            String annotationContent = ((ExampleAnnotation) e.getAnnotation(ExampleAnnotation.class)).changeTo();
            System.out.println(annotationContent);
/*
            JCTree.JCCompilationUnit jccUnit = (JCTree.JCCompilationUnit) trees.getPath(e).getCompilationUnit();
            // ce ima kaksne podelemente (ce je class), potem so v 'jccUnit.defs' in so tipa JCTree
*/

            //JCTree.JCVariableDecl decl = (JCTree.JCVariableDecl) trees.getPath(e).getCompilationUnit();
            //TODO
            // example is used on a String and changes the initialization value to the value in the annotation
            Tree tree = trees.getPath(e).getLeaf();

            if (tree instanceof JCTree.JCVariableDecl) {
                // TODO: reliably determine the type (probably something inside varDecl.varType .type or .sym)

                JCTree.JCVariableDecl varDecl = (JCTree.JCVariableDecl) tree;
                System.out.println("Overwriting the String declaration \"" + ((JCTree.JCLiteral)varDecl.init).getValue() + "\" with \"" + annotationContent + "\"");
                varDecl.init = treeMaker.Literal(annotationContent);

             } else {
                System.out.println("Something went wrong / annotated value is not a String declaration");
            }


        }

        return false;
    }
}
