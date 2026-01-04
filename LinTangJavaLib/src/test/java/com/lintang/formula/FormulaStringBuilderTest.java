package com.lintang.formula;

import com.lintang.formula.ast.ASTNode;
import com.lintang.formula.visitor.FormulaStringBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for FormulaStringBuilder visitor.
 */
@DisplayName("Formula String Builder Tests")
class FormulaStringBuilderTest {

    @Test
    @DisplayName("Reconstruct simple number")
    void testReconstructSimpleNumber() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("42");
        FormulaStringBuilder builder = new FormulaStringBuilder();
        String result = ast.accept(builder);
        
        assertThat(result).isEqualTo("42.0");
    }

    @Test
    @DisplayName("Reconstruct addition")
    void testReconstructAddition() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("2 + 3");
        FormulaStringBuilder builder = new FormulaStringBuilder();
        String result = ast.accept(builder);
        
        assertThat(result).contains("+");
        assertThat(result).contains("2.0");
        assertThat(result).contains("3.0");
    }

    @Test
    @DisplayName("Reconstruct with parentheses for operator precedence")
    void testReconstructWithParentheses() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("2 + 3 * 4");
        FormulaStringBuilder builder = new FormulaStringBuilder();
        String result = ast.accept(builder);
        
        // Should have parentheses around 3 * 4
        assertThat(result).contains("(3.0 * 4.0)");
    }

    @Test
    @DisplayName("Reconstruct complex expression maintains structure")
    void testReconstructComplexExpression() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("(2 + 3) * 4");
        FormulaStringBuilder builder = new FormulaStringBuilder();
        String result = ast.accept(builder);
        
        assertThat(result).contains("*");
        assertThat(result).contains("+");
    }

    @Test
    @DisplayName("Reconstruct cell reference")
    void testReconstructCellReference() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("A1");
        FormulaStringBuilder builder = new FormulaStringBuilder();
        String result = ast.accept(builder);
        
        assertThat(result).isEqualTo("A1");
    }

    @Test
    @DisplayName("Reconstruct cell range")
    void testReconstructCellRange() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("A1:B10");
        FormulaStringBuilder builder = new FormulaStringBuilder();
        String result = ast.accept(builder);
        
        assertThat(result).isEqualTo("A1:B10");
    }

    @Test
    @DisplayName("Reconstruct function call")
    void testReconstructFunctionCall() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("SUM(A1:A10)");
        FormulaStringBuilder builder = new FormulaStringBuilder();
        String result = ast.accept(builder);
        
        assertThat(result).contains("SUM");
        assertThat(result).contains("A1:A10");
    }

    @Test
    @DisplayName("Reconstruct function with multiple arguments")
    void testReconstructFunctionMultipleArgs() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("MAX(1, 2, 3)");
        FormulaStringBuilder builder = new FormulaStringBuilder();
        String result = ast.accept(builder);
        
        assertThat(result).contains("MAX");
        assertThat(result).contains("1.0");
        assertThat(result).contains("2.0");
        assertThat(result).contains("3.0");
    }

    @Test
    @DisplayName("Reconstruct string literal with quotes")
    void testReconstructStringLiteral() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("\"hello\"");
        FormulaStringBuilder builder = new FormulaStringBuilder();
        String result = ast.accept(builder);
        
        assertThat(result).contains("hello");
    }

    @Test
    @DisplayName("Reconstruct boolean true")
    void testReconstructBooleanTrue() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("TRUE");
        FormulaStringBuilder builder = new FormulaStringBuilder();
        String result = ast.accept(builder);
        
        assertThat(result).containsIgnoringCase("true");
    }

    @Test
    @DisplayName("Reconstruct boolean false")
    void testReconstructBooleanFalse() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("FALSE");
        FormulaStringBuilder builder = new FormulaStringBuilder();
        String result = ast.accept(builder);
        
        assertThat(result).containsIgnoringCase("false");
    }

    @Test
    @DisplayName("Reconstruct unary negation")
    void testReconstructUnaryNegation() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("-5");
        FormulaStringBuilder builder = new FormulaStringBuilder();
        String result = ast.accept(builder);
        
        assertThat(result).contains("-");
    }

    @Test
    @DisplayName("Reconstruct all operators")
    void testReconstructAllOperators() throws Exception {
        String[] formulas = {"+", "-", "*", "/", "^"};
        FormulaStringBuilder builder = new FormulaStringBuilder();
        
        for (String op : formulas) {
            String formula = String.format("2 %s 3", op);
            ASTNode ast = FormulaParserUtil.parse(formula);
            String result = ast.accept(builder);
            assertThat(result).contains(op);
        }
    }

    @Test
    @DisplayName("Reconstruct maintains formula readability")
    void testReconstructMaintainsReadability() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("A1 + B2 * C3");
        FormulaStringBuilder builder = new FormulaStringBuilder();
        String result = ast.accept(builder);
        
        assertThat(result)
            .contains("A1")
            .contains("B2")
            .contains("C3")
            .contains("+")
            .contains("*");
    }
}
