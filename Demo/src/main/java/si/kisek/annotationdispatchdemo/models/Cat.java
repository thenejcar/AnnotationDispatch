package si.kisek.annotationdispatchdemo.models;

import si.kisek.annotationdispatch.MultiDispatchVisitable;

@MultiDispatchVisitable
public class Cat extends Mammal{
    @Override
    public String describe() {
        return "Cat";
    }
}
