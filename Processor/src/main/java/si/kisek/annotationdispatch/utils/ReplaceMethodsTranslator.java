package si.kisek.annotationdispatch.utils;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Name;
import si.kisek.annotationdispatch.models.MethodInstance;


/*
* The translator uses the inbuilt mechanisms to find and replace all the original methods with the new, generated ones
* it simply looks for JCMethodInvocation statements and replaces the method name inside
* */
public class ReplaceMethodsTranslator extends TreeTranslator {

    private MethodInstance targetMethod;
    private Name replacementName;
    public int counter;

    public ReplaceMethodsTranslator(MethodInstance targetMethod, Name replacementName) {
        this.targetMethod = targetMethod;
        this.replacementName = replacementName;

        this.counter = 0;
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation jcMethodInvocation) {
        // check if method invocation is invoking the target method (name and num. of parameters matches)


        if (jcMethodInvocation.meth instanceof JCTree.JCIdent) {
            // direct usages
            JCTree.JCIdent methodIdent = (JCTree.JCIdent) jcMethodInvocation.meth;

            if (methodIdent.name.equals(targetMethod.getMm().getName()) && jcMethodInvocation.args.size() == targetMethod.getParameters().size()) {
                methodIdent.name = replacementName; // replace the name with the generated method
                counter++;
            }
        } else if (jcMethodInvocation.meth instanceof JCTree.JCFieldAccess) {
            // non-static usages via this.XXXX or static usages via ClassName.XXXX
            JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) jcMethodInvocation.meth;

            if (fieldAccess.name.equals(targetMethod.getMm().getName()) && jcMethodInvocation.args.size() == targetMethod.getParameters().size()) {
                fieldAccess.name = replacementName; // replace the name with the generated method
                counter++;
            }
        }

        super.visitApply(jcMethodInvocation);
    }


}
