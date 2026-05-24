package net.focik.homeoffice.library.infrastructure.dto;

import lombok.*;

import jakarta.persistence.*;
import net.focik.homeoffice.audit.AuditableEntity;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@ToString
@Table(name = "library_authors")
public
class AuthorDbDto extends AuditableEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
}
