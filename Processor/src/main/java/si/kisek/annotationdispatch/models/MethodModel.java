package si.kisek.annotationdispatch.models;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Name;

import java.util.*;

/*
 * Model that defines a unique method
 * When using multiple dispatch, methods that belong to the same model, will be treated as the same method
 */
public class MethodModel {
    protected Name name;
    protected int numParameters;
    protected JCTree.JCExpression returnValue;
    protected JCTree.JCModifiers modifiers;
    protected JCTree.JCClassDecl parentClass;
    private String randomness;

    public MethodModel(Name name, int numParameters, JCTree.JCExpression returnValue, JCTree.JCModifiers modifiers, JCTree.JCClassDecl parentClass) {
        this.name = name;
        this.numParameters = numParameters;
        this.returnValue = returnValue;
        this.modifiers = modifiers;
        this.modifiers.annotations = com.sun.tools.javac.util.List.from(new JCTree.JCAnnotation[0]); // ignore annotations from the original method
        this.parentClass = parentClass;
        this.randomness = UUID.randomUUID().toString().replace("-", "");
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public int getNumParameters() {
        return numParameters;
    }

    public void setNumParameters(int numParameters) {
        this.numParameters = numParameters;
    }

    public JCTree.JCExpression getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(JCTree.JCExpression returnValue) {
        this.returnValue = returnValue;
    }

    public JCTree.JCModifiers getModifiers() {
        return modifiers;
    }

    public void setModifiers(JCTree.JCModifiers modifiers) {
        this.modifiers = modifiers;
    }

    public JCTree.JCClassDecl getParentClass() {
        return parentClass;
    }

    public void setParentClass(JCTree.JCClassDecl parentClass) {
        this.parentClass = parentClass;
    }

    public String getRandomness() {
        return randomness;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodModel)) return false;
        MethodModel that = (MethodModel) o;
        return numParameters == that.numParameters &&
                Objects.equals(name, that.name) &&
                Objects.equals(returnValue.type, that.returnValue.type) && // return value needs to be the same type
                Objects.equals(modifiers.flags, that.modifiers.flags) &&   // flags need to be the same (Annotations are ignored)
                Objects.equals(parentClass, that.parentClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, numParameters, returnValue.type, modifiers.flags, parentClass);
    }


    public JCTree.JCMethodDecl generateDispatchMethod(TreeMaker tm, JavacElements elements) {
        Name generatedName = elements.getName(name.toString() + "_" + randomness);  // generate a random name

        JCTree.JCMethodDecl method = tm.MethodDef(
                modifiers,            // modifiers from original method
                generatedName,
                returnValue,          // return type from original method
                com.sun.tools.javac.util.List.from(new JCTree.JCTypeParameter[0]), // no type parameters (TODO: check this)
                com.sun.tools.javac.util.List.from(new JCTree.JCVariableDecl[0]),  // arguments are added below
                com.sun.tools.javac.util.List.from(new JCTree.JCExpression[0]),    // no exception throwing
                null,                               // body is added below
                null                           // no default value
        );

        List<JCTree.JCVariableDecl> parameters = new ArrayList<>();
        for (int i=0; i<numParameters; i++) {
            parameters.add(tm.Param(
                    elements.getName("arg" + i),
                    new Type.ClassType(
                            new Type.JCNoType(),
                            com.sun.tools.javac.util.List.from(new Type[0]),
                            elements.getTypeElement("java.lang.Object")
                    ),
                    method.sym
            ));
        }

        // create the parameters and add them to the method
        method.params = com.sun.tools.javac.util.List.from(parameters);

        return method;
    }
}
