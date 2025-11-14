package com.example.sec_kros.Repositories;

import com.example.sec_kros.Entities.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByLogin(String login);
    boolean existsByEmail(String email);
    boolean existsByLogin(String login);
    List<Employee> findByPositionContaining(String position);
    long countByPositionContaining(String position);
}