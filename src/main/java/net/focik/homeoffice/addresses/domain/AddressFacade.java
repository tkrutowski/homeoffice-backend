package net.focik.homeoffice.addresses.domain;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.audit.AuditAction;
import net.focik.homeoffice.audit.AuditLog;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AddressFacade {

    private final AddressService addressService;

    @AuditLog(action = AuditAction.CREATE, entityType = "Address")
    public Address addAddress(Address address) {
        return addressService.addAddress(address);
    }

    @AuditLog(action = AuditAction.UPDATE, entityType = "Address")
    public Address updateAddress(Address address) {
        return addressService.updateAddress(address);
    }

    public Address getAddress(Integer id) {
        return addressService.getAddress(id);
    }

    @AuditLog(action = AuditAction.DELETE, entityType = "Address")
    public void deleteAddress(Integer id){
        addressService.deleteAddress(id);
    }

}
