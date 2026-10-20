import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TypeVisitor extends ASTVisitor {

    private static class TypeScope {
        TypeScope parent;
        Map<String, Symbol> symbols = new LinkedHashMap<>();

        TypeScope(TypeScope parent) {
            this.parent = parent;
        }
    }

    private TypeScope currentScope = null;
    private final List<String> errors = new ArrayList<>();
    private int loopDepth = 0;
    private String currentFunctionReturnType = null;

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<String> getErrors() {
        return errors;
    }

    private void reportError(String message) {
        errors.add(message);
        System.out.println("Type Error: " + message);
    }

    private void enterScope() {
        currentScope = new TypeScope(currentScope);
    }

    private void exitScope() {
        if (currentScope != null) {
            currentScope = currentScope.parent;
        }
    }

    private void define(Symbol symbol) {
        if (currentScope == null) {
            enterScope();
        }
        if (!currentScope.symbols.containsKey(symbol.name)) {
            currentScope.symbols.put(symbol.name, symbol);
        }
    }

    private Symbol lookup(String name) {
        TypeScope scope = currentScope;
        while (scope != null) {
            if (scope.symbols.containsKey(name)) {
                return scope.symbols.get(name);
            }
            scope = scope.parent;
        }
        return null;
    }

    private String normalizeType(String type) {
        if (type == null) return "unknown";
        if (type.equals("bool")) return "boolean";
        return type;
    }

    private boolean isNumeric(String type) {
        type = normalizeType(type);
        return type.equals("int") || type.equals("float");
    }

    private boolean isAssignable(String targetType, String valueType) {
        targetType = normalizeType(targetType);
        valueType = normalizeType(valueType);
        if (targetType.equals("error") || valueType.equals("error")) return true;
        if (targetType.equals(valueType)) return true;
        // Allow assigning an int value to a float variable.
        return targetType.equals("float") && valueType.equals("int");
    }

    private String numericResultType(String left, String right) {
        left = normalizeType(left);
        right = normalizeType(right);
        if (left.equals("float") || right.equals("float")) return "float";
        return "int";
    }

    @Override
    public void visitProgramNode(ProgramNode node) {
        enterScope();

        // First pass: make all top-level functions visible before checking calls.
        for (Statement stmt : node.statements) {
            if (stmt instanceof FunctionDeclaration) {
                FunctionDeclaration fn = (FunctionDeclaration) stmt;
                List<String> parameterTypes = new ArrayList<>();
                for (Parameter parameter : fn.parameters) {
                    parameterTypes.add(normalizeType(parameter.type));
                }
                define(new Symbol(fn.name, "function", normalizeType(fn.returnType), parameterTypes));
            }
        }

        for (Statement stmt : node.statements) {
            stmt.accept(this);
        }

        exitScope();
    }

    @Override
    public void visitVariableDeclaration(VariableDeclaration node) {
        String declaredType = normalizeType(node.type);
        if (node.initializer != null) {
            String initType = evaluate(node.initializer);
            if (!isAssignable(declaredType, initType)) {
                reportError("Cannot initialize variable '" + node.name + "' of type " + declaredType + " with value of type " + initType + ".");
            }
        }
        define(new Symbol(node.name, declaredType, "variable"));
    }

    @Override
    public void visitPokemonLoadStatement(PokemonLoadStatement node) {
        define(new Symbol(node.name, "Pokemon", "variable"));
    }

    @Override
    public void visitAssignmentStatement(AssignmentStatement node) {
        String targetType = evaluate(node.target);
        String valueType = evaluate(node.value);

        if (!(node.target instanceof IdentifierExpression) && !(node.target instanceof FieldAccessExpression)) {
            reportError("Left side of assignment must be a variable or Pokemon field.");
        }

        if (node.operator.equals("=")) {
            if (!isAssignable(targetType, valueType)) {
                reportError("Cannot assign value of type " + valueType + " to target of type " + targetType + ".");
            }
        } else if (node.operator.equals("+=") || node.operator.equals("-=")) {
            if (!isNumeric(targetType) || !isNumeric(valueType)) {
                reportError("Operator '" + node.operator + "' requires numeric target and numeric value.");
            }
        } else {
            reportError("Unknown assignment operator '" + node.operator + "'.");
        }
    }

    @Override
    public void visitSpellDatabaseStatement(SpellDatabaseStatement node) {
        // Valid statement, no type rule needed.
    }

    @Override
    public void visitShowStatement(ShowStatement node) {
        Symbol symbol = lookup(node.name);
        if (symbol == null) {
            reportError("Cannot show undefined identifier '" + node.name + "'.");
        } else if (!normalizeType(symbol.type).equals("Pokemon")) {
            reportError("show can only be used with Pokemon values, but '" + node.name + "' is " + symbol.type + ".");
        }
    }

    @Override
    public void visitPrintStatement(PrintStatement node) {
        if (node.expression != null) {
            evaluate(node.expression);
        }
    }

    @Override
    public void visitSpellStatement(SpellStatement node) {
        if (!node.moveKeyword.equals("move1") && !node.moveKeyword.equals("move2")
                && !node.moveKeyword.equals("move3") && !node.moveKeyword.equals("move4")) {
            reportError("Invalid move '" + node.moveKeyword + "'. Only move1, move2, move3, and move4 are allowed.");
        }

        Symbol attacker = lookup(node.attacker);
        if (attacker == null) {
            reportError("Undefined attacker '" + node.attacker + "'.");
        } else if (!normalizeType(attacker.type).equals("Pokemon")) {
            reportError("Spell attacker '" + node.attacker + "' must be Pokemon, but found " + attacker.type + ".");
        }

        Symbol target = lookup(node.target);
        if (target == null) {
            reportError("Undefined target '" + node.target + "'.");
        } else if (!normalizeType(target.type).equals("Pokemon")) {
            reportError("Spell target '" + node.target + "' must be Pokemon, but found " + target.type + ".");
        }
    }

    @Override
    public void visitBlockStatement(BlockStatement node) {
        enterScope();
        for (Statement stmt : node.statements) {
            stmt.accept(this);
        }
        exitScope();
    }

    @Override
    public void visitIfStatement(IfStatement node) {
        String conditionType = evaluate(node.condition);
        if (!normalizeType(conditionType).equals("boolean")) {
            reportError("if condition must be boolean, but found " + conditionType + ".");
        }

        if (node.thenBlock != null) node.thenBlock.accept(this);

        if (node.elseIfParts != null) {
            for (ElseIfPart part : node.elseIfParts) {
                String elseifType = evaluate(part.condition);
                if (!normalizeType(elseifType).equals("boolean")) {
                    reportError("elseif condition must be boolean, but found " + elseifType + ".");
                }
                if (part.block != null) part.block.accept(this);
            }
        }

        if (node.elseBlock != null) node.elseBlock.accept(this);
    }

    @Override
    public void visitWhileStatement(WhileStatement node) {
        String conditionType = evaluate(node.condition);
        if (!normalizeType(conditionType).equals("boolean")) {
            reportError("while condition must be boolean, but found " + conditionType + ".");
        }

        loopDepth++;
        if (node.block != null) node.block.accept(this);
        loopDepth--;
    }

    @Override
    public void visitForStatement(ForStatement node) {
        enterScope();

        if (node.init != null) node.init.accept(this);

        String conditionType = evaluate(node.condition);
        if (!normalizeType(conditionType).equals("boolean")) {
            reportError("for condition must be boolean, but found " + conditionType + ".");
        }

        if (node.update != null) node.update.accept(this);

        loopDepth++;
        if (node.block != null) node.block.accept(this);
        loopDepth--;

        exitScope();
    }

    @Override
    public void visitFunctionDeclaration(FunctionDeclaration node) {
        Symbol existing = lookup(node.name);
        if (existing == null || !"function".equals(existing.kind)) {
            List<String> parameterTypes = new ArrayList<>();
            for (Parameter parameter : node.parameters) {
                parameterTypes.add(normalizeType(parameter.type));
            }
            define(new Symbol(node.name, "function", normalizeType(node.returnType), parameterTypes));
        }

        enterScope();
        for (Parameter parameter : node.parameters) {
            define(new Symbol(parameter.name, normalizeType(parameter.type), "parameter"));
        }

        String previousReturnType = currentFunctionReturnType;
        currentFunctionReturnType = normalizeType(node.returnType);
        if (node.body != null) {
            // Visit the function body statements in this function scope instead of adding an extra block scope for parameters.
            for (Statement stmt : node.body.statements) {
                stmt.accept(this);
            }
        }
        currentFunctionReturnType = previousReturnType;

        exitScope();
    }

    @Override
    public void visitFunctionCallStatement(FunctionCallStatement node) {
        if (node.call != null) {
            evaluate(node.call);
        }
    }

    @Override
    public void visitBreakStatement(BreakStatement node) {
        if (loopDepth == 0) {
            reportError("break can only be used inside a loop.");
        }
    }

    @Override
    public void visitContinueStatement(ContinueStatement node) {
        if (loopDepth == 0) {
            reportError("continue can only be used inside a loop.");
        }
    }

    @Override
    public void visitReturnStatement(ReturnStatement node) {
        if (currentFunctionReturnType == null) {
            reportError("return can only be used inside a function.");
            if (node.value != null) evaluate(node.value);
            return;
        }

        if (currentFunctionReturnType.equals("void")) {
            if (node.value != null) {
                String valueType = evaluate(node.value);
                reportError("void function should not return a value of type " + valueType + ".");
            }
        } else {
            if (node.value == null) {
                reportError("Function with return type " + currentFunctionReturnType + " must return a value.");
            } else {
                String valueType = evaluate(node.value);
                if (!isAssignable(currentFunctionReturnType, valueType)) {
                    reportError("Return type mismatch: expected " + currentFunctionReturnType + " but found " + valueType + ".");
                }
            }
        }
    }

    private String evaluate(Expression node) {
        if (node == null) return "void";

        if (node instanceof LiteralExpression) {
            return evaluateLiteral((LiteralExpression) node);
        }
        if (node instanceof IdentifierExpression) {
            return evaluateIdentifier((IdentifierExpression) node);
        }
        if (node instanceof FieldAccessExpression) {
            return evaluateFieldAccess((FieldAccessExpression) node);
        }
        if (node instanceof UnaryExpression) {
            return evaluateUnary((UnaryExpression) node);
        }
        if (node instanceof BinaryExpression) {
            return evaluateBinary((BinaryExpression) node);
        }
        if (node instanceof FunctionCallExpression) {
            return evaluateFunctionCall((FunctionCallExpression) node);
        }

        reportError("Unknown expression type: " + node.getClass().getSimpleName());
        return "error";
    }

    private String evaluateLiteral(LiteralExpression node) {
        String value = node.value;
        if (value == null) return "error";
        if (value.equals("true") || value.equals("false")) return "boolean";
        if (value.startsWith("\"") && value.endsWith("\"")) return "String";
        if (value.contains(".")) return "float";
        return "int";
    }

    private String evaluateIdentifier(IdentifierExpression node) {
        Symbol symbol = lookup(node.name);
        if (symbol == null) {
            reportError("Identifier '" + node.name + "' is not defined.");
            return "error";
        }
        if ("function".equals(symbol.kind)) {
            reportError("Function '" + node.name + "' cannot be used as a variable.");
            return "error";
        }
        return normalizeType(symbol.type);
    }

    private String evaluateFieldAccess(FieldAccessExpression node) {
        String objectType = evaluate(node.object);
        if (!normalizeType(objectType).equals("Pokemon")) {
            reportError("Pokemon fields are only allowed on Pokemon values.");
            return "error";
        }

        if (node.field.equals("name") || node.field.equals("type")) {
            return "String";
        }

        if (!node.field.equals("hp") && !node.field.equals("attack") && !node.field.equals("defend")) {
            reportError("Invalid Pokemon field '." + node.field + "'. Only .hp, .attack, .defend, .name, and .type are allowed.");
            return "error";
        }

        return "int";
    }

    private String evaluateUnary(UnaryExpression node) {
        String rightType = evaluate(node.right);
        if (node.operator.equals("!")) {
            if (!normalizeType(rightType).equals("boolean")) {
                reportError("Operator ! requires boolean, but found " + rightType + ".");
                return "error";
            }
            return "boolean";
        }
        if (node.operator.equals("-") || node.operator.equals("+")) {
            if (!isNumeric(rightType)) {
                reportError("Unary operator '" + node.operator + "' requires numeric value, but found " + rightType + ".");
                return "error";
            }
            return normalizeType(rightType);
        }
        reportError("Unknown unary operator '" + node.operator + "'.");
        return "error";
    }

    private String evaluateBinary(BinaryExpression node) {
        String leftType = evaluate(node.left);
        String rightType = evaluate(node.right);
        String op = node.operator;

        if (op.equals("+")) {
            if (normalizeType(leftType).equals("String") || normalizeType(rightType).equals("String")) {
                return "String";
            }
            if (!isNumeric(leftType) || !isNumeric(rightType)) {
                reportError("Operator '+' requires numeric operands or at least one String operand, but found " + leftType + " and " + rightType + ".");
                return "error";
            }
            return numericResultType(leftType, rightType);
        }

        if (op.equals("-") || op.equals("*") || op.equals("/")) {
            if (!isNumeric(leftType) || !isNumeric(rightType)) {
                reportError("Arithmetic operator '" + op + "' requires numeric operands, but found " + leftType + " and " + rightType + ".");
                return "error";
            }
            return numericResultType(leftType, rightType);
        }

        if (op.equals(">") || op.equals("<") || op.equals(">=") || op.equals("<=")) {
            if (!isNumeric(leftType) || !isNumeric(rightType)) {
                reportError("Relational operator '" + op + "' requires numeric operands, but found " + leftType + " and " + rightType + ".");
                return "error";
            }
            return "boolean";
        }

        if (op.equals("==") || op.equals("!=")) {
            if (!isAssignable(leftType, rightType) && !isAssignable(rightType, leftType)) {
                reportError("Equality operator '" + op + "' cannot compare " + leftType + " with " + rightType + ".");
                return "error";
            }
            return "boolean";
        }

        if (op.equals("&&") || op.equals("||")) {
            if (!normalizeType(leftType).equals("boolean") || !normalizeType(rightType).equals("boolean")) {
                reportError("Logical operator '" + op + "' requires boolean operands, but found " + leftType + " and " + rightType + ".");
                return "error";
            }
            return "boolean";
        }

        reportError("Unknown binary operator '" + op + "'.");
        return "error";
    }

    private String evaluateFunctionCall(FunctionCallExpression node) {
        Symbol symbol = lookup(node.name);
        if (symbol == null) {
            reportError("Function '" + node.name + "' is not defined.");
            for (Expression argument : node.arguments) evaluate(argument);
            return "error";
        }
        if (!"function".equals(symbol.kind)) {
            reportError("'" + node.name + "' is not a function.");
            for (Expression argument : node.arguments) evaluate(argument);
            return "error";
        }

        if (node.arguments.size() != symbol.parameterTypes.size()) {
            reportError("Function '" + node.name + "' expects " + symbol.parameterTypes.size()
                    + " arguments but got " + node.arguments.size() + ".");
        }

        int limit = Math.min(node.arguments.size(), symbol.parameterTypes.size());
        for (int i = 0; i < node.arguments.size(); i++) {
            String actualType = evaluate(node.arguments.get(i));
            if (i < limit) {
                String expectedType = normalizeType(symbol.parameterTypes.get(i));
                if (!isAssignable(expectedType, actualType)) {
                    reportError("Function '" + node.name + "' argument " + (i + 1)
                            + " expects " + expectedType + " but found " + actualType + ".");
                }
            }
        }

        return normalizeType(symbol.returnType);
    }
}
