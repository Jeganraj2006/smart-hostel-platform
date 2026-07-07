import api from './api';

export const visitorService = {
    checkIn: (data) => api.post('/visitors', data),
    checkOut: (id) => api.put(`/visitors/${id}/checkout`),
    getActive: () => api.get('/visitors/active'),
    uploadPhoto: (id, file) => {
        const formData = new FormData();
        formData.append('file', file);
        return api.post(`/visitors/${id}/photo`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });
    }
};
