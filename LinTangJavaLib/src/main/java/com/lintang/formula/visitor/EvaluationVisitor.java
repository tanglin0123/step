package com.lintang.formula.visitor;

import com.lintang.formula.ast.*;

/**
 * Visitor that evaluates the formula AST (simple evaluation without actual cell values).
 * Demonstrates how to implement a custom visitor for AST traversal.
 */
public class EvaluationVisitor implements ASTVisitor<Double> {

    @Override
    public Double visit(BinaryOpNode node) {
        double left = node.getLeft().accept(this);
        double right = node.getRight().accept(this);

        return switch (node.getOperator()) {
            case "+" -> left + right;
            case "-" -> left - right;
            case "*" -> left * right;
            case "/" -> {
                if (right == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                yield left / right;
            }
            case "^" -> Math.pow(left, right);
            default -> throw new IllegalArgumentException("Unknown operator: " + node.getOperator());
        };
    }

    @Override
    public Double visit(UnaryOpNode node) {
        double operand = node.getOperand().accept(this);
        return switch (node.getOperator()) {
            case "+" -> operand;
            case "-" -> -operand;
            default -> throw new IllegalArgumentException("Unknown unary operator: " + node.getOperator());
        };
    }

    @Override
    public Double visit(NumberNode node) {
        return node.getValue();
    }

    @Override
    public Double visit(StringNode node) {
        throw new UnsupportedOperationException("String evaluation not supported");
    }

    @Override
    public Double visit(BooleanNode node) {
        return node.getValue() ? 1.0 : 0.0;
    }

    @Override
    public Double visit(CellRefNode node) {
        throw new UnsupportedOperationException("Cell reference evaluation requires actual cell data");
    }

    @Override
    public Double visit(CellRangeNode node) {
        throw new UnsupportedOperationException("Cell range evaluation requires actual cell data");
    }

    @Override
    public Double visit(FunctionCallNode node) {
        throw new UnsupportedOperationException("Function evaluation not fully implemented");
    }
}
