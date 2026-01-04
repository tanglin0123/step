package com.lintang.formula.visitor;

import com.lintang.formula.ast.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Visitor that extracts all cell references from a formula.
 * Demonstrates practical use of the visitor pattern for AST analysis.
 */
public class CellReferenceExtractor implements ASTVisitor<Void> {
    private final Set<String> cellReferences = new HashSet<>();

    public Set<String> getCellReferences() {
        return cellReferences;
    }

    @Override
    public Void visit(BinaryOpNode node) {
        node.getLeft().accept(this);
        node.getRight().accept(this);
        return null;
    }

    @Override
    public Void visit(UnaryOpNode node) {
        node.getOperand().accept(this);
        return null;
    }

    @Override
    public Void visit(NumberNode node) {
        return null;
    }

    @Override
    public Void visit(StringNode node) {
        return null;
    }

    @Override
    public Void visit(BooleanNode node) {
        return null;
    }

    @Override
    public Void visit(CellRefNode node) {
        cellReferences.add(node.getCellRef());
        return null;
    }

    @Override
    public Void visit(CellRangeNode node) {
        cellReferences.add(node.getStartCell() + ":" + node.getEndCell());
        return null;
    }

    @Override
    public Void visit(FunctionCallNode node) {
        for (ASTNode arg : node.getArguments()) {
            arg.accept(this);
        }
        return null;
    }
}
