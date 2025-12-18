package com.example.sec_kros.services;

import com.example.sec_kros.DTO.ClientDTO;
import com.example.sec_kros.Entities.Client;
import com.example.sec_kros.Repositories.ClientRepository;
import com.example.sec_kros.Services.ClientService;
import com.example.sec_kros.Services.PasswordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private PasswordService passwordService;

    @InjectMocks
    private ClientService clientService;

    // ========== Тесты для getAllClients() ==========

    @Test
    void getAllClients_ShouldReturnAllClients_WhenClientsExist() {
        // Arrange
        List<Client> expectedClients = Arrays.asList(
                createTestClient(1L, "client1@example.com"),
                createTestClient(2L, "client2@example.com")
        );
        when(clientRepository.findAll()).thenReturn(expectedClients);

        // Act
        List<Client> result = clientService.getAllClients();

        // Assert
        assertThat(result).isNotNull().hasSize(2).isEqualTo(expectedClients);
        verify(clientRepository).findAll();
    }

    @Test
    void getAllClients_ShouldReturnEmptyList_WhenNoClientsExist() {
        // Arrange
        when(clientRepository.findAll()).thenReturn(List.of());

        // Act
        List<Client> result = clientService.getAllClients();

        // Assert
        assertThat(result).isNotNull().isEmpty();
        verify(clientRepository).findAll();
    }

    // ========== Тесты для getClientById() ==========

    @Test
    void getClientById_ShouldReturnClient_WhenClientExists() {
        // Arrange
        Long clientId = 1L;
        Client expectedClient = createTestClient(clientId, "test@example.com");
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(expectedClient));

        // Act
        Optional<Client> result = clientService.getClientById(clientId);

        // Assert
        assertThat(result).isPresent().contains(expectedClient);
        verify(clientRepository).findById(clientId);
    }

    @Test
    void getClientById_ShouldReturnEmptyOptional_WhenClientNotExists() {
        // Arrange
        Long nonExistentId = 999L;
        when(clientRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act
        Optional<Client> result = clientService.getClientById(nonExistentId);

        // Assert
        assertThat(result).isEmpty();
        verify(clientRepository).findById(nonExistentId);
    }

    @Test
    void getClientById_ShouldReturnEmptyOptional_WhenIdIsNull() {
        // Act
        Optional<Client> result = clientService.getClientById(null);

        // Assert
        assertThat(result).isEmpty();
        verify(clientRepository, never()).findById(any());
    }

    // ========== Тесты для createClient() ==========

    @Test
    void createClient_ShouldCreateAndSaveClient_WhenValidDTO() {
        // Arrange
        ClientDTO clientDTO = createValidClientDTO();
        String hashedPassword = "hashed_default123";

        when(passwordService.hashPassword("default123")).thenReturn(hashedPassword);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
            Client savedClient = invocation.getArgument(0);
            savedClient.setId(1L);
            return savedClient;
        });

        // Act
        Client result = clientService.createClient(clientDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo(clientDTO.getEmail());
        assertThat(result.getPasswordHash()).isEqualTo(hashedPassword);
        assertThat(result.getCreatedAt()).isNotNull();

        verify(passwordService).hashPassword("default123");
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void createClient_ShouldThrowException_WhenDTOIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> clientService.createClient(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ClientDTO cannot be null");

        verifyNoInteractions(passwordService, clientRepository);
    }

    @Test
    void createClient_ShouldHandleNullFieldsInDTO() {
        // Arrange
        ClientDTO clientDTO = new ClientDTO();
        clientDTO.setEmail("test@example.com");
        // остальные поля null

        String hashedPassword = "hashed_default123";
        when(passwordService.hashPassword("default123")).thenReturn(hashedPassword);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
            Client savedClient = invocation.getArgument(0);
            savedClient.setId(1L);
            return savedClient;
        });

        // Act
        Client result = clientService.createClient(clientDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getFirstName()).isNull();
        assertThat(result.getLastName()).isNull();

        verify(passwordService).hashPassword("default123");
        verify(clientRepository).save(any(Client.class));
    }

    // ========== Тесты для updateClient() ==========

    @Test
    void updateClient_ShouldUpdateClient_WhenClientExists() {
        // Arrange
        Long clientId = 1L;
        Client existingClient = createTestClient(clientId, "old@example.com");

        ClientDTO updateDTO = createValidClientDTO();
        updateDTO.setEmail("new@example.com");

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(existingClient));
        when(clientRepository.save(any(Client.class))).thenReturn(existingClient);

        // Act
        Client result = clientService.updateClient(clientId, updateDTO);

        // Assert
        assertThat(result).isNotNull().isEqualTo(existingClient);
        verify(clientRepository).findById(clientId);
        verify(clientRepository).save(existingClient);
    }

    @Test
    void updateClient_ShouldReturnNull_WhenClientNotExists() {
        // Arrange
        Long nonExistentId = 999L;
        ClientDTO updateDTO = new ClientDTO();

        when(clientRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act
        Client result = clientService.updateClient(nonExistentId, updateDTO);

        // Assert
        assertThat(result).isNull();
        verify(clientRepository).findById(nonExistentId);
        verify(clientRepository, never()).save(any());
    }

    @Test
    void updateClient_ShouldReturnNull_WhenIdIsNull() {
        // Act
        Client result = clientService.updateClient(null, new ClientDTO());

        // Assert
        assertThat(result).isNull();
        verify(clientRepository, never()).findById(any());
        verify(clientRepository, never()).save(any());
    }

    @Test
    void updateClient_ShouldReturnNull_WhenDTOIsNull() {
        // Act
        Client result = clientService.updateClient(1L, null);

        // Assert
        assertThat(result).isNull();
        verify(clientRepository, never()).findById(any());
        verify(clientRepository, never()).save(any());
    }

    @Test
    void updateClient_ShouldHandlePartialUpdate() {
        // Arrange
        Long clientId = 1L;
        Client existingClient = createTestClient(clientId, "old@example.com");

        ClientDTO partialUpdateDTO = new ClientDTO();
        partialUpdateDTO.setEmail("new@example.com"); // только email меняем
        // остальные поля null

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(existingClient));
        when(clientRepository.save(any(Client.class))).thenReturn(existingClient);

        // Act
        Client result = clientService.updateClient(clientId, partialUpdateDTO);

        // Assert
        assertThat(result).isNotNull();
        verify(clientRepository).findById(clientId);
        verify(clientRepository).save(existingClient);
    }

    // ========== Тесты для deleteClient() ==========

    @Test
    void deleteClient_ShouldReturnTrueAndDelete_WhenClientExists() {
        // Arrange
        Long clientId = 1L;
        when(clientRepository.existsById(clientId)).thenReturn(true);
        doNothing().when(clientRepository).deleteById(clientId);

        // Act
        boolean result = clientService.deleteClient(clientId);

        // Assert
        assertThat(result).isTrue();
        verify(clientRepository).existsById(clientId);
        verify(clientRepository).deleteById(clientId);
    }

    @Test
    void deleteClient_ShouldReturnFalse_WhenClientNotExists() {
        // Arrange
        Long nonExistentId = 999L;
        when(clientRepository.existsById(nonExistentId)).thenReturn(false);

        // Act
        boolean result = clientService.deleteClient(nonExistentId);

        // Assert
        assertThat(result).isFalse();
        verify(clientRepository).existsById(nonExistentId);
        verify(clientRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteClient_ShouldReturnFalse_WhenIdIsNull() {
        // Act
        boolean result = clientService.deleteClient(null);

        // Assert
        assertThat(result).isFalse();
        verify(clientRepository, never()).existsById(any());
        verify(clientRepository, never()).deleteById(any());
    }

    // ========== Тесты для existsByEmail() ==========

    @Test
    void existsByEmail_ShouldReturnTrue_WhenEmailExists() {
        // Arrange
        String email = "existing@example.com";
        when(clientRepository.existsByEmail(email)).thenReturn(true);

        // Act
        boolean result = clientService.existsByEmail(email);

        // Assert
        assertThat(result).isTrue();
        verify(clientRepository).existsByEmail(email);
    }

    @Test
    void existsByEmail_ShouldReturnFalse_WhenEmailNotExists() {
        // Arrange
        String email = "nonexistent@example.com";
        when(clientRepository.existsByEmail(email)).thenReturn(false);

        // Act
        boolean result = clientService.existsByEmail(email);

        // Assert
        assertThat(result).isFalse();
        verify(clientRepository).existsByEmail(email);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void existsByEmail_ShouldReturnFalse_ForNullOrEmptyEmail(String email) {
        // Act
        boolean result = clientService.existsByEmail(email);

        // Assert
        assertThat(result).isFalse();
        verify(clientRepository, never()).existsByEmail(anyString());
    }

    // ========== Тесты для findByEmail() ==========

    @Test
    void findByEmail_ShouldReturnClient_WhenEmailExists() {
        // Arrange
        String email = "test@example.com";
        Client expectedClient = createTestClient(1L, email);
        when(clientRepository.findByEmail(email)).thenReturn(Optional.of(expectedClient));

        // Act
        Client result = clientService.findByEmail(email);

        // Assert
        assertThat(result).isNotNull().isEqualTo(expectedClient);
        verify(clientRepository).findByEmail(email);
    }

    @Test
    void findByEmail_ShouldReturnNull_WhenEmailNotExists() {
        // Arrange
        String email = "nonexistent@example.com";
        when(clientRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        Client result = clientService.findByEmail(email);

        // Assert
        assertThat(result).isNull();
        verify(clientRepository).findByEmail(email);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void findByEmail_ShouldReturnNull_ForNullOrEmptyEmail(String email) {
        // Act
        Client result = clientService.findByEmail(email);

        // Assert
        assertThat(result).isNull();
        verify(clientRepository, never()).findByEmail(anyString());
    }

    // ========== Вспомогательные методы ==========

    private Client createTestClient(Long id, String email) {
        Client client = new Client();
        client.setId(id);
        client.setLastName("Иванов");
        client.setFirstName("Иван");
        client.setPatronymic("Иванович");
        client.setPhone("+79991234567");
        client.setEmail(email);
        client.setAddress("г. Москва, ул. Примерная, д. 1");
        client.setPasswordHash("hashed_password");
        client.setCreatedAt(LocalDateTime.now());
        return client;
    }

    private ClientDTO createValidClientDTO() {
        ClientDTO dto = new ClientDTO();
        dto.setLastName("Петров");
        dto.setFirstName("Петр");
        dto.setPatronymic("Петрович");
        dto.setPhone("+79997654321");
        dto.setEmail("petrov@example.com");
        dto.setAddress("г. Санкт-Петербург, Невский пр., д. 10");
        return dto;
    }
}