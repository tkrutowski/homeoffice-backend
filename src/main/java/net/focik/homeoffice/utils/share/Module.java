package net.focik.homeoffice.utils.share;

import lombok.Getter;

@Getter
public enum Module {
    CARD("/images/cards/"),
    DEVICE("/images/devices/"),
    BOOK("/images/books/");

    private final String imageDirectory;

    Module(String imageDirectory) {
        this.imageDirectory = imageDirectory;
    }

}
