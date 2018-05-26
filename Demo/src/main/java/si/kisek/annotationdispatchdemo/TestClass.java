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

        climbing(cat, dog, tree);
        climbing(animal, animal, tree);
        climbing(cat, cat, oakTree);
        climbing(lizard, dog, appleTree);
        climbing(dog, lizard, pineTree);
        climbing(lizard, dog, oakTree);

    }

    @MultiDispatch
    static void climbing(Animal o1, Animal o2, Tree o3) {
        System.out.println(String.format("called: Animal and Animal climbing on a tree         | real: %s and %s climbing on %s",
                o1.describe(), o2.describe(), o3.whichTree()));
    }

    @MultiDispatch
    static void climbing(Mammal o1, Reptile o2, Tree o3) {
        System.out.println(String.format("called: Mammal and Reptile climbing on a tree        | real: %s and %s climbing on %s",
                o1.describe(), o2.describe(), o3.whichTree()));
    }

    @MultiDispatch
    static void climbing(Cat o1, Cat o2, Oak o3) {
        System.out.println(String.format("called: Cat and Cat climbing on an oak tree          | real: %s and %s climbing on %s",
                o1.describe(), o2.describe(), o3.whichTree()));
    }

    @MultiDispatch
    static void climbing(Reptile o1, Mammal o2, Apple o3) {
        System.out.println(String.format("called: Reptile and Mammal climbing on an apple tree | real: %s and %s climbing on %s",
                o1.describe(), o2.describe(), o3.whichTree()));
    }

}
