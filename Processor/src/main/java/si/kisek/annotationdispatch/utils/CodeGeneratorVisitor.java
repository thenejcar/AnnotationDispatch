package si.kisek.annotationdispatch.utils;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import si.kisek.annotationdispatch.models.AcceptMethod;
import si.kisek.annotationdispatch.models.MethodInstance;
import si.kisek.annotationdispatch.models.MethodModel;

import java.util.*;

import static si.kisek.annotationdispatch.utils.Utils.javacList;

public class CodeGeneratorVisitor {

    private TreeMaker tm;
    private JavacElements el;
    private JavacTypes types;
    private Symtab symtab;
    private MethodModel mm;
    private HashMap<Type, Set<AcceptMethod>> acceptMethods;
    private Set<MethodInstance> methodInstances;

    // additional classes generated
    private JCTree.JCClassDecl visitableDecl;
    private JCTree.JCClassDecl visitorDecl;
    private JCTree.JCClassDecl exceptionDecl;

    public CodeGeneratorVisitor(TreeMaker tm, JavacElements el, JavacTypes types, Symtab symtab, MethodModel mm, HashMap<Type, Set<AcceptMethod>> acceptMethods, Set<MethodInstance> methodInstances) {
        this.tm = tm;
        this.el = el;
        this.types = types;
        this.symtab = symtab;
        this.mm = mm;
        this.acceptMethods = acceptMethods;
        this.methodInstances = methodInstances;
    }

    public MethodModel getCurrentMm() {
        return mm;
    }

    public HashMap<Type, Set<AcceptMethod>> getCurrentAcceptMethods() {
        return acceptMethods;
    }

    public Set<MethodInstance> getCurrentMethodInstances() {
        return methodInstances;
    }

    public void generateVisitableAndVisitor() {
        this.visitableDecl = createStaticVisitableInterface();
        this.visitorDecl = createStaticVisitorClass();
        this.exceptionDecl = createExceptionClass();

        // use the Visitable type in accept methods instead of the placeholder
        for (Type t : acceptMethods.keySet()) {
            Set<AcceptMethod> fixedMethods = new HashSet<>();
            for (AcceptMethod am : acceptMethods.get(t)) {

                List<JCTree.JCVariableDecl> fixedUndefParams = new ArrayList<>();
                for (JCTree.JCVariableDecl param : am.getUndefinedParameters()) {
                    fixedUndefParams.add(tm.Param(
                            param.name,
                            new Type.ClassType(new Type.JCNoType(), javacList(new Type[0]), this.visitableDecl.sym),
                            param.sym
                    ));
                }
                fixedMethods.add(new AcceptMethod(am.getName(), am.getMethodModel(), am.getSym(), am.getLevel(), am.isRoot(), am.getDefinedParameters(), fixedUndefParams));
            }
            acceptMethods.put(t, fixedMethods);
        }
        // finalize the classes with visit/accept methods
        this.fillVisitableInterface();
        this.fillVisitorClass();
    }

    /*
     * Creates an empty static interface Visitable inside the parent class
     * body will be created in another method
     * */
    public JCTree.JCClassDecl createStaticVisitableInterface() {
        JCTree.JCClassDecl newClass = tm.ClassDef(
                tm.Modifiers(Flags.PUBLIC | Flags.STATIC | Flags.INTERFACE),
                el.getName(mm.getVisitableName()),
                javacList(new JCTree.JCTypeParameter[0]),
                null,
                javacList(new JCTree.JCExpression[0]),
                javacList(new JCTree[0])
        );

        // create the Symbol for Visitable and add it to the parent class
        Symbol.ClassSymbol newSymbol = new Symbol.ClassSymbol(
                newClass.mods.flags,
                newClass.name,
                mm.getParentClass().sym
        );
        newSymbol.members_field = new Scope(newSymbol);

        mm.getParentClass().defs = mm.getParentClass().defs.append(newClass);
        newClass.sym = newSymbol;
        Utils.addSymbolToClass(mm.getParentClass(), newSymbol);

        return newClass;
    }

    public void fillVisitableInterface() {
        Set<AcceptMethod> alreadyImplemented = new HashSet<>();

        for (Set<AcceptMethod> s : this.acceptMethods.values()) {
            for (AcceptMethod am : s) {
                if (alreadyImplemented.contains(am))
                    continue;
                else
                    alreadyImplemented.add(am);

                JCTree.JCMethodDecl methodDecl = generateEmptyAcceptMethod(am);
                JCTree.JCVariableDecl visitorParam = createVisitorParam(methodDecl);

                // add parameters
                List<JCTree.JCVariableDecl> parameters = new ArrayList<>();
                parameters.add(visitorParam);
                parameters.addAll(am.getDefinedParameters());
                parameters.addAll(am.getUndefinedParameters());
                methodDecl.params = javacList(parameters);

                // interface method has empty body

                // add to class and register in symtab
                this.visitableDecl.defs = this.visitableDecl.defs.append(methodDecl);
                Utils.addSymbolToClass(this.visitableDecl, createSymbolForMethod(methodDecl, this.visitableDecl.sym));
            }
        }
    }


    /*
     * Creates an empty  static class Visitor inside the parent class
     * body will be created in another method
     * */
    public JCTree.JCClassDecl createStaticVisitorClass() {
        JCTree.JCClassDecl newClass = tm.ClassDef(
                tm.Modifiers(Flags.PUBLIC | Flags.STATIC),
                el.getName(mm.getVisitorName()),
                javacList(new JCTree.JCTypeParameter[0]),
                null,
                javacList(new JCTree.JCExpression[0]),
                javacList(new JCTree[0])
        );

        // create the Symbol for Visitor and add it to the parent class
        Symbol.ClassSymbol newSymbol = new Symbol.ClassSymbol(
                newClass.mods.flags,
                newClass.name,
                mm.getParentClass().sym
        );
        newSymbol.members_field = new Scope(newSymbol);

        mm.getParentClass().defs = mm.getParentClass().defs.append(newClass);
        newClass.sym = newSymbol;
        Utils.addSymbolToClass(mm.getParentClass(), newSymbol);

        return newClass;
    }

    public void fillVisitorClass() {
        Set<AcceptMethod> alreadyImplemented = new HashSet<>();

        for (Set<AcceptMethod> s : acceptMethods.values()) {
            for (AcceptMethod am : s) {
                // skip the acceptMethods with no undefined parameters -- those are added separately
                if (alreadyImplemented.contains(am) || am.getDefinedParameters().size() < 1)
                    continue;
                else
                    alreadyImplemented.add(am);

                JCTree.JCMethodDecl methodDecl = tm.MethodDef(
                        tm.Modifiers(Flags.PUBLIC),
                        el.getName("visit" + am.getLevel()),
                        mm.getReturnValue(),
                        com.sun.tools.javac.util.List.from(new JCTree.JCTypeParameter[0]),
                        com.sun.tools.javac.util.List.from(new JCTree.JCVariableDecl[0]),  // arguments are added later
                        com.sun.tools.javac.util.List.from(new JCTree.JCExpression[0]),    // no exception throwing
                        null,                               // body is added below
                        null                           // no default value
                );

                // parameters: defined then undefined
                List<JCTree.JCVariableDecl> parameters = new ArrayList<>();
                parameters.addAll(am.getDefinedParameters());

                JCTree.JCVariableDecl currentParam = tm.Param(
                        el.getName("o" + am.getLevel()),
                        new Type.ClassType(
                                new Type.JCNoType(),
                                javacList(new Type[0]),
                                this.visitableDecl.sym
                        ),
                        methodDecl.sym
                );
                parameters.add(currentParam);
                parameters.addAll(am.getUndefinedParameters());
                methodDecl.params = javacList(parameters);

                JCTree.JCExpression call;

                // body: for regular visit-accept calls
                List<JCTree.JCExpression> callParameters = new ArrayList<>();
                callParameters.add(tm.Ident(el.getName("this")));
                for (JCTree.JCVariableDecl paramDecl : am.getDefinedParameters()) {
                    callParameters.add(tm.Ident(paramDecl.name));
                }
                for (JCTree.JCVariableDecl param : am.getUndefinedParameters()) {
                    callParameters.add(tm.Ident(param.name));
                }

                call = tm.Apply(
                        javacList(new JCTree.JCExpression[0]),
                        tm.Select(tm.Ident(currentParam.getName()), el.getName("accept" + (am.getLevel() + 1))),
                        javacList(callParameters)
                );


                List<JCTree.JCStatement> returnBlock = new ArrayList<>();
                if (mm.isVoid()) {
                    returnBlock.add(tm.Exec(call));
                } else {
                    returnBlock.add(tm.Return(call));
                }
                methodDecl.body = tm.Block(0, javacList(returnBlock));

                // add to class and register in symtab
                this.visitorDecl.defs = this.visitorDecl.defs.append(methodDecl);
                Utils.addSymbolToClass(this.visitorDecl, createSymbolForMethod(methodDecl, this.visitorDecl.sym));
            }
        }

        for (MethodInstance mi : methodInstances) {

            JCTree.JCMethodDecl methodDecl = tm.MethodDef(
                    tm.Modifiers(Flags.PUBLIC),
                    el.getName("visit" + mi.getNumParameters()),
                    mm.getReturnValue(),
                    com.sun.tools.javac.util.List.from(new JCTree.JCTypeParameter[0]),
                    com.sun.tools.javac.util.List.from(new JCTree.JCVariableDecl[0]),  // arguments are added later
                    com.sun.tools.javac.util.List.from(new JCTree.JCExpression[0]),    // no exception throwing
                    null,                               // body is added below
                    null                           // no default value
            );

            List<JCTree.JCVariableDecl> methodParameters = new ArrayList<>();
            List<JCTree.JCExpression> callParameters = new ArrayList<>();
            int i = 0;
            for (Type type : mi.getParameters()) {
                JCTree.JCVariableDecl param = tm.Param(
                        el.getName("d" + i),
                        type,
                        methodDecl.sym
                );
                methodParameters.add(param);
                callParameters.add(tm.Ident(param.name));
                i += 1;
            }

            // parameters
            methodDecl.params = javacList(methodParameters);


            //TODO: support for nonstatic visitor

            // call to original method
            JCTree.JCMethodInvocation call = tm.Apply(
                    javacList(new JCTree.JCExpression[0]),
                    tm.Select(tm.Ident(mm.getParentClass().name), mm.getName()),
                    javacList(callParameters)
            );

            List<JCTree.JCStatement> returnBlock = new ArrayList<>();
            if (mm.isVoid()) {
                returnBlock.add(tm.Exec(call));
            } else {
                returnBlock.add(tm.Return(call));
            }
            methodDecl.body = tm.Block(0, javacList(returnBlock));

            // add to class and register in symtab
            this.visitorDecl.defs = this.visitorDecl.defs.append(methodDecl);
            Utils.addSymbolToClass(this.visitorDecl, createSymbolForMethod(methodDecl, this.visitorDecl.sym));
        }

        System.out.println(visitorDecl.getSimpleName().toString() + " filled with visit methods");
    }

    /*
     * Creates an empty  static class Visitor inside the parent class
     * body will be created in another method
     * */
    public JCTree.JCClassDecl createExceptionClass() {
        JCTree.JCClassDecl newClass = tm.ClassDef(
                tm.Modifiers(Flags.PUBLIC | Flags.STATIC),
                el.getName(mm.getExceptionName()),
                javacList(new JCTree.JCTypeParameter[0]),
                null,
                javacList(new JCTree.JCExpression[0]),
                javacList(new JCTree[0])
        );

        newClass.extending = tm.Ident(el.getName("RuntimeException"));

        JCTree.JCMethodDecl constructor = tm.MethodDef(
                tm.Modifiers(Flags.PUBLIC),
                el.getName("<init>"),
                null,
                com.sun.tools.javac.util.List.from(new JCTree.JCTypeParameter[0]),
                com.sun.tools.javac.util.List.from(new JCTree.JCVariableDecl[0]),  // arguments are added later
                com.sun.tools.javac.util.List.from(new JCTree.JCExpression[0]),    // no exception throwing
                null,                               // empty
                null                           // no default value
        );

        JCTree.JCVariableDecl stringMessage = tm.Param(
                el.getName("message"),
                new Type.ClassType(new Type.JCNoType(), javacList(new Type[0]), el.getTypeElement("java.lang.String")),
                constructor.sym
        );

        constructor.params = javacList(Collections.singletonList(stringMessage));

        constructor.body = tm.Block(0, javacList(Collections.singletonList(
                tm.Exec(tm.Apply(
                        javacList(new JCTree.JCExpression[0]),
                        tm.Ident(el.getName("super")),
                        javacList(Collections.singletonList(tm.Ident(stringMessage.name)))
                ))
        )));

        newClass.defs = javacList(Collections.singletonList(constructor));

        // create the Symbol for Exception class
        Symbol.ClassSymbol newSymbol = new Symbol.ClassSymbol(
                newClass.mods.flags,
                newClass.name,
                mm.getParentClass().sym
        );
        newSymbol.members_field = new Scope(newSymbol);

        mm.getParentClass().defs = mm.getParentClass().defs.append(newClass);
        newClass.sym = newSymbol;
        Utils.addSymbolToClass(mm.getParentClass(), newSymbol);

        // create symbol for the constructor method
//        Utils.addSymbolToClass(newClass, createSymbolForMethod(constructor, newSymbol));

        this.exceptionDecl = newClass;
        return newClass;
    }


    public void modifyVisitableClass(JCTree.JCClassDecl classDecl) {

        if (!this.acceptMethods.containsKey(classDecl.sym.type)) {
            // class that is not used as a parameter can be left alone TODO: what about root accept Types?
            // it should inherit accepts from parent, or is incompatible anyways
            return;
        }

        // add the implements modifier
        classDecl.implementing = classDecl.implementing.append(tm.Ident(el.getName(mm.getVisitableName())));
        ((Type.ClassType) classDecl.sym.type).interfaces_field = javacList(Collections.singletonList(visitableDecl.sym.type));
        ((Type.ClassType) classDecl.sym.type).all_interfaces_field = javacList(Collections.singletonList(visitableDecl.sym.type));

        Set<AcceptMethod> alreadyImplemented = new HashSet<>();

        for (Type t : acceptMethods.keySet()) {

            for (AcceptMethod am : acceptMethods.get(t)) {
                if (alreadyImplemented.contains(am)) {
                    continue;
                }
                alreadyImplemented.add(am);

                JCTree.JCMethodDecl methodDecl = generateEmptyAcceptMethod(am);

                JCTree.JCVariableDecl visitorParam = createVisitorParam(methodDecl);

                List<JCTree.JCVariableDecl> parameters = new ArrayList<>();
                parameters.add(visitorParam);
                parameters.addAll(am.getDefinedParameters());
                parameters.addAll(am.getUndefinedParameters());
                methodDecl.params = javacList(parameters);

                List<JCTree.JCExpression> visitParameters = new ArrayList<>();
                for (JCTree.JCVariableDecl paramDecl : am.getDefinedParameters()) {
                    visitParameters.add(tm.Ident(paramDecl.name));
                }
                visitParameters.add(tm.Ident(el.getName("this")));
                for (JCTree.JCVariableDecl paramDecl : am.getUndefinedParameters()) {
                    visitParameters.add(tm.Ident(paramDecl.name));
                }

                List<JCTree.JCExpression> superParameters = new ArrayList<>();
                for (JCTree.JCVariableDecl paramDecl : parameters) {
                    superParameters.add(tm.Ident(paramDecl.name));
                }

                if (t != null && t.equals(classDecl.sym.type)) {
                    // implement the body, because the method is relevant for this type

                    JCTree.JCMethodInvocation visitCall = tm.Apply(
                            javacList(new JCTree.JCExpression[0]),
                            tm.Select(tm.Ident(visitorParam.getName()), el.getName("visit" + (am.getLevel() + 1))),
                            javacList(visitParameters)
                    );

                    JCTree.JCMethodInvocation superCall = tm.Apply(
                            javacList(new JCTree.JCExpression[0]),
                            tm.Select(tm.Ident(el.getName("super")), el.getName("accept" + (am.getLevel() + 1))),
                            javacList(superParameters)
                    );


                    List<JCTree.JCStatement> tryBlock = new ArrayList<>();
                    List<JCTree.JCStatement> catchBlock = new ArrayList<>();
                    if (mm.isVoid()) {
                        tryBlock.add(tm.Exec(visitCall));
                        catchBlock.add(tm.Exec(superCall));
                    } else {
                        tryBlock.add(tm.Return(visitCall));
                        catchBlock.add(tm.Return(superCall));
                    }

                    if (am.isRoot()) {
                        // no try catch -- just call the visit and let the Exception kill the program is needed
                        methodDecl.body = tm.Block(0, javacList(tryBlock));
                    } else {
                        // try catch -- catch the DispatchException and call super.accept
                        methodDecl.body = tm.Block(0, javacList(Collections.singletonList(tm.Try(
                                tm.Block(0, javacList(tryBlock)),
                                javacList(Collections.singletonList(tm.Catch(
                                        tm.Param(
                                                el.getName("de"),
                                                new Type.ClassType(
                                                        new Type.JCNoType(),
                                                        javacList(new Type[0]),
                                                        this.exceptionDecl.sym),
                                                null
                                        ),
                                        tm.Block(0, javacList(catchBlock))
                                ))),
                                null)
                        )));
                    }

                } else {
                    // throw exception when called, since the method is not relevant for this type

                    List<JCTree.JCExpression> exceptionParams = new ArrayList<>();
                    exceptionParams.add(tm.Literal(mm.getName().toString() + " not dispatched properly"));
//                    for (JCTree.JCVariableDecl varDecl : am.getDefinedParameters())
//                        exceptionParams.add(tm.Ident(varDecl.getName()));
//                    exceptionParams.add(tm.Ident(el.getName("this")));
//                    for (JCTree.JCVariableDecl varDecl : am.getUndefinedParameters()) {
//                        exceptionParams.add(tm.Ident(varDecl.getName()));
//                    }

                    methodDecl.body = tm.Block(0, javacList(Collections.singletonList(
                            tm.Throw(tm.NewClass(
                                    null,
                                    javacList(new JCTree.JCExpression[0]),
                                    tm.Ident(this.exceptionDecl.name),
                                    javacList(exceptionParams),
                                    null
                            ))
                    )));
                }

                // add the method to class and register it in the symbol table
                classDecl.defs = classDecl.defs.append(methodDecl);
                Utils.addSymbolToClass(classDecl, createSymbolForMethod(methodDecl, classDecl.sym));
            }
        }

        // add DispatchException and Visitor to the table
        Utils.addSymbolToClass(classDecl, this.exceptionDecl.sym);
        Utils.addSymbolToClass(classDecl, this.visitorDecl.sym);
        Utils.addSymbolToClass(classDecl, this.visitableDecl.sym);

        // also add the parent class of original methods
        Utils.addSymbolToClass(classDecl, mm.getParentClass().sym);

        System.out.println("Visitable class " + classDecl.name + " modified.");
    }

    public JCTree.JCMethodDecl createVisitorInitMethod() {
        JCTree.JCMethodDecl methodDecl = tm.MethodDef(
                tm.Modifiers(Flags.PUBLIC | Flags.STATIC),
                el.getName("dispatch_" + mm.getRandomness()),
                mm.getReturnValue(),
                com.sun.tools.javac.util.List.from(new JCTree.JCTypeParameter[0]),
                com.sun.tools.javac.util.List.from(new JCTree.JCVariableDecl[0]),  // arguments are added later
                com.sun.tools.javac.util.List.from(new JCTree.JCExpression[0]),    // no exception throwing
                null,                               // empty
                null                           // no default value
        );

        List<JCTree.JCVariableDecl> params = new ArrayList<>();
        for (int i = 0; i < mm.getNumParameters(); i++) {
            params.add(tm.Param(
                    el.getName("o" + i),
                    new Type.ClassType(new Type.JCNoType(), javacList(new Type[0]), visitableDecl.sym),
                    methodDecl.sym
            ));
        }
        methodDecl.params = javacList(params);

        List<JCTree.JCExpression> callParameters = new ArrayList<>();
        callParameters.add(tm.NewClass(
                null,
                javacList(new JCTree.JCExpression[0]),
                tm.Ident(visitorDecl.name),
                javacList(new JCTree.JCExpression[0]),
                null
        ));
        ListIterator<JCTree.JCVariableDecl> iter = params.listIterator();
        iter.next();
        while (iter.hasNext()) {
            callParameters.add(tm.Ident(iter.next().name));
        }

        JCTree.JCMethodInvocation acceptCall = tm.Apply(
                javacList(new JCTree.JCExpression[0]),
                tm.Select(tm.Ident(params.get(0).name), el.getName("accept1")),
                javacList(callParameters)
        );

        if (mm.isVoid())
            methodDecl.body = tm.Block(0, javacList(Collections.singletonList(tm.Exec(acceptCall))));
        else
            methodDecl.body = tm.Block(0, javacList(Collections.singletonList(tm.Return(acceptCall))));

        mm.getParentClass().defs = mm.getParentClass().defs.append(methodDecl);
        Utils.addSymbolToClass(mm.getParentClass(), createSymbolForMethod(methodDecl, mm.getParentClass().sym));

        return methodDecl;
    }


    // helper methods to reduce clutter in the code

    private JCTree.JCMethodDecl generateEmptyAcceptMethod(AcceptMethod am) {
        return tm.MethodDef(
                tm.Modifiers(Flags.PUBLIC),
                el.getName(am.getName()),
                mm.getReturnValue(),
                com.sun.tools.javac.util.List.from(new JCTree.JCTypeParameter[0]),
                com.sun.tools.javac.util.List.from(new JCTree.JCVariableDecl[0]),  // arguments are added later
                com.sun.tools.javac.util.List.from(new JCTree.JCExpression[0]),    // no exception throwing
                null,                               // empty
                null                           // no default value
        );
    }

    private JCTree.JCVariableDecl createVisitorParam(JCTree.JCMethodDecl methodDecl) {
        return tm.Param(
                el.getName("v"),
                new Type.ClassType(new Type.JCNoType(), javacList(new Type[0]), this.visitorDecl.sym),
                methodDecl.sym
        );
    }

    private Symbol.MethodSymbol createSymbolForMethod(JCTree.JCMethodDecl methodDecl, Symbol.ClassSymbol parentClassSymbol) {

        List<Type> types = new ArrayList<>();
        for (JCTree.JCVariableDecl param : methodDecl.getParameters())
            types.add(param.type);


        Type retType = methodDecl.getReturnType() != null ? methodDecl.getReturnType().type : null;

        return new Symbol.MethodSymbol(
                methodDecl.mods.flags,
                methodDecl.name,
                new Type.MethodType(javacList(types), retType, javacList(new Type[0]), symtab.methodClass),
                parentClassSymbol
        );
    }
}
