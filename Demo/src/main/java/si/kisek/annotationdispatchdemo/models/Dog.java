package si.kisek.annotationdispatchdemo.models;

import si.kisek.annotationdispatch.MultiDispatchVisitable;

@MultiDispatchVisitable
public class Dog extends Mammal {
    @Override
    String describe() {
        return "Dog";
    }
}
