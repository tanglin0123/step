package com.lintang.formula.ast;

/**
 * Visitor interface for traversing the Excel formula AST.
 */
public interface ASTVisitor<T> {
    T visit(BinaryOpNode node);
    T visit(UnaryOpNode node);
    T visit(NumberNode node);
    T visit(StringNode node);
    T visit(BooleanNode node);
    T visit(CellRefNode node);
    T visit(CellRangeNode node);
    T visit(FunctionCallNode node);
}
