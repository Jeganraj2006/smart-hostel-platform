import api from './api';

export const leaveService = {
    apply: (data) => api.post('/leaves/apply', data),
    getMyLeaves: () => api.get('/leaves/my'),
    getPending: () => api.get('/leaves/pending'),
    approve: (id, level) => api.put(`/leaves/${id}/approve`, { level }),
    reject: (id, reason) => api.put(`/leaves/${id}/reject`, { reason }),
    sendReminder: (id) => api.post(`/leaves/${id}/reminder`),
    emergencyOverride: (id, data) => api.post(`/leaves/${id}/emergency-override`, data),
};