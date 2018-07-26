package si.kisek.annotationdispatchtesting.model;

import com.sun.tools.javac.code.Type;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MethodInstanceTree {

    private class Node {
        public GeneratedClass type;
        public int level;
        public List<Node> subtree;
        public MethodInstance leaf;

        public Node(GeneratedClass parentType, int level, List<Node> subtree, MethodInstance leaf) {
            this.type = type; // type that lead to this branch
            this.level = level;           // switch depth / parameter number that we will be switching by
            this.subtree = subtree;       // subtrees that we will be switching into
            this.leaf = leaf;             // if we have determined all paremeters, we have a leaf instead of a subtree
        }
    }

    private MethodModel mm;
    private GeneratedClass types;
    private Node root;
    private List<MethodInstance> sortedInstances = null;
    private List<List<GeneratedClass>> flatTypes;

    public MethodInstanceTree(MethodModel mm, Set<MethodInstance> instances, List<List<GeneratedClass>> flatTypes) {
        this.mm = mm;
        this.flatTypes = flatTypes;
        this.root = buildTree(null, 0, instances);
    }


    private Node buildTree(GeneratedClass parentType, int argPos, Set<MethodInstance> instances) {
        if (mm.getNumParameters() > argPos) {
            // this is not the last parameter, recursively build subtrees from sorted nodes
            HashMap<GeneratedClass, Set<MethodInstance>> methods = new HashMap<>();
            // separate method instances based on parameter type at 'argPos'
            for (MethodInstance mi : instances) {
                GeneratedClass t = mi.getParameters().get(argPos);
                if (!methods.containsKey(t)) {
                    methods.put(t, new HashSet<>());
                }
                methods.get(t).add(mi);
            }

            // get the type tree for the current parameter
            List<GeneratedClass> argTypes = flatTypes.get(argPos);

            List<Node> subtrees = new ArrayList<>();
            for (GeneratedClass t : argTypes) {
                Set<MethodInstance> recInstances = methods.getOrDefault(t, null);
                if (recInstances != null && recInstances.size() > 0) {
                    subtrees.add(buildTree(t, argPos + 1, recInstances));
                }
            }
            return new Node(parentType, argPos, subtrees, null);
        } else {
            // this is the last parameter, return the one remaining methodInstance instead of a subtree
            if (instances.size() > 1)
                throw new RuntimeException("Something went wrong, there are multiple methods with same signature");

            return new Node(parentType, argPos, null, instances.iterator().next());
        }
    }

    public Node getRoot() {
        return root;
    }

    public List<MethodInstance> getSortedInstances() {
        // sorted instances are lazy, because we don't need them every time we use the method switcher
        if (this.sortedInstances == null) {
            this.sortedInstances = new ArrayList<>();
            dfs(this.root);
        }

        return this.sortedInstances;
    }

    // sort the method instances by listing the leaves with dfs
    private void dfs(Node node) {
        if (node.subtree != null)
            for (Node child : node.subtree) {
                dfs(child);
            }
        if (node.leaf != null)
            sortedInstances.add(node.leaf);
    }
}
