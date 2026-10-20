public abstract class ASTVisitor {

    public void visit(ASTNode node) {
        if (node == null) return;

        if (node instanceof ProgramNode) visitProgramNode((ProgramNode) node);
        else if (node instanceof VariableDeclaration) visitVariableDeclaration((VariableDeclaration) node);
        else if (node instanceof AssignmentStatement) visitAssignmentStatement((AssignmentStatement) node);
        else if (node instanceof PokemonLoadStatement) visitPokemonLoadStatement((PokemonLoadStatement) node);
        else if (node instanceof SpellDatabaseStatement) visitSpellDatabaseStatement((SpellDatabaseStatement) node);
        else if (node instanceof ShowStatement) visitShowStatement((ShowStatement) node);
        else if (node instanceof PrintStatement) visitPrintStatement((PrintStatement) node);
        else if (node instanceof SpellStatement) visitSpellStatement((SpellStatement) node);
        else if (node instanceof BlockStatement) visitBlockStatement((BlockStatement) node);
        else if (node instanceof IfStatement) visitIfStatement((IfStatement) node);
        else if (node instanceof WhileStatement) visitWhileStatement((WhileStatement) node);
        else if (node instanceof ForStatement) visitForStatement((ForStatement) node);
        else if (node instanceof FunctionDeclaration) visitFunctionDeclaration((FunctionDeclaration) node);
        else if (node instanceof FunctionCallStatement) visitFunctionCallStatement((FunctionCallStatement) node);
        else if (node instanceof BreakStatement) visitBreakStatement((BreakStatement) node);
        else if (node instanceof ContinueStatement) visitContinueStatement((ContinueStatement) node);
        else if (node instanceof ReturnStatement) visitReturnStatement((ReturnStatement) node);
        else if (node instanceof LiteralExpression) visitLiteralExpression((LiteralExpression) node);
        else if (node instanceof IdentifierExpression) visitIdentifierExpression((IdentifierExpression) node);
        else if (node instanceof FieldAccessExpression) visitFieldAccessExpression((FieldAccessExpression) node);
        else if (node instanceof UnaryExpression) visitUnaryExpression((UnaryExpression) node);
        else if (node instanceof BinaryExpression) visitBinaryExpression((BinaryExpression) node);
        else if (node instanceof FunctionCallExpression) visitFunctionCallExpression((FunctionCallExpression) node);
    }

    public void visitProgramNode(ProgramNode node) {}
    public void visitVariableDeclaration(VariableDeclaration node) {}
    public void visitAssignmentStatement(AssignmentStatement node) {}
    public void visitPokemonLoadStatement(PokemonLoadStatement node) {}
    public void visitSpellDatabaseStatement(SpellDatabaseStatement node) {}
    public void visitShowStatement(ShowStatement node) {}
    public void visitPrintStatement(PrintStatement node) {}
    public void visitSpellStatement(SpellStatement node) {}
    public void visitBlockStatement(BlockStatement node) {}
    public void visitIfStatement(IfStatement node) {}
    public void visitWhileStatement(WhileStatement node) {}
    public void visitForStatement(ForStatement node) {}
    public void visitFunctionDeclaration(FunctionDeclaration node) {}
    public void visitFunctionCallStatement(FunctionCallStatement node) {}
    public void visitBreakStatement(BreakStatement node) {}
    public void visitContinueStatement(ContinueStatement node) {}
    public void visitReturnStatement(ReturnStatement node) {}
    public void visitLiteralExpression(LiteralExpression node) {}
    public void visitIdentifierExpression(IdentifierExpression node) {}
    public void visitFieldAccessExpression(FieldAccessExpression node) {}
    public void visitUnaryExpression(UnaryExpression node) {}
    public void visitBinaryExpression(BinaryExpression node) {}
    public void visitFunctionCallExpression(FunctionCallExpression node) {}
}