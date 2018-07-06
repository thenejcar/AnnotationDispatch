package si.kisek.annotationdispatch.utils;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import si.kisek.annotationdispatch.models.MethodInstance;
import si.kisek.annotationdispatch.models.MethodModel;
import si.kisek.annotationdispatch.models.MethodSwitcher;

import javax.swing.text.html.HTML;
import java.util.*;
import java.util.stream.Collectors;

import static si.kisek.annotationdispatch.utils.Utils.asJavacList;
import static si.kisek.annotationdispatch.utils.Utils.emptyExpr;
import static si.kisek.annotationdispatch.utils.Utils.javacList;

public class CodeGeneratorReflection {
    private TreeMaker tm;
    private JavacElements el;
    private JavacTypes types;
    private Symtab symtab;
    private Map<MethodModel, Set<MethodInstance>> originalMethods;
    private List<MethodInstance> sortedInstances;
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

    public JCTree.JCVariableDecl generateMethodMapDecl() {

        // Map<String, Map<Integer, List<Method>>> methodMap_xxxx
        return tm.VarDef(
                tm.Modifiers(Flags.PRIVATE),
                el.getName(methodMapName()),
                tm.TypeApply(
                        tm.Ident(el.getName("java.util.Map")),
                        asJavacList(
                                tm.Ident(el.getName("String")),
                                tm.TypeApply(
                                        tm.Ident(el.getName("java.util.Map")),
                                        asJavacList(
                                                tm.Ident(el.getName("Integer")),
                                                tm.TypeApply(
                                                        tm.Ident(el.getName("java.util.List")),
                                                        javacList(Collections.singletonList(tm.Ident(el.getName("java.lang.reflect.Method"))))
                                                )
                                        )
                                )
                        )
                ),
                null // init is done in the initMethod
        );
    }

    public JCTree.JCMethodDecl generateInitMethod() {
        JCTree.JCMethodDecl initMethod = tm.MethodDef(
                tm.Modifiers(Flags.PRIVATE),
                el.getName("init_" + randomness),
                null, // TODO: void
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
                        tm.TypeApply(tm.Ident(el.getName("HashMap")), emptyExpr()),
                        emptyExpr(),
                        null
                )
        )));

        for (MethodModel mm : originalMethods.keySet()) {
            // for each MM generate a try-catch block that adds the Methods to the methodMap

            List<JCTree.JCStatement> tryBlock = new ArrayList<>();

            Name tmpListName = el.getName("list");
            // create a list of methods
            JCTree.JCVariableDecl list = tm.VarDef(
                    tm.Modifiers(0),
                    tmpListName,
                    tm.TypeApply(tm.Ident(el.getName("java.util.List")), asJavacList(tm.Ident(el.getName("java.lang.reflect.Method")))),
                    tm.NewClass(null, emptyExpr(), tm.Ident(el.getName("java.util.ArrayList")), emptyExpr(), null)
            );
            tryBlock.add(list);

            List<MethodInstance> sortedInstances = new MethodSwitcher(types, mm, originalMethods.get(mm)).getSortedInstances();

            for (MethodInstance instance : sortedInstances) {
                // for each instance generate a call that looks like
                //    methodIsntances.add(this.getClass().getMethod("mm_name", param1, param2, param3))

                List<JCTree.JCExpression> callParameters = new ArrayList<>();
                callParameters.add(tm.Literal(TypeTag.CLASS, mm.getName().toString()));
                callParameters.addAll(
                        instance.getParameters().stream().map((Type t) ->
                                tm.Select(tm.Ident(t.tsym.name), el.getName("class"))
                        ).collect(Collectors.toList())
                );

                tryBlock.add(tm.Exec(tm.Apply(
                        emptyExpr(),
                        tm.Select(tm.Ident(tmpListName), el.getName("add")),
                        asJavacList(
                                tm.Apply(
                                        emptyExpr(),
                                        tm.Select(
                                                tm.Apply(
                                                        emptyExpr(),
                                                        tm.Select(
                                                                tm.Ident(el.getName("this")),
                                                                el.getName("getClass")
                                                        ),
                                                        emptyExpr()
                                                ),
                                                el.getName("getMethod")
                                        ),
                                        javacList(callParameters)
                                )
                        )
                )));
            }

            // add the list to the map with 3 statements

            // methodMap.putIfAbsent("mm_name", new HashMap<>())
            tryBlock.add(tm.Exec(tm.Apply(
                    emptyExpr(),
                    tm.Select(tm.Ident(el.getName(methodMapName())), el.getName("putIfAbsent")),
                    asJavacList(
                            tm.Literal(TypeTag.CLASS, mm.getName().toString()),
                            tm.NewClass(null, emptyExpr(), tm.Ident(el.getName("java.util.HashMap")), emptyExpr(), null)
                    )
            )));

            Name tmpMapName = el.getName("map");
            // Map<Integer, List<Method> map = methodMap.get("mm_name")
            tryBlock.add(tm.VarDef(
                    tm.Modifiers(0),
                    tmpMapName,
                    tm.TypeApply(
                            tm.Ident(el.getName("java.util.Map")),
                            asJavacList(
                                    tm.Ident(el.getName("Integer")),
                                    tm.TypeApply(
                                            tm.Ident(el.getName("java.util.List")),
                                            javacList(Collections.singletonList(tm.Ident(el.getName("java.lang.reflect.Method"))))
                                    )
                            )
                    ),
                    tm.Apply(
                            emptyExpr(),
                            tm.Select(tm.Ident(el.getName(methodMapName())), el.getName("get")),
                            asJavacList(tm.Literal(TypeTag.CLASS, mm.getName().toString()))
                    )
            ));

            // map.put(123, list)
            tryBlock.add(tm.Exec(tm.Apply(
                    emptyExpr(),
                    tm.Select(tm.Ident(tmpMapName), el.getName("put")),
                    asJavacList(
                            tm.Literal(TypeTag.INT, mm.getNumParameters()),
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


    public Map<MethodModel, JCTree.JCMethodDecl> generateDispatchers() {
        JCTree.JCVariableDecl[] params = new JCTree.JCVariableDecl[2];
        params[0] = tm.VarDef(
                tm.Modifiers(0),
                el.getName("methodName"),
                tm.Ident(el.getName("String")),
                null
        );
        params[0] = tm.VarDef(
                tm.Modifiers(0),
                el.getName("args"),
                tm.TypeArray(
                        tm.Ident(el.getName("Object"))
                ),
                null
        );
        JCTree.JCMethodDecl dispatcher = tm.MethodDef(
                tm.Modifiers(Flags.PRIVATE),
                el.getName("dispatch_" + randomness),
                null,
                com.sun.tools.javac.util.List.from(new JCTree.JCTypeParameter[0]),
                javacList(params),
                com.sun.tools.javac.util.List.from(new JCTree.JCExpression[0]),    // no exception throwing
                null,                               // body added later
                null                           // no default value
        );

        Name argsToMethod = el.getName("argsToMethod");

        List<JCTree.JCStatement> stats = new ArrayList<>();

        // Map<Integer, List<Method>> argsToMethod = methodMap.get(methodName);
        stats.add(tm.VarDef(
                tm.Modifiers(0),
                argsToMethod,
                tm.TypeApply(
                        tm.Ident(el.getName("java.util.Map")),
                        asJavacList(
                                tm.Ident(el.getName("Integer")),
                                tm.TypeApply(
                                        tm.Ident(el.getName("java.util.List")),
                                        javacList(Collections.singletonList(tm.Ident(el.getName("java.lang.reflect.Method"))))
                                )
                        )
                ),
                tm.Apply(
                        emptyExpr(),
                        tm.Select(tm.Ident(el.getName(methodMapName())), el.getName("get")),
                        asJavacList(tm.Literal(TypeTag.CLASS, tm.Ident(params[0].getName())))
                )
        ));
/*
        // throw error if argsToMethod is null. We can skip this, really.
        stats.add(tm.If(
                tm.Parens(tm.Binary(JCTree.Tag.EQ, tm.Ident(argsToMethod), tm.Literal(TypeTag.BOT, null))),
                tm.Block(0, asJavacList(tm.Throw(tm.NewClass(
                        null,
                        emptyExpr(),
                        tm.Ident(el.getName("RuntimeException")),
                        asJavacList(tm.Literal(TypeTag.CLASS, "argsToMethod map not initialised, internal processor error")),
                        null
                )))),
                null
        ));
*/

        // get list of possible methods
        // List<Method> candidates = argsToMethod.get(args.length)
        Name candidates = el.getName("candidates");
        stats.add(tm.VarDef(
                tm.Modifiers(0),
                candidates,
                tm.TypeApply(tm.Ident(el.getName("java.util.List")), asJavacList(tm.Ident(el.getName("java.lang.reflect.Method")))),
                tm.Apply(
                        emptyExpr(),
                        tm.Select(tm.Ident(argsToMethod), el.getName("get")),
                        asJavacList(tm.Select(tm.Ident(params[1].name), el.getName("length")))
                )
        ));

        // if (candidates == null) throw new RuntimeException("Dispatching parameters failed: " + methodName + numParameters)
        stats.add(tm.If(
                tm.Parens(tm.Binary(JCTree.Tag.EQ, tm.Ident(candidates), tm.Literal(TypeTag.BOT, null))),
                tm.Block(0, asJavacList(tm.Throw(tm.NewClass(
                        null,
                        emptyExpr(),
                        tm.Ident(el.getName("RuntimeException")),
                        asJavacList(tm.Binary(
                                JCTree.Tag.PLUS,
                                tm.Binary(
                                    JCTree.Tag.PLUS,
                                    tm.Literal(TypeTag.CLASS, "Dispatching parameters failed: "),
                                    tm.Ident(params[0].getName())
                        ),
                                tm.Select(tm.Ident(params[1].getName()), el.getName("length"))
                        )),
                        null
                )))),
                null
        ));

        // Method bestMatch = null;
        Name bestMatch = el.getName("bestMatch");
        stats.add(tm.VarDef(
                tm.Modifiers(0),
                bestMatch,
                tm.Ident(el.getName("java.lang.reflect.Method")),
                tm.Literal(TypeTag.BOT, null)
        ));

        // for each candidate, check if we get a match
        // they are already sorted, so the first match is best match
        // something like this:
        // for (Method m : candidates)
        //     for (int i=0; i<args.length; i++)
        //         if (m.getParameterTypes()[i].isAssignableFrom(args[i].getClass()))
        //             bestMatch = m; break;
        JCTree.JCVariableDecl outerForIterable = tm.VarDef(
                tm.Modifiers(0),
                el.getName("m"),
                tm.Ident(el.getName("java.lang.reflect.Method")),
                null
        );
        JCTree.JCVariableDecl innerForIndex = tm.VarDef(
                tm.Modifiers(0),
                el.getName("i"),
                tm.TypeIdent(TypeTag.INT),
                tm.Literal(TypeTag.INT, 0)
        );

        JCTree.JCIf innermostIf = tm.If(
                tm.Parens(tm.Apply(
                        emptyExpr(),
                        // m.getParameterTypes()[i].isAssignableFrom
                        tm.Select(
                                tm.Indexed(
                                    tm.Apply(
                                            emptyExpr(),
                                            tm.Select(tm.Ident(outerForIterable.getName()), el.getName("getParameterTypes")),
                                            emptyExpr()
                                    ),
                                    tm.Ident(innerForIndex.getName())
                                ),
                                el.getName("isAssignableFrom")
                        ),
                        // args[i].getClass
                        asJavacList(tm.Apply(
                                emptyExpr(),
                                tm.Select(
                                        tm.Indexed(params[1].sym, tm.Ident(innerForIndex.getName())),
                                        el.getName("getClass")
                                ),
                                emptyExpr()
                        ))
                )),
                tm.Block(0, asJavacList(
                        tm.Exec(tm.Assign(tm.Ident(bestMatch), tm.Ident(outerForIterable))),
                        tm.Break(null)
                )),
                null
        );
        stats.add(tm.ForeachLoop(
                outerForIterable,
                tm.Ident(candidates),
                tm.Block(0, asJavacList(
                        tm.ForLoop(
                                asJavacList(innerForIndex),
                                tm.Binary(JCTree.Tag.LT, tm.Ident(innerForIndex.getName()), tm.Select(tm.Ident(params[1].getName()), el.getName("length"))),
                                asJavacList(tm.Exec(tm.Unary(JCTree.Tag.POSTINC, tm.Ident(innerForIndex.getName())))),
                                tm.Block(0, asJavacList(innermostIf))
                        ),
                        tm.If(
                                tm.Parens(tm.Binary(JCTree.Tag.NE, tm.Ident(bestMatch), tm.Literal(TypeTag.BOT, null))),
                                tm.Block(0, asJavacList(tm.Break(null))),
                                null
                        )
                ))
        ));

        // if (bestMatch == null) throw new RuntimeException("no match")
        stats.add(tm.If(
                tm.Parens(tm.Binary(JCTree.Tag.EQ, tm.Ident(bestMatch), tm.Literal(TypeTag.BOT, null))),
                tm.Block(0, asJavacList(tm.Throw(tm.NewClass(
                        null,
                        emptyExpr(),
                        tm.Ident(el.getName("RuntimeException")),
                        asJavacList(tm.Literal(TypeTag.CLASS, "no matching original method to dispatch the call to")),
                        null
                )))),                null
        ));

        // TODO: Object[] parameters = new Object[bestMatch.getParameterCount()];

        return null;
    }
}
