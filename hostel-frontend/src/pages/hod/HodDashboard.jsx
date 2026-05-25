import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { leaveService } from '../../services/leaveService';
import useAuthStore from '../../store/authStore';

export default function HodDashboard() {
    const { user } = useAuthStore();
    const queryClient = useQueryClient();

    const { data: leaves = [], isLoading } = useQuery({
        queryKey: ['pendingLeaves'],
        queryFn: () => leaveService.getPending().then(r => r.data),
        retry: false,
    });

    const approveMutation = useMutation({
        mutationFn: (id) => leaveService.approve(id, 2),
        onSuccess: () => {
            toast.success('Leave approved!');
            queryClient.invalidateQueries(['pendingLeaves']);
        },
    });

    const rejectMutation = useMutation({
        mutationFn: ({ id, reason }) => leaveService.reject(id, reason),
        onSuccess: () => {
            toast.success('Leave rejected');
            queryClient.invalidateQueries(['pendingLeaves']);
        },
    });

    return (
        <DashboardLayout>
            <div style={{ marginBottom: '24px' }}>
                <h1 style={{ fontSize: '22px', fontWeight: '700', color: '#0f172a' }}>
                    HOD Dashboard
                </h1>
                <p style={{ fontSize: '14px', color: '#64748b', marginTop: '4px' }}>
                    Welcome, {user?.name} — Pending approvals from your department
                </p>
            </div>

            <div style={{ background: 'white', borderRadius: '12px', border: '1px solid #f1f5f9', padding: '20px' }}>
                <h2 style={{ fontSize: '16px', fontWeight: '600', marginBottom: '16px' }}>
                    Pending Casual Leave Approvals ({leaves.length})
                </h2>

                {isLoading ? (
                    <p style={{ color: '#94a3b8', textAlign: 'center', padding: '32px' }}>Loading...</p>
                ) : leaves.length === 0 ? (
                    <div style={{ textAlign: 'center', padding: '40px', color: '#94a3b8' }}>
                        <div style={{ fontSize: '32px', marginBottom: '8px' }}>✅</div>
                        <p>No pending approvals</p>
                    </div>
                ) : (
                    leaves.map((leave) => (
                        <div key={leave.id} style={{
                            border: '1px solid #f1f5f9', borderRadius: '10px',
                            padding: '16px', marginBottom: '12px',
                        }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <div>
                                    <p style={{ fontWeight: '600', color: '#0f172a' }}>{leave.studentName}</p>
                                    <p style={{ fontSize: '13px', color: '#64748b', marginTop: '4px' }}>
                                        {leave.leaveType} · {leave.fromDate} → {leave.toDate}
                                    </p>
                                    <p style={{ fontSize: '13px', color: '#475569', marginTop: '4px', fontStyle: 'italic' }}>
                                        "{leave.reason}"
                                    </p>
                                </div>
                                <div style={{ display: 'flex', gap: '8px' }}>
                                    <button
                                        onClick={() => approveMutation.mutate(leave.id)}
                                        style={{
                                            background: '#16a34a', color: 'white', border: 'none',
                                            borderRadius: '8px', padding: '8px 16px', fontSize: '13px', cursor: 'pointer',
                                        }}
                                    >
                                        ✅ Approve
                                    </button>
                                    <button
                                        onClick={() => rejectMutation.mutate({ id: leave.id, reason: 'Rejected by HOD' })}
                                        style={{
                                            background: 'none', color: '#dc2626', border: '1px solid #fecaca',
                                            borderRadius: '8px', padding: '8px 16px', fontSize: '13px', cursor: 'pointer',
                                        }}
                                    >
                                        ❌ Reject
                                    </button>
                                </div>
                            </div>
                        </div>
                    ))
                )}
            </div>
        </DashboardLayout>
    );
}