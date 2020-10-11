# Work tracker

## shockscript-analysis

### Semantics subpackage

- [ ] Context
- [ ] Names
- [ ] Slot (indicates the object it was defined in)
  - [ ] Variable slot
  - [ ] Virtual slot
  - [ ] Method slot
- [ ] Meta data
- [ ] Namespaces (explicit, public, private, protected, internal)
- [ ] Namespace set
- [ ] Scope chain
- [ ] Value
  - [ ] Constant
    - [ ] undefined constant
    - [ ] null constant
    - [ ] Namespace constant
    - [ ] String constant
    - [ ] Boolean constant
    - [ ] Number constant
    - [ ] uint constant
    - [ ] int constant
    - [ ] BigInt constant
    - [ ] Char constant
    - [ ] Enum constant (variable Number representation)
  - [ ] Object value (indicates the object it was defined in)
    - [ ] Frame
      - [ ] Activation
    - [ ] Type
      - [ ] \*
      - [ ] void
      - [ ] null
      - [ ] Class
      - [ ] Enum
      - [ ] Interface
      - [ ] Instantiated type
      - [ ] Nullable
      - [ ] Tuple
      - [ ] Type parameter
    - [ ] Package
    - [ ] Delegate
  - [ ] Reference value
  - [ ] Conversion value (indicates whether it is performed explicitly and if it is optional)

## Differences from previous version

- [ ] No parameterized type, with the exception of Vector.
- [ ] Untyped rest parameter in functions.
- [ ] Only tuples as structural types.
- [ ] Enum manipulation changes.

### Verification subpackage