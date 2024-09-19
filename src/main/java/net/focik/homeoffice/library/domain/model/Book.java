package net.focik.homeoffice.library.domain.model;

import lombok.*;

import java.util.Objects;
import java.util.Set;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Book {
    private Integer id;
    private Series series;
    private Set<Author> authors;
    private Set<Category> categories;
    private String title;
    private String description;
    private String cover;
    private String bookInSeriesNo;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book)) return false;
        Book book = (Book) o;
        return Objects.equals(getBookInSeriesNo(), book.getBookInSeriesNo()) &&
                Objects.equals(getSeries(), book.getSeries()) &&
                getAuthors().equals(book.getAuthors()) &&
                getTitle().equals(book.getTitle());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSeries(), getAuthors(), getTitle(), getBookInSeriesNo());
    }

    @Override
    public String toString() {
        return "Book{" +
                "series='" + series + '\'' +
                ", authors=" + authors +
                ", categories=" + categories +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", bookInSeriesNo=" + bookInSeriesNo +
                '}';
    }
}

