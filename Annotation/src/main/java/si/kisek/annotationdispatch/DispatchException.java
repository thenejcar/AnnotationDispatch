package si.kisek.annotationdispatch;

public class DispatchException extends RuntimeException{

    public DispatchException(String message) {
        super(message);
    }

    public DispatchException(String name, Object... objects) {
        super(getDescription(name, objects));
    }
    public DispatchException(String name, Class... classes) {
        super(getDescription(name, classes));
    }

    private static String getDescription(String name, Object... objects) {
        Class[] classes = new Class[objects.length];
        for (int i = 0; i < objects.length; i++) classes[i] = objects[i].getClass();
        return getDescription(name, classes);
    }


    private static String getDescription(String name, Class... classes) {
        StringBuilder sb = new StringBuilder();
        sb.append("No method with name ").append(name).append(" and parameters ");
        for (Class c : classes) {
            sb.append(String.valueOf(c));
        }
        sb.append(" is available.");
        return sb.toString();
    }
}
