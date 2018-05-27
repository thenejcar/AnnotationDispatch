package si.kisek.annotationdispatchdemo;


import si.kisek.annotationdispatch.MultiDispatch;
import si.kisek.annotationdispatch.MultiDispatchClass;
import si.kisek.annotationdispatch.MultiDispatchVisitable;
import java.lang.RuntimeException;
//import si.kisek.annotationdispatchdemo.models.*;

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


    @MultiDispatchVisitable
    public static class Animal {
        public String describe() {
            return "Animal";
        }
    }

    @MultiDispatchVisitable
    public static class Apple extends Tree {
        @Override
        public String whichTree() {
            return "an apple tree";
        }
    }

    @MultiDispatchVisitable
    public static class Cat extends Mammal {
        @Override
        public String describe() {
            return "Cat";
        }
    }

    @MultiDispatchVisitable
    public static class Dog extends Mammal {
        @Override
        public String describe() {
            return "Dog";
        }
    }

    @MultiDispatchVisitable
    public static class Lizard extends Reptile {
        @Override
        public String describe() {
            return "Lizard";
        }
    }

    @MultiDispatchVisitable
    public static class Mammal extends Animal {
        @Override
        public String describe() {
            return "Mammal";
        }
    }

    @MultiDispatchVisitable
    public static class Oak extends Tree {
        @Override
        public String whichTree() {
            return "an oak tree";
        }
    }

    @MultiDispatchVisitable
    public static class Pine extends Tree {
        @Override
        public String whichTree() {
            return "a pine tree";
        }
    }

    @MultiDispatchVisitable
    public static class Reptile extends Animal {
        @Override
        public String describe() {
            return "Reptile";
        }
    }

    @MultiDispatchVisitable
    public static class Tree {
        public String whichTree() {
            return "a tree";
        }
    }
}
