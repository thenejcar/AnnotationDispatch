package si.kisek.annotationdispatchtesting.model;

import java.util.Objects;

public class GeneratedClass {

    private String name;
    private String code;
    private GeneratedClass parent;
    private GeneratedClass root;

    public GeneratedClass(String name, String code, GeneratedClass parent, GeneratedClass root) {
        this.name = name;
        this.code = code;
        this.parent = parent;
        if (root == null)
            this.root = this; // this is root
        else
            this.root = root;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public GeneratedClass getParent() {
        return parent;
    }

    public GeneratedClass getRoot() {
        return root;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GeneratedClass)) return false;
        GeneratedClass that = (GeneratedClass) o;
        return Objects.equals(getName(), that.getName()) &&
                Objects.equals(getCode(), that.getCode());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getName(), getCode());
    }

    @Override
    public String toString() {
        return "GeneratedClass{" +
                name + '\'' +
                '}';
    }
}
