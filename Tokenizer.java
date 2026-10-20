import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Tokenizer {

    enum TokenType {
        KEYWORD,
        IDENTIFIER,
        INTEGER,
        FLOAT,
        STRING,
        OPERATOR,
        DELIMITER,
        EOF
    }

    static class Token {
        TokenType type;
        String value;
        int line;

        public Token(TokenType type, String value, int line) {
            this.type = type;
            this.value = value;
            this.line = line;
        }

        @Override
        public String toString() {
            return "Line " + line + ": <" + type + ", " + value + ">";
        }
    }

    private final String input;
    private int pos;
    private int line;

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "if", "else", "elseif", "while", "for", "return",
            "int", "float", "string", "bool", "boolean", "String", "Pokemon", "void",
            "true", "false",
            "spelldatabase", "show", "print", "function",
            "break", "continue",
            "move1", "move2", "move3", "move4"
    ));

    private static final Set<Character> DELIMITERS = new HashSet<>(Arrays.asList(
            '(', ')', '{', '}', ';', ',', '.'
    ));

    public Tokenizer(String input) {
        this.input = input;
        this.pos = 0;
        this.line = 1;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (!isAtEnd()) {
            skipWhitespaceAndComments();

            if (isAtEnd()) {
                break;
            }

            char c = peek();

            if (Character.isLetter(c) || c == '_') {
                tokens.add(readIdentifierOrKeyword());
            } else if (Character.isDigit(c)) {
                tokens.add(readNumber());
            } else if (isStringQuote(c)) {
                tokens.add(readString());
            } else if (isOperatorStart(c)) {
                tokens.add(readOperator());
            } else if (DELIMITERS.contains(c)) {
                tokens.add(new Token(TokenType.DELIMITER, String.valueOf(c), line));
                advance();
            } else {
                error("Invalid character: '" + c + "'");
                advance();
            }
        }

        tokens.add(new Token(TokenType.EOF, "EOF", line));
        return tokens;
    }

    private void skipWhitespaceAndComments() {
        boolean skipping = true;

        while (skipping && !isAtEnd()) {
            skipping = false;

            while (!isAtEnd() && Character.isWhitespace(peek())) {
                if (peek() == '\n') {
                    line++;
                }
                advance();
                skipping = true;
            }

            if (!isAtEnd() && peek() == '/' && peekNext() == '/') {
                advance();
                advance();
                while (!isAtEnd() && peek() != '\n') {
                    advance();
                }
                skipping = true;
            } else if (!isAtEnd() && peek() == '/' && peekNext() == '*') {
                int startLine = line;
                advance();
                advance();

                boolean closed = false;
                while (!isAtEnd()) {
                    if (peek() == '\n') {
                        line++;
                    }
                    if (peek() == '*' && peekNext() == '/') {
                        advance();
                        advance();
                        closed = true;
                        break;
                    }
                    advance();
                }

                if (!closed) {
                    System.out.println("Lexer Error at line " + startLine + ": Unterminated comment");
                }

                skipping = true;
            }
        }
    }

    private Token readIdentifierOrKeyword() {
        int start = pos;
        int tokenLine = line;

        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            advance();
        }

        String text = input.substring(start, pos);

        if (KEYWORDS.contains(text)) {
            return new Token(TokenType.KEYWORD, text, tokenLine);
        } else {
            return new Token(TokenType.IDENTIFIER, text, tokenLine);
        }
    }

    private Token readNumber() {
        int start = pos;
        int tokenLine = line;

        while (!isAtEnd() && Character.isDigit(peek())) {
            advance();
        }

        boolean isFloat = false;

        if (!isAtEnd() && peek() == '.') {
            isFloat = true;
            advance();

            if (isAtEnd() || !Character.isDigit(peek())) {
                errorAtLine(tokenLine, "Invalid number format");
                return new Token(TokenType.FLOAT, input.substring(start, pos), tokenLine);
            }

            while (!isAtEnd() && Character.isDigit(peek())) {
                advance();
            }

            if (!isAtEnd() && peek() == '.') {
                while (!isAtEnd() && (Character.isDigit(peek()) || peek() == '.')) {
                    advance();
                }
                errorAtLine(tokenLine, "Invalid number format");
                return new Token(TokenType.FLOAT, input.substring(start, pos), tokenLine);
            }
        }

        String text = input.substring(start, pos);

        if (isFloat) {
            return new Token(TokenType.FLOAT, text, tokenLine);
        } else {
            return new Token(TokenType.INTEGER, text, tokenLine);
        }
    }

    private Token readString() {
        int tokenLine = line;
        char openingQuote = peek();
        advance();

        StringBuilder sb = new StringBuilder();
        boolean closed = false;

        while (!isAtEnd()) {
            char c = peek();

            if (isClosingStringQuote(c, openingQuote)) {
                closed = true;
                advance();
                break;
            }

            if (c == '\n') {
                line++;
            }

            sb.append(c);
            advance();
        }

        if (!closed) {
            errorAtLine(tokenLine, "Unterminated string");
        }

        return new Token(TokenType.STRING, "\"" + sb.toString() + "\"", tokenLine);
    }

    private Token readOperator() {
        int tokenLine = line;

        if (!isAtEnd()) {
            if (peek() == '=' && peekNext() == '=') {
                advance();
                advance();
                return new Token(TokenType.OPERATOR, "==", tokenLine);
            }
            if (peek() == '!' && peekNext() == '=') {
                advance();
                advance();
                return new Token(TokenType.OPERATOR, "!=", tokenLine);
            }
            if (peek() == '<' && peekNext() == '=') {
                advance();
                advance();
                return new Token(TokenType.OPERATOR, "<=", tokenLine);
            }
            if (peek() == '>' && peekNext() == '=') {
                advance();
                advance();
                return new Token(TokenType.OPERATOR, ">=", tokenLine);
            }
            if (peek() == '+' && peekNext() == '=') {
                advance();
                advance();
                return new Token(TokenType.OPERATOR, "+=", tokenLine);
            }
            if (peek() == '-' && peekNext() == '=') {
                advance();
                advance();
                return new Token(TokenType.OPERATOR, "-=", tokenLine);
            }
            if (peek() == '+' && peekNext() == '+') {
                advance();
                advance();
                return new Token(TokenType.OPERATOR, "++", tokenLine);
            }
            if (peek() == '&' && peekNext() == '&') {
                advance();
                advance();
                return new Token(TokenType.OPERATOR, "&&", tokenLine);
            }
            if (peek() == '|' && peekNext() == '|') {
                advance();
                advance();
                return new Token(TokenType.OPERATOR, "||", tokenLine);
            }
        }

        char c = peek();
        if (c == '+' || c == '-' || c == '*' || c == '/' ||
            c == '=' || c == '!' || c == '<' || c == '>') {
            advance();
            return new Token(TokenType.OPERATOR, String.valueOf(c), tokenLine);
        }

        error("Invalid operator: '" + c + "'");
        advance();
        return new Token(TokenType.OPERATOR, String.valueOf(c), tokenLine);
    }

    private boolean isOperatorStart(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' ||
               c == '=' || c == '!' || c == '<' || c == '>' ||
               c == '&' || c == '|';
    }

    private boolean isStringQuote(char c) {
        return c == '"' || c == '\u201c' || c == '\u201d';
    }

    private boolean isClosingStringQuote(char c, char openingQuote) {
        if (openingQuote == '\u201c') {
            return c == '\u201d' || c == '"';
        }
        if (openingQuote == '\u201d') {
            return c == '\u201d' || c == '"';
        }
        return c == '"';
    }

    private boolean isAtEnd() {
        return pos >= input.length();
    }

    private char peek() {
        return input.charAt(pos);
    }

    private char peekNext() {
        if (pos + 1 >= input.length()) {
            return '\0';
        }
        return input.charAt(pos + 1);
    }

    private void advance() {
        pos++;
    }

    private void error(String message) {
        System.out.println("Lexer Error at line " + line + ": " + message);
    }

    private void errorAtLine(int errorLine, String message) {
        System.out.println("Lexer Error at line " + errorLine + ": " + message);
    }
}
