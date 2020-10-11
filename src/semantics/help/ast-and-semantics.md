# AST & Semantics

## Destructuring

- Array patterns may apply to tuple and Array-based types. Array-based types have readable dynamic properties with numeric key. A slice method is not required for the rest pattern.

## Call operator

- A call operator whose `obj.isValue` and `obj.staticType` is Generator or ?Generator returns the result of starting or resuming the Generator function.

## Unary operator

Operand type T (undefined proxy; positive and bitwise not operators):

- T may be a numeric type parameter or nullable numeric type parameter (?T).

Numeric operand type:

- The operator implementation is directly on codegen.

## Binary operator

Operand type T (undefined proxy; arithmetic and bitwise operators):

- T may be a numeric type parameter or nullable numeric type parameter (?T).

Numeric operand type (arithmetic and bitwise operators):

- The operator implementation is on codegen.

String:

- Equality operators (===, !==) implementation is on codegen.