package com.recoyx.shockscript.analysis.parsing;

import com.recoyx.shockscript.analysis.*;
import com.recoyx.shockscript.analysis.semantics.Operator;
import com.recoyx.shockscript.analysis.semantics.MetaData;
import com.recoyx.shockscript.analysis.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.stream.IntStream;

final class UnderlyingParser
{
    static final class AttributeData
    {
        public Vector<MetaData> metaData;
        public Ast.ExpressionNode qualifier;
        public boolean staticModifier;
        public boolean overrideModifier;
        public boolean finalModifier;
        public boolean nativeModifier;
        public boolean dynamicModifier;

        public boolean isEmpty()
        {
            return metaData == null && !hasModifiers();
        }

        public boolean hasModifiers()
        {
            return qualifier != null || staticModifier || overrideModifier || finalModifier || nativeModifier || dynamicModifier;
        }
    }

    static final class CurlySection
    {
        public String term;
        public Span span;

        public CurlySection(String term, Span span)
        {
            this.term = term;
            this.span = span;
        }
    }

    public Lexer lexer;
    public Script script;
    private TokenMetrics token;
    private TokenMetrics previousToken = new TokenMetrics();
    private IntVector locations = new IntVector();
    private Vector<CurlySection> curlyStack = new Vector<>();
    private AttributeData attributeData;
    private IntVector functionFlagsStack = new IntVector();

    /*
     * Result after parsing a directive or statement.
     */
    private boolean semicolonInserted = false;
    /*
     * Result of the methods <code>filterUnaryOperator()</code> and <code>filterBinaryOperator()</code>.
     */
    private OperatorPrecedence nextPrecedence = null;
    /*
     * Result of the methods <code>filterUnaryOperator()</code> and <code>filterBinaryOperator()</code>,
     * also assigned manually in short regions of the code.
     */
    private Operator filteredOperator = null;

    public UnderlyingParser(Lexer lexer)
    {
        this.lexer = lexer;
        script = lexer.script();
        token = lexer.token;
    }

    public void clearState()
    {
        lexer.mode = Lexer.Mode.NORMAL;
        curlyStack.setSize(0);
        locations.setLength(0);
        functionFlagsStack.setLength(0);
    }

    public void markLocation()
    {
        locations.push(token.start);
        locations.push(token.firstLine);
    }

    public void pushLocation(Span span)
    {
        locations.push(span.start());
        locations.push(span.firstLine());
    }

    public void duplicateLocation()
    {
        var l = locations.length();
        locations.push(locations.get(l - 2));
        locations.push(locations.get(l - 1));
    }

    public Span popLocation()
    {
        return new Span(locations.pop(), locations.pop(), previousToken.lastLine, previousToken.end);
    }

    public Parser.State state()
    {
        Parser.State state = new Parser.State();
        state.index = lexer.index;
        state.line = lexer.line;
        state.numLineStarts = script.lineStarts.length();
        state.lexerMode = lexer.mode;
        token.copyTo(state.token);
        previousToken.copyTo(state.previousToken);
        state.numComments = script.comments.size();
        state.numProblems = script.problems.size();
        state.numLocations = locations.length();
        state.numCurlySymbols = curlyStack.size();
        state.numFunctionFlags = functionFlagsStack.length();
        state.numSubscripts = script.subscripts == null ? -1 : script.subscripts.size();
        return state;
    }

    public void setState(Parser.State state)
    {
        lexer.index = state.index;
        lexer.line = state.line;
        script.lineStarts.setLength(state.numLineStarts);
        lexer.mode = state.lexerMode;
        state.token.copyTo(token);
        state.previousToken.copyTo(previousToken);
        script.problems.setSize(state.numProblems);
        script.comments.setSize(state.numComments);
        locations.setLength(state.numLocations);
        curlyStack.setSize(state.numCurlySymbols);
        functionFlagsStack.setLength(state.numFunctionFlags);
        if (state.numSubscripts == -1)
        {
            script.subscripts = null;
        }
        else
        {
            script.subscripts.setSize(state.numSubscripts);
        }
    }

    public int functionFlags()
    {
        return functionFlagsStack.length() == 0 ? -1 : functionFlagsStack.get(functionFlagsStack.length() - 1);
    }

    public void setFunctionFlags(int flags)
    {
        functionFlagsStack.set(functionFlagsStack.length() - 1, functionFlagsStack.get(functionFlagsStack.length() - 1) | flags);
    }

    public void openParen(String term)
    {
        curlyStack.add(new CurlySection(term, token.getSpan()));
        expect(Token.LPAREN);
    }

    public void closeParen()
    {
        var section = curlyStack.pop();
        if (token.type != Token.RPAREN)
        {
            throw reportSyntaxError(Problem.Constants.EXPECTING_TO_CLOSE, this.token.getSpan(), Problem.Argument.createToken(Token.RPAREN), Problem.Argument.createTerm(section.term), Problem.Argument.createNumber(section.span.firstLine()));
        }
        next();
    }

    public void openBracket(String term)
    {
        curlyStack.add(new CurlySection(term, token.getSpan()));
        expect(Token.LBRACKET);
    }

    public void closeBracket()
    {
        var section = curlyStack.pop();
        if (token.type != Token.RBRACKET)
        {
            throw reportSyntaxError(Problem.Constants.EXPECTING_TO_CLOSE, this.token.getSpan(), Problem.Argument.createToken(Token.RBRACKET), Problem.Argument.createTerm(section.term), Problem.Argument.createNumber(section.span.firstLine()));
        }
        next();
    }

    public void openBrace(String term)
    {
        curlyStack.add(new CurlySection(term, token.getSpan()));
        expect(Token.LBRACE);
    }

    public void closeBrace()
    {
        var section = curlyStack.pop();
        if (token.type != Token.RBRACE)
        {
            throw reportSyntaxError(Problem.Constants.EXPECTING_TO_CLOSE, this.token.getSpan(), Problem.Argument.createToken(Token.RBRACE), Problem.Argument.createTerm(section.term), Problem.Argument.createNumber(section.span.firstLine()));
        }
        next();
    }

    public void openLeftAngle(String term)
    {
        curlyStack.add(new CurlySection(term, token.getSpan()));
        expect(Token.LT);
    }

    public void closeRightAngle()
    {
        var section = curlyStack.pop();

        if (token.type == Token.RIGHT_SHIFT
        ||  token.type == Token.UNSIGNED_RIGHT_SHIFT
        ||  token.type == Token.GE)
        {
            this.token.type =  this.token.type == Token.RIGHT_SHIFT ? Token.GT
                            :  this.token.type == Token.GE          ? Token.EQUALS : Token.UNSIGNED_RIGHT_SHIFT;
            this.previousToken.start = this.token.start;
            this.previousToken.firstLine = this.token.firstLine;
            this.previousToken.end = this.token.end + 1;
            this.previousToken.lastLine = this.token.lastLine;
            ++this.token.start;
        }
        else if (token.type != Token.GT)
        {
            throw reportSyntaxError(Problem.Constants.EXPECTING_TO_CLOSE, this.token.getSpan(), Problem.Argument.createToken(Token.RBRACE), Problem.Argument.createTerm(section.term), Problem.Argument.createNumber(section.span.firstLine()));
        }
        else
        {
            next();
        }
    }

    public Problem reportSyntaxError(Problem.Constants errorId, Span span, Problem.Argument... rest)
    {
        return this.script.collectProblem(new Problem("syntaxError", errorId, span, this.script, rest));
    }

    public Problem reportVerifyError(Problem.Constants errorId, Span span, Problem.Argument... rest)
    {
        return this.script.collectProblem(new Problem("verifyError", errorId, span, this.script, rest));
    }

    public Problem reportWarning(Problem.Constants errorId, Span span, Problem.Argument... rest)
    {
        return this.script.collectProblem(new Problem("warning", errorId, span, this.script, rest));
    }

    public Problem warn(Problem.Constants errorId, Span span, Problem.Argument... rest)
    {
        return this.reportWarning(errorId, span, rest);
    }

    public void next()
    {
        this.previousToken.firstLine = this.token.firstLine;
        this.previousToken.start = this.token.start;
        this.previousToken.lastLine = this.token.lastLine;
        this.previousToken.end = this.token.end;
        this.lexer.shift();
    }

    public boolean consume(Token tokenType)
    {
        if (this.token.type == tokenType)
        {
            this.next();
            return true;
        }
        return false;
    }

    public String consumeIdentifier()
    {
        if (this.token.type == Token.IDENTIFIER)
        {
            var str = this.token.stringValue;
            this.next();
            return str;
        }
        return null;
    }

    public boolean consumeContextKeyword(String keyword)
    {
        if (this.token.type == Token.IDENTIFIER && this.token.stringValue.equals(keyword))
        {
            this.next();
            return true;
        }
        return false;
    }

    public Problem expect(Token tokenType)
    {
        if (this.token.type == tokenType)
        {
            this.next();
            return null;
        }
        else
        {
            var problem = this.reportSyntaxError(Problem.Constants.EXPECTING_BEFORE, this.token.getSpan(), Problem.Argument.createToken(tokenType), Problem.Argument.createToken(token.type));
            this.next();
            return problem;
        }
    }

    public String expectIdentifier()
    {
        var str = this.token.stringValue;
        var problem = this.expect(Token.IDENTIFIER);
        if (problem != null)
        {
            throw problem;
        }
        return str;
    }

    public Problem expectContextKeyword(String keyword)
    {
        if (this.token.type == Token.IDENTIFIER && this.token.stringValue.equals(keyword))
        {
            this.next();
            return null;
        }
        else
        {
            var problem = this.reportSyntaxError(Problem.Constants.EXPECTING_BEFORE, this.token.getSpan(), Problem.Argument.createQuote(keyword), Problem.Argument.createToken(token.type));
            this.next();
            return problem;
        }
    }

    public boolean isTokenAtNewLine()
    {
        return this.previousToken.lastLine != this.token.firstLine;
    }

    public void invalidateLineBreak()
    {
        if (this.isTokenAtNewLine())
        {
            this.reportSyntaxError(Problem.Constants.UNEXPECTED_BEFORE, this.token.getSpan(), Problem.Argument.createTerm("lineBreak"), Problem.Argument.createToken(token.type));
        }
    }

    public Ast.ExpressionNode parseTypeAnnotation()
    {
        return parseTypeAnnotation(false);
    }

    public Ast.ExpressionNode parseTypeAnnotation(boolean allowVoid)
    {
        markLocation();
        Ast.ExpressionNode node = null;
        // void
         if (token.type == Token.VOID)
        {
            if (!allowVoid)
            {
                reportSyntaxError(Problem.Constants.ILLEGAL_VOID_TYPE, this.token.getSpan());
            }
            next();
            node = new Ast.VoidTypeNode();
            node.span = popLocation();
            return node;
        }
        else
        {
        	return parseNonAssignmentExpression(false);
        }
    }

    public Ast.ExpressionNode parseNonAssignmentExpression()
    {
        return parseNonAssignmentExpression(true);
    }

    public Ast.ExpressionNode parseNonAssignmentExpression(boolean allowIn)
    {
        return parseExpression(allowIn, OperatorPrecedence.TERNARY_OPERATOR, false);
    }

    public Ast.ExpressionNode parseExpression()
    {
        return parseExpression(true);
    }

    public Ast.ExpressionNode parseExpression(boolean allowIn)
    {
        return parseExpression(allowIn, OperatorPrecedence.LIST_OPERATOR);
    }

    public Ast.ExpressionNode parseExpression(boolean allowIn, OperatorPrecedence minPrecedence)
    {
        return parseExpression(allowIn, minPrecedence, true);
    }

    public Ast.ExpressionNode parseExpression(boolean allowIn, OperatorPrecedence minPrecedence, boolean allowAssignment)
    {
        var expr = parseOptExpression(allowIn, minPrecedence, allowAssignment);
        if (expr == null)
        {
            throw this.reportSyntaxError(Problem.Constants.UNALLOWED_HERE, this.token.getSpan(), Problem.Argument.createToken(this.token.type));
        }
        return expr;
    }

    public Ast.ExpressionNode parseOptExpression()
    {
        return parseOptExpression(true);
    }

    public Ast.ExpressionNode parseOptExpression(boolean allowIn)
    {
        return parseOptExpression(allowIn, OperatorPrecedence.LIST_OPERATOR);
    }

    public Ast.ExpressionNode parseOptExpression(boolean allowIn, OperatorPrecedence minPrecedence)
    {
        return parseOptExpression(allowIn, minPrecedence, true);
    }

    public Ast.ExpressionNode parseOptExpression(boolean allowIn, OperatorPrecedence minPrecedence, boolean allowAssignment)
    {
        var expr = this.parseOptPrimaryExpression(allowIn, allowAssignment);
        if (expr != null)
        {
            return this.parseSubexpression(expr, allowIn, minPrecedence, allowAssignment);
        }
        if (this.token.type == Token.SUPER && minPrecedence.valueOf() <= OperatorPrecedence.UNARY_OPERATOR.valueOf())
        {
            this.markLocation();
            this.next();
            var argumentList = this.token.type == Token.LPAREN ? this.parseArguments() : null;
            return this.parseSuperOperator(argumentList, allowIn, minPrecedence);
        }
        if (minPrecedence.valueOf() <= OperatorPrecedence.UNARY_OPERATOR.valueOf())
        {
            Ast.ExpressionNode subexpr = null;
            if (this.filterUnaryOperator(this.token.type))
            {
                var operator = this.filteredOperator;
                this.markLocation();
                this.next();
                subexpr = this.parseExpression(allowIn, this.nextPrecedence);
                expr = new Ast.UnaryOperatorNode(operator, subexpr);
                expr.span = this.popLocation();
                if (((Ast.UnaryOperatorNode) expr).type == Operator.YIELD)
                {
                    if (this.functionFlags() == -1 || (this.functionFlags() & 2) != 0)
                        this.reportSyntaxError(Problem.Constants.UNALLOWED_HERE, expr.span, Problem.Argument.createToken(Token.YIELD));
                    else this.setFunctionFlags(1);
                }
                else if (((Ast.UnaryOperatorNode) expr).type == Operator.AWAIT)
                {
                    if (this.functionFlags() == -1 || (this.functionFlags() & 1) != 0)
                        this.reportSyntaxError(Problem.Constants.UNALLOWED_HERE, expr.span, Problem.Argument.createToken(Token.AWAIT));
                    else this.setFunctionFlags(2);
                }
                expr = this.parseSubexpression(expr, allowIn, minPrecedence);
            }
            else if (this.token.type == Token.QUESTION_MARK)
            {
            	markLocation();
            	next();
            	expr = new Ast.NullableTypeNode(parseExpression(false, OperatorPrecedence.UNARY_OPERATOR));
            	expr.span = popLocation();
            	return this.parseSubexpression(expr, allowIn, minPrecedence);
            }
            else if (this.token.type == Token.INCREMENT
                ||   this.token.type == Token.DECREMENT)
            {
                this.markLocation();
                var operator = this.filteredOperator = (this.token.type == Token.INCREMENT ? Operator.INCREMENT : Operator.DECREMENT);
                this.next();
                this.invalidateLineBreak();
                subexpr = this.parseExpression(allowIn, OperatorPrecedence.POSTFIX_OPERATOR);
                expr = new Ast.UnaryOperatorNode(operator, subexpr);
                expr.span = this.popLocation();
                expr = this.parseSubexpression(expr, allowIn, minPrecedence);
            }
        }
        return expr != null ? this.parseSubexpression(expr, allowIn, minPrecedence, allowAssignment) : null;
    }

    private boolean filterUnaryOperator(Token tokenType)
    {
        this.filteredOperator = null;

        if (tokenType.isKeyword())
        {
            this.filteredOperator = tokenType == Token.VOID ? Operator.VOID
                            :       tokenType == Token.TYPEOF ? Operator.TYPEOF : null;
            if (this.filteredOperator != null)
                this.nextPrecedence = OperatorPrecedence.UNARY_OPERATOR;
            else
            {
                this.filteredOperator = tokenType == Token.DELETE ? Operator.DELETE : null;
                if (this.filteredOperator != null)
                    this.nextPrecedence = OperatorPrecedence.POSTFIX_OPERATOR;
                else
                {
                    this.filteredOperator = tokenType == Token.YIELD ? Operator.YIELD :
                                            tokenType == Token.AWAIT ? Operator.AWAIT : null;
                    if (this.filteredOperator != null)
                        this.nextPrecedence = OperatorPrecedence.ASSIGNMENT_OPERATOR;
                }
            }
        }
        else if (tokenType.isPunctuator())
        {
            if (tokenType == Token.EXCLAMATION_MARK)
            {
                this.filteredOperator = Operator.LOGICAL_NOT;
            }
            else if (tokenType == Token.PLUS)
            {
                this.filteredOperator = Operator.POSITIVE;
            }
            else if (tokenType == Token.MINUS)
            {
                this.filteredOperator = Operator.NEGATE;
            }
            else if (tokenType == Token.BIT_NOT)
            {
                this.filteredOperator = Operator.BITWISE_NOT;
            }
            else
            {
                this.filteredOperator = null;
            }

            if (this.filteredOperator != null)
            {
                this.nextPrecedence = OperatorPrecedence.UNARY_OPERATOR;
            }
        }

        return this.filteredOperator != null;
    }

    private boolean filterBinaryOperator(Token tokenType)
    {
        this.filteredOperator = null;

        if (!tokenType.isPunctuator())
        {
            return false;
        }

        if (tokenType == Token.PLUS)
        {
            this.filteredOperator = Operator.ADD;
            this.nextPrecedence = OperatorPrecedence.ADDITIVE_OPERATOR;
        }
        if (tokenType == Token.MINUS)
        {
            this.filteredOperator = Operator.SUBTRACT;
            this.nextPrecedence = OperatorPrecedence.ADDITIVE_OPERATOR;
        }
        if (tokenType == Token.TIMES)
        {
            this.filteredOperator = Operator.MULTIPLY;
            this.nextPrecedence = OperatorPrecedence.MULTIPLICATIVE_OPERATOR;
        }
        if (tokenType == Token.SLASH)
        {
            this.filteredOperator = Operator.DIVIDE;
            this.nextPrecedence = OperatorPrecedence.MULTIPLICATIVE_OPERATOR;
        }
        if (tokenType == Token.REMAINDER)
        {
            this.filteredOperator = Operator.REMAINDER;
            this.nextPrecedence = OperatorPrecedence.MULTIPLICATIVE_OPERATOR;
        }
        if (tokenType == Token.LEFT_SHIFT)
        {
            this.filteredOperator = Operator.LEFT_SHIFT;
            this.nextPrecedence = OperatorPrecedence.SHIFT_OPERATOR;
        }
        if (tokenType == Token.RIGHT_SHIFT)
        {
            this.filteredOperator = Operator.RIGHT_SHIFT;
            this.nextPrecedence = OperatorPrecedence.SHIFT_OPERATOR;
        }
        if (tokenType == Token.UNSIGNED_RIGHT_SHIFT)
        {
            this.filteredOperator = Operator.UNSIGNED_RIGHT_SHIFT;
            this.nextPrecedence = OperatorPrecedence.SHIFT_OPERATOR;
        }
        if (tokenType == Token.BIT_AND)
        {
            this.filteredOperator = Operator.BITWISE_AND;
            this.nextPrecedence = OperatorPrecedence.BIT_AND_OPERATOR;
        }
        if (tokenType == Token.BIT_XOR)
        {
            this.filteredOperator = Operator.BITWISE_XOR;
            this.nextPrecedence = OperatorPrecedence.BIT_XOR_OPERATOR;
        }
        if (tokenType == Token.BIT_OR)
        {
            this.filteredOperator = Operator.BITWISE_OR;
            this.nextPrecedence = OperatorPrecedence.BIT_OR_OPERATOR;
        }
        if (tokenType == Token.LOGICAL_AND)
        {
            this.filteredOperator = Operator.LOGICAL_AND;
            this.nextPrecedence = OperatorPrecedence.LOGICAL_AND_OPERATOR;
        }
        if (tokenType == Token.LOGICAL_OR)
        {
            this.filteredOperator = Operator.LOGICAL_OR;
            this.nextPrecedence = OperatorPrecedence.LOGICAL_OR_OPERATOR;
        }
        if (tokenType == Token.EQUALS)
        {
            this.filteredOperator = Operator.EQUALS;
            this.nextPrecedence = OperatorPrecedence.EQUALITY_OPERATOR;
        }
        if (tokenType == Token.NOT_EQUALS)
        {
            this.filteredOperator = Operator.NOT_EQUALS;
            this.nextPrecedence = OperatorPrecedence.EQUALITY_OPERATOR;
        }
        if (tokenType == Token.STRICT_EQUALS)
        {
            this.filteredOperator = Operator.STRICT_EQUALS;
            this.nextPrecedence = OperatorPrecedence.EQUALITY_OPERATOR;
        }
        if (tokenType == Token.STRICT_NOT_EQUALS)
        {
            this.filteredOperator = Operator.STRICT_NOT_EQUALS;
            this.nextPrecedence = OperatorPrecedence.EQUALITY_OPERATOR;
        }
        if (tokenType == Token.LT)
        {
            this.filteredOperator = Operator.LT;
            this.nextPrecedence = OperatorPrecedence.RELATIONAL_OPERATOR;
        }
        if (tokenType == Token.GT)
        {
            this.filteredOperator = Operator.GT;
            this.nextPrecedence = OperatorPrecedence.RELATIONAL_OPERATOR;
        }
        if (tokenType == Token.LE)
        {
            this.filteredOperator = Operator.LE;
            this.nextPrecedence = OperatorPrecedence.RELATIONAL_OPERATOR;
        }
        if (tokenType == Token.GE)
        {
            this.filteredOperator = Operator.GE;
            this.nextPrecedence = OperatorPrecedence.RELATIONAL_OPERATOR;
        }

        return this.filteredOperator != null;
    }

    public Ast.ExpressionNode parseSuperOperator(Vector<Ast.ExpressionNode> arguments, boolean allowIn, OperatorPrecedence minPrecedence)
    {
        var problem = this.expect(Token.DOT);
        if (problem != null)
        {
            throw problem;
        }
        var id = this.parseQualifiedIdentifier(true);
        var node = new Ast.SuperDotNode(arguments, (Ast.SimpleIdNode) id);
        node.span = this.popLocation();
        return this.parseSubexpression(node, allowIn, minPrecedence);
    }

    public Ast.ExpressionNode parseSubexpression(Ast.ExpressionNode base, boolean allowIn, OperatorPrecedence minPrecedence)
    {
        return parseSubexpression(base, allowIn, minPrecedence, true);
    }

    public Ast.ExpressionNode parseSubexpression(Ast.ExpressionNode base, boolean allowIn, OperatorPrecedence minPrecedence, boolean allowAssignment)
    {
        Ast.ExpressionNode node = base;
        Ast.ExpressionNode node2 = null;
        Ast.QualifiedIdNode id = null;

        for (;;)
        {
            if (this.consume(Token.DOT))
            {
                if ((node2 = this.parseOptQualifiedIdentifier()) != null)
                {
                    node = new Ast.DotNode(node, (Ast.QualifiedIdNode) node2);
                    this.pushLocation(base.span);
                    node.span = this.popLocation();
                }
                else if (this.token.type == Token.LPAREN)
                {
                    node = this.parseFilterOperator(node);
                }
                else if (this.token.type == Token.LT)
                {
                    this.openLeftAngle("argumentList");
                    var dot_arguments = new Vector<Ast.ExpressionNode>();
                    do
                    {
                        dot_arguments.add(this.parseTypeAnnotation());
                    }
                    while (this.consume(Token.COMMA));
                    this.closeRightAngle();
                    node = new Ast.TypeArgumentsNode(node, dot_arguments);
                    this.pushLocation(base.span);
                    node.span = this.popLocation();
                }
                else
                {
                    this.expectIdentifier();
                }
            }
            // bracket operator is indentation-aware
            else if (this.token.type == Token.LBRACKET && (!this.isTokenAtNewLine() || lexer.getLineIndentation(base.span.firstLine()) < lexer.getLineIndentation(this.token.firstLine)))
            {
                this.openBracket("expression");
                node2 = this.parseExpression();
                this.closeBracket();
                node = new Ast.BracketsNode(node, node2);
                this.pushLocation(base.span);
                node.span = this.popLocation();
            }
            else if (this.consume(Token.DESCENDANTS))
            {
                id = (Ast.QualifiedIdNode) this.parseQualifiedIdentifier();
                if (id instanceof Ast.AttributeIDNode)
                {
                    reportSyntaxError(Problem.Constants.ATTRIBUTE_ID_NOT_ALLOWED_HERE, id.span);
                }
                node = new Ast.DescendantsNode(node, id);
                this.pushLocation(base.span);
                node.span = this.popLocation();
            }
            // call operator is indentation-aware
            else if (this.token.type == Token.LPAREN && (!this.isTokenAtNewLine() || lexer.getLineIndentation(base.span.firstLine()) < lexer.getLineIndentation(this.token.firstLine)))
            {
                node = new Ast.CallNode(node, this.parseArguments());
                this.pushLocation(base.span);
                node.span = this.popLocation();
            }
            else if ((this.token.type == Token.AS
                ||    this.token.type == Token.IS
                ||    this.token.type == Token.INSTANCEOF)
                &&    minPrecedence.valueOf() <= OperatorPrecedence.RELATIONAL_OPERATOR.valueOf())
            {
                var type_op = this.token.type == Token.AS ? "as" : this.token.type == Token.IS ? "is" : "instanceof";
                this.next();
                Parser.State typecheck_state = null;
                Ast.PatternNode typecheck_pattern = null;
                String op_binding = null;

                if (type_op != "as")
                {
                    typecheck_state = state();
                    typecheck_pattern = parseOptPattern(false);
                    if (typecheck_pattern == null || typecheck_pattern.type == null)
                    {
                        setState(typecheck_state);
                        typecheck_pattern = null;
                    }
                }

                node = new Ast.TypeOperatorNode(type_op, node, typecheck_pattern == null ? this.parseTypeAnnotation(false) : null);
                ((Ast.TypeOperatorNode) node).pattern = typecheck_pattern;
                this.pushLocation(base.span);
                node.span = this.popLocation();
            }
            else if (this.filterBinaryOperator(this.token.type) && minPrecedence.valueOf() <= this.nextPrecedence.valueOf())
            {
                var operator = this.filteredOperator;
                this.next();
                node = new Ast.BinaryOperatorNode(operator, node, this.parseExpression(allowIn, OperatorPrecedence.fromValue(this.nextPrecedence.valueOf() + 1)));
                this.pushLocation(base.span);
                node.span = this.popLocation();
            }
            else if (minPrecedence.valueOf() <= OperatorPrecedence.RELATIONAL_OPERATOR.valueOf()
                && allowIn && this.consume(Token.IN))
            {
                node = new Ast.BinaryOperatorNode(Operator.IN, node, this.parseExpression(allowIn, OperatorPrecedence.SHIFT_OPERATOR));
                this.pushLocation(base.span);
                node.span = this.popLocation();
            }
            else if (minPrecedence.valueOf() <= OperatorPrecedence.TERNARY_OPERATOR.valueOf()
                && this.consume(Token.QUESTION_MARK))
            {
                node2 = this.parseExpression(allowIn, OperatorPrecedence.ASSIGNMENT_OPERATOR);
                Ast.ExpressionNode node3 = null;
                if (this.consume(Token.COLON))
                {
                    node3 = this.parseExpression(allowIn, OperatorPrecedence.TERNARY_OPERATOR);
                }
                else
                {
                    throw this.expect(Token.COLON);
                }
                node = new Ast.TernaryNode(node, node2, node3);
                this.pushLocation(base.span);
                node.span = this.popLocation();
            }
            else if (token.type.isAssign() && minPrecedence.valueOf() <= OperatorPrecedence.ASSIGNMENT_OPERATOR.valueOf() && allowAssignment)
            {
                var compound_operator = token.type.getCompoundAssignmentOperator();
                this.next();
                node = new Ast.AssignmentNode(compound_operator, node, this.parseExpression(allowIn, OperatorPrecedence.TERNARY_OPERATOR));
                this.pushLocation(base.span);
                node.span = this.popLocation();
            }
            else if (this.token.type == Token.COMMA && minPrecedence.valueOf() <= OperatorPrecedence.LIST_OPERATOR.valueOf())
            {
                var expressions = new Vector<Ast.ExpressionNode>();
                expressions.add(node);
                while (this.consume(Token.COMMA))
                {
                    expressions.add(this.parseExpression(allowIn, OperatorPrecedence.TERNARY_OPERATOR));
                }
                node = new Ast.ListExpressionNode(expressions);
                this.pushLocation(base.span);
                node.span = this.popLocation();
            }
            else if (this.token.type == Token.INCREMENT
                ||   this.token.type == Token.DECREMENT)
            {
                var update_op = this.token.type == Token.INCREMENT ? Operator.POST_INCREMENT : Operator.POST_DECREMENT;
                this.invalidateLineBreak();
                this.next();
                node = new Ast.UnaryOperatorNode(update_op, node);
                this.pushLocation(base.span);
                node.span = this.popLocation();
            }
            else
            {
                break;
            }
        }

        return node;
    }

    private Ast.ExpressionNode parseFilterOperator(Ast.ExpressionNode base)
    {
        this.functionFlagsStack.push(this.functionFlags() == -1 ? -1 : 0);
        var filter = this.parseParenListExpression();
        var flags = this.functionFlagsStack.pop();
        this.pushLocation(base.span);
        if (this.token.type == Token.COLON_COLON)
        {
            var id = this.parseQualifiedIdentifierFinal(filter);
            base = new Ast.DotNode(base, (Ast.QualifiedIdNode) id);
            if (flags != -1)
            {
                this.setFunctionFlags(flags);
            }
        }
        else
        {
            base = new Ast.FilterNode(base, filter);
            if (flags != -1 && ((flags & 1) != 0) || ((flags & 2) != 0))
            {
                this.duplicateLocation();
                this.reportSyntaxError(((flags & 1) != 0) ? Problem.Constants.FILTER_MUST_NOT_CONTAIN_YIELD : Problem.Constants.FILTER_MUST_NOT_CONTAIN_AWAIT, this.popLocation());
            }
        }
        base.span = this.popLocation();
        return base;
    }

    private Ast.ExpressionNode parseOptPrimaryExpression()
    {
        return parseOptPrimaryExpression(true);
    }

    private Ast.ExpressionNode parseOptPrimaryExpression(boolean allowIn)
    {
        return parseOptPrimaryExpression(allowIn, true);
    }

    private Ast.ExpressionNode parseOptPrimaryExpression(boolean allowIn, boolean allowAssignment)
    {
        this.markLocation();
        {
            Ast.ExpressionNode node = null;
            var str = this.consumeIdentifier();
            if (str != null)
            {
                return this.parseIdentifierStartedExpression(str);
            }
            if (this.token.type == Token.STRING_LITERAL)
            {
                node = new Ast.StringLiteralNode(this.token.stringValue);
                this.next();
                node.span = this.popLocation();
                return node;
            }
            if (this.token.type == Token.NUMERIC_LITERAL)
            {
                node = new Ast.NumericLiteralNode(this.token.numberValue);
                this.next();
                node.span = this.popLocation();
                return node;
            }
            if (this.token.type == Token.BOOLEAN_LITERAL)
            {
                node = new Ast.BooleanLiteralNode(this.token.booleanValue);
                this.next();
                node.span = this.popLocation();
                return node;
            }
            if (this.consume(Token.NULL_LITERAL))
            {
                node = new Ast.NullLiteralNode();
                node.span = this.popLocation();
                return node;
            }
            if (this.consume(Token.THIS_LITERAL))
            {
                node = new Ast.ThisLiteralNode();
                node.span = this.popLocation();
                return node;
            }
            if (this.token.type == Token.SLASH || this.token.type == Token.DIVIDE_ASSIGN)
            {
                this.lexer.scanRegExpLiteral();
                node = new Ast.RegExpLiteralNode(this.token.stringValue, this.token.regExpFlags);
                this.next();
                node.span = this.popLocation();
                return node;
            }
            if (this.token.type == Token.LPAREN)
            {
                this.popLocation();
                node = this.parseParenListExpression();
                if (this.token.type == Token.COLON_COLON)
                {
                    node = this.parseQualifiedIdentifierFinal(node);
                }
                return node;
            }
            if ((node = this.parseOptReservedNamespace()) != null)
            {
                this.popLocation();
                return this.token.type == Token.COLON_COLON ? this.parseQualifiedIdentifierFinal(node) : node;
            }
            if (this.token.type == Token.LBRACKET)
            {
                this.popLocation();
                return this.parseArrayLiteral(allowIn, allowAssignment);
            }
            if (this.token.type == Token.LBRACE)
            {
                this.popLocation();
                return this.parseObjectLiteral(allowIn, allowAssignment);
            }
            if (this.consume(Token.NEW))
            {
                var new_base = this.parseTypeAnnotation(false);
                node = new Ast.NewOperatorNode(new_base, this.token.type == Token.LPAREN ? this.parseArguments() : null);
                node.span = this.popLocation();
                return node;
            }
            if (this.consume(Token.FUNCTION))
            {
                str = this.consumeIdentifier();
                var fn_common = this.parseFunctionCommon();
                node = new Ast.FunctionExpressionNode(str, fn_common);
                node.span = this.popLocation();

                if (fn_common.body == null)
                {
                    reportSyntaxError(Problem.Constants.FUNCTION_OMITS_BODY, node.span);
                }

                return node;
            }
            if (this.token.type == Token.LT)
            {
                if (lexer.lookahead(0) == 0x21 || lexer.lookahead(0) == 0x3F)
                {
                    this.lexer.scanXMLMarkup();
                    node = new Ast.XMLMarkupNode(this.token.stringValue);
                    this.next();
                    node.span = this.popLocation();
                    return node;
                }
                else
                {
                    this.lexer.mode = Lexer.Mode.XML_TAG;
                    this.next();
                    if (this.token.type == Token.GT)
                    {
                        return this.parseXMLListLiteral(this.popLocation());
                    }
                    else
                    {
                        return this.parseXMLElementLiteral(true, this.popLocation());
                    }
                }
            }
            if ((node = this.parseOptQualifiedIdentifier()) != null)
            {
                this.popLocation();
                return node;
            }
            if (this.token.type == Token.COLON)
            {
                this.reportSyntaxError(Problem.Constants.UNALLOWED_HERE, token.getSpan(), Problem.Argument.createToken(this.token.type));
                this.next();
                this.parseTypeAnnotation();
            }
        }
        popLocation();
        return null;
    }

    public Ast.ExpressionNode parseParenListExpression()
    {
        this.markLocation();
        this.openParen("parens");
        var expr = this.parseExpression();
        this.closeParen();
        var node = new Ast.ParenExpressionNode(expr);
        node.span = this.popLocation();
        return node;
    }

    public Vector<Ast.ExpressionNode> parseArguments()
    {
        var arguments = new Vector<Ast.ExpressionNode>();
        this.openParen("argumentList");
        if (this.token.type != Token.RPAREN)
        {
            do
            {
                var expr = this.parseExpression(true, OperatorPrecedence.ASSIGNMENT_OPERATOR);
                arguments.add(expr);
            }
            while (this.consume(Token.COMMA));
        }
        this.closeParen();
        return arguments;
    }

    /**
     * Parses expression that has started with a previous identifier this.token.
     * The identifier location instanceof assumed to have been marked.
     */
    public Ast.ExpressionNode parseIdentifierStartedExpression(String str)
    {
        if (this.token.type == Token.STRING_LITERAL && str.equals("embed"))
        {
            var src = this.token.stringValue;
            this.next();
            var node = new Ast.EmbedExpressionNode(src);
            node.span = this.popLocation();
            return node;
        }
        var id = new Ast.SimpleIdNode(null, str);
        id.span = this.popLocation();
        return this.parseQualifiedIdentifierFinal(id);
    }

    public Ast.ExpressionNode parseQualifiedIdentifier()
    {
        return parseQualifiedIdentifier(false);
    }

    public Ast.ExpressionNode parseQualifiedIdentifier(boolean onlySimple)
    {
        var node = this.parseOptQualifiedIdentifier(onlySimple);
        if (node == null)
        {
            throw this.expect(Token.IDENTIFIER);
        }
        return node;
    }

    public Ast.ExpressionNode parseOptQualifiedIdentifier()
    {
        return parseOptQualifiedIdentifier(false);
    }

    public Ast.ExpressionNode parseOptQualifiedIdentifier(boolean onlySimple)
    {
        this.markLocation();
        var str = this.consumeIdentifier();
        Ast.ExpressionNode node = null;
        if (str != null)
        {
            node = new Ast.SimpleIdNode(null, str);
            node.span = this.popLocation();
        }
        else if (this.token.type == Token.LPAREN)
        {
            this.popLocation();
            node = this.parseParenListExpression();
            if (node != null && this.token.type != Token.COLON_COLON)
            {
                throw this.expect(Token.COLON_COLON);
            }
        }
        else if (this.token.type == Token.TIMES)
        {
            this.next();
            node = new Ast.SimpleIdNode(null, "*");
            node.span = this.popLocation();
        }
        else if (this.token.type == Token.ATTRIBUTE && !onlySimple)
        {
            this.next();
            if (this.token.type == Token.ATTRIBUTE)
            {
                throw this.reportSyntaxError(Problem.Constants.UNALLOWED_HERE, this.token.getSpan(), Problem.Argument.createToken(this.token.type));
            }
            Ast.ExpressionNode brackets = null;
            if (this.token.type == Token.LBRACKET)
            {
                this.openBracket("expression");
                brackets = this.parseExpression();
                this.closeBracket();
            }
            if (brackets != null)
            {
                var brackets_id = new Ast.ExpressionIdNode(null, brackets);
                brackets_id.span = brackets.span;
                node = new Ast.AttributeIDNode(brackets_id);
            }
            else
            {
                node = new Ast.AttributeIDNode((Ast.QualifiedIdNode) this.parseQualifiedIdentifier());
            }
            node.span = this.popLocation();
        }
        else
        {
            this.popLocation();
            node = this.parseOptReservedNamespace();
            if (node != null && this.token.type != Token.COLON_COLON)
            {
                throw this.expect(Token.COLON_COLON);
            }
        }

        if (node != null)
        {
            return this.parseQualifiedIdentifierFinal(node, onlySimple);
        }
        else
        {
            return null;
        }
    }

    public Ast.ExpressionNode parseQualifiedIdentifierFinal(Ast.ExpressionNode expr)
    {
        return parseQualifiedIdentifierFinal(expr, false);
    }

    public Ast.ExpressionNode parseQualifiedIdentifierFinal(Ast.ExpressionNode expr, boolean onlySimple)
    {
        if (this.consume(Token.COLON_COLON))
        {
            this.pushLocation(expr.span);
            if (this.token.type == Token.LBRACKET && !onlySimple)
            {
                this.openBracket("expression");
                expr = new Ast.ExpressionIdNode(expr, this.parseExpression());
                this.closeBracket();
            }
            else if (this.consume(Token.TIMES))
            {
                expr = new Ast.SimpleIdNode(expr, "*");
            }
            else
            {
                expr = new Ast.SimpleIdNode(expr, this.expectIdentifier());
            }
            expr.span = this.popLocation();
        }
        return expr;
    }

    private Ast.ExpressionNode parseArrayLiteral()
    {
        return parseArrayLiteral(true);
    }

    private Ast.ExpressionNode parseArrayLiteral(boolean allowIn)
    {
        return parseArrayLiteral(allowIn, true);
    }

    private Ast.ExpressionNode parseArrayLiteral(boolean allowIn, boolean allowAssignment)
    {
        var elements = new Vector<Ast.ExpressionNode>();

        var bracket_state = this.state();

        this.markLocation();
        this.openBracket("expression");
        {
            do
            {
                while (this.consume(Token.COMMA))
                {
                    elements.add(null);
                }
                if (this.token.type == Token.RBRACKET)
                {
                    break;
                }
                Ast.ExpressionNode expr = null;
                var spreadSpan = this.token.getSpan();

                if (this.consume(Token.ELLIPSIS))
                {
                    expr = this.parseExpression(true, OperatorPrecedence.ASSIGNMENT_OPERATOR);
                    expr = new Ast.SpreadOperatorNode(expr);
                    expr.span = spreadSpan;
                }
                else expr = this.parseOptExpression(true, OperatorPrecedence.ASSIGNMENT_OPERATOR);

                if (expr == null)
                    break;
                elements.add(expr);
            }
            while (this.consume(Token.COMMA));
        }
        this.closeBracket();

        if (this.token.type == Token.ASSIGN && allowAssignment)
        {
            this.setState(bracket_state);
            this.markLocation();
            var left = this.parsePattern();
            this.next();
            var right = this.parseExpression(allowIn, OperatorPrecedence.ASSIGNMENT_OPERATOR);
            var assign = new Ast.PatternAssignmentNode(left, right);
            assign.span = this.popLocation();
            return assign;
        }

        var node = new Ast.ArrayLiteralNode(elements);
        node.span = this.popLocation();
        return node;
    }

    private Ast.ExpressionNode parseObjectLiteral(boolean allowIn, boolean allowAssignment)
    {
        var fields = new Vector<Ast.ObjectFieldNode>();
        Ast.ExpressionNode rest = null;

        var brace_state = this.state();

        this.markLocation();
        this.openBrace("expression");
        {
            do
            {
                if (this.token.type == Token.RBRACE)
                {
                    break;
                }
                if (this.consume(Token.ELLIPSIS))
                {
                    rest = this.parseExpression(true, OperatorPrecedence.ASSIGNMENT_OPERATOR);
                    break;
                }
                this.markLocation();
                Ast.ExpressionNode key = null;
                if (this.token.type == Token.NUMERIC_LITERAL
                ||  this.token.type == Token.STRING_LITERAL)
                {
                    key = this.parseOptPrimaryExpression();
                }
                else if (this.token.type != Token.IDENTIFIER)
                {
                    break;
                }
                if (key == null && this.token.type == Token.IDENTIFIER)
                {
                    key = new Ast.SimpleIdNode(null, this.token.stringValue);
                    key.span = token.getSpan();
                    this.next();
                }
                var problem = this.expect(Token.COLON);
                if (problem != null)
                {
                    continue;
                }
                var field = new Ast.ObjectFieldNode(key, this.parseExpression(true, OperatorPrecedence.ASSIGNMENT_OPERATOR));
                field.span = this.popLocation();
                fields.add(field);
            }
            while (this.consume(Token.COMMA));
        }
        this.closeBrace();

        if (this.token.type == Token.ASSIGN && allowAssignment)
        {
            this.setState(brace_state);
            this.markLocation();
            var left = this.parsePattern();
            this.next();
            var right = this.parseExpression(allowIn, OperatorPrecedence.ASSIGNMENT_OPERATOR);
            var assign = new Ast.PatternAssignmentNode(left, right);
            assign.span = this.popLocation();
            return assign;
        }

        var node = new Ast.ObjectLiteralNode(fields, rest);
        node.span = this.popLocation();
        return node;
    }

    private Ast.ExpressionNode parseXMLListLiteral(Span start)
    {
        this.pushLocation(start);
        this.lexer.mode = Lexer.Mode.XML_CONTENT;
        this.next();
        var nodes = this.parseXMLContent();
        this.lexer.mode = Lexer.Mode.NORMAL;
        this.expect(Token.GT);
        var node = new Ast.XMLListNode(nodes);
        node.span = this.popLocation();
        return node;
    }

    private Ast.XMLNode parseXMLElementLiteral(boolean asRoot, Span start)
    {
        this.pushLocation(start);
        Object open_name = null;
        Object close_name = null;
        Vector<Ast.XMLAttributeNode> attributes = null;
        Vector<Ast.XMLNode> children = null;

        if (this.token.type == Token.LBRACE)
        {
            this.lexer.mode = Lexer.Mode.NORMAL;
            this.openBrace("expression");
            open_name = this.parseExpression();
            this.lexer.mode = Lexer.Mode.XML_TAG;
            this.closeBrace();
        }
        else
        {
            open_name = this.token.stringValue;
            this.expect(Token.XML_NAME);
        }

        while (this.consume(Token.XML_WHITESPACE))
        {
            Ast.XMLAttributeNode attrib = null;
            if (this.token.type == Token.LBRACE)
            {
                this.markLocation();
                this.lexer.mode = Lexer.Mode.NORMAL;
                this.openBrace("expression");
                attrib = new Ast.XMLAttributeNode(null, this.parseExpression());
                attrib.span = this.popLocation();
                attributes = attributes == null ? new Vector<>() : attributes;
                attributes.add(attrib);
                this.lexer.mode = Lexer.Mode.XML_TAG;
                this.closeBrace();
            }
            else if (this.token.type == Token.XML_NAME)
            {
                this.markLocation();
                var attrib_name = this.token.stringValue;
                this.next();
                this.consume(Token.XML_WHITESPACE);

                Object attrib_value = null;

                if (this.consume(Token.ASSIGN))
                {
                    this.consume(Token.XML_WHITESPACE);
                    if (this.token.type == Token.LBRACE)
                    {
                        this.lexer.mode = Lexer.Mode.NORMAL;
                        this.openBrace("expression");
                        attrib_value = this.parseExpression();
                        this.lexer.mode = Lexer.Mode.XML_TAG;
                        this.closeBrace();
                    }
                    else
                    {
                        attrib_value = this.token.stringValue;
                        this.expect(Token.XML_ATTRIBUTE_VALUE);
                    }
                }
                else
                {
                    var problem = this.expect(Token.ASSIGN);
                    if (problem != null)
                    {
                        throw problem;
                    }
                }

                attrib = new Ast.XMLAttributeNode(attrib_name, attrib_value);
                attrib.span = this.popLocation();
                attributes = attributes == null ? new Vector<>() : attributes;
                attributes.add(attrib);
            }
            else
            {
                break;
            }
        }

        if (this.token.type == Token.XML_SLASH_GT)
        {
            this.lexer.mode = asRoot ? Lexer.Mode.NORMAL : Lexer.Mode.XML_CONTENT;
            this.next();
        }
        else
        {
            this.lexer.mode = Lexer.Mode.XML_CONTENT;
            children = this.parseXMLContent();
            if (this.token.type == Token.LBRACE)
            {
                this.lexer.mode = Lexer.Mode.NORMAL;
                this.openBrace("expression");
                close_name = this.parseExpression();
                this.lexer.mode = Lexer.Mode.XML_TAG;
                this.closeBrace();
            }
            else
            {
                close_name = this.token.stringValue;
                this.expect(Token.XML_NAME);
            }
            this.lexer.mode = asRoot ? Lexer.Mode.NORMAL : Lexer.Mode.XML_CONTENT;
            this.expect(Token.GT);
        }

        var node = new Ast.XMLElementNode(open_name, close_name, attributes, children);
        node.span = this.popLocation();
        return node;
    }

    private Vector<Ast.XMLNode> parseXMLContent()
    {
        var nodes = new Vector<Ast.XMLNode>();
        Ast.XMLNode single_node = null;
        for (;;)
        {
            if (this.token.type == Token.LT)
            {
                this.markLocation();
                this.lexer.mode = Lexer.Mode.XML_TAG;
                this.next();
                nodes.add(this.parseXMLElementLiteral(false, this.popLocation()));
            }
            else if (this.token.type == Token.XML_MARKUP)
            {
                this.markLocation();
                single_node = new Ast.XMLMarkupNode(this.token.stringValue);
                single_node.span = this.popLocation();
                this.next();
                nodes.add(single_node);
            }
            else if (this.token.type == Token.XML_TEXT)
            {
                this.markLocation();
                single_node = new Ast.XMLTextNode(this.token.stringValue);
                single_node.span = this.popLocation();
                this.next();
                nodes.add(single_node);
            }
            else if (this.token.type == Token.LBRACE)
            {
                this.lexer.mode = Lexer.Mode.NORMAL;
                this.openBrace("expression");
                single_node = new Ast.XMLTextNode(this.parseExpression());
                single_node.span = this.popLocation();
                nodes.add(single_node);
                this.lexer.mode = Lexer.Mode.XML_CONTENT;
                this.closeBrace();
            }
            else
            {
                this.lexer.mode = Lexer.Mode.XML_TAG;
                this.expect(Token.LT);
                break;
            }
        }
        return nodes;
    }

    private Ast.ExpressionNode parseOptReservedNamespace()
    {
        if (this.token.type == Token.PUBLIC
        ||  this.token.type == Token.INTERNAL
        ||  this.token.type == Token.PRIVATE
        ||  this.token.type == Token.PROTECTED)
        {
            this.markLocation();
            var k = this.token.type;
            this.next();
            var node = new Ast.ReservedNamespaceNode
                ( k == Token.PUBLIC ? "public"
                : k == Token.PRIVATE ? "private"
                : k == Token.PROTECTED ? "protected" : "internal" );
            node.span = this.popLocation();
            return node;
        }
        return null;
    }

    private Ast.PatternNode parsePattern()
    {
        return parsePattern(true);
    }

    private Ast.PatternNode parsePattern(boolean allowTypeOperator)
    {
        var p = this.parseOptPattern(allowTypeOperator);
        if (p == null)
        {
            throw this.reportSyntaxError(Problem.Constants.UNALLOWED_HERE, this.token.getSpan(), Problem.Argument.createToken(this.token.type));
        }
        return p;
    }

    private Ast.PatternNode parseOptPattern()
    {
        return parseOptPattern(true);
    }

    private Ast.PatternNode parseOptPattern(boolean allowTypeOperator)
    {
        this.markLocation();
        var str = this.consumeIdentifier();
        Ast.PatternNode pattern = null;
        Ast.PatternNode subpattern = null;

        if (str != null)
        {
            pattern = new Ast.NamePatternNode(str, this.consume(Token.COLON) ? this.parseTypeAnnotation(false) : null);
        }
        else if (this.token.type == Token.LBRACE)
        {
            var fields = new Vector<Ast.ObjectPatternFieldNode>();
            this.openBrace("expression");
            do
            {
                var id = this.parseOptQualifiedIdentifier(true);
                id = id instanceof Ast.SimpleIdNode ? id : null;
                if (id != null)
                {
                    subpattern = this.consume(Token.COLON) ? this.parsePattern(allowTypeOperator) : null;
                    var field = new Ast.ObjectPatternFieldNode((Ast.SimpleIdNode) id, subpattern);
                    this.pushLocation(id.span);
                    field.span = this.popLocation();
                    fields.add(field);
                }
                else if (this.token.type == Token.RBRACE)
                {
                    break;
                }
                else
                {
                    this.expect(Token.IDENTIFIER);
                    break;
                }
            }
            while (this.consume(Token.COMMA));
            this.closeBrace();
            pattern = new Ast.ObjectPatternNode(fields, this.consume(Token.COLON) ? this.parseTypeAnnotation(false) : null);
        }
        else if (this.token.type == Token.LBRACKET)
        {
            var patterns = new Vector<Ast.PatternNode>();
            this.openBracket("expression");
            do
            {
                while (this.consume(Token.COMMA))
                {
                    patterns.add(null);
                }

                if (this.token.type == Token.RBRACE)
                {
                    break;
                }
                subpattern = this.parseOptPattern(allowTypeOperator);
                if (subpattern == null)
                {
                    break;
                }
                patterns.add(subpattern);
            }
            while (this.consume(Token.COMMA));
            this.closeBracket();
            pattern = new Ast.ArrayPatternNode(patterns, this.consume(Token.COLON) ? this.parseTypeAnnotation(false) : null);
        }

        if (pattern != null)
        {
            pattern.span = this.popLocation();
        }
        else
        {
            this.popLocation();
        }
        return pattern;
    }

    /*
     * Parses statement.
     *
     * <b>Note:</b> this method updates the <code>this.semicolonInserted</code> property.
     */
    public Ast.StatementNode parseStatement(Parser.Context context)
    {
        var stmt = this.parseOptStatement(context);
        if (stmt == null)
        {
            throw this.reportSyntaxError(Problem.Constants.UNALLOWED_HERE, this.token.getSpan(), Problem.Argument.createToken(this.token.type));
        }
        return stmt;
    }

    private Ast.StatementNode parseOptStatement(Parser.Context context)
    {
        this.semicolonInserted = false;

        Span marker = null;
        Ast.StatementNode stmt = null;
        Ast.StatementNode substmt = null;
        String str = "";
        Ast.ExpressionNode expr = null;

        if (this.token.type == Token.IDENTIFIER)
        {
            marker = this.token.getSpan();
            return this.parseIdentifierStartedStatement(this.consumeIdentifier(), context, marker);
        }
        else if (this.token.type.isKeyword())
        {
            // super()
            // super().x
            // super.x
            if (this.token.type == Token.SUPER)
            {
                this.markLocation();
                this.next();
                var super_arguments = this.token.type == Token.LPAREN ? this.parseArguments() : null;
                if (super_arguments != null && this.token.type != Token.DOT)
                {
                    this.parseSemicolon();
                    stmt = new Ast.SuperStatementNode(super_arguments);
                    stmt.span = this.popLocation();
                    if (!context.atConstructorBlock || context.foundSuperStatement)
                    {
                        this.reportSyntaxError(Problem.Constants.UNALLOWED_HERE, stmt.span, Problem.Argument.createTerm("superStatement"));
                    }
                    context.foundSuperStatement = true;
                }
                else
                {
                    this.duplicateLocation();
                    var super_dot = this.parseSuperOperator(super_arguments, true, OperatorPrecedence.LIST_OPERATOR);
                    this.parseSemicolon();
                    stmt = new Ast.ExpressionStatementNode(super_dot);
                    stmt.span = this.popLocation();
                }
            }
            // break
            // break label
            else if (this.token.type == Token.BREAK)
            {
                this.markLocation();
                this.next();
                marker = this.token.getSpan();
                str = isTokenAtNewLine() ? null : this.consumeIdentifier();
                this.parseSemicolon();
                stmt = new Ast.BreakNode(str);
                stmt.span = this.popLocation();
                Ast.Node break_target = null;
                if (str != null)
                {
                    if ((break_target = context.labels != null ? context.labels.get(str) : null) == null)
                    {
                        this.reportVerifyError(Problem.Constants.UNDEFINED_LABEL, marker, Problem.Argument.createQuote(str));
                    }
                }
                else if ((break_target = context.lastBreakableStatement) == null)
                {
                    this.reportSyntaxError(Problem.Constants.UNALLOWED_HERE, stmt.span, Problem.Argument.createToken(Token.BREAK));
                }
                ((Ast.BreakNode) stmt).targetStatement = break_target;
            }
            // continue
            // continue label
            else if (this.token.type == Token.CONTINUE)
            {
                this.markLocation();
                this.next();
                marker = this.token.getSpan();
                str = isTokenAtNewLine() ? null : this.consumeIdentifier();
                this.parseSemicolon();
                stmt = new Ast.ContinueNode(str);
                stmt.span = this.popLocation();
                Ast.Node cont_target = null;
                if (str != null)
                {
                    if ((cont_target = context.labels != null ? context.labels.get(str) : null) == null)
                    {
                        this.reportVerifyError(Problem.Constants.UNDEFINED_LABEL, marker, Problem.Argument.createQuote(str));
                    }
                }
                else if ((cont_target = context.lastContinuableStatement) == null)
                {
                    this.reportSyntaxError(Problem.Constants.UNALLOWED_HERE, stmt.span, Problem.Argument.createToken(Token.CONTINUE));
                }
                ((Ast.ContinueNode) stmt).targetStatement = cont_target;
            }
            // return
            // return expression
            else if (this.token.type == Token.RETURN)
            {
                this.markLocation();
                this.next();
                this.parseSemicolon();
                expr = null;
                if (!this.semicolonInserted && (expr = this.parseOptExpression()) != null)
                {
                    this.parseSemicolon();
                }
                stmt = new Ast.ReturnNode(expr);
                stmt.span = this.popLocation();
                if (!context.underFunction)
                {
                    this.reportSyntaxError(Problem.Constants.UNALLOWED_HERE, stmt.span, Problem.Argument.createToken(Token.RETURN));
                }
            }
            // throw exc
            else if (this.token.type == Token.THROW)
            {
                this.markLocation();
                this.next();
                expr = this.parseExpression();
                this.parseSemicolon();
                stmt = new Ast.ThrowNode(expr);
                stmt.span = this.popLocation();
            }
            // default xml namespace = xmlNS
            else if (this.token.type == Token.DEFAULT)
            {
                this.markLocation();
                this.next();
                this.invalidateLineBreak();
                this.expectContextKeyword("xml");
                this.invalidateLineBreak();
                this.expectContextKeyword("namespace");
                this.expect(Token.ASSIGN);
                expr = this.parseNonAssignmentExpression(false);
                this.parseSemicolon();
                stmt = new Ast.DXNSStatementNode(expr);
                stmt.span = this.popLocation();
            }
            // with (o) this.statement
            else if (this.token.type == Token.WITH)
            {
                this.markLocation();
                this.next();
                expr = this.token.type == Token.LPAREN ? this.parseParenListExpression() : null;
                if (expr != null)
                {
                    substmt = this.parseSubstatement(context.duplicate());
                    stmt = new Ast.WithStatementNode(((Ast.ParenExpressionNode) expr).expression, substmt);
                }
                else
                {
                    this.expect(Token.LPAREN);
                    stmt = new Ast.EmptyStatementNode();
                }
                stmt.span = this.popLocation();
            }
            // try {}
            // try {} finally {}
            // try {} catch-elements
            // try {} catch-elements finally {}
            else if (this.token.type == Token.TRY)
            {
                this.markLocation();
                var try_block = this.parseBlock(context.duplicate());
                var catch_elements = new Vector<Ast.CatchNode>();
                Ast.BlockNode finally_block = null;
                while (this.token.type == Token.CATCH)
                {
                    this.markLocation();
                    this.next();
                    Ast.PatternNode pattern_node = null;
                    if (this.token.type == Token.LPAREN)
                    {
                        this.openParen("parens");
                        pattern_node = this.parsePattern();
                        this.closeParen();
                    }
                    else
                    {
                        this.reportSyntaxError(Problem.Constants.EXPECTING_BEFORE, this.token.getSpan(), Problem.Argument.createToken(Token.LPAREN), Problem.Argument.createToken(this.token.type));
                        pattern_node = this.parsePattern();
                    }
                    var catch_node = new Ast.CatchNode(pattern_node, null);
                    var catch_ctx = context.duplicate();
                    catch_ctx.lastContinuableStatement = catch_node;
                    catch_node.block = this.parseBlock(catch_ctx);
                    catch_node.span = this.popLocation();
                    catch_elements.add(catch_node);
                }
                if (this.consume(Token.FINALLY))
                {
                    finally_block = this.parseBlock(context.duplicate());
                }
                if (catch_elements.size() == 0 && finally_block == null)
                {
                    this.expect(Token.CATCH);
                }

                stmt = new Ast.TryStatementNode(try_block, catch_elements, finally_block);
                stmt.span = this.popLocation();
            }
            else if (this.token.type == Token.SWITCH)
            {
                stmt = this.parseSwitchStatement(context);
            }
            else if (this.token.type == Token.FOR)
            {
                stmt = this.parseForStatement(context);
            }
            else if (this.token.type == Token.WHILE)
            {
                this.markLocation();
                this.next();
                this.openParen("parens");
                var while_head = this.parseExpression();
                this.closeParen();
                stmt = new Ast.WhileStatementNode(while_head, null);
                var while_ctx = context.duplicate();
                while_ctx.lastBreakableStatement = stmt;
                while_ctx.lastContinuableStatement = stmt;
                if (context.nextLoopLabel != "")
                {
                    while_ctx.labels = while_ctx.labels == null ? new HashMap<>() : while_ctx.labels;
                    while_ctx.labels.put(context.nextLoopLabel, stmt);
                }
                ((Ast.WhileStatementNode) stmt).substatement = this.parseSubstatement(while_ctx);
                stmt.span = this.popLocation();
            }
            else if (this.token.type == Token.DO)
            {
                this.markLocation();
                this.next();
                stmt = new Ast.DoStatementNode(null, null);
                var do_ctx = context.duplicate();
                do_ctx.lastBreakableStatement = stmt;
                do_ctx.lastContinuableStatement = stmt;
                if (context.nextLoopLabel != "")
                {
                    do_ctx.labels = do_ctx.labels == null ? new HashMap<>() : do_ctx.labels;
                    do_ctx.labels.put(context.nextLoopLabel, stmt);
                }
                ((Ast.DoStatementNode) stmt).substatement = this.parseSubstatement(do_ctx);
                if (!this.semicolonInserted)
                {
                    this.reportSyntaxError(Problem.Constants.EXPECTING_BEFORE, this.token.getSpan(), Problem.Argument.createToken(Token.SEMICOLON), Problem.Argument.createToken(this.token.type));
                }
                this.expect(Token.WHILE);
                ((Ast.DoStatementNode) stmt).expression = ((Ast.ParenExpressionNode) this.parseParenListExpression()).expression;
                this.parseSemicolon();
                stmt.span = this.popLocation();
            }
            else if (this.token.type == Token.IF)
            {
                this.markLocation();
                this.next();
                this.openParen("parens");
                var if_head = this.parseExpression();
                this.closeParen();
                var if_stmt1 = this.parseSubstatement(context.duplicate());
                Ast.StatementNode if_stmt2 = null;
                if (this.token.type == Token.ELSE)
                {
                    if (!this.semicolonInserted)
                    {
                        this.reportSyntaxError(Problem.Constants.EXPECTING_BEFORE, this.token.getSpan(), Problem.Argument.createToken(Token.SEMICOLON), Problem.Argument.createToken(this.token.type));
                    }
                    this.next();
                    if_stmt2 = this.parseSubstatement(context.duplicate());
                }
                stmt = new Ast.IfStatementNode(if_head, if_stmt1, if_stmt2);
                stmt.span = this.popLocation();
            }
            else if (this.token.type == Token.VAR
                ||   this.token.type == Token.CONST)
            {
                this.markLocation();
                var readOnly = this.consume(Token.CONST);
                if (!readOnly)
                {
                    this.consume(Token.VAR);
                }
                this.openParen("parens");
                var varBindings = new Vector<Ast.VarBindingNode>();
                do
                {
                    varBindings.add(this.parseVarBinding());
                }
                while (this.consume(Token.COMMA));
                this.closeParen();
                substmt = this.parseSubstatement(context.duplicate());
                stmt = new Ast.VarStatementNode(readOnly, varBindings, substmt);
                stmt.span = this.popLocation();
            }
        }
        else if (this.token.type == Token.LBRACE)
        {
            stmt = this.parseBlock(context);
        }

        if (stmt == null)
        {
            expr = this.parseOptExpression();
            if (expr != null)
            {
                this.parseSemicolon();
                stmt = new Ast.ExpressionStatementNode(expr);
                this.pushLocation(expr.span);
                stmt.span = this.popLocation();
            }
        }

        return stmt;
    }

    public Ast.StatementNode parseSubstatement(Parser.Context context)
    {
        if (this.token.type == Token.SEMICOLON)
        {
            return this.parseEmptyStatement();
        }
        return this.parseStatement(context);
    }

    public void parseSemicolon()
    {
        this.semicolonInserted = this.consume(Token.SEMICOLON) || this.isTokenAtNewLine() || this.token.type == Token.RBRACE;
    }

    public Ast.StatementNode parseEmptyStatement()
    {
        this.markLocation();
        this.next();
        var node = new Ast.EmptyStatementNode();
        node.span = this.popLocation();
        this.semicolonInserted = true;
        return node;
    }

    public Ast.VarBindingNode parseVarBinding()
    {
        return parseVarBinding(true);
    }

    public Ast.VarBindingNode parseVarBinding(boolean allowIn)
    {
        this.markLocation();
        var pattern = this.parsePattern();
        var initialiser = this.consume(Token.ASSIGN) ? this.parseExpression(allowIn, OperatorPrecedence.ASSIGNMENT_OPERATOR) : null;
        var node = new Ast.VarBindingNode(pattern, initialiser);
        node.span = this.popLocation();
        return node;
    }

    public Ast.SimpleVarDeclarationNode parseOptSimpleVarDeclaration()
    {
        return parseOptSimpleVarDeclaration(true);
    }

    public Ast.SimpleVarDeclarationNode parseOptSimpleVarDeclaration(boolean allowIn)
    {
        var prefix =  this.token.type == Token.VAR ? 0
                    : this.token.type == Token.CONST ? 1 : -1;
        if (prefix == -1)
        {
            return null;
        }
        this.markLocation();
        this.next();
        var bindings = new Vector<Ast.VarBindingNode>();
        do
        {
            bindings.add(this.parseVarBinding(allowIn));
        }
        while (this.consume(Token.COMMA));
        var node = new Ast.SimpleVarDeclarationNode(prefix == 1, bindings);
        node.span = this.popLocation();
        return node;
    }

    public Ast.StatementNode parseIdentifierStartedStatement(String str, Parser.Context context, Span marker)
    {
        this.pushLocation(marker);
        Ast.StatementNode stmt = null;

        if (this.consume(Token.COLON))
        {
            var label_ctx = context.duplicate();
            label_ctx.nextLoopLabel = str;
            var substmt = this.parseSubstatement(label_ctx);
            if (!substmt.isIterationStatement())
            {
                this.reportSyntaxError(Problem.Constants.ILLEGAL_LABEL_STATEMENT, substmt.span);
            }
            stmt = new Ast.LabeledStatementNode(str, substmt);
        }
        else
        {
            this.duplicateLocation();
            var expr = this.parseSubexpression(this.parseIdentifierStartedExpression(str), true, OperatorPrecedence.LIST_OPERATOR);
            this.parseSemicolon();
            stmt = new Ast.ExpressionStatementNode(expr);
        }
        stmt.span = this.popLocation();
        return stmt;
    }

    public Ast.StatementNode parseSwitchStatement(Parser.Context context)
    {
        this.markLocation();
        this.next();
        if (this.consumeContextKeyword("type"))
        {
            return this.parseSwitchTypeStatement(context);
        }

        var discriminant = ((Ast.ParenExpressionNode) this.parseParenListExpression()).expression;
        Ast.SwitchCaseNode lastCase = null;
        Ast.ExpressionNode expr = null;
        var cases = new Vector<Ast.SwitchCaseNode>();

        this.openBrace("block");
        this.semicolonInserted = true;

        var node = new Ast.SwitchStatementNode(discriminant, cases);
        var switch_ctx = context.duplicate();
        switch_ctx.lastBreakableStatement = node;

        while (this.token.type != Token.RBRACE)
        {
            if (!this.semicolonInserted)
            {
                throw this.reportSyntaxError(Problem.Constants.EXPECTING_BEFORE, this.token.getSpan(), Problem.Argument.createToken(Token.SEMICOLON));
            }
            this.markLocation();
            if (this.consume(Token.CASE))
            {
                expr = this.parseExpression();
                this.expect(Token.COLON);
                lastCase = new Ast.SwitchCaseNode(expr, null);
                lastCase.span = this.popLocation();
                cases.add(lastCase);
            }
            else if (this.consume(Token.DEFAULT))
            {
                this.expect(Token.COLON);
                lastCase = new Ast.SwitchCaseNode(null, null);
                lastCase.span = this.popLocation();
                cases.add(lastCase);
            }
            else if (lastCase != null)
            {
                this.popLocation();
                var drtv = this.parseOptDirective(switch_ctx);
                if (drtv == null)
                {
                    break;
                }
                lastCase.directives = lastCase.directives == null ? new Vector<>() : lastCase.directives;
                lastCase.directives.add(drtv);
                lastCase.span = new Span(lastCase.span.firstLine(), lastCase.span.start(), drtv.span.lastLine(), drtv.span.end());
            }
            else
            {
                break;
            }
        }
        this.closeBrace();
        this.semicolonInserted = true;
        node.span = this.popLocation();
        return node;
    }

    public Ast.StatementNode parseSwitchTypeStatement(Parser.Context context)
    {
        var discriminant = ((Ast.ParenExpressionNode) this.parseParenListExpression()).expression;
        var cases = new Vector<Ast.SwitchTypeCaseNode>();
        this.openBrace("block");
        while (this.token.type == Token.CASE)
        {
            this.markLocation();
            this.next();
            this.openParen("parens");
            var pattern = this.parsePattern();
            this.closeParen();
            var block = this.parseBlock(context.duplicate());
            var caseElement = new Ast.SwitchTypeCaseNode(pattern, block);
            caseElement.span = this.popLocation();
            cases.add(caseElement);
        }
        this.closeBrace();
        this.semicolonInserted = true;
        var node = new Ast.SwitchTypeStatementNode(discriminant, cases);
        node.span = this.popLocation();
        return node;
    }

    public Ast.StatementNode parseForStatement(Parser.Context context)
    {
        this.markLocation();
        this.next();
        if (this.consumeContextKeyword("each"))
        {
            return this.parseForEachStatement(context);
        }
        this.openParen("parens");
        Ast.Node expr1 = this.parseOptSimpleVarDeclaration(false);
        expr1 = expr1 == null ? this.parseOptExpression(false, OperatorPrecedence.POSTFIX_OPERATOR) : expr1;
        if (expr1 != null && this.token.type == Token.IN)
        {
            return this.parseForInStatement(false, expr1, context);
        }
        if (expr1 != null && (expr1 instanceof Ast.ExpressionNode))
        {
            expr1 = this.parseSubexpression((Ast.ExpressionNode) expr1, false, OperatorPrecedence.LIST_OPERATOR);
        }
        this.expect(Token.SEMICOLON);
        var expr2 = this.parseOptExpression();
        this.expect(Token.SEMICOLON);
        var expr3 = this.parseOptExpression();
        this.closeParen();
        var stmt = new Ast.ForStatementNode(expr1, expr2, expr3, null);
        var for_ctx = context.duplicate();
        for_ctx.lastBreakableStatement = stmt;
        for_ctx.lastContinuableStatement = stmt;
        if (context.nextLoopLabel != "")
        {
            for_ctx.labels = for_ctx.labels == null ? new HashMap<>() : null;
            for_ctx.labels.put(context.nextLoopLabel, stmt);
        }
        stmt.substatement = this.parseSubstatement(for_ctx);
        stmt.span = this.popLocation();
        return stmt;
    }

    public Ast.StatementNode parseForEachStatement(Parser.Context context)
    {
        this.openParen("parens");
        Ast.Node left = this.parseOptSimpleVarDeclaration(false);
        left = left == null ? this.parseExpression(false, OperatorPrecedence.POSTFIX_OPERATOR) : left;
        return this.parseForInStatement(true, left, context);
    }

    public Ast.StatementNode parseForInStatement(boolean each, Ast.Node left, Parser.Context context)
    {
        var declr = left instanceof Ast.SimpleVarDeclarationNode ? ((Ast.SimpleVarDeclarationNode) left) : null;
        if (declr != null)
        {
            if (declr.bindings.size() != 1)
            {
                this.reportSyntaxError(Problem.Constants.UNEXPECTED_MULTIPLE_VARIABLES, declr.span);
            }
            else if (declr.bindings.get(0).initialiser != null)
            {
                this.reportSyntaxError(Problem.Constants.UNALLOWED_HERE, declr.bindings.get(0).initialiser.span, Problem.Argument.createTerm("initialiser"));
            }
        }
        if (!this.consume(Token.IN))
        {
            throw this.expect(Token.IN);
        }
        var right = this.parseExpression();
        this.closeParen();
        var stmt = new Ast.ForInStatementNode(each, left, right, null);
        var for_ctx = context.duplicate();
        for_ctx.lastBreakableStatement = stmt;
        for_ctx.lastContinuableStatement = stmt;
        if (context.nextLoopLabel != "")
        {
            for_ctx.labels = for_ctx.labels == null ? new HashMap<>() : for_ctx.labels;
            for_ctx.labels.put(context.nextLoopLabel, stmt);
        }
        stmt.substatement = this.parseSubstatement(for_ctx);
        stmt.span = this.popLocation();
        return stmt;
    }

    public Ast.BlockNode parseBlock(Parser.Context context)
    {
        this.markLocation();
        this.openBrace("block");
        var directives = this.parseOptDirectives(context);
        while (this.token.type != Token.RBRACE
            && this.token.type != Token.EOF
            && directives.size() != 0
            && !this.semicolonInserted)
        {
            throw this.reportSyntaxError(Problem.Constants.EXPECTING_BEFORE, this.token.getSpan(), Problem.Argument.createToken(Token.SEMICOLON), Problem.Argument.createToken(this.token.type));
        }
        this.closeBrace();
        this.semicolonInserted = true;
        var node = new Ast.BlockNode(directives);
        node.span = this.popLocation();
        return node;
    }

    public Vector<Ast.DirectiveNode> parseOptDirectives(Parser.Context context)
    {
        var result = new Vector<Ast.DirectiveNode>();
        var drtv = this.parseOptDirective(context);
        if (drtv == null)
        {
            return result;
        }
        result.add(drtv);
        for (;;)
        {
            if (this.semicolonInserted && (drtv = this.parseOptDirective(context)) != null)
            {
                result.add(drtv);
            }
            else if (this.token.type == Token.SEMICOLON)
            {
                result.add(this.parseEmptyStatement());
            }
            else
            {
                break;
            }
        }
        return result;
    }

    private Ast.DirectiveNode parseOptDirective(Parser.Context context)
    {
        this.semicolonInserted = false;
        var str = "";
        Ast.DirectiveNode drtv = null;
        Ast.ExpressionNode expr = null;
        Span marker = null;

        if (this.token.type == Token.IDENTIFIER)
        {
            this.markLocation();
            str = this.consumeIdentifier();
            // include directive
            if (str.equals("include"))
            {
                return this.parseIncludeDirective(context);
            }
            else if (this.token.type == Token.IDENTIFIER && !this.isTokenAtNewLine())
            {
                this.attributeData = new AttributeData();
                this.duplicateLocation();
                if ((drtv = this.parseOptAnnotatableDefinition(this.popLocation(), context, str)) != null)
                {
                    this.popLocation();
                }
                else
                {
                    this.duplicateLocation();
                    this.validateAttribute(str, this.popLocation());
                    this.parseAttributeCombination();
                    drtv = this.parseOptAnnotatableDefinition(this.popLocation(), context);
                    if (drtv == null)
                    {
                        throw this.reportSyntaxError(Problem.Constants.UNEXPECTED_TOKEN, this.token.getSpan());
                    }
                }
                return drtv;
            }
            else if (token.type.isKeyword() && this.precedingReservedNamespace() && !this.isTokenAtNewLine())
            {
                this.attributeData = new AttributeData();
                this.duplicateLocation();
                this.validateAttribute(str, this.popLocation());
                this.parseAttributeCombination();
                drtv = this.parseOptAnnotatableDefinition(this.popLocation(), context);
                if (drtv == null)
                {
                    throw this.reportSyntaxError(Problem.Constants.UNEXPECTED_TOKEN, this.token.getSpan());
                }
                return drtv;
            }
            else if (this.precedingKeywordDefinition())
            {
                this.attributeData = new AttributeData();
                this.duplicateLocation();
                this.validateAttribute(str, this.popLocation());
                return this.parseOptAnnotatableDefinition(this.popLocation(), context);
            }
            else
            {
                return this.parseIdentifierStartedStatement(str, context, this.popLocation());
            }
        }
        else if (token.type.isKeyword())
        {
            if (this.token.type == Token.IMPORT)
            {
                return this.parseImportDirective();
            }
            else if (this.token.type == Token.USE)
            {
                return this.parseUseDirective();
            }
            else if (this.precedingKeywordDefinition())
            {
                this.attributeData = new AttributeData();
                return this.parseOptAnnotatableDefinition(this.token.getSpan(), context);
            }
            else if ((expr = this.parseOptReservedNamespace()) != null)
            {
                if (this.token.type == Token.IDENTIFIER || this.precedingKeywordDefinition())
                {
                    this.attributeData = new AttributeData();
                    this.attributeData.qualifier = expr;
                    this.parseAttributeCombination();
                    pushLocation(expr.span);
                    drtv = this.parseOptAnnotatableDefinition(this.popLocation(), context);
                    if (drtv == null)
                    {
                        throw this.reportSyntaxError(Problem.Constants.UNEXPECTED_TOKEN, marker);
                    }
                    return drtv;
                }
                else
                {
                    if (this.token.type == Token.COLON_COLON)
                    {
                        expr = parseQualifiedIdentifierFinal(expr);
                    }
                    expr = this.parseSubexpression(expr, true, OperatorPrecedence.LIST_OPERATOR);
                    this.parseSemicolon();
                    this.pushLocation(expr.span);
                    drtv = new Ast.ExpressionStatementNode(expr);
                    drtv.span = this.popLocation();
                    return drtv;
                }
            }
        }
        else if (this.token.type == Token.LBRACKET)
        {
            return this.parseBracketStartedDirective(context);
        }
        else if (this.token.type == Token.SEMICOLON)
        {
            return this.parseEmptyStatement();
        }
        return this.parseOptStatement(context);
    }

    private Ast.DirectiveNode parseBracketStartedDirective(Parser.Context context)
    {
        this.markLocation();
        Ast.DirectiveNode drtv = null;
        Ast.ExpressionNode expr = this.parseArrayLiteral(true);
        Ast.ExpressionNode brackets = null;

        if (expr instanceof Ast.ArrayLiteralNode)
        {
            while (this.token.type == Token.LBRACKET)
            {
                this.openBracket("expression");
                brackets = this.parseExpression();
                this.closeBracket();
                this.pushLocation(expr.span);
                expr = new Ast.BracketsNode(expr, brackets);
                expr.span = this.popLocation();
            }
            if (this.token.type == Token.IDENTIFIER || this.precedingKeywordDefinition() || this.precedingReservedNamespace())
            {
                this.pushLocation(expr.span);
                this.attributeData = new AttributeData();
                this.attributeData.metaData = this.filterMetaData(expr);
                this.parseAttributeCombination();
                drtv = this.parseOptAnnotatableDefinition(this.popLocation(), context);
                if (drtv == null)
                {
                    throw this.reportSyntaxError(Problem.Constants.UNEXPECTED_TOKEN, this.token.getSpan());
                }
                return drtv;
            }
        }

        expr = this.parseSubexpression(expr, true, OperatorPrecedence.LIST_OPERATOR);
        drtv = new Ast.ExpressionStatementNode(expr);
        this.parseSemicolon();
        this.pushLocation(expr.span);
        drtv.span = this.popLocation();
        return drtv;
    }

    private Vector<MetaData> filterMetaData(Ast.Node node)
    {
        var result = new Vector<MetaData>();
        Ast.ArrayLiteralNode array_literal = null;

        while (node instanceof Ast.BracketsNode)
        {
            processSingleMetaData(((Ast.BracketsNode) node).key, result);
            node = ((Ast.BracketsNode) node).base;
        }

        array_literal = (Ast.ArrayLiteralNode) node;
        if (array_literal.elements.size() > 0)
        {
            processSingleMetaData(array_literal.elements.get(0), result);
        }

        return result;
    }

    private void processSingleMetaData(Ast.ExpressionNode expr, Vector<MetaData> result)
    {
        Ast.ArrayLiteralNode array_literal = null;
        Ast.BracketsNode brackets = null;

        MetaData metaData = null;
        String name = "";
        var call_node = expr instanceof Ast.CallNode ? ((Ast.CallNode) expr) : null;
        if (call_node != null)
        {
            if (call_node.base instanceof Ast.SimpleIdNode)
            {
                name = ((Ast.SimpleIdNode) call_node.base).name;
                if (((Ast.SimpleIdNode) call_node.base).qualifier instanceof Ast.SimpleIdNode)
                {
                    name = ((Ast.SimpleIdNode) ((Ast.SimpleIdNode) call_node.base).qualifier).name + "::" + name;
                }
            }
            if (name != "")
            {
                var entries = new Vector<MetaData.Entry>();
                for (var argument : call_node.arguments)
                {
                    var entry = this.filterMetaDataEntry(argument);
                    if (entry != null)
                    {
                        entries.add(entry);
                    }
                }
                metaData = new MetaData(name, entries);
            }
        }
        else if (expr instanceof Ast.SimpleIdNode)
        {
            name = ((Ast.SimpleIdNode) expr).name;
            if (((Ast.SimpleIdNode) expr).qualifier instanceof Ast.SimpleIdNode)
            {
                name = ((Ast.SimpleIdNode) ((Ast.SimpleIdNode) expr).qualifier).name + "::" + name;
            }
            metaData = new MetaData(name, null);
        }

        if (metaData != null)
        {
            result.add(metaData);
        }
    }

    private MetaData.Entry filterMetaDataEntry(Ast.ExpressionNode expr)
    {
        var assignExpr = expr instanceof Ast.AssignmentNode ? ((Ast.AssignmentNode) expr) : null;
        var name = "";
        Ast.ExpressionNode literal = null;
        Ast.SimpleIdNode simpleID = null;
        MetaData.Entry entry = null;

        if (assignExpr != null && assignExpr.compound == null)
        {
            simpleID = assignExpr.left instanceof Ast.SimpleIdNode ? ((Ast.SimpleIdNode) assignExpr.left) : null;
            if (simpleID != null)
            {
                name = simpleID.name;
                if (simpleID.qualifier instanceof Ast.SimpleIdNode)
                {
                    name = ((Ast.SimpleIdNode) simpleID.qualifier).name + "::" + name;
                }
            }
            literal =  (assignExpr.right instanceof Ast.StringLiteralNode
                    ||  assignExpr.right instanceof Ast.NumericLiteralNode
                    ||  assignExpr.right instanceof Ast.BooleanLiteralNode) ? assignExpr.right : null;
            if (name != "" && literal != null)
            {
                entry = new MetaData.Entry(name, literal instanceof Ast.StringLiteralNode ? ((Ast.StringLiteralNode) literal).value : literal instanceof Ast.NumericLiteralNode ? ((Ast.NumericLiteralNode) literal).value : ((Ast.BooleanLiteralNode) literal).value);
                return entry;
            }
            else
            {
                return null;
            }
        }
        else
        {
            literal =  (expr instanceof Ast.StringLiteralNode
                    ||  expr instanceof Ast.NumericLiteralNode
                    ||  expr instanceof Ast.BooleanLiteralNode) ? expr : null;
            if (literal != null)
            {
                entry = new MetaData.Entry(null, literal instanceof Ast.StringLiteralNode ? ((Ast.StringLiteralNode) literal).value : literal instanceof Ast.NumericLiteralNode ? ((Ast.NumericLiteralNode) literal).value : ((Ast.BooleanLiteralNode) literal).value);
                return entry;
            }
            return null;
        }
    }

    /**
     * Validates identifier for attribute combination.
     */
    private void validateAttribute(String str, Span span)
    {
        switch (str)
        {
            case "static":
                this.attributeData.staticModifier = true;
                break;
            case "override":
                this.attributeData.overrideModifier = true;
                break;
            case "native":
                this.attributeData.nativeModifier = true;
                break;
            case "final":
                this.attributeData.finalModifier = true;
                break;
            case "dynamic":
                this.attributeData.dynamicModifier = true;
                break;
            default:
            {
                if (this.attributeData.qualifier != null)
                {
                    this.reportSyntaxError(Problem.Constants.DUPLICATE_NAMESPACE_ATTRIBUTE, span);
                }
                else
                {
                    this.attributeData.qualifier = new Ast.SimpleIdNode(null, str);
                    this.attributeData.qualifier.span = span;
                }
            }
        }
    }

    private boolean precedingKeywordDefinition()
    {
        return token.type.isKeyword() &&
            (  this.token.type == Token.VAR
            || this.token.type == Token.CONST
            || this.token.type == Token.FUNCTION
            || this.token.type == Token.CLASS
            || this.token.type == Token.INTERFACE);
    }


    private boolean precedingContextKeywordDefinition()
    {
        return this.token.type == Token.IDENTIFIER &&
            (  this.token.stringValue.equals("enum")
            || this.token.stringValue.equals("type")
            || this.token.stringValue.equals("namespace"));
    }

    private boolean precedingReservedNamespace()
    {
        return this.token.type == Token.PUBLIC
            || this.token.type == Token.INTERNAL
            || this.token.type == Token.PRIVATE
            || this.token.type == Token.PROTECTED;
    }

    private void parseAttributeCombination()
    {
        var startPhase = this.attributeData.hasModifiers();

        for (;;)
        {
            var str = this.token.stringValue;
            if (this.token.type == Token.IDENTIFIER && !this.precedingContextKeywordDefinition())
            {
                if (!startPhase)
                {
                    this.invalidateLineBreak();
                }
                this.validateAttribute(str, this.token.getSpan());
                this.next();
            }
            else if (this.precedingReservedNamespace())
            {
                if (!startPhase)
                {
                    this.invalidateLineBreak();
                }
                var ns = this.parseOptReservedNamespace();
                if (this.attributeData.qualifier != null)
                {
                    this.reportSyntaxError(Problem.Constants.DUPLICATE_NAMESPACE_ATTRIBUTE, ns.span);
                }
                else
                {
                    this.attributeData.qualifier = ns;
                }
            }
            else
            {
                break;
            }
            startPhase = false;
        }
    }

    private Ast.IncludeDirectiveNode parseIncludeDirective(Parser.Context context)
    {
        return parseIncludeDirective(context, true);
    }

    private Ast.IncludeDirectiveNode parseIncludeDirective(Parser.Context context, boolean allowPackages)
    {
        var src = this.token.stringValue;
        this.expect(Token.STRING_LITERAL);
        this.parseSemicolon();
        var node = new Ast.IncludeDirectiveNode(src);
        node.span = this.popLocation();

        // process source
        {
            Path file = null;
            if (this.script.url() != null)
            {
                try
                {
                    URI uri = new URI(script.url());
                    file = Paths.get(uri).resolve(node.src);
                }
                catch (URISyntaxException exc)
                {
                    this.reportWarning(Problem.Constants.INCLUDE_PROCESSING_ERROR, node.span);
                }
            }
            else
            {
                file = Paths.get(node.src);
            }
            String text = null;
            if (file != null)
            {
                try
                {
                    text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                }
                catch (IOException exc)
                {
                    this.reportWarning(Problem.Constants.INCLUDE_PROCESSING_ERROR, node.span);
                }
                catch (SecurityException exc)
                {
                    this.reportWarning(Problem.Constants.INCLUDE_PROCESSING_ERROR, node.span);
                }
            }
            if (text != null)
            {
                node.subscript = new Script(text, file.toUri().toString());
                this.script.subscripts = this.script.subscripts == null ? new Vector<>() : this.script.subscripts;
                this.script.subscripts.add(node.subscript);

                try
                {
                    var parser = new UnderlyingParser(new Lexer(node.subscript));
                    try
                    {
                        parser.lexer.shift();
                    }
                    catch (Problem exc)
                    {
                    }
                    Vector<Ast.DirectiveNode> topIncludings = null;
                    if (allowPackages)
                    {
                        while (parser.token.type == Token.PACKAGE)
                        {
                            node.subpackages = node.subpackages == null ? new Vector<>() : node.subpackages;
                            node.subpackages.add(parser.parsePackageDefinition());
                        }
                        while (parser.token.type == Token.IDENTIFIER && parser.token.stringValue.equals("include"))
                        {
                            parser.markLocation();
                            parser.next();
                            Ast.IncludeDirectiveNode drtv = parser.parseIncludeDirective(context, true);
                            if (drtv.subpackages != null)
                            {
                                for (var p : drtv.subpackages)
                                {
                                    node.subpackages = node.subpackages == null ? new Vector<>() : node.subpackages;
                                    node.subpackages.add(p);
                                }
                                drtv.subpackages = null;
                            }
                            topIncludings = topIncludings == null ? new Vector<>() : topIncludings;
                            topIncludings.add(drtv);
                            if (drtv.subdirectives != null && drtv.subdirectives.size() != 0)
                            {
                                break;
                            }
                        }
                    }

                    var directives = parser.parseOptDirectives(context);
                    if (topIncludings != null)
                    {
                        for (int i = topIncludings.size(); --i != -1;)
                        {
                            directives.add(0, topIncludings.get(i));
                        }
                    }
                    node.subdirectives = directives;

                    if (parser.token.type != Token.EOF)
                    {
                        parser.reportSyntaxError(Problem.Constants.UNALLOWED_HERE, parser.token.getSpan(), Problem.Argument.createToken(parser.token.type));
                    }
                }
                catch (Problem exc)
                {
                }

                if (node.subscript.problems != null)
                {
                    for (var problem : node.subscript.problems)
                    {
                        this.script.collectProblem(problem);
                    }
                }
            }
        }

        return node;
    }

    private Ast.DirectiveNode parseUseDirective()
    {
        this.markLocation();
        this.next();
        Ast.DirectiveNode drtv = null;

        if (this.consume(Token.DEFAULT))
        {
            this.expectContextKeyword("namespace");
            drtv = new Ast.UseDefaultDirectiveNode(this.parseExpression());
            this.parseSemicolon();
            drtv.span = this.popLocation();
        }
        else
        {
            this.expectContextKeyword("namespace");
            drtv = new Ast.UseDirectiveNode(this.parseExpression());
            this.parseSemicolon();
            drtv.span = this.popLocation();
        }
        return drtv;
    }

    private Ast.DirectiveNode parseImportDirective()
    {
        this.markLocation();
        this.next();
        this.markLocation();
        var str = this.expectIdentifier();
        String alias = "";
        Span aliasSpan = null;
        var importName = new Vector<String>();

        if (this.token.type == Token.ASSIGN)
        {
            alias = str;
            aliasSpan = this.popLocation();
            this.next();
            this.markLocation();
            importName.add(this.expectIdentifier());
        }
        else
        {
            importName.add(str);
            if (this.token.type != Token.DOT)
            {
                this.reportSyntaxError(Problem.Constants.EXPECTING_BEFORE, this.token.getSpan(), Problem.Argument.createToken(Token.DOT), Problem.Argument.createToken(this.token.type));
            }
        }

        boolean wildcard = false;

        while (this.consume(Token.DOT))
        {
            if (alias != "" && this.consume(Token.TIMES))
            {
                wildcard = true;
                break;
            }
            importName.add(this.expectIdentifier());
        }

        this.parseSemicolon();

        var node = new Ast.ImportDirectiveNode(alias, importName.join("."), wildcard);
        node.aliasSpan = aliasSpan;
        node.importNameSpan = this.popLocation();
        node.span = this.popLocation();
        return node;
    }

    private Ast.PackageDefinitionNode parsePackageDefinition()
    {
        this.markLocation();
        this.next();
        var id = this.consumeIdentifier();
        id = id == null ? "" : id;
        while (this.consume(Token.DOT))
        {
            id += "." + this.expectIdentifier();
        }
        var context = new Parser.Context();
        context.atPackageFrame = true;
        var block = this.parseBlock(context);
        var node = new Ast.PackageDefinitionNode(id, block);
        node.span = this.popLocation();
        node.script = this.script;
        return node;
    }

    private Ast.DirectiveNode parseOptAnnotatableDefinition(Span marker, Parser.Context context)
    {
        return parseOptAnnotatableDefinition(marker, context, null);
    }

    private Ast.DirectiveNode parseOptAnnotatableDefinition(Span marker, Parser.Context context, String str)
    {
        if (str == null || str.equals(""))
        {
            if (this.token.type.isKeyword())
            {
                if (this.token.type == Token.CLASS)
                {
                    return this.parseClassDefinition(marker, context);
                }
                if (this.token.type == Token.INTERFACE)
                {
                    return this.parseInterfaceDefinition(marker, context);
                }
                if (this.token.type == Token.FUNCTION)
                {
                    return this.parseFunctionDefinition(marker, context);
                }
                if (this.token.type == Token.VAR
                ||  this.token.type == Token.CONST)
                {
                    return this.parseVarDefinition(marker, context);
                }
            }
            else
            {
                str = this.expectIdentifier();
            }
        }
        switch (str)
        {
            case "enum":
                return this.parseEnumDefinition(marker, context);
            case "type":
                return this.parseTypeDefinition(marker, context);
            case "namespace":
                return this.parseNamespaceDefinition(marker, context);
            default:
                return null;
        }
    }

    private Ast.DirectiveNode parseVarDefinition(Span marker, Parser.Context context)
    {
        this.pushLocation(marker);
        var readOnly = this.token.type == Token.CONST;
        if (!readOnly && context.atEnumFrame)
        {
            this.reportSyntaxError(Problem.Constants.INVALID_ENUM_CONSTANT, this.token.getSpan());
        }
        this.next();
        var bindings = new Vector<Ast.VarBindingNode>();
        if (this.token.type == Token.LPAREN && this.attributeData.isEmpty())
        {
            // variable enclosing statement
            this.openParen("parens");
            do
            {
                bindings.add(this.parseVarBinding(true));
            }
            while (this.consume(Token.COMMA));
            this.closeParen();
            var stmt = new Ast.VarStatementNode(readOnly, bindings, this.parseSubstatement(context.duplicate()));
            stmt.span = this.popLocation();
            return stmt;
        }
        else
        {
            var attributeData = this.attributeData;

            do
            {
                var binding = this.parseVarBinding(true);
                if (context.atEnumFrame && (!(binding.pattern instanceof Ast.NamePatternNode) || binding.pattern.type != null))
                {
                    this.reportSyntaxError(Problem.Constants.INVALID_ENUM_CONSTANT, binding.span);
                }
                bindings.add(binding);
            }
            while (this.consume(Token.COMMA));
            this.parseSemicolon();
            var node = new Ast.VarDefinitionNode(readOnly, bindings);
            this.completeDefinition(node, attributeData);

            if (node.isStatic() && !context.atClassFrame && !context.atEnumFrame)
            {
                this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("static"));
            }

            if (node.hasOverride())
            {
                this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("override"));
            }

            if (node.isNative())
            {
                this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("native"));
            }

            if (node.isFinal())
            {
                this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("final"));
            }

            if (node.isDynamic())
                this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("dynamic"));

            return node;
        }
    }

    private Ast.DirectiveNode parseFunctionDefinition(Span marker, Parser.Context context)
    {
        this.pushLocation(marker);
        this.next();

        var attributeData = this.attributeData;

        var nameSpan = this.token.getSpan();
        var str = this.expectIdentifier();
        boolean getter = false;
        boolean setter = false;
        boolean atConstructor = false;

        if (this.token.type == Token.IDENTIFIER)
        {
            if (str.equals("get"))
            {
                getter = true;
                nameSpan = this.token.getSpan();
                str = this.consumeIdentifier();
            }
            else if (str.equals("set"))
            {
                setter = true;
                nameSpan = this.token.getSpan();
                str = this.consumeIdentifier();
            }
        }
        else if (str.equals(context.classLocalName))
        {
            atConstructor = true;
        }

        var common = this.parseFunctionCommon(true, atConstructor);
        var node = new Ast.FunctionDefinitionNode(str, common);
        node.nameSpan = nameSpan;
        node.setIsGetter(getter);
        node.setIsSetter(setter);
        node.setIsConstructor(atConstructor);
        this.completeDefinition(node, attributeData);

        if ((node.isConstructor() || node.isGetter() || node.isSetter()) && (node.hasYield() || node.hasAwait()))
            this.reportSyntaxError(node.hasYield() ? Problem.Constants.FUNCTION_MUST_NOT_CONTAIN_YIELD : Problem.Constants.FUNCTION_MUST_NOT_CONTAIN_AWAIT, node.nameSpan);

        if (node.isStatic() && !context.atClassFrame && !context.atEnumFrame)
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.nameSpan, Problem.Argument.createQuote("static"));
        }

        if (node.hasOverride()  && ((!context.atClassFrame && !context.atEnumFrame) || node.isStatic()))
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("override"));
        }

        if (node.isNative())
        {
            if (context.atInterfaceFrame)
            {
                this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("native"));
            }
            if (node.common.body != null)
            {
                this.reportSyntaxError(Problem.Constants.FUNCTION_MUST_NOT_SPECIFY_BODY, node.nameSpan);
            }
        }
        else if (context.atInterfaceFrame)
        {
            // function body in interface is optional
        }
        else
        {
            if (node.common.body == null)
            {
                this.reportSyntaxError(Problem.Constants.FUNCTION_OMITS_BODY, node.nameSpan);
            }
        }

        if (node.isFinal() && !context.atClassFrame)
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("final"));
        }

        if (node.isDynamic())
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("dynamic"));

        return node;
    }

    private Ast.FunctionCommonNode parseFunctionCommon()
    {
        return parseFunctionCommon(false);
    }

    private Ast.FunctionCommonNode parseFunctionCommon(boolean forFunctionDefinition)
    {
        return parseFunctionCommon(forFunctionDefinition, false);
    }

    private Ast.FunctionCommonNode parseFunctionCommon(boolean forFunctionDefinition, boolean atConstructor)
    {
        this.markLocation();
        this.openParen("parameterList");
        Vector<Ast.PatternNode> params = null;
        Vector<Ast.VarBindingNode> optParams = null;
        String rest = null;

        if (this.token.type != Token.RPAREN)
        {
            do
            {
                if (this.consume(Token.ELLIPSIS))
                {
                    rest = expectIdentifier();
                    break;
                }
                else
                {
                    var binding = this.parseVarBinding(true);
                    if (binding.initialiser != null)
                    {
                        optParams = optParams == null ? new Vector<>() : optParams;
                        optParams.add(binding);
                    }
                    else
                    {
                        if (optParams != null)
                        {
                            this.reportSyntaxError(Problem.Constants.ILLEGAL_REQUIRED_PARAM, binding.span);
                        }
                        params = params == null ? new Vector<>() : params;
                        params.add(binding.pattern);
                    }
                }
            }
            while (this.consume(Token.COMMA));
        }
        this.closeParen();
        var result = this.consume(Token.COLON) ? this.parseTypeAnnotation(true) : null;
        if (atConstructor && result != null)
        {
            this.reportSyntaxError(Problem.Constants.CONSTRUCTOR_MUST_NOT_ANNOTATE_RETURN, result.span);
        }
        this.functionFlagsStack.push(0);
        var body = this.parseFunctionBody(forFunctionDefinition, atConstructor);
        var flags = this.functionFlagsStack.pop();
        var common = new Ast.FunctionCommonNode(params, optParams, rest, result, body);
        common.span = this.popLocation();
        common.setYield((flags & 1) != 0);
        common.setAwait((flags & 2) != 0);
        return common;
    }

    public Ast.TypeIdNode parseTypedIdentifier()
    {
        this.markLocation();
        var str = this.expectIdentifier();
        var node = new Ast.TypeIdNode(str, this.consume(Token.COLON) ? this.parseTypeAnnotation() : null);
        node.span = this.popLocation();
        return node;
    }

    private Ast.Node parseFunctionBody()
    {
        return parseFunctionBody(false);
    }

    private Ast.Node parseFunctionBody(boolean forFunctionDefinition)
    {
        return parseFunctionBody(forFunctionDefinition, false);
    }

    private Ast.Node parseFunctionBody(boolean forFunctionDefinition, boolean atConstructor)
    {
        var context = new Parser.Context();
        context.underFunction = true;
        context.atConstructorBlock = atConstructor;
        var block = this.token.type == Token.LBRACE ? this.parseBlock(context) : null;
        if (block != null)
        {
            return block;
        }
        if (this.token.type == Token.EOF || (this.isTokenAtNewLine() && (lexer.getLineIndentation(this.token.firstLine) <= lexer.getLineIndentation(this.previousToken.lastLine))))
        {
            if (forFunctionDefinition)
            {
                this.parseSemicolon();
            }
            return null;
        }
        var expr = this.parseExpression(true, OperatorPrecedence.ASSIGNMENT_OPERATOR);
        if (forFunctionDefinition)
        {
            this.parseSemicolon();
        }
        return expr;
    }

    private Ast.DirectiveNode parseNamespaceDefinition(Span marker, Parser.Context context)
    {
        this.pushLocation(marker);
        var attributeData = this.attributeData;
        var nameSpan = this.token.getSpan();
        var str = this.expectIdentifier();
        var expr = this.consume(Token.ASSIGN) ? this.parseExpression() : null;
        this.parseSemicolon();
        var node = new Ast.NamespaceDefinitionNode(str, expr);
        node.nameSpan = nameSpan;
        this.completeDefinition(node, attributeData);

        if (node.isStatic() && !context.atClassFrame && !context.atEnumFrame)
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("static"));
        }

        if (node.hasOverride())
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("override"));
        }

        if (node.isNative())
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("native"));
        }

        if (node.isFinal())
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("final"));
        }

        if (node.isDynamic())
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("dynamic"));

        return node;
    }

    private Ast.DirectiveNode parseTypeDefinition(Span marker, Parser.Context context)
    {
        this.pushLocation(marker);
        var attributeData = this.attributeData;
        var nameSpan = this.token.getSpan();
        var str = this.expectIdentifier();
        this.expect(Token.ASSIGN);
        var type = this.parseTypeAnnotation();
        this.parseSemicolon();
        var node = new Ast.TypeDefinitionNode(str, type);
        node.nameSpan = nameSpan;
        this.completeDefinition(node, attributeData);

        if (node.isStatic())
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("static"));
        }

        if (node.hasOverride())
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("override"));
        }

        if (node.isNative())
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("native"));
        }

        if (node.isFinal())
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("final"));

        if (node.isDynamic())
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("dynamic"));

        return node;
    }

    private Ast.DirectiveNode parseClassDefinition(Span marker, Parser.Context parentContext)
    {
        this.pushLocation(marker);
        this.next();
        var attributeData = this.attributeData;
        var nameSpan = this.token.getSpan();
        var name = this.expectIdentifier();
        Ast.ExpressionNode extendsClass = null;
        Vector<Ast.ExpressionNode> implementsList = null;
        if (this.consumeContextKeyword("extends"))
        {
            extendsClass = this.parseTypeAnnotation();
        }
        if (this.consumeContextKeyword("implements"))
        {
            implementsList = new Vector<>();
            do
            {
                implementsList.add(this.parseTypeAnnotation());
            }
            while (this.consume(Token.COMMA));
        }
        var context = new Parser.Context();
        context.atClassFrame = true;
        context.classLocalName = name;
        var block = this.parseBlock(context);
        var node = new Ast.ClassDefinitionNode(name, extendsClass, implementsList, block);
        node.nameSpan = nameSpan;
        this.completeDefinition(node, attributeData);

        if (node.isStatic())
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("static"));
        }

        if (node.hasOverride())
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("override"));
        }

        if (node.isNative())
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("native"));
        }

        return node;
    }

    private Ast.DirectiveNode parseInterfaceDefinition(Span marker, Parser.Context parentContext)
    {
        this.pushLocation(marker);
        this.next();
        var attributeData = this.attributeData;
        var nameSpan = this.token.getSpan();
        var name = this.expectIdentifier();
        Vector<Ast.ExpressionNode> extendsList = null;
        if (this.consumeContextKeyword("extends"))
        {
            extendsList = new Vector<>();
            do
            {
                extendsList.add(this.parseTypeAnnotation());
            }
            while (this.consume(Token.COMMA));
        }
        var context = new Parser.Context();
        context.atInterfaceFrame = true;
        var block = this.parseBlock(context);
        var node = new Ast.InterfaceDefinitionNode(name, extendsList, block);
        node.nameSpan = nameSpan;
        this.completeDefinition(node, attributeData);

        for (var drtv : block.directives)
        {
            if (!(drtv instanceof Ast.FunctionDefinitionNode || drtv instanceof Ast.ClassDefinitionNode || drtv instanceof Ast.InterfaceDefinitionNode || drtv instanceof Ast.EnumDefinitionNode))
            {
                this.reportSyntaxError(Problem.Constants.DIRECTIVE_NOT_ALLOWED_IN_INTERFACE, drtv.span);
            }
        }

        if (node.isStatic())
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("static"));
        }

        if (node.hasOverride())
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("override"));
        }

        if (node.isNative())
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("native"));
        }

        if (node.isFinal())
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("final"));
        }

        if (node.isDynamic())
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("dynamic"));

        return node;
    }

    private Ast.DirectiveNode parseEnumDefinition(Span marker, Parser.Context parentContext)
    {
        this.pushLocation(marker);
        var attributeData = this.attributeData;
        var nameSpan = this.token.getSpan();
        var name = this.expectIdentifier();
        var numericType = this.consume(Token.COLON) ? this.parseTypeAnnotation() : null;
        var context = new Parser.Context();
        context.atEnumFrame = true;
        var block = this.parseBlock(context);
        var node = new Ast.EnumDefinitionNode(name, numericType, block);
        node.nameSpan = nameSpan;
        this.completeDefinition(node, attributeData);

        if (node.isStatic())
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("static"));
        }

        if (node.hasOverride())
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("override"));
        }

        if (node.isNative())
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("native"));
        }

        if (node.isFinal())
        {
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("final"));
        }

        if (node.isDynamic())
            this.reportSyntaxError(Problem.Constants.ATTRIBUTE_UNALLOWED_FOR_DEFINITION, node.span, Problem.Argument.createQuote("dynamic"));

        return node;
    }

    private void completeDefinition(Ast.DefinitionNode node, AttributeData attributeData)
    {
        node.span = this.popLocation();
        node.accessModifier = attributeData.qualifier;
        node.metaDataArray = attributeData.metaData;
        node.setIsFinal(attributeData.finalModifier);
        node.setIsNative(attributeData.nativeModifier);
        node.setOverride(attributeData.overrideModifier);
        node.setIsStatic(attributeData.staticModifier);
        node.setIsDynamic(attributeData.dynamicModifier);
    }

    public Ast.ProgramNode parseProgram()
    {
        this.markLocation();
        var packages = new Vector<Ast.PackageDefinitionNode>();
        while (this.token.type == Token.PACKAGE)
        {
            packages.add(this.parsePackageDefinition());
        }
        var context = new Parser.Context();
        context.atTopLevelProgram = true;

        Vector<Ast.DirectiveNode> topIncludings = null;
        while (this.token.type == Token.IDENTIFIER && this.token.stringValue.equals("include"))
        {
            this.markLocation();
            this.next();
            Ast.IncludeDirectiveNode drtv = this.parseIncludeDirective(context, true);
            if (drtv.subpackages != null)
            {
                for (var p : drtv.subpackages)
                {
                    packages.add(p);
                }
                drtv.subpackages = null;
            }
            topIncludings = topIncludings == null ? new Vector<>() : topIncludings;
            topIncludings.add(drtv);
            if (drtv.subdirectives != null && drtv.subdirectives.size() != 0)
            {
                break;
            }
        }
        var directives = this.parseOptDirectives(context);
        if (topIncludings != null)
        {
            for (var i = topIncludings.size(); --i != -1;)
            {
                directives.add(0, topIncludings.get(i));
            }
        }
        while (this.token.type != Token.EOF)
        {
            var drtv2 = this.parseOptDirectives(context);
            if (drtv2.size() != 0)
            {
                var span = drtv2.get(0).span;
                this.reportSyntaxError(Problem.Constants.EXPECTING_BEFORE, Span.pointer(span.firstLine(), span.start()), Problem.Argument.createToken(Token.SEMICOLON), Problem.Argument.createTerm("directive"));
            }
            else
            {
                this.reportSyntaxError(Problem.Constants.UNALLOWED_HERE, this.token.getSpan(), Problem.Argument.createToken(this.token.type));
                this.next();
            }
        }
        var program = new Ast.ProgramNode(packages, directives);
        program.script = this.script;
        program.span = this.popLocation();
        return program;
    }
}