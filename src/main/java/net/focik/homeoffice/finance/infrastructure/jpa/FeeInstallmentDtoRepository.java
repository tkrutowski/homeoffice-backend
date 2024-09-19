package net.focik.homeoffice.finance.infrastructure.jpa;

import net.focik.homeoffice.finance.infrastructure.dto.FeeInstallmentDbDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


interface FeeInstallmentDtoRepository extends JpaRepository<FeeInstallmentDbDto, Integer> {

//    List<FeeInstallmentDbDto> findAllByIdFeeAndPaymentDeadlineContaining(Integer idFee, String data);

//    List<FeeInstallmentDbDto> findAllByPaymentDeadlineContaining(String year);

    List<FeeInstallmentDbDto> findAllByIdFee(Integer idFee);

    void deleteByIdFee(Integer integer);
}
