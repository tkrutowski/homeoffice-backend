package net.focik.homeoffice.library.infrastructure.dto;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "library_books")
public
class BookDbDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_series")
    private SeriesDbDto series;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinTable(name = "library_books_authors",
            joinColumns = {@JoinColumn(name = "id_book")},
            inverseJoinColumns = {@JoinColumn(name = "id_author")})
    private Set<AuthorDbDto> authors = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinTable(name = "library_books_categories",
            joinColumns = {@JoinColumn(name = "id_book")},
            inverseJoinColumns = {@JoinColumn(name = "id_category")})
    private Set<CategoryDbDto> categories = new HashSet<>();
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String cover;
    private String bookInSeriesNo;
}
