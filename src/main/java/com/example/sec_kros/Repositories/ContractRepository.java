package com.example.sec_kros.Repositories;

import com.example.sec_kros.Entities.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    List<Contract> findByClientId(Long clientId);
    List<Contract> findByStatus(String status);
    List<Contract> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}