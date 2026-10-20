import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScopeVisitor extends ASTVisitor {

    private static class Scope {
        String scopeName;
        Scope parent;
        Map<String, Symbol> symbols = new LinkedHashMap<>();

        Scope(String scopeName, Scope parent) {
            this.scopeName = scopeName;
            this.parent = parent;
        }
    }

    private Scope currentScope = null;
    private final List<String> errors = new ArrayList<>();

    private void enterScope(String scopeName) {
        currentScope = new Scope(scopeName, currentScope);
    }

    private void exitScope() {
        if (currentScope != null) {
            currentScope = currentScope.parent;
        }
    }

    private void reportError(String message) {
        errors.add(message);
        System.out.println("Scope Error: " + message);
    }

    private void define(Symbol symbol) {
        if (currentScope.symbols.containsKey(symbol.name)) {
            reportError("Redefinition of '" + symbol.name + "' in " + currentScope.scopeName + ".");
            return;
        }
        currentScope.symbols.put(symbol.name, symbol);
    }

    private Symbol lookup(String name) {
        Scope scope = currentScope;
        while (scope != null) {
            if (scope.symbols.containsKey(name)) {
                return scope.symbols.get(name);
            }
            scope = scope.parent;
        }
        return null;
    }

    private Symbol lookupInCurrentScope(String name) {
        if (currentScope == null) {
            return null;
        }
        return currentScope.symbols.get(name);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<String> getErrors() {
        return errors;
    }

    @Override
    public void visitProgramNode(ProgramNode node) {
        enterScope("global scope");
        for (Statement stmt : node.statements) {
            stmt.accept(this);
        }
        exitScope();
    }

    @Override
    public void visitVariableDeclaration(VariableDeclaration node) {
        if (node.initializer != null) {
            node.initializer.accept(this);
        }
        define(new Symbol(node.name, node.type, "variable"));
    }

    @Override
    public void visitAssignmentStatement(AssignmentStatement node) {
        if (node.target != null) {
            node.target.accept(this);
        }
        if (node.value != null) {
            node.value.accept(this);
        }
    }

    @Override
    public void visitPokemonLoadStatement(PokemonLoadStatement node) {
        define(new Symbol(node.name, "Pokemon", "variable"));
    }

    @Override
    public void visitSpellDatabaseStatement(SpellDatabaseStatement node) {
        // no identifiers to track
    }

    @Override
    public void visitShowStatement(ShowStatement node) {
        if (lookup(node.name) == null) {
            reportError("Identifier '" + node.name + "' is not defined in scope.");
        }
    }

    @Override
    public void visitPrintStatement(PrintStatement node) {
        if (node.expression != null) {
            node.expression.accept(this);
        }
    }

    @Override
    public void visitSpellStatement(SpellStatement node) {
        Symbol attacker = lookup(node.attacker);
        if (attacker == null) {
            reportError("Identifier '" + node.attacker + "' is not defined in scope.");
        }

        Symbol target = lookup(node.target);
        if (target == null) {
            reportError("Identifier '" + node.target + "' is not defined in scope.");
        }
    }

    @Override
    public void visitBlockStatement(BlockStatement node) {
        enterScope("block scope");
        for (Statement stmt : node.statements) {
            stmt.accept(this);
        }
        exitScope();
    }

    @Override
    public void visitIfStatement(IfStatement node) {
        if (node.condition != null) {
            node.condition.accept(this);
        }

        if (node.thenBlock != null) {
            node.thenBlock.accept(this);
        }

        for (ElseIfPart part : node.elseIfParts) {
            if (part.condition != null) {
                part.condition.accept(this);
            }
            if (part.block != null) {
                part.block.accept(this);
            }
        }

        if (node.elseBlock != null) {
            node.elseBlock.accept(this);
        }
    }

    @Override
    public void visitWhileStatement(WhileStatement node) {
        if (node.condition != null) {
            node.condition.accept(this);
        }
        if (node.block != null) {
            node.block.accept(this);
        }
    }

    @Override
    public void visitForStatement(ForStatement node) {
        enterScope("for scope");

        if (node.init != null) {
            node.init.accept(this);
        }
        if (node.condition != null) {
            node.condition.accept(this);
        }
        if (node.update != null) {
            node.update.accept(this);
        }
        if (node.block != null) {
            node.block.accept(this);
        }

        exitScope();
    }

    @Override
    public void visitFunctionDeclaration(FunctionDeclaration node) {
        List<String> parameterTypes = new ArrayList<>();
        for (Parameter parameter : node.parameters) {
            parameterTypes.add(parameter.type);
        }
        define(new Symbol(node.name, "function", node.returnType, parameterTypes));

        enterScope("function scope");

        for (Parameter parameter : node.parameters) {
            if (lookupInCurrentScope(parameter.name) != null) {
                reportError("Redefinition of parameter '" + parameter.name + "' in function '" + node.name + "'.");
            } else {
                define(new Symbol(parameter.name, parameter.type, "parameter"));
            }
        }

        if (node.body != null) {
            node.body.accept(this);
        }

        exitScope();
    }

    @Override
    public void visitFunctionCallStatement(FunctionCallStatement node) {
        if (node.call != null) {
            node.call.accept(this);
        }
    }

    @Override
    public void visitBreakStatement(BreakStatement node) {
        // no identifiers to track
    }

    @Override
    public void visitContinueStatement(ContinueStatement node) {
        // no identifiers to track
    }

    @Override
    public void visitReturnStatement(ReturnStatement node) {
        if (node.value != null) {
            node.value.accept(this);
        }
    }

    @Override
    public void visitLiteralExpression(LiteralExpression node) {
        // literals do not need symbol lookup
    }

    @Override
    public void visitIdentifierExpression(IdentifierExpression node) {
        if (lookup(node.name) == null) {
            reportError("Identifier '" + node.name + "' is not defined in scope.");
        }
    }

    @Override
    public void visitFieldAccessExpression(FieldAccessExpression node) {
        if (node.object != null) {
            node.object.accept(this);
        }
    }

    @Override
    public void visitUnaryExpression(UnaryExpression node) {
        if (node.right != null) {
            node.right.accept(this);
        }
    }

    @Override
    public void visitBinaryExpression(BinaryExpression node) {
        if (node.left != null) {
            node.left.accept(this);
        }
        if (node.right != null) {
            node.right.accept(this);
        }
    }

    @Override
    public void visitFunctionCallExpression(FunctionCallExpression node) {
        Symbol symbol = lookup(node.name);

        if (symbol == null) {
            reportError("Function '" + node.name + "' is not defined in scope.");
        } else if (!"function".equals(symbol.kind)) {
            reportError("'" + node.name + "' is not a function.");
        }

        for (Expression argument : node.arguments) {
            argument.accept(this);
        }
    }
}
