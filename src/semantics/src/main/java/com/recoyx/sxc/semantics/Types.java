package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.google.common.primitives.*;
import com.recoyx.sxc.semantics.errors.*;
import com.recoyx.sxc.util.VectorUtils;

public final class Types
{
    public static class Type extends Symbol
    {
        protected Delegate _delegate;

        @Override
        public Delegate delegate()
        {
            return _delegate;
        }

        @Override
        public void setDelegate(Delegate delegate)
        {
            _delegate = delegate;
        }

        @Override
        public Symbol superClass()
        {
            return delegate() != null && delegate().inherit() != null ? delegate().inherit().type() : null;
        }

        /**
         * Indicates whether object literal is publicly applicable for the type or not.
         */
        @Override
        public boolean isInitialisable()
        {
            return false;
        }

        /**
         * Indicates whether new operator is publicly applicable to the type or not.
         */
        @Override
        public boolean isConstructable()
        {
            return false;
        }

        @Override
        public boolean isSubtypeOf(Symbol type)
        {
            if (this == type)
                return true;
            var superClass = this.superClass();
            if (superClass != null && (superClass == type || superClass.isSubtypeOf(type)))
                return true;
            var itrfcs = this.implementedInterfaces();
            if (itrfcs != null && type.isInterfaceType())
            {
                for (var itrfc : itrfcs)
                    if (itrfc == type || itrfc.isSubtypeOf(type))
                        return true;
            }
            var superItrfcs = this.superInterfaces();
            if (superItrfcs != null)
            {
                for (var itrfc : superItrfcs)
                    if (itrfc == type || itrfc.isSubtypeOf(type))
                        return true;
            }
            return false;
        }

        @Override
        public Symbol lookupName(Symbol name)
            throws AmbiguousReferenceError
        {
            Symbol s = null;
            if (this.isInstantiatedType())
            {
                s = origin().lookupName(name);
                if (s != null)
                {
                    s = s.replaceType(this);
                }
                return s;
            }
            else
            {
                s = ownNames() != null ? ownNames().lookupName(name) : null;
                if (s != null)
                {
                    return pool().createTypeProperty(this, s);
                }
            }
            return null;
        }

        @Override
        public Symbol lookupMultiName(Vector<Symbol> namespaces, String localName)
            throws AmbiguousReferenceError
        {
            Symbol s = null;
            if (this.isInstantiatedType())
            {
                s = origin().lookupMultiName(namespaces, localName);
                if (s != null)
                {
                    s = s.replaceType(this);
                }
                return s;
            }
            else
            {
                s = ownNames() != null ? ownNames().lookupMultiName(namespaces, localName) : null;
                if (s != null)
                {
                    return pool().createTypeProperty(this, s);
                }
            }
            return null;
        }

        @Override
        public boolean equalsOrInstantiatedFrom(Symbol type)
        {
            if (this == type)
            {
                return true;
            }
            var o = this.origin();
            return o != null ? o == type : false;
        }

        @Override
        public boolean containsTypeParams()
        {
            Vector<Symbol> list = null;

            if (this.isClassType() || this.isInterfaceType())
            {
                if (this.isInstantiatedType())
                {
                    for (var arg : this.arguments())
                    {
                        if (arg.containsTypeParams())
                        {
                            return true;
                        }
                    }
                }
                else if ((list = this.typeParams()) != null)
                {
                    return true;
                }
            }
            else if (this.isNullableType())
            {
                if (this.overType().containsTypeParams())
                {
                    return true;
                }
            }
            else if (this.isFunctionType())
            {
                var fParams = this.params();
                if (fParams != null)
                {
                    for (var p : fParams)
                    {
                        if (p.containsTypeParams())
                        {
                            return true;
                        }
                    }
                }
                var fOptParams = this.optParams();
                if (fOptParams != null)
                {
                    for (var p : fOptParams)
                    {
                        if (p.containsTypeParams())
                        {
                            return true;
                        }
                    }
                }
                return this.result().containsTypeParams();
            }
            else if (this.isTupleType())
            {
                for (var e : this.tupleElements())
                {
                    if (e.containsTypeParams())
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public Symbol escapeType()
        {
            return this.isNullableType() ? this.overType() : this;
        }

        @Override
        public Symbol replaceType(Symbol argumentType)
        {
            return _replaceType(argumentType.origin().typeParams(), argumentType.arguments());
        }

        private final Symbol _replaceType(Vector<Symbol> params, Vector<Symbol> arguments)
        {
            int i = -1;
            Vector<Symbol> newArguments = null;
            if (this.isTypeParameter())
            {
                i = params.indexOf(this);
                return i == -1 ? this : arguments.get(i);
            }
            else if (this.isInstantiatedType())
            {
                newArguments = new Vector<>();
                for (var argument : this.arguments())
                {
                    newArguments.add(((Type)argument)._replaceType(params, arguments));
                }
                return pool().createInstantiatedType(this.origin(), newArguments);
            }
            else if (this.isClassType() || this.isInterfaceType())
            {
                if (this.typeParams() == null)
                {
                    return this;
                }
                newArguments = new Vector<>();
                for (var p : this.typeParams())
                {
                    i = params.indexOf(p);
                    newArguments.add(i == -1 ? p : arguments.get(i));
                }
                return pool().createInstantiatedType(this, newArguments);
            }
            else if (this.isFunctionType())
            {
                Vector<Symbol> fParams = null;
                Vector<Symbol> fOptParams = null;
                Symbol fResult = null;

                if (params() != null)
                {
                    fParams = new Vector<>();
                    for (var p : params())
                    {
                        fParams.add(((Type)p)._replaceType(params, arguments));
                    }
                }

                if (optParams() != null)
                {
                    fOptParams = new Vector<>();
                    for (var p : optParams())
                    {
                        fOptParams.add(((Type)p)._replaceType(params, arguments));
                    }
                }

                fResult = ((Type)result())._replaceType(params, arguments);
                return pool().createFunctionType(fParams, fOptParams, this.rest(), fResult);
            }
            else if (this.isNullableType())
            {
                return pool().createNullableType(((Type)overType())._replaceType(params, arguments));
            }
            else if (this.isTupleType())
            {
                Vector<Symbol> newElements = new Vector<>();
                for (var p : this.tupleElements())
                {
                    newElements.add(((Type)p)._replaceType(params, arguments));
                }
                return pool().createTupleType(newArguments);
            }
            return this;
        }
    }

    public final static class AnyType extends Type
    {
        public AnyType()
        {
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.ANY_TYPE;
        }

        @Override
        public boolean containsUndefined()
        {
            return true;
        }

        @Override
        public boolean containsNull()
        {
            return true;
        }

        @Override
        public Symbol defaultValue()
        {
            return pool().createUndefinedConstantValue(this);
        }

        @Override
        public String toString()
        {
            return "*";
        }
    }

    public final static class VoidType extends Type
    {
        public VoidType()
        {
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.VOID_TYPE;
        }

        @Override
        public String toString()
        {
            return "void";
        }
    }

    public final static class NullType extends Type
    {
        public NullType()
        {
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.NULL_TYPE;
        }

        @Override
        public boolean containsNull()
        {
            return true;
        }

        @Override
        public String toString()
        {
            return "null";
        }
    }

    public final static class ClassType extends Type
    {
        private Symbol _name;
        private int _flags;
        private Symbol _definitionPackage;
        private final Names _ownNames = new Names();
        private Vector<Symbol> _typeParams;
        private Symbol _constructorFunction;
        private Symbol _privateNamespace;
        private Symbol _protectedNamespace;
        private Vector<Symbol> _interfaces;
        private Vector<Symbol> _subclasses;

        public ClassType(Symbol name, boolean markFinal, boolean classCfgPrimitive, boolean classCfgUnion, boolean markDynamic)
        {
            _name = name;
            _flags = (markFinal ? 1 : 0) | (classCfgPrimitive ? 1 | 2 : 0) | (classCfgUnion ? 1 | 2 | 4 : 0) | (markDynamic ? 32 : 0);
            _delegate = new Delegate(this);
            _delegate.setOwnNames(new Names());
            setIsInitialisable(false);
            setIsConstructable(true);
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.CLASS_TYPE;
        }

        @Override
        public Symbol name()
        {
            return _name;
        }

        @Override
        public Vector<Symbol> typeParams()
        {
            return _typeParams;
        }

        @Override
        public void setTypeParams(Vector<Symbol> vector)
        {
            _typeParams = vector;
        }

        /**
         * Equivalent to type parameter collection.
         */
        @Override
        public Vector<Symbol> arguments()
        {
            return _typeParams;
        }

        @Override
        public Vector<Symbol> subclasses()
        {
            return _subclasses;
        }

        @Override
        public void initSubclasses()
        {
            _subclasses = _subclasses == null ? new Vector<>() : _subclasses;
        }

        @Override
        public Vector<Symbol> implementedInterfaces()
        {
            return _interfaces;
        }

        @Override
        public void initImplementedInterfaces()
        {
            _interfaces = _interfaces == null ? new Vector<>() : _interfaces;
        }

        @Override
        public Symbol protectedNamespace()
        {
            return _protectedNamespace;
        }

        @Override
        public void setProtectedNamespace(Symbol ns)
        {
            _protectedNamespace = ns;
        }

        @Override
        public Symbol privateNamespace()
        {
            return _privateNamespace;
        }

        @Override
        public void setPrivateNamespace(Symbol ns)
        {
            _privateNamespace = ns;
        }

        @Override
        public Symbol definitionPackage()
        {
            return _definitionPackage;
        }

        @Override
        public void setDefinitionPackage(Symbol pckg)
        {
            _definitionPackage = pckg;
        }

        @Override
        public Names ownNames()
        {
            return _ownNames;
        }

        @Override
        public boolean isInitialisable()
        {
            return (this._flags & 8) != 0;
        }

        @Override
        public void setIsInitialisable(boolean value)
        {
            this._flags = value ? this._flags | 8 : (this._flags & 8) != 0 ? this._flags ^ 8 : this._flags;
        }

        @Override
        public boolean isConstructable()
        {
            return (this._flags & 16) != 0;
        }

        @Override
        public void setIsConstructable(boolean value)
        {
            this._flags = value ? this._flags | 16 : (this._flags & 16) != 0 ? this._flags ^ 16 : this._flags;
        }

        @Override
        public boolean markFinal()
        {
            return (this._flags & 1) != 0;
        }

        @Override
        public boolean markDynamic()
        {
            return (this._flags & 32) != 0;
        }

        @Override
        public boolean classCfgPrimitive()
        {
            return (this._flags & 2) != 0;
        }

        @Override
        public boolean classCfgUnion()
        {
            return (this._flags & 4) != 0;
        }

        @Override
        public boolean isClassType()
        {
            return true;
        }

        @Override
        public String packageID()
        {
            return definitionPackage() == null ? "" : definitionPackage().packageID();
        }

        @Override
        public Symbol constructorFunction()
        {
            return _constructorFunction;
        }

        @Override
        public void setConstructorFunction(Symbol symbol)
        {
            _constructorFunction = symbol;
        }

        @Override
        public Symbol defaultValue()
        {
            var pool = this.pool();
            if (this == pool.booleanType)
            {
                return pool().createBooleanConstantValue(false);
            }
            if (this == pool.numberType)
            {
                return pool().createNumberConstantValue(Double.valueOf(0));
            }

            if (this == pool.intType)
            {
                return pool().createIntConstantValue(0);
            }

            if (this == pool.bigIntType)
            {
                return pool().createBigIntConstantValue(java.math.BigInteger.ZERO);
            }

            if (this == pool.uintType
            ||  this == pool.charType)
            {
                return pool().createUnsignedIntConstantValue(UnsignedInteger.valueOf(0));
            }

            if (this == pool.stringType)
            {
                return pool().createStringConstantValue("");
            }

            return null;
        }

        @Override
        public void extend(Symbol type)
            throws InheritingNameConflictError
        {
            if (type.isSubtypeOf(this))
                return;

            Symbol superClass = this.superClass();

            if (superClass != null)
            {
                var list = superClass.subclasses();
                int i = list == null ? -1 : list.indexOf(this);
                if (i != -1)
                {
                    list.remove(i);
                }
                delegate().setInherit(null);
            }

            delegate().setInherit(type.delegate());
            type.initSubclasses();
            type.subclasses().add(this);
        }

        @Override
        public void implement(Symbol type)
        {
            this.initImplementedInterfaces();
            if (!this._interfaces.contains(type))
            {
                this._interfaces.add(type);
            }
        }

        @Override
        public void verifyInterfaceImplementations(InterfaceImplementationEvents errorHandler)
        {
            if (_interfaces == null)
            {
                return;
            }

            for (var itrfc : _interfaces)
            {
                for (var pair : itrfc.delegate().names())
                {
                    var name = pair.key;
                    var symbol = pair.value;
                    var implSymbol = name.namespace().isExplicitNamespace()
                        ? delegate().lookupName(name)
                        : delegate().lookupReservedNamespaceName(SymbolKind.PUBLIC_NAMESPACE, name.localName());
                    implSymbol = implSymbol == null ? delegate().lookupReservedNamespaceName(SymbolKind.INTERNAL_NAMESPACE, name.localName()) : implSymbol;

                    if (symbol.isVirtualProperty())
                    {
                        _verifyVirtualPropertyImpl(name, symbol, implSymbol, errorHandler);
                    }
                    else
                    {
                        _verifyMethodImpl(name, symbol, implSymbol, errorHandler);
                    }
                }
            }
        }

        private void _verifyVirtualPropertyImpl(Symbol name, Symbol property, Symbol implProperty, InterfaceImplementationEvents errorHandler)
        {
            if (implProperty == null)
            {
                if (property.getter() != null && property.getter().markNative())
                {
                    errorHandler.onundefined("getter", name, property.getter().signature());
                }
                if (property.setter() != null && property.setter().markNative())
                {
                    errorHandler.onundefined("setter", name, property.setter().signature());
                }
            }
            else
            {
                if (implProperty.isVirtualProperty())
                {
                    if (property.getter() != null && property.getter().markNative() && implProperty.getter() == null)
                    {
                        errorHandler.onundefined("getter", name, property.getter().signature());
                    }
                    else if (property.getter() != null && property.getter().signature() != implProperty.getter().signature())
                    {
                        errorHandler.onundefined("getter", name, property.getter().signature());
                    }
                    if (property.setter() != null && property.setter().markNative() && implProperty.setter() == null)
                    {
                        errorHandler.onundefined("setter", name, property.setter().signature());
                    }
                    else if (property.setter() != null && property.setter().signature() != implProperty.setter().signature())
                    {
                        errorHandler.onundefined("setter", name, property.setter().signature());
                    }
                }
                else
                {
                    errorHandler.onwrong("virtualProperty", name);
                }
            }
        }

        private void _verifyMethodImpl(Symbol name, Symbol method, Symbol implMethod, InterfaceImplementationEvents errorHandler)
        {
            if (implMethod == null)
            {
                if (method.markNative())
                {
                    errorHandler.onundefined("method", name, method.signature());
                }
            }
            else
            {
                if (!implMethod.isFunction())
                {
                    errorHandler.onwrong("method", name);
                }
                else if (implMethod.signature() != method.signature())
                {
                    errorHandler.onundefined("method", name, method.signature());
                }
            }
        }

        @Override
        public String toString()
        {
            String p = "";
            if (typeParams() != null)
            {
                var builder = new StringBuilder();
                for (var p2 : typeParams())
                {
                    builder.append((builder.length() == 0 ? "" : ", ") + p2.toString());
                }
                p = ".<" + builder.toString() + ">";
            }
            String pckg = packageID();
            return (pckg.equals("") ? "" : pckg + ".") + _name.toString() + p;
        }
    }

    public final static class InterfaceType extends Type
    {
        private Symbol _name;
        private Vector<Symbol> _typeParams;
        private Symbol _definitionPackage;
        private Vector<Symbol> _implementors = new Vector<>();
        private Vector<Symbol> _subInterfaces;
        private Vector<Symbol> _superInterfaces;

        public InterfaceType(Symbol name)
        {
            _name = name;
            _delegate = new Delegate(this);
            _delegate.setOwnNames(new Names());
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.INTERFACE_TYPE;
        }

        @Override
        public boolean isInterfaceType()
        {
            return true;
        }

        @Override
        public Symbol name()
        {
            return _name;
        }

        @Override
        public Symbol definitionPackage()
        {
            return _definitionPackage;
        }

        @Override
        public void setDefinitionPackage(Symbol pckg)
        {
            _definitionPackage = pckg;
        }

        @Override
        public String packageID()
        {
            return definitionPackage() == null ? "" : definitionPackage().packageID();
        }

        @Override
        public Vector<Symbol> implementors()
        {
            return _implementors;
        }

        @Override
        public Vector<Symbol> superInterfaces()
        {
            return _superInterfaces;
        }

        @Override
        public void initSuperInterfaces()
        {
            _superInterfaces = _superInterfaces == null ? new Vector<>() : _superInterfaces;
        }

        @Override
        public Vector<Symbol> subInterfaces()
        {
            return _subInterfaces;
        }

        @Override
        public void initSubInterfaces()
        {
            _subInterfaces = _subInterfaces == null ? new Vector<>() : _subInterfaces;
        }

        @Override
        public Vector<Symbol> typeParams()
        {
            return _typeParams;
        }

        @Override
        public void setTypeParams(Vector<Symbol> vector)
        {
            _typeParams = vector;
        }

        /**
         * Equivalent to type parameter collection.
         */
        @Override
        public Vector<Symbol> arguments()
        {
            return _typeParams;
        }

        @Override
        public void extend(Symbol type)
            throws InheritingNameConflictError
        {
            if (type.isSubtypeOf(this) || this.isSubtypeOf(type))
            {
                return;
            }
            InheritingNameConflictError error = null;

            for (var pair : type.delegate().ownNames())
            {
                if (delegate().ownNames().hasName(pair.key))
                {
                    error = error == null ? new InheritingNameConflictError(pair.key, type) : error;
                }
                else
                {
                    delegate().ownNames().defineName(pair.key, pair.value);
                }
            }

            if (error != null)
            {
                throw error;
            }
        }

        @Override
        public String toString()
        {
            String p = "";
            if (typeParams() != null)
            {
                var builder = new StringBuilder();
                for (var p2 : typeParams())
                {
                    builder.append((builder.length() == 0 ? "" : ", ") + p2.toString());
                }
                p = ".<" + builder.toString() + ">";
            }
            String pckg = packageID();
            return (pckg.equals("") ? "" : pckg + ".") + _name.toString() + p;
        }
    }

    public final static class EnumType extends Type
    {
        private Symbol _name;
        private Symbol _numericType;
        private boolean _isFlagEnum;
        private Symbol _definitionPackage;
        private Symbol _privateNamespace;
        private HashMap<String, Symbol> _constants = new HashMap<>();
        private Names _ownNames = new Names();

        public EnumType(Symbol name, Symbol numericType, boolean isFlagEnum)
        {
            _name = name;
            _numericType = numericType;
            _isFlagEnum = isFlagEnum;
            _delegate = new Delegate(this);
            _delegate.setOwnNames(new Names());
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.ENUM_TYPE;
        }

        @Override
        public Symbol name()
        {
            return _name;
        }

        @Override
        public Symbol privateNamespace()
        {
            return _privateNamespace;
        }

        @Override
        public void setPrivateNamespace(Symbol ns)
        {
            _privateNamespace = ns;
        }

        @Override
        public Symbol definitionPackage()
        {
            return _definitionPackage;
        }

        @Override
        public void setDefinitionPackage(Symbol pckg)
        {
            _definitionPackage = pckg;
        }

        @Override
        public String packageID()
        {
            return definitionPackage() == null ? "" : definitionPackage().packageID();
        }

        @Override
        public boolean isFlagEnum()
        {
            return _isFlagEnum;
        }

        @Override
        public Names ownNames()
        {
            return _ownNames;
        }

        @Override
        public Symbol numericType()
        {
            return _numericType;
        }

        @Override
        public void setNumericType(Symbol type)
        {
            _numericType = type;
        }

        @Override
        public Symbol defaultValue()
        {
            return pool().createEnumConstantValue(0, this);
        }

        @Override
        public void initEnumOperators(Symbol publicNs)
        {
            if (!isFlagEnum())
            {
                return;
            }
            var pool = this.pool();

			// proxy::hasProperty()
            var hasProxy = pool.createFunction(pool.proxyHasPropertyName, pool.createFunctionType(new Symbol[] {this}, null, false, pool.booleanType));
            hasProxy.setMarkNative(true);
            delegate().ownNames().defineName(pool.proxyHasPropertyName, hasProxy);

			// filter()
            var filterFn = pool.createFunction(pool.createName(publicNs, "filter"), pool.createFunctionType((Vector<Symbol>) null, null, true, this));
            filterFn.setMarkNative(true);
            delegate().ownNames().defineName(filterFn.name(), filterFn);

            var addOperatorSignature = pool.createFunctionType(new Symbol[] {this}, null, false, this);
            var addOperators = new Vector<Symbol>();
            for (int i = 0; i != 2; ++i)
            {
                var fn = pool.createFunction(null, addOperatorSignature);
                fn.setMarkNative(true);
                addOperators.add(fn);
            }
            delegate().initOwnOperators();
            delegate().ownOperators().put(Operator.ADD, addOperators.get(0));
            delegate().ownOperators().put(Operator.SUBTRACT, addOperators.get(1));
        }

        @Override
        public Symbol getConstant(String id)
        {
            return _constants.get(id);
        }

        @Override
        public void setConstant(String id, Symbol value)
        {
            _constants.put(id, value);
        }

        @Override
        public String toString()
        {
            return (this.packageID() != "" ? this.packageID() + "." : "") + _name.toString();
        }
    }

    public final static class InstantiatedType extends Type
    {
        private Symbol _origin;
        private Vector<Symbol> _arguments;
        private Vector<Symbol> _subclasses;
        private Vector<Symbol> _subInterfaces;

        public InstantiatedType(Symbol origin, Vector<Symbol> arguments)
        {
            _origin = origin;
            _arguments = arguments;
            _delegate = new Delegate(this);
            _delegate.setInherit(origin.delegate());
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.INSTANTIATED_TYPE;
        }

        @Override
        public Symbol origin()
        {
            return _origin;
        }

        @Override
        public Vector<Symbol> arguments()
        {
            return _arguments;
        }

        @Override
        public boolean isClassType()
        {
            return _origin.isClassType();
        }

        @Override
        public boolean isInterfaceType()
        {
            return _origin.isInterfaceType();
        }

        @Override
        public Symbol name()
        {
            return _origin.name();
        }

        @Override
        public boolean containsNull()
        {
            return false;
        }

        @Override
        public boolean isInitialisable()
        {
            return _origin.isInitialisable();
        }

        @Override
        public boolean isConstructable()
        {
            return _origin.isConstructable();
        }

        @Override
        public boolean classCfgPrimitive()
        {
            return _origin.classCfgPrimitive();
        }

        @Override
        public boolean markFinal()
        {
            return _origin.markFinal();
        }

        @Override
        public boolean classCfgUnion()
        {
            return _origin.classCfgUnion();
        }

        @Override
        public Symbol superClass()
        {
            var superClass = _origin.superClass();
            return superClass != null ? superClass.replaceType(this) : null;
        }

        @Override
        public Vector<Symbol> subclasses()
        {
            return _subclasses;
        }

        @Override
        public void initSubclasses()
        {
            _subclasses = _subclasses == null ? new Vector<>() : _subclasses;
        }

        @Override
        public Vector<Symbol> subInterfaces()
        {
            return _subInterfaces;
        }

        @Override
        public void initSubInterfaces()
        {
            _subInterfaces = _subInterfaces == null ? new Vector<>() : _subInterfaces;
        }

        @Override
        public Vector<Symbol> implementedInterfaces()
        {
            var list = _origin.implementedInterfaces();
            if (list == null)
            {
                return null;
            }
            var r = new Vector<Symbol>();
            for (var type : list)
            {
                r.add(type.replaceType(this));
            }
            return r;
        }

        @Override
        public Vector<Symbol> superInterfaces()
        {
            var list = _origin.superInterfaces();
            if (list == null)
            {
                return null;
            }
            var r = new Vector<Symbol>();
            for (var type : list)
            {
                r.add(type.replaceType(this));
            }
            return r;
        }

        @Override
        public Symbol definitionPackage()
        {
            return _origin.definitionPackage();
        }

        @Override
        public String packageID()
        {
            return _origin.packageID();
        }

        @Override
        public Symbol constructorFunction()
        {
            var f = _origin.constructorFunction();
            return f == null ? null : f.replaceType(this);
        }

        @Override
        public boolean isInstantiated()
        {
            return true;
        }

        @Override
        public String toString()
        {
            var builder = new StringBuilder();
            for (var type : _arguments)
            {
                builder.append((builder.length() == 0 ? "" : ", ") + type.toString());
            }
            return String.format("%1$s.<%2$s>", (packageID().equals("") ? "" : packageID() + ".") + _origin.name().toString(), builder.toString());
        }
    }

    public final static class FunctionType extends Type
    {
        private Vector<Symbol> _params;
        private Vector<Symbol> _optParams;
        private boolean _rest;
        private Symbol _result;

        public FunctionType(Vector<Symbol> params, Vector<Symbol> optParams, boolean rest, Symbol result)
        {
            _params = params;
            _optParams = optParams;
            _rest = rest;
            _result = result;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.FUNCTION_TYPE;
        }

        @Override
        public Vector<Symbol> params()
        {
            return _params;
        }

        @Override
        public Vector<Symbol> optParams()
        {
            return _optParams;
        }

        @Override
        public boolean rest()
        {
            return _rest;
        }

        @Override
        public Symbol result()
        {
            return _result;
        }

        @Override
        public Symbol superClass()
        {
            return pool().objectType;
        }

        @Override
        public String toString()
        {
            var builder = new StringBuilder();
            if (params() != null)
            {
                for (var type : params())
                {
                    builder.append((builder.length() == 0 ? "" : ", ") + type.toString());
                }
            }
            if (optParams() != null)
            {
                for (var type : optParams())
                {
                    builder.append((builder.length() == 0 ? "" : ", ") + type.toString());
                }
            }
            if (rest())
            {
                builder.append((builder.length() == 0 ? "" : ", ") + "...");
            }

            return String.format("function(%1$s):%2$s", builder.toString(), result().toString());
        }
    }

    public final static class TupleType extends Type
    {
        private Vector<Symbol> _elements;

        public TupleType(Vector<Symbol> elements)
        {
            _elements = elements;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.TUPLE_TYPE;
        }

        @Override
        public Vector<Symbol> tupleElements()
        {
            return _elements;
        }

        @Override
        public Symbol superClass()
        {
            return pool().objectType;
        }

        @Override
        public String toString()
        {
            var vector = new Vector<String>();
            for (var element : _elements)
            {
                vector.add(element.toString());
            }
            return String.format("[%1$s]", VectorUtils.join(vector, ", "));
        }
    }

    public final static class NullableType extends Type
    {
        private Symbol _overType;

        public NullableType(Symbol overType)
        {
            _overType = overType;
            _delegate = overType.delegate();
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.NULLABLE_TYPE;
        }

        @Override
        public Symbol overType()
        {
            return _overType;
        }

        @Override
        public Symbol superClass()
        {
            return _overType.superClass();
        }

        @Override
        public boolean containsUndefined()
        {
            return false;
        }

        @Override
        public boolean containsNull()
        {
            return true;
        }

        @Override
        public Symbol origin()
        {
            return _overType.origin();
        }

        @Override
        public boolean isInitialisable()
        {
            return _overType.isInitialisable();
        }

        @Override
        public boolean isConstructable()
        {
            return _overType.isConstructable();
        }

        @Override
        public boolean isFlagEnum()
        {
            return _overType.isFlagEnum();
        }

        @Override
        public Symbol defaultValue()
        {
            return pool().createUndefinedConstantValue(this);
        }

        @Override
        public String toString()
        {
            return "?" + _overType.toString();
        }
    }

    public final static class TypeParameterSymbol extends Type
    {
        private Symbol _name;
        private Symbol _definitionType;
        private Symbol _defaultType;
        private Vector<Symbol> _implements;

        public TypeParameterSymbol(Symbol name, Symbol definitionType, Symbol anyType)
        {
            _name = name;
            _definitionType = definitionType;
            _defaultType = anyType;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.TYPE_PARAMETER;
        }

        @Override
        public Symbol name()
        {
            return _name;
        }

        @Override
        public Symbol definitionType()
        {
            return _definitionType;
        }

        @Override
        public Symbol defaultType()
        {
            return _defaultType;
        }

        @Override
        public void setDefaultType(Symbol type)
        {
            _defaultType = type;
        }

        @Override
        public Vector<Symbol> implementedInterfaces()
        {
            return _implements;
        }

        @Override
        public void initImplementedInterfaces()
        {
            _implements = _implements == null ? new Vector<>() : _implements;
        }

        @Override
        public String toString()
        {
            return _name.toString();
        }
    }

    public final static class VerifyingType extends Type
    {
        public VerifyingType()
        {
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.VERIFYING_TYPE;
        }

        @Override
        public boolean containsUndefined()
        {
            return true;
        }

        @Override
        public boolean containsNull()
        {
            return true;
        }

        @Override
        public String toString()
        {
            return "*";
        }
    }
}