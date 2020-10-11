package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.recoyx.sxc.semantics.errors.*;

public class ProxyPropertyTrait
{
    static protected class Original extends ProxyPropertyTrait
    {
        private Symbol _keyType;
        private Symbol _valueType;
        private Symbol _getMethod;
        private Symbol _setMethod;
        private Symbol _deleteMethod;

        public Original(Symbol keyType, Symbol valueType)
        {
            _keyType = keyType;
            _valueType = valueType;
        }

        @Override
        public Symbol keyType()
        {
            return _keyType;
        }

        @Override
        public Symbol valueType()
        {
            return _valueType;
        }

        @Override
        public Symbol getMethod()
        {
            return _getMethod;
        }

        @Override
        public void setGetMethod(Symbol symbol)
        {
            _getMethod = symbol;
        }

        @Override
        public Symbol setMethod()
        {
            return _setMethod;
        }

        @Override
        public void setSetMethod(Symbol symbol)
        {
            _setMethod = symbol;
        }

        @Override
        public Symbol deleteMethod()
        {
            return _deleteMethod;
        }

        @Override
        public void setDeleteMethod(Symbol symbol)
        {
            _deleteMethod = symbol;
        }
    }

    static protected class Instantiated extends ProxyPropertyTrait
    {
        private ProxyPropertyTrait _origin;
        private Symbol _declaratorType;

        public Instantiated(ProxyPropertyTrait origin, Symbol declaratorType)
        {
            _origin = origin;
            _declaratorType = declaratorType;
        }

        @Override
        public boolean isInstantiated()
        {
            return true;
        }

        @Override
        public ProxyPropertyTrait origin()
        {
            return _origin;
        }

        @Override
        public Symbol declaratorType()
        {
            return _declaratorType;
        }

        @Override
        public Symbol keyType()
        {
            return _origin.keyType().replaceType(_declaratorType);
        }

        @Override
        public Symbol valueType()
        {
            return _origin.valueType().replaceType(_declaratorType);
        }

        @Override
        public Symbol getMethod()
        {
            var f = _origin.getMethod();
            return f != null ? f.replaceType(_declaratorType) : null;
        }

        @Override
        public Symbol setMethod()
        {
            var f = _origin.setMethod();
            return f != null ? f.replaceType(_declaratorType) : null;
        }

        @Override
        public Symbol deleteMethod()
        {
            var f = _origin.deleteMethod();
            return f != null ? f.replaceType(_declaratorType) : null;
        }
    }

    public final SymbolPool pool()
    {
        return keyType().pool();
    }

    public boolean isInstantiated()
    {
        return false;
    }

    public ProxyPropertyTrait origin()
    {
        return null;
    }

    public Symbol declaratorType()
    {
        return null;
    }

    public Symbol keyType()
    {
        return null;
    }

    public Symbol valueType()
    {
        return null;
    }

    public Symbol getMethod()
    {
        return null;
    }

    public void setGetMethod(Symbol symbol)
    {
    }

    public Symbol setMethod()
    {
        return null;
    }

    public void setSetMethod(Symbol symbol)
    {
    }

    public Symbol deleteMethod()
    {
        return null;
    }

    public void setDeleteMethod(Symbol symbol)
    {
    }
}