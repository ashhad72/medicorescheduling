/* ============================================================
   MediCore — History Page Script
   Filterable list of past simulation runs grouped by comparison
   ============================================================ */

let allComparisons = [];
let currentFilter = 'ALL';

// ── Load History ────────────────────────────────────────────

async function loadHistory() {
    const container = document.getElementById('runList');
    container.innerHTML = `
        <div class="empty-state">
            <div class="icon">📡</div>
            Loading simulation history...
        </div>`;

    // Fetch recent comparisons (covers all departments)
    const result = await apiFetch('/simulation/history/recent');

    if (!result.ok || !Array.isArray(result.data)) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="icon">⚠</div>
                Failed to load history.
            </div>`;
        return;
    }

    allComparisons = result.data;
    renderHistory();
}

// ── Render Filtered List ─────────────────────────────────────

function renderHistory() {
    const container = document.getElementById('runList');

    const filtered = currentFilter === 'ALL'
        ? allComparisons
        : allComparisons.filter(c => c.departmentType === currentFilter);

    if (filtered.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="icon">📋</div>
                No simulation runs found${currentFilter !== 'ALL' ? ' for ' + DEPARTMENTS[currentFilter].name : ''}.<br>
                <a href="/simulation" style="color:var(--cyan)">Run a simulation →</a>
            </div>`;
        return;
    }

    container.innerHTML = filtered.map((c, i) => {
        const dept = c.departmentType;
        const meta = DEPARTMENTS[dept] || { icon: '🏥', name: dept };
        const cssClass = dept.toLowerCase();

        const run1 = c.simulationRun1;
        const run2 = c.simulationRun2;

        const algo1Name = run1 ? (ALGORITHM_LABELS[run1.algorithmName] || run1.algorithmName) : '?';
        const algo2Name = run2 ? (ALGORITHM_LABELS[run2.algorithmName] || run2.algorithmName) : '?';

        const winner = ALGORITHM_LABELS[c.winnerAlgorithm] || c.winnerAlgorithm || 'DRAW';
        const isDraw = (c.winnerAlgorithm === 'DRAW');

        const wtWinner = ALGORITHM_LABELS[c.winnerWaitingTime]    || c.winnerWaitingTime;
        const ttWinner = ALGORITHM_LABELS[c.winnerTurnaroundTime] || c.winnerTurnaroundTime;
        const rtWinner = ALGORITHM_LABELS[c.winnerResponseTime]   || c.winnerResponseTime;

        return `
            <div class="run-card ${cssClass}" style="animation-delay:${i * 0.05}s">
                <div class="dept-icon-box">${meta.icon}</div>

                <div class="info">
                    <div class="algo-name">
                        ${meta.name} — ${algo1Name} <span style="color:var(--text-dim); font-weight:400;">vs</span> ${algo2Name}
                    </div>
                    <div class="timestamp">
                        ${formatTimestamp(c.comparedAt)}
                        ${!isDraw ? `&nbsp;·&nbsp; 🏆 Winner: <span style="color:var(--emerald)">${winner}</span>` : '&nbsp;·&nbsp; Result: <span style="color:var(--amber)">DRAW</span>'}
                    </div>
                </div>

                <div class="metric">
                    <div class="num" style="${wtWinner === algo1Name || wtWinner === algo2Name ? `color:var(--emerald)` : ''}">${wtWinner}</div>
                    <div class="lbl">Best WT</div>
                </div>

                <div class="metric">
                    <div class="num">${ttWinner}</div>
                    <div class="lbl">Best TT</div>
                </div>

                <div class="metric">
                    <div class="num">${rtWinner}</div>
                    <div class="lbl">Best RT</div>
                </div>

                <div class="metric">
                    <div class="num" style="color:var(--text)">#${c.id}</div>
                    <div class="lbl">Run ID</div>
                </div>
            </div>
        `;
    }).join('');
}

// ── Filter ────────────────────────────────────────────────────

function filterHistory(dept, btn) {
    currentFilter = dept;

    document.querySelectorAll('.history-filter-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');

    renderHistory();
}

// ── Init ────────────────────────────────────────────────────────

(function initHistoryPage() {
    loadHistory();
})();