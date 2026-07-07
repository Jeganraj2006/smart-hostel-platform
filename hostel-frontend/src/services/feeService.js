import api from './api';

export const feeService = {
    getMyFees:     ()  => api.get('/fees/my'),
    getAll:        ()  => api.get('/fees'),
    payFee:        (id) => api.post(`/fees/${id}/pay`),
    getReceipt:    (id) => api.get(`/fees/${id}/receipt`, { responseType: 'blob' }),
    getRiskReport: ()  => api.get('/fees/risk'),
};