package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.google.common.primitives.*;
import com.recoyx.sxc.semantics.errors.*;
import com.recoyx.sxc.util.VectorUtils;

public final class Values
{
    public static class Value extends Symbol
    {
        private Symbol _type;

        public Value(Symbol type)
        {
            _type = type;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.VALUE;
        }

        @Override
        public Symbol staticType()
        {
            return _type;
        }

        @Override
        public void setStaticType(Symbol type)
        {
            _type = type;
        }

        @Override
        public Symbol lookupName(Symbol name)
            throws AmbiguousReferenceError
        {
            var d = _type.delegate();
            var s = d == null ? null : d.lookupName(name);
            return s == null ? null : pool().createObjectProperty(this, s);
        }

        @Override
        public Symbol lookupMultiName(Vector<Symbol> namespaces, String localName)
            throws AmbiguousReferenceError
        {
            var d = _type.delegate();
            var s = d == null ? null : d.lookupMultiName(namespaces, localName);
            if (s != null)
            {
                return pool().createObjectProperty(this, s);
            }
            var trait = d == null ? null : d.searchProxyPropertyTrait();
            if (trait != null && pool().isNameType(trait.keyType()))
            {
                return pool().createObjectProxyProperty(this, trait);
            }
            return null; 
        }

        @Override
        public Symbol accessProxyProperty()
        {
            var d = _type.delegate();
            var trait = d == null ? null : d.searchProxyPropertyTrait();
            return trait == null ? null : pool().createObjectProxyProperty(this, trait);
        }

        @Override
        public Symbol accessAttribute()
        {
            var d = _type.delegate();
            var trait = d == null ? null : d.searchAttributeTrait();
            return trait == null ? null : pool().createObjectAttribute(this, trait);
        }

        @Override
        public Symbol objectFilter()
        {
            var d = _type.delegate();
            Symbol proxy = d == null ? null : d.searchFilterProxy();
            return proxy == null ? null : pool().createObjectFilter(this, proxy);
        }

        @Override
        public Symbol objectDescendants()
        {
            var d = _type.delegate();
            Symbol proxy = pool().validateGetDescendantsProxy(d == null ? null : d.lookupName(pool().proxyGetDescendantsName));
            return proxy == null ? null : pool().createObjectDescendants(this, proxy);
        }

        @Override
        public Symbol explicitConversion(Symbol toType)
        {
            var subconversion = implicitConversion(toType);
            if (subconversion != null)
            {
                return subconversion;
            }
            var pool = _pool;
            var fromType = _type;

            // subclass conversion
            if (fromType.isClassType())
            {
                if (toType.isSubtypeOf(fromType))
                {
                    return pool.createConversionResult(this, ConversionKind.SUBCLASS, toType);
                }
            }
            // sub-interface or implementor conversion
            else if (fromType.isInterfaceType())
            {
                if (toType.isClassType() && toType.isSubtypeOf(fromType))
                {
                    return pool.createConversionResult(this, ConversionKind.IMPLEMENTOR, toType);
                }
                else if (toType.isInterfaceType() && toType.isSubtypeOf(fromType))
                {
                    return pool.createConversionResult(this, ConversionKind.SUB_INTERFACE, toType);
                }
            }
            // enumTypedObject as NumericType
            else if (fromType.isEnumType() && toType == fromType.numericType())
            {
                return pool.createConversionResult(this, ConversionKind.ENUM_NUMBER, toType);
            }
            // typeParameterTypedValue as T
            else if (fromType.isTypeParameter())
            {
                return pool.createConversionResult(this, ConversionKind.FROM_TYPE_PARAMETER, toType);
            }

            // value as String
            if (toType == pool.stringType)
            {
                return pool.createConversionResult(this, ConversionKind.STRING, toType);
            }
            // ContravariantVector(vector)
            else if (toType.origin() == pool.vectorType && fromType.origin() == pool.vectorType && !toType.isNullableType() && !fromType.isNullableType())
            {
                if (toType.isSubtypeOf(fromType))
                {
                    return pool.createConversionResult(this, ConversionKind.CONTRAVARIANT_VECTOR, toType);
                }
            }
            // T(v)
            else if (toType.isTypeParameter())
            {
                return pool.createConversionResult(this, ConversionKind.TO_TYPE_PARAMETER, toType);
            }
            // Enum("constantName")
            else if (fromType == pool.stringType && toType.isEnumType())
            {
                return pool.createConversionResult(this, ConversionKind.FROM_STRING, toType);
            }

            return null;
        }

        @Override
        public Symbol implicitConversion(Symbol toType)
        {
            var pool = _pool;
            var fromType = _type;
            Symbol subconversion = null;

            if (toType == fromType)
            {
                return this;
            }
            // *
            else if (toType == pool.anyType)
            {
                return pool.createConversionResult(this, ConversionKind.ANY, toType);
            }
            // Numeric types
            else if (pool.isNumericType(fromType) && pool.isNumericType(toType))
            {
                return pool.createConversionResult(this, ConversionKind.NUMERIC_FROM_NUMERIC, toType);
            }
            // Covariant vector
            else if (toType.origin() == pool.vectorType && fromType.origin() == pool.vectorType && !toType.isNullableType() && !fromType.isNullableType())
            {
                if (fromType.isSubtypeOf(toType))
                {
                    return pool.createConversionResult(this, ConversionKind.COVARIANT_VECTOR, toType);
                }
            }
            // Super class
            else if (toType.isClassType() && fromType.isSubtypeOf(toType))
            {
                return pool.createConversionResult(this, ConversionKind.SUPER_CLASS, toType);
            }
            // Implemented interface or super interface
            else if (toType.isInterfaceType() && (fromType.isClassType() || fromType.isInterfaceType()) && fromType.isSubtypeOf(toType))
            {
                if (fromType.isInterfaceType())
                {
                    return pool.createConversionResult(this, ConversionKind.SUPER_INTERFACE, toType);
                }
                else
                {
                    return pool.createConversionResult(this, ConversionKind.IMPLEMENTED_INTERFACE, toType);
                }
            }
            // Nullable from non-nullable
            else if (toType.isNullableType())
            {
                subconversion = this.implicitConversion(toType.overType());
                if (subconversion != null)
                {
                    return pool.createConversionResult(subconversion, ConversionKind.NULLABLE, toType);
                }
            }
            // Non-nullable from nullable
            else if (!toType.containsUndefined() && fromType.isNullableType())
            {
                subconversion = _implicitMutatingConversion(fromType.overType(), toType);
                if (subconversion != null)
                {
                    return pool.createConversionResult(subconversion, ConversionKind.NON_NULLABLE, toType);
                }
            }
            else if (toType.isVerifyingType())
            {
                return pool.createConversionResult(this, ConversionKind.TO_VERIFYING_TYPE, toType);
            }

            // From *
            if (fromType == pool.anyType)
            {
                return pool.createConversionResult(this, ConversionKind.FROM_ANY, toType);
            }
            // String from char
            else if (fromType == pool.charType && toType == pool.stringType)
            {
                return pool.createConversionResult(this, ConversionKind.STRING, toType);
            }
            // null to nullable
            else if (fromType == pool.nullType && toType.containsNull())
            {
                return pool.createNullConstantValue(toType);
            }
            // invalidatedValue as AnyType
            else if (fromType.isVerifyingType())
            {
                return pool.createConversionResult(this, ConversionKind.FROM_VERIFYING_TYPE, toType);
            }

            return constantImplicitConversion(toType);
        }

        private Symbol _implicitMutatingConversion(Symbol fromType, Symbol toType)
        {
            var k = _type;
            _type = fromType;
            var conv = implicitConversion(toType);
            if (conv != null)
            {
                if (conv.isConversionResult())
                {
                    var list = new Vector<Symbol>();
                    while (conv.isConversionResult())
                    {
                        list.add(conv);
                        conv = conv.origin();
                    }
                    conv = this;
                    for (var conv2 : list)
                    {
                        conv = pool().createConversionResult(conv, conv2.conversionKind(), conv2.staticType());
                    }
                }
            }

            _type = k;
            return conv;
        }
    }

    public final static class ThisSymbol extends Value
    {
        private Activation _activation;

        public ThisSymbol(Symbol type)
        {
            super(type);
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.THIS;
        }

        @Override
        public boolean readOnly()
        {
            return true;
        }

        @Override
        public boolean writeOnly()
        {
            return false;
        }

        @Override
        public Activation activation()
        {
            return _activation;
        }

        @Override
        public void setActivation(Activation obj)
        {
            _activation = obj;
        }
    }

    public final static class PackageProperty extends Value
    {
        private Symbol _base;
        private Symbol _property;

        public PackageProperty(Symbol base, Symbol property, Symbol type)
        {
            super(type);
            _base = base;
            _property = property;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.PACKAGE_PROPERTY;
        }

        @Override
        public Symbol basePackage()
        {
            return _base;
        }

        @Override
        public Symbol accessingProperty()
        {
            return _property;
        }

        @Override
        public boolean readOnly()
        {
            return _property.readOnly();
        }

        @Override
        public boolean writeOnly()
        {
            return _property.writeOnly();
        }
    }

    public final static class TypeProperty extends Value
    {
        private Symbol _base;
        private Symbol _property;

        public TypeProperty(Symbol base, Symbol property, Symbol type)
        {
            super(type);
            _base = base;
            _property = property;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.TYPE_PROPERTY;
        }

        @Override
        public Symbol baseType()
        {
            return _base;
        }

        @Override
        public Symbol accessingProperty()
        {
            return _property;
        }

        @Override
        public boolean readOnly()
        {
            return _property.readOnly();
        }

        @Override
        public boolean writeOnly()
        {
            return _property.writeOnly();
        }
    }

    public final static class FrameProperty extends Value
    {
        private Symbol _base;
        private Symbol _property;

        public FrameProperty(Symbol base, Symbol property, Symbol type)
        {
            super(type);
            _base = base;
            _property = property;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.FRAME_PROPERTY;
        }

        @Override
        public Symbol baseFrame()
        {
            return _base;
        }

        @Override
        public Symbol accessingProperty()
        {
            return _property;
        }

        @Override
        public boolean readOnly()
        {
            return _property.readOnly();
        }

        @Override
        public boolean writeOnly()
        {
            return _property.writeOnly();
        }
    }

    public final static class ObjectProperty extends Value
    {
        private Symbol _base;
        private Symbol _property;

        public ObjectProperty(Symbol base, Symbol property, Symbol type)
        {
            super(type);
            _base = base;
            _property = property;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.OBJECT_PROPERTY;
        }

        @Override
        public Symbol baseObject()
        {
            return _base;
        }

        @Override
        public Symbol accessingProperty()
        {
            return _property;
        }

        @Override
        public boolean readOnly()
        {
            return _property.readOnly();
        }

        @Override
        public boolean writeOnly()
        {
            return _property.writeOnly();
        }
    }

    public final static class ObjectProxyProperty extends Value
    {
        private Symbol _base;
        private ProxyPropertyTrait _trait;

        public ObjectProxyProperty(Symbol base, ProxyPropertyTrait trait)
        {
            super(trait.valueType());
            _base = base;
            _trait = trait;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.OBJECT_PROXY_PROPERTY;
        }

        @Override
        public Symbol baseObject()
        {
            return _base;
        }

        @Override
        public ProxyPropertyTrait accessingTrait()
        {
            return _trait;
        }

        @Override
        public boolean readOnly()
        {
            return _trait.setMethod() == null;
        }

        @Override
        public boolean writeOnly()
        {
            return _trait.getMethod() == null;
        }

        @Override
        public boolean deletable()
        {
            return _trait.deleteMethod() != null;
        }
    }

    public final static class ObjectDynamicProperty extends Value
    {
        private Symbol _base;

        public ObjectDynamicProperty(Symbol base, Symbol anyType)
        {
            super(anyType);
            _base = base;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.OBJECT_DYNAMIC_PROPERTY;
        }

        @Override
        public Symbol baseObject()
        {
            return _base;
        }

        @Override
        public boolean readOnly()
        {
            return false;
        }

        @Override
        public boolean writeOnly()
        {
            return false;
        }

        @Override
        public boolean deletable()
        {
            return true;
        }
    }

    public final static class ObjectAttribute extends Value
    {
        private Symbol _base;
        private ProxyPropertyTrait _trait;

        public ObjectAttribute(Symbol base, ProxyPropertyTrait trait)
        {
            super(trait.valueType());
            _base = base;
            _trait = trait;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.OBJECT_DYNAMIC_PROPERTY;
        }

        @Override
        public Symbol baseObject()
        {
            return _base;
        }

        @Override
        public ProxyPropertyTrait accessingTrait()
        {
            return _trait;
        }

        @Override
        public boolean readOnly()
        {
            return _trait.setMethod() == null;
        }

        @Override
        public boolean writeOnly()
        {
            return _trait.getMethod() == null;
        }

        @Override
        public boolean deletable()
        {
            return _trait.deleteMethod() != null;
        }
    }

    public final static class ObjectFilter extends Value
    {
        private Symbol _base;
        private Symbol _property;

        public ObjectFilter(Symbol base, Symbol property)
        {
            super(property.signature().result());
            _base = base;
            _property = property;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.OBJECT_FILTER;
        }

        @Override
        public Symbol baseObject()
        {
            return _base;
        }

        @Override
        public Symbol accessingProperty()
        {
            return _property;
        }

        @Override
        public boolean readOnly()
        {
            return true;
        }

        @Override
        public boolean writeOnly()
        {
            return false;
        }
    }

    public final static class ObjectDescendants extends Value
    {
        private Symbol _base;
        private Symbol _property;

        public ObjectDescendants(Symbol base, Symbol property)
        {
            super(property.signature().result());
            _base = base;
            _property = property;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.OBJECT_DESCENDANTS;
        }

        @Override
        public Symbol baseObject()
        {
            return _base;
        }

        @Override
        public Symbol accessingProperty()
        {
            return _property;
        }

        @Override
        public boolean readOnly()
        {
            return true;
        }

        @Override
        public boolean writeOnly()
        {
            return false;
        }
    }

    public final static class TupleElement extends Value
    {
        private Symbol _base;
        private int _index;

        public TupleElement(Symbol base, int index)
        {
            super(base.staticType().escapeType().tupleElements().get(index));
            _base = base;
            _index = index;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.TUPLE_ELEMENT;
        }

        @Override
        public Symbol baseObject()
        {
            return _base;
        }

        @Override
        public int elementIndex()
        {
            return _index;
        }

        @Override
        public boolean readOnly()
        {
            return true;
        }

        @Override
        public boolean writeOnly()
        {
            return false;
        }
    }

    public final static class ConversionResult extends Value
    {
        private Symbol _origin;
        private ConversionKind _kind;

        public ConversionResult(Symbol origin, ConversionKind kind, Symbol toType)
        {
            super(toType);
            _origin = origin;
            _kind = kind;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.CONVERSION_RESULT;
        }

        @Override
        public Symbol origin()
        {
            return _origin;
        }

        @Override
        public ConversionKind conversionKind()
        {
            return _kind;
        }

        @Override
        public boolean readOnly()
        {
            return true;
        }

        @Override
        public boolean writeOnly()
        {
            return false;
        }
    }

    /**
     * Holds a conversion performed by the <code>as</code> operator.
     * This value must have a nullable type.
     */
    public final static class AsConversion extends Value
    {
        private Symbol _conversion;

        public AsConversion(Symbol conversion, Symbol type)
        {
            super(type);
            _conversion = conversion;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.AS_CONVERSION;
        }

        @Override
        public Symbol conversion()
        {
            return _conversion;
        }

        @Override
        public boolean readOnly()
        {
            return true;
        }

        @Override
        public boolean writeOnly()
        {
            return false;
        }
    }

    /**
     * Holds a conversion performed by the call operator.
     */
    public final static class CallConversion extends Value
    {
        private Symbol _conversion;

        public CallConversion(Symbol conversion)
        {
            super(conversion.staticType());
            _conversion = conversion;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.CALL_CONVERSION;
        }

        @Override
        public Symbol conversion()
        {
            return _conversion;
        }

        @Override
        public boolean readOnly()
        {
            return true;
        }

        @Override
        public boolean writeOnly()
        {
            return false;
        }
    }

    public static class Constant extends Value
    {
        public Constant(Symbol type)
        {
            super(type);
        }

        @Override
        public Symbol constantImplicitConversion(Symbol toType)
        {
            var fromType = staticType();
            if (fromType == toType)
            {
                return this;
            }
            var pool = _pool;
            if (this.isNumberConstantValue() && pool.isNumericType(toType.escapeType()))
            {
                var toType2 = toType.escapeType();

                if (toType2 == pool.numberType)
                {
                    return pool.createNumberConstantValue(this.numberValue(), toType);
                }

                if (toType2 == pool.uintType
                ||  toType2 == pool.charType)
                {
                    return pool.createUnsignedIntConstantValue(UnsignedInteger.fromIntBits((int) this.numberValue()), toType);
                }

                if (toType2 == pool.bigIntType)
                {
                    return pool.createBigIntConstantValue(java.math.BigInteger.valueOf((long) this.numberValue()), toType);
                }

                if (toType2 == pool.intType)
                {
                    return pool.createIntConstantValue((int) this.numberValue(), toType);
                }
            }
            else if (this.isStringConstantValue())
            {
                // var ch:Char = "x"
                if (toType.escapeType() == pool.charType)
                {
                    return pool.createUnsignedIntConstantValue(UnsignedInteger.valueOf(this.stringValue().codePointAt(0)), toType);
                }
                else if (toType.escapeType().isEnumType())
                {
                    var enum_type = toType.escapeType();
                    var enum_member = enum_type.getConstant(this.stringValue());
                    if (enum_member != null)
                    {
                        return enum_member;
                    }
                }
            }
            else if (this.isUndefinedConstantValue())
            {
                if (toType.containsUndefined())
                {
                    return pool.createUndefinedConstantValue(toType);
                }

                if (toType.containsNull())
                {
                    return pool.createNullConstantValue(toType);
                }

                if (toType.escapeType().isFlagEnum())
                {
                    return pool.createEnumConstantValue(0, toType);
                }
            }
            else if (this.isNullConstantValue() && toType.containsNull())
            {
                return pool.createNullConstantValue(toType);
            }
            return null;
        }
    }

    /**
     * BooleanConstant.
     *
     * <code>staticType()</code> is expected return Boolean or ?Boolean.
     */
    public final static class BooleanConstant extends Constant
    {
        private boolean _value;

        public BooleanConstant(boolean value, Symbol type)
        {
            super(type);
            _value = value;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.BOOLEAN_CONSTANT;
        }

        @Override
        public boolean booleanValue()
        {
            return _value;
        }
    }

    /**
     * EnumConstant.
     *
     * <code>staticType()</code> is expected return E or ?E.
     */
    public final static class EnumConstant extends Constant
    {
        private Object _value;

        public EnumConstant(Object value, Symbol type)
        {
            super(type);
            _value = value;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.ENUM_CONSTANT;
        }

        @Override
        public Object enumValue()
        {
            return _value;
        }

        @Override
        public Symbol enumOr(Symbol arg)
        {
        	if (_value instanceof Integer)
        		return pool().createEnumConstantValue(((int) _value) | ((int) ((EnumConstant) arg)._value), staticType());
        	else if (_value instanceof UnsignedInteger)
        		return pool().createEnumConstantValue(UnsignedInteger.valueOf(((UnsignedInteger) _value).longValue() | (((UnsignedInteger) ((EnumConstant) arg)._value).longValue())), staticType());
        	else
        		return pool().createEnumConstantValue(((java.math.BigInteger) _value).or((java.math.BigInteger) ((EnumConstant) arg)._value), staticType());
        }

        @Override
        public Symbol enumXor(Symbol arg)
        {
        	if (_value instanceof Integer)
        		return pool().createEnumConstantValue(((int) _value) ^ ((int) ((EnumConstant) arg)._value), staticType());
        	else if (_value instanceof UnsignedInteger)
        		return pool().createEnumConstantValue(UnsignedInteger.valueOf(((UnsignedInteger) _value).longValue() ^ ((UnsignedInteger) ((EnumConstant) arg)._value).longValue()), staticType());
        	else
        		return pool().createEnumConstantValue(((java.math.BigInteger) _value).xor((java.math.BigInteger) ((EnumConstant) arg)._value), staticType());
        }

        @Override
        public Symbol enumAnd(Symbol arg)
        {
        	if (_value instanceof Integer)
        		return pool().createEnumConstantValue(((int) _value) & ((int) ((EnumConstant) arg)._value), staticType());
        	else if (_value instanceof UnsignedInteger)
        		return pool().createEnumConstantValue(UnsignedInteger.valueOf(((UnsignedInteger) _value).longValue() & (((UnsignedInteger) ((EnumConstant) arg)._value).longValue())), staticType());
        	else
        		return pool().createEnumConstantValue(((java.math.BigInteger) _value).and((java.math.BigInteger) ((EnumConstant) arg)._value), staticType());
        }
    }

    /**
     * IntConstant.
     *
     * <code>staticType()</code> is expected return int or ?int.
     */
    public final static class IntConstant extends Constant
    {
        private int _value;

        public IntConstant(int value, Symbol type)
        {
            super(type);
            _value = value;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.INT_CONSTANT;
        }

        @Override
        public int intValue()
        {
            return _value;
        }
    }

    /**
     * UnsignedIntConstant.
     *
     * <code>staticType()</code> is expected return uint, ?uint, Char or ?Char.
     */
    public final static class UnsignedIntConstant extends Constant
    {
        private UnsignedInteger _value;

        public UnsignedIntConstant(UnsignedInteger value, Symbol type)
        {
            super(type);
            _value = value;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.UNSIGNED_INT_CONSTANT;
        }

        @Override
        public UnsignedInteger uintValue()
        {
            return _value;
        }
    }
    /**
     * BigIntConstant.
     *
     * <code>staticType()</code> is expected return BigInt or ?BigInt.
     */
    public final static class BigIntConstant extends Constant
    {
        private java.math.BigInteger _value;

        public BigIntConstant(java.math.BigInteger value, Symbol type)
        {
            super(type);
            _value = value;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.BIG_INT_CONSTANT;
        }

        @Override
        public java.math.BigInteger bigIntValue()
        {
            return _value;
        }
    }

    /**
     * NamespaceConstant.
     *
     * <code>staticType()</code> is expected to return Namespace.
     */
    public final static class NamespaceConstant extends Constant
    {
        private Symbol _namespace;

        public NamespaceConstant(Symbol namespace, Symbol type)
        {
            super(type);
            _namespace = namespace;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.NAMESPACE_CONSTANT;
        }

        @Override
        public Symbol namespace()
        {
            return _namespace;
        }
    }

    /**
     * NullConstant.
     *
     * <code>staticType()</code> is expected to return *, nullable type or the null type.
     */
    public final static class NullConstant extends Constant
    {
        public NullConstant(Symbol type)
        {
            super(type);
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.NULL_CONSTANT;
        }
    }

    /**
     * NumberConstant.
     *
     * <code>staticType()</code> is expected to return Number or ?Number.
     */
    public final static class NumberConstant extends Constant
    {
        private double _value;

        public NumberConstant(double value, Symbol type)
        {
            super(type);
            _value = value;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.NUMBER_CONSTANT;
        }

        @Override
        public double numberValue()
        {
            return _value;
        }
    }

    /**
     * StringConstant.
     *
     * <code>staticType()</code> is expected to return String or ?String.
     */
    public final static class StringConstant extends Constant
    {
        private String _value;

        public StringConstant(String value, Symbol type)
        {
            super(type);
            _value = value;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.STRING_CONSTANT;
        }

        @Override
        public String stringValue()
        {
            return _value;
        }
    }

    /**
     * UndefinedConstant.
     *
     * <code>staticType()</code> is expected to return *, void or ?T.
     */
    public final static class UndefinedConstant extends Constant
    {
        public UndefinedConstant(Symbol type)
        {
            super(type);
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.UNDEFINED_CONSTANT;
        }
    }

    /**
     * BooleanLogicalOrSymbol represents result of a logical and/or operation
     * where results are incompatible, thus resulting into Boolean.
     */
    public final static class BooleanLogicalAndOrSymbol extends Value
    {
        public BooleanLogicalAndOrSymbol(Symbol type)
        {
            super(type);
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.BOOLEAN_LOGICAL_AND_OR;
        }
    }
}