package net.focik.homeoffice.devices.domain.model;


import lombok.Getter;

@Getter
public enum ComputerType {
    LAPTOP("Laptop"), DESKTOP("Desktop"), TABLET("Tablet");

    private final String viewName;

    ComputerType(String viewName) {
        this.viewName = viewName;
    }

}
