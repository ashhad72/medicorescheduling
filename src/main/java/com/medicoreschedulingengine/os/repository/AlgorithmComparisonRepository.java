package com.medicoreschedulingengine.os.repository;

import com.medicoreschedulingengine.os.model.entity.AlgorithmComparison;
import com.medicoreschedulingengine.os.model.enums.DepartmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlgorithmComparisonRepository extends JpaRepository<AlgorithmComparison, Long> {

    List<AlgorithmComparison> findByDepartmentTypeOrderByComparedAtDesc(DepartmentType departmentType);

    List<AlgorithmComparison> findTop10ByOrderByComparedAtDesc();
}