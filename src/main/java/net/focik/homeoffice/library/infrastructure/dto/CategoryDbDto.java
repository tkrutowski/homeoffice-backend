package net.focik.homeoffice.library.infrastructure.dto;


import lombok.*;

import jakarta.persistence.*;
import net.focik.homeoffice.audit.AuditableEntity;


@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
 @ToString
@Table(name = "library_categories")
public
class CategoryDbDto extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    private String name;
}
