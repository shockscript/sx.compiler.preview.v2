# Rust codegen. tricks

## External crates

- [ryu-ecmascript](https://crates.io/crates/ryu-ecmascript) (ToString operator applied to Number)
- [pgc](https://crates.io/crates/pgc) (garbage collector)

## Auto boxing

Primitive classes are represented as `struct`s and union classes are represented as `union`s. Besides a `struct`/`union` generated in Rust, another `struct` may be generated for the boxed form of primitive and union classes.

## Class inheriting

```
use std::rc::Rc;

#[repr(C)]
struct A {
    ...
}

impl A {
    fn as_b(self:&Rc<Self>) -> Rc<B> {
        unsafe { Rc::from_raw(Rc::into_raw(self.clone()).cast::<B>()) }
    }
}

#[repr(C)]
struct B {
    _a: A,
    ...
}

impl B {
    fn new() -> Rc<Self> {
        Rc::new(B { ... })
    }

    fn as_a(self: &Rc<Self>) -> Rc<A> {
        unsafe { Rc::from_raw(Rc::into_raw(self.clone()).cast::<A>()) }
    }
}
```

## Debugging

Lines and columns won't retain, as I guess there's no way to manipulate generated Rust spans, but I'll attempt to retain qualified definition names.

## Dictionary

Weak keys on Dictionary can be supported by PR-ing WeakMap to rust-gc.

## Multi-threading

The concurrency API is left unimplemented since rust-gc objects are local-per-thread.