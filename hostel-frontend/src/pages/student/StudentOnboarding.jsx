import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';
import { studentService } from '../../services/studentService';

export default function StudentOnboarding() {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { register, handleSubmit, formState: { errors } } = useForm();

  const onSubmit = async (data) => {
    setLoading(true);
    try {
      const payload = {
        ...data,
        cleanlinessLevel: parseInt(data.cleanlinessLevel, 10),
      };
      await studentService.submitPreferences(payload);
      toast.success('Roommate preferences saved successfully!');
      navigate('/student');
    } catch (err) {
      toast.error(err.response?.data?.error || err.response?.data || 'Failed to submit preferences');
    } finally {
      setLoading(false);
    }
  };

  const containerStyle = {
    minHeight: '100vh',
    background: 'linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '24px',
  };

  const cardStyle = {
    background: 'white',
    borderRadius: '16px',
    border: '1px solid #e2e8f0',
    width: '100%',
    maxWidth: '520px',
    padding: '40px',
    boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.05)',
  };

  const titleStyle = {
    fontSize: '24px',
    fontWeight: '700',
    color: '#0f172a',
    marginBottom: '8px',
    textAlign: 'center',
  };

  const subtitleStyle = {
    fontSize: '14px',
    color: '#64748b',
    marginBottom: '32px',
    textAlign: 'center',
    lineHeight: '1.5',
  };

  const groupStyle = {
    marginBottom: '20px',
  };

  const labelStyle = {
    display: 'block',
    fontSize: '14px',
    fontWeight: '600',
    color: '#334155',
    marginBottom: '8px',
  };

  const selectStyle = {
    width: '100%',
    border: '1px solid #cbd5e1',
    borderRadius: '8px',
    padding: '10px 14px',
    fontSize: '14px',
    color: '#334155',
    outline: 'none',
    boxSizing: 'border-box',
    background: 'white',
    cursor: 'pointer',
    transition: 'border-color 0.2s',
  };

  const inputStyle = {
    width: '100%',
    border: '1px solid #cbd5e1',
    borderRadius: '8px',
    padding: '10px 14px',
    fontSize: '14px',
    color: '#334155',
    outline: 'none',
    boxSizing: 'border-box',
    transition: 'border-color 0.2s',
  };

  const buttonStyle = {
    width: '100%',
    background: '#2563eb',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    padding: '12px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer',
    marginTop: '28px',
    transition: 'background-color 0.2s',
  };

  return (
    <div style={containerStyle}>
      <div style={cardStyle}>
        <h1 style={titleStyle}>Roommate Preferences Onboarding</h1>
        <p style={subtitleStyle}>
          Please take a moment to specify your preferences. This helps us ensure compatibility with your roommates.
        </p>

        <form onSubmit={handleSubmit(onSubmit)}>
          <div style={groupStyle}>
            <label style={labelStyle}>Sleep Schedule</label>
            <select {...register('sleepSchedule', { required: 'Please select your sleep schedule' })} style={selectStyle}>
              <option value="">Choose your schedule...</option>
              <option value="EARLY_BIRD">🌅 Early Bird (Sleep early, wake up early)</option>
              <option value="NIGHT_OWL">🦉 Night Owl (Sleep late, wake up late)</option>
            </select>
            {errors.sleepSchedule && <span style={{ color: '#ef4444', fontSize: '12px' }}>{errors.sleepSchedule.message}</span>}
          </div>

          <div style={groupStyle}>
            <label style={labelStyle}>Cleanliness Level</label>
            <select {...register('cleanlinessLevel', { required: 'Please select cleanliness level' })} style={selectStyle}>
              <option value="">Choose level...</option>
              <option value="1">⭐ 1 (Relaxed/Minimalist)</option>
              <option value="2">⭐⭐ 2 (Moderate cleanliness)</option>
              <option value="3">⭐⭐⭐ 3 (Average/Standard)</option>
              <option value="4">⭐⭐⭐⭐ 4 (Very clean)</option>
              <option value="5">⭐⭐⭐⭐⭐ 5 (Spotless/Meticulous)</option>
            </select>
            {errors.cleanlinessLevel && <span style={{ color: '#ef4444', fontSize: '12px' }}>{errors.cleanlinessLevel.message}</span>}
          </div>

          <div style={groupStyle}>
            <label style={labelStyle}>Study Habit</label>
            <select {...register('studyHabit', { required: 'Please select study habit' })} style={selectStyle}>
              <option value="">Choose habit...</option>
              <option value="SILENT">🔇 Silent (Requires absolute silence)</option>
              <option value="MUSIC_OK">🎵 Music OK (Background noise/music is fine)</option>
              <option value="GROUP_STUDY">👥 Group Study (Prefers working together)</option>
            </select>
            {errors.studyHabit && <span style={{ color: '#ef4444', fontSize: '12px' }}>{errors.studyHabit.message}</span>}
          </div>

          <div style={groupStyle}>
            <label style={labelStyle}>Preferred Language</label>
            <input
              type="text"
              placeholder="e.g. English, Spanish, Hindi, Tamil..."
              {...register('preferredLanguage', { required: 'Language is required' })}
              style={inputStyle}
            />
            {errors.preferredLanguage && <span style={{ color: '#ef4444', fontSize: '12px' }}>{errors.preferredLanguage.message}</span>}
          </div>

          <button
            type="submit"
            disabled={loading}
            style={{
              ...buttonStyle,
              background: loading ? '#93c5fd' : '#2563eb',
              cursor: loading ? 'not-allowed' : 'pointer'
            }}
          >
            {loading ? 'Submitting...' : 'Save and Continue'}
          </button>
        </form>
      </div>
    </div>
  );
}
