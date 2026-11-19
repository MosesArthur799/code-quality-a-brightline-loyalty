# Brightline Loyalty Service: Analysis (Pre-Coding Check-In)

Status:Analysis only. No refactoring has been done yet.

---

## 1. What Was Inherited

A small loyalty points service generated quickly with an AI assistant. Six files, five production files and one test file:

- `PointsCalculator.java`: calculates points for a purchase
- `Member.java`: holds member data (id, name, tier as a `String`)
- `MemberRepository.java`: interface with `Member findById(String id)`
- `InMemoryMemberRepository.java`: three hardcoded members in a `HashMap`
- `Demo.java`: prints three results
- `PointsCalculatorTest.java`: three tests

Running the original demo prints:

```
Awarded 10 points to Kofi Mensah
Result m1 (STANDARD), GHS 100  -> 10
Awarded 50 points to Ama Owusu
Result m2 (GOLD), GHS 250      -> 50
Result unknown member, GHS 100 -> 0
```

Running the original tests prints `Tests run: 3, Failures: 0, Errors: 0` and `BUILD SUCCESS`.

Everything compiles and everything is green, but the suite would stay green with the bugs below still present. The demo only prints and asserts nothing, so it is not a safety net either.

---

## 2. What Is Wrong With the Inherited Code

Ranked from most to least serious. The reason for each position is given.

| Rank | Problem | Principle or standard broken |
|---|---|---|
| 1 | Tier rules hardcoded in an `if/else if` chain with no final `else` | OCP (§12.2) |
| 2 | Every failure swallowed by `catch (Exception e) { return 0; }` | AmaliTech §6.1 |
| 3 | Calculator builds its own repository | DIP (§12.5) |
| 4 | Purchase is never validated | Missing input checks (no direct rule in the standard) |
| 5 | Repository returns `null` for a missing member | Null as a hidden contract (no direct rule in the standard) |
| 6 | One method does six jobs and is 28 lines long | SRP (§12.1), §4.1, §4.2 |
| 7 | Primitive obsession: `String` tier, `double` money | Weak types (no direct rule in the standard) |
| 8 | Magic numbers | §3.4 |
| 9 | `System.out.println` inside the calculation | §4.4 |

### Defect 1 (Most Serious): Tier rules hardcoded, and an unknown tier is silently ignored

**Location:** `PointsCalculator.java`, the tier chain inside `calculatePoints`.

```java
if (member.getTier().equals("STANDARD")) {
    points = points * 1;
} else if (member.getTier().equals("SILVER")) {
    points = (int) (points * 1.5);
} else if (member.getTier().equals("GOLD")) {
    points = points * 2;
}
```

**Root cause:** the tier rules live inside the method that coordinates the calculation. There is no interface and no class per tier. There is also no final `else`, so a tier that matches no branch (`"PLATINUM"`, or a typo such as `"Gold"`) gets no multiplier and no error. The member quietly receives base points. The `points * 1` branch does nothing at all, which is dead code.

**Business risk:** Brightline has said a new tier is coming. Adding it means editing this chain. If the developer forgets the new branch, nothing fails: the new tier simply earns the wrong points. The structure makes the scheduled change dangerous, and no test can currently reach the unknown-tier path because the three members are fixed.

**Why this is ranked first:** the change is already scheduled, and this is exactly where it would go wrong silently. Defect 2 is a close second and arguably worse today, because it corrupts results now. It ranks below Defect 1 only because Defect 1 is the one the business has already scheduled.

**Violated:** Open/Closed Principle, AmaliTech §12.2. The calculator must be modified to extend its behaviour.

### Defect 2: Every failure is silently hidden

**Location:** `PointsCalculator.java`, the catch block.

```java
} catch (Exception e) {
    // something went wrong (for example the member was not found) - award nothing
    return 0;
}
```

**Root cause:** one broad catch turns every error into 0, as if the calculation succeeded. The comment shows this was deliberate.

**Business risk:** three different failures all become 0:

- an unknown member (`findById` returns `null`, then `member.getTier()` throws a `NullPointerException`)
- a member whose tier is `null` (the same `NullPointerException`)
- any bug anywhere in the method

The caller cannot tell a genuine 0 from a failure. The demo confirms it: `Result unknown member, GHS 100 -> 0`, with no message. This is also why hollow tests can pass: a wrong or exploding result becomes a plain 0.

**Violated:** AmaliTech §6.1, do not ignore caught exceptions.

### Defect 3: The calculator creates its own repository

**Location:** `PointsCalculator.java`, line 7.

```java
private final MemberRepository repository = new InMemoryMemberRepository();
```

**Root cause:** the high-level class instantiates its low-level dependency. The class does not take one from outside, and the repository fills itself with hardcoded data in its constructor.

**Business risk:** a test cannot supply a fake repository or its own members, so every test of the calculator also runs the real repository. A bug in one hides behind the other, and failures that need a controlled repository (a member with a `null` tier, a tier with no rule, a repository that throws) cannot be set up at all.

**Violated:** Dependency Inversion Principle, AmaliTech §12.5.

### Defect 4: The purchase is never validated (malformed purchases)

**Location:** `calculatePoints(String memberId, double amount)`.

**Root cause:** nothing checks `memberId` or `amount`.

**Business risk:**

- A negative amount (`-50`) returns `-5` points and prints "Awarded -5 points". No failure occurs.
- `Double.POSITIVE_INFINITY` becomes `Integer.MAX_VALUE`, and the bonus addition then overflows to a negative number.
- `Double.NaN` silently returns `0`.
- A null or blank `memberId` ends up in the swallowed-failure path from Defect 2.

### Defect 5: The repository returns `null` for a missing member

**Location:** `InMemoryMemberRepository.java` and `MemberRepository.java`.

```java
return members.get(id); // returns null when the id is not found
```

**Root cause:** the interface declares `Member`. The implementation sometimes returns `null`. Nothing in the type system or any Javadoc warns the caller. The comment acknowledges the problem and does nothing about it.

**Business risk:** any caller that forgets a `null` check gets a `NullPointerException`. In this code that exception is then hidden by Defect 2.

### Defect 6: One method does six jobs and is too long

**Location:** `PointsCalculator.calculatePoints`, about 28 lines including comments and blank lines (the standard says rarely over 20).

The single method fetches the member, calculates base points, applies the tier multiplier, applies the bonus, prints to the console and handles errors.

**Business risk:** the class has six reasons to change. A change to the bonus rule risks breaking the tier logic, and a change to error handling risks breaking the calculation.

**Violated:** Single Responsibility Principle, AmaliTech §12.1, §4.1 and §4.2.

### Defect 7: Primitive obsession (`String` tier, `double` money)

**Locations:** `Member.java` (the tier field, documented only by the comment `// "STANDARD", "SILVER", "GOLD"`) and `calculatePoints(..., double amount)`.

**Root cause:** a comment is doing the job of a type. A `String` accepts any text, so a typo compiles and then falls through (see Defect 1). `Member` also accepts `null` for any field. `double` cannot represent every decimal exactly, so an amount built from other values can land just below a boundary (just under 10.0, for example) and truncate to fewer points than expected.

**Business risk:** invalid tiers and imprecise money are possible by construction, and the compiler cannot help.

### Defect 8: Magic numbers

**Location:** `PointsCalculator.java`.

The literals `10`, `1`, `1.5`, `2`, `500` and `50` have no names. If the bonus threshold changes from 500 to 750, a developer must search for the number and hope to find every occurrence.

**Violated:** AmaliTech §3.4.

### Defect 9: `System.out.println` inside the calculation

**Location:** `PointsCalculator.java`.

```java
System.out.println("Awarded " + points + " points to " + member.getName());
```

**Root cause:** console output is a hidden side effect in a method that claims only to calculate and return points.

**Business risk:** in production this prints to nobody, and tests cannot check it. The method is impure.

**Violated:** AmaliTech §4.4.

### Behaviours the refactor must preserve

These are visible in the current code. There is no specification in the repository to confirm they are intended, so they are treated as the current behaviour:

- Base points are one per GHS 10, truncated (`(int) (amount / 10)`).
- The Silver multiplier of 1.5 is applied to the base points and truncated again.
- The bonus is a flat 50 added after the multiplier, only when the amount is strictly greater than 500.

---

## 3. Which AI-Generated Tests Pass Without Testing Anything

Ranked from most to least misleading.

### Rank 1: `calculatesGoldPoints`

```java
PointsCalculator calculator = mock(PointsCalculator.class);
when(calculator.calculatePoints("m2", 250.0)).thenReturn(50);
assertEquals(50, calculator.calculatePoints("m2", 250.0));
```

**Why it tests nothing:** it mocks the class being tested. The real `calculatePoints` never runs, and neither does the constructor. The test tells a mock to return 50 and then asserts that it returned 50. It only proves Mockito works. It is the most misleading because it looks like it checks a specific correct value (50 points for a Gold member spending 250).

### Rank 2: `awardsPointsForAPurchase`

```java
int points = calculator.calculatePoints("m3", 200.0);
assertNotNull(points); // a result came back
```

**Why it tests nothing:** `points` is a primitive `int`. It is boxed to an `Integer` for the call, and a boxed primitive can never be `null`, so this assertion can never fail. It passes if the method returns the wrong number, and it passes whenever Defect 2 turns a failure into 0. The correct value for m3 (SILVER, GHS 200) is 30, and the test never checks it.

### Rank 3: `awardsPointsToAMember`

```java
int points = calculator.calculatePoints("m1", 100.0);
assertTrue(points > 0); // it awarded some points
```

**Why it is weak:** it passes for any positive number. If the formula returned 1 instead of 10, it would still pass. It is the least bad of the three because it does fail if the result is 0, but it never asserts the correct value (10).

### Missing tests (a separate problem from the three above)

Beyond the three hollow tests, nothing tests:

- the bonus boundary (exactly 500 versus just above it)
- the Silver truncation
- an unknown member
- an unknown or `null` tier
- a negative, zero, NaN or infinite amount
- any failure at all

All three real-code tests also run the real repository (Defect 3), so none of them is isolated.

---