package com.recoyx.sxc.semantics;

import java.util.Vector;

public final class MetaData
{
	public String name;
	public final Vector<MetaDataEntry> entries;

	public MetaData(String name, Vector<MetaDataEntry> entries)
	{
		this.name = name;
		this.entries = entries;
	}

	public MetaDataEntry findEntry(String name)
	{
		for (var entry : entries)
		{
			if (entry.name.equals(name))
				return entry;
		}
		return null;
	}
}