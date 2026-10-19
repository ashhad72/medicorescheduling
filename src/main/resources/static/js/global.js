/* ============================================================
   MediCore — Global Utilities
   Shared across all pages: API helper, formatting, animations
   ============================================================ */

// ── API Base ───────────────────────────────────────────────
const API_BASE = '/api';

/**
 * Generic fetch wrapper with JSON parsing and error handling.
 *
 * @param {string} url     - endpoint path (relative to API_BASE)
 * @param {object} options - fetch options (method, body, etc.)
 * @returns {Promise<{ok: boolean, status: number, data: any}>}
 */
async function apiFetch(url, options = {}) {
    try {
        const res = await fetch(API_BASE + url, {
            headers: { 'Content-Type': 'application/json' },
            ...options
        });

        let data;
        const contentType = res.headers.get('content-type') || '';
        if (contentType.includes('application/json')) {
            data = await res.json();
        } else {
            data = await res.text();
        }

        return { ok: res.ok, status: res.status, data };
    } catch (err) {
        return { ok: false, status: 0, data: 'Network error — is the backend running?' };
    }
}

// ── Department Metadata ──────────────────────────────────────
const DEPARTMENTS = {
    ER: {
        name: 'Emergency',
        icon: '🚨',
        algorithms: ['PREEMPTIVE_PRIORITY', 'MULTILEVEL_QUEUE'],
        color: 'red'
    },
    OPD: {
        name: 'Outpatient',
        icon: '🏥',
        algorithms: ['FCFS', 'ROUND_ROBIN'],
        color: 'emerald'
    },
    ICU: {
        name: 'Intensive Care',
        icon: '💉',
        algorithms: ['SRTF', 'EDF'],
        color: 'amber'
    },
    SURGERY: {
        name: 'Surgery',
        icon: '🔪',
        algorithms: ['SJF', 'HRRN'],
        color: 'purple'
    }
};

const ALGORITHM_LABELS = {
    FCFS:                'FCFS',
    ROUND_ROBIN:         'Round Robin',
    SJF:                 'SJF',
    SRTF:                'SRTF',
    PREEMPTIVE_PRIORITY: 'Preemptive Priority',
    MULTILEVEL_QUEUE:    'Multilevel Queue',
    HRRN:                'HRRN',
    EDF:                 'EDF'
};

// ── Gantt Chart Colour Palette ───────────────────────────────
const GANTT_COLOURS = [
    '#00C897', '#42D9FF', '#FFB830', '#FF4D6D', '#9B6DFF',
    '#5BD68A', '#67B3FF', '#FFD166', '#FF8FA3', '#C792EA'
];

// ── Number Formatting ─────────────────────────────────────────

/**
 * Formats a number to 2 decimal places.
 */
function fmt(num) {
    return Number(num).toFixed(2);
}

/**
 * Formats a timestamp string into a readable date/time.
 */
function formatTimestamp(isoString) {
    if (!isoString) return '—';
    const date = new Date(isoString);
    return date.toLocaleString('en-US', {
        month: 'short', day: 'numeric',
        hour: '2-digit', minute: '2-digit'
    });
}

/**
 * Returns the current time as HH:MM:SS.
 */
function currentTimeString() {
    return new Date().toLocaleTimeString('en-US', { hour12: false });
}

// ── Count-Up Animation ────────────────────────────────────────

/**
 * Animates a number counting up from 0 to target value.
 *
 * @param {HTMLElement} el     - element to update
 * @param {number}      target - final value
 * @param {number}      duration - animation duration in ms
 */
function animateCountUp(el, target, duration = 800) {
    if (!el) return;
    const start = 0;
    const startTime = performance.now();

    function tick(now) {
        const progress = Math.min((now - startTime) / duration, 1);
        const eased = 1 - Math.pow(1 - progress, 3); // ease-out cubic
        const value = Math.round(start + (target - start) * eased);
        el.textContent = value;

        if (progress < 1) requestAnimationFrame(tick);
        else el.textContent = target;
    }

    requestAnimationFrame(tick);
}

// ── Department Badge Helper ───────────────────────────────────

/**
 * Returns the badge CSS class for a department type.
 */
function deptBadgeClass(dept) {
    return 'badge-' + dept.toLowerCase();
}

/**
 * Returns the badge CSS class for a patient status.
 */
function statusBadgeClass(status) {
    return 'badge-' + status.toLowerCase();
}

// ── Sync Time Display (used on dashboard) ─────────────────────

function startSyncClock(elementId) {
    const el = document.getElementById(elementId);
    if (!el) return;

    function update() {
        el.textContent = currentTimeString();
    }

    update();
    setInterval(update, 1000);
}

// ── Safe Element Setter ────────────────────────────────────────

/**
 * Safely sets text content of an element if it exists.
 */
function setText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
}

/**
 * Safely sets the width style of an element (for bars).
 */
function setWidth(id, percent) {
    const el = document.getElementById(id);
    if (el) el.style.width = Math.max(0, Math.min(100, percent)) + '%';
}