package si.kisek.annotationdispatchdemo.models;

import si.kisek.annotationdispatch.MultiDispatchVisitable;

//@MultiDispatchVisitable
public class Apple extends Tree {
    @Override
    public String whichTree(){
        return "an apple tree";
    }
}
