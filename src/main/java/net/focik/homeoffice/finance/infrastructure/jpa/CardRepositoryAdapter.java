package net.focik.homeoffice.finance.infrastructure.jpa;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.card.Card;
import net.focik.homeoffice.finance.domain.card.port.secondary.CardRepository;
import net.focik.homeoffice.finance.infrastructure.dto.CardDbDto;
import net.focik.homeoffice.finance.infrastructure.mapper.JpaCardMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Primary
@AllArgsConstructor
class CardRepositoryAdapter implements CardRepository {

    CardDtoRepository cardDtoRepository;
    JpaCardMapper mapper;

    @Override
    public Card saveCard(Card card) {
        CardDbDto dbDto = mapper.toDto(card);
        CardDbDto saved = cardDtoRepository.save(dbDto);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Card> findCardById(Integer id) {

        Optional<CardDbDto> byId = cardDtoRepository.findById(id);
        return byId.map(mapper::toDomain);
    }

    @Override
    public List<Card> findCardByUserId(Integer idUser) {
        List<CardDbDto> allByIdUser = cardDtoRepository.findAllByIdUser(idUser);
        return allByIdUser.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Card> findCardByName(String cardName) {
        List<CardDbDto> allByIdUser = cardDtoRepository.findAllByCardName(cardName);
        return allByIdUser.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Card> findAll() {
        return cardDtoRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteCardById(int idCard) {
        cardDtoRepository.deleteById(idCard);
    }

    @Override
    public List<Card> findCardByBankId(Integer idBank) {
        return cardDtoRepository.findAllByIdBank(idBank).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

}