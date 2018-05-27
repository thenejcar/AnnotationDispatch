package si.kisek.annotationdispatchdemo.models;

import si.kisek.annotationdispatch.MultiDispatchVisitable;

//@MultiDispatchVisitable
public class Reptile extends Animal {
    @Override
    public String describe() {
        return "Reptile";
    }
}
