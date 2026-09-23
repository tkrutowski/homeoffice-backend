package net.focik.homeoffice.userservice.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private boolean enabled;
    private Date joinDate;
    private Date lastLoginDate;
    private Date lastLoginDateDisplay;
    //    private boolean isNotLocked;
    private boolean notLocked;
    private Integer idEmployee;
    private String avatarUrl;

}
