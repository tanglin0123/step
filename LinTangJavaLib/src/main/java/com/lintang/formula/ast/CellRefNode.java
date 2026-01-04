package com.lintang.formula.ast;

/**
 * AST node representing a cell reference (e.g., A1, B2).
 */
public class CellRefNode extends ASTNode {
    private final String cellRef;

    public CellRefNode(String cellRef) {
        this.cellRef = cellRef;
    }

    public String getCellRef() {
        return cellRef;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "CellRefNode{" +
                "cellRef='" + cellRef + '\'' +
                '}';
    }
}
