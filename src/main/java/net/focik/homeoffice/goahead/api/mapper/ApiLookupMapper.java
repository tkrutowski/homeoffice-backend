package net.focik.homeoffice.goahead.api.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.goahead.api.dto.AddressDto;
import net.focik.homeoffice.goahead.api.dto.LookupResponseDto;
import net.focik.homeoffice.goahead.domain.company.LookupResponse;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiLookupMapper {

    public LookupResponseDto toDto(LookupResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getSubject() == null) {
            return null;
        }

        LookupResponse.Subject subject = response.getResult().getSubject();

        String firstAccountNumber = null;
        if (subject.getAccountNumbers() != null && !subject.getAccountNumbers().isEmpty()) {
            firstAccountNumber = subject.getAccountNumbers().get(0);
        }

        return LookupResponseDto.builder()
                .name(subject.getName())
                .nip(subject.getNip())
                .regon(subject.getRegon())
                .accountNumber(firstAccountNumber)
                .addressDto(parseAddress(subject.getWorkingAddress()))
                .build();
    }

    private AddressDto parseAddress(String addressString) {
        if (addressString == null || addressString.isBlank()) {
            return null;
        }

        // MF API usually returns addresses like: "ul. Kwiatowa 1, 00-001 Warszawa"
        // or "Kwiatowa 1, 00-001 Warszawa" or "Kwiatowa 1 lok. 2, 00-001 Warszawa"
        AddressDto addressDto = new AddressDto();

        try {
            int lastCommaIndex = addressString.lastIndexOf(',');
            if (lastCommaIndex != -1) {
                String street = addressString.substring(0, lastCommaIndex).trim();
                String cityAndZip = addressString.substring(lastCommaIndex + 1).trim();

                String[] cityParts = cityAndZip.split(" ", 2);
                if (cityParts.length == 2) {
                    addressDto.setZip(cityParts[0]);
                    addressDto.setCity(cityParts[1]);
                } else {
                    addressDto.setCity(cityAndZip);
                }
                addressDto.setStreet(street);
            } else {
                addressDto.setStreet(addressString);
            }
        } catch (Exception e) {
            addressDto.setStreet(addressString); // Fallback to assigning everything to street
        }

        return addressDto;
    }
}
