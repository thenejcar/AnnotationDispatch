package si.kisek.annotationdispatch.models;

import com.sun.tools.javac.code.Type;

import javax.lang.model.util.Types;
import java.util.*;
import java.util.stream.Collectors;

public class MethodSwitcher {


    public class SwitcherNode {
        public Type parentType;
        public int level;
        public List<SwitcherNode> subtree;
        public MethodInstance leaf;

        public SwitcherNode(Type parentType, int level, List<SwitcherNode> subtree, MethodInstance leaf) {
            this.parentType = parentType; // type that lead to this branch
            this.level = level;           // switch depth / parameter number that we will be switching by
            this.subtree = subtree;       // subtrees that we will be switching into
            this.leaf = leaf;             // if we have determined all paremeters, we have a leaf instead of a subtree
        }
    }

    private MethodModel methodModel;
    private Types types;
    private SwitcherNode root;
    private List<MethodInstance> sortedInstances = null;

    /*
     * build a method switcher from given method instances
     * */
    public MethodSwitcher(Types types, MethodModel mm, Set<MethodInstance> instances) {
        this.types = types;
        this.methodModel = mm;

        root = buildTree(null, 0, instances);
    }

    private SwitcherNode buildTree(Type parentType, int argPos, Set<MethodInstance> instances) {
        if (methodModel.numParameters > argPos) {
            // this is not the last parameter, recursively build subtrees from sorted nodes
            HashMap<Type, Set<MethodInstance>> methods = new HashMap<>();
            // separate method instances based on parameter type at 'argPos'
            for (MethodInstance mi : instances) {
                Type t = mi.getParameters().get(argPos);
                if (!methods.containsKey(t)) {
                    methods.put(t, new HashSet<>());
                }
                methods.get(t).add(mi);
            }

            // sort possible types using a TypeTree
            TypeTree typeTree = new TypeTree(types);
            methods.keySet().forEach(typeTree::addType);

            // build a subtree for each
            List<SwitcherNode> subtrees = typeTree.flatten().stream().map(t ->
                            buildTree(t, argPos + 1, methods.get(t))
                    ).collect(Collectors.toList());

            return new SwitcherNode(parentType, argPos, subtrees, null);
        } else {
            // this is the last parameter, return the one remaining methodInstance instead of a subtree
            if (instances.size() > 1)
                throw new RuntimeException("Something went wrong, there are multiple methods with same signature");

            return new SwitcherNode(parentType, argPos, null, instances.iterator().next());
        }
    }

    public SwitcherNode getRoot() {
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
    private void dfs(SwitcherNode node) {
        if (node.subtree != null)
            for (SwitcherNode child : node.subtree) {
                dfs(child);
            }
        if (node.leaf != null)
            sortedInstances.add(node.leaf);
    }
}
