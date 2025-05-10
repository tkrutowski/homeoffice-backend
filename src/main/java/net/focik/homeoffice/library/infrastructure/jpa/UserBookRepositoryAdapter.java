package net.focik.homeoffice.library.infrastructure.jpa;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.library.domain.model.ReadingStatus;
import net.focik.homeoffice.library.domain.model.UserBook;
import net.focik.homeoffice.library.domain.port.secondary.UserBookRepository;
import net.focik.homeoffice.library.infrastructure.dto.UserBookDbDto;
import net.focik.homeoffice.library.infrastructure.mapper.JpaUserBookMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class UserBookRepositoryAdapter implements UserBookRepository {

    private UserBookDtoRepository userBookDtoRepository;
    private final JpaUserBookMapper mapper;

    @Override
    public UserBook add(UserBook userBook) {
        UserBookDbDto userBookDbDto = mapper.toDto(userBook);
        if (userBookDbDto.getId() == 0){
            userBookDbDto.setId(null);
        }
        UserBookDbDto save = userBookDtoRepository.save(userBookDbDto);
        return mapper.toDomain(save);
    }

    @Override
    public UserBook edit(UserBook userBook) {
        UserBookDbDto userBookDbDto = mapper.toDto(userBook);
        UserBookDbDto save = userBookDtoRepository.save(userBookDbDto);
        return mapper.toDomain(save);
    }

    @Override
    public boolean delete(Integer id) {
        userBookDtoRepository.deleteById(id);
        return true;
    }

    @Override
    public List<UserBook> findAllByUser(Long idUser) {
        return userBookDtoRepository.findAllByUser_Id(idUser)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserBook> findAllByIdBook(Integer idBook) {
        return userBookDtoRepository.findAllByBook_Id(idBook)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserBook> findAllByUserAndReadStatus(Long idUser, ReadingStatus readingStatus) {
        List<UserBookDbDto> userBookDtos = userBookDtoRepository.findAllByReadingStatusAndUser_Id(readingStatus, idUser);
        return userBookDtos.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserBook> findAllByUserAndReadStatusAndYear(Long idUser, ReadingStatus readingStatus, LocalDate startDate, LocalDate stopDate) {
        List<UserBookDbDto> userBookDtos = userBookDtoRepository.findAllByReadingStatusAndUser_IdAndReadToBetween(readingStatus, idUser, startDate, stopDate);
        return userBookDtos.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserBook> findById(Integer id) {
        return userBookDtoRepository.findById(id)
                .map(mapper::toDomain);
    }

}
