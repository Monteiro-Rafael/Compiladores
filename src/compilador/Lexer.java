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

public class Lexer {
    private static Map<String, Token> tokenCache = new HashMap<>();
    public static ArrayList<Token> tokenize(String input) {
        ArrayList<Token> tokens = new ArrayList<>();

        String[] operators = {"\\+", "-", "\\*", "/", "\\(", "\\)"};
        String numberPattern = "\\d+";
        String identifierPattern = "[a-zA-Z]+";
        String keywordPattern = "if|while|for|int|else";

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
                } else {
                    token = new Token("OPERATOR", match);
                }
                tokens.add(token);
                tokenCache.put(match, token);  // Cache the new token
            }
        }

        return tokens;
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

        ArrayList<Token> tokens = tokenize(input.toString());

        for (Token token : tokens) {
            System.out.println("Type: " + token.type + ", Value: " + token.value);
        }
    }
}