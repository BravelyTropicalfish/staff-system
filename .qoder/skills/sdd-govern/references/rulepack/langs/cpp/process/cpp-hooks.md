# cpp-hooks — C++ Hooks

## Metadata

| Field | Value |
|---|---|
| **ID** | cpp-hooks |
| **Category** | process |
| **Priority** | Medium |
| **Activation** | auto |
| **Applies To** | all |
| **Stack** | cpp |

## Rule

## Build Hooks

Run these checks before committing C++ changes:

```bash
# Format check
clang-format --dry-run --Werror src/*.cpp src/*.hpp

# Static analysis
clang-tidy src/*.cpp -- -std=c++17

# Build
cmake --build build

# Tests
ctest --test-dir build --output-on-failure
```

## Recommended CI Pipeline

1. **clang-format** — formatting check
2. **clang-tidy** — static analysis
3. **cppcheck** — additional analysis
4. **cmake build** — compilation
5. **ctest** — test execution with sanitizers

## Rationale

Stack-specific hooks and automation norms for `cpp` keep agent output aligned with the project's chosen stack without duplicating common Rules.

## Verification

| Check | Blocks |
|---|---|
| PR review against this rule when touching `cpp` sources | pr-merge |

## Examples

See Good/Bad (or CORRECT/WRONG) samples in the Rule section when present.

## Exceptions

- None unless recorded in the project decision log / org policy.
