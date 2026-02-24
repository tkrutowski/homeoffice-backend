package net.focik.homeoffice.utils.share;

import lombok.Getter;

@Getter
public enum Module {
    CARD("/images/cards/"),
    DEVICE_IMAGES("/homeoffice/devices/images/"),
    DEVICE_FILES("/homeoffice/devices/files/"),
    BOOK("/homeoffice/images/books/"),
    GO_AHEAD("/goahead/");

    private final String directory;

    Module(String directory) {
        this.directory = directory;
    }

}
