package si.kisek.annotationdispatch.utils;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Name;
import si.kisek.annotationdispatch.models.AcceptMethod;
import si.kisek.annotationdispatch.models.MethodInstance;
import si.kisek.annotationdispatch.models.MethodModel;

import javax.annotation.processing.Messager;
import java.util.*;
import java.util.stream.Collectors;

import static si.kisek.annotationdispatch.utils.Utils.*;

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

    private Messager msg;

    //private JCTree.JCClassDecl exceptionDecl;

    public CodeGeneratorVisitor(TreeMaker tm, JavacElements el, JavacTypes types, Symtab symtab, MethodModel mm, HashMap<Type, Set<AcceptMethod>> acceptMethods, Set<MethodInstance> methodInstances, Messager msg) {
        this.tm = tm;
        this.el = el;
        this.types = types;
        this.symtab = symtab;
        this.mm = mm;
        this.acceptMethods = acceptMethods;
        this.methodInstances = methodInstances;
        this.msg = msg;
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

    public String getVisitableInterfaceName() {
        return visitableDecl.getSimpleName().toString();
    }

    public void generateVisitableAndVisitor() {
        this.visitableDecl = createStaticVisitableInterface();
        this.visitorDecl = createStaticVisitorClass();
        //this.exceptionDecl = createExceptionClass();
    }

    public void fillVisitableAndVisitor() {
        // use the Visitable type in accept methods instead of the placeholder
        for (Type t : acceptMethods.keySet()) {
            Set<AcceptMethod> fixedMethods = new HashSet<>();
            for (AcceptMethod am : acceptMethods.get(t)) {

                List<JCTree.JCVariableDecl> fixedUndefParams = new ArrayList<>();
                for (JCTree.JCVariableDecl param : am.getUndefinedParameters()) {
                    fixedUndefParams.add(tm.Param(
                            param.name,
                            new Type.ClassType(new Type.JCNoType(), javacList(new Type[0]), this.visitableDecl.sym),
                            am.getSym()
                    ));
                }
                fixedMethods.add(new AcceptMethod(am.getName(), am.getMethodModel(), am.getSym(), am.getLevel(), am.getDefinedParameters(), fixedUndefParams));
            }
            acceptMethods.put(t, fixedMethods);
        }
        // finalize the classes with visit/accept methods
        this.fillVisitableInterface();
        this.fillVisitorClass();
    }

    /*
     * Creates an empty static interface Visitable inside the parent class
     * body will be created in fillVisitableInterface() method
     * */
    public JCTree.JCClassDecl createStaticVisitableInterface() {
        JCTree.JCClassDecl newClass = tm.ClassDef(
                tm.Modifiers(Flags.PUBLIC | Flags.STATIC | Flags.INTERFACE),
                el.getName(mm.getVisitableName()),
                javacList(new JCTree.JCTypeParameter[0]),
                null,
                emptyExpr(),
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
                //TODO: Utils.addNewMethod(visitableDecl, methodDecl, symtab, msg);
                this.visitableDecl.defs = this.visitableDecl.defs.append(methodDecl);

                Symbol.MethodSymbol methodSymbol = createSymbolForMethod(methodDecl, this.visitableDecl.sym);

                Utils.addSymbolToClass(this.visitableDecl, methodSymbol);
                Utils.addSymbolToClass(mm.getParentClass(), methodSymbol);
                Utils.addSymbolToClass(this.visitorDecl, methodSymbol);
            }
        }
    }


    /*
     * Creates an empty  static class Visitor inside the parent class
     * body will be created in fillVisitorClass() method
     * */
    public JCTree.JCClassDecl createStaticVisitorClass() {
        JCTree.JCClassDecl newClass = tm.ClassDef(
                tm.Modifiers(Flags.PUBLIC | Flags.STATIC),
                el.getName(mm.getVisitorName()),
                javacList(new JCTree.JCTypeParameter[0]),
                null,
                emptyExpr(),
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
        if (!mm.isStatic()) {
            // non-static visitor gets a variable of the 'this' class
            JCTree.JCVariableDecl receiverDecl = tm.VarDef(
                            tm.Modifiers(Flags.PUBLIC),
                            el.getName("receiver"),
                            tm.Ident(mm.getParentClass().getSimpleName()),
                            null);
            Utils.addNewField(visitorDecl, receiverDecl, symtab, msg);
        }

        if (!mm.isVoid()) {
            // non-void visitor will a variable for the return value
            JCTree.JCVariableDecl returnVal = tm.VarDef(
                    tm.Modifiers(Flags.PUBLIC),
                    el.getName("returnVal"),
                    mm.getReturnValue(),
                    null
            );
            Utils.addNewField(visitorDecl, returnVal, symtab, msg);
        }

        Set<AcceptMethod> alreadyImplemented = new HashSet<>();

        for (Set<AcceptMethod> s : acceptMethods.values()) {
            for (AcceptMethod am : s) {
                // skip the already implemented methods (some entries in the hashMap can be duplicates)
                // also skip the accept methods with no undefined parameters -- those are added separately
                if (alreadyImplemented.contains(am) || am.getDefinedParameters().size() < 1)
                    continue;
                else
                    alreadyImplemented.add(am);

                JCTree.JCMethodDecl methodDecl = tm.MethodDef(
                        tm.Modifiers(Flags.PUBLIC),
                        el.getName("visit" + am.getLevel()),
                        tm.TypeIdent(TypeTag.BOOLEAN),
                        javacList(new JCTree.JCTypeParameter[0]),
                        javacList(new JCTree.JCVariableDecl[0]),  // arguments are added later
                        emptyExpr(),    // no exception throwing
                        null,                               // body is added below
                        null                           // no default value
                );

                // parameters: first defined then undefined
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

                // body: for regular visit-accept calls we just call the accept method one level above
                List<JCTree.JCExpression> callParameters = new ArrayList<>();
                callParameters.add(tm.Ident(el.getName("this")));
                for (JCTree.JCVariableDecl paramDecl : am.getDefinedParameters()) {
                    callParameters.add(tm.Ident(paramDecl.name));
                }
                for (JCTree.JCVariableDecl param : am.getUndefinedParameters()) {
                    callParameters.add(tm.Ident(param.name));
                }

                call = tm.Apply(
                        emptyExpr(),
                        tm.Select(tm.Ident(currentParam.getName()), el.getName("accept" + (am.getLevel() + 1))),
                        javacList(callParameters)
                );

                methodDecl.body = tm.Block(0, asJavacList(tm.Return(call))); // we return the result (for void types this will be boolean)

                // add to class and register in symtab
                // TODO: Utils.addNewMethod(visitorDecl, methodDecl, symtab, msg);

                Symbol.MethodSymbol methodSymbol = createSymbolForMethod(methodDecl, this.visitorDecl.sym);
                this.visitorDecl.defs = this.visitorDecl.defs.append(methodDecl);
                Utils.addSymbolToClass(this.visitorDecl, methodSymbol);
            }
        }

        // the last level visit methods are equivalent to the method instances
        for (MethodInstance mi : methodInstances) {

            JCTree.JCMethodDecl methodDecl = tm.MethodDef(
                    tm.Modifiers(Flags.PUBLIC),
                    el.getName("visit" + mi.getNumParameters()),
                    tm.TypeIdent(TypeTag.BOOLEAN),
                    javacList(new JCTree.JCTypeParameter[0]),
                    javacList(new JCTree.JCVariableDecl[0]),  // arguments are added later
                    emptyExpr(),    // no exception throwing
                    null,                               // body is added below
                    null                           // no default value
            );

            // all parameters are defined
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


            // call to original method
            JCTree.JCMethodInvocation call;
            if (mm.isStatic()) {
                call = tm.Apply(
                        emptyExpr(),
                        tm.Select(tm.Ident(mm.getParentClass().name), mm.getName()),
                        javacList(callParameters)
                );
            } else {
                call = tm.Apply(
                        emptyExpr(),
                        tm.Select(tm.Ident(el.getName("receiver")), mm.getName()),
                        javacList(callParameters)
                );
            }

            List<JCTree.JCStatement> returnBlock = new ArrayList<>();
            if (mm.isVoid()) {
                // return true
                returnBlock.add(tm.Exec(call));
                returnBlock.add(tm.Return(tm.Literal(TypeTag.BOOLEAN, 1)));
            } else {
                // store result in returnVal and return true
                returnBlock.add(tm.Exec(tm.Assign(tm.Ident(el.getName("returnVal")), call)));
                returnBlock.add(tm.Return(tm.Literal(TypeTag.BOOLEAN, 1)));
            }
            methodDecl.body = tm.Block(0, javacList(returnBlock));

            // add to class and register in symtab
            //TODO: Utils.addNewMethod(visitorDecl, methodDecl, symtab, msg);
            this.visitorDecl.defs = this.visitorDecl.defs.append(methodDecl);

            Symbol.MethodSymbol methodSymbol = createSymbolForMethod(methodDecl, this.visitorDecl.sym);
            Utils.addSymbolToClass(this.visitorDecl, methodSymbol);
        }
    }

    /*
     * Creates an empty  static class Visitor inside the parent class
     * body will be created in another method
     * */
//    public JCTree.JCClassDecl createExceptionClass() {
//        JCTree.JCClassDecl newClass = tm.ClassDef(
//                tm.Modifiers(Flags.PUBLIC | Flags.STATIC),
//                el.getName(mm.getExceptionName()),
//                javacList(new JCTree.JCTypeParameter[0]),
//                null,
//                emptyExpr(),
//                javacList(new JCTree[0])
//        );
//
//        newClass.extending = tm.Ident(el.getName("RuntimeException"));
//
//        JCTree.JCMethodDecl constructor = tm.MethodDef(
//                tm.Modifiers(Flags.PUBLIC),
//                el.getName("<init>"),
//                null,
//                com.sun.tools.javac.util.List.from(new JCTree.JCTypeParameter[0]),
//                com.sun.tools.javac.util.List.from(new JCTree.JCVariableDecl[0]),  // arguments are added later
//                com.sun.tools.javac.util.List.from(new JCTree.JCExpression[0]),    // no exception throwing
//                null,                               // empty
//                null                           // no default value
//        );
//
//        JCTree.JCVariableDecl stringMessage = tm.Param(
//                el.getName("message"),
//                new Type.ClassType(new Type.JCNoType(), javacList(new Type[0]), el.getTypeElement("java.lang.String")),
//                constructor.sym
//        );
//
//        constructor.params = javacList(Collections.singletonList(stringMessage));
//
//        constructor.body = tm.Block(0, javacList(Collections.singletonList(
//                tm.Exec(tm.Apply(
//                        emptyExpr(),
//                        tm.Ident(el.getName("super")),
//                        javacList(Collections.singletonList(tm.Ident(stringMessage.name)))
//                ))
//        )));
//
//        // create the Symbol for Exception class
//        Symbol.ClassSymbol newSymbol = new Symbol.ClassSymbol(
//                newClass.mods.flags,
//                newClass.name,
//                mm.getParentClass().sym
//        );
//        newSymbol.members_field = new Scope(newSymbol);
//
//        mm.getParentClass().defs = mm.getParentClass().defs.append(newClass);
//        newClass.sym = newSymbol;
//        Utils.addSymbolToClass(mm.getParentClass(), newSymbol);
//
//        JCTree.JCVariableDecl serialVersionUID = tm.VarDef(
//                tm.Modifiers(Flags.STATIC | Flags.FINAL),
//                el.getName("serialVersionUID"),
//                tm.TypeIdent(TypeTag.LONG),
//                tm.Literal(TypeTag.LONG, 12345L)
//        );
//
//        Symbol.VarSymbol varSymbol = new Symbol.VarSymbol(
//                serialVersionUID.mods.flags,
//                serialVersionUID.name,
//                new Type.TypeVar(serialVersionUID.name, serialVersionUID.sym, serialVersionUID.vartype.type),
//                newClass.sym
//        );
//        serialVersionUID.sym = varSymbol;
//
//        // use reflection to add the generated method symbol to the parent class
//        Utils.addSymbolToClass(newClass, varSymbol);
//
//        // finally add a body for the class
//        newClass.defs = asJavacList(constructor, serialVersionUID);
//
//        // create symbol for the constructor method
////        Utils.addSymbolToClass(newClass, createSymbolForMethod(constructor, newSymbol));
//
//        this.exceptionDecl = newClass;
//        return newClass;
//    }


    public void modifyVisitableClass(JCTree.JCClassDecl classDecl, boolean isRootType, JCTree.JCCompilationUnit compilationUnit) {

        if (!this.acceptMethods.containsKey(classDecl.sym.type)) {
            // class that is not used as a parameter can be left alone
            // it should inherit accepts from parent, or is incompatible anyways TODO: what about root accept Types? - check if it implements this.visitable?
            return;
        }

        // add the implements modifier
        classDecl.implementing = classDecl.implementing.append(tm.Ident(visitableDecl.sym.name));
        ((Type.ClassType) classDecl.sym.type).interfaces_field = javacList(Collections.singletonList(visitableDecl.sym.type));
        ((Type.ClassType) classDecl.sym.type).all_interfaces_field = javacList(Collections.singletonList(visitableDecl.sym.type));

        Set<AcceptMethod> alreadyImplemented = new HashSet<>();

        // get a list of all types, with current class's type being first
        // this is necessary, so that any duplicated methods of this class are implemented properly (with body)
        List<Type> typesForMethods = new ArrayList<>();
        typesForMethods.add(classDecl.sym.type); // first, add the current method's type
        for (Type t : acceptMethods.keySet()) {
            if (!t.equals(classDecl.sym.type))
                typesForMethods.add(t);
        }

        for (Type t : typesForMethods) {
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
                            emptyExpr(),
                            tm.Select(tm.Ident(visitorParam.getName()), el.getName("visit" + (am.getLevel() + 1))),
                            javacList(visitParameters)
                    );

                    JCTree.JCMethodInvocation superCall = tm.Apply(
                            emptyExpr(),
                            tm.Select(tm.Ident(el.getName("super")), el.getName("accept" + (am.getLevel() + 1))),
                            javacList(superParameters)
                    );

                    // return false when we get a dispatching miss
                    // if ( visit() ) { return true; } else { return super.accept(); }
                    methodDecl.body = tm.Block(0, asJavacList(
                            tm.If(
                                    visitCall,
                                    tm.Block(0, asJavacList(tm.Return(tm.Literal(TypeTag.BOOLEAN, 1)))),
                                    tm.Block(0, asJavacList(
                                            isRootType ? tm.Return(tm.Literal(TypeTag.BOOLEAN, 0)) : tm.Return(superCall)
                                    ))
                            )
                    ));

                } else {
                    // return false if this is root Type (has no visitable supertype), otherwise pass to super
                    if (isRootType) {
                        // return false when called, since the method is not relevant for this type
                        methodDecl.body = tm.Block(0, asJavacList(
                                tm.Return(tm.Literal(TypeTag.BOOLEAN, 0))
                        ));


                    } else {
                        methodDecl.body = tm.Block(0, asJavacList(
                                tm.Return(tm.Apply(
                                        emptyExpr(),
                                        tm.Select(tm.Ident(el.getName("super")), el.getName("accept" + (am.getLevel() + 1))),
                                        javacList(superParameters)
                                ))
                        ));
                    }
                }

                // add the method to class and register it in the symbol table
                // TODO: addNewMethod(classDecl, methodDecl, symtab, msg);

                Symbol.MethodSymbol methodSymbol = createSymbolForMethod(methodDecl, classDecl.sym);

                classDecl.defs = classDecl.defs.append(methodDecl);
                Utils.addSymbolToClass(classDecl, methodSymbol);

                // add the method to parent class too
                Utils.addSymbolToClass(mm.getParentClass(), methodSymbol);
                Utils.addSymbolToClass(this.visitorDecl, methodSymbol);
            }
        }

        // add Visitable and Visitor to the table
        Utils.addSymbolToClass(classDecl, this.visitorDecl.sym);
        Utils.addSymbolToClass(classDecl, this.visitableDecl.sym);

        // if class is not in the same compilation unit as the visitor and visitable classes, import them here
        if (compilationUnit != mm.getParentPath().getCompilationUnit()) {
            Utils.addImports(compilationUnit, Arrays.asList(
                    tm.Import(fullNameToJCFieldAccess(this.visitableDecl.sym.fullname.toString().split("\\."), -1), false)
            ));
        }
        // also add the parent class of original methods
        Utils.addSymbolToClass(classDecl, mm.getParentClass().sym);

        //System.out.println("Visitable class " + classDecl.name + " modified.");
    }

    /*
     * Used for imports
     * */
    // tm.Select(tm.Select(tm.Ident(this.elements.getName("java")), this.elements.getName("util")), this.elements.getName("*"))
    private JCTree.JCExpression fullNameToJCFieldAccess(String[] names, int index) {
        if (names.length + index <= 0) {
            return tm.Ident(el.getName(names[0]));
        } else {
            int end = names.length + index;
            return tm.Select(fullNameToJCFieldAccess(names, index - 1), el.getName(names[end]));
        }
    }


    public JCTree.JCMethodDecl createVisitorInitMethod() {
        JCTree.JCMethodDecl methodDecl = tm.MethodDef(
                tm.Modifiers(mm.getModifiers().flags),
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

        Name visitorInstance = el.getName("visitorInstance");

        List<JCTree.JCStatement> methodBody = new ArrayList<>();
        // instantiate the visitor
        methodBody.add(tm.VarDef(
                tm.Modifiers(0),
                visitorInstance,
                tm.Ident(this.visitorDecl.getSimpleName()),
                tm.NewClass(
                        null,
                        emptyExpr(),
                        tm.Ident(visitorDecl.name),
                        emptyExpr(),
                        null
                ))
        );

        if (!mm.isStatic()) {
            // in non-static methods, pass 'this' to visitor
            // visitor_instance.receiver = this;
            methodBody.add(tm.Exec(tm.Assign(
                    tm.Select(tm.Ident(visitorInstance), el.getName("receiver")),
                    tm.Ident(el.getName("this"))
            )));
        }

        // create the parameters for the first accept method and call it
        List<JCTree.JCExpression> callParameters = new ArrayList<>();
        callParameters.add(tm.Ident(visitorInstance));
        ListIterator<JCTree.JCVariableDecl> iter = params.listIterator();
        iter.next();
        while (iter.hasNext()) {
            callParameters.add(tm.Ident(iter.next().name));
        }

        JCTree.JCMethodInvocation acceptCall = tm.Apply(
                emptyExpr(),
                tm.Select(tm.Ident(params.get(0).name), el.getName("accept1")),
                javacList(callParameters)
        );
        methodBody.add(tm.Exec(acceptCall));

        if (!mm.isVoid()) {
            methodBody.add(tm.Return(tm.Select(tm.Ident(visitorInstance), el.getName("returnVal"))));
        }


        methodDecl.body = tm.Block(0, javacList(methodBody));

        Utils.addNewMethod(mm.getParentClass(), methodDecl, symtab, msg);

//        mm.getParentClass().defs = mm.getParentClass().defs.append(methodDecl);
//        Utils.addSymbolToClass(mm.getParentClass(), createSymbolForMethod(methodDecl, mm.getParentClass().sym));

        return methodDecl;
    }


    // helper methods to reduce clutter in the code

    private JCTree.JCMethodDecl generateEmptyAcceptMethod(AcceptMethod am) {
        return tm.MethodDef(
                tm.Modifiers(Flags.PUBLIC),
                el.getName(am.getName()),
                tm.TypeIdent(TypeTag.BOOLEAN),
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

        Symbol.MethodSymbol symbol = new Symbol.MethodSymbol(
                methodDecl.mods.flags,
                methodDecl.name,
                new Type.MethodType(javacList(types), retType, javacList(new Type[0]), symtab.methodClass),
                parentClassSymbol
        );

        // correct the symbols in the method declaration and its parameters
        methodDecl.sym = symbol;
        methodDecl.params = javacList(methodDecl.getParameters().stream().map(x -> tm.Param(x.name, x.type, symbol)).collect(Collectors.toList()));

        return symbol;
    }
}
