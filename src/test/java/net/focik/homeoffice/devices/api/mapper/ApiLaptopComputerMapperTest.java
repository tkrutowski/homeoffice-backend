package net.focik.homeoffice.devices.api.mapper;

import net.focik.homeoffice.devices.api.dto.LaptopComputerDto;
import net.focik.homeoffice.devices.domain.model.LaptopComputer;
import net.focik.homeoffice.utils.share.ActiveStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApiLaptopComputerMapper Tests")
class ApiLaptopComputerMapperTest {

    private ApiLaptopComputerMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ApiLaptopComputerMapper();
    }

    @Test
    @DisplayName("Should map LaptopComputerDto to LaptopComputer domain object")
    void shouldMapDtoToDomain() {
        // Given
        LaptopComputerDto dto = new LaptopComputerDto();
        dto.setId(1);
        dto.setIdUser(100);
        dto.setName("MacBook Pro");
        dto.setActiveStatus(ActiveStatus.ACTIVE);
        dto.setInfo("Work laptop");
        dto.setCpu("Intel i7");
        dto.setGpu("Iris Xe");
        dto.setRam("16GB");
        dto.setStorage("512GB SSD");
        dto.setDisplay("15.6\" Retina");

        // When
        LaptopComputer computer = mapper.toDomain(dto);

        // Then
        assertNotNull(computer);
        assertEquals(1, computer.getId());
        assertEquals(100, computer.getIdUser());
        assertEquals("MacBook Pro", computer.getName());
        assertEquals(ActiveStatus.ACTIVE, computer.getStatus());
        assertEquals("Work laptop", computer.getInfo());
        assertEquals("Intel i7", computer.getCpu());
        assertEquals("Iris Xe", computer.getGpu());
        assertEquals("16GB", computer.getRam());
        assertEquals("512GB SSD", computer.getStorage());
        assertEquals("15.6\" Retina", computer.getDisplay());
    }

    @Test
    @DisplayName("Should map LaptopComputer domain to LaptopComputerDto")
    void shouldMapDomainToDto() {
        // Given
        LaptopComputer computer = new LaptopComputer();
        computer.setId(2);
        computer.setIdUser(101);
        computer.setName("Dell XPS");
        computer.setStatus(ActiveStatus.ACTIVE);
        computer.setInfo("Development laptop");
        computer.setCpu("AMD Ryzen 7");
        computer.setGpu("RTX 3050");
        computer.setRam("32GB");
        computer.setStorage("1TB SSD");
        computer.setDisplay("13.4\" FHD");

        // When
        LaptopComputerDto dto = mapper.toDto(computer);

        // Then
        assertNotNull(dto);
        assertEquals(2, dto.getId());
        assertEquals(101, dto.getIdUser());
        assertEquals("Dell XPS", dto.getName());
        assertEquals(ActiveStatus.ACTIVE, dto.getActiveStatus());
        assertEquals("Development laptop", dto.getInfo());
        assertEquals("AMD Ryzen 7", dto.getCpu());
        assertEquals("RTX 3050", dto.getGpu());
        assertEquals("32GB", dto.getRam());
        assertEquals("1TB SSD", dto.getStorage());
        assertEquals("13.4\" FHD", dto.getDisplay());
    }

    @Test
    @DisplayName("Should handle null values in mapping")
    void shouldHandleNullValues() {
        // Given
        LaptopComputerDto dto = new LaptopComputerDto();
        dto.setId(3);
        dto.setIdUser(102);
        dto.setName("Budget Laptop");
        dto.setCpu(null);
        dto.setGpu(null);
        dto.setDisplay(null);

        // When
        LaptopComputer computer = mapper.toDomain(dto);

        // Then
        assertNotNull(computer);
        assertEquals(3, computer.getId());
        assertNull(computer.getCpu());
        assertNull(computer.getGpu());
        assertNull(computer.getDisplay());
    }

    @Test
    @DisplayName("Should map all fields from LaptopComputer to DTO")
    void shouldMapAllFieldsToDto() {
        // Given
        LaptopComputer computer = new LaptopComputer();
        computer.setId(4);
        computer.setIdUser(103);
        computer.setName("ThinkPad X1");
        computer.setStatus(ActiveStatus.INACTIVE);
        computer.setInfo("Old laptop");
        computer.setCpu("Intel i5");
        computer.setGpu("Intel UHD");
        computer.setRam("8GB");
        computer.setStorage("256GB SSD");
        computer.setDisplay("14.0\" FHD");

        // When
        LaptopComputerDto dto = mapper.toDto(computer);

        // Then
        assertNotNull(dto);
        assertEquals("ThinkPad X1", dto.getName());
        assertEquals(ActiveStatus.INACTIVE, dto.getActiveStatus());
        assertEquals("Intel i5", dto.getCpu());
        assertEquals("256GB SSD", dto.getStorage());
        assertEquals("14.0\" FHD", dto.getDisplay());
    }

    @Test
    @DisplayName("Should handle empty strings in mapping")
    void shouldHandleEmptyStrings() {
        // Given
        LaptopComputerDto dto = new LaptopComputerDto();
        dto.setId(5);
        dto.setIdUser(104);
        dto.setName("");
        dto.setCpu("");
        dto.setDisplay("");

        // When
        LaptopComputer computer = mapper.toDomain(dto);

        // Then
        assertNotNull(computer);
        assertEquals("", computer.getName());
        assertEquals("", computer.getCpu());
        assertEquals("", computer.getDisplay());
    }
}
