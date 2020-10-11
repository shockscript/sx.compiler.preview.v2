package com.recoyx.shockscript.analysis;

import com.recoyx.shockscript.analysis.parsing.Token;
import com.recoyx.shockscript.analysis.semantics.Symbol;
import com.recoyx.shockscript.analysis.util.*;
import java.util.HashMap;
import java.util.UnknownFormatConversionException;
import java.util.Vector;

public final class Problem extends RuntimeException
{
    static public enum Constants
    {
        UNEXPECTED_CHARACTER(0x400),
        UNEXPECTED_EOF(0x401),
        EXPECTING_BEFORE(0x402),
        ILLEGAL_ID_CHARACTER(0x403),
        ILLEGAL_LINE_BREAK_INSIDE_STRING(0x404),
        ILLEGAL_VOID_TYPE(0x405),
        UNEXPECTED_TOKEN(0x406),
        EXPECTING_TO_CLOSE(0x407),
        ILLEGAL_REQUIRED_PARAM(0x408),
        TUPLE_LENGTH_ONE(0x409),
        FUNCTION_OMITS_BODY(0x40A),
        UNEXPECTED_BEFORE(0x40B),
        UNALLOWED_HERE(0x40C),
        UNDEFINED_LABEL(0x40D),
        ILLEGAL_LABEL_STATEMENT(0x40E),
        UNEXPECTED_MULTIPLE_VARIABLES(0x40F),
        DUPLICATE_NAMESPACE_ATTRIBUTE(0x410),
        INCLUDE_PROCESSING_ERROR(0x411),
        ATTRIBUTE_UNALLOWED_FOR_DEFINITION(0x412),
        FUNCTION_MUST_NOT_SPECIFY_BODY(0x413),
        DIRECTIVE_NOT_ALLOWED_IN_INTERFACE(0x414),
        INVALID_ENUM_CONSTANT(0x415),
        RESERVED_90(0x416),
        CONSTRUCTOR_MUST_NOT_ANNOTATE_RETURN(0x417),
        ATTRIBUTE_ID_NOT_ALLOWED_HERE(0x418),
        FILTER_MUST_NOT_CONTAIN_YIELD(0x419),
        FUNCTION_MUST_NOT_CONTAIN_YIELD(0x41A),
        RESERVED_27(0x41B),
        RESERVED_28(0x41C),
        RESERVED_29(0x41D),
        RESERVED_30(0x41E),
        RESERVED_31(0x41F),
        AMBIGUOUS_REFERENCE(0x420),
        TYPE_NOT_FOUND(0x421),
        EMPTY_PACKAGE(0x422),
        NOT_A_GENERIC_TYPE(0x423),
        NOT_A_NAMESPACE_CONSTANT(0x424),
        NOT_A_CONSTANT_EXPRESSION(0x425),
        UNDEFINED_PROPERTY(0x426),
        UNDEFINED_PROPERTY_THROUGH_REFERENCE(0x427),
        UNDEFINED_ENUM_CONSTANT(0x428),
        COULD_NOT_RESOLVE_PROPERTY_HERE(0x429),
        RESERVED_NAMESPACE_NOT_FOUND(0x42A),
        RESERVED_32(0x42B),
        CANNOT_USE_PACKAGE_AS_VALUE(0x42C),
        CANNOT_USE_TYPE_AS_VALUE(0x42D),
        CANNOT_ACCESS_LEXICAL_PROPERTY(0x42E),
        EXPECTED_TYPE(0x42F),
        AMBIGUOUS_DYNAMIC_REFERENCE(0x430),
        UNDEFINED_DYNAMIC_PROPERTY(0x431),
        UNDEFINED_ATTRIBUTE(0x432),
        UNKNOWN_TYPE_INITIALISER(0x433),
        VARIABLE_NOT_FOUND(0x434),
        CANNOT_INITIALISE_DICTIONARY_OF_KEY_TYPE(0x435),
        INCOMPATIBLE_FIELD_KEY(0x436),
        CANNOT_INITIALISE_TYPE(0x437),
        UNALLOWED_TUPLE_SPREAD_OPERATOR(0x438),
        MAX_TUPLE_LENGTH_REACHED(0x439),
        CANNOT_CONSTRUCT_TYPE(0x43A),
        WRONG_NUM_ARGUMENTS(0x43B),
        RESERVED_400(0x43C),
        RESERVED_401(0x43D),
        RESERVED_402(0x43E),
        FUNCTION_SIGNATURE_MUST_MATCH(0x43F),
        UNTYPED_PARAMETER(0x440),
        UNTYPED_RESULT(0x441),
        NAMESPACE_CONFLICT(0x442),
        UNTYPED_EMBEDDED_CONTENT(0x443),
        UNSUPPORTED_EMBEDDED_CONTENT_TYPE(0x444),
        NO_OBJECT_ON_SUPER(0x445),
        NO_LIMIT_ON_SUPER(0x446),
        OBJECT_IS_NOT_FUNCTION(0x447),
        EXPRESSION_IS_NOT_A_TYPE_REFERENCE(0x448),
        UNSUPPORTED_FILTER(0x449),
        REFERENCE_IS_READ_ONLY(0x44A),
        REFERENCE_IS_WRITE_ONLY(0x44B),
        UNSUPPORTED_DESCENDANTS(0x44C),
        UNSUPPORTED_DELETE_OPERATOR(0x44D),
        VALUE_IS_NOT_NUMERIC(0x44E),
        UNSUPPORTED_OPERATOR(0x44F),
        RESERVED_403(0x450),
        TYPE_IS_NOT_CLASS(0x451),
        UNSUPPORTED_IN_OPERATOR(0x452),
        TERNARY_HAS_NO_RESULT_TYPE(0x453),
        UNSUPPORTED_ARRAY_PATTERN(0x454),
        ACCESS_MODIFIER_NOT_ALLOWED_HERE(0x455),
        OBJECT_LITERAL_MUST_INITIALISE_ONE_FIELD(0x456),
        UNTYPED_VARIABLE(0x457),
        UNSUPPORTED_ENUM_TYPE(0x458),
        INACCESSIBLE_PROPERTY(0x459),
        IDENTIFIER_IS_NOT_VARIABLE_PROPERTY(0x45A),
        NOT_ALL_PATHS_RETURN(0x45B),
        CONSTRUCTOR_ALREADY_DEFINED(0x45C),
        INVALID_GETTER_SIGNATURE(0x45D),
        INVALID_SETTER_SIGNATURE(0x45E),
        INCOMPATIBLE_OVERRIDE_SIGNATURE(0x45F),
        OVERRIDING_UNDEFINED_METHOD(0x460),
        TYPE_IS_NOT_INTERFACE(0x461),
        MISSING_IMPL_METHOD(0x462),
        MISSING_IMPL_GETTER(0x463),
        MISSING_IMPL_SETTER(0x464),
        WRONG_METHOD_IMPL_DEFINITION(0x465),
        WRONG_PROPERTY_IMPL_DEFINITION(0x466),
        UNION_VARIABLE_MUST_BE_NULLABLE(0x467),
        PRIMITIVE_CLASS_VARIABLE_MUST_BE_READ_ONLY(0x468),
        CANNOT_EXTEND_FINAL_CLASS(0x469),
        CONFLICT_INHERITING_ITRFC_NAME(0x46A),
        PRIMITIVE_CLASS_MUST_EXTEND_OBJECT(0x46B),
        TYPE_IS_NOT_ITERABLE(0x46C),
        WRONG_ITEM_TYPE(0x46D),
        STATEMENT_MUST_RESULT_VALUE(0x46E),
        UNTYPED_EXCEPTION(0x46F),
        UNTYPED_CASE(0x470),
        SUPER_CLASS_HAS_NO_DEFAULT_CONSTRUCTOR(0x471),
        OVERRIDING_FINAL_METHOD(0x472),
        INCOMPATIBLE_TYPES(0x473),
        UNALLOWED_PATTERN_ON_UNION_CLASS(0x474),
        UNALLOWED_UNION_WITH_SUPER_OPERATOR(0x475),
        UNALLOWED_ACCESS_WITH_SUPER_OPERATOR(0x476),
        PROCESSING_TYPE_ANNOTATION(0x477),
        EXPECTING_NUMERIC_DATA_TYPE(0x478),
        REDEFINING_SUPER_CLASS_PROPERTY(0x479),
        FUNCTION_MUST_NOT_CONTAIN_AWAIT(0x47a),
        FILTER_MUST_NOT_CONTAIN_AWAIT(0x47b),
        RESERVED_125(0x47c);

        private int _id;

        Constants(int id)
        {
            this._id = id;
        }

        static public int constantStart()
        {
            return 0x400;
        }

        public int valueOf()
        {
            return _id;
        }
    }

    static public final class Terms
    {
        static public final String HEX_DIGIT = "hexDigit";
        static public final String TYPE_ANNOTATION = "typeAnnotation";
        static public final String UNION = "union";
        static public final String TUPLE = "tuple";
        static public final String PARAMETER_LIST = "parameterList";
        static public final String ARGUMENT_LIST = "argumentList";
        static public final String EXPRESSION = "expression";
        static public final String STATEMENT = "statement";
        static public final String DIRECTIVE = "directive";
        static public final String PARENS = "parens";
        static public final String LINE_BREAK = "lineBreak";
        static public final String BLOCK = "block";
        static public final String SUPER_STATEMENT = "superStatement";
        static public final String INITIALISER = "initialiser";
    }

    static public class Emitter
    {
        static private String[] _problems = new String[]
        {
            "Unexpected character %1$s.",
            "Unexpected end of program.",
            "Expecting %1$s before %2$s.",
            "Character not permitted in identifier: %1$s.",
            "Line break is not permitted within string literal.",
            "Void type is not permitted here.",
            "Unexpected token.",
            "Expecting %1$s to close %2$s at line %3$s.",
            "Required parameters are not permitted after optional parameters.",
            "Tuple with length one is not permitted.",
            "Function must not omit its body.",
            "Unexpected %1$s before %2$s.",
            "%1$s is unallowed here.",
            "Undefined label %1$s.",
            "Label must not be specified for this statement.",
            "Multiple variables are not permitted here.",
            "Duplicate namespace attribute.",
            "Error on processing include directive.",
            "Attribute %1$s is not allowed for this definition.",
            "Function must not have body.",
            "Directive not permitted in interface block.",
            "Enum constant must not contain attribute or type annotation or destructure.",
            "",
            "Constructor definition must not contain return type annotation.",
            "Attribute identifier is not permitted here.",
            "Filter operator must not contain yield operator.",
            "Function must not contain yield operator.",
            "",
            "",
            "",
            "",
            "",
            "Ambiguous reference to %1$s.",
            "Type was not found or was not a compile-time constant: %1$s.",
            "No definitions matching %1$s could be found.",
            "Type is not generic: %1$s.",
            "Namespace is not a compile-time constant.",
            "Value is not a compile-time constant.",
            "Access of undefined property %1$s.",
            "Access of undefined property %1$s through reference with static type %2$s.",
            "Enum %1$s has no constant %2$s.",
            "Property could not be resolved here.",
            "%1$s namespace not found here.",
            "",
            "Cannot use package as value.",
            "Cannot use type as value.",
            "Cannot access lexical property here.",
            "Expecting value with static type %1$s.",
            "Ambiguous dynamic reference.",
            "Access of undefined property through reference with static type %1$s.",
            "Access of undefined attribute through reference with static type %1$s.",
            "Initialiser of unknown type.",
            "Variable property not found: %1$s.",
            "Cannot initialise Dictionary of key type %1$s.",
            "Incompatible field key.",
            "Type is not initialisable: %1$s.",
            "Rest operator is not permitted for tuple.",
            "Tuple length exceeded: %1$s.",
            "Type is not constructable: %1$s.",
            "Wrong number of arguments: expected %1$s.",
            "",
            "",
            "",
            "Function signature must match %1$s.",
            "Found untyped parameter.",
            "Function has untyped return.",
            "A conflict exists in namespace %1$s.",
            "Embedded content is untyped.",
            "Unsupported embedded content type: %1$s.",
            "Super operator has no object here.",
            "Super operator has no limit here.",
            "Object is not function typed.",
            "Expression is not a type reference.",
            "Filter operator is not supported by type %1$s.",
            "Reference is read-only.",
            "Reference is write-only.",
            "Descendants operator is not supported by type %1$s.",
            "Delete operator is not supported on type %1$s.",
            "Value is not number typed.",
            "Type %1$s does not support operator %2$s.",
            "",
            "Type is not a class: %1$s.",
            "\"in\" operator is not supported by type %1$s.",
            "Ternary operator has incompatible results.",
            "Array pattern is not applicable to type %1$s.",
            "Access modifier is not allowed here.",
            "Object literal for class %1$s must initialise exactly one field.",
            "Variable binding is not typed.",
            "Unsupported enumeration representation.",
            "Attempted access of inaccessible property %1$s through reference with static type %2$s.",
            "Identifier does not resolve to a variable property.",
            "Not all paths of this function return value.",
            "Constructor has been already defined.",
            "Invalid getter signature.",
            "Invalid setter signature.",
            "Expecting signature %1$s while overriding method %2$s.",
            "Overriding undefined method %1$s.",
            "Type is not an interface: %1$s.",
            "Missing to implement method %1$s: %2$s.",
            "Missing to implement getter %1$s: %2$s.",
            "Missing to implement setter %1$s: %2$s.",
            "Wrong implementation definition for %1$s: expected method.",
            "Wrong implementation definition for %1$s: expected virtual property.",
            "Variable binding in an union class must be nullable.",
            "Variable definition in a primitive class must be read-only.",
            "Cannot extend a class marked final.",
            "Conflict on inheriting a name %1$s from the interface %2$s.",
            "Primitive class must extend Object.",
            "Type is not iterable: %1$s.",
            "Wrong item type: expected %1$s.",
            "Return statement must return value.",
            "Untyped exception.",
            "Untyped case.",
            "Super class has no default constructor: super statement required.",
            "Cannot override a method marked final.",
            "Incompatible types: cannot convert %1$s to %2$s.",
            "Pattern is not allowed on union class.",
            "Unallowed structural union type with super operator.",
            "Access of unallowed symbol with super operator.",
            "Error while processing type annotation.",
            "Expecting numeric data type.",
            "Redefining property %1$s from super class.",
            "Function must not contain await operator.",
            "Filter operator must not contain await operator.",
            "",
        };

        private static HashMap<String, String> _terms = new HashMap<>();

        static
        {
            _terms.put("hexDigit", "hex. digit");
            _terms.put("typeAnnotation", "type annotation");
            _terms.put("union", "union");
            _terms.put("tuple", "tuple");
            _terms.put("parameterList", "parameter list");
            _terms.put("argumentList", "argument list");
            _terms.put("expression", "expression");
            _terms.put("statement", "statement");
            _terms.put("directive", "directive");
            _terms.put("parens", "parentheses group");
            _terms.put("lineBreak", "line break");
            _terms.put("block", "block");
            _terms.put("superStatement", "super statement");
            _terms.put("initialiser", "initialiser");
        }

        static private String[] _defaultTokens = new String[]
        {
            "end of program",
            "identifier",
            "string literal",
            "boolean literal",
            "numeric literal",
            "null literal",
            "this literal",
            "regular expression",
            "whitespace",
            "attribute value",
            "markup",
            "text",
            "name",
            "</",
            "/>",
        };

        static private String[] _punctuatorTokens = new String[]
        {
            "dot",
            "descendants",
            "ellipsis",
            "comma",
            "semicolon",
            "colon",
            "::",
            "(",
            ")",
            "[",
            "]",
            "{",
            "}",
            "attribute",
            "?",
            "!",
            "++",
            "--",
            "+",
            "-",
            "*",
            "/",
            "%",
            "&",
            "^",
            "|",
            "~",
            "<<",
            ">>",
            ">>>",
            "<",
            ">",
            "<=",
            ">=",
            "==",
            "!=",
            "&&",
            "||",
        };

        static private String[] _keywordTokens = new String[]
        {
            "as",
            "do",
            "if",
            "in",
            "is",
            "for",
            "new",
            "try",
            "use",
            "var",
            "case",
            "else",
            "void",
            "with",
            "break",
            "catch",
            "class",
            "const",
            "super",
            "throw",
            "while",
            "delete",
            "import",
            "public",
            "return",
            "switch",
            "typeof",
            "default",
            "finally",
            "package",
            "private",
            "continue",
            "function",
            "internal",
            "interface",
            "protected",
            "instanceof",
            "yield",
            "await",
        };

        static private String[] _assignmentTokens = new String[]
        {
            "=",
            "+=",
            "-=",
            "*=",
            "/=",
            "%=",
            "&=",
            "^=",
            "|=",
            "<<=",
            ">>=",
            ">>>=",
            "||=",
            "^^=",
            "&&=",
            "<<=",
            ">>=",
            ">>>=",
        };

        public String emitProblem(Problem p)
        {
            var stringBuilder = new Vector<Object>();
            for (var argument : p.arguments())
            {
                stringBuilder.add(emitArgument(argument));
            }
            return emitForm(p.errorId(), stringBuilder.toArray());
        }

        public String emitForm(Problem.Constants errorId, Object... arguments)
        {
            String msg = String.format(_problems[errorId.valueOf() - Problem.Constants.constantStart()], arguments);
            msg = new String(new int[]{msg.codePointAt(0)}, 0, 1) + msg.substring((msg.codePointAt(0) >> 16) == 0 ? 1 : 2);
            return msg;
        }

        public String emitArgument(Problem.Argument argument)
        {
            if (argument.isTokenArgument())
            {
                return emitToken(argument.tokenType());
            }
            if (argument.isQuoteArgument())
            {
                return "\"" + argument.quote() + "\"";
            }
            if (argument.isTermArgument())
            {
                return _terms.get(argument.termID());
            }
            if (argument.isNumberArgument())
            {
                var l = (long) argument.number();
                if (((double) l) == argument.number())
                {
                    return Long.toString(l);
                }
                return Double.toString(argument.number());
            }
            if (argument.isSymbolArgument())
            {
                var symbol = argument.symbol();
                if (symbol.isType())
                {
                    return symbol.toString();
                }
                else if (symbol.property() != null)
                {
                    return symbol.property().name().toString();
                }
                else
                {
                    return symbol.name().toString();
                }
            }
            return "";
        }

        public String emitToken(Token tokenType)
        {
            int value = tokenType.valueOf();
            if ((value >> 6) == 0)
            {
                return _defaultTokens[value];
            }
            if ((value >> 6) == 1)
            {
                return _punctuatorTokens[value - 0x40];
            }
            if ((value >> 7) == 1)
            {
                return "\"" + _keywordTokens[value - 0x80] + "\"";
            }
            return _assignmentTokens[value - 0x100];
        }
    }

    static public class Argument
    {
        static public Problem.Argument createQuote(String quote)
        {
            return new QuoteArgument(quote);
        }

        static public Problem.Argument createSymbol(Symbol symbol)
        {
            return new SymbolArgument(symbol);
        }

        static public Problem.Argument createType(Symbol symbol)
        {
            return new SymbolArgument(symbol);
        }

        static public Problem.Argument createTerm(String id)
        {
            return new TermArgument(id);
        }

        static public Problem.Argument createToken(Token tokenType)
        {
            return new TokenArgument(tokenType);
        }

        static public Problem.Argument createNumber(double number)
        {
            return new NumberArgument(number);
        }

        public final boolean isQuoteArgument()
        {
            return this instanceof QuoteArgument;
        }

        public final boolean isSymbolArgument()
        {
            return this instanceof SymbolArgument;
        }

        public final boolean isTermArgument()
        {
            return this instanceof TermArgument;
        }

        public final boolean isTokenArgument()
        {
            return this instanceof TokenArgument;
        }

        public final boolean isNumberArgument()
        {
            return this instanceof NumberArgument;
        }

        public String quote()
        {
            return "";
        }

        /**
        * Attaches a semantic Symbol to a Problem.
        */
        public Symbol symbol()
        {
            return null;
        }

        public String termID()
        {
            return "";
        }

        public Token tokenType()
        {
            return null;
        }

        public double number()
        {
            return 0;
        }

        static protected final class QuoteArgument extends Problem.Argument
        {
            private String _str;

            public QuoteArgument(String str)
            {
                _str = str;
            }

            @Override
            public String quote()
            {
                return _str;
            }
        }

        static protected final class SymbolArgument extends Problem.Argument
        {
            private Symbol _symbol;

            public SymbolArgument(Symbol symbol)
            {
                _symbol = symbol;
            }

            @Override
            public Symbol symbol()
            {
                return _symbol;
            }
        }

        static protected final class TermArgument extends Problem.Argument
        {
            private String _id;

            public TermArgument(String id)
            {
                _id = id;
            }

            @Override
            public String termID()
            {
                return _id;
            }
        }

        static protected final class TokenArgument extends Problem.Argument
        {
            private Token _type;

            public TokenArgument(Token type)
            {
                _type = type;
            }

            @Override
            public Token tokenType()
            {
                return _type;
            }
        }

        static protected final class NumberArgument extends Problem.Argument
        {
            private double _value;

            public NumberArgument(double value)
            {
                _value = value;
            }

            @Override
            public double number()
            {
                return _value;
            }
        }
    }

    private String _type;
    private Problem.Constants _errorId;
    private Span _span;
    private Script _script;
    private Problem.Argument[] _arguments;

    public Problem(String type, Problem.Constants errorId, Span span, Script script, Problem.Argument... rest)
    {
        super();
        _type = type;
        _errorId = errorId;
        _span = span;
        _script = script;
        _arguments = rest;
    }

    public String type()
    {
        return _type;
    }

    public Problem.Constants errorId()
    {
        return _errorId;
    }

    public Span span()
    {
        return _span;
    }

    public Script script()
    {
        return _script;
    }

    public Problem.Argument[] arguments()
    {
        return _arguments;
    }

    @Override
    public String toString()
    {
        var str1 = _type.equals("warning") ? "Warning: Warning #"
                :  _type.equals("syntaxError") ? "SyntaxError: Error #" : "VerifyError: Error #";
        var location = "\n   at " + (_script.url() == null || _script.url().equals("") ? "undefined" : _script.url())+":"+Integer.toString(_span.firstLine())+":"+Integer.toString(_span.start() - _script.getLineStart(this.span().firstLine()) + 1);
        return str1 + new Integer(_errorId.valueOf()).toString() + ": " + new Problem.Emitter().emitProblem(this) + location;
    }
}