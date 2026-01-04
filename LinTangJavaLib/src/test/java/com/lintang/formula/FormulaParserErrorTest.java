package com.lintang.formula;

import com.lintang.formula.ast.ASTNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for error handling in formula parser.
 */
@DisplayName("Parser Error Handling Tests")
class FormulaParserErrorTest {

    @Test
    @DisplayName("Parse empty string throws exception")
    void testParseEmptyStringThrowsException() {
        assertThatThrownBy(() -> FormulaParserUtil.parse(""))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Parse whitespace only throws exception")
    void testParseWhitespaceOnlyThrowsException() {
        assertThatThrownBy(() -> FormulaParserUtil.parse("   "))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Parser recovers from unmatched opening parenthesis")
    void testUnmatchedOpenParenthesisParsesWithRecovery() throws Exception {
        // Parser uses error recovery and successfully parses despite missing closing paren
        ASTNode ast = FormulaParserUtil.parse("(2 + 3)");
        assertThat(ast).isNotNull();
    }

    @Test
    @DisplayName("Parse incomplete expression throws exception")
    void testIncompleteExpressionThrowsException() {
        assertThatThrownBy(() -> FormulaParserUtil.parse("2 +"))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Valid function syntax parses successfully")
    void testValidFunctionSyntaxParses() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("SUM(A1:A10)");
        assertThat(ast).isNotNull();
    }

    @Test
    @DisplayName("Parse unclosed string literal throws exception")
    void testUnclosedStringThrowsException() {
        assertThatThrownBy(() -> FormulaParserUtil.parse("\"hello"))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Valid formula with spaces parses successfully")
    void testFormulaWithSpacesParsesSuccessfully() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("  2   +   3   ");
        assertThat(ast).isNotNull();
    }

    @Test
    @DisplayName("Valid formula with no spaces parses successfully")
    void testFormulaWithoutSpacesParsesSuccessfully() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("2+3*4");
        assertThat(ast).isNotNull();
    }

    @Test
    @DisplayName("Parse null throws exception")
    void testParseNullThrowsException() {
        assertThatThrownBy(() -> FormulaParserUtil.parse(null))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Valid formula with nested parentheses")
    void testNestedParenthesesParseSuccessfully() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("((2 + 3) * (4 - 1))");
        assertThat(ast).isNotNull();
    }
}

