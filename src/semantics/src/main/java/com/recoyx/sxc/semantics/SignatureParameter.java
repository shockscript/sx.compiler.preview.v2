package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.recoyx.sxc.semantics.errors.*;

public final class SignatureParameter
{
    private final String _position;
    private final Symbol _type;

    public SignatureParameter(String position, Symbol type)
    {
        _position = position;
        _type = type;
    }

    /**
     * One of the strings "required", "optional" and "rest".
     */
    public String position()
    {
        return _position;
    }

    /**
     * If the position is <code>rest</code>, this property is undefined.
     */
    public Symbol type()
    {
        return _type;
    }
}