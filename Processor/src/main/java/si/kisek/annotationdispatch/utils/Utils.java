package si.kisek.annotationdispatch.utils;

import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Utils {

    // da niso predolge vrstice
    public static <T> com.sun.tools.javac.util.List<T> javacList(Iterable<T> normalIterable) {
        return com.sun.tools.javac.util.List.from(normalIterable);
    }

    public static <T> com.sun.tools.javac.util.List<T> javacList(T[] array) {
        return com.sun.tools.javac.util.List.from(array);
    }

    public static void addSymbolToClass(JCTree.JCClassDecl classDecl, Symbol symbol) {
        Scope scope;
        try {
            Field field = Symbol.ClassSymbol.class.getField("members_field");
            Method enterSym = field.getType().getMethod("enter", Symbol.class);

            scope = (Scope) field.get(classDecl.sym); // get the class members field
            enterSym.invoke(scope, symbol);  // enter the symbol
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to inject symbol into the class, you are using the wrong compiler version or we have bugs in the annotation processor");
        }
    }
}
