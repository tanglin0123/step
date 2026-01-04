package com.lintang.formula;

import com.lintang.formula.ast.ASTNode;
import com.lintang.formula.visitor.CellReferenceExtractor;
import com.lintang.formula.visitor.EvaluationVisitor;
import com.lintang.formula.visitor.FormulaStringBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests combining multiple features of the formula parser.
 */
@DisplayName("Integration Tests")
class FormulaIntegrationTest {

    @Test
    @DisplayName("Parse, evaluate, and reconstruct numeric formula")
    void testParseEvaluateReconstruct() throws Exception {
        String original = "2 + 3 * 4";
        
        // Parse
        ASTNode ast = FormulaParserUtil.parse(original);
        assertThat(ast).isNotNull();
        
        // Evaluate
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        assertThat(result).isEqualTo(14.0);
        
        // Reconstruct
        FormulaStringBuilder builder = new FormulaStringBuilder();
        String reconstructed = ast.accept(builder);
        assertThat(reconstructed).isNotEmpty();
    }

    @Test
    @DisplayName("Parse formula with cells and extract references")
    void testParseAndExtractCellReferences() throws Exception {
        String formula = "SUM(A1:A10) + SUM(B1:B10)";
        
        ASTNode ast = FormulaParserUtil.parse(formula);
        assertThat(ast).isNotNull();
        
        CellReferenceExtractor extractor = new CellReferenceExtractor();
        ast.accept(extractor);
        Set<String> cells = extractor.getCellReferences();
        
        assertThat(cells)
            .hasSize(2)
            .containsExactlyInAnyOrder("A1:A10", "B1:B10");
    }

    @Test
    @DisplayName("Complex formula with mixed operations")
    void testComplexFormulaMixedOperations() throws Exception {
        String formula = "(10 + 5) * 2 - 8 / 4";
        
        ASTNode ast = FormulaParserUtil.parse(formula);
        
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        // (15) * 2 - 2 = 30 - 2 = 28
        assertThat(result).isEqualTo(28.0);
        
        FormulaStringBuilder builder = new FormulaStringBuilder();
        String reconstructed = ast.accept(builder);
        assertThat(reconstructed).isNotEmpty();
    }

    @Test
    @DisplayName("Formula with function and cell references")
    void testFormulaWithFunctionAndCells() throws Exception {
        String formula = "MAX(SUM(A1:A10), SUM(B1:B10), SUM(C1:C5))";
        
        ASTNode ast = FormulaParserUtil.parse(formula);
        
        CellReferenceExtractor extractor = new CellReferenceExtractor();
        ast.accept(extractor);
        Set<String> cells = extractor.getCellReferences();
        
        assertThat(cells).containsExactlyInAnyOrder("A1:A10", "B1:B10", "C1:C5");
        
        FormulaStringBuilder builder = new FormulaStringBuilder();
        String reconstructed = ast.accept(builder);
        assertThat(reconstructed).contains("MAX");
    }

    @Test
    @DisplayName("Nested function calls")
    void testNestedFunctionCalls() throws Exception {
        String formula = "MAX(SUM(A1:A10), SUM(B1:B10))";
        
        ASTNode ast = FormulaParserUtil.parse(formula);
        assertThat(ast).isNotNull();
        
        CellReferenceExtractor extractor = new CellReferenceExtractor();
        ast.accept(extractor);
        Set<String> cells = extractor.getCellReferences();
        
        assertThat(cells).containsExactlyInAnyOrder("A1:A10", "B1:B10");
    }

    @Test
    @DisplayName("Exponentiation with complex operands")
    void testExponentiationWithComplexOperands() throws Exception {
        String formula = "(2 + 3) ^ 2";
        
        ASTNode ast = FormulaParserUtil.parse(formula);
        
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        assertThat(result).isEqualTo(25.0);
    }

    @Test
    @DisplayName("Unary operators in complex expression")
    void testUnaryOperatorsInComplex() throws Exception {
        String formula = "-5 * (3 + 2)";
        
        ASTNode ast = FormulaParserUtil.parse(formula);
        
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        assertThat(result).isEqualTo(-25.0);
        
        FormulaStringBuilder builder = new FormulaStringBuilder();
        String reconstructed = ast.accept(builder);
        assertThat(reconstructed).contains("-");
    }

    @Test
    @DisplayName("Decimal operations with cell references")
    void testDecimalOperationsWithCells() throws Exception {
        String formula = "3.5 * A1 + 2.5";
        
        ASTNode ast = FormulaParserUtil.parse(formula);
        
        CellReferenceExtractor extractor = new CellReferenceExtractor();
        ast.accept(extractor);
        Set<String> cells = extractor.getCellReferences();
        
        assertThat(cells).contains("A1");
    }

    @Test
    @DisplayName("Multiple operations maintain correct precedence")
    void testMultipleOperationsPrecedence() throws Exception {
        String formula = "2 + 3 * 4 - 5 / 2 + 1 ^ 2";
        
        ASTNode ast = FormulaParserUtil.parse(formula);
        
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        // 2 + 12 - 2.5 + 1 = 12.5
        assertThat(result).isEqualTo(12.5);
    }

    @Test
    @DisplayName("String literals in function arguments")
    void testStringLiteralsInFunction() throws Exception {
        String formula = "CONCATENATE(\"Hello\", \" \", \"World\")";
        
        ASTNode ast = FormulaParserUtil.parse(formula);
        assertThat(ast).isNotNull();
        
        FormulaStringBuilder builder = new FormulaStringBuilder();
        String reconstructed = ast.accept(builder);
        assertThat(reconstructed).contains("Hello");
        assertThat(reconstructed).contains("World");
    }

    @Test
    @DisplayName("Formula with multiple cell ranges")
    void testFormulaWithMultipleCellRanges() throws Exception {
        String formula = "SUM(A1:A10) + SUM(B1:B10) + SUM(C1:C10)";
        
        ASTNode ast = FormulaParserUtil.parse(formula);
        
        CellReferenceExtractor extractor = new CellReferenceExtractor();
        ast.accept(extractor);
        Set<String> cells = extractor.getCellReferences();
        
        assertThat(cells).hasSize(3);
        assertThat(cells).containsExactlyInAnyOrder("A1:A10", "B1:B10", "C1:C10");
    }

    @Test
    @DisplayName("All operators combined in single formula")
    void testAllOperatorsCombined() throws Exception {
        String formula = "2 + 3 - 4 * 5 / 2 ^ 1";
        
        ASTNode ast = FormulaParserUtil.parse(formula);
        
        EvaluationVisitor evaluator = new EvaluationVisitor();
        double result = ast.accept(evaluator);
        // 2 + 3 - (4 * 5 / 2) = 2 + 3 - 10 = -5
        assertThat(result).isEqualTo(-5.0);
    }
}
