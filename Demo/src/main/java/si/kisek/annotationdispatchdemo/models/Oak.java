package si.kisek.annotationdispatchdemo.models;

import si.kisek.annotationdispatch.MultiDispatchVisitable;

//@MultiDispatchVisitable
public class Oak extends Tree {
    @Override
    public String whichTree(){
        return "an oak tree";
    }
}
