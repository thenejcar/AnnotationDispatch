package si.kisek.annotationdispatch.models;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.stream.Collectors;

import static si.kisek.annotationdispatch.utils.Utils.javacList;

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

    private JCTree.JCMethodDecl methodDecl;


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

    public JCTree.JCMethodDecl getMethodDecl() {
        return methodDecl;
    }

    public void setMethodDecl(JCTree.JCMethodDecl methodDecl) {
        this.methodDecl = methodDecl;
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
                Objects.equals(getUndefinedParameters().size(), that.getUndefinedParameters().size());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getMethodModel(), getSym(), getLevel(), getDefinedParameters(), getUndefinedParameters().size());
    }


    /*
    * Replaces the placeholder java.lang.Object with actual type of the Visitable interface
    * this has to be done AFTER visitable is added to the symtables and before accept methods are generated
    * */
    public void fixUndefinedParameterTypes(Symbol.TypeSymbol visitableTypeSymbol) {
        List<JCTree.JCVariableDecl> fixedParameters = new ArrayList<>();
        for (JCTree.JCVariableDecl param : undefinedParameters) {
            param.type = new Type.ClassType(
                    new Type.JCNoType(),
                    javacList(new Type[0]),
                    visitableTypeSymbol
            );
            fixedParameters.add(param);
        }
        this.undefinedParameters = fixedParameters;
    }

    /*
     * Emits the default code for this accept method (the one the throws an exception)
     * */
    @Deprecated
    public String emitInterfaceAcceptCode() {
        StringBuilder sb = new StringBuilder("public ").append(mm.getReturnValue()).append(" accept")
                .append(definedParameters.size() + 1)
                .append("(")
                .append(mm.getVisitorName()).append(" v");

        if (definedParameters.size() > 0 || undefinedParameters.size() > 0)
            sb.append(", ");

        ListIterator<JCTree.JCVariableDecl> defIter = definedParameters.listIterator();
        while (defIter.hasNext()) {
            JCTree.JCVariableDecl decl = defIter.next();
            sb.append(decl.type.tsym.name.toString()).append(" ").append(decl.name);
            if (defIter.hasNext() || undefinedParameters.size() > 0) {
                sb.append(", ");
            }
        }

        ListIterator<JCTree.JCVariableDecl> undefIter = undefinedParameters.listIterator();
        while (undefIter.hasNext()) {
            JCTree.JCVariableDecl decl = undefIter.next();
            sb.append(mm.getVisitableName()).append(" ").append(decl.name);
            if (undefIter.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(");");

        return sb.toString();
    }

    @Deprecated
    public String emitVisitorCode() {

        StringBuilder sb = new StringBuilder()
                .append("public ").append(mm.getReturnValue()).append(" visit").append(this.level)
                .append("(");

        StringBuilder acceptCall = new StringBuilder();

        ListIterator<JCTree.JCVariableDecl> defIter = definedParameters.listIterator();
        while (defIter.hasNext()) {
            JCTree.JCVariableDecl decl = defIter.next();
            sb.append(decl.type.tsym.name.toString()).append(" ").append(decl.name);
            acceptCall.append(decl.name);
            if (defIter.hasNext() || undefinedParameters.size() > 0) {
                acceptCall.append(", ");
            }
            sb.append(", ");
        }
        sb.append(mm.getVisitableName()).append(" o").append(this.level);
        if (undefinedParameters.size() > 0)
            sb.append(", ");

        ListIterator<JCTree.JCVariableDecl> undefIter = undefinedParameters.listIterator();
        while (undefIter.hasNext()) {
            JCTree.JCVariableDecl decl = undefIter.next();
            sb.append(mm.getVisitableName()).append(" ").append(decl.name);
            acceptCall.append(decl.name);
            if (undefIter.hasNext()) {
                sb.append(", ");
                acceptCall.append(", ");
            }
        }

        sb.append("){ ");
        if (!mm.isVoid()) {
            sb.append("return ");
        }
        sb.append("o").append(this.level)
                .append(".accept").append(this.level + 1).append("(this, ").append(acceptCall.toString())
                .append("); }");


        return sb.toString();

    }
}
