import api from './api';

export const roomService = {
    getAll: () => api.get('/rooms'),
    getAvailable: () => api.get('/rooms/available'),
    requestSwap: (data) => api.post('/rooms/swap-request', data),
    allocate: (data) => api.post('/rooms/allocate', data),
};