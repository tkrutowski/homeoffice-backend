package net.focik.homeoffice.finance.api.dto;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Builder
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class CardDto {
    private int id;
    private int idBank;
    private int idUser;
    private String name;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate activationDate;
    private int limit;
    private int repaymentDay;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;
    private String otherInfo;
    private String activeStatus;
    private String cardNumber;
    private int closingDay;
    private String imageUrl;
}