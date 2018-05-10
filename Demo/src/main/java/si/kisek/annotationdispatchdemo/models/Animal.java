package si.kisek.annotationdispatchdemo.models;


import si.kisek.annotationdispatch.MultiDispatchVisitable;
import si.kisek.annotationdispatchdemo.TestClass;

@MultiDispatchVisitable
public class Animal {
    String describe() {
        return "Animal";
    }
}
