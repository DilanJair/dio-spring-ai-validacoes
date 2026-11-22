package dio.budgeting.application;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PersistTransactionUseCaseTest {
    private TransactionRepository repository;
    private PersistTransactionUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = mock(TransactionRepository.class);
        useCase = new PersistTransactionUseCase(repository);
    }

    @ParameterizedTest
    @CsvSource({"1, 0.01", "3590, 35.90", "9007199254740993, 90071992547409.93"})
    void shouldSaveTransactionWhenInputIsValid(long amount, String expectedAmount) {
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var output = useCase.execute(new PersistTransactionInput("Compra no mercado", amount, Category.GROCERIES));

        var transaction = ArgumentCaptor.forClass(Transaction.class);
        verify(repository).save(transaction.capture());
        assertThat(transaction.getValue().getDescription()).isEqualTo("Compra no mercado");
        assertThat(transaction.getValue().getAmount()).isEqualTo(amount);
        assertThat(transaction.getValue().getCategory()).isEqualTo(Category.GROCERIES);
        assertThat(output.id()).isEqualTo(transaction.getValue().getId().uuid().toString());
        assertThat(output.value()).isEqualTo(new BigDecimal(expectedAmount));
    }

    @Test
    void shouldRejectMissingInputBeforeSaving() {
        assertThatExceptionOfType(InvalidTransactionException.class)
                .isThrownBy(() -> useCase.execute(null))
                .withMessage("Os dados da transação são obrigatórios.");
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void shouldRejectMissingOrBlankDescriptionBeforeSaving(String description) {
        var input = new PersistTransactionInput(description, 3590, Category.GROCERIES);

        assertThatExceptionOfType(InvalidTransactionException.class)
                .isThrownBy(() -> useCase.execute(input))
                .withMessage("A descrição é obrigatória.");
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, Long.MIN_VALUE})
    void shouldRejectNonPositiveAmountBeforeSaving(long amount) {
        var input = new PersistTransactionInput("Compra no mercado", amount, Category.GROCERIES);

        assertThatExceptionOfType(InvalidTransactionException.class)
                .isThrownBy(() -> useCase.execute(input))
                .withMessage("O valor deve ser maior que zero.");
        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectMissingCategoryBeforeSaving() {
        var input = new PersistTransactionInput("Compra no mercado", 3590, null);

        assertThatExceptionOfType(InvalidTransactionException.class)
                .isThrownBy(() -> useCase.execute(input))
                .withMessage("A categoria é obrigatória.");
        verifyNoInteractions(repository);
    }
}
