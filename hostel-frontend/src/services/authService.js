import api from './api';

export const authService = {
    register: (data) => api.post('/auth/register', data),
    login: (data) => api.post('/auth/login', data),
    logout: () => {
        localStorage.clear();
        window.location.href = '/login';
    },
};

export const registrationService = {
    getPending: () => api.get('/registrations/pending'),
    approve: (id) => api.put(`/registrations/${id}/approve`),
    reject: (id, reason) => api.put(`/registrations/${id}/reject`, { reason }),
    getAllUsers: () => api.get('/registrations/users'),
};