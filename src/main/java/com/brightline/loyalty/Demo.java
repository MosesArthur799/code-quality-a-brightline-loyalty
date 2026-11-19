package com.brightline.loyalty;

import com.brightline.loyalty.domain.MemberId;
import com.brightline.loyalty.domain.Purchase;
import com.brightline.loyalty.exception.BrightlineLoyaltyException;
import com.brightline.loyalty.exception.InvalidPurchaseException;
import com.brightline.loyalty.service.PointsCalculator;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Demo {

    private static final Logger LOGGER = Logger.getLogger(Demo.class.getName());
    private static final MemberId STANDARD_MEMBER_ID = new MemberId("m1");
    private static final MemberId GOLD_MEMBER_ID = new MemberId("m2");
    private static final MemberId SILVER_MEMBER_ID = new MemberId("m3");
    private static final MemberId UNKNOWN_MEMBER_ID = new MemberId("unknown");
    private static final BigDecimal STANDARD_PURCHASE = new BigDecimal("100");
    private static final BigDecimal GOLD_PURCHASE = new BigDecimal("250");
    private static final BigDecimal SILVER_PURCHASE = new BigDecimal("200");
    private static final BigDecimal INVALID_PURCHASE = new BigDecimal("-50");
    private static final String MALFORMED_AMOUNT = "abc";

    private final PointsCalculator calculator;
    private final PrintWriter output;

    Demo(PointsCalculator calculator, PrintWriter output) {
        this.calculator = calculator;
        this.output = output;
    }

    public static void main(String[] args) {
        PrintWriter output = new PrintWriter(System.out, true, StandardCharsets.UTF_8);
        PrintWriter errorOutput = new PrintWriter(System.err, true, StandardCharsets.UTF_8);
        try {
            new Demo(new LoyaltyServiceFactory().createCalculator(), output).runExamples();
        } catch (BrightlineLoyaltyException expectedException) {
            errorOutput.printf("Demo could not complete: %s%n", expectedException.getMessage());
        } catch (RuntimeException unexpectedException) {
            LOGGER.log(
                    Level.SEVERE, "Unexpected failure during demo execution", unexpectedException);
            errorOutput.println("Demo could not complete because of an unexpected system error.");
        }
    }

    private void runExamples() {
        show(STANDARD_MEMBER_ID, STANDARD_PURCHASE);
        show(GOLD_MEMBER_ID, GOLD_PURCHASE);
        show(SILVER_MEMBER_ID, SILVER_PURCHASE);
        show(UNKNOWN_MEMBER_ID, STANDARD_PURCHASE);
        show(STANDARD_MEMBER_ID, INVALID_PURCHASE);
        showRawAmount(STANDARD_MEMBER_ID, MALFORMED_AMOUNT);
    }

    void show(MemberId memberId, BigDecimal amount) {
        String displayAmount = amount == null ? "<missing>" : amount.toPlainString();
        show(memberId, displayAmount, () -> new Purchase(memberId, amount));
    }

    void showRawAmount(MemberId memberId, String amount) {
        show(memberId, amount, () -> new Purchase(memberId, parseAmount(amount)));
    }

    private void show(MemberId memberId, String amount, Supplier<Purchase> purchaseSupplier) {
        String displayMemberId = memberId == null ? "<missing>" : memberId.value();
        String outcome;
        try {
            outcome = calculator.calculatePoints(purchaseSupplier.get()) + " points";
        } catch (BrightlineLoyaltyException loyaltyException) {
            outcome = loyaltyException.getMessage();
        } catch (RuntimeException unexpectedException) {
            LOGGER.log(
                    Level.SEVERE,
                    "Unexpected failure processing member " + displayMemberId
                            + " purchase amount GHS " + amount,
                    unexpectedException);
            outcome = "Unable to process this purchase right now.";
        }
        output.printf("Result %s, GHS %s -> %s%n", displayMemberId, amount, outcome);
    }

    static BigDecimal parseAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            throw new InvalidPurchaseException("Purchase amount must not be blank");
        }
        try {
            return new BigDecimal(amount);
        } catch (NumberFormatException numberFormatException) {
            throw new InvalidPurchaseException(
                    "Purchase amount is not a number: " + amount, numberFormatException);
        }
    }
}
