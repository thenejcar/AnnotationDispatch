package si.kisek.annotationdispatchdemo.models;

import si.kisek.annotationdispatch.MultiDispatchVisitable;

//@MultiDispatchVisitable
public class Lizard extends Reptile{
    @Override
    public String describe() {
        return "Lizard";
    }
}
