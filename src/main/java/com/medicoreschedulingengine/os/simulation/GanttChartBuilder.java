package com.medicoreschedulingengine.os.simulation;

import com.medicoreschedulingengine.os.model.dto.AlgorithmResultDTO;
import com.medicoreschedulingengine.os.model.dto.GanttEntryDTO;
import com.medicoreschedulingengine.os.model.entity.ExecutionLog;
import com.medicoreschedulingengine.os.repository.ExecutionLogRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GanttChartBuilder
 *
 * Converts raw execution log data into a structured format
 * that the frontend can directly render as a Gantt chart.
 *
 * Responsibilities:
 *   1. Build Gantt entries from in-memory AlgorithmResultDTO (during simulation)
 *   2. Build Gantt entries from persisted ExecutionLog records (history view)
 *   3. Merge consecutive entries for the same patient (cleaner visualization)
 *   4. Generate a colour map — each patient gets a consistent colour on the chart
 *   5. Calculate timeline boundaries (total start, total end)
 */
@Component
public class GanttChartBuilder {

    private final ExecutionLogRepository executionLogRepository;

    // Predefined colour palette for up to 20 patients on the chart
    private static final List<String> COLOUR_PALETTE = Arrays.asList(
            "#4F86C6", "#E8735A", "#57C4AD", "#F5A623", "#9B59B6",
            "#2ECC71", "#E74C3C", "#3498DB", "#F39C12", "#1ABC9C",
            "#D35400", "#8E44AD", "#27AE60", "#C0392B", "#2980B9",
            "#16A085", "#E67E22", "#2C3E50", "#7F8C8D", "#BDC3C7"
    );

    public GanttChartBuilder(ExecutionLogRepository executionLogRepository) {
        this.executionLogRepository = executionLogRepository;
    }

    // ── Build from AlgorithmResultDTO (live simulation) ───────

    /**
     * Builds the complete Gantt chart data structure from an
     * AlgorithmResultDTO produced during a live simulation run.
     *
     * Returns a GanttChartData object containing:
     *   - ordered list of Gantt entries
     *   - colour map (patientId -> hex colour)
     *   - timeline start and end
     *   - total duration
     *
     * @param result  AlgorithmResultDTO from a completed algorithm run
     * @return        GanttChartData ready for frontend rendering
     */
    public GanttChartData buildFromResult(AlgorithmResultDTO result) {
        List<GanttEntryDTO> entries = result.getGanttEntries();

        if (entries == null || entries.isEmpty()) {
            return GanttChartData.empty(result.getAlgorithmName());
        }

        // Sort by start time to ensure correct order
        List<GanttEntryDTO> sorted = entries.stream()
                .sorted(Comparator.comparingInt(GanttEntryDTO::getStartTime))
                .collect(Collectors.toList());

        // Build colour map
        Map<Long, String> colourMap = buildColourMap(sorted);

        // Timeline boundaries
        int timelineStart = sorted.get(0).getStartTime();
        int timelineEnd   = sorted.stream()
                .mapToInt(GanttEntryDTO::getEndTime)
                .max()
                .orElse(0);

        return new GanttChartData(
                result.getAlgorithmName(),
                sorted,
                colourMap,
                timelineStart,
                timelineEnd,
                timelineEnd - timelineStart
        );
    }

    // ── Build from ExecutionLogs (history view) ───────────────

    /**
     * Builds Gantt chart data from persisted ExecutionLog records.
     * Used when viewing historical simulation runs from the DB.
     *
     * @param simulationRunId  ID of the simulation run to render
     * @return                 GanttChartData ready for frontend rendering
     */
    public GanttChartData buildFromSimulationRunId(Long simulationRunId) {
        List<ExecutionLog> logs = executionLogRepository
                .findGanttDataByRunId(simulationRunId);

        if (logs.isEmpty()) {
            return GanttChartData.empty("UNKNOWN");
        }

        // Convert ExecutionLog entities to GanttEntryDTOs
        List<GanttEntryDTO> entries = logs.stream()
                .map(log -> new GanttEntryDTO(
                        log.getPatient().getId(),
                        log.getPatientName(),
                        log.getStartTime(),
                        log.getEndTime(),
                        log.getAlgorithmUsed()
                ))
                .collect(Collectors.toList());

        String algorithmName = logs.get(0).getAlgorithmUsed();
        Map<Long, String> colourMap = buildColourMap(entries);

        int timelineStart = entries.stream()
                .mapToInt(GanttEntryDTO::getStartTime).min().orElse(0);
        int timelineEnd   = entries.stream()
                .mapToInt(GanttEntryDTO::getEndTime).max().orElse(0);

        return new GanttChartData(
                algorithmName,
                entries,
                colourMap,
                timelineStart,
                timelineEnd,
                timelineEnd - timelineStart
        );
    }

    // ── Merge Consecutive Entries ─────────────────────────────

    /**
     * Merges consecutive Gantt entries for the same patient into
     * a single wider block.
     *
     * Example — before merge:
     *   [P1: 0-2] [P1: 2-4] [P2: 4-6] [P1: 6-8]
     *
     * After merge (only consecutive blocks merged):
     *   [P1: 0-4] [P2: 4-6] [P1: 6-8]
     *
     * This is useful for non-preemptive algorithms (FCFS, SJF, HRRN)
     * where a patient always runs in one unbroken block.
     *
     * @param entries  Raw list of Gantt entries
     * @return         Merged list
     */
    public List<GanttEntryDTO> mergeConsecutiveEntries(List<GanttEntryDTO> entries) {
        if (entries == null || entries.size() <= 1) return entries;

        List<GanttEntryDTO> merged = new ArrayList<>();
        GanttEntryDTO current = entries.get(0);

        for (int i = 1; i < entries.size(); i++) {
            GanttEntryDTO next = entries.get(i);

            boolean samePatient     = current.getPatientId().equals(next.getPatientId());
            boolean consecutive     = current.getEndTime() == next.getStartTime();

            if (samePatient && consecutive) {
                // Extend current block
                current = new GanttEntryDTO(
                        current.getPatientId(),
                        current.getPatientName(),
                        current.getStartTime(),
                        next.getEndTime(),
                        current.getAlgorithmUsed()
                );
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    // ── Colour Map ────────────────────────────────────────────

    /**
     * Assigns a consistent hex colour to each unique patient ID.
     * The same patient always gets the same colour within a chart.
     *
     * @param entries  Gantt entries to extract patient IDs from
     * @return         Map of patientId -> hex colour string
     */
    public Map<Long, String> buildColourMap(List<GanttEntryDTO> entries) {
        Map<Long, String> colourMap = new LinkedHashMap<>();
        int colourIndex = 0;

        for (GanttEntryDTO entry : entries) {
            if (!colourMap.containsKey(entry.getPatientId())) {
                colourMap.put(
                        entry.getPatientId(),
                        COLOUR_PALETTE.get(colourIndex % COLOUR_PALETTE.size())
                );
                colourIndex++;
            }
        }
        return colourMap;
    }

    // ── Summary Stats ─────────────────────────────────────────

    /**
     * Calculates idle time gaps in the Gantt chart.
     * Idle time = periods where no patient is being treated (CPU idle).
     *
     * @param entries  Sorted Gantt entries
     * @return         Total idle time units
     */
    public int calculateIdleTime(List<GanttEntryDTO> entries) {
        if (entries == null || entries.size() <= 1) return 0;

        int idleTime = 0;
        for (int i = 1; i < entries.size(); i++) {
            int gap = entries.get(i).getStartTime() - entries.get(i - 1).getEndTime();
            if (gap > 0) idleTime += gap;
        }
        return idleTime;
    }

    // ── Inner Class: GanttChartData ───────────────────────────

    /**
     * Data transfer object wrapping all Gantt chart information
     * needed by the frontend to render the timeline.
     */
    public static class GanttChartData {

        private final String               algorithmName;
        private final List<GanttEntryDTO>  entries;
        private final Map<Long, String>    colourMap;
        private final int                  timelineStart;
        private final int                  timelineEnd;
        private final int                  totalDuration;

        public GanttChartData(String algorithmName,
                              List<GanttEntryDTO> entries,
                              Map<Long, String> colourMap,
                              int timelineStart,
                              int timelineEnd,
                              int totalDuration) {
            this.algorithmName = algorithmName;
            this.entries       = entries;
            this.colourMap     = colourMap;
            this.timelineStart = timelineStart;
            this.timelineEnd   = timelineEnd;
            this.totalDuration = totalDuration;
        }

        public static GanttChartData empty(String algorithmName) {
            return new GanttChartData(
                    algorithmName,
                    Collections.emptyList(),
                    Collections.emptyMap(),
                    0, 0, 0
            );
        }

        public String getAlgorithmName()          { return algorithmName; }
        public List<GanttEntryDTO> getEntries()   { return entries;       }
        public Map<Long, String> getColourMap()   { return colourMap;     }
        public int getTimelineStart()             { return timelineStart; }
        public int getTimelineEnd()               { return timelineEnd;   }
        public int getTotalDuration()             { return totalDuration; }
    }
}