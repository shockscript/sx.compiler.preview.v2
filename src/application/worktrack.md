# Work track

## Verifier

- [ ] Report incorrect use of language meta data.
- [ ] Report incorrect proxy definitions.

## Generic constraints

ShockScript is introducing beautiful generic constraints, using meta data.

With this, the following changes should occur:

- [ ] Remove arithmetic internals from the Range class (constructor included).
- Verify and consume every `[Where]` meta data. It may appear in the following forms. The meta data may repeat as many times as necessary.
  - [x] `[Where("T", numeric=true)]`
  - [ ] `[Where("T", extends="IoC")]`
  - [ ] `[Where("T", implements="I")]`
  - [x] `[Where("T", defaultType="DefaultType")]`
- `typeParameter` semantics
  - [x] `isNumericTypeParameter`
  - [x] Default type
  - [x] Implements interfaces
- [ ] Fully define type parameter constraints among directive sequence verification immediately after the gamma type definitions phase.
  - [ ] For every implemented interface, derive every property and operator.
- [x] Unary and binary operators: skip proxy validation for numeric type parameters.
- [ ] Verifier: left every `extends`/`implements` constrained argument on `.<...>` unresolved and resolve every of them on every class/interface of directive sequence verification immediately after the gamma phase (after interface 2).
- [ ] Verifier: when a generic type is referenced omitting arguments, default types are used. Then, if any default type does not satisfy type parameter constraints, a verify error is reported on the enclosing reference node.

## Friend types

- [ ] `[Friend("T")]` (applicable to class/enum)
- [ ] Bytecode record
- [ ] Semantics record