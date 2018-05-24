package si.kisek.annotationdispatchdemo;


import si.kisek.annotationdispatch.MultiDispatch;
import si.kisek.annotationdispatch.MultiDispatchClass;
import si.kisek.annotationdispatchdemo.models.*;

@MultiDispatchClass
public class TestClass {

    public static void main(String[] args) {

        AnotherClass.doSomething();

        Animal animal = new Animal();
        Animal mammal = new Mammal();
        Animal cat = new Cat();
        Animal dog = new Dog();
        Animal reptile = new Reptile();
        Animal lizard = new Lizard();

        Tree tree = new Tree();
        Tree oakTree = new Oak();
        Tree pineTree = new Pine();
        Tree appleTree = new Apple();

        climbing(cat, tree);
        climbing(animal, tree);
        climbing(cat, oakTree);
        climbing(lizard, appleTree);
        climbing(dog, pineTree);
        climbing(lizard, oakTree);

    }

    @MultiDispatch
    static void climbing(Animal o1, Tree o2) {
        System.out.println(String.format("called: Animal climbing on a tree         | real: %s climbing on %s", o1.describe(), o2.whichTree()));
    }

    @MultiDispatch
    static void climbing(Mammal o1, Tree o2) {
        System.out.println(String.format("called: Mammal climbing on a tree         | real: %s climbing on %s", o1.describe(), o2.whichTree()));
    }

    @MultiDispatch
    static void climbing(Cat o1, Oak o2) {
        System.out.println(String.format("called: Cat climbing on an oak tree       | real: %s climbing on %s", o1.describe(), o2.whichTree()));
    }

    @MultiDispatch
    static void climbing(Reptile o1, Apple o2) {
        System.out.println(String.format("called: Reptile climbing on an apple tree | real: %s climbing on %s", o1.describe(), o2.whichTree()));
    }

}
