import java.util.ArrayList;
import java.util.List;

abstract class ASTNode {
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}

abstract class Statement extends ASTNode {
}

abstract class Expression extends ASTNode {
}

class ProgramNode extends ASTNode {
    List<Statement> statements;

    public ProgramNode(List<Statement> statements) {
        this.statements = statements;
    }

    @Override
    public String toString() {
        return "Program" + statements;
    }
}

class Parameter {
    String type;
    String name;

    public Parameter(String type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public String toString() {
        return type + " " + name;
    }
}

class VariableDeclaration extends Statement {
    String type;
    String name;
    Expression initializer;

    public VariableDeclaration(String type, String name, Expression initializer) {
        this.type = type;
        this.name = name;
        this.initializer = initializer;
    }

    @Override
    public String toString() {
        return "VarDecl(" + type + " " + name + " = " + initializer + ")";
    }
}

class AssignmentStatement extends Statement {
    Expression target;
    String operator;
    Expression value;

    public AssignmentStatement(Expression target, String operator, Expression value) {
        this.target = target;
        this.operator = operator;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Assign(" + target + " " + operator + " " + value + ")";
    }
}

class PokemonLoadStatement extends Statement {
    String name;
    String source;

    public PokemonLoadStatement(String name, String source) {
        this.name = name;
        this.source = source;
    }

    @Override
    public String toString() {
        return "PokemonLoad(" + name + ", " + source + ")";
    }
}

class SpellDatabaseStatement extends Statement {
    String databaseFile;

    public SpellDatabaseStatement(String databaseFile) {
        this.databaseFile = databaseFile;
    }

    @Override
    public String toString() {
        return "SpellDatabase(" + databaseFile + ")";
    }
}

class ShowStatement extends Statement {
    String name;

    public ShowStatement(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Show(" + name + ")";
    }
}

class PrintStatement extends Statement {
    Expression expression;

    public PrintStatement(Expression expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return "Print(" + expression + ")";
    }
}

class SpellStatement extends Statement {
    String attacker;
    String moveKeyword;
    String target;

    public SpellStatement(String attacker, String moveKeyword, String target) {
        this.attacker = attacker;
        this.moveKeyword = moveKeyword;
        this.target = target;
    }

    @Override
    public String toString() {
        return "Spell(" + attacker + " " + moveKeyword + " " + target + ")";
    }
}

class BlockStatement extends Statement {
    List<Statement> statements = new ArrayList<>();

    public BlockStatement(List<Statement> statements) {
        this.statements = statements;
    }

    @Override
    public String toString() {
        return "Block" + statements;
    }
}

class IfStatement extends Statement {
    Expression condition;
    BlockStatement thenBlock;
    List<ElseIfPart> elseIfParts;
    BlockStatement elseBlock;

    public IfStatement(Expression condition, BlockStatement thenBlock,
                       List<ElseIfPart> elseIfParts, BlockStatement elseBlock) {
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseIfParts = elseIfParts;
        this.elseBlock = elseBlock;
    }

    @Override
    public String toString() {
        return "If(" + condition + ", " + thenBlock + ", elseif=" + elseIfParts + ", else=" + elseBlock + ")";
    }
}

class ElseIfPart {
    Expression condition;
    BlockStatement block;

    public ElseIfPart(Expression condition, BlockStatement block) {
        this.condition = condition;
        this.block = block;
    }

    @Override
    public String toString() {
        return "ElseIf(" + condition + ", " + block + ")";
    }
}

class WhileStatement extends Statement {
    Expression condition;
    BlockStatement block;

    public WhileStatement(Expression condition, BlockStatement block) {
        this.condition = condition;
        this.block = block;
    }

    @Override
    public String toString() {
        return "While(" + condition + ", " + block + ")";
    }
}

class ForStatement extends Statement {
    VariableDeclaration init;
    Expression condition;
    AssignmentStatement update;
    BlockStatement block;

    public ForStatement(VariableDeclaration init, Expression condition,
                        AssignmentStatement update, BlockStatement block) {
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.block = block;
    }

    @Override
    public String toString() {
        return "For(" + init + "; " + condition + "; " + update + ", " + block + ")";
    }
}

class FunctionDeclaration extends Statement {
    String returnType;
    String name;
    List<Parameter> parameters;
    BlockStatement body;

    public FunctionDeclaration(String returnType, String name,
                               List<Parameter> parameters, BlockStatement body) {
        this.returnType = returnType;
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }

    @Override
    public String toString() {
        return "FunctionDecl(" + returnType + " " + name + parameters + " " + body + ")";
    }
}

class FunctionCallStatement extends Statement {
    FunctionCallExpression call;

    public FunctionCallStatement(FunctionCallExpression call) {
        this.call = call;
    }

    @Override
    public String toString() {
        return "FunctionCallStmt(" + call + ")";
    }
}

class BreakStatement extends Statement {
    @Override
    public String toString() {
        return "Break";
    }
}

class ContinueStatement extends Statement {
    @Override
    public String toString() {
        return "Continue";
    }
}

class ReturnStatement extends Statement {
    Expression value;

    public ReturnStatement(Expression value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Return(" + value + ")";
    }
}

class LiteralExpression extends Expression {
    String value;

    public LiteralExpression(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}

class IdentifierExpression extends Expression {
    String name;

    public IdentifierExpression(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}

class FieldAccessExpression extends Expression {
    Expression object;
    String field;

    public FieldAccessExpression(Expression object, String field) {
        this.object = object;
        this.field = field;
    }

    @Override
    public String toString() {
        return object + "." + field;
    }
}

class UnaryExpression extends Expression {
    String operator;
    Expression right;

    public UnaryExpression(String operator, Expression right) {
        this.operator = operator;
        this.right = right;
    }

    @Override
    public String toString() {
        return "(" + operator + right + ")";
    }
}

class BinaryExpression extends Expression {
    Expression left;
    String operator;
    Expression right;

    public BinaryExpression(Expression left, String operator, Expression right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public String toString() {
        return "(" + left + " " + operator + " " + right + ")";
    }
}

class FunctionCallExpression extends Expression {
    String name;
    List<Expression> arguments;

    public FunctionCallExpression(String name, List<Expression> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        return name + "(" + arguments + ")";
    }
}