package com.recoyx.sxc.semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import com.recoyx.sxc.semantics.errors.*;

public final class MethodOverload
{
    /**
     * Links required parameters to matching types.
     */
    public HashMap<Integer, Symbol> match = new HashMap<>();

    public Symbol method = null;
}