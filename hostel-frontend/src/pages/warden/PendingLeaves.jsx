import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { leaveService } from '../../services/leaveService';

export default function PendingLeaves() {
    const queryClient = useQueryClient();
    const [rejectingId, setRejectingId] = useState(null);
    const [rejectReason, setRejectReason] = useState('');
    const [overrideId, setOverrideId] = useState(null);
    const [overrideReason, setOverrideReason] = useState('');

    const { data: leaves = [], isLoading } = useQuery({
        queryKey: ['pendingLeaves'],
        queryFn: () => leaveService.getPending().then(r => r.data),
        retry: false,
    });

    const approveMutation = useMutation({
        mutationFn: (id) => leaveService.approve(id, 1),
        onSuccess: () => {
            toast.success('Leave approved!');
            queryClient.invalidateQueries(['pendingLeaves']);
        },
        onError: () => toast.error('Failed to approve'),
    });

    const rejectMutation = useMutation({
        mutationFn: ({ id, reason }) => leaveService.reject(id, reason),
        onSuccess: () => {
            toast.success('Leave rejected');
            setRejectingId(null);
            setRejectReason('');
            queryClient.invalidateQueries(['pendingLeaves']);
        },
    });

    const overrideMutation = useMutation({
        mutationFn: ({ id, reason }) => leaveService.emergencyOverride(id, { reason }),
        onSuccess: () => {
            toast.success('Emergency override applied. Super Admin notified.');
            setOverrideId(null);
            setOverrideReason('');
            queryClient.invalidateQueries(['pendingLeaves']);
        },
        onError: () => toast.error('Override failed'),
    });

    return (
        <DashboardLayout>
            <div style={{ marginBottom: '24px' }}>
                <h1 style={{ fontSize: '22px', fontWeight: '700', color: '#0f172a' }}>Pending Leave Requests</h1>
                <p style={{ fontSize: '14px', color: '#64748b', marginTop: '4px' }}>
                    {leaves.length} requests awaiting your approval
                </p>
            </div>

            {isLoading ? (
                <div style={{ textAlign: 'center', padding: '60px', color: '#94a3b8' }}>Loading...</div>
            ) : leaves.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '60px', background: 'white', borderRadius: '12px', border: '1px solid #f1f5f9' }}>
                    <div style={{ fontSize: '40px', marginBottom: '12px' }}>✅</div>
                    <p style={{ color: '#64748b', fontSize: '14px' }}>All caught up — no pending requests</p>
                </div>
            ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                    {leaves.map((leave) => (
                        <div key={leave.id} style={{
                            background: 'white', borderRadius: '12px',
                            border: '1px solid #f1f5f9', padding: '22px',
                        }}>
                            {/* Header */}
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '12px' }}>
                                <div>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
                                        <h3 style={{ fontSize: '16px', fontWeight: '600', color: '#0f172a' }}>{leave.studentName}</h3>
                                        <span style={{
                                            fontSize: '11px', fontWeight: '600', padding: '2px 10px',
                                            borderRadius: '20px', background: '#eff6ff', color: '#1d4ed8',
                                        }}>
                      {leave.leaveType}
                    </span>
                                    </div>
                                    <p style={{ fontSize: '13px', color: '#64748b' }}>🏠 Room: {leave.roomNo || 'N/A'} &nbsp;|&nbsp; 📅 {leave.fromDate} → {leave.toDate}</p>
                                </div>

                                {/* Action buttons */}
                                <div style={{ display: 'flex', gap: '8px' }}>
                                    <button
                                        onClick={() => approveMutation.mutate(leave.id)}
                                        disabled={approveMutation.isPending}
                                        style={{
                                            background: '#16a34a', color: 'white', border: 'none',
                                            borderRadius: '8px', padding: '8px 16px', fontSize: '13px',
                                            fontWeight: '500', cursor: 'pointer',
                                        }}
                                    >
                                        ✅ Approve
                                    </button>
                                    <button
                                        onClick={() => setRejectingId(leave.id)}
                                        style={{
                                            background: 'none', color: '#dc2626', border: '1px solid #fecaca',
                                            borderRadius: '8px', padding: '8px 16px', fontSize: '13px',
                                            fontWeight: '500', cursor: 'pointer',
                                        }}
                                    >
                                        ❌ Reject
                                    </button>
                                    <button
                                        onClick={() => setOverrideId(leave.id)}
                                        style={{
                                            background: '#fffbeb', color: '#b45309', border: '1px solid #fde68a',
                                            borderRadius: '8px', padding: '8px 14px', fontSize: '12px',
                                            fontWeight: '500', cursor: 'pointer',
                                        }}
                                    >
                                        ⚡ Override
                                    </button>
                                </div>
                            </div>

                            {/* Reason */}
                            <div style={{
                                background: '#f8fafc', borderRadius: '8px', padding: '12px 14px',
                                fontSize: '13px', color: '#475569', fontStyle: 'italic',
                            }}>
                                "{leave.reason}"
                            </div>

                            {/* Reject form */}
                            {rejectingId === leave.id && (
                                <div style={{ marginTop: '14px', paddingTop: '14px', borderTop: '1px solid #f1f5f9' }}>
                                    <p style={{ fontSize: '13px', fontWeight: '500', color: '#374151', marginBottom: '8px' }}>Rejection reason:</p>
                                    <input
                                        value={rejectReason}
                                        onChange={e => setRejectReason(e.target.value)}
                                        placeholder="Enter reason for rejection..."
                                        style={{
                                            width: '100%', border: '1px solid #e2e8f0', borderRadius: '8px',
                                            padding: '9px 12px', fontSize: '13px', outline: 'none', boxSizing: 'border-box',
                                        }}
                                    />
                                    <div style={{ display: 'flex', gap: '8px', marginTop: '10px' }}>
                                        <button
                                            onClick={() => rejectMutation.mutate({ id: leave.id, reason: rejectReason })}
                                            disabled={!rejectReason}
                                            style={{
                                                background: '#dc2626', color: 'white', border: 'none',
                                                borderRadius: '7px', padding: '8px 16px', fontSize: '13px', cursor: 'pointer',
                                            }}
                                        >
                                            Confirm Reject
                                        </button>
                                        <button
                                            onClick={() => { setRejectingId(null); setRejectReason(''); }}
                                            style={{
                                                background: 'none', border: '1px solid #e2e8f0', borderRadius: '7px',
                                                padding: '8px 14px', fontSize: '13px', color: '#64748b', cursor: 'pointer',
                                            }}
                                        >
                                            Cancel
                                        </button>
                                    </div>
                                </div>
                            )}

                            {/* Emergency override form */}
                            {overrideId === leave.id && (
                                <div style={{
                                    marginTop: '14px', paddingTop: '14px', borderTop: '1px solid #f1f5f9',
                                    background: '#fffbeb', borderRadius: '8px', padding: '14px',
                                }}>
                                    <p style={{ fontSize: '13px', fontWeight: '600', color: '#b45309', marginBottom: '6px' }}>
                                        ⚡ Emergency Override — Super Admin will be notified
                                    </p>
                                    <p style={{ fontSize: '12px', color: '#92400e', marginBottom: '10px' }}>
                                        This bypasses all approval levels. Provide a mandatory reason.
                                    </p>
                                    <input
                                        value={overrideReason}
                                        onChange={e => setOverrideReason(e.target.value)}
                                        placeholder="Mandatory: Enter reason for emergency override..."
                                        style={{
                                            width: '100%', border: '1px solid #fde68a', borderRadius: '8px',
                                            padding: '9px 12px', fontSize: '13px', outline: 'none',
                                            boxSizing: 'border-box', background: 'white',
                                        }}
                                    />
                                    <div style={{ display: 'flex', gap: '8px', marginTop: '10px' }}>
                                        <button
                                            onClick={() => overrideMutation.mutate({ id: leave.id, reason: overrideReason })}
                                            disabled={!overrideReason || overrideMutation.isPending}
                                            style={{
                                                background: '#d97706', color: 'white', border: 'none',
                                                borderRadius: '7px', padding: '8px 16px', fontSize: '13px', cursor: 'pointer',
                                            }}
                                        >
                                            {overrideMutation.isPending ? 'Processing...' : '⚡ Confirm Override'}
                                        </button>
                                        <button
                                            onClick={() => { setOverrideId(null); setOverrideReason(''); }}
                                            style={{
                                                background: 'none', border: '1px solid #e2e8f0', borderRadius: '7px',
                                                padding: '8px 14px', fontSize: '13px', color: '#64748b', cursor: 'pointer',
                                            }}
                                        >
                                            Cancel
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            )}
        </DashboardLayout>
    );
}