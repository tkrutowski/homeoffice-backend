package net.focik.homeoffice.userservice.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserNameDto {

    private Long id;
    private String firstName;
    private String lastName;
}
