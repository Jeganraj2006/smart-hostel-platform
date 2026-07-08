import { Link, useNavigate, useLocation } from 'react-router-dom';
import useAuthStore from '../../store/authStore';
import toast from 'react-hot-toast';

const navItems = {
    STUDENT: [
        { label: '🏠 Dashboard', path: '/student' },
        { label: '📝 Apply Leave', path: '/student/leave/apply' },
        { label: '📋 Leave History', path: '/student/leave/history' },
        { label: '🔧 Complaints', path: '/student/complaints' },
        { label: '💳 Fee Status', path: '/student/fees' },
    ],
    WARDEN: [
        { label: '🏠 Dashboard', path: '/warden' },
        { label: '📋 Pending Leaves', path: '/warden/leaves' },
        { label: '👤 Registrations', path: '/warden/registrations' },
        { label: '🏨 Room Map', path: '/warden/rooms' },
        { label: '🔧 Allocations', path: '/admin/allocations' },
        { label: '💰 Fee Risk', path: '/warden/fee-risk' },
        { label: '🛠️ Maintenance', path: '/warden/preventive-maintenance' },
        { label: '📷 Visitor Log', path: '/warden/visitors' },
        { label: '🚨 Emergency', path: '/warden/broadcast' },
    ],
    HOD: [
        { label: '🏠 Dashboard', path: '/hod' },
        { label: '📋 Pending Leaves', path: '/hod/leaves' },
        { label: '📊 Analytics', path: '/admin/analytics' },
    ],
    STAFF: [
        { label: '🏠 Dashboard', path: '/staff' },
        { label: '✅ Attendance', path: '/staff/attendance' },
        { label: '🔧 Complaints', path: '/staff/complaints' },
    ],
    PARENT: [
        { label: '🏠 Dashboard', path: '/parent' },
        { label: '📋 Leave Approvals', path: '/parent/leaves' },
        { label: '📜 Student Timeline', path: '/parent/timeline' },
    ],
    SUPER_ADMIN: [
        { label: '🏠 Dashboard', path: '/admin' },
        { label: '👥 Role Manager', path: '/admin/roles' },
        { label: '🔗 Chain Config', path: '/admin/chains' },
        { label: '📜 Audit Logs', path: '/admin/audit' },
        { label: '🔧 Allocations', path: '/admin/allocations' },
        { label: '💰 Fee Risk', path: '/warden/fee-risk' },
        { label: '🛠️ Maintenance', path: '/warden/preventive-maintenance' },
        { label: '📷 Visitor Log', path: '/warden/visitors' },
        { label: '🚨 Emergency', path: '/warden/broadcast' },
        { label: '📊 Analytics', path: '/admin/analytics' },
    ],
    SECURITY_GUARD: [
        { label: '🏠 Dashboard', path: '/security' },
        { label: '📷 Gate Scanner', path: '/security/scanner' },
        { label: '📷 Visitor Log', path: '/warden/visitors' },
    ],
};

export default function DashboardLayout({ children }) {
    const { user, clearAuth } = useAuthStore();
    const navigate = useNavigate();
    const location = useLocation();

    const handleLogout = () => {
        clearAuth();
        toast.success('Logged out successfully');
        navigate('/login');
    };

    const items = navItems[user?.role] || [];

    const roleColors = {
        STUDENT: 'bg-blue-100 text-blue-700',
        WARDEN: 'bg-purple-100 text-purple-700',
        HOD: 'bg-green-100 text-green-700',
        STAFF: 'bg-amber-100 text-amber-700',
        PARENT: 'bg-pink-100 text-pink-700',
        SUPER_ADMIN: 'bg-red-100 text-red-700',
        SECURITY_GUARD: 'bg-teal-100 text-teal-700',
    };

    return (
        <div style={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
            {/* Sidebar */}
            <aside style={{
                width: '240px',
                minWidth: '240px',
                background: 'white',
                borderRight: '1px solid #f1f5f9',
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden'
            }}>
                {/* Logo */}
                <div style={{ padding: '24px 20px', borderBottom: '1px solid #f1f5f9' }}>
                    <div style={{ fontSize: '24px', marginBottom: '8px' }}>🏨</div>
                    <h1 style={{ fontSize: '15px', fontWeight: '700', color: '#0f172a' }}>
                        Smart Hostel
                    </h1>
                    <p style={{ fontSize: '12px', color: '#64748b', marginTop: '4px' }}>
                        {user?.name}
                    </p>
                    <span style={{
                        display: 'inline-block',
                        marginTop: '8px',
                        fontSize: '11px',
                        fontWeight: '500',
                        padding: '2px 8px',
                        borderRadius: '20px',
                    }}
                          className={roleColors[user?.role]}
                    >
            {user?.role}
          </span>
                </div>

                {/* Nav links */}
                <nav style={{ flex: 1, padding: '12px', overflowY: 'auto' }}>
                    {items.map((item) => (
                        <Link
                            key={item.path}
                            to={item.path}
                            style={{
                                display: 'block',
                                padding: '9px 12px',
                                borderRadius: '8px',
                                fontSize: '13px',
                                fontWeight: location.pathname === item.path ? '600' : '400',
                                color: location.pathname === item.path ? 'white' : '#475569',
                                background: location.pathname === item.path ? '#1E40AF' : 'transparent',
                                marginBottom: '2px',
                                textDecoration: 'none',
                                transition: 'all 0.15s',
                            }}
                        >
                            {item.label}
                        </Link>
                    ))}
                </nav>

                {/* Logout */}
                <div style={{ padding: '12px', borderTop: '1px solid #f1f5f9' }}>
                    <button
                        onClick={handleLogout}
                        style={{
                            width: '100%',
                            padding: '8px 12px',
                            borderRadius: '8px',
                            fontSize: '13px',
                            color: '#ef4444',
                            background: 'none',
                            border: 'none',
                            cursor: 'pointer',
                            textAlign: 'left',
                        }}
                    >
                        🚪 Logout
                    </button>
                </div>
            </aside>

            {/* Main content */}
            <main style={{ flex: 1, overflowY: 'auto', padding: '32px' }}>
                {children}
            </main>
        </div>
    );
}