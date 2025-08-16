package net.focik.homeoffice.library.infrastructure.jpa;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.library.domain.model.Author;
import net.focik.homeoffice.library.domain.port.secondary.AuthorRepository;
import net.focik.homeoffice.library.infrastructure.dto.AuthorDbDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class AuthorRepositoryAdapter implements AuthorRepository {

    private final AuthorDtoRepository authorDtoRepository;
    private final ModelMapper mapper;

    @Override
    public Author add(Author author) {
        AuthorDbDto authorDbDto = mapper.map(author, AuthorDbDto.class);
        if (authorDbDto.getId() == 0){
            authorDbDto.setId(null);
        }
        AuthorDbDto saved = authorDtoRepository.save(authorDbDto);
        return  mapper.map(saved, Author.class);
    }

    @Override
    public Optional<Author> findById(Integer id) {
        return authorDtoRepository.findById(id).map(authorDto -> mapper.map(authorDto, Author.class));
    }

    @Override
    public List<Author> findAll() {
        return authorDtoRepository.findAllByOrderByLastNameAsc().stream()
                .map(authorDto -> mapper.map(authorDto, Author.class))
                .collect(Collectors.toList());
    }

    @Override
    public boolean delete(Integer id) {
        try {
            authorDtoRepository.deleteById(id);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public Optional<Author> edit(Author author) {
        AuthorDbDto dbDto = authorDtoRepository.save(mapper.map(author, AuthorDbDto.class));
        return Optional.of(mapper.map(dbDto, Author.class));
    }

    @Override
    public Optional<Author> findByFirstNameAndLastName(String firstName, String lastName) {
        return authorDtoRepository.findAuthorDtoByFirstNameAndLastNameIgnoreCase(firstName, lastName)
                .map(authorDto -> mapper.map(authorDto, Author.class));
    }
}
