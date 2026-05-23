package net.focik.homeoffice.fileService.domain.model;

import lombok.*;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class InvoiceClaudeResponseDto {
    private String number;
    private String sellDate;
    private String invoiceDate;
    private String paymentDate;
    private String paymentMethod;

    private String supplierName;
    private String supplierNip;
    private String supplierStreet;
    private String supplierZip;
    private String supplierCity;
    private String supplierAccount;
    private String supplierBank;

    private List<InvoiceItemDto> items;

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @ToString
    public static class InvoiceItemDto {
        private String name;
        private String unit;
        private String quantity;
        private String amountNet;
        private String amountVat;
        private String amountGross;
        private String vatRate;
    }
}
