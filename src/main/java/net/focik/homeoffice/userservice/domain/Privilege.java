package net.focik.homeoffice.userservice.domain;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.focik.homeoffice.utils.share.PrivilegeType;

import jakarta.persistence.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "privileges")
public class Privilege {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "read_privilege")
    private PrivilegeType read;

    @Enumerated(EnumType.STRING)
    @Column(name = "write_privilege")
    private PrivilegeType write;

    @Column(name = "delete_privilege")
    @Enumerated(EnumType.STRING)
    private PrivilegeType delete;

    public String getFullReadName() {
        return String.format("%s_%s", role.getName().substring("ROLE_".length()), read);
    }

    public String getFullWriteName() {
        return String.format("%s_%s", role.getName().substring("ROLE_".length()), write);
    }

    public String getFullDeleteName() {
        return String.format("%s_%s", role.getName().substring("ROLE_".length()), delete);
    }
}
