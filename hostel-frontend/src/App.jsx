import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import ProtectedRoute from './routes/ProtectedRoute';

import HodDashboard from './pages/hod/HodDashboard';
import ParentDashboard from './pages/parent/ParentDashboard';
import TrustTimeline from './pages/parent/TrustTimeline';
import StaffDashboard from './pages/staff/StaffDashboard';

import Login from './pages/auth/Login';
import Register from './pages/auth/Register';
import Unauthorized from './pages/Unauthorized';

import StudentDashboard from './pages/student/StudentDashboard';
import StudentOnboarding from './pages/student/StudentOnboarding';
import LeaveApply from './pages/student/LeaveApply';
import MyLeaves from './pages/student/MyLeaves';
import MyComplaints from './pages/student/MyComplaints';
import MyFees from './pages/student/MyFees';

import WardenDashboard from './pages/warden/WardenDashboard';
import PendingLeaves from './pages/warden/PendingLeaves';
import PendingRegistrations from './pages/warden/PendingRegistrations';
import PreventiveMaintenance from './pages/warden/PreventiveMaintenance';
import FeeRiskDashboard from './pages/warden/FeeRiskDashboard';
import VisitorLog from './pages/warden/VisitorLog';
import EmergencyBroadcast from './pages/warden/EmergencyBroadcast';

import AdminDashboard from './pages/admin/AdminDashboard';
import RoomAllocation from './pages/admin/RoomAllocation';
import Analytics from './pages/admin/Analytics';

import SecurityDashboard from './pages/security/SecurityDashboard';
import GateScanner from './pages/security/GateScanner';

const queryClient = new QueryClient();

const ADMIN_ROLES = ['SUPER_ADMIN', 'ADMIN'];
const WARDEN_ADMIN = ['WARDEN', 'SUPER_ADMIN', 'ADMIN'];

export default function App() {
    return (
        <QueryClientProvider client={queryClient}>
            <BrowserRouter>
                <Toaster position="top-right" />
                <Routes>
                    <Route path="/login" element={<Login />} />
                    <Route path="/register" element={<Register />} />
                    <Route path="/unauthorized" element={<Unauthorized />} />

                    {/* ── Student ── */}
                    <Route path="/student" element={
                        <ProtectedRoute allowedRoles={['STUDENT']}>
                            <StudentDashboard />
                        </ProtectedRoute>
                    } />
                    <Route path="/student/onboarding" element={
                        <ProtectedRoute allowedRoles={['STUDENT']}>
                            <StudentOnboarding />
                        </ProtectedRoute>
                    } />
                    <Route path="/student/leave/apply" element={
                        <ProtectedRoute allowedRoles={['STUDENT']}>
                            <LeaveApply />
                        </ProtectedRoute>
                    } />
                    <Route path="/student/leave/history" element={
                        <ProtectedRoute allowedRoles={['STUDENT']}>
                            <MyLeaves />
                        </ProtectedRoute>
                    } />
                    <Route path="/student/complaints" element={
                        <ProtectedRoute allowedRoles={['STUDENT']}>
                            <MyComplaints />
                        </ProtectedRoute>
                    } />
                    <Route path="/student/fees" element={
                        <ProtectedRoute allowedRoles={['STUDENT']}>
                            <MyFees />
                        </ProtectedRoute>
                    } />

                    {/* ── Warden ── */}
                    <Route path="/warden" element={
                        <ProtectedRoute allowedRoles={['WARDEN']}>
                            <WardenDashboard />
                        </ProtectedRoute>
                    } />
                    <Route path="/warden/leaves" element={
                        <ProtectedRoute allowedRoles={['WARDEN', 'HOD', ...ADMIN_ROLES]}>
                            <PendingLeaves />
                        </ProtectedRoute>
                    } />
                    {/* Registrations — warden approves new sign-ups */}
                    <Route path="/warden/registrations" element={
                        <ProtectedRoute allowedRoles={WARDEN_ADMIN}>
                            <PendingRegistrations />
                        </ProtectedRoute>
                    } />
                    {/* Room map — placeholder, re-uses RoomAllocation page */}
                    <Route path="/warden/rooms" element={
                        <ProtectedRoute allowedRoles={WARDEN_ADMIN}>
                            <RoomAllocation />
                        </ProtectedRoute>
                    } />
                    <Route path="/warden/preventive-maintenance" element={
                        <ProtectedRoute allowedRoles={[...WARDEN_ADMIN, 'HOD']}>
                            <PreventiveMaintenance />
                        </ProtectedRoute>
                    } />
                    <Route path="/warden/fee-risk" element={
                        <ProtectedRoute allowedRoles={[...WARDEN_ADMIN, 'HOD']}>
                            <FeeRiskDashboard />
                        </ProtectedRoute>
                    } />
                    <Route path="/warden/visitors" element={
                        <ProtectedRoute allowedRoles={[...WARDEN_ADMIN, 'SECURITY_GUARD']}>
                            <VisitorLog />
                        </ProtectedRoute>
                    } />
                    <Route path="/warden/broadcast" element={
                        <ProtectedRoute allowedRoles={WARDEN_ADMIN}>
                            <EmergencyBroadcast />
                        </ProtectedRoute>
                    } />

                    {/* ── Admin ── */}
                    <Route path="/admin" element={
                        <ProtectedRoute allowedRoles={ADMIN_ROLES}>
                            <AdminDashboard />
                        </ProtectedRoute>
                    } />
                    <Route path="/admin/allocations" element={
                        <ProtectedRoute allowedRoles={WARDEN_ADMIN}>
                            <RoomAllocation />
                        </ProtectedRoute>
                    } />
                    <Route path="/admin/analytics" element={
                        <ProtectedRoute allowedRoles={[...ADMIN_ROLES, 'HOD']}>
                            <Analytics />
                        </ProtectedRoute>
                    } />

                    {/* ── HOD ── */}
                    <Route path="/hod" element={
                        <ProtectedRoute allowedRoles={['HOD']}>
                            <HodDashboard />
                        </ProtectedRoute>
                    } />
                    {/* HOD also reviews leaves */}
                    <Route path="/hod/leaves" element={
                        <ProtectedRoute allowedRoles={['HOD', ...ADMIN_ROLES]}>
                            <PendingLeaves />
                        </ProtectedRoute>
                    } />

                    {/* ── Parent ── */}
                    <Route path="/parent" element={
                        <ProtectedRoute allowedRoles={['PARENT']}>
                            <ParentDashboard />
                        </ProtectedRoute>
                    } />
                    <Route path="/parent/timeline" element={
                        <ProtectedRoute allowedRoles={['PARENT']}>
                            <TrustTimeline />
                        </ProtectedRoute>
                    } />
                    {/* Parent leave approvals — shows ward's leave history */}
                    <Route path="/parent/leaves" element={
                        <ProtectedRoute allowedRoles={['PARENT']}>
                            <MyLeaves />
                        </ProtectedRoute>
                    } />

                    {/* ── Staff ── */}
                    <Route path="/staff" element={
                        <ProtectedRoute allowedRoles={['STAFF']}>
                            <StaffDashboard />
                        </ProtectedRoute>
                    } />
                    {/* Staff sub-pages — map to StaffDashboard until dedicated pages are built */}
                    <Route path="/staff/attendance" element={
                        <ProtectedRoute allowedRoles={['STAFF']}>
                            <StaffDashboard />
                        </ProtectedRoute>
                    } />
                    <Route path="/staff/complaints" element={
                        <ProtectedRoute allowedRoles={['STAFF']}>
                            <StaffDashboard />
                        </ProtectedRoute>
                    } />

                    {/* ── Security ── */}
                    <Route path="/security" element={
                        <ProtectedRoute allowedRoles={['SECURITY_GUARD']}>
                            <SecurityDashboard />
                        </ProtectedRoute>
                    } />
                    <Route path="/security/scanner" element={
                        <ProtectedRoute allowedRoles={['SECURITY_GUARD']}>
                            <GateScanner />
                        </ProtectedRoute>
                    } />

                    <Route path="/" element={<Navigate to="/login" replace />} />
                </Routes>
            </BrowserRouter>
        </QueryClientProvider>
    );
}