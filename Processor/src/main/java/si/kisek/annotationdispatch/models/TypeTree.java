package si.kisek.annotationdispatch.models;

import com.sun.tools.javac.code.Type;

import javax.lang.model.util.Types;
import java.util.*;

public class TypeTree {

    private class Node {
        public Type type;
        public Node parent;
        public List<Node> children;

        public Node(Type type, Node parent, List<Node> children) {
            this.type = type;
            this.parent = parent;
            this.children = children;
        }
    }

    private Types types;
    public Node root;
    public Set<Type> typeSet;

    public TypeTree(Types types) {
        this.root = new Node(null, null, new ArrayList<>());
        this.types = types;
        this.typeSet = new HashSet<>();
    }

    /*
    * add type to the correct position in a type tree
    * */
    public void addType(Type t) {
        if (!typeSet.contains(t)) {
            typeSet.add(t);
            // recursively add the type to the tree
            insertType(t, root, root.children);
        }
        // else already sorted, skip
    }

    /*
     * inserts a type into a subtree of a specified Node (or doesn't insert if Type already exists in the tree)
     * returns the node for this type
     */
    private Node insertType(Type t, Node parent, List<Node> children) {
        ListIterator<Node> childIterator = children.listIterator();
        while (childIterator.hasNext()) {
            Node c = childIterator.next();

            if (types.isSubtype(t, c.type)) {
                // recurse into child's tree
                return insertType(t, c, c.children);
            } else if (types.isSubtype(c.type, t)) {
                List<Node> l = new ArrayList<>();
                l.add(c);
                Node newNode = new Node(t, parent, l);
                c.parent = newNode;
                childIterator.remove();
                childIterator.add(newNode);

                // check if any other root is also a subtype and connect it to newNode
                while (childIterator.hasNext()) {
                    c = childIterator.next();
                    if (types.isSubtype(c.type, t)) {
                        // add this one to the newNode's subtree
                        c.parent = newNode;
                        newNode.children.add(c);
                        childIterator.remove();
                    }
                }
                return newNode;
            }
        }
        // we haven't inserted a node yet, add it on this level
        Node newNode = new Node(t, parent, new ArrayList<>());
        children.add(newNode);
        return newNode;
    }


    /*
     * flatten the tree using dfs
     * */
    public ArrayList<Type> flatten() {
        return flatten(root);
    }

    private ArrayList<Type> flatten(Node n) {
        ArrayList<Type> l = new ArrayList<>();
        for (Node c : n.children) {
            l.addAll(flatten(c));
        }
        if (n.type != null) {
            l.add(n.type);
        }
        return l;
    }

}
