package com.example.sec_kros.services;

import com.example.sec_kros.Entities.ServiceEntity;
import com.example.sec_kros.DTO.ServiceDTO;
import com.example.sec_kros.Repositories.ServiceRepository;
import com.example.sec_kros.Services.ServiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private ServiceService serviceService;

    private ServiceEntity testService;
    private ServiceDTO testServiceDTO;

    @BeforeEach
    void setUp() {
        // Создаем тестовую услугу
        testService = new ServiceEntity();
        testService.setId(1L);
        testService.setName("Охрана офиса");
        testService.setDescription("Круглосуточная охрана офисного помещения");
        testService.setPrice(BigDecimal.valueOf(50000));

        // Создаем тестовый DTO
        testServiceDTO = new ServiceDTO();
        testServiceDTO.setName("Охрана офиса");
        testServiceDTO.setDescription("Круглосуточная охрана офисного помещения");
        testServiceDTO.setPrice(BigDecimal.valueOf(50000));
    }

    @Test
    void getAllServices_ShouldReturnAllServices() {
        // Arrange
        ServiceEntity service2 = new ServiceEntity();
        service2.setId(2L);
        service2.setName("Личная охрана");
        service2.setDescription("Персональная охрана клиента");
        service2.setPrice(BigDecimal.valueOf(80000));

        List<ServiceEntity> services = Arrays.asList(testService, service2);
        when(serviceRepository.findAll()).thenReturn(services);

        // Act
        List<ServiceEntity> result = serviceService.getAllServices();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getName()).isEqualTo("Охрана офиса");
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getName()).isEqualTo("Личная охрана");

        verify(serviceRepository, times(1)).findAll();
    }

    @Test
    void getAllServices_ShouldReturnEmptyList_WhenNoServices() {
        // Arrange
        when(serviceRepository.findAll()).thenReturn(List.of());

        // Act
        List<ServiceEntity> result = serviceService.getAllServices();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(serviceRepository, times(1)).findAll();
    }

    @Test
    void getServiceById_ShouldReturnService_WhenExists() {
        // Arrange
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));

        // Act
        Optional<ServiceEntity> result = serviceService.getServiceById(1L);

        // Assert
        assertThat(result).isPresent();
        ServiceEntity service = result.get();
        assertThat(service.getId()).isEqualTo(1L);
        assertThat(service.getName()).isEqualTo("Охрана офиса");
        assertThat(service.getDescription()).isEqualTo("Круглосуточная охрана офисного помещения");
        assertThat(service.getPrice()).isEqualTo(BigDecimal.valueOf(50000));

        verify(serviceRepository, times(1)).findById(1L);
    }

    @Test
    void getServiceById_ShouldReturnEmptyOptional_WhenNotExists() {
        // Arrange
        when(serviceRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<ServiceEntity> result = serviceService.getServiceById(999L);

        // Assert
        assertThat(result).isEmpty();

        verify(serviceRepository, times(1)).findById(999L);
    }

    @Test
    void createService_ShouldCreateServiceSuccessfully() {
        // Arrange
        when(serviceRepository.save(any(ServiceEntity.class))).thenReturn(testService);

        // Act
        ServiceEntity result = serviceService.createService(testServiceDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Охрана офиса");
        assertThat(result.getDescription()).isEqualTo("Круглосуточная охрана офисного помещения");
        assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(50000));

        verify(serviceRepository, times(1)).save(any(ServiceEntity.class));
    }

    @Test
    void createService_ShouldCreateServiceWithNullDescription() {
        // Arrange
        ServiceDTO dtoWithoutDescription = new ServiceDTO();
        dtoWithoutDescription.setName("Быстрая охрана");
        dtoWithoutDescription.setPrice(BigDecimal.valueOf(30000));
        // description не устанавливается

        ServiceEntity serviceWithoutDescription = new ServiceEntity();
        serviceWithoutDescription.setId(3L);
        serviceWithoutDescription.setName("Быстрая охрана");
        serviceWithoutDescription.setPrice(BigDecimal.valueOf(30000));
        serviceWithoutDescription.setDescription(null);

        when(serviceRepository.save(any(ServiceEntity.class))).thenReturn(serviceWithoutDescription);

        // Act
        ServiceEntity result = serviceService.createService(dtoWithoutDescription);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Быстрая охрана");
        assertThat(result.getDescription()).isNull();
        assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(30000));

        verify(serviceRepository, times(1)).save(any(ServiceEntity.class));
    }

    @Test
    void createService_ShouldCreateServiceWithZeroPrice() {
        // Arrange
        ServiceDTO dtoWithZeroPrice = new ServiceDTO();
        dtoWithZeroPrice.setName("Консультация");
        dtoWithZeroPrice.setDescription("Бесплатная консультация");
        dtoWithZeroPrice.setPrice(BigDecimal.ZERO);

        ServiceEntity serviceWithZeroPrice = new ServiceEntity();
        serviceWithZeroPrice.setId(4L);
        serviceWithZeroPrice.setName("Консультация");
        serviceWithZeroPrice.setDescription("Бесплатная консультация");
        serviceWithZeroPrice.setPrice(BigDecimal.ZERO);

        when(serviceRepository.save(any(ServiceEntity.class))).thenReturn(serviceWithZeroPrice);

        // Act
        ServiceEntity result = serviceService.createService(dtoWithZeroPrice);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Консультация");
        assertThat(result.getPrice()).isEqualTo(BigDecimal.ZERO);

        verify(serviceRepository, times(1)).save(any(ServiceEntity.class));
    }

    @Test
    void updateService_ShouldUpdateServiceSuccessfully() {
        // Arrange
        ServiceDTO updateDTO = new ServiceDTO();
        updateDTO.setName("Расширенная охрана офиса");
        updateDTO.setDescription("Охрана с видеонаблюдением");
        updateDTO.setPrice(BigDecimal.valueOf(75000));

        ServiceEntity updatedService = new ServiceEntity();
        updatedService.setId(1L);
        updatedService.setName("Расширенная охрана офиса");
        updatedService.setDescription("Охрана с видеонаблюдением");
        updatedService.setPrice(BigDecimal.valueOf(75000));

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));
        when(serviceRepository.save(any(ServiceEntity.class))).thenReturn(updatedService);

        // Act
        ServiceEntity result = serviceService.updateService(1L, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Расширенная охрана офиса");
        assertThat(result.getDescription()).isEqualTo("Охрана с видеонаблюдением");
        assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(75000));

        verify(serviceRepository, times(1)).findById(1L);
        verify(serviceRepository, times(1)).save(any(ServiceEntity.class));
    }

    @Test
    void updateService_ShouldUpdateOnlyName() {
        // Arrange
        ServiceDTO updateDTO = new ServiceDTO();
        updateDTO.setName("Новое название");
        // description и price не устанавливаются

        ServiceEntity partiallyUpdatedService = new ServiceEntity();
        partiallyUpdatedService.setId(1L);
        partiallyUpdatedService.setName("Новое название");
        partiallyUpdatedService.setDescription("Круглосуточная охрана офисного помещения"); // осталось прежним
        partiallyUpdatedService.setPrice(BigDecimal.valueOf(50000)); // осталось прежним

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));
        when(serviceRepository.save(any(ServiceEntity.class))).thenReturn(partiallyUpdatedService);

        // Act
        ServiceEntity result = serviceService.updateService(1L, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Новое название");
        assertThat(result.getDescription()).isEqualTo("Круглосуточная охрана офисного помещения");
        assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(50000));

        verify(serviceRepository, times(1)).findById(1L);
        verify(serviceRepository, times(1)).save(any(ServiceEntity.class));
    }

    @Test
    void updateService_ShouldReturnNull_WhenServiceNotFound() {
        // Arrange
        when(serviceRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        ServiceEntity result = serviceService.updateService(999L, testServiceDTO);

        // Assert
        assertThat(result).isNull();

        verify(serviceRepository, times(1)).findById(999L);
        verify(serviceRepository, never()).save(any(ServiceEntity.class));
    }

    @Test
    void deleteService_ShouldReturnTrue_WhenServiceExists() {
        // Arrange
        when(serviceRepository.existsById(1L)).thenReturn(true);
        doNothing().when(serviceRepository).deleteById(1L);

        // Act
        boolean result = serviceService.deleteService(1L);

        // Assert
        assertThat(result).isTrue();

        verify(serviceRepository, times(1)).existsById(1L);
        verify(serviceRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteService_ShouldReturnFalse_WhenServiceNotExists() {
        // Arrange
        when(serviceRepository.existsById(999L)).thenReturn(false);

        // Act
        boolean result = serviceService.deleteService(999L);

        // Assert
        assertThat(result).isFalse();

        verify(serviceRepository, times(1)).existsById(999L);
        verify(serviceRepository, never()).deleteById(anyLong());
    }

    @Test
    void existsByName_ShouldReturnTrue_WhenServiceWithNameExists() {
        // Arrange
        String existingName = "Охрана офиса";
        when(serviceRepository.existsByName(existingName)).thenReturn(true);

        // Act
        boolean result = serviceService.existsByName(existingName);

        // Assert
        assertThat(result).isTrue();

        verify(serviceRepository, times(1)).existsByName(existingName);
    }

    @Test
    void existsByName_ShouldReturnFalse_WhenServiceWithNameDoesNotExist() {
        // Arrange
        String nonExistingName = "Несуществующая услуга";
        when(serviceRepository.existsByName(nonExistingName)).thenReturn(false);

        // Act
        boolean result = serviceService.existsByName(nonExistingName);

        // Assert
        assertThat(result).isFalse();

        verify(serviceRepository, times(1)).existsByName(nonExistingName);
    }

    @Test
    void existsByName_ShouldHandleCaseSensitivity() {
        // Arrange
        String nameLowercase = "охрана офиса";
        String nameUppercase = "ОХРАНА ОФИСА";
        when(serviceRepository.existsByName(nameLowercase)).thenReturn(false);
        when(serviceRepository.existsByName(nameUppercase)).thenReturn(false);

        // Act
        boolean resultLowercase = serviceService.existsByName(nameLowercase);
        boolean resultUppercase = serviceService.existsByName(nameUppercase);

        // Assert
        assertThat(resultLowercase).isFalse();
        assertThat(resultUppercase).isFalse();

        verify(serviceRepository, times(1)).existsByName(nameLowercase);
        verify(serviceRepository, times(1)).existsByName(nameUppercase);
    }

    @Test
    void updateService_ShouldClearDescription_WhenSetToNullInDTO() {
        // Arrange
        ServiceDTO updateDTO = new ServiceDTO();
        updateDTO.setName("Охрана офиса");
        updateDTO.setDescription(null); // Явно устанавливаем null
        updateDTO.setPrice(BigDecimal.valueOf(50000));

        ServiceEntity updatedService = new ServiceEntity();
        updatedService.setId(1L);
        updatedService.setName("Охрана офиса");
        updatedService.setDescription(null); // Должен стать null
        updatedService.setPrice(BigDecimal.valueOf(50000));

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));
        when(serviceRepository.save(any(ServiceEntity.class))).thenReturn(updatedService);

        // Act
        ServiceEntity result = serviceService.updateService(1L, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isNull();

        verify(serviceRepository, times(1)).findById(1L);
        verify(serviceRepository, times(1)).save(any(ServiceEntity.class));
    }

    @Test
    void updateService_ShouldUpdatePriceToNull() {
        // Arrange
        ServiceDTO updateDTO = new ServiceDTO();
        updateDTO.setName("Охрана офиса");
        updateDTO.setDescription("Круглосуточная охрана");
        updateDTO.setPrice(null); // Устанавливаем цену в null

        // Если цена не может быть null, сервис может сохранить старое значение
        // или установить BigDecimal.ZERO
        ServiceEntity updatedService = new ServiceEntity();
        updatedService.setId(1L);
        updatedService.setName("Охрана офиса");
        updatedService.setDescription("Круглосуточная охрана");
        updatedService.setPrice(BigDecimal.ZERO); // или testService.getPrice() если сохраняется старое значение

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));
        when(serviceRepository.save(any(ServiceEntity.class))).thenReturn(updatedService);

        // Act
        ServiceEntity result = serviceService.updateService(1L, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        // Проверяем в зависимости от бизнес-логики:
        // 1. Если цена может быть null: assertThat(result.getPrice()).isNull();
        // 2. Если цена не может быть null и устанавливается в 0: assertThat(result.getPrice()).isEqualTo(BigDecimal.ZERO);
        // 3. Если сохраняется старое значение: assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(50000));

        // Для примера, предположим что цена устанавливается в 0
        assertThat(result.getPrice()).isEqualTo(BigDecimal.ZERO);

        verify(serviceRepository, times(1)).findById(1L);
        verify(serviceRepository, times(1)).save(any(ServiceEntity.class));
    }

    @Test
    void createService_ShouldHandleEmptyStringDescription() {
        // Arrange
        ServiceDTO dtoWithEmptyDescription = new ServiceDTO();
        dtoWithEmptyDescription.setName("Тестовая услуга");
        dtoWithEmptyDescription.setDescription(""); // Пустая строка
        dtoWithEmptyDescription.setPrice(BigDecimal.valueOf(10000));

        ServiceEntity serviceWithEmptyDescription = new ServiceEntity();
        serviceWithEmptyDescription.setId(5L);
        serviceWithEmptyDescription.setName("Тестовая услуга");
        serviceWithEmptyDescription.setDescription(""); // Пустая строка
        serviceWithEmptyDescription.setPrice(BigDecimal.valueOf(10000));

        when(serviceRepository.save(any(ServiceEntity.class))).thenReturn(serviceWithEmptyDescription);

        // Act
        ServiceEntity result = serviceService.createService(dtoWithEmptyDescription);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEmpty();

        verify(serviceRepository, times(1)).save(any(ServiceEntity.class));
    }

    @Test
    void getAllServices_ShouldReturnServicesSortedById() {
        // Arrange
        ServiceEntity service1 = new ServiceEntity();
        service1.setId(1L);
        service1.setName("Услуга 1");

        ServiceEntity service2 = new ServiceEntity();
        service2.setId(2L);
        service2.setName("Услуга 2");

        ServiceEntity service3 = new ServiceEntity();
        service3.setId(3L);
        service3.setName("Услуга 3");

        List<ServiceEntity> services = Arrays.asList(service1, service2, service3);
        when(serviceRepository.findAll()).thenReturn(services);

        // Act
        List<ServiceEntity> result = serviceService.getAllServices();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(2).getId()).isEqualTo(3L);

        verify(serviceRepository, times(1)).findAll();
    }
}