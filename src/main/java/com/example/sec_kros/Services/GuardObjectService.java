package com.example.sec_kros.Services;

import com.example.sec_kros.Entities.GuardObject;
import com.example.sec_kros.Entities.Client;
import com.example.sec_kros.Entities.Contract;
import com.example.sec_kros.DTO.GuardObjectDTO;
import com.example.sec_kros.Repositories.GuardObjectRepository;
import com.example.sec_kros.Repositories.ClientRepository;
import com.example.sec_kros.Repositories.ContractRepository;
import com.example.sec_kros.Repositories.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class GuardObjectService {

    @Autowired
    private GuardObjectRepository guardObjectRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    public List<GuardObject> getAllGuardObjects() {
        return guardObjectRepository.findAll();
    }

    public Optional<GuardObject> getGuardObjectById(Long id) {
        return guardObjectRepository.findById(id);
    }

    public GuardObject createGuardObject(GuardObjectDTO guardObjectDTO) {
        Client client = clientRepository.findById(guardObjectDTO.getClientId())
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));
        Contract contract = contractRepository.findById(guardObjectDTO.getContractId())
                .orElseThrow(() -> new RuntimeException("Договор не найден"));

        // Проверяем, что договор принадлежит клиенту
        if (!contract.getClient().getId().equals(client.getId())) {
            throw new RuntimeException("Договор не принадлежит выбранному клиенту");
        }

        GuardObject guardObject = new GuardObject();
        guardObject.setClient(client);
        guardObject.setContract(contract);
        guardObject.setName(guardObjectDTO.getName());
        guardObject.setAddress(guardObjectDTO.getAddress());
        guardObject.setLatitude(guardObjectDTO.getLatitude());
        guardObject.setLongitude(guardObjectDTO.getLongitude());
        guardObject.setDescription(guardObjectDTO.getDescription());

        return guardObjectRepository.save(guardObject);
    }

    public GuardObject updateGuardObject(Long id, GuardObjectDTO guardObjectDTO) {
        return guardObjectRepository.findById(id)
                .map(guardObject -> {
                    Client client = clientRepository.findById(guardObjectDTO.getClientId())
                            .orElseThrow(() -> new RuntimeException("Клиент не найден"));
                    Contract contract = contractRepository.findById(guardObjectDTO.getContractId())
                            .orElseThrow(() -> new RuntimeException("Договор не найден"));

                    // Проверяем, что договор принадлежит клиенту
                    if (!contract.getClient().getId().equals(client.getId())) {
                        throw new RuntimeException("Договор не принадлежит выбранному клиенту");
                    }

                    guardObject.setClient(client);
                    guardObject.setContract(contract);
                    guardObject.setName(guardObjectDTO.getName());
                    guardObject.setAddress(guardObjectDTO.getAddress());
                    guardObject.setLatitude(guardObjectDTO.getLatitude());
                    guardObject.setLongitude(guardObjectDTO.getLongitude());
                    guardObject.setDescription(guardObjectDTO.getDescription());

                    return guardObjectRepository.save(guardObject);
                })
                .orElse(null);
    }

    public boolean deleteGuardObject(Long id) {
        Optional<GuardObject> guardObject = guardObjectRepository.findById(id);
        if (guardObject.isPresent()) {
            // Проверяем, есть ли связанные расписания
            boolean hasSchedules = guardObjectRepository.hasSchedules(id);

            if (hasSchedules) {
                // Не удаляем объект, если есть связанные расписания
                return false;
            }

            // Если нет связанных записей - удаляем
            guardObjectRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Добавьте метод для проверки возможности удаления
    public boolean canDeleteGuardObject(Long id) {
        Optional<GuardObject> guardObject = guardObjectRepository.findById(id);
        if (guardObject.isPresent()) {
            boolean hasSchedules = guardObjectRepository.hasSchedules(id);
            return !hasSchedules;
        }
        return false;
    }

    public List<GuardObject> getGuardObjectsByClientId(Long clientId) {
        return guardObjectRepository.findByClientId(clientId);
    }

    public List<GuardObject> getGuardObjectsByContractId(Long contractId) {
        return guardObjectRepository.findByContractId(contractId);
    }
}