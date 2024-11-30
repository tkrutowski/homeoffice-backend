package net.focik.homeoffice.finance.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Europe/Warsaw")
    private LocalDate activationDate;
    private int limit;
    private int repaymentDay;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Europe/Warsaw")
    private LocalDate expirationDate;
    private String otherInfo;
    private String activeStatus;
    private String cardNumber;
    private int closingDay;
    private String imageUrl;
}