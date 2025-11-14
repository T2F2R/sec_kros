package com.example.sec_kros.Repositories;

import com.example.sec_kros.Entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Client> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}