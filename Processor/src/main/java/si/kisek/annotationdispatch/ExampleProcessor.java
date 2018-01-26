package si.kisek.annotationdispatch;


import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes("si.kisek.annotationdispatch.ExampleAnnotation")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ExampleProcessor extends AbstractProcessor {

    private Trees trees; // compiler's AST


    //get the compiler trees
    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        trees = Trees.instance(processingEnv);
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



        }

        return false;
    }
}
