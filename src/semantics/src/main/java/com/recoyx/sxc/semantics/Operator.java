package com.recoyx.sxc.semantics;

public enum Operator
{
    TYPEOF,
    IN,
    INCREMENT,
    DECREMENT,
    POST_INCREMENT,
    POST_DECREMENT,
    DELETE,
    VOID,
    YIELD,
    POSITIVE,
    NEGATE,
    BITWISE_NOT,
    LOGICAL_NOT,
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
    REMAINDER,
    BITWISE_AND,
    BITWISE_XOR,
    BITWISE_OR,
    LEFT_SHIFT,
    RIGHT_SHIFT,
    UNSIGNED_RIGHT_SHIFT,
    LT,
    GT,
    LE,
    GE,
    EQUALS,
    STRICT_EQUALS,
    NOT_EQUALS,
    STRICT_NOT_EQUALS,
    LOGICAL_AND,
    LOGICAL_XOR,
    LOGICAL_OR;

    public String id()
    {
        switch (this)
        {
            case TYPEOF: return "typeof";
            case IN: return "in";
            case DELETE: return "delete";
            case VOID: return "void";
            case YIELD: return "yield";
            case INCREMENT: return "++";
            case DECREMENT: return "--";
            case POST_INCREMENT: return "++";
            case POST_DECREMENT: return "--";
            case POSITIVE: return "+";
            case NEGATE: return "-";
            case BITWISE_NOT: return "~";
            case LOGICAL_NOT: return "!";
            case ADD: return "+";
            case SUBTRACT: return "-";
            case MULTIPLY: return "*";
            case DIVIDE: return "/";
            case REMAINDER: return "%";
            case BITWISE_AND: return "&";
            case BITWISE_XOR: return "^";
            case BITWISE_OR: return "|";
            case LEFT_SHIFT: return "<<";
            case RIGHT_SHIFT: return ">>";
            case UNSIGNED_RIGHT_SHIFT: return ">>>";
            case LT: return "<";
            case GT: return ">";
            case LE: return "<=";
            case GE: return ">=";
            case EQUALS: return "==";
            case NOT_EQUALS: return "!=";
            case LOGICAL_AND: return "&&";
            case LOGICAL_XOR: return "^^";
            case LOGICAL_OR: return "||";
        }
        return "";
    }

    public boolean isUnary()
    {
        return this == INCREMENT      || this == DECREMENT
            || this == POST_INCREMENT || this == POST_DECREMENT
            || this == VOID           || this == POSITIVE
            || this == BITWISE_NOT    || this == NEGATE
            || this == DELETE         || this == LOGICAL_NOT
            || this == TYPEOF;
    }

    public boolean resultsBoolean()
    {
        return this == EQUALS   ||   this == NOT_EQUALS
            || this == LT       ||   this == GT
            || this == LE       ||   this == GE
            || this == DELETE   ||   this == LOGICAL_NOT;
    }

    static public Operator fromQualifiedName(Symbol name)
    {
        var pool = name.pool();
        if (name == pool.proxyNegateName) return NEGATE;
        if (name == pool.proxyBitNotName) return BITWISE_NOT;
        if (name == pool.proxyEqualsName) return EQUALS;
        if (name == pool.proxyNotEqualsName) return NOT_EQUALS;
        if (name == pool.proxyLtName) return LT;
        if (name == pool.proxyGtName) return GT;
        if (name == pool.proxyLeName) return LE;
        if (name == pool.proxyGeName) return GE;
        if (name == pool.proxyAddName) return ADD;
        if (name == pool.proxySubtractName) return SUBTRACT;
        if (name == pool.proxyMultiplyName) return MULTIPLY;
        if (name == pool.proxyDivideName) return DIVIDE;
        if (name == pool.proxyRemainderName) return REMAINDER;
        if (name == pool.proxyBitAndName) return BITWISE_AND;
        if (name == pool.proxyBitXorName) return BITWISE_XOR;
        if (name == pool.proxyBitOrName) return BITWISE_OR;
        if (name == pool.proxyLeftShiftName) return LEFT_SHIFT;
        if (name == pool.proxyRightShiftName) return RIGHT_SHIFT;
        if (name == pool.proxyUnsignedRightShiftName) return UNSIGNED_RIGHT_SHIFT;
        return null;
    }
}