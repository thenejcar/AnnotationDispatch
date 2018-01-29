package si.kisek.annotationdispatch;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;

import java.util.List;

public class MethodInstance {

    private Name name;
    private List<Type> parameters;
    private JCTree.JCExpression returnValue;
    private JCTree.JCModifiers modifiers;
    private JCTree.JCClassDecl parentClass;

    public MethodInstance(Name name, List<Type> parameters, JCTree.JCExpression returnValue, JCTree.JCModifiers modifiers, JCTree.JCClassDecl parentClass) {
        this.name = name;
        this.parameters = parameters;
        this.returnValue = returnValue;
        this.modifiers = modifiers;
        this.parentClass = parentClass;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public List<Type> getParameters() {
        return parameters;
    }

    public JCTree.JCModifiers getModifiers() {
        return modifiers;
    }

    public JCTree.JCClassDecl getParentClass() {
        return parentClass;
    }

    public void setParameters(List<Type> parameters) {
        this.parameters = parameters;
    }

    public JCTree.JCExpression getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(JCTree.JCExpression returnValue) {
        this.returnValue = returnValue;
    }

    public void setModifiers(JCTree.JCModifiers modifiers) {
        this.modifiers = modifiers;
    }

    public void setParentClass(JCTree.JCClassDecl parentClass) {
        this.parentClass = parentClass;
    }
}
