public class ASTPrinterVisitor extends ASTVisitor {

    @Override
    public void visitProgramNode(ProgramNode node) {
        System.out.println("Program");
        for (Statement stmt : node.statements) {
            visit(stmt);
        }
    }

    @Override
    public void visitVariableDeclaration(VariableDeclaration node) {
        System.out.println("Variable Declaration -> Type: " + node.type + ", Name: " + node.name);
        if (node.initializer != null) {
            visit(node.initializer);
        }
    }

    @Override
    public void visitAssignmentStatement(AssignmentStatement node) {
        System.out.println("Assignment -> Operator: " + node.operator);
        visit(node.target);
        visit(node.value);
    }

    @Override
    public void visitPokemonLoadStatement(PokemonLoadStatement node) {
        System.out.println("Pokemon Load -> Name: " + node.name + ", Source: " + node.source);
    }

    @Override
    public void visitSpellDatabaseStatement(SpellDatabaseStatement node) {
        System.out.println("Spell Database -> File: " + node.databaseFile);
    }

    @Override
    public void visitShowStatement(ShowStatement node) {
        System.out.println("Show Statement -> " + node.name);
    }

    @Override
    public void visitPrintStatement(PrintStatement node) {
        System.out.println("Print Statement");
        visit(node.expression);
    }

    @Override
    public void visitSpellStatement(SpellStatement node) {
        System.out.println("Spell Statement -> " + node.attacker + " " + node.moveKeyword + " " + node.target);
    }

    @Override
    public void visitBlockStatement(BlockStatement node) {
        System.out.println("Block Statement");
        for (Statement stmt : node.statements) {
            visit(stmt);
        }
    }

    @Override
    public void visitIfStatement(IfStatement node) {
        System.out.println("If Statement");
        visit(node.condition);
        visit(node.thenBlock);

        if (node.elseIfParts != null) {
            for (ElseIfPart part : node.elseIfParts) {
                System.out.println("Else If Part");
                visit(part.condition);
                visit(part.block);
            }
        }

        if (node.elseBlock != null) {
            System.out.println("Else Part");
            visit(node.elseBlock);
        }
    }

    @Override
    public void visitWhileStatement(WhileStatement node) {
        System.out.println("While Statement");
        visit(node.condition);
        visit(node.block);
    }

    @Override
    public void visitForStatement(ForStatement node) {
        System.out.println("For Statement");
        if (node.init != null) {
            visit(node.init);
        }
        if (node.condition != null) {
            visit(node.condition);
        }
        if (node.update != null) {
            visit(node.update);
        }
        visit(node.block);
    }

    @Override
    public void visitFunctionDeclaration(FunctionDeclaration node) {
        System.out.println("Function Declaration -> " + node.name + ", Return Type: " + node.returnType);

        if (node.parameters != null) {
            for (Parameter p : node.parameters) {
                System.out.println("Parameter -> Type: " + p.type + ", Name: " + p.name);
            }
        }

        visit(node.body);
    }

    @Override
    public void visitFunctionCallStatement(FunctionCallStatement node) {
        System.out.println("Function Call Statement");
        visit(node.call);
    }

    @Override
    public void visitBreakStatement(BreakStatement node) {
        System.out.println("Break Statement");
    }

    @Override
    public void visitContinueStatement(ContinueStatement node) {
        System.out.println("Continue Statement");
    }

    @Override
    public void visitReturnStatement(ReturnStatement node) {
        System.out.println("Return Statement");
        if (node.value != null) {
            visit(node.value);
        }
    }

    @Override
    public void visitLiteralExpression(LiteralExpression node) {
        System.out.println("Literal -> " + node.value);
    }

    @Override
    public void visitIdentifierExpression(IdentifierExpression node) {
        System.out.println("Identifier -> " + node.name);
    }

    @Override
    public void visitFieldAccessExpression(FieldAccessExpression node) {
        System.out.println("Field Access -> " + node.field);
        visit(node.object);
    }

    @Override
    public void visitUnaryExpression(UnaryExpression node) {
        System.out.println("Unary Expression -> " + node.operator);
        visit(node.right);
    }

    @Override
    public void visitBinaryExpression(BinaryExpression node) {
        System.out.println("Binary Expression -> " + node.operator);
        visit(node.left);
        visit(node.right);
    }

    @Override
    public void visitFunctionCallExpression(FunctionCallExpression node) {
        System.out.println("Function Call -> " + node.name);
        if (node.arguments != null) {
            for (Expression arg : node.arguments) {
                visit(arg);
            }
        }
    }
}