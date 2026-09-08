package net.focik.homeoffice.finance.infrastructure.adapter;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.transaction.model.BankTransaction;
import net.focik.homeoffice.finance.domain.transaction.port.secondary.BankTransactionRepository;
import net.focik.homeoffice.finance.infrastructure.dto.BankTransactionDbDto;
import net.focik.homeoffice.finance.infrastructure.jpa.BankTransactionDtoRepository;
import net.focik.homeoffice.finance.infrastructure.mapper.JpaBankTransactionMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Primary
@AllArgsConstructor
class BankTransactionRepositoryAdapter implements BankTransactionRepository {

    private final BankTransactionDtoRepository bankTransactionDtoRepository;
    private final JpaBankTransactionMapper mapper;

    @Override
    public BankTransaction saveBankTransaction(BankTransaction bankTransaction) {
        if (bankTransaction.getId() != null && bankTransaction.getId() == 0) {
            bankTransaction.setId(null);
        }
        BankTransactionDbDto saved = bankTransactionDtoRepository.save(mapper.toDto(bankTransaction));
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BankTransaction> findBankTransactionById(Integer id) {
        Optional<BankTransactionDbDto> byId = bankTransactionDtoRepository.findById(id);
        return byId.map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankTransaction> findBankTransactionByUserId(Integer idUser) {
        return bankTransactionDtoRepository.findAllByIdUser(idUser).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankTransaction> findBankTransactionBetween(Integer idUser, LocalDate dateFrom, LocalDate dateTo) {
        return bankTransactionDtoRepository.findByUserAndDateRange(idUser, dateFrom, dateTo).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteBankTransactionById(int id) {
        bankTransactionDtoRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankTransaction> findAll() {
        return bankTransactionDtoRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByTransactionDateAndAmountAndIdUser(java.time.LocalDate transactionDate, java.math.BigDecimal amount, int idUser) {
        return bankTransactionDtoRepository.existsByTransactionDateAndAmountAndIdUser(transactionDate, amount, idUser);
    }
}
