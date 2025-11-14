package com.example.sec_kros.Repositories;

import com.example.sec_kros.Entities.GuardObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GuardObjectRepository extends JpaRepository<GuardObject, Long> {
    List<GuardObject> findByClientId(Long clientId);
    List<GuardObject> findByContractId(Long contractId);
    boolean existsByNameAndClientId(String name, Long clientId);
    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END FROM GuardObject g WHERE g.contract.id = :contractId")
    boolean existsByContractId(@Param("contractId") Long contractId);
    // Проверяем, есть ли расписания для объекта охраны
    @Query("SELECT COUNT(s) > 0 FROM Schedule s WHERE s.guardObject.id = :guardObjectId")
    boolean hasSchedules(@Param("guardObjectId") Long guardObjectId);
}