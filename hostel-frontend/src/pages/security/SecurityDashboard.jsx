import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import DashboardLayout from '../../components/layout/DashboardLayout';
import { gateService } from '../../services/gateService';

export default function SecurityDashboard() {
    const { data: statusList = [], isLoading, refetch } = useQuery({
        queryKey: ['gateStatus'],
        queryFn: () => gateService.getGateStatus().then((r) => r.data),
        refetchInterval: 30000, // auto-refresh every 30 seconds
    });

    const outList = statusList.filter((item) => item.attendance?.status === 'OUT');
    const overdueList = statusList.filter((item) => item.attendance?.status === 'OVERDUE');

    return (
        <DashboardLayout>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
                <div>
                    <h1 style={{ fontSize: '24px', fontWeight: '700', color: '#0f172a' }}>🏠 Gate Security Dashboard</h1>
                    <p style={{ fontSize: '14px', color: '#64748b', marginTop: '4px' }}>
                        Real-time tracking of student campus exit/entry status.
                    </p>
                </div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <button
                        onClick={() => refetch()}
                        style={{
                            background: '#f1f5f9',
                            color: '#475569',
                            border: 'none',
                            borderRadius: '8px',
                            padding: '9px 16px',
                            fontSize: '14px',
                            fontWeight: '500',
                            cursor: 'pointer',
                        }}
                    >
                        🔄 Refresh
                    </button>
                    <Link
                        to="/security/scanner"
                        style={{
                            background: '#0D9488',
                            color: 'white',
                            textDecoration: 'none',
                            borderRadius: '8px',
                            padding: '9px 18px',
                            fontSize: '14px',
                            fontWeight: '500',
                        }}
                    >
                        📷 Open Gate Scanner
                    </Link>
                </div>
            </div>

            {/* Quick Stats Cards */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '24px' }}>
                <div style={{ background: 'white', border: '1px solid #e2e8f0', borderRadius: '12px', padding: '24px', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                    <div style={{ fontSize: '13px', fontWeight: '600', color: '#b45309', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Currently Out</div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginTop: '8px' }}>
                        <span style={{ fontSize: '36px', fontWeight: '700', color: '#0f172a' }}>{outList.length}</span>
                        <span style={{ fontSize: '13px', color: '#64748b' }}>active out-passes</span>
                    </div>
                </div>

                <div style={{ background: 'white', border: '1px solid #e2e8f0', borderRadius: '12px', padding: '24px', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                    <div style={{ fontSize: '13px', fontWeight: '600', color: '#b91c1c', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Overdue Returns</div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginTop: '8px' }}>
                        <span style={{ fontSize: '36px', fontWeight: '700', color: '#b91c1c' }}>{overdueList.length}</span>
                        <span style={{ fontSize: '13px', color: '#64748b' }}>missed deadlines</span>
                    </div>
                </div>
            </div>

            {/* List Table */}
            <div style={{ background: 'white', border: '1px solid #e2e8f0', borderRadius: '12px', overflow: 'hidden', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                <div style={{ padding: '20px', borderBottom: '1px solid #f1f5f9' }}>
                    <h2 style={{ fontSize: '16px', fontWeight: '600', color: '#0f172a' }}>Gate Movement Log (Today)</h2>
                </div>

                {isLoading ? (
                    <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>Loading status...</div>
                ) : statusList.length === 0 ? (
                    <div style={{ padding: '60px', textAlign: 'center' }}>
                        <div style={{ fontSize: '40px', marginBottom: '12px' }}>🌿</div>
                        <p style={{ color: '#64748b', fontSize: '14px' }}>All students are inside the hostel campus. No active exit logs found.</p>
                    </div>
                ) : (
                    <div style={{ overflowX: 'auto' }}>
                        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
                            <thead>
                                <tr style={{ background: '#f8fafc', borderBottom: '1px solid #f1f5f9', color: '#475569', fontWeight: '500' }}>
                                    <th style={{ padding: '14px 20px' }}>Student</th>
                                    <th style={{ padding: '14px 20px' }}>Room</th>
                                    <th style={{ padding: '14px 20px' }}>Status</th>
                                    <th style={{ padding: '14px 20px' }}>Exit Time</th>
                                    <th style={{ padding: '14px 20px' }}>Expected Return</th>
                                </tr>
                            </thead>
                            <tbody>
                                {statusList.map((item) => {
                                    const { attendance, studentName, studentEmail, studentPhone, roomNo, blockName } = item;
                                    return (
                                        <tr key={attendance.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                                            <td style={{ padding: '14px 20px' }}>
                                                <div style={{ fontWeight: '500', color: '#0f172a' }}>{studentName}</div>
                                                <div style={{ fontSize: '12px', color: '#64748b', marginTop: '2px' }}>
                                                    {studentEmail} • {studentPhone}
                                                </div>
                                            </td>
                                            <td style={{ padding: '14px 20px', color: '#334155' }}>
                                                Room {roomNo} ({blockName})
                                            </td>
                                            <td style={{ padding: '14px 20px' }}>
                                                <span style={{
                                                    fontSize: '11px',
                                                    fontWeight: '600',
                                                    padding: '2px 10px',
                                                    borderRadius: '20px',
                                                    background: attendance.status === 'OVERDUE' ? '#fef2f2' : '#fffbeb',
                                                    color: attendance.status === 'OVERDUE' ? '#ef4444' : '#d97706',
                                                }}>
                                                    {attendance.status}
                                                </span>
                                            </td>
                                            <td style={{ padding: '14px 20px', color: '#475569' }}>
                                                {attendance.exitScannedAt ? new Date(attendance.exitScannedAt).toLocaleString() : 'N/A'}
                                            </td>
                                            <td style={{ padding: '14px 20px', color: '#475569' }}>
                                                {attendance.expectedReturnAt ? new Date(attendance.expectedReturnAt).toLocaleString() : 'N/A'}
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </DashboardLayout>
    );
}
