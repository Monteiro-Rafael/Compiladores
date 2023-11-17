package compilador;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Token {
    String type;
    String value;

    Token(String type, String value) {
        this.type = type;
        this.value = value;
    }
}

class Lexer {
    private static Map<String, Token> tokenCache = new HashMap<>();

    public static ArrayList<Token> tokenize(String input) {
        ArrayList<Token> tokens = new ArrayList<>();

        String[] operators = {"\\+", "-", "\\*", "/", "\\(", "\\)", "\\{", "\\}", ";"};
        String numberPattern = "\\d+";
        String identifierPattern = "[a-zA-Z]+";
        String keywordPattern = "if|while|for|int|else|char|bool";

        String regexPattern = String.join("|", operators) + "|" + numberPattern + "|" + keywordPattern + "|" + identifierPattern;
        System.out.println("Regex Pattern: " + regexPattern);

        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String match = matcher.group();

            if (tokenCache.containsKey(match)) {
                tokens.add(tokenCache.get(match));  // Reuse cached token
            } else {
                Token token;
                if (match.matches(numberPattern)) {
                    token = new Token("NUM", match);
                } else if (match.matches(keywordPattern)) {
                    token = new Token("KEYWORD", match);
                } else if (match.matches(identifierPattern)) {
                    token = new Token("ID", match);
                } else if (match.equals("(")) {
                    token = new Token("LPAREN", match);
                } else if (match.equals(")")) {
                    token = new Token("RPAREN", match);
                } else if (match.equals("{")) {
                    token = new Token("LBRACE", match);
                } else if (match.equals("}")) {
                    token = new Token("RBRACE", match);
                } else if (match.equals(";")) {
                    token = new Token("SEMICOLON", match);
                } else {
                    token = new Token("OPERATOR", match);
                }
                tokens.add(token);
                tokenCache.put(match, token);  // Cache the new token
            }
        }

        return tokens;
    }
}

class Symbol {
    String type;

    Symbol(String type) {
        this.type = type;
    }
}

class SymTable {
    private Map<String, Symbol> symbolTable;

    SymTable() {
        this.symbolTable = new HashMap<>();
    }

    SymTable(SymTable parent) {
        this.symbolTable = (parent != null) ? new HashMap<>(parent.symbolTable) : new HashMap<>();
    }

    void put(String id, Symbol symbol) {
        symbolTable.put(id, symbol);
    }

    Symbol get(String id) {
        return symbolTable.get(id);
    }

    Map<String, Symbol> getSymbolTable() {
        return symbolTable;
    }
}

public class Parser {
    private static SymTable symTable;
    private static int currentLine = 1;

    private static void error(String message) {
        System.out.println("Error at line " + currentLine + ": " + message);
        System.exit(1);
    }

    private static void match(ArrayList<Token> tokens, String expectedType) {
        if (!tokens.isEmpty() && (tokens.get(0).value.equals(expectedType) || tokens.get(0).type.equals(expectedType))) {
            tokens.remove(0);
        } else {
            error("Expected token of type " + expectedType);
        }
    }

    private static void block(ArrayList<Token> tokens) {
        SymTable saved = symTable;
        symTable = new SymTable(saved);

        match(tokens, "{");
        decls(tokens);
        stmts(tokens);
        match(tokens, "}");

        symTable = saved;
    }

    private static void decls(ArrayList<Token> tokens) {
        if (!tokens.isEmpty() && (tokens.get(0).value.equals("int") || tokens.get(0).value.equals("char") || tokens.get(0).value.equals("bool"))) {
            decl(tokens);
            declsPrime(tokens);
        }
    }

    private static void declsPrime(ArrayList<Token> tokens) {
        if (!tokens.isEmpty() && (tokens.get(0).type.equals("int") || tokens.get(0).type.equals("char") || tokens.get(0).type.equals("bool"))) {
            decl(tokens);
            declsPrime(tokens);
        }
    }

    private static void decl(ArrayList<Token> tokens) {
        String type = tokens.get(0).value;
        match(tokens, type);

        String id = tokens.get(0).value;
        match(tokens, "ID");

        match(tokens, ";");

        Symbol s = new Symbol(type);
        symTable.put(id, s);
    }

    private static void stmts(ArrayList<Token> tokens) {
        if (!tokens.isEmpty() && (tokens.get(0).type.equals("{")
                || tokens.get(0).type.equals("int")
                || tokens.get(0).type.equals("char")
                || tokens.get(0).type.equals("bool")
                || tokens.get(0).type.equals("ID")
                || tokens.get(0).value.equals("{")
                || tokens.get(0).value.equals("int")
                || tokens.get(0).value.equals("char")
                || tokens.get(0).value.equals("bool")
                || tokens.get(0).value.equals("ID"))) {
            stmt(tokens);
            stmtsPrime(tokens);
        }
    }

    private static void stmtsPrime(ArrayList<Token> tokens) {
        if (!tokens.isEmpty() && (tokens.get(0).type.equals("{") || tokens.get(0).type.equals("int") || tokens.get(0).type.equals("char") || tokens.get(0).type.equals("bool") || tokens.get(0).type.equals("ID"))) {
            stmt(tokens);
            stmtsPrime(tokens);
        }
    }

    private static void stmt(ArrayList<Token> tokens) {
        if (!tokens.isEmpty() && tokens.get(0).type.equals("{")) {
            block(tokens);
        } else if (!tokens.isEmpty()
                && (tokens.get(0).type.equals("int")
                 || tokens.get(0).type.equals("char")
                 || tokens.get(0).type.equals("bool"))
                 || tokens.get(0).value.equals("int")
                 || tokens.get(0).value.equals("char")
                 || tokens.get(0).value.equals("bool")) {
            decl(tokens);
        } else if (!tokens.isEmpty() && (tokens.get(0).type.equals("ID") || tokens.get(0).value.equals("ID"))) {
            fact(tokens);
            match(tokens, ";");
        } else {
            error("Invalid statement");
        }
    }

    private static void fact(ArrayList<Token> tokens) {
        String id = tokens.get(0).value;
        match(tokens, "ID");

        Symbol s = symTable.get(id);
        if (s == null) {
            error("Undeclared variable: " + id);
        }

        System.out.print(id + ":" + s.type + "; ");
    }

    public static void main(String[] args) {
        String inputFileName = "resources/sample.txt";
        StringBuilder input = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(inputFileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                input.append(line).append(" ");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(input);

        ArrayList<Token> tokens = Lexer.tokenize(input.toString());

        // Inicialize a tabela de símbolos aqui
        symTable = new SymTable();

        // Continue com a análise sintática
        block(tokens);

        // Print symbol table
        System.out.print("{ ");
        for (Map.Entry<String, Symbol> entry : symTable.getSymbolTable().entrySet()) {
            System.out.print(entry.getKey() + ":" + entry.getValue().type + "; ");
        }
        System.out.print("}");
    }
}
