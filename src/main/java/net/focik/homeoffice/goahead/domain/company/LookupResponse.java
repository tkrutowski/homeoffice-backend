package net.focik.homeoffice.goahead.domain.company;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LookupResponse {
    private Result result;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result {
        private Subject subject;
        private String requestId;
        private String requestDateTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Subject {
        private String name;
        private String nip;
        private String statusVat;
        private String regon;
        private String pesel;
        private String krs;
        private String residenceAddress;
        private String workingAddress;
        private List<String> representatives;
        private List<String> authorizedClerks;
        private List<String> partners;
        private LocalDate registrationLegalDate;
        private LocalDate registrationDenialDate;
        private String registrationDenialBasis;
        private LocalDate restorationDate;
        private String restorationBasis;
        private LocalDate removalDate;
        private String removalBasis;
        private List<String> accountNumbers;
        private boolean hasVirtualAccounts;
    }
}