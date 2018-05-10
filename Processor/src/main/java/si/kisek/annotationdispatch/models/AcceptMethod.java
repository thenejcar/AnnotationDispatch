package si.kisek.annotationdispatch.models;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;

import java.util.List;
import java.util.Objects;

/*
 * Class that represents one accept method in the dispatch visitor
 * Each visitable class should implement all of them, but most (those that do not match its type) should return an Exception
 * */
public class AcceptMethod {
    private String name; // Name, looks like "12randomness34_accept1"
    private MethodModel mm; // Method that this visitor is dispatching
    private Symbol.MethodSymbol sym; // symbol of the method that will be created from this class
    private int level; // which parameter is being defined with this method
    private List<JCTree.JCVariableDecl> definedParameters; // all parameters before 'level' are already defined
    private List<JCTree.JCVariableDecl> undefinedParameters; // parameters after 'define' use the root type for this MM


    public AcceptMethod(String name, MethodModel mm, Symbol.MethodSymbol sym, int level, List<JCTree.JCVariableDecl> definedParameters, List<JCTree.JCVariableDecl> undefinedParameters) {
        this.name = name;
        this.mm = mm;
        this.sym = sym;
        this.level = level;
        this.definedParameters = definedParameters;
        this.undefinedParameters = undefinedParameters;
    }

    public String getName() {
        return name;
    }

    public MethodModel getMethodModel() {
        return mm;
    }

    public Symbol.MethodSymbol getSym() {
        return sym;
    }

    public int getLevel() {
        return level;
    }

    public List<JCTree.JCVariableDecl> getDefinedParameters() {
        return definedParameters;
    }

    public List<JCTree.JCVariableDecl> getUndefinedParameters() {
        return undefinedParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AcceptMethod)) return false;
        AcceptMethod that = (AcceptMethod) o;
        return getLevel() == that.getLevel() &&
                Objects.equals(getName(), that.getName()) &&
                Objects.equals(getMethodModel(), that.getMethodModel()) &&
                Objects.equals(getSym(), that.getSym()) &&
                Objects.equals(getDefinedParameters(), that.getDefinedParameters()) &&
                Objects.equals(getUndefinedParameters(), that.getUndefinedParameters());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getMethodModel(), getSym(), getLevel(), getDefinedParameters(), getUndefinedParameters());
    }
}
