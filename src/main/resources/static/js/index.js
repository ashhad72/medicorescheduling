/* ============================================================
   MediCore — Landing Page Script
   ECG canvas animation + live patient counter
   ============================================================ */

// ── ECG Canvas Animation ──────────────────────────────────────

(function initECG() {
    const canvas = document.getElementById('ecgCanvas');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    let width, height;

    function resize() {
        const rect = canvas.getBoundingClientRect();
        width  = canvas.width  = rect.width  * devicePixelRatio;
        height = canvas.height = rect.height * devicePixelRatio;
        ctx.scale(devicePixelRatio, devicePixelRatio);
    }

    resize();
    window.addEventListener('resize', () => {
        ctx.setTransform(1, 0, 0, 1, 0, 0);
        resize();
    });

    // ECG waveform pattern — one heartbeat cycle
    // Values represent y-offset multipliers (-1 to 1) along x positions (0 to 1)
    const beatPattern = [
        { x: 0.00, y: 0    },
        { x: 0.08, y: 0    },
        { x: 0.11, y: -0.15},
        { x: 0.14, y: 0.05 },
        { x: 0.17, y: -0.85},
        { x: 0.20, y: 0.6  },
        { x: 0.23, y: -0.1 },
        { x: 0.26, y: 0    },
        { x: 0.40, y: 0    },
        { x: 0.44, y: -0.25},
        { x: 0.50, y: 0    },
        { x: 1.00, y: 0    }
    ];

    let offset = 0;
    const speed = 1.4; // px per frame

    function getY(xNorm) {
        // xNorm is 0-1 position within one beat cycle
        for (let i = 0; i < beatPattern.length - 1; i++) {
            const a = beatPattern[i];
            const b = beatPattern[i + 1];
            if (xNorm >= a.x && xNorm <= b.x) {
                const t = (xNorm - a.x) / (b.x - a.x || 1);
                return a.y + (b.y - a.y) * t;
            }
        }
        return 0;
    }

    function draw() {
        const w = width  / devicePixelRatio;
        const h = height / devicePixelRatio;

        ctx.clearRect(0, 0, w, h);

        const beatWidth = w * 0.5; // each heartbeat spans 50% of canvas width
        const midY = h / 2;
        const amplitude = h * 0.42;

        // Draw main line
        ctx.beginPath();
        ctx.lineWidth = 2;
        ctx.strokeStyle = '#00C897';
        ctx.shadowColor = '#00C897';
        ctx.shadowBlur = 8;

        for (let px = 0; px <= w; px++) {
            const cyclePos = ((px + offset) % beatWidth) / beatWidth;
            const y = midY - getY(cyclePos) * amplitude;

            if (px === 0) ctx.moveTo(px, y);
            else ctx.lineTo(px, y);
        }

        ctx.stroke();
        ctx.shadowBlur = 0;

        // Leading dot (bright pulse point)
        const dotCycle = ((w + offset) % beatWidth) / beatWidth;
        const dotY = midY - getY(dotCycle) * amplitude;
        ctx.beginPath();
        ctx.arc(w - 2, dotY, 3, 0, Math.PI * 2);
        ctx.fillStyle = '#42D9FF';
        ctx.shadowColor = '#42D9FF';
        ctx.shadowBlur = 12;
        ctx.fill();
        ctx.shadowBlur = 0;

        offset += speed;
        requestAnimationFrame(draw);
    }

    draw();
})();

// ── Live Patient Counter ──────────────────────────────────────

(async function loadLivePatientCount() {
    const el = document.getElementById('livePatientCount');
    if (!el) return;

    const result = await apiFetch('/patients');

    if (result.ok && Array.isArray(result.data)) {
        animateCountUp(el, result.data.length, 1000);
    } else {
        el.textContent = '0';
    }
})();