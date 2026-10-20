import java.util.ArrayList;
import java.util.List;

public class Symbol {
    String name;
    String type;
    String kind;
    List<String> parameterTypes;
    String returnType;

    public Symbol(String name, String type, String kind) {
        this.name = name;
        this.type = type;
        this.kind = kind;
        this.parameterTypes = new ArrayList<>();
        this.returnType = type;
    }

    public Symbol(String name, String kind, String returnType, List<String> parameterTypes) {
        this.name = name;
        this.type = returnType;
        this.kind = kind;
        this.returnType = returnType;
        this.parameterTypes = new ArrayList<>(parameterTypes);
    }

    @Override
    public String toString() {
        if ("function".equals(kind)) {
            return kind + " " + name + parameterTypes + " -> " + returnType;
        }
        return kind + " " + type + " " + name;
    }
}
