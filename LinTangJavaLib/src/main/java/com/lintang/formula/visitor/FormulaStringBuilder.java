package com.lintang.formula.visitor;

import com.lintang.formula.ast.*;

/**
 * Visitor that converts an AST back to a formula string (pretty-printing).
 * Demonstrates AST traversal for code generation.
 */
public class FormulaStringBuilder implements ASTVisitor<String> {

    @Override
    public String visit(BinaryOpNode node) {
        String left = node.getLeft().accept(this);
        String right = node.getRight().accept(this);
        return "(" + left + " " + node.getOperator() + " " + right + ")";
    }

    @Override
    public String visit(UnaryOpNode node) {
        String operand = node.getOperand().accept(this);
        return node.getOperator() + operand;
    }

    @Override
    public String visit(NumberNode node) {
        return String.valueOf(node.getValue());
    }

    @Override
    public String visit(StringNode node) {
        return "\"" + node.getValue() + "\"";
    }

    @Override
    public String visit(BooleanNode node) {
        return String.valueOf(node.getValue()).toUpperCase();
    }

    @Override
    public String visit(CellRefNode node) {
        return node.getCellRef();
    }

    @Override
    public String visit(CellRangeNode node) {
        return node.getStartCell() + ":" + node.getEndCell();
    }

    @Override
    public String visit(FunctionCallNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.getFunctionName()).append("(");

        for (int i = 0; i < node.getArguments().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(node.getArguments().get(i).accept(this));
        }

        sb.append(")");
        return sb.toString();
    }
}
