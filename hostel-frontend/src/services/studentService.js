import api from './api';

export const studentService = {
    getPreferences: () => api.get('/students/preferences'),
    submitPreferences: (data) => api.put('/students/preferences', data),
};
