import { useQuery } from '@tanstack/react-query';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { feeService } from '../../services/feeService';

/* ── Tier config ─────────────────────────────────────────────────────────── */
const TIER = {
    HIGH:   { bg: 'rgba(220,38,38,0.1)',   color: '#dc2626', border: '#fca5a5', icon: '🔴', bar: '#ef4444' },
    MEDIUM: { bg: 'rgba(217,119,6,0.1)',   color: '#d97706', border: '#fcd34d', icon: '🟡', bar: '#f59e0b' },
    LOW:    { bg: 'rgba(22,163,74,0.1)',   color: '#16a34a', border: '#86efac', icon: '🟢', bar: '#22c55e' },
};

/* ── Risk bar (visual indicator) ─────────────────────────────────────────── */
function RiskBar({ points, max = 12 }) {
    const pct = Math.min((points / max) * 100, 100);
    const color = pct >= 60 ? '#ef4444' : pct >= 30 ? '#f59e0b' : '#22c55e';
    return (
        <div style={{ height: '6px', background: '#f1f5f9', borderRadius: '99px', overflow: 'hidden', marginTop: '4px' }}>
            <div style={{
                height: '100%', width: `${pct}%`, background: color,
                borderRadius: '99px', transition: 'width 0.4s ease',
            }} />
        </div>
    );
}

/* ── Stat pill ───────────────────────────────────────────────────────────── */
function Pill({ label, value, color }) {
    return (
        <span style={{
            fontSize: '11px', fontWeight: '600',
            background: `${color}18`, color,
            border: `1px solid ${color}33`,
            borderRadius: '20px', padding: '2px 9px',
        }}>{label}: {value}</span>
    );
}

export default function FeeRiskDashboard() {
    const { data: students = [], isLoading } = useQuery({
        queryKey: ['feeRisk'],
        queryFn: () => feeService.getRiskReport().then(r => r.data),
        retry: false,
    });

    const highCount   = students.filter(s => s.tier === 'HIGH').length;
    const mediumCount = students.filter(s => s.tier === 'MEDIUM').length;

    return (
        <DashboardLayout>
            {/* Header */}
            <div style={{ marginBottom: '28px' }}>
                <h1 style={{ fontSize: '24px', fontWeight: '800', color: '#0f172a', margin: 0 }}>
                    📊 Fee Risk Dashboard
                </h1>
                <p style={{ fontSize: '14px', color: '#64748b', marginTop: '6px' }}>
                    Students with MEDIUM or HIGH payment risk, sorted by risk score.
                </p>
            </div>

            {/* Summary strip */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: '16px', marginBottom: '24px' }}>
                {[
                    { label: 'At-Risk Students', value: students.length, color: '#6366f1', bg: 'rgba(99,102,241,0.07)' },
                    { label: 'High Risk',         value: highCount,       color: '#dc2626', bg: 'rgba(220,38,38,0.07)' },
                    { label: 'Medium Risk',       value: mediumCount,     color: '#d97706', bg: 'rgba(217,119,6,0.07)' },
                ].map(c => (
                    <div key={c.label} style={{ background: c.bg, borderRadius: '14px', padding: '20px 22px', border: `1px solid ${c.color}22` }}>
                        <p style={{ fontSize: '11px', fontWeight: '600', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.05em', margin: 0 }}>{c.label}</p>
                        <p style={{ fontSize: '28px', fontWeight: '800', color: c.color, marginTop: '6px', marginBottom: 0 }}>{c.value}</p>
                    </div>
                ))}
            </div>

            {/* Student list */}
            {isLoading ? (
                <div style={{ textAlign: 'center', padding: '60px', color: '#94a3b8' }}>
                    <div style={{ fontSize: '32px', marginBottom: '10px' }}>⏳</div>
                    Analysing fee records…
                </div>
            ) : students.length === 0 ? (
                <div style={{
                    textAlign: 'center', padding: '60px', background: 'white',
                    borderRadius: '14px', border: '1px solid #f1f5f9',
                }}>
                    <div style={{ fontSize: '40px', marginBottom: '12px' }}>✅</div>
                    <p style={{ color: '#64748b', fontWeight: '600' }}>No students at risk.</p>
                    <p style={{ color: '#94a3b8', fontSize: '13px', marginTop: '4px' }}>All recorded payment histories are in good standing.</p>
                </div>
            ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    {students.map((s, i) => {
                        const t = TIER[s.tier] || TIER.MEDIUM;
                        return (
                            <div key={s.studentId} style={{
                                background: 'white', borderRadius: '14px',
                                border: `1px solid ${s.tier === 'HIGH' ? '#fca5a5' : '#e2e8f0'}`,
                                padding: '18px 22px',
                                boxShadow: s.tier === 'HIGH' ? '0 2px 12px rgba(220,38,38,0.08)' : '0 1px 4px rgba(0,0,0,0.04)',
                                transition: 'transform 0.15s',
                            }}
                                onMouseEnter={e => e.currentTarget.style.transform = 'translateY(-1px)'}
                                onMouseLeave={e => e.currentTarget.style.transform = 'none'}
                            >
                                <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '16px' }}>
                                    {/* Left — identity */}
                                    <div style={{ display: 'flex', gap: '14px', alignItems: 'center', minWidth: 0 }}>
                                        {/* Rank badge */}
                                        <div style={{
                                            width: '38px', height: '38px', borderRadius: '10px', flexShrink: 0,
                                            background: t.bg, border: `1px solid ${t.border}`,
                                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                                            fontSize: '16px', fontWeight: '700', color: t.color,
                                        }}>
                                            #{i + 1}
                                        </div>
                                        <div style={{ minWidth: 0 }}>
                                            <p style={{ margin: 0, fontSize: '15px', fontWeight: '700', color: '#0f172a', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                                {s.studentName}
                                            </p>
                                            <p style={{ margin: '2px 0 0', fontSize: '12px', color: '#64748b' }}>
                                                {s.email}
                                            </p>
                                        </div>
                                    </div>

                                    {/* Right — tier badge */}
                                    <span style={{
                                        flexShrink: 0, fontSize: '12px', fontWeight: '800',
                                        padding: '5px 14px', borderRadius: '20px',
                                        background: t.bg, color: t.color, border: `1px solid ${t.border}`,
                                        letterSpacing: '0.05em',
                                    }}>
                                        {t.icon} {s.tier} RISK
                                    </span>
                                </div>

                                {/* Risk bar */}
                                <div style={{ marginTop: '14px' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2px' }}>
                                        <span style={{ fontSize: '11px', color: '#94a3b8', fontWeight: '500' }}>Risk Score</span>
                                        <span style={{ fontSize: '13px', fontWeight: '700', color: t.color }}>{s.riskPoints} pts</span>
                                    </div>
                                    <RiskBar points={s.riskPoints} />
                                </div>

                                {/* Breakdown pills + reason */}
                                <div style={{ marginTop: '12px', display: 'flex', gap: '6px', flexWrap: 'wrap', alignItems: 'center' }}>
                                    <Pill label="Overdue"  value={s.overdueCount}    color="#dc2626" />
                                    <Pill label="Late"     value={s.paidLateCount}   color="#d97706" />
                                    <Pill label="On Time"  value={s.paidOnTimeCount} color="#16a34a" />
                                    <span style={{ fontSize: '12px', color: '#64748b', marginLeft: '4px', fontStyle: 'italic' }}>
                                        — {s.reason}
                                    </span>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </DashboardLayout>
    );
}
