package net.focik.homeoffice.devices.domain.model;


import lombok.*;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.utils.share.ActiveStatus;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Computer {
        private AppUser user;
        private String  name;
        private ActiveStatus status;
        private String info;
}
