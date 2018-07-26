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
public class SpeedTest {

    public static void main(String[] args) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("Test1.java"));
            String str = Generator.generateTestClass("Test1", 3, 5, 10, 10, 3, 3);
            writer.write(str);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
