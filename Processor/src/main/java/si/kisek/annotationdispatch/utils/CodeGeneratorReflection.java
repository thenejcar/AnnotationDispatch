package si.kisek.annotationdispatch.utils;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import si.kisek.annotationdispatch.models.MethodInstance;
import si.kisek.annotationdispatch.models.MethodModel;
import si.kisek.annotationdispatch.models.MethodSwitcher;

import javax.lang.model.type.TypeMirror;
import javax.swing.text.html.HTML;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static si.kisek.annotationdispatch.utils.Utils.asJavacList;
import static si.kisek.annotationdispatch.utils.Utils.emptyExpr;
import static si.kisek.annotationdispatch.utils.Utils.javacList;

public class CodeGeneratorReflection {
    private TreeMaker tm;
    private JavacElements el;
    private JavacTypes types;
    private Symtab symtab;
    private Map<MethodModel, Set<MethodInstance>> originalMethods;
    private String randomness;


    public CodeGeneratorReflection(TreeMaker tm, JavacElements el, JavacTypes types, Symtab symtab, Map<MethodModel, Set<MethodInstance>> originalMethods) {
        this.tm = tm;
        this.el = el;
        this.types = types;
        this.symtab = symtab;
        this.originalMethods = originalMethods;
        this.randomness = UUID.randomUUID().toString().replace("-", "");
    }

    private String methodMapName() {
        return "methodMap_" + randomness;
    }

    private String initMethodName() {
        return "init_" + randomness;
    }

    /*
     * The 'method table', which is a HashMap that can find a list of methods based on their name and number of parameters
     * */
    public JCTree.JCVariableDecl generateMethodMapDecl() {

        // private static Map<String, List<Method>> methodMap_xxxx
        return tm.VarDef(
                tm.Modifiers(Flags.PRIVATE | Flags.STATIC),
                el.getName(methodMapName()),
                tm.TypeApply(
                        tm.Ident(el.getName("Map")),
                        asJavacList(
                                tm.Ident(el.getName("String")),
                                tm.TypeApply(
                                        tm.Ident(el.getName("List")),
                                        javacList(Collections.singletonList(tm.Ident(el.getName("Method"))))
                                )
                        )
                ),
                null // init is done in the initMethod
        );
    }

    /*
     * The method that initialises and fill the method table (methodMap)
     * */
    public JCTree.JCMethodDecl generateInitMethod() {
        JCTree.JCMethodDecl initMethod = tm.MethodDef(
                tm.Modifiers(Flags.PRIVATE | Flags.STATIC),
                el.getName(initMethodName()),
                tm.TypeIdent(TypeTag.VOID), // void return type
                com.sun.tools.javac.util.List.from(new JCTree.JCTypeParameter[0]),
                com.sun.tools.javac.util.List.from(new JCTree.JCVariableDecl[0]),  // no arguments
                com.sun.tools.javac.util.List.from(new JCTree.JCExpression[0]),    // no exception throwing
                null,                               // empty
                null                           // no default value
        );

        List<JCTree.JCStatement> stats = new ArrayList<>();

        // initialize the methodMap
        stats.add(tm.Exec(tm.Assign(
                tm.Ident(el.getName(methodMapName())),
                tm.NewClass(
                        null,
                        emptyExpr(),
                        tm.TypeApply(
                                tm.Ident(el.getName("HashMap")),
                                asJavacList(
                                        tm.Ident(el.getName("String")),
                                        tm.TypeApply(
                                                tm.Ident(el.getName("List")),
                                                asJavacList(
                                                        tm.Ident(el.getName("Method"))
                                                )
                                        )
                                )
                        ),
                        emptyExpr(),
                        null
                )
        )));

        for (MethodModel mm : originalMethods.keySet()) {
            // for each MM generate a try-catch block that adds the Methods to the methodMap

            List<JCTree.JCStatement> tryBlock = new ArrayList<>();

            // create a list of methods
            // List list = new ArrayList<Method>();
            Name tmpListName = el.getName("list");
            tryBlock.add(tm.VarDef(
                    tm.Modifiers(0),
                    tmpListName,
                    tm.TypeApply(tm.Ident(el.getName("List")), asJavacList(tm.Ident(el.getName("Method")))),
                    tm.NewClass(
                            null,
                            emptyExpr(),
                            tm.TypeApply(
                                    tm.Ident(el.getName("ArrayList")),
                                    asJavacList(tm.Ident(el.getName("Method")))
                            ),
                            emptyExpr(),
                            null
                    )
            ));

            // declaredMethod placeholder
            Name tmpMethodName = el.getName("method");
            tryBlock.add(tm.VarDef(
                    tm.Modifiers(0),
                    tmpMethodName,
                    tm.Ident(el.getName("Method")),
                    null
            ));

            // getting and adding methods is generated in the correct order
            // method instances are sorted in the same way as in the 'if-instanceof tree' dispatch method
            // we build the if-instanceof tree, and then use DFS to get a flat list of method instances
            List<MethodInstance> sortedInstances = new MethodSwitcher(types, mm, originalMethods.get(mm)).getSortedInstances();

            for (MethodInstance instance : sortedInstances) {
                // for each instance, get the Method object, set it to accessible and add it to the list
                // method = this.class.getMethod("mm_name", param1, param2, param3);

                List<JCTree.JCExpression> callParameters = new ArrayList<>();
                callParameters.add(tm.Literal(TypeTag.CLASS, mm.getName().toString()));
                callParameters.addAll(
                        instance.getParameters().stream().map((Type t) ->
                                tm.Select(tm.Ident(t.tsym.name), el.getName("class"))
                        ).collect(Collectors.toList())
                );
                tryBlock.add(tm.Exec(tm.Assign(
                        tm.Ident(tmpMethodName),
                        tm.Apply(
                                emptyExpr(),
                                tm.Select(
                                        tm.Select(
                                                tm.Ident(mm.getParentClass().getSimpleName()),
                                                el.getName("class")
                                        ),
                                        el.getName("getDeclaredMethod")
                                ),
                                javacList(callParameters)
                        )
                )));

                // method.setAccessible(true);
                tryBlock.add(tm.Exec(tm.Apply(
                        emptyExpr(),
                        tm.Select(tm.Ident(tmpMethodName), el.getName("setAccessible")),
                        asJavacList(tm.Literal(TypeTag.BOOLEAN, 1))
                )));

                // list.add(method)
                tryBlock.add(tm.Exec(tm.Apply(
                        emptyExpr(),
                        tm.Select(tm.Ident(tmpListName), el.getName("add")),
                        asJavacList(tm.Ident(tmpMethodName))
                )));
            }

            // add the list to the map

            // model's id = name + "_" + number of parameters
            JCTree.JCExpression modelId = tm.Binary(
                    JCTree.Tag.PLUS,
                    tm.Literal(TypeTag.CLASS, mm.getName().toString() + "_"),
                    tm.Literal(TypeTag.INT, mm.getNumParameters())
            );

            // methodMap.put("methodName_" + numParameters, list)
            tryBlock.add(tm.Exec(tm.Apply(
                    emptyExpr(),
                    tm.Select(tm.Ident(el.getName(methodMapName())), el.getName("put")),
                    asJavacList(
                            modelId,
                            tm.Ident(tmpListName)
                    )
            )));

            stats.add(tm.Try(
                    tm.Block(0, javacList(tryBlock)),
                    asJavacList(tm.Catch(
                            tm.VarDef(
                                    tm.Modifiers(0),
                                    el.getName("e"),
                                    tm.Ident(el.getName("NoSuchMethodException")),
                                    null
                            ),
                            tm.Block(0, asJavacList(
                                    tm.Exec(tm.Apply(
                                            emptyExpr(),
                                            tm.Select(tm.Ident(el.getName("e")), el.getName("printStackTrace")),
                                            emptyExpr()
                                    ))
                            ))
                    )),
                    null));
        }

        // set stats as the body of the init method
        initMethod.body = tm.Block(0, javacList(stats));
        return initMethod;
    }


    /*
     * Generate a dispatcher for each MethodModel
     * the dispatcher will look for matching methods, checking them one by one
     * they are already in the correct order (starting with most specific)
     * */
    public Map<MethodModel, JCTree.JCMethodDecl> generateDispatchers() {

        Map<MethodModel, JCTree.JCMethodDecl> dispatchers = new HashMap<>();

        for (MethodModel mm : originalMethods.keySet()) {

            JCTree.JCMethodDecl dispatcher = mm.generateDispatchMethod(tm, el);
            List<JCTree.JCVariableDecl> args = dispatcher.params;

            List<JCTree.JCStatement> stats = new ArrayList<>();

            // if init call was not done performed yet, do it now
            stats.add(tm.If(
                    tm.Binary(JCTree.Tag.EQ, tm.Ident(el.getName(methodMapName())), tm.Literal(TypeTag.BOT, null)),
                    tm.Block(0, asJavacList(tm.Exec(tm.Apply(emptyExpr(), tm.Ident(el.getName(initMethodName())), emptyExpr())))),
                    null
            ));


            // get list of possible methods
            // List<Method> candidates = methodMap.get("name" + "_" + numParameters))
            Name candidates = el.getName("candidates");
            stats.add(tm.VarDef(
                    tm.Modifiers(0),
                    candidates,
                    tm.TypeApply(tm.Ident(el.getName("List")), asJavacList(tm.Ident(el.getName("Method")))),
                    tm.Apply(
                            emptyExpr(),
                            tm.Select(tm.Ident(el.getName(methodMapName())), el.getName("get")),
                            asJavacList(
                                    tm.Binary(
                                            JCTree.Tag.PLUS,
                                            tm.Literal(TypeTag.CLASS, mm.getName().toString() + "_"),
                                            tm.Literal(TypeTag.INT, mm.getNumParameters())
                                    )
                            )
                    )
            ));

            // if (candidates == null) throw new RuntimeException("Dispatching parameters failed: " + methodName + numParameters)
            stats.add(tm.If(
                    tm.Parens(tm.Binary(JCTree.Tag.EQ, tm.Ident(candidates), tm.Literal(TypeTag.BOT, null))),
                    tm.Block(0, asJavacList(tm.Throw(tm.NewClass(
                            null,
                            emptyExpr(),
                            tm.Ident(el.getName("RuntimeException")),
                            asJavacList(tm.Literal(TypeTag.CLASS, "Dispatching parameters failed: " + mm.toString())),
                            null
                    )))),
                    null
            ));

            // for each candidate, check if we get a match
            // they are already sorted, so the first match is the correct one
            // something like this:
            // for (Method m : candidates)
            //     if (m.getParameterTypes()[0].isAssignableFrom(arg0.getClass()) &&
            //         m.getParameterTypes()[1].isAssignableFrom(arg1.getClass()) &&
            //         m.getParameterTypes()[2].isAssignableFrom(arg2.getClass())) {
            //         try {
            //             return bestMatch.invoke(this/null, args);
            //         } catch (Exception e) {
            //             e.printStackTrace();
            //             return null;
            //         }
            //     }

            JCTree.JCVariableDecl outerForIterable = tm.VarDef(
                    tm.Modifiers(0),
                    el.getName("m"),
                    tm.Ident(el.getName("Method")),
                    null
            );

            // parameters for invocation: this or null + new Object[] {arg0, arg1, ...}
            List<JCTree.JCExpression> invParams = new ArrayList<>();

            if (mm.isStatic()) {
                invParams.add(tm.Literal(TypeTag.BOT, null));
            } else {
                invParams.add(tm.Ident(el.getName("this")));
            }

            invParams.addAll(args.stream().map((arg) -> tm.Ident(arg.getName())).collect(Collectors.toList()));

            // void methods have different return code
            // invocation and catchBlock have no Return expression
            Name exception = el.getName("e");
            JCTree.JCBlock invocationBlock;
            JCTree.JCBlock catchBlock;

            if (mm.getReturnValue() instanceof JCTree.JCPrimitiveTypeTree && ((JCTree.JCPrimitiveTypeTree) mm.getReturnValue()).typetag.equals(TypeTag.VOID)) {
                invocationBlock = tm.Block(0, asJavacList(
                        tm.Exec(
                                tm.Apply(
                                        emptyExpr(),
                                        tm.Select(tm.Ident(outerForIterable.name), el.getName("invoke")),
                                        javacList(invParams)
                                )
                        ),
                        tm.Return(null)
                ));
                catchBlock = tm.Block(0, asJavacList(
                        tm.Exec(tm.Apply(emptyExpr(), tm.Select(tm.Ident(exception), el.getName("printStackTrace")), emptyExpr())),
                        tm.Return(null)
                ));
            } else {
                invocationBlock = tm.Block(0, asJavacList(
                        tm.Return(
                                tm.TypeCast(
                                        mm.getReturnValue(),
                                        tm.Apply(
                                                emptyExpr(),
                                                tm.Select(tm.Ident(outerForIterable.name), el.getName("invoke")),
                                                javacList(invParams)
                                        )
                                )
                        )
                ));
                catchBlock = tm.Block(0, asJavacList(
                        tm.Exec(tm.Apply(emptyExpr(), tm.Select(tm.Ident(exception), el.getName("printStackTrace")), emptyExpr())),
                        tm.Return(
                                tm.Literal(TypeTag.BOT, null)
                        )
                ));
            }

            stats.add(tm.ForeachLoop(
                    outerForIterable,
                    tm.Ident(candidates),
                    tm.Block(0, asJavacList(
                            tm.If(
                                    tm.Parens(generateAssignabilityCheck(outerForIterable, mm.getNumParameters(), args)),
                                    tm.Block(0, asJavacList(
                                            tm.Try(
                                                    invocationBlock,
                                                    asJavacList(tm.Catch(
                                                            tm.VarDef(tm.Modifiers(0), exception, tm.Ident(el.getName("Exception")), null),
                                                            catchBlock
                                                    )),
                                                    null

                                            )
                                    )),
                                    null
                            )
                    ))
            ));


            List<JCTree.JCExpression> stringFormatParams = new ArrayList<>();
            stringFormatParams.add(tm.Literal(
                    TypeTag.CLASS,
                    "No matching original method for name '" +
                            mm.getName().toString() +
                            "' and parameters " +
                            String.join(", ", Collections.nCopies(mm.getNumParameters(), "%s"))
                    )
            );
            stringFormatParams.addAll(args.stream().map((arg) ->
                    tm.Apply(emptyExpr(), tm.Select(tm.Ident(arg.getName()), el.getName("getClass")), emptyExpr())
            ).collect(Collectors.toList()));

            // if there was no match:   throw new RuntimeException("no match")
            stats.add(tm.Throw(tm.NewClass(
                    null,
                    emptyExpr(),
                    tm.Ident(el.getName("RuntimeException")),
                    asJavacList(tm.Apply(
                            emptyExpr(),
                            tm.Select(tm.Ident(el.getName("String")), el.getName("format")),
                            javacList(stringFormatParams)
                    )),
                    null
            )));

            dispatcher.body = tm.Block(0, javacList(stats));
            dispatchers.put(mm, dispatcher);
        }

        return dispatchers;
    }

    /*
     * generate a chain of Binary() classes that look like
     *     m.getParameterTypes()[0].isAssignableFrom(arg0.getClass()) &&
     *     m.getParameterTypes()[1].isAssignableFrom(arg1.getClass()) &&
     *     m.getParameterTypes()[2].isAssignableFrom(arg2.getClass())
     *
     * they are built recursively from the innermost check outwards
     * */
    private JCTree.JCExpression generateAssignabilityCheck(JCTree.JCVariableDecl candidate, int n, List<JCTree.JCVariableDecl> args) {

        JCTree.JCExpression curr = tm.Apply(
                emptyExpr(),
                // m.getParameterTypes()[n-1].isAssignableFrom
                tm.Select(
                        tm.Indexed(
                                tm.Apply(
                                        emptyExpr(),
                                        tm.Select(tm.Ident(candidate.getName()), el.getName("getParameterTypes")),
                                        emptyExpr()
                                ),
                                tm.Literal(TypeTag.INT, n - 1)
                        ),
                        el.getName("isAssignableFrom")
                ),
                // arg_i.getClass
                asJavacList(tm.Apply(
                        emptyExpr(),
                        tm.Select(
                                tm.Ident(args.get(n - 1).getName()),
                                el.getName("getClass")
                        ),
                        emptyExpr()
                ))
        );

        if (n == 1) {
            return curr;
        } else {
            return tm.Binary(JCTree.Tag.AND, generateAssignabilityCheck(candidate, n - 1, args), curr);
        }
    }
}
