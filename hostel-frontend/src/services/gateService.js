import api from './api';

export const gateService = {
    scanGatepass: (leaveId) => api.post('/gate/scan', { leaveId }),
    getGateStatus: () => api.get('/gate/status'),
};
