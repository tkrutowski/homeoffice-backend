package net.focik.homeoffice.addresses.infrastructure.jpa;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.addresses.domain.Address;
import net.focik.homeoffice.addresses.domain.port.AddressRepository;
import net.focik.homeoffice.addresses.infrastructure.dto.AddressDbDto;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@AllArgsConstructor
class AddressRepositoryAdapter implements AddressRepository {

    AddressDtoRepository addressDtoRepository;
    JpaMapper mapper;

    @Override
    public Address save(Address address) {
        AddressDbDto save = addressDtoRepository.save(mapper.toDto(address));
        return mapper.toDomain(save);
    }

    @Override
    public Optional<Address> findById(Integer id) {
        if (id == null)
            return Optional.empty();

        Optional<AddressDbDto> byId = addressDtoRepository.findById(id);

        if (byId.isPresent())
            return Optional.of(mapper.toDomain(byId.get()));

        return Optional.empty();
    }

    @Override
    public void deleteAddress(Integer id) {
        addressDtoRepository.deleteById(id);
    }
}
