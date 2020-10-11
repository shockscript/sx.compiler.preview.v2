package com.recoyx.shockscript.analysis.semantics;

public enum Operator
{
    TYPEOF("typeof"),
    IN("in"),
    INCREMENT("++"),
    DECREMENT("--"),
    POST_INCREMENT("++"),
    POST_DECREMENT("--"),
    DELETE("delete"),
    VOID("void"),
    YIELD("yield"),
    AWAIT("await"),
    POSITIVE("+"),
    NEGATE("-"),
    BITWISE_NOT("~"),
    LOGICAL_NOT("!"),
    ADD("+"),
    SUBTRACT("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    REMAINDER("%"),
    BITWISE_AND("&"),
    BITWISE_XOR("^"),
    BITWISE_OR("|"),
    LEFT_SHIFT("<<"),
    RIGHT_SHIFT(">>"),
    UNSIGNED_RIGHT_SHIFT(">>>"),
    LT("<"),
    GT(">"),
    LE("<="),
    GE(">="),
    EQUALS("=="),
    STRICT_EQUALS("==="),
    NOT_EQUALS("!="),
    STRICT_NOT_EQUALS("!=="),
    LOGICAL_AND("&&"),
    LOGICAL_XOR("^^"),
    LOGICAL_OR("||");

    private String _name;

    Operator(String name)
    {
        this._name = name;
    }

    public boolean isUnary()
    {
        return this == INCREMENT      || this == DECREMENT
            || this == POST_INCREMENT || this == POST_DECREMENT
            || this == VOID           || this == POSITIVE
            || this == BITWISE_NOT    || this == NEGATE
            || this == DELETE         || this == LOGICAL_NOT
            || this == TYPEOF         || this == YIELD;
    }

    public boolean resultsBoolean()
    {
        return this == EQUALS        ||   this == NOT_EQUALS
            || this == STRICT_EQUALS ||   this == STRICT_NOT_EQUALS
            || this == LT            ||   this == GT
            || this == LE            ||   this == GE
            || this == DELETE        ||   this == LOGICAL_NOT
            || this == IN;
    }

    @Override
    public String toString()
    {
        return this.name();
    }

    static public Operator fromProxyName(Context context, Symbol name)
    {
        if (name == context.proxyNegateName) return NEGATE;
        if (name == context.proxyBitNotName) return BITWISE_NOT;
        if (name == context.proxyEqualsName) return STRICT_EQUALS;
        if (name == context.proxyNotEqualsName) return STRICT_NOT_EQUALS;
        if (name == context.proxyLessThanName) return LT;
        if (name == context.proxyGreaterThanName) return GT;
        if (name == context.proxyLessThanOrEqualsName) return LE;
        if (name == context.proxyGreaterThanOrEqualsName) return GE;
        if (name == context.proxyAddName) return ADD;
        if (name == context.proxySubtractName) return SUBTRACT;
        if (name == context.proxyMultiplyName) return MULTIPLY;
        if (name == context.proxyDivideName) return DIVIDE;
        if (name == context.proxyRemainderName) return REMAINDER;
        if (name == context.proxyBitAndName) return BITWISE_AND;
        if (name == context.proxyBitXorName) return BITWISE_XOR;
        if (name == context.proxyBitOrName) return BITWISE_OR;
        if (name == context.proxyLeftShiftName) return LEFT_SHIFT;
        if (name == context.proxyRightShiftName) return RIGHT_SHIFT;
        if (name == context.proxyUnsignedRightShiftName) return UNSIGNED_RIGHT_SHIFT;
        return null;
    }
}