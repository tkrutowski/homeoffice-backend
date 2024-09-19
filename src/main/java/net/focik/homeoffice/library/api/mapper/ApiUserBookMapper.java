package net.focik.homeoffice.library.api.mapper;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.library.api.dto.*;
import net.focik.homeoffice.library.domain.model.*;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.utils.UserHelper;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

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
                .editionType(new EditionTypeDto(userBook.getEditionType().name(), userBook.getEditionType().getViewName()))
                .readingStatus(new ReadingStatusDto(userBook.getReadingStatus().name(), userBook.getReadingStatus().getViewValue()))
                .ownershipStatus(new OwnershipStatusDto(userBook.getOwnershipStatus().name(), userBook.getOwnershipStatus().getViewValue()))
                .readFrom(userBook.getReadFrom() != null ? userBook.getReadFrom().toString() : "")
                .readTo(userBook.getReadTo() != null ? userBook.getReadTo().toString() : "")
                .info(userBook.getInfo() != null ? userBook.getInfo() : "")
                .build();
    }

    public UserBook toDomain(UserBookApiDto dto) {
        return UserBook.builder()
                .id(dto.getId())
                .user(UserHelper.getUser())
                .bookstore(new Bookstore(dto.getIdBookstore()))
                .book(bookMapper.toDomain(dto.getBook()))
                .editionType(EditionType.valueOf(dto.getEditionType().getName()))
                .readingStatus(ReadingStatus.valueOf(dto.getReadingStatus().getName()))
                .ownershipStatus(OwnershipStatus.valueOf(dto.getOwnershipStatus().getName()))
                .readFrom(Try.of(() -> LocalDate.parse(dto.getReadFrom())).getOrNull())
                .readTo(Try.of(() -> LocalDate.parse(dto.getReadTo())).getOrNull())
                .info(dto.getInfo())
                .build();
    }

}