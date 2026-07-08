import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { leaveService } from '../../services/leaveService';

const statusStyle = (status) => {
    const map = {
        APPROVED: { bg: '#f0fdf4', color: '#16a34a' },
        REJECTED: { bg: '#fef2f2', color: '#dc2626' },
        PENDING: { bg: '#fffbeb', color: '#d97706' },
    };
    return map[status] || { bg: '#f1f5f9', color: '#475569' };
};

export default function MyLeaves() {
    const [filter, setFilter] = useState('ALL');

    const { data: leaves = [], isLoading } = useQuery({
        queryKey: ['myLeaves'],
        queryFn: () => leaveService.getMyLeaves().then(r => r.data),
        retry: false,
    });

    const reminderMutation = useMutation({
        mutationFn: (id) => leaveService.sendReminder(id),
        onSuccess: () => toast.success('Reminder sent to approver!'),
        onError: () => toast.error('Failed to send reminder'),
    });

    const filtered = filter === 'ALL' ? leaves : leaves.filter(l => l.status === filter);

    return (
        <DashboardLayout>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
                <div>
                    <h1 style={{ fontSize: '22px', fontWeight: '700', color: '#0f172a' }}>Leave History</h1>
                    <p style={{ fontSize: '14px', color: '#64748b', marginTop: '4px' }}>{leaves.length} total requests</p>
                </div>
                <Link to="/student/leave/apply" style={{
                    background: '#1E40AF', color: 'white', textDecoration: 'none',
                    borderRadius: '8px', padding: '9px 18px', fontSize: '14px', fontWeight: '500',
                }}>
                    + New Request
                </Link>
            </div>

            {/* Filter tabs */}
            <div style={{ display: 'flex', gap: '8px', marginBottom: '20px' }}>
                {['ALL', 'PENDING', 'APPROVED', 'REJECTED'].map(f => (
                    <button
                        key={f}
                        onClick={() => setFilter(f)}
                        style={{
                            padding: '6px 16px', borderRadius: '20px', fontSize: '13px', fontWeight: '500',
                            border: 'none', cursor: 'pointer',
                            background: filter === f ? '#1E40AF' : '#f1f5f9',
                            color: filter === f ? 'white' : '#475569',
                        }}
                    >
                        {f}
                    </button>
                ))}
            </div>

            {isLoading ? (
                <div style={{ textAlign: 'center', padding: '60px', color: '#94a3b8' }}>Loading...</div>
            ) : filtered.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '60px', background: 'white', borderRadius: '12px', border: '1px solid #f1f5f9' }}>
                    <div style={{ fontSize: '40px', marginBottom: '12px' }}>📭</div>
                    <p style={{ color: '#64748b', fontSize: '14px' }}>No leave requests found</p>
                    <Link to="/student/leave/apply" style={{ color: '#1E40AF', fontSize: '14px', textDecoration: 'none', display: 'block', marginTop: '10px' }}>
                        Apply for leave →
                    </Link>
                </div>
            ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    {filtered.map((leave) => {
                        const sc = statusStyle(leave.status);
                        return (
                            <div key={leave.id} style={{
                                background: 'white', borderRadius: '12px',
                                border: '1px solid #f1f5f9', padding: '20px',
                            }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                                    <div style={{ flex: 1 }}>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
                                            <h3 style={{ fontSize: '15px', fontWeight: '600', color: '#0f172a' }}>{leave.leaveType}</h3>
                                            <span style={{
                                                fontSize: '11px', fontWeight: '600', padding: '2px 10px',
                                                borderRadius: '20px', background: sc.bg, color: sc.color,
                                            }}>
                        {leave.status}
                      </span>
                                        </div>
                                        <p style={{ fontSize: '13px', color: '#64748b' }}>
                                            📅 {leave.fromDate} → {leave.toDate}
                                        </p>
                                        {leave.reason && (
                                            <p style={{ fontSize: '13px', color: '#475569', marginTop: '6px', fontStyle: 'italic' }}>
                                                "{leave.reason}"
                                            </p>
                                        )}
                                        {leave.rejectionReason && (
                                            <p style={{ fontSize: '13px', color: '#dc2626', marginTop: '6px' }}>
                                                ❌ Rejected: {leave.rejectionReason}
                                            </p>
                                        )}
                                    </div>

                                    {/* Reminder button for pending leaves */}
                                    {leave.status === 'PENDING' && (
                                        <button
                                            onClick={() => reminderMutation.mutate(leave.id)}
                                            disabled={reminderMutation.isPending}
                                            style={{
                                                background: '#fffbeb', color: '#b45309', border: '1px solid #fde68a',
                                                borderRadius: '8px', padding: '7px 14px', fontSize: '13px',
                                                fontWeight: '500', cursor: 'pointer',
                                            }}
                                        >
                                            🔔 Send Reminder
                                        </button>
                                    )}
                                </div>

                                {leave.qrCode && (
                                    <div style={{ marginTop: '12px', display: 'flex', alignItems: 'center', gap: '12px', background: '#f8fafc', padding: '12px', borderRadius: '8px' }}>
                                        <img 
                                            src={`data:image/png;base64,${leave.qrCode}`} 
                                            alt="Gatepass QR" 
                                            style={{ width: '100px', height: '100px', background: 'white', border: '1px solid #e2e8f0', borderRadius: '6px', padding: '4px' }} 
                                        />
                                        <div>
                                            <p style={{ fontSize: '13px', fontWeight: '600', color: '#0f172a' }}>🎟️ Gatepass QR Code</p>
                                            <p style={{ fontSize: '11px', color: '#64748b', marginTop: '2px', lineHeight: '1.4' }}>
                                                Scan this QR code at the security gate when leaving or entering the hostel campus.
                                            </p>
                                        </div>
                                    </div>
                                )}

                                {/* Approval chain progress */}
                                {leave.approvalSteps && (
                                    <div style={{ marginTop: '14px', paddingTop: '14px', borderTop: '1px solid #f8fafc' }}>
                                        <p style={{ fontSize: '12px', color: '#94a3b8', marginBottom: '8px' }}>Approval Progress</p>
                                        <div style={{ display: 'flex', gap: '8px' }}>
                                            {leave.approvalSteps.map((step, i) => (
                                                <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <span style={{
                              fontSize: '12px', padding: '3px 10px', borderRadius: '20px', fontWeight: '500',
                              background: step.approved ? '#f0fdf4' : step.current ? '#fffbeb' : '#f8fafc',
                              color: step.approved ? '#16a34a' : step.current ? '#d97706' : '#94a3b8',
                          }}>
                            {step.approved ? '✅' : step.current ? '⏳' : '○'} {step.role}
                          </span>
                                                    {i < leave.approvalSteps.length - 1 && (
                                                        <span style={{ color: '#cbd5e1', fontSize: '12px' }}>→</span>
                                                    )}
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>
            )}
        </DashboardLayout>
    );
}