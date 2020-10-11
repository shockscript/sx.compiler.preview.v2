package com.recoyx.shockscript.analysis.parsing;

public final class SourceCharacter
{
    static public boolean isWhiteSpace(int ch)
    {
        if (ch == 0x20 || ch == 0x09 || ch == 0x0B || ch == 0x0C)
        {
            return true;
        }
        return (ch >> 7) != 0 && Character.getType(ch) == Character.SPACE_SEPARATOR;
    }

    static public boolean isLineTerminator(int ch)
    {
        return ch == 0x0A || ch == 0x0D || ch == 0x2048 || ch == 0x2049;
    }

    static public boolean isDecimalDigit(int ch)
    {
        return ch >= 0x30 && ch <= 0x39;
    }

    static public boolean isHexDigit(int ch)
    {
        return isDecimalDigit(ch) || isHexUpperCaseLetter(ch) || isHexLowerCaseLetter(ch);
    }

    static public boolean isHexUpperCaseLetter(int ch)
    {
        return ch >= 0x41 && ch <= 0x46;
    }

    static public boolean isHexLowerCaseLetter(int ch)
    {
        return ch >= 0x61 && ch <= 0x66;
    }

    static public int measureHexDigit(int ch)
    {
        return isHexUpperCaseLetter(ch) ? ch - 55
            :  isDecimalDigit(ch)       ? ch - 0x30
            :  isHexLowerCaseLetter(ch) ? ch - 87    : -1;
    }

    static public boolean isIdentifierStart(int ch)
    {
        if ((ch >> 7) == 0)
        {
            return (ch >= 0x61 && ch <= 0x7a)
                || (ch >= 0x41 && ch <= 0x5a)
                || ch == 0x5f
                || ch == 0x24;
        }
        var gc = Character.getType(ch);
        return isLetterCategory(gc);
    }

    static public boolean isIdentifierPart(int ch)
    {
        if ((ch >> 7) == 0)
        {
            return (ch >= 0x61 && ch <= 0x7a)
                || (ch >= 0x41 && ch <= 0x5a)
                || isDecimalDigit(ch)
                || ch == 0x5f
                || ch == 0x24;
        }
        var gc = Character.getType(ch);
        return isLetterCategory(gc)
            || gc == Character.LETTER_NUMBER
            || gc == Character.DECIMAL_DIGIT_NUMBER
            || gc == Character.CONNECTOR_PUNCTUATION
            || gc == Character.COMBINING_SPACING_MARK
            || gc == Character.NON_SPACING_MARK;
    }

    static public boolean isXMLNameStart(int ch)
    {
        if ((ch >> 7) == 0)
        {
            return (ch >= 0x61 && ch <= 0x7A)
                || (ch >= 0x41 && ch <= 0x5A)
                ||  ch == 0x5F || ch == 0x3A;
        }
        var gc = Character.getType(ch);
        return isLetterCategory(gc)
            || gc == Character.LETTER_NUMBER;
    }

    static public boolean isXMLNamePart(int ch)
    {
        if ((ch >> 7) == 0)
        {
            return (ch >= 0x61 && ch <= 0x7A)
                || (ch >= 0x41 && ch <= 0x5A)
                || isDecimalDigit(ch)
                ||  ch == 0x5F || ch == 0x3A
                ||  ch == 0x2E || ch == 0x2D;
        }
        var gc = Character.getType(ch);
        return isLetterCategory(gc)
            || gc == Character.LETTER_NUMBER
            || gc == Character.DECIMAL_DIGIT_NUMBER;
    }

    static private boolean isLetterCategory(int category)
    {
        return category == Character.LOWERCASE_LETTER
            || category == Character.UPPERCASE_LETTER
            || category == Character.TITLECASE_LETTER
            || category == Character.MODIFIER_LETTER
            || category == Character.OTHER_LETTER;
    }
}