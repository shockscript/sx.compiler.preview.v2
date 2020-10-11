package com.recoyx.sxc.parser;

import com.recoyx.sxc.semantics.Operator;

public enum Token
{
    EOF(0x0),
    IDENTIFIER(0x1),
    STRING_LITERAL(0x2),
    BOOLEAN_LITERAL(0x3),
    NUMERIC_LITERAL(0x4),
    NULL_LITERAL(0x5),
    THIS_LITERAL(0x6),
    REG_EXP_LITERAL(0x7),
    XML_WHITESPACE(0x8),
    XML_ATTRIBUTE_VALUE(0x9),
    XML_MARKUP(0xA),
    XML_TEXT(0xB),
    XML_NAME(0xC),
    XML_LT_SLASH(0xD),
    XML_SLASH_GT(0xE),

    DOT(0x40),
    DESCENDANTS(0x41),
    ELLIPSIS(0x42),
    COMMA(0x43),
    SEMICOLON(0x44),
    COLON(0x45),
    COLON_COLON(0x46),
    LPAREN(0x47),
    RPAREN(0x48),
    LBRACKET(0x49),
    RBRACKET(0x4A),
    LBRACE(0x4B),
    RBRACE(0x4C),
    ATTRIBUTE(0x4D),
    QUESTION_MARK(0x4E),
    EXCLAMATION_MARK(0x4F),
    INCREMENT(0x50),
    DECREMENT(0x51),
    PLUS(0x52),
    MINUS(0x53),
    TIMES(0x54),
    SLASH(0x55),
    REMAINDER(0x56),
    BIT_AND(0x57),
    BIT_XOR(0x58),
    BIT_OR(0x59),
    BIT_NOT(0x5A),
    LEFT_SHIFT(0x5B),
    RIGHT_SHIFT(0x5C),
    UNSIGNED_RIGHT_SHIFT(0x5D),
    LT(0x5E),
    GT(0x5F),
    LE(0x60),
    GE(0x61),
    EQUALS(0x62),
    NOT_EQUALS(0x63),
    LOGICAL_AND(0x64),
    LOGICAL_OR(0x65),
    STRICT_EQUALS(0x66),
    STRICT_NOT_EQUALS(0x67),

    AS(0x80),
    DO(0x81),
    IF(0x82),
    IN(0x83),
    IS(0x84),
    FOR(0x85),
    NEW(0x86),
    TRY(0x87),
    USE(0x88),
    VAR(0x89),
    CASE(0x8A),
    ELSE(0x8B),
    VOID(0x8C),
    WITH(0x8D),
    BREAK(0x8E),
    CATCH(0x8F),
    CLASS(0x90),
    CONST(0x91),
    SUPER(0x92),
    THROW(0x93),
    WHILE(0x94),
    DELETE(0x95),
    IMPORT(0x96),
    PUBLIC(0x97),
    RETURN(0x98),
    SWITCH(0x99),
    TYPEOF(0x9A),
    DEFAULT(0x9B),
    FINALLY(0x9C),
    PACKAGE(0x9D),
    PRIVATE(0x9E),
    CONTINUE(0x9F),
    FUNCTION(0xA0),
    INTERNAL(0xA1),
    INTERFACE(0xA2),
    PROTECTED(0xA3),
    INSTANCEOF(0xA4),
    YIELD(0xA5),

    ASSIGN(0x100),
    ADD_ASSIGN(0x101),
    SUBTRACT_ASSIGN(0x102),
    MULTIPLY_ASSIGN(0x103),
    DIVIDE_ASSIGN(0x104),
    REMAINDER_ASSIGN(0x105),
    BIT_AND_ASSIGN(0x106),
    BIT_XOR_ASSIGN(0x107),
    BIT_OR_ASSIGN(0x108),
    LOGICAL_AND_ASSIGN(0x109),
    LOGICAL_XOR_ASSIGN(0x10A),
    LOGICAL_OR_ASSIGN(0x10B),
    LSHIFT_ASSIGN(0x10C),
    RSHIFT_ASSIGN(0x10D),
    UNSIGNED_RSHIFT_ASSIGN(0x10E);

    private int _value;

    Token(int value)
    {
        _value = value;
    }

    public int valueOf()
    {
        return _value;
    }

    public boolean isDefaultToken()
    {
        return (valueOf() >> 6) != 0;
    }

    public boolean isPunctuator()
    {
        return (valueOf() >> 6) == 1;
    }

    public boolean isKeyword()
    {
        return (valueOf() >> 7) == 1;
    }

    public boolean isAssign()
    {
        return (valueOf() >> 8) == 1;
    }

    public Operator getCompoundAssignmentOperator()
    {
        if (this == ADD_ASSIGN)
        {
            return Operator.ADD;
        }
        if (this == SUBTRACT_ASSIGN)
        {
            return Operator.SUBTRACT;
        }

        if (this == MULTIPLY_ASSIGN) return Operator.MULTIPLY;
        if (this == DIVIDE_ASSIGN) return Operator.DIVIDE;
        if (this == REMAINDER_ASSIGN) return Operator.REMAINDER;
        if (this == BIT_AND_ASSIGN) return Operator.BITWISE_AND;
        if (this == BIT_XOR_ASSIGN) return Operator.BITWISE_XOR;
        if (this == BIT_OR_ASSIGN) return Operator.BITWISE_OR;
        if (this == LSHIFT_ASSIGN) return Operator.LEFT_SHIFT;
        if (this == RSHIFT_ASSIGN) return Operator.RIGHT_SHIFT;
        if (this == UNSIGNED_RSHIFT_ASSIGN) return Operator.UNSIGNED_RIGHT_SHIFT;
        if (this == LOGICAL_AND_ASSIGN) return Operator.LOGICAL_AND;
        if (this == LOGICAL_XOR_ASSIGN) return Operator.LOGICAL_XOR;
        if (this == LOGICAL_OR_ASSIGN) return Operator.LOGICAL_OR;

        return null;
    }
}