package si.kisek.annotationdispatchdemo;

import si.kisek.annotationdispatch.MultiDispatch;
import si.kisek.annotationdispatch.MultiDispatchClass;
import si.kisek.annotationdispatch.MultiDispatchVisitable;

@MultiDispatchClass
public class AnotherClass {


    @MultiDispatchVisitable
    static class Nekej {
        public AnotherClass a_field;

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

        n1.a_field = new AnotherClass();

        Nekej n2 = new NekajDrugega();
        NekajDrugega nd = new NekajDrugega();

        AnotherClass ths = new AnotherClass();

        // testing the non-static methods
        System.out.println("n1 (should be " + n1.metoda() + "): " + ths.kdoJeParameter(n1));

        System.out.println("n2 (should be " + n2.metoda() + "): " + ths.kdoJeParameter(n2));

        System.out.println("nd (should be " + nd.metoda() + "): " + ths.kdoJeParameter(nd));

        System.out.println("oba: " + ths.kdoJeParameter(n1, nd));
    }


    @MultiDispatch
    String kdoJeParameter(Nekej n) {
        String description = "parameter je nekej";
        return description;
    }

    @MultiDispatch
    String kdoJeParameter(NekajDrugega t) {
        String description = "parameter je nekaj drugega";
        return description;
    }

    String kdoJeParameter(Nekej n, NekajDrugega nd) {
        String description = "parametra sta oba";
        return description;
    }

}
