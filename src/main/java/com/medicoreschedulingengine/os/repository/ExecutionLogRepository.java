package com.medicoreschedulingengine.os.repository;

import com.medicoreschedulingengine.os.model.entity.ExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long> {

    List<ExecutionLog> findBySimulationRunIdOrderByStartTimeAsc(Long simulationRunId);

    List<ExecutionLog> findByPatientId(Long patientId);

    @Query("SELECT e FROM ExecutionLog e WHERE e.simulationRun.id = :runId ORDER BY e.startTime ASC")
    List<ExecutionLog> findGanttDataByRunId(@Param("runId") Long runId);
}