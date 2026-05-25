import { useNavigate } from 'react-router-dom';

export default function Unauthorized() {
    const navigate = useNavigate();
    return (
        <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', textAlign: 'center' }}>
            <div>
                <div style={{ fontSize: '64px' }}>🚫</div>
                <h1 style={{ fontSize: '24px', fontWeight: '700', color: '#0f172a', marginTop: '16px' }}>Access Denied</h1>
                <p style={{ color: '#64748b', marginTop: '8px' }}>You don't have permission to view this page.</p>
                <button
                    onClick={() => navigate(-1)}
                    style={{ marginTop: '20px', background: '#1E40AF', color: 'white', border: 'none', borderRadius: '8px', padding: '10px 24px', cursor: 'pointer', fontSize: '14px' }}
                >
                    Go Back
                </button>
            </div>
        </div>
    );
}