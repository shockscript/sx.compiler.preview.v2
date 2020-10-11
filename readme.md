# ShockScript compiler

Currently work is going on applying runtime reflection for ShockScript and target ShockScript → WASM.

This repository is the last version of various attempts on writting ShockScript compiler infrastructure (they never reached to complete codegen.) since 2017.

Copyright © 2017 @Hydroper

## Reference

- [Semantics API](https://shockscript.github.io/sxc/semantics-api/)
- [Parser API](https://shockscript.github.io/sxc/parser-api/)
- Verifier: exposes minimal methods.

## Building

NOTE: The current repository does not currently build. If you want to test a previous version, stop on [this commit](https://github.com/shockscript/sxc/archive/ad46840bc8335a81a620ebc2350690b879bd2d77.zip).

Requirements: JDK 14+ and Maven. Ubuntu install command: `sudo apt install openjdk-14-jdk maven`.

To build the compiler application (which does not generate code yet), run:

```bash
. build.sh
```

## License

Licensed under [Mozilla Public License 2.0](https://www.mozilla.org/en-US/MPL/2.0/).