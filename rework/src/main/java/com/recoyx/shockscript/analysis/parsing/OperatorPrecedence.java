package com.recoyx.shockscript.analysis.parsing;

public enum OperatorPrecedence
{
    POSTFIX_OPERATOR(15),
    UNARY_OPERATOR(14),
    MULTIPLICATIVE_OPERATOR(13),
    ADDITIVE_OPERATOR(12),
    SHIFT_OPERATOR(11),
    RELATIONAL_OPERATOR(10),
    EQUALITY_OPERATOR(9),
    BIT_AND_OPERATOR(8),
    BIT_XOR_OPERATOR(7),
    BIT_OR_OPERATOR(6),
    LOGICAL_AND_OPERATOR(5),
    LOGICAL_OR_OPERATOR(4),
    TERNARY_OPERATOR(3),
    ASSIGNMENT_OPERATOR(2),
    LIST_OPERATOR(1);

    private int _value;

    OperatorPrecedence(int value)
    {
        _value = value;
    }

    static public OperatorPrecedence fromValue(int value)
    {
        if (value == POSTFIX_OPERATOR.valueOf())
            return POSTFIX_OPERATOR;
        if (value == UNARY_OPERATOR.valueOf())
            return UNARY_OPERATOR;
        if (value == MULTIPLICATIVE_OPERATOR.valueOf())
            return MULTIPLICATIVE_OPERATOR;
        if (value == ADDITIVE_OPERATOR.valueOf())
            return ADDITIVE_OPERATOR;
        if (value == SHIFT_OPERATOR.valueOf())
            return SHIFT_OPERATOR;
        if (value == RELATIONAL_OPERATOR.valueOf())
            return RELATIONAL_OPERATOR;
        if (value == EQUALITY_OPERATOR.valueOf())
            return EQUALITY_OPERATOR;
        if (value == BIT_AND_OPERATOR.valueOf())
            return BIT_AND_OPERATOR;
        if (value == BIT_XOR_OPERATOR.valueOf())
            return BIT_XOR_OPERATOR;
        if (value == BIT_OR_OPERATOR.valueOf())
            return BIT_OR_OPERATOR;
        if (value == LOGICAL_AND_OPERATOR.valueOf())
            return LOGICAL_AND_OPERATOR;
        if (value == LOGICAL_OR_OPERATOR.valueOf())
            return LOGICAL_OR_OPERATOR;
        if (value == TERNARY_OPERATOR.valueOf())
            return TERNARY_OPERATOR;
        if (value == ASSIGNMENT_OPERATOR.valueOf())
            return ASSIGNMENT_OPERATOR;
        if (value == LIST_OPERATOR.valueOf())
            return LIST_OPERATOR;
        return null;
    }

    public int valueOf()
    {
        return _value;
    }
}