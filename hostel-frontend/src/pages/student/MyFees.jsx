import { useQuery } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { feeService } from '../../services/feeService';

const statusStyle = (status) => {
    const map = {
        PAID: { bg: '#f0fdf4', color: '#16a34a', icon: '✅' },
        PENDING: { bg: '#fffbeb', color: '#d97706', icon: '⏳' },
        OVERDUE: { bg: '#fef2f2', color: '#dc2626', icon: '🚨' },
    };
    return map[status] || { bg: '#f1f5f9', color: '#475569', icon: '❓' };
};

export default function MyFees() {
    const { data: fees = [], isLoading } = useQuery({
        queryKey: ['myFees'],
        queryFn: () => feeService.getMyFees().then(r => r.data),
        retry: false,
    });

    const totalDue = fees.filter(f => f.status !== 'PAID').reduce((sum, f) => sum + (f.amount || 0), 0);
    const totalPaid = fees.filter(f => f.status === 'PAID').reduce((sum, f) => sum + (f.amount || 0), 0);
    const hasOverdue = fees.some(f => f.status === 'OVERDUE');

    const downloadReceipt = async (id) => {
        try {
            const res = await feeService.getReceipt(id);
            const url = window.URL.createObjectURL(new Blob([res.data]));
            const a = document.createElement('a');
            a.href = url;
            a.download = `receipt-${id}.pdf`;
            a.click();
        } catch {
            toast.error('Receipt not available yet');
        }
    };

    return (
        <DashboardLayout>
            <div style={{ marginBottom: '24px' }}>
                <h1 style={{ fontSize: '22px', fontWeight: '700', color: '#0f172a' }}>Fee Status</h1>
                <p style={{ fontSize: '14px', color: '#64748b', marginTop: '4px' }}>Track your hostel fee payments</p>
            </div>

            {/* Summary cards */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', marginBottom: '24px' }}>
                <div style={{ background: 'white', borderRadius: '12px', border: '1px solid #f1f5f9', padding: '20px' }}>
                    <p style={{ fontSize: '12px', color: '#64748b', fontWeight: '500', textTransform: 'uppercase' }}>Total Paid</p>
                    <p style={{ fontSize: '24px', fontWeight: '700', color: '#16a34a', marginTop: '4px' }}>₹{totalPaid.toLocaleString()}</p>
                </div>
                <div style={{ background: 'white', borderRadius: '12px', border: '1px solid #f1f5f9', padding: '20px' }}>
                    <p style={{ fontSize: '12px', color: '#64748b', fontWeight: '500', textTransform: 'uppercase' }}>Amount Due</p>
                    <p style={{ fontSize: '24px', fontWeight: '700', color: '#d97706', marginTop: '4px' }}>₹{totalDue.toLocaleString()}</p>
                </div>
                <div style={{ background: hasOverdue ? '#fef2f2' : 'white', borderRadius: '12px', border: `1px solid ${hasOverdue ? '#fecaca' : '#f1f5f9'}`, padding: '20px' }}>
                    <p style={{ fontSize: '12px', color: '#64748b', fontWeight: '500', textTransform: 'uppercase' }}>Status</p>
                    <p style={{ fontSize: '20px', fontWeight: '700', color: hasOverdue ? '#dc2626' : '#16a34a', marginTop: '4px' }}>
                        {hasOverdue ? '🚨 Has Overdue' : '✅ All Clear'}
                    </p>
                </div>
            </div>

            {hasOverdue && (
                <div style={{
                    background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '10px',
                    padding: '14px 18px', marginBottom: '20px', fontSize: '14px', color: '#dc2626',
                }}>
                    ⚠️ You have overdue fees. Leave approvals may be blocked until dues are cleared.
                </div>
            )}

            {/* Fee list */}
            {isLoading ? (
                <div style={{ textAlign: 'center', padding: '60px', color: '#94a3b8' }}>Loading...</div>
            ) : fees.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '60px', background: 'white', borderRadius: '12px', border: '1px solid #f1f5f9' }}>
                    <p style={{ color: '#64748b', fontSize: '14px' }}>No fee records found</p>
                </div>
            ) : (
                <div style={{ background: 'white', borderRadius: '12px', border: '1px solid #f1f5f9', overflow: 'hidden' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <thead>
                        <tr style={{ background: '#f8fafc', borderBottom: '1px solid #f1f5f9' }}>
                            {['Fee Type', 'Amount', 'Due Date', 'Status', 'Action'].map(h => (
                                <th key={h} style={{ padding: '12px 16px', textAlign: 'left', fontSize: '12px', fontWeight: '600', color: '#64748b', textTransform: 'uppercase' }}>
                                    {h}
                                </th>
                            ))}
                        </tr>
                        </thead>
                        <tbody>
                        {fees.map((fee, i) => {
                            const ss = statusStyle(fee.status);
                            return (
                                <tr key={fee.id} style={{ borderBottom: i < fees.length - 1 ? '1px solid #f8fafc' : 'none' }}>
                                    <td style={{ padding: '14px 16px', fontSize: '14px', fontWeight: '500', color: '#0f172a' }}>
                                        {fee.feeType}
                                    </td>
                                    <td style={{ padding: '14px 16px', fontSize: '14px', color: '#374151', fontWeight: '600' }}>
                                        ₹{fee.amount?.toLocaleString()}
                                    </td>
                                    <td style={{ padding: '14px 16px', fontSize: '13px', color: '#64748b' }}>
                                        {fee.dueDate}
                                    </td>
                                    <td style={{ padding: '14px 16px' }}>
                      <span style={{
                          fontSize: '12px', fontWeight: '600', padding: '3px 10px',
                          borderRadius: '20px', background: ss.bg, color: ss.color,
                      }}>
                        {ss.icon} {fee.status}
                      </span>
                                    </td>
                                    <td style={{ padding: '14px 16px' }}>
                                        {fee.status === 'PAID' && (
                                            <button
                                                onClick={() => downloadReceipt(fee.id)}
                                                style={{
                                                    background: 'none', border: '1px solid #e2e8f0', borderRadius: '6px',
                                                    padding: '5px 12px', fontSize: '12px', color: '#475569', cursor: 'pointer',
                                                }}
                                            >
                                                📄 Receipt
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            );
                        })}
                        </tbody>
                    </table>
                </div>
            )}
        </DashboardLayout>
    );
}