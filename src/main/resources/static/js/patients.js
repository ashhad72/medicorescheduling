/* ============================================================
   MediCore — Patients Page Script
   Add, view, filter, and delete patients
   ============================================================ */

// ── Add Patient ────────────────────────────────────────────

async function addPatient() {
    const successMsg = document.getElementById('successMsg');
    const errorMsg    = document.getElementById('errorMsg');

    successMsg.classList.remove('show');
    errorMsg.classList.remove('show');

    const name           = document.getElementById('name').value.trim();
    const departmentType = document.getElementById('departmentType').value;
    const arrivalTime    = parseInt(document.getElementById('arrivalTime').value);
    const burstTime      = parseInt(document.getElementById('burstTime').value);
    const priority       = parseInt(document.getElementById('priority').value);

    // ── Client-side validation ────────────────────────────
    if (!name) {
        showError('Patient name is required.');
        return;
    }
    if (isNaN(arrivalTime) || arrivalTime < 0) {
        showError('Arrival time must be 0 or greater.');
        return;
    }
    if (isNaN(burstTime) || burstTime < 1) {
        showError('Burst time must be at least 1.');
        return;
    }
    if (isNaN(priority) || priority < 1) {
        showError('Priority must be at least 1.');
        return;
    }

    const body = { name, departmentType, arrivalTime, burstTime, priority };

    const result = await apiFetch('/patients', {
        method: 'POST',
        body: JSON.stringify(body)
    });

    if (result.ok) {
        showSuccess(`Patient "${result.data.name}" admitted to ${DEPARTMENTS[departmentType].name}.`);
        document.getElementById('name').value = '';
        document.getElementById('arrivalTime').value = 0;
        document.getElementById('burstTime').value = 1;
        document.getElementById('priority').value = 1;

        // Reload table — show this department if filter is ALL or matches
        loadPatients();
    } else {
        showError(typeof result.data === 'string' ? result.data : 'Failed to add patient.');
    }
}

function showSuccess(msg) {
    const el = document.getElementById('successMsg');
    el.textContent = msg;
    el.classList.add('show');
    setTimeout(() => el.classList.remove('show'), 4000);
}

function showError(msg) {
    const el = document.getElementById('errorMsg');
    el.textContent = msg;
    el.classList.add('show');
    setTimeout(() => el.classList.remove('show'), 4000);
}

// ── Load Patients ──────────────────────────────────────────

async function loadPatients() {
    const dept  = document.getElementById('filterDept').value;
    const tbody = document.getElementById('patientTable');

    tbody.innerHTML = `<tr><td colspan="8"><div class="empty-state">Loading patients...</div></td></tr>`;

    const url = dept === 'ALL'
        ? '/patients'
        : `/patients/department/${dept}`;

    const result = await apiFetch(url);

    if (!result.ok) {
        tbody.innerHTML = `<tr><td colspan="8"><div class="empty-state">⚠ Failed to load patients.</div></td></tr>`;
        setText('patientCount', '0');
        return;
    }

    const patients = result.data;
    setText('patientCount', patients.length);

    if (patients.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8">
                    <div class="empty-state">
                        <div class="icon">🏥</div>
                        No patients in this queue yet.
                    </div>
                </td>
            </tr>`;
        return;
    }

    tbody.innerHTML = patients.map((p, i) => `
        <tr style="animation-delay:${i * 0.04}s">
            <td>${i + 1}</td>
            <td style="font-family:var(--font-body); font-weight:500;">${escapeHtml(p.name)}</td>
            <td><span class="badge ${deptBadgeClass(p.departmentType)}">${p.departmentType}</span></td>
            <td>${p.arrivalTime}</td>
            <td>${p.burstTime}</td>
            <td>${priorityPill(p.priority)}</td>
            <td><span class="badge ${statusBadgeClass(p.status)}">${p.status}</span></td>
            <td><button class="btn btn-danger" onclick="deletePatient(${p.id})">Delete</button></td>
        </tr>
    `).join('');
}

// ── Priority Pill Helper ───────────────────────────────────

function priorityPill(priority) {
    let cls = 'p4';
    if (priority === 1) cls = 'p1';
    else if (priority <= 3) cls = 'p2';
    else if (priority <= 5) cls = 'p3';

    return `<span class="priority-pill ${cls}">${priority}</span>`;
}

// ── Delete Patient ─────────────────────────────────────────

async function deletePatient(id) {
    if (!confirm('Discharge this patient from the system?')) return;

    const result = await apiFetch(`/patients/${id}`, { method: 'DELETE' });

    if (result.ok) {
        loadPatients();
    } else {
        showError('Failed to delete patient.');
    }
}

// ── HTML Escape (basic XSS guard for names) ────────────────

function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// ── Init ────────────────────────────────────────────────────

(function initPatientsPage() {
    // Pre-select department from URL query param (?dept=ER)
    const urlDept = new URLSearchParams(window.location.search).get('dept');
    if (urlDept && DEPARTMENTS[urlDept]) {
        document.getElementById('filterDept').value = urlDept;
        document.getElementById('departmentType').value = urlDept;
    }

    loadPatients();
})();