import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { leaveService } from '../../services/leaveService';
import useAuthStore from '../../store/authStore';

export default function WardenDashboard() {
    const { user } = useAuthStore();

    const { data: pendingLeaves = [] } = useQuery({
        queryKey: ['pendingLeaves'],
        queryFn: () => leaveService.getPending().then(r => r.data),
        retry: false,
    });

    return (
        <DashboardLayout>
            <div style={{ marginBottom: '28px' }}>
                <h1 style={{ fontSize: '24px', fontWeight: '700', color: '#0f172a' }}>
                    Warden Dashboard 👋
                </h1>
                <p style={{ fontSize: '14px', color: '#64748b', marginTop: '4px' }}>
                    Welcome back, {user?.name}
                </p>
            </div>

            {/* Stats */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', marginBottom: '28px' }}>
                <div style={{ background: 'white', borderRadius: '12px', border: '1px solid #f1f5f9', padding: '20px' }}>
                    <p style={{ fontSize: '12px', color: '#64748b', fontWeight: '500', textTransform: 'uppercase' }}>Pending Approvals</p>
                    <p style={{ fontSize: '32px', fontWeight: '700', color: '#d97706', marginTop: '4px' }}>{pendingLeaves.length}</p>
                </div>
                <div style={{ background: 'white', borderRadius: '12px', border: '1px solid #f1f5f9', padding: '20px' }}>
                    <p style={{ fontSize: '12px', color: '#64748b', fontWeight: '500', textTransform: 'uppercase' }}>Students Outside</p>
                    <p style={{ fontSize: '32px', fontWeight: '700', color: '#2563eb', marginTop: '4px' }}>—</p>
                </div>
                <div style={{ background: 'white', borderRadius: '12px', border: '1px solid #f1f5f9', padding: '20px' }}>
                    <p style={{ fontSize: '12px', color: '#64748b', fontWeight: '500', textTransform: 'uppercase' }}>Overdue Returns</p>
                    <p style={{ fontSize: '32px', fontWeight: '700', color: '#dc2626', marginTop: '4px' }}>—</p>
                </div>
            </div>

            {/* Pending leaves preview */}
            <div style={{ background: 'white', borderRadius: '12px', border: '1px solid #f1f5f9', padding: '24px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                    <h2 style={{ fontSize: '16px', fontWeight: '600', color: '#0f172a' }}>Pending Leave Requests</h2>
                    <Link to="/warden/leaves" style={{ fontSize: '13px', color: '#1E40AF', textDecoration: 'none' }}>
                        View all →
                    </Link>
                </div>

                {pendingLeaves.length === 0 ? (
                    <div style={{ textAlign: 'center', padding: '32px', color: '#94a3b8' }}>
                        <div style={{ fontSize: '32px', marginBottom: '8px' }}>✅</div>
                        <p style={{ fontSize: '14px' }}>No pending requests</p>
                    </div>
                ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                        {pendingLeaves.slice(0, 5).map((leave) => (
                            <div key={leave.id} style={{
                                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                                padding: '12px', borderRadius: '8px', background: '#f8fafc',
                            }}>
                                <div>
                                    <p style={{ fontSize: '14px', fontWeight: '500', color: '#0f172a' }}>{leave.studentName}</p>
                                    <p style={{ fontSize: '12px', color: '#64748b' }}>{leave.leaveType} · {leave.fromDate} → {leave.toDate}</p>
                                </div>
                                <Link to="/warden/leaves" style={{
                                    background: '#1E40AF', color: 'white', textDecoration: 'none',
                                    borderRadius: '6px', padding: '5px 14px', fontSize: '13px',
                                }}>
                                    Review
                                </Link>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </DashboardLayout>
    );
}