package com.recoyx.shockscript.analysis.semantics;

import com.recoyx.shockscript.analysis.util.Vector;

public final class MetaData
{
	static public final class Entry
	{
		public String name;
		public Object value;

		public Entry(String name, Object value)
		{
			this.name = name;
			this.value = value;
		}
	}

	public String name;
	public final Vector<MetaData.Entry> entries;

	public MetaData(String name, Vector<MetaData.Entry> entries)
	{
		this.name = name;
		this.entries = entries;
	}

	public MetaData.Entry findEntry(String name)
	{
		for (var entry : entries)
			if (entry.name.equals(name))
				return entry;
		return null;
	}
}