package net.focik.homeoffice.finance.infrastructure.adapter;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategory;
import net.focik.homeoffice.finance.domain.transaction.port.secondary.TransactionCategoryRepository;
import net.focik.homeoffice.finance.infrastructure.jpa.TransactionCategoryDtoRepository;
import net.focik.homeoffice.finance.infrastructure.mapper.JpaTransactionCategoryMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@AllArgsConstructor
public class TransactionCategoryRepositoryAdapter implements TransactionCategoryRepository {

    private final TransactionCategoryDtoRepository jpaRepository;
    private final JpaTransactionCategoryMapper mapper;

    @Override
    public TransactionCategory saveTransactionCategory(TransactionCategory transactionCategory) {
        var dbDto = mapper.toDto(transactionCategory);
        if (dbDto.getId() != null && dbDto.getId() == 0){
            dbDto.setId(null);
        }
        var saved = jpaRepository.save(dbDto);
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteTransactionCategory(int id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<TransactionCategory> findAllTransactionCategories() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<TransactionCategory> findTransactionCategoryById(int id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }
}
