package com.medicoreschedulingengine.os.simulation;

import com.medicoreschedulingengine.os.model.dto.AlgorithmResultDTO;
import com.medicoreschedulingengine.os.model.dto.ComparisonResponseDTO;
import com.medicoreschedulingengine.os.model.dto.SimulationResponseDTO;
import com.medicoreschedulingengine.os.model.entity.AlgorithmComparison;
import com.medicoreschedulingengine.os.model.entity.Patient;
import com.medicoreschedulingengine.os.model.entity.SimulationRun;
import com.medicoreschedulingengine.os.model.enums.DepartmentType;
import com.medicoreschedulingengine.os.repository.AlgorithmComparisonRepository;
import com.medicoreschedulingengine.os.repository.SimulationRepository;
import com.medicoreschedulingengine.os.scheduler.*;
import com.medicoreschedulingengine.os.service.DepartmentService;
import com.medicoreschedulingengine.os.service.MetricsService;
import com.medicoreschedulingengine.os.service.PatientService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * DepartmentSimulationEngine
 *
 * The top-level orchestrator for the MediCore simulation system.
 *
 * Flow for each department simulation:
 *   1. Validate department has patients
 *   2. Load original patients from DB
 *   3. Clone patients twice (one copy per algorithm)
 *   4. Run Algorithm 1 via SimulationRunner
 *   5. Run Algorithm 2 via SimulationRunner
 *   6. Compare results via MetricsService
 *   7. Persist AlgorithmComparison to DB
 *   8. Build and return SimulationResponseDTO
 *
 * Department → Algorithm Assignment:
 *   ER      → PrioritySchedulingStrategy  vs MultilevelQueueStrategy
 *   OPD     → FCFSStrategy               vs RoundRobinStrategy
 *   ICU     → SRTFStrategy               vs EDFStrategy
 *   SURGERY → SJFStrategy               vs HRRNStrategy
 */
@Component
@Transactional
public class DepartmentSimulationEngine {

    private final PatientService                patientService;
    private final DepartmentService             departmentService;
    private final MetricsService                metricsService;
    private final SimulationRunner              simulationRunner;
    private final AlgorithmComparisonRepository comparisonRepository;
    private final SimulationRepository          simulationRepository;

    // ── Algorithm instances ───────────────────────────────────
    private final FCFSStrategy               fcfsStrategy;
    private final RoundRobinStrategy         roundRobinStrategy;
    private final SJFStrategy                sjfStrategy;
    private final SRTFStrategy               srtfStrategy;
    private final PrioritySchedulingStrategy priorityStrategy;
    private final MultilevelQueueStrategy    mlqStrategy;
    private final HRRNStrategy               hrrnStrategy;
    private final EDFStrategy               edfStrategy;

    public DepartmentSimulationEngine(
            PatientService patientService,
            DepartmentService departmentService,
            MetricsService metricsService,
            SimulationRunner simulationRunner,
            AlgorithmComparisonRepository comparisonRepository,
            SimulationRepository simulationRepository,
            FCFSStrategy fcfsStrategy,
            RoundRobinStrategy roundRobinStrategy,
            SJFStrategy sjfStrategy,
            SRTFStrategy srtfStrategy,
            PrioritySchedulingStrategy priorityStrategy,
            MultilevelQueueStrategy mlqStrategy,
            HRRNStrategy hrrnStrategy,
            EDFStrategy edfStrategy) {

        this.patientService      = patientService;
        this.departmentService   = departmentService;
        this.metricsService      = metricsService;
        this.simulationRunner    = simulationRunner;
        this.comparisonRepository = comparisonRepository;
        this.simulationRepository = simulationRepository;
        this.fcfsStrategy        = fcfsStrategy;
        this.roundRobinStrategy  = roundRobinStrategy;
        this.sjfStrategy         = sjfStrategy;
        this.srtfStrategy        = srtfStrategy;
        this.priorityStrategy    = priorityStrategy;
        this.mlqStrategy         = mlqStrategy;
        this.hrrnStrategy        = hrrnStrategy;
        this.edfStrategy         = edfStrategy;
    }

    // ── Main Entry Point ──────────────────────────────────────

    /**
     * Runs a full department simulation — both algorithms, comparison,
     * DB persistence, and full response construction.
     *
     * @param departmentType  The department to simulate
     * @return                SimulationResponseDTO with full results
     * @throws IllegalStateException if department has no patients
     */
    public SimulationResponseDTO simulate(DepartmentType departmentType) {

        System.out.println("\n========================================");
        System.out.println("[Engine] Simulation started: " + departmentType);
        System.out.println("========================================");

        // ── Step 1: Validate ──────────────────────────────────
        if (!patientService.departmentHasPatients(departmentType)) {
            throw new IllegalStateException(
                    "No patients found in department: " + departmentType
                            + ". Please add patients before running a simulation.");
        }

        // ── Step 2: Load original patients ───────────────────
        List<Patient> originalPatients =
                patientService.getRawPatientsByDepartment(departmentType);

        System.out.println("[Engine] Loaded " + originalPatients.size()
                + " patients for " + departmentType);

        // ── Step 3: Get assigned algorithms ───────────────────
        SchedulingStrategy[] algorithms = getAlgorithmsForDepartment(departmentType);
        SchedulingStrategy strategy1    = algorithms[0];
        SchedulingStrategy strategy2    = algorithms[1];

        System.out.println("[Engine] Algorithms: "
                + strategy1.getAlgorithmName()
                + " vs "
                + strategy2.getAlgorithmName());

        // ── Step 4: Clone patients for each algorithm ─────────
        List<Patient> clone1 = clonePatients(originalPatients);
        List<Patient> clone2 = clonePatients(originalPatients);

        // ── Step 5: Run both algorithms via SimulationRunner ──
        AlgorithmResultDTO result1 = simulationRunner.run(
                strategy1, clone1, originalPatients, departmentType);

        AlgorithmResultDTO result2 = simulationRunner.run(
                strategy2, clone2, originalPatients, departmentType);

        // ── Step 6: Compare results ───────────────────────────
        AlgorithmComparison comparison = buildAndSaveComparison(
                result1, result2, departmentType);

        System.out.println("[Engine] Winner: " + comparison.getWinnerAlgorithm());

        // ── Step 7: Build response ────────────────────────────
        ComparisonResponseDTO comparisonDTO =
                new ComparisonResponseDTO(departmentType, result1, result2);
        comparisonDTO.setComparisonId(comparison.getId());

        String deptName = departmentService
                .getDepartmentByType(departmentType).getName();

        System.out.println("[Engine] Simulation complete: " + departmentType);
        System.out.println("========================================\n");

        return new SimulationResponseDTO(departmentType, deptName, comparisonDTO);
    }

    // ── Dry Run (no DB persistence) ───────────────────────────

    /**
     * Runs both algorithms without saving to the database.
     * Used for previewing results before the user confirms saving.
     *
     * @param departmentType  The department to simulate
     * @return                SimulationResponseDTO with full results (no DB IDs)
     */
    public SimulationResponseDTO drySimulate(DepartmentType departmentType) {

        if (!patientService.departmentHasPatients(departmentType)) {
            throw new IllegalStateException(
                    "No patients found in department: " + departmentType);
        }

        List<Patient> originalPatients =
                patientService.getRawPatientsByDepartment(departmentType);

        SchedulingStrategy[] algorithms = getAlgorithmsForDepartment(departmentType);

        AlgorithmResultDTO result1 = simulationRunner.dryRun(
                algorithms[0], clonePatients(originalPatients), departmentType);

        AlgorithmResultDTO result2 = simulationRunner.dryRun(
                algorithms[1], clonePatients(originalPatients), departmentType);

        ComparisonResponseDTO comparisonDTO =
                new ComparisonResponseDTO(departmentType, result1, result2);

        String deptName = departmentService
                .getDepartmentByType(departmentType).getName();

        return new SimulationResponseDTO(departmentType, deptName, comparisonDTO);
    }

    // ── History ───────────────────────────────────────────────

    /**
     * Fetches all past simulation runs for a department from the DB.
     *
     * @param departmentType  Department to query
     * @return                List of SimulationRun entities newest first
     */
    @Transactional(readOnly = true)
    public List<SimulationRun> getSimulationHistory(DepartmentType departmentType) {
        return simulationRepository
                .findByDepartmentTypeOrderByRunAtDesc(departmentType);
    }

    /**
     * Fetches the 10 most recent comparisons across all departments.
     *
     * @return List of AlgorithmComparison entities
     */
    @Transactional(readOnly = true)
    public List<AlgorithmComparison> getRecentComparisons() {
        return comparisonRepository.findTop10ByOrderByComparedAtDesc();
    }

    // ── Internal Helpers ──────────────────────────────────────

    /**
     * Returns the two SchedulingStrategy instances for a department.
     *
     * @param departmentType  The department
     * @return                Array of exactly two strategies [algo1, algo2]
     */
    private SchedulingStrategy[] getAlgorithmsForDepartment(DepartmentType departmentType) {
        if (departmentType == DepartmentType.ER) {
            return new SchedulingStrategy[]{ priorityStrategy, mlqStrategy };
        } else if (departmentType == DepartmentType.OPD) {
            return new SchedulingStrategy[]{ fcfsStrategy, roundRobinStrategy };
        } else if (departmentType == DepartmentType.ICU) {
            return new SchedulingStrategy[]{ srtfStrategy, edfStrategy };
        } else if (departmentType == DepartmentType.SURGERY) {
            return new SchedulingStrategy[]{ sjfStrategy, hrrnStrategy };
        } else {
            throw new IllegalArgumentException(
                    "Unknown department type: " + departmentType);
        }
    }

    /**
     * Creates a deep clone of a patient list so each algorithm
     * receives an independent copy with no shared mutable state.
     *
     * Cloned patients retain the same IDs as originals so Gantt
     * entries and metric lookups can reference the correct patient.
     *
     * @param originals  Original patient list from DB
     * @return           Independent cloned list
     */
    private List<Patient> clonePatients(List<Patient> originals) {
        List<Patient> clones = new ArrayList<>();
        for (Patient p : originals) {
            Patient clone = new Patient();
            clone.setId(p.getId());
            clone.setName(p.getName());
            clone.setArrivalTime(p.getArrivalTime());
            clone.setBurstTime(p.getBurstTime());
            clone.setPriority(p.getPriority());
            clone.setRemainingTime(p.getBurstTime()); // always reset to full burst time
            clone.setStatus(com.medicoreschedulingengine.os.model.enums.PatientStatus.WAITING);
            clone.setDepartment(p.getDepartment());
            clones.add(clone);
        }
        return clones;
    }

    /**
     * Builds, populates, and persists an AlgorithmComparison entity
     * from two completed algorithm results.
     *
     * @return  Saved AlgorithmComparison entity with DB ID set
     */
    private AlgorithmComparison buildAndSaveComparison(AlgorithmResultDTO r1,
                                                       AlgorithmResultDTO r2,
                                                       DepartmentType departmentType) {
        // Fetch the saved SimulationRun entities using their IDs
        SimulationRun run1 = simulationRepository.findById(r1.getSimulationRunId())
                .orElseThrow(() -> new IllegalStateException(
                        "SimulationRun not found: " + r1.getSimulationRunId()));

        SimulationRun run2 = simulationRepository.findById(r2.getSimulationRunId())
                .orElseThrow(() -> new IllegalStateException(
                        "SimulationRun not found: " + r2.getSimulationRunId()));

        AlgorithmComparison comparison = new AlgorithmComparison(run1, run2, departmentType);
        comparison.setWinnerWaitingTime(metricsService.compareWaitingTime(r1, r2));
        comparison.setWinnerTurnaroundTime(metricsService.compareTurnaroundTime(r1, r2));
        comparison.setWinnerResponseTime(metricsService.compareResponseTime(r1, r2));
        comparison.setWinnerAlgorithm(metricsService.determineOverallWinner(r1, r2));

        return comparisonRepository.save(comparison);
    }
}