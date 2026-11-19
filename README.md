# Brightline Loyalty Service

A Java service that calculates loyalty points from a member's tier and purchase amount. It uses an in-memory member repository and includes a command-line demo. This project refactors the inherited implementation described in [ANALYSIS.md](ANALYSIS.md).

> **Pre-coding check-in:** [ANALYSIS.md](ANALYSIS.md) is the analysis and planned approach written before any code. It is left unchanged. This README describes what was built.

---

## 1. Deliverables Map

| # | Deliverable | Where it is |
| --- | --- | --- |
| 1 | Analysis of the inherited code and tests | [ANALYSIS.md](ANALYSIS.md), sections 2 and 3 |
| 2 | Refactored code, injected dependencies, and extensible tier rules | `src/main/java/com/brightline/loyalty/`, sections 4 to 6 |
| 3 | Domain exceptions and error handling | `exception/` package, section 7 |
| 4 | JUnit 5 and Mockito test suite | `src/test/java/com/brightline/loyalty/`, section 8 |
| 5 | Design decisions and what was kept, fixed, or rewritten | Sections 5, 6, 11, and 12 |
| 6 | Improvement beyond the brief: parameterized tests | Section 9 |

## 2. Business Rules

These are the behaviours observed in the inherited code and recorded in `ANALYSIS.md`:

* Base points are one point per complete GHS 10; fractional points are discarded. For example, GHS 99.99 earns 9 base points.
* The member's tier rule is applied to the base points:

  | Tier | Calculation |
  | --- | --- |
  | `STANDARD` | 1x base points |
  | `SILVER` | 1.5x base points, truncated to a whole point |
  | `GOLD` | 2x base points |

* A flat 50-point bonus is added after the tier calculation only when the purchase is strictly greater than GHS 500. Exactly GHS 500 does not qualify.
* A zero-value purchase is accepted and earns 0 points.

## 3. Project Structure

```text
src/
├── main/java/com/brightline/loyalty/
│   ├── bonus/
│   │   ├── BonusPolicy.java
│   │   └── ThresholdBonusPolicy.java
│   ├── domain/
│   │   ├── Member.java
│   │   ├── MemberId.java
│   │   ├── Purchase.java
│   │   └── Tier.java
│   ├── exception/
│   │   ├── BrightlineLoyaltyException.java
│   │   ├── InvalidPurchaseException.java
│   │   ├── MemberNotFoundException.java
│   │   └── UnknownTierException.java
│   ├── repository/
│   │   ├── InMemoryMemberRepository.java
│   │   └── MemberRepository.java
│   ├── rules/
│   │   ├── GoldTierRule.java
│   │   ├── SilverTierRule.java
│   │   ├── StandardTierRule.java
│   │   ├── TierPointsRule.java
│   │   └── TierPointsRuleResolver.java
│   ├── service/
│   │   └── PointsCalculator.java
│   ├── Demo.java
│   └── LoyaltyServiceFactory.java
└── test/java/com/brightline/loyalty/
    ├── bonus/ThresholdBonusPolicyTest.java
    ├── domain/MemberTest.java
    ├── domain/MemberIdTest.java
    ├── domain/PurchaseTest.java
    ├── repository/InMemoryMemberRepositoryTest.java
    ├── rules/TierPointsRuleResolverTest.java
    ├── rules/TierPointsRuleTest.java
    ├── service/PointsCalculatorTest.java
    ├── DemoTest.java
    └── LoyaltyServiceFactoryTest.java
```

## 4. Calculation and Production Wiring

`PointsCalculator` looks up the member, resolves the rule for the member's tier, calculates base points from the purchase amount, applies the tier rule, and adds the bonus. It receives a `MemberRepository`, `TierPointsRuleResolver`, and `BonusPolicy` through its constructor.

`LoyaltyServiceFactory.productionRules()` creates the standard, silver, and gold rules. `createCalculator()` uses that list to build the resolver and wires it with `InMemoryMemberRepository` and `ThresholdBonusPolicy`. The in-memory repository contains `m1` (STANDARD), `m2` (GOLD), and `m3` (SILVER).

`MemberId` is the typed identifier used by `Member`, `Purchase`, and `MemberRepository`. The demo and in-memory repository construct IDs from strings at their boundaries; the calculation and lookup path uses `MemberId`. `Tier` contains only enum constants; it does not duplicate each constant's name in a label field.

## 5. Key Decisions and Why

| Problem recorded in `ANALYSIS.md` | Decision | Standard reference |
| --- | --- | --- |
| Tier logic was an `if/else if` chain with no final `else` | Use one `TierPointsRule` implementation per tier. `TierPointsRuleResolver` receives the rules and throws `UnknownTierException` when a tier has none; it never defaults to a multiplier. | §12.2 |
| A broad catch hid every failure as 0 | `PointsCalculator` catches arithmetic overflow only, wraps it with context, and preserves its cause. Other failures are not converted into a successful result. | §6.1, §6.3 |
| The calculator created its own repository | Inject the repository, rule resolver, and bonus policy. `LoyaltyServiceFactory` wires production implementations. | §12.5 |
| A purchase was never validated | `Purchase` validates its member ID and amount when constructed; `PointsCalculator` also rejects a null purchase. | No direct rule |
| The repository returned `null` for a missing member | `MemberRepository.findById` returns `Optional<Member>`. | No direct rule |
| One method handled lookup, calculation, output, and errors | Keep calculation, tier rules, bonus policy, and CLI presentation in separate responsibilities. | §12.1, §4.1, §4.2 |
| Tier and money used weak primitive types | Use `Tier`, `MemberId`, and `BigDecimal`; `Member` is a record that rejects null fields. | No direct rule |
| Business values were magic numbers | Use named constants for the base-point divisor, tier multipliers, threshold, and bonus. | §3.4 |
| Calculation printed directly to the console | Keep console output in `Demo`; the calculation service does not print. | §4.4 |

## 6. Adding a New Tier

To add a tier such as `PLATINUM`, `PointsCalculator`, `TierPointsRuleResolver`, and the existing tier-rule classes do not need to change:

1. Add the value to the `Tier` enum.
2. Implement `TierPointsRule` for the new tier.
3. Include the rule in `LoyaltyServiceFactory.productionRules()`.

If a tier is added to the enum without a rule, the resolver throws `UnknownTierException`. `LoyaltyServiceFactoryTest.everyTierHasARegisteredRule` checks each enum value against the production rule list.

## 7. Validation and Exception Handling

`BrightlineLoyaltyException` extends `RuntimeException`. Callers can catch it for any loyalty failure or catch a child exception for a specific failure. Wrapping code preserves the original cause.

| Failure | Exception | Raised in | Handling and test |
| --- | --- | --- | --- |
| Member ID not found | `MemberNotFoundException` | `PointsCalculator` | `Demo` displays the message; tested in `PointsCalculatorTest` and `DemoTest`. |
| Tier has no registered rule | `UnknownTierException` | `TierPointsRuleResolver` | Propagates to the caller; tested in `TierPointsRuleResolverTest` and `PointsCalculatorTest`. |
| Null purchase, null or negative amount, or null member ID in a purchase | `InvalidPurchaseException` | `Purchase`, `PointsCalculator` | `Demo` displays the message; tested in `PurchaseTest`, `PointsCalculatorTest`, and `DemoTest`. |
| Point calculation overflows an `int` | `ArithmeticException` wrapped in `InvalidPurchaseException`, with cause preserved | `PointsCalculator` | `Demo` handles it as a loyalty failure; tested in `PointsCalculatorTest` and `TierPointsRuleTest`. |
| Amount text is not a number | `NumberFormatException` wrapped in `InvalidPurchaseException`, with cause preserved | `Demo.parseAmount` | Displayed as an expected input error; tested in `DemoTest`. |
| Amount text is null or blank | `InvalidPurchaseException` | `Demo.parseAmount` | Displayed as an expected input error; tested in `DemoTest`. |
| Member ID value is null or blank | `IllegalArgumentException` | `MemberId` | Tested in `MemberIdTest`. |
| Member ID, name, or tier is null | `NullPointerException` | `Member` | Tested in `MemberTest`. |
| Two rules support the same tier | `IllegalStateException` | `TierPointsRuleResolver` | Tested in `TierPointsRuleResolverTest`. |
| Null tier passed to the resolver | `NullPointerException` | `TierPointsRuleResolver` | Tested in `TierPointsRuleResolverTest`. |
| Unexpected runtime failure while processing an example | Original `RuntimeException` | Calculation or its dependencies | `Demo` logs it with `java.util.logging`, including context and cause, and displays a controlled message; tested in `DemoTest`. |

`Demo` handles expected domain and input failures per example, so the remaining examples continue. Unexpected runtime failures are logged and shown as failures, not converted to points. `Demo.main` also has top-level handlers for failures during application wiring or startup.

## 8. Tests

The test suite uses JUnit 5 and Mockito. `PointsCalculatorTest` injects a Mockito mock of `MemberRepository` into the real calculator, tier rules, and bonus policy. Tests assert results, exception types, messages, or causes.

| Test class | Test cases | Coverage |
| --- | ---: | --- |
| `PointsCalculatorTest` | 17 | Nine tier-and-amount results, zero amount, null purchase, unknown member, missing tier rule, and four overflow cases with cause preservation. |
| `TierPointsRuleTest` | 14 | Each rule's tier and calculations for multiple base-point values; Silver and Gold overflow. |
| `DemoTest` | 11 | Demo output, amount parsing and parse cause, null and blank amounts, null member ID, and unexpected-failure logging with a controlled message. |
| `ThresholdBonusPolicyTest` | 5 | Bonus behavior at and around the GHS 500 threshold. |
| `MemberIdTest` | 5 | Retains a valid ID and rejects null and blank values. |
| `PurchaseTest` | 5 | Valid and zero purchases; null member ID, null amount, and negative amount. |
| `MemberTest` | 4 | Retains member fields and rejects null ID, name, or tier. |
| `InMemoryMemberRepositoryTest` | 4 | Finds the three seeded members; returns an empty `Optional` for an unknown ID. |
| `TierPointsRuleResolverTest` | 4 | Resolves a registered rule and rejects an unknown tier, duplicate rule, or null tier. |
| `LoyaltyServiceFactoryTest` | 4 | Registers a rule for every enum tier and creates a calculator that awards points to a seeded member. |

**Latest verification.** `mvn clean verify` with JDK 25.0.5 and Maven 3.9.15 recorded 73 tests, 0 failures, 0 errors, and 0 skipped.

**Coverage.** JaCoCo wrote the report to `target/site/jacoco/index.html`. The run recorded 614/636 instructions (96.54%), 139/144 lines (96.53%), and 24/24 branches (100%). The uncovered instructions are in the two top-level catch handlers in `Demo.main`; per-example expected and unexpected failures are tested.

**Known test omission.** Tests do not directly assert the `Objects.requireNonNull` constructor guards in `PointsCalculator` or `TierPointsRuleResolver`.

## 9. The Improvement Beyond the Brief: Parameterized Tests

Ten parameterized test methods use `@MethodSource`, `@CsvSource`, `@EnumSource`, `@ValueSource`, and `@NullAndEmptySource`. The tier rules and bonus policy are input/output tables, so the same test body checks multiple cases.

The nine rows in `PointsCalculatorTest` cover tier calculations, truncation, and the bonus boundary:

| Tier | Amount | Expected points |
| --- | ---: | ---: |
| STANDARD | 99.99 | 9 |
| STANDARD | 500 | 50 |
| STANDARD | 500.01 | 100 |
| SILVER | 10 | 1 |
| SILVER | 50 | 7 |
| SILVER | 200 | 30 |
| GOLD | 250 | 50 |
| GOLD | 500 | 100 |
| GOLD | 500.01 | 150 |

The suite also checks four overflow inputs at different calculation steps (base points, Silver result, Gold result, and bonus addition) with one parameterized test body. Parameterized tests make it easier to add cases, but do not prove that the suite detects every possible defect.

## 10. Requirements and Commands

The Maven compiler is configured for Java 25 using `maven.compiler.release` in `pom.xml`. The verified environment used JDK 25.0.5 and Maven 3.9.15. JUnit 5 and Mockito are test-scoped dependencies; JaCoCo generates coverage, and the exec plugin runs the demo.

Run tests and generate the coverage report:

```shell
mvn clean verify
```

Run the demo:

```shell
mvn compile exec:java
```

The exec plugin is configured to run `com.brightline.loyalty.Demo`. The verified demo output is:

```text
Result m1, GHS 100 -> 10 points
Result m2, GHS 250 -> 50 points
Result m3, GHS 200 -> 30 points
Result unknown, GHS 100 -> Member not found: unknown
Result m1, GHS -50 -> Purchase amount must not be negative: -50
Result m1, GHS abc -> Purchase amount is not a number: abc
```

## 11. What Was Kept, Fixed, or Rewritten

| Item | Decision | Reason |
| --- | --- | --- |
| Points formula: one point per GHS 10, tier multipliers, and bonus above GHS 500 | Kept | Preserves the observed behavior, including truncation. |
| `Member` | Rewritten as a record with `MemberId` and `Tier` | Replaces weak raw identifier and tier types. |
| `MemberRepository` | Fixed | Returns `Optional<Member>` and accepts `MemberId`. |
| `InMemoryMemberRepository` | Fixed | Keeps the three seeded members and returns an `Optional`. |
| `PointsCalculator` | Rewritten | Coordinates injected dependencies without printing or swallowing failures. |
| `Demo` | Rewritten | Owns CLI output, input parsing, and boundary error handling. |
| Three inherited tests | Rewritten | As documented in `ANALYSIS.md`, none asserted a correct calculated value. |
| `Tier`, `MemberId`, `Purchase`, exception types, tier rules, bonus policy, and `LoyaltyServiceFactory` | Added | Provide typed domain data, validation, extensible rules, and application wiring. |

## 12. Decisions Not Settled by `ANALYSIS.md`

* The base-points formula is implemented in `PointsCalculator` with the named constant `GHS_PER_POINT` and a private method; it is not a separate class.
* `PointsCalculator` catches `ArithmeticException` to report overflow as `InvalidPurchaseException` while retaining the cause.
* `MemberId` rejects null or blank values with `IllegalArgumentException`, outside the `BrightlineLoyaltyException` hierarchy.
* `Demo.parseAmount` parses text at the CLI boundary, so `Purchase` receives a `BigDecimal`.
* `Demo` logs unexpected per-example failures at `SEVERE` and continues to the next example.
* The `STANDARD` rule applies a multiplier of 1, so every tier uses the same rule interface.
