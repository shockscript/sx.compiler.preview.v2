package com.recoyx.sxc.semantics;

import java.util.Vector;

public final class NamespaceSet extends Symbol
{
	private Vector<Symbol> _nss = new Vector<>();

	@Override
	public SymbolKind kind()
	{
		return SymbolKind.NAMESPACE_SET;
	}

	@Override
	public Vector<Symbol> nss()
	{
		return _nss;
	}
}