package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.google.common.primitives.*;
import com.recoyx.sxc.semantics.errors.*;
import com.recoyx.sxc.util.VectorUtils;

public final class SymbolPool
{
    public Symbol topFrame;
    public Symbol topPackage;
    public Symbol sxGlobalPackage;
    public Symbol proxyNamespace;
    public Symbol shockscriptNamespace;
    public Symbol anyType;
    public Symbol voidType;
    public Symbol nullType;
    public Symbol verifyingType;
    public Symbol objectType;
    public Symbol classType;
    public Symbol arrayType;
    public Symbol namespaceType;
    public Symbol qnameType;
    public Symbol stringType;
    public Symbol uintType;
    public Symbol intType;
    public Symbol bigIntType;
    public Symbol charType;
    public Symbol numberType;
    public Vector<Symbol> numericTypes;
    public Vector<Symbol> integerTypes;
    public Symbol booleanType;
    public Symbol functionType;
    public Symbol vectorType;
    public Symbol dictionaryType;
    public Symbol regExpType;
    public Symbol xmlType;
    public Symbol xmlListType;
    public Symbol byteArrayType;
    public Symbol generatorType;

    public Symbol proxyGetPropertyName;
    public Symbol proxySetPropertyName;
    public Symbol proxyDeletePropertyName;
    public Symbol proxyHasPropertyName;
    public Symbol proxyGetAttributeName;
    public Symbol proxySetAttributeName;
    public Symbol proxyDeleteAttributeName;
    public Symbol proxyGetDescendantsName;
    public Symbol proxyFilterName;
    public Symbol proxyCompareName;
    public Symbol proxyToBooleanName;
    public Symbol proxyDescendingCompareName;
    public Symbol proxyNextNameIndexName;
    public Symbol proxyNextNameName;
    public Symbol proxyNextValueName;
    public Symbol proxyNegateName;
    public Symbol proxyBitNotName;
    public Symbol proxyEqualsName;
    public Symbol proxyNotEqualsName;
    public Symbol proxyLtName;
    public Symbol proxyGtName;
    public Symbol proxyLeName;
    public Symbol proxyGeName;
    public Symbol proxyAddName;
    public Symbol proxySubtractName;
    public Symbol proxyMultiplyName;
    public Symbol proxyDivideName;
    public Symbol proxyRemainderName;
    public Symbol proxyBitAndName;
    public Symbol proxyBitXorName;
    public Symbol proxyBitOrName;
    public Symbol proxyLeftShiftName;
    public Symbol proxyRightShiftName;
    public Symbol proxyUnsignedRightShiftName;

    public HashMap<String, Symbol> packages = null;
    public HashMap<Symbol, Vector<Symbol>> typeInstantiations = null;

    private final HashMap<Symbol, Vector<Symbol>> _typeInstantiations = new HashMap<>();
    private final Vector<Vector<Symbol>> _functionTypes = new Vector<>();
    private final Vector<Vector<Symbol>> _tupleTypes = new Vector<>();
    private final Vector<Vector<Symbol>> _unionTypes = new Vector<>();
    private final HashMap<Symbol, Symbol> _nullableTypes = new HashMap<>();
    private final HashMap<Symbol, HashMap<Symbol, Symbol>> _instantiatedSymbols = new HashMap<>();
    private final HashMap<ProxyPropertyTrait, HashMap<Symbol, ProxyPropertyTrait>> _instantiatedTraits = new HashMap<>();
    private final Vector<Vector<Symbol>> _names = new Vector<>();
    private final HashMap<String, Symbol> _uriNamespaces = new HashMap<>();
    private final HashMap<String, Symbol> _packages = new HashMap<>();

    public SymbolPool()
    {
        this.packages = _packages;
        this.typeInstantiations = _typeInstantiations;
        this.topPackage = this.createPackage("");
        this.sxGlobalPackage = this.createPackage("sx.global");
        this.topFrame = this.createBlockFrame();
        this.topFrame.importPackage(this.topPackage);
        this.proxyNamespace = this.createExplicitNamespace("proxy", "http://shockscript.org/stl/sx/global/proxy");
        this.shockscriptNamespace = this.createExplicitNamespace("shockscript");

        this.anyType = this.createAnyType();
        this.voidType = this.createVoidType();
        this.nullType = this.createNullType();
        this.verifyingType = this.createVerifyingType();
        this.objectType = this.defineStlGlobalClass("Object", false);
        this.objectType.setIsInitialisable(true);
        this.classType = this.defineStlGlobalClass("Class");
        this.arrayType = this.defineStlGlobalClass("Array", false);
        this.namespaceType = this.defineStlGlobalClass("Namespace");
        this.qnameType = this.defineStlGlobalClass("QName");
        this.stringType = this.defineStlGlobalClass("String", true, true);
        this.booleanType = this.defineStlGlobalClass("Boolean", true, true);
        this.functionType = this.defineStlGlobalClass("Function");
        this.uintType = this.defineStlGlobalClass("uint", true, true);
        this.intType = this.defineStlGlobalClass("int", true, true);
        this.bigIntType = this.defineStlGlobalClass("BigInt", true, true);
        this.numberType = this.defineStlGlobalClass("Number", true, true);
        this.charType = this.defineStlGlobalClass("Char", true, true);
        numericTypes = VectorUtils.fromArray(new Symbol[] { uintType, intType, numberType, charType, bigIntType, });
        integerTypes = VectorUtils.fromArray(new Symbol[] { uintType, intType, charType, bigIntType, });
        for (var numeric_type : numericTypes)
            initNumericType(numeric_type);
        this.vectorType = this.defineStlGlobalClass("Vector", false);
        this.vectorType.setTypeParams(new Symbol[] {this.createTypeParameter(this.createName(this.vectorType.privateNamespace(), "E"), vectorType)});
        this.dictionaryType = this.defineStlGlobalClass("Dictionary", false);
        this.regExpType = this.defineStlGlobalClass("RegExp");
        this.xmlType = this.defineStlGlobalClass("XML");
        this.xmlListType = this.defineStlGlobalClass("XMLList");
        this.byteArrayType = this.defineStlGlobalClass("ByteArray");
        this.generatorType = this.defineStlGlobalClass("Generator");

        // initialise names
        this.proxyGetPropertyName = this.createName(this.proxyNamespace, "getProperty");
        this.proxySetPropertyName = this.createName(this.proxyNamespace, "setProperty");
        this.proxyDeletePropertyName = this.createName(this.proxyNamespace, "deleteProperty");
        this.proxyHasPropertyName = this.createName(this.proxyNamespace, "hasProperty");
        this.proxyGetAttributeName = this.createName(this.proxyNamespace, "getAttribute");
        this.proxySetAttributeName = this.createName(this.proxyNamespace, "setAttribute");
        this.proxyDeleteAttributeName = this.createName(this.proxyNamespace, "deleteAttribute");
        this.proxyGetDescendantsName = this.createName(this.proxyNamespace, "getDescendants");
        this.proxyFilterName = this.createName(this.proxyNamespace, "filter");
        this.proxyCompareName = this.createName(this.proxyNamespace, "fromExplicit");
        this.proxyToBooleanName = this.createName(this.proxyNamespace, "toBoolean");
        this.proxyDescendingCompareName = this.createName(this.proxyNamespace, "descendingCompare");
        this.proxyNextNameIndexName = this.createName(this.proxyNamespace, "nextNameIndex");
        this.proxyNextNameName = this.createName(this.proxyNamespace, "nextName");
        this.proxyNextValueName = this.createName(this.proxyNamespace, "nextValue");
        this.proxyNegateName = this.createName(this.proxyNamespace, "negate");
        this.proxyBitNotName = this.createName(this.proxyNamespace, "bitNot");
        this.proxyEqualsName = this.createName(this.proxyNamespace, "equals");
        this.proxyNotEqualsName = this.createName(this.proxyNamespace, "notEquals");
        this.proxyLtName = this.createName(this.proxyNamespace, "lessThan");
        this.proxyGtName = this.createName(this.proxyNamespace, "greaterThan");
        this.proxyLeName = this.createName(this.proxyNamespace, "lessOrEquals");
        this.proxyGeName = this.createName(this.proxyNamespace, "greaterOrEquals");
        this.proxyAddName = this.createName(this.proxyNamespace, "add");
        this.proxySubtractName = this.createName(this.proxyNamespace, "subtract");
        this.proxyMultiplyName = this.createName(this.proxyNamespace, "multiply");
        this.proxyDivideName = this.createName(this.proxyNamespace, "divide");
        this.proxyRemainderName = this.createName(this.proxyNamespace, "remainder");
        this.proxyBitAndName = this.createName(this.proxyNamespace, "bitAnd");
        this.proxyBitXorName = this.createName(this.proxyNamespace, "bitXor");
        this.proxyBitOrName = this.createName(this.proxyNamespace, "bitOr");
        this.proxyLeftShiftName = this.createName(this.proxyNamespace, "leftShift");
        this.proxyRightShiftName = this.createName(this.proxyNamespace, "rightShift");
        this.proxyUnsignedRightShiftName = this.createName(this.proxyNamespace, "unsignedRightShift");

        // "undefined" property
        var undefinedVariable = this.createVariableProperty(this.createName(this.sxGlobalPackage.publicNamespace(), "undefined"), true, this.voidType);
        undefinedVariable.setInitialValue(this.createUndefinedConstantValue());
        this.sxGlobalPackage.ownNames().defineName(undefinedVariable.name(), undefinedVariable);

        // "NaN" property
        var nanVariable = this.createVariableProperty(this.createName(this.sxGlobalPackage.publicNamespace(), "NaN"), true, this.numberType);
        nanVariable.setInitialValue(this.createNumberConstantValue(Double.NaN));
        this.sxGlobalPackage.ownNames().defineName(nanVariable.name(), nanVariable);

        // "Infinity" property
        var infVariable = this.createVariableProperty(this.createName(this.sxGlobalPackage.publicNamespace(), "Infinity"), true, this.numberType);
        infVariable.setInitialValue(this.createNumberConstantValue(Double.POSITIVE_INFINITY));
        this.sxGlobalPackage.ownNames().defineName(infVariable.name(), infVariable);

        // "proxy" property
        this.sxGlobalPackage.ownNames().defineName(this.createName(sxGlobalPackage.publicNamespace(), "proxy"), this.proxyNamespace);
    }

    public ProxyPropertyTrait createProxyPropertyTrait(Symbol keyType, Symbol valueType)
    {
        return new ProxyPropertyTrait.Original(keyType, valueType);
    }

    public ProxyPropertyTrait createInstantiatedProxyPropertyTrait(ProxyPropertyTrait origin, Symbol declaratorType)
    {
        var originList = _instantiatedTraits.get(origin);
        if (originList == null)
        {
            _instantiatedTraits.put(origin, originList = new HashMap<>());
        }
        var r = originList.get(declaratorType);
        if (r == null)
        {
            r = new ProxyPropertyTrait.Instantiated(origin, declaratorType);
            originList.put(declaratorType, r);
        }
        return r;
    }

    public Symbol createBlockFrame()
    {
        var frame = new Frames.BlockFrame();
        frame._pool = this;
        return frame;
    }

    public Symbol createClassFrame(Symbol symbol)
    {
        var frame = new Frames.ClassFrame(symbol);
        frame._pool = this;
        return frame;
    }

    public Symbol createEnumFrame(Symbol symbol)
    {
        var frame = new Frames.EnumFrame(symbol);
        frame._pool = this;
        return frame;
    }

    public Symbol createInterfaceFrame(Symbol symbol)
    {
        var frame = new Frames.InterfaceFrame(symbol);
        frame._pool = this;
        return frame;
    }

    public Symbol createPackageFrame(Symbol symbol)
    {
        var frame = new Frames.PackageFrame(symbol);
        frame._pool = this;
        return frame;
    }

    public Symbol createParameterFrame(Symbol parameterThis)
    {
        var frame = new Frames.ParameterFrame(parameterThis);
        frame._pool = this;
        return frame;
    }

    public Symbol createParameterFrame()
    {
        var frame = new Frames.ParameterFrame();
        frame._pool = this;
        return frame;
    }

    public Symbol createWithFrame(Symbol type)
    {
        var frame = new Frames.WithFrame();
        frame._pool = this;
        frame._symbol = createFrameProperty(frame, createVariableProperty(null, true, type));
        return frame;
    }

    public Symbol createForFrame()
    {
        var frame = new Frames.ForFrame();
        frame._pool = this;
        return frame;
    }

    public Symbol createConditionFrame()
    {
        var frame = new Frames.ConditionFrame();
        frame._pool = this;
        return frame;
    }

    public Symbol createFunction(Symbol name, Symbol signature)
    {
        var symbol = new SxcFunction(name, signature);
        symbol._pool = this;
        return symbol;
    }

    public Symbol createInstantiatedFunction(Symbol origin, Symbol declaratorType)
    {
        var originList = _instantiatedSymbols.get(origin);
        if (originList == null)
        {
            _instantiatedSymbols.put(origin, originList = new HashMap<>());
        }
        var r = originList.get(declaratorType);
        if (r == null)
        {
            r = new InstantiatedSxcFunction(origin, declaratorType, this);
            r._pool = this;
            originList.put(declaratorType, r);
        }
        return r;
    }

    public Symbol createName(Symbol namespace, String localName)
    {
        for (int i = _names.size(); i < localName.length(); ++i)
        {
            _names.add(new Vector<>());
        }
        var list = _names.get(localName.length() - 1);
        for (var name : list)
        {
            if (name.localName().equals(localName) && name.namespace() == namespace)
            {
                return name;
            }
        }
        Symbol name = new SxcName(namespace, localName);
        name._pool = this;
        list.add(name);
        return name;
    }

    public Symbol createReservedNamespace(String type, Symbol definitionPackage)
    {
        var ns = new ReservedNamespace(type, definitionPackage);
        ns._pool = this;
        return ns;
    }

    public Symbol createReservedNamespace(String type)
    {
        return createReservedNamespace(type, null);
    }

    public Symbol createExplicitNamespace(String prefix, String uri)
    {
        Symbol ns = null;
        uri = uri.equals("") ? null : uri;
        if (uri != null)
        {
            ns = _uriNamespaces.get(uri);
            if (ns != null)
            {
                return ns;
            }
        }
        ns = new ExplicitNamespace(prefix, uri);
        ns._pool = this;
        if (uri != null)
        {
            _uriNamespaces.put(uri, ns);
        }
        return ns;
    }

    public Symbol createExplicitNamespace(String prefix)
    {
        return createExplicitNamespace(prefix, "");
    }

	public Symbol createNss()
	{
		var nss = new NamespaceSet();
		nss._pool = this;
		return nss;
	}

    public Symbol createPackage(String id)
    {
        id = id == null ? "" : id;
        var symbol = _packages.get(id);
        if (symbol != null)
        {
            return symbol;
        }
        symbol = new Package(id);
        symbol.setPublicNamespace(createReservedNamespace("public", symbol));
        symbol.setInternalNamespace(createReservedNamespace("internal", symbol));
        symbol._pool = this;
        _packages.put(id, symbol);
        if (id != "")
        {
            var path = VectorUtils.fromArray(id.split("\\."));
            if (path.size() > 1)
            {
                String id2 = "";
                for (String part : path.subList(0, path.size() - 1))
                {
                    id2 += id2 != "" ? "." + part : part;
                }
                createPackage(id2);
            }
        }
        return symbol;
    }

    public Symbol createAnyType()
    {
        if (anyType != null)
        {
            return anyType;
        }
        Types.AnyType type = new Types.AnyType();
        type._pool = this;
        anyType = type;
        return type;
    }

    public Symbol createVoidType()
    {
        if (voidType != null)
        {
            return voidType;
        }
        Symbol type = new Types.VoidType();
        type._pool = this;
        voidType = type;
        return type;
    }

    public Symbol createNullType()
    {
        if (nullType != null)
        {
            return nullType;
        }
        Symbol type = new Types.NullType();
        type._pool = this;
        nullType = type;
        return type;
    }

    public Symbol createClassType(Symbol name, Boolean asFinal, Boolean asPrimitive, Boolean asUnion, Boolean asDynamic)
    {
        Symbol type = new Types.ClassType(name, asFinal, asPrimitive, asUnion, asDynamic);
        type._pool = this;
        type.setPrivateNamespace(createReservedNamespace("private"));
        type.setProtectedNamespace(createReservedNamespace("protected"));
        type.delegate().setInherit(objectType != null ? objectType.delegate() : null);
        return type;
    }

    public Symbol createClassType(Symbol name, Boolean asFinal, Boolean asPrimitive, Boolean asUnion)
    {
        return createClassType(name, asFinal, asPrimitive, asPrimitive, asUnion);
    }

    public Symbol createClassType(Symbol name, Boolean asFinal, Boolean asPrimitive)
    {
        return createClassType(name, asFinal, asPrimitive, false);
    }

    public Symbol createClassType(Symbol name, Boolean asFinal)
    {
        return createClassType(name, asFinal, false);
    }

    public Symbol createClassType(Symbol name)
    {
        return createClassType(name, false);
    }

    public Symbol createInterfaceType(Symbol name)
    {
        Symbol type = new Types.InterfaceType(name);
        type._pool = this;
        return type;
    }

    public Symbol createEnumType(Symbol name, Symbol numericType, boolean flagEnum, Symbol publicNs)
    {
        Symbol type = new Types.EnumType(name, numericType, flagEnum);
        type._pool = this;
        type.setPrivateNamespace(createReservedNamespace("private"));
        type.initEnumOperators(publicNs);
        type.delegate().setInherit(objectType != null ? objectType.delegate() : null);
        return type;
    }

    public Symbol createInstantiatedType(Symbol origin, Symbol[] arguments)
    {
        return createInstantiatedType(origin, VectorUtils.fromArray(arguments));
    }

    public Symbol createInstantiatedType(Symbol origin, Vector<Symbol> arguments)
    {
        Symbol type = null;
        var params = origin.typeParams();
        int i = 0;
        boolean equalsOrigin = true;

        for (var type2 : arguments)
        {
            if (type2 != params.get(i++))
            {
                equalsOrigin = false;
                break;
            }
        }

        if (equalsOrigin)
        {
            return origin;
        }

        int l = arguments.size();
        var list = _typeInstantiations.get(origin);
        if (list == null)
        {
            _typeInstantiations.put(origin, list = new Vector<>());
        }

        search:
        for (var type2 : list)
        {
            var arguments2 = type2.arguments();
            for (i = 0; i != l; ++i)
            {
                if (arguments2.get(i) != arguments.get(i))
                {
                    continue search;
                }
            }
            return type2;
        }

        type = new Types.InstantiatedType(origin, arguments);
        type._pool = this;
        list.add(type);
        return type;
    }

    public Symbol createFunctionType(Vector<Symbol> params, Vector<Symbol> optParams, boolean rest, Symbol result)
    {
        Symbol type = null;

        int i = 0;
        int l1 = params != null ? params.size() : 0;
        int l2 = optParams != null ? optParams.size() : 0;

        if (l1 >= _functionTypes.size())
        {
            for (i = _functionTypes.size(); i <= l1; ++i)
            {
                _functionTypes.add(new Vector<>());
            }
        }

        var list = _functionTypes.get(l1);

        search:
        for (var type2 : list)
        {
            var params2 = type2.params();
            if (params2 != null)
            {
                if (params == null)
                {
                    continue search;
                }
                for (i = 0; i != l1; ++i)
                {
                    if (params.get(i) != params2.get(i))
                    {
                        continue search;
                    }
                }
            }
            else if (params != null)
            {
                continue search;
            }

            var optParams2 = type2.optParams();
            if (optParams2 != null)
            {
                if (optParams == null || optParams.size() != optParams2.size())
                {
                    continue search;
                }
                for (i = 0; i != l2; ++i)
                {
                    if (optParams.get(i) != optParams2.get(i))
                    {
                        continue search;
                    }
                }
            }
            else if (optParams != null)
            {
                continue search;
            }

            var rest2 = type2.rest();
            if (rest2)
            {
                if (!rest)
                    continue search;
            }
            else if (rest)
                continue search;

            if (type2.result() == result)
            {
                return type2;
            }
        }

        type = new Types.FunctionType(params, optParams, rest, result);
        type._pool = this;
        type.setDelegate(objectType != null ? objectType.delegate() : null);
        list.add(type);
        return type;
    }

    public Symbol createFunctionType(Symbol[] params, Vector<Symbol> optParams, boolean rest, Symbol result)
    {
        Vector<Symbol> paramVector = null;
        if (params != null)
        {
            paramVector = VectorUtils.fromArray(params);
        }
        return createFunctionType(paramVector, optParams, rest, result);
    }

    public Symbol createTupleType(Vector<Symbol> elements)
    {
        Symbol type = null;
        int i = 0;
        int l = elements.size();

        if (l > _tupleTypes.size())
        {
            for (i = _tupleTypes.size(); i < l; ++i)
            {
                _tupleTypes.add(new Vector<>());
            }
        }

        var list = _tupleTypes.get(l - 1);

        search:
        for (var type2 : list)
        {
            var elements2 = type2.tupleElements();
            for (i = 0; i != l; ++i)
            {
                if (elements.get(i) != elements2.get(i))
                {
                    continue search;
                }
            }
            return type2;
        }

        type = new Types.TupleType(elements);
        type._pool = this;
        type.setDelegate(objectType != null ? objectType.delegate() : null);
        list.add(type);
        return type;
    }

    public Symbol createNullableType(Symbol overType)
    {
        if (overType.containsNull())
        {
            return overType;
        }
        Symbol type = _nullableTypes.get(overType);
        if (type != null)
        {
            return type;
        }
        type = new Types.NullableType(overType);
        type._pool = this;
        _nullableTypes.put(overType, type);
        return type;
    }

    public Symbol createTypeParameter(Symbol name, Symbol definitionType)
    {
        Symbol type = new Types.TypeParameterSymbol(name, definitionType, anyType);
        type._pool = this;
        return type;
    }

    public Symbol createVerifyingType()
    {
        if (verifyingType != null)
        {
            return verifyingType;
        }
        Symbol type = new Types.VerifyingType();
        type._pool = this;
        verifyingType = type;
        return type;
    }

    public Symbol createValue(Symbol type)
    {
        Symbol value = new Values.Value(type);
        value._pool = this;
        return value;
    }

    public Symbol createThis(Symbol type)
    {
        Symbol value = new Values.ThisSymbol(type);
        value._pool = this;
        return value;
    }

    public Symbol createPackageProperty(Symbol pckg, Symbol property)
    {
        if (property.isFunction() || property.isVariableProperty() || property.isVirtualProperty())
        {
            Symbol value = new Values.PackageProperty(pckg, property, property.signature() == null ? property.staticType() : functionType);
            value._pool = this;
            return value;
        }
        return property;
    }

    public Symbol createTypeProperty(Symbol type, Symbol property)
    {
        if (property.isFunction() || property.isVariableProperty() || property.isVirtualProperty())
        {
            Symbol value = new Values.TypeProperty(type, property, property.signature() == null ? property.staticType() : functionType);
            value._pool = this;
            return value;
        }
        return property;
    }

    public Symbol createBooleanLogicalAndOr()
    {
        Symbol value = new Values.BooleanLogicalAndOrSymbol(booleanType);
        value._pool = this;
        return value;
    }

    public Symbol createFrameProperty(Symbol frame, Symbol property)
    {
        if (property.isFunction() || property.isVariableProperty() || property.isVirtualProperty())
        {
            Symbol value = new Values.FrameProperty(frame, property, property.signature() == null ? property.staticType() : functionType);
            value._pool = this;
            return value;
        }
        return property;
    }

    public Symbol createObjectProperty(Symbol obj, Symbol property)
    {
        if (property.isFunction() || property.isVariableProperty() || property.isVirtualProperty())
        {
            Symbol value = new Values.ObjectProperty(obj, property, property.signature() == null ? property.staticType() : functionType);
            value._pool = this;
            return value;
        }
        if (property.isNamespace())
        {
            return createNamespaceConstantValue(property);
        }
        return property;
    }

    public Symbol createObjectProxyProperty(Symbol obj, ProxyPropertyTrait trait)
    {
        Symbol value = new Values.ObjectProxyProperty(obj, trait);
        value._pool = this;
        return value;
    }

    public Symbol createObjectDynamicProperty(Symbol obj)
    {
        Symbol value = new Values.ObjectDynamicProperty(obj, anyType);
        value._pool = this;
        return value;
    }

    public Symbol createObjectAttribute(Symbol obj, ProxyPropertyTrait trait)
    {
        Symbol value = new Values.ObjectAttribute(obj, trait);
        value._pool = this;
        return value;
    }

    public Symbol createObjectFilter(Symbol obj, Symbol proxy)
    {
        Symbol value = new Values.ObjectFilter(obj, proxy);
        value._pool = this;
        return value;
    }

    public Symbol createObjectDescendants(Symbol obj, Symbol proxy)
    {
        Symbol value = new Values.ObjectDescendants(obj, proxy);
        value._pool = this;
        return value;
    }

    public Symbol createTupleElement(Symbol obj, int index)
    {
        Symbol value = new Values.TupleElement(obj, index);
        value._pool = this;
        return value;
    }

    public Symbol createConversionResult(Symbol origin, ConversionKind kind, Symbol toType)
    {
        Symbol value = new Values.ConversionResult(origin, kind, toType);
        value._pool = this;
        return value;
    }

    public Symbol createAsConversion(Symbol conversion)
    {
        Symbol value = new Values.AsConversion(conversion, createNullableType(conversion.staticType()));
        value._pool = this;
        return value;
    }

    public Symbol createCallConversion(Symbol conversion)
    {
        Symbol value = new Values.CallConversion(conversion);
        value._pool = this;
        return value;
    }

    public Symbol createBooleanConstantValue(boolean value, Symbol type)
    {
        Symbol value2 = new Values.BooleanConstant(value, type);
        value2._pool = this;
        return value2;
    }

    public Symbol createBooleanConstantValue(boolean value)
    {
        return createBooleanConstantValue(value, booleanType);
    }

    public Symbol createEnumConstantValue(Object value, Symbol type)
    {
        Symbol value2 = new Values.EnumConstant(value, type);
        value2._pool = this;
        return value2;
    }

    public Symbol createIntConstantValue(int value, Symbol type)
    {
        Symbol value2 = new Values.IntConstant(value, type);
        value2._pool = this;
        return value2;
    }

    public Symbol createIntConstantValue(int value)
    {
        return createIntConstantValue(value, intType);
    }

    public Symbol createUnsignedIntConstantValue(UnsignedInteger value, Symbol type)
    {
        Symbol value2 = new Values.UnsignedIntConstant(value, type);
        value2._pool = this;
        return value2;
    }

    public Symbol createUnsignedIntConstantValue(UnsignedInteger value)
    {
        return createUnsignedIntConstantValue(value, uintType);
    }

    public Symbol createBigIntConstantValue(java.math.BigInteger value, Symbol type)
    {
        Symbol value2 = new Values.BigIntConstant(value, type);
        value2._pool = this;
        return value2;
    }

    public Symbol createBigIntConstantValue(java.math.BigInteger value)
    {
        return createBigIntConstantValue(value, bigIntType);
    }

    public Symbol createNamespaceConstantValue(Symbol ns)
    {
        Symbol value = new Values.NamespaceConstant(ns, namespaceType);
        value._pool = this;
        return value;
    }

    public Symbol createNullConstantValue(Symbol type)
    {
        Symbol value = new Values.NullConstant(type);
        value._pool = this;
        return value;
    }

    public Symbol createNullConstantValue()
    {
        return createNullConstantValue(nullType);
    }

    public Symbol createNumberConstantValue(double value, Symbol type)
    {
        Symbol value2 = new Values.NumberConstant(value, type);
        value2._pool = this;
        return value2;
    }

    public Symbol createNumberConstantValue(double value)
    {
        return createNumberConstantValue(value, numberType);
    }

    public Symbol createStringConstantValue(String str, Symbol type)
    {
        Symbol value = new Values.StringConstant(str, type);
        value._pool = this;
        return value;
    }

    public Symbol createStringConstantValue(String str)
    {
        return createStringConstantValue(str, stringType);
    }

    public Symbol createUndefinedConstantValue(Symbol type)
    {
        Symbol value = new Values.UndefinedConstant(type);
        value._pool = this;
        return value;
    }

    public Symbol createUndefinedConstantValue()
    {
        return createUndefinedConstantValue(voidType);
    }

    public Symbol createVariableProperty(Symbol name, boolean readOnly, Symbol type)
    {
        Symbol property = new VariableProperty(name, readOnly, type);
        property._pool = this;
        return property;
    }

    public Symbol createInstantiatedVariableProperty(Symbol origin, Symbol declaratorType)
    {
        var originList = _instantiatedSymbols.get(origin);
        if (originList == null)
        {
            _instantiatedSymbols.put(origin, originList = new HashMap<>());
        }
        var r = originList.get(declaratorType);
        if (r == null)
        {
            r = new InstantiatedVariableProperty(origin, origin.staticType().replaceType(declaratorType));
            r._pool = this;
            originList.put(declaratorType, r);
        }
        return r;
    }

    public Symbol createVirtualProperty(Symbol name, Symbol type)
    {
        Symbol property = new VirtualProperty(name, type);
        property._pool = this;
        return property;
    }

    public Symbol createInstantiatedVirtualProperty(Symbol origin, Symbol declaratorType)
    {
        var originList = _instantiatedSymbols.get(origin);
        if (originList == null)
        {
            _instantiatedSymbols.put(origin, originList = new HashMap<>());
        }
        var r = originList.get(declaratorType);
        if (r == null)
        {
            r = new InstantiatedVirtualProperty(origin, declaratorType);
            r._pool = this;
            originList.put(declaratorType, r);
            if (origin.getter() != null)
            {
                r.setGetter(origin.pool().createInstantiatedFunction(origin.getter(), declaratorType));
            }
            if (origin.setter() != null)
            {
                r.setSetter(origin.pool().createInstantiatedFunction(origin.setter(), declaratorType));
            }
        }
        return r;
    }

    public Symbol findPackage(String id)
    {
        return _packages.get(id);
    }

    public boolean isNumericType(Symbol type)
    {
        return numericTypes.contains(type);
    }

    public boolean isIntegerType(Symbol type)
    {
        return integerTypes.contains(type);
    }

    public boolean isNameType(Symbol type)
    {
        type = type.escapeType();
        return type == anyType || type == qnameType || type == stringType;
    }

    public boolean isArrayType(Symbol type)
    {
        type = type.escapeType();
        return type.equalsOrInstantiatedFrom(vectorType) || type.isTupleType();
    }

    public Symbol validateHasPropertyProxy(Symbol symbol)
    {
        if (symbol == null || !symbol.isFunction())
        {
            return null;
        }
        var s = symbol.signature();
        if (s.params() == null || s.params().size() != 1 || s.optParams() != null || s.rest())
        {
            return null;
        }
        return s.result() == booleanType ? symbol : null;
    }

    public Symbol validateGetDescendantsProxy(Symbol symbol)
    {
        if (symbol == null || !symbol.isFunction())
        {
            return null;
        }
        var s = symbol.signature();
        if (s.params() == null || s.params().size() != 1 || s.optParams() != null || s.rest())
        {
            return null;
        }
        return isNameType(s.params().get(0)) ? symbol : null;
    }

    public Symbol validateNextNameIndexProxy(Symbol symbol)
    {
        if (symbol == null || !symbol.isFunction())
        {
            return null;
        }
        var s = symbol.signature();
        var pool = s.pool();
        if (s.params() == null || s.params().size() != 1 || !pool.isNumericType(s.params().get(0)) || s.optParams() != null || s.rest() || !pool.isNumericType(s.result()))
        {
            return null;
        }
        return s.params().get(0) == s.result() ? symbol : null;
    }

    public Symbol validateNextNameOrValueProxy(Symbol symbol)
    {
        if (symbol == null || !symbol.isFunction())
        {
            return null;
        }
        var s = symbol.signature();
        var pool = s.pool();
        if (s.params() == null || s.params().size() != 1 || !pool.isNumericType(s.params().get(0)) || s.optParams() != null || s.rest())
        {
            return null;
        }
        return symbol;
    }

    private Symbol defineStlGlobalClass(String name, boolean asFinal, boolean asPrimitive, boolean asUnion)
    {
        var p = sxGlobalPackage;
        var qname = createName(p.publicNamespace(), name);
        var symbol = createClassType(qname, asFinal, asPrimitive, asUnion);
        symbol.setDefinitionPackage(p);
        p.ownNames().defineName(qname, symbol);
        return symbol;
    }

    private Symbol defineStlGlobalClass(String name, boolean asFinal, boolean asPrimitive)
    {
        return defineStlGlobalClass(name, asFinal, asPrimitive, false);
    }

    private Symbol defineStlGlobalClass(String name, boolean asFinal)
    {
        return defineStlGlobalClass(name, asFinal, false);
    }

    private Symbol defineStlGlobalClass(String name)
    {
        return defineStlGlobalClass(name, true);
    }

    private void initNumericType(Symbol type)
    {
        var unary = createFunctionType((Vector<Symbol>) null, null, false, type);
        var binary = createFunctionType(new Symbol[] {type}, null, false, type);
        var binaryBoolean = createFunctionType(new Symbol[] {type}, null, false, booleanType);
        type.delegate().initOwnOperators();
        type.delegate().ownOperators().put(Operator.NEGATE, createFunction(null, unary));
        type.delegate().ownOperators().put(Operator.BITWISE_NOT, createFunction(null, unary));
        type.delegate().ownOperators().put(Operator.ADD, createFunction(null, binary));
        type.delegate().ownOperators().put(Operator.SUBTRACT, createFunction(null, binary));
        type.delegate().ownOperators().put(Operator.MULTIPLY, createFunction(null, binary));
        type.delegate().ownOperators().put(Operator.DIVIDE, createFunction(null, binary));
        type.delegate().ownOperators().put(Operator.REMAINDER, createFunction(null, binary));
        type.delegate().ownOperators().put(Operator.BITWISE_AND, createFunction(null, binary));
        type.delegate().ownOperators().put(Operator.BITWISE_XOR, createFunction(null, binary));
        type.delegate().ownOperators().put(Operator.BITWISE_OR, createFunction(null, binary));
        type.delegate().ownOperators().put(Operator.LEFT_SHIFT, createFunction(null, binary));
        type.delegate().ownOperators().put(Operator.RIGHT_SHIFT, createFunction(null, binary));
        type.delegate().ownOperators().put(Operator.UNSIGNED_RIGHT_SHIFT, createFunction(null, binary));
        type.delegate().ownOperators().put(Operator.LT, createFunction(null, binaryBoolean));
        type.delegate().ownOperators().put(Operator.GT, createFunction(null, binaryBoolean));
        type.delegate().ownOperators().put(Operator.LE, createFunction(null, binaryBoolean));
        type.delegate().ownOperators().put(Operator.GE, createFunction(null, binaryBoolean));
    }
}