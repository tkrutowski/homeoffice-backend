package net.focik.homeoffice.finance.infrastructure.jpa;

import net.focik.homeoffice.finance.infrastructure.dto.LoanDbDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


interface LoanDtoRepository extends JpaRepository<LoanDbDto, Integer> {

    List<LoanDbDto> findAllByIdUser(Integer idUser);

    List<LoanDbDto> findAllByBank_Id(Integer bankId);
}
