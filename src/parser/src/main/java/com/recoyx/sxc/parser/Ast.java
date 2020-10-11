package com.recoyx.sxc.parser;

import com.recoyx.sxc.semantics.*;
import java.util.Vector;

public final class Ast
{
    static public class Node
    {
        public Span span;
    }

    static public class PatternNode extends Node
    {
        public Symbol semNsVariable = null;
        public ExpressionNode type = null;

        public PatternNode(ExpressionNode type)
        {
            super();
            this.type = type;
        }
    }

    static public class ExpressionNode extends Node
    {
        public Symbol semNsResult = null;

        /**
         * @return Vector of frame property accesses.
         */
        public Vector<Symbol> retrieveBindings()
        {
            return null;
        }
    }

    static public class QualifiedIdNode extends ExpressionNode
    {
        public ExpressionNode qualifier;

        public QualifiedIdNode(ExpressionNode qualifier)
        {
            this.qualifier = qualifier;
        }
    }

    static public class DirectiveNode extends Node
    {
    }

    static public class DefinitionNode extends DirectiveNode
    {
        public Vector<MetaData> metaDataArray = null;
        public ExpressionNode accessModifier = null;
        public Span nameSpan = null;
        public Symbol semNsSymbol = null;
        private int _modifiers = 0;

        public boolean markStatic()
        {
            return (_modifiers & 1) != 0;
        }

        public void setMarkStatic(boolean value)
        {
            _modifiers = value ? _modifiers | 1 : (_modifiers & 1) != 0 ? _modifiers ^ 1 : _modifiers;
        }

        public boolean markOverride()
        {
            return (_modifiers & 2) != 0;
        }

        public void setMarkOverride(boolean value)
        {
            _modifiers = value ? _modifiers | 2 : (_modifiers & 2) != 0 ? _modifiers ^ 2 : _modifiers;
        }

        public boolean markFinal()
        {
            return (_modifiers & 4) != 0;
        }

        public void setMarkFinal(boolean value)
        {
            _modifiers = value ? _modifiers | 4 : (_modifiers & 4) != 0 ? _modifiers ^ 4 : _modifiers;
        }

        public boolean markNative()
        {
            return (_modifiers & 8) != 0;
        }

        public void setMarkNative(boolean value)
        {
            _modifiers = value ? _modifiers | 8 : (_modifiers & 8) != 0 ? _modifiers ^ 8 : _modifiers;
        }

        public boolean markDynamic()
        {
            return (_modifiers & 16) != 0;
        }

        public void setMarkDynamic(boolean value)
        {
            _modifiers = value ? _modifiers | 16 : (_modifiers & 16) != 0 ? _modifiers ^ 16 : _modifiers;
        }

        public MetaData getMetaData(String name)
        {
            if (metaDataArray == null)
            {
                return null;
            }
            for (var meta_data : metaDataArray)
            {
                if (meta_data.name.equals(name))
                {
                    return meta_data;
                }
            }
            return null;
        }

        public boolean deleteMetaData(MetaData metaData)
        {
            return metaDataArray == null ? false : metaDataArray.remove(metaData);
        }

        public boolean deleteMetaData(String name)
        {
            if (metaDataArray == null)
            {
                return false;
            }
            for (int i = 0; i != metaDataArray.size(); ++i)
            {
                if (metaDataArray.get(i).name.equals(name))
                {
                    metaDataArray.remove(i);
                    return true;
                }
            }
            return false;
        }
    }

    static public class StatementNode extends DirectiveNode
    {
        public boolean isIterationStatement()
        {
            return false;
        }
    }

    /**
     * Array initializer.
     *
     * <ul>
     * <li>Array initializer assigned to *, Object or Array reference will return an Array object.</li>
     * <li>Array initializer assigned to Vector reference will return a Vector object.</li>
     * <li>Array initializer assigned to tuple reference will return a tuple object.</li>
     * <li>Array initializer assigned to flags enum reference will return an enum object.</li>
     * </ul>
     */
    static public final class ArrayLiteralNode extends ExpressionNode
    {
        public Vector<ExpressionNode> elements;
        public ExpressionNode rest;

        public ArrayLiteralNode(Vector<ExpressionNode> elements, ExpressionNode rest)
        {
            super();
            this.elements = elements;
            this.rest = rest;
        }
    }

    static public final class ArrayPatternNode extends PatternNode
    {
        public Vector<PatternNode> elements;
        public PatternNode rest;

        public ArrayPatternNode(Vector<PatternNode> elements, PatternNode rest, ExpressionNode type)
        {
            super(type);
            this.elements = elements;
            this.rest = rest;
        }
    }

    static public final class AssignmentNode extends ExpressionNode
    {
        public Operator compound;
        public ExpressionNode left;
        public ExpressionNode right;

        public AssignmentNode(Operator compound, ExpressionNode left, ExpressionNode right)
        {
            super();
            this.compound = compound;
            this.left = left;
            this.right = right;
        }
    }

    static public final class AttributeIDNode extends QualifiedIdNode
    {
        public QualifiedIdNode id;

        public AttributeIDNode(QualifiedIdNode id)
        {
            super(null);
            this.id = id;
        }
    }

    static public final class BinaryOperatorNode extends ExpressionNode
    {
        public Operator type;
        public ExpressionNode left;
        public ExpressionNode right;

        /**
         * Optional condition frame.
         */
        public Symbol semNsRightFrame;

        public BinaryOperatorNode(Operator type, ExpressionNode left, ExpressionNode right)
        {
            super();
            this.type = type;
            this.left = left;
            this.right = right;
        }

        @Override
        public Vector<Symbol> retrieveBindings()
        {
            var bindings = left.retrieveBindings();
            var bindings2 = right.retrieveBindings();
            if (bindings == null && bindings2 != null)
            {
                bindings = new Vector<>();
            }
            if (bindings2 != null)
            {
                for (var b : bindings2)
                {
                    bindings.add(b);
                }
            }
            return bindings;
        }
    }

    static public final class BlockNode extends StatementNode
    {
        public Vector<DirectiveNode> directives;
        public Symbol semNsFrame = null;

        public BlockNode(Vector<DirectiveNode> directives)
        {
            super();
            this.directives = directives;
        }
    }

    static public final class BooleanLiteralNode extends ExpressionNode
    {
        public boolean value;

        public BooleanLiteralNode(boolean value)
        {
            super();
            this.value = value;
        }
    }

    static public final class BracketsNode extends ExpressionNode
    {
        public ExpressionNode base;
        public ExpressionNode key;

        public BracketsNode(ExpressionNode base, ExpressionNode key)
        {
            super();
            this.base = base;
            this.key = key;
        }
    }

    static public final class BreakNode extends StatementNode
    {
        public String label;
        public Node targetStatement = null;

        public BreakNode(String label)
        {
            super();
            this.label = label;
        }
    }

    /**
     * Call operator node.
     *
     * <list>
     * <ul>If applied to a type, how the result is computed depends on the
     * <code>semNs::isTypeConversion</code> and <code>semNs::isTypeConstruct</code> properties,
     * and if both are false, then the sole argument gives the result.</ul>
     * </list>
     */
    static public final class CallNode extends ExpressionNode
    {
        public ExpressionNode base;
        public Vector<ExpressionNode> arguments;

        /**
         * <code>true</code> if the call operator is applied to a type, performing an explicit type conversion.
         */
        public boolean semNsIsTypeConversion = false;

        /**
         * <code>true</code> if the call operator is applied to a type, invoking its constructor.
         */
        public boolean semNsIsTypeConstruct = false;

        public CallNode(ExpressionNode base, Vector<ExpressionNode> arguments)
        {
            super();
            this.base = base;
            this.arguments = arguments;
        }
    }

    static public final class CatchNode extends Node
    {
        public PatternNode pattern;
        public BlockNode block;

        public CatchNode(PatternNode pattern, BlockNode block)
        {
            super();
            this.pattern = pattern;
            this.block = block;
        }
    }

    static public final class ClassDefinitionNode extends DefinitionNode
    {
        public String name;
        public Vector<String> typeParams;
        public ExpressionNode extendsNode;
        public Vector<ExpressionNode> implementsList;
        public BlockNode block;

        public ClassDefinitionNode(String name, Vector<String> typeParams, ExpressionNode extendsNode, Vector<ExpressionNode> implementsList, BlockNode block)
        {
            super();
            this.name = name;
            this.typeParams = typeParams;
            this.extendsNode = extendsNode;
            this.implementsList = implementsList;
            this.block = block;
        }
    }

    static public final class CommentNode extends Node
    {
        public String content;
        public boolean multiline;

        public CommentNode(String content, boolean multiline)
        {
            super();
            this.content = content;
            this.multiline = multiline;
        }
    }

    static public final class ContinueNode extends StatementNode
    {
        public String label;
        public Node targetStatement;

        public ContinueNode(String label)
        {
            super();
            this.label = label;
        }
    }

    static public final class DescendantsNode extends ExpressionNode
    {
        public ExpressionNode base;
        public QualifiedIdNode id;

        public DescendantsNode(ExpressionNode base, QualifiedIdNode id)
        {
            super();
            this.base = base;
            this.id = id;
        }
    }

    static public final class DoStatementNode extends StatementNode
    {
        public StatementNode substatement;
        public ExpressionNode expression;

        public DoStatementNode(StatementNode substatement, ExpressionNode expression)
        {
            super();
            this.substatement = substatement;
            this.expression = expression;
        }

        @Override
        public boolean isIterationStatement()
        {
            return true;
        }
    }

    static public final class DotNode extends ExpressionNode
    {
        public ExpressionNode base;
        public QualifiedIdNode id;

        public DotNode(ExpressionNode base, QualifiedIdNode id)
        {
            super();
            this.base = base;
            this.id = id;
        }
    }

    static public final class DXNSStatementNode extends StatementNode
    {
        public ExpressionNode expression;

        public DXNSStatementNode(ExpressionNode expression)
        {
            super();
            this.expression = expression;
        }
    }

    static public final class EmbedExpressionNode extends ExpressionNode
    {
        public String src;

        public EmbedExpressionNode(String src)
        {
            super();
            this.src = src;
        }
    }

    static public final class EmptyStatementNode extends StatementNode
    {
    }

    static public final class EnumDefinitionNode extends DefinitionNode
    {
        public String name;
        public ExpressionNode type;
        public BlockNode block;

        public EnumDefinitionNode(String name, ExpressionNode type, BlockNode block)
        {
            super();
            this.name = name;
            this.type = type;
            this.block = block;
        }
    }

    static public final class ExpressionIdNode extends QualifiedIdNode
    {
        public ExpressionNode key;

        public ExpressionIdNode(ExpressionNode qualifier, ExpressionNode key)
        {
            super(qualifier);
            this.key = key;
        }
    }

    static public final class ExpressionStatementNode extends StatementNode
    {
        public ExpressionNode expression;

        public ExpressionStatementNode(ExpressionNode expression)
        {
            super();
            this.expression = expression;
        }
    }

    static public final class FilterNode extends ExpressionNode
    {
        public ExpressionNode base;
        public ExpressionNode expression;
        public Activation semNsActivation;

        public FilterNode(ExpressionNode base, ExpressionNode expression)
        {
            super();
            this.base = base;
            this.expression = expression;
        }
    }

    static public final class ForInStatementNode extends StatementNode
    {
        public boolean isEach;
        public Node left;
        public ExpressionNode right;
        public StatementNode substatement;

        public Symbol semNsFrame;

        public ForInStatementNode(boolean isEach, Node left, ExpressionNode right, StatementNode substatement)
        {
            super();
            this.isEach = isEach;
            this.left = left;
            this.right = right;
            this.substatement = substatement;
        }

        @Override
        public boolean isIterationStatement()
        {
            return true;
        }
    }

    static public final class ForStatementNode extends StatementNode
    {
        public Node expression1;
        public ExpressionNode expression2;
        public ExpressionNode expression3;
        public StatementNode substatement;

        public Symbol semNsFrame;

        public ForStatementNode(Node expression1, ExpressionNode expression2, ExpressionNode expression3, StatementNode substatement)
        {
            super();
            this.expression1 = expression1;
            this.expression2 = expression2;
            this.expression3 = expression3;
        }

        @Override
        public boolean isIterationStatement()
        {
            return true;
        }
    }

    static public final class FunctionCommonNode extends Node
    {
        public Vector<PatternNode> params;
        public Vector<VarBindingNode> optParams;
        public String rest;
        public ExpressionNode result;
        public Node body;
        public Symbol semNsFrame = null;
        private int _flags = 0;

        public FunctionCommonNode(Vector<PatternNode> params, Vector<VarBindingNode> optParams, String rest, ExpressionNode result, Node body)
        {
            super();
            this.params = params;
            this.optParams = optParams;
            this.rest = rest;
            this.result = result;
            this.body = body;
        }

        public boolean markGetter()
        {
            return (_flags & 1) != 0;
        }

        public void setMarkGetter(boolean value)
        {
            _flags = value ? _flags | 1 : (_flags & 1) != 0 ? _flags ^ 1 : _flags;
        }

        public boolean markSetter()
        {
            return (_flags & 2) != 0;
        }

        public void setMarkSetter(boolean value)
        {
            _flags = value ? _flags | 2 : (_flags & 2) != 0 ? _flags ^ 2 : _flags;
        }

        public boolean markConstructor()
        {
            return (_flags & 4) != 0;
        }

        public void setMarkConstructor(boolean value)
        {
            _flags = value ? _flags | 4 : (_flags & 4) != 0 ? _flags ^ 4 : _flags;
        }

        public boolean markYielding()
        {
            return (_flags & 8) != 0;
        }

        public void setMarkYielding(boolean value)
        {
            _flags = value ? _flags | 8 : (_flags & 8) != 0 ? _flags ^ 8 : _flags;
        }
    }

    static public final class FunctionDefinitionNode extends DefinitionNode
    {
        public String name;
        public FunctionCommonNode common;

        /**
         * In a constructor definition consisting of an Ast.BlockNode body,
         * this indicates the index of the first statement where <code>this</code> is accessed.
         * It may be equals <code>-1</code> if <code>this</code> is never accessed.
         *
         * Note that this is true if <code>this</code> is accessed lexically.
         */
        public int semNsInstanceInitialiserEnd = -1;

        public FunctionDefinitionNode(String name, FunctionCommonNode common)
        {
            super();
            this.name = name;
            this.common = common;
        }

        public boolean markGetter()
        {
            return common.markGetter();
        }

        public void setMarkGetter(boolean value)
        {
            common.setMarkGetter(value);
        }

        public boolean markSetter()
        {
            return common.markSetter();
        }

        public void setMarkSetter(boolean value)
        {
            common.setMarkSetter(value);
        }

        public boolean markConstructor()
        {
            return common.markConstructor();
        }

        public void setMarkConstructor(boolean value)
        {
            common.setMarkConstructor(value);
        }

        public boolean markYielding()
        {
            return common.markYielding();
        }

        public void setMarkYielding(boolean value)
        {
            common.setMarkYielding(value);
        }
    }

    static public final class FunctionExpressionNode extends ExpressionNode
    {
        public String name;
        public FunctionCommonNode common;

        public Symbol semNsFunction;
        public Symbol semNsFrame;

        public FunctionExpressionNode(String name, FunctionCommonNode common)
        {
            super();
            this.name = name;
            this.common = common;
        }
    }

    static public final class IfStatementNode extends StatementNode
    {
        public ExpressionNode expression;
        public StatementNode consequent;
        public StatementNode alternative;

        /**
         * Optional condition frame.
         */
        public Symbol semNsConsequentFrame;

        public IfStatementNode(ExpressionNode expression, StatementNode consequent, StatementNode alternative)
        {
            super();
            this.expression = expression;
            this.consequent = consequent;
            this.alternative = alternative;
        }
    }

    static public final class ImportDirectiveNode extends DirectiveNode
    {
        public String alias;
        public Span aliasSpan;
        public String importName;
        public Span importNameSpan;
        public boolean wildcard;

        public Symbol semNsExtracted;

        public ImportDirectiveNode(String alias, String importName, boolean wildcard)
        {
            super();
            this.alias = alias;
            this.importName = importName;
            this.wildcard = wildcard;
        }
    }

    static public final class IncludeDirectiveNode extends DirectiveNode
    {
        public String src;
        public Script subscript;
        public Vector<PackageDefinitionNode> subpackages;
        public Vector<DirectiveNode> subdirectives;

        public IncludeDirectiveNode(String src)
        {
            super();
            this.src = src;
        }
    }

    static public final class InterfaceDefinitionNode extends DefinitionNode
    {
        public String name;
        public Vector<String> typeParams;
        public Vector<ExpressionNode> extendsList;
        public BlockNode block;

        public InterfaceDefinitionNode(String name, Vector<String> typeParams, Vector<ExpressionNode> extendsList, BlockNode block)
        {
            super();
            this.name = name;
            this.typeParams = typeParams;
            this.extendsList = extendsList;
            this.block = block;
        }
    }

    static public final class LabeledStatementNode extends StatementNode
    {
        public String label;
        public StatementNode substatement;

        public LabeledStatementNode(String label, StatementNode substatement)
        {
            super();
            this.label = label;
            this.substatement = substatement;
        }
    }

    static public final class ListExpressionNode extends ExpressionNode
    {
        public Vector<ExpressionNode> expressions;

        public ListExpressionNode(Vector<ExpressionNode> expressions)
        {
            super();
            this.expressions = expressions;
        }
    }

    static public final class MetaDataEntryNode extends Node
    {
        public String name;
        public Span nameSpan;
        public ExpressionNode literal;

        public MetaDataEntryNode(String name, ExpressionNode literal)
        {
            super();
            this.name = name;
            this.literal = literal;
        }
    }

    static public final class NamePatternNode extends PatternNode
    {
        public String name;

        public NamePatternNode(String name, ExpressionNode type)
        {
            super(type);
            this.name = name;
        }
    }

    static public final class NamespaceDefinitionNode extends DefinitionNode
    {
        public String name;
        public ExpressionNode expression;

        public NamespaceDefinitionNode(String name, ExpressionNode expression)
        {
            super();
            this.name = name;
            this.expression = expression;
        }
    }

    static public final class NewOperatorNode extends ExpressionNode
    {
        public ExpressionNode base;
        public Vector<ExpressionNode> arguments;

        public NewOperatorNode(ExpressionNode base, Vector<ExpressionNode> arguments)
        {
            super();
            this.base = base;
            this.arguments = arguments;
        }
    }

    static public final class NullableTypeNode extends ExpressionNode
    {
        public ExpressionNode type;

        public NullableTypeNode(ExpressionNode type)
        {
            super();
            this.type = type;
        }
    }

    static public final class NullLiteralNode extends ExpressionNode
    {
    }

    static public final class NumericLiteralNode extends ExpressionNode
    {
        public double value;

        public NumericLiteralNode(double value)
        {
            super();
            this.value = value;
        }
    }

    static public final class ObjectFieldNode extends Node
    {
        public ExpressionNode key;
        public ExpressionNode value;

        public Symbol semNsVariable = null;

        public ObjectFieldNode(ExpressionNode key, ExpressionNode value)
        {
            super();
            this.key = key;
            this.value = value;
        }
    }

    static public final class ObjectLiteralNode extends ExpressionNode
    {
        public Vector<ObjectFieldNode> fields;
        public ExpressionNode rest;

        public ObjectLiteralNode(Vector<ObjectFieldNode> fields, ExpressionNode rest)
        {
            super();
            this.fields = fields;
            this.rest = rest;
        }
    }

    static public final class ObjectPatternFieldNode extends Node
    {
        public SimpleIdNode id;
        public PatternNode subpattern;

        public Symbol semNsExtracted = null;

        public ObjectPatternFieldNode(SimpleIdNode id, PatternNode subpattern)
        {
            super();
            this.id = id;
            this.subpattern = subpattern;
        }
    }

    static public final class ObjectPatternNode extends PatternNode
    {
        public Vector<ObjectPatternFieldNode> fields;

        public ObjectPatternNode(Vector<ObjectPatternFieldNode> fields, ExpressionNode type)
        {
            super(type);
            this.fields = fields;
        }
    }

    static public final class PackageDefinitionNode extends Node
    {
        public String id;
        public BlockNode block;
        public Script script;
        public Symbol semNsSymbol;

        public PackageDefinitionNode(String id, BlockNode block)
        {
            super();
            this.id = id;
            this.block = block;
        }
    }

    static public final class ParenExpressionNode extends ExpressionNode
    {
        public ExpressionNode expression;

        public ParenExpressionNode(ExpressionNode expression)
        {
            super();
            this.expression = expression;
        }
    }

    static public final class PatternAssignmentNode extends ExpressionNode
    {
        public PatternNode left;
        public ExpressionNode right;

        public PatternAssignmentNode(PatternNode left, ExpressionNode right)
        {
            super();
            this.left = left;
            this.right = right;
        }
    }

    static public final class ProgramNode extends Node
    {
        public Vector<PackageDefinitionNode> packages;
        public Vector<DirectiveNode> directives;
        public Script script;
        public Activation semNsActivation;
        public Symbol semNsFrame;

        public ProgramNode(Vector<PackageDefinitionNode> packages, Vector<DirectiveNode> directives)
        {
            super();
            this.packages = packages;
            this.directives = directives;
        }
    }

    static public final class RegExpLiteralNode extends ExpressionNode
    {
        public String body;
        public String flags;

        public RegExpLiteralNode(String body, String flags)
        {
            super();
            this.body = body;
            this.flags = flags;
        }
    }

    static public final class ReservedNamespaceNode extends ExpressionNode
    {
        public String type;

        public ReservedNamespaceNode(String type)
        {
            super();
            this.type = type;
        }
    }

    static public final class ReturnNode extends StatementNode
    {
        public ExpressionNode expression;

        public ReturnNode(ExpressionNode expression)
        {
            super();
            this.expression = expression;
        }
    }

    static public final class SimpleIdNode extends QualifiedIdNode
    {
        public String name;

        public SimpleIdNode(ExpressionNode qualifier, String name)
        {
            super(qualifier);
            this.name = name;
        }
    }

    static public final class SimpleVarDeclarationNode extends Node
    {
        public boolean readOnly;
        public Vector<VarBindingNode> bindings;

        public SimpleVarDeclarationNode(boolean readOnly, Vector<VarBindingNode> bindings)
        {
            super();
            this.readOnly = readOnly;
            this.bindings = bindings;
        }
    }

    static public final class StringLiteralNode extends ExpressionNode
    {
        public String value;

        public StringLiteralNode(String value)
        {
            super();
            this.value = value;
        }
    }

    static public final class SuperDotNode extends ExpressionNode
    {
        public Vector<ExpressionNode> arguments;
        public SimpleIdNode id;

        public Symbol semNsBase;

        public SuperDotNode(Vector<ExpressionNode> arguments, SimpleIdNode id)
        {
            super();
            this.arguments = arguments;
            this.id = id;
        }
    }

    static public final class SuperStatementNode extends StatementNode
    {
        public Vector<ExpressionNode> arguments;
        public Symbol semNsFunction;

        public SuperStatementNode(Vector<ExpressionNode> arguments)
        {
            super();
            this.arguments = arguments;
        }
    }

    static public final class SwitchCaseNode extends Node
    {
        public ExpressionNode expression;
        public Vector<DirectiveNode> directives;

        public SwitchCaseNode(ExpressionNode expression, Vector<DirectiveNode> directives)
        {
            super();
            this.expression = expression;
            this.directives = directives;
        }
    }

    static public final class SwitchStatementNode extends StatementNode
    {
        public ExpressionNode discriminant;
        public Vector<SwitchCaseNode> caseNodes;

        public SwitchStatementNode(ExpressionNode discriminant, Vector<SwitchCaseNode> caseNodes)
        {
            super();
            this.discriminant = discriminant;
            this.caseNodes = caseNodes;
        }
    }

    static public final class SwitchTypeCaseNode extends Node
    {
        public PatternNode pattern;
        public BlockNode block;
        public Symbol semNsConversion;

        public SwitchTypeCaseNode(PatternNode pattern, BlockNode block)
        {
            super();
            this.pattern = pattern;
            this.block = block;
        }
    }

    static public final class SwitchTypeStatementNode extends StatementNode
    {
        public ExpressionNode discriminant;
        public Vector<SwitchTypeCaseNode> caseNodes;

        public SwitchTypeStatementNode(ExpressionNode discriminant, Vector<SwitchTypeCaseNode> caseNodes)
        {
            super();
            this.discriminant = discriminant;
            this.caseNodes = caseNodes;
        }
    }

    static public final class TernaryNode extends ExpressionNode
    {
        public ExpressionNode expression1;
        public ExpressionNode expression2;
        public ExpressionNode expression3;

        /**
         * Optional condition frame.
         */
        public Symbol semNsConsequentFrame;

        public TernaryNode(ExpressionNode expression1, ExpressionNode expression2, ExpressionNode expression3)
        {
            super();
            this.expression1 = expression1;
            this.expression2 = expression2;
            this.expression3 = expression3;
        }
    }

    static public final class ThisLiteralNode extends ExpressionNode
    {
    }

    static public final class ThrowNode extends StatementNode
    {
        public ExpressionNode expression;

        public ThrowNode(ExpressionNode expression)
        {
            super();
            this.expression = expression;
        }
    }

    static public final class TryStatementNode extends StatementNode
    {
        public BlockNode block;
        public Vector<CatchNode> catchNodes;
        public BlockNode finallyBlock;

        public TryStatementNode(BlockNode block, Vector<CatchNode> catchNodes, BlockNode finallyBlock)
        {
            super();
            this.block = block;
            this.catchNodes = catchNodes;
            this.finallyBlock = finallyBlock;
        }
    }

    static public final class TypeArgumentsNode extends ExpressionNode
    {
        public ExpressionNode base;
        public Vector<ExpressionNode> arguments;

        public TypeArgumentsNode(ExpressionNode base, Vector<ExpressionNode> arguments)
        {
            super();
            this.base = base;
            this.arguments = arguments;
        }
    }

    static public final class TypeDefinitionNode extends DefinitionNode
    {
        public String name;
        public ExpressionNode type;

        public TypeDefinitionNode(String name, ExpressionNode type)
        {
            super();
            this.name = name;
            this.type = type;
        }
    }

    static public final class TypeIdNode extends Node
    {
        public String name;
        public ExpressionNode type;
        public Symbol semNsVariable = null;

        public TypeIdNode(String name, ExpressionNode type)
        {
            super();
            this.name = name;
            this.type = type;
        }
    }

    static public final class TypeOperatorNode extends ExpressionNode
    {
        public String operator;
        public ExpressionNode left;

        /**
         * Optional if right pattern is specified.
         */
        public ExpressionNode right;

        /**
         * Optional right pattern for instanceof/is operators if right type annotation is specified.
         * It may appear in the form <code>x is x:T</code>, where the later <code>x</code> is the pattern.
         */
        public PatternNode pattern;

        public Symbol semNsBindingFrame;

        public TypeOperatorNode(String operator, ExpressionNode left, ExpressionNode right)
        {
            super();
            this.operator = operator;
            this.left = left;
            this.right = right;
        }

        @Override
        public Vector<Symbol> retrieveBindings()
        {
            if (semNsBindingFrame == null)
            {
                return null;
            }
            var pool = semNsBindingFrame.pool();
            var bindings = new Vector<Symbol>();
            for (var pair : semNsBindingFrame.ownNames())
            {
                bindings.add(pool.createFrameProperty(semNsBindingFrame, pair.value));
            }
            return bindings;
        }
    }

    static public final class UnaryOperatorNode extends ExpressionNode
    {
        public Operator type;
        public ExpressionNode argument;

        public UnaryOperatorNode(Operator type, ExpressionNode argument)
        {
            super();
            this.type = type;
            this.argument = argument;
        }
    }

    static public final class UseDefaultDirectiveNode extends DirectiveNode
    {
        public ExpressionNode expression;

        public UseDefaultDirectiveNode(ExpressionNode expression)
        {
            super();
            this.expression = expression;
        }
    }

    static public final class UseDirectiveNode extends DirectiveNode
    {
        public ExpressionNode expression;

        public UseDirectiveNode(ExpressionNode expression)
        {
            super();
            this.expression = expression;
        }
    }

    static public final class VarBindingNode extends Node
    {
        public PatternNode pattern;
        public ExpressionNode initialiser;
        public String semNsConstantID = null;

        public VarBindingNode(PatternNode pattern, ExpressionNode initialiser)
        {
            super();
            this.pattern = pattern;
            this.initialiser = initialiser;
        }
    }

    static public final class VarDefinitionNode extends DefinitionNode
    {
        public boolean readOnly;
        public Vector<VarBindingNode> bindings;
        public boolean semNsValidated = true;

        public VarDefinitionNode(boolean readOnly, Vector<VarBindingNode> bindings)
        {
            super();
            this.readOnly = readOnly;
            this.bindings = bindings;
        }
    }

    static public final class VarStatementNode extends StatementNode
    {
        public boolean readOnly;
        public Vector<VarBindingNode> bindings;
        public StatementNode substatement;
        public Symbol semNsFrame = null;

        public VarStatementNode(boolean readOnly, Vector<VarBindingNode> bindings, StatementNode substatement)
        {
            super();
            this.readOnly = readOnly;
            this.bindings = bindings;
            this.substatement = substatement;
        }
    }

    static public final class VoidExpressionNode extends ExpressionNode
    {
    }

    static public final class VoidTypeNode extends ExpressionNode
    {
    }

    static public final class WhileStatementNode extends StatementNode
    {
        public ExpressionNode expression;
        public StatementNode substatement;

        /**
         * Optional condition frame.
         */
        public Symbol semNsConsequentFrame;

        public WhileStatementNode(ExpressionNode expression, StatementNode substatement)
        {
            super();
            this.expression = expression;
            this.substatement = substatement;
        }

        @Override
        public boolean isIterationStatement()
        {
            return true;
        }
    }

    static public final class WithStatementNode extends StatementNode
    {
        public ExpressionNode expression;
        public StatementNode substatement;
        public Symbol semNsFrame = null;

        public WithStatementNode(ExpressionNode expression, StatementNode substatement)
        {
            super();
            this.expression = expression;
            this.substatement = substatement;
        }
    }

    static public final class XMLListNode extends ExpressionNode
    {
        public Vector<XMLNode> nodes;

        public XMLListNode(Vector<XMLNode> nodes)
        {
            super();
            this.nodes = nodes;
        }
    }

    static public class XMLNode extends ExpressionNode
    {
    }

    static public final class XMLAttributeNode extends XMLNode
    {
        public String name;
        public Object value;

        public XMLAttributeNode(String name, Object value)
        {
            super();
            this.name = name;
            this.value = value;
        }
    }

    static public final class XMLElementNode extends XMLNode
    {
        public Object openName;
        public Object closeName;
        public Vector<XMLAttributeNode> attributes;
        public Vector<XMLNode> childNodes;

        public XMLElementNode(Object openName, Object closeName, Vector<XMLAttributeNode> attributes, Vector<XMLNode> childNodes)
        {
            super();
            this.openName = openName;
            this.closeName = closeName;
            this.attributes = attributes;
            this.childNodes = childNodes;
        }
    }

    static public final class XMLMarkupNode extends XMLNode
    {
        public String content;

        public XMLMarkupNode(String content)
        {
            super();
            this.content = content;
        }
    }

    static public final class XMLTextNode extends XMLNode
    {
        public Object content;

        public XMLTextNode(Object content)
        {
            super();
            this.content = content;
        }
    }
}