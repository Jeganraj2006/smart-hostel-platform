import DashboardLayout from '../../components/layout/DashboardLayout';
import useAuthStore from '../../store/authStore';

const ConfigCard = ({ icon, title, desc, onClick }) => (
    <div
        onClick={onClick}
        style={{
            background: 'white', borderRadius: '12px', border: '1px solid #f1f5f9',
            padding: '24px', cursor: 'pointer', transition: 'box-shadow 0.2s',
        }}
        onMouseEnter={e => e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.08)'}
        onMouseLeave={e => e.currentTarget.style.boxShadow = 'none'}
    >
        <div style={{ fontSize: '28px', marginBottom: '12px' }}>{icon}</div>
        <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#0f172a', marginBottom: '6px' }}>{title}</h3>
        <p style={{ fontSize: '13px', color: '#64748b' }}>{desc}</p>
        <p style={{ fontSize: '13px', color: '#1E40AF', marginTop: '12px', fontWeight: '500' }}>Configure →</p>
    </div>
);

export default function AdminDashboard() {
    const { user } = useAuthStore();

    return (
        <DashboardLayout>
            <div style={{ marginBottom: '28px' }}>
                <h1 style={{ fontSize: '24px', fontWeight: '700', color: '#0f172a' }}>
                    Super Admin Panel
                </h1>
                <p style={{ fontSize: '14px', color: '#64748b', marginTop: '4px' }}>
                    Welcome, {user?.name} — Full system control
                </p>
            </div>

            {/* Warning banner */}
            <div style={{
                background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '10px',
                padding: '14px 18px', marginBottom: '24px', fontSize: '14px', color: '#dc2626',
            }}>
                🔐 You have Super Admin access. All actions are audit logged.
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px' }}>
                <ConfigCard
                    icon="🔗"
                    title="Approval Chain Manager"
                    desc="Configure who approves what leave type and in which order"
                    onClick={() => alert('Chain Config — Coming in Phase 4')}
                />
                <ConfigCard
                    icon="👥"
                    title="Role Manager"
                    desc="Create, edit, and assign roles to users dynamically"
                    onClick={() => alert('Role Manager — Coming in Phase 4')}
                />
                <ConfigCard
                    icon="⏱️"
                    title="Reminder & Timing Rules"
                    desc="Set waiting time, reminder limits, and re-apply limits"
                    onClick={() => alert('Reminder Config — Coming in Phase 4')}
                />
                <ConfigCard
                    icon="🔒"
                    title="Exam Lock Periods"
                    desc="Set date ranges where leave is automatically blocked"
                    onClick={() => alert('Exam Lock — Coming in Phase 4')}
                />
                <ConfigCard
                    icon="📊"
                    title="Leave Quota Manager"
                    desc="Set max leaves per month per type per student"
                    onClick={() => alert('Quota Manager — Coming in Phase 4')}
                />
                <ConfigCard
                    icon="📜"
                    title="Audit Logs"
                    desc="View all system actions with timestamps and IP addresses"
                    onClick={() => alert('Audit Logs — Coming in Phase 4')}
                />
            </div>
        </DashboardLayout>
    );
}