package com.medicoreschedulingengine.os.scheduler;

import com.medicoreschedulingengine.os.model.dto.AlgorithmResultDTO;
import com.medicoreschedulingengine.os.model.entity.Patient;

import java.util.List;

/**
 * Common contract for all scheduling algorithms.
 *
 * Every algorithm receives a CLONED list of patients so the original
 * dataset is never mutated. Each implementation returns a fully
 * populated AlgorithmResultDTO containing:
 *   - aggregate metrics (avgWT, avgTT, avgRT)
 *   - per-patient metrics
 *   - Gantt chart entries
 */
public interface SchedulingStrategy {

    /**
     * Execute the scheduling algorithm on the given patient list.
     *
     * @param patients A cloned, independent copy of the patient dataset.
     * @return         AlgorithmResultDTO with all metrics and Gantt data.
     */
    AlgorithmResultDTO execute(List<Patient> patients);

    /**
     * Returns the algorithm name — used for labelling results and DB storage.
     */
    String getAlgorithmName();
}