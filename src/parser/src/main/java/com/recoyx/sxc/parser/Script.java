package com.recoyx.sxc.parser;

import com.recoyx.sxc.util.IntVector;
import com.recoyx.sxc.util.VectorUtils;
import java.util.Vector;

public final class Script
{
    public Vector<Ast.CommentNode> comments = new Vector<>();
    public Vector<Script> subscripts = null;
    public Vector<Problem> problems = new Vector<>();
    protected IntVector lineStarts = new IntVector();
    private String _source;
    private String _url;
    private boolean _invalidated;

    public Script(String source, String url)
    {
        _source = source;
        _url = url;
        lineStarts.push(0);
        lineStarts.push(0);
    }

    public Script(String source)
    {
        this(source, null);
    }

    public String source()
    {
        return _source;
    }

    public String url()
    {
        return _url;
    }

    public boolean invalidated()
    {
        return _invalidated;
    }

    public int getLineStart(int lineNum)
    {
        return lineStarts.get(lineNum);
    }

    public int getLineStartCodeUnits(int lineNum)
    {
        return _source.offsetByCodePoints(0, getLineStart(lineNum));
    }

    public int getLineIndentation(int lineNum)
    {
        int lineStart = this.getLineStartCodeUnits(lineNum);
        int i = 0;
        var str = _source.substring(lineStart);
        while (i != str.length() && SxcSourceCharacter.isWhiteSpace((int) str.charAt(i)))
        {
            ++i;
        }
        return i;
    }

    public Problem collectProblem(Problem problem)
    {
        if (!problem.type().equals("warning"))
        {
            _invalidated = true;
        }
        problems.add(problem);
        return problem;
    }
}