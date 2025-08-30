package net.focik.homeoffice.utils.share;

import lombok.Getter;

@Getter
public enum Module {
    CARD("/images/cards/"),
    DEVICE_IMAGES("/devices/images/"),
    DEVICE_FILES("/devices/files/"),
    BOOK("/images/books/");

    private final String directory;

    Module(String directory) {
        this.directory = directory;
    }

}
