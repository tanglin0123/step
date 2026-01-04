package com.lintang.formula;

import com.lintang.formula.ast.*;
import com.lintang.formula.visitor.EvaluationVisitor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for formula evaluation using EvaluationVisitor.
 */
@DisplayName("Evaluation Visitor Tests")
class EvaluationVisitorTest {

    @Test
    @DisplayName("Evaluate simple number")
    void testEvaluateSimpleNumber() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("42");
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        
        assertThat(result).isEqualTo(42.0);
    }

    @Test
    @DisplayName("Evaluate addition")
    void testEvaluateAddition() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("2 + 3");
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        
        assertThat(result).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Evaluate subtraction")
    void testEvaluateSubtraction() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("10 - 5");
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        
        assertThat(result).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Evaluate multiplication")
    void testEvaluateMultiplication() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("3 * 4");
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        
        assertThat(result).isEqualTo(12.0);
    }

    @Test
    @DisplayName("Evaluate division")
    void testEvaluateDivision() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("10 / 2");
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        
        assertThat(result).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Evaluate exponentiation")
    void testEvaluateExponentiation() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("2 ^ 3");
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        
        assertThat(result).isEqualTo(8.0);
    }

    @Test
    @DisplayName("Evaluate operator precedence: 2 + 3 * 4 = 14")
    void testEvaluateOperatorPrecedence() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("2 + 3 * 4");
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        
        assertThat(result).isEqualTo(14.0);
    }

    @Test
    @DisplayName("Evaluate parenthesized expression: (2 + 3) * 4 = 20")
    void testEvaluateParenthesizedExpression() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("(2 + 3) * 4");
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        
        assertThat(result).isEqualTo(20.0);
    }

    @Test
    @DisplayName("Evaluate unary negation")
    void testEvaluateUnaryNegation() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("-5");
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        
        assertThat(result).isEqualTo(-5.0);
    }

    @Test
    @DisplayName("Evaluate unary positive")
    void testEvaluateUnaryPositive() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("+5");
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        
        assertThat(result).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Evaluate complex nested expression")
    void testEvaluateComplexExpression() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("(10 + 5) * 2 - 10 / 2");
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        
        // (15) * 2 - 5 = 30 - 5 = 25
        assertThat(result).isEqualTo(25.0);
    }

    @Test
    @DisplayName("Evaluate decimal operations")
    void testEvaluateDecimalOperations() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("3.5 + 2.5");
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        
        assertThat(result).isEqualTo(6.0);
    }

    @Test
    @DisplayName("Evaluate division by zero throws exception")
    void testDivisionByZeroThrowsException() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("10 / 0");
        EvaluationVisitor evaluator = new EvaluationVisitor();
        
        assertThatThrownBy(() -> ast.accept(evaluator))
            .isInstanceOf(ArithmeticException.class)
            .hasMessage("Division by zero");
    }

    @Test
    @DisplayName("Evaluate boolean as 1.0")
    void testEvaluateBooleanTrue() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("TRUE");
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        
        assertThat(result).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Evaluate boolean false as 0.0")
    void testEvaluateBooleanFalse() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("FALSE");
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Evaluate string throws UnsupportedOperationException")
    void testEvaluateStringThrowsException() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("\"text\"");
        EvaluationVisitor evaluator = new EvaluationVisitor();
        
        assertThatThrownBy(() -> ast.accept(evaluator))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Evaluate cell reference throws UnsupportedOperationException")
    void testEvaluateCellReferenceThrowsException() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("A1");
        EvaluationVisitor evaluator = new EvaluationVisitor();
        
        assertThatThrownBy(() -> ast.accept(evaluator))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Evaluate function call throws UnsupportedOperationException")
    void testEvaluateFunctionThrowsException() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("SUM(A1:A10)");
        EvaluationVisitor evaluator = new EvaluationVisitor();
        
        assertThatThrownBy(() -> ast.accept(evaluator))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "1 + 1, 2.0",
        "10 - 3, 7.0",
        "4 * 5, 20.0",
        "20 / 4, 5.0",
        "3 ^ 2, 9.0",
        "-10, -10.0",
        "+5, 5.0",
    })
    @DisplayName("Parameterized evaluation tests")
    void testEvaluateParameterized(String formula, double expected) throws Exception {
        ASTNode ast = FormulaParserUtil.parse(formula);
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        
        assertThat(result).isEqualTo(expected);
    }
}
