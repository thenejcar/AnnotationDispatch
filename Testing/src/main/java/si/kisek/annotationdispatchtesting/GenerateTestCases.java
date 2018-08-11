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

    private static final String GENERATED_PATH = "speedtest/src/main/java/";


    public static void main(String[] args) {

        // number of total calls = nMulitmethods * nCallsEach

        // testing the number of parameters per method
        System.out.println("Generating test: Parameters");
        for (int i : new int[] {1, 2, 3, 5, 10, 15, 20}) {
            String str = Generator.generateTestClass("Parameters" + i, 1, 5, 100, i, 3, 3, false);

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(GENERATED_PATH + "Parameters" + i + ".java"));
                writer.write(str);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // testing parameters with void method
        System.out.println("Generating test: ParametersVoid");
        for (int i : new int[] {1, 2, 3, 5, 10, 15, 20}) {
            String str = Generator.generateTestClass("ParametersVoid" + i, 1, 5, 100, i, 3, 3, true);

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(GENERATED_PATH + "ParametersVoid" + i + ".java"));
                writer.write(str);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // testing the number of classes available
        System.out.println("Generating test: Classes");
        for (int i : new int[] {1, 2, 3, 4, 5, 8, 10}) {
            String str = Generator.generateTestClass("Classes" + i, 1, 5, 100, 5, i, 3, false);

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(GENERATED_PATH + "Classes" + i + ".java"));
                writer.write(str);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // testing the number of multimethods
        System.out.println("Generating test: Methods");
        for (int i : new int[] {1, 2, 4, 5, 10, 20, 25}) {
            String str = Generator.generateTestClass("Methods" + i, i, 5, (100 / i), 5, 3, 3, false);

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(GENERATED_PATH + "Methods" + i + ".java"));
                writer.write(str);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

}
