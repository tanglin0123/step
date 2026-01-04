package com.lintang.formula.ast;

/**
 * Base class for all AST nodes in Excel formula abstract syntax tree.
 */
public abstract class ASTNode {
    public abstract <T> T accept(ASTVisitor<T> visitor);
}
