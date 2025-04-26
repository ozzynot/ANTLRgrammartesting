package easycalc;

import easycalc.grammar.*;
import org.antlr.v4.runtime.Token;
import java.util.*;

public class AnalysisListener extends EasyCalcBaseListener{

    private final List<String> errorList = new ArrayList<>();
    private final SortedMap<String, String> symbolTable = new TreeMap<>();
    private final Stack<String> tStack = new Stack<>();
    private final Set<Integer> reportedLines = new HashSet<>();

    public String getSymbolTableString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : symbolTable.entrySet()) {
            sb.append(entry.getKey()).append(" -> ").append(entry.getValue().toUpperCase()).append("\n");
        }
        return sb.toString().trim();
    }

    public String getErrorMessageString() {
        return String.join("\n", errorList);
    }

    /*
    Implementation of Error methods
     */

    private void addRedefErr(Token token) {         // EXAMPLE FROM CLASS, shows how to obtain [line:pos]
        int line = token.getLine();
        int pos = token.getCharPositionInLine() + 1;
        String msg = "redefinition of " + token.getText() + " at " + line + ":" + pos;      // Fixed to match the test cases
        errorList.add(msg);
    }

    private void addUndefErr(Token token) {
        int line = token.getLine();
        int pos = token.getCharPositionInLine() + 1;
        String msg = token.getText() + " undefined at " + line + ":" + pos;
        errorList.add(msg);
    }

    private void addTypeClashErr(Token token) {
        int line = token.getLine();
        int pos = token.getCharPositionInLine() + 1;

        if(reportedLines.contains(line)) return;

        reportedLines.add(line);
        String msg = "Type Clash at " + line + ":" + pos;
        errorList.add(msg);
    }

    private void addArgErr(Token optrtoken, Token opndToken, String type) {
        int line = opndToken.getLine();
        int pos = opndToken.getCharPositionInLine() + 1;
        String msg = optrtoken.getText() + " undefined for " + type.toUpperCase() + " at " + line + ":" + pos;
        errorList.add(msg);
    }

    /*
    Redefinition errors
     */

    @Override
    public void enterDeclar(EasyCalcParser.DeclarContext ctx) {
        String varName = ctx.ID().getText();
        Token varToken = ctx.ID().getSymbol();
        String type = ctx.getStart().getText();

        if (symbolTable.containsKey(varName)) {
            addRedefErr(varToken);
        } else {
            symbolTable.put(varName, type);
        }
    }

    @Override
    public void exitDeclar(EasyCalcParser.DeclarContext ctx) {
        return;
    }

    /*
    Undefined Vars
     */

    @Override
    public void enterIdExpr(EasyCalcParser.IdExprContext ctx) {
        String varName = ctx.ID().getText();
        Token varToken = ctx.ID().getSymbol();

        if(varName.equals("true") || varName.equals("false")) {
            return;
        }

        if (!symbolTable.containsKey(varName)) {
            addUndefErr(varToken);
        } else {
            tStack.push(symbolTable.get(varName));
        }
    }

    @Override
    public void enterAssignStmt(EasyCalcParser.AssignStmtContext ctx) {
        String varName = ctx.ID().getText();
        Token varToken = ctx.ID().getSymbol();

        if (!symbolTable.containsKey(varName)) {
            addUndefErr(varToken);
            return;
        }

        String expectedType = symbolTable.get(varName);

        if(ctx.expr() instanceof EasyCalcParser.IdExprContext) {
            String assignedVar = ((EasyCalcParser.IdExprContext) ctx.expr()).ID().getText();
            if (symbolTable.containsKey(assignedVar)) {
                tStack.push(symbolTable.get(assignedVar));
            }
        }

        if(!tStack.isEmpty()) {
            String assignedType = tStack.pop();

            if (!expectedType.equals(assignedType) && !assignedType.equals("error")) {
                addTypeClashErr(varToken);
            }
        }
    }

    /*
    Type Clash errors in expressions
     */

    @Override
    public void exitMulDivExpr(EasyCalcParser.MulDivExprContext ctx) {
        handleBinaryExpr(ctx.op.getText(), ctx.getStart());
    }

    @Override
    public void exitAddSubExpr(EasyCalcParser.AddSubExprContext ctx) {
        handleBinaryExpr(ctx.op.getText(), ctx.getStart());
    }

    @Override
    public void exitLessGrtrExpr(EasyCalcParser.LessGrtrExprContext ctx) {
        handleBinaryExpr(ctx.op.getText(), ctx.getStart());
    }

    @Override
    public void exitEqualExpr(EasyCalcParser.EqualExprContext ctx) {
        handleBinaryExpr(ctx.op.getText(), ctx.getStart());
    }

    @Override
    public void exitLitExpr(EasyCalcParser.LitExprContext ctx) {
        String val = ctx.getText();

        if(val.equals("true") || val.equals("false")) {
            tStack.push("bool");
        } else if(val.contains(".")) {
            tStack.push("real");
        } else {
            tStack.push("int");
        }
    }

    private void handleBinaryExpr(String op, Token opToken) {
        if(tStack.size() < 2) return;

        String right = tStack.pop();
        String left = tStack.pop();

        Token opndToken  = opToken;



        if(left.equals("bool") || right.equals("bool")) {
            addArgErr(opToken, opToken, "BOOL");
            tStack.push("error");
            return;
        }

        if(!left.equals(right)) {
            addTypeClashErr(opToken);
            tStack.push("error");
        } else {
            tStack.push(left);
        }
    }

    /*
    Argument Errors in to_int, to_real, and bool
     */

    @Override
    public void exitToExpr(EasyCalcParser.ToExprContext ctx) {
        String func = ctx.op.getText();
        Token token = ctx.getStart();
        String operand = tStack.pop();

        if((func.equals("to_int") && !operand.equals("real")) ||
        (func.equals("to_real") && !operand.equals("int"))) {
            addArgErr(token, ctx.expr().getStart(), operand);
            tStack.push("error");
        } else {
            tStack.push(func.equals("to_int") ? "int" : "real");
        }
    }

    @Override
    public void exitAndExpr(EasyCalcParser.AndExprContext ctx) {
        handleBooleanExpr(ctx.op.getText(), ctx.getStart(), ctx.expr(1).getStart());
    }

    @Override
    public void exitOrExpr(EasyCalcParser.OrExprContext ctx) {
        handleBooleanExpr(ctx.op.getText(), ctx.getStart(), ctx.expr(1).getStart());
    }

    private void handleBooleanExpr(String op, Token opToken, Token rightOpndToken) {
        if(tStack.size() < 2) return;

        String right = tStack.pop();
        String left = tStack.pop();

        if(!left.equals("bool") || !right.equals("bool")) {
            String invalidType = !left.equals("bool") ? left : right;
            addArgErr(opToken, opToken, invalidType);
            tStack.push("error");
        } else {
            tStack.push("bool");
        }
    }

    /*
    Handling if Expressions
     */

    @Override
    public void exitIfExpr(EasyCalcParser.IfExprContext ctx) {
        String elseType = tStack.pop();
        String thenType = tStack.pop();
        String ifType = tStack.pop();

        if(!ifType.equals("bool")) {
            addArgErr(ctx.IF().getSymbol(), ctx.expr(0).getStart(), ifType.toUpperCase());
            tStack.push("error");
            return;
        }

        if(!thenType.equals(elseType)) {
            addTypeClashErr(ctx.expr(1).getStart());
        }

        tStack.push(thenType);
    }
}

