import api from './api';

export const emergencyService = {
    broadcast: (data) => api.post('/emergency/broadcast', data)
};
