package com.lintang.formula;

import com.lintang.formula.ast.ASTNode;
import com.lintang.formula.visitor.CellReferenceExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CellReferenceExtractor visitor.
 */
@DisplayName("Cell Reference Extractor Tests")
class CellReferenceExtractorTest {

    @Test
    @DisplayName("Extract single cell reference")
    void testExtractSingleCell() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("A1");
        CellReferenceExtractor extractor = new CellReferenceExtractor();
        ast.accept(extractor);
        Set<String> cells = extractor.getCellReferences();
        
        assertThat(cells)
            .hasSize(1)
            .contains("A1");
    }

    @Test
    @DisplayName("Extract multiple cell references")
    void testExtractMultipleCells() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("A1 + B2 + C3");
        CellReferenceExtractor extractor = new CellReferenceExtractor();
        ast.accept(extractor);
        Set<String> cells = extractor.getCellReferences();
        
        assertThat(cells)
            .hasSize(3)
            .containsExactlyInAnyOrder("A1", "B2", "C3");
    }

    @Test
    @DisplayName("Extract cell range")
    void testExtractCellRange() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("A1:A10");
        CellReferenceExtractor extractor = new CellReferenceExtractor();
        ast.accept(extractor);
        Set<String> cells = extractor.getCellReferences();
        
        assertThat(cells)
            .hasSize(1)
            .contains("A1:A10");
    }

    @Test
    @DisplayName("Extract mixed cells and ranges")
    void testExtractMixedCellsAndRanges() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("SUM(A1:A10) + B5 * C3");
        CellReferenceExtractor extractor = new CellReferenceExtractor();
        ast.accept(extractor);
        Set<String> cells = extractor.getCellReferences();
        
        assertThat(cells)
            .hasSize(3)
            .containsExactlyInAnyOrder("A1:A10", "B5", "C3");
    }

    @Test
    @DisplayName("No cell references in numeric formula")
    void testNoReferencesInNumericFormula() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("2 + 3 * 4");
        CellReferenceExtractor extractor = new CellReferenceExtractor();
        ast.accept(extractor);
        Set<String> cells = extractor.getCellReferences();
        
        assertThat(cells).isEmpty();
    }

    @Test
    @DisplayName("Extract cells from nested function calls")
    void testExtractCellsFromFunction() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("MAX(SUM(A1:A10), SUM(B1:B10))");
        CellReferenceExtractor extractor = new CellReferenceExtractor();
        ast.accept(extractor);
        Set<String> cells = extractor.getCellReferences();
        
        assertThat(cells)
            .hasSize(2)
            .containsExactlyInAnyOrder("A1:A10", "B1:B10");
    }

    @Test
    @DisplayName("Extract duplicate cell references as single entry")
    void testExtractDuplicateCells() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("A1 + A1");
        CellReferenceExtractor extractor = new CellReferenceExtractor();
        ast.accept(extractor);
        Set<String> cells = extractor.getCellReferences();
        
        assertThat(cells)
            .hasSize(1)
            .contains("A1");
    }

    @Test
    @DisplayName("Extract cells from complex formula")
    void testExtractFromComplexFormula() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("(A1 + B1) * (C1 - D1) / SUM(E1:E10)");
        CellReferenceExtractor extractor = new CellReferenceExtractor();
        ast.accept(extractor);
        Set<String> cells = extractor.getCellReferences();
        
        assertThat(cells)
            .hasSize(5)
            .containsExactlyInAnyOrder("A1", "B1", "C1", "D1", "E1:E10");
    }

    @Test
    @DisplayName("Extract cells with various cell addresses")
    void testExtractVariousCellAddresses() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("Z99 + AA1 + ZZ100");
        CellReferenceExtractor extractor = new CellReferenceExtractor();
        ast.accept(extractor);
        Set<String> cells = extractor.getCellReferences();
        
        assertThat(cells)
            .hasSize(3)
            .containsExactlyInAnyOrder("Z99", "AA1", "ZZ100");
    }

    @Test
    @DisplayName("Extract cells from formula with multiple ranges")
    void testExtractMultipleRanges() throws Exception {
        ASTNode ast = FormulaParserUtil.parse("SUM(A1:A10) + AVERAGE(B1:B20) - COUNT(C1:C5)");
        CellReferenceExtractor extractor = new CellReferenceExtractor();
        ast.accept(extractor);
        Set<String> cells = extractor.getCellReferences();
        
        assertThat(cells)
            .hasSize(3)
            .containsExactlyInAnyOrder("A1:A10", "B1:B20", "C1:C5");
    }
}
