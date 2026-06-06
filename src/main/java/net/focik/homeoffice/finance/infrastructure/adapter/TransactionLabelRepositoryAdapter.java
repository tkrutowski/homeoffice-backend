package net.focik.homeoffice.finance.infrastructure.adapter;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionLabel;
import net.focik.homeoffice.finance.domain.transaction.port.secondary.TransactionLabelRepository;
import net.focik.homeoffice.finance.infrastructure.jpa.TransactionLabelDtoRepository;
import net.focik.homeoffice.finance.infrastructure.mapper.JpaTransactionLabelMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@AllArgsConstructor
public class TransactionLabelRepositoryAdapter implements TransactionLabelRepository {

    private final TransactionLabelDtoRepository jpaRepository;
    private final JpaTransactionLabelMapper mapper;

    @Override
    public TransactionLabel saveTransactionLabel(TransactionLabel transactionLabel) {
        var dbDto = mapper.toDto(transactionLabel);
        if (dbDto.getId() != null && dbDto.getId() == 0){
            dbDto.setId(null);
        }
        var saved = jpaRepository.save(dbDto);
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteTransactionLabel(int id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<TransactionLabel> findAllTransactionLabels() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<TransactionLabel> findTransactionLabelById(int id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }
}
