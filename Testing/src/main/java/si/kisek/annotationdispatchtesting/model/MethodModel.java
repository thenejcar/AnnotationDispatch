package si.kisek.annotationdispatchtesting.model;

import java.util.*;

/*
* Represents methods with the same name and num of parameters, similar to MethodModel in the AnnotationProcessor
* */
public class MethodModel {
    private String name;
    private String modifiers;
    private int numParameters;
    private HashMap<GeneratedClass, String> objects;
    private Set<String> initCode;
    private List<GeneratedClass> classes;
    private boolean isVoid;


    public MethodModel(String name, String modifiers, int numParameters, boolean isVoid) {
        this.name = name;
        this.modifiers = modifiers;
        this.numParameters = numParameters;
        this.isVoid = isVoid;

        this.objects = new HashMap<>();
        this.initCode = new HashSet<>();
        this.classes = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModifiers() {
        return modifiers;
    }

    public void setModifiers(String modifiers) {
        this.modifiers = modifiers;
    }

    public int getNumParameters() {
        return numParameters;
    }

    public void setNumParameters(int numParameters) {
        this.numParameters = numParameters;
    }

    public boolean isVoid() {
        return isVoid;
    }

    public void addObject(GeneratedClass type, String varName, String initCode) {
        this.objects.put(type, varName);
        this.initCode.add("        " + initCode + "\n");
    }

    public HashMap<GeneratedClass, String> getObjects() {
        return objects;
    }

    public Set<String> getInitCode() {
        return initCode;
    }

    public void addClasses(List<GeneratedClass> classes) {
        this.classes.addAll(classes);
    }

    public List<GeneratedClass> getClasses() {
        return classes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodModel)) return false;
        MethodModel that = (MethodModel) o;
        return getNumParameters() == that.getNumParameters() &&
                Objects.equals(getName(), that.getName()) &&
                Objects.equals(getModifiers(), that.getModifiers());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getName(), getModifiers(), getNumParameters());
    }
}
