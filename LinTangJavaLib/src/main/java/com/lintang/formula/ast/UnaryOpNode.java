package com.lintang.formula.ast;

/**
 * AST node representing a unary operation (e.g., -x, +x).
 */
public class UnaryOpNode extends ASTNode {
    private final String operator;
    private final ASTNode operand;

    public UnaryOpNode(String operator, ASTNode operand) {
        this.operator = operator;
        this.operand = operand;
    }

    public String getOperator() {
        return operator;
    }

    public ASTNode getOperand() {
        return operand;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "UnaryOpNode{" +
                "operator='" + operator + '\'' +
                ", operand=" + operand +
                '}';
    }
}
