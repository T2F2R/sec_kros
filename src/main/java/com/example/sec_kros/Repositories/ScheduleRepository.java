package com.example.sec_kros.Repositories;

import com.example.sec_kros.Entities.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByEmployeeId(Long employeeId);
    List<Schedule> findByGuardObjectId(Long guardObjectId);
    @Query("SELECT COUNT(s) > 0 FROM Schedule s WHERE s.guardObject.id = :guardObjectId")
    boolean existsByGuardObjectId(@Param("guardObjectId") Long guardObjectId);
    List<Schedule> findByGuardObject_Contract_Id(Long contractId);
}