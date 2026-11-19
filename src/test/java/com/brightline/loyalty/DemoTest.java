package com.brightline.loyalty;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.brightline.loyalty.bonus.ThresholdBonusPolicy;
import com.brightline.loyalty.domain.MemberId;
import com.brightline.loyalty.exception.InvalidPurchaseException;
import com.brightline.loyalty.repository.MemberRepository;
import com.brightline.loyalty.rules.StandardTierRule;
import com.brightline.loyalty.rules.TierPointsRuleResolver;
import com.brightline.loyalty.service.PointsCalculator;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DemoTest {

    @Test
    @ResourceLock(Resources.SYSTEM_OUT)
    void reportsSuccessfulAndRejectedPurchasesWithoutStopping() {
        PrintStream originalOutput = System.out;
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(capturedOutput, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            Demo.main(new String[0]);
        } finally {
            System.setOut(originalOutput);
        }

        String result = capturedOutput.toString(StandardCharsets.UTF_8);
        assertAll(
                () -> assertTrue(result.contains("Result m1, GHS 100 -> 10 points")),
                () -> assertTrue(result.contains(
                        "Result unknown, GHS 100 -> Member not found: unknown")),
                () -> assertTrue(result.contains(
                        "Result m1, GHS -50 -> Purchase amount must not be negative: -50")),
                () -> assertTrue(result.contains(
                        "Result m1, GHS abc -> Purchase amount is not a number: abc")));
    }

    @Test
    void parsesAValidAmountAtTheDemoBoundary() {
        assertEquals(new BigDecimal("100.50"), Demo.parseAmount("100.50"),
                "The parsed amount should preserve the supplied decimal value");
    }

    @Test
    void processesAValidRawAmountThroughTheDemoBoundary() {
        StringWriter capturedOutput = new StringWriter();
        Demo demo = new Demo(
                new LoyaltyServiceFactory().createCalculator(),
                new PrintWriter(capturedOutput, true));

        demo.showRawAmount(new MemberId("m1"), "100");

        assertTrue(capturedOutput.toString().contains("Result m1, GHS 100 -> 10 points"),
                () -> "Expected a successful result for a valid amount, output was: "
                        + capturedOutput);
    }

    @Test
    void preservesTheCauseWhenAnAmountCannotBeParsed() {
        InvalidPurchaseException failure =
                assertThrows(InvalidPurchaseException.class, () -> Demo.parseAmount("abc"));

        assertInstanceOf(NumberFormatException.class, failure.getCause());
    }

    @ParameterizedTest(name = "rejects null or blank purchase amount: {0}")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsNullOrBlankAmounts(String amount) {
        InvalidPurchaseException failure =
                assertThrows(InvalidPurchaseException.class, () -> Demo.parseAmount(amount));

        assertEquals("Purchase amount must not be blank", failure.getMessage(),
                () -> "Rejected amount: " + amount);
    }

    @Test
    void reportsANullAmountAsAnExpectedInputError() {
        StringWriter capturedOutput = new StringWriter();
        Demo demo = new Demo(
                new LoyaltyServiceFactory().createCalculator(),
                new PrintWriter(capturedOutput, true));

        demo.show(new MemberId("m1"), null);

        assertTrue(capturedOutput.toString().contains(
                "Result m1, GHS <missing> -> Purchase amount must not be null"));
    }

    @Test
    void reportsANullMemberIdAsAnExpectedInputError() {
        StringWriter capturedOutput = new StringWriter();
        Demo demo = new Demo(
                new LoyaltyServiceFactory().createCalculator(),
                new PrintWriter(capturedOutput, true));

        demo.show(null, BigDecimal.TEN);

        assertTrue(capturedOutput.toString().contains(
                "Result <missing>, GHS 10 -> Member id must not be blank"));
    }

    @Test
    @ResourceLock("Demo logger")
    void logsUnexpectedFailuresAndDisplaysAControlledMessage() {
        IllegalStateException rootCause = new IllegalStateException("repository unavailable");
        MemberRepository failingRepository = memberId -> {
            throw rootCause;
        };
        PointsCalculator calculator = new PointsCalculator(
                failingRepository,
                new TierPointsRuleResolver(List.of(new StandardTierRule())),
                new ThresholdBonusPolicy());
        StringWriter capturedOutput = new StringWriter();
        List<LogRecord> logRecords = new ArrayList<>();
        Logger logger = Logger.getLogger(Demo.class.getName());
        Level previousLevel = logger.getLevel();
        boolean previousParentHandlers = logger.getUseParentHandlers();
        Handler[] existingHandlers = logger.getHandlers();
        Handler testHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                logRecords.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        try {
            for (Handler existingHandler : existingHandlers) {
                logger.removeHandler(existingHandler);
            }
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.ALL);
            logger.addHandler(testHandler);

            Demo demo = new Demo(calculator, new PrintWriter(capturedOutput, true));
            demo.show(new MemberId("m1"), BigDecimal.TEN);
        } finally {
            logger.removeHandler(testHandler);
            for (Handler existingHandler : existingHandlers) {
                logger.addHandler(existingHandler);
            }
            logger.setLevel(previousLevel);
            logger.setUseParentHandlers(previousParentHandlers);
        }

        assertAll(
                () -> assertTrue(capturedOutput.toString().contains(
                        "Unable to process this purchase right now.")),
                () -> assertEquals(1, logRecords.size()),
                () -> assertEquals(Level.SEVERE, logRecords.get(0).getLevel()),
                () -> assertTrue(logRecords.get(0).getMessage().contains("member m1")),
                () -> assertSame(rootCause, logRecords.get(0).getThrown()));
    }
}
