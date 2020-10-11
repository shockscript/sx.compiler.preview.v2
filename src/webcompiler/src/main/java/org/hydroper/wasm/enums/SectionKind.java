package org.hydroper.wasm.enums;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum SectionKind
{
    NONE(-1),
    CUSTOM(0),
    TYPE(1),
    IMPORT(2),
    FUNCTION(3),
    TABLE(4),
    MEMORY(5),
    GLOBAL(6),
    EXPORT(7),
    START(8),
    ELEMENT(9),
    CODE(10),
    DATA(11);

    private static final Map<Integer, SectionKind> _fromValues;

    static
    {
        Map<Integer, SectionKind> elements = new HashMap<>();
        for (SectionKind value : values())
        {
            elements.put(value.valueOf(), value);
        }
        _fromValues = Collections.unmodifiableMap(elements);
    }

    private int _value;

    SectionKind(int value)
    {
        _value = value;
    }

    public SectionKind fromValue(int value)
    {
        return _fromValues.get(value);
    }

    public int valueOf()
    {
        return _value;
    }
}