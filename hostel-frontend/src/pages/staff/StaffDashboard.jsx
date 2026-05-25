import DashboardLayout from '../../components/layout/DashboardLayout';
import useAuthStore from '../../store/authStore';

export default function StaffDashboard() {
    const { user } = useAuthStore();
    return (
        <DashboardLayout>
            <div style={{ marginBottom: '24px' }}>
                <h1 style={{ fontSize: '22px', fontWeight: '700', color: '#0f172a' }}>Staff Dashboard</h1>
                <p style={{ fontSize: '14px', color: '#64748b', marginTop: '4px' }}>Welcome, {user?.name}</p>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px' }}>
                {[
                    { icon: '✅', label: 'QR Attendance', desc: 'Scan student QR codes at gate' },
                    { icon: '🔧', label: 'Complaints', desc: 'Manage assigned complaints' },
                    { icon: '📦', label: 'Assets', desc: 'Track hostel inventory' },
                ].map(card => (
                    <div key={card.label} style={{
                        background: 'white', borderRadius: '12px',
                        border: '1px solid #f1f5f9', padding: '24px',
                    }}>
                        <div style={{ fontSize: '28px', marginBottom: '10px' }}>{card.icon}</div>
                        <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#0f172a' }}>{card.label}</h3>
                        <p style={{ fontSize: '13px', color: '#64748b', marginTop: '4px' }}>{card.desc}</p>
                    </div>
                ))}
            </div>
        </DashboardLayout>
    );
}