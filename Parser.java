import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Tokenizer.Token> tokens;
    private int current = 0;

    public Parser(List<Tokenizer.Token> tokens) {
        this.tokens = tokens;
    }

    public boolean isAtEnd() {
        return peek().type == Tokenizer.TokenType.EOF;
    }

    public Statement parseStatement() {
        if (isTypeKeyword()) {
            return parseTypeBasedStatement();
        }

        if (matchKeyword("spelldatabase")) {
            return parseSpellDatabaseStatement();
        }

        if (matchKeyword("show")) {
            return parseShowStatement();
        }

        if (matchKeyword("print")) {
            return parsePrintStatement();
        }

        if (matchKeyword("if")) {
            return parseIfStatement();
        }

        if (matchKeyword("while")) {
            return parseWhileStatement();
        }

        if (matchKeyword("for")) {
            return parseForStatement();
        }

        if (matchKeyword("function")) {
            return parseFunctionDeclaration();
        }

        if (matchKeyword("break")) {
            consumeStatementTerminator("Expected ';' after break.");
            return new BreakStatement();
        }

        if (matchKeyword("continue")) {
            consumeStatementTerminator("Expected ';' after continue.");
            return new ContinueStatement();
        }

        if (matchKeyword("return")) {
            Expression value = parseExpression();
            consumeStatementTerminator("Expected ';' after return value.");
            return new ReturnStatement(value);
        }

        if (checkIdentifierLike()) {
            return parseIdentifierStartedStatement();
        }

        throw error(peek(), "Expected a statement.");
    }

    private Statement parseTypeBasedStatement() {
        String type = consumeKeywordType("Expected type.").value;
        Tokenizer.Token nameToken = consumeIdentifier("Expected identifier after type.");
        consumeOperator("=", "Expected '=' after identifier.");

        if (type.equals("Pokemon")
                && check(Tokenizer.TokenType.KEYWORD, "Pokemon")
                && checkNext(Tokenizer.TokenType.DELIMITER, "(")) {

            advance(); // consume Pokemon
            consumeDelimiter("(", "Expected '(' after Pokemon.");
            Tokenizer.Token str = consumeType(Tokenizer.TokenType.STRING, "Expected string literal in Pokemon load.");
            consumeDelimiter(")", "Expected ')' after Pokemon string.");
            consumeStatementTerminator("Expected ';' after Pokemon load statement.");
            return new PokemonLoadStatement(nameToken.value, str.value);
        }

        Expression initializer = parseExpression();
        consumeStatementTerminator("Expected ';' after variable declaration.");
        return new VariableDeclaration(type, nameToken.value, initializer);
    }

    private Statement parseSpellDatabaseStatement() {
        Tokenizer.Token file = consumeType(Tokenizer.TokenType.STRING, "Expected string literal after spelldatabase.");
        consumeStatementTerminator("Expected ';' after spelldatabase statement.");
        return new SpellDatabaseStatement(file.value);
    }

    private Statement parseShowStatement() {
        Tokenizer.Token id = consumeIdentifier("Expected identifier after show.");
        consumeStatementTerminator("Expected ';' after show statement.");
        return new ShowStatement(id.value);
    }

    private Statement parsePrintStatement() {
        Expression expr = parseExpression();
        consumeStatementTerminator("Expected ';' after print statement.");
        return new PrintStatement(expr);
    }

    private Statement parseIfStatement() {
        consumeDelimiter("(", "Expected '(' after if.");
        Expression condition = parseExpression();
        consumeDelimiter(")", "Expected ')' after if condition.");
        BlockStatement thenBlock = parseBlock();

        List<ElseIfPart> elseIfParts = new ArrayList<>();
        while (matchKeyword("elseif")) {
            consumeDelimiter("(", "Expected '(' after elseif.");
            Expression elseifCondition = parseExpression();
            consumeDelimiter(")", "Expected ')' after elseif condition.");
            BlockStatement elseifBlock = parseBlock();
            elseIfParts.add(new ElseIfPart(elseifCondition, elseifBlock));
        }

        BlockStatement elseBlock = null;
        if (matchKeyword("else")) {
            elseBlock = parseBlock();
        }

        return new IfStatement(condition, thenBlock, elseIfParts, elseBlock);
    }

    private Statement parseWhileStatement() {
        consumeDelimiter("(", "Expected '(' after while.");
        Expression condition = parseExpression();
        consumeDelimiter(")", "Expected ')' after while condition.");
        BlockStatement block = parseBlock();
        return new WhileStatement(condition, block);
    }

    private Statement parseForStatement() {
        consumeDelimiter("(", "Expected '(' after for.");

        VariableDeclaration init = parseForInit();
        consumeDelimiter(";", "Expected ';' after for initialization.");

        Expression condition = parseExpression();
        consumeDelimiter(";", "Expected ';' after for condition.");

        AssignmentStatement update = parseForUpdate();

        consumeDelimiter(")", "Expected ')' after for update.");
        BlockStatement block = parseBlock();

        return new ForStatement(init, condition, update, block);
    }

    private VariableDeclaration parseForInit() {
        Tokenizer.Token type = consumeKeyword("int", "Expected 'int' in for initialization.");
        Tokenizer.Token name = consumeIdentifier("Expected identifier in for initialization.");
        consumeOperator("=", "Expected '=' in for initialization.");
        Expression initValue = parseExpression();
        return new VariableDeclaration(type.value, name.value, initValue);
    }

    private AssignmentStatement parseForUpdate() {
        Tokenizer.Token id = consumeIdentifier("Expected identifier in for update.");
        consumeOperator("++", "Expected '++' in for update.");

        IdentifierExpression target = new IdentifierExpression(id.value);
        LiteralExpression one = new LiteralExpression("1");
        BinaryExpression increment = new BinaryExpression(target, "+", one);

        return new AssignmentStatement(target, "=", increment);
    }

    private Statement parseFunctionDeclaration() {
        String returnType = parseReturnType();
        Tokenizer.Token name = consumeIdentifier("Expected function name.");
        consumeDelimiter("(", "Expected '(' after function name.");
        List<Parameter> parameters = parseParameterList();
        consumeDelimiter(")", "Expected ')' after parameter list.");
        BlockStatement body = parseBlock();
        return new FunctionDeclaration(returnType, name.value, parameters, body);
    }

    private String parseReturnType() {
        if (matchKeyword("void")) {
            return "void";
        }
        if (isTypeKeyword()) {
            return advance().value;
        }
        throw error(peek(), "Expected return type.");
    }

    private List<Parameter> parseParameterList() {
        List<Parameter> parameters = new ArrayList<>();

        if (check(Tokenizer.TokenType.DELIMITER, ")")) {
            return parameters;
        }

        parameters.add(parseParameter());

        while (matchDelimiter(",")) {
            parameters.add(parseParameter());
        }

        return parameters;
    }

    private Parameter parseParameter() {
        Tokenizer.Token type = consumeKeywordType("Expected parameter type.");
        Tokenizer.Token name = consumeIdentifier("Expected parameter name.");
        return new Parameter(type.value, name.value);
    }

    private Statement parseIdentifierStartedStatement() {
        Tokenizer.Token first = consumeIdentifier("Expected identifier.");

        if (isMoveKeyword(peek())) {
            String move = advance().value;
            Tokenizer.Token target = consumeIdentifier("Expected target identifier after move.");
            consumeStatementTerminator("Expected ';' after spell statement.");
            return new SpellStatement(first.value, move, target.value);
        }

        if (check(Tokenizer.TokenType.DELIMITER, "(")) {
            FunctionCallExpression call = finishFunctionCall(first.value);
            consumeStatementTerminator("Expected ';' after function call.");
            return new FunctionCallStatement(call);
        }

        Expression target = finishLvalue(first);

        if (matchOperator("=") || matchOperator("+=") || matchOperator("-=")) {
            String op = previous().value;
            Expression value = parseExpression();
            consumeStatementTerminator("Expected ';' after assignment.");
            return new AssignmentStatement(target, op, value);
        }

        throw error(peek(), "Expected assignment, spell statement, or function call.");
    }

    private BlockStatement parseBlock() {
        consumeDelimiter("{", "Expected '{' to start block.");

        List<Statement> statements = new ArrayList<>();
        while (!check(Tokenizer.TokenType.DELIMITER, "}") && !isAtEnd()) {
            statements.add(parseStatement());
        }

        consumeDelimiter("}", "Expected '}' to close block.");
        return new BlockStatement(statements);
    }

    public Expression parseExpression() {
        return parseLogicalOr();
    }

    private Expression parseLogicalOr() {
        Expression expr = parseLogicalAnd();

        while (matchOperator("||")) {
            String op = previous().value;
            Expression right = parseLogicalAnd();
            expr = new BinaryExpression(expr, op, right);
        }

        return expr;
    }

    private Expression parseLogicalAnd() {
        Expression expr = parseEquality();

        while (matchOperator("&&")) {
            String op = previous().value;
            Expression right = parseEquality();
            expr = new BinaryExpression(expr, op, right);
        }

        return expr;
    }

    private Expression parseEquality() {
        Expression expr = parseRelational();

        while (matchOperator("==") || matchOperator("!=")) {
            String op = previous().value;
            Expression right = parseRelational();
            expr = new BinaryExpression(expr, op, right);
        }

        return expr;
    }

    private Expression parseRelational() {
        Expression expr = parseAdditive();

        while (matchOperator(">") || matchOperator("<") || matchOperator(">=") || matchOperator("<=")) {
            String op = previous().value;
            Expression right = parseAdditive();
            expr = new BinaryExpression(expr, op, right);
        }

        return expr;
    }

    private Expression parseAdditive() {
        Expression expr = parseMultiplicative();

        while (matchOperator("+") || matchOperator("-")) {
            String op = previous().value;
            Expression right = parseMultiplicative();
            expr = new BinaryExpression(expr, op, right);
        }

        return expr;
    }

    private Expression parseMultiplicative() {
        Expression expr = parseUnary();

        while (matchOperator("*") || matchOperator("/")) {
            String op = previous().value;
            Expression right = parseUnary();
            expr = new BinaryExpression(expr, op, right);
        }

        return expr;
    }

    private Expression parseUnary() {
        if (matchOperator("!") || matchOperator("-") || matchOperator("+")) {
            String op = previous().value;
            Expression right = parseUnary();
            return new UnaryExpression(op, right);
        }

        return parsePrimary();
    }

    private Expression parsePrimary() {
        if (matchType(Tokenizer.TokenType.INTEGER) || matchType(Tokenizer.TokenType.FLOAT) || matchType(Tokenizer.TokenType.STRING)) {
            return new LiteralExpression(previous().value);
        }

        if (matchKeyword("true") || matchKeyword("false")) {
            return new LiteralExpression(previous().value);
        }

        if (matchDelimiter("(")) {
            Expression expr = parseExpression();
            consumeDelimiter(")", "Expected ')' after expression.");
            return expr;
        }

        if (matchIdentifierLike()) {
            Tokenizer.Token id = previous();

            if (check(Tokenizer.TokenType.DELIMITER, "(")) {
                return finishFunctionCall(id.value);
            }

            return finishLvalue(id);
        }

        throw error(peek(), "Expected expression.");
    }

    private Expression finishLvalue(Tokenizer.Token firstIdentifier) {
        Expression expr = new IdentifierExpression(firstIdentifier.value);

        if (matchDelimiter(".")) {
            Tokenizer.Token field = consumePokemonField("Expected pokemon field after '.'.");
            expr = new FieldAccessExpression(expr, field.value);
        }

        return expr;
    }

    private FunctionCallExpression finishFunctionCall(String name) {
        consumeDelimiter("(", "Expected '(' after function name.");
        List<Expression> arguments = parseArgumentList();
        consumeDelimiter(")", "Expected ')' after argument list.");
        return new FunctionCallExpression(name, arguments);
    }

    private List<Expression> parseArgumentList() {
        List<Expression> arguments = new ArrayList<>();

        if (check(Tokenizer.TokenType.DELIMITER, ")")) {
            return arguments;
        }

        arguments.add(parseExpression());

        while (matchDelimiter(",")) {
            arguments.add(parseExpression());
        }

        return arguments;
    }

    public void synchronize() {
        if (!isAtEnd()) {
            advance();
        }

        while (!isAtEnd()) {
            if (previous().type == Tokenizer.TokenType.DELIMITER && previous().value.equals(";")) {
                return;
            }

            if (check(Tokenizer.TokenType.DELIMITER, "}")) {
                return;
            }

            if (isLikelyStatementStart(peek())) {
                return;
            }

            advance();
        }
    }

    private boolean isLikelyStatementStart(Tokenizer.Token token) {
        if (token.type == Tokenizer.TokenType.IDENTIFIER) {
            return true;
        }

        if (token.type == Tokenizer.TokenType.KEYWORD) {
            String v = token.value;
            return v.equals("int") || v.equals("float") || v.equals("boolean") || v.equals("bool") || v.equals("String") || v.equals("string") || v.equals("Pokemon")
                    || v.equals("spelldatabase") || v.equals("show") || v.equals("print")
                    || v.equals("if") || v.equals("while") || v.equals("for")
                    || v.equals("function") || v.equals("break") || v.equals("continue")
                    || v.equals("return") || v.equals("else") || v.equals("elseif");
        }

        return false;
    }

    private boolean isTypeKeyword() {
        if (check(Tokenizer.TokenType.KEYWORD, "bool")) {
            return checkNextIdentifierLike();
        }

        return check(Tokenizer.TokenType.KEYWORD, "int")
                || check(Tokenizer.TokenType.KEYWORD, "float")
                || check(Tokenizer.TokenType.KEYWORD, "boolean")
                || check(Tokenizer.TokenType.KEYWORD, "String")
                || check(Tokenizer.TokenType.KEYWORD, "string")
                || check(Tokenizer.TokenType.KEYWORD, "Pokemon");
    }

    private boolean isMoveKeyword(Tokenizer.Token token) {
        return token.type == Tokenizer.TokenType.KEYWORD &&
                (token.value.equals("move1") || token.value.equals("move2")
                        || token.value.equals("move3") || token.value.equals("move4"));
    }

    private boolean check(Tokenizer.TokenType type, String value) {
        if (isAtEnd()) return false;
        return peek().type == type && peek().value.equals(value);
    }

    private boolean checkNext(Tokenizer.TokenType type, String value) {
        if (current + 1 >= tokens.size()) return false;
        Tokenizer.Token t = tokens.get(current + 1);
        return t.type == type && t.value.equals(value);
    }

    private boolean checkType(Tokenizer.TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean checkIdentifierLike() {
        if (isAtEnd()) return false;
        return isIdentifierLike(peek());
    }

    private boolean checkNextIdentifierLike() {
        if (current + 1 >= tokens.size()) return false;
        return isIdentifierLike(tokens.get(current + 1));
    }

    private boolean isIdentifierLike(Tokenizer.Token token) {
        return token.type == Tokenizer.TokenType.IDENTIFIER
                || (token.type == Tokenizer.TokenType.KEYWORD && token.value.equals("bool"));
    }

    private boolean matchKeyword(String value) {
        if (check(Tokenizer.TokenType.KEYWORD, value)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean matchOperator(String value) {
        if (check(Tokenizer.TokenType.OPERATOR, value)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean matchDelimiter(String value) {
        if (check(Tokenizer.TokenType.DELIMITER, value)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean matchType(Tokenizer.TokenType type) {
        if (checkType(type)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean matchIdentifierLike() {
        if (checkIdentifierLike()) {
            advance();
            return true;
        }
        return false;
    }

    private Tokenizer.Token consumeKeyword(String value, String message) {
        if (check(Tokenizer.TokenType.KEYWORD, value)) return advance();
        throw error(peek(), message);
    }

    private Tokenizer.Token consumeKeywordType(String message) {
        if (isTypeKeyword()) return advance();
        throw error(peek(), message);
    }

    private Tokenizer.Token consumePokemonField(String message) {
        if (checkType(Tokenizer.TokenType.IDENTIFIER)) {
            return advance();
        }

        if (check(Tokenizer.TokenType.KEYWORD, "hp") ||
            check(Tokenizer.TokenType.KEYWORD, "attack") ||
            check(Tokenizer.TokenType.KEYWORD, "defend") ||
            check(Tokenizer.TokenType.KEYWORD, "name") ||
            check(Tokenizer.TokenType.KEYWORD, "type")) {
            return advance();
        }

        throw error(peek(), message);
    }

    private Tokenizer.Token consumeOperator(String value, String message) {
        if (check(Tokenizer.TokenType.OPERATOR, value)) return advance();
        throw error(peek(), message);
    }

    private Tokenizer.Token consumeDelimiter(String value, String message) {
        if (check(Tokenizer.TokenType.DELIMITER, value)) return advance();
        throw error(peek(), message);
    }

    private void consumeStatementTerminator(String message) {
        if (matchDelimiter(";")) {
            return;
        }

        Tokenizer.Token previous = previous();
        Tokenizer.Token next = peek();
        if (previous.line < next.line && (isLikelyStatementStart(next)
                || (next.type == Tokenizer.TokenType.DELIMITER && next.value.equals("}"))
                || next.type == Tokenizer.TokenType.EOF)) {
            return;
        }

        throw error(next, message);
    }

    private Tokenizer.Token consumeType(Tokenizer.TokenType type, String message) {
        if (checkType(type)) return advance();
        throw error(peek(), message);
    }

    private Tokenizer.Token consumeIdentifier(String message) {
        if (checkIdentifierLike()) return advance();
        throw error(peek(), message);
    }

    private Tokenizer.Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private Tokenizer.Token peek() {
        return tokens.get(current);
    }

    private Tokenizer.Token previous() {
        return tokens.get(current - 1);
    }

    private ParseException error(Tokenizer.Token token, String message) {
        return new ParseException("Line " + token.line + " near '" + token.value + "': " + message);
    }
}
