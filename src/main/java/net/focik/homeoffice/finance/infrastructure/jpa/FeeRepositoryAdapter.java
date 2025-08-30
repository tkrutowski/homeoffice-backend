package net.focik.homeoffice.finance.infrastructure.jpa;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.finance.domain.fee.Fee;
import net.focik.homeoffice.finance.domain.fee.FeeInstallment;
import net.focik.homeoffice.finance.domain.fee.port.secondary.FeeRepository;
import net.focik.homeoffice.finance.infrastructure.dto.FeeDbDto;
import net.focik.homeoffice.finance.infrastructure.dto.FeeInstallmentDbDto;
import net.focik.homeoffice.finance.infrastructure.mapper.JpaFeeMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@Primary
@AllArgsConstructor
class FeeRepositoryAdapter implements FeeRepository {

    FeeDtoRepository feeDtoRepository;
    FeeInstallmentDtoRepository feeInstallmentDtoRepository;
    JpaFeeMapper mapper;


    @Override
    public Fee saveFee(Fee fee) {
        FeeDbDto dbDto = mapper.toDto(fee);

        log.debug("Saving fee: {}", dbDto);
        FeeDbDto saved = feeDtoRepository.save(dbDto);

        log.debug("Saved fee: {}", saved);
        Fee domain = mapper.toDomain(dbDto);
        log.debug("Mapped fee to domain: {}", domain);
        return domain;
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
        return byId.map(feeDbDto -> mapper.toDomain(feeDbDto));
    }

    @Override
    public Optional<FeeInstallment> findFeeInstallmentById(Integer id) {
        Optional<FeeInstallmentDbDto> byId = feeInstallmentDtoRepository.findById(id);
        return byId.map(feeInstallmentDbDto -> mapper.toDomain(feeInstallmentDbDto));
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

    @Override
    public List<Fee> findFeeByFirmId(Integer idFirm) {
        return feeDtoRepository.findAllByFirm_Id(idFirm).stream()
                .map(loanDto -> mapper.toDomain(loanDto))
                .collect(Collectors.toList());
    }

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
