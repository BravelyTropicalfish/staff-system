# cpp-security — C++ Security

## Metadata

| Field | Value |
|---|---|
| **ID** | cpp-security |
| **Category** | security |
| **Priority** | Critical |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | cpp |

## Rule

## Memory Safety

- Never use raw `new`/`delete` — use smart pointers
- Never use C-style arrays — use `std::array` or `std::vector`
- Never use `malloc`/`free` — use C++ allocation
- Avoid `reinterpret_cast` unless absolutely necessary

## Buffer Overflows

- Use `std::string` over `char*`
- Use `.at()` for bounds-checked access when safety matters
- Never use `strcpy`, `strcat`, `sprintf` — use `std::string` or `fmt::format`

## Undefined Behavior

- Always initialize variables
- Avoid signed integer overflow
- Never dereference null or dangling pointers
- Use sanitizers in CI:
  ```bash
  cmake -DCMAKE_CXX_FLAGS="-fsanitize=address,undefined" ..
  ```

## Static Analysis

- Use **clang-tidy** for automated checks:
  ```bash
  clang-tidy --checks='*' src/*.cpp
  ```
- Use **cppcheck** for additional analysis:
  ```bash
  cppcheck --enable=all src/
  ```

## Reference

## Rationale

Stack-specific security norms for `cpp` keep agent output aligned with the project's chosen stack without duplicating common Rules.

## Verification

| Check | Blocks |
|---|---|
| PR review against this rule when touching `cpp` sources | pr-merge |

## Examples

See Good/Bad (or CORRECT/WRONG) samples in the Rule section when present.

## Exceptions

- None unless recorded in the project decision log / org policy.
