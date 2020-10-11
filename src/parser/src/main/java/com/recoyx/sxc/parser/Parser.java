package com.recoyx.sxc.parser;

import com.recoyx.sxc.util.VectorUtils;
import java.util.HashMap;
import java.util.Vector;
import java.util.stream.IntStream;

public final class Parser
{
    static public final class State
    {
        /**
        * @private
        */
        protected int index = 0;

        /**
        * @private
        */
        protected int line = 1;

        /**
        * @private
        */
        protected int numLineStarts = 0;

        /**
        * @private
        */
        protected Lexer.Mode lexerMode = Lexer.Mode.NORMAL;

        /**
        * @private
        */
        protected TokenMetrics token = new TokenMetrics();

        /**
        * @private
        */
        protected TokenMetrics previousToken = new TokenMetrics();

        /**
        * @private
        */
        protected int numComments;

        /**
        * @private
        */
        protected int numProblems = -1;

        /**
        * @private
        */
        protected int numLocations;

        /**
        * @private
        */
        protected int numCurlySymbols;

        /**
        * @private
        */
        protected int numFunctionFlags;

        /**
        * @private
        */
        protected int numSubscripts = -1;

        protected State()
        {
        }
    }

    static public final class Context
    {
        public boolean atTopLevelProgram = false;
        public boolean atPackageFrame = false;
        public boolean atClassFrame = false;
        public boolean atEnumFrame = false;
        public boolean atInterfaceFrame = false;
        public boolean atConstructorBlock = false;
        public boolean foundSuperStatement = false;
        public boolean underFunction = false;
        public HashMap<String, Ast.StatementNode> labels = null;
        public String classLocalName = "";
        public Ast.Node lastBreakableStatement = null;
        public Ast.Node lastContinuableStatement = null;
        public String nextLoopLabel = "";

        public void initLabels()
        {
            labels = labels == null ? new HashMap<>() : null;
        }

        public Parser.Context duplicate()
        {
            var context = new Parser.Context();
            context.underFunction = underFunction;
            if (labels != null)
            {
                context.labels = (HashMap<String, Ast.StatementNode>) labels.clone();
            }
            context.lastBreakableStatement = lastBreakableStatement;
            context.lastContinuableStatement = lastContinuableStatement;
            return context;
        }
    }

    private UnderlyingParser _privateParser;
    private Lexer _lexer;

    public Parser(String argument)
    {
        this(new Script(argument));
    }

    public Parser(Script script)
    {
        this(new Lexer(script));
    }

    public Parser(Lexer lexer)
    {
        try
        {
            lexer.shift();
        }
        catch (Problem problem)
        {
        }
        _lexer = lexer;
        _privateParser = new UnderlyingParser(lexer);
    }

    public Lexer lexer()
    {
        return _lexer;
    }

    public Script script()
    {
        return _privateParser.script;
    }

    public Parser.State state()
    {
        return _privateParser.state();
    }

    public void setState(Parser.State state)
    {
        _privateParser.setState(state);
    }

    public Ast.ProgramNode parseProgram()
    {
        _privateParser.clearState();
        Ast.ProgramNode program = null;
        try
        {
            program = _privateParser.parseProgram();
        }
        catch (Problem exc)
        {
        }
        return script().invalidated() ? null : program;
    }

    public Ast.ExpressionNode parseTypeAnnotation()
    {
        _privateParser.clearState();
        Ast.ExpressionNode node = null;
        try
        {
            node = _privateParser.parseTypeAnnotation();
        }
        catch (Problem exc)
        {
        }
        return script().invalidated() ? null : node;
    }

    public Vector<Ast.TypeIdNode> parseTypedIdentifierList()
    {
        _privateParser.clearState();
        Vector<Ast.TypeIdNode> list = new Vector<>();
        try
        {
            do
            {
                list.add(_privateParser.parseTypedIdentifier());
            }
            while (_privateParser.consume(Token.COMMA));
        }
        catch (Problem exc)
        {
        }
        return script().invalidated() ? null : list;
    }
}