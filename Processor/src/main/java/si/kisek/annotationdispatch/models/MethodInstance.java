package si.kisek.annotationdispatch.models;

import com.sun.tools.javac.code.Type;

import java.util.List;
import java.util.Objects;

/*
* Represents a method instance -- a specific annotated method. Defined by the method model and the types of parameters
* */
public class MethodInstance{

    private MethodModel mm;
    private List<Type> parameters; // parameter types for this particular instance

    public MethodInstance(MethodModel model, List<Type> parameters) {
        // model defines method's name, num. of parameters, return type, modifiers and parent class
        this.mm = model;
        this.parameters = parameters; // parameters for this particular instance
    }

    public List<Type> getParameters() {
        return parameters;
    }

    public MethodModel getMm() {
        return mm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodInstance)) return false;
        MethodInstance that = (MethodInstance) o;
        return Objects.equals(mm, that.mm) &&
                Objects.equals(parameters, that.parameters);  // parameter types need to be the same
    }

    @Override
    public int hashCode() {
        return Objects.hash(mm, parameters);
    }

    public int getNumParameters() {

        return parameters.size();
    }
}
