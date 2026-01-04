package com.lintang.formula;

import com.lintang.formula.ast.ASTNode;

/**
 * Simple test program to verify the ANTLR parser works.
 */
public class SimpleParserTest {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Simple ANTLR Parser Test ===\n");

        String[] formulas = {
            "2 + 3 * 4",
            "(10 - 5) / 2",
            "A1 + B2"
        };

        for (String formula : formulas) {
            System.out.println("Parsing: " + formula);
            try {
                ASTNode ast = FormulaParserUtil.parse(formula);
                System.out.println("  Result: " + ast);
            } catch (Exception e) {
                System.out.println("  Error: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();
        }
    }
}
