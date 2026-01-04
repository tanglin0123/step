package com.lintang.formula;

import com.lintang.formula.ast.ASTNode;
import com.lintang.formula.visitor.CellReferenceExtractor;
import com.lintang.formula.visitor.EvaluationVisitor;
import com.lintang.formula.visitor.FormulaStringBuilder;

/**
 * Sample demonstration of using ANTLR to parse formulas to AST with Visitor pattern.
 *
 * This class shows several use cases:
 * 1. Parsing a formula string to AST
 * 2. Evaluating simple numeric expressions  
 * 3. Extracting cell references from formulas
 * 4. Converting AST back to formula string
 */
public class FormulaParserSample {

    public static void main(String[] args) {
        System.out.println("=== Formula Parser Sample with ANTLR ===\n");

        // Sample formulas to parse
        String[] formulas = {
            "2 + 3 * 4",
            "(10 - 5) / 2",
            "2 ^ 3 + 1",
            "SUM(A1:A10) + B5",
            "-5 * (3 + 2)"
        };

        for (String formula : formulas) {
            System.out.println("Formula: " + formula);
            parseAndAnalyzeFormula(formula);
            System.out.println();
        }
    }

    /**
     * Parse and analyze a formula using different visitors.
     */
    private static void parseAndAnalyzeFormula(String formula) {
        try {
            // Step 1: Parse the formula to AST
            ASTNode ast = FormulaParserUtil.parse(formula);
            System.out.println("  AST: " + ast);

            // Step 2: Extract cell references
            CellReferenceExtractor extractor = new CellReferenceExtractor();
            ast.accept(extractor);
            if (!extractor.getCellReferences().isEmpty()) {
                System.out.println("  Cell References: " + extractor.getCellReferences());
            }

            // Step 3: Evaluate simple numeric expressions
            try {
                EvaluationVisitor evaluator = new EvaluationVisitor();
                double result = ast.accept(evaluator);
                System.out.println("  Evaluated Result: " + result);
            } catch (UnsupportedOperationException | ArithmeticException e) {
                System.out.println("  Evaluation: " + e.getMessage());
            }

            // Step 4: Convert AST back to formula string
            FormulaStringBuilder stringBuilder = new FormulaStringBuilder();
            String reconstructed = ast.accept(stringBuilder);
            System.out.println("  Reconstructed: " + reconstructed);

        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example: Extract all cell references from a formula.
     */
    public static void extractCellReferences(String formula) throws Exception {
        ASTNode ast = FormulaParserUtil.parse(formula);
        CellReferenceExtractor extractor = new CellReferenceExtractor();
        ast.accept(extractor);
        System.out.println("Cells in formula '" + formula + "': " + extractor.getCellReferences());
    }

    /**
     * Example: Evaluate a simple arithmetic formula.
     */
    public static double evaluateFormula(String formula) throws Exception {
        ASTNode ast = FormulaParserUtil.parse(formula);
        EvaluationVisitor evaluator = new EvaluationVisitor();
        return ast.accept(evaluator);
    }

    /**
     * Example: Format a formula for display.
     */
    public static String formatFormula(String formula) throws Exception {
        ASTNode ast = FormulaParserUtil.parse(formula);
        FormulaStringBuilder builder = new FormulaStringBuilder();
        return ast.accept(builder);
    }
}
