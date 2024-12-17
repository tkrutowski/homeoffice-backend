package net.focik.homeoffice.finance.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@ToString
public class FeeDto {
    private int id;
    private FirmDto firm;
    private int idUser;
    private String name;
    private String feeNumber;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Europe/Warsaw")
    private LocalDate date;
    private FeeFrequencyDto feeFrequency;
    private Integer numberOfPayments;//ile razy pobrać
    private Number amount;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Europe/Warsaw")
    private LocalDate firstPaymentDate;
    private String accountNumber;
    private PaymentStatusDto feeStatus;
    private String otherInfo;
    private List<FeeInstallmentDto> installmentList;
}