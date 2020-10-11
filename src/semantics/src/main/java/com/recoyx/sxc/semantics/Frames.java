package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.recoyx.sxc.semantics.errors.*;

public final class Frames
{
    static public class Frame extends Symbol
    {
        private Symbol _parentFrame;
        private Activation _activation;

        @Override
        public Symbol parentFrame()
        {
            return _parentFrame;
        }

        @Override
        public void setParentFrame(Symbol frame)
        {
            _parentFrame = frame;
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

        @Override
        public Symbol searchPublicNamespace()
        {
            for (Symbol frame = this; frame != null; frame = frame.parentFrame())
            {
                if (frame.isPackageFrame())
                {
                    return frame.symbol().publicNamespace();
                }
            }
            return null;
        }

        @Override
        public Symbol searchPrivateNamespace()
        {
            Symbol ns = null;
            for (Symbol frame = this; frame != null; frame = frame.parentFrame())
            {
                if (frame.symbol() != null && (ns = frame.symbol().privateNamespace()) != null)
                {
                    return ns;
                }
            }
            return null;
        }

        @Override
        public Symbol searchProtectedNamespace()
        {
            Symbol ns = null;
            for (Symbol frame = this; frame != null; frame = frame.parentFrame())
            {
                if (frame.symbol() != null && (ns = frame.symbol().protectedNamespace()) != null)
                {
                    return ns;
                }
            }
            return null;
        }

        public Symbol searchInternalNamespace()
        {
            Symbol ns = null;
            for (Symbol frame = this; frame != null; frame = frame.parentFrame())
            {
                if (frame.symbol() != null && (ns = frame.symbol().internalNamespace()) != null)
                {
                    break;
                }
                else if ((ns = frame.internalNamespace()) != null)
                {
                    break;
                }
            }
            return ns;
        }

        @Override
        public Vector<Symbol> getFrameListing()
        {
            var list = new Vector<Symbol>();
            for (Symbol frame = this; frame != null; frame = frame.parentFrame())
            {
                list.insertElementAt(frame, 0);
            }
            return list;
        }
    }

    static public class BlockFrame extends Frame
    {
        private Names _ownNames = new Names();
        private Vector<Symbol> _openNamespaceList;
        private Symbol _defaultNamespace;
        private Vector<Symbol> _importPackageList;
        private Symbol _internalNamespace;

        public BlockFrame()
        {
            super();
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.BLOCK_FRAME;
        }

        @Override
        public Names ownNames()
        {
            return _ownNames;
        }

        @Override
        public Vector<Symbol> openNamespaceList()
        {
            return _openNamespaceList;
        }

        @Override
        public Symbol defaultNamespace()
        {
            return _defaultNamespace;
        }

        @Override
        public void setDefaultNamespace(Symbol ns)
        {
            _defaultNamespace = ns;
        }

        @Override
        public Symbol internalNamespace()
        {
            return _internalNamespace;
        }

        @Override
        public void setInternalNamespace(Symbol ns)
        {
            _internalNamespace = ns;
        }

        @Override
        public Vector<Symbol> importPackageList()
        {
            return _importPackageList;
        }

        @Override
        public void importPackage(Symbol pckg)
        {
            importPackage(pckg, true);
        }

        @Override
        public void importPackage(Symbol pckg, boolean openPublic)
        {
            if (symbol() == pckg)
            {
                return;
            }
            _importPackageList = _importPackageList != null ? _importPackageList : new Vector<Symbol>();
            if (!_importPackageList.contains(pckg))
            {
                _importPackageList.add(pckg);
            }
            if (openPublic)
            {
                openNamespace(pckg.publicNamespace());
            }
        }

        @Override
        public void openNamespace(Symbol ns)
        {
            _openNamespaceList = _openNamespaceList != null ? _openNamespaceList : new Vector<Symbol>();
            _openNamespaceList.add(ns);
        }

        @Override
        public Symbol lookupName(Symbol name)
            throws AmbiguousReferenceError
        {
            var s = _ownNames.lookupName(name);
            if (s != null)
            {
                return pool().createFrameProperty(this, s);
            }
            if (this._importPackageList != null)
            {
                for (var p : this._importPackageList)
                {
                    if ((s = p.lookupName(name)) != null)
                    {
                        return s;
                    }
                }
            }
            return this.parentFrame() != null ? this.parentFrame().lookupName(name) : null;
        }

        @Override
        public Symbol lookupMultiName(Vector<Symbol> namespaces, String localName)
            throws AmbiguousReferenceError
        {
            Symbol s1 = null
                ,  s2 = null;
            var innerSymbol = symbol();
            if (innerSymbol != null)
            {
                // additional multi-name lookups on package, class, interface and enum
                s1 = innerSymbol.lookupMultiName(namespaces, localName);
                if (innerSymbol.isClassType())
                {
                    for (var superClass : innerSymbol.descendingClassHierarchy())
                    {
                        if ((s2 = superClass.lookupMultiName(namespaces, localName)) != null)
                        {
                            if (s1 != null)
                            {
                                throw new AmbiguousReferenceError(localName);
                            }
                            else
                            {
                                s1 = s2;
                            }
                        }
                    }
                }
                if (innerSymbol.isType())
                {
                    s2 = innerSymbol.delegate().lookupMultiName(namespaces, localName);
                    if (s2 != null && s2.isNamespace())
                    {
                        if (s1 != null)
                        {
                            throw new AmbiguousReferenceError(localName);
                        }
                        else
                        {
                            s1 = s2;
                        }
                    }
                }
            }
            s2 = _ownNames.lookupMultiName(namespaces, localName);
            if (s2 != null)
            {
                if (s1 != null)
                {
                    throw new AmbiguousReferenceError(localName);
                }
                else
                {
                    s1 = pool().createFrameProperty(this, s2);
                }
            }
            if (_importPackageList != null)
            {
                for (var pckg : _importPackageList)
                {
                    if ((s2 = pckg.lookupMultiName(namespaces, localName)) != null)
                    {
                        if (s1 != null)
                        {
                            throw new AmbiguousReferenceError(localName);
                        }
                        else
                        {
                            s1 = s2;
                        }
                    }
                }
            }
            return s1 != null ? s1 : parentFrame() != null ? parentFrame().lookupMultiName(namespaces, localName) : null;
        }
    }

    static public class ClassFrame extends BlockFrame
    {
        private Symbol _symbol;

        public ClassFrame(Symbol symbol)
        {
            super();
            _symbol = symbol;
            for (var superClass : symbol.ascendingClassHierarchy())
            {
                openNamespace(superClass.protectedNamespace());
            }
            openNamespace(symbol.privateNamespace());
            openNamespace(symbol.protectedNamespace());
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.CLASS_FRAME;
        }

        @Override
        public Symbol symbol()
        {
            return _symbol;
        }

        @Override
        public Symbol lookupName(Symbol name)
            throws AmbiguousReferenceError
        {
            Symbol s = _symbol.lookupName(name);
            if (s != null)
            {
                return s;
            }
            for (var type : symbol().descendingClassHierarchy())
            {
                if ((s = type.lookupName(name)) != null)
                {
                    return s;
                }
            }
            return super.lookupName(name);
        }
    }

    static public class EnumFrame extends BlockFrame
    {
        private Symbol _symbol;

        public EnumFrame(Symbol symbol)
        {
            super();
            _symbol = symbol;
            openNamespace(symbol.privateNamespace());
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.ENUM_FRAME;
        }

        @Override
        public Symbol symbol()
        {
            return _symbol;
        }

        @Override
        public Symbol lookupName(Symbol name)
            throws AmbiguousReferenceError
        {
            Symbol s = _symbol.lookupName(name);
            return s != null ? s : super.lookupName(name);
        }
    }

    static public class InterfaceFrame extends BlockFrame
    {
        private Symbol _symbol;

        public InterfaceFrame(Symbol symbol)
        {
            super();
            _symbol = symbol;
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.INTERFACE_FRAME;
        }

        @Override
        public Symbol symbol()
        {
            return _symbol;
        }

        @Override
        public Symbol lookupName(Symbol name)
            throws AmbiguousReferenceError
        {
            Symbol s = _symbol.lookupName(name);
            return s != null ? s : super.lookupName(name);
        }
    }

    static public class PackageFrame extends BlockFrame
    {
        private Symbol _symbol;

        public PackageFrame(Symbol symbol)
        {
            super();
            _symbol = symbol;
            openNamespace(_symbol.publicNamespace());
            openNamespace(_symbol.internalNamespace());
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.PACKAGE_FRAME;
        }

        @Override
        public Symbol symbol()
        {
            return _symbol;
        }

        @Override
        public Symbol lookupName(Symbol name)
            throws AmbiguousReferenceError
        {
            Symbol s = _symbol.lookupName(name);
            return s != null ? s : super.lookupName(name);
        }
    }

    static public class ParameterFrame extends Frame
    {
        private Names _ownNames = new Names();
        private Symbol _parameterThis;

        public ParameterFrame(Symbol parameterThis)
        {
            super();
            _parameterThis = parameterThis;
        }

        public ParameterFrame()
        {
            this(null);
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.PARAMETER_FRAME;
        }

        @Override
        public Names ownNames()
        {
            return _ownNames;
        }

        @Override
        public Symbol parameterThis()
        {
            return _parameterThis;
        }

        @Override
        public Symbol lookupName(Symbol name)
            throws AmbiguousReferenceError
        {
            var s = _ownNames.lookupName(name);
            if (s != null)
            {
                return pool().createFrameProperty(this, s);
            }
            s = parameterThis() != null ? parameterThis().lookupName(name) : null;
            return s != null ? s : parentFrame() != null ? parentFrame().lookupName(name) : null;
        }

        @Override
        public Symbol lookupMultiName(Vector<Symbol> namespaces, String localName)
            throws AmbiguousReferenceError
        {
            var s = _ownNames.lookupMultiName(namespaces, localName);
            if (s != null)
            {
                return pool().createFrameProperty(this, s);
            }
            s = _parameterThis != null ? _parameterThis.lookupMultiName(namespaces, localName) : null;
            if (s != null && s.kind() != SymbolKind.OBJECT_DYNAMIC_PROPERTY)
            {
                return s;
            }
            Symbol s2 = parentFrame() != null ? parentFrame().lookupMultiName(namespaces, localName) : null;
            return s2 != null ? s2 : s;
        }
    }

    static public class WithFrame extends Frame
    {
        /**
         * @private
         */
        protected Symbol _symbol;

        public WithFrame()
        {
            super();
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.WITH_FRAME;
        }

        @Override
        public Symbol symbol()
        {
            return _symbol;
        }

        @Override
        public Symbol lookupName(Symbol name)
            throws AmbiguousReferenceError
        {
            var s = _symbol.lookupName(name);
            return s != null ? s : parentFrame() != null ? parentFrame().lookupName(name) : null;
        }

        @Override
        public Symbol lookupMultiName(Vector<Symbol> namespaces, String localName)
            throws AmbiguousReferenceError
        {
            var s = _symbol.lookupMultiName(namespaces, localName);
            if (s != null && s.kind() != SymbolKind.OBJECT_DYNAMIC_PROPERTY)
            {
                return s;
            }
            Symbol s2 = parentFrame() != null ? parentFrame().lookupMultiName(namespaces, localName) : null;
            return s2 != null ? s2 : s;
        }
    }

    static public class ForFrame extends Frame
    {
        private final Names _ownNames = new Names();

        public ForFrame()
        {
            super();
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.FOR_FRAME;
        }

        @Override
        public Names ownNames()
        {
            return _ownNames;
        }

        @Override
        public Symbol lookupName(Symbol name)
            throws AmbiguousReferenceError
        {
            var s = _ownNames.lookupName(name);
            if (s != null)
            {
                return pool().createFrameProperty(this, s);
            }
            return parentFrame() != null ? parentFrame().lookupName(name) : null;
        }

        @Override
        public Symbol lookupMultiName(Vector<Symbol> namespaces, String localName)
            throws AmbiguousReferenceError
        {
            var s = _ownNames.lookupMultiName(namespaces, localName);
            if (s != null)
            {
                return pool().createFrameProperty(this, s);
            }
            return parentFrame() != null ? parentFrame().lookupMultiName(namespaces, localName) : null;
        }
    }

    static public class ConditionFrame extends Frame
    {
        private final Names _ownNames = new Names();

        public ConditionFrame()
        {
            super();
        }

        @Override
        public SymbolKind kind()
        {
            return SymbolKind.CONDITION_FRAME;
        }

        @Override
        public Names ownNames()
        {
            return _ownNames;
        }

        @Override
        public Symbol lookupName(Symbol name)
            throws AmbiguousReferenceError
        {
            var s = _ownNames.lookupName(name);
            if (s != null)
            {
                return pool().createFrameProperty(this, s);
            }
            return parentFrame() != null ? parentFrame().lookupName(name) : null;
        }

        @Override
        public Symbol lookupMultiName(Vector<Symbol> namespaces, String localName)
            throws AmbiguousReferenceError
        {
            var s = _ownNames.lookupMultiName(namespaces, localName);
            if (s != null)
            {
                return pool().createFrameProperty(this, s);
            }
            return parentFrame() != null ? parentFrame().lookupMultiName(namespaces, localName) : null;
        }
    }
}