package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.recoyx.sxc.semantics.errors.*;

public final class SignatureConsumer implements Iterable<SignatureParameter>
{
    private Symbol _signature;
    private int _minLength;
    private double _maxLength;
    private int _index;

    public SignatureConsumer(Symbol signature)
    {
        _signature = signature;
        var params = signature.params();
        _minLength = params == null ? 0 : params.size();
        var optParams = signature.optParams();
        _maxLength = _minLength + (optParams == null ? 0 : optParams.size());
    }

    public Symbol signature()
    {
        return _signature;
    }

    public int minLength()
    {
        return _minLength;
    }

    public double maxLength()
    {
        return !_signature.rest() ? _maxLength : Double.POSITIVE_INFINITY;
    }

    public int index()
    {
        return _index;
    }

    public SignatureParameter shift()
    {
        var s = _signature;
        if (_index >= _maxLength)
        {
            if (!s.rest())
                return null;
            else
            {
                ++_index;
                return new SignatureParameter("rest", null);
            }
        }
        else if (_index >= _minLength)
        {
            return new SignatureParameter("optional", s.optParams().get((this._index++) - this._minLength));
        }
        else
        {
            return new SignatureParameter("required", s.params().get(_index++));
        }
    }

    public Iterator<SignatureParameter> iterator()
    {
        return new ParameterIterator();
    }

    private class ParameterIterator implements Iterator<SignatureParameter>
    {
        public SignatureParameter parameter = null;

        public boolean hasNext()
        {
            parameter = shift();
            return parameter != null;
        }

        public SignatureParameter next()
        {
            return parameter;
        }
    }
}