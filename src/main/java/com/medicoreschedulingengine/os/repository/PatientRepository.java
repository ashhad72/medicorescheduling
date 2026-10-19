package com.medicoreschedulingengine.os.repository;

import com.medicoreschedulingengine.os.model.entity.Patient;
import com.medicoreschedulingengine.os.model.enums.DepartmentType;
import com.medicoreschedulingengine.os.model.enums.PatientStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    List<Patient> findByDepartmentId(Long departmentId);

    List<Patient> findByStatus(PatientStatus status);

    List<Patient> findByDepartmentIdOrderByArrivalTimeAsc(Long departmentId);

    @Query("SELECT p FROM Patient p WHERE p.department.departmentType = :type ORDER BY p.arrivalTime ASC")
    List<Patient> findByDepartmentTypeOrderByArrivalTime(@Param("type") DepartmentType type);

    @Query("SELECT p FROM Patient p WHERE p.department.departmentType = :type ORDER BY p.priority ASC, p.arrivalTime ASC")
    List<Patient> findByDepartmentTypeOrderByPriority(@Param("type") DepartmentType type);
}