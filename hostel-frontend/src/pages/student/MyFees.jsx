import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { feeService } from '../../services/feeService';

/* ── Status chip config ─────────────────────────────────────────────────── */
const STATUS = {
    PAID:    { bg: 'rgba(22,163,74,0.12)',  color: '#16a34a', border: '#86efac', icon: '✅', label: 'Paid' },
    PENDING: { bg: 'rgba(217,119,6,0.12)',  color: '#d97706', border: '#fcd34d', icon: '⏳', label: 'Pending' },
    OVERDUE: { bg: 'rgba(220,38,38,0.12)',  color: '#dc2626', border: '#fca5a5', icon: '🚨', label: 'Overdue' },
};

/* ── Shared button style builder ─────────────────────────────────────────── */
const btn = (variant = 'ghost') => {
    const base = {
        border: 'none', borderRadius: '8px', padding: '7px 14px',
        fontSize: '12px', fontWeight: '600', cursor: 'pointer',
        display: 'inline-flex', alignItems: 'center', gap: '5px',
        transition: 'all 0.15s',
    };
    if (variant === 'pay')     return { ...base, background: 'linear-gradient(135deg,#6366f1,#8b5cf6)', color: '#fff' };
    if (variant === 'receipt') return { ...base, background: 'rgba(99,102,241,0.1)', color: '#6366f1', border: '1px solid rgba(99,102,241,0.25)' };
    return { ...base, background: '#f1f5f9', color: '#475569' };
};

export default function MyFees() {
    const qc = useQueryClient();
    const [paying, setPaying] = useState(null);     // feeId currently being paid
    const [downloading, setDownloading] = useState(null);

    const { data: fees = [], isLoading } = useQuery({
        queryKey: ['myFees'],
        queryFn: () => feeService.getMyFees().then(r => r.data),
        retry: false,
    });

    const totalDue   = fees.filter(f => f.status !== 'PAID').reduce((s, f) => s + (f.amount || 0), 0);
    const totalPaid  = fees.filter(f => f.status === 'PAID').reduce((s, f) => s + (f.amount || 0), 0);
    const hasOverdue = fees.some(f => f.status === 'OVERDUE');

    /* ── Pay Now ─────────────────────────────────────────────────────────── */
    const handlePay = async (fee) => {
        setPaying(fee.id);
        const toastId = toast.loading(`Processing payment for ${fee.feeType}…`);
        try {
            await feeService.payFee(fee.id);
            toast.success(`₹${fee.amount?.toLocaleString()} paid successfully!`, { id: toastId });
            qc.invalidateQueries(['myFees']);
        } catch (err) {
            const msg = err?.response?.data?.error || 'Payment failed. Please try again.';
            toast.error(msg, { id: toastId });
        } finally {
            setPaying(null);
        }
    };

    /* ── Download Receipt ────────────────────────────────────────────────── */
    const handleReceipt = async (fee) => {
        setDownloading(fee.id);
        try {
            const res = await feeService.getReceipt(fee.id);
            const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
            const a   = document.createElement('a');
            a.href     = url;
            a.download = `receipt-${fee.id}.pdf`;
            a.click();
            window.URL.revokeObjectURL(url);
            toast.success('Receipt downloaded!');
        } catch {
            toast.error('Receipt not available.');
        } finally {
            setDownloading(null);
        }
    };

    /* ── Render ──────────────────────────────────────────────────────────── */
    return (
        <DashboardLayout>
            {/* Page header */}
            <div style={{ marginBottom: '28px' }}>
                <h1 style={{ fontSize: '24px', fontWeight: '800', color: '#0f172a', margin: 0 }}>
                    💳 Fee Status
                </h1>
                <p style={{ fontSize: '14px', color: '#64748b', marginTop: '6px' }}>
                    View, pay, and download receipts for your hostel fees.
                </p>
            </div>

            {/* Summary strip */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: '16px', marginBottom: '24px' }}>
                {[
                    { label: 'Total Paid',  value: `₹${totalPaid.toLocaleString()}`,  color: '#16a34a', bg: 'rgba(22,163,74,0.07)'  },
                    { label: 'Amount Due',  value: `₹${totalDue.toLocaleString()}`,   color: '#d97706', bg: 'rgba(217,119,6,0.07)'  },
                    {
                        label: 'Account Health',
                        value: hasOverdue ? '🚨 Overdue' : '✅ Clear',
                        color: hasOverdue ? '#dc2626' : '#16a34a',
                        bg:    hasOverdue ? 'rgba(220,38,38,0.07)' : 'rgba(22,163,74,0.07)',
                    },
                ].map(c => (
                    <div key={c.label} style={{
                        background: c.bg, borderRadius: '14px', padding: '20px 22px',
                        border: `1px solid ${c.color}22`,
                    }}>
                        <p style={{ fontSize: '11px', fontWeight: '600', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.05em', margin: 0 }}>{c.label}</p>
                        <p style={{ fontSize: '22px', fontWeight: '800', color: c.color, marginTop: '6px', marginBottom: 0 }}>{c.value}</p>
                    </div>
                ))}
            </div>

            {/* Overdue warning banner */}
            {hasOverdue && (
                <div style={{
                    background: 'rgba(220,38,38,0.07)', border: '1px solid #fca5a5',
                    borderRadius: '10px', padding: '13px 18px', marginBottom: '20px',
                    fontSize: '13px', color: '#dc2626', fontWeight: '500',
                }}>
                    ⚠️ You have overdue fees. Leave applications may be blocked until dues are cleared.
                </div>
            )}

            {/* Fee table */}
            {isLoading ? (
                <div style={{ textAlign: 'center', padding: '60px', color: '#94a3b8' }}>Loading fees…</div>
            ) : fees.length === 0 ? (
                <div style={{
                    textAlign: 'center', padding: '60px', background: 'white',
                    borderRadius: '14px', border: '1px solid #f1f5f9',
                }}>
                    <p style={{ color: '#94a3b8', fontSize: '14px' }}>No fee records found.</p>
                </div>
            ) : (
                <div style={{ background: 'white', borderRadius: '14px', border: '1px solid #e2e8f0', overflow: 'hidden', boxShadow: '0 1px 4px rgba(0,0,0,0.05)' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <thead>
                            <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
                                {['Fee Type', 'Amount', 'Due Date', 'Paid Date', 'Status', 'Actions'].map(h => (
                                    <th key={h} style={{
                                        padding: '13px 16px', textAlign: 'left',
                                        fontSize: '11px', fontWeight: '700',
                                        color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.05em',
                                    }}>{h}</th>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {fees.map((fee, i) => {
                                const ss = STATUS[fee.status] || STATUS.PENDING;
                                const isLast = i === fees.length - 1;
                                const isPayingThis = paying === fee.id;
                                const isDlThis     = downloading === fee.id;
                                return (
                                    <tr key={fee.id} style={{
                                        borderBottom: isLast ? 'none' : '1px solid #f1f5f9',
                                        transition: 'background 0.1s',
                                    }}
                                        onMouseEnter={e => e.currentTarget.style.background = '#fafafa'}
                                        onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                                    >
                                        {/* Fee type */}
                                        <td style={{ padding: '15px 16px', fontSize: '14px', fontWeight: '600', color: '#0f172a' }}>
                                            {fee.feeType}
                                        </td>
                                        {/* Amount */}
                                        <td style={{ padding: '15px 16px', fontSize: '14px', fontWeight: '700', color: '#334155' }}>
                                            ₹{fee.amount?.toLocaleString()}
                                        </td>
                                        {/* Due date */}
                                        <td style={{ padding: '15px 16px', fontSize: '13px', color: '#64748b' }}>
                                            {fee.dueDate || '—'}
                                        </td>
                                        {/* Paid date */}
                                        <td style={{ padding: '15px 16px', fontSize: '13px', color: fee.paidDate ? '#16a34a' : '#94a3b8' }}>
                                            {fee.paidDate || '—'}
                                        </td>
                                        {/* Status chip */}
                                        <td style={{ padding: '15px 16px' }}>
                                            <span style={{
                                                fontSize: '11px', fontWeight: '700', padding: '4px 11px',
                                                borderRadius: '20px', border: `1px solid ${ss.border}`,
                                                background: ss.bg, color: ss.color,
                                            }}>
                                                {ss.icon} {ss.label}
                                            </span>
                                        </td>
                                        {/* Action buttons */}
                                        <td style={{ padding: '15px 16px' }}>
                                            <div style={{ display: 'flex', gap: '8px' }}>
                                                {/* Pay Now — shown for PENDING and OVERDUE */}
                                                {fee.status !== 'PAID' && (
                                                    <button
                                                        id={`pay-btn-${fee.id}`}
                                                        disabled={isPayingThis}
                                                        onClick={() => handlePay(fee)}
                                                        style={{ ...btn('pay'), opacity: isPayingThis ? 0.7 : 1 }}
                                                    >
                                                        {isPayingThis ? '⏳ Processing…' : '💳 Pay Now'}
                                                    </button>
                                                )}
                                                {/* Download Receipt — only for PAID fees */}
                                                {fee.status === 'PAID' && (
                                                    <button
                                                        id={`receipt-btn-${fee.id}`}
                                                        disabled={isDlThis}
                                                        onClick={() => handleReceipt(fee)}
                                                        style={{ ...btn('receipt'), opacity: isDlThis ? 0.7 : 1 }}
                                                    >
                                                        {isDlThis ? '⏳ Downloading…' : '📄 Receipt'}
                                                    </button>
                                                )}
                                            </div>
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