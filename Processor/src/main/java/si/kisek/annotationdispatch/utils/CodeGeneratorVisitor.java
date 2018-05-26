package si.kisek.annotationdispatch.utils;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.TreeMaker;
import si.kisek.annotationdispatch.models.AcceptMethod;
import si.kisek.annotationdispatch.models.MethodModel;

import java.util.*;

import static si.kisek.annotationdispatch.utils.Utils.javacList;

public class CodeGeneratorVisitor {

    private TreeMaker tm;
    private JavacElements el;
    private Symtab symtab;
    private MethodModel mm;
    private HashMap<Type, Set<AcceptMethod>> acceptMethods;

    public CodeGeneratorVisitor(TreeMaker tm, JavacElements el, Symtab symtab, MethodModel mm, HashMap<Type, Set<AcceptMethod>> acceptMethods) {
        this.tm = tm;
        this.el = el;
        this.symtab = symtab;
        this.mm = mm;
        this.acceptMethods = acceptMethods;
    }

    public void modifyVisitableClass(JCTree.JCClassDecl classDecl) {
        //TODO: add DispatchException to symbol table
        // add the implements modifier
        classDecl.implementing.append(tm.Ident(el.getName(mm.getVisitableName())));

        Set<AcceptMethod> alreadyImplemented = new HashSet<>();

        for (Type t : acceptMethods.keySet()) {

            for (AcceptMethod am : acceptMethods.get(t)) {
                if (alreadyImplemented.contains(am)) { continue; }
                alreadyImplemented.add(am);

                JCTree.JCMethodDecl methodDecl = tm.MethodDef(
                        tm.Modifiers(1), // public
                        el.getName(am.getName()),
                        mm.getReturnValue(),
                        com.sun.tools.javac.util.List.from(new JCTree.JCTypeParameter[0]), // no type parameters (TODO: check this)
                        com.sun.tools.javac.util.List.from(new JCTree.JCVariableDecl[0]),  // arguments are added below
                        com.sun.tools.javac.util.List.from(new JCTree.JCExpression[0]),    // no exception throwing
                        null,                               // body is added below
                        null                           // no default value
                );

                JCTree.JCVariableDecl visitorParam = tm.Param(
                        el.getName("v"),
                        new Type.ClassType(new Type.JCNoType(), javacList(new Type[0]), el.getTypeElement("java.lang.Object")), //TODO: fix
                        methodDecl.sym
                );

                List<JCTree.JCVariableDecl> parameters = new ArrayList<>();
                parameters.add(visitorParam);
                parameters.addAll(am.getDefinedParameters());
                parameters.addAll(am.getUndefinedParameters());
                methodDecl.params = javacList(parameters);

                List<JCTree.JCExpression> callParameters = new ArrayList<>();
                callParameters.add(tm.Ident(el.getName("this")));
                for (JCTree.JCVariableDecl paramDecl : am.getUndefinedParameters()) {
                    // set the Visitable type and add to parameters
                    //TODO: fix paramDecl.type.tsym = el.getTypeElement(mm.getPackageName() + "." + mm.getVisitableName());
                    callParameters.add(tm.Ident(paramDecl.name));
                }

                JCTree.JCMethodInvocation visitCall = tm.Apply(
                        javacList(new JCTree.JCExpression[0]),
                        tm.Select(tm.Ident(visitorParam.getName()), el.getName("visit" + am.getLevel())),
                        javacList(callParameters)
                );

                if (t != null && t.equals(classDecl.sym.type)) {
                    // implement the body, because the method is relevant for this type

                    List<JCTree.JCStatement> returnBlock = new ArrayList<>();
                    if (mm.isVoid()) {
                        returnBlock.add(tm.Exec(visitCall));
                    } else {
                        returnBlock.add(tm.Return(visitCall));
                    }
                    methodDecl.body = tm.Block(0, javacList(returnBlock));

                } else {
                    // throw exception when called, since the method is not relevant for this type

                    List<JCTree.JCExpression> exceptionParams = new ArrayList<>();
                    exceptionParams.add(tm.Literal(mm.getName().toString()));
                    for (JCTree.JCVariableDecl varDecl : am.getDefinedParameters())
                        exceptionParams.add(tm.Ident(varDecl.getName()));
                    exceptionParams.add(tm.Ident(el.getName("this")));
                    for (JCTree.JCVariableDecl varDecl : am.getUndefinedParameters()) {
                        // set the Visitable type and add to parameters
                        //TODO: fix varDecl.type.tsym = el.getTypeElement(mm.getPackageName() + "." + mm.getVisitableName());
                        exceptionParams.add(tm.Ident(varDecl.getName()));
                    }

                    methodDecl.body = tm.Block(0, javacList(Collections.singletonList(
                            tm.Throw(tm.NewClass(
                                    null,
                                    javacList(new JCTree.JCExpression[0]),
                                    tm.Ident(el.getName("si.kisek.annotationdispatch.DispatchException")),
                                    javacList(exceptionParams),
                                    null
                            ))
                    )));
                }

                classDecl.defs = classDecl.defs.append(methodDecl);

                List<Type> types = new ArrayList<>();
                for (JCTree.JCVariableDecl param : methodDecl.getParameters())
                    types.add(param.type);

                // register the new method in the symbol table
                Symbol.MethodSymbol methodSymbol = new Symbol.MethodSymbol(
                        methodDecl.mods.flags,
                        methodDecl.name,
                        new Type.MethodType(javacList(types), methodDecl.getReturnType().type, javacList(new Type[0]), symtab.methodClass),
                        classDecl.sym
                );
                Utils.addMethodSymbolToClass(classDecl, methodSymbol);
            }
        }

        System.out.println("Visitable class " + classDecl.name + " modified.");
    }
}
