import api from './api';

export const complaintService = {
    raise: (data) => api.post('/complaints', data),
    getMyComplaints: () => api.get('/complaints/my'),
    getAll: () => api.get('/complaints'),
    updateStatus: (id, status) => api.put(`/complaints/${id}/status`, { status }),
    rate: (id, rating) => api.put(`/complaints/${id}/rate`, { rating }),
    getPreventiveFlags: () => api.get('/complaints/preventive-flags'),
    resolvePreventiveFlag: (id) => api.put(`/complaints/preventive-flags/${id}/resolve`),
};