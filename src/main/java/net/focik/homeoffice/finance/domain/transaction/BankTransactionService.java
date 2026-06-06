package net.focik.homeoffice.finance.domain.transaction;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.exception.BankTransactionNotFoundException;
import net.focik.homeoffice.finance.domain.exception.BankTransactionNotValidException;
import net.focik.homeoffice.finance.domain.transaction.model.BankTransaction;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategoryType;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionType;
import net.focik.homeoffice.finance.domain.transaction.port.secondary.BankTransactionRepository;
import net.focik.homeoffice.utils.UserHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.focik.homeoffice.finance.domain.transaction.model.TransactionType.TRANSFER_IN;
import static net.focik.homeoffice.finance.domain.transaction.model.TransactionType.TRANSFER_OUT;

@Service
@AllArgsConstructor
class BankTransactionService {

    private final BankTransactionRepository bankTransactionRepository;

    BankTransaction addBankTransaction(BankTransaction bankTransaction) {
        bankTransaction.setIdUser(UserHelper.getCurrentUserId());
        bankTransaction.setTransactionType(calculateType(bankTransaction));
        if (isNotValid(bankTransaction))
            throw new BankTransactionNotValidException();
        return bankTransactionRepository.saveBankTransaction(bankTransaction);
    }

    private TransactionType calculateType(BankTransaction bankTransaction) {
        TransactionCategoryType categoryType = bankTransaction.getTransactionCategory().getType();
        return switch (categoryType) {
            case INCOME:
                yield TRANSFER_IN;
            default:
                yield TRANSFER_OUT;
        };
    }

    BankTransaction updateBankTransaction(BankTransaction bankTransaction) {
        bankTransaction.setTransactionType(calculateType(bankTransaction));
        if (isNotValid(bankTransaction))
            throw new BankTransactionNotValidException();
        return bankTransactionRepository.saveBankTransaction(bankTransaction);
    }

    BankTransaction findBankTransactionById(int idTransaction) {
        Optional<BankTransaction> transactionById = bankTransactionRepository.findBankTransactionById(idTransaction);

        if (transactionById.isEmpty()) {
            throw new BankTransactionNotFoundException(idTransaction);
        }

        return transactionById.get();
    }

    @Transactional
    void deleteBankTransaction(int idTransaction) {
        bankTransactionRepository.deleteBankTransactionById(idTransaction);
    }

    List<BankTransaction> findBankTransactionBetween(Integer idUser, LocalDate dateFrom, LocalDate dateTo) {
        return bankTransactionRepository.findBankTransactionBetween(idUser, dateFrom, dateTo);
    }

    private boolean isNotValid(BankTransaction bankTransaction) {
        if (Objects.equals(bankTransaction.getAmount(), BigDecimal.ZERO))
            return true;
        if (bankTransaction.getTransactionDate() == null)
            return true;
        if (bankTransaction.getTransactionType() == null)
            return true;
        return bankTransaction.getIdUser() == 0;
    }
}
