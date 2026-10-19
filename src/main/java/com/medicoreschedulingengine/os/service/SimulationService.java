package com.medicoreschedulingengine.os.service;

import com.medicoreschedulingengine.os.model.dto.AlgorithmResultDTO;
import com.medicoreschedulingengine.os.model.dto.ComparisonResponseDTO;
import com.medicoreschedulingengine.os.model.dto.SimulationResponseDTO;
import com.medicoreschedulingengine.os.model.entity.AlgorithmComparison;
import com.medicoreschedulingengine.os.model.entity.ExecutionLog;
import com.medicoreschedulingengine.os.model.entity.Patient;
import com.medicoreschedulingengine.os.model.entity.SimulationRun;
import com.medicoreschedulingengine.os.model.enums.DepartmentType;
import com.medicoreschedulingengine.os.repository.AlgorithmComparisonRepository;
import com.medicoreschedulingengine.os.repository.ExecutionLogRepository;
import com.medicoreschedulingengine.os.repository.SimulationRepository;
import com.medicoreschedulingengine.os.scheduler.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * SimulationService
 *
 * The central orchestration service for MediCore.
 *
 * Responsibilities:
 *   1. Receives a simulation request for a department
 *   2. Loads patients for that department
 *   3. Clones the patient list for each algorithm
 *   4. Runs both assigned algorithms
 *   5. Verifies metrics via MetricsService
 *   6. Persists SimulationRun + ExecutionLog records
 *   7. Saves the AlgorithmComparison result
 *   8. Returns a SimulationResponseDTO to the controller
 *
 * Department → Algorithm Mapping:
 *   ER      → PrioritySchedulingStrategy  vs MultilevelQueueStrategy
 *   OPD     → FCFSStrategy               vs RoundRobinStrategy
 *   ICU     → SRTFStrategy               vs EDFStrategy
 *   SURGERY → SJFStrategy               vs HRRNStrategy
 */
@Service
@Transactional
public class SimulationService {

    private final PatientService                patientService;
    private final DepartmentService             departmentService;
    private final MetricsService                metricsService;

    private final SimulationRepository          simulationRepository;
    private final ExecutionLogRepository        executionLogRepository;
    private final AlgorithmComparisonRepository comparisonRepository;

    // ── Scheduling algorithm instances ────────────────────────
    private final FCFSStrategy                  fcfsStrategy;
    private final RoundRobinStrategy            roundRobinStrategy;
    private final SJFStrategy                   sjfStrategy;
    private final SRTFStrategy                  srtfStrategy;
    private final PrioritySchedulingStrategy    priorityStrategy;
    private final MultilevelQueueStrategy       mlqStrategy;
    private final HRRNStrategy                  hrrnStrategy;
    private final EDFStrategy                   edfStrategy;

    public SimulationService(PatientService patientService,
                             DepartmentService departmentService,
                             MetricsService metricsService,
                             SimulationRepository simulationRepository,
                             ExecutionLogRepository executionLogRepository,
                             AlgorithmComparisonRepository comparisonRepository,
                             FCFSStrategy fcfsStrategy,
                             RoundRobinStrategy roundRobinStrategy,
                             SJFStrategy sjfStrategy,
                             SRTFStrategy srtfStrategy,
                             PrioritySchedulingStrategy priorityStrategy,
                             MultilevelQueueStrategy mlqStrategy,
                             HRRNStrategy hrrnStrategy,
                             EDFStrategy edfStrategy) {

        this.patientService         = patientService;
        this.departmentService      = departmentService;
        this.metricsService         = metricsService;
        this.simulationRepository   = simulationRepository;
        this.executionLogRepository = executionLogRepository;
        this.comparisonRepository   = comparisonRepository;
        this.fcfsStrategy           = fcfsStrategy;
        this.roundRobinStrategy     = roundRobinStrategy;
        this.sjfStrategy            = sjfStrategy;
        this.srtfStrategy           = srtfStrategy;
        this.priorityStrategy       = priorityStrategy;
        this.mlqStrategy            = mlqStrategy;
        this.hrrnStrategy           = hrrnStrategy;
        this.edfStrategy            = edfStrategy;
    }

    // ── Main Simulation Entry Point ───────────────────────────

    /**
     * Runs a full simulation for the given department.
     * Executes both assigned algorithms, saves results, and returns
     * a complete SimulationResponseDTO with comparison and Gantt data.
     *
     * @param departmentType  Department to simulate (ER, OPD, ICU, SURGERY)
     * @return                Full simulation response with metrics and Gantt charts
     * @throws IllegalStateException if the department has no patients
     */
    public SimulationResponseDTO runSimulation(DepartmentType departmentType) {

        // ── Step 1: Validate department has patients ──────────
        if (!patientService.departmentHasPatients(departmentType)) {
            throw new IllegalStateException(
                    "No patients found in department: " + departmentType
                            + ". Please add patients before running a simulation.");
        }

        // ── Step 2: Load original patient list ────────────────
        List<Patient> originalPatients =
                patientService.getRawPatientsByDepartment(departmentType);

        // ── Step 3: Get the two algorithms for this department ─
        SchedulingStrategy[] algorithms = getAlgorithmsForDepartment(departmentType);
        SchedulingStrategy strategy1 = algorithms[0];
        SchedulingStrategy strategy2 = algorithms[1];

        // ── Step 4: Clone patients for each algorithm ─────────
        // CRITICAL: each algorithm must receive an independent copy
        // so mutations (remainingTime, status) don't bleed across runs
        List<Patient> clonedForAlgo1 = clonePatients(originalPatients);
        List<Patient> clonedForAlgo2 = clonePatients(originalPatients);

        // ── Step 5: Execute both algorithms ───────────────────
        AlgorithmResultDTO result1 = strategy1.execute(clonedForAlgo1);
        AlgorithmResultDTO result2 = strategy2.execute(clonedForAlgo2);

        // Set department type on results
        result1.setDepartmentType(departmentType);
        result2.setDepartmentType(departmentType);

        // ── Step 6: Verify and recalculate metrics ────────────
        result1 = metricsService.verifyAndRecalculateMetrics(result1);
        result2 = metricsService.verifyAndRecalculateMetrics(result2);

        // ── Step 7: Persist SimulationRun entities ────────────
        SimulationRun run1 = metricsService.buildSimulationRun(result1);
        SimulationRun run2 = metricsService.buildSimulationRun(result2);

        run1 = simulationRepository.save(run1);
        run2 = simulationRepository.save(run2);

        result1.setSimulationRunId(run1.getId());
        result2.setSimulationRunId(run2.getId());

        // ── Step 8: Persist ExecutionLog entities ─────────────
        List<ExecutionLog> logs1 =
                metricsService.buildExecutionLogs(result1, run1, originalPatients);
        List<ExecutionLog> logs2 =
                metricsService.buildExecutionLogs(result2, run2, originalPatients);

        executionLogRepository.saveAll(logs1);
        executionLogRepository.saveAll(logs2);

        // ── Step 9: Save AlgorithmComparison ──────────────────
        AlgorithmComparison comparison = buildAndSaveComparison(result1, result2, run1, run2, departmentType);

        // ── Step 10: Build and return response ────────────────
        ComparisonResponseDTO comparisonDTO = new ComparisonResponseDTO(
                departmentType, result1, result2);
        comparisonDTO.setComparisonId(comparison.getId());

        String deptName = departmentService.getDepartmentByType(departmentType).getName();

        metricsService.printMetricsSummary(result1);
        metricsService.printMetricsSummary(result2);

        return new SimulationResponseDTO(departmentType, deptName, comparisonDTO);
    }

    // ── History Queries ───────────────────────────────────────

    /**
     * Returns all past simulation runs for a department, newest first.
     *
     * @param departmentType  Department to query
     * @return                List of SimulationRun entities
     */
    @Transactional(readOnly = true)
    public List<SimulationRun> getSimulationHistory(DepartmentType departmentType) {
        return simulationRepository
                .findByDepartmentTypeOrderByRunAtDesc(departmentType);
    }

    /**
     * Returns the 10 most recent comparison results across all departments.
     *
     * @return List of AlgorithmComparison entities
     */
    @Transactional(readOnly = true)
    public List<AlgorithmComparison> getRecentComparisons() {
        return comparisonRepository.findTop10ByOrderByComparedAtDesc();
    }

    // ── Internal Helpers ──────────────────────────────────────

    /**
     * Returns the two SchedulingStrategy instances assigned to a department.
     *
     * @param departmentType  The department
     * @return                Array of exactly two strategies
     */
    private SchedulingStrategy[] getAlgorithmsForDepartment(DepartmentType departmentType) {
        return switch (departmentType) {
            case ER      -> new SchedulingStrategy[]{ priorityStrategy,  mlqStrategy      };
            case OPD     -> new SchedulingStrategy[]{ fcfsStrategy,      roundRobinStrategy };
            case ICU     -> new SchedulingStrategy[]{ srtfStrategy,      edfStrategy      };
            case SURGERY -> new SchedulingStrategy[]{ sjfStrategy,       hrrnStrategy     };
        };
    }

    /**
     * Creates a deep clone of a patient list.
     *
     * Each cloned patient has the same field values as the original
     * but is a new independent object. This is critical because
     * scheduling algorithms mutate remainingTime and status during execution.
     *
     * Note: Cloned patients share the same IDs as originals so that
     * Gantt entries and metric lookups can reference the correct patient.
     *
     * @param originals  Original patient list from the database
     * @return           Independent cloned list
     */
    private List<Patient> clonePatients(List<Patient> originals) {
        List<Patient> clones = new ArrayList<>();
        for (Patient original : originals) {
            Patient clone = new Patient();
            clone.setId(original.getId());
            clone.setName(original.getName());
            clone.setArrivalTime(original.getArrivalTime());
            clone.setBurstTime(original.getBurstTime());
            clone.setPriority(original.getPriority());
            clone.setRemainingTime(original.getBurstTime()); // always reset
            clone.setStatus(com.medicoreschedulingengine.os.model.enums.PatientStatus.WAITING);
            clone.setDepartment(original.getDepartment());
            clones.add(clone);
        }
        return clones;
    }

    /**
     * Builds and persists an AlgorithmComparison entity from two results.
     *
     * @return  Saved AlgorithmComparison entity
     */
    private AlgorithmComparison buildAndSaveComparison(AlgorithmResultDTO r1,
                                                       AlgorithmResultDTO r2,
                                                       SimulationRun run1,
                                                       SimulationRun run2,
                                                       DepartmentType departmentType) {
        AlgorithmComparison comparison = new AlgorithmComparison(run1, run2, departmentType);

        comparison.setWinnerWaitingTime(metricsService.compareWaitingTime(r1, r2));
        comparison.setWinnerTurnaroundTime(metricsService.compareTurnaroundTime(r1, r2));
        comparison.setWinnerResponseTime(metricsService.compareResponseTime(r1, r2));
        comparison.setWinnerAlgorithm(metricsService.determineOverallWinner(r1, r2));

        return comparisonRepository.save(comparison);
    }
}