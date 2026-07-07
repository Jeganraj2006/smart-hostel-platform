import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import ProtectedRoute from './routes/ProtectedRoute';

import HodDashboard from './pages/hod/HodDashboard';
import ParentDashboard from './pages/parent/ParentDashboard';
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
import PreventiveMaintenance from './pages/warden/PreventiveMaintenance';
import FeeRiskDashboard from './pages/warden/FeeRiskDashboard';
import VisitorLog from './pages/warden/VisitorLog';
import EmergencyBroadcast from './pages/warden/EmergencyBroadcast';

import AdminDashboard from './pages/admin/AdminDashboard';
import RoomAllocation from './pages/admin/RoomAllocation';

import SecurityDashboard from './pages/security/SecurityDashboard';
import GateScanner from './pages/security/GateScanner';

const queryClient = new QueryClient();

export default function App() {
    return (
        <QueryClientProvider client={queryClient}>
            <BrowserRouter>
                <Toaster position="top-right" />
                <Routes>
                    <Route path="/login" element={<Login />} />
                    <Route path="/register" element={<Register />} />
                    <Route path="/unauthorized" element={<Unauthorized />} />

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

                    <Route path="/warden" element={
                        <ProtectedRoute allowedRoles={['WARDEN']}>
                            <WardenDashboard />
                        </ProtectedRoute>
                    } />
                    <Route path="/warden/leaves" element={
                        <ProtectedRoute allowedRoles={['WARDEN']}>
                            <PendingLeaves />
                        </ProtectedRoute>
                    } />
                    <Route path="/warden/preventive-maintenance" element={
                        <ProtectedRoute allowedRoles={['WARDEN', 'SUPER_ADMIN']}>
                            <PreventiveMaintenance />
                        </ProtectedRoute>
                    } />
                    <Route path="/warden/fee-risk" element={
                        <ProtectedRoute allowedRoles={['WARDEN', 'SUPER_ADMIN']}>
                            <FeeRiskDashboard />
                        </ProtectedRoute>
                    } />
                    <Route path="/warden/visitors" element={
                        <ProtectedRoute allowedRoles={['WARDEN', 'SECURITY_GUARD', 'SUPER_ADMIN']}>
                            <VisitorLog />
                        </ProtectedRoute>
                    } />
                    <Route path="/warden/broadcast" element={
                        <ProtectedRoute allowedRoles={['WARDEN', 'SUPER_ADMIN']}>
                            <EmergencyBroadcast />
                        </ProtectedRoute>
                    } />

                    <Route path="/admin" element={
                        <ProtectedRoute allowedRoles={['SUPER_ADMIN']}>
                            <AdminDashboard />
                        </ProtectedRoute>
                    } />
                    <Route path="/admin/allocations" element={
                        <ProtectedRoute allowedRoles={['SUPER_ADMIN', 'WARDEN']}>
                            <RoomAllocation />
                        </ProtectedRoute>
                    } />

                    <Route path="/hod" element={
                        <ProtectedRoute allowedRoles={['HOD']}>
                            <HodDashboard />
                        </ProtectedRoute>
                    } />

                    <Route path="/parent" element={
                        <ProtectedRoute allowedRoles={['PARENT']}>
                            <ParentDashboard />
                        </ProtectedRoute>
                    } />

                    <Route path="/staff" element={
                        <ProtectedRoute allowedRoles={['STAFF']}>
                            <StaffDashboard />
                        </ProtectedRoute>
                    } />

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