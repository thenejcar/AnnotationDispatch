package si.kisek.annotationdispatch.models;

import com.sun.tools.javac.tree.JCTree;

import java.util.Map;
import java.util.Set;

/*
* Class representing all the methods that need to be generated inside one class (parentClass)
* needed for the Reflection method, since there we generate 3 Decls per class, not 1 per MethodModel
* */
public class ReflectionModelGroup {

    private JCTree.JCClassDecl parentClass;
    private Map<MethodModel, Set<MethodInstance>> originalMethods;
    private JCTree.JCMethodDecl initMethod;
    private JCTree.JCVariableDecl methodMapDecl;
    private JCTree.JCMethodDecl dispatcher;

    public ReflectionModelGroup(Map<MethodModel, Set<MethodInstance>> originalMethods, JCTree.JCClassDecl parentClass, JCTree.JCMethodDecl initMethod, JCTree.JCVariableDecl methodMapDecl, JCTree.JCMethodDecl dispatcher) {
        this.originalMethods = originalMethods;
        this.parentClass = parentClass;
        this.initMethod = initMethod;
        this.methodMapDecl = methodMapDecl;
        this.dispatcher = dispatcher;
    }

    public Map<MethodModel, Set<MethodInstance>> getOriginalMethods() {
        return originalMethods;
    }

    public void setOriginalMethods(Map<MethodModel, Set<MethodInstance>> originalMethods) {
        this.originalMethods = originalMethods;
    }

    public JCTree.JCClassDecl getParentClass() {
        return parentClass;
    }

    public void setParentClass(JCTree.JCClassDecl parentClass) {
        this.parentClass = parentClass;
    }

    public JCTree.JCMethodDecl getInitMethod() {
        return initMethod;
    }

    public void setInitMethod(JCTree.JCMethodDecl initMethod) {
        this.initMethod = initMethod;
    }

    public JCTree.JCVariableDecl getMethodMapDecl() {
        return methodMapDecl;
    }

    public void setMethodMapDecl(JCTree.JCVariableDecl methodMapDecl) {
        this.methodMapDecl = methodMapDecl;
    }

    public JCTree.JCMethodDecl getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(JCTree.JCMethodDecl dispatcher) {
        this.dispatcher = dispatcher;
    }
}
