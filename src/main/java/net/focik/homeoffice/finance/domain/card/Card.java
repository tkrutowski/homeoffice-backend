package net.focik.homeoffice.finance.domain.card;

import lombok.*;
import net.focik.homeoffice.utils.share.ActiveStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Card {

    private int id;
    private int idBank;
    private int idUser;
    private String cardName;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate activationDate;
    private int limit;
    private int repaymentDay;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;
    private String otherInfo;
    private ActiveStatus activeStatus;
    private String cardNumber;
    private int closingDay;
    private String imageUrl;
    private boolean multi;

    public void changeActiveStatus(ActiveStatus activeStatus) {
        this.activeStatus = activeStatus;
    }
}