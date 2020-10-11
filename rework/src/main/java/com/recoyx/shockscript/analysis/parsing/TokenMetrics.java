package com.recoyx.shockscript.analysis.parsing;

import com.recoyx.shockscript.analysis.*;
import java.util.stream.IntStream;
import java.util.Vector;

public final class TokenMetrics
{
    public Token type = Token.EOF;
    public String stringValue = "";
    public double numberValue = Double.NaN;
    public boolean booleanValue = false;
    public String regExpFlags = "";
    public int start = 0;
    public int end = 0;
    public int firstLine = 1;
    public int lastLine = 1;

    public Span getSpan()
    {
        return new Span(firstLine, start, lastLine, end);
    }

    public void copyTo(TokenMetrics data2)
    {
        data2.type = this.type;
        data2.stringValue = this.stringValue;
        data2.numberValue = this.numberValue;
        data2.booleanValue = this.booleanValue;
        data2.regExpFlags = this.regExpFlags;
        data2.start = this.start;
        data2.end = this.end;
        data2.firstLine = this.firstLine;
        data2.lastLine = this.lastLine;
    }
}