import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { visitorService } from '../../services/visitorService';

export default function VisitorLog() {
    const qc = useQueryClient();
    const [name, setName] = useState('');
    const [phone, setPhone] = useState('');
    const [purpose, setPurpose] = useState('');
    const [hostStudentId, setHostStudentId] = useState('');
    const [selectedFile, setSelectedFile] = useState(null);
    const [submitting, setSubmitting] = useState(false);
    const [checkingOutId, setCheckingOutId] = useState(null);

    const { data: activeVisitors = [], isLoading } = useQuery({
        queryKey: ['activeVisitors'],
        queryFn: () => visitorService.getActive().then(r => r.data),
        retry: false,
    });

    const handleFileChange = (e) => {
        if (e.target.files && e.target.files[0]) {
            setSelectedFile(e.target.files[0]);
        }
    };

    const handleCheckIn = async (e) => {
        e.preventDefault();
        if (!name.trim()) {
            toast.error('Visitor name is required');
            return;
        }

        setSubmitting(true);
        const toastId = toast.loading('Checking in visitor...');
        try {
            const res = await visitorService.checkIn({
                visitorName: name,
                visitorPhone: phone,
                purpose,
                hostStudentId
            });

            const visitorId = res.data.id;

            // If photo was selected, upload it
            if (selectedFile) {
                toast.loading('Uploading visitor photo...', { id: toastId });
                await visitorService.uploadPhoto(visitorId, selectedFile);
            }

            toast.success('Visitor checked in successfully!', { id: toastId });
            setName('');
            setPhone('');
            setPurpose('');
            setHostStudentId('');
            setSelectedFile(null);
            // Reset the file input
            const fileInput = document.getElementById('visitor-photo-input');
            if (fileInput) fileInput.value = '';

            qc.invalidateQueries(['activeVisitors']);
        } catch (err) {
            const msg = err?.response?.data?.error || 'Failed to check in visitor';
            toast.error(msg, { id: toastId });
        } finally {
            setSubmitting(false);
        }
    };

    const handleCheckOut = async (id) => {
        setCheckingOutId(id);
        const toastId = toast.loading('Checking out visitor...');
        try {
            await visitorService.checkOut(id);
            toast.success('Visitor checked out successfully', { id: toastId });
            qc.invalidateQueries(['activeVisitors']);
        } catch (err) {
            const msg = err?.response?.data?.error || 'Failed to check out visitor';
            toast.error(msg, { id: toastId });
        } finally {
            setCheckingOutId(null);
        }
    };

    return (
        <DashboardLayout>
            <div style={{ marginBottom: '28px' }}>
                <h1 style={{ fontSize: '24px', fontWeight: '800', color: '#0f172a', margin: 0 }}>
                    📷 Visitor Gate Pass & Log
                </h1>
                <p style={{ fontSize: '14px', color: '#64748b', marginTop: '6px' }}>
                    Check-in new visitors, upload verification photos, and manage active visitors.
                </p>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '24px', alignItems: 'start' }}>
                {/* Check-In Form */}
                <div style={{ background: 'white', borderRadius: '14px', border: '1px solid #e2e8f0', padding: '24px', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                    <h2 style={{ fontSize: '16px', fontWeight: '700', color: '#0f172a', marginTop: 0, marginBottom: '20px' }}>
                        ➕ New Check-In
                    </h2>
                    <form onSubmit={handleCheckIn} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                        <div>
                            <label style={{ fontSize: '12px', fontWeight: '600', color: '#475569', marginBottom: '6px', display: 'block', textTransform: 'uppercase' }}>
                                Visitor Name *
                            </label>
                            <input
                                type="text"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="Enter visitor's full name"
                                style={{ width: '100%', padding: '10px 14px', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '14px', boxSizing: 'border-box' }}
                                required
                            />
                        </div>

                        <div>
                            <label style={{ fontSize: '12px', fontWeight: '600', color: '#475569', marginBottom: '6px', display: 'block', textTransform: 'uppercase' }}>
                                Visitor Phone
                            </label>
                            <input
                                type="tel"
                                value={phone}
                                onChange={(e) => setPhone(e.target.value)}
                                placeholder="e.g. +91 98765 43210"
                                style={{ width: '100%', padding: '10px 14px', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '14px', boxSizing: 'border-box' }}
                            />
                        </div>

                        <div>
                            <label style={{ fontSize: '12px', fontWeight: '600', color: '#475569', marginBottom: '6px', display: 'block', textTransform: 'uppercase' }}>
                                Purpose of Visit
                            </label>
                            <input
                                type="text"
                                value={purpose}
                                onChange={(e) => setPurpose(e.target.value)}
                                placeholder="e.g. Guardian meet, delivery"
                                style={{ width: '100%', padding: '10px 14px', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '14px', boxSizing: 'border-box' }}
                            />
                        </div>

                        <div>
                            <label style={{ fontSize: '12px', fontWeight: '600', color: '#475569', marginBottom: '6px', display: 'block', textTransform: 'uppercase' }}>
                                Host Student ID
                            </label>
                            <input
                                type="text"
                                value={hostStudentId}
                                onChange={(e) => setHostStudentId(e.target.value)}
                                placeholder="Student registration ID"
                                style={{ width: '100%', padding: '10px 14px', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '14px', boxSizing: 'border-box' }}
                            />
                        </div>

                        <div>
                            <label style={{ fontSize: '12px', fontWeight: '600', color: '#475569', marginBottom: '6px', display: 'block', textTransform: 'uppercase' }}>
                                Photo Verification (Optional)
                            </label>
                            <input
                                id="visitor-photo-input"
                                type="file"
                                accept="image/*"
                                onChange={handleFileChange}
                                style={{ fontSize: '13px', color: '#64748b' }}
                            />
                        </div>

                        <button
                            type="submit"
                            disabled={submitting}
                            style={{
                                background: 'linear-gradient(135deg, #0f172a, #1e293b)',
                                color: 'white',
                                border: 'none',
                                borderRadius: '8px',
                                padding: '12px 20px',
                                fontSize: '14px',
                                fontWeight: '600',
                                cursor: 'pointer',
                                marginTop: '8px',
                                opacity: submitting ? 0.7 : 1,
                                transition: 'opacity 0.2s',
                            }}
                        >
                            {submitting ? '⏳ Checking In...' : '🔑 Complete Check-In'}
                        </button>
                    </form>
                </div>

                {/* Active Visitors Table */}
                <div style={{ background: 'white', borderRadius: '14px', border: '1px solid #e2e8f0', padding: '24px', boxShadow: '0 1px 3px rgba(0,0,0,0.05)', overflow: 'hidden' }}>
                    <h2 style={{ fontSize: '16px', fontWeight: '700', color: '#0f172a', marginTop: 0, marginBottom: '20px' }}>
                        🟢 Currently Inside ({activeVisitors.length})
                    </h2>

                    {isLoading ? (
                        <div style={{ textAlign: 'center', padding: '40px', color: '#94a3b8' }}>Loading active visitors...</div>
                    ) : activeVisitors.length === 0 ? (
                        <div style={{ textAlign: 'center', padding: '60px', color: '#94a3b8' }}>
                            <div style={{ fontSize: '32px', marginBottom: '8px' }}>🍃</div>
                            <p style={{ fontSize: '14px', margin: 0 }}>No active visitors inside right now.</p>
                        </div>
                    ) : (
                        <div style={{ overflowX: 'auto' }}>
                            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                                <thead>
                                    <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
                                        <th style={{ padding: '12px 16px', fontSize: '11px', fontWeight: '700', color: '#64748b', textTransform: 'uppercase' }}>Photo</th>
                                        <th style={{ padding: '12px 16px', fontSize: '11px', fontWeight: '700', color: '#64748b', textTransform: 'uppercase' }}>Visitor Details</th>
                                        <th style={{ padding: '12px 16px', fontSize: '11px', fontWeight: '700', color: '#64748b', textTransform: 'uppercase' }}>Host ID</th>
                                        <th style={{ padding: '12px 16px', fontSize: '11px', fontWeight: '700', color: '#64748b', textTransform: 'uppercase' }}>Check-In Time</th>
                                        <th style={{ padding: '12px 16px', fontSize: '11px', fontWeight: '700', color: '#64748b', textTransform: 'uppercase' }}>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {activeVisitors.map((v) => {
                                        const dateStr = v.checkInAt ? new Date(v.checkInAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '—';
                                        return (
                                            <tr key={v.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                                                <td style={{ padding: '14px 16px' }}>
                                                    {v.photoUrl ? (
                                                        <img
                                                            src={v.photoUrl}
                                                            alt={v.visitorName}
                                                            style={{ width: '40px', height: '40px', borderRadius: '8px', objectFit: 'cover', border: '1px solid #cbd5e1' }}
                                                        />
                                                    ) : (
                                                        <div style={{ width: '40px', height: '40px', borderRadius: '8px', background: '#f1f5f9', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '18px', color: '#94a3b8', border: '1px solid #cbd5e1' }}>
                                                            👤
                                                        </div>
                                                    )}
                                                </td>
                                                <td style={{ padding: '14px 16px' }}>
                                                    <div style={{ fontSize: '14px', fontWeight: '600', color: '#0f172a' }}>{v.visitorName}</div>
                                                    {v.visitorPhone && <div style={{ fontSize: '12px', color: '#64748b' }}>📞 {v.visitorPhone}</div>}
                                                    {v.purpose && <div style={{ fontSize: '12px', color: '#64748b', fontStyle: 'italic' }}>📌 {v.purpose}</div>}
                                                </td>
                                                <td style={{ padding: '14px 16px', fontSize: '13px', color: '#334155', fontWeight: '500' }}>
                                                    {v.hostStudentId || '—'}
                                                </td>
                                                <td style={{ padding: '14px 16px', fontSize: '13px', color: '#64748b' }}>
                                                    {dateStr}
                                                </td>
                                                <td style={{ padding: '14px 16px' }}>
                                                    <button
                                                        onClick={() => handleCheckOut(v.id)}
                                                        disabled={checkingOutId === v.id}
                                                        style={{
                                                            background: '#fee2e2',
                                                            color: '#dc2626',
                                                            border: '1px solid #fca5a5',
                                                            borderRadius: '6px',
                                                            padding: '6px 12px',
                                                            fontSize: '12px',
                                                            fontWeight: '600',
                                                            cursor: 'pointer',
                                                            opacity: checkingOutId === v.id ? 0.7 : 1,
                                                            transition: 'all 0.15s',
                                                        }}
                                                    >
                                                        {checkingOutId === v.id ? '⏳ Checkout...' : '🚪 Checkout'}
                                                    </button>
                                                </td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </div>
        </DashboardLayout>
    );
}
