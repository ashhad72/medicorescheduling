package com.medicoreschedulingengine.os.controller;

import com.medicoreschedulingengine.os.model.dto.PatientRequestDTO;
import com.medicoreschedulingengine.os.model.dto.PatientResponseDTO;
import com.medicoreschedulingengine.os.model.enums.DepartmentType;
import com.medicoreschedulingengine.os.model.enums.PatientStatus;
import com.medicoreschedulingengine.os.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PatientController
 *
 * REST endpoints for all patient-related operations.
 *
 * Base URL: /api/patients
 *
 * Endpoints:
 *   POST   /api/patients                          → Add a new patient
 *   GET    /api/patients                          → Get all patients
 *   GET    /api/patients/{id}                     → Get patient by ID
 *   GET    /api/patients/department/{type}        → Get patients by department
 *   GET    /api/patients/status/{status}          → Get patients by status
 *   DELETE /api/patients/{id}                     → Delete a patient
 *   DELETE /api/patients/department/{type}        → Delete all patients in a department
 *   PUT    /api/patients/department/{type}/reset  → Reset patients in a department
 */
@RestController
@RequestMapping("/api/patients")
@CrossOrigin(origins = "*")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    // ── POST /api/patients ────────────────────────────────────

    /**
     * Adds a new patient to the system.
     *
     * Request body example:
     * {
     *   "name": "John Doe",
     *   "arrivalTime": 0,
     *   "burstTime": 5,
     *   "priority": 2,
     *   "departmentType": "ER"
     * }
     */
    @PostMapping
    public ResponseEntity<?> addPatient(@Valid @RequestBody PatientRequestDTO dto) {
        try {
            PatientResponseDTO response = patientService.addPatient(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── GET /api/patients ─────────────────────────────────────

    /**
     * Returns all patients in the system.
     */
    @GetMapping
    public ResponseEntity<List<PatientResponseDTO>> getAllPatients() {
        return ResponseEntity.ok(patientService.getAllPatients());
    }

    // ── GET /api/patients/{id} ────────────────────────────────

    /**
     * Returns a single patient by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPatientById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(patientService.getPatientById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // ── GET /api/patients/department/{type} ───────────────────

    /**
     * Returns all patients in a specific department, sorted by arrival time.
     *
     * Example: GET /api/patients/department/ER
     */
    @GetMapping("/department/{type}")
    public ResponseEntity<?> getPatientsByDepartment(@PathVariable String type) {
        try {
            DepartmentType departmentType = DepartmentType.valueOf(type.toUpperCase());
            return ResponseEntity.ok(
                    patientService.getPatientsByDepartment(departmentType));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body("Invalid department type: " + type
                            + ". Valid values: ER, OPD, ICU, SURGERY");
        }
    }

    // ── GET /api/patients/status/{status} ────────────────────

    /**
     * Returns all patients with a given status.
     *
     * Example: GET /api/patients/status/WAITING
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getPatientsByStatus(@PathVariable String status) {
        try {
            PatientStatus patientStatus = PatientStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(
                    patientService.getPatientsByStatus(patientStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body("Invalid status: " + status
                            + ". Valid values: WAITING, RUNNING, COMPLETED");
        }
    }

    // ── DELETE /api/patients/{id} ─────────────────────────────

    /**
     * Deletes a patient by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePatient(@PathVariable Long id) {
        try {
            patientService.deletePatient(id);
            return ResponseEntity.ok("Patient deleted successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // ── DELETE /api/patients/department/{type} ────────────────

    /**
     * Deletes all patients in a department.
     * Useful for clearing a department before adding a fresh set of patients.
     *
     * Example: DELETE /api/patients/department/OPD
     */
    @DeleteMapping("/department/{type}")
    public ResponseEntity<?> deletePatientsByDepartment(@PathVariable String type) {
        try {
            DepartmentType departmentType = DepartmentType.valueOf(type.toUpperCase());
            patientService.deletePatientsByDepartment(departmentType);
            return ResponseEntity.ok(
                    "All patients deleted from department: " + departmentType);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body("Invalid department type: " + type);
        }
    }

    // ── PUT /api/patients/department/{type}/reset ─────────────

    /**
     * Resets all patients in a department back to WAITING status
     * with full remaining time.
     *
     * Example: PUT /api/patients/department/ICU/reset
     */
    @PutMapping("/department/{type}/reset")
    public ResponseEntity<?> resetPatientsByDepartment(@PathVariable String type) {
        try {
            DepartmentType departmentType = DepartmentType.valueOf(type.toUpperCase());
            patientService.resetPatientsByDepartment(departmentType);
            return ResponseEntity.ok(
                    "Patients reset successfully in department: " + departmentType);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body("Invalid department type: " + type);
        }
    }
}