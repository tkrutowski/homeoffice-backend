package net.focik.homeoffice.goahead.api.dto;

import lombok.*;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class AddressDto {
    private Integer id;
    private String city;
    private String street;
    private String zip;


    @Override
    public String toString() {
        return "ul. " + street + ", " + zip + " " + city;
    }

}
