package net.focik.homeoffice.library.domain.port.primary;

import net.focik.homeoffice.library.domain.model.AudiobookAvailability;

public interface FindAudiobookAvailabilityUseCase {
    AudiobookAvailability checkAvailability(Integer bookId);
}
