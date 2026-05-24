package net.focik.homeoffice.library.infrastructure.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
 import lombok.ToString;
import net.focik.homeoffice.audit.AuditableEntity;


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
