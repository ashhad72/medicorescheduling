/* ============================================================
   MediCore — Simulation Page Script
   Department selection, simulation execution, animated
   comparison bars, and premium interactive Gantt charts
   ============================================================ */

let selectedDept = null;

// ── Department Selection ────────────────────────────────────

function selectDept(dept, el) {
    document.querySelectorAll('.dept-select-card').forEach(c => c.classList.remove('selected'));
    el.classList.add('selected');

    selectedDept = dept;

    document.getElementById('runBtn').disabled = false;
    document.getElementById('errorMsg').classList.remove('show');

    const meta = DEPARTMENTS[dept];
    setText('selectedDeptLabel', `${meta.icon} ${meta.name} selected — ready to simulate`);

    // Hide previous results when switching departments
    document.getElementById('results').classList.remove('show');
}

// ── Run Simulation ───────────────────────────────────────────

async function runSimulation() {
    if (!selectedDept) return;

    const runBtn   = document.getElementById('runBtn');
    const pulse    = document.getElementById('simPulse');
    const results  = document.getElementById('results');
    const errorMsg = document.getElementById('errorMsg');

    runBtn.disabled = true;
    pulse.classList.add('show');
    results.classList.remove('show');
    errorMsg.classList.remove('show');

    const result = await apiFetch(`/simulation/run/${selectedDept}`, { method: 'POST' });

    pulse.classList.remove('show');
    runBtn.disabled = false;

    if (!result.ok) {
        errorMsg.textContent = typeof result.data === 'string'
            ? result.data
            : 'Simulation failed. Please try again.';
        errorMsg.classList.add('show');
        return;
    }

    renderResults(result.data);
}

// ── Render Full Results ──────────────────────────────────────

function renderResults(data) {
    const c  = data.comparison;
    const r1 = c.result1;
    const r2 = c.result2;

    // ── Winner banner ──────────────────────────────────────
    setText('winnerName', ALGORITHM_LABELS[c.overallWinner] || c.overallWinner);
    setText('wtWinner',   ALGORITHM_LABELS[c.winnerWaitingTime]    || c.winnerWaitingTime);
    setText('ttWinner',   ALGORITHM_LABELS[c.winnerTurnaroundTime] || c.winnerTurnaroundTime);
    setText('rtWinner',   ALGORITHM_LABELS[c.winnerResponseTime]   || c.winnerResponseTime);

    // ── Algorithm names ─────────────────────────────────────
    setText('algo1Name', ALGORITHM_LABELS[r1.algorithmName] || r1.algorithmName);
    setText('algo2Name', ALGORITHM_LABELS[r2.algorithmName] || r2.algorithmName);

    // ── Crown / winner card highlight ───────────────────────
    const algo1Card = document.getElementById('algo1Card');
    const algo2Card = document.getElementById('algo2Card');
    algo1Card.classList.remove('winner');
    algo2Card.classList.remove('winner');

    if (c.overallWinner === r1.algorithmName)      algo1Card.classList.add('winner');
    else if (c.overallWinner === r2.algorithmName) algo2Card.classList.add('winner');

    // ── Metric values ────────────────────────────────────────
    setText('algo1WT', fmt(r1.avgWaitingTime));
    setText('algo1TT', fmt(r1.avgTurnaroundTime));
    setText('algo1RT', fmt(r1.avgResponseTime));
    setText('algo1TP', r1.totalPatients);

    setText('algo2WT', fmt(r2.avgWaitingTime));
    setText('algo2TT', fmt(r2.avgTurnaroundTime));
    setText('algo2RT', fmt(r2.avgResponseTime));
    setText('algo2TP', r2.totalPatients);

    // ── Animated comparison bars ────────────────────────────
    renderComparisonBar('algo1WTBar', 'algo2WTBar', 'algo1WT', 'algo2WT', r1.avgWaitingTime,    r2.avgWaitingTime);
    renderComparisonBar('algo1TTBar', 'algo2TTBar', 'algo1TT', 'algo2TT', r1.avgTurnaroundTime, r2.avgTurnaroundTime);
    renderComparisonBar('algo1RTBar', 'algo2RTBar', 'algo1RT', 'algo2RT', r1.avgResponseTime,   r2.avgResponseTime);

    // ── Gantt charts ─────────────────────────────────────────
    renderGantt('gantt1Chart', 'gantt1Ruler', 'gantt1Legend', 'gantt1Label', r1.ganttEntries, r1.algorithmName);
    renderGantt('gantt2Chart', 'gantt2Ruler', 'gantt2Legend', 'gantt2Label', r2.ganttEntries, r2.algorithmName);

    // ── Per-patient tables ───────────────────────────────────
    renderMetricsTable('metricsTable1', r1.patientMetrics);
    renderMetricsTable('metricsTable2', r2.patientMetrics);

    // ── Reveal results ───────────────────────────────────────
    const resultsEl = document.getElementById('results');
    resultsEl.classList.add('show');
    resultsEl.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// ── Animated Comparison Bars ──────────────────────────────────

/**
 * Renders two bars where the lower value gets the full "best" width
 * and emerald color, while the other bar is scaled proportionally.
 * Bars animate from 0 to their target width.
 */
function renderComparisonBar(barId1, barId2, numId1, numId2, val1, val2) {
    const bar1 = document.getElementById(barId1);
    const bar2 = document.getElementById(barId2);
    const num1 = document.getElementById(numId1);
    const num2 = document.getElementById(numId2);

    bar1.classList.remove('best');
    bar2.classList.remove('best');
    num1.classList.remove('best');
    num2.classList.remove('best');

    const maxVal = Math.max(val1, val2, 0.01);

    // Reset to 0 first so the transition animates on every run
    bar1.style.width = '0%';
    bar2.style.width = '0%';

    const pct1 = (val1 / maxVal) * 100;
    const pct2 = (val2 / maxVal) * 100;

    // Mark the lower (better) value
    if (val1 <= val2) {
        bar1.classList.add('best');
        num1.classList.add('best');
    } else {
        bar2.classList.add('best');
        num2.classList.add('best');
    }

    // Trigger animation on next frame
    requestAnimationFrame(() => {
        requestAnimationFrame(() => {
            bar1.style.width = pct1 + '%';
            bar2.style.width = pct2 + '%';
        });
    });
}

// ── Gantt Chart Rendering ──────────────────────────────────────

/**
 * Renders a premium animated Gantt chart with:
 *  - per-patient consistent colours
 *  - hover tooltips showing full details
 *  - a time ruler underneath
 *  - a colour legend
 *  - blocks that animate in sequentially (left to right)
 */
function renderGantt(chartId, rulerId, legendId, labelId, entries, algorithmName) {
    const chart = document.getElementById(chartId);
    const ruler = document.getElementById(rulerId);
    const legend = document.getElementById(legendId);

    setText(labelId, ALGORITHM_LABELS[algorithmName] || algorithmName);

    if (!entries || entries.length === 0) {
        chart.innerHTML = `<div class="empty-state" style="width:100%;">No execution data.</div>`;
        ruler.innerHTML = '';
        legend.innerHTML = '';
        return;
    }

    // ── Assign consistent colour per patient ────────────────
    const colourMap = {};
    let colourIndex = 0;
    entries.forEach(e => {
        if (!(e.patientId in colourMap)) {
            colourMap[e.patientId] = GANTT_COLOURS[colourIndex % GANTT_COLOURS.length];
            colourIndex++;
        }
    });

    const timelineStart = entries[0].startTime;
    const timelineEnd   = entries[entries.length - 1].endTime;
    const totalDuration = Math.max(1, timelineEnd - timelineStart);

    // ── Build Gantt blocks ────────────────────────────────────
    chart.innerHTML = entries.map((e, i) => {
        const duration = e.endTime - e.startTime;
        const widthPct = (duration / totalDuration) * 100;
        const colour   = colourMap[e.patientId];

        return `
            <div class="gantt-block"
                 style="width:0%; background:${colour}; animation: ganttGrow 0.5s ease forwards; animation-delay: ${i * 0.08}s;"
                 data-target-width="${widthPct}"
                 data-patient="${escapeHtmlSim(e.patientName)}"
                 data-start="${e.startTime}"
                 data-end="${e.endTime}"
                 data-duration="${duration}">
                ${duration / totalDuration > 0.04 ? escapeHtmlSim(e.patientName) : ''}
                <div class="tooltip">
                    <strong>${escapeHtmlSim(e.patientName)}</strong><br>
                    Time: ${e.startTime} → ${e.endTime}<br>
                    Duration: ${duration} unit${duration !== 1 ? 's' : ''}
                </div>
            </div>
        `;
    }).join('');

    // Apply target widths after insertion (so CSS transition / animation works)
    requestAnimationFrame(() => {
        chart.querySelectorAll('.gantt-block').forEach(block => {
            const target = block.getAttribute('data-target-width');
            block.style.width = target + '%';
        });
    });

    // ── Build ruler (time markers) ────────────────────────────
    const rulerStep = calculateRulerStep(totalDuration);
    let rulerHtml = '';
    for (let t = timelineStart; t <= timelineEnd; t += rulerStep) {
        const widthPct = (rulerStep / totalDuration) * 100;
        rulerHtml += `<span style="width:${widthPct}%">${t}</span>`;
    }
    ruler.innerHTML = rulerHtml;

    // ── Build legend ───────────────────────────────────────────
    const seen = new Set();
    legend.innerHTML = entries
        .filter(e => {
            if (seen.has(e.patientId)) return false;
            seen.add(e.patientId);
            return true;
        })
        .map(e => `
            <div class="legend-chip">
                <span class="dot" style="background:${colourMap[e.patientId]}"></span>
                ${escapeHtmlSim(e.patientName)}
            </div>
        `).join('');
}

/**
 * Picks a sensible ruler step so we don't render too many labels.
 */
function calculateRulerStep(totalDuration) {
    if (totalDuration <= 10) return 1;
    if (totalDuration <= 20) return 2;
    if (totalDuration <= 50) return 5;
    if (totalDuration <= 100) return 10;
    return Math.ceil(totalDuration / 20);
}

// ── Per-Patient Metrics Table ──────────────────────────────────

function renderMetricsTable(tbodyId, metrics) {
    const tbody = document.getElementById(tbodyId);

    if (!metrics || metrics.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8"><div class="empty-state">No data available.</div></td></tr>`;
        return;
    }

    tbody.innerHTML = metrics.map((m, i) => `
        <tr style="animation-delay:${i * 0.04}s">
            <td style="font-family:var(--font-body); font-weight:500;">${escapeHtmlSim(m.patientName)}</td>
            <td>${m.arrivalTime}</td>
            <td>${m.burstTime}</td>
            <td>${m.priority}</td>
            <td>${m.completionTime}</td>
            <td>${m.waitingTime}</td>
            <td>${m.turnaroundTime}</td>
            <td>${m.responseTime}</td>
        </tr>
    `).join('');
}

// ── Tab Switching ───────────────────────────────────────────────

function switchTab(tabId, btn) {
    const panel = btn.closest('.panel');
    panel.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
    panel.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));

    document.getElementById(tabId).classList.add('active');
    btn.classList.add('active');
}

// ── HTML Escape ───────────────────────────────────────────────

function escapeHtmlSim(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// ── Inject Gantt Animation Keyframes ─────────────────────────

(function injectGanttKeyframes() {
    const style = document.createElement('style');
    style.textContent = `
        @keyframes ganttGrow {
            from { opacity: 0; }
            to   { opacity: 1; }
        }
    `;
    document.head.appendChild(style);
})();