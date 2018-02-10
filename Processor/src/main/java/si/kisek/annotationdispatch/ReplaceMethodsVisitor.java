package si.kisek.annotationdispatch;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Name;


public class ReplaceMethodsVisitor extends TreeTranslator {

    private MethodInstance targetMethod;
    private Name replacementName;

    public ReplaceMethodsVisitor(MethodInstance targetMethod, Name replacementName) {
        this.targetMethod = targetMethod;
        this.replacementName = replacementName;
    }


    @Override
    public void visitApply(JCTree.JCMethodInvocation jcMethodInvocation) {
        // check if method invocation is invoking the target method
        if (jcMethodInvocation.meth instanceof JCTree.JCIdent) {
            JCTree.JCIdent methodIdent = (JCTree.JCIdent) jcMethodInvocation.meth;

            if (methodIdent.name.equals(targetMethod.getName()) && jcMethodInvocation.args.size() == targetMethod.getParameters().size()) {
                methodIdent.name = replacementName; // replace the name with the generated method
            }
        }

        super.visitApply(jcMethodInvocation);
    }


}
