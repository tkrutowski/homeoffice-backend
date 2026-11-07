package net.focik.homeoffice.addresses.infrastructure.jpa;

import net.focik.homeoffice.addresses.infrastructure.dto.AddressDbDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

interface AddressDtoRepository extends CrudRepository<AddressDbDto, Integer> {
}
