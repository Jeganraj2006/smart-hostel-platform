import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import toast from 'react-hot-toast';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { registrationService } from '../../services/authService';

export default function PendingRegistrations() {
    const queryClient = useQueryClient();
    const [rejectId, setRejectId] = useState(null);
    const [rejectReason, setRejectReason] = useState('');

    const { data: users = [], isLoading, error } = useQuery({
        queryKey: ['pendingRegistrations'],
        queryFn: () => registrationService.getPending().then(r => r.data),
        retry: false,
    });

    const approveMutation = useMutation({
        mutationFn: (id) => registrationService.approve(id),
        onSuccess: () => {
            toast.success('User approved — they can now login!');
            queryClient.invalidateQueries(['pendingRegistrations']);
        },
        onError: () => toast.error('Approval failed'),
    });

    const rejectMutation = useMutation({
        mutationFn: ({ id, reason }) => registrationService.reject(id, reason),
        onSuccess: () => {
            toast.success('Registration rejected');
            setRejectId(null);
            setRejectReason('');
            queryClient.invalidateQueries(['pendingRegistrations']);
        },
    });

    return (
        <DashboardLayout>
            <div style={{ marginBottom: '24px' }}>
                <h1 style={{ fontSize: '22px', fontWeight: '700', color: '#0f172a' }}>
                    Pending Registrations
                </h1>
                <p style={{ fontSize: '14px', color: '#64748b', marginTop: '4px' }}>
                    {users.length} users awaiting your approval
                </p>
            </div>

            {isLoading ? (
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '60px' }}>
                    <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-slate-800 mb-4"></div>
                    <p style={{ color: '#64748b', fontSize: '14px' }}>Loading registrations...</p>
                </div>
            ) : error ? (
                <div style={{
                    background: '#fee2e2', borderRadius: '12px', border: '1px solid #fecaca',
                    padding: '24px', textAlign: 'center', color: '#991b1b', maxWidth: '500px', margin: '40px auto'
                }}>
                    <span style={{ fontSize: '32px', marginBottom: '8px', display: 'block' }}>⚠️</span>
                    <h3 style={{ fontSize: '16px', fontWeight: '700' }}>Failed to load pending registrations</h3>
                    <p style={{ fontSize: '13px', marginTop: '4px' }}>{error.message || 'Please check your connection.'}</p>
                </div>
            ) : users.length === 0 ? (
                <div style={{
                    background: 'white', borderRadius: '12px', border: '1px solid #f1f5f9',
                    padding: '60px', textAlign: 'center',
                }}>
                    <div style={{ fontSize: '40px', marginBottom: '12px' }}>✅</div>
                    <p style={{ color: '#64748b' }}>No pending registration requests</p>
                </div>
            ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    {users.map((user) => (
                        <div key={user.id} style={{
                            background: 'white', borderRadius: '12px',
                            border: '1px solid #f1f5f9', padding: '20px',
                        }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <div>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
                                        <h3 style={{ fontSize: '16px', fontWeight: '600', color: '#0f172a' }}>
                                            {user.name}
                                        </h3>
                                        <span style={{
                                            fontSize: '11px', fontWeight: '600', padding: '2px 10px',
                                            borderRadius: '20px', background: '#eff6ff', color: '#1d4ed8',
                                        }}>
                      {user.role}
                    </span>
                                    </div>
                                    <p style={{ fontSize: '13px', color: '#64748b' }}>
                                        📧 {user.email} &nbsp;|&nbsp; 📱 {user.phone}
                                    </p>
                                    <p style={{ fontSize: '12px', color: '#94a3b8', marginTop: '4px' }}>
                                        Requested: {new Date(user.createdAt).toLocaleDateString()}
                                    </p>
                                    {user.role === 'PARENT' && (
                                        <div style={{ fontSize: '12px', color: '#475569', fontWeight: '600', marginTop: '6px', background: '#f8fafc', border: '1px dashed #cbd5e1', borderRadius: '6px', padding: '6px 10px', display: 'inline-block' }}>
                                            🔗 Claimed Linkage: <span style={{ color: '#0f172a' }}>{user.childEmailOrId}</span>
                                        </div>
                                    )}
                                </div>

                                <div style={{ display: 'flex', gap: '8px' }}>
                                    <button
                                        onClick={() => approveMutation.mutate(user.id)}
                                        disabled={approveMutation.isPending}
                                        style={{
                                            background: '#16a34a', color: 'white', border: 'none',
                                            borderRadius: '8px', padding: '9px 18px', fontSize: '13px',
                                            fontWeight: '600', cursor: 'pointer',
                                        }}
                                    >
                                        ✅ Approve
                                    </button>
                                    <button
                                        onClick={() => setRejectId(user.id)}
                                        style={{
                                            background: 'none', color: '#dc2626', border: '1px solid #fecaca',
                                            borderRadius: '8px', padding: '9px 16px', fontSize: '13px',
                                            fontWeight: '500', cursor: 'pointer',
                                        }}
                                    >
                                        ❌ Reject
                                    </button>
                                </div>
                            </div>

                            {rejectId === user.id && (
                                <div style={{
                                    marginTop: '14px', paddingTop: '14px',
                                    borderTop: '1px solid #f1f5f9',
                                }}>
                                    <p style={{ fontSize: '13px', fontWeight: '500', color: '#374151', marginBottom: '8px' }}>
                                        Rejection reason:
                                    </p>
                                    <input
                                        value={rejectReason}
                                        onChange={e => setRejectReason(e.target.value)}
                                        placeholder="Enter reason..."
                                        style={{
                                            width: '100%', border: '1px solid #e2e8f0', borderRadius: '8px',
                                            padding: '9px 12px', fontSize: '13px', outline: 'none', boxSizing: 'border-box',
                                        }}
                                    />
                                    <div style={{ display: 'flex', gap: '8px', marginTop: '10px' }}>
                                        <button
                                            onClick={() => rejectMutation.mutate({ id: user.id, reason: rejectReason })}
                                            disabled={!rejectReason}
                                            style={{
                                                background: '#dc2626', color: 'white', border: 'none',
                                                borderRadius: '7px', padding: '8px 16px', fontSize: '13px', cursor: 'pointer',
                                            }}
                                        >
                                            Confirm Reject
                                        </button>
                                        <button
                                            onClick={() => { setRejectId(null); setRejectReason(''); }}
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