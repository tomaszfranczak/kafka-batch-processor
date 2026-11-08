package io.github.tomaszfranczak.batchprocessor.transaction;

import io.github.tomaszfranczak.batchprocessor.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static io.github.tomaszfranczak.batchprocessor.transaction.TestTransactions.VALID_TRANSACTION_JSON;
import static io.github.tomaszfranczak.batchprocessor.transaction.TestTransactions.validTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@WebMvcTest(TransactionController.class)
@Import(SecurityConfig.class)
class TransactionControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private TransactionPublisher publisher;

    @Test
    @WithMockUser
    void validTransactionIsPublishedAndAccepted() {
        // when
        MvcTestResult result = postTransaction(VALID_TRANSACTION_JSON);

        // then
        assertThat(result).hasStatus(HttpStatus.ACCEPTED);
        verify(publisher).publish(validTransaction().build());
    }

    @Test
    @WithMockUser
    void invalidTransactionIsRejectedWithEveryInvalidField() {
        // given
        String transaction = VALID_TRANSACTION_JSON
                .replace("\"price\": 67250.50", "\"price\": 0")
                .replace("\"symbol\": \"BTC-USDT\"", "\"symbol\": \"btc\"");

        // when
        MvcTestResult result = postTransaction(transaction);

        // then
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors").asMap().containsOnlyKeys("price", "symbol");
        verifyNoInteractions(publisher);
    }

    @Test
    @WithMockUser
    void validationMessagesAreInEnglishRegardlessOfTheRequestedLanguage() {
        // given
        String transaction = VALID_TRANSACTION_JSON.replace("\"price\": 67250.50", "\"price\": 0");

        // when
        MvcTestResult result = mvc.post().uri("/api/transactions")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "pl-PL")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transaction)
                .exchange();

        // then
        assertThat(result).bodyJson().extractingPath("$.errors.price").isEqualTo("must be greater than 0");
    }

    @Test
    @WithMockUser
    void malformedJsonIsRejected() {
        // when
        MvcTestResult result = postTransaction("{ \"transactionId\": ");

        // then
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(publisher);
    }

    @Test
    @WithMockUser
    void unknownSideIsRejected() {
        // given
        String transaction = VALID_TRANSACTION_JSON.replace("\"side\": \"BUY\"", "\"side\": \"HOLD\"");

        // when
        MvcTestResult result = postTransaction(transaction);

        // then
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(publisher);
    }

    @Test
    @WithMockUser
    void publishFailureIsReportedAsServiceUnavailable() {
        // given
        willThrow(new TransactionPublishException("tx-1", new RuntimeException("broker unavailable")))
                .given(publisher).publish(any());

        // when
        MvcTestResult result = postTransaction(VALID_TRANSACTION_JSON);

        // then
        assertThat(result).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void requestWithoutCredentialsIsRejected() {
        // when
        MvcTestResult result = postTransaction(VALID_TRANSACTION_JSON);

        // then
        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(publisher);
    }

    private MvcTestResult postTransaction(String body) {
        return mvc.post().uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }
}
