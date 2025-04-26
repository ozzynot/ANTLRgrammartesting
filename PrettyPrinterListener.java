package easycalc;

import easycalc.grammar.*;
import java.util.Stack;

public class PrettyPrinterListener extends EasyCalcBaseListener {

    private final StringBuilder sb = new StringBuilder();
    private final Stack<String> stack = new Stack<>();

    // Program output
    public String getProgramString() {
        return sb.toString() + "$$\n";
    }

    // Exit declaration
    public void exitDeclar(EasyCalcParser.DeclarContext ctx) {
        sb.append(ctx.type.getText()).append(" ").append(ctx.ID().getText()).append(";\n");
    }

    // := appending
    public void exitAssignStmt(EasyCalcParser.AssignStmtContext ctx) {
        String exprStr = stack.pop();
        sb.append(ctx.ID().getText()).append(" := ").append(exprStr).append(";\n");
    }

    // Appends "read ID;" to sb
    public void exitReadStmt(EasyCalcParser.ReadStmtContext ctx) {
        sb.append("read ").append(ctx.ID().getText()).append(";\n");
    }

    // Pops expression and appends "write expr;" to sb
    public void exitWriteStmt(EasyCalcParser.WriteStmtContext ctx) {
        sb.append("write ").append(stack.pop()).append(";\n");
    }

    // Pushes literals to stack
    public void exitLitExpr(EasyCalcParser.LitExprContext ctx) {
        stack.push(ctx.getText());
    }

    // Pushes Identifiers to stack
    public void exitIdExpr(EasyCalcParser.IdExprContext ctx) {
        stack.push(ctx.ID().getText());
    }

    // Wraps top stack value in ()
    public void exitParenExpr(EasyCalcParser.ParenExprContext ctx) {
        String exprStr = stack.pop();
        // Push the modified expression
        stack.push(exprStr);
    }

    // Ensures the corresponding formatted string is stored correctly
    public void exitToExpr(EasyCalcParser.ToExprContext ctx) {
        stack.push(ctx.op.getText() + "(" + stack.pop() + ")");
    }

    // The following several pops right operand then left, and pushes formatted string back
    public void exitMulDivExpr(EasyCalcParser.MulDivExprContext ctx) {
        String right = stack.pop();
        String left = stack.pop();

        //Ensure precedence by adding parentheses around mul/div
        if (left.contains("+") || left.contains("-")) {
            left = "(" + left + ")";
        }
        if (right.contains("+") || right.contains("-")) {
            right = "(" + right + ")";
        }

        stack.push(left + " " + ctx.op.getText() + " " + right);
    }

    public void exitAddSubExpr(EasyCalcParser.AddSubExprContext ctx) {
        String right = stack.pop();
        String left = stack.pop();

        //Ensure precedence by adding parentheses around mul/div
        if (left.contains("*") || left.contains("/")) {
            left = "(" + left + ")";
        }
        if (right.contains("*") || right.contains("/")) {
            right = "(" + right + ")";
        }

        stack.push(left + " " + ctx.op.getText() + " " + right);
    }

    public void exitLessGrtrExpr(EasyCalcParser.LessGrtrExprContext ctx) {
        String right = stack.pop();
        String left = stack.pop();
        stack.push(left + " " + ctx.op.getText() + " " + right);
    }

    public void exitEqualExpr(EasyCalcParser.EqualExprContext ctx) {
        String right = stack.pop();
        String left = stack.pop();
        stack.push(left + " == " + right);
    }

    public void exitAndExpr(EasyCalcParser.AndExprContext ctx) {
        String right = stack.pop();
        String left = stack.pop();
        stack.push(left + " and " + right);
    }

    public void exitOrExpr(EasyCalcParser.OrExprContext ctx) {
        String right = stack.pop();
        String left = stack.pop();
        stack.push(left + " or " + right);
    }

    // Pops else, then, and condition, then pushes the following:
    // "if condition then expr1 else expr2"
    public void exitIfExpr(EasyCalcParser.IfExprContext ctx) {
        String elseExpr = stack.pop();
        String thenExpr = stack.pop();
        String condition = stack.pop();
        stack.push("if " + condition + " then " + thenExpr + " else " + elseExpr);
    }
}
