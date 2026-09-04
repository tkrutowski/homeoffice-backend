package net.focik.homeoffice.devices.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.focik.homeoffice.utils.share.ActiveStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ComputerDto JSON Polymorphism Tests")
class ComputerDtoPolymorphismTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should serialize DesktopComputerDto with computerType DESKTOP")
    void shouldSerializeDesktopComputerDto() throws Exception {
        // Given
        DesktopComputerDto dto = new DesktopComputerDto();
        dto.setId(1);
        dto.setIdUser(100);
        dto.setName("Gaming PC");
        dto.setActiveStatus(ActiveStatus.ACTIVE);
        dto.setInfo("High-end gaming machine");

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then
        assertTrue(json.contains("\"computerType\":\"DESKTOP\""));
        assertTrue(json.contains("\"name\":\"Gaming PC\""));
        assertTrue(json.contains("\"id\":1"));
    }

    @Test
    @DisplayName("Should serialize LaptopComputerDto with computerType LAPTOP")
    void shouldSerializeLaptopComputerDto() throws Exception {
        // Given
        LaptopComputerDto dto = new LaptopComputerDto();
        dto.setId(2);
        dto.setIdUser(101);
        dto.setName("MacBook Pro");
        dto.setActiveStatus(ActiveStatus.ACTIVE);
        dto.setCpu("Intel i7");
        dto.setGpu("Iris Xe");
        dto.setRam("16GB");
        dto.setStorage("512GB SSD");
        dto.setDisplay("15.6\" Retina");

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then
        assertTrue(json.contains("\"computerType\":\"LAPTOP\""));
        assertTrue(json.contains("\"name\":\"MacBook Pro\""));
        assertTrue(json.contains("\"cpu\":\"Intel i7\""));
        assertTrue(json.contains("\"display\":\"15.6\\\" Retina\""));
    }

    @Test
    @DisplayName("Should deserialize DesktopComputerDto from JSON")
    void shouldDeserializeDesktopComputerDto() throws Exception {
        // Given
        String json = """
            {
                "computerType": "DESKTOP",
                "id": 1,
                "idUser": 100,
                "name": "Gaming PC",
                "activeStatus": "ACTIVE",
                "info": "High-end gaming machine"
            }
            """;

        // When
        ComputerDto dto = objectMapper.readValue(json, ComputerDto.class);

        // Then
        assertNotNull(dto);
        assertTrue(dto instanceof DesktopComputerDto);
        DesktopComputerDto desktopDto = (DesktopComputerDto) dto;
        assertEquals(1, desktopDto.getId());
        assertEquals("Gaming PC", desktopDto.getName());
        assertEquals(ActiveStatus.ACTIVE, desktopDto.getActiveStatus());
    }

    @Test
    @DisplayName("Should deserialize LaptopComputerDto from JSON")
    void shouldDeserializeLaptopComputerDto() throws Exception {
        // Given
        String json = """
            {
                "computerType": "LAPTOP",
                "id": 2,
                "idUser": 101,
                "name": "MacBook Pro",
                "activeStatus": "ACTIVE",
                "cpu": "Intel i7",
                "gpu": "Iris Xe",
                "ram": "16GB",
                "storage": "512GB SSD",
                "display": "15.6\\\" Retina"
            }
            """;

        // When
        ComputerDto dto = objectMapper.readValue(json, ComputerDto.class);

        // Then
        assertNotNull(dto);
        assertTrue(dto instanceof LaptopComputerDto);
        LaptopComputerDto laptopDto = (LaptopComputerDto) dto;
        assertEquals(2, laptopDto.getId());
        assertEquals("MacBook Pro", laptopDto.getName());
        assertEquals("Intel i7", laptopDto.getCpu());
        assertEquals("15.6\" Retina", laptopDto.getDisplay());
    }

    @Test
    @DisplayName("Should deserialize list with mixed computer types")
    void shouldDeserializeListWithMixedTypes() throws Exception {
        // Given
        String json = """
            [
                {
                    "computerType": "DESKTOP",
                    "id": 1,
                    "name": "Gaming PC",
                    "activeStatus": "ACTIVE"
                },
                {
                    "computerType": "LAPTOP",
                    "id": 2,
                    "name": "MacBook Pro",
                    "activeStatus": "ACTIVE",
                    "cpu": "Intel i7"
                }
            ]
            """;

        // When
        List<ComputerDto> computers = objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructCollectionType(List.class, ComputerDto.class));

        // Then
        assertEquals(2, computers.size());
        assertTrue(computers.get(0) instanceof DesktopComputerDto);
        assertTrue(computers.get(1) instanceof LaptopComputerDto);
        assertEquals("Gaming PC", computers.get(0).getName());
        assertEquals("MacBook Pro", computers.get(1).getName());
    }

    @Test
    @DisplayName("Should preserve computerType field in serialization")
    void shouldPreserveComputerTypeField() throws Exception {
        // Given
        DesktopComputerDto dto = new DesktopComputerDto();
        dto.setId(1);
        dto.setName("PC");

        // When
        String json = objectMapper.writeValueAsString(dto);
        ComputerDto deserialized = objectMapper.readValue(json, ComputerDto.class);

        // Then
        assertTrue(deserialized instanceof DesktopComputerDto);
        assertEquals(1, deserialized.getId());
        assertEquals("PC", deserialized.getName());
    }

    @Test
    @DisplayName("Should handle null fields in serialization")
    void shouldHandleNullFieldsInSerialization() throws Exception {
        // Given
        LaptopComputerDto dto = new LaptopComputerDto();
        dto.setId(3);
        dto.setName("Laptop");
        dto.setCpu(null);
        dto.setDisplay(null);

        // When
        String json = objectMapper.writeValueAsString(dto);
        LaptopComputerDto deserialized = (LaptopComputerDto) objectMapper.readValue(json, ComputerDto.class);

        // Then
        assertNotNull(deserialized);
        assertEquals(3, deserialized.getId());
        assertNull(deserialized.getCpu());
        assertNull(deserialized.getDisplay());
    }

}
