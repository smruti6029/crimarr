package com.excel.dynamic.formula.enums;

public enum Operator {
    PLUS("+"),
    MINUS("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    MODULUS("%"),
    POWER("^"),
    AND("&"),
    OR("|"),
    XOR("^"),
    NOT("!"),
    BITWISE_AND("&"),
    BITWISE_OR("|"),
    BITWISE_XOR("^"),
    SHIFT_LEFT("<<"),
    SHIFT_RIGHT(">>"),
    LOGICAL_AND("&&"),
    LOGICAL_OR("||"),
    LESS_THAN("<"),
    GREATER_THAN(">"),
    LESS_THAN_OR_EQUAL("<="),
    GREATER_THAN_OR_EQUAL(">="),
    EQUAL("=="),
    NOT_EQUAL("!="),
    ASSIGN("="),
    INCREMENT("++"),
    DECREMENT("--"),
    OPEN_PARENTHESIS("("),
    CLOSE_PARENTHESIS(")"),
    COMMA(","),
    COLON(":"),
    SEMICOLON(";"),
    QUESTION_MARK("?"),
    DOT("."),
    ELLIPSIS("..."),
    TERNARY_CONDITIONAL("? :"),
    ARROW("->"),
    DOUBLE_ARROW("=>"),
    BITWISE_NOT("~"),
    BITWISE_SHIFT_LEFT("<<<"),
    BITWISE_SHIFT_RIGHT(">>>");

    private final String symbol;

    Operator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public static boolean isOperator(String symbol) {
        for (Operator operator : Operator.values()) {
            if (operator.getSymbol().equals(symbol)) {
                return true;
            }
        }
        return false;
    }
}


