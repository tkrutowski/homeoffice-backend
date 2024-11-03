package net.focik.homeoffice.devices.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectNotFoundException;

public class DeviceNotFoundException extends ObjectNotFoundException {
    public DeviceNotFoundException(Integer id) {
        super(String.format("Nie znaleziono urządzenia o id %s",id));
    }
}
