import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { leaveService } from '../../services/leaveService';

const leaveTypes = [
    { value: 'OUTPASS', label: 'Outpass', desc: 'Few hours outing', chain: 'Warden → Parent' },
    { value: 'CASUAL', label: 'Casual Leave', desc: '1–2 days', chain: 'Warden → HOD → Parent' },
    { value: 'HOLIDAY', label: 'Holiday Leave', desc: 'Vacation period', chain: 'Warden → Parent' },
    { value: 'MEDICAL', label: 'Medical Leave', desc: 'Health related', chain: 'Warden → Parent + Document' },
    { value: 'EMERGENCY', label: 'Emergency Leave', desc: 'Urgent situations', chain: 'Warden Override' },
];

export default function LeaveApply() {
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();
    const { register, handleSubmit, watch, formState: { errors } } = useForm();
    const selectedType = watch('leaveType');
    const selectedInfo = leaveTypes.find(l => l.value === selectedType);

    const inputStyle = {
        width: '100%', border: '1px solid #e2e8f0', borderRadius: '8px',
        padding: '10px 14px', fontSize: '14px', outline: 'none', boxSizing: 'border-box',
    };

    const labelStyle = {
        display: 'block', fontSize: '13px', fontWeight: '500',
        color: '#374151', marginBottom: '6px',
    };

    const onSubmit = async (data) => {
        setLoading(true);
        try {
            await leaveService.apply(data);
            toast.success('Leave request submitted successfully!');
            navigate('/student/leave/history');
        } catch (err) {
            toast.error(err.response?.data || 'Failed to submit leave request');
        } finally {
            setLoading(false);
        }
    };

    return (
        <DashboardLayout>
            <div style={{ maxWidth: '600px' }}>
                <div style={{ marginBottom: '24px' }}>
                    <h1 style={{ fontSize: '22px', fontWeight: '700', color: '#0f172a' }}>Apply for Leave</h1>
                    <p style={{ fontSize: '14px', color: '#64748b', marginTop: '4px' }}>
                        Fill in the details below. Your request will go through the approval chain automatically.
                    </p>
                </div>

                <div style={{ background: 'white', borderRadius: '12px', border: '1px solid #f1f5f9', padding: '28px' }}>
                    <form onSubmit={handleSubmit(onSubmit)}>

                        {/* Leave Type */}
                        <div style={{ marginBottom: '20px' }}>
                            <label style={labelStyle}>Leave Type</label>
                            <select
                                {...register('leaveType', { required: 'Please select a leave type' })}
                                style={inputStyle}
                            >
                                <option value="">Select leave type...</option>
                                {leaveTypes.map(t => (
                                    <option key={t.value} value={t.value}>{t.label} — {t.desc}</option>
                                ))}
                            </select>
                            {errors.leaveType && <p style={{ color: '#ef4444', fontSize: '12px', marginTop: '4px' }}>{errors.leaveType.message}</p>}

                            {selectedInfo && (
                                <div style={{
                                    marginTop: '8px', padding: '10px 14px', borderRadius: '8px',
                                    background: '#eff6ff', border: '1px solid #bfdbfe',
                                    fontSize: '13px', color: '#1d4ed8',
                                }}>
                                    📋 Approval chain: <strong>{selectedInfo.chain}</strong>
                                </div>
                            )}
                        </div>

                        {/* Date range */}
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '20px' }}>
                            <div>
                                <label style={labelStyle}>From Date</label>
                                <input
                                    type="date"
                                    {...register('fromDate', { required: 'From date is required' })}
                                    style={inputStyle}
                                />
                                {errors.fromDate && <p style={{ color: '#ef4444', fontSize: '12px', marginTop: '4px' }}>{errors.fromDate.message}</p>}
                            </div>
                            <div>
                                <label style={labelStyle}>To Date</label>
                                <input
                                    type="date"
                                    {...register('toDate', { required: 'To date is required' })}
                                    style={inputStyle}
                                />
                                {errors.toDate && <p style={{ color: '#ef4444', fontSize: '12px', marginTop: '4px' }}>{errors.toDate.message}</p>}
                            </div>
                        </div>

                        {/* Return time for outpass */}
                        {selectedType === 'OUTPASS' && (
                            <div style={{ marginBottom: '20px' }}>
                                <label style={labelStyle}>Expected Return Time</label>
                                <input
                                    type="time"
                                    {...register('returnTime')}
                                    style={inputStyle}
                                />
                            </div>
                        )}

                        {/* Reason */}
                        <div style={{ marginBottom: '20px' }}>
                            <label style={labelStyle}>Reason</label>
                            <textarea
                                {...register('reason', { required: 'Please provide a reason', minLength: { value: 10, message: 'Reason too short' } })}
                                rows={3}
                                placeholder="Describe your reason clearly..."
                                style={{ ...inputStyle, resize: 'none' }}
                            />
                            {errors.reason && <p style={{ color: '#ef4444', fontSize: '12px', marginTop: '4px' }}>{errors.reason.message}</p>}
                        </div>

                        {/* Place to visit */}
                        <div style={{ marginBottom: '20px' }}>
                            <label style={labelStyle}>Place to Visit / Destination</label>
                            <input
                                {...register('destination')}
                                placeholder="Home address or destination"
                                style={inputStyle}
                            />
                        </div>

                        {/* Document upload */}
                        <div style={{ marginBottom: '24px' }}>
                            <label style={labelStyle}>
                                Supporting Document
                                <span style={{ color: '#94a3b8', fontWeight: '400', marginLeft: '6px' }}>
                  {selectedType === 'MEDICAL' ? '(Required for medical leave)' : '(Optional)'}
                </span>
                            </label>
                            <input
                                type="file"
                                accept=".jpg,.jpeg,.png,.pdf"
                                {...register('document')}
                                style={{ ...inputStyle, padding: '8px' }}
                            />
                            <p style={{ fontSize: '12px', color: '#94a3b8', marginTop: '4px' }}>
                                Accepted: JPG, PNG, PDF (max 5MB)
                            </p>
                        </div>

                        {/* Buttons */}
                        <div style={{ display: 'flex', gap: '12px' }}>
                            <button
                                type="submit"
                                disabled={loading}
                                style={{
                                    background: loading ? '#93c5fd' : '#1E40AF',
                                    color: 'white', border: 'none', borderRadius: '8px',
                                    padding: '10px 24px', fontSize: '14px', fontWeight: '600',
                                    cursor: loading ? 'not-allowed' : 'pointer',
                                }}
                            >
                                {loading ? 'Submitting...' : '📤 Submit Request'}
                            </button>
                            <button
                                type="button"
                                onClick={() => navigate('/student')}
                                style={{
                                    background: 'none', color: '#64748b', border: '1px solid #e2e8f0',
                                    borderRadius: '8px', padding: '10px 20px', fontSize: '14px', cursor: 'pointer',
                                }}
                            >
                                Cancel
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </DashboardLayout>
    );
}