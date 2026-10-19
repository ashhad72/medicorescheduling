package com.medicoreschedulingengine.os.controller;

import com.medicoreschedulingengine.os.model.entity.Department;
import com.medicoreschedulingengine.os.model.enums.DepartmentType;
import com.medicoreschedulingengine.os.service.DepartmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * DepartmentController
 *
 * REST endpoints for all department-related operations.
 *
 * Base URL: /api/departments
 *
 * Endpoints:
 *   GET /api/departments                        → Get all departments
 *   GET /api/departments/{type}                 → Get department by type
 *   GET /api/departments/{type}/algorithms      → Get algorithm names for department
 *   GET /api/departments/{type}/info            → Get scheduling info description
 */
@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = "*")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    // ── GET /api/departments ──────────────────────────────────

    /**
     * Returns all four departments.
     */
    @GetMapping
    public ResponseEntity<List<Department>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    // ── GET /api/departments/{type} ───────────────────────────

    /**
     * Returns a single department by its type.
     *
     * Example: GET /api/departments/ER
     */
    @GetMapping("/{type}")
    public ResponseEntity<?> getDepartmentByType(@PathVariable String type) {
        try {
            DepartmentType departmentType = DepartmentType.valueOf(type.toUpperCase());
            Department department = departmentService.getDepartmentByType(departmentType);
            return ResponseEntity.ok(department);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body("Invalid department type: " + type
                            + ". Valid values: ER, OPD, ICU, SURGERY");
        }
    }

    // ── GET /api/departments/{type}/algorithms ────────────────

    /**
     * Returns the two algorithm names assigned to a department.
     * Used by the frontend to label the comparison result panels.
     *
     * Example: GET /api/departments/OPD/algorithms
     * Response: ["FCFS", "ROUND_ROBIN"]
     */
    @GetMapping("/{type}/algorithms")
    public ResponseEntity<?> getAlgorithmsForDepartment(@PathVariable String type) {
        try {
            DepartmentType departmentType = DepartmentType.valueOf(type.toUpperCase());
            String[] algorithms = departmentService.getAlgorithmsForDepartment(departmentType);
            return ResponseEntity.ok(algorithms);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body("Invalid department type: " + type);
        }
    }

    // ── GET /api/departments/{type}/info ──────────────────────

    /**
     * Returns a human-readable description of the scheduling approach
     * used by the department.
     * Used by the frontend info/tooltip panels.
     *
     * Example: GET /api/departments/SURGERY/info
     */
    @GetMapping("/{type}/info")
    public ResponseEntity<?> getDepartmentSchedulingInfo(@PathVariable String type) {
        try {
            DepartmentType departmentType = DepartmentType.valueOf(type.toUpperCase());
            String info = departmentService.getDepartmentSchedulingInfo(departmentType);
            return ResponseEntity.ok(info);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body("Invalid department type: " + type);
        }
    }
}