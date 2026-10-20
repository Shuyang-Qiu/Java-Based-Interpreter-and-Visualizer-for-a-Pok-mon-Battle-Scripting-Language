import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter the path to the test code file: ");
        String path = scanner.nextLine();

        try {
            String source = new String(Files.readAllBytes(Paths.get(path)));

            Tokenizer tokenizer = new Tokenizer(source);
            List<Tokenizer.Token> tokens = tokenizer.tokenize();

            Parser parser = new Parser(tokens);

            List<Statement> statements = new ArrayList<>();
            int parseErrorCount = 0;

            while (!parser.isAtEnd()) {
                try {
                    Statement stmt = parser.parseStatement();
                    statements.add(stmt);
                } catch (ParseException e) {
                    parseErrorCount++;
                    System.out.println("Parse Error: " + e.getMessage());
                    parser.synchronize();
                }
            }

            System.out.println();
            System.out.println(
                    "Number of successfully parsed statements: " + statements.size());
            System.out.println("Number of parse errors: " + parseErrorCount);

            if (parseErrorCount > 0) {
                System.out.println(
                        "Parsing finished with errors. Scope analysis not started.");
                return;
            }

            ProgramNode program = new ProgramNode(statements);
            ScopeVisitor scopeVisitor = new ScopeVisitor();
            program.accept(scopeVisitor);

            if (scopeVisitor.hasErrors()) {
                System.out.println();
                System.out.println(
                        "Number of scope errors: " + scopeVisitor.getErrors().size());
            } else {
                System.out.println("pass scope analysis");
            }

            TypeVisitor typeVisitor = new TypeVisitor();
            program.accept(typeVisitor);

            if (typeVisitor.hasErrors()) {
                System.out.println();
                System.out.println(
                        "Number of type errors: " + typeVisitor.getErrors().size());
            } else {
                System.out.println("pass type analysis");
            }

            if (!scopeVisitor.hasErrors() && !typeVisitor.hasErrors()) {
                System.out.println(
                        "pass type analysis and other language-specific semantic rules");
                InterpreterVisitor interpreter = new InterpreterVisitor();
                program.accept(interpreter);

                System.out.println(
                        "Battle events count: " + interpreter.getBattleEvents().size());
                System.out.println("Launching JavaFX visualizer...");

                BattleVisualizer.openWindow(interpreter.getBattleEvents());
            }

        } catch (IOException e) {
            System.out.println("File Error: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }
}
