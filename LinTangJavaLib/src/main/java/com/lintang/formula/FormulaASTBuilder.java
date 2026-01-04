package com.lintang.formula;

import com.lintang.formula.ast.*;

/**
 * Custom visitor implementation that builds an AST from the ANTLR parse tree.
 * This class extends the generated FormulaBaseVisitor.
 */
public class FormulaASTBuilder extends FormulaBaseVisitor<ASTNode> {

    @Override
    public ASTNode visitFormula(FormulaParser.FormulaContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public ASTNode visitExpression(FormulaParser.ExpressionContext ctx) {
        return visit(ctx.additive());
    }

    @Override
    public ASTNode visitAdditive(FormulaParser.AdditiveContext ctx) {
        ASTNode result = visit(ctx.multiplicative(0));

        for (int i = 1; i < ctx.multiplicative().size(); i++) {
            String operator = ctx.getChild(2 * i - 1).getText();
            ASTNode right = visit(ctx.multiplicative(i));
            result = new BinaryOpNode(operator, result, right);
        }

        return result;
    }

    @Override
    public ASTNode visitMultiplicative(FormulaParser.MultiplicativeContext ctx) {
        ASTNode result = visit(ctx.unary(0));

        for (int i = 1; i < ctx.unary().size(); i++) {
            String operator = ctx.getChild(2 * i - 1).getText();
            ASTNode right = visit(ctx.unary(i));
            result = new BinaryOpNode(operator, result, right);
        }

        return result;
    }

    @Override
    public ASTNode visitUnary(FormulaParser.UnaryContext ctx) {
        if (ctx.MINUS() != null || ctx.PLUS() != null) {
            String operator = ctx.getChild(0).getText();
            ASTNode operand = visit(ctx.power());
            return new UnaryOpNode(operator, operand);
        }
        return visit(ctx.power());
    }

    @Override
    public ASTNode visitPower(FormulaParser.PowerContext ctx) {
        ASTNode result = visit(ctx.primary(0));

        for (int i = 1; i < ctx.primary().size(); i++) {
            String operator = ctx.POWER(i - 1).getText();
            ASTNode right = visit(ctx.primary(i));
            result = new BinaryOpNode(operator, result, right);
        }

        return result;
    }

    @Override
    public ASTNode visitPrimary(FormulaParser.PrimaryContext ctx) {
        if (ctx.atom() != null) {
            return visit(ctx.atom());
        } else if (ctx.functionCall() != null) {
            return visit(ctx.functionCall());
        } else if (ctx.cellReference() != null) {
            return visit(ctx.cellReference());
        } else {
            // LPAREN expression RPAREN
            return visit(ctx.expression());
        }
    }

    @Override
    public ASTNode visitAtom(FormulaParser.AtomContext ctx) {
        if (ctx.NUMBER() != null) {
            double value = Double.parseDouble(ctx.NUMBER().getText());
            return new NumberNode(value);
        } else if (ctx.STRING() != null) {
            String text = ctx.STRING().getText();
            // Remove quotes
            String value = text.substring(1, text.length() - 1);
            return new StringNode(value);
        } else if (ctx.BOOLEAN() != null) {
            boolean value = Boolean.parseBoolean(ctx.BOOLEAN().getText());
            return new BooleanNode(value);
        }
        throw new RuntimeException("Unknown atom type");
    }

    @Override
    public ASTNode visitFunctionCall(FormulaParser.FunctionCallContext ctx) {
        String functionName = ctx.IDENTIFIER().getText();
        FunctionCallNode functionNode = new FunctionCallNode(functionName);

        if (ctx.argList() != null) {
            for (FormulaParser.ExpressionContext exprCtx : ctx.argList().expression()) {
                functionNode.addArgument(visit(exprCtx));
            }
        }

        return functionNode;
    }

    @Override
    public ASTNode visitCellReference(FormulaParser.CellReferenceContext ctx) {
        if (ctx.CELL_RANGE() != null) {
            return new CellRangeNode(ctx.CELL_RANGE().getText());
        } else {
            return new CellRefNode(ctx.CELL_REF().getText());
        }
    }
}
