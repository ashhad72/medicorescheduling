package com.medicoreschedulingengine.os.service;

import com.medicoreschedulingengine.os.model.entity.Department;
import com.medicoreschedulingengine.os.model.enums.DepartmentType;
import com.medicoreschedulingengine.os.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * DepartmentService
 *
 * Handles all department-related business logic.
 * The four departments are seeded by the SQL schema, so this service
 * is primarily read-only — it fetches and validates departments.
 */
@Service
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    // ── Fetch ─────────────────────────────────────────────────

    /**
     * Returns all four departments.
     */
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    /**
     * Returns a department by its type.
     *
     * @param departmentType  ER, OPD, ICU, or SURGERY
     * @return                Department entity
     * @throws IllegalArgumentException if not found
     */
    public Department getDepartmentByType(DepartmentType departmentType) {
        return departmentRepository.findByDepartmentType(departmentType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Department not found: " + departmentType));
    }

    /**
     * Returns a department by its ID.
     *
     * @throws IllegalArgumentException if not found
     */
    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Department not found with id: " + id));
    }

    // ── Info Helpers ──────────────────────────────────────────

    /**
     * Returns the two algorithm names assigned to a department.
     * Used by the frontend to label the comparison results.
     *
     * @param departmentType  The department
     * @return                Array of two algorithm name strings
     */
    public String[] getAlgorithmsForDepartment(DepartmentType departmentType) {
        if (departmentType == DepartmentType.ER) {
            return new String[]{"PREEMPTIVE_PRIORITY", "MULTILEVEL_QUEUE"};
        } else if (departmentType == DepartmentType.OPD) {
            return new String[]{"FCFS", "ROUND_ROBIN"};
        } else if (departmentType == DepartmentType.ICU) {
            return new String[]{"SRTF", "EDF"};
        } else if (departmentType == DepartmentType.SURGERY) {
            return new String[]{"SJF", "HRRN"};
        } else {
            throw new IllegalArgumentException("Unknown department type: " + departmentType);
        }
    }

    /**
     * Returns a human-readable description of the scheduling approach
     * for a department — useful for the frontend info panel.
     *
     * @param departmentType  The department
     * @return                Description string
     */
    public String getDepartmentSchedulingInfo(DepartmentType departmentType) {
        String erInfo      = "Emergency Department uses Preemptive Priority and Multilevel Queue scheduling. Critical patients always interrupt ongoing treatment.";
        String opdInfo     = "Outpatient Department uses FCFS and Round Robin scheduling. Patients are served fairly in arrival order or with equal time slices.";
        String icuInfo     = "Intensive Care Unit uses SRTF and EDF scheduling. Patient conditions evolve dynamically — the most urgent case always takes priority.";
        String surgeryInfo = "Surgery Department uses SJF and HRRN scheduling. Short procedures run first, but long-waiting surgeries gain priority over time to prevent starvation.";

        if (departmentType == DepartmentType.ER)      return erInfo;
        if (departmentType == DepartmentType.OPD)     return opdInfo;
        if (departmentType == DepartmentType.ICU)     return icuInfo;
        if (departmentType == DepartmentType.SURGERY) return surgeryInfo;
        throw new IllegalArgumentException("Unknown department type: " + departmentType);
    }

    // ── Validation ────────────────────────────────────────────

    /**
     * Returns true if a department exists in the database.
     *
     * @param departmentType  Department to check
     */
    public boolean departmentExists(DepartmentType departmentType) {
        return departmentRepository.existsByDepartmentType(departmentType);
    }
}