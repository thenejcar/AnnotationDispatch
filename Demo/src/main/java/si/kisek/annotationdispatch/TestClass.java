package si.kisek.annotationdispatch;


public class TestClass {

    @ExampleAnnotation(changeTo = "Vrednost v anotaciji")
    private static String value = "To je privzeta vrednost spremenljivke value.";


    static class Nekej {

        void metoda() {
            System.out.println("js sem nekaj");
        }
    }

    static class Tralaala extends Nekej {
        @Override
        void metoda() {
            System.out.println("js sem tralala");
        }
    }

    public static void main(String[] args) {
        System.out.println("TestClass starting...");

        System.out.println(value);

        Nekej n = new Nekej();
        Nekej t = new Tralaala();

        n.metoda();
        t.metoda();


        kdoJeParameter(n);
        kdoJeParameter(t);


    }


    static void kdoJeParameter(Nekej n) {
        System.out.println("not je n");
    }

    static void kdoJeParameter(Tralaala t) {
        System.out.println("not je t");
    }
}
