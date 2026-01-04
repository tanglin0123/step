package com.lintang.formula;

import com.lintang.formula.ast.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for basic formula parsing functionality.
 */
@DisplayName("Formula Parser Basic Tests")
class FormulaParserBasicTest {

    @Test
    @DisplayName("Parse simple number")
    void testParseSimpleNumber() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("42");
        
        assertThat(ast)
            .isInstanceOf(NumberNode.class);
        assertThat(((NumberNode) ast).getValue())
            .isEqualTo(42.0);
    }

    @Test
    @DisplayName("Parse decimal number")
    void testParseDecimalNumber() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("3.14");
        
        assertThat(ast)
            .isInstanceOf(NumberNode.class);
        assertThat(((NumberNode) ast).getValue())
            .isEqualTo(3.14);
    }

    @Test
    @DisplayName("Parse string literal")
    void testParseStringLiteral() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("\"hello\"");
        
        assertThat(ast)
            .isInstanceOf(StringNode.class);
        assertThat(((StringNode) ast).getValue())
            .isEqualTo("hello");
    }

    @Test
    @DisplayName("Parse boolean true")
    void testParseBooleanTrue() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("TRUE");
        
        assertThat(ast)
            .isInstanceOf(BooleanNode.class);
        assertThat(((BooleanNode) ast).getValue())
            .isTrue();
    }

    @Test
    @DisplayName("Parse boolean false")
    void testParseBooleanFalse() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("FALSE");
        
        assertThat(ast)
            .isInstanceOf(BooleanNode.class);
        assertThat(((BooleanNode) ast).getValue())
            .isFalse();
    }

    @Test
    @DisplayName("Parse single cell reference")
    void testParseCellReference() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("A1");
        
        assertThat(ast)
            .isInstanceOf(CellRefNode.class);
        assertThat(((CellRefNode) ast).getCellRef())
            .isEqualTo("A1");
    }

    @Test
    @DisplayName("Parse cell range")
    void testParseCellRange() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("A1:B10");
        
        assertThat(ast)
            .isInstanceOf(CellRangeNode.class);
        CellRangeNode rangeNode = (CellRangeNode) ast;
        assertThat(rangeNode.getStartCell()).isEqualTo("A1");
        assertThat(rangeNode.getEndCell()).isEqualTo("B10");
    }

    @Test
    @DisplayName("Parse simple binary operation (addition)")
    void testParseBinaryAddition() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("2 + 3");
        
        assertThat(ast)
            .isInstanceOf(BinaryOpNode.class);
        BinaryOpNode binOp = (BinaryOpNode) ast;
        assertThat(binOp.getOperator()).isEqualTo("+");
        assertThat(binOp.getLeft()).isInstanceOf(NumberNode.class);
        assertThat(binOp.getRight()).isInstanceOf(NumberNode.class);
    }

    @Test
    @DisplayName("Parse unary negation")
    void testParseUnaryNegation() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("-5");
        
        assertThat(ast)
            .isInstanceOf(UnaryOpNode.class);
        UnaryOpNode unaryOp = (UnaryOpNode) ast;
        assertThat(unaryOp.getOperator()).isEqualTo("-");
        assertThat(unaryOp.getOperand()).isInstanceOf(NumberNode.class);
    }

    @Test
    @DisplayName("Parse unary positive")
    void testParseUnaryPositive() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("+5");
        
        assertThat(ast)
            .isInstanceOf(UnaryOpNode.class);
        UnaryOpNode unaryOp = (UnaryOpNode) ast;
        assertThat(unaryOp.getOperator()).isEqualTo("+");
    }

    @Test
    @DisplayName("Parse simple function call")
    void testParseSimpleFunctionCall() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("SUM(A1:A10)");
        
        assertThat(ast)
            .isInstanceOf(FunctionCallNode.class);
        FunctionCallNode funcNode = (FunctionCallNode) ast;
        assertThat(funcNode.getFunctionName()).isEqualTo("SUM");
        assertThat(funcNode.getArguments()).hasSize(1);
    }

    @Test
    @DisplayName("Parse function with multiple arguments")
    void testParseFunctionWithMultipleArguments() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("MAX(1, 2, 3)");
        
        assertThat(ast)
            .isInstanceOf(FunctionCallNode.class);
        FunctionCallNode funcNode = (FunctionCallNode) ast;
        assertThat(funcNode.getFunctionName()).isEqualTo("MAX");
        assertThat(funcNode.getArguments()).hasSize(3);
    }

    @Test
    @DisplayName("Parse operator precedence - multiplication before addition")
    void testOperatorPrecedence() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("2 + 3 * 4");
        
        assertThat(ast).isInstanceOf(BinaryOpNode.class);
        BinaryOpNode root = (BinaryOpNode) ast;
        assertThat(root.getOperator()).isEqualTo("+");
        assertThat(root.getLeft()).isInstanceOf(NumberNode.class);
        assertThat(root.getRight()).isInstanceOf(BinaryOpNode.class);
        
        BinaryOpNode rightOp = (BinaryOpNode) root.getRight();
        assertThat(rightOp.getOperator()).isEqualTo("*");
    }

    @Test
    @DisplayName("Parse parenthesized expression")
    void testParenthesizedExpression() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("(2 + 3) * 4");
        
        assertThat(ast).isInstanceOf(BinaryOpNode.class);
        BinaryOpNode root = (BinaryOpNode) ast;
        assertThat(root.getOperator()).isEqualTo("*");
        assertThat(root.getLeft()).isInstanceOf(BinaryOpNode.class);
        assertThat(root.getRight()).isInstanceOf(NumberNode.class);
    }

    @Test
    @DisplayName("Parse exponentiation operator")
    void testExponentiationOperator() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("2 ^ 3");
        
        assertThat(ast).isInstanceOf(BinaryOpNode.class);
        BinaryOpNode binOp = (BinaryOpNode) ast;
        assertThat(binOp.getOperator()).isEqualTo("^");
    }

    @Test
    @DisplayName("Parse complex nested formula")
    void testComplexNestedFormula() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("(A1 + B1) * (C1 - D1) / E1");
        
        assertThat(ast).isInstanceOf(BinaryOpNode.class);
        // Just verify it parses without exceptions
    }

    @Test
    @DisplayName("Parse division operator")
    void testDivisionOperator() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("10 / 2");
        
        assertThat(ast).isInstanceOf(BinaryOpNode.class);
        BinaryOpNode binOp = (BinaryOpNode) ast;
        assertThat(binOp.getOperator()).isEqualTo("/");
    }

    @Test
    @DisplayName("Parse subtraction operator")
    void testSubtractionOperator() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("10 - 5");
        
        assertThat(ast).isInstanceOf(BinaryOpNode.class);
        BinaryOpNode binOp = (BinaryOpNode) ast;
        assertThat(binOp.getOperator()).isEqualTo("-");
    }
}
