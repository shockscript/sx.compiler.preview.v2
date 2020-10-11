package com.recoyx.sxc.parser;

import com.recoyx.sxc.util.VectorUtils;
import java.util.stream.IntStream;
import java.util.Vector;

public final class Lexer
{
    static public enum Mode
    {
        NORMAL,
        XML_TAG,
        XML_CONTENT;
    }

    public TokenMetrics token = new TokenMetrics();
    public Lexer.Mode mode = Lexer.Mode.NORMAL;

    private int[] _charCodes;
    protected int index = 0;
    protected int line = 1;
    private int _sliceStart = -1;
    private Script _script;

    static private int[] UNSIGNED_RSHIFT_ASSIGN = ">>>=".codePoints().toArray();
    static private int[] ELLIPSIS = "...".codePoints().toArray();
    static private int[] LOGICAL_AND_ASSIGN = "&&=".codePoints().toArray();
    static private int[] LOGICAL_XOR_ASSIGN = "^^=".codePoints().toArray();
    static private int[] LOGICAL_OR_ASSIGN = "||=".codePoints().toArray();
    static private int[] UNSIGNED_RIGHT_SHIFT = ">>>".codePoints().toArray();
    static private int[] EQUALS_3 = "===".codePoints().toArray();
    static private int[] NOT_EQUALS_3 = "!==".codePoints().toArray();
    static private int[] LSHIFT_ASSIGN = "<<=".codePoints().toArray();
    static private int[] RSHIFT_ASSIGN = ">>=".codePoints().toArray();
    static private int[] DESCENDANTS = "..".codePoints().toArray();
    static private int[] COLON_COLON = "::".codePoints().toArray();
    static private int[] INCREMENT = "++".codePoints().toArray();
    static private int[] DECREMENT = "--".codePoints().toArray();
    static private int[] LEFT_SHIFT = "<<".codePoints().toArray();
    static private int[] RIGHT_SHIFT = ">>".codePoints().toArray();
    static private int[] EQUALS = "==".codePoints().toArray();
    static private int[] NOT_EQUALS = "!=".codePoints().toArray();
    static private int[] LE = "<=".codePoints().toArray();
    static private int[] GE = ">=".codePoints().toArray();
    static private int[] LOGICAL_AND = "&&".codePoints().toArray();
    static private int[] LOGICAL_OR = "||".codePoints().toArray();
    static private int[] ADD_ASSIGN = "+=".codePoints().toArray();
    static private int[] SUBTRACT_ASSIGN = "-=".codePoints().toArray();
    static private int[] MULTIPLY_ASSIGN = "*=".codePoints().toArray();
    static private int[] DIVIDE_ASSIGN = "/=".codePoints().toArray();
    static private int[] REMAINDER_ASSIGN = "%=".codePoints().toArray();
    static private int[] BIT_AND_ASSIGN = "&=".codePoints().toArray();
    static private int[] BIT_XOR_ASSIGN = "^=".codePoints().toArray();
    static private int[] BIT_OR_ASSIGN = "|=".codePoints().toArray();

    static private int[] XML_CDATA_START = "![CDATA[".codePoints().toArray();
    static private int[] XML_COMMENT_START = "!--".codePoints().toArray();

    static private final String NEWLINE = Character.toString((char) 0x0a);
    static private final String CARRIAGE_RETURN = Character.toString((char) 0x0d);
    static private final String VERTICAL_TAB = Character.toString((char) 0x0b);
    static private final String TAB = Character.toString((char) 9);
    static private final String B_ESCAPE_VALUE = Character.toString((char) 8);
    static private final String F_ESCAPE_VALUE = Character.toString((char) 0x0c);
    static private final String NULL_ESCAPE_VALUE = Character.toString((char) 0);

    public Lexer(Script script)
    {
        _script = script;
        _charCodes = script.source().codePoints().toArray();
    }

    private static String stringifyCharCode(int charCode)
    {
        return new String(new int[] {charCode}, 0, 1);
    }

    public Script script()
    {
        return _script;
    }

    public int getLineIndentation(int lineNum)
    {
        int lineStart = _script.getLineStart(lineNum);
        int i = lineStart;
        while (i != _charCodes.length && SxcSourceCharacter.isWhiteSpace(_charCodes[i]))
        {
            ++i;
        }
        return i - lineStart;
    }

    public boolean hasRemaining()
    {
        return _charCodes.length != index;
    }

    public int shiftCharCode()
    {
        return _charCodes[index++];
    }

    public int lookahead(int index)
    {
        var index2 = this.index + index;
        return index2 < _charCodes.length ? _charCodes[index2] : 0;
    }

    public void beginSlice()
    {
        _sliceStart = index;
    }

    public String endSlice()
    {
        if (_sliceStart == -1)
        {
            return "";
        }
        return new String(_charCodes, _sliceStart, index - _sliceStart);
    }

    public boolean matchingArray(int[] charCodes)
    {
        if ((index + charCodes.length) >= this._charCodes.length)
        {
            return false;
        }
        for (int i = 0; i != charCodes.length; ++i)
        {
            if (this._charCodes[index + i] != charCodes[i])
            {
                return false;
            }
        }
        return true;
    }

    private Problem reportUnexpectedCharacter()
    {
        Problem problem = null;
        if (!hasRemaining())
        {
            problem = new Problem("syntaxError", Problem.Constants.UNEXPECTED_EOF, getCharacterPointer(), _script);
        }
        else
        {
            problem = new Problem("syntaxError", Problem.Constants.UNEXPECTED_CHARACTER, getCharacterPointer(), _script, Problem.Argument.createQuote(stringifyCharCode(lookahead(0))));
        }
        return _script.collectProblem(problem);
    }

    private Span getCharacterPointer()
    {
        return Span.pointer(line, index);
    }

    private Problem.Argument getCharacterArgument()
    {
        return hasRemaining() ? Problem.Argument.createQuote(stringifyCharCode(lookahead(0))) : Problem.Argument.createToken(Token.EOF);
    }

    private void beginToken()
    {
        token.start = index;
        token.firstLine = line;
    }

    private void endToken(Token type)
    {
        token.type = type;
        token.end = index;
        token.lastLine = line;
    }

    private void endFixedToken(Token type, int length)
    {
        index += length;
        endToken(type);
    }

    public void shift()
    {
        if (mode == Lexer.Mode.NORMAL)
        {
            normalScan();
        }
        else if (mode == Lexer.Mode.XML_TAG)
        {
            xmlTagScan();
        }
        else
        {
            xmlContentScan();
        }
    }

    public void scanRegExpLiteral()
    {
        int ch = 0;
        index = token.start + 1;
        beginSlice();
        for (;;)
        {
            ch = lookahead(0);
            if (ch == 0x5C)
            {
                shiftCharCode();
                if (!scanLineTerminator(lookahead(0)))
                {
                    if (!hasRemaining())
                    {
                        throw reportUnexpectedCharacter();
                    }
                    shiftCharCode();
                }
            }
            else if (ch == 0x2F)
            {
                break;
            }
            else if (!hasRemaining())
            {
                throw reportUnexpectedCharacter();
            }
            else if (!scanLineTerminator(ch))
            {
                shiftCharCode();
            }
        }
        var body = endSlice();
        shiftCharCode();
        var flags = "";

        for (;;)
        {
            ch = lookahead(0);
            if (SxcSourceCharacter.isIdentifierPart(ch))
            {
                flags += stringifyCharCode(shiftCharCode());
            }
            else if (ch == 0x5C)
            {
                var start = index;
                shiftCharCode();
                if (lookahead(0) == 0x75)
                {
                    shiftCharCode();
                    ch = scanUnicodeEscape();
                    flags += stringifyCharCode(ch);
                }
                else
                {
                    if (!hasRemaining())
                    {
                        throw reportUnexpectedCharacter();
                    }
                    flags += stringifyCharCode(shiftCharCode());
                }
            }
            else
            {
                break;
            }
        }

        endToken(Token.REG_EXP_LITERAL);
        token.stringValue = body;
        token.regExpFlags = flags;
    }

    private void normalScan()
    {
        int ch = 0;
        for (;;)
        {
            ch = lookahead(0);
            if (SxcSourceCharacter.isWhiteSpace(ch))
            {
                shiftCharCode();
            }
            else if (!scanLineTerminator(ch) && !scanComment())
            {
                break;
            }
        }

        beginToken();

        if (SxcSourceCharacter.isIdentifierStart(ch))
        {
            beginSlice();
            shiftCharCode();
            for (;;)
            {
                ch = lookahead(0);
                if (SxcSourceCharacter.isIdentifierPart(ch))
                {
                    shiftCharCode();
                }
                else if (ch == 0x5C)
                {
                    scanEscapedIdentifier(endSlice());
                    break;
                }
                else
                {
                    var idString = endSlice();
                    var keyword = filterKeyword(idString);

                    if (keyword == null)
                    {
                        endToken(Token.IDENTIFIER);
                        token.stringValue = idString;
                    }
                    else
                    {
                        endToken(keyword);
                    }

                    break;
                }
            }
        }
        else if (SxcSourceCharacter.isDecimalDigit(ch))
        {
            scanNumericLiteral(ch, false);
        }
        else
        {
            if (matchingArray(UNSIGNED_RSHIFT_ASSIGN))
            {
                endFixedToken(Token.UNSIGNED_RSHIFT_ASSIGN, 4);
                return;
            }

            if (matchingArray(ELLIPSIS))
            {
                endFixedToken(Token.ELLIPSIS, 3);
                return;
            }
            if (matchingArray(LOGICAL_AND_ASSIGN))
            {
                endFixedToken(Token.LOGICAL_AND_ASSIGN, 3);
                return;
            }
            if (matchingArray(LOGICAL_XOR_ASSIGN))
            {
                endFixedToken(Token.LOGICAL_XOR_ASSIGN, 3);
                return;
            }
            if (matchingArray(LOGICAL_OR_ASSIGN))
            {
                endFixedToken(Token.LOGICAL_OR_ASSIGN, 3);
                return;
            }
            if (matchingArray(UNSIGNED_RIGHT_SHIFT))
            {
                endFixedToken(Token.UNSIGNED_RIGHT_SHIFT, 3);
                return;
            }
            if (matchingArray(EQUALS_3))
            {
                endFixedToken(Token.STRICT_EQUALS, 3);
                return;
            }
            if (matchingArray(NOT_EQUALS_3))
            {
                endFixedToken(Token.STRICT_NOT_EQUALS, 3);
                return;
            }
            if (matchingArray(LSHIFT_ASSIGN))
            {
                endFixedToken(Token.LSHIFT_ASSIGN, 3);
                return;
            }
            if (matchingArray(RSHIFT_ASSIGN))
            {
                endFixedToken(Token.RSHIFT_ASSIGN, 3);
                return;
            }
            if (matchingArray(DESCENDANTS))
            {
                endFixedToken(Token.DESCENDANTS, 2);
                return;
            }
            if (matchingArray(COLON_COLON))
            {
                endFixedToken(Token.COLON_COLON, 2);
                return;
            }
            if (matchingArray(INCREMENT))
            {
                endFixedToken(Token.INCREMENT, 2);
                return;
            }
            if (matchingArray(DECREMENT))
            {
                endFixedToken(Token.DECREMENT, 2);
                return;
            }
            if (matchingArray(LEFT_SHIFT))
            {
                endFixedToken(Token.LEFT_SHIFT, 2);
                return;
            }
            if (matchingArray(RIGHT_SHIFT))
            {
                endFixedToken(Token.RIGHT_SHIFT, 2);
                return;
            }
            if (matchingArray(EQUALS))
            {
                endFixedToken(Token.EQUALS, 2);
                return;
            }
            if (matchingArray(NOT_EQUALS))
            {
                endFixedToken(Token.NOT_EQUALS, 2);
                return;
            }
            if (matchingArray(LE))
            {
                endFixedToken(Token.LE, 2);
                return;
            }
            if (matchingArray(GE))
            {
                endFixedToken(Token.GE, 2);
                return;
            }
            if (matchingArray(LOGICAL_AND))
            {
                endFixedToken(Token.LOGICAL_AND, 2);
                return;
            }
            if (matchingArray(LOGICAL_OR))
            {
                endFixedToken(Token.LOGICAL_OR, 2);
                return;
            }
            if (matchingArray(ADD_ASSIGN))
            {
                endFixedToken(Token.ADD_ASSIGN, 2);
                return;
            }
            if (matchingArray(SUBTRACT_ASSIGN))
            {
                endFixedToken(Token.SUBTRACT_ASSIGN, 2);
                return;
            }
            if (matchingArray(MULTIPLY_ASSIGN))
            {
                endFixedToken(Token.MULTIPLY_ASSIGN, 2);
                return;
            }
            if (matchingArray(DIVIDE_ASSIGN))
            {
                endFixedToken(Token.DIVIDE_ASSIGN, 2);
                return;
            }
            if (matchingArray(REMAINDER_ASSIGN))
            {
                endFixedToken(Token.REMAINDER_ASSIGN, 2);
                return;
            }
            if (matchingArray(BIT_AND_ASSIGN))
            {
                endFixedToken(Token.BIT_AND_ASSIGN, 2);
                return;
            }
            if (matchingArray(BIT_XOR_ASSIGN))
            {
                endFixedToken(Token.BIT_XOR_ASSIGN, 2);
                return;
            }
            if (matchingArray(BIT_OR_ASSIGN))
            {
                endFixedToken(Token.BIT_OR_ASSIGN, 2);
                return;
            }

            switch (ch)
            {
                case 0x2e:
                {
                    if (SxcSourceCharacter.isDecimalDigit(lookahead(1)))
                    {
                        scanNumericLiteral(0, true);
                        return;
                    }
                    endFixedToken(Token.DOT, 1);
                    return;
                }
                case 0x2c:
                    endFixedToken(Token.COMMA, 1);
                    return;
                case 0x3d:
                    endFixedToken(Token.ASSIGN, 1);
                    return;
                case 0x3a:
                    endFixedToken(Token.COLON, 1);
                    return;
                case 0x3b:
                    endFixedToken(Token.SEMICOLON, 1);
                    return;
                case 0x28:
                    endFixedToken(Token.LPAREN, 1);
                    return;
                case 0x29:
                    endFixedToken(Token.RPAREN, 1);
                    return;
                case 0x5B:
                    endFixedToken(Token.LBRACKET, 1);
                    return;
                case 0x5D:
                    endFixedToken(Token.RBRACKET, 1);
                    return;
                case 0x7B:
                    endFixedToken(Token.LBRACE, 1);
                    return;
                case 0x7D:
                    endFixedToken(Token.RBRACE, 1);
                    return;
                case 0x22:
                case 0x27:
                    scanStringLiteral(ch);
                    return;
                case 0x40:
                    endFixedToken(Token.ATTRIBUTE, 1);
                    return;
                case 0x3f:
                    endFixedToken(Token.QUESTION_MARK, 1);
                    return;
                case 0x21:
                    endFixedToken(Token.EXCLAMATION_MARK, 1);
                    return;
                case 0x2b:
                    endFixedToken(Token.PLUS, 1);
                    return;
                case 0x2d:
                    endFixedToken(Token.MINUS, 1);
                    return;
                case 0x2a:
                    endFixedToken(Token.TIMES, 1);
                    return;
                case 0x2f:
                    endFixedToken(Token.SLASH, 1);
                    return;
                case 0x25:
                    endFixedToken(Token.REMAINDER, 1);
                    return;
                case 0x26:
                    endFixedToken(Token.BIT_AND, 1);
                    return;
                case 0x5e:
                    endFixedToken(Token.BIT_XOR, 1);
                    return;
                case 0x7c:
                    endFixedToken(Token.BIT_OR, 1);
                    return;
                case 0x7e:
                    endFixedToken(Token.BIT_NOT, 1);
                    return;
                case 0x3c:
                    endFixedToken(Token.LT, 1);
                    return;
                case 0x3e:
                    endFixedToken(Token.GT, 1);
                    return;
                // escape identifier
                case 0x5c:
                {
                    shiftCharCode();
                    if (lookahead(0) == 0x75)
                    {
                        shiftCharCode();
                        ch = scanUnicodeEscape();
                    }
                    else
                    {
                        if (!hasRemaining())
                        {
                            throw reportUnexpectedCharacter();
                        }
                        ch = shiftCharCode();
                    }
                    scanEscapedIdentifier(ch == 0x23 ? "" : stringifyCharCode(ch));
                    return;
                }
                default:
                {
                    if (hasRemaining())
                    {
                        throw this.reportUnexpectedCharacter();
                    }
                    else
                    {
                        endToken(Token.EOF);
                    }
                }
            }
        }
    }

    private boolean scanLineTerminator(int ch)
    {
        if (SxcSourceCharacter.isLineTerminator(ch))
        {
            if (ch == 0x0d && lookahead(1) == 0x0a)
            {
                shiftCharCode();
            }
            shiftCharCode();
            ++line;
            _script.lineStarts.push(index);
            return true;
        }
        return false;
    }

    private boolean scanComment()
    {
        if (lookahead(0) != 0x2f)
        {
            return false;
        }

        int ch = lookahead(1);

        if (ch == 0x2a)
        {
            var span = getCharacterPointer();
            index += 2;
            var builder = new StringBuilder();
            int nested_sections = 1;
            beginSlice();

            for (;;)
            {
                ch = lookahead(0);
                if (ch == 0x2a && lookahead(1) == 0x2f)
                {
                    if ((--nested_sections) != 0)
                    {
                        index += 2;
                    }
                    else
                    {
                        builder.append(endSlice());
                        index += 2;
                        break;
                    }
                }
                else if (ch == 0x2f && lookahead(1) == 0x2a)
                {
                    ++nested_sections;
                    index += 2;
                }
                else if (SxcSourceCharacter.isLineTerminator(ch))
                {
                    builder.append(endSlice());
                    scanLineTerminator(ch);
                    builder.append(ch == 0x0d ? "\n" : stringifyCharCode(ch));
                    beginSlice();
                }
                else if (hasRemaining())
                {
                    shiftCharCode();
                }
                else
                {
                    throw reportUnexpectedCharacter();
                }
            }

            var node = new Ast.CommentNode(builder.toString(), true);
            node.span = new Span(span.firstLine(), span.start(), line, index);
            _script.comments.add(node);
            return true;
        }

        if (ch == 0x2f)
        {
            var start = index;
            index += 2;
            beginSlice();

            while (hasRemaining() && !SxcSourceCharacter.isLineTerminator(lookahead(0)))
            {
                shiftCharCode();
            }

            var content = endSlice();
            var node = new Ast.CommentNode(content, false);
            node.span = new Span(line, start, line, index);
            _script.comments.add(node);
            return true;
        }

        return false;
    }

    private void scanNumericLiteral(int ch, boolean onDot)
    {
        beginSlice();
        shiftCharCode();

        boolean prependDot = onDot;
        boolean appendDot = false;
        boolean appendZero = false;

        if (onDot)
        {
            do
            {
                shiftCharCode();
            }
            while (SxcSourceCharacter.isDecimalDigit(lookahead(0)));
        }
        else
        {
            if (ch == 0x30)
            {
                ch = lookahead(0);
                if (ch == 0x78 || ch == 0x58)
                {
                    scanHexLiteral();
                    return;
                }
            }
            else
            {
                while (SxcSourceCharacter.isDecimalDigit(lookahead(0)))
                {
                    shiftCharCode();
                }
            }

            appendDot = true;

            if (lookahead(0) == 0x2e)
            {
                appendDot = false;
                appendZero = true;
                shiftCharCode();
                while (SxcSourceCharacter.isDecimalDigit(lookahead(0)))
                {
                    appendZero = false;
                    shiftCharCode();
                }
            }
        }

        ch = lookahead(0);

        if (ch == 0x65 || ch == 0x45)
        {
            appendDot = false;
            appendZero = false;
            shiftCharCode();
            ch = lookahead(0);
            if (ch == 0x2b || ch == 0x2d)
            {
                shiftCharCode();
            }
            if (!SxcSourceCharacter.isDecimalDigit(lookahead(0)))
            {
                throw reportUnexpectedCharacter();
            }
            while (SxcSourceCharacter.isDecimalDigit(lookahead(0)))
            {
                shiftCharCode();
            }
        }

        endToken(Token.NUMERIC_LITERAL);
        token.numberValue = Double.parseDouble((prependDot ? "0." : "") + endSlice() + (appendDot ? ".0" : appendZero ? "0" : ""));
    }

    private void scanHexLiteral()
    {
        shiftCharCode();
        var value = scanHexDigit();
        int digit = -1;
        while ((digit = SxcSourceCharacter.measureHexDigit(lookahead(0))) != -1)
        {
            value = (value << 4) | digit;
            shiftCharCode();
        }
        endToken(Token.NUMERIC_LITERAL);
        token.numberValue = value;
    }

    private void scanStringLiteral(int delim)
    {
        shiftCharCode();
        if (lookahead(0) == delim && lookahead(1) == delim)
        {
            scanTripleStringLiteral(delim);
            return;
        }
        StringBuilder builder = null;
        beginSlice();

        for (;;)
        {
            int ch = lookahead(0);
            if (ch == 0x5c)
            {
                builder = builder == null ? new StringBuilder() : builder;
                builder.append(endSlice());
                builder.append(scanEscapeSequence());
                beginSlice();
            }
            else if (ch == delim)
            {
                break;
            }
            else if (SxcSourceCharacter.isLineTerminator(ch))
            {
                _script.collectProblem(new Problem("syntaxError", Problem.Constants.ILLEGAL_LINE_BREAK_INSIDE_STRING, getCharacterPointer(), _script));
                scanLineTerminator(ch);
            }
            else if (!hasRemaining())
            {
                throw reportUnexpectedCharacter();
            }
            else
            {
                shiftCharCode();
            }
        }

        var str = endSlice();
        if (builder != null)
        {
            builder.append(str);
        }
        shiftCharCode();
        endToken(Token.STRING_LITERAL);
        token.stringValue = builder == null ? str : builder.toString();
    }

    private void scanTripleStringLiteral(int delim)
    {
        index += 2;
        var lines = new Vector<String>();
        var builder = new StringBuilder();
        scanLineTerminator(lookahead(0));
        beginSlice();

        for (;;)
        {
            var ch = lookahead(0);
            if (ch == 0x5c)
            {
                builder.append(endSlice());
                builder.append(scanEscapeSequence());
                beginSlice();
            }
            else if (ch == delim && lookahead(1) == delim && lookahead(2) == delim)
            {
                break;
            }
            else if (SxcSourceCharacter.isLineTerminator(ch))
            {
                builder.append(endSlice());
                lines.add(builder.toString());
                builder = new StringBuilder();
                scanLineTerminator(ch);
                beginSlice();
            }
            else if (!hasRemaining())
            {
                throw reportUnexpectedCharacter();
            }
            else
            {
                shiftCharCode();
            }
        }

        builder.append(endSlice());
        lines.add(builder.toString());
        index += 3;
        endToken(Token.STRING_LITERAL);
        token.stringValue = joinTripleStringLiteralLines(lines);
    }

    private String joinTripleStringLiteralLines(Vector<String> lines)
    {
        var lead_line = VectorUtils.pop(lines);
        int indent = 0;
        int i = 0;

        for (i = 0; i != lead_line.length(); ++i)
        {
            if (!SxcSourceCharacter.isWhiteSpace(lead_line.codePointAt(i)))
            {
                break;
            }
        }

        indent = i;

        for (int j = 0; j != lines.size(); ++j)
        {
            var line = lines.get(j);
            // count whitespace to eliminate
            for (i = 0; i != line.length(); ++i)
            {
                if (!SxcSourceCharacter.isWhiteSpace(line.codePointAt(i)) || i >= indent)
                {
                    break;
                }
            }
            lines.set(j, line.substring(i));
        }

        lines.add(lead_line.substring(indent));
        return VectorUtils.join(lines, "\n");
    }

    private void scanEscapedIdentifier(String lastString)
    {
        var builder = new StringBuilder();
        builder.append(lastString);
        beginSlice();

        for (;;)
        {
            var ch = lookahead(0);
            if (SxcSourceCharacter.isIdentifierPart(ch))
            {
                shiftCharCode();
            }
            else if (ch == 0x5c)
            {
                builder.append(endSlice());
                shiftCharCode();
                if (lookahead(0) == 0x75)
                {
                    shiftCharCode();
                    ch = scanUnicodeEscape();
                }
                else
                {
                    if (!hasRemaining())
                    {
                        throw reportUnexpectedCharacter();
                    }
                    ch = shiftCharCode();
                }
                builder.append(ch == 0x23 ? "" : stringifyCharCode(ch));
                beginSlice();
            }
            else
            {
                break;
            }
        }

        builder.append(endSlice());
        endToken(Token.IDENTIFIER);
        token.stringValue = builder.toString();
    }

    private Token filterKeyword(String str)
    {
        switch (str.length())
        {
            case 1:
                return null;
            case 2:
                return  str.equals("as") ? Token.AS :
                        str.equals("do") ? Token.DO :
                        str.equals("if") ? Token.IF :
                        str.equals("in") ? Token.IN :
                        str.equals("is") ? Token.IS : null;
            case 3:
                return  str.equals("for") ? Token.FOR :
                        str.equals("new") ? Token.NEW :
                        str.equals("try") ? Token.TRY :
                        str.equals("use") ? Token.USE :
                        str.equals("var") ? Token.VAR : null;
            case 4:
                if (str.equals("true"))
                {
                    token.booleanValue = true;
                    return Token.BOOLEAN_LITERAL;
                }
                return  str.equals("case") ? Token.CASE :
                        str.equals("else") ? Token.ELSE :
                        str.equals("null") ? Token.NULL_LITERAL :
                        str.equals("this") ? Token.THIS_LITERAL :
                        str.equals("void") ? Token.VOID :
                        str.equals("with") ? Token.WITH : null;
            case 5:
                if (str.equals("false"))
                {
                    this.token.booleanValue = false;
                    return Token.BOOLEAN_LITERAL;
                }
                return  str.equals("break") ? Token.BREAK :
                        str.equals("catch") ? Token.CATCH :
                        str.equals("class") ? Token.CLASS :
                        str.equals("const") ? Token.CONST :
                        str.equals("super") ? Token.SUPER :
                        str.equals("throw") ? Token.THROW :
                        str.equals("while") ? Token.WHILE :
                        str.equals("yield") ? Token.YIELD : null;
            case 6:
                return  str.equals("delete") ? Token.DELETE :
                        str.equals("import") ? Token.IMPORT :
                        str.equals("public") ? Token.PUBLIC :
                        str.equals("return") ? Token.RETURN :
                        str.equals("switch") ? Token.SWITCH :
                        str.equals("typeof") ? Token.TYPEOF : null;
            case 7:
                return  str.equals("default") ? Token.DEFAULT :
                        str.equals("finally") ? Token.FINALLY :
                        str.equals("package") ? Token.PACKAGE :
                        str.equals("private") ? Token.PRIVATE : null;
            case 8:
                return  str.equals("continue") ? Token.CONTINUE :
                        str.equals("function") ? Token.FUNCTION :
                        str.equals("internal") ? Token.INTERNAL : null;
            case 9:
                return  str.equals("interface") ? Token.INTERFACE :
                        str.equals("protected") ? Token.PROTECTED : null;
            case 10:
                return  str.equals("instanceof") ? Token.INSTANCEOF : null;
            default:
                return null;
        }
    }

    private String scanEscapeSequence()
    {
        shiftCharCode();
        if (!hasRemaining())
        {
            throw reportUnexpectedCharacter();
        }
        var ch = shiftCharCode();
        switch (ch)
        {
            case 0x75:
                return stringifyCharCode(this.scanUnicodeEscape());
            case 0x78:
                return stringifyCharCode((this.scanHexDigit() << 4) | this.scanHexDigit());
            case 0x6e:
                return NEWLINE;
            case 0x72:
                return CARRIAGE_RETURN;
            case 0x76:
                return VERTICAL_TAB;
            case 0x74:
                return TAB;
            case 0x62:
                return B_ESCAPE_VALUE;
            case 0x66:
                return F_ESCAPE_VALUE;
            case 0x30:
                return NULL_ESCAPE_VALUE;
            case 0x22:
                return "\"";
            case 0x27:
                return "'";
            case 0x5C:
                return "\\";
        }
        --index;
        if (scanLineTerminator(ch))
        {
            return "";
        }
        reportUnexpectedCharacter();
        shiftCharCode();
        return "";
    }

    private int scanUnicodeEscape()
    {
        var ch = lookahead(0);
        if (ch == 0x7b)
        {
            shiftCharCode();
            var r = scanHexDigit();
            for (;;)
            {
                ch = lookahead(0);
                if (ch == 0x7d)
                {
                    shiftCharCode();
                    break;
                }
                r = (r << 4) | scanHexDigit();
            }
            if (r > 0x10ffff)
            {
                r = 0;
            }
            return r;
        }
        else
        {
            return (scanHexDigit() << 12)
                |  (scanHexDigit() <<  8)
                |  (scanHexDigit() <<  4)
                |   scanHexDigit();
        }
    }

    private int scanHexDigit()
    {
        int value = SxcSourceCharacter.measureHexDigit(lookahead(0));
        if (value == -1)
        {
            throw _script.collectProblem(new Problem("syntaxError", Problem.Constants.EXPECTING_BEFORE, this.getCharacterPointer(), _script, Problem.Argument.createTerm("hexDigit"), this.getCharacterArgument()));
        }
        shiftCharCode();
        return value;
    }

    private void xmlTagScan()
    {
        beginToken();
        beginSlice();
        var ch = lookahead(0);
        if (ch == 0x20)
        {
            shiftCharCode();
            scanXMLWhitespace();
        }
        else if (SxcSourceCharacter.isXMLNameStart(ch))
        {
            do
            {
                shiftCharCode();
            }
            while (SxcSourceCharacter.isXMLNamePart(lookahead(0)));
            endToken(Token.XML_NAME);
            token.stringValue = endSlice();
        }
        else if (ch == 0x22 || ch == 0x27)
        {
            scanXMLAttributeValue(ch);
        }
        else if (ch == 0x2f && lookahead(1) == 0x3e)
        {
            endFixedToken(Token.XML_SLASH_GT, 2);
        }
        else if (ch == 0x3e)
        {
            endFixedToken(Token.GT, 1);
        }
        else if (ch == 0x3d)
        {
            endFixedToken(Token.ASSIGN, 1);
        }
        else if (ch == 0x7b)
        {
            endFixedToken(Token.LBRACE, 1);
        }
        else if (ch == 0x0a || ch == 0x09 || ch == 0x0d)
        {
            scanXMLWhitespace();
        }
        else
        {
            throw reportUnexpectedCharacter();
        }
    }

    private void scanXMLWhitespace()
    {
        for (;;)
        {
            var ch = lookahead(0);
            if (ch == 0x20 || ch == 0x09)
            {
                shiftCharCode();
            }
            else if (ch == 0x0d || ch == 0x0a)
            {
                scanLineTerminator(ch);
            }
            else
            {
                break;
            }
        }
        endToken(Token.XML_WHITESPACE);
    }

    private void scanXMLAttributeValue(int delim)
    {
        shiftCharCode();
        beginSlice();

        for (;;)
        {
            var ch = lookahead(0);
            if (ch == delim)
            {
                break;
            }
            else if (hasRemaining())
            {
                if (!scanLineTerminator(ch))
                {
                    shiftCharCode();
                }
            }
            else
            {
                throw reportUnexpectedCharacter();
            }
        }

        var str = endSlice();
        shiftCharCode();
        endToken(Token.XML_ATTRIBUTE_VALUE);
        token.stringValue = str.replaceAll("\r\n", "\n");
    }

    private void xmlContentScan()
    {
        beginToken();
        var ch = lookahead(0);

        if (ch == 0x3c)
        {
            shiftCharCode();
            if (!scanXMLMarkup())
            {
                endToken(Token.LT);
            }
        }
        else if (ch == 0x7b)
        {
            endFixedToken(Token.LBRACE, 1);
        }
        else if (!hasRemaining())
        {
            throw reportUnexpectedCharacter();
        }
        else
        {
            beginSlice();
            if (!scanLineTerminator(ch))
            {
                shiftCharCode();
            }
            for (;;)
            {
                ch = lookahead(0);
                if (ch == 0x3c || !hasRemaining())
                {
                    break;
                }
                else if (!scanLineTerminator(ch))
                {
                    shiftCharCode();
                }
            }
            var text = endSlice();
            endToken(Token.XML_TEXT);
            token.stringValue = text.replaceAll("\r\n", "\n");
        }
    }

    public boolean scanXMLMarkup()
    {
        if (matchingArray(XML_CDATA_START))
        {
            scanXMLCDATA();
            return true;
        }
        if (matchingArray(XML_COMMENT_START))
        {
            scanXMLComment();
            return true;
        }
        if (lookahead(0) == 0x3f)
        {
            scanXMLPI();
            return true;
        }
        return false;
    }

    private void scanXMLCDATA()
    {
        index += 8;
        beginSlice();

        for (;;)
        {
            var ch = lookahead(0);
            if (ch == 0x5d && lookahead(1) == 0x5d && lookahead(2) == 0x3e)
            {
                index += 3;
                break;
            }
            else if (!scanLineTerminator(ch))
            {
                if (!hasRemaining())
                {
                    throw reportUnexpectedCharacter();
                }
                shiftCharCode();
            }
        }
        endToken(Token.XML_MARKUP);
        token.stringValue = "<![CDATA[" + endSlice().replaceAll("\r\n", "\n");
    }

    private void scanXMLComment()
    {
        index += 3;
        beginSlice();
        for (;;)
        {
            var ch = lookahead(0);
            if (ch == 0x2d && lookahead(1) == 0x2d && lookahead(2) == 0x3e)
            {
                index += 3;
                break;
            }
            else if (!scanLineTerminator(ch))
            {
                if (!hasRemaining())
                {
                    throw reportUnexpectedCharacter();
                }
                shiftCharCode();
            }
        }
        endToken(Token.XML_MARKUP);
        token.stringValue = "<!--" + endSlice().replaceAll("\r\n", "\n");
    }

    private void scanXMLPI()
    {
        shiftCharCode();
        beginSlice();
        for (;;)
        {
            var ch = lookahead(0);
            if (ch == 0x3f && lookahead(1) == 0x3e)
            {
                index += 2;
                break;
            }
            else if (!scanLineTerminator(ch))
            {
                if (!hasRemaining())
                {
                    throw reportUnexpectedCharacter();
                }
                shiftCharCode();
            }
        }
        endToken(Token.XML_MARKUP);
        token.stringValue = "<?" + endSlice().replaceAll("\r\n", "\n");
    }
}