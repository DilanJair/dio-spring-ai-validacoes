package dio.budgeting.infrastructure.http;

import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionControllerTest {
    private TransactionRepository repository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws IOException {
        repository = mock(TransactionRepository.class);
        var controller = new TransactionController(
                new PersistTransactionUseCase(repository),
                new ListTransactionsByCategoryUseCase(repository),
                mock(TranscriptionModel.class),
                new ClassPathResource("prompts/system-message.st"),
                ChatClient.builder(mock(ChatModel.class)),
                mock(TextToSpeechModel.class));

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @ParameterizedTest
    @CsvSource({"1, 0.01", "3590, 35.90", "9007199254740993, 90071992547409.93"})
    void shouldReturnCreatedWhenTransactionIsValid(long amount, String expectedAmount) throws Exception {
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Compra no mercado","category":"GROCERIES","amount":%d}
                                """.formatted(amount)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id").isNotEmpty())
                .andExpect(jsonPath("description").value("Compra no mercado"))
                .andReturn();

        var mapper = JsonMapper.builder().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).build();
        var responseAmount = mapper.readTree(result.getResponse().getContentAsByteArray()).get("amount");
        assertThat(responseAmount.isNumber()).isTrue();
        assertThat(responseAmount.decimalValue()).isEqualByComparingTo(new BigDecimal(expectedAmount));
        verify(repository).save(any(Transaction.class));
    }

    @Test
    void shouldPropagateIllegalArgumentExceptionFromRepository() {
        var failure = new IllegalArgumentException("Falha interna do repositorio");
        when(repository.save(any(Transaction.class))).thenThrow(failure);

        assertThatThrownBy(() -> mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"description":"Compra no mercado","category":"GROCERIES","amount":3590}
                        """)))
                .hasCause(failure);
        verify(repository).save(any(Transaction.class));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, -3590})
    void shouldReturnBadRequestWhenAmountIsNonPositive(long amount) throws Exception {
        var request = """
                {"description":"Compra no mercado","category":"GROCERIES","amount":%d}
                """.formatted(amount);

        mockMvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("title").value("Transação inválida"))
                .andExpect(jsonPath("detail").value("O valor deve ser maior que zero."));
        verifyNoInteractions(repository);
    }

    @Test
    void shouldReturnBadRequestWhenDescriptionIsMissing() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"GROCERIES","amount":3590}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("detail").value("A descrição é obrigatória."));
        verifyNoInteractions(repository);
    }

    @Test
    void shouldReturnBadRequestWhenDescriptionIsBlank() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"   ","category":"GROCERIES","amount":3590}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("detail").value("A descrição é obrigatória."));
        verifyNoInteractions(repository);
    }

    @Test
    void shouldReturnBadRequestWhenCategoryIsMissing() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Compra no mercado","amount":3590}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("detail").value("A categoria é obrigatória."));
        verifyNoInteractions(repository);
    }

    @Test
    void shouldReturnBadRequestWhenAmountIsMissing() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Compra no mercado","category":"GROCERIES"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }

    @Test
    void shouldReturnBadRequestWhenCategoryIsUnknown() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Compra no mercado","category":"UNKNOWN","amount":3590}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }
}
