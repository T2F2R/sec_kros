package com.example.sec_kros.Services;

import com.example.sec_kros.DTO.ClientDTO;
import com.example.sec_kros.Entities.Client;
import com.example.sec_kros.Repositories.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PasswordService passwordService;

    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    public Optional<Client> getClientById(Long id) {
        return clientRepository.findById(id);
    }

    public Client createClient(ClientDTO clientDTO) {
        Client client = new Client();
        client.setLastName(clientDTO.getLastName());
        client.setFirstName(clientDTO.getFirstName());
        client.setPatronymic(clientDTO.getPatronymic());
        client.setPhone(clientDTO.getPhone());
        client.setEmail(clientDTO.getEmail());
        client.setAddress(clientDTO.getAddress());
        client.setPasswordHash(passwordService.hashPassword("default123"));
        client.setCreatedAt(LocalDateTime.now());

        return clientRepository.save(client);
    }

    public Client updateClient(Long id, ClientDTO clientDTO) {
        Optional<Client> existingClient = clientRepository.findById(id);
        if (existingClient.isPresent()) {
            Client client = existingClient.get();
            client.setLastName(clientDTO.getLastName());
            client.setFirstName(clientDTO.getFirstName());
            client.setPatronymic(clientDTO.getPatronymic());
            client.setPhone(clientDTO.getPhone());
            client.setEmail(clientDTO.getEmail());
            client.setAddress(clientDTO.getAddress());

            return clientRepository.save(client);
        }
        return null;
    }

    public boolean deleteClient(Long id) {
        if (clientRepository.existsById(id)) {
            clientRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean existsByEmail(String email) {
        return clientRepository.existsByEmail(email);
    }

    public Client findByEmail(String email) {
        return clientRepository.findByEmail(email).orElse(null);
    }
}