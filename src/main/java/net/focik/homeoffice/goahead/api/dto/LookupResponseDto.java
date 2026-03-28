package net.focik.homeoffice.goahead.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LookupResponseDto {
    private String name;
    private String nip;
    private String regon;
    private String accountNumber;
    private AddressDto addressDto;
}
