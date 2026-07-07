import React, { useState } from 'react';
import toast from 'react-hot-toast';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { emergencyService } from '../../services/emergencyService';

export default function EmergencyBroadcast() {
    const [scope, setScope] = useState('HOSTEL');
    const [blockName, setBlockName] = useState('');
    const [message, setMessage] = useState('');
    const [sending, setSending] = useState(false);
    const [showConfirm, setShowConfirm] = useState(false);

    const handleSendClick = (e) => {
        e.preventDefault();
        if (!message.trim()) {
            toast.error('Please enter a broadcast message');
            return;
        }
        if (scope === 'BLOCK' && !blockName.trim()) {
            toast.error('Block name is required for block scope');
            return;
        }
        setShowConfirm(true);
    };

    const triggerBroadcast = async () => {
        setShowConfirm(false);
        setSending(true);
        const toastId = toast.loading('Broadcasting emergency alert...');
        try {
            const res = await emergencyService.broadcast({
                scope,
                blockName: scope === 'BLOCK' ? blockName : undefined,
                message
            });
            toast.success(`Broadcast sent successfully to ${res.data.recipientCount} student(s)!`, { id: toastId });
            setMessage('');
            setBlockName('');
        } catch (err) {
            const msg = err?.response?.data?.error || 'Failed to send emergency broadcast';
            toast.error(msg, { id: toastId });
        } finally {
            setSending(false);
        }
    };

    return (
        <DashboardLayout>
            <div style={{ marginBottom: '28px' }}>
                <h1 style={{ fontSize: '24px', fontWeight: '800', color: '#0f172a', margin: 0 }}>
                    🚨 Emergency Broadcast
                </h1>
                <p style={{ fontSize: '14px', color: '#64748b', marginTop: '6px' }}>
                    Send critical alerts directly to students. Broadcast actions are audited and logged for accountability.
                </p>
            </div>

            <div style={{ maxWidth: '600px', background: 'white', borderRadius: '14px', border: '1px solid #e2e8f0', padding: '28px', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                <form onSubmit={handleSendClick} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                    <div>
                        <label style={{ fontSize: '12px', fontWeight: '700', color: '#475569', marginBottom: '8px', display: 'block', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                            Broadcast Target / Scope
                        </label>
                        <div style={{ display: 'flex', gap: '12px' }}>
                            <button
                                type="button"
                                onClick={() => setScope('HOSTEL')}
                                style={{
                                    flex: 1, padding: '12px', borderRadius: '10px', fontSize: '14px', fontWeight: '600', cursor: 'pointer',
                                    border: scope === 'HOSTEL' ? '2px solid #ef4444' : '1px solid #cbd5e1',
                                    background: scope === 'HOSTEL' ? '#fef2f2' : 'white',
                                    color: scope === 'HOSTEL' ? '#b91c1c' : '#475569',
                                    transition: 'all 0.15s'
                                }}
                            >
                                🏢 Entire Hostel
                            </button>
                            <button
                                type="button"
                                onClick={() => setScope('BLOCK')}
                                style={{
                                    flex: 1, padding: '12px', borderRadius: '10px', fontSize: '14px', fontWeight: '600', cursor: 'pointer',
                                    border: scope === 'BLOCK' ? '2px solid #ef4444' : '1px solid #cbd5e1',
                                    background: scope === 'BLOCK' ? '#fef2f2' : 'white',
                                    color: scope === 'BLOCK' ? '#b91c1c' : '#475569',
                                    transition: 'all 0.15s'
                                }}
                            >
                                🧱 Specific Block
                            </button>
                        </div>
                    </div>

                    {scope === 'BLOCK' && (
                        <div>
                            <label style={{ fontSize: '12px', fontWeight: '700', color: '#475569', marginBottom: '8px', display: 'block', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                                Block Name *
                            </label>
                            <input
                                type="text"
                                value={blockName}
                                onChange={(e) => setBlockName(e.target.value)}
                                placeholder="e.g. A, B, C"
                                style={{ width: '100%', padding: '10px 14px', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '14px', boxSizing: 'border-box' }}
                                required
                            />
                        </div>
                    )}

                    <div>
                        <label style={{ fontSize: '12px', fontWeight: '700', color: '#475569', marginBottom: '8px', display: 'block', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                            Alert Message *
                        </label>
                        <textarea
                            value={message}
                            onChange={(e) => setMessage(e.target.value)}
                            rows={5}
                            placeholder="Type emergency alert details here..."
                            style={{ width: '100%', padding: '12px 14px', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '14px', boxSizing: 'border-box', fontFamily: 'inherit', resize: 'vertical' }}
                            required
                        />
                    </div>

                    <div style={{ background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '10px', padding: '14px 18px', display: 'flex', gap: '10px', alignItems: 'center' }}>
                        <span style={{ fontSize: '20px' }}>⚠️</span>
                        <div style={{ fontSize: '12px', color: '#b91c1c', fontWeight: '500', lineHeight: '1.4' }}>
                            <strong>WARNING:</strong> This will send high-priority alerts to students instantly. Ensure the scope and message content are correct.
                        </div>
                    </div>

                    <button
                        type="submit"
                        disabled={sending}
                        style={{
                            background: 'linear-gradient(135deg, #dc2626, #b91c1c)',
                            color: 'white',
                            border: 'none',
                            borderRadius: '10px',
                            padding: '14px 20px',
                            fontSize: '15px',
                            fontWeight: '700',
                            cursor: 'pointer',
                            opacity: sending ? 0.7 : 1,
                            boxShadow: '0 4px 6px -1px rgba(220, 38, 38, 0.2)',
                            transition: 'opacity 0.2s',
                            letterSpacing: '0.02em'
                        }}
                    >
                        {sending ? '⏳ Sending Alert...' : '📢 Broadcast Alert Now'}
                    </button>
                </form>
            </div>

            {/* Custom Premium Confirmation Modal */}
            {showConfirm && (
                <div style={{
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    backgroundColor: 'rgba(15, 23, 42, 0.65)', backdropFilter: 'blur(4px)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    zIndex: 9999
                }}>
                    <div style={{
                        background: 'white', borderRadius: '16px', padding: '28px',
                        maxWidth: '440px', width: '90%', boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
                        border: '1px solid #f1f5f9', animation: 'scaleUp 0.15s ease-out'
                    }}>
                        <div style={{ fontSize: '36px', textAlign: 'center', marginBottom: '16px' }}>🚨</div>
                        <h3 style={{ fontSize: '18px', fontWeight: '800', color: '#0f172a', textAlign: 'center', margin: '0 0 10px' }}>
                            Confirm Emergency Broadcast
                        </h3>
                        <p style={{ fontSize: '14px', color: '#64748b', textAlign: 'center', margin: '0 0 20px', lineHeight: '1.5' }}>
                            Are you absolutely sure you want to broadcast this alert to <strong>{scope === 'BLOCK' ? `Block ${blockName}` : 'All Hostel Students'}</strong>?
                        </p>
                        <div style={{ display: 'flex', gap: '12px' }}>
                            <button
                                onClick={() => setShowConfirm(false)}
                                style={{
                                    flex: 1, padding: '12px', border: '1px solid #cbd5e1', borderRadius: '10px',
                                    fontSize: '14px', fontWeight: '600', cursor: 'pointer', background: 'white', color: '#475569'
                                }}
                            >
                                Cancel
                            </button>
                            <button
                                onClick={triggerBroadcast}
                                style={{
                                    flex: 1, padding: '12px', border: 'none', borderRadius: '10px',
                                    fontSize: '14px', fontWeight: '700', cursor: 'pointer', background: '#dc2626', color: 'white'
                                }}
                            >
                                Yes, Broadcast
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </DashboardLayout>
    );
}
