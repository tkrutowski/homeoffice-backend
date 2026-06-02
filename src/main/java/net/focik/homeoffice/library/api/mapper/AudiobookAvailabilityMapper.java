package net.focik.homeoffice.library.api.mapper;

import net.focik.homeoffice.library.api.dto.AudiobookAvailabilityResponseDto;
import net.focik.homeoffice.library.domain.model.AudiobookAvailability;
import net.focik.homeoffice.library.domain.model.AudiobookPlatformResult;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class AudiobookAvailabilityMapper {

    public AudiobookAvailabilityResponseDto toResponseDto(AudiobookAvailability availability) {
        return AudiobookAvailabilityResponseDto.builder()
                .bookId(availability.getBookId())
                .title(availability.getTitle())
                .author(availability.getAuthor())
                .results(availability.getResults().stream()
                        .map(this::toResultDto)
                        .collect(Collectors.toList()))
                .checkedAt(availability.getCheckedAt())
                .build();
    }

    private AudiobookAvailabilityResponseDto.PlatformResultDto toResultDto(AudiobookPlatformResult result) {
        return AudiobookAvailabilityResponseDto.PlatformResultDto.builder()
                .bookstoreId(result.getBookstoreId())
                .platformName(result.getPlatformName())
                .available(result.getAvailable())
                .url(result.getUrl())
                .error(result.getError())
                .build();
    }
}
