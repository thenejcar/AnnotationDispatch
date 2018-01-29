package si.kisek.annotationdispatch;


@MultiDispatchClass_Demo
public class TestClass {

    @ExampleAnnotation(changeTo = "Vrednost v anotaciji")
    private static String value = "To je privzeta vrednost spremenljivke value.";


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

    public static void main(String[] args) {
        System.out.println("TestClass starting...");

        System.out.println(value);

        Nekej n = new Nekej();
        Nekej t = new NekajDrugega();

        // single dispatch -- default java behavior
        n.metoda();
        t.metoda();

        // multip[le dispatch -- needs the annotations on method declarations
        kdoJeParameter(n);
        kdoJeParameter(t);


    }


    @MultiDispatch_Demo
    static String kdoJeParameter(Nekej n) {
        String description = "parameter je nekej drugega";
        System.out.println(description);
        return description;
    }

    @MultiDispatch_Demo
    static String kdoJeParameter(NekajDrugega t) {
        String description = "parameter je nekaj drugega";
        System.out.println(description);
        return description;
    }
/* something like this will be generated by the processor */
    public static String ifinstanceof(Object arg1) {
        if (arg1 instanceof Nekej) {
            if (arg1 instanceof NekajDrugega)
                return kdoJeParameter((NekajDrugega) arg1);
            else
                return kdoJeParameter((Nekej) arg1);
        }

        throw new RuntimeException("No method definition for runtime argument of type " + arg1.getClass());
    }

}
