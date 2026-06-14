package net.focik.homeoffice.finance.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@ToString
public class PurchaseImportDto {
    private int idCard;
    private int idFirm;
    private int idUser;
    private String name;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Europe/Warsaw")
    private LocalDate purchaseDate;
    private String amount;
    private String otherInfo;
    private boolean exists;
}
