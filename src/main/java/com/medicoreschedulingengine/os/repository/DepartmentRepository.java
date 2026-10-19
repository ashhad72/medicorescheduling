package com.medicoreschedulingengine.os.repository;

import com.medicoreschedulingengine.os.model.entity.Department;
import com.medicoreschedulingengine.os.model.enums.DepartmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByDepartmentType(DepartmentType departmentType);

    boolean existsByDepartmentType(DepartmentType departmentType);
}