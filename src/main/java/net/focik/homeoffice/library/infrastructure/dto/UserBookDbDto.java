package net.focik.homeoffice.library.infrastructure.dto;

import lombok.*;
import net.focik.homeoffice.library.domain.model.EditionType;
import net.focik.homeoffice.library.domain.model.OwnershipStatus;
import net.focik.homeoffice.library.domain.model.ReadingStatus;
import net.focik.homeoffice.userservice.domain.AppUser;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.*;
import java.time.LocalDate;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "library_userbooks")
public
class UserBookDbDto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_book")
    private BookDbDto book;

    @ManyToOne
    @JoinColumn(name = "id_user")
    private AppUser user;

    @ManyToOne
    @JoinColumn(name = "id_bookstore")
    private BookstoreDbDto bookstore;

    @Column(name = "edition_type")
    @Enumerated(EnumType.STRING)
    private EditionType editionType;

    @Column(name = "reading_status")
    @Enumerated(EnumType.STRING)
    private ReadingStatus readingStatus;

    @Column(name = "ownership_status")
    @Enumerated(EnumType.STRING)
    private OwnershipStatus ownershipStatus;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "read_from")
    private LocalDate readFrom;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "read_to")
    private LocalDate readTo;

    private String info;

}