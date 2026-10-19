/* ============================================================
   MediCore — Dashboard Page Script
   Live stats, department queue map, recent activity feed
   ============================================================ */

const DEPT_KEYS = ['ER', 'OPD', 'ICU', 'SURGERY'];

const COUNT_ELEMENTS = {
    ER:      { count: 'erCount',      bar: 'erBar'      },
    OPD:     { count: 'opdCount',     bar: 'opdBar'     },
    ICU:     { count: 'icuCount',     bar: 'icuBar'     },
    SURGERY: { count: 'surgeryCount', bar: 'surgeryBar' }
};

// ── Main Loader ────────────────────────────────────────────

async function loadDashboard() {
    await Promise.all([
        loadDepartmentQueues(),
        loadTreatmentsCompleted(),
        loadRecentActivity()
    ]);
}

// ── Department Queue Sizes ──────────────────────────────────

async function loadDepartmentQueues() {
    let totalPatients   = 0;
    let totalWaiting    = 0;

    const counts = {};

    // Fetch patient lists for all 4 departments in parallel
    const results = await Promise.all(
        DEPT_KEYS.map(dept => apiFetch(`/patients/department/${dept}`))
    );

    results.forEach((result, i) => {
        const dept = DEPT_KEYS[i];
        const list = result.ok && Array.isArray(result.data) ? result.data : [];

        counts[dept] = list.length;
        totalPatients += list.length;
        totalWaiting   += list.filter(p => p.status === 'WAITING').length;
    });

    // Update map nodes
    const maxCount = Math.max(1, ...Object.values(counts));

    DEPT_KEYS.forEach(dept => {
        const els = COUNT_ELEMENTS[dept];
        const count = counts[dept];

        setText(els.count, count);
        setWidth(els.bar, (count / maxCount) * 100);
    });

    // Update top stat cards
    animateCountUp(document.getElementById('statTotalPatients'), totalPatients);
    animateCountUp(document.getElementById('statWaiting'), totalWaiting);
}

// ── Treatments Completed (from simulation history) ───────────

async function loadTreatmentsCompleted() {
    let totalProcessed = 0;

    const results = await Promise.all(
        DEPT_KEYS.map(dept => apiFetch(`/simulation/history/${dept}`))
    );

    results.forEach(result => {
        if (result.ok && Array.isArray(result.data)) {
            // Each SimulationRun has totalPatients — sum across all runs
            result.data.forEach(run => {
                totalProcessed += run.totalPatients || 0;
            });
        }
    });

    animateCountUp(document.getElementById('statCompleted'), totalProcessed);
}

// ── Recent Activity Feed ─────────────────────────────────────

async function loadRecentActivity() {
    const container = document.getElementById('activityList');

    const result = await apiFetch('/simulation/history/recent');

    if (!result.ok || !Array.isArray(result.data) || result.data.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="icon">📡</div>
                No simulations have been run yet.<br>
                <a href="/simulation" style="color:var(--cyan)">Run your first simulation →</a>
            </div>`;
        setText('statRuns', 0);
        return;
    }

    const comparisons = result.data;
    animateCountUp(document.getElementById('statRuns'), comparisons.length);

    container.innerHTML = comparisons.map(c => {
        const dept = c.departmentType;
        const meta = DEPARTMENTS[dept] || { icon: '🏥', name: dept };
        const cssClass = dept.toLowerCase();

        const algo1 = c.simulationRun1 ? ALGORITHM_LABELS[c.simulationRun1.algorithmName] || c.simulationRun1.algorithmName : '?';
        const algo2 = c.simulationRun2 ? ALGORITHM_LABELS[c.simulationRun2.algorithmName] || c.simulationRun2.algorithmName : '?';
        const winner = ALGORITHM_LABELS[c.winnerAlgorithm] || c.winnerAlgorithm || 'DRAW';

        return `
            <div class="activity-item ${cssClass}">
                <div class="dept-icon">${meta.icon}</div>
                <div class="info">
                    <div class="title">${meta.name} — ${algo1} vs ${algo2}</div>
                    <div class="meta">${formatTimestamp(c.comparedAt)}</div>
                </div>
                <div class="winner-tag">🏆 ${winner}</div>
            </div>
        `;
    }).join('');
}

// ── Init ────────────────────────────────────────────────────

(function initDashboard() {
    startSyncClock('syncTime');
    loadDashboard();

    // Refresh every 30 seconds for a "live" feel
    setInterval(loadDashboard, 30000);
})();