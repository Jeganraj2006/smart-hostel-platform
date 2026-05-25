import { create } from 'zustand';

const useAuthStore = create((set) => ({
    user: JSON.parse(localStorage.getItem('user')) || null,
    token: localStorage.getItem('accessToken') || null,

    setAuth: (user, token) => {
        localStorage.setItem('user', JSON.stringify(user));
        localStorage.setItem('accessToken', token);
        set({ user, token });
    },

    clearAuth: () => {
        localStorage.clear();
        set({ user: null, token: null });
    },
}));

export default useAuthStore;