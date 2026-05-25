import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { complaintService } from '../../services/complaintService';

const categories = ['Plumbing', 'Electrical', 'WiFi', 'Cleanliness', 'Furniture', 'Water', 'Other'];
const priorities = ['LOW', 'MEDIUM', 'CRITICAL'];

const statusStyle = (status) => {
  const map = {
    RESOLVED: { bg: '#f0fdf4', color: '#16a34a', label: '✅ Resolved' },
    IN_PROGRESS: { bg: '#eff6ff', color: '#2563eb', label: '🔄 In Progress' },
    OPEN: { bg: '#fffbeb', color: '#d97706', label: '🟡 Open' },
  };
  return map[status] || { bg: '#f1f5f9', color: '#475569', label: status };
};

export default function MyComplaints() {
  const queryClient = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const { register, handleSubmit, reset, formState: { errors } } = useForm();

  const { data: complaints = [], isLoading } = useQuery({
    queryKey: ['myComplaints'],
    queryFn: () => complaintService.getMyComplaints().then(r => r.data),
    retry: false,
  });

  const raiseMutation = useMutation({
    mutationFn: (data) => complaintService.raise(data),
    onSuccess: () => {
      toast.success('Complaint raised successfully!');
      queryClient.invalidateQueries(['myComplaints']);
      setShowForm(false);
      reset();
    },
    onError: () => toast.error('Failed to raise complaint'),
  });

  const rateMutation = useMutation({
    mutationFn: ({ id, rating }) => complaintService.rate(id, rating),
    onSuccess: () => {
      toast.success('Rating submitted!');
      queryClient.invalidateQueries(['myComplaints']);
    },
  });

  const inputStyle = {
    width: '100%', border: '1px solid #e2e8f0', borderRadius: '8px',
    padding: '10px 14px', fontSize: '14px', outline: 'none', boxSizing: 'border-box',
  };

  return (
      <DashboardLayout>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
          <div>
            <h1 style={{ fontSize: '22px', fontWeight: '700', color: '#0f172a' }}>My Complaints</h1>
            <p style={{ fontSize: '14px', color: '#64748b', marginTop: '4px' }}>{complaints.length} total complaints</p>
          </div>
          <button
              onClick={() => setShowForm(!showForm)}
              style={{
                background: '#1E40AF', color: 'white', border: 'none',
                borderRadius: '8px', padding: '9px 18px', fontSize: '14px',
                fontWeight: '500', cursor: 'pointer',
              }}
          >
            {showForm ? '✕ Cancel' : '+ Raise Complaint'}
          </button>
        </div>

        {/* Raise Complaint Form */}
        {showForm && (
            <div style={{ background: 'white', borderRadius: '12px', border: '1px solid #bfdbfe', padding: '24px', marginBottom: '20px' }}>
              <h2 style={{ fontSize: '16px', fontWeight: '600', color: '#0f172a', marginBottom: '16px' }}>New Complaint</h2>
              <form onSubmit={handleSubmit(data => raiseMutation.mutate(data))}>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '13px', fontWeight: '500', color: '#374151', marginBottom: '6px' }}>Category</label>
                    <select {...register('category', { required: true })} style={inputStyle}>
                      <option value="">Select category...</option>
                      {categories.map(c => <option key={c} value={c}>{c}</option>)}
                    </select>
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '13px', fontWeight: '500', color: '#374151', marginBottom: '6px' }}>Priority</label>
                    <select {...register('priority', { required: true })} style={inputStyle}>
                      {priorities.map(p => <option key={p} value={p}>{p}</option>)}
                    </select>
                  </div>
                </div>
                <div style={{ marginBottom: '16px' }}>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: '500', color: '#374151', marginBottom: '6px' }}>Description</label>
                  <textarea
                      {...register('description', { required: 'Describe the issue' })}
                      rows={3}
                      placeholder="Describe the issue in detail..."
                      style={{ ...inputStyle, resize: 'none' }}
                  />
                  {errors.description && <p style={{ color: '#ef4444', fontSize: '12px', marginTop: '4px' }}>{errors.description.message}</p>}
                </div>
                <div style={{ marginBottom: '20px' }}>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: '500', color: '#374151', marginBottom: '6px' }}>Asset ID (if applicable)</label>
                  <input
                      {...register('assetId')}
                      placeholder="e.g. FAN-B2-221"
                      style={inputStyle}
                  />
                </div>
                <button
                    type="submit"
                    disabled={raiseMutation.isPending}
                    style={{
                      background: '#1E40AF', color: 'white', border: 'none',
                      borderRadius: '8px', padding: '10px 20px', fontSize: '14px',
                      fontWeight: '500', cursor: 'pointer',
                    }}
                >
                  {raiseMutation.isPending ? 'Submitting...' : '📤 Submit Complaint'}
                </button>
              </form>
            </div>
        )}

        {/* Complaints list */}
        {isLoading ? (
            <div style={{ textAlign: 'center', padding: '60px', color: '#94a3b8' }}>Loading...</div>
        ) : complaints.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '60px', background: 'white', borderRadius: '12px', border: '1px solid #f1f5f9' }}>
              <div style={{ fontSize: '40px', marginBottom: '12px' }}>🎉</div>
              <p style={{ color: '#64748b', fontSize: '14px' }}>No complaints raised — everything is good!</p>
            </div>
        ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {complaints.map((c) => {
                const ss = statusStyle(c.status);
                return (
                    <div key={c.id} style={{ background: 'white', borderRadius: '12px', border: '1px solid #f1f5f9', padding: '20px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <div>
                          <div style={{ display: 'flex', gap: '8px', alignItems: 'center', marginBottom: '6px' }}>
                            <span style={{ fontSize: '15px', fontWeight: '600', color: '#0f172a' }}>{c.category}</span>
                            <span style={{
                              fontSize: '11px', fontWeight: '600', padding: '2px 8px',
                              borderRadius: '20px',
                              background: c.priority === 'CRITICAL' ? '#fef2f2' : c.priority === 'MEDIUM' ? '#fffbeb' : '#f0fdf4',
                              color: c.priority === 'CRITICAL' ? '#dc2626' : c.priority === 'MEDIUM' ? '#d97706' : '#16a34a',
                            }}>
                        {c.priority}
                      </span>
                          </div>
                          <p style={{ fontSize: '13px', color: '#475569' }}>{c.description}</p>
                          {c.assetId && <p style={{ fontSize: '12px', color: '#94a3b8', marginTop: '4px' }}>Asset: {c.assetId}</p>}
                          {c.assignedTo && <p style={{ fontSize: '12px', color: '#64748b', marginTop: '4px' }}>👷 Assigned to: {c.assignedTo}</p>}
                        </div>
                        <span style={{
                          fontSize: '12px', fontWeight: '500', padding: '4px 12px',
                          borderRadius: '20px', background: ss.bg, color: ss.color, whiteSpace: 'nowrap',
                        }}>
                    {ss.label}
                  </span>
                      </div>

                      {/* Rating for resolved complaints */}
                      {c.status === 'RESOLVED' && !c.rating && (
                          <div style={{ marginTop: '12px', paddingTop: '12px', borderTop: '1px solid #f8fafc' }}>
                            <p style={{ fontSize: '13px', color: '#64748b', marginBottom: '8px' }}>Rate the resolution:</p>
                            <div style={{ display: 'flex', gap: '6px' }}>
                              {[1, 2, 3, 4, 5].map(r => (
                                  <button
                                      key={r}
                                      onClick={() => rateMutation.mutate({ id: c.id, rating: r })}
                                      style={{
                                        background: 'none', border: '1px solid #e2e8f0', borderRadius: '6px',
                                        padding: '4px 10px', cursor: 'pointer', fontSize: '14px',
                                      }}
                                  >
                                    {'⭐'.repeat(r)}
                                  </button>
                              ))}
                            </div>
                          </div>
                      )}
                      {c.rating && (
                          <p style={{ fontSize: '13px', color: '#64748b', marginTop: '8px' }}>
                            Your rating: {'⭐'.repeat(c.rating)}
                          </p>
                      )}
                    </div>
                );
              })}
            </div>
        )}
      </DashboardLayout>
  );
}