import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';
import { authService } from '../../services/authService';

const roles = ['STUDENT', 'WARDEN', 'HOD', 'STAFF', 'PARENT', 'SECURITY_GUARD'];

export default function Register() {
    const [loading, setLoading] = useState(false);
    const [submitted, setSubmitted] = useState(false);
    const { register, handleSubmit, watch, formState: { errors } } = useForm();
    const selectedRole = watch('role');

    const onSubmit = async (data) => {
        setLoading(true);
        try {
            await authService.register(data);
            setSubmitted(true);
            toast.success('Registration request sent!');
        } catch (err) {
            toast.error(err.response?.data?.error || err.response?.data || 'Registration failed');
        } finally {
            setLoading(false);
        }
    };

    // Show success state after submission
    if (submitted) {
        return (
            <div style={{
                minHeight: '100vh',
                background: 'linear-gradient(135deg, #eff6ff 0%, #eef2ff 100%)',
                display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '16px',
            }}>
                <div style={{
                    background: 'white', borderRadius: '16px', border: '1px solid #e2e8f0',
                    width: '100%', maxWidth: '420px', padding: '40px', textAlign: 'center',
                    boxShadow: '0 4px 24px rgba(0,0,0,0.06)',
                }}>
                    <div style={{ fontSize: '56px', marginBottom: '16px' }}>⏳</div>
                    <h2 style={{ fontSize: '20px', fontWeight: '700', color: '#0f172a' }}>
                        Request Submitted!
                    </h2>
                    <p style={{ fontSize: '14px', color: '#64748b', marginTop: '10px', lineHeight: '1.6' }}>
                        Your registration request has been sent to the warden for verification.
                        You will be able to login once your account is approved.
                    </p>
                    <div style={{
                        marginTop: '20px', background: '#fffbeb', border: '1px solid #fde68a',
                        borderRadius: '10px', padding: '14px', fontSize: '13px', color: '#92400e',
                    }}>
                        📋 The warden will verify your details and approve or deny your request.
                    </div>
                    <Link to="/login" style={{
                        display: 'block', marginTop: '20px', background: '#1E40AF', color: 'white',
                        textDecoration: 'none', borderRadius: '8px', padding: '11px',
                        fontSize: '14px', fontWeight: '600',
                    }}>
                        Go to Login
                    </Link>
                </div>
            </div>
        );
    }

    const inputStyle = {
        width: '100%', border: '1px solid #e2e8f0', borderRadius: '8px',
        padding: '10px 14px', fontSize: '14px', outline: 'none', boxSizing: 'border-box',
    };

    return (
        <div style={{
            minHeight: '100vh',
            background: 'linear-gradient(135deg, #eff6ff 0%, #eef2ff 100%)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '16px',
        }}>
            <div style={{
                background: 'white', borderRadius: '16px', border: '1px solid #e2e8f0',
                width: '100%', maxWidth: '420px', padding: '40px',
                boxShadow: '0 4px 24px rgba(0,0,0,0.06)',
            }}>
                <div style={{ textAlign: 'center', marginBottom: '24px' }}>
                    <div style={{ fontSize: '40px', marginBottom: '10px' }}>🏨</div>
                    <h2 style={{ fontSize: '20px', fontWeight: '700', color: '#0f172a' }}>
                        Request Access
                    </h2>
                    <p style={{ fontSize: '13px', color: '#64748b', marginTop: '4px' }}>
                        Submit your details — warden will verify and approve
                    </p>
                </div>

                <div style={{
                    background: '#eff6ff', border: '1px solid #bfdbfe',
                    borderRadius: '8px', padding: '12px', marginBottom: '20px',
                    fontSize: '13px', color: '#1d4ed8',
                }}>
                    ℹ️ Your account will be activated only after warden approval.
                </div>

                <form onSubmit={handleSubmit(onSubmit)}>
                    {[
                        { name: 'name', label: 'Full Name', type: 'text', placeholder: 'Your full name' },
                        { name: 'email', label: 'Email Address', type: 'email', placeholder: 'you@college.edu' },
                        { name: 'phone', label: 'Phone Number', type: 'text', placeholder: '10-digit mobile number' },
                    ].map(field => (
                        <div key={field.name} style={{ marginBottom: '14px' }}>
                            <label style={{ display: 'block', fontSize: '13px', fontWeight: '500', color: '#374151', marginBottom: '6px' }}>
                                {field.label}
                            </label>
                            <input
                                type={field.type}
                                {...register(field.name, { required: `${field.label} is required` })}
                                placeholder={field.placeholder}
                                style={inputStyle}
                            />
                            {errors[field.name] && (
                                <p style={{ color: '#ef4444', fontSize: '12px', marginTop: '4px' }}>
                                    {errors[field.name].message}
                                </p>
                            )}
                        </div>
                    ))}

                    <div style={{ marginBottom: '14px' }}>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '500', color: '#374151', marginBottom: '6px' }}>
                            Role
                        </label>
                        <select {...register('role', { required: 'Select a role' })} style={inputStyle}>
                            <option value="">Select your role...</option>
                            {roles.map(r => <option key={r} value={r}>{r}</option>)}
                        </select>
                        {errors.role && (
                            <p style={{ color: '#ef4444', fontSize: '12px', marginTop: '4px' }}>
                                {errors.role.message}
                            </p>
                        )}
                    </div>

                    {selectedRole === 'PARENT' && (
                        <div style={{ marginBottom: '14px' }}>
                            <label style={{ display: 'block', fontSize: '13px', fontWeight: '500', color: '#374151', marginBottom: '6px' }}>
                                Child's Email or Student ID *
                            </label>
                            <input
                                type="text"
                                {...register('childEmailOrId', { required: "Child's email or student ID is required for PARENT registration" })}
                                placeholder="Child's email address or ID"
                                style={inputStyle}
                            />
                            {errors.childEmailOrId && (
                                <p style={{ color: '#ef4444', fontSize: '12px', marginTop: '4px' }}>
                                    {errors.childEmailOrId.message}
                                </p>
                            )}
                        </div>
                    )}


                    <div style={{ marginBottom: '24px' }}>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: '500', color: '#374151', marginBottom: '6px' }}>
                            Password
                        </label>
                        <input
                            type="password"
                            {...register('password', { required: 'Password is required', minLength: { value: 6, message: 'Min 6 characters' } })}
                            placeholder="••••••••"
                            style={inputStyle}
                        />
                        {errors.password && (
                            <p style={{ color: '#ef4444', fontSize: '12px', marginTop: '4px' }}>
                                {errors.password.message}
                            </p>
                        )}
                    </div>

                    <div style={{ marginBottom: '20px', display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
                        <input
                            type="checkbox"
                            id="consentGiven"
                            {...register('consentGiven', { required: 'You must consent to data processing under DPDP Act 2023' })}
                            style={{ marginTop: '3px' }}
                        />
                        <label htmlFor="consentGiven" style={{ fontSize: '12px', color: '#4b5563', lineHeight: '1.4' }}>
                            I consent to the collection and processing of my personal data for the purpose of hostel management and onboarding as per India's Digital Personal Data Protection (DPDP) Act 2023.
                        </label>
                    </div>
                    {errors.consentGiven && (
                        <p style={{ color: '#ef4444', fontSize: '12px', marginTop: '-14px', marginBottom: '14px' }}>
                            {errors.consentGiven.message}
                        </p>
                    )}

                    <button
                        type="submit"
                        disabled={loading}
                        style={{
                            width: '100%', background: loading ? '#93c5fd' : '#1E40AF',
                            color: 'white', border: 'none', borderRadius: '8px',
                            padding: '11px', fontSize: '14px', fontWeight: '600',
                            cursor: loading ? 'not-allowed' : 'pointer',
                        }}
                    >
                        {loading ? 'Sending request...' : '📤 Send Registration Request'}
                    </button>
                </form>

                <p style={{ textAlign: 'center', fontSize: '13px', color: '#64748b', marginTop: '20px' }}>
                    Already approved?{' '}
                    <Link to="/login" style={{ color: '#1E40AF', fontWeight: '500', textDecoration: 'none' }}>
                        Sign in
                    </Link>
                </p>
            </div>
        </div>
    );
}