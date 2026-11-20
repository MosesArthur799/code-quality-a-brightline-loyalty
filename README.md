
# Brightline Loyalty Service
 
A Java service that calculates loyalty points from a member's tier and purchase amount. It uses an in-memory member repository and includes a command-line demo. It is a refactoring of an AI-generated starter that compiled and passed its tests but hid failures, hard-wired its dependencies and could not be extended safely.
 
> **Pre-coding analysis:** [ANALYSIS.md](ANALYSIS.md) was written before any code. It records what was wrong with the inherited code and which of its three tests proved nothing. It is left unchanged. This README describes what was built and why.
 
---
 
## 1. Deliverables Map
 
| # | Brief deliverable | Where it is |
| --- | --- | --- |
| 1 | Analysis of the inherited code and tests | [ANALYSIS.md](ANALYSIS.md), sections 2 and 3 |
| 2 | Refactored code (one job per class, injected dependencies, new tier fits) | `src/main/java/com/brightline/loyalty/`, sections 4 to 6 below |
| 3 | Exception handling and custom exceptions | `exception/` package, section 7 below |
| 4 | JUnit 5 and Mockito test suite | `src/test/java/com/brightline/loyalty/`, section 8 below |
| 5 | Rationale: decisions, new tier, kept / fixed / rewritten | Sections 5, 6 and 11 below |
| 6 | One improvement beyond the brief | Parameterized tests, section 9 below |
 
## 2. Business Rules
 
* Base points are one point per complete GHS 10; fractional points are discarded. For example, GHS 99.99 earns 9 base points.
* The member's tier rule is applied to base points:
  | Tier | Calculation |
  | --- | --- |
  | `STANDARD` | 1x base points |
  | `SILVER` | 1.5x base points, rounded down |
  | `GOLD` | 2x base points |
* A purchase strictly greater than GHS 500 receives a flat 50-point bonus after the tier calculation. Exactly GHS 500 does not qualify.
* A zero-value purchase is accepted and earns 0 points.
These are the behaviours of the inherited code (listed in ANALYSIS.md under "Behaviours the refactor must preserve"). The inherited code had no specification, so they were kept as found.
 
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
 
## 4. How the Calculation Works
 
`PointsCalculator` looks up the member, resolves the rule for the member's tier, calculates base points from the purchase amount, applies the tier rule, and adds the bonus. It receives a `MemberRepository`, a `TierPointsRuleResolver` and a `BonusPolicy` through its constructor.
 
`LoyaltyServiceFactory.productionRules()` creates the standard, silver and gold rules. `createCalculator()` uses that list to build the resolver and wires it with `InMemoryMemberRepository` and `ThresholdBonusPolicy`. The in-memory repository contains members `m1` (STANDARD), `m2` (GOLD) and `m3` (SILVER).
 
`MemberId` is the value type for member identifiers in `Member`, `Purchase` and `MemberRepository`. Raw strings are created into `MemberId` only at the boundaries (the demo and the in-memory repository).
 
## 5. Key Decisions and Why
 
Each decision answers a problem recorded in ANALYSIS.md.
 
| ANALYSIS.md problem | Decision | Standard |
| --- | --- | --- |
| Defect 1: tier rules in an `if/else if` chain with no final `else` | One `TierPointsRule` class per tier. `TierPointsRuleResolver` receives the list from outside and throws `UnknownTierException` when a tier has no rule. It never falls back to a default multiplier. | §12.2 |
| Defect 2: `catch (Exception e) { return 0; }` hid every failure | No catch-all. `PointsCalculator` catches only `ArithmeticException` and rethrows it with context. Other failures reach the caller. | §6.1, §6.3 |
| Defect 3: the calculator built its own repository | Repository, resolver and bonus policy are injected through the constructor. `LoyaltyServiceFactory` is the one place that wires production objects. | §12.5 |
| Defect 4: purchase never validated | `Purchase` validates itself when it is created. The amount is a `BigDecimal`. `PointsCalculator` rejects a null purchase. | none directly in the standard |
| Defect 5: `null` returned for a missing member | `MemberRepository.findById` returns `Optional<Member>`. | none directly in the standard |
| Defect 6: one method doing six jobs | Lookup, tier rule, bonus and output each have their own class. `calculatePoints` only coordinates. | §12.1, §4.1, §4.2 |
| Defect 7: `String` tier and `double` money | `Tier` enum, `BigDecimal` amounts, `MemberId` value type, and records that reject null fields. | none directly in the standard |
| Defect 8: magic numbers | Named constants in each class that needs one. | §3.4 |
| Defect 9: `System.out.println` inside the calculation | The calculation prints nothing. Only `Demo` writes output. | §4.4 |
 
## 6. Adding a New Tier
 
To add a tier such as `PLATINUM`, `PointsCalculator`, `TierPointsRuleResolver` and the existing tier classes need no edit:
 
1. Add the value to the `Tier` enum.
2. Implement `TierPointsRule` for it in a new class.
3. Include that rule in `LoyaltyServiceFactory.productionRules()`.
If a tier is added to the enum without a rule, the resolver throws `UnknownTierException`, and `LoyaltyServiceFactoryTest.everyTierHasARegisteredRule` fails because it runs once for every `Tier` value against the production rule list.
 
## 7. Validation and Exception Handling
 
`BrightlineLoyaltyException` extends `RuntimeException`. A caller can catch it for any loyalty failure, or catch one of its children for a specific one. Where one exception wraps another, the original is kept as the cause.
 
| Failure | Exception | Raised in | Handled in | Tested in |
| --- | --- | --- | --- | --- |
| Member id not found | `MemberNotFoundException` | `PointsCalculator` | `Demo` (prints the message) | `PointsCalculatorTest`, `DemoTest` |
| Tier with no registered rule | `UnknownTierException` | `TierPointsRuleResolver` | caller | `TierPointsRuleResolverTest`, `PointsCalculatorTest` |
| Null purchase, null or negative amount, null member id in a purchase | `InvalidPurchaseException` | `Purchase`, `PointsCalculator` | `Demo` (prints the message) | `PurchaseTest`, `PointsCalculatorTest`, `DemoTest` |
| Amount too large to award points (an `int` overflows) | `ArithmeticException` wrapped in `InvalidPurchaseException`, cause kept | `PointsCalculator` (the only `catch` in the service) | `Demo` | `PointsCalculatorTest`, `TierPointsRuleTest` |
| Amount text is not a number | `NumberFormatException` wrapped in `InvalidPurchaseException`, cause kept | `Demo.parseAmount` | `Demo` | `DemoTest` |
| Amount text is null or blank | `InvalidPurchaseException` | `Demo.parseAmount` | `Demo` | `DemoTest` |
| Null or blank member id value | `IllegalArgumentException` | `MemberId` | caller | `MemberIdTest` |
| Null member id, name or tier | `NullPointerException` | `Member` | caller | `MemberTest` |
| Two rules for the same tier | `IllegalStateException` | `TierPointsRuleResolver` | caller | `TierPointsRuleResolverTest` |
| Null tier passed to the resolver | `NullPointerException` | `TierPointsRuleResolver` | caller | `TierPointsRuleResolverTest` |
| Unexpected `RuntimeException` while processing an example | unchanged | anywhere | `Demo` logs it at `SEVERE` with its cause and prints a controlled message | `DemoTest` |
 
`Demo` is the outer boundary. It catches `BrightlineLoyaltyException` for each example and prints the message. It also catches an unexpected `RuntimeException` for each example, logs it with `java.util.logging` including the cause, and prints "Unable to process this purchase right now." The failure is shown as a failure, never as a success. It is not hidden.
 
## 8. Tests
 
The suite uses JUnit 5 and Mockito. `PointsCalculatorTest` injects a Mockito mock of `MemberRepository` into the real calculator, real rules and the real bonus policy. Each test asserts a calculated value, an exact exception type, a message or a cause, not only that a mock was called.
 
| Test class | Tests | What it proves |
| --- | --- | --- |
| `PointsCalculatorTest` | 17 | The nine-row points table, zero amount, null purchase, unknown member, tier with no rule, and four overflow cases with the cause kept. |
| `TierPointsRuleTest` | 14 | Each rule declares its tier and gives the right result for several base-point values. Silver and gold fail loudly on overflow. |
| `DemoTest` | 11 | The demo's printed output, amount parsing, the cause of a parse failure, null and blank amounts, null member id, and a logged unexpected failure with a controlled message. |
| `ThresholdBonusPolicyTest` | 5 | The bonus applies only strictly above GHS 500. |
| `MemberIdTest` | 5 | Null and blank ids are rejected. |
| `PurchaseTest` | 5 | Valid, zero, null member id, null amount and negative amount. |
| `MemberTest` | 4 | Values are kept. Null id, name and tier are rejected. |
| `InMemoryMemberRepositoryTest` | 4 | The three seeded members are found. An unknown id gives an empty `Optional`. |
| `TierPointsRuleResolverTest` | 4 | The registered rule is returned. An unknown tier, a duplicate rule and a null tier are rejected. |
| `LoyaltyServiceFactoryTest` | 4 | Every `Tier` has a production rule. The production calculator awards points to a seeded member. |
 
**Results.** The last saved Maven run (JDK 25.0.5, 2026-11-19) recorded 73 tests with 0 failures, 0 errors and 0 skipped.
 
**Coverage.** The JaCoCo report saved by that run recorded 614 of 636 instructions (96.54%), 144 of 149 lines and 24 of 24 branches. The only uncovered code is the two catch handlers in `Demo.main`. The standard (§9) asks for 100% coverage of statements and branches, so this is a known gap.
 
**Not asserted by any test:** the `Objects.requireNonNull` checks in the constructors of `PointsCalculator` and `TierPointsRuleResolver`.
 
## 9. The One Improvement Beyond the Brief: Parameterized Tests
 
Ten test methods are parameterized, using `@MethodSource`, `@CsvSource`, `@EnumSource` and `@ValueSource`. The tier rules and the bonus policy are tables of input to output, so one test body runs against many rows.
 
* **Boundaries and truncation are where this code is most likely to break.** These rows (`PointsCalculatorTest`) pin the behaviour:
  | Tier | Amount | Expected points |
  | --- | --- | --- |
  | STANDARD | 99.99 | 9 |
  | STANDARD | 500 | 50 |
  | STANDARD | 500.01 | 100 |
  | SILVER | 10 | 1 |
  | SILVER | 50 | 7 |
  | SILVER | 200 | 30 |
  | GOLD | 250 | 50 |
  | GOLD | 500 | 100 |
  | GOLD | 500.01 | 150 |
  A single-input test can pass by coincidence. A table with boundary rows cannot.
* **A new tier costs one set of table rows, not a new test method.**
* **A guard against forgetting a rule.** `@EnumSource(Tier.class)` runs once per tier and asserts that the production wiring has a rule for it.
* **The overflow cases are rows too.** Four amounts that overflow at different steps (base points, the Silver result, the Gold result and the bonus) are checked by one test body.
What it does not do: parameterized tests show that more cases pass. They do not prove the suite would catch every bug.
 
## 10. Requirements and Commands
 
The Maven compiler is configured for Java 25 (`maven.compiler.release` in `pom.xml`). The saved test run used JDK 25.0.5.
 
Run the tests and generate the coverage report (`target/site/jacoco/index.html`):
 
```shell
mvn clean verify
```
 
Run the demo:
 
```shell
mvn compile exec:java
```
 
The exec plugin is configured to run `com.brightline.loyalty.Demo`. Its output is:
 
```text
Result m1, GHS 100 -> 10 points
Result m2, GHS 250 -> 50 points
Result m3, GHS 200 -> 30 points
Result unknown, GHS 100 -> Member not found: unknown
Result m1, GHS -50 -> Purchase amount must not be negative: -50
Result m1, GHS abc -> Purchase amount is not a number: abc
```
 
## 11. What Was Kept, Fixed or Rewritten
 
| Item | Decision | Reason |
| --- | --- | --- |
| Points formula (1 per GHS 10, tier multipliers, bonus above 500) | Kept | Behaviour preserved, including truncation. |
| `Member` | Rewritten as a record with a `MemberId` and a `Tier` | The fields were right, but the `String` tier and the raw id were weak. |
| `MemberRepository` | Fixed | Returns `Optional<Member>` and takes a `MemberId`. |
| `InMemoryMemberRepository` | Fixed | Same three members, looked up through `Optional.ofNullable(...)`. |
| `PointsCalculator` | Rewritten | Six jobs split into separate classes. |
| `Demo` | Rewritten | Writes through an injected `PrintWriter`, shows the failure paths and handles its own errors. |
| All three original tests | Rewritten | None of them asserted a correct value. Replaced by the suite in section 8. |
| `Tier`, `MemberId`, `Purchase`, the exceptions, the rules, the bonus policy, `LoyaltyServiceFactory` | New | Each answers a problem listed in section 5. |
 
## 12. Decisions Not Settled by ANALYSIS.md
 
* The base-points formula (one point per GHS 10) lives in `PointsCalculator` as a named constant and one private method, not in a separate class.
* `PointsCalculator` has one `catch`, for `ArithmeticException`, to turn an overflow into `InvalidPurchaseException` with context.
* An overflow is reported as `InvalidPurchaseException`, not a separate exception type, because an amount too large to award points for is an input the service cannot honour. The caller handles it like any other invalid purchase, and the original `ArithmeticException` is kept as the cause.
* `MemberId` rejects null or blank values with `IllegalArgumentException`, which is outside the `BrightlineLoyaltyException` hierarchy.
* `Demo` parses text amounts itself (`Demo.parseAmount`), so `Purchase` only ever receives a `BigDecimal`.
* `Demo` logs unexpected failures at `SEVERE` and continues with the next example, as described in section 7.
* The `STANDARD` rule applies a multiplier of 1, so every tier goes through the same rule interface.
 
 
