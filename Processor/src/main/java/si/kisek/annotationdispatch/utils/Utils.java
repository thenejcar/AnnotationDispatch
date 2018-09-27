package si.kisek.annotationdispatch.utils;

import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    // utility methods that convert normal lists in javac lists
    public static <T> com.sun.tools.javac.util.List<T> javacList(Iterable<T> normalIterable) {
        return com.sun.tools.javac.util.List.from(normalIterable);
    }

    public static <T> com.sun.tools.javac.util.List<T> javacList(T[] array) {
        return com.sun.tools.javac.util.List.from(array);
    }

    public static <T> com.sun.tools.javac.util.List<T> asJavacList(T... elements) {
        return com.sun.tools.javac.util.List.from(elements);
    }

    public static com.sun.tools.javac.util.List<JCTree.JCExpression> emptyExpr() {
        return javacList(new JCTree.JCExpression[0]);
    }


    /*
    * Utility method for adding imports to a class
    * */
    public static void addImports(JCTree.JCCompilationUnit compUnit, List<JCTree> imports) {
        List<JCTree> defs = new ArrayList<>();
        defs.addAll(imports);
        defs.addAll(compUnit.defs);
        compUnit.defs = javacList(defs);
    }

    /*
     * Inject generated method in the parent class of the original ones
     * */
    public static void addNewMethod(JCTree.JCClassDecl classDecl, JCTree.JCMethodDecl generatedMethod, Symtab symtab, Messager msg) {

        classDecl.defs = classDecl.defs.append(generatedMethod);  // add the method to the class

        List<Type> paramTypes = new ArrayList<>();
        for (JCTree.JCVariableDecl varDecl : generatedMethod.params) {
            paramTypes.add(varDecl.type == null ? varDecl.vartype.type : varDecl.type);
        }

        Type returnType = generatedMethod.getReturnType() == null ? new Type.JCVoidType() : generatedMethod.getReturnType().type;

        Symbol.MethodSymbol methodSymbol = new Symbol.MethodSymbol(
                generatedMethod.mods.flags,
                generatedMethod.name,
                new Type.MethodType(javacList(paramTypes), returnType, javacList(new Type[0]), symtab.methodClass),
                classDecl.sym
        );

        // use reflection to add the generated method symbol to the parent class
        Utils.addSymbolToClass(classDecl, methodSymbol);

        if (msg != null)
            msg.printMessage(Diagnostic.Kind.NOTE, "Method " + generatedMethod.name + " added to " + classDecl.name);
    }

    public static void addNewField(JCTree.JCClassDecl classDecl, JCTree.JCVariableDecl generatedVar, Symtab symtab, Messager msg) {
        classDecl.defs = classDecl.defs.append(generatedVar);  // add the method to the class

        Symbol.VarSymbol varSymbol = new Symbol.VarSymbol(
                generatedVar.mods.flags,
                generatedVar.name,
                new Type.TypeVar(generatedVar.name, generatedVar.sym, generatedVar.vartype.type),
                classDecl.sym
        );

        // use reflection to add the generated method symbol to the parent class
        Utils.addSymbolToClass(classDecl, varSymbol);

        if (msg != null)
            msg.printMessage(Diagnostic.Kind.NOTE, "Variable " + generatedVar.name + " added to " + classDecl.name);
    }



    /*
    * When adding a new field or method to a class, we need to modify its 'members_field' symbol table
    * this can only be done via reflection, since the api is not public
    *
    * This part will likely change with different JDK versions. Might want to look at how Lombok deals with compatibility with JDK 11.
    *
    * Also, accessing fields like this might be difficult in the future,
    * look at: https://stackoverflow.com/questions/46230413/jdk9-an-illegal-reflective-access-operation-has-occurred-org-python-core-pysys
    * */
    public static void addSymbolToClass(JCTree.JCClassDecl classDecl, Symbol symbol) {
        Scope scope;
        try {
            Field field = Symbol.ClassSymbol.class.getField("members_field");
            Method enterSym = field.getType().getMethod("enter", Symbol.class);

            scope = (Scope) field.get(classDecl.sym); // get the class members field
            enterSym.invoke(scope, symbol);           // enter the symbol
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to inject symbol into the class, you are using a compiler version other than 1.8, or we have bugs in the annotation processor.");
        }
    }
}
