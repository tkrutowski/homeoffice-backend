package net.focik.homeoffice.library.api.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.library.api.dto.*;
import net.focik.homeoffice.library.domain.model.*;
import net.focik.homeoffice.utils.UserHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiUserBookMapper {
private final ApiBookMapper bookMapper;

    public UserBookApiDto toDto(UserBook userBook) {
        return UserBookApiDto.builder()
                .id(userBook.getId() != null ? userBook.getId() : 0)
                .idUser(Math.toIntExact(userBook.getUser().getId()))
                .idBookstore(userBook.getBookstore().getId())
                .book(bookMapper.toDto(userBook.getBook()))
                .editionType(userBook.getEditionType())
                .readingStatus(userBook.getReadingStatus())
                .ownershipStatus(userBook.getOwnershipStatus())
                .readFrom(userBook.getReadFrom())
                .readTo(userBook.getReadTo())
                .info(userBook.getInfo() != null ? userBook.getInfo() : "")
                .build();
    }

    public UserBook toDomain(UserBookApiDto dto) {
        return UserBook.builder()
                .id(dto.getId())
                .user(UserHelper.getUser())
                .bookstore(new Bookstore(dto.getIdBookstore()))
                .book(bookMapper.toDomain(dto.getBook()))
                .editionType(dto.getEditionType())
                .readingStatus(dto.getReadingStatus())
                .ownershipStatus(dto.getOwnershipStatus())
                .readFrom(dto.getReadFrom())
                .readTo(dto.getReadTo())
                .info(dto.getInfo())
                .build();
    }

}