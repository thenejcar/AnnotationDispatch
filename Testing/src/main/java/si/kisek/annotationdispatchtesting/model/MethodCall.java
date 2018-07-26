package si.kisek.annotationdispatchtesting.model;

import javax.annotation.Generated;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MethodCall {
    private MethodInstance method;
    private List<GeneratedClass> types;

    public MethodCall(MethodInstance method, List<GeneratedClass> types) {
        this.method = method;
        this.types = types;
    }

    public MethodInstance getMethod() {
        return method;
    }

    public void setMethod(MethodInstance method) {
        this.method = method;
    }

    public List<GeneratedClass> getTypes() {
        return types;
    }

    public void setTypes(List<GeneratedClass> types) {
        this.types = types;
    }


}
