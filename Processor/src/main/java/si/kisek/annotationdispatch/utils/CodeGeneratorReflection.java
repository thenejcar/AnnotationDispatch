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

                tryBlock.add(tm.Exec(tm.App(
                        tm.Select(tm.Ident(tmpListName), el.getName("add")),
                        asJavacList(
                                tm.App(
                                        tm.Select(
                                                tm.App(
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
            tryBlock.add(tm.Exec(tm.App(
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
                    tm.App(
                            tm.Select(tm.Ident(el.getName(methodMapName())), el.getName("get")),
                            asJavacList(tm.Literal(TypeTag.CLASS, mm.getName().toString()))
                    )
            ));

            // map.put(123, list)
            tryBlock.add(tm.Exec(tm.App(
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
                                    tm.Exec(tm.App(tm.Select(tm.Ident(el.getName("e")), el.getName("printStackTrace")), emptyExpr()))
                            ))
                    )),
                    null));
        }

        // set stats as the body of the init method
        initMethod.body = tm.Block(0, javacList(stats));
        return initMethod;
    }


    public Map<MethodModel, JCTree.JCMethodDecl> generateDispatchers() {
        return null;
    }
}
