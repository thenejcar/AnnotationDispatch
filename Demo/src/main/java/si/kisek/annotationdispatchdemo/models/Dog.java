package si.kisek.annotationdispatchdemo.models;

import si.kisek.annotationdispatch.MultiDispatchVisitable;

@MultiDispatchVisitable
public class Dog extends Mammal {
    @Override
    public String describe() {
        return "Dog";
    }
}
