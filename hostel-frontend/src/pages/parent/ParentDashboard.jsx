import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { leaveService } from '../../services/leaveService';

export default function ParentDashboard() {
    const queryClient = useQueryClient();

    const { data: leaves = [], isLoading } = useQuery({
        queryKey: ['pendingLeaves'],
        queryFn: () => leaveService.getPending().then(r => r.data),
        retry: false,
    });

    const approveMutation = useMutation({
        mutationFn: (id) => leaveService.approve(id, 3),
        onSuccess: () => {
            toast.success('Leave approved — Outpass will be generated!');
            queryClient.invalidateQueries(['pendingLeaves']);
        },
    });

    const rejectMutation = useMutation({
        mutationFn: ({ id }) => leaveService.reject(id, 'Rejected by Parent'),
        onSuccess: () => {
            toast.success('Leave rejected');
            queryClient.invalidateQueries(['pendingLeaves']);
        },
    });

    return (
        <DashboardLayout>
            <div style={{ marginBottom: '24px' }}>
                <h1 style={{ fontSize: '22px', fontWeight: '700', color: '#0f172a' }}>Parent Portal</h1>
                <p style={{ fontSize: '14px', color: '#64748b', marginTop: '4px' }}>
                    Review and approve your ward's leave requests
                </p>
            </div>

            <div style={{ background: 'white', borderRadius: '12px', border: '1px solid #f1f5f9', padding: '20px' }}>
                <h2 style={{ fontSize: '16px', fontWeight: '600', marginBottom: '16px' }}>
                    Pending Approvals ({leaves.length})
                </h2>

                {isLoading ? (
                    <p style={{ color: '#94a3b8', textAlign: 'center', padding: '32px' }}>Loading...</p>
                ) : leaves.length === 0 ? (
                    <div style={{ textAlign: 'center', padding: '40px', color: '#94a3b8' }}>
                        <div style={{ fontSize: '32px', marginBottom: '8px' }}>✅</div>
                        <p>No pending approvals for your ward</p>
                    </div>
                ) : (
                    leaves.map((leave) => (
                        <div key={leave.id} style={{
                            background: '#fffbeb', border: '1px solid #fde68a',
                            borderRadius: '10px', padding: '20px', marginBottom: '12px',
                        }}>
                            <p style={{ fontWeight: '700', color: '#0f172a', fontSize: '16px' }}>
                                🎓 {leave.studentName}
                            </p>
                            <div style={{ marginTop: '10px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                                <div style={{ fontSize: '13px', color: '#475569' }}>
                                    <strong>Type:</strong> {leave.leaveType}
                                </div>
                                <div style={{ fontSize: '13px', color: '#475569' }}>
                                    <strong>Duration:</strong> {leave.fromDate} → {leave.toDate}
                                </div>
                                <div style={{ fontSize: '13px', color: '#475569', gridColumn: 'span 2' }}>
                                    <strong>Reason:</strong> {leave.reason}
                                </div>
                                <div style={{ fontSize: '13px', color: '#475569' }}>
                                    <strong>Destination:</strong> {leave.destination || 'N/A'}
                                </div>
                            </div>
                            <div style={{ display: 'flex', gap: '10px', marginTop: '14px' }}>
                                <button
                                    onClick={() => approveMutation.mutate(leave.id)}
                                    style={{
                                        background: '#16a34a', color: 'white', border: 'none',
                                        borderRadius: '8px', padding: '10px 20px', fontSize: '14px',
                                        fontWeight: '600', cursor: 'pointer',
                                    }}
                                >
                                    ✅ Approve — Generate Outpass
                                </button>
                                <button
                                    onClick={() => rejectMutation.mutate({ id: leave.id })}
                                    style={{
                                        background: 'none', color: '#dc2626', border: '1px solid #fecaca',
                                        borderRadius: '8px', padding: '10px 16px', fontSize: '14px', cursor: 'pointer',
                                    }}
                                >
                                    ❌ Reject
                                </button>
                            </div>
                        </div>
                    ))
                )}
            </div>
        </DashboardLayout>
    );
}