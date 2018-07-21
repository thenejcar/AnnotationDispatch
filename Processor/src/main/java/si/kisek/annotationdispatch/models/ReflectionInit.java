package si.kisek.annotationdispatch.models;

import com.sun.tools.javac.tree.JCTree;

public class ReflectionInit {

    private JCTree.JCMethodDecl initMethod;
    private JCTree.JCVariableDecl methodMap;
    private String radnomness;

    public ReflectionInit(JCTree.JCMethodDecl initMethod, JCTree.JCVariableDecl methodMap, String radnomness) {
        this.initMethod = initMethod;
        this.methodMap = methodMap;
        this.radnomness = radnomness;
    }

    public JCTree.JCMethodDecl getInitMethod() {
        return initMethod;
    }

    public void setInitMethod(JCTree.JCMethodDecl initMethod) {
        this.initMethod = initMethod;
    }

    public JCTree.JCVariableDecl getMethodMap() {
        return methodMap;
    }

    public void setMethodMap(JCTree.JCVariableDecl methodMap) {
        this.methodMap = methodMap;
    }

    public String getRadnomness() {
        return radnomness;
    }

    public void setRadnomness(String radnomness) {
        this.radnomness = radnomness;
    }
}
