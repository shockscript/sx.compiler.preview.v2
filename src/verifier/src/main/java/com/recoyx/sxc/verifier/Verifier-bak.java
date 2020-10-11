package com.recoyx.sxc.verifier;

import com.recoyx.sxc.parser.ast;
import com.recoyx.sxc.parser.Parser;
import com.recoyx.sxc.parser.Problem;
import com.recoyx.sxc.parser.Problem.Argument;
import com.recoyx.sxc.parser.Problem.Constants;
import com.recoyx.sxc.parser.Script;
import com.recoyx.sxc.parser.Span;
import com.recoyx.sxc.semantics.*;
import com.recoyx.sxc.semantics.errors.*;
import com.recoyx.sxc.parser.NodeVisitor;
import com.recoyx.sxc.util.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;

/**
 * Verifier resolves program symbols and enforces
 * language verification rules with AST nodes.
 */
public final class Verifier
{
    public SymbolPool pool = new SymbolPool();

    public ScopeChain scopeChain;

    public Vector<Problem> problems = new Vector<>();

    public boolean allowDuplicates = false;

    private Vector<Script> _scripts = new Vector<>();

    private boolean _invalidated = false;

    private Activation currentActivation;

    private Symbol currentFunction;

    private Ast.FunctionCommonNode currentFunctionCommon;

    private Vector<Activation> _activations = new Vector<>();

    private Vector<Symbol> _functions = new Vector<>();

    private Vector<Ast.FunctionCommonNode> _functionCommons = new Vector<>();

    public Verifier()
    {
        scopeChain = new ScopeChain(pool);
        this.enterFrame(pool.topFrame);
    }

    public boolean invalidated()
    {
        return _invalidated;
    }

    private void reportVerifyError(Problem.Constants errorId, Span span, Problem.Argument... rest)
    {
        if (_scripts.size() == 0)
        {
            throw new RuntimeException("No script to associate the problem with.");
        }
        problems.add(new Problem("verifyError", errorId, span, _scripts.get(_scripts.size() - 1), rest));
        _invalidated = true;
    }

    private void reportWarning(Problem.Constants errorId, Span span, Problem.Argument... rest)
    {
        if (_scripts.size() == 0)
        {
            throw new RuntimeException("No script to associate the problem with.");
        }
        problems.add(new Problem("warning", errorId, span, _scripts.get(_scripts.size() - 1), rest));
    }

    public Symbol currentFrame()
    {
        return scopeChain.currentFrame();
    }

    public void enterScript(Script script)
    {
        _scripts.add(script);
    }

    public void exitScript()
    {
        VectorUtils.pop(_scripts);
    }

    public void enterFunction(Symbol symbol, Ast.FunctionCommonNode common)
    {
        this._activations.add(symbol.activation());
        this._functions.add(symbol);
        this._functionCommons.add(common);
        this.currentActivation = symbol.activation();
        this.currentFunction = symbol;
        this.currentFunctionCommon = common;
    }

    public void exitFunction()
    {
        exitActivation();
    }

    public void enterActivation(Activation activation)
    {
        this._activations.add(activation);
        this._functions.add(null);
        this._functionCommons.add(null);
        this.currentActivation = activation;
        this.currentFunction = null;
        this.currentFunctionCommon = null;
    }

    public void exitActivation()
    {
        VectorUtils.pop(this._activations);
        VectorUtils.pop(this._functions);
        VectorUtils.pop(this._functionCommons);
        this.currentActivation = VectorUtils.last(this._activations);
        this.currentFunction = VectorUtils.last(this._functions);
        this.currentFunctionCommon = VectorUtils.last(this._functionCommons);
    }

    public void enterFrame(Symbol frame)
    {
        scopeChain.enterFrame(frame);
    }

    public void exitFrame()
    {
        scopeChain.exitFrame();
    }

    public Symbol resolveObjectProperty(Symbol obj, Ast.Node id)
    {
        return resolveObjectProperty(obj, id, 0);
    }

    public Symbol resolveObjectProperty(Symbol obj, Ast.Node id, int flags)
    {
        Symbol qname = null;
        String str = "";
        Ast.StringLiteralNode strLiteral = null;

        if (id instanceof Ast.SimpleIdNode simpleID)
        {
            str = simpleID.name;

            if (simpleID.qualifier != null)
            {
                var qual = verifyNamespaceConstant(simpleID.qualifier);
                if (qual == null)
                {
                    return null;
                }
                qname = pool.createName(qual, str);
            }
        }
        else
        {
            strLiteral = (Ast.StringLiteralNode) id;
            str = strLiteral.value;
        }

        Symbol p = null;

        try
        {
            p = qname == null ? obj.lookupMultiName(scopeChain.openNamespaceList(), str) : obj.lookupName(qname);
        }
        catch (AmbiguousReferenceError error)
        {
            this.reportVerifyError(Problem.Constants.AMBIGUOUS_REFERENCE, id.span, Problem.Argument.createQuote(qname == null ? str : qname.toString()));
            return null;
        }

        if (p == null)
        {
            p = qname == null ? obj.lookupMultiName(null, str) : null;
            if (p != null && (flags & VerifyFlags.NON_OBJECT_REFERENCE) == 0)
            {
                this.reportVerifyError(Problem.Constants.INACCESSIBLE_PROPERTY, id.span, Problem.Argument.createQuote(qname == null ? str : qname.toString()), Problem.Argument.createSymbol(obj.staticType()));
            }
            else
            {
                if ((flags & VerifyFlags.NON_OBJECT_REFERENCE) == 0)
                {
                    this.reportVerifyError(Problem.Constants.UNDEFINED_PROPERTY_THROUGH_REFERENCE, id.span, Problem.Argument.createQuote(qname == null ? str : qname.toString()), Problem.Argument.createSymbol(obj.staticType()));
                }
                else
                {
                    this.reportVerifyError(Problem.Constants.UNDEFINED_PROPERTY, id.span, Problem.Argument.createQuote(qname == null ? str : qname.toString()));
                }
            }
            return null;
        }

        if (p.isValue() && p.staticType() == null)
        {
            this.reportVerifyError(Problem.Constants.COULD_NOT_RESOLVE_PROPERTY_HERE, id.span);
            return null;
        }

        if (p.isObjectProperty())
        {
            if (p.accessingProperty().isFunction() && (flags & VerifyFlags.OBJECT_CALL) == 0)
            {
                p.accessingProperty().setIsBoundMethod(true);
            }
        }
        else if (p.isNamespace())
        {
            p = this.pool.createNamespaceConstantValue(p);
        }
        else if (p.isType() && p.typeParams() != null && (flags & VerifyFlags.ARGUMENTING_TYPE) == 0)
        {
            var arguments = new Vector<Symbol>();
            for (var typeParam : p.typeParams())
            {
                arguments.add(pool.anyType);
            }
            p = pool.createInstantiatedType(p, arguments);
        }

        return p;
    }

    public Symbol resolveLexicalProperty(Ast.SimpleIdNode id)
    {
        return resolveLexicalProperty(id, 0, true);
    }

    public Symbol resolveLexicalProperty(Ast.SimpleIdNode id, int flags)
    {
        return resolveLexicalProperty(id, flags, true);
    }

    public Symbol resolveLexicalProperty(Ast.SimpleIdNode id, int flags, boolean reportError)
    {
        Symbol qname = null;
        String str = id.name;

        if (id.qualifier != null)
        {
            var qual = verifyNamespaceConstant(id.qualifier);
            if (qual == null)
            {
                return null;
            }
            qname = pool.createName(qual, str);
        }

        Symbol p = null;

        try
        {
            p = qname == null ? scopeChain.lookupMultiName(scopeChain.openNamespaceList(), str) : scopeChain.lookupName(qname);
            p = p == null && qname == null ? pool.findPackage(str) : p;
        }
        catch (AmbiguousReferenceError error)
        {
            if (reportError)
            {
                this.reportVerifyError(Problem.Constants.AMBIGUOUS_REFERENCE, id.span, Problem.Argument.createQuote(qname == null ? str : qname.toString()));
            }
            return null;
        }

        if (p == null)
        {
            if (reportError)
            {
                this.reportVerifyError(Problem.Constants.UNDEFINED_PROPERTY, id.span, Problem.Argument.createQuote(qname == null ? str : qname.toString()));
            }
            return null;
        }

        if (p.isValue() && p.staticType() == null)
        {
            if (reportError)
            {
                this.reportVerifyError(Problem.Constants.COULD_NOT_RESOLVE_PROPERTY_HERE, id.span);
            }
            return null;
        }

        if (p.isObjectProperty())
        {
            if (p.accessingProperty().isFunction() && (flags & VerifyFlags.OBJECT_CALL) == 0)
            {
                p.accessingProperty().setIsBoundMethod(true);
            }
            if (p.baseObject().isThis())
            {
                if (p.baseObject().activation() != currentActivation)
                {
                    p.baseObject().activation().setScopeExtendedProperty(p.baseObject());
                }
            }
            // code may capture property from an enclosing with frame, such as in:
            //
            // with (0x0a)
            // {
            //     function f():void { trace(toString(16)) }
            // }
            //
            else if (p.baseObject().isFrameProperty())
            {
                var p2 = p.baseObject();
                if (p2.baseFrame().activation() != this.currentActivation)
                {
                    p2.baseFrame().activation().setScopeExtendedProperty(p2.accessingProperty());
                }
            }
        }
        else if (p.isFrameProperty())
        {
            if (p.baseFrame().activation() != currentActivation)
            {
                p.baseFrame().activation().setScopeExtendedProperty(p.accessingProperty());
            }
        }
        else if (p.isNamespace())
        {
            p = this.pool.createNamespaceConstantValue(p);
        }
        else if (p.isType() && p.typeParams() != null && (flags & VerifyFlags.ARGUMENTING_TYPE) == 0)
        {
            p = instantiateTypeWithDefaultArguments(p, id.span);
        }

        return p;
    }

    private Symbol instantiateTypeWithDefaultArguments(Symbol type, Span span)
    { 
        var arguments = new Vector<Symbol>();
        var foundExtendBound = false;
        for (var typeParam : type.typeParams())
        {
            var r = typeParam.defaultType();
            arguments.add(r);
            checkTypeParameterBounds1(typeParam, r, span);
            if (typeParam.superClass() != null || typeParam.implementedInterfaces() != null)
            {
                foundExtendBound = true;
            }
        }
        return pool.createInstantiatedType(type, arguments);
    }

    public Symbol resolveTypeOrPackageProperty(Symbol baseSymbol, Ast.SimpleIdNode id)
    {
        return resolveTypeOrPackageProperty(baseSymbol, id, true);
    }

    public Symbol resolveTypeOrPackageProperty(Symbol baseSymbol, Ast.SimpleIdNode id, boolean reportError)
    {
        return resolveTypeOrPackageProperty(baseSymbol, id, reportError, 0);
    }

    public Symbol resolveTypeOrPackageProperty(Symbol baseSymbol, Ast.SimpleIdNode id, boolean reportError, int flags)
    {
        Symbol qname = null;
        String str = id.name;

        if (id.qualifier != null)
        {
            var qual = verifyNamespaceConstant(id.qualifier);
            if (qual == null)
            {
                return null;
            }
            qname = pool.createName(qual, str);
        }

        Symbol p = null;

        try
        {
            p = qname == null ? baseSymbol.lookupMultiName(scopeChain.openNamespaceList(), str) : baseSymbol.lookupName(qname);
            if (p == null && qname == null && baseSymbol.isPackage())
            {
                p = pool.findPackage(baseSymbol.packageID() + "." + str);
            }
        }
        catch (AmbiguousReferenceError error)
        {
            if (reportError)
            {
                this.reportVerifyError(Problem.Constants.AMBIGUOUS_REFERENCE, id.span, Problem.Argument.createQuote(qname == null ? str : qname.toString()));
            }
            return null;
        }

        if (p == null)
        {
            if (reportError)
            {
                this.reportVerifyError(Problem.Constants.UNDEFINED_PROPERTY, id.span, Problem.Argument.createQuote(qname == null ? str : qname.toString()));
            }
            return null;
        }

        if (p.isValue() && p.staticType() == null)
        {
            this.reportVerifyError(Problem.Constants.COULD_NOT_RESOLVE_PROPERTY_HERE, id.span);
            return null;
        }
        else if (p.isNamespace())
        {
            p = this.pool.createNamespaceConstantValue(p);
        }
        else if (p.isType() && p.typeParams() != null && (flags & VerifyFlags.ARGUMENTING_TYPE) == 0)
        {
            var arguments = new Vector<Symbol>();
            for (var typeParam : p.typeParams())
            {
                arguments.add(pool.anyType);
            }
            p = pool.createInstantiatedType(p, arguments);
        }

        return p;
    }

    public void organiseProblems(Vector<Ast.ProgramNode> programs)
    {
        for (var problem : this.problems)
        {
            problem.script().collectProblem(problem);
        }
        for (var program : programs)
        {
            this._organiseSingleScriptProblems(program.script);
        }
    }

    private void _organiseSingleScriptProblems(Script script)
    {
        script.problems.sort(new Comparator<>()
        {
            public int compare(Problem a, Problem b)
            {
                return a.span().compareTo(b.span());
            }

            public boolean equals(Object obj)
            {
                return ((Object)this) == obj;
            }
        });
        if (script.subscripts != null)
        {
            for (var subscript : script.subscripts)
            {
                this._organiseSingleScriptProblems(subscript);
            }
        }
    }

    public Symbol verifyConstantExpression(Ast.ExpressionNode node)
    {
        return verifyConstantExpression(node, null);
    }

    public Symbol verifyConstantExpression(Ast.ExpressionNode node, Symbol inferenceType)
    {
        return verifyConstantExpression(node, inferenceType, true);
    }

    public Symbol verifyConstantExpression(Ast.ExpressionNode node, Symbol inferenceType, boolean reportError)
    {
        var r = node.semNSResult;

        Symbol base = null;
        Symbol symbol1 = null;
        Symbol symbol2 = null;

        if (r != null)
        {
            return r.isVerifyingType() ? null : r;
        }

        if (node instanceof Ast.SimpleIdNode simpleID)
        {
            var foundDynamicID = false;
            if (simpleID.qualifier != null)
            {
                symbol1 = verifyExpression(simpleID.qualifier);
                if (symbol1 != null && !symbol1.isConstantValue())
                {
                    foundDynamicID = true;
                }
            }
            r = foundDynamicID ? null : resolveLexicalProperty(simpleID, 0, reportError);
            if (r != null)
            {
                r = validateConstantProperty(node, r, reportError);
            }
        }
        else if (node instanceof Ast.DotNode dot)
        {
            if (dot.id instanceof Ast.SimpleIdNode)
            {
                base = verifyConstantExpression(dot.base, null, reportError);
                if (base != null && (base.isType() || base.isPackage()))
                {
                    r = resolveTypeOrPackageProperty(base, (Ast.SimpleIdNode) dot.id, reportError);
                    if (r != null)
                    {
                        r = this.validateConstantProperty(node, r, reportError);
                    }
                }
                else if (base != null && reportError)
                {
                    this.reportVerifyError(Problem.Constants.NOT_A_CONSTANT_EXPRESSION, node.span);
                }
            }
            else if (reportError)
            {
                this.reportVerifyError(Problem.Constants.NOT_A_CONSTANT_EXPRESSION, node.span);
            }
        }
        else if (node instanceof Ast.BooleanLiteralNode)
        {
            r = pool.createBooleanConstantValue(((Ast.BooleanLiteralNode) node).value);
        }
        else if (node instanceof Ast.NumericLiteralNode)
        {
            r = pool.createNumberConstantValue(((Ast.NumericLiteralNode) node).value);
            if (inferenceType != null && pool.isNumericType(inferenceType.escapeType()))
            {
                r = r.constantImplicitConversion(inferenceType);
            }
        }
        else if (node instanceof Ast.StringLiteralNode)
        {
            r = verifyStringLiteral((Ast.StringLiteralNode) node, inferenceType, reportError);
        }
        else if (node instanceof Ast.NullLiteralNode)
        {
            r = pool.createNullConstantValue(inferenceType != null && inferenceType.containsNull() ? inferenceType : pool.nullType);
        }
        else if (node instanceof Ast.ReservedNamespaceNode)
        {
            r = verifyReservedNamespace((Ast.ReservedNamespaceNode) node, reportError);
        }
        else if (node instanceof Ast.UnaryOperatorNode)
        {
            r = verifyConstantUnaryOperator((Ast.UnaryOperatorNode) node, inferenceType, reportError);
        }
        else if (node instanceof Ast.BinaryOperatorNode)
        {
            r = verifyConstantBinaryOperator((Ast.BinaryOperatorNode) node, inferenceType, reportError);
        }
        else if (node instanceof Ast.ParenExpressionNode)
        {
            r = verifyConstantExpression(((Ast.ParenExpressionNode) node).expression, inferenceType, reportError);
        }
        else if (reportError)
        {
            this.reportVerifyError(Problem.Constants.NOT_A_CONSTANT_EXPRESSION, node.span);
        }

        if (r != null)
        {
            node.semNSResult = r;
            if (inferenceType != null && r.isValue())
            {
                r = r.constantImplicitConversion(inferenceType);
                if (r != null)
                {
                    node.semNSResult = r;
                }
            }
            r = node.semNSResult;
            if (!reportError)
            {
                node.semNSResult = null;
            }
            return r;
        }

        if (reportError)
        {
            node.semNSResult = pool.verifyingType;
        }

        return null;
    }

    public Symbol verifyConstantValue(Ast.ExpressionNode node)
    {
        return verifyConstantValue(node, null);
    }

    public Symbol verifyConstantValue(Ast.ExpressionNode node, Symbol inferenceType)
    {
        return verifyConstantValue(node, inferenceType, true);
    }

    public Symbol verifyConstantValue(Ast.ExpressionNode node, Symbol inferenceType, boolean reportError)
    {
        var r = verifyConstantExpression(node, inferenceType, reportError);
        if (r == null)
        {
            return null;
        }
        if (r.isConstantValue())
        {
            return r;
        }
        else if (r.isType() && reportError)
        {
            this.reportVerifyError(Problem.Constants.CANNOT_USE_TYPE_AS_VALUE, node.span);
        }
        else if (r.isPackage() && reportError)
        {
            this.reportVerifyError(Problem.Constants.CANNOT_USE_PACKAGE_AS_VALUE, node.span);
        }
        if (reportError)
        {
            node.semNSResult = pool.verifyingType;
        }
        return null;
    }

    public Symbol limitConstantType(Ast.ExpressionNode node, Symbol type)
    {
        var r = verifyConstantValue(node, type);
        if (r == null)
        {
            return null;
        }
        var conv = r.constantImplicitConversion(type);
        if (conv != null)
        {
            node.semNSResult = conv;
            return conv;
        }
        else
        {
            this.reportVerifyError(Problem.Constants.INCOMPATIBLE_TYPES, node.span, Problem.Argument.createSymbol(r.staticType()), Problem.Argument.createSymbol(type));
        }
        node.semNSResult = pool.verifyingType;
        return null;
    }

    public Symbol verifyNamespaceConstant(Ast.ExpressionNode node)
    {
        Symbol s = verifyConstantExpression(node);
        if (s == null)
        {
            return null;
        }
        if (s.isNamespaceConstantValue())
        {
            return s.namespace();
        }
        this.reportVerifyError(Problem.Constants.NOT_A_NAMESPACE_CONSTANT, node.span);
        node.semNSResult = pool.createVerifyingType();
        return null;
    }

    private Symbol validateConstantProperty(Ast.ExpressionNode node, Symbol symbol, boolean reportError)
    {
        Symbol p = null;
        if (symbol.isTypeProperty() || symbol.isPackageProperty())
        {
            p = symbol.accessingProperty();
        }
        else if (symbol.isType() || symbol.isPackage() || symbol.isNamespaceConstantValue())
        {
            return symbol;
        }
        else if (symbol.isNamespace())
        {
            return pool.createNamespaceConstantValue(symbol);
        }

        if (p != null && p.isVariableProperty() && p.readOnly() && p.initialValue() != null)
        {
            return p.initialValue();
        }
        else if (reportError)
        {
            this.reportVerifyError(Problem.Constants.NOT_A_CONSTANT_EXPRESSION, node.span);
        }
        return null;
    }

    private Symbol verifyStringLiteral(Ast.StringLiteralNode node, Symbol inferenceType)
    {
        return verifyStringLiteral(node, inferenceType, true);
    }

    private Symbol verifyStringLiteral(Ast.StringLiteralNode node, Symbol inferenceType, boolean reportUndefined)
    {
        if (inferenceType != null && inferenceType.escapeType().isEnumType())
        {
            var constant = inferenceType.escapeType().getConstant(node.value);
            if (constant == null && reportUndefined)
            {
                this.reportVerifyError(Problem.Constants.UNDEFINED_ENUMERATION_CONSTANT, node.span, Problem.Argument.createSymbol(inferenceType), Problem.Argument.createQuote(node.value));
            }
            return constant;
        }
        return pool.createStringConstantValue(node.value);
    }

    private Symbol verifyConstantUnaryOperator(Ast.UnaryOperatorNode node, Symbol inferenceType, boolean reportError)
    {
        var argument = verifyConstantExpression(node.argument, inferenceType, reportError);
        if (argument == null)
        {
            return null;
        }
        if (node.type == Operator.VOID)
        {
            return pool.createUndefinedConstantValue();
        }
        // Number operations
        if (argument.isNumberConstantValue())
        {
            if (node.type == Operator.NEGATE)
                return pool.createNumberConstantValue(-argument.numberValue(), argument.staticType());
            if (node.type == Operator.BITWISE_NOT)
                return pool.createNumberConstantValue((double) (~((long) argument.numberValue())), argument.staticType());
        }
        // int operations
        else if (argument.isIntConstantValue())
        {
            if (node.type == Operator.NEGATE)
                return pool.createIntConstantValue(-argument.intValue(), argument.staticType());
            if (node.type == Operator.BITWISE_NOT)
                return pool.createIntConstantValue(~argument.intValue(), argument.staticType());
        }
        // uint operations
        else if (argument.isUnsignedIntConstantValue())
        {
            if (node.type == Operator.BITWISE_NOT)
                return pool.createUnsignedIntConstantValue(UnsignedInteger.valueOf(~argument.uintValue().longValue()), argument.staticType());
        }

        if (reportError)
        {
            this.reportVerifyError(Problem.Constants.NOT_A_CONSTANT_EXPRESSION, node.span);
        }
        return null;
    }

    private Symbol verifyConstantBinaryOperator(Ast.BinaryOperatorNode node, Symbol inferenceType, boolean reportError)
    {
        var left = this.verifyConstantValue(node.left, inferenceType, reportError);
        if (left == null)
        {
            return null;
        }
        var right = this.verifyConstantValue(node.right, inferenceType, reportError);
        if (right == null)
        {
            return null;
        }

        if (left.isNumberConstantValue() && right.isNumberConstantValue())
        {
            if (node.type == Operator.ADD)
                return pool.createNumberConstantValue(left.numberValue() + right.numberValue(), left.staticType());
            if (node.type == Operator.SUBTRACT)
                return pool.createNumberConstantValue(left.numberValue() - right.numberValue(), left.staticType());
            if (node.type == Operator.MULTIPLY)
                return pool.createNumberConstantValue(left.numberValue() * right.numberValue(), left.staticType());
            if (node.type == Operator.DIVIDE)
                return pool.createNumberConstantValue(left.numberValue() / right.numberValue(), left.staticType());
            if (node.type == Operator.REMAINDER)
                return pool.createNumberConstantValue(left.numberValue() % right.numberValue(), left.staticType());
            if (node.type == Operator.BITWISE_AND)
                return pool.createNumberConstantValue((double) (((long) left.numberValue()) & ((long) right.numberValue())), left.staticType());
            if (node.type == Operator.BITWISE_XOR)
                return pool.createNumberConstantValue((double) (((long) left.numberValue()) ^ ((long) right.numberValue())), left.staticType());
            if (node.type == Operator.BITWISE_OR)
                return pool.createNumberConstantValue((double) (((long) left.numberValue()) | ((long) right.numberValue())), left.staticType());
            if (node.type == Operator.LEFT_SHIFT)
                return pool.createNumberConstantValue((double) (((long) left.numberValue()) << ((long) right.numberValue())), left.staticType());
            if (node.type == Operator.RIGHT_SHIFT)
                return pool.createNumberConstantValue((double) (((long) left.numberValue()) >> ((long) right.numberValue())), left.staticType());
            // boring to implement in Java:
            // if (node.type == Operator.UNSIGNED_RIGHT_SHIFT)
            if (node.type == Operator.EQUALS)
                return pool.createBooleanConstantValue(left.numberValue() == right.numberValue());
            if (node.type == Operator.NOT_EQUALS)
                return pool.createBooleanConstantValue(left.numberValue() != right.numberValue());
            if (node.type == Operator.LT)
                return pool.createBooleanConstantValue(left.numberValue() < right.numberValue());
            if (node.type == Operator.GT)
                return pool.createBooleanConstantValue(left.numberValue() > right.numberValue());
            if (node.type == Operator.LE)
                return pool.createBooleanConstantValue(left.numberValue() <= right.numberValue());
            if (node.type == Operator.GE)
                return pool.createBooleanConstantValue(left.numberValue() >= right.numberValue());
        }
        else if (left.isIntConstantValue() && right.isIntConstantValue())
        {
            if (node.type == Operator.ADD)
                return pool.createIntConstantValue(left.intValue() + right.intValue(), left.staticType());
            if (node.type == Operator.SUBTRACT)
                return pool.createIntConstantValue(left.intValue() - right.intValue(), left.staticType());
            if (node.type == Operator.MULTIPLY)
                return pool.createIntConstantValue(left.intValue() * right.intValue(), left.staticType());
            if (node.type == Operator.DIVIDE)
                return pool.createIntConstantValue(left.intValue() / right.intValue(), left.staticType());
            if (node.type == Operator.REMAINDER)
                return pool.createIntConstantValue(left.intValue() % right.intValue(), left.staticType());
            if (node.type == Operator.BITWISE_AND)
                return pool.createIntConstantValue(left.intValue() & right.intValue(), left.staticType());
            if (node.type == Operator.BITWISE_XOR)
                return pool.createIntConstantValue(left.intValue() | right.intValue(), left.staticType());
            if (node.type == Operator.BITWISE_OR)
                return pool.createIntConstantValue(left.intValue() ^ right.intValue(), left.staticType());
            if (node.type == Operator.LEFT_SHIFT)
                return pool.createIntConstantValue(left.intValue() << right.intValue(), left.staticType());
            if (node.type == Operator.RIGHT_SHIFT)
                return pool.createIntConstantValue(left.intValue() >> right.intValue(), left.staticType());
            // boring to implement in Java
            // if (node.type == Operator.UNSIGNED_RIGHT_SHIFT)
            //    return pool.createIntConstantValue(left.intValue() >> right.intValue(), left.staticType());
            if (node.type == Operator.EQUALS)
                return pool.createBooleanConstantValue(left.intValue() == right.intValue());
            if (node.type == Operator.NOT_EQUALS)
                return pool.createBooleanConstantValue(left.intValue() != right.intValue());
            if (node.type == Operator.LT)
                return pool.createBooleanConstantValue(left.intValue() < right.intValue());
            if (node.type == Operator.GT)
                return pool.createBooleanConstantValue(left.intValue() > right.intValue());
            if (node.type == Operator.LE)
                return pool.createBooleanConstantValue(left.intValue() <= right.intValue());
            if (node.type == Operator.GE)
                return pool.createBooleanConstantValue(left.intValue() >= right.intValue());
        }
        else if (left.isUnsignedIntConstantValue() && right.isUnsignedIntConstantValue())
        {
            if (node.type == Operator.ADD)
                return pool.createUnsignedIntConstantValue(left.uintValue().plus(right.uintValue()), left.staticType());
            if (node.type == Operator.SUBTRACT)
                return pool.createUnsignedIntConstantValue(left.uintValue().minus(right.uintValue()), left.staticType());
            if (node.type == Operator.MULTIPLY)
                return pool.createUnsignedIntConstantValue(left.uintValue().times(right.uintValue()), left.staticType());
            if (node.type == Operator.DIVIDE)
                return pool.createUnsignedIntConstantValue(left.uintValue().dividedBy(right.uintValue()), left.staticType());
            if (node.type == Operator.REMAINDER)
                return pool.createUnsignedIntConstantValue(left.uintValue().mod(right.uintValue()), left.staticType());
            if (node.type == Operator.BITWISE_AND)
                return pool.createUnsignedIntConstantValue(UnsignedInteger.valueOf(left.uintValue().longValue() & right.uintValue().longValue()), left.staticType());
            if (node.type == Operator.BITWISE_XOR)
                return pool.createUnsignedIntConstantValue(UnsignedInteger.valueOf(left.uintValue().longValue() | right.uintValue().longValue()), left.staticType());
            if (node.type == Operator.BITWISE_OR)
                return pool.createUnsignedIntConstantValue(UnsignedInteger.valueOf(left.uintValue().longValue() ^ right.uintValue().longValue()), left.staticType());
            if (node.type == Operator.LEFT_SHIFT)
                return pool.createUnsignedIntConstantValue(UnsignedInteger.valueOf(left.uintValue().longValue() << right.uintValue().longValue()), left.staticType());
            if (node.type == Operator.RIGHT_SHIFT)
                return pool.createUnsignedIntConstantValue(UnsignedInteger.valueOf(left.uintValue().longValue() >> right.uintValue().longValue()), left.staticType());
            // boring to implement in Java:
            // if (node.type == Operator.UNSIGNED_RIGHT_SHIFT)
            if (node.type == Operator.EQUALS)
                return pool.createBooleanConstantValue(left.uintValue().equals(right.uintValue()));
            if (node.type == Operator.NOT_EQUALS)
                return pool.createBooleanConstantValue(!left.uintValue().equals(right.uintValue()));
            if (node.type == Operator.LT)
                return pool.createBooleanConstantValue(left.uintValue().compareTo(right.uintValue()) < 0);
            if (node.type == Operator.GT)
                return pool.createBooleanConstantValue(left.uintValue().compareTo(right.uintValue()) > 0);
            if (node.type == Operator.LE)
                return pool.createBooleanConstantValue(left.uintValue().compareTo(right.uintValue()) <= 0);
            if (node.type == Operator.GE)
                return pool.createBooleanConstantValue(left.uintValue().compareTo(right.uintValue()) >= 0);
        }
        else if (left.isBigIntConstantValue() && right.isBigIntConstantValue())
        {
            if (node.type == Operator.ADD)
                return pool.createBigIntConstantValue(left.bigIntValue().add(right.bigIntValue()), left.staticType());
            if (node.type == Operator.SUBTRACT)
                return pool.createBigIntConstantValue(left.bigIntValue().subtract(right.bigIntValue()), left.staticType());
            if (node.type == Operator.MULTIPLY)
                return pool.createBigIntConstantValue(left.bigIntValue().multiply(right.bigIntValue()), left.staticType());
            if (node.type == Operator.DIVIDE)
                return pool.createBigIntConstantValue(left.bigIntValue().divide(right.bigIntValue()), left.staticType());
            if (node.type == Operator.REMAINDER)
                return pool.createBigIntConstantValue(left.bigIntValue().mod(right.bigIntValue()), left.staticType());
            if (node.type == Operator.EQUALS)
                return pool.createBooleanConstantValue(left.bigIntValue().equals(right.bigIntValue()));
            if (node.type == Operator.NOT_EQUALS)
                return pool.createBooleanConstantValue(!left.bigIntValue().equals(right.bigIntValue()));
            if (node.type == Operator.LT)
                return pool.createBooleanConstantValue(left.bigIntValue().compareTo(right.bigIntValue()) < 0);
            if (node.type == Operator.GT)
                return pool.createBooleanConstantValue(left.bigIntValue().compareTo(right.bigIntValue()) > 0);
            if (node.type == Operator.LE)
                return pool.createBooleanConstantValue(left.bigIntValue().compareTo(right.bigIntValue()) <= 0);
            if (node.type == Operator.GE)
                return pool.createBooleanConstantValue(left.bigIntValue().compareTo(right.bigIntValue()) >= 0);
        }
        else if (left.isBooleanConstantValue() && right.isBooleanConstantValue())
        {
            if (node.type == Operator.LOGICAL_AND)
                return pool.createBooleanConstantValue(left.booleanValue() && right.booleanValue(), left.staticType());
            if (node.type == Operator.LOGICAL_OR)
                return pool.createBooleanConstantValue(left.booleanValue() || right.booleanValue(), left.staticType());
        }
        else if (left.isStringConstantValue() && right.isStringConstantValue())
        {
            if (node.type == Operator.ADD)
                return pool.createStringConstantValue(left.stringValue() + right.stringValue(), left.staticType());
        }
        else if (left.isEnumConstantValue() && right.isEnumConstantValue())
        {
            if (node.type == Operator.ADD)
                return left.enumOr(right);
            if (node.type == Operator.SUBTRACT)
                return left.enumXor(right);
        }

        if (reportError)
        {
            this.reportVerifyError(Problem.Constants.NOT_A_CONSTANT_EXPRESSION, node.span);
        }
        return null;
    }

    private Symbol verifyReservedNamespace(Ast.ReservedNamespaceNode node)
    {
        return verifyReservedNamespace(node, true);
    }

    private Symbol verifyReservedNamespace(Ast.ReservedNamespaceNode node, boolean reportUndefined)
    {
        var s =   node.type.equals("public") ? this.scopeChain.currentFrame().searchPublicNamespace()
                : node.type.equals("private") ? this.scopeChain.currentFrame().searchPrivateNamespace()
                : node.type.equals("protected") ? this.scopeChain.currentFrame().searchProtectedNamespace() : this.scopeChain.currentFrame().searchInternalNamespace();
        if (s == null)
        {
            if (reportUndefined)
            {
                this.reportVerifyError(Problem.Constants.RESERVED_NAMESPACE_NOT_FOUND, node.span, Problem.Argument.createQuote(node.type));
            }
            return null;
        }
        return this.pool.createNamespaceConstantValue(s);
    }

    private final class DirectiveVerifier
    {
        private int phase = 0;

        public boolean hasRemaining()
        {
            return phase < 10;
        }

        public void incrementPhase()
        {
            ++phase;
        }

        public void verify(Vector<Ast.DirectiveNode> directives)
        {
            switch (phase)
            {
                case 0:
                    phase1VerifyDirectives(directives);
                    break;
                case 1:
                    phase2VerifyDirectives(directives);
                    break;
                case 2:
                {
                    for (var drtv : directives)
                    {
                        if (drtv instanceof Ast.TypeDefinitionNode)
                        {
                            phase3VerifyTypeDefinition((Ast.TypeDefinitionNode) drtv);
                        }
                    }
                    break;
                }
                case 3:
                {
                    break;
                }
                case 4:
                    phase3VerifyDirectives(directives);
                    break;
                case 5:
                    phase32VerifyInterfaceDefinitions(directives);
                    break;
                case 6:
                    phase4VerifyDirectives(directives);
                case 7:
                    phase4DeriveInterfaceOperators(directives);
                    break;
                case 9:
                    phase5VerifyDirectives(directives);
                    break;
            }
        }
    }

    private void verifyDirectives(Vector<Ast.DirectiveNode> directives)
    {
        var dv = new DirectiveVerifier();
        while (dv.hasRemaining())
        {
            dv.verify(directives);
            dv.incrementPhase();
        }
    }

    private void phase4DeriveInterfaceOperators(Vector<Ast.DirectiveNode> directives)
    {
        for (var drtv : directives)
        {
            if (drtv instanceof Ast.IncludeDirectiveNode)
            {
                var include_drtv = (Ast.IncludeDirectiveNode) drtv;
                if (include_drtv.subdirectives != null)
                {
                    phase4DeriveInterfaceOperators(include_drtv.subdirectives);
                }
            }
            else if (drtv instanceof Ast.InterfaceDefinitionNode)
            {
                var itrfc_defn = (Ast.InterfaceDefinitionNode) drtv;
                var itrfc = itrfc_defn.semNSSymbol;
                if (itrfc != null && itrfc.superInterfaces() != null)
                {
                    for (var super_itrfc : itrfc.superInterfaces())
                    {
                        if (super_itrfc.delegate().ownOperators() != null)
                        {
                            itrfc.delegate().initOwnOperators();
                            super_itrfc.delegate().ownOperators().forEach((Operator k, Symbol v) ->
                            {
                                itrfc.delegate().ownOperators().put(k, v);
                            });
                        }
                    }
                }
            }
        }
    }

    private void phase1VerifyDirectives(Vector<Ast.DirectiveNode> directives)
    {
        for (var drtv : directives)
        {
            if (drtv instanceof Ast.StatementNode)
            {
            }
            else if (drtv instanceof Ast.NamespaceDefinitionNode)
            {
                this.phase1VerifyNamespaceDefinition((Ast.NamespaceDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.ImportDirectiveNode)
            {
                this.phase1VerifyImportDirective((Ast.ImportDirectiveNode) drtv);
            }
            else if (drtv instanceof Ast.UseDirectiveNode)
            {
                this.phase1VerifyUseDirective((Ast.UseDirectiveNode) drtv);
            }
            else if (drtv instanceof Ast.UseDefaultDirectiveNode)
            {
                this.phase1VerifyUseDefaultDirective((Ast.UseDefaultDirectiveNode) drtv);
            }
            else if (drtv instanceof Ast.IncludeDirectiveNode)
            {
                this.enterScript(((Ast.IncludeDirectiveNode) drtv).subscript);
                this.phase1VerifyDirectives(((Ast.IncludeDirectiveNode) drtv).subdirectives);
                this.exitScript();
            }
        }
    }

    private void phase2VerifyDirectives(Vector<Ast.DirectiveNode> directives)
    {
        for (var drtv : directives)
        {
            if (drtv instanceof Ast.StatementNode)
            {
            }
            else if (drtv instanceof Ast.VarDefinitionNode)
            {
                this.phase2VerifyVarDefinition((Ast.VarDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.FunctionDefinitionNode)
            {
                this.phase2VerifyFunctionDefinition((Ast.FunctionDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.ClassDefinitionNode)
            {
                this.phase2VerifyClassDefinition((Ast.ClassDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.InterfaceDefinitionNode)
            {
                this.phase2VerifyInterfaceDefinition((Ast.InterfaceDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.EnumDefinitionNode)
            {
                this.phase2VerifyEnumDefinition((Ast.EnumDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.IncludeDirectiveNode)
            {
                this.enterScript(((Ast.IncludeDirectiveNode) drtv).subscript);
                this.phase2VerifyDirectives(((Ast.IncludeDirectiveNode) drtv).subdirectives);
                this.exitScript();
            }
        }
    }

    private void phase3VerifyDirectives(Vector<Ast.DirectiveNode> directives)
    {
        for (var drtv : directives)
        {
            if (drtv instanceof Ast.StatementNode)
            {
            }
            else if (drtv instanceof Ast.ImportDirectiveNode)
            {
                this.phase3VerifyImportDirective((Ast.ImportDirectiveNode) drtv);
            }
            else if (drtv instanceof Ast.FunctionDefinitionNode)
            {
                this.phase3VerifyFunctionDefinition((Ast.FunctionDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.VarDefinitionNode)
            {
                this.phase3VerifyVarDefinition((Ast.VarDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.ClassDefinitionNode)
            {
                this.phase3VerifyClassDefinition((Ast.ClassDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.InterfaceDefinitionNode)
            {
                this.phase3VerifyInterfaceDefinition((Ast.InterfaceDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.EnumDefinitionNode)
            {
                this.phase3VerifyEnumDefinition((Ast.EnumDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.NamespaceDefinitionNode)
            {
                this.phase3VerifyNamespaceDefinition((Ast.NamespaceDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.TypeDefinitionNode)
            {
                this.phase3VerifyTypeDefinition((Ast.TypeDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.IncludeDirectiveNode)
            {
                this.enterScript(((Ast.IncludeDirectiveNode) drtv).subscript);
                this.phase3VerifyDirectives(((Ast.IncludeDirectiveNode) drtv).subdirectives);
                this.exitScript();
            }
        }
    }

    private void phase4VerifyDirectives(Vector<Ast.DirectiveNode> directives)
    {
        for (var drtv : directives)
        {
            if (drtv instanceof Ast.StatementNode)
            {
            }
            else if (drtv instanceof Ast.FunctionDefinitionNode)
            {
                this.phase4VerifyFunctionDefinition((Ast.FunctionDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.VarDefinitionNode)
            {
                this.phase4VerifyVarDefinition((Ast.VarDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.ClassDefinitionNode)
            {
                this.phase4VerifyClassDefinition((Ast.ClassDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.InterfaceDefinitionNode)
            {
                this.phase4VerifyInterfaceDefinition((Ast.InterfaceDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.EnumDefinitionNode)
            {
                this.phase4VerifyEnumDefinition((Ast.EnumDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.IncludeDirectiveNode)
            {
                this.enterScript(((Ast.IncludeDirectiveNode) drtv).subscript);
                this.phase4VerifyDirectives(((Ast.IncludeDirectiveNode) drtv).subdirectives);
                this.exitScript();
            }
        }
    }

    private void phase5VerifyDirectives(Vector<Ast.DirectiveNode> directives)
    {
        for (var drtv : directives)
        {
            if (drtv instanceof Ast.StatementNode)
            {
                this.verifyStatement((Ast.StatementNode) drtv);
            }
            else if (drtv instanceof Ast.FunctionDefinitionNode)
            {
                this.phase5VerifyFunctionDefinition((Ast.FunctionDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.ClassDefinitionNode)
            {
                this.phase5VerifyClassDefinition((Ast.ClassDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.InterfaceDefinitionNode)
            {
                this.phase5VerifyInterfaceDefinition((Ast.InterfaceDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.EnumDefinitionNode)
            {
                this.phase5VerifyEnumDefinition((Ast.EnumDefinitionNode) drtv);
            }
            else if (drtv instanceof Ast.IncludeDirectiveNode)
            {
                this.enterScript(((Ast.IncludeDirectiveNode) drtv).subscript);
                this.phase5VerifyDirectives(((Ast.IncludeDirectiveNode) drtv).subdirectives);
                this.exitScript();
            }
        }
    }

    private Symbol resolveDefinitionQualifier(Ast.DefinitionNode node)
    {
        if (node.accessModifier == null)
        {
            return this.currentFrame().defaultNamespace();
        }
        var q = this.verifyNamespaceConstant(node.accessModifier);
        if (q == null)
        {
            return null;
        }
        if (currentFrame().isBlockFrame())
        {
            if (!q.isInternalNamespace())
            {
                this.reportVerifyError(Problem.Constants.ACCESS_MODIFIER_NOT_ALLOWED_HERE, node.accessModifier.span);
            }
        }
        return q;
    }

    private Names getDefinitionNameCollection(Ast.DefinitionNode node)
    {
        if (node.markStatic())
        {
            return this.currentFrame().symbol().ownNames();
        }
        if (this.currentFrame().symbol() != null)
        {
            if (this.currentFrame().symbol().isType())
            {
                return this.currentFrame().symbol().delegate().ownNames();
            }
            if (this.currentFrame().symbol().isPackage())
            {
                return this.currentFrame().symbol().ownNames();
            }
        }
        return this.currentFrame().ownNames();
    }

    private void phase1VerifyNamespaceDefinition(Ast.NamespaceDefinitionNode node)
    {
        var qual = this.resolveDefinitionQualifier(node);
        if (qual == null)
        {
            return;
        }
        Symbol assignedNS = null;

        if (node.expression != null)
        {
            Ast.StringLiteralNode str_literal = null;
            if (node.expression instanceof Ast.StringLiteralNode)
            {
                str_literal = (Ast.StringLiteralNode) node.expression;
                var p = str_literal.value.indexOf(":") == -1 ? this.pool.createPackage(str_literal.value) : null;
                assignedNS = p != null ? p.publicNamespace() : this.pool.createExplicitNamespace(node.name, str_literal.value);
            }
            else
            {
                assignedNS = this.verifyNamespaceConstant(node.expression);
            }
        }
        else
        {
            assignedNS = this.pool.createExplicitNamespace(node.name, "");
        }

        node.semNSSymbol = assignedNS;

        if (assignedNS == null)
        {
            return;
        }

        // if this is a package alias, then define name
        // into the frame's own names, not in any current type traits,
        // as long as this is not marked static

        // additionaly, if this is a package alias without access modifier,
        // it will use the internal namespace

        var name = this.pool.createName(assignedNS.definitionPackage() != null && node.accessModifier == null ? this.currentFrame().searchInternalNamespace() : qual, node.name);
        var intoNames = assignedNS.definitionPackage() != null && !node.markStatic() ? this.currentFrame().ownNames() : this.getDefinitionNameCollection(node);
        var k = intoNames.lookupName(name);
        if (k != null)
        {
            if (k.isNamespace() && this.allowDuplicates)
            {
                node.semNSSymbol = k;
            }
            else
            {
                this.reportVerifyError(Problem.Constants.NAMESPACE_CONFLICT, node.nameSpan, Problem.Argument.createQuote(qual.toString()));
            }
        }
        else
        {
            if (assignedNS.definitionPackage() != null)
            {
                this.currentFrame().importPackage(assignedNS.definitionPackage(), false);
            }
            intoNames.defineName(name, assignedNS);
        }
    }

    private void phase3VerifyNamespaceDefinition(Ast.NamespaceDefinitionNode node)
    {
        var ns = node.semNSSymbol;
        if (ns == null || ns.definitionPackage() == null)
        {
            return;
        }
        var p = ns.definitionPackage();
        if (p.ownNames().length() == 0)
        {
            reportVerifyError(Problem.Constants.EMPTY_PACKAGE, node.expression.span, Problem.Argument.createQuote(p.packageID() + ".*"));
        }
    }

    private void phase1VerifyImportDirective(Ast.ImportDirectiveNode node)
    {
        Symbol p = null;
        if (node.wildcard)
        {
            p = this.pool.createPackage(node.importName);
            boolean aliasSpecified = node.alias != "" && node.alias != null;
            this.currentFrame().importPackage(p, !aliasSpecified);
            if (!aliasSpecified)
            {
                this.scopeChain.openNamespaceList().add(p.publicNamespace());
            }
            if (aliasSpecified)
            {
                var internalNS = this.currentFrame().searchInternalNamespace();
                var name = this.pool.createName(internalNS, node.alias);
                if (!this.currentFrame().ownNames().hasName(name))
                {
                    this.currentFrame().ownNames().defineName(name, p.publicNamespace());
                }
            }
            node.semNSExtracted = p;
        }
        else
        {
            var importPath = VectorUtils.fromArray(node.importName.split("\\."));
            p = this.pool.createPackage(VectorUtils.join(VectorUtils.slice(importPath, 0, importPath.size() - 1), "."));
            var itemName = importPath.lastElement();

            try
            {
                var property = p.lookupName(this.pool.createName(p.publicNamespace(), itemName));
                if (property != null)
                {
                    var name2 = this.pool.createName(this.currentFrame().searchInternalNamespace(), (node.alias != "" && node.alias != null) ? node.alias : itemName);
                    if (!this.currentFrame().ownNames().hasName(name2))
                    {
                        this.currentFrame().ownNames().defineName(name2, property);
                    }
                }
            }
            catch (AmbiguousReferenceError exc)
            {
                this.reportVerifyError(Problem.Constants.AMBIGUOUS_REFERENCE, node.importNameSpan, Problem.Argument.createQuote(itemName));
            }
        }
    }

    private void phase3VerifyImportDirective(Ast.ImportDirectiveNode node)
    {
        Symbol p = null;
        if (node.wildcard)
        {
            p = node.semNSExtracted;
            if (p.ownNames().length() == 0)
            {
                this.reportWarning(Problem.Constants.EMPTY_PACKAGE, node.importNameSpan, Problem.Argument.createQuote(p.packageID() + ".*"));
            }
        }
        else
        {
            var importPath = VectorUtils.fromArray(node.importName.split("\\."));
            p = this.pool.createPackage(VectorUtils.join(VectorUtils.slice(importPath, 0, importPath.size() - 1), "."));
            var itemName = importPath.lastElement();

            try
            {
                var property = p.lookupName(this.pool.createName(p.publicNamespace(), itemName));
                if (property != null)
                {
                    var name = this.pool.createName(this.currentFrame().searchInternalNamespace(), (node.alias != "" && node.alias != null) ? node.alias : itemName);
                    if (!this.currentFrame().ownNames().hasName(name))
                    {
                        this.currentFrame().ownNames().defineName(name, property);
                    }
                }
                else
                {
                    this.reportVerifyError(Problem.Constants.EMPTY_PACKAGE, node.importNameSpan, Problem.Argument.createQuote(p.packageID()+"."+itemName));
                }
            }
            catch (AmbiguousReferenceError exc)
            {
                this.reportVerifyError(Problem.Constants.AMBIGUOUS_REFERENCE, node.importNameSpan, Problem.Argument.createQuote(itemName));
            }
        }
    }

    private void phase1VerifyUseDirective(Ast.UseDirectiveNode node)
    {
        Symbol q = null;
        if (node.expression instanceof Ast.ListExpressionNode)
        {
            for (var subExpr : ((Ast.ListExpressionNode) node.expression).expressions)
            {
                q = verifyNamespaceConstant(subExpr);
                if (q != null)
                {
                    currentFrame().openNamespace(q);
                    scopeChain.openNamespaceList().add(q);
                }
            }
        }
        else
        {
            q = verifyNamespaceConstant(node.expression);
            if (q != null)
            {
                currentFrame().openNamespace(q);
                scopeChain.openNamespaceList().add(q);
            }
        }
    }

    private void phase1VerifyUseDefaultDirective(Ast.UseDefaultDirectiveNode node)
    {
        var q = this.verifyNamespaceConstant(node.expression);
        if (q != null)
        {
            if (currentFrame().isBlockFrame())
            {
                if (!q.isInternalNamespace())
                {
                    this.reportVerifyError(Problem.Constants.ACCESS_MODIFIER_NOT_ALLOWED_HERE, node.expression.span);
                }
            }
            this.currentFrame().setDefaultNamespace(q);
        }
    }

    private void phase3VerifyTypeDefinition(Ast.TypeDefinitionNode node)
    {
        if (node.semNSSymbol != null)
        {
            return;
        }
        var q = this.resolveDefinitionQualifier(node);
        if (q == null)
        {
            return;
        }
        var name = this.pool.createName(q, node.name);
        var type = this.verifyTypeAnnotation(node.type);
        var intoNames = this.getDefinitionNameCollection(node);
        if (intoNames.hasName(name))
        {
            this.reportVerifyError(Problem.Constants.NAMESPACE_CONFLICT, node.nameSpan, Problem.Argument.createQuote(q.toString()));
        }
        else
        {
            intoNames.defineName(name, type);
        }
        node.semNSSymbol = type;
    }

    private void phase2VerifyVarDefinition(Ast.VarDefinitionNode node)
    {
        if (!node.semNSValidated)
        {
            return;
        }
        var q = this.resolveDefinitionQualifier(node);
        if (q == null)
        {
            node.semNSValidated = false;
            return;
        }
        var intoNames = this.getDefinitionNameCollection(node);
        for (var binding : node.bindings)
        {
            this.phase2DeclarePattern(binding.pattern, node.readOnly, q, intoNames);
        }
    }

    private void phase3VerifyVarDefinition(Ast.VarDefinitionNode node)
    {
        if (!node.semNSValidated)
        {
            return;
        }
        for (var binding : node.bindings)
        {
            if (binding.pattern.type == null)
            {
                continue;
            }
            var type = this.verifyTypeAnnotation(binding.pattern.type);
            var variable = binding.pattern.semNSVariable;
            if (type != null)
            {
                variable.setStaticType(type);
            }
        }
    }

    private void phase4VerifyVarDefinition(Ast.VarDefinitionNode node)
    {
        if (!node.semNSValidated)
        {
            return;
        }
        if (!node.readOnly && this.currentFrame().isClassFrame() && this.currentFrame().symbol().classCfgPrimitive())
        {
            this.reportVerifyError(Problem.Constants.PRIMITIVE_CLASS_VARIABLE_MUST_BE_READ_ONLY, node.span);
        }
        for (var binding : node.bindings)
        {
            this.phase4VerifyVarBinding(binding);
        }
    }

    private void phase4VerifyVarBinding(Ast.VarBindingNode binding)
    {
        Symbol varType = null;
        if (binding.pattern.type != null)
        {
            varType = this.verifyTypeAnnotation(binding.pattern.type);
        }

        if (binding.initialiser != null && (binding.pattern.type != null ? !binding.pattern.type.semNSResult.isVerifyingType() : true))
        {
            var iv = this.verifyValue(binding.initialiser, varType);
            if (iv != null && iv.isConstantValue())
            {
                binding.pattern.semNSVariable.setInitialValue(iv);
            }
            varType = varType != null ? varType : (iv != null ? iv.staticType() : null);
            if (iv != null)
            {
                this.limitType(binding.initialiser, varType);
            }
        }
        else if (varType == null)
        {
            // untyped binding
            if (binding.pattern.type != null ? !binding.pattern.type.semNSResult.isVerifyingType() : true)
            {
                this.reportVerifyError(Problem.Constants.UNTYPED_VARIABLE, binding.span);
            }
            varType = this.pool.createVerifyingType();
        }

        this.phase4DeclarePattern(binding.pattern, this.pool.createValue(varType == null ? pool.verifyingType : varType));
    }

    private void phase2VerifyEnumDefinition(Ast.EnumDefinitionNode node)
    {
        var qual = this.resolveDefinitionQualifier(node);
        if (qual == null)
        {
            return;
        }
        var name = this.pool.createName(qual, node.name);
        var intoNames = this.getDefinitionNameCollection(node);
        var k = intoNames.lookupName(name);
        Symbol type = null;

        if (k != null)
        {
            // duplicate enum
            if (allowDuplicates)
            {
                type = k;
            }
            else
            {
                this.reportVerifyError(Problem.Constants.NAMESPACE_CONFLICT, node.nameSpan, Problem.Argument.createQuote(qual.toString()));
            }
        }
        else
        {
        	var enumMetadata = node.getMetaData("Enum");
        	node.deleteMetaData(enumMetadata);
            var flags = enumMetadata.findEntry("flags") != null && enumMetadata.findEntry("flags").value == true;
            type = this.pool.createEnumType(name, null, flags, scopeChain.currentFrame().searchPublicNamespace() != null ? scopeChain.currentFrame().searchPublicNamespace() : scopeChain.currentFrame().searchInternalNamespace());
            type.setDefinitionPackage(this.currentFrame().isPackageFrame() ? this.currentFrame().symbol() : null);
            intoNames.defineName(name, type);
            node.semNSSymbol = type;
        }

        node.semNSSymbol = type;
        node.deleteMetaData("Enum");

        if (type != null)
        {
            node.block.semNSFrame = this.pool.createEnumFrame(type);
            node.block.semNSFrame.setDefaultNamespace(this.currentFrame().searchPublicNamespace());
            node.block.semNSFrame.setDefaultNamespace(node.block.semNSFrame.defaultNamespace() == null ? this.currentFrame().searchInternalNamespace() : node.block.semNSFrame.defaultNamespace());
            node.block.semNSFrame.setActivation(this.currentActivation);
        }
    }

    private void phase3VerifyEnumDefinition(Ast.EnumDefinitionNode node)
    {
        var type = node.semNSSymbol;
        if (type == null)
        {
            return;
        }
        var ut = type.isFlagEnum() ? this.pool.uintType : pool.numberType;
        if (node.type != null)
        {
            // numeric type of flagging enum is restricted to {NativeInt, uint, byte, long}
            ut = this.verifyTypeAnnotation(node.type);
            if (ut != null && (type.isFlagEnum() ? !this.pool.isIntegerType(ut) : !pool.isNumericType(ut)))
            {
                this.reportVerifyError(Problem.Constants.UNSUPPORTED_ENUMERATION_TYPE, node.type.span);
                ut = null;
            }
            ut = ut == null ? (type.isFlagEnum() ? this.pool.uintType : pool.numberType) : ut;
        }

        type.setNumericType(ut);

        var publicNS = scopeChain.currentFrame().searchPublicNamespace();
        publicNS = publicNS == null ? scopeChain.currentFrame().searchInternalNamespace() : publicNS;

        var fValueOf = pool.createFunction(pool.createName(publicNS, "valueOf"), pool.createFunctionType((Vector<Symbol>) null, null, null, type.numericType()));
        fValueOf.setMarkNative(true);
        type.delegate().ownNames().defineName(fValueOf.name(), fValueOf);

        this.enterFrame(node.block.semNSFrame);

        // process variables
        for (var drtv : node.block.directives)
        {
            if (!(drtv instanceof Ast.VarDefinitionNode))
            {
                continue;
            }
            ((Ast.VarDefinitionNode) drtv).semNSValidated = false;
            var qual = this.resolveDefinitionQualifier((Ast.DefinitionNode) drtv);
            if (qual != null)
            {
                Object counter = type.isFlagEnum() ? NumericUtils.one(ut.javaClass()) : NumericUtils.zero(ut.javaClass());
                for (var binding : ((Ast.VarDefinitionNode) drtv).bindings)
                {
                    counter = this.defineConstant(binding, type, qual, counter);
                }
            }
        }

        this.phase1VerifyDirectives(node.block.directives);
        this.phase2VerifyDirectives(node.block.directives);
        this.phase3VerifyDirectives(node.block.directives);

        this.exitFrame();
    }

    private Object defineConstant(Ast.VarBindingNode binding, Symbol enumType, Symbol qual, Object counter)
    {
        var propertyName = this.pool.createName(qual, ((Ast.NamePatternNode) binding.pattern).name);
        var variable = this.pool.createVariableProperty(propertyName, true, enumType);
        Symbol numSymbol = null;
        String id = "";

        // either verify initialiser or increment counter
        if (binding.initialiser != null)
        {
            var array_literal = binding.initialiser instanceof Ast.ArrayLiteralNode ? (Ast.ArrayLiteralNode) binding.initialiser : null;
            Ast.StringLiteralNode str_literal = null;
            if (array_literal != null && array_literal.rest == null && array_literal.elements.size() == 2)
            {
                int j = -1;
                if (array_literal.elements.get(0) instanceof Ast.StringLiteralNode)
                {
                    str_literal = (Ast.StringLiteralNode) array_literal.elements.get(0);
                    id = str_literal.value;
                    j = 1;
                }
                else if (array_literal.elements.get(1) instanceof Ast.StringLiteralNode)
                {
                    str_literal = (Ast.StringLiteralNode) array_literal.elements.get(1);
                    id = str_literal.value;
                    j = 0;
                }
                if (j == -1)
                {
                    numSymbol = this.limitConstantType(binding.initialiser, enumType.numericType());
                }
                else
                {
                    numSymbol = this.limitConstantType(array_literal.elements.get(j), enumType.numericType());
                }
            }
            else if ((str_literal = (binding.initialiser instanceof Ast.StringLiteralNode ? (Ast.StringLiteralNode) binding.initialiser : null)) != null)
            {
                id = str_literal.value;
            }
            else
            {
                numSymbol = this.limitConstantType(binding.initialiser, enumType.numericType());
            }
        }

        var value = counter = numSymbol != null ? numSymbol.boxedNumberValue() : counter;
        var constantValue = this.pool.createEnumConstantValue(value, enumType);
        counter = enumType.isFlagEnum() ? NumericUtils.per2(counter) : NumericUtils.increment(counter);

        variable.setInitialValue(constantValue);

        // set constant name
        id = id.equals("") ? this.generateConstantID(propertyName.localName(), qual) : id;
        binding.semNSConstantID = id;
        enumType.setConstant(id, constantValue);

        if (enumType.ownNames().hasName(propertyName))
        {
            this.reportVerifyError(Problem.Constants.NAMESPACE_CONFLICT, binding.pattern.span, Problem.Argument.createQuote(qual.toString()));
        }
        else
        {
            enumType.ownNames().defineName(propertyName, variable);
        }

        return counter;
    }

    private String generateConstantID(String propertyUnqualifiedName, Symbol qual)
    {
        var id = "";
        var parts = VectorUtils.fromArray(propertyUnqualifiedName.split("\\_"));
        var builder = new StringBuilder();
        builder.append(VectorUtils.shift(parts).toLowerCase());
        for (var str : parts)
        {
            if (str.equals(""))
            {
                continue;
            }
            builder.append(new String(new int[] {str.codePointAt(0)}, 0, 1).toUpperCase());
            if (str.length() > 1)
            {
                var cp = str.codePointAt(0);
                builder.append(str.substring((cp >> 16) != 0 ? 2 : 1).toLowerCase());
            }
        }
        if (qual.isExplicitNamespace())
        {
            return qual.prefix() + ":" + builder.toString();
        }
        else
        {
            return builder.toString();
        }
    }

    private void phase4VerifyEnumDefinition(Ast.EnumDefinitionNode node)
    {
        var type = node.semNSSymbol;
        if (type == null)
        {
            return;
        }
        this.enterFrame(node.block.semNSFrame);
        this.phase4VerifyDirectives(node.block.directives);
        this.exitFrame();
    }

    private void phase5VerifyEnumDefinition(Ast.EnumDefinitionNode node)
    {
        var type = node.semNSSymbol;
        if (type == null)
        {
            return;
        }
        this.enterFrame(node.block.semNSFrame);
        this.phase5VerifyDirectives(node.block.directives);
        this.exitFrame();
    }

    private void phase2VerifyFunctionDefinition(Ast.FunctionDefinitionNode node)
    {
        if (node.markGetter() || node.markSetter() || node.markConstructor())
        {
            if (node.markGetter())
            {
                this.phase2VerifyGetterOrSetterDefinition(node, true);
            }
            else if (node.markSetter())
            {
                this.phase2VerifyGetterOrSetterDefinition(node, false);
            }
            else if (node.markConstructor())
            {
                this.phase2VerifyConstructorDefinition(node);
            }
            return;
        }
        var qual = this.resolveDefinitionQualifier(node);
        if (qual == null)
        {
            return;
        }
        var name = this.pool.createName(qual, node.name);
        var intoNames = this.getDefinitionNameCollection(node);
        if (intoNames.hasName(name))
        {
            this.reportVerifyError(Problem.Constants.NAMESPACE_CONFLICT, node.nameSpan, Problem.Argument.createQuote(qual.toString()));
        }
        else
        {
            var et = this.currentFrame().symbol() != null && this.currentFrame().symbol().isType() ? this.currentFrame().symbol() : null;
            var frame = this.pool.createParameterFrame(et != null && !node.markStatic() ? this.pool.createThis(et) : null);
            var activation = new Activation(frame);
            frame.setActivation(activation);
            if (frame.parameterThis() != null)
            {
                frame.parameterThis().setActivation(activation);
            }
            node.common.semNSFrame = frame;
            var f = this.pool.createFunction(name, null);
            f.setActivation(activation);
            f.setDefinitionPackage(this.currentFrame().isPackageFrame() ? this.currentFrame().symbol() : null);
            f.setMarkYielding(node.common.markYielding());
            f.setMarkFinal(node.markFinal());
            f.setMarkNative(node.markNative());
            intoNames.defineName(name, f);
            node.semNSSymbol = f;
        }
    }

    private void phase2VerifyGetterOrSetterDefinition(Ast.FunctionDefinitionNode node, boolean isGetter)
    {
        var qual = this.resolveDefinitionQualifier(node);
        if (qual == null)
        {
            return;
        }
        var name = this.pool.createName(qual, node.name);
        var intoNames = this.getDefinitionNameCollection(node);
        var k = intoNames.lookupName(name);

        Symbol et = null;
        Symbol frame = null;
        Symbol f = null;
        Activation activation = null;

        if (k != null && k.isVirtualProperty() && (isGetter ? k.getter() == null : k.setter() == null))
        {
            et = this.currentFrame().symbol() != null && this.currentFrame().symbol().isType() ? this.currentFrame().symbol() : null;
            frame = this.pool.createParameterFrame(et != null && !node.markStatic() ? this.pool.createThis(et) : null);
            activation = new Activation(frame);
            frame.setActivation(activation);
            node.common.semNSFrame = frame;

            f = this.pool.createFunction(name, null);
            f.setOwnerVirtualProperty(k);
            f.setDefinitionPackage(this.currentFrame().isPackageFrame() ? this.currentFrame().symbol() : null);
            f.setActivation(activation);
            if (isGetter)
            {
                k.setGetter(f);
            }
            else
            {
                k.setSetter(f);
            }
            node.semNSSymbol = f;
        }
        else if (k != null)
        {
            this.reportVerifyError(Problem.Constants.NAMESPACE_CONFLICT, node.nameSpan, Problem.Argument.createQuote(qual.toString()));
        }
        else
        {
            et = this.currentFrame().symbol() != null && this.currentFrame().symbol().isType() ? this.currentFrame().symbol() : null;
            frame = this.pool.createParameterFrame(et != null && !node.markStatic() ? this.pool.createThis(et) : null);
            activation = new Activation(frame);
            frame.setActivation(activation);
            node.common.semNSFrame = frame;
            var vp = this.pool.createVirtualProperty(name, null);
            f = this.pool.createFunction(name, null);
            f.setOwnerVirtualProperty(vp);
            f.setDefinitionPackage(this.currentFrame().isPackageFrame() ? this.currentFrame().symbol() : null);
            if (isGetter)
            {
                vp.setGetter(f);
            }
            else
            {
                vp.setSetter(f);
            }
            node.semNSSymbol = f;
            intoNames.defineName(name, vp);
        }
    }

    private void phase2VerifyConstructorDefinition(Ast.FunctionDefinitionNode node)
    {
        var qual = this.resolveDefinitionQualifier(node);
        if (qual == null)
        {
            return;
        }
        var ec = this.currentFrame().symbol();
        if (ec.constructorFunction() != null)
        {
            this.reportVerifyError(Problem.Constants.CONSTRUCTOR_ALREADY_DEFINED, node.nameSpan);
        }
        else
        {
            var frame = this.pool.createParameterFrame(this.pool.createThis(ec));
            var activation = new Activation(frame);
            frame.setActivation(activation);
            node.common.semNSFrame = frame;
            var f = this.pool.createFunction(ec.name(), null);
            f.setActivation(activation);
            f.setDefinitionPackage(this.currentFrame().isPackageFrame() ? this.currentFrame().symbol() : null);
            ec.setConstructorFunction(f);
            node.semNSSymbol = f;
        }
    }

    private void phase3VerifyFunctionDefinition(Ast.FunctionDefinitionNode node)
    {
        var f = node.semNSSymbol;
        if (f == null)
        {
            return;
        }
        f.setMarkNative(node.common.body == null);
        f.setMarkFinal(node.markFinal());
        Symbol bs = null;
        if (node.markSetter() && f.ownerVirtualProperty().staticType() != null)
        {
            bs = this.pool.createFunctionType(new Symbol[] {f.ownerVirtualProperty().staticType()}, null, null, this.pool.voidType);
        }
        else if (node.markGetter() && f.ownerVirtualProperty().staticType() != null)
        {
            bs = this.pool.createFunctionType((Vector<Symbol>) null, null, null, f.ownerVirtualProperty().staticType());
        }

        var s = this.resolveSignature(node.common, node.nameSpan, bs);
        f.setSignature(s);
        Symbol vp = null;

        if (node.markGetter())
        {
            // limit getter signature
            vp = f.ownerVirtualProperty();
            if (s.params() != null || s.optParams() != null || s.rest() != null)
            {
                vp.setGetter(null);
                this.reportVerifyError(Problem.Constants.INVALID_GETTER_SIGNATURE, node.nameSpan);
            }
            else
            {
                vp.setStaticType(s.result());
            }
        }
        else if (node.markSetter())
        {
            // limit setter signature
            vp = f.ownerVirtualProperty();
            if (s.params() == null || s.params().size() != 1 || s.optParams() != null || s.rest() != null)
            {
                vp.setSetter(null);
                this.reportVerifyError(Problem.Constants.INVALID_SETTER_SIGNATURE, node.nameSpan);
            }
            else
            {
                vp.setStaticType(s.params().get(0));
            }
        }
    }

    private void phase4VerifyFunctionDefinition(Ast.FunctionDefinitionNode node)
    {
        var f = node.semNSSymbol;
        if (f == null)
        {
            return;
        }

        var et = this.currentFrame().symbol();

        if (node.markOverride())
        {
            f.setMarkOverride(true);
            try
            {
                f.override(et.delegate());
            }
            catch (IncompatibleOverrideSignatureError exc)
            {
                this.reportVerifyError(Problem.Constants.INCOMPATIBLE_OVERRIDE_SIGNATURE, node.nameSpan, Problem.Argument.createQuote(f.name().toString()), Problem.Argument.createSymbol(exc.expectedSignature));
            }
            catch (NoMethodToOverrideError exc)
            {
                this.reportVerifyError(Problem.Constants.OVERRIDING_UNDEFINED_METHOD, node.nameSpan, Problem.Argument.createQuote(f.name().toString()));
            }
            catch (OverridingFinalMethodError exc)
            {
                this.reportVerifyError(Problem.Constants.OVERRIDING_FINAL_METHOD, node.nameSpan);
            }
        }
        // report duplicate property
        else
        {
            var delegate = this.currentFrame().symbol() != null ? this.currentFrame().symbol().delegate() : null;
            if (!node.markConstructor() && delegate != null && delegate.inherit() != null && delegate.inherit().lookupName(f.name()) != null)
            {
            	this.reportVerifyError(Problem.Constants.REDEFINING_SUPER_CLASS_PROPERTY, node.nameSpan, Problem.Argument.createQuote(f.name().toString()));
            }
        }

        // verify super constructor use
        if (node.markConstructor())
        {
            var superClass = this.currentFrame().symbol().superClass();
            if (superClass != null)
            {
                Symbol superCF = null;
                for (var h = superClass; h != null; h = h.superClass())
                {
                    if ((superCF = h.constructorFunction()) != null)
                    {
                        break;
                    }
                }
                var s = superCF != null ? superCF.signature() : null;
                if (s != null && (s.params() != null || s.optParams() != null || s.rest() != null))
                {
                    boolean hasSuperStmt = false;
                    if (node.common.body instanceof Ast.BlockNode)
                    {
                        for (var drtv : ((Ast.BlockNode) node.common.body).directives)
                        {
                            if (drtv instanceof Ast.SuperStatementNode)
                            {
                                hasSuperStmt = true;
                                break;
                            }
                        }
                    }
                    if (!hasSuperStmt)
                    {
                        this.reportVerifyError(Problem.Constants.SUPER_CLASS_HAS_NO_DEFAULT_CONSTRUCTOR, node.nameSpan);
                    }
                }
            }
        }

        // validate proxy
        if (f.name().namespace() == this.pool.proxyNamespace && (et.isClassType() || et.isEnumType() || et.isInterfaceType()))
        {
            this.validateProxyDefinition(node, et, f);
        }
    }

    private void validateProxyDefinition(Ast.FunctionDefinitionNode node, Symbol et, Symbol f)
    {
        var name = f.name();
        Symbol typeK = null;
        Symbol typeV = null;
        ProxyPropertyTrait tr;

        var operator = Operator.fromQualifiedName(name);
        if (operator != null && !node.markStatic())
        {
            var es = this.pool.createFunctionType(operator.isUnary() ? null : new Symbol[] {et}, null, null, operator.resultsBoolean() ? this.pool.booleanType : et);
            if (f.signature() == es)
            {
                et.delegate().initOwnOperators();
                et.delegate().ownOperators().put(operator, f);
            }
        }
        else if (!node.markStatic())
        {
            var s = f.signature();

            // proxy::getProperty(name:K):V
            if (name == this.pool.proxyGetPropertyName)
            {
                if (!(s.params() == null || s.params().size() != 1 || s.optParams() != null || s.rest() != null))
                {
                    typeK = s.params().get(0);
                    typeV = s.result();
                    tr = et.delegate().ownProxyPropertyTrait();
                    if (tr == null)
                    {
                        et.delegate().setOwnProxyPropertyTrait(tr = this.pool.createProxyPropertyTrait(typeK, typeV));
                        tr.setGetMethod(f);
                    }
                    else if (typeK == tr.keyType() && typeV == tr.valueType())
                    {
                        tr.setGetMethod(f);
                    }
                }
            }
            // proxy::setProperty(name:K, value:V):void
            else if (name == this.pool.proxySetPropertyName)
            {
                if (!(s.params() == null || s.params().size() != 2 || s.optParams() != null || s.rest() != null || s.result() != this.pool.voidType))
                {
                    typeK = s.params().get(0);
                    typeV = s.params().get(1);
                    tr = et.delegate().ownProxyPropertyTrait();

                    if (tr == null)
                    {
                        et.delegate().setOwnProxyPropertyTrait(tr = this.pool.createProxyPropertyTrait(typeK, typeV));
                        tr.setSetMethod(f);
                    }
                    else if (typeK == tr.keyType() && typeV == tr.valueType())
                    {
                        tr.setSetMethod(f);
                    }
                }
            }
            // proxy::deleteProperty(name:K):Boolean
            else if (name == this.pool.proxyDeletePropertyName)
            {
                if (!(s.params() == null || s.params().size() != 1 || s.optParams() != null || s.rest() != null || s.result() != this.pool.booleanType))
                {
                    typeK = s.params().get(0);
                    tr = et.delegate().ownProxyPropertyTrait();
                    if (tr != null && typeK == tr.keyType())
                    {
                        tr.setDeleteMethod(f);
                    }
                }
            }
            // proxy::getAttribute(name:K):V
            else if (name == this.pool.proxyGetAttributeName)
            {
                if (!(s.params() == null || s.params().size() != 1 || s.optParams() != null || s.rest() != null))
                {
                    typeK = s.params().get(0);
                    typeV = s.result();
                    if (pool.isNameType(typeK))
                    {
                        tr = et.delegate().ownAttributeTrait();
                        if (tr == null)
                        {
                            et.delegate().setOwnAttributeTrait(tr = this.pool.createProxyPropertyTrait(typeK, typeV));
                            tr.setGetMethod(f);
                        }
                        else if (typeK == tr.keyType() && typeV == tr.valueType())
                        {
                            tr.setGetMethod(f);
                        }
                    }
                }
            }
            // proxy::setAttribute(name:K, value:V):void
            else if (name == this.pool.proxySetAttributeName)
            {
                if (!(s.params() == null || s.params().size() != 2 || s.optParams() != null || s.rest() != null || s.result() != this.pool.voidType))
                {
                    typeK = s.params().get(0);
                    typeV = s.params().get(1);
                    if (pool.isNameType(typeK))
                    {
                        tr = et.delegate().ownAttributeTrait();

                        if (tr == null)
                        {
                            et.delegate().setOwnAttributeTrait(tr = this.pool.createProxyPropertyTrait(typeK, typeV));
                            tr.setSetMethod(f);
                        }
                        else if (typeK == tr.keyType() && typeV == tr.valueType())
                        {
                            tr.setSetMethod(f);
                        }
                    }
                }
            }
            // proxy::deleteAttribute(name:K):Boolean
            else if (name == this.pool.proxyDeleteAttributeName)
            {
                if (!(s.params() == null || s.params().size() != 1 || s.optParams() != null || s.rest() != null || s.result() != this.pool.booleanType))
                {
                    typeK = s.params().get(0);
                    tr = et.delegate().ownAttributeTrait();
                    if (tr != null && typeK == tr.keyType())
                    {
                        tr.setDeleteMethod(f);
                    }
                }
            }
            // proxy::filter(f:function(E):Boolean):V
            else if (name == this.pool.proxyFilterName)
            {
                if (!(s.params() == null || s.params().size() != 1 || s.optParams() != null || s.rest() != null))
                {
                    var p1 = s.params().get(0);
                    if (p1.isFunctionType() && p1.params() != null && p1.params().size() == 1 && p1.optParams() == null && p1.rest() == null && p1.result() == this.pool.booleanType)
                    {
                        et.delegate().setOwnFilterProxy(f);
                    }
                }
            }
        }
    }

    private void phase5VerifyFunctionDefinition(Ast.FunctionDefinitionNode node)
    {
        var f = node.semNSSymbol;
        if (f == null)
        {
            return;
        }
        var et = this.currentFrame().symbol() != null && this.currentFrame().symbol().isType() ? this.currentFrame().symbol() : null;
        this.prepareFunctionActivation(node.common, f.signature());
        f.setActivation(node.common.semNSFrame.activation());

        if (node.markConstructor() && node.common.body instanceof Ast.BlockNode)
        {
            phase5VerifyConstructorDefinition(node, et);
        }
        else if (node.common.body != null)
        {
            this.enterFunction(f, node.common);
            this.verifyFunctionBody(node.common, f, node.nameSpan);
            this.exitFunction();
        }
    }

    private void phase5VerifyConstructorDefinition(Ast.FunctionDefinitionNode node, Symbol et)
    {
        var f = node.semNSSymbol;
        var block = (Ast.BlockNode) node.common.body;
        var frame = this.pool.createBlockFrame();
        frame.setDefaultNamespace(this.currentFrame().searchInternalNamespace());
        frame.setActivation(f.activation());
        block.semNSFrame = frame;

        this.enterFunction(f, node.common);
        this.enterFrame(node.common.semNSFrame);
        this.enterFrame(frame);

        // collect read-only variables and mutate their state

        Vector<Symbol> read_only_vars = new Vector<>();

        for (var name_pair : et.delegate().names())
        {
            if (name_pair.value.isVariableProperty() && name_pair.value.readOnly())
            {
                read_only_vars.add(name_pair.value);
            }
        }

        for (Symbol read_only_var : read_only_vars)
        {
            read_only_var.setReadOnly(false);
        }

        var drtv_sequence = block.directives;
        this.verifyDirectives(drtv_sequence);

        Ast.DirectiveNode drtv = null;
        int sup_index = -1;
        int i = 0;

        for (var drtv2 : drtv_sequence)
        {
            if (drtv2 instanceof Ast.SuperStatementNode)
            {
                sup_index = i;
                break;
            }
            ++i;
        }

        var this_checker = new ThisChecker();
        var reassign_checker = new ThisConstantReassignChecker();

        // - unallow use of this before super()
        // - detect variable initialisation end
        // - unallow reassignment of read-only variables before this is accessed

        int first_this_access_index = -1;

        for (i = 0; i != drtv_sequence.size(); ++i)
        {
            drtv = drtv_sequence.get(i);
            this_checker.visit(drtv, null);
            first_this_access_index = i;
            if (this_checker.accessNodes != null)
            {
                break;
            }
        }

        node.semNSInstanceInitialiserEnd = i;

        for (; i != drtv_sequence.size(); ++i)
        {
            drtv = drtv_sequence.get(i);
            reassign_checker.visit(drtv, null);
        }

        if (this_checker.accessNodes != null && sup_index != -1 && first_this_access_index < sup_index)
        {
            // accessing "this" before super statement
            for (var expr1 : this_checker.accessNodes)
            {
                reportVerifyError(Problem.Constants.CANNOT_ACCESS_THIS_BEFORE_SUPER, expr1.span);
            }
        }

        if (reassign_checker.accessNodes != null)
        {
            // re-assigning read-only variable properties
            for (var expr2 : reassign_checker.accessNodes)
            {
                reportVerifyError(Problem.Constants.REFERENCE_IS_READ_ONLY, expr2.span);
            }
        }

        for (Symbol read_only_var : read_only_vars)
        {
            read_only_var.setReadOnly(true);
        }

        this.exitFrame();
        this.exitFrame();
        this.exitFunction();
    }

    private static class ThisChecker extends NodeVisitor<Object, Object>
    {
        public Vector<Ast.ExpressionNode> accessNodes;
        static public final Object ASSIGNMENT_LEFT = new Object();

        @Override
        public Object visit(Ast.SimpleIdNode node, Object argument)
        {
            _checkSymbol(node.semNSResult, node, argument);
            super.visit(node, null);
            return null;
        }

        @Override
        public Object visit(Ast.ExpressionIdNode node, Object argument)
        {
            _checkSymbol(node.semNSResult, node, argument);
            super.visit(node, null);
            return null;
        }

        @Override
        public Object visit(Ast.AttributeIDNode node, Object argument)
        {
            _checkSymbol(node.semNSResult, node, argument);
            super.visit(node, null);
            return null;
        }

        @Override
        public Object visit(Ast.SuperDotNode node, Object argument)
        {
            _checkSymbol(node.semNSResult, node, argument);
            super.visit(node, null);
            return null;
        }

        @Override
        public Object visit(Ast.AssignmentNode node, Object argument)
        {
            visit(node.left, ASSIGNMENT_LEFT);
            visit(node.right, null);
            return null;
        }

        private void _checkSymbol(Symbol symbol, Ast.ExpressionNode node, Object argument)
        {
            if (symbol == null)
            {
                return;
            }
            var obj = symbol.baseObject();
            if (((obj != null && obj.isThis()) || symbol.isThis()))
            {
                if (argument != ASSIGNMENT_LEFT || !obj.accessingProperty().isVariableProperty())
                {
                    accessNodes = accessNodes !=  null ? accessNodes : new Vector<>();
                    accessNodes.add(node);
                }
            }
        }
    }

    private static class ThisConstantReassignChecker extends NodeVisitor<Object, Object>
    {
        public Vector<Ast.ExpressionNode> accessNodes = null;

        @Override
        public Object visit(Ast.AssignmentNode node, Object argument)
        {
            var left = node.left.semNSResult;
            var left_obj = left != null ? left.baseObject() : null;
            if (left_obj != null && left_obj.isThis() && left.accessingProperty() != null && left.accessingProperty().isVariableProperty() && left.accessingProperty().readOnly())
            {
                accessNodes = accessNodes == null ? new Vector<>() : accessNodes;
                accessNodes.add(node.left);
            }
            super.visit(node, null);
            return null;
        }
    }

    private void phase2VerifyClassDefinition(Ast.ClassDefinitionNode node)
    {
        var qual = this.resolveDefinitionQualifier(node);
        if (qual == null)
        {
            return;
        }
        var classMetadata = node.getMetaData("Class");
        var name = this.pool.createName(qual, node.name);
        Names intoNames = this.getDefinitionNameCollection(node);
        Symbol type = null;
        var k = intoNames.lookupName(name);
        if (k != null)
        {
            if (k.isClassType() && this.allowDuplicates)
            {
                type = k;
            }
            else
            {
                this.reportVerifyError(Problem.Constants.NAMESPACE_CONFLICT, node.nameSpan, Problem.Argument.createQuote(qual.toString()));
            }
        }
        else
        {
            var isPrimitive = classMetadata.findEntry("primitive") != null && classMetadata.findEntry("primitive").value == true;
            var isUnion = classMetadata.findEntry("union") != null && classMetadata.findEntry("union").value == true;
            type = this.pool.createClassType(name, node.markFinal(), isPrimitive, isUnion);
            intoNames.defineName(name, type);
        }

        node.semNSSymbol = type;

        node.deleteMetaData("Class");

        if (classMetadata.findEntry("dynamicInit") != null && classMetadata.findEntry("dynamicInit").value == true)
            type.setIsInitialisable(true);

        if (classMetadata.findEntry("constructor") != null && classMetadata.findEntry("constructor").value == false)
            type.setIsConstructable(false);

        Symbol frame = null;

        if (type != null)
        {
            frame = node.block.semNSFrame = this.pool.createClassFrame(type);
            frame.setDefaultNamespace(this.currentFrame().searchPublicNamespace());
            frame.setDefaultNamespace(frame.defaultNamespace() == null ? this.currentFrame().searchInternalNamespace() : frame.defaultNamespace());
            frame.setActivation(this.currentActivation);
        }

        if (node.typeParams != null && type != null && type.typeParams() == null)
        {
            type.setTypeParams(new Vector<>());
            for (var p : node.typeParams)
            {
                var typeP = this.pool.createTypeParameter(pool.createName(frame.defaultNamespace(), p), type);
                type.typeParams().add(typeP);
            }
        }

        if (type != null && type.typeParams() != null)
        {
            for (var typeP2 : type.typeParams())
            {
                var name2 = this.pool.createName(frame.defaultNamespace(), typeP2.name().localName());
                if (!frame.ownNames().hasName(name2))
                {
                    frame.ownNames().defineName(name2, typeP2);
                }
            }
        }
    }

    private void phase3VerifyClassDefinition(Ast.ClassDefinitionNode node)
    {
        var type = node.semNSSymbol;
        if (type == null)
        {
            return;
        }
        if (node.extendsNode != null)
        {
            Symbol frame = null;

            // allow type parameters to appear in an extends clause
            if (type.typeParams() != null)
            {
                frame = this.pool.createParameterFrame(null);
                frame.setActivation(this.currentActivation);
                for (var p1 : type.typeParams())
                {
                    frame.ownNames().defineName(p1.name(), p1);
                }
                this.enterFrame(frame);
            }

            var type2 = this.verifyTypeAnnotation(node.extendsNode);
            if (type2 != null && type2.isClassType())
            {
                if (type2.markFinal())
                {
                    this.reportVerifyError(Problem.Constants.CANNOT_EXTEND_FINAL_CLASS, node.extendsNode.span);
                }
                else if (type.classCfgPrimitive() && type2 != this.pool.objectType)
                {
                    this.reportVerifyError(Problem.Constants.PRIMITIVE_CLASS_MUST_EXTEND_OBJECT, node.nameSpan);
                }
                else
                {
                    type.extend(type2);
                }
            }
            else if (type2 != null)
            {
                this.reportVerifyError(Problem.Constants.TYPE_IS_NOT_CLASS, node.extendsNode.span, Problem.Argument.createSymbol(type2));
            }

            if (frame != null)
            {
                this.exitFrame();
            }
        }
        if (node.implementsList != null)
        {
            Symbol frame = null;

            // allow type parameters to appear in an implements clause
            if (type.typeParams() != null)
            {
                frame = this.pool.createParameterFrame(null);
                frame.setActivation(this.currentActivation);
                for (var p2 : type.typeParams())
                {
                    frame.ownNames().defineName(p2.name(), p2);
                }
                this.enterFrame(frame);
            }
            for (var annotation : node.implementsList)
            {
                var type2 = this.verifyTypeAnnotation(annotation);
                if (type2 != null && type2.isInterfaceType())
                {
                    type.implement(type2);
                }
                else if (type2 != null)
                {
                    this.reportVerifyError(Problem.Constants.TYPE_IS_NOT_INTERFACE, annotation.span, Problem.Argument.createSymbol(type2));
                }
            }
            if (frame != null)
            {
                this.exitFrame();
            }
        }

        this.enterFrame(node.block.semNSFrame);
        this.phase1VerifyDirectives(node.block.directives);
        this.phase2VerifyDirectives(node.block.directives);
        this.phase3VerifyDirectives(node.block.directives);
        this.exitFrame();
    }

    private void phase4VerifyClassDefinition(Ast.ClassDefinitionNode node)
    {
        var type = node.semNSSymbol;
        if (type == null)
        {
            return;
        }
        var verifier = this;
        type.verifyInterfaceImplementations(new InterfaceImplementationEvents()
        {
            public void onundefined(String kind, Symbol name, Symbol signature)
            {
                // missing implementation
                if (kind.equals("getter"))
                {
                    verifier.reportVerifyError(Problem.Constants.MISSING_IMPL_GETTER, node.nameSpan, Problem.Argument.createQuote(name.toString()), Problem.Argument.createSymbol(signature));
                }
                else if (kind.equals("setter"))
                {
                    verifier.reportVerifyError(Problem.Constants.MISSING_IMPL_SETTER, node.nameSpan, Problem.Argument.createQuote(name.toString()), Problem.Argument.createSymbol(signature));
                }
                else
                {
                    verifier.reportVerifyError(Problem.Constants.MISSING_IMPL_METHOD, node.nameSpan, Problem.Argument.createQuote(name.toString()), Problem.Argument.createSymbol(signature));
                }
            }

            public void onwrong(String kind, Symbol name)
            {
                // wrong definition
                if (kind.equals("virtualProperty"))
                {
                    verifier.reportVerifyError(Problem.Constants.WRONG_PROPERTY_IMPL_DEFINITION, node.nameSpan, Problem.Argument.createQuote(name.toString()));
                }
                else
                {
                    verifier.reportVerifyError(Problem.Constants.WRONG_METHOD_IMPL_DEFINITION, node.nameSpan, Problem.Argument.createQuote(name.toString()));
                }
            }
        });

        this.enterFrame(node.block.semNSFrame);
        this.phase4VerifyDirectives(node.block.directives);
        this.exitFrame();

        if (type.classCfgUnion())
        {
            for (var drtv : node.block.directives)
            {
                Ast.VarDefinitionNode var_defn = null;
                if (drtv instanceof Ast.VarDefinitionNode)
                {
                    var_defn = (Ast.VarDefinitionNode) drtv;
                    for (var binding : var_defn.bindings)
                    {
                        if (!(binding.pattern instanceof Ast.NamePatternNode))
                        {
                            reportVerifyError(Problem.Constants.UNALLOWED_PATTERN_ON_UNION_CLASS, binding.pattern.span);
                        }
                        else if (!binding.pattern.semNSVariable.staticType().containsUndefined())
                        {
                            reportVerifyError(Problem.Constants.UNION_VARIABLE_MUST_BE_NULLABLE, binding.pattern.span);
                        }
                    }
                }
            }
        }
    }

    private void phase5VerifyClassDefinition(Ast.ClassDefinitionNode node)
    {
        var type = node.semNSSymbol;
        if (type == null)
        {
            return;
        }
        this.enterFrame(node.block.semNSFrame);
        this.phase5VerifyDirectives(node.block.directives);
        this.exitFrame();
    }

    private void phase2VerifyInterfaceDefinition(Ast.InterfaceDefinitionNode node)
    {
        var qual = this.resolveDefinitionQualifier(node);
        if (qual == null)
        {
            return;
        }
        var name = this.pool.createName(qual, node.name);
        var intoNames = this.getDefinitionNameCollection(node);
        Symbol type = null;
        var k = intoNames.lookupName(name);
        if (k != null)
        {
            if (k.isInterfaceType() && this.allowDuplicates)
            {
                type = k;
            }
            else
            {
                this.reportVerifyError(Problem.Constants.NAMESPACE_CONFLICT, node.nameSpan, Problem.Argument.createQuote(qual.toString()));
            }
        }
        else
        {
            type = this.pool.createInterfaceType(name);
            intoNames.defineName(name, type);
        }

        Symbol frame = null;

        if (type != null)
        {
            frame = node.block.semNSFrame = this.pool.createInterfaceFrame(type);
            frame.setDefaultNamespace(this.currentFrame().searchPublicNamespace());
            frame.setDefaultNamespace(frame.defaultNamespace() == null ? this.currentFrame().searchInternalNamespace() : frame.defaultNamespace());
            frame.setActivation(this.currentActivation);
        }

        node.semNSSymbol = type;

        if (node.typeParams != null && type != null && type.typeParams() == null)
        {
            type.setTypeParams(new Vector<>());
            for (var p : node.typeParams)
            {
                var typeP = this.pool.createTypeParameter(pool.createName(frame.defaultNamespace(), p), type);
                type.typeParams().add(typeP);
            }
        }

        if (type != null && type.typeParams() != null)
        {
            for (var typeP2 : type.typeParams())
            {
                var name2 = this.pool.createName(frame.defaultNamespace(), typeP2.name().localName());
                if (!frame.ownNames().hasName(name2))
                {
                    frame.ownNames().defineName(name2, typeP2);
                }
            }
        }
    }

    private void phase3VerifyInterfaceDefinition(Ast.InterfaceDefinitionNode node)
    {
        var type = node.semNSSymbol;
        if (type == null)
        {
            return;
        }
        this.enterFrame(node.block.semNSFrame);
        this.phase1VerifyDirectives(node.block.directives);
        this.phase2VerifyDirectives(node.block.directives);
        this.phase3VerifyDirectives(node.block.directives);
        this.exitFrame();
    }

    private void phase32VerifyInterfaceDefinitions(Vector<Ast.DirectiveNode> directives)
    {
        for (var drtv : directives)
        {
            if (drtv instanceof Ast.IncludeDirectiveNode)
            {
                var include_drtv = (Ast.IncludeDirectiveNode) drtv;
                if (include_drtv.subdirectives != null)
                {
                    phase32VerifyInterfaceDefinitions(include_drtv.subdirectives);
                }
            }
            else if (drtv instanceof Ast.InterfaceDefinitionNode)
            {
                var itrfc_defn = (Ast.InterfaceDefinitionNode) drtv;
                phase3VerifyInterfaceDefinition2(itrfc_defn);
            }
        }
    }

    private void phase3VerifyInterfaceDefinition2(Ast.InterfaceDefinitionNode node)
    {
        var type = node.semNSSymbol;
        if (type == null)
        {
            return;
        }
        if (node.extendsList != null)
        {
            Symbol frame = null;
            // allow type parameters to appear in an implements clause
            if (type.typeParams() != null)
            {
                frame = this.pool.createParameterFrame(null);
                frame.setActivation(this.currentActivation);
                for (var p : type.typeParams())
                {
                    frame.ownNames().defineName(p.name(), p);
                }
                this.enterFrame(frame);
            }
            for (var annotation : node.extendsList)
            {
                var type2 = this.verifyTypeAnnotation(annotation);
                if (type2 != null && type2.isInterfaceType())
                {
                    try
                    {
                        type.extend(type2);
                    }
                    catch (InheritingNameConflictError exc)
                    {
                        this.reportVerifyError(Problem.Constants.CONFLICT_INHERITING_ITRFC_NAME, node.nameSpan, Problem.Argument.createQuote(exc.name.toString()), Problem.Argument.createSymbol(exc.inheritedInterface));
                    }
                }
                else if (type2 != null)
                {
                    this.reportVerifyError(Problem.Constants.TYPE_IS_NOT_INTERFACE, annotation.span, Problem.Argument.createSymbol(type2));
                }
            }
            if (frame != null)
            {
                this.exitFrame();
            }
        }
    }

    private void phase4VerifyInterfaceDefinition(Ast.InterfaceDefinitionNode node)
    {
        var type = node.semNSSymbol;
        if (type == null)
        {
            return;
        }
        this.enterFrame(node.block.semNSFrame);
        this.phase4VerifyDirectives(node.block.directives);
        this.exitFrame();
    }

    private void phase5VerifyInterfaceDefinition(Ast.InterfaceDefinitionNode node)
    {
        var type = node.semNSSymbol;
        if (type == null)
        {
            return;
        }
        this.enterFrame(node.block.semNSFrame);
        this.phase5VerifyDirectives(node.block.directives);
        this.exitFrame();
    }

    /**
     * Feature removed. "Where" meta-data
     */
    private void declareTypeParameterBounds(Ast.DefinitionNode defn, Symbol classOrItrfc)
    {
        /*
        if (classOrItrfc.typeParams() == null)
        {
            return;
        }
        Vector<Ast.MetaDataNode> collected_meta_data = null;
        if (defn.metaDataArray != null)
        {
            for (var meta_data : defn.metaDataArray)
            {
                if (meta_data.name.equals("Where"))
                {
                    collected_meta_data = collected_meta_data == null ? new Vector<>() : collected_meta_data;
                    collected_meta_data.add(meta_data);
                }
            }
        }

        Symbol type_param = null;

        if (collected_meta_data != null)
        {
            for (var meta_data : collected_meta_data)
            {
                if (meta_data.entries == null || meta_data.entries.size() != 2 || !(meta_data.entries.get(0).name == "" || meta_data.entries.get(0).name == null) || !(meta_data.entries.get(0).literal instanceof Ast.StringLiteralNode))
                {
                    continue;
                }
                var numeric_entry = meta_data.getEntry("numeric");
                Ast.MetaDataEntryNode default_entry = null;
                if (numeric_entry != null && numeric_entry.literal instanceof Ast.BooleanLiteralNode literal)
                {
                    type_param = classOrItrfc.getTypeParam(((Ast.StringLiteralNode) meta_data.entries.firstElement().literal).value);
                    if (type_param != null)
                    {
                        type_param.setIsNumericTypeParameter(literal.value);
                    }
                    defn.deleteMetaData(meta_data);
                }
                else if ((default_entry = meta_data.getEntry("defaultType")) != null && default_entry.literal instanceof Ast.StringLiteralNode literal)
                {
                    type_param = classOrItrfc.getTypeParam(((Ast.StringLiteralNode) meta_data.entries.firstElement().literal).value);
                    var subscript = new Script(literal.value);
                    enterScript(subscript);
                    var default_type_annotation = new Parser(subscript).parseTypeAnnotation();
                    Symbol default_type = default_type_annotation == null ? null : verifyTypeAnnotation(default_type_annotation);
                    exitScript();
                    if (default_type == null)
                    {
                        this.reportVerifyError(Problem.Constants.PROCESSING_TYPE_ANNOTATION, literal.span);
                    }
                    if (type_param != null && default_type != null)
                    {
                        type_param.setDefaultType(default_type);
                    }
                    defn.deleteMetaData(meta_data);
                }
            }
        }
        */
    }

    private void checkTypeParameterBounds1(Symbol typeParam, Symbol argument, Span span)
    {
        if (typeParam.isNumericTypeParameter() && !pool.isNumericType(argument))
        {
            this.reportVerifyError(Problem.Constants.EXPECTING_NUMERIC_DATA_TYPE, span);
        }
    }

    private Symbol limitType(Ast.ExpressionNode node, Symbol type)
    {
        var v = this.verifyValue(node, type);
        if (v == null)
        {
            return null;
        }
        var k = v.staticType();
        node.semNSResult = v = v.implicitConversion(type);
        if (v == null)
        {
            this.reportVerifyError(Problem.Constants.INCOMPATIBLE_TYPES, node.span, Problem.Argument.createSymbol(k), Problem.Argument.createSymbol(type));
            node.semNSResult = this.pool.verifyingType;
            return null;
        }
        return v;
    }

    private Symbol tryConverting(Ast.ExpressionNode node, Symbol type)
    {
        var v = this.verifyValue(node, type);
        if (v == null)
        {
            return null;
        }
        var k = v.staticType();
        node.semNSResult = v = v.implicitConversion(type);
        return v;
    }

    public Symbol verifyExpression(Ast.ExpressionNode node)
    {
        return verifyExpression(node, null);
    }

    public Symbol verifyExpression(Ast.ExpressionNode node, Symbol inferenceType)
    {
        return verifyExpression(node, inferenceType, 0);
    }

    public Symbol verifyExpression(Ast.ExpressionNode node, Symbol inferenceType, int flags)
    {
        var r = node.semNSResult;
        if (r != null)
        {
            return r.isVerifyingType() ? null : r;
        }
        r = this.verifyConstantValue(node, inferenceType, false);
        if (r != null)
        {
            node.semNSResult = r;
            if (inferenceType != null && r.isValue())
            {
                r = r.constantImplicitConversion(inferenceType);
                if (r != null)
                {
                    node.semNSResult = r;
                }
            }
            return node.semNSResult;
        }

        Symbol base = null;

        // qualified identifier
        if (node instanceof Ast.SimpleIdNode simple_id)
        {
            if (simple_id.qualifier != null)
            {
                // if qualifier is runtime namespace, redirect to dynamic property
                var qual = this.verifyValue(simple_id.qualifier);
                if (qual != null && qual.isConstantValue())
                {
                    r = this.resolveLexicalProperty(simple_id, flags);
                }
                else if (qual != null)
                {
                    r = this.verifyProxyPropertyID(simple_id, node.span);
                }
            }
            else
            {
                r = this.resolveLexicalProperty(simple_id, flags);
            }
        }
        // dot operator
        else if (node instanceof Ast.DotNode dot)
        {
            if (dot.id instanceof Ast.SimpleIdNode)
            {
                r = this.verifySimpleDotOperator(dot, flags);
            }
            else if (dot.id instanceof Ast.ExpressionIdNode)
            {
                base = this.verifyValue(dot.base, null, VerifyFlags.OBJECT_DOT);
                if (base != null)
                {
                    r = this.verifyProxyPropertyID(dot.id, node.span, base);
                }
            }
            else
            {
                base = this.verifyValue(dot.base, null, VerifyFlags.OBJECT_DOT);
                if (base != null)
                {
                    r = this.verifyAttributeID(dot.id, node.span, base);
                }
            }
        }
        // string literal
        else if (node instanceof Ast.StringLiteralNode strltr)
        {
            r = this.verifyStringLiteral(strltr, inferenceType);
        }
        // this literal
        else if (node instanceof Ast.ThisLiteralNode)
        {
            Symbol thisInst = null;
            for (var frame = this.currentFrame(); frame != null; frame = frame.parentFrame())
            {
                if ((thisInst = frame.parameterThis()) != null)
                {
                    break;
                }
            }
            if (thisInst != null)
            {
                r = thisInst;
                if (thisInst.activation() != this.currentActivation)
                {
                    thisInst.activation().setScopeExtendedProperty(thisInst);
                }
            }
            else
            {
                this.reportVerifyError(Problem.Constants.CANNOT_ACCESS_THIS_HERE, node.span);
            }
        }
        // qualified identifier
        else if (node instanceof Ast.ExpressionIdNode exprid)
        {
            r = this.verifyProxyPropertyID(exprid, node.span, null);
        }
        // qualified identifier
        else if (node instanceof Ast.AttributeIDNode attrid)
        {
            r = this.verifyAttributeID(attrid.id, node.span, null);
        }
        // reserved namespace
        else if (node instanceof Ast.ReservedNamespaceNode reservedns)
        {
            r = this.verifyReservedNamespace(reservedns);
        }
        // regular expression
        else if (node instanceof Ast.RegExpLiteralNode)
        {
            r = this.pool.createValue(this.pool.regExpType);
        }
        // object literal
        else if (node instanceof Ast.ObjectLiteralNode objltr)
        {
            r = this.verifyObjectLiteral(objltr, inferenceType);
        }
        // array literal
        else if (node instanceof Ast.ArrayLiteralNode arrayltr)
        {
            r = this.verifyArrayLiteral(arrayltr, inferenceType);
        }
        // new operator
        else if (node instanceof Ast.NewOperatorNode newop)
        {
            r = this.verifyNewOperator(newop);
        }
        // function expression
        else if (node instanceof Ast.FunctionExpressionNode fexpr)
        {
            r = this.verifyFunctionExpression(fexpr, inferenceType);
        }
        // embed expression
        else if (node instanceof Ast.EmbedExpressionNode)
        {
            if (inferenceType == null)
            {
                this.reportVerifyError(Problem.Constants.UNTYPED_EMBEDDED_CONTENT, node.span);
            }
            else if (inferenceType.escapeType() != this.pool.byteArrayType
                ||   inferenceType.escapeType() != this.pool.stringType
                ||   inferenceType.escapeType() != this.pool.xmlType)
            {
                this.reportVerifyError(Problem.Constants.UNSUPPORTED_EMBEDDED_CONTENT_TYPE, node.span, Problem.Argument.createSymbol(inferenceType));
            }
            else
            {
                r = this.pool.createValue(inferenceType.escapeType());
            }
        }
        // super dot operator
        else if (node instanceof Ast.SuperDotNode superdot)
        {
            r = this.verifySuperDot(superdot, flags);
        }
        // call operator
        else if (node instanceof Ast.CallNode call)
        {
            base = this.verifyExpression(call.base, null, VerifyFlags.OBJECT_CALL);
            if (base != null && base.isType())
            {
                if (call.arguments.size() != 1)
                {
                    r = this.verifyNewOperator(call);
                    call.semNSIsTypeConstruct = true;
                }
                // Enum("id")
                else if (base.isEnumType() && call.arguments.firstElement() instanceof Ast.StringLiteralNode str_literal)
                {
                    r = this.verifyConstantExpression(str_literal, base);
                }
                else
                {
                    r = this.verifyNewOperator(call, true);
                    if (r == null)
                    {
                        // If sole argument is an array or object literal, the result
                        // of the operator is the result of verifying the argument with the context type.
                        // This is useful for untyped code.
                        if (call.arguments.firstElement() instanceof Ast.ArrayLiteralNode || call.arguments.firstElement() instanceof Ast.ObjectLiteralNode)
                        {
                            r = this.limitType(call.arguments.firstElement(), base);
                        }
                        else
                        {
                            var argument = this.verifyValue(call.arguments.firstElement());
                            call.semNSIsTypeConversion = true;
                            r = argument.explicitConversion(base);
                            if (r == null)
                            {
                                this.reportVerifyError(Problem.Constants.INCOMPATIBLE_TYPES, call.span, Problem.Argument.createSymbol(argument.staticType()), Problem.Argument.createSymbol(base));
                            }
                            else
                                r = pool.createCallConversion(r);
                        }
                    }
                    else
                        call.semNSIsTypeConstruct = true;
                }
            }
            else if (base != null && base.staticType().escapeType().equalsOrInstantiatedFrom(this.pool.generatorType))
            {
                // Generator call
                var gt = base.staticType().escapeType();
                var itemType = gt.arguments().get(0);
                if (call.arguments.size() > 0)
                {
                    this.reportVerifyError(Problem.Constants.WRONG_NUM_ARGUMENTS, node.span, Problem.Argument.createNumber(0));
                }
                r = this.pool.createValue(itemType);
            }
            else
            {
                base = this.verifyValue(call.base, null, VerifyFlags.OBJECT_CALL);
                if (base != null && !base.staticType().escapeType().isFunctionType())
                {
                    this.reportVerifyError(Problem.Constants.OBJECT_IS_NOT_FUNCTION, call.base.span);
                }
                else if (base != null)
                {
                    this.verifyCallArguments(call.arguments, node, base.staticType().escapeType());
                    r = this.pool.createValue(base.staticType().escapeType().result());
                }
            }
        }
        // type arguments operator
        else if (node instanceof Ast.TypeArgumentsNode typeargs)
        {
            base = this.verifyExpression(typeargs.base, null, VerifyFlags.ARGUMENTING_TYPE);
            if (base != null && !base.isType())
            {
                this.reportVerifyError(Problem.Constants.EXPRESSION_IS_NOT_A_TYPE_REFERENCE, typeargs.base.span);
            }
            else if (base != null)
            {
                r = this.verifyTypeArguments(base, typeargs.arguments, node.span);
            }
        }
        // brackets operator
        else if (node instanceof Ast.BracketsNode brackets)
        {
            r = this.verifyBracketsOperator(brackets);
        }
        // filter operator
        else if (node instanceof Ast.FilterNode filterop)
        {
            r = this.verifyFilterOperator(filterop);
        }
        // descendants operator
        else if (node instanceof Ast.DescendantsNode desc)
        {
            r = this.verifyDescendantsOperator(desc);
        }
        // unary operator
        else if (node instanceof Ast.UnaryOperatorNode unaryop)
        {
            r = this.verifyUnaryOperator(unaryop, inferenceType);
        }
        // type operator
        else if (node instanceof Ast.TypeOperatorNode typeop)
        {
            r = this.verifyTypeOperator(typeop);
        }
        // binary operator
        else if (node instanceof Ast.BinaryOperatorNode binop)
        {
            r = this.verifyBinaryOperator(binop, inferenceType);
        }
        // ternary operator
        else if (node instanceof Ast.TernaryNode ternop)
        {
            r = this.verifyTernaryOperator(ternop, inferenceType);
        }
        // assignment operator
        else if (node instanceof Ast.AssignmentNode)
        {
            r = this.verifyAssignmentOperator((Ast.AssignmentNode) node, flags);
        }
        // list operator
        else if (node instanceof Ast.ListExpressionNode list_expr)
        {
            int i = 0;
            for (var subExpr : list_expr.expressions)
            {
                int subflags = i == list_expr.expressions.size() - 1 ? flags : 0;
                r = this.verifyExpression(subExpr, inferenceType, subflags);
                ++i;
            }
        }
        // parens expression
        else if (node instanceof Ast.ParenExpressionNode)
        {
            r = this.verifyExpression(((Ast.ParenExpressionNode) node).expression, inferenceType, flags);
        }
        // pattern assignment operator
        else if (node instanceof Ast.PatternAssignmentNode)
        {
            throw new RuntimeException("Assignment pattern operator is unimplemented.");
        }
        // XML
        else if (node instanceof Ast.XMLListNode)
        {
            this.verifyXMLList((Ast.XMLListNode) node);
            r = this.pool.createValue(this.pool.xmlListType);
        }
        // XML
        else if (node instanceof Ast.XMLNode)
        {
            this.verifyXMLLiteral((Ast.XMLNode) node);
            r = this.pool.createValue(this.pool.xmlType);
        }
        else
        {
            throw new RuntimeException("");
        }

        if (r != null)
        {
            if (r.isValue())
            {
                if ((flags & VerifyFlags.ASSIGNMENT_LEFT_HAND_SIDE) != 0)
                {
                    if (r.readOnly())
                    {
                        this.reportVerifyError(Problem.Constants.REFERENCE_IS_READ_ONLY, node.span);
                    }
                }
                else if ((flags & VerifyFlags.OBJECT_DELETE) == 0)
                {
                    if (r.writeOnly())
                    {
                        this.reportVerifyError(Problem.Constants.REFERENCE_IS_WRITE_ONLY, node.span);
                    }
                }
                if (r.staticType() != null && r.staticType().isVerifyingType())
                {
                    node.semNSResult = this.pool.verifyingType;
                    return null;
                }
            }
            return node.semNSResult = r;
        }
        node.semNSResult = this.pool.verifyingType;
        return null;
    }

    public Symbol verifyValue(Ast.ExpressionNode node)
    {
        return verifyValue(node, null);
    }

    public Symbol verifyValue(Ast.ExpressionNode node, Symbol inferenceType)
    {
        return verifyValue(node, inferenceType, 0);
    }

    public Symbol verifyValue(Ast.ExpressionNode node, Symbol inferenceType, int flags)
    {
        var r = this.verifyExpression(node, inferenceType, flags);
        if (r == null)
        {
            return null;
        }
        if (r.isValue())
        {
            return r;
        }
        else if (r.isPackage())
        {
            this.reportVerifyError(Problem.Constants.CANNOT_USE_PACKAGE_AS_VALUE, node.span);
        }
        else if (r.isType())
        {
            this.reportVerifyError(Problem.Constants.CANNOT_USE_TYPE_AS_VALUE, node.span);
        }
        node.semNSResult = this.pool.verifyingType;
        return null;
    }

    /*
     * Method used occasionaly when context with possible type inference
     * involves with a verifying type, preventing wrong problem reports.
     */
    private void verifyInvalidatedExpression(Ast.ExpressionNode node, int flags)
    {
        if (node instanceof Ast.ParenExpressionNode)
        {
            this.verifyInvalidatedExpression(((Ast.ParenExpressionNode) node).expression, flags);
        }
        else if (!(node instanceof Ast.FunctionExpressionNode || node instanceof Ast.ObjectLiteralNode || node instanceof Ast.ArrayLiteralNode || node instanceof Ast.BinaryOperatorNode || node instanceof Ast.TernaryNode || node instanceof Ast.UnaryOperatorNode || node instanceof Ast.EmbedExpressionNode || node instanceof Ast.ListExpressionNode))
        {
            this.verifyExpression(node, null, flags);
        }
    }

    private void verifyInvalidatedExpression(Ast.ExpressionNode node)
    {
        verifyInvalidatedExpression(node, 0);
    }

    private Symbol verifySuperDot(Ast.SuperDotNode node, int flags)
    {
        Symbol limit = null;
        Symbol obj = null;

        if (node.arguments != null && node.arguments.size() != 0)
        {
            for (var node2 : node.arguments)
            {
                this.verifyValue(node2);
            }
            obj = node.arguments.lastElement().semNSResult;
            obj = obj != null && obj.isValue() ? obj : null;
            if (obj == null)
            {
                return null;
            }
            if (obj.staticType().escapeType().isUnionType())
            {
                reportVerifyError(Problem.Constants.UNALLOWED_UNION_WITH_SUPER_OPERATOR, node.arguments.lastElement().span);
                return null;
            }
        }
        else
        {
            Symbol thisInst = null;
            for (var frame = this.currentFrame(); frame != null; frame = frame.parentFrame())
            {
                if ((thisInst = frame.parameterThis()) != null)
                {
                    break;
                }
            }
            if (thisInst != null)
            {
                if (thisInst.activation() != this.currentActivation)
                {
                    thisInst.activation().setScopeExtendedProperty(thisInst);
                }
            }
            obj = thisInst;
        }
        if (obj == null)
        {
            this.reportVerifyError(Problem.Constants.NO_OBJECT_ON_SUPER, node.span);
            return null;
        }
        limit = obj.staticType().superClass();
        if (limit == null)
        {
            this.reportVerifyError(Problem.Constants.NO_LIMIT_ON_SUPER, node.span);
            return null;
        }

        var r = this.resolveObjectProperty(this.pool.createValue(limit), node.id, flags);

        if (r != null && (r.isObjectProxyProperty() || r.isObjectFilter() || r.isObjectDescendants()))
        {
            reportVerifyError(Problem.Constants.UNALLOWED_ACCESS_WITH_SUPER_OPERATOR, node.span);
        }

        return r;
    }

    private Symbol verifySimpleDotOperator(Ast.DotNode node, int flags)
    {
        var id = (Ast.SimpleIdNode) node.id;
        if (id.qualifier != null)
        {
            // if qualifier is runtime namespace, redirect to dynamic property
            var qual = this.verifyValue(id.qualifier);
            if (qual != null && !qual.isConstantValue())
            {
                var obj = this.verifyValue(node.base);
                return obj != null ? this.verifyProxyPropertyID(id, node.span, obj) : null;
            }
        }
        var base = this.verifyExpression(node.base, null, VerifyFlags.OBJECT_DOT);
        if (base != null)
        {
            if (base.isValue())
            {
                return this.resolveObjectProperty(base, id, flags);
            }
            else if (base.isType() || base.isPackage())
            {
                return this.resolveTypeOrPackageProperty(base, id, true, flags);
            }
        }
        return null;
    }

    private Symbol verifyProxyPropertyID(Ast.QualifiedIdNode id, Span fullSpan)
    {
        return verifyProxyPropertyID(id, fullSpan, null);
    }

    private Symbol verifyProxyPropertyID(Ast.QualifiedIdNode id, Span fullSpan, Symbol obj)
    {
        if (obj == null)
        {
            if (this.currentFrame().isWithFrame())
            {
                obj = this.currentFrame().symbol();
                if (obj.baseFrame().activation() != this.currentActivation)
                {
                    obj.baseFrame().activation().setScopeExtendedProperty(obj.accessingProperty());
                }
            }
            else
            {
                this.reportVerifyError(Problem.Constants.CANNOT_ACCESS_LEXICAL_PROPERTY, fullSpan);
                return null;
            }
        }
        var dp = obj.accessProxyProperty();
        var supported = dp != null;
        supported = supported && this.pool.isNameType(dp.accessingTrait().keyType());
        if (!supported)
        {
            this.reportVerifyError(Problem.Constants.UNDEFINED_DYNAMIC_PROPERTY, fullSpan, Problem.Argument.createSymbol(obj.staticType()));
            return null;
        }
        if (id.qualifier != null)
        {
            this.limitType(id.qualifier, this.pool.namespaceType);
        }
        if (id instanceof Ast.ExpressionIdNode)
        {
            this.limitType(((Ast.ExpressionIdNode) id).key, this.pool.stringType);
        }
        return dp;
    }

    private Symbol verifyAttributeID(Ast.QualifiedIdNode id, Span fullSpan)
    {
        return verifyAttributeID(id, fullSpan, null);
    }

    private Symbol verifyAttributeID(Ast.QualifiedIdNode id, Span fullSpan, Symbol obj)
    {
        if (obj == null)
        {
            if (this.currentFrame().isWithFrame())
            {
                obj = this.currentFrame().symbol();
            }
            else
            {
                this.reportVerifyError(Problem.Constants.CANNOT_ACCESS_LEXICAL_PROPERTY, fullSpan);
                return null;
            }
        }
        var attr = obj.accessAttribute();
        if (attr == null)
        {
            this.reportVerifyError(Problem.Constants.UNDEFINED_ATTRIBUTE, fullSpan, Problem.Argument.createSymbol(obj.staticType()));
            return null;
        }
        if (id.qualifier != null)
        {
            this.limitType(id.qualifier, this.pool.namespaceType);
        }
        if (id instanceof Ast.ExpressionIdNode)
        {
            this.limitType(((Ast.ExpressionIdNode) id).key, this.pool.stringType);
        }
        return attr;
    }

    private Symbol verifyObjectLiteral(Ast.ObjectLiteralNode node, Symbol inferenceType)
    {
        if (inferenceType == null)
        {
            this.reportVerifyError(Problem.Constants.UNKNOWN_TYPE_INITIALISER, node.span);
            return null;
        }
        inferenceType = inferenceType.escapeType();

        if (node.rest != null)
        {
            this.limitType(node.rest, inferenceType);
        }

        if (inferenceType.equalsOrInstantiatedFrom(this.pool.dictionaryType))
        {
            return this.verifyObjectLiteralV2(node, inferenceType);
        }

        var validInitialiser = true;
        if (!inferenceType.isInitialisable() || !inferenceType.isConstructable())
        {
            // initialiser may be private within class
            validInitialiser = false;
            for (var frame = this.currentFrame(); frame != null; frame = frame.parentFrame())
            {
                if (frame.isClassFrame() && inferenceType.equalsOrInstantiatedFrom(frame.symbol()))
                {
                    validInitialiser = true;
                    break;
                }
            }
        }
        if (!validInitialiser)
        {
            this.reportVerifyError(Problem.Constants.CANNOT_INITIALISE_TYPE, node.span, Problem.Argument.createSymbol(inferenceType));
        }

        var fakeObj = this.pool.createValue(inferenceType);
        Symbol p = null;

        for (var field : node.fields)
        {
            var key = field.key;
            if (key instanceof Ast.SimpleIdNode)
            {
                p = this.resolveObjectProperty(fakeObj, (Ast.SimpleIdNode) key, VerifyFlags.NON_OBJECT_REFERENCE);
                if (p != null && p.isObjectProperty() && p.accessingProperty().isVariableProperty())
                {
                    p = p.accessingProperty();
                    field.semNSVariable = p;
                    this.limitType(field.value, p.staticType());
                }
                else
                {
                    if (p != null)
                    {
                        this.reportVerifyError(Problem.Constants. IDENTIFIER_IS_NOT_VARIABLE_PROPERTY, key.span);
                    }
                    this.verifyValue(field.value);
                }
            }
            else if (key instanceof Ast.StringLiteralNode)
            {
                var fakeID = new Ast.SimpleIdNode(null, ((Ast.StringLiteralNode) key).value);
                fakeID.span = key.span;
                p = this.resolveObjectProperty(fakeObj, fakeID, VerifyFlags.NON_OBJECT_REFERENCE);
                if (p != null && p.isObjectProperty() && p.accessingProperty().isVariableProperty())
                {
                    p = p.accessingProperty();
                    field.semNSVariable = p;
                    this.limitType(field.value, p.staticType());
                }
                else
                {
                    if (p != null)
                    {
                        this.reportVerifyError(Problem.Constants.IDENTIFIER_IS_NOT_VARIABLE_PROPERTY, key.span);
                    }
                    this.verifyValue(field.value);
                }
            }
            else
            {
                this.reportVerifyError(Problem.Constants.UNDEFINED_PROPERTY, key.span, Problem.Argument.createQuote(((Ast.StringLiteralNode) key).value));
                this.verifyValue(field.value);
            }
        }

        // union class is restricted to one field
        if (inferenceType.classCfgUnion())
        {
            if (node.fields.size() != 1)
            {
                this.reportVerifyError(Problem.Constants.OBJECT_LITERAL_MUST_INITIALISE_ONE_FIELD, node.span, Problem.Argument.createSymbol(inferenceType));
            }
        }
        return this.pool.createValue(inferenceType);
    }

    private Symbol verifyObjectLiteralV2(Ast.ObjectLiteralNode node, Symbol inferenceType)
    {
        var typeK = inferenceType.arguments().get(0).escapeType();
        var typeV = inferenceType.arguments().get(1);

        Ast.ExpressionNode key = null;
        Ast.ExpressionNode value = null;

        // Dictionary.<Enum,V>
        if (typeK.isEnumType())
        {
            for (var pair1 : node.fields)
            {
                key = pair1.key;
                value = pair1.value;
                if (!(key instanceof Ast.StringLiteralNode))
                {
                    this.reportVerifyError(Problem.Constants.INCOMPATIBLE_FIELD_KEY, key.span);
                }
                else
                {
                    this.limitType(key, typeK);
                }
                this.limitType(value, typeV);
            }
        }
        // Dictionary.<String,V>
        else if (typeK == this.pool.stringType)
        {
            for (var pair2 : node.fields)
            {
                key = pair2.key;
                value = pair2.value;

                if (key instanceof Ast.SimpleIdNode && ((Ast.SimpleIdNode) key).qualifier == null)
                {
                }
                else if (!(key instanceof Ast.StringLiteralNode))
                {
                    this.reportVerifyError(Problem.Constants.INCOMPATIBLE_FIELD_KEY, key.span);
                }
                this.limitType(value, typeV);
            }
        }
        // Dictionary.<NumericType,V>
        else if (this.pool.isNumericType(typeK))
        {
            for (var pair3 : node.fields)
            {
                key = pair3.key;
                value = pair3.value;

                if (!(key instanceof Ast.NumericLiteralNode))
                {
                    this.reportVerifyError(Problem.Constants.INCOMPATIBLE_FIELD_KEY, key.span);
                }
                this.limitType(value, typeV);
            }
        }
        else if (node.fields.size() != 0)
        {
            this.reportVerifyError(Problem.Constants.CANNOT_INITIALISE_DICTIONARY_OF_KEY_TYPE, node.span, Problem.Argument.createSymbol(typeK));
        }

        return this.pool.createValue(inferenceType);
    }

    private Symbol verifyArrayLiteral(Ast.ArrayLiteralNode node, Symbol inferenceType)
    {
        if (inferenceType == null)
        {
            this.reportVerifyError(Problem.Constants.UNKNOWN_TYPE_INITIALISER, node.span);
            return null;
        }
        inferenceType = inferenceType.escapeType();
        Symbol elementType = null;

        // array literal for Vector
        if (inferenceType.equalsOrInstantiatedFrom(this.pool.vectorType))
        {
            elementType = inferenceType.arguments().get(0);
            for (var el : node.elements)
            {
                if (el != null)
                {
                    this.limitType(el, elementType);
                }
            }
            if (node.rest != null)
            {
                this.limitType(node.rest, inferenceType);
            }
        }
        // array literal for tuple
        else if (inferenceType.isTupleType())
        {
            var num = inferenceType.tupleElements().size();
            for (int i = 0; i != node.elements.size(); ++i)
            {
                var el2 = node.elements.get(i);
                elementType = i < num ? inferenceType.tupleElements().get(i) : null;
                if (elementType == null)
                {
                    this.verifyValue(el2);
                }
                else
                {
                    this.limitType(el2, elementType);
                }
            }
            if (node.rest != null)
            {
                this.reportVerifyError(Problem.Constants.UNALLOWED_TUPLE_SPREAD_OPERATOR, node.rest.span);
            }
        }
        else
        {
            this.reportVerifyError(Problem.Constants.CANNOT_INITIALISE_TYPE, node.span, Problem.Argument.createSymbol(inferenceType));
        }

        return this.pool.createValue(inferenceType);
    }

    private Symbol verifyNewOperator(Ast.ExpressionNode node)
    {
        return verifyNewOperator(node, false);
    }

    /**
     * Verifies either a new operator or a call operator applied to a type.
     */
    private Symbol verifyNewOperator(Ast.ExpressionNode node, boolean optional)
    {
        var new_operator = node instanceof Ast.NewOperatorNode ? (Ast.NewOperatorNode) node : null;
        var call_operator = new_operator != null ? null : (Ast.CallNode) node;
        var base = new_operator != null ? this.verifyTypeAnnotation(new_operator.base) : this.verifyExpression(call_operator.base);
        if (base == null)
        {
            return null;
        }
        base = base.escapeType();
        var validOperator = true;

        if (!base.isConstructable())
        {
            validOperator = false;

            // constructor may be private within class
            for (var frame = this.currentFrame(); frame != null; frame = frame.parentFrame())
            {
                if (frame.isClassFrame() && base.equalsOrInstantiatedFrom(frame.symbol()))
                {
                    validOperator = true;
                    break;
                }
            }
        }
        if (!validOperator)
        {
            if (!optional)
            {
                this.reportVerifyError(Problem.Constants.CANNOT_CONSTRUCT_TYPE, node.span, Problem.Argument.createSymbol(base));
            }
            return null;
        }

        Symbol f = null;
        for (var h = base; h != null; h = h.superClass())
        {
            if ((f = h.constructorFunction()) != null)
            {
                break;
            }
        }

        if (f == null)
        {
            throw new RuntimeException("");
        }

        var arguments = call_operator == null ? new_operator.arguments : call_operator.arguments;

        // Special rule for call operator applied to type, which flexibilises
        // untyped code initialiser.
        if (optional && call_operator != null && (arguments.size() == 1 && arguments.get(0) instanceof Ast.ObjectLiteralNode) && !check1stParamIsObjectLiteralCompatible(f.signature()))
        {
            return null;
        }
        else if (optional && call_operator != null && (arguments.size() == 1 && arguments.get(0) instanceof Ast.ArrayLiteralNode) && !check1stParamIsArrayLiteralCompatible(f.signature()))
        {
            return null;
        }

        if (!this.verifyCallArguments(arguments == null ? new Vector<>() : arguments, node, f.signature(), optional))
        {
            return null;
        }

        return this.pool.createValue(base);
    }

    /**
     * Checks whether object literal can be applied to the first parameter of a function.
     * This is used by untyped code.
     */
    private boolean check1stParamIsObjectLiteralCompatible(Symbol signature)
    {
        var params = signature.params();
        if (params != null && params.get(0).isInitialisable())
            return true;
        var optParams = signature.optParams();
        if (optParams != null && optParams.get(1).isInitialisable())
            return true;
        var rest = signature.rest();
        if (rest != null && rest.arguments().get(0).isInitialisable())
            return true;
        return false;
    }

    /**
     * Checks whether array literal can be applied to the first parameter of a function.
     * This is used by untyped code.
     */
    private boolean check1stParamIsArrayLiteralCompatible(Symbol signature)
    {
        var params = signature.params();
        if (params != null && pool.isArrayType(params.get(0)))
            return true;
        var optParams = signature.optParams();
        if (optParams != null && pool.isArrayType(optParams.get(1)))
            return true;
        var rest = signature.rest();
        if (rest != null && pool.isArrayType(rest.arguments().get(0)))
            return true;
        return false;
    }

    private boolean verifyCallArguments(Vector<Ast.ExpressionNode> arguments, Ast.Node fullNode, Symbol signature)
    {
        return verifyCallArguments(arguments, fullNode, signature, false);
    }

    private boolean verifyCallArguments(Vector<Ast.ExpressionNode> arguments, Ast.Node fullNode, Symbol signature, boolean optional)
    {
        var restType = signature.rest();
        var parameters = new SignatureConsumer(signature);
        if (arguments.size() < parameters.minLength() || ((double) arguments.size()) > parameters.maxLength())
        {
            if (!optional)
                this.reportVerifyError(Problem.Constants.WRONG_NUM_ARGUMENTS, fullNode.span, Problem.Argument.createNumber((double) parameters.minLength()));
        }
        else if (((double) arguments.size()) > parameters.maxLength())
        {
            if (!optional)
                this.reportVerifyError(Problem.Constants.WRONG_NUM_ARGUMENTS, fullNode.span, Problem.Argument.createNumber(parameters.maxLength()));
        }
        int i = 0;
        for (var param : parameters)
        {
            if (i >= arguments.size())
            {
                break;
            }
            if (param == null)
            {
                break;
            }
            else if (param.position().equals("required") || param.position().equals("optional"))
            {
                if (param.type().isVerifyingType())
                {
                    this.verifyInvalidatedExpression(arguments.get(i));
                }
                else
                {
                    if (optional)
                    {
                        if (tryConverting(arguments.get(i), param.type()) == null)
                        {
                            return false;
                        }
                    }
                    else
                    {
                        this.limitType(arguments.get(i), param.type());
                    }
                }
            }
            else
            {
                if (param.type().isVerifyingType())
                {
                    this.verifyInvalidatedExpression(arguments.get(i));
                }
                else
                {
                    var restValue = this.verifyValue(arguments.get(i), param.type());
                    if (restValue != null)
                    {
                        var conv = restValue.implicitConversion(restType);
                        arguments.get(i).semNSResult = conv;
                        if (conv == null)
                        {
                            arguments.get(i).semNSResult = restValue;
                            if (optional)
                            {
                                if (this.tryConverting(arguments.get(i), param.type()) == null)
                                {
                                    return false;
                                }
                            }
                            else
                            {
                                this.limitType(arguments.get(i), param.type());
                            }
                        }
                    }
                }
            }
            ++i;
        }
        for (; i < arguments.size(); ++i)
        {
            this.verifyInvalidatedExpression(arguments.get(i));
        }
        return true;
    }

    private Symbol verifyFunctionExpression(Ast.FunctionExpressionNode node, Symbol inferenceType)
    {
        var name = node.name != "" && node.name != null ? this.pool.createName(this.currentFrame().searchInternalNamespace(), node.name) : null;
        Symbol signature = null;
        var common = node.common;

        var frame = this.pool.createParameterFrame(null);
        var activation = new Activation(frame);
        node.common.semNSFrame = frame;
        frame.setActivation(activation);

        signature = this.resolveSignature(node.common, Span.pointer(node.span.firstLine(), node.span.start()));
        this.prepareFunctionActivation(node.common, signature);

        var f = this.pool.createFunction(name, signature);
        f.setActivation(activation);

        if (name != null)
        {
            activation.frame().ownNames().defineName(name, f);
        }
        this.enterFunction(f, node.common);
        this.verifyFunctionBody(node.common, f, Span.pointer(node.span.firstLine(), node.span.start()));
        this.exitFunction();
        node.semNSFunction = f;
        node.semNSFrame = activation.frame();
        return this.pool.createValue(this.pool.functionType);
    }

    private Symbol verifyBracketsOperator(Ast.BracketsNode node)
    {
        var obj = this.verifyValue(node.base);
        if (obj == null)
        {
            this.verifyInvalidatedExpression(node.key);
            return null;
        }

        if (obj.staticType().escapeType().isTupleType())
        {
            var tupleType = obj.staticType().escapeType();
            var value = this.limitConstantType(node.key, this.pool.uintType);
            if (value == null)
            {
                return null;
            }
            var i = value.uintValue().longValue();
            if (i >= tupleType.tupleElements().size())
            {
                this.reportVerifyError(Problem.Constants.MAX_TUPLE_LENGTH_REACHED, node.key.span, Problem.Argument.createNumber((double) i));
                return null;
            }
            return this.pool.createTupleElement(obj, (int) i);
        }
        else
        {
            var dp = obj.accessProxyProperty();
            if (dp == null)
            {
                this.reportVerifyError(Problem.Constants.UNDEFINED_DYNAMIC_PROPERTY, node.span, Problem.Argument.createSymbol(obj.staticType()));
                this.verifyInvalidatedExpression(node.key);
                return null;
            }
            this.limitType(node.key, dp.accessingTrait().keyType());
            return dp;
        }
    }

    private Symbol verifyFilterOperator(Ast.FilterNode node)
    {
        var obj = this.verifyValue(node.base);
        if (obj == null)
        {
            return null;
        }
        var filter = obj.objectFilter();
        if (filter == null)
        {
            this.reportVerifyError(Problem.Constants.UNSUPPORTED_FILTER, node.span, Problem.Argument.createSymbol(obj.staticType()));
            return null;
        }
        var proxy_signature = filter.accessingProperty().signature();
        var frame = this.pool.createWithFrame(proxy_signature.params().firstElement());
        var activation = new Activation(frame);
        frame.setActivation(activation);
        node.semNSActivation = activation;
        this.enterActivation(activation);
        this.enterFrame(frame);
        this.verifyValue(node.expression);
        this.exitFrame();
        this.exitActivation();

        return filter;
    }

    private Symbol verifyDescendantsOperator(Ast.DescendantsNode node)
    {
        var obj = this.verifyValue(node.base);
        if (obj == null)
        {
            return null;
        }
        var desc = obj.objectDescendants();
        if (desc == null)
        {
            this.reportVerifyError(Problem.Constants.UNSUPPORTED_DESCENDANTS, node.span, Problem.Argument.createSymbol(obj.staticType()));
            return null;
        }
        if (node.id.qualifier != null)
        {
            this.limitType(node.id.qualifier, this.pool.namespaceType);
        }
        if (node.id instanceof Ast.ExpressionIdNode)
        {
            this.limitType(((Ast.ExpressionIdNode) node.id).key, this.pool.stringType);
        }
        return desc;
    }

    private Symbol verifyUnaryOperator(Ast.UnaryOperatorNode node, Symbol inferenceType)
    {
        if (node.type == Operator.YIELD)
        {
            var s = this.currentFunction.signature();
            var obj = this.limitType(node.argument, s.result().arguments().firstElement());
            if (obj == null)
            {
                return null;
            }
            return this.pool.createValue(obj.staticType());
        }

        var obj = this.verifyValue(node.argument, inferenceType, node.type == Operator.DELETE ? VerifyFlags.OBJECT_DELETE : 0);
        if (obj == null)
        {
            return null;
        }

        if (node.type == Operator.LOGICAL_NOT)
        {
            return this.pool.createValue(this.pool.booleanType);
        }
        if (node.type == Operator.DELETE)
        {
            return this.verifyDeleteOperator(node, obj);
        }
        if (node.type == Operator.INCREMENT || node.type == Operator.DECREMENT
        ||  node.type == Operator.POST_INCREMENT || node.type == Operator.POST_DECREMENT
        ||  node.type == Operator.POSITIVE)
        {
            if (!this.pool.isNumericType(obj.staticType()))
            {
                this.reportVerifyError(Problem.Constants.VALUE_IS_NOT_NUMERIC, node.span);
            }
            if (node.type != Operator.POSITIVE)
            {
                checkMutatedVariable(obj);

                if (obj.readOnly())
                {
                    this.reportVerifyError(Problem.Constants.REFERENCE_IS_READ_ONLY, node.argument.span);
                }
            }
            return this.pool.createValue(obj.staticType());
        }
        if (node.type == Operator.TYPEOF)
        {
            return this.pool.createValue(this.pool.stringType);
        }
        if (node.type == Operator.VOID)
        {
            return this.pool.createUndefinedConstantValue();
        }

        obj = this.limitType(node.argument, obj.staticType().escapeType());
        if (obj.staticType().isNumericTypeParameter())
        {
            return this.pool.createValue(obj.staticType());
        }
        var proxy = obj.staticType().delegate() != null ? obj.staticType().delegate().searchOperator(node.type) : null;
        if (proxy == null)
        {
            this.reportVerifyError(Problem.Constants.UNSUPPORTED_OPERATOR, node.span, Problem.Argument.createSymbol(obj.staticType()), Problem.Argument.createQuote( node.type.id() ));
            return null;
        }
        return this.pool.createValue(proxy.signature().result());
    }

    private void checkMutatedVariable(Symbol symbol)
    {
        if (symbol.isVariableProperty())
        {
            symbol.setReassigned(true);
        }
        else if ((symbol.isObjectProperty() || symbol.isFrameProperty() || symbol.isPackageProperty() || symbol.isTypeProperty()) && symbol.accessingProperty().isVariableProperty())
        {
            symbol.accessingProperty().setReassigned(true);
        }
    }

    private Symbol verifyDeleteOperator(Ast.UnaryOperatorNode node, Symbol access)
    {
        var dp_or_attrib =
            access.isObjectProxyProperty()
        ||  access.isObjectAttribute();

        if (!dp_or_attrib || !access.deletable())
        {
            this.reportVerifyError(Problem.Constants.UNSUPPORTED_DELETE_OPERATOR, node.span, Problem.Argument.createSymbol(access.staticType()));
        }

        return this.pool.createValue(this.pool.booleanType);
    }

    private Symbol verifyTypeOperator(Ast.TypeOperatorNode node)
    {
        var value = this.verifyValue(node.left);
        var type = this.verifyTypeAnnotation(node.pattern == null ? node.right : node.pattern.type);
        if (type == null)
        {
            return null;
        }
        type = type.escapeType();
        switch (node.operator)
        {
            case "as":
            {
                if (value == null)
                {
                    return this.pool.createValue(type);
                }
                var conv = value.explicitConversion(type);
                if (conv == null)
                {
                    this.reportVerifyError(Problem.Constants.INCOMPATIBLE_TYPES, node.span, Problem.Argument.createSymbol(value.staticType()), Problem.Argument.createSymbol(type));
                }
                return conv != null ? pool.createAsConversion(conv) : null;
            }
            case "instanceof":
            {
                if (!type.isClassType())
                {
                    this.reportVerifyError(Problem.Constants.TYPE_IS_NOT_CLASS, node.pattern == null ? node.right.span : node.pattern.type.span);
                }
                break;
            }
        }

        if (node.pattern != null)
        {
            node.semNSBindingFrame = pool.createConditionFrame();
            node.semNSBindingFrame.setActivation(currentActivation);
            enterFrame(node.semNSBindingFrame);
            phase2DeclarePattern(node.pattern, false, currentFrame().searchInternalNamespace(), node.semNSBindingFrame.ownNames());
            phase4DeclarePattern(node.pattern, pool.createValue(type));
            exitFrame();
        }

        // is
        return this.pool.createValue(this.pool.booleanType);
    }

    private Symbol verifyBinaryOperator(Ast.BinaryOperatorNode node, Symbol inferenceType)
    {
        Symbol left = null;
        Symbol right = null;
        Symbol operandType = null;
        Vector<Symbol> bindings = null;

        if (inferenceType != null && inferenceType.isVerifyingType())
        {
            this.verifyInvalidatedExpression(node.left);
            this.verifyInvalidatedExpression(node.right);
            return null;
        }
        if (node.type == Operator.LOGICAL_AND || node.type == Operator.LOGICAL_OR)
        {
            left = this.verifyValue(node.left, inferenceType);
            if (left != null && (bindings = node.left.retrieveBindings()) != null)
            {
                node.semNSRightFrame = pool.createConditionFrame();
                node.semNSRightFrame.setActivation(currentActivation);
                for (var b : bindings)
                {
                    node.semNSRightFrame.ownNames().defineName(b.accessingProperty().name(), b);
                }
                enterFrame(node.semNSRightFrame);
            }

            right = this.verifyValue(node.right, left != null ? left.staticType() : inferenceType);

            if (node.semNSRightFrame != null)
            {
                exitFrame();
            }

            if (left == null || right == null)
            {
                return null;
            }
            operandType = left.staticType();

            // if operands are incompatible, logical and/or (&&, ||)
            // results into Boolean

            var conv = right.implicitConversion(operandType);
            node.right.semNSResult = conv != null ? conv : right;
            return conv == null ? this.pool.createBooleanLogicalAndOr() : this.pool.createValue(operandType);
        }
        if (node.type == Operator.EQUALS || node.type == Operator.NOT_EQUALS)
        {
            left = this.verifyValue(node.left, inferenceType);
            right = this.verifyValue(node.right, left != null ? left.staticType() : inferenceType);
            if (left == null || right == null)
            {
                return null;
            }
            operandType = left.staticType();

            if (right.staticType().containsUndefined() && !operandType.containsUndefined() && right.escapeType().staticType() == operandType)
            {
                this.limitType(node.left, right.staticType());
            }
            else
            {
                this.limitType(node.right, operandType);
            }
            return this.pool.createValue(this.pool.booleanType);
        }
        if (node.type == Operator.IN)
        {
            right = this.verifyValue(node.right);
            if (right == null)
            {
                this.verifyValue(node.left);
                return this.pool.createValue(this.pool.booleanType);
            }
            var in_proxy = this.pool.validateHasPropertyProxy(right.staticType().delegate() != null ? right.staticType().delegate().lookupName(this.pool.proxyHasPropertyName) : null);
            if (in_proxy == null)
            {
                this.reportVerifyError(Problem.Constants.UNSUPPORTED_IN_OPERATOR, node.span, Problem.Argument.createSymbol(right.staticType()));
                return null;
            }
            this.limitType(node.left, in_proxy.signature().params().firstElement());
            return this.pool.createValue(this.pool.booleanType);
        }

        left = this.verifyValue(node.left, inferenceType);

        if (left == null)
        {
            this.verifyValue(node.right);
            return null;
        }

        operandType = left.staticType();
        this.limitType(node.right, operandType);

        if (operandType.isNumericTypeParameter())
        {
            return this.pool.createValue(node.type.resultsBoolean() ? pool.booleanType : operandType);
        }

        var proxy = operandType.delegate() != null ? operandType.delegate().searchOperator(node.type) : null;
        if (proxy == null)
        {
            this.reportVerifyError(Problem.Constants.UNSUPPORTED_OPERATOR, node.span, Problem.Argument.createSymbol(operandType), Problem.Argument.createQuote(node.type.id()));
            return null;
        }
        return this.pool.createValue(proxy.signature().result());
    }

    private Symbol verifyTernaryOperator(Ast.TernaryNode node, Symbol inferenceType)
    {
        this.verifyValue(node.expression1);
        if (inferenceType != null && inferenceType.isVerifyingType())
        {
            this.verifyInvalidatedExpression(node.expression2);
            this.verifyInvalidatedExpression(node.expression3);
            return null;
        }

        var bindings = node.expression1.retrieveBindings();

        if (bindings != null)
        {
            node.semNSConsequentFrame = pool.createConditionFrame();
            node.semNSConsequentFrame.setActivation(currentActivation);
            for (var b : bindings)
            {
                node.semNSConsequentFrame.ownNames().defineName(b.accessingProperty().name(), b);
            }
            enterFrame(node.semNSConsequentFrame);
        }

        var result1 = this.verifyValue(node.expression2, inferenceType);

        if (node.semNSConsequentFrame != null)
        {
            exitFrame();
        }

        var result2 = this.verifyValue(node.expression3, inferenceType);

        if (result1 != null && result2 != null)
        {
            if (result1.implicitConversion(result2.staticType()) != null)
            {
                this.limitType(node.expression2, result2.staticType());
                return this.pool.createValue(result2.staticType());
            }
            else if (result2.implicitConversion(result1.staticType()) != null)
            {
                this.limitType(node.expression3, result1.staticType());
                return this.pool.createValue(result1.staticType());
            }
            else if (inferenceType != null)
            {
                this.limitType(node.expression2, inferenceType);
                this.limitType(node.expression3, inferenceType);
                return this.pool.createValue(inferenceType);
            }
            else
            {
                this.reportVerifyError(Problem.Constants.TERNARY_HAS_NO_RESULT_TYPE, node.span);
                return null;
            }
        }
        else
        {
            return null;
        }
    }

    private Symbol verifyAssignmentOperator(Ast.AssignmentNode node, int flags)
    {
        var left = this.verifyValue(node.left, null, VerifyFlags.ASSIGNMENT_LEFT_HAND_SIDE);
        if (left == null)
        {
            this.verifyInvalidatedExpression(node.right);
            return null;
        }

        checkMutatedVariable(left);

        var operandType = left.staticType();
        this.limitType(node.right, operandType);

        if (node.compound != null && node.compound != Operator.LOGICAL_AND && node.compound != Operator.LOGICAL_XOR && node.compound != Operator.LOGICAL_OR)
        {
            var proxy = operandType.delegate() != null ? operandType.delegate().searchOperator(node.compound) : null;

            if (proxy == null)
            {
                this.reportVerifyError(Problem.Constants.UNSUPPORTED_OPERATOR, node.span, Problem.Argument.createSymbol(operandType), Problem.Argument.createQuote(node.compound.id()));
            }
        }

        return this.pool.createValue(operandType);
    }

    private void verifyXMLList(Ast.XMLListNode node)
    {
        for (var el : node.nodes)
        {
            this.verifyXMLLiteral(el);
        }
    }

    private void verifyXMLLiteral(Ast.ExpressionNode node)
    {
        if (node instanceof Ast.XMLElementNode)
        {
            var element = (Ast.XMLElementNode) node;
            if (element.openName instanceof Ast.ExpressionNode)
            {
                this.verifyValue((Ast.ExpressionNode) element.openName, this.pool.stringType);
            }
            if (element.closeName instanceof Ast.ExpressionNode)
            {
                this.verifyValue((Ast.ExpressionNode) element.closeName, this.pool.stringType);
            }
            if (element.attributes != null)
            {
                for (var attrib : element.attributes)
                {
                    if (attrib.value instanceof Ast.ExpressionNode)
                    {
                        this.verifyValue((Ast.ExpressionNode) attrib.value, this.pool.stringType);
                    }
                }
            }
            if (element.childNodes != null)
            {
                for (var child : element.childNodes)
                {
                    this.verifyXMLLiteral(child);
                }
            }
        }
        else if (node instanceof Ast.XMLTextNode)
        {
            var txt = (Ast.XMLTextNode) node;
            if (txt.content instanceof Ast.ExpressionNode)
            {
                this.verifyValue((Ast.ExpressionNode) txt.content, this.pool.stringType);
            }
        }
    }

    private Symbol resolveSignature(Ast.FunctionCommonNode common, Span functionPointer)
    {
        return resolveSignature(common, functionPointer, null);
    }

    private Symbol resolveSignature(Ast.FunctionCommonNode common, Span functionPointer, Symbol baseSignature)
    {
        return resolveSignature(common, functionPointer, baseSignature, null);
    }

    private Symbol resolveSignature(Ast.FunctionCommonNode common, Span functionPointer, Symbol baseSignature, Symbol result)
    {
        if (baseSignature != null)
        {
            return this._resolveSignature2(common, functionPointer, baseSignature);
        }
    	
        enterFrame(common.semNSFrame);

        Vector<Symbol> params = null;
        Vector<Symbol> optParams = null;
        Symbol rest = null;
        Symbol type = null;

        if (common.params != null)
        {
            params = new Vector<>();
            for (var p : common.params)
            {
                if (p.type != null)
                {
                    type = this.verifyTypeAnnotation(p.type);
                    params.add(type != null ? type : this.pool.verifyingType);
                }
                else
                {
                    this.reportVerifyError(Problem.Constants.UNTYPED_PARAMETER, p.span);
                    params.add(this.pool.verifyingType);
                }
            }
        }

        if (common.optParams != null)
        {
            optParams = new Vector<>();
            for (var p2 : common.optParams)
            {
                if (p2.pattern.type != null)
                {
                    type = this.verifyTypeAnnotation(p2.pattern.type);
                    optParams.add(type != null ? type : this.pool.verifyingType);
                }
                else
                {
                    this.reportVerifyError(Problem.Constants.UNTYPED_PARAMETER, p2.span);
                    optParams.add(this.pool.verifyingType);
                }
            }
        }

        if (common.rest != null)
        {
            if (common.rest.type != null)
            {
                rest = this.verifyTypeAnnotation(common.rest.type);
                rest = rest == null ? pool.verifyingType : rest;
                if (!rest.equalsOrInstantiatedFrom(this.pool.vectorType))
                {
                    rest = this.pool.createInstantiatedType(this.pool.vectorType, new Symbol[] {rest});
                }
            }
            else
            {
                this.reportVerifyError(Problem.Constants.UNTYPED_PARAMETER, common.rest.span);
                rest = this.pool.createInstantiatedType(this.pool.vectorType, new Symbol[] {this.pool.verifyingType});
            }
        }

        if (common.result != null)
        {
            result = this.verifyTypeAnnotation(common.result);
            result = result == null ? pool.verifyingType : result;
        }
        else if (common.markConstructor())
        {
            result = this.pool.voidType;
        }
        else if (result == null)
        {
            this.reportVerifyError(Problem.Constants.UNTYPED_RESULT, functionPointer);
            result = this.pool.verifyingType;
        }

        if (common.markYielding())
        {
            result = this.pool.createInstantiatedType(this.pool.generatorType, new Symbol[] {result});
        }

        exitFrame();

        return this.pool.createFunctionType(params, optParams, rest, result);
    }

    /*
     * Verifies signature bound to another inferred signature.
     */
    private Symbol _resolveSignature2(Ast.FunctionCommonNode common, Span functionPointer, Symbol baseSignature)
    {
        enterFrame(common.semNSFrame);

        var invalidated = false;
        int i = 0;
        int j = 0;
        var paramNodes = common.params;
        var optParamNodes = common.optParams;
        var restNode = common.rest;
        Symbol type = null;

        p: for (;;)
        {
            if (paramNodes != null)
            {
                if (baseSignature.params() == null || paramNodes.size() > baseSignature.params().size())
                {
                    invalidated = true;
                    break p;
                }
                for (var p : paramNodes)
                {
                    if (p.type != null)
                    {
                        type = this.verifyTypeAnnotation(p.type);
                        if (type != null && type != baseSignature.params().get(i))
                        {
                            invalidated = true;
                            break p;
                        }
                    }
                }
                paramNodes = null;
            }
            else if (optParamNodes != null)
            {
                if (baseSignature.optParams() == null || optParamNodes.size() != baseSignature.optParams().size())
                {
                    invalidated = true;
                    break p;
                }
                for (var p2 : optParamNodes)
                {
                    if (p2.pattern.type != null)
                    {
                        type = this.verifyTypeAnnotation(p2.pattern.type);
                        if (type != null && type != baseSignature.optParams().get(i))
                        {
                            invalidated = true;
                            break p;
                        }
                    }
                }
                optParamNodes = null;
            }
            else if (restNode != null)
            {
                if (baseSignature.rest() == null)
                {
                    invalidated = true;
                    break;
                }
                type = restNode.type != null ? this.verifyTypeAnnotation(restNode.type) : null;
                if (type != null && type != baseSignature.rest())
                {
                    invalidated = true;
                }
                restNode = null;
                break;
            }
            else
            {
                break;
            }
        }
        if (common.result != null)
        {
            type = this.verifyTypeAnnotation(common.result);
            if (type != null && type != baseSignature.result())
            {
                invalidated = true;
            }
        }
        if (invalidated)
        {
            this.reportVerifyError(Problem.Constants.FUNCTION_SIGNATURE_MUST_MATCH, functionPointer, Problem.Argument.createSymbol(baseSignature));
        }

        exitFrame();
        return baseSignature;
    }

    private Activation prepareFunctionActivation(Ast.FunctionCommonNode common, Symbol signature)
    {
        return prepareFunctionActivation(common, signature, null);
    }

    private Activation prepareFunctionActivation(Ast.FunctionCommonNode common, Symbol signature, Symbol parameterThis)
    {
        var frame = common.semNSFrame != null ? common.semNSFrame : this.pool.createParameterFrame(parameterThis);
        common.semNSFrame = frame;
        var activation = frame.activation();
        activation = activation == null ? new Activation(frame) : activation;
        frame.setActivation(activation);

        if (frame.parameterThis() != null)
        {
            frame.parameterThis().setActivation(activation);
        }

        var internalNS = this.scopeChain.currentFrame().searchInternalNamespace();
        this.enterActivation(activation);
        this.enterFrame(frame);
        int i = 0;
        Symbol type = null;
        {
            // activation required parameters
            if (common.params != null)
            {
                for (i = 0; i != common.params.size(); ++i)
                {
                    var p = common.params.get(i);
                    type = signature.params() != null && i < signature.params().size() ? signature.params().get(i) : this.pool.verifyingType;
                    this.phase2DeclarePattern(p, false, internalNS, frame.ownNames());
                    this.phase4DeclarePattern(p, this.pool.createValue(type));
                }
            }
            // activation optional parameters
            if (common.optParams != null)
            {
                for (i = 0; i != common.optParams.size(); ++i)
                {
                    var binding = common.optParams.get(i);
                    type = signature.optParams() != null && i < signature.optParams().size() ? signature.optParams().get(i) : this.pool.verifyingType;
                    this.phase2DeclarePattern(binding.pattern, false, internalNS, frame.ownNames());
                    this.phase4DeclarePattern(binding.pattern, this.pool.createValue(type));

                    if (binding.initialiser != null)
                    {
                        var constant = this.verifyConstantValue(binding.initialiser, type);
                        if (constant != null)
                        {
                            binding.pattern.semNSVariable.setInitialValue(constant);
                        }
                    }
                }
            }
            // activation rest parameter
            if (common.rest != null)
            {
                var name = this.pool.createName(internalNS, common.rest.name);
                type = signature.rest() != null ? signature.rest() : this.pool.verifyingType;
                var variable = this.pool.createVariableProperty(name, false, type);
                common.rest.semNSVariable = variable;

                if (frame.ownNames().hasName(name))
                {
                    this.reportVerifyError(Problem.Constants.NAMESPACE_CONFLICT, common.rest.span, Problem.Argument.createQuote(name.namespace().toString()));
                }
                else
                {
                    frame.ownNames().defineName(name, variable);
                }
            }
        }
        this.exitFrame();
        this.exitActivation();
        return activation;
    }

    private void verifyFunctionBody(Ast.FunctionCommonNode common, Symbol fSymbol, Span functionPointer)
    {
        var s = fSymbol.signature();
        fSymbol.setActivation(common.semNSFrame.activation());
        this.enterFrame(common.semNSFrame);

        if (common.body instanceof Ast.BlockNode)
        {
            var block = (Ast.BlockNode) common.body;
            this.verifyStatement(block);

            // any result type other than *, void or Generator (from yield operator)
            // must be explicitly returned from the function
            var r = s.result();
            if (!common.markYielding() && !r.isVoidType() && !r.isAnyType() && !r.isVerifyingType())
            {
                if (!this.verifyReturnPath(block))
                {
                    this.reportVerifyError(Problem.Constants.NOT_ALL_PATHS_RETURN, functionPointer);
                }
            }
        }
        else
        {
            if (common.markYielding())
            {
                this.verifyValue((Ast.ExpressionNode) common.body);
            }
            else
            {
                this.limitType((Ast.ExpressionNode) common.body, s.result());
            }
        }
        this.exitFrame();
    }

    private boolean verifyReturnPath(Ast.DirectiveNode drtv)
    {
        Vector<Ast.DirectiveNode> directives = null;

        if (drtv instanceof Ast.BlockNode block)
        {
            directives = block.directives;
            if (directives.size() == 0)
            {
                return false;
            }
            for (int i = 0; i != directives.size(); ++i)
            {
                var subdrtv = directives.get(i);
                if (this.verifyReturnPath(subdrtv))
                {
                    return true;
                }
            }
            return false;
        }
        else if (drtv instanceof Ast.ReturnNode
            ||   drtv instanceof Ast.ThrowNode)
        {
            return true;
        }
        else if (drtv instanceof Ast.TryStatementNode trystmt)
        {
            if (!this.verifyReturnPath(trystmt.block))
            {
                return false;
            }
            if (trystmt.finallyBlock != null)
            {
                return this.verifyReturnPath(trystmt.finallyBlock);
            }
            for (var catchnode : trystmt.catchNodes)
            {
                if (!this.verifyReturnPath(catchnode.block))
                {
                    return false;
                }
            }
            return true;
        }
        else if (drtv instanceof Ast.IfStatementNode ifstmt)
        {
            return this.verifyReturnPath(ifstmt.consequent)
                && (ifstmt.alternative != null ? this.verifyReturnPath(ifstmt.alternative) : true);
        }
        else if (drtv instanceof Ast.SwitchStatementNode switchstmt)
        {
            for (var casenode : switchstmt.caseNodes)
            {
                if (casenode.expression == null && casenode.directives != null && casenode.directives.size() != 0 && this.verifyReturnPath(casenode.directives.lastElement()))
                {
                    return true;
                }
            }
        }
        else if (drtv instanceof Ast.VarStatementNode varstmt)
            return verifyReturnPath(varstmt.substatement);
        return false;
    }

    private void phase2DeclarePattern(Ast.PatternNode pattern, boolean readOnly, Symbol qual, Names intoNames)
    {
        Symbol name = null;
        Symbol variable = null;
        Symbol k = null;

        if (pattern instanceof Ast.NamePatternNode)
        {
            var name_pattern = (Ast.NamePatternNode) pattern;
            name = this.pool.createName(qual, name_pattern.name);
            variable = this.pool.createVariableProperty(name, readOnly, null);
            pattern.semNSVariable = variable;
            k = intoNames.lookupName(name);
            if (k != null)
            {
                if (k.isVariableProperty() && this.allowDuplicates)
                {
                    pattern.semNSVariable = k;
                }
                else
                {
                    this.reportVerifyError(Problem.Constants.NAMESPACE_CONFLICT, pattern.span, Problem.Argument.createQuote(qual.toString()));
                }
            }
            else
            {
                intoNames.defineName(name, variable);
            }
        }
        else if (pattern instanceof Ast.ObjectPatternNode)
        {
            var obj_pattern = (Ast.ObjectPatternNode) pattern;
            pattern.semNSVariable = this.pool.createVariableProperty(null, true, null);
            for (var field : obj_pattern.fields)
            {
                if (field.subpattern != null)
                {
                    this.phase2DeclarePattern(field.subpattern, readOnly, qual, intoNames);
                }
                else
                {
                    var id = field.id;
                    name = this.pool.createName(qual, id.name);
                    variable = this.pool.createVariableProperty(name, readOnly, null);
                    id.semNSResult = variable;

                    if (intoNames.hasName(name))
                    {
                        this.reportVerifyError(Problem.Constants.NAMESPACE_CONFLICT, field.span, Problem.Argument.createQuote(qual.toString()));
                    }
                    else
                    {
                        intoNames.defineName(name, variable);
                    }
                }
            }
        }
        else
        {
            var array_pattern = (Ast.ArrayPatternNode) pattern;
            pattern.semNSVariable = this.pool.createVariableProperty(null, true, null);
            for (var el : array_pattern.elements)
            {
                if (el != null)
                {
                    this.phase2DeclarePattern(el, readOnly, qual, intoNames);
                }
            }
            if (array_pattern.rest != null)
            {
                this.phase2DeclarePattern(array_pattern.rest, readOnly, qual, intoNames);
            }
        }
    }

    private void phase4DeclarePattern(Ast.PatternNode pattern, Symbol value)
    {
        Symbol annotatedType = null;
        if (pattern.type != null)
        {
            annotatedType = this.verifyTypeAnnotation(pattern.type);
        }
        pattern.semNSVariable.setStaticType(value.staticType());

        // name pattern
        if (pattern instanceof Ast.NamePatternNode)
        {
        }
        // object pattern
        else if (pattern instanceof Ast.ObjectPatternNode)
        {
            for (var field : ((Ast.ObjectPatternNode) pattern).fields)
            {
                var id = field.id;
                var variable = field.subpattern != null ? null : id.semNSResult;

                var fv = field.semNSExtracted =
                    value.staticType().isVerifyingType() ? value : this.resolveObjectProperty(value, id);
                fv = fv == null ? this.pool.createValue(this.pool.verifyingType) : fv;

                if (variable != null)
                {
                    variable.setStaticType(fv.staticType());
                }
                else
                {
                    this.phase4DeclarePattern(field.subpattern, this.pool.createValue(this.pool.verifyingType));
                }
            }
        }
        // array patern
        else
        {
            this._phase4DeclareArrayPattern((Ast.ArrayPatternNode) pattern, value);
        }

        if (annotatedType != null && !pattern.semNSVariable.staticType().isVerifyingType() && pattern.semNSVariable.staticType() != annotatedType)
        {
            this.reportVerifyError(Problem.Constants.EXPECTED_TYPE, pattern.span, Problem.Argument.createSymbol(annotatedType));
        }
    }

    private void _phase4DeclareArrayPattern(Ast.ArrayPatternNode pattern, Symbol value)
    {
        pattern.semNSVariable.setStaticType(value.staticType());
        var arrayType = value.staticType().escapeType();
        Symbol type = null;
        ProxyPropertyTrait dptrait = null;

        // array pattern for tuple
        if (arrayType.isTupleType())
        {
            var tuple_elements = arrayType.tupleElements();
            if (pattern.elements.size() > tuple_elements.size())
            {
                this.reportVerifyError(Problem.Constants.MAX_TUPLE_LENGTH_REACHED, pattern.span, Problem.Argument.createNumber(pattern.elements.size()));
            }
            for (int i = 0; i != pattern.elements.size(); ++i)
            {
                var subpattern = pattern.elements.get(i);
                if (subpattern != null)
                {
                    type = i < tuple_elements.size() ? tuple_elements.get(i) : this.pool.verifyingType;
                    this.phase4DeclarePattern(subpattern, this.pool.createValue(type));
                }
            }
            if (pattern.rest != null)
            {
                this.reportVerifyError(Problem.Constants.UNALLOWED_TUPLE_SPREAD_OPERATOR, pattern.rest.span);
                this.phase4DeclarePattern(pattern.rest, this.pool.createValue(arrayType));
            }
        }
        // array pattern for Array-based type
        else if (arrayType.delegate() != null && (dptrait = arrayType.delegate().searchProxyPropertyTrait()) != null && pool.isNumericType(dptrait.keyType()))
        {
            var elementType = dptrait.valueType();
            for (var el2 : pattern.elements)
            {
                if (el2 != null)
                {
                    this.phase4DeclarePattern(el2, this.pool.createValue(elementType));
                }
            }
            if (pattern.rest != null)
            {
                this.phase4DeclarePattern(pattern.rest, this.pool.createValue(arrayType));
            }
        }
        else
        {
            if (!arrayType.isVerifyingType())
            {
                this.reportVerifyError(Problem.Constants.UNSUPPORTED_ARRAY_PATTERN, pattern.span, Problem.Argument.createSymbol(arrayType));
            }
            for (var el3 : pattern.elements)
            {
                if (el3 != null)
                {
                    this.phase4DeclarePattern(el3, this.pool.createValue(this.pool.verifyingType));
                }
            }
            if (pattern.rest != null)
            {
                this.phase4DeclarePattern(pattern.rest, this.pool.createValue(this.pool.verifyingType));
            }
        }
    }

    public void verifyPrograms(Vector<Ast.ProgramNode> programs)
    {
        Symbol p = null;
        Symbol packageFrame = null;

        var dv = new DirectiveVerifier();

        for (var program : programs)
        {
            for (var packageNode : program.packages)
            {
                this.enterScript(packageNode.script);
                p = this.pool.createPackage(packageNode.id);
                packageFrame = this.pool.createPackageFrame(p);
                packageFrame.setDefaultNamespace(p.publicNamespace());
                packageNode.semNSSymbol = p;
                packageNode.block.semNSFrame = packageFrame;
                this.enterFrame(packageNode.block.semNSFrame);
                dv.verify(packageNode.block.directives);
                this.exitFrame();
                this.exitScript();
            }
        }

        dv.incrementPhase();

        while (dv.hasRemaining())
        {
            for (var program : programs)
            {
                for (var packageNode : program.packages)
                {
                    this.enterScript(packageNode.script);
                    this.enterFrame(packageNode.block.semNSFrame);
                    dv.verify(packageNode.block.directives);
                    this.exitFrame();
                    this.exitScript();
                }
            }
            dv.incrementPhase();
        }

        for (var program : programs)
        {
            if (program.directives != null)
            {
                this.enterScript(program.script);

                var frame = program.semNSFrame = this.pool.createBlockFrame();
                frame.setInternalNamespace(this.pool.createReservedNamespace("internal"));
                frame.setDefaultNamespace(frame.internalNamespace());
                frame.openNamespace(frame.internalNamespace());

                var activation = program.semNSActivation = new Activation(frame);
                frame.setActivation(activation);
                this.enterActivation(activation);
                this.enterFrame(frame);
                this.verifyDirectives(program.directives);
                this.exitFrame();
                this.exitActivation();

                this.exitScript();
            }
        }
    }

    private void verifyStatement(Ast.StatementNode stmt)
    {
        Vector<Symbol> bindings = null;

        if (stmt instanceof Ast.ExpressionStatementNode)
        {
            var expr_stmt = (Ast.ExpressionStatementNode) stmt;
            this.verifyValue(expr_stmt.expression);
        }
        else if (stmt.isIterationStatement())
        {
            if (stmt instanceof Ast.ForStatementNode)
            {
                this.verifyForStatement((Ast.ForStatementNode) stmt);
            }
            else if (stmt instanceof Ast.ForInStatementNode)
            {
                this.verifyForInStatement((Ast.ForInStatementNode) stmt);
            }
            else if (stmt instanceof Ast.WhileStatementNode)
            {
                var while_stmt = (Ast.WhileStatementNode) stmt;

                bindings = while_stmt.expression.retrieveBindings();

                if (bindings != null)
                {
                    while_stmt.semNSConsequentFrame = pool.createConditionFrame();
                    while_stmt.semNSConsequentFrame.setActivation(currentActivation);
                    for (var b : bindings)
                    {
                        while_stmt.semNSConsequentFrame.ownNames().defineName(b.accessingProperty().name(), b);
                    }
                    enterFrame(while_stmt.semNSConsequentFrame);
                }

                this.verifyExpression(while_stmt.expression);

                if (while_stmt.semNSConsequentFrame != null)
                {
                    exitFrame();
                }

                this.verifyStatement(while_stmt.substatement);
            }
            else if (stmt instanceof Ast.DoStatementNode)
            {
                var do_stmt = (Ast.DoStatementNode) stmt;
                this.verifyStatement(do_stmt.substatement);
                this.verifyExpression(do_stmt.expression);
            }
        }
        else if (stmt instanceof Ast.BlockNode)
        {
            var block = (Ast.BlockNode) stmt;
            var block_frame = block.semNSFrame = this.pool.createBlockFrame();
            block_frame.setDefaultNamespace(this.currentFrame().searchInternalNamespace());
            block_frame.setActivation(this.currentActivation);
            this.enterFrame(block_frame);
            this.verifyDirectives(block.directives);
            this.exitFrame();
        }
        else if (stmt instanceof Ast.BreakNode)
        {
        }
        else if (stmt instanceof Ast.ContinueNode)
        {
        }
        else if (stmt instanceof Ast.DXNSStatementNode)
        {
            var dxns_stmt = (Ast.DXNSStatementNode) stmt;
            this.limitType(dxns_stmt.expression, this.pool.namespaceType);
        }
        else if (stmt instanceof Ast.EmptyStatementNode)
        {
        }
        else if (stmt instanceof Ast.IfStatementNode)
        {
            var if_stmt = (Ast.IfStatementNode) stmt;
            this.verifyValue(if_stmt.expression);

            bindings = if_stmt.expression.retrieveBindings();
            if (bindings != null)
            {
                if_stmt.semNSConsequentFrame = pool.createConditionFrame();
                if_stmt.semNSConsequentFrame.setActivation(currentActivation);
                for (var b : bindings)
                {
                    if_stmt.semNSConsequentFrame.ownNames().defineName(b.accessingProperty().name(), b);
                }
                enterFrame(if_stmt.semNSConsequentFrame);
            }

            this.verifyStatement(if_stmt.consequent);

            if (if_stmt.semNSConsequentFrame != null)
            {
                exitFrame();
            }

            if (if_stmt.alternative != null)
            {
                this.verifyStatement(if_stmt.alternative);
            }
        }
        else if (stmt instanceof Ast.LabeledStatementNode)
        {
            var labeled_stmt = (Ast.LabeledStatementNode) stmt;
            this.verifyStatement(labeled_stmt.substatement);
        }
        else if (stmt instanceof Ast.ReturnNode)
        {
            var ret_stmt = (Ast.ReturnNode) stmt;
            var f = this.currentFunction;
            var common = this.currentFunctionCommon;
            var s = f.signature();
            var r = s.result();
            var mustResultValue = !common.markYielding() && !r.isVoidType() && !r.isAnyType() && !r.isVerifyingType();

            if (ret_stmt.expression != null)
            {
                if (r.isVerifyingType())
                {
                    this.verifyValue(ret_stmt.expression);
                }
                else
                {
                    this.limitType(ret_stmt.expression, common.markYielding() ? this.pool.voidType : r);
                }
            }
            else if (mustResultValue)
            {
                this.reportVerifyError(Problem.Constants.STATEMENT_MUST_RESULT_VALUE, stmt.span);
            }
        }
        else if (stmt instanceof Ast.SuperStatementNode)
        {
            this.verifySuperStatement((Ast.SuperStatementNode) stmt);
        }
        else if (stmt instanceof Ast.SwitchStatementNode)
        {
            this.verifySwitchStatement((Ast.SwitchStatementNode) stmt);
        }
        else if (stmt instanceof Ast.SwitchTypeStatementNode)
        {
            this.verifySwitchTypeStatement((Ast.SwitchTypeStatementNode) stmt);
        }
        else if (stmt instanceof Ast.ThrowNode)
        {
            var k = this.verifyValue(((Ast.ThrowNode) stmt).expression);
        }
        else if (stmt instanceof Ast.TryStatementNode)
        {
            this.verifyTryStatement((Ast.TryStatementNode) stmt);
        }
        else if (stmt instanceof Ast.VarStatementNode)
        {
            this.verifyVarStatement((Ast.VarStatementNode) stmt);
        }
        else if (stmt instanceof Ast.WithStatementNode)
        {
            var with_stmt = (Ast.WithStatementNode) stmt;
            var with_value = this.verifyValue(with_stmt.expression);
            if (with_value == null)
            {
                return;
            }
            var with_frame = with_stmt.semNSFrame = this.pool.createWithFrame(with_value.staticType());
            with_frame.setActivation(this.currentActivation);
            this.enterFrame(with_frame);
            this.verifyStatement(with_stmt.substatement);
            this.exitFrame();
        }
        else
        {
            throw new RuntimeException("");
        }
    }

    private void verifyForStatement(Ast.ForStatementNode node)
    {
        var frame = this.pool.createForFrame();
        frame.setActivation(this.currentActivation);
        node.semNSFrame = frame;
        this.enterFrame(frame);

        if (node.expression1 != null)
        {
            if (node.expression1 instanceof Ast.SimpleVarDeclarationNode)
            {
                var declr = (Ast.SimpleVarDeclarationNode) node.expression1;
                for (var binding : declr.bindings)
                {
                    this.phase2DeclarePattern(binding.pattern, declr.readOnly, this.currentFrame().searchInternalNamespace(), frame.ownNames());
                }
                for (var binding : declr.bindings)
                {
                    this.phase4VerifyVarBinding(binding);
                }
            }
            else
            {
                this.verifyExpression((Ast.ExpressionNode) node.expression1);
            }
        }
        if (node.expression2 != null)
        {
            this.verifyExpression(node.expression2);
        }
        if (node.expression3 != null)
        {
            this.verifyExpression(node.expression3);
        }
        this.verifyStatement(node.substatement);
        this.exitFrame();
    }

    private void verifyForInStatement(Ast.ForInStatementNode node)
    {
        var frame = this.pool.createForFrame();
        frame.setActivation(this.currentActivation);
        node.semNSFrame = frame;
        this.enterFrame(frame);
        var iterable = this.verifyValue(node.right);
        Symbol itemType = null;
        if (iterable != null)
        {
            // Generator 
            if (iterable.staticType().escapeType().equalsOrInstantiatedFrom(this.pool.generatorType))
            {
                var gt = iterable.staticType().escapeType();
                itemType = gt.arguments().firstElement();
            }
            // iteration proxies
            else
            {
                var nextNameIndexProxy = this.pool.validateNextNameIndexProxy(iterable.staticType().delegate() != null ? iterable.staticType().delegate().lookupName(this.pool.proxyNextNameIndexName) : null);
                var nextNameOrValueProxy = this.pool.validateNextNameOrValueProxy(iterable.staticType().delegate() != null ? iterable.staticType().delegate().lookupName(node.isEach ? this.pool.proxyNextValueName : this.pool.proxyNextNameName) : null);

                if (nextNameIndexProxy == null || nextNameOrValueProxy == null || nextNameIndexProxy.signature().result() != nextNameOrValueProxy.signature().params().get(0))
                {
                    this.reportVerifyError(Problem.Constants.TYPE_IS_NOT_ITERABLE, node.right.span, Problem.Argument.createSymbol(iterable.staticType()));
                }
                else
                {
                    itemType = nextNameOrValueProxy.signature().result();
                }
            }
        }
        itemType = itemType == null ? this.pool.verifyingType : itemType;
        var declr = node.left instanceof Ast.SimpleVarDeclarationNode ? (Ast.SimpleVarDeclarationNode) node.left : null;
        if (declr != null)
        {
            var binding = declr.bindings.firstElement();
            this.phase2DeclarePattern(binding.pattern, declr.readOnly, this.currentFrame().searchInternalNamespace(), frame.ownNames());
            this.phase4DeclarePattern(binding.pattern, this.pool.createValue(itemType));
        }
        else
        {
            var target = this.verifyValue((Ast.ExpressionNode) node.left, itemType, VerifyFlags.ASSIGNMENT_LEFT_HAND_SIDE);
            if (target != null && target.staticType() != itemType && !itemType.isVerifyingType())
            {
                this.reportVerifyError(Problem.Constants.WRONG_ITEM_TYPE, node.left.span, Problem.Argument.createSymbol(itemType));
            }
        }
        this.verifyStatement(node.substatement);
        this.exitFrame();
    }

    private void verifyVarStatement(Ast.VarStatementNode node)
    {
        var frame = node.semNSFrame = this.pool.createBlockFrame();
        frame.setActivation(this.currentActivation);
        frame.setDefaultNamespace(this.currentFrame().searchInternalNamespace());
        for (var binding : node.bindings)
        {
            this.enterFrame(frame);
            this.phase2DeclarePattern(binding.pattern, node.readOnly, frame.defaultNamespace(), frame.ownNames());
            this.exitFrame();
            this.phase4VerifyVarBinding(binding);
        }
        this.enterFrame(frame);
        this.verifyStatement(node.substatement);
        this.exitFrame();
    }

    private void verifyTryStatement(Ast.TryStatementNode node)
    {
        this.verifyStatement(node.block);
        for (var catchNode : node.catchNodes)
        {
            var frame = catchNode.block.semNSFrame = this.pool.createBlockFrame();
            frame.setActivation(this.currentActivation);
            frame.setDefaultNamespace(this.currentFrame().searchInternalNamespace());
            this.phase2DeclarePattern(catchNode.pattern, false, frame.defaultNamespace(), frame.ownNames());
            Symbol excType = null;
            if (catchNode.pattern.type != null)
            {
                excType = this.verifyTypeAnnotation(catchNode.pattern.type);
            }
            else
            {
                this.reportVerifyError(Problem.Constants.UNTYPED_EXCEPTION, catchNode.pattern.span);
            }
            excType = excType == null ? this.pool.verifyingType : excType;
            this.phase4DeclarePattern(catchNode.pattern, this.pool.createValue(excType));
            this.enterFrame(frame);
            this.verifyDirectives(catchNode.block.directives);
            this.exitFrame();
        }
        if (node.finallyBlock != null)
        {
            this.verifyStatement(node.finallyBlock);
        }
    }

    private void verifySwitchStatement(Ast.SwitchStatementNode node)
    {
        var d = this.verifyValue(node.discriminant);
        for (var caseNode : node.caseNodes)
        {
            if (caseNode.expression != null)
            {
                if (d != null)
                {
                    this.limitType(caseNode.expression, d.staticType());
                }
                else
                {
                    this.verifyInvalidatedExpression(caseNode.expression);
                }
            }
            if (caseNode.directives != null)
            {
                this.verifyDirectives(caseNode.directives);
            }
        }
    }

    private void verifySwitchTypeStatement(Ast.SwitchTypeStatementNode node)
    {
        var d = this.verifyValue(node.discriminant);

        Symbol conv = null;

        for (var caseNode : node.caseNodes)
        {
            var frame = caseNode.block.semNSFrame = this.pool.createBlockFrame();
            frame.setActivation(this.currentActivation);
            frame.setDefaultNamespace(this.currentFrame().searchInternalNamespace());
            this.phase2DeclarePattern(caseNode.pattern, false, frame.defaultNamespace(), frame.ownNames());
            Symbol excType = null;
            if (caseNode.pattern.type != null)
            {
                excType = this.verifyTypeAnnotation(caseNode.pattern.type);
                if (d != null && excType != null)
                {
                    conv = d.explicitConversion(excType);
                    if (conv == null)
                    {
                        reportVerifyError(Problem.Constants.INCOMPATIBLE_TYPES, caseNode.pattern.type.span, Problem.Argument.createSymbol(d.staticType()), Problem.Argument.createSymbol(excType));
                    }
                    caseNode.semNSConversion = conv;
                }
            }
            else
            {
                this.reportVerifyError(Problem.Constants.UNTYPED_EXCEPTION, caseNode.pattern.span);
            }
            excType = excType == null ? this.pool.verifyingType : excType;
            this.phase4DeclarePattern(caseNode.pattern, this.pool.createValue(excType));
            this.enterFrame(frame);
            this.verifyDirectives(caseNode.block.directives);
            this.exitFrame();
        }
    }

    private void verifySuperStatement(Ast.SuperStatementNode node)
    {
        var ec = this.currentFrame().parentFrame().parentFrame().symbol();
        var superClass = ec.superClass();
        if (superClass == null)
        {
            throw new RuntimeException("");
        }
        Symbol cf = null;
        for (var h = superClass; h != null; h = h.superClass())
        {
            if ((cf = h.constructorFunction()) != null)
            {
                break;
            }
        }
        if (cf == null)
        {
            throw new RuntimeException("");
        }
        this.verifyCallArguments(node.arguments, node, cf.signature());
        node.semNSFunction = cf;
    }

    private Symbol verifyTypeAnnotation(Ast.TypeNode node)
    {
        var r = node.semNSResult;
        if (r != null)
        {
            return r.isVerifyingType() ? null : r;
        }

        Symbol type = null;

        if (node instanceof Ast.IDTypeNode)
        {
            node.semNSResult = this.verifyIDTypeAnnotation((Ast.IDTypeNode) node);
            if (node.semNSResult == null)
            {
                node.semNSResult = this.pool.verifyingType;
                return null;
            }
        }
        else if (node instanceof Ast.VoidTypeNode)
        {
            node.semNSResult = this.pool.voidType;
        }
        else if (node instanceof Ast.AnyTypeNode)
        {
            node.semNSResult = this.pool.anyType;
        }
        else if (node instanceof Ast.NullableTypeNode)
        {
            var nullating_type = this.verifyTypeAnnotation(((Ast.NullableTypeNode) node).subtype);
            if (nullating_type == null)
            {
                node.semNSResult = this.pool.verifyingType;
                return null;
            }
            node.semNSResult = this.pool.createNullableType(nullating_type);
        }
        else if (node instanceof Ast.FunctionTypeNode)
        {
            var func_node = (Ast.FunctionTypeNode) node;

            Vector<Symbol> params = null;
            Vector<Symbol> optParams = null;
            Symbol rest = null;
            Symbol result = null;

            if (func_node.params != null)
            {
                params = new Vector<>();
                for (var node2 : func_node.params)
                {
                    type = this.verifyTypeAnnotation(node2);
                    if (type == null)
                    {
                        node.semNSResult = this.pool.verifyingType;
                        return null;
                    }
                    else
                    {
                        params.add(type);
                    }
                }
            }
            if (func_node.optParams != null)
            {
                optParams = new Vector<>();
                for (var node2 : func_node.optParams)
                {
                    type = this.verifyTypeAnnotation(node2);
                    if (type == null)
                    {
                        node.semNSResult = this.pool.verifyingType;
                        return null;
                    }
                    else
                    {
                        optParams.add(type);
                    }
                }
            }
            if (func_node.rest != null)
            {
                rest = this.verifyTypeAnnotation(func_node.rest);
                if (rest == null)
                {
                    node.semNSResult = this.pool.verifyingType;
                    return null;
                }
            }
            result = this.verifyTypeAnnotation(func_node.result);
            if (result == null)
            {
                node.semNSResult = this.pool.verifyingType;
                return null;
            }
            return node.semNSResult = this.pool.createFunctionType(params, optParams, rest, result);
        }
        else if (node instanceof Ast.TupleTypeNode)
        {
            Vector<Symbol> elements = new Vector<>();
            Symbol tuple_last_el = null;
            for (var node2 : ((Ast.TupleTypeNode) node).subtypes)
            {
                if (node2 == null)
                {
                    elements.add(tuple_last_el == null ? pool.anyType : tuple_last_el);
                    continue;
                }
                tuple_last_el = this.verifyTypeAnnotation(node2);
                if (tuple_last_el == null)
                {
                    node.semNSResult = this.pool.verifyingType;
                    return null;
                }
                else
                {
                    elements.add(tuple_last_el);
                }
            }
            return node.semNSResult = this.pool.createTupleType(elements);
        }
        else if (node instanceof Ast.UnionTypeNode)
        {
            var members = new Vector<Symbol>();
            for (var node2 : ((Ast.UnionTypeNode) node).members)
            {
                type = this.verifyTypeAnnotation(node2);
                if (type == null)
                {
                    node.semNSResult = this.pool.verifyingType;
                    return null;
                }
                else if (!members.contains(type))
                {
                    members.add(type);
                }
            }
            return node.semNSResult = this.pool.createUnionType(members);
        }
        else
        {
            throw new RuntimeException("");
        }

        return node.semNSResult;
    }

    private Symbol verifyIDTypeAnnotation(Ast.IDTypeNode node)
    {
        Symbol pckg = null;
        if (node.packageID != "" && node.packageID != null)
        {
            pckg = this.pool.findPackage(node.packageID);
            if (pckg == null)
            {
                this.reportVerifyError(Problem.Constants.EMPTY_PACKAGE, node.span, Problem.Argument.createQuote(node.packageID + ".*"));
                return null;
            }
        }

        Symbol p = null;

        if (node.qualifier != null)
        {
            var qual = this.verifyNamespaceConstant(node.qualifier);
            if (qual == null)
            {
                return null;
            }
            var name = this.pool.createName(qual, node.name);
            try
            {
                p = pckg != null ? pckg.lookupName(name) : this.scopeChain.lookupName(name);
            }
            catch (AmbiguousReferenceError exc)
            {
                this.reportVerifyError(Problem.Constants.AMBIGUOUS_REFERENCE, node.span, Problem.Argument.createQuote(name.toString()));
                return null;
            }
            if (p == null || !p.isType())
            {
                this.reportVerifyError(Problem.Constants.TYPE_NOT_FOUND, node.span, Problem.Argument.createQuote(name.toString()));
                return null;
            }
        }
        else
        {
            try
            {
                p = pckg != null ? pckg.lookupMultiName(scopeChain.openNamespaceList(), node.name) : this.scopeChain.lookupMultiName(this.scopeChain.openNamespaceList(), node.name);
            }
            catch (AmbiguousReferenceError exc)
            {
                this.reportVerifyError(Problem.Constants.AMBIGUOUS_REFERENCE, node.span, Problem.Argument.createQuote(node.name));
                return null;
            }
            if (p == null || !p.isType())
            {
                this.reportVerifyError(Problem.Constants.TYPE_NOT_FOUND, node.span, Problem.Argument.createQuote(node.name));
                return null;
            }
        }

        if (node.arguments != null)
        {
            return this.verifyTypeArguments(p, node.arguments, node.span);
        }

        var typeParams = p.typeParams();
        if (typeParams != null)
        {
            p = instantiateTypeWithDefaultArguments(p, node.span);
        }
        return p;
    }

    private Symbol verifyTypeArguments(Symbol baseType, Vector<Ast.TypeNode> arguments, Span baseSpan)
    {
        var typeParams = baseType.typeParams();
        if (typeParams == null)
        {
            this.reportVerifyError(Problem.Constants.NOT_A_GENERIC_TYPE, baseSpan, Problem.Argument.createSymbol(baseType));
            return null;
        }
        if (arguments.size() > typeParams.size())
        {
            this.reportVerifyError(Problem.Constants.WRONG_NUM_ARGUMENTS, baseSpan, Problem.Argument.createNumber(typeParams.size()));
        }
        var argumentSymbols = new Vector<Symbol>();
        int i = 0;

        for (; i != arguments.size(); ++i)
        {
            var type = this.verifyTypeAnnotation(arguments.get(i));

            if (type == null)
            {
                return null;
            }
            if (i < typeParams.size())
            {
                argumentSymbols.add(type);
            }
        }

        for (; i < typeParams.size(); ++i)
        {
            argumentSymbols.add(typeParams.get(i).defaultType());
        }

        for (i = 0; i != typeParams.size(); ++i)
        {
            this.checkTypeParameterBounds1(typeParams.get(i), argumentSymbols.get(i), arguments.get(i).span);
        }

        return this.pool.createInstantiatedType(baseType, argumentSymbols);
    }
}