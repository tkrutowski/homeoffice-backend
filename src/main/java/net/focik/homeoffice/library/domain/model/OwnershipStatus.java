package net.focik.homeoffice.library.domain.model;


import lombok.Getter;

@Getter
public enum OwnershipStatus {
    HAVE("Mam"), WANT("Chce"), READ_ONLY("Tylko czytam");

    private final String viewValue;

    OwnershipStatus(String viewValue) {
        this.viewValue = viewValue;
    }

}
