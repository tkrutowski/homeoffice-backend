package net.focik.homeoffice.finance.infrastructure.jpa;

import net.focik.homeoffice.finance.infrastructure.dto.CardDbDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface CardDtoRepository extends JpaRepository<CardDbDto, Integer> {

    List<CardDbDto> findAllByIdUser(Integer idUser);
    List<CardDbDto> findAllByIdBank(Integer idBank);
    List<CardDbDto> findAllByCardName(String name);
}
