package si.kisek.annotationdispatchdemo.models;

import si.kisek.annotationdispatch.MultiDispatchVisitable;

//@MultiDispatchVisitable
public class Pine extends Tree {
    @Override
    public String whichTree(){
        return "a pine tree";
    }
}
