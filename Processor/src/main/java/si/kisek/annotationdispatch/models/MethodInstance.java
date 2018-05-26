package si.kisek.annotationdispatch.models;

import com.sun.tools.javac.code.Type;

import java.util.List;
import java.util.Objects;

public class MethodInstance extends MethodModel{

    private List<Type> parameters; // parameter types for this particular instance

    public MethodInstance(MethodModel model, List<Type> parameters) {
        // model defines method's name, num. of parameters, return type, modifiers and parent class
        super(model.getName(), model.getNumParameters(), model.getReturnValue(), model.getModifiers(), model.getParentClass(), model.getParentElement());
        this.parameters = parameters; // parameters for this particular instance
    }

    public List<Type> getParameters() {
        return parameters;
    }

    public void setParameters(List<Type> parameters) {
        this.parameters = parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodInstance)) return false;
        MethodInstance that = (MethodInstance) o;
        return numParameters == that.numParameters && // same as for methodModel
                Objects.equals(name, that.name) &&
                Objects.equals(returnValue.type, that.returnValue.type) &&
                Objects.equals(modifiers.flags, that.modifiers.flags) &&
                Objects.equals(parentClass, that.parentClass) &&
                Objects.equals(parameters, that.parameters);  // parameter types need to be the same
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, numParameters, returnValue.type, modifiers.flags, parentClass, parameters);
    }
}
