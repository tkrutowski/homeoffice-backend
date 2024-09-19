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
public class LoanDto {
    private int id;
    private BankDto bank;
    private int idUser;
    private String name;
    private Number amount;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Europe/Warsaw")
    private LocalDate date;
    private String loanNumber;
    private String accountNumber;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Europe/Warsaw")
    private LocalDate firstPaymentDate;
    private int numberOfInstallments;
    private Number installmentAmount;
    private PaymentStatusDto loanStatus;
    private Number loanCost;//prowizja itp
    private String otherInfo;
    private String amountToPay;
    private List<LoanInstallmentDto> installmentList;
}