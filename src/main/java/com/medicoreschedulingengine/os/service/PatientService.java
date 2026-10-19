package com.medicoreschedulingengine.os.service;

import com.medicoreschedulingengine.os.model.dto.PatientRequestDTO;
import com.medicoreschedulingengine.os.model.dto.PatientResponseDTO;
import com.medicoreschedulingengine.os.model.entity.Department;
import com.medicoreschedulingengine.os.model.entity.Patient;
import com.medicoreschedulingengine.os.model.enums.DepartmentType;
import com.medicoreschedulingengine.os.model.enums.PatientStatus;
import com.medicoreschedulingengine.os.repository.DepartmentRepository;
import com.medicoreschedulingengine.os.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PatientService
 *
 * Handles all patient-related business logic:
 *   - Adding new patients to a department
 *   - Fetching patients (all, by department, by status)
 *   - Deleting patients
 *   - Resetting patient state before a simulation run
 */
@Service
@Transactional
public class PatientService {

    private final PatientRepository    patientRepository;
    private final DepartmentRepository departmentRepository;

    public PatientService(PatientRepository patientRepository,
                          DepartmentRepository departmentRepository) {
        this.patientRepository    = patientRepository;
        this.departmentRepository = departmentRepository;
    }

    // ── Add Patient ───────────────────────────────────────────

    /**
     * Creates and persists a new patient from the request DTO.
     * Looks up the department by DepartmentType and assigns it.
     *
     * @param dto  PatientRequestDTO from the frontend
     * @return     PatientResponseDTO with full patient details
     * @throws IllegalArgumentException if department not found
     */
    public PatientResponseDTO addPatient(PatientRequestDTO dto) {
        Department department = departmentRepository
                .findByDepartmentType(dto.getDepartmentType())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Department not found: " + dto.getDepartmentType()));

        Patient patient = new Patient(
                dto.getName(),
                dto.getArrivalTime(),
                dto.getBurstTime(),
                dto.getPriority(),
                department
        );

        Patient saved = patientRepository.save(patient);
        return PatientResponseDTO.from(saved);
    }

    // ── Fetch Patients ────────────────────────────────────────

    /**
     * Returns all patients in the system.
     */
    @Transactional(readOnly = true)
    public List<PatientResponseDTO> getAllPatients() {
        return patientRepository.findAll()
                .stream()
                .map(PatientResponseDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * Returns a single patient by ID.
     *
     * @throws IllegalArgumentException if not found
     */
    @Transactional(readOnly = true)
    public PatientResponseDTO getPatientById(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found with id: " + id));
        return PatientResponseDTO.from(patient);
    }

    /**
     * Returns all patients belonging to a specific department.
     *
     * @param departmentType  The department to filter by
     */
    @Transactional(readOnly = true)
    public List<PatientResponseDTO> getPatientsByDepartment(DepartmentType departmentType) {
        return patientRepository
                .findByDepartmentTypeOrderByArrivalTime(departmentType)
                .stream()
                .map(PatientResponseDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * Returns all patients with a given status.
     *
     * @param status  WAITING, RUNNING, or COMPLETED
     */
    @Transactional(readOnly = true)
    public List<PatientResponseDTO> getPatientsByStatus(PatientStatus status) {
        return patientRepository.findByStatus(status)
                .stream()
                .map(PatientResponseDTO::from)
                .collect(Collectors.toList());
    }

    // ── Raw Entity Access (used internally by simulation) ─────

    /**
     * Returns raw Patient entities for a department — used by
     * DepartmentSimulationEngine before cloning and running algorithms.
     *
     * @param departmentType  Department to load patients for
     * @return                List of Patient entities sorted by arrival time
     */
    @Transactional(readOnly = true)
    public List<Patient> getRawPatientsByDepartment(DepartmentType departmentType) {
        return patientRepository.findByDepartmentTypeOrderByArrivalTime(departmentType);
    }

    // ── Delete ────────────────────────────────────────────────

    /**
     * Deletes a patient by ID.
     *
     * @throws IllegalArgumentException if not found
     */
    public void deletePatient(Long id) {
        if (!patientRepository.existsById(id)) {
            throw new IllegalArgumentException("Patient not found with id: " + id);
        }
        patientRepository.deleteById(id);
    }

    /**
     * Deletes all patients in a specific department.
     * Useful for clearing a department before a fresh simulation.
     *
     * @param departmentType  Department to clear
     */
    public void deletePatientsByDepartment(DepartmentType departmentType) {
        List<Patient> patients = patientRepository
                .findByDepartmentTypeOrderByArrivalTime(departmentType);
        patientRepository.deleteAll(patients);
    }

    // ── Reset ─────────────────────────────────────────────────

    /**
     * Resets all patients in a department back to WAITING status
     * with full remaining time. Called after a simulation run so
     * patients can be re-simulated if needed.
     *
     * @param departmentType  Department to reset
     */
    public void resetPatientsByDepartment(DepartmentType departmentType) {
        List<Patient> patients = patientRepository
                .findByDepartmentTypeOrderByArrivalTime(departmentType);
        patients.forEach(Patient::reset);
        patientRepository.saveAll(patients);
    }

    // ── Validation ────────────────────────────────────────────

    /**
     * Returns true if a department has at least one patient —
     * used by SimulationService before starting a run.
     *
     * @param departmentType  Department to check
     */
    @Transactional(readOnly = true)
    public boolean departmentHasPatients(DepartmentType departmentType) {
        return !patientRepository
                .findByDepartmentTypeOrderByArrivalTime(departmentType)
                .isEmpty();
    }
}