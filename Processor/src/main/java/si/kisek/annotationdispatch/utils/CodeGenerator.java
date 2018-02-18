package si.kisek.annotationdispatch.utils;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import si.kisek.annotationdispatch.models.MethodSwitcher;

import javax.lang.model.util.Elements;
import java.util.*;

import static si.kisek.annotationdispatch.utils.Utils.javacList;

public class CodeGenerator {
    private TreeMaker tm;
    private JavacElements el;
    private JCTree.JCMethodDecl generatedMethod;

    public CodeGenerator(TreeMaker tm, JavacElements el, JCTree.JCMethodDecl generatedMethod) {
        this.tm = tm;
        this.el = el;
        this.generatedMethod = generatedMethod;
    }

    public JCTree.JCStatement generateIfInstanceOf(MethodSwitcher.SwitcherNode parentnode) {
        if (parentnode.subtree != null) {
            // generate a list of if statements
            List<JCTree.JCStatement> ifStatements = new ArrayList<>();
            for (MethodSwitcher.SwitcherNode node : parentnode.subtree) {
                ifStatements.add(tm.If(
                        tm.Parens(tm.TypeTest(tm.Ident(generatedMethod.params.get(parentnode.level)), tm.Ident(node.parentType.tsym))),
                        tm.Block(0, javacList(Collections.singletonList(generateIfInstanceOf(node)))),
                        null
                ));
            }
            return tm.Block(0, javacList(ifStatements));
        } else {
            // generate a method call at the leaves of the if tree

            // cast types on parameters for this method instances
            List<JCTree.JCExpression> parameters = new ArrayList<>();
            ListIterator<Type> instanceParamsIterator = parentnode.leaf.getParameters().listIterator();
            ListIterator<JCTree.JCVariableDecl> genParamsIterator = generatedMethod.params.listIterator();

            while (instanceParamsIterator.hasNext() && genParamsIterator.hasNext()) {
                Type t = instanceParamsIterator.next();
                JCTree.JCVariableDecl var = genParamsIterator.next();
                parameters.add(tm.TypeCast(t.tsym.type, tm.Ident(var)));
            }
            //return the method
            if (parentnode.leaf.getReturnValue() == null || parentnode.leaf.getReturnValue().type.tsym.name.toString().equals("void")) {
                // for void methods, exec and return without a value
                List<JCTree.JCStatement> returnBlock = new ArrayList<>();
                returnBlock.add(tm.Exec(
                        tm.Apply(
                                javacList(new JCTree.JCExpression[0]),
                                tm.Ident(parentnode.leaf.getName()),
                                javacList(parameters)
                        )
                ));
                returnBlock.add(tm.Return(null));


                return tm.Block(0, javacList(returnBlock));
            } else {
                // return the value
                return tm.Return(
                        tm.Apply(
                                javacList(new JCTree.JCExpression[0]),
                                tm.Ident(parentnode.leaf.getName()),
                                javacList(parameters)
                        )
                );
            }
        }
    }


    /*
     * builds a chain of if/elseif statements from an iterator
     *
     * if arg instanceof Class1 -> generateIfInstanceOf(node1)
     * else if arg instanceof Class2 -> generateIfInstanceOf(node2)
     * ...
     * */
    @Deprecated
    private JCTree.JCIf chainElseIfInstanceof(ListIterator<MethodSwitcher.SwitcherNode> iterator, int level) {
        if (!iterator.hasNext()) {
            return null;
        } else {
            MethodSwitcher.SwitcherNode node = iterator.next();
            Symbol symbol = node.parentType.tsym;
            return tm.If(
                    tm.Parens(tm.TypeTest(tm.Ident(generatedMethod.params.get(level)), tm.Ident(symbol))),
                    tm.Block(0, javacList(Collections.singletonList(generateIfInstanceOf(node)))),
                    chainElseIfInstanceof(iterator, level)

            );
        }

    }

    private JCTree.JCStatement listIfInstanceOfStatements(MethodSwitcher.SwitcherNode parentnode) {
        List<JCTree.JCStatement> ifStatements = new ArrayList<>();
        for (MethodSwitcher.SwitcherNode node : parentnode.subtree) {
            ifStatements.add(tm.If(
                    tm.Parens(tm.TypeTest(tm.Ident(generatedMethod.params.get(parentnode.level)), tm.Ident(parentnode.parentType.tsym))),
                    tm.Block(0, javacList(Collections.singletonList(generateIfInstanceOf(node)))),
                    null
            ));
        }
        return tm.Block(0, javacList(ifStatements));
    }

    public JCTree.JCStatement generateDefaultThrowStat(JCTree.JCMethodDecl generatedMethod) {

        return tm.Throw(tm.NewClass(
                null,
                javacList(new JCTree.JCExpression[0]),
                tm.Ident(el.getName("RuntimeException")),
                javacList(Collections.singletonList(
                        errorMessage(generatedMethod.params.listIterator())
                )),
                null
                )
        );
    }

    private JCTree.JCBinary errorMessage(ListIterator<JCTree.JCVariableDecl> iterator) {

        JCTree.JCBinary res = tm.Binary(
                JCTree.Tag.PLUS,
                tm.Literal("No method definition for provided combination of types: "),
                tm.Apply(
                        javacList(new JCTree.JCExpression[0]),
                        tm.Select(tm.Ident(iterator.next()), el.getName("getClass")),
                        javacList(new JCTree.JCExpression[0])
                )
        );

        while (iterator.hasNext()) {
            res = tm.Binary(
                    JCTree.Tag.PLUS,
                    tm.Binary(
                            JCTree.Tag.PLUS,
                            res,
                            tm.Literal(", ")
                    ),
                    tm.Apply(
                            javacList(new JCTree.JCExpression[0]),
                            tm.Select(tm.Ident(iterator.next()), el.getName("getClass")),
                            javacList(new JCTree.JCExpression[0])
                    )
            );
        }

        return res;
    }
}
