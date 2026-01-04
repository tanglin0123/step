package com.lintang.formula.ast;

/**
 * AST node representing a cell range (e.g., A1:B10).
 */
public class CellRangeNode extends ASTNode {
    private final String startCell;
    private final String endCell;

    public CellRangeNode(String cellRange) {
        String[] parts = cellRange.split(":");
        this.startCell = parts[0];
        this.endCell = parts[1];
    }

    public CellRangeNode(String startCell, String endCell) {
        this.startCell = startCell;
        this.endCell = endCell;
    }

    public String getStartCell() {
        return startCell;
    }

    public String getEndCell() {
        return endCell;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "CellRangeNode{" +
                "startCell='" + startCell + '\'' +
                ", endCell='" + endCell + '\'' +
                '}';
    }
}
