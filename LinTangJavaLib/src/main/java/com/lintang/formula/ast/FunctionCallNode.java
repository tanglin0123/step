package com.lintang.formula.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * AST node representing a function call (e.g., SUM(A1:A10)).
 */
public class FunctionCallNode extends ASTNode {
    private final String functionName;
    private final List<ASTNode> arguments;

    public FunctionCallNode(String functionName) {
        this.functionName = functionName;
        this.arguments = new ArrayList<>();
    }

    public FunctionCallNode(String functionName, List<ASTNode> arguments) {
        this.functionName = functionName;
        this.arguments = new ArrayList<>(arguments);
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<ASTNode> getArguments() {
        return arguments;
    }

    public void addArgument(ASTNode argument) {
        this.arguments.add(argument);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "FunctionCallNode{" +
                "functionName='" + functionName + '\'' +
                ", arguments=" + arguments +
                '}';
    }
}
