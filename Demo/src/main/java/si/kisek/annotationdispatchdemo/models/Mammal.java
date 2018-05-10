package si.kisek.annotationdispatchdemo.models;

import si.kisek.annotationdispatch.MultiDispatchVisitable;

@MultiDispatchVisitable
public class Mammal extends Animal {
    @Override
    String describe() {
        return "Mammal";
    }
}
