package com.example.sec_kros.services;

import com.example.sec_kros.DTO.GuardObjectDTO;
import com.example.sec_kros.Entities.Client;
import com.example.sec_kros.Entities.Contract;
import com.example.sec_kros.Entities.GuardObject;
import com.example.sec_kros.Repositories.*;
import com.example.sec_kros.Services.GuardObjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuardObjectServiceTest {

    @Mock
    private GuardObjectRepository guardObjectRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private GuardObjectService guardObjectService;

    @Test
    void getAllGuardObjects_ShouldReturnAllGuardObjects() {
        // Arrange
        List<GuardObject> guardObjects = Arrays.asList(
                createGuardObject(1L, "Объект 1"),
                createGuardObject(2L, "Объект 2")
        );
        when(guardObjectRepository.findAll()).thenReturn(guardObjects);

        // Act
        List<GuardObject> result = guardObjectService.getAllGuardObjects();

        // Assert
        assertThat(result).hasSize(2).containsAll(guardObjects);
        verify(guardObjectRepository).findAll();
    }

    @Test
    void getAllGuardObjects_ShouldReturnEmptyList() {
        // Arrange
        when(guardObjectRepository.findAll()).thenReturn(List.of());

        // Act
        List<GuardObject> result = guardObjectService.getAllGuardObjects();

        // Assert
        assertThat(result).isEmpty();
        verify(guardObjectRepository).findAll();
    }

    @Test
    void getGuardObjectById_ShouldReturnGuardObject() {
        // Arrange
        Long objectId = 1L;
        GuardObject guardObject = createGuardObject(objectId, "Объект");
        when(guardObjectRepository.findById(objectId)).thenReturn(Optional.of(guardObject));

        // Act
        Optional<GuardObject> result = guardObjectService.getGuardObjectById(objectId);

        // Assert
        assertThat(result).isPresent().contains(guardObject);
        verify(guardObjectRepository).findById(objectId);
    }

    @Test
    void getGuardObjectById_ShouldReturnEmpty_WhenNotFound() {
        // Arrange
        Long nonExistentId = 999L;
        when(guardObjectRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act
        Optional<GuardObject> result = guardObjectService.getGuardObjectById(nonExistentId);

        // Assert
        assertThat(result).isEmpty();
        verify(guardObjectRepository).findById(nonExistentId);
    }

    @Test
    void createGuardObject_ShouldCreateGuardObject_WhenValidData() {
        // Arrange
        GuardObjectDTO dto = createValidGuardObjectDTO();
        Client client = createClient(1L);
        Contract contract = createContract(1L, client);

        when(clientRepository.findById(dto.getClientId())).thenReturn(Optional.of(client));
        when(contractRepository.findById(dto.getContractId())).thenReturn(Optional.of(contract));
        when(guardObjectRepository.save(any(GuardObject.class))).thenAnswer(invocation -> {
            GuardObject saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        GuardObject result = guardObjectService.createGuardObject(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo(dto.getName());
        assertThat(result.getAddress()).isEqualTo(dto.getAddress());
        assertThat(result.getClient()).isEqualTo(client);
        assertThat(result.getContract()).isEqualTo(contract);

        verify(clientRepository).findById(dto.getClientId());
        verify(contractRepository).findById(dto.getContractId());
        verify(guardObjectRepository).save(any(GuardObject.class));
    }

    @Test
    void createGuardObject_ShouldThrowException_WhenClientNotFound() {
        // Arrange
        GuardObjectDTO dto = createValidGuardObjectDTO();
        when(clientRepository.findById(dto.getClientId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> guardObjectService.createGuardObject(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Клиент не найден");

        verify(clientRepository).findById(dto.getClientId());
        verify(contractRepository, never()).findById(anyLong());
        verify(guardObjectRepository, never()).save(any());
    }

    @Test
    void createGuardObject_ShouldThrowException_WhenContractNotFound() {
        // Arrange
        GuardObjectDTO dto = createValidGuardObjectDTO();
        Client client = createClient(1L);

        when(clientRepository.findById(dto.getClientId())).thenReturn(Optional.of(client));
        when(contractRepository.findById(dto.getContractId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> guardObjectService.createGuardObject(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Договор не найден");

        verify(clientRepository).findById(dto.getClientId());
        verify(contractRepository).findById(dto.getContractId());
        verify(guardObjectRepository, never()).save(any());
    }

    @Test
    void createGuardObject_ShouldThrowException_WhenContractDoesNotBelongToClient() {
        // Arrange
        GuardObjectDTO dto = createValidGuardObjectDTO();
        Client client1 = createClient(1L);
        Client client2 = createClient(2L);
        Contract contract = createContract(1L, client2); // Контракт принадлежит другому клиенту

        when(clientRepository.findById(dto.getClientId())).thenReturn(Optional.of(client1));
        when(contractRepository.findById(dto.getContractId())).thenReturn(Optional.of(contract));

        // Act & Assert
        assertThatThrownBy(() -> guardObjectService.createGuardObject(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Договор не принадлежит выбранному клиенту");

        verify(clientRepository).findById(dto.getClientId());
        verify(contractRepository).findById(dto.getContractId());
        verify(guardObjectRepository, never()).save(any());
    }

    @Test
    void createGuardObject_ShouldSetAllFieldsCorrectly() {
        // Arrange
        GuardObjectDTO dto = createValidGuardObjectDTO();
        Client client = createClient(1L);
        Contract contract = createContract(1L, client);

        when(clientRepository.findById(dto.getClientId())).thenReturn(Optional.of(client));
        when(contractRepository.findById(dto.getContractId())).thenReturn(Optional.of(contract));

        ArgumentCaptor<GuardObject> captor = ArgumentCaptor.forClass(GuardObject.class);
        when(guardObjectRepository.save(captor.capture())).thenAnswer(invocation -> {
            GuardObject saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        guardObjectService.createGuardObject(dto);

        // Assert
        GuardObject savedGuardObject = captor.getValue();
        assertThat(savedGuardObject.getClient()).isEqualTo(client);
        assertThat(savedGuardObject.getContract()).isEqualTo(contract);
        assertThat(savedGuardObject.getName()).isEqualTo(dto.getName());
        assertThat(savedGuardObject.getAddress()).isEqualTo(dto.getAddress());
        assertThat(savedGuardObject.getLatitude()).isEqualByComparingTo(dto.getLatitude());
        assertThat(savedGuardObject.getLongitude()).isEqualByComparingTo(dto.getLongitude());
        assertThat(savedGuardObject.getDescription()).isEqualTo(dto.getDescription());
    }

    @Test
    void updateGuardObject_ShouldUpdateGuardObject_WhenExists() {
        // Arrange
        Long objectId = 1L;
        GuardObject existingObject = createGuardObject(objectId, "Старое название");
        GuardObjectDTO dto = createValidGuardObjectDTO();
        Client client = createClient(1L);
        Contract contract = createContract(1L, client);

        when(guardObjectRepository.findById(objectId)).thenReturn(Optional.of(existingObject));
        when(clientRepository.findById(dto.getClientId())).thenReturn(Optional.of(client));
        when(contractRepository.findById(dto.getContractId())).thenReturn(Optional.of(contract));
        when(guardObjectRepository.save(any(GuardObject.class))).thenReturn(existingObject);

        // Act
        GuardObject result = guardObjectService.updateGuardObject(objectId, dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(dto.getName());

        verify(guardObjectRepository).findById(objectId);
        verify(clientRepository).findById(dto.getClientId());
        verify(contractRepository).findById(dto.getContractId());
        verify(guardObjectRepository).save(existingObject);
    }

    @Test
    void updateGuardObject_ShouldReturnNull_WhenNotFound() {
        // Arrange
        Long nonExistentId = 999L;
        GuardObjectDTO dto = createValidGuardObjectDTO();
        when(guardObjectRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act
        GuardObject result = guardObjectService.updateGuardObject(nonExistentId, dto);

        // Assert
        assertThat(result).isNull();
        verify(guardObjectRepository).findById(nonExistentId);
        verify(clientRepository, never()).findById(anyLong());
        verify(contractRepository, never()).findById(anyLong());
        verify(guardObjectRepository, never()).save(any());
    }

    @Test
    void updateGuardObject_ShouldThrowException_WhenContractDoesNotBelongToClient() {
        // Arrange
        Long objectId = 1L;
        GuardObject existingObject = createGuardObject(objectId, "Объект");
        GuardObjectDTO dto = createValidGuardObjectDTO();
        Client client1 = createClient(1L);
        Client client2 = createClient(2L);
        Contract contract = createContract(1L, client2); // Контракт другого клиента

        when(guardObjectRepository.findById(objectId)).thenReturn(Optional.of(existingObject));
        when(clientRepository.findById(dto.getClientId())).thenReturn(Optional.of(client1));
        when(contractRepository.findById(dto.getContractId())).thenReturn(Optional.of(contract));

        // Act & Assert
        assertThatThrownBy(() -> guardObjectService.updateGuardObject(objectId, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Договор не принадлежит выбранному клиенту");

        verify(guardObjectRepository).findById(objectId);
        verify(clientRepository).findById(dto.getClientId());
        verify(contractRepository).findById(dto.getContractId());
        verify(guardObjectRepository, never()).save(any());
    }

    @Test
    void deleteGuardObject_ShouldReturnTrue_WhenNoSchedulesExist() {
        // Arrange
        Long objectId = 1L;
        GuardObject guardObject = createGuardObject(objectId, "Объект");

        when(guardObjectRepository.findById(objectId)).thenReturn(Optional.of(guardObject));
        when(guardObjectRepository.hasSchedules(objectId)).thenReturn(false);
        doNothing().when(guardObjectRepository).deleteById(objectId);

        // Act
        boolean result = guardObjectService.deleteGuardObject(objectId);

        // Assert
        assertThat(result).isTrue();
        verify(guardObjectRepository).findById(objectId);
        verify(guardObjectRepository).hasSchedules(objectId);
        verify(guardObjectRepository).deleteById(objectId);
    }

    @Test
    void deleteGuardObject_ShouldReturnFalse_WhenSchedulesExist() {
        // Arrange
        Long objectId = 1L;
        GuardObject guardObject = createGuardObject(objectId, "Объект");

        when(guardObjectRepository.findById(objectId)).thenReturn(Optional.of(guardObject));
        when(guardObjectRepository.hasSchedules(objectId)).thenReturn(true);

        // Act
        boolean result = guardObjectService.deleteGuardObject(objectId);

        // Assert
        assertThat(result).isFalse();
        verify(guardObjectRepository).findById(objectId);
        verify(guardObjectRepository).hasSchedules(objectId);
        verify(guardObjectRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteGuardObject_ShouldReturnFalse_WhenObjectNotFound() {
        // Arrange
        Long nonExistentId = 999L;
        when(guardObjectRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act
        boolean result = guardObjectService.deleteGuardObject(nonExistentId);

        // Assert
        assertThat(result).isFalse();
        verify(guardObjectRepository).findById(nonExistentId);
        verify(guardObjectRepository, never()).hasSchedules(anyLong());
        verify(guardObjectRepository, never()).deleteById(anyLong());
    }

    @Test
    void canDeleteGuardObject_ShouldReturnTrue_WhenNoSchedules() {
        // Arrange
        Long objectId = 1L;
        GuardObject guardObject = createGuardObject(objectId, "Объект");

        when(guardObjectRepository.findById(objectId)).thenReturn(Optional.of(guardObject));
        when(guardObjectRepository.hasSchedules(objectId)).thenReturn(false);

        // Act
        boolean result = guardObjectService.canDeleteGuardObject(objectId);

        // Assert
        assertThat(result).isTrue();
        verify(guardObjectRepository).findById(objectId);
        verify(guardObjectRepository).hasSchedules(objectId);
    }

    @Test
    void canDeleteGuardObject_ShouldReturnFalse_WhenSchedulesExist() {
        // Arrange
        Long objectId = 1L;
        GuardObject guardObject = createGuardObject(objectId, "Объект");

        when(guardObjectRepository.findById(objectId)).thenReturn(Optional.of(guardObject));
        when(guardObjectRepository.hasSchedules(objectId)).thenReturn(true);

        // Act
        boolean result = guardObjectService.canDeleteGuardObject(objectId);

        // Assert
        assertThat(result).isFalse();
        verify(guardObjectRepository).findById(objectId);
        verify(guardObjectRepository).hasSchedules(objectId);
    }

    @Test
    void canDeleteGuardObject_ShouldReturnFalse_WhenObjectNotFound() {
        // Arrange
        Long nonExistentId = 999L;
        when(guardObjectRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act
        boolean result = guardObjectService.canDeleteGuardObject(nonExistentId);

        // Assert
        assertThat(result).isFalse();
        verify(guardObjectRepository).findById(nonExistentId);
        verify(guardObjectRepository, never()).hasSchedules(anyLong());
    }

    @Test
    void getGuardObjectsByClientId_ShouldReturnObjects() {
        // Arrange
        Long clientId = 1L;
        List<GuardObject> guardObjects = Arrays.asList(
                createGuardObject(1L, "Объект 1"),
                createGuardObject(2L, "Объект 2")
        );
        when(guardObjectRepository.findByClientId(clientId)).thenReturn(guardObjects);

        // Act
        List<GuardObject> result = guardObjectService.getGuardObjectsByClientId(clientId);

        // Assert
        assertThat(result).hasSize(2).containsAll(guardObjects);
        verify(guardObjectRepository).findByClientId(clientId);
    }

    @Test
    void getGuardObjectsByContractId_ShouldReturnObjects() {
        // Arrange
        Long contractId = 1L;
        List<GuardObject> guardObjects = Arrays.asList(
                createGuardObject(1L, "Объект 1"),
                createGuardObject(2L, "Объект 2")
        );
        when(guardObjectRepository.findByContractId(contractId)).thenReturn(guardObjects);

        // Act
        List<GuardObject> result = guardObjectService.getGuardObjectsByContractId(contractId);

        // Assert
        assertThat(result).hasSize(2).containsAll(guardObjects);
        verify(guardObjectRepository).findByContractId(contractId);
    }

    @Test
    void createGuardObject_ShouldHandleNullCoordinates() {
        // Arrange
        GuardObjectDTO dto = createValidGuardObjectDTO();
        dto.setLatitude(null);
        dto.setLongitude(null);

        Client client = createClient(1L);
        Contract contract = createContract(1L, client);

        when(clientRepository.findById(dto.getClientId())).thenReturn(Optional.of(client));
        when(contractRepository.findById(dto.getContractId())).thenReturn(Optional.of(contract));

        ArgumentCaptor<GuardObject> captor = ArgumentCaptor.forClass(GuardObject.class);
        when(guardObjectRepository.save(captor.capture())).thenAnswer(invocation -> {
            GuardObject saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        guardObjectService.createGuardObject(dto);

        // Assert
        GuardObject savedGuardObject = captor.getValue();
        assertThat(savedGuardObject.getLatitude()).isNull();
        assertThat(savedGuardObject.getLongitude()).isNull();
    }

    @Test
    void updateGuardObject_ShouldHandleNullCoordinates() {
        // Arrange
        Long objectId = 1L;
        GuardObject existingObject = createGuardObject(objectId, "Объект");
        GuardObjectDTO dto = createValidGuardObjectDTO();
        dto.setLatitude(null);
        dto.setLongitude(null);

        Client client = createClient(1L);
        Contract contract = createContract(1L, client);

        when(guardObjectRepository.findById(objectId)).thenReturn(Optional.of(existingObject));
        when(clientRepository.findById(dto.getClientId())).thenReturn(Optional.of(client));
        when(contractRepository.findById(dto.getContractId())).thenReturn(Optional.of(contract));
        when(guardObjectRepository.save(any(GuardObject.class))).thenReturn(existingObject);

        // Act
        GuardObject result = guardObjectService.updateGuardObject(objectId, dto);

        // Assert
        assertThat(result).isNotNull();
        verify(guardObjectRepository).save(existingObject);
    }

    @Test
    void createGuardObject_ShouldHandleNullDescription() {
        // Arrange
        GuardObjectDTO dto = createValidGuardObjectDTO();
        dto.setDescription(null);

        Client client = createClient(1L);
        Contract contract = createContract(1L, client);

        when(clientRepository.findById(dto.getClientId())).thenReturn(Optional.of(client));
        when(contractRepository.findById(dto.getContractId())).thenReturn(Optional.of(contract));

        ArgumentCaptor<GuardObject> captor = ArgumentCaptor.forClass(GuardObject.class);
        when(guardObjectRepository.save(captor.capture())).thenAnswer(invocation -> {
            GuardObject saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        guardObjectService.createGuardObject(dto);

        // Assert
        GuardObject savedGuardObject = captor.getValue();
        assertThat(savedGuardObject.getDescription()).isNull();
    }

    // Вспомогательные методы
    private GuardObject createGuardObject(Long id, String name) {
        GuardObject guardObject = new GuardObject();
        guardObject.setId(id);
        guardObject.setName(name);
        guardObject.setAddress("ул. Примерная, 1");
        guardObject.setLatitude(BigDecimal.valueOf(55.7558));
        guardObject.setLongitude(BigDecimal.valueOf(37.6173));
        guardObject.setDescription("Описание объекта");
        return guardObject;
    }

    private Client createClient(Long id) {
        Client client = new Client();
        client.setId(id);
        client.setFirstName("Иван");
        client.setLastName("Иванов");
        client.setEmail("ivanov@example.com");
        return client;
    }

    private Contract createContract(Long id, Client client) {
        Contract contract = new Contract();
        contract.setId(id);
        contract.setClient(client);
        contract.setStartDate(LocalDate.now().plusDays(1));
        contract.setEndDate(LocalDate.now().plusDays(31));
        contract.setStatus("active");
        contract.setTotalAmount(BigDecimal.valueOf(10000));
        return contract;
    }

    private GuardObjectDTO createValidGuardObjectDTO() {
        GuardObjectDTO dto = new GuardObjectDTO();
        dto.setClientId(1L);
        dto.setContractId(1L);
        dto.setName("Офисный комплекс");
        dto.setAddress("ул. Ленина, 1");
        dto.setLatitude(BigDecimal.valueOf(55.7558)); // BigDecimal вместо Double
        dto.setLongitude(BigDecimal.valueOf(37.6173)); // BigDecimal вместо Double
        dto.setDescription("Многоэтажный офисный комплекс с подземной парковкой");
        return dto;
    }
}