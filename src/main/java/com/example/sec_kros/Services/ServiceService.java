package com.example.sec_kros.Services;

import com.example.sec_kros.Entities.ServiceEntity;
import com.example.sec_kros.DTO.ServiceDTO;
import com.example.sec_kros.Repositories.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ServiceService {

    @Autowired
    private ServiceRepository serviceRepository;

    public List<ServiceEntity> getAllServices() {
        return serviceRepository.findAll();
    }

    public Optional<ServiceEntity> getServiceById(Long id) {
        return serviceRepository.findById(id);
    }

    public ServiceEntity createService(ServiceDTO serviceDTO) {
        ServiceEntity service = new ServiceEntity();
        service.setName(serviceDTO.getName());
        service.setDescription(serviceDTO.getDescription());
        service.setPrice(serviceDTO.getPrice());

        return serviceRepository.save(service);
    }

    public ServiceEntity updateService(Long id, ServiceDTO serviceDTO) {
        return serviceRepository.findById(id)
                .map(service -> {
                    service.setName(serviceDTO.getName());
                    service.setDescription(serviceDTO.getDescription());
                    service.setPrice(serviceDTO.getPrice());

                    return serviceRepository.save(service);
                })
                .orElse(null);
    }

    public boolean deleteService(Long id) {
        if (serviceRepository.existsById(id)) {
            serviceRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean existsByName(String name) {
        return serviceRepository.existsByName(name);
    }
}