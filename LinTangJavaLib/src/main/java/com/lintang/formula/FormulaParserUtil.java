package com.lintang.formula;

import com.lintang.formula.ast.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

/**
 * Main parser utility class for formulas.
 * Converts a formula string to an AST using ANTLR.
 */
public class FormulaParserUtil {

    /**
     * Parses a formula string and returns the AST.
     *
     * @param formula The formula string to parse
     * @return The root node of the AST
     * @throws Exception If parsing fails
     */
    public static ASTNode parse(String formula) throws Exception {
        // Create lexer and token stream
        FormulaLexer lexer = new FormulaLexer(CharStreams.fromString(formula));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Create parser and parse the formula
        FormulaParser parser = new FormulaParser(tokens);

        // Get the parse tree
        FormulaParser.FormulaContext parseTree = parser.formula();

        // Use custom visitor to build AST
        FormulaASTBuilder builder = new FormulaASTBuilder();
        return builder.visit(parseTree);
    }

    /**
     * Parses a formula and returns the AST as a pretty-printed string.
     *
     * @param formula The formula string to parse
     * @return String representation of the AST
     * @throws Exception If parsing fails
     */
    public static String parseToString(String formula) throws Exception {
        ASTNode ast = parse(formula);
        return ast.toString();
    }
}
