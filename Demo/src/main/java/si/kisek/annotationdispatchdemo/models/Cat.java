package si.kisek.annotationdispatchdemo.models;

import si.kisek.annotationdispatch.MultiDispatchVisitable;

@MultiDispatchVisitable
public class Cat extends Mammal{
    @Override
    String describe() {
        return "Cat";
    }
}
