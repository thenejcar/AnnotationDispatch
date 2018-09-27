package si.kisek.annotationdispatchtesting;

import si.kisek.annotationdispatchtesting.generator.Generator;
import si.kisek.annotationdispatchtesting.model.MethodInstance;
import si.kisek.annotationdispatchtesting.model.MethodModel;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/*
* Measure the difference in time if takes to dispatch calls for each of the three dispatch mechanisms
* */
public class GenerateTestCases {

    private static final String GENERATED_PATH = "generated-tests/src/main/java/";


    private static void write(String filename, String content) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            writer.write(content);
            writer.flush();
            writer.close();
            System.out.println("    wrote " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        // number of total calls = nMulitmethods * nCallsEach

        // testing the number of parameters per method
        System.out.println("Generating test: Parameters");
        for (int i : new int[] {1, 2, 3, 5, 10, 15, 20}) {
            String str = Generator.generateTestClass("Parameters" + i, 1, 5, 100, i, 3, 3, 3, false);
            write(GENERATED_PATH + "Parameters" + i + ".java", str);
        }

        // testing the number of classes available -- don't overdo this, or we get COMPILATION ERROR: too many constants
        System.out.println("Generating test: Classes");
        for (int i : new int[] {1, 2, 3, 4, 5, 8, 10}) {
            String str = Generator.generateTestClass("Classes" + i, 1, 5, 100, 5, 3, i, 3, false);
            write(GENERATED_PATH + "Classes" + i + ".java", str);
        }

        System.out.println("Generating test: ClassesWidth");
        for (int i : new int[] {1, 2, 3, 4, 5, 8, 10}) {
            String str = Generator.generateTestClass("ClassesWidth" + i, 1, 5, 100, 5, 3,  4, i, false);
            write(GENERATED_PATH + "ClassesWidth" + i + ".java", str);
        }

        // testing the number of multimethods
        System.out.println("Generating test: Methods");
        for (int i : new int[] {1, 2, 4, 5, 10, 20, 25}) {
            String str = Generator.generateTestClass("Methods" + i, i, 5, (100 / i), 5, 3, 3, 3, false);
            write(GENERATED_PATH + "Methods" + i + ".java", str);
        }

        // testing the number of instances
        System.out.println("Generating test: Instances");
        for (int i : new int[] {1, 2, 4, 5, 10, 20, 25}) {
            String str = Generator.generateTestClass("Instances" + i, 1, i, 100, 5, 3, 3, 3, false);
            write(GENERATED_PATH + "Instances" + i + ".java", str);
        }



        // testing parameters with void method
        System.out.println("Generating test: ParametersVoid");
        for (int i : new int[] {1, 2, 3, 5, 10, 15, 20}) {
            String str = Generator.generateTestClass("ParametersVoid" + i, 1, 5, 100, i, 3, 3, 3, true);
            write(GENERATED_PATH + "ParametersVoid" + i + ".java", str);
        }

        // testing the number of classes available -- don't overdo this, or we get COMPILATION ERROR: too many constants
        System.out.println("Generating test: ClassesVoid");
        for (int i : new int[] {1, 2, 3, 4, 5, 8, 10}) {
            String str = Generator.generateTestClass("ClassesVoid" + i, 1, 5, 100, 5, 3, i, 3, true);
            write(GENERATED_PATH + "ClassesVoid" + i + ".java", str);
        }


        // testing the number of multimethods
        System.out.println("Generating test: MethodsVoid");
        for (int i : new int[] {1, 2, 4, 5, 10, 20, 25}) {
            String str = Generator.generateTestClass("MethodsVoid" + i, i, 5, (100 / i), 5, 3, 3, 3, true);
            write(GENERATED_PATH + "MethodsVoid" + i + ".java", str);
        }

        // testing the number of instances
        System.out.println("Generating test: InstancesVoid");
        for (int i : new int[] {1, 2, 4, 5, 10, 20, 25}) {
            String str = Generator.generateTestClass("InstancesVoid" + i, 1, i, 100, 5, 3, 3, 3, true);
            write(GENERATED_PATH + "InstancesVoid" + i + ".java", str);
        }
    }

}
