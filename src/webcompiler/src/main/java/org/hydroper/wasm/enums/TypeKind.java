package org.hydroper.wasm.enums;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum TypeKind
{
    I32((byte) 0x7f),
    I64((byte) 0x7e),
    F32((byte) 0x7d),
    F64((byte) 0x7c),
    ANYFUNC((byte) 0x70),
    FUNC((byte) 0x60),
    VOID((byte) 0x40);

    private static final Map<Byte, TypeKind> _fromValues;

    static
    {
        Map<Byte, TypeKind> elements = new HashMap<>();
        for (TypeKind value : values())
        {
            elements.put(value.valueOf(), value);
        }
        _fromValues = Collections.unmodifiableMap(elements);
    }

    private byte _value;

    TypeKind(byte value)
    {
        _value = value;
    }

    public TypeKind fromValue(byte value)
    {
        return _fromValues.get(value);
    }

    public byte valueOf()
    {
        return _value;
    }
}