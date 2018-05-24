package si.kisek.annotationdispatchdemo;

import si.kisek.annotationdispatch.MultiDispatch;
import si.kisek.annotationdispatch.MultiDispatchClass;

@MultiDispatchClass
public class AnotherClass {


    static class Nekej {
        void metoda() {
            System.out.println("jaz sem nekej");
        }
    }

    static class NekajDrugega extends Nekej {
        @Override
        void metoda() {
            System.out.println("jaz sem nekaj drugega");
        }
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

        // multip[le dispatch -- needs the annotations on method declarations
        System.out.print("n1: ");
        kdoJeParameter(n1);
        System.out.print("n2: ");
        kdoJeParameter(n2);
        System.out.print("n3: ");
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
