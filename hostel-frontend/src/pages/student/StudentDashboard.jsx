import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { leaveService } from '../../services/leaveService';
import { feeService } from '../../services/feeService';
import { complaintService } from '../../services/complaintService';
import { studentService } from '../../services/studentService';
import useAuthStore from '../../store/authStore';

const StatCard = ({ label, value, color, bg, to, icon }) => (
    <Link to={to} style={{ textDecoration: 'none' }}>
        <div style={{
            background: 'white',
            borderRadius: '12px',
            border: '1px solid #f1f5f9',
            padding: '20px',
            display: 'flex',
            alignItems: 'center',
            gap: '16px',
            cursor: 'pointer',
            transition: 'box-shadow 0.2s',
        }}
             onMouseEnter={e => e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.08)'}
             onMouseLeave={e => e.currentTarget.style.boxShadow = 'none'}
        >
            <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: bg, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '22px' }}>
                {icon}
            </div>
            <div>
                <p style={{ fontSize: '12px', color: '#64748b', fontWeight: '500', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{label}</p>
                <p style={{ fontSize: '26px', fontWeight: '700', color: color, marginTop: '2px' }}>{value}</p>
            </div>
        </div>
    </Link>
);

export default function StudentDashboard() {
    const { user } = useAuthStore();
    const navigate = useNavigate();

    const { data: prefData } = useQuery({
        queryKey: ['studentPreferences'],
        queryFn: () => studentService.getPreferences().then(r => r.data),
        retry: false,
    });

    useEffect(() => {
        if (prefData && prefData.hasPreferences === false) {
            navigate('/student/onboarding', { replace: true });
        }
    }, [prefData, navigate]);

    const { data: leaves = [] } = useQuery({
        queryKey: ['myLeaves'],
        queryFn: () => leaveService.getMyLeaves().then(r => r.data),
        retry: false,
    });

    const { data: fees = [] } = useQuery({
        queryKey: ['myFees'],
        queryFn: () => feeService.getMyFees().then(r => r.data),
        retry: false,
    });

    const { data: complaints = [] } = useQuery({
        queryKey: ['myComplaints'],
        queryFn: () => complaintService.getMyComplaints().then(r => r.data),
        retry: false,
    });

    const pending = leaves.filter(l => l.status === 'PENDING').length;
    const approved = leaves.filter(l => l.status === 'APPROVED').length;
    const overdueFees = fees.filter(f => f.status === 'OVERDUE').length;
    const openComplaints = complaints.filter(c => c.status !== 'RESOLVED').length;

    const statusColor = (status) => {
        if (status === 'APPROVED') return { bg: '#f0fdf4', color: '#16a34a' };
        if (status === 'REJECTED') return { bg: '#fef2f2', color: '#dc2626' };
        return { bg: '#fffbeb', color: '#d97706' };
    };

    return (
        <DashboardLayout>
            {/* Header */}
            <div style={{ marginBottom: '28px' }}>
                <h1 style={{ fontSize: '24px', fontWeight: '700', color: '#0f172a' }}>
                    Hello, {user?.name} 👋
                </h1>
                <p style={{ fontSize: '14px', color: '#64748b', marginTop: '4px' }}>
                    Here's your hostel overview for today
                </p>
            </div>

            {/* Stat cards */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '16px', marginBottom: '28px' }}>
                <StatCard label="Pending Leaves" value={pending} color="#d97706" bg="#fffbeb" to="/student/leave/history" icon="⏳" />
                <StatCard label="Approved Leaves" value={approved} color="#16a34a" bg="#f0fdf4" to="/student/leave/history" icon="✅" />
                <StatCard label="Overdue Fees" value={overdueFees} color="#dc2626" bg="#fef2f2" to="/student/fees" icon="💳" />
                <StatCard label="Open Complaints" value={openComplaints} color="#2563eb" bg="#eff6ff" to="/student/complaints" icon="🔧" />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
                {/* Quick Actions */}
                <div style={{ background: 'white', borderRadius: '12px', border: '1px solid #f1f5f9', padding: '24px' }}>
                    <h2 style={{ fontSize: '16px', fontWeight: '600', color: '#0f172a', marginBottom: '16px' }}>
                        Quick Actions
                    </h2>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                        {[
                            { to: '/student/leave/apply', icon: '📝', label: 'Apply for Leave', bg: '#eff6ff', color: '#1d4ed8' },
                            { to: '/student/complaints', icon: '🔧', label: 'Raise a Complaint', bg: '#fffbeb', color: '#b45309' },
                            { to: '/student/fees', icon: '💳', label: 'View Fee Status', bg: '#f0fdf4', color: '#15803d' },
                            { to: '/student/leave/history', icon: '📋', label: 'Leave History', bg: '#fdf4ff', color: '#7e22ce' },
                        ].map(item => (
                            <Link key={item.to} to={item.to} style={{ textDecoration: 'none' }}>
                                <div style={{
                                    display: 'flex', alignItems: 'center', gap: '12px',
                                    padding: '12px 14px', borderRadius: '8px', background: item.bg,
                                    color: item.color, fontSize: '14px', fontWeight: '500', cursor: 'pointer',
                                }}>
                                    <span>{item.icon}</span>
                                    <span>{item.label}</span>
                                    <span style={{ marginLeft: 'auto' }}>→</span>
                                </div>
                            </Link>
                        ))}
                    </div>
                </div>

                {/* Recent Leave Requests */}
                <div style={{ background: 'white', borderRadius: '12px', border: '1px solid #f1f5f9', padding: '24px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                        <h2 style={{ fontSize: '16px', fontWeight: '600', color: '#0f172a' }}>Recent Leaves</h2>
                        <Link to="/student/leave/history" style={{ fontSize: '13px', color: '#1E40AF', textDecoration: 'none' }}>View all →</Link>
                    </div>

                    {leaves.length === 0 ? (
                        <div style={{ textAlign: 'center', padding: '32px 0', color: '#94a3b8' }}>
                            <div style={{ fontSize: '32px', marginBottom: '8px' }}>📭</div>
                            <p style={{ fontSize: '13px' }}>No leave requests yet</p>
                            <Link to="/student/leave/apply" style={{ fontSize: '13px', color: '#1E40AF', textDecoration: 'none', marginTop: '8px', display: 'block' }}>
                                Apply for your first leave →
                            </Link>
                        </div>
                    ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                            {leaves.slice(0, 4).map((leave) => {
                                const sc = statusColor(leave.status);
                                return (
                                    <div key={leave.id} style={{
                                        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                                        padding: '10px 0', borderBottom: '1px solid #f8fafc',
                                    }}>
                                        <div>
                                            <p style={{ fontSize: '14px', fontWeight: '500', color: '#1e293b' }}>{leave.leaveType}</p>
                                            <p style={{ fontSize: '12px', color: '#94a3b8', marginTop: '2px' }}>
                                                {leave.fromDate} → {leave.toDate}
                                            </p>
                                        </div>
                                        <span style={{
                                            fontSize: '11px', fontWeight: '600', padding: '3px 10px',
                                            borderRadius: '20px', background: sc.bg, color: sc.color,
                                        }}>
                      {leave.status}
                    </span>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>
            </div>
        </DashboardLayout>
    );
}