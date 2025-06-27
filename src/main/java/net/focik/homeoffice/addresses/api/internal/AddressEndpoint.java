package net.focik.homeoffice.addresses.api.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.addresses.domain.Address;
import net.focik.homeoffice.addresses.domain.AddressFacade;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Log4j2
@Component
public class AddressEndpoint {

    private final AddressFacade addressFacade;

    public Address getAddress(Integer id) {
        log.info("ADDRESS-INTERNAL-SERVICE: Try find address for  id = {}", id);
        Address address = addressFacade.getAddress(id);
        log.info(address != null ? "ADDRESS-INTERNAL-SERVICE: Found address for id = " + id : "ADDRESS-INTERNAL-SERVICE: Not found address for id = " + id);
        return address;
    }

    public Address addAddress(Address address) {
        log.info("ADDRESS-INTERNAL-SERVICE: Try add address: {}", address);
        Address saved = addressFacade.addAddress(address);
        log.info(saved != null ? "ADDRESS-INTERNAL-SERVICE: Added address id = " + saved.getId() : "ADDRESS-INTERNAL-SERVICE: Not added address: " + address);
        return saved;
    }

    public Address updateAddress(Address address) {
        log.info("ADDRESS-INTERNAL-SERVICE: Try update address: {}", address);
        Address saved = addressFacade.updateAddress(address);
        log.info(saved != null ? "ADDRESS-INTERNAL-SERVICE: updated address id = " + saved.getId() : "ADDRESS-INTERNAL-SERVICE: Not added address: " + address);
        return saved;
    }

    public void deleteAddress(Integer id) {
        log.info("ADDRESS-INTERNAL-SERVICE: Try delete address ID: {}", id);
        addressFacade.deleteAddress(id);
        log.info("ADDRESS-INTERNAL-SERVICE: deleted address by id = {}", id);
    }
}
