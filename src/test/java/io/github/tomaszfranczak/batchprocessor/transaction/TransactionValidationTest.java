package io.github.tomaszfranczak.batchprocessor.transaction;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static io.github.tomaszfranczak.batchprocessor.transaction.TransactionFixtures.aTransaction;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void validTransactionHasNoViolations() {
        // given
        Transaction transaction = aTransaction().build();

        // when
        List<String> invalidFields = invalidFields(transaction);

        // then
        assertThat(invalidFields).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void blankTransactionIdIsRejected(String transactionId) {
        // given
        Transaction transaction = aTransaction().transactionId(transactionId).build();

        // when
        List<String> invalidFields = invalidFields(transaction);

        // then
        assertThat(invalidFields).containsExactly("transactionId");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void blankOrderIdIsRejected(String orderId) {
        // given
        Transaction transaction = aTransaction().orderId(orderId).build();

        // when
        List<String> invalidFields = invalidFields(transaction);

        // then
        assertThat(invalidFields).containsExactly("orderId");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void blankAccountIdIsRejected(String accountId) {
        // given
        Transaction transaction = aTransaction().accountId(accountId).build();

        // when
        List<String> invalidFields = invalidFields(transaction);

        // then
        assertThat(invalidFields).containsExactly("accountId");
    }

    @ParameterizedTest
    @ValueSource(strings = {"BTC-USDT", "ETH-BTC", "1INCH-USDT"})
    void symbolInBaseQuoteFormatIsAccepted(String symbol) {
        // given
        Transaction transaction = aTransaction().symbol(symbol).build();

        // when
        List<String> invalidFields = invalidFields(transaction);

        // then
        assertThat(invalidFields).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "BTC", "btc-usdt", "BTC-", "-USDT", "BTC/USDT", "BTC-USDT-PERP", " BTC-USDT"})
    void symbolNotInBaseQuoteFormatIsRejected(String symbol) {
        // given
        Transaction transaction = aTransaction().symbol(symbol).build();

        // when
        List<String> invalidFields = invalidFields(transaction);

        // then
        assertThat(invalidFields).containsExactly("symbol");
    }

    @Test
    void missingSideIsRejected() {
        // given
        Transaction transaction = aTransaction().side(null).build();

        // when
        List<String> invalidFields = invalidFields(transaction);

        // then
        assertThat(invalidFields).containsExactly("side");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "0.00", "-1", "-0.0001"})
    void nonPositivePriceIsRejected(BigDecimal price) {
        // given
        Transaction transaction = aTransaction().price(price).build();

        // when
        List<String> invalidFields = invalidFields(transaction);

        // then
        assertThat(invalidFields).containsExactly("price");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "0.00", "-1", "-0.0001"})
    void nonPositiveQuantityIsRejected(BigDecimal quantity) {
        // given
        Transaction transaction = aTransaction().quantity(quantity).build();

        // when
        List<String> invalidFields = invalidFields(transaction);

        // then
        assertThat(invalidFields).containsExactly("quantity");
    }

    @Test
    void missingExecutionTimeIsRejected() {
        // given
        Transaction transaction = aTransaction().executionTime(null).build();

        // when
        List<String> invalidFields = invalidFields(transaction);

        // then
        assertThat(invalidFields).containsExactly("executionTime");
    }

    @Test
    void everyInvalidFieldIsReported() {
        // given
        Transaction transaction = aTransaction().transactionId("").price(BigDecimal.ZERO).side(null).build();

        // when
        List<String> invalidFields = invalidFields(transaction);

        // then
        assertThat(invalidFields).containsExactlyInAnyOrder("transactionId", "price", "side");
    }

    private static List<String> invalidFields(Transaction transaction) {
        return validator.validate(transaction).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .toList();
    }
}
