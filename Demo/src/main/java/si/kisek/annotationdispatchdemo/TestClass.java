package si.kisek.annotationdispatchdemo;


import si.kisek.annotationdispatch.MultiDispatch;
import si.kisek.annotationdispatch.MultiDispatchClass;

@MultiDispatchClass
public class TestClass {

    static void testMethod(String arg1) {
        System.out.println("***** test method called *****");
        AnotherClass.doSomething();
    }

    public class TestInterface {
        public Object testInterfaceMethod1(String a, Object b) {
            return "Return Value";
        }
    }

    public class InlineClassTest extends TestInterface {

    }

    static class Zival {
        String describe() {
            return "Zival";
        }
    }

    static class Sesalec extends Zival {
        @Override
        String describe() {
            return "Sesalec";
        }
    }

    static class Macek extends Sesalec {
        @Override
        String describe() {
            return "Macek";
        }
    }

    static class Pes extends Sesalec {
        @Override
        String describe() {
            return "Pes";
        }
    }

    static class Mis extends Sesalec {
        @Override
        String describe() {
            return "Mis";
        }
    }

    static class Siamec extends Macek {
        @Override
        String describe() {
            return "Siamec";
        }
    }

    static class Plazilec extends Zival {
        @Override
        String describe() {
            return "Plazilec";
        }
    }

    static class Kaca extends Plazilec {
        @Override
        String describe() {
            return "Kaca";
        }
    }

    static class Piton extends Kaca {
        @Override
        String describe() {
            return "Piton";
        }
    }

    static class MaineCoon extends Macek {
        @Override
        String describe() {
            return "MaineCoon";
        }
    }

    static class Drevo {
        String describe() {
            return "Drevo";
        }
    }

    static class Listavec extends Drevo {
        @Override
        String describe() {
            return "Listavec";
        }
    }

    static class Hrast extends Listavec {
        @Override
        String describe() {
            return "Hrast";
        }
    }

    static class Lipa extends Listavec {
        @Override
        String describe() {
            return "Lipa";
        }
    }

    static class Iglavec extends Drevo {
        @Override
        String describe() {
            return "Iglavec";
        }
    }

    static class Smreka extends Iglavec {
        @Override
        String describe() {
            return "Smreka";
        }
    }

    public static void main(String[] args) {

        AnotherClass.doSomething();

        Zival zival = new Zival();
        Zival sesalec = new Sesalec();
        Zival macek = new Macek();
        Zival siamec = new Siamec();
        Zival maineCoon = new MaineCoon();
        Zival pes = new Pes();
        Zival mis = new Mis();
        Zival plazilec = new Plazilec();
        Zival kaca = new Kaca();
        Zival piton = new Piton();

        Drevo drevo = new Drevo();
        Drevo listavec = new Listavec();
        Drevo hrast = new Hrast();
        Drevo lipa = new Lipa();
        Drevo iglavec = new Iglavec();
        Drevo smreka = new Smreka();

        pleza(macek, drevo);
        pleza(zival, drevo);
        pleza(maineCoon, hrast);
        pleza(macek, lipa);
        pleza(piton, drevo);
        pleza(mis, hrast);
        pleza(pes, smreka);
        pleza(kaca, lipa);
        pleza(sesalec, iglavec);
        pleza(siamec, listavec);
        pleza(plazilec, smreka);
    }

    @MultiDispatch
    static void pleza(Siamec o1, Hrast o2) {
        System.out.println(o1.describe() + " pleza na " + o2.describe() + " = Siamec pleza na Hrast");
    }

    @MultiDispatch
    static void pleza(Siamec o1, Lipa o2) {
        System.out.println(o1.describe() + " pleza na " + o2.describe() + " = Siamec pleza na Lipo");
    }

    @MultiDispatch
    static void pleza(MaineCoon o1, Listavec o2) {
        System.out.println(o1.describe() + " pleza na " + o2.describe() + " = Maine Coon pleza na Listavec");
    }

    @MultiDispatch
    static void pleza(Macek o1, Lipa o2) {
        System.out.println(o1.describe() + " pleza na " + o2.describe() + " = Macek pleza na Lipo");
    }

    @MultiDispatch
    static void pleza(Macek o1, Hrast o2) {
        System.out.println(o1.describe() + " pleza na " + o2.describe() + " = Macek pleza na Hrast");
    }

    @MultiDispatch
    static void pleza(Macek o1, Drevo o2) {
        System.out.println(o1.describe() + " pleza na " + o2.describe() + " = Macek pleza na Drevo");
    }

    @MultiDispatch
    static void pleza(Pes o1, Hrast o2) {
        System.out.println(o1.describe() + " pleza na " + o2.describe() + " = Pes pleza na Hrast");
    }

    @MultiDispatch
    static void pleza(Mis o1, Drevo o2) {
        System.out.println(o1.describe() + " pleza na " + o2.describe() + " = Mis pleza na Drevo");
    }

    @MultiDispatch
    static void pleza(Sesalec o1, Drevo o2) {
        System.out.println(o1.describe() + " pleza na " + o2.describe() + " = Sesalec pleza na Drevo");
    }

    @MultiDispatch
    static void pleza(Piton o1, Drevo o2) {
        System.out.println(o1.describe() + " pleza na " + o2.describe() + " = Piton pleza na Drevo");
    }

    @MultiDispatch
    static void pleza(Plazilec o1, Lipa o2) {
        System.out.println(o1.describe() + " pleza na " + o2.describe() + " = Plazilec pleza na Lipo");
    }

    @MultiDispatch
    static void pleza(Zival o1, Drevo o2) {
        System.out.println(o1.describe() + " pleza na " + o2.describe() + " = Zival pleza na Drevo");
    }

    public static void plezanjeSwitcher(Object o1, Object o2) {
        System.out.println("describe switcher: " + o1.getClass() + " " + o2.getClass());

        if (o1 instanceof Siamec) {
            if (o2 instanceof Lipa) {
                pleza((Siamec) o1, (Lipa) o2);
                return;
            }
            if (o2 instanceof Hrast) {
                pleza((Siamec) o1, (Hrast) o2);
                // else -- metode ni bilo mogoce najti
                // Ce obstaja kaksna varianta, kjer o1 ni Macek, jo bomo ujeli v naslednjih ifih
                // sicer metoda ne obstaja in vrzi exception
                return;
            }
        }
        if (o1 instanceof Macek) {
            if (o2 instanceof Hrast) {
                pleza((Macek) o1, (Hrast) o2);
                return;
            }
            if (o2 instanceof Lipa) {
                pleza((Macek) o1, (Lipa) o2);
                return;
            }
            if (o2 instanceof Drevo) {
                pleza((Macek) o1, (Drevo) o2);
                return;
            }

        }

        throw new RuntimeException("No such method");

    }
}
