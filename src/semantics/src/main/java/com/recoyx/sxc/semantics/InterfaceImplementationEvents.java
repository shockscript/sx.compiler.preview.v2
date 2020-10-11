package com.recoyx.sxc.semantics;

public interface InterfaceImplementationEvents
{
    void onundefined(String kind, Symbol name, Symbol signature);
    void onwrong(String kind, Symbol name);
}