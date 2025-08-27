package net.focik.homeoffice.library.api.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@Builder
public class AuthorDto {
    private Integer id;
    private String firstName;
    private String lastName;
}
