package si.kisek.annotationdispatchdemo;

import si.kisek.annotationdispatch.MultiDispatch;
import si.kisek.annotationdispatch.MultiDispatchClass;
import si.kisek.annotationdispatch.MultiDispatchVisitable;

@MultiDispatchClass
public class DemoIzBesedila {


    public static void main(String[] args) {

        Zival arg0 = new Macek();
        Zival arg1 = new Macek();
        Zival arg2 = new Ptica();
        metoda(arg0, arg1, arg2);
    }


    @MultiDispatchVisitable
    static class Zival {}
    @MultiDispatchVisitable
    static class Sesalec extends Zival {}
    @MultiDispatchVisitable
    static class Ptica extends Zival {}
    @MultiDispatchVisitable
    static class Macek extends Sesalec {}
    @MultiDispatchVisitable
    static class Pes extends Sesalec {}

    @MultiDispatch
    static void metoda(Macek arg0, Pes arg1, Ptica arg2) {
        System.out.println("Klicali so se Macek, Pes in Ptica");
    }
    @MultiDispatch
    static void metoda(Sesalec arg0, Sesalec arg1, Sesalec arg2) {
        System.out.println("Klicali so se Sesalec, Sesalec in Sesalec");
    }
    @MultiDispatch
    static void metoda(Sesalec arg0, Zival arg1, Zival arg2) {
        System.out.println("Klicali so se Sesalec, Zival in Zival");
    }
    @MultiDispatch
    static void metoda(Zival arg0, Zival arg1, Zival arg2) {
        System.out.println("Klicali so se Zival, Zival in Zival");
    }
}
