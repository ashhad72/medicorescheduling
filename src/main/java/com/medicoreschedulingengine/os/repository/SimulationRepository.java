package com.medicoreschedulingengine.os.repository;

import com.medicoreschedulingengine.os.model.entity.SimulationRun;
import com.medicoreschedulingengine.os.model.enums.DepartmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimulationRepository extends JpaRepository<SimulationRun, Long> {

    List<SimulationRun> findByDepartmentTypeOrderByRunAtDesc(DepartmentType departmentType);

    List<SimulationRun> findByAlgorithmNameOrderByRunAtDesc(String algorithmName);

    List<SimulationRun> findByDepartmentTypeAndAlgorithmName(DepartmentType departmentType, String algorithmName);
}