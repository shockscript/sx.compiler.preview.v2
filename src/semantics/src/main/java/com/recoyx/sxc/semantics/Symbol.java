package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.recoyx.sxc.semantics.errors.*;
import com.recoyx.sxc.util.VectorUtils;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;

public class Symbol
{
    /**
     * @private
     */
    protected SymbolPool _pool;

    public SymbolPool pool()
    {
        return _pool;
    }

    public SymbolKind kind()
    {
        return SymbolKind.UNASSIGNED;
    }

    public boolean readOnly()
    {
        return true;
    }

    public void setReadOnly(boolean value)
    {
    }

    public boolean writeOnly()
    {
        return false;
    }

    public boolean deletable()
    {
        return false;
    }

    public boolean reassigned()
    {
        return false;
    }

    public void setReassigned(boolean value)
    {
    }

    public String prefix()
    {
        return null;
    }

    public String uri()
    {
        return null;
    }

    public Symbol name()
    {
        return null;
    }

    public Symbol superClass()
    {
        return null;
    }

    public boolean containsUndefined()
    {
        return false;
    }

    public boolean containsNull()
    {
        return false;
    }

    public Symbol escapeType()
    {
        return (Symbol) this;
    }

    public boolean equalsOrInstantiatedFrom(Symbol type)
    {
        return false;
    }

    public Symbol definitionType()
    {
        return null;
    }

    public Symbol definitionPackage()
    {
        return null;
    }

    public void setDefinitionPackage(Symbol pckg)
    {
    }

    public String packageID()
    {
        return null;
    }

    public Symbol constructorFunction()
    {
        return null;
    }

    public void setConstructorFunction(Symbol symbol)
    {
    }

    public Symbol staticType()
    {
        return null;
    }

    public void setStaticType(Symbol type)
    {
    }

    public Symbol signature()
    {
        return null;
    }

    public void setSignature(Symbol signature)
    {
    }

    public Symbol namespace()
    {
        return null;
    }

    public String localName()
    {
        return null;
    }

	public Vector<Symbol> nss()
	{
		return null;
	}

    public Vector<Symbol> getFrameListing()
    {
        return null;
    }

    public final boolean isPublicNamespace()
    {
        return kind() == SymbolKind.PUBLIC_NAMESPACE;
    }

    public final boolean isPrivateNamespace()
    {
        return kind() == SymbolKind.PRIVATE_NAMESPACE;
    }

    public final boolean isProtectedNamespace()
    {
        return kind() == SymbolKind.PROTECTED_NAMESPACE;
    }

    public final boolean isInternalNamespace()
    {
        return kind() == SymbolKind.INTERNAL_NAMESPACE;
    }

    public final boolean isExplicitNamespace()
    {
        return kind() == SymbolKind.EXPLICIT_NAMESPACE;
    }

	public final boolean isNss()
	{
		return kind() == SymbolKind.NAMESPACE_SET;
	}

    public final boolean isName()
    {
        return kind() == SymbolKind.NAME;
    }

    public final boolean isPackage()
    {
        return kind() == SymbolKind.PACKAGE;
    }

    public final boolean isAnyType()
    {
        return kind() == SymbolKind.ANY_TYPE;
    }

    public final boolean isVoidType()
    {
        return kind() == SymbolKind.VOID_TYPE;
    }

    public final boolean isNullType()
    {
        return kind() == SymbolKind.NULL_TYPE;
    }

    public final boolean isEnumType()
    {
        return kind() == SymbolKind.ENUM_TYPE;
    }

    public final boolean isInstantiatedType()
    {
        return kind() == SymbolKind.INSTANTIATED_TYPE;
    }

    public final boolean isFunctionType()
    {
        return kind() == SymbolKind.FUNCTION_TYPE;
    }

    public final boolean isTupleType()
    {
        return kind() == SymbolKind.TUPLE_TYPE;
    }

    public final boolean isNullableType()
    {
        return kind() == SymbolKind.NULLABLE_TYPE;
    }

    public final boolean isTypeParameter()
    {
        return kind() == SymbolKind.TYPE_PARAMETER;
    }

    public final boolean isVerifyingType()
    {
        return kind() == SymbolKind.VERIFYING_TYPE;
    }

    public final boolean isBlockFrame()
    {
        return kind() == SymbolKind.BLOCK_FRAME;
    }

    public final boolean isClassFrame()
    {
        return kind() == SymbolKind.CLASS_FRAME;
    }

    public final boolean isEnumFrame()
    {
        return kind() == SymbolKind.ENUM_FRAME;
    }

    public final boolean isInterfaceFrame()
    {
        return kind() == SymbolKind.INTERFACE_FRAME;
    }

    public final boolean isPackageFrame()
    {
        return kind() == SymbolKind.PACKAGE_FRAME;
    }

    public final boolean isParameterFrame()
    {
        return kind() == SymbolKind.PARAMETER_FRAME;
    }

    public final boolean isWithFrame()
    {
        return kind() == SymbolKind.WITH_FRAME;
    }

    public final boolean isForFrame()
    {
        return kind() == SymbolKind.FOR_FRAME;
    }

    public final boolean isConditionFrame()
    {
        return kind() == SymbolKind.CONDITION_FRAME;
    }

    public final boolean isFunction()
    {
        return kind() == SymbolKind.FUNCTION;
    }

    public final boolean isVariableProperty()
    {
        return kind() == SymbolKind.VARIABLE_PROPERTY;
    }

    public final boolean isVirtualProperty()
    {
        return kind() == SymbolKind.VIRTUAL_PROPERTY;
    }

    public final boolean isThis()
    {
        return kind() == SymbolKind.THIS;
    }

    public final boolean isTupleElement()
    {
        return kind() == SymbolKind.TUPLE_ELEMENT;
    }

    public final boolean isBooleanConstantValue()
    {
        return kind() == SymbolKind.BOOLEAN_CONSTANT;
    }

    public final boolean isEnumConstantValue()
    {
        return kind() == SymbolKind.ENUM_CONSTANT;
    }

    public final boolean isIntConstantValue()
    {
        return kind() == SymbolKind.INT_CONSTANT;
    }

    public final boolean isNamespaceConstantValue()
    {
        return kind() == SymbolKind.NAMESPACE_CONSTANT;
    }

    public final boolean isNullConstantValue()
    {
        return kind() == SymbolKind.NULL_CONSTANT;
    }

    public final boolean isNumberConstantValue()
    {
        return kind() == SymbolKind.NUMBER_CONSTANT;
    }

    public final boolean isStringConstantValue()
    {
        return kind() == SymbolKind.STRING_CONSTANT;
    }

    public final boolean isUndefinedConstantValue()
    {
        return kind() == SymbolKind.UNDEFINED_CONSTANT;
    }

    public final boolean isUnsignedIntConstantValue()
    {
        return kind() == SymbolKind.UNSIGNED_INT_CONSTANT;
    }

    public final boolean isBigIntConstantValue()
    {
        return kind() == SymbolKind.BIG_INT_CONSTANT;
    }

    public final boolean isConversionResult()
    {
        return kind() == SymbolKind.CONVERSION_RESULT;
    }

    public final boolean isAsConversion()
    {
        return kind() == SymbolKind.AS_CONVERSION;
    }

    public final boolean isCallConversion()
    {
        return kind() == SymbolKind.CALL_CONVERSION;
    }

    public final boolean isPackageProperty()
    {
        return kind() == SymbolKind.PACKAGE_PROPERTY;
    }

    public final boolean isFrameProperty()
    {
        return kind() == SymbolKind.FRAME_PROPERTY;
    }

    public final boolean isObjectProperty()
    {
        return kind() == SymbolKind.OBJECT_PROPERTY;
    }

    public final boolean isObjectProxyProperty()
    {
        return kind() == SymbolKind.OBJECT_DYNAMIC_PROPERTY;
    }

    public final boolean isObjectAttribute()
    {
        return kind() == SymbolKind.OBJECT_ATTRIBUTE;
    }

    public final boolean isObjectFilter()
    {
        return kind() == SymbolKind.OBJECT_FILTER;
    }

    public final boolean isObjectDescendants()
    {
        return kind() == SymbolKind.OBJECT_DESCENDANTS;
    }

    public final boolean isTypeProperty()
    {
        return kind() == SymbolKind.TYPE_PROPERTY;
    }

    public final boolean isBooleanLogicalAndOr()
    {
        return kind() == SymbolKind.BOOLEAN_LOGICAL_AND_OR;
    }

    public final boolean isType()
    {
        return this instanceof Types.Type;
    }

    public final boolean isFrame()
    {
        return this instanceof Frames.Frame;
    }

    public boolean isClassType()
    {
        return false;
    }

    public boolean isInterfaceType()
    {
        return false;
    }

    public final boolean isNamespace()
    {
        return this instanceof Namespace;
    }

    public final boolean isValue()
    {
        return this instanceof Values.Value;
    }

    public final boolean isConstantValue()
    {
        return this instanceof Values.Constant;
    }

    public Vector<Symbol> typeParams()
    {
        return null;
    }

    public void setTypeParams(Vector<Symbol> list)
    {
    }

    public void setTypeParams(Symbol[] list)
    {
        setTypeParams(VectorUtils.fromArray(list));
    }

    public Vector<Symbol> subclasses()
    {
        return null;
    }

    public void initSubclasses()
    {
    }

    public Vector<Symbol> implementedInterfaces()
    {
        return null;
    }

    public void initImplementedInterfaces()
    {
    }

    public Names ownNames()
    {
        return null;
    }

    /**
     * Indicates whether object literal is publicly applicable for a Type or not.
     */
    public boolean isInitialisable()
    {
        return false;
    }

    public void setIsInitialisable(boolean value)
    {
    }

    /**
     * Indicates whether new operator is publicly applicable to a Type or not.
     */
    public boolean isConstructable()
    {
        return false;
    }

    public void setIsConstructable(boolean value)
    {
    }

    public boolean markFinal()
    {
        return false;
    }

    public void setMarkFinal(boolean value)
    {
    }

    public boolean classCfgPrimitive()
    {
        return false;
    }

    public boolean classCfgUnion()
    {
        return false;
    }

    public boolean markNative()
    {
        return false;
    }

    public void setMarkNative(boolean value)
    {
    }

    public boolean markOverride()
    {
        return false;
    }

    public void setMarkOverride(boolean value)
    {
    }

    public boolean markDynamic()
    {
        return false;
    }

    public void setMarkDynamic(boolean value)
    {
    }

    public Symbol conversion()
    {
        return null;
    }

    public Delegate delegate()
    {
        return null;
    }

    public void setDelegate(Delegate delegate)
    {
    }

    public Symbol overType()
    {
        return null;
    }

    public boolean isSubtypeOf(Symbol type)
    {
        return false;
    }

    public Symbol publicNamespace()
    {
        return null;
    }

    public void setPublicNamespace(Symbol ns)
    {
    }

    public Symbol privateNamespace()
    {
        return null;
    }

    public void setPrivateNamespace(Symbol ns)
    {
    }

    public Symbol protectedNamespace()
    {
        return null;
    }

    public void setProtectedNamespace(Symbol ns)
    {
    }

    public Symbol internalNamespace()
    {
        return null;
    }

    public void setInternalNamespace(Symbol ns)
    {
    }

    public boolean containsTypeParams()
    {
        return false;
    }

    public Symbol baseType()
    {
        return null;
    }

    public Symbol basePackage()
    {
        return null;
    }

    public Symbol baseObject()
    {
        return null;
    }

    public Symbol baseFrame()
    {
        return null;
    }

    public Symbol accessingProperty()
    {
        return null;
    }

    public ProxyPropertyTrait accessingTrait()
    {
        return null;
    }

    public Vector<Symbol> implementors()
    {
        return null;
    }

    public void initImplementors()
    {
    }

    public Vector<Symbol> superInterfaces()
    {
        return null;
    }

    public void initSuperInterfaces()
    {
    }

    public Vector<Symbol> subInterfaces()
    {
        return null;
    }

    public void initSubInterfaces()
    {
    }

    public Symbol origin()
    {
        return null;
    }

    public Vector<Symbol> arguments()
    {
        return null;
    }

    public boolean isInstantiated()
    {
        return false;
    }

    public Symbol numericType()
    {
        return null;
    }

    public void setNumericType(Symbol type)
    {
    }

    public boolean isFlagEnum()
    {
        return false;
    }

    public Vector<Symbol> params()
    {
        return null;
    }

    public Vector<Symbol> optParams()
    {
        return null;
    }

    public boolean rest()
    {
        return false;
    }

    public Symbol result()
    {
        return null;
    }

    public Vector<Symbol> tupleElements()
    {
        return null;
    }

    public Symbol parentFrame()
    {
        return null;
    }

    public void setParentFrame(Symbol frame)
    {
    }

    public Vector<Symbol> openNamespaceList()
    {
        return null;
    }

    public Symbol defaultNamespace()
    {
        return null;
    }

    public void setDefaultNamespace(Symbol ns)
    {
    }

    public Vector<Symbol> importPackageList()
    {
        return null;
    }

    public Activation activation()
    {
        return null;
    }

    public void setActivation(Activation obj)
    {
    }

    public Symbol symbol()
    {
        return null;
    }

    public boolean isBoundMethod()
    {
        return false;
    }

    public void setIsBoundMethod(boolean value)
    {
    }

    public Symbol parameterThis()
    {
        return null;
    }

    public int elementIndex()
    {
        return 0;
    }

    public Symbol getTypeParam(String name)
    {
        var seq = typeParams();
        if (seq == null)
        {
            return null;
        }
        for (var p : seq)
        {
            if (p.name().localName().equals(name))
            {
                return p;
            }
        }
        return null;
    }

    public double numberValue()
    {
        return 0.0;
    }

    public int intValue()
    {
        return 0;
    }

    public UnsignedInteger uintValue()
    {
        return UnsignedInteger.valueOf(0);
    }

    public java.math.BigInteger bigIntValue()
    {
        return java.math.BigInteger.ZERO;
    }

    public Object enumValue()
    {
    	return null;
    }

	public Symbol enumOr(Symbol arg)
	{
		return null;
	}

	public Symbol enumXor(Symbol arg)
	{
		return null;
	}

	public Symbol enumAnd(Symbol arg)
	{
		return null;
	}

    public boolean booleanValue()
    {
        return false;
    }

    public String stringValue()
    {
        return "";
    }

    public Class<?> javaClass()
    {
    	if (this == pool().numberType)
    		return Double.class;
	    else if (this == pool().uintType)
			return UnsignedInteger.class;
	    else if (this == pool().intType)
			return Integer.class;
	    else if (this == pool().bigIntType)
			return java.math.BigInteger.class;
	    return null;
    }

	public Object boxedNumberValue()
	{
		if (this instanceof Values.NumberConstant)
			return ((Values.NumberConstant) this).numberValue();
		else if (this instanceof Values.IntConstant)
			return ((Values.IntConstant) this).intValue();
		else if (this instanceof Values.UnsignedIntConstant)
			return ((Values.UnsignedIntConstant) this).uintValue();
		else if (this instanceof Values.BigIntConstant)
			return ((Values.BigIntConstant) this).bigIntValue();
		else
			return null;
	}

    public Vector<MethodOverload> methodOverloads()
    {
        return null;
    }

    public void initMethodOverloads()
    {
    }

    public ConversionKind conversionKind()
    {
        return null;
    }

    public Symbol defaultType()
    {
        return null;
    }

    public void setDefaultType(Symbol type)
    {
    }

    public Symbol defaultValue()
    {
        return null;
    }

    public Symbol ownerVirtualProperty()
    {
        return null;
    }

    public void setOwnerVirtualProperty(Symbol property)
    {
    }

    public HashMap<Symbol, Symbol> overriders()
    {
        return null;
    }

    public void initOverriders()
    {
    }

    public Symbol initialValue()
    {
        return null;
    }

    public void setInitialValue(Symbol value)
    {
    }

    public Symbol getter()
    {
        return null;
    }

    public void setGetter(Symbol getter)
    {
    }

    public Symbol setter()
    {
        return null;
    }

    public void setSetter(Symbol setter)
    {
    }

    public boolean markYielding()
    {
        return false;
    }

    public void setMarkYielding(boolean value)
    {
    }

    public Symbol replaceType(Symbol argument)
    {
        if (this.isVariableProperty())
        {
            return this.pool().createInstantiatedVariableProperty(this, argument);
        }
        else if (this.isVirtualProperty())
        {
            return this.pool().createInstantiatedVirtualProperty(this, argument);
        }
        else if (this.isFunction())
        {
            return this.pool().createInstantiatedFunction(this, argument);
        }
        else if (this.isTypeProperty())
        {
            return this.pool().createTypeProperty(this.baseType(), this.accessingProperty().replaceType(argument));
        }
        return this;
    }

    public Symbol accessProxyProperty()
    {
        return null;
    }

    public Symbol accessAttribute()
    {
        return null;
    }

    public Symbol objectFilter()
    {
        return null;
    }

    public Symbol objectDescendants()
    {
        return null;
    }

    public Symbol lookupName(Symbol name)
        throws AmbiguousReferenceError
    {
        return null;
    }

    public Symbol lookupMultiName(Vector<Symbol> namespaces, String localName)
        throws AmbiguousReferenceError
    {
        return null;
    }

    public void extend(Symbol type)
        throws InheritingNameConflictError
    {
    }

    public void implement(Symbol type)
    {
    }

    public void verifyInterfaceImplementations(InterfaceImplementationEvents errorHandler)
    {
    }

    public Symbol getConstant(String id)
    {
        return null;
    }

    public void setConstant(String id, Symbol value)
    {
    }

    public Symbol explicitConversion(Symbol toType)
    {
        return null;
    }

    public Symbol implicitConversion(Symbol toType)
    {
        return null;
    }

    public Symbol constantImplicitConversion(Symbol toType)
    {
        return null;
    }

    public void importPackage(Symbol pckg)
    {
    }

    public void importPackage(Symbol pckg, boolean openPublic)
    {
    }

    public void openNamespace(Symbol ns)
    {
    }

    public Symbol searchPublicNamespace()
    {
        return null;
    }

    public Symbol searchPrivateNamespace()
    {
        return null;
    }

    public Symbol searchProtectedNamespace()
    {
        return null;
    }

    public Symbol searchInternalNamespace()
    {
        return null;
    }

    public void override(Delegate delegate)
        throws IncompatibleOverrideSignatureError
            ,  NoMethodToOverrideError
            ,  OverridingFinalMethodError
    {
    }

    public void initEnumOperators(Symbol publicNs)
    {
    }

    public Iterable<Symbol> ascendingClassHierarchy()
    {
        var r = new Vector<Symbol>();
        for (var symbol = this; (symbol = symbol.superClass()) != null;)
        {
            r.insertElementAt(symbol, 0);
        }
        return r;
    }

    public Iterable<Symbol> descendingClassHierarchy()
    {
        var symbol = this;
        return new Iterable<Symbol>()
        {
            public Iterator<Symbol> iterator()
            {
                return new DescendingClassHierarchy(symbol);
            }
        };
    }

    /**
     * @private
     */
    protected final class DescendingClassHierarchy implements Iterator<Symbol>
    {
        Symbol symbol = null;

        public DescendingClassHierarchy(Symbol symbol)
        {
            this.symbol = symbol;
        }

        public boolean hasNext()
        {
            return symbol.superClass() != null;
        }

        public Symbol next()
        {
            return symbol = symbol.superClass();
        }
    }
}