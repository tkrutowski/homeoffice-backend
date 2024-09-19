package net.focik.homeoffice.finance.infrastructure.jpa;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.fee.Fee;
import net.focik.homeoffice.finance.domain.fee.FeeInstallment;
import net.focik.homeoffice.finance.domain.fee.port.secondary.FeeRepository;
import net.focik.homeoffice.finance.infrastructure.dto.FeeDbDto;
import net.focik.homeoffice.finance.infrastructure.dto.FeeInstallmentDbDto;
import net.focik.homeoffice.finance.infrastructure.mapper.JpaFeeMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Primary
@AllArgsConstructor
class FeeRepositoryAdapter implements FeeRepository {

    FeeDtoRepository feeDtoRepository;
    FeeInstallmentDtoRepository feeInstallmentDtoRepository;
    JpaFeeMapper mapper;


    @Override
    public Fee saveFee(Fee fee) {
        FeeDbDto saved = feeDtoRepository.save(mapper.toDto(fee));
        return mapper.toDomain(saved);
    }

    @Override
    public FeeInstallment saveFeeInstallment(FeeInstallment feeInstallment) {
        FeeInstallmentDbDto saved = feeInstallmentDtoRepository.save(mapper.toDto(feeInstallment));
        return mapper.toDomain(saved);
    }

    @Override
    public List<FeeInstallment> saveFeeInstallment(List<FeeInstallment> feeInstallments) {
        List<FeeInstallmentDbDto> dbDtoList = feeInstallments.stream().map(mapper::toDto).collect(Collectors.toList());
        List<FeeInstallmentDbDto> feeInstallmentDbDtos = feeInstallmentDtoRepository.saveAll(dbDtoList);
        return feeInstallmentDbDtos.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<Fee> findFeeById(Integer id) {
        Optional<FeeDbDto> byId = feeDtoRepository.findById(id);
        if (byId.isEmpty())
            return Optional.empty();

        return Optional.of(mapper.toDomain(byId.get()));
    }

    @Override
    public Optional<FeeInstallment> findFeeInstallmentById(Integer id) {
        Optional<FeeInstallmentDbDto> byId = feeInstallmentDtoRepository.findById(id);

        if (byId.isEmpty())
            return Optional.empty();

        return Optional.of(mapper.toDomain(byId.get()));
    }

    @Override
    public List<Fee> findFeeByUserId(Integer idUser) {
        return feeDtoRepository.findAllByIdUser(idUser).stream()
                .map(loanDto -> mapper.toDomain(loanDto))
                .collect(Collectors.toList());
    }

    @Override
    public List<Fee> findAll() {
        return feeDtoRepository.findAll().stream()
                .map(loanDto -> mapper.toDomain(loanDto))
                .collect(Collectors.toList());
    }

//    @Override
//    public List<FeeInstallment> findFeeInstallmentByDate(LocalDate date) {
//        List<FeeInstallmentDbDto> installmentDbDtos = feeInstallmentDtoRepository
//                .findAllByPaymentDeadlineContaining(String.valueOf(date.getYear()));
//
//
//
//        return installmentDbDtos.stream()
//                .map(l -> mapper.toDomain(l))
//                .collect(Collectors.toList());
//    }

    @Override
    public List<FeeInstallment> findFeeInstallmentByFeeId(Integer loanId) {
        return feeInstallmentDtoRepository.findAllByIdFee(loanId).stream()
                .map(loanInstallmentDto -> mapper.toDomain(loanInstallmentDto))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteFeeById(int idFee) {
        feeDtoRepository.deleteById(idFee);
    }

    @Override
    public void deleteFeeInstallmentById(int idFeeInstallment) {
        feeInstallmentDtoRepository.deleteById(idFeeInstallment);
    }

    @Override
    public void deleteFeeInstallmentByIdFee(int idFee) {
        feeInstallmentDtoRepository.deleteByIdFee(idFee);
    }
}
