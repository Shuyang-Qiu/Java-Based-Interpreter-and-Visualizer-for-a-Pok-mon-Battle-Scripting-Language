import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InterpreterVisitor extends ASTVisitor {
    private static class RuntimeScope {
        RuntimeScope parent;
        Map<String, Object> values = new LinkedHashMap<>();

        RuntimeScope(RuntimeScope parent) {
            this.parent = parent;
        }
    }

    private static class PokemonValue {
        String variableName;
        String source;
        String displayName;
        String type;
        int hp;
        int attack;
        int defend;

        PokemonValue(String variableName, String source) {
            this.variableName = variableName;
            this.source = source;
            this.displayName = variableName;
            this.type = "Pokemon";
            this.hp = 100;
            this.attack = 25;
            this.defend = 10;
        }
    }

    private static class ReturnSignal extends RuntimeException {
        Object value;

        ReturnSignal(Object value) {
            this.value = value;
        }
    }

    private static class BreakSignal extends RuntimeException {
    }

    private static class ContinueSignal extends RuntimeException {
    }

    private RuntimeScope currentScope = null;
    private final Map<String, FunctionDeclaration> functions = new LinkedHashMap<>();
    private final Deque<String> spellDatabases = new ArrayDeque<>();
    private final List<BattleEvent> battleEvents = new ArrayList<>();

    public List<BattleEvent> getBattleEvents() {
        return battleEvents;
    }

    private void enterScope() {
        currentScope = new RuntimeScope(currentScope);
    }

    private void exitScope() {
        if (currentScope != null) {
            currentScope = currentScope.parent;
        }
    }

    private void define(String name, Object value) {
        currentScope.values.put(name, value);
    }

    private RuntimeScope findScope(String name) {
        RuntimeScope scope = currentScope;
        while (scope != null) {
            if (scope.values.containsKey(name)) {
                return scope;
            }
            scope = scope.parent;
        }
        return null;
    }

    private Object getVariable(String name) {
        RuntimeScope scope = findScope(name);
        if (scope == null) {
            throw new RuntimeException("Runtime Error: Undefined variable '" + name + "'.");
        }
        return scope.values.get(name);
    }

    private void assignVariable(String name, Object value) {
        RuntimeScope scope = findScope(name);
        if (scope == null) {
            throw new RuntimeException("Runtime Error: Undefined variable '" + name + "'.");
        }
        scope.values.put(name, value);
    }

    @Override
    public void visitProgramNode(ProgramNode node) {
        enterScope();

        for (Statement statement : node.statements) {
            if (statement instanceof FunctionDeclaration) {
                FunctionDeclaration function = (FunctionDeclaration) statement;
                functions.put(function.name, function);
            }
        }

        System.out.println();
        System.out.println("===== Interpreter Output =====");
        battleEvents.add(BattleEvent.text("===== Interpreter Output ====="));
        for (Statement statement : node.statements) {
            if (!(statement instanceof FunctionDeclaration)) {
                statement.accept(this);
            }
        }
        System.out.println("===== End Interpreter Output =====");
        battleEvents.add(BattleEvent.text("===== End Interpreter Output ====="));

        exitScope();
    }

    @Override
    public void visitVariableDeclaration(VariableDeclaration node) {
        Object value = evaluate(node.initializer);
        define(node.name, value);
    }

    @Override
    public void visitAssignmentStatement(AssignmentStatement node) {
        Object value = evaluate(node.value);

        if (node.target instanceof IdentifierExpression) {
            IdentifierExpression target = (IdentifierExpression) node.target;
            Object newValue = value;
            if (node.operator.equals("+=") || node.operator.equals("-=")) {
                Object oldValue = getVariable(target.name);
                newValue = applyBinary(oldValue, node.operator.substring(0, 1), value);
            }
            assignVariable(target.name, newValue);
            return;
        }

        if (node.target instanceof FieldAccessExpression) {
            FieldAccessExpression target = (FieldAccessExpression) node.target;
            PokemonValue pokemon = requirePokemon(evaluate(target.object));
            int newValue = toInt(value);
            if (node.operator.equals("+=") || node.operator.equals("-=")) {
                int oldValue = toInt(getPokemonField(pokemon, target.field));
                int delta = toInt(value);
                newValue = node.operator.equals("+=") ? oldValue + delta : oldValue - delta;
            }
            setPokemonField(pokemon, target.field, newValue);
        }
    }

    @Override
    public void visitPokemonLoadStatement(PokemonLoadStatement node) {
        PokemonValue pokemon = new PokemonValue(node.name, stripQuotes(node.source));
        define(node.name, pokemon);
        String message = "Loaded Pokemon " + node.name + " from " + quoteForDisplay(pokemon.source)
                + " with hp=" + pokemon.hp + ", attack=" + pokemon.attack + ", defend=" + pokemon.defend + ".";
        System.out.println(message);
        battleEvents.add(BattleEvent.pokemonLoaded(node.name, pokemon.source, pokemon.hp, pokemon.attack, pokemon.defend));
    }

    @Override
    public void visitSpellDatabaseStatement(SpellDatabaseStatement node) {
        String databaseFile = stripQuotes(node.databaseFile);
        spellDatabases.push(databaseFile);
        String message = "Loaded spell database from " + quoteForDisplay(databaseFile) + ".";
        System.out.println(message);
        battleEvents.add(BattleEvent.text(message));
    }

    @Override
    public void visitShowStatement(ShowStatement node) {
        PokemonValue pokemon = requirePokemon(getVariable(node.name));
        String message = node.name + " status: hp=" + pokemon.hp
                + ", attack=" + pokemon.attack
                + ", defend=" + pokemon.defend
                + ", source=" + quoteForDisplay(pokemon.source) + ".";
        System.out.println(message);
        battleEvents.add(BattleEvent.text(message));
    }

    @Override
    public void visitPrintStatement(PrintStatement node) {
        String message = formatValue(evaluate(node.expression));
        System.out.println(message);
        battleEvents.add(BattleEvent.text(message));
    }

    @Override
    public void visitSpellStatement(SpellStatement node) {
        PokemonValue attacker = requirePokemon(getVariable(node.attacker));
        PokemonValue target = requirePokemon(getVariable(node.target));

        int baseDamage = movePower(node.moveKeyword);
        int damage = Math.max(1, attacker.attack + baseDamage - target.defend);
        int beforeHp = target.hp;
        target.hp = target.hp - damage;

        String moveMessage = node.attacker + " uses " + node.moveKeyword + " on " + node.target + ".";
        System.out.println(moveMessage);
        if (!spellDatabases.isEmpty()) {
            String databaseMessage = "The move is resolved using spell database " + quoteForDisplay(spellDatabases.peek()) + ".";
            System.out.println(databaseMessage);
            battleEvents.add(BattleEvent.text(databaseMessage));
        }
        String damageMessage = node.target + " takes " + damage + " damage: hp " + beforeHp + " -> " + target.hp + ".";
        System.out.println(damageMessage);
        battleEvents.add(BattleEvent.spell(node.attacker, node.target, node.moveKeyword, damage, beforeHp, target.hp));
        if (target.hp <= 0) {
            String faintMessage = node.target + " has fainted.";
            System.out.println(faintMessage);
        }
    }

    @Override
    public void visitBlockStatement(BlockStatement node) {
        executeBlock(node.statements, true);
    }

    @Override
    public void visitIfStatement(IfStatement node) {
        if (toBoolean(evaluate(node.condition))) {
            node.thenBlock.accept(this);
            return;
        }

        if (node.elseIfParts != null) {
            for (ElseIfPart part : node.elseIfParts) {
                if (toBoolean(evaluate(part.condition))) {
                    part.block.accept(this);
                    return;
                }
            }
        }

        if (node.elseBlock != null) {
            node.elseBlock.accept(this);
        }
    }

    @Override
    public void visitWhileStatement(WhileStatement node) {
        while (toBoolean(evaluate(node.condition))) {
            try {
                node.block.accept(this);
            } catch (ContinueSignal signal) {
                continue;
            } catch (BreakSignal signal) {
                break;
            }
        }
    }

    @Override
    public void visitForStatement(ForStatement node) {
        enterScope();
        try {
            node.init.accept(this);
            while (toBoolean(evaluate(node.condition))) {
                try {
                    node.block.accept(this);
                } catch (ContinueSignal signal) {
                    // Continue to the update step.
                } catch (BreakSignal signal) {
                    break;
                }
                node.update.accept(this);
            }
        } finally {
            exitScope();
        }
    }

    @Override
    public void visitFunctionDeclaration(FunctionDeclaration node) {
        functions.put(node.name, node);
    }

    @Override
    public void visitFunctionCallStatement(FunctionCallStatement node) {
        evaluate(node.call);
    }

    @Override
    public void visitBreakStatement(BreakStatement node) {
        throw new BreakSignal();
    }

    @Override
    public void visitContinueStatement(ContinueStatement node) {
        throw new ContinueSignal();
    }

    @Override
    public void visitReturnStatement(ReturnStatement node) {
        throw new ReturnSignal(evaluate(node.value));
    }

    private void executeBlock(List<Statement> statements, boolean createScope) {
        if (createScope) {
            enterScope();
        }
        try {
            for (Statement statement : statements) {
                statement.accept(this);
            }
        } finally {
            if (createScope) {
                exitScope();
            }
        }
    }

    private Object evaluate(Expression node) {
        if (node instanceof LiteralExpression) {
            return evaluateLiteral((LiteralExpression) node);
        }
        if (node instanceof IdentifierExpression) {
            return getVariable(((IdentifierExpression) node).name);
        }
        if (node instanceof FieldAccessExpression) {
            FieldAccessExpression field = (FieldAccessExpression) node;
            return getPokemonField(requirePokemon(evaluate(field.object)), field.field);
        }
        if (node instanceof UnaryExpression) {
            UnaryExpression unary = (UnaryExpression) node;
            Object right = evaluate(unary.right);
            if (unary.operator.equals("!")) return !toBoolean(right);
            if (unary.operator.equals("-")) return negate(right);
            if (unary.operator.equals("+")) return right;
        }
        if (node instanceof BinaryExpression) {
            BinaryExpression binary = (BinaryExpression) node;
            return applyBinary(evaluate(binary.left), binary.operator, evaluate(binary.right));
        }
        if (node instanceof FunctionCallExpression) {
            return callFunction((FunctionCallExpression) node);
        }
        throw new RuntimeException("Runtime Error: Unsupported expression " + node + ".");
    }

    private Object evaluateLiteral(LiteralExpression node) {
        String value = node.value;
        if (value.equals("true")) return true;
        if (value.equals("false")) return false;
        if (value.startsWith("\"") && value.endsWith("\"")) return stripQuotes(value);
        if (value.contains(".")) return Double.parseDouble(value);
        return Integer.parseInt(value);
    }

    private Object callFunction(FunctionCallExpression node) {
        FunctionDeclaration function = functions.get(node.name);
        if (function == null) {
            throw new RuntimeException("Runtime Error: Undefined function '" + node.name + "'.");
        }

        enterScope();
        try {
            for (int i = 0; i < function.parameters.size(); i++) {
                define(function.parameters.get(i).name, evaluate(node.arguments.get(i)));
            }

            try {
                executeBlock(function.body.statements, false);
            } catch (ReturnSignal signal) {
                return signal.value;
            }

            return null;
        } finally {
            exitScope();
        }
    }

    private Object applyBinary(Object left, String operator, Object right) {
        if (operator.equals("+") && (left instanceof String || right instanceof String)) {
            return formatValue(left) + formatValue(right);
        }

        if (operator.equals("+") || operator.equals("-") || operator.equals("*") || operator.equals("/")) {
            return numericBinary(left, operator, right);
        }

        if (operator.equals(">")) return toDouble(left) > toDouble(right);
        if (operator.equals("<")) return toDouble(left) < toDouble(right);
        if (operator.equals(">=")) return toDouble(left) >= toDouble(right);
        if (operator.equals("<=")) return toDouble(left) <= toDouble(right);
        if (operator.equals("==")) return valuesEqual(left, right);
        if (operator.equals("!=")) return !valuesEqual(left, right);
        if (operator.equals("&&")) return toBoolean(left) && toBoolean(right);
        if (operator.equals("||")) return toBoolean(left) || toBoolean(right);

        throw new RuntimeException("Runtime Error: Unsupported operator '" + operator + "'.");
    }

    private Object numericBinary(Object left, String operator, Object right) {
        boolean floatResult = left instanceof Double || right instanceof Double;
        double l = toDouble(left);
        double r = toDouble(right);
        double result;

        if (operator.equals("+")) result = l + r;
        else if (operator.equals("-")) result = l - r;
        else if (operator.equals("*")) result = l * r;
        else result = l / r;

        if (floatResult || operator.equals("/")) {
            return result;
        }
        return (int) result;
    }

    private Object negate(Object value) {
        if (value instanceof Double) {
            return -((Double) value);
        }
        return -toInt(value);
    }

    private boolean valuesEqual(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return Double.compare(toDouble(left), toDouble(right)) == 0;
        }
        return left == null ? right == null : left.equals(right);
    }

    private PokemonValue requirePokemon(Object value) {
        if (value instanceof PokemonValue) {
            return (PokemonValue) value;
        }
        throw new RuntimeException("Runtime Error: Expected Pokemon value.");
    }

    private Object getPokemonField(PokemonValue pokemon, String field) {
        if (field.equals("hp")) return pokemon.hp;
        if (field.equals("attack")) return pokemon.attack;
        if (field.equals("defend")) return pokemon.defend;
        if (field.equals("name")) return pokemon.displayName;
        if (field.equals("type")) return pokemon.type;
        throw new RuntimeException("Runtime Error: Unknown Pokemon field '." + field + "'.");
    }

    private void setPokemonField(PokemonValue pokemon, String field, int value) {
        if (field.equals("hp")) pokemon.hp = value;
        else if (field.equals("attack")) pokemon.attack = value;
        else if (field.equals("defend")) pokemon.defend = value;
        else throw new RuntimeException("Runtime Error: Unknown Pokemon field '." + field + "'.");
    }

    private int movePower(String moveKeyword) {
        if (moveKeyword.equals("move1")) return 8;
        if (moveKeyword.equals("move2")) return 12;
        if (moveKeyword.equals("move3")) return 16;
        return 20;
    }

    private boolean toBoolean(Object value) {
        return (Boolean) value;
    }

    private int toInt(Object value) {
        if (value instanceof Integer) return (Integer) value;
        return ((Double) value).intValue();
    }

    private double toDouble(Object value) {
        if (value instanceof Integer) return (Integer) value;
        return (Double) value;
    }

    private String stripQuotes(String value) {
        if (value != null && value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String quoteForDisplay(String value) {
        return "\"" + value + "\"";
    }

    private String formatValue(Object value) {
        if (value instanceof PokemonValue) {
            PokemonValue pokemon = (PokemonValue) value;
            return pokemon.variableName + "(hp=" + pokemon.hp
                    + ", attack=" + pokemon.attack
                    + ", defend=" + pokemon.defend + ")";
        }
        if (value instanceof Double) {
            double number = (Double) value;
            if (number == Math.rint(number)) {
                return String.valueOf((int) number);
            }
        }
        return String.valueOf(value);
    }
}
