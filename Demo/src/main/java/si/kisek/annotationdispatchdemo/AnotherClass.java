package si.kisek.annotationdispatchdemo;

import si.kisek.annotationdispatch.MultiDispatch;
import si.kisek.annotationdispatch.MultiDispatchClass;
import si.kisek.annotationdispatch.MultiDispatchVisitable;

@MultiDispatchClass
public class AnotherClass {


    @MultiDispatchVisitable
    static class Nekej {
        String metoda() {
            return "Nekej";
        }
    }

    @MultiDispatchVisitable
    static class NekajDrugega extends Nekej {
        @Override
        String metoda() {
            return "NekajDrugega";
        }
    }

    public static void main(String[] args) {
        doSomething();
    }


    public static void doSomething() {

        Nekej n1 = new Nekej();
        Nekej n2 = new NekajDrugega();
        NekajDrugega nd = new NekajDrugega();

        // single dispatch -- default java behavior
        System.out.print("n1: ");
        n1.metoda();
        System.out.print("n2: ");
        n2.metoda();
        System.out.print("nd: ");
        nd.metoda();

        System.out.println();

        // multiple dispatch -- needs the annotations on method declarations
        System.out.print("n1 (should be " + n1.metoda() + "): ");
        kdoJeParameter(n1);
        System.out.print("n2 (should be " + n2.metoda() + "): ");
        kdoJeParameter(n2);
        System.out.print("nd (should be " + nd.metoda() + "): ");
        kdoJeParameter(nd);

        System.out.print("oba: ");
        kdoJeParameter(n1, nd);
    }


    @MultiDispatch
    static String kdoJeParameter(Nekej n) {
        String description = "parameter je nekej";
        System.out.println(description);
        return description;
    }

    @MultiDispatch
    static String kdoJeParameter(NekajDrugega t) {
        String description = "parameter je nekaj drugega";
        System.out.println(description);
        return description;
    }

    static String kdoJeParameter(Nekej n, NekajDrugega nd) {
        String description = "parametra sta oba";
        System.out.println(description);
        return description;
    }

}
