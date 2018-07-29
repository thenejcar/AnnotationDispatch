package si.kisek.annotationdispatchtesting.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MethodInstance {
    private MethodModel mm;
    private String code;
    private List<GeneratedClass> parameters;
    private String parametersHash;

    private List<String> exampleCalls;

    public MethodInstance(MethodModel mm, String code, List<GeneratedClass> parameters, String parametersHash) {
        this.mm = mm;
        this.code = code;
        this.parameters = parameters;
        this.parametersHash = parametersHash;
        this.exampleCalls = new ArrayList<>();
    }

    public MethodModel getMm() {
        return mm;
    }

    public void setMm(MethodModel mm) {
        this.mm = mm;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<GeneratedClass> getParameters() {
        return parameters;
    }

    public void setParameters(List<GeneratedClass> parameters) {
        this.parameters = parameters;
    }

    public String getParametersHash() {
        return parametersHash;
    }

    public void setParametersHash(String parametersHash) {
        this.parametersHash = parametersHash;
    }

    public void addExampleCall(String call) {
        this.exampleCalls.add(call);
    }

    public List<String> getExampleCalls() {
        return exampleCalls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodInstance)) return false;
        if (!super.equals(o)) return false;
        MethodInstance that = (MethodInstance) o;
        return Objects.equals(getMm(), that.getMm()) &&
                Objects.equals(getCode(), that.getCode()) &&
                Objects.equals(getParametersHash(), that.getParametersHash());
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(), getMm(), getCode(), getParametersHash());
    }
}
