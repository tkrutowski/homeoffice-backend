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
    private CardType cardType;
    /**
     * Dzień zamknięcia cyklu rozliczeniowego - dotyczy tylko kart typu {@link CardType#CREDIT}.
     */
    private Integer closingDay;
    /**
     * Dzień spłaty po zamknięciu cyklu - dotyczy tylko kart typu {@link CardType#CREDIT}.
     */
    private Integer repaymentDay;
    /**
     * Liczba dni od daty zakupu do terminu płatności - dotyczy tylko kart typu {@link CardType#DEFERRED_PAYMENT}.
     */
    private Integer paymentTermDays;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;
    private String otherInfo;
    private ActiveStatus activeStatus;
    private String cardNumber;
    private String imageUrl;
    private boolean multi;

    public void changeActiveStatus(ActiveStatus activeStatus) {
        this.activeStatus = activeStatus;
    }
}