package si.kisek.annotationdispatchtesting.model;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*
* A tree of classes that can be used for parameters
* */
public class ClassTree {

    private GeneratedClass genClass;
    private List<ClassTree> children;

    public ClassTree(GeneratedClass genClass, List<ClassTree> children) {
        this.genClass = genClass;
        this.children = children;
    }

    public GeneratedClass getGenClass() {
        return genClass;
    }

    public void setGenClass(GeneratedClass genClass) {
        this.genClass = genClass;
    }

    public List<ClassTree> getChildren() {
        return children;
    }

    public void setChildren(List<ClassTree> children) {
        this.children = children;
    }

    public int count() {
        if (children == null || children.size() == 0)
            return 1;
        else
            return 1 + children.stream().mapToInt(ClassTree::count).sum();
    }

    public int getDepth() {
        if (children == null || children.size() == 0)
            return 1;
        else {
            return 1 + children.stream().map(ClassTree::getDepth).max(Integer::compare).get();
        }
    }

    /*
    * get the classes with dfs, generating a sorted most-specific-first list of all classes
    * */
    public List<GeneratedClass> flatten() {
        return dfsFlatten(this);
    }

    private static List<GeneratedClass> dfsFlatten(ClassTree tree) {
        if (tree.children == null || tree.children.size() == 0) {
            return Collections.singletonList(tree.genClass);
        } else {
            List<GeneratedClass> list = tree.children.stream().flatMap(x -> dfsFlatten(x).stream()).collect(Collectors.toList());
            list.add(tree.genClass);
            return list;
        }
    }

    public static ClassTree generateWidthDepth(String prefix, int depth, int maxChildren) {
        ClassTree tree = generate(prefix, depth, maxChildren);
        while (tree.getDepth() != depth + 1) {
            System.out.println("Depth not correct, generating a new ClassTree");
            tree = generate(prefix, depth, maxChildren);
        }

        return tree;
    }

    private static ClassTree generate(String prefix, int maxDepth, int maxChildren) {
        String body = "private static class " + prefix + " {\n" +
                "    public String identify() {\n" +
                "        return \"" + prefix + "\";\n" +
                "    }\n" +
                "}";
        GeneratedClass genClass = new GeneratedClass(prefix, body, null, null);

        return new ClassTree(genClass, IntStream.range(0, maxChildren).mapToObj(i ->
                generate(
                        prefix + "_",
                        i,
                        1,
                        genClass,
                        maxDepth,
                        maxChildren,
                        genClass
                )
        ).collect(Collectors.toList()));
    }

    public static ClassTree generate(String prefix, int number, int level, GeneratedClass parent, int maxDepth, int maxChildren, GeneratedClass root) {
        String name = prefix + "_" + number;

        String body = "    public static class " + name + " extends " + parent.getName() + " {\n" +
                "        @Override\n" +
                "        public String identify() {\n" +
                "            return \"" + name + "\";\n" +
                "        }\n" +
                "    }";
        GeneratedClass genClass = new GeneratedClass(name, body, parent, root);

        if (level < maxDepth) {
            int r = new Random().nextInt(maxChildren);

            return new ClassTree(genClass, IntStream.rangeClosed(0, r).mapToObj(i ->
                    generate(
                            prefix + "_" + number,
                            i,
                            level + 1,
                            genClass,
                            maxDepth,
                            maxChildren,
                            root
                    )
            ).collect(Collectors.toList()));
        } else {
            return new ClassTree(genClass, Collections.emptyList());
        }
    }

    public boolean isSupertype(GeneratedClass superType, GeneratedClass type) {
        if (superType.equals(type)) return true;

        boolean foundSuper = false;

        Queue<ClassTree> q = new LinkedList<>();
        q.add(this);
        while (!q.isEmpty()) {
            ClassTree t = q.poll();
            if (t == null) break;

            if (!foundSuper && t.genClass.equals(superType)) {
                // only look in the superclass's subtree
                foundSuper = true;
                q.clear();
            } else if (!foundSuper && t.genClass.equals(type)) {
                // type is not under supertype
                return false;
            } else if (t.genClass.equals(type)) {
                return true;
            }


            q.addAll(t.children);
        }

        // type not found in the subtree
        return false;
    }
}
