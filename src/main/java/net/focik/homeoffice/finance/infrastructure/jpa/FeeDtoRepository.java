package net.focik.homeoffice.finance.infrastructure.jpa;

import net.focik.homeoffice.finance.infrastructure.dto.FeeDbDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


interface FeeDtoRepository extends JpaRepository<FeeDbDto, Integer> {

    List<FeeDbDto> findAllByIdUser(Integer idUser);

    List<FeeDbDto> findAllByFirm_Id(Integer firmId);
}
