package si.kisek.annotationdispatchtesting.generator;

import jdk.internal.org.objectweb.asm.tree.MethodInsnNode;
import si.kisek.annotationdispatchtesting.model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/*
 * Generate test cases for multiple dispatch
 * */
public class Generator {

    public static String generateTestClass(String className, int nMultimethods, int nInstancesEach, int nCallsEach, int nParameters, int classesRoots, int classesDepth, int classesWidth, boolean isVoid) {

        Map<MethodModel, List<MethodInstance>> methodMap = IntStream.range(0, nMultimethods).mapToObj(i ->
                new MethodModel("m" + "_" + i + "_" + UUID.randomUUID().toString().replace("-", "").substring(4, 8), "public static", nParameters, isVoid)
        ).collect(Collectors.toMap(
                mm -> mm,
                mm -> generateMethods(mm, nInstancesEach, nCallsEach, classesDepth, classesDepth, classesWidth)
        ));

        List<String> parameterClasses = methodMap.keySet().stream().flatMap(mm -> mm.getClasses().stream().map(gc ->
                "    @MultiDispatchVisitable\n" + gc.getCode()
        )).sorted().collect(Collectors.toList());

        List<String> parameterObjectsInit = methodMap.keySet().stream().flatMap(mm -> mm.getInitCode().stream())
                .sorted().collect(Collectors.toList());

        List<String> testCalls = methodMap.values().stream().flatMap(Collection::stream).flatMap(mi ->
                mi.getExampleCalls().stream().map(x -> "        " + x)
        ).sorted().collect(Collectors.toList());

        List<String> methodDefinitions = methodMap.values().stream().flatMap(Collection::stream).map(mi ->
                "    @MultiDispatch\n" + "    " + mi.getCode()
        ).sorted().collect(Collectors.toList());

        return "" +
                "import si.kisek.annotationdispatch.MultiDispatch;\n" +
                "import si.kisek.annotationdispatch.MultiDispatchClass;\n" +
                "import si.kisek.annotationdispatch.MultiDispatchVisitable;\n" +
                "\n" +
                "import java.util.*;\n" +
                "\n" +
                "@MultiDispatchClass\n" +
                "public class " + className + " {\n" +
                "\n" +
                "    // Parameter classes\n" +
                String.join("\n", parameterClasses) +
                "\n" +
                "    // Method definitions\n" +
                String.join("\n", methodDefinitions) +
                "\n" +
                "\n" +
                "    public static String globalString = \"default\";\n" +
                "\n" +
                "    public static void main(String[] args) {\n" +
                "\n" +
                "        // Parameter objects\n" +
                String.join("\n", parameterObjectsInit) +
                "\n\n" +
                "        // Test calls\n" +
                "        long t = System.nanoTime();\n" +
                "        for (int i=0; i<200000; i++) {\n" +
                String.join("\n", testCalls) +
                "\n\n" +
                "        }\n" +
                "        t = System.nanoTime() - t;\n" +
                "        System.out.println(t);\n" +
                "    }\n" +
                "\n" +
                "\n}\n";


    }

    private static <T> List<T> nFromList(List<T> list, int n) {

        List<T> result = new ArrayList<>();
        Random r = new Random();
        while (result.size() < n)
            result.add(list.get(r.nextInt(list.size())));

        return result;
    }

    public static List<MethodInstance> generateMethods(MethodModel mm, int nMethods, int nCalls, int classesRoots, int classesDepth, int classesWidth) {
        List<ClassTree> allClasses = generateNames(classesRoots, 'A', 'Z').stream().map(name ->
                ClassTree.generateWidthDepth(name + "_" + mm.getName(), classesDepth, classesWidth)
        ).collect(Collectors.toList());

        List<ClassTree> parameterClasses = nFromList(allClasses, mm.getNumParameters());

        final List<List<GeneratedClass>> flatParameterClasses = parameterClasses.stream().map(ClassTree::flatten).collect(Collectors.toList());
        mm.addClasses(flatParameterClasses.stream().flatMap(Collection::stream).collect(Collectors.toList()));

        final Random r = new Random();

        Set<MethodInstance> methodInstances = IntStream.range(1, nMethods).mapToObj(i ->
                // pick random classes for parameters and generate a method instance
                generateMethodInstance(mm, flatParameterClasses.stream().map(flatList -> flatList.get(r.nextInt(flatList.size()))).collect(Collectors.toList()))
        ).collect(Collectors.toSet());

        // add another method with root classes (so tht we don't have to doublecheck when calling the methods)
        methodInstances.add(generateMethodInstance(mm, parameterClasses.stream().map(ClassTree::getGenClass).collect(Collectors.toList())));

        // sort the generated methodInstances, most specific parameter type s first
        // (mostly copied from MethodSwitcher from annotation processor)
        List<MethodInstance> sortedMethods = new MethodInstanceTree(mm, methodInstances, flatParameterClasses).getSortedInstances();

        Map<MethodInstance, List<List<GeneratedClass>>> exampleCalls = new HashMap<>();

        // generate calls for the generated methods
        int numGenerated = 0;
        while (numGenerated < nCalls) {
            List<GeneratedClass> randomParams = flatParameterClasses.stream().map(flatList -> flatList.get(r.nextInt(flatList.size()))).collect(Collectors.toList());

            for (MethodInstance mi : sortedMethods) {
                boolean allOk = true;
                for (int i = 0; i < mm.getNumParameters(); i++) {
                    if (!parameterClasses.get(i).isSupertype(mi.getParameters().get(i), randomParams.get(i))) {
                        allOk = false;
                        break;
                    }
                }
                if (allOk) {
                    exampleCalls.putIfAbsent(mi, new ArrayList<>());
                    exampleCalls.get(mi).add(randomParams);
                    numGenerated++;
                    break;
                }
            }
        }

        // generate Objects that will be used as parameters, only use types that are actually used
        exampleCalls.values().stream().flatMap(x -> x.stream().flatMap(Collection::stream))
                .collect(Collectors.toSet())
                .forEach(gc -> {
                    String objectName = gc.getName().toLowerCase();
                    mm.addObject(gc, objectName, gc.getRoot().getName() + " " + objectName + " = new " + gc.getName() + "();");
                });
        for (MethodInstance mi : exampleCalls.keySet()) {
            for (List<GeneratedClass> exampleCall : exampleCalls.get(mi)) {
                mi.addExampleCall(emitCode(mm.getObjects(), mi, exampleCall));
            }
        }

        return sortedMethods;
    }

    private static MethodInstance generateMethodInstance(MethodModel mm, List<GeneratedClass> params) {
        List<String> parameters = new ArrayList<>();
        int index = 0;
        for (GeneratedClass gc : params) {
            parameters.add(gc.getName() + " arg" + index);
            index++;
        }

        String parametersHash = String.valueOf(Objects.hash(params));

        String code = mm.getModifiers() + (mm.isVoid() ? " void " : " String ") + mm.getName() + "(" + String.join(", ", parameters) + ") {" +
                (mm.isVoid() ?
                        "    globalString = \"" + parametersHash + "\";" // assign to globalString parameter
                        :
                        "    return \"" + parametersHash + "\";" // return string literal
                ) + "}";

        return new MethodInstance(mm, code, params, parametersHash, mm.isVoid());
    }

    private static String emitCode(HashMap<GeneratedClass, String> objects, MethodInstance method, List<GeneratedClass> exampleCall) {
        if (method.isVoid()) {
            return method.getMm().getName() + "(" + String.join(", ", exampleCall.stream().map(objects::get).collect(Collectors.toList())) + ");\n" +
                    "        if (!globalString.equals(\"" + method.getParametersHash() + "\"))" +
                    " System.out.println(\"Bad dispatch in call to " + method.getMm().getName() + " / " + String.join(", ", exampleCall.stream().map(Object::toString).collect(Collectors.toList())) + "\");";
        } else {
            return "if (!" +
                    method.getMm().getName() + "(" + String.join(", ", exampleCall.stream().map(objects::get).collect(Collectors.toList())) + ")" +
                    ".equals(\"" + method.getParametersHash() + "\"))" +
                    " System.out.println(\"Bad dispatch in call to " + method.getMm().getName() + " / " + String.join(", ", exampleCall.stream().map(Object::toString).collect(Collectors.toList())) + "\");";
        }
    }


    private static List<String> generateNames(int n, char from, char to) {
        List<String> result = new ArrayList<>();

        char[] name = new char[]{from};
        for (int i = 0; i < n; i++) {
            result.add(new String(name));

            int pos = name.length - 1;
            while (pos >= 0) {
                if ((int) name[pos] < (int) to)
                    break;
                name[pos] = from;
                pos = pos - 1;

            }
            if (pos >= 0) {
                name[pos] = (char) (name[pos] + 1);
            } else {
                char[] tmp = new char[name.length + 1];
                System.arraycopy(name, 0, tmp, 0, name.length);
                tmp[tmp.length - 1] = from;
                name = tmp;
            }
        }

        return result;
    }
}
