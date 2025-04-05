package net.focik.homeoffice.devices.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectCanNotBeDeletedException;

public class DeviceCanNotBeDeletedException extends ObjectCanNotBeDeletedException {
    public DeviceCanNotBeDeletedException(String needToRemove) {
        super(String.format("Aby usunąć urządzenie musisz najpierw usunąć %s", needToRemove));
    }

}
