- [ ] NSS support: `namespace p = "com.recoyx.product";`; this will contain any subpackage namespaces.
- [ ] Auto normalize quote sections (may nest) and Algebra less-than/greater-than characters on InputElementDiv and InputElementRegExp.
- [ ] Reform on with statement: runtime lexical property computation. Every enclosing frame will describe a property dictionary at runtime. Based on E4X, lexical name calls resolve to parent frames only. Constant expressions are consequently impossible under with().
- [ ] Support a sxc.json file (which describes "external" functions and "overload" method optimizations).
- [ ] Allow 'this' literal to refer to the current inheritance class by introducing a ThisClass symbol.

```
class A
{
    static function f():void
    {
        trace(this);
    }
}

class B extends A {}

B.f();
```

- [ ] Allow overriding class self-attached methods.
- [ ] Support the following proxy for enums and for every enum define a self-attached convertDefault() method (equivalent to a default `E(s)` call).

```
enum E
{
    static proxy function fromExplicit(v:*):E
    {
        return E.convertDefault(v);
    }
}
```

- [x] Standard library: eliminate alternative numeric types (keep only Char, uint, int, Number and introduce BigInt).
- [ ] Restart the STL sources.
  - [ ] Object.valueOf()
  - [ ] Object.constructor
  - [ ] sx.global.getQualifiedDefinition()
  - [ ] The Range class is not parameterized anymore; it adapts to any type passed via the constructor. It simply stores the start, end and step arguments as the type \*.
  - [ ] Calling Range(10, 0) gives -1 step.
  - [ ] Delete Range.inclusive. Change the Range constructor's third argument to step and delete the RangeOptions class.
  - [ ] Support Array concatenation via "+".
  - [ ] Generator shall not be parameterized.
  - [ ] Allow String.apply to take an Object or Dictionary: `"${id}".apply({ id: "foo" })`
  - [ ] Define CharArray and eliminate CharInput and StringOutput classes.
  - [ ] uint and int are 64-bits types. Modify their MIN_VALUE and MAX_VALUE constants.
  - [ ] Dictionary should not be a parameterized type. It should also support alternative methods such as getProperty(), setProperty() and deleteProperty().
  - [ ] Promise, Set
- [ ] 'await' operator (usable in functions annotated to return Promise). 'await' cannot be paired with 'yield'.
- [ ] Optional parameter's default value is allowed to be non-constant
- [ ] Generalize spread operator on object initialiser and array initialiser.
- [x] Change enum constant's internal value (should be any numeric type).
- [x] ?T contains undefined? = false
- [x] undefined implicitly converts to ?T
- [x] Char, Number, uint, int and BigInt implicitly convert to each other.
- [x] Eliminate structural function and union types.
- [x] Type configuration and meta-data
  - [x] The meta-data "TypeConfig" replaces "Primitive" and "Union".
  - [x] The meta-data "TypeConfig" replaces "Flags" and "EnumConfig".
  - [x] Class cannot be, by default, initialized by {}.
  - [x] Remove the "minimal" option (serialization) of enums. They will always be serializable.
  - [x] Class option "privateInitialiser" is now "dynamicInit".
  - [x] Class option "constructor=Boolean"
- [ ] Support flags construct `var e:E = ["flag1", "flag2", "flag3"];` and optimize it. Array will implicitly convert to flags enums.
- [x] Re-arrange enum operators
  - Do not use bitwise operators for flagging; instead use "+" for set inclusion and "-" for set exclusion.
  - Define E.filter(); example: e.filter("deletesPosts", "editsPosts") will only keep the specified flags if set.
  - SXDoc: document filter(), valueOf()
- [ ] Reflection
  - [x] Define the classes Array, Function and Class. Class represents either a class, interface, enum or tuple type. Class will allow to enumerate the enum constants.
  - [x] Rework on dynamic property symbols as proxy property symbols.
  - [x] Re-introduce dynamic property symbol, whose static type is always \*. This symbol means "1) resolve for trait, otherwise for 2) proxy-supported property or otherwise for 3) dynamic property".
  - [x] Delete structural union access support and symbols.
  - [ ] Allow array initializer typed by \*, Object or Array. Default input/output type is \*, do not throw verify error if there is no context type.
  - [ ] Fix object initializer typed by \*, Object or Dictionary. Default input/output type is \*, do not throw verify error if there is no context type.
    - Dictionary is not parameterized.
  - [ ] Extend operator mechanism:
    - [ ] Delete operator
      - Applicable to \* or Object typed object.
    - [ ] Call operator
      - Applicable to direct Function (result from definition reference).
      - Applicable to indirect Function.
    - [ ] Property operators (dot, brackets, descendants and filter)
      - Applicable to \* and Object.
      - Dot or brackets will result in either a compile-time trait or dynamic property.
    - [ ] Type operators
      - Right operand is allowed to be a \*, Object or Class reference.
    - [ ] Bitwise, multiplicative and additive operators
      are applicable to \* and Object,
      and not anymore applicable to type parameters.
  - [ ] Brackets operator shall be applicable to any type.
  - [ ] Dot/brackets operators are similiar to AS3.0; however SX deduces statically when it's access of a proxy-supported property.
  - [ ] "new" operator applies to expression.
  - [ ] Refresh call operator applied to type. Eliminate unnecessary code for single object or array initializer arguments.
    - Single argument call operator applied to a type will first try a compile time conversion. If it fails, then the result is a 1) runtime conversion or 2) runtime constructor invokation.
  - [ ] Refresh new operator. Eliminate unnecessary code for special single argument cases.
  - [ ] Return of accessing functions and methods should be Function, although their compile-time signature is matched on call.
- [ ] The language will support "dynamic" classes. Object class shall be dynamic and the immediate result of the expression {}; it shall implicitly convert to Dictionary or initializable classes. Subclasses of Object won't be dynamic unless they contain the "dynamic" modifier.
- [ ] Define the instance property Function.length
- [ ] Allow overriding method with specialized return type.
- [ ] A lexical reference "com.qux.x" resolves in one of the following ways:
  - Package: com
  - Package: com.qux
  - Package: com.qux.x
  - Non-package symbol (preference if defined)
- [ ] ShockScript STL (standard library) will move to packages such as:
  - "sx.utils" (proxy, among others)
  - "sx.global" (shall be automatically imported)
  - "sx.intl" (internationalization)
  - "sx.compiler" (feature enabled when eval() is enabled)
  - "sx.reflection" (describeType(), describeParameterTypes())
- [ ] String.charAt() and String.charCodeAt() shall be equivalent.
- [ ] Nested types.
- [ ] Support for proxy::fromExplicit() (before named proxy::convert()).
- [ ] Support for sx.global.eval() function. Unlike ECMAScript, eval() doesn't derive the callee scope. In ShockScript, eval() does not derive all packages from the callee program and is meant as a form of sandboxed scripting in applications. It supports a second argument: `{ packages: {}, scope: {}, warnMissingTypeDecl: Boolean }`, where a package can be entirely imported via "\*" and "scope" allows to specify names for the root frame. The eval() program may return value other than undefined. eval() must return compiler diagnostics and the resulting value together.
- [ ] Differently from AS3.0, you can dynamically call Function objects omitting parameters. Parameters whose type lacks default value cannot be omitted, however.
- [ ] The rest parameter is now untyped: Array. Do not allow rest parameter to be typed.
- [ ] Class static properties shall be inherited.
- [ ] Allow to override method with extra optional parameters.
- [ ] Call operator of exactly _one_ argument over a Class object will either convert value or construct a new object, based on a runtime signature match. Call operator will otherwise construct a new object.
- [ ] Compile-time optimization of `for(var i:uint in Range(0, j))`.
- [ ] Per-script option: disable the type annotation miss warning.
- [x] The Char type supports all operators supported by the Number type.

## Semantic model reform

- [ ] Introduce ObjectValue symbol, from which Package, Delegate, Activation, Frame and Type must inherit.
- [ ] Activation < Frame
- [ ] Types must not store the "package" they're in specifically. A new "definedIn" property will serve as full qualifier. The new method "fullyQualifiedName()" will help this purpose.
