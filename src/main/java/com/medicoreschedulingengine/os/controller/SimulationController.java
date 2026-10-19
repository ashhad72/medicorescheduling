package com.medicoreschedulingengine.os.controller;

import com.medicoreschedulingengine.os.model.dto.SimulationResponseDTO;
import com.medicoreschedulingengine.os.model.entity.AlgorithmComparison;
import com.medicoreschedulingengine.os.model.entity.SimulationRun;
import com.medicoreschedulingengine.os.model.enums.DepartmentType;
import com.medicoreschedulingengine.os.simulation.DepartmentSimulationEngine;
import com.medicoreschedulingengine.os.simulation.GanttChartBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SimulationController
 *
 * REST endpoints for running simulations and retrieving results.
 *
 * Base URL: /api/simulation
 *
 * Endpoints:
 *   POST /api/simulation/run/{department}           → Run full simulation (saves to DB)
 *   POST /api/simulation/preview/{department}       → Dry run (no DB save)
 *   GET  /api/simulation/history/{department}       → Get past runs for a department
 *   GET  /api/simulation/history/recent             → Get 10 most recent comparisons
 *   GET  /api/simulation/gantt/{simulationRunId}    → Get Gantt chart data for a run
 */
@RestController
@RequestMapping("/api/simulation")
@CrossOrigin(origins = "*")
public class SimulationController {

    private final DepartmentSimulationEngine simulationEngine;
    private final GanttChartBuilder          ganttChartBuilder;

    public SimulationController(DepartmentSimulationEngine simulationEngine,
                                GanttChartBuilder ganttChartBuilder) {
        this.simulationEngine  = simulationEngine;
        this.ganttChartBuilder = ganttChartBuilder;
    }

    // ── POST /api/simulation/run/{department} ─────────────────

    /**
     * Runs a full simulation for the given department.
     * Executes both assigned algorithms, saves results to DB,
     * and returns the full comparison response.
     *
     * Example: POST /api/simulation/run/ER
     *
     * Response includes:
     *   - Both algorithm results with metrics
     *   - Per-patient metrics table
     *   - Gantt chart entries for both algorithms
     *   - Winner summary
     */
    @PostMapping("/run/{department}")
    public ResponseEntity<?> runSimulation(@PathVariable String department) {
        try {
            DepartmentType departmentType = DepartmentType.valueOf(department.toUpperCase());
            SimulationResponseDTO response = simulationEngine.simulate(departmentType);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body("Invalid department: " + department
                            + ". Valid values: ER, OPD, ICU, SURGERY");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Simulation failed: " + e.getMessage());
        }
    }

    // ── POST /api/simulation/preview/{department} ─────────────

    /**
     * Runs a dry simulation — executes both algorithms and returns
     * results WITHOUT saving anything to the database.
     *
     * Useful for frontend "preview" before the user decides to save.
     *
     * Example: POST /api/simulation/preview/OPD
     */
    @PostMapping("/preview/{department}")
    public ResponseEntity<?> previewSimulation(@PathVariable String department) {
        try {
            DepartmentType departmentType = DepartmentType.valueOf(department.toUpperCase());
            SimulationResponseDTO response = simulationEngine.drySimulate(departmentType);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body("Invalid department: " + department
                            + ". Valid values: ER, OPD, ICU, SURGERY");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Preview failed: " + e.getMessage());
        }
    }

    // ── GET /api/simulation/history/{department} ──────────────

    /**
     * Returns all past simulation runs for a department, newest first.
     *
     * Example: GET /api/simulation/history/SURGERY
     */
    @GetMapping("/history/{department}")
    public ResponseEntity<?> getSimulationHistory(@PathVariable String department) {
        try {
            DepartmentType departmentType = DepartmentType.valueOf(department.toUpperCase());
            List<SimulationRun> history =
                    simulationEngine.getSimulationHistory(departmentType);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body("Invalid department: " + department);
        }
    }

    // ── GET /api/simulation/history/recent ────────────────────

    /**
     * Returns the 10 most recent algorithm comparisons
     * across all departments.
     *
     * Used by the dashboard to show recent activity.
     */
    @GetMapping("/history/recent")
    public ResponseEntity<List<AlgorithmComparison>> getRecentComparisons() {
        return ResponseEntity.ok(simulationEngine.getRecentComparisons());
    }

    // ── GET /api/simulation/gantt/{simulationRunId} ───────────

    /**
     * Returns the Gantt chart data for a specific simulation run.
     * Used when viewing historical runs from the history page.
     *
     * Example: GET /api/simulation/gantt/42
     *
     * Response includes:
     *   - Ordered list of Gantt entries (patientId, name, start, end, duration)
     *   - Colour map (patientId -> hex colour)
     *   - Timeline start, end, and total duration
     */
    @GetMapping("/gantt/{simulationRunId}")
    public ResponseEntity<?> getGanttData(@PathVariable Long simulationRunId) {
        try {
            GanttChartBuilder.GanttChartData ganttData =
                    ganttChartBuilder.buildFromSimulationRunId(simulationRunId);
            return ResponseEntity.ok(ganttData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to build Gantt chart: " + e.getMessage());
        }
    }
}