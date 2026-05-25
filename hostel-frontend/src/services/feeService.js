import api from './api';

export const feeService = {
    getMyFees: () => api.get('/fees/my'),
    getAll: () => api.get('/fees'),
    getReceipt: (id) => api.get(`/fees/${id}/receipt`, { responseType: 'blob' }),
};