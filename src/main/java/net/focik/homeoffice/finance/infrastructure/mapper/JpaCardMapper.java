package net.focik.homeoffice.finance.infrastructure.mapper;

import net.focik.homeoffice.finance.domain.card.Card;
import net.focik.homeoffice.finance.infrastructure.dto.CardDbDto;
import org.springframework.stereotype.Component;

@Component
public class JpaCardMapper {

    public CardDbDto toDto(Card card) {
        return CardDbDto.builder()
                .id(card.getId())
                .idBank(card.getIdBank())
                .idUser(card.getIdUser())
                .cardName(card.getCardName())
                .activationDate(card.getActivationDate())
                .limit(card.getLimit())
                .repaymentDay(card.getRepaymentDay())
                .expirationDate(card.getExpirationDate())
                .otherInfo(card.getOtherInfo())
                .activeStatus(card.getActiveStatus())
                .cardNumber(card.getCardNumber())
                .closingDay(card.getClosingDay())
                .imageUrl(card.getImageUrl())
                .multi(card.isMulti())
                .build();
    }

    public Card toDomain(CardDbDto dto) {
        return Card.builder()
                .id(dto.getId())
                .idBank(dto.getIdBank())
                .idUser(dto.getIdUser())
                .cardName(dto.getCardName())
                .activationDate(dto.getActivationDate())
                .limit(dto.getLimit())
                .repaymentDay(dto.getRepaymentDay())
                .expirationDate(dto.getExpirationDate())
                .otherInfo(dto.getOtherInfo())
                .activeStatus(dto.getActiveStatus())
                .cardNumber(dto.getCardNumber())
                .closingDay(dto.getClosingDay())
                .imageUrl(dto.getImageUrl())
                .multi(dto.getMulti())
                .build();
    }
}