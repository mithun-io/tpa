import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';

import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import Dashboard from './pages/Dashboard';
import AdminDashboard from './pages/AdminDashboard';
import AnalyticsDashboard from './pages/AnalyticsDashboard';
import ClaimList from './pages/ClaimList';
import ClaimDetail from './pages/ClaimDetail';
import UploadClaim from './pages/UploadClaim';
import Profile from './pages/Profile';
import ChangePassword from './pages/ChangePassword';
import CarrierRegister from './pages/CarrierRegister';
import CarrierDashboard from './pages/CarrierDashboard';
import FraudDashboard from './pages/FraudDashboard';
import Layout from './components/Layout';
import ErrorBoundary from './components/ErrorBoundary';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 4000,
            style: {
              background: '#1e293b',
              color: '#e2e8f0',
              border: '1px solid #334155',
            },
          }}
        />
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/carrier-register" element={<CarrierRegister />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />

          {/* Protected routes (wrapped in Layout with Sidebar + Navbar) */}
          <Route path="/" element={
            <ProtectedRoute>
              <ErrorBoundary>
                <Layout />
              </ErrorBoundary>
            </ProtectedRoute>
          }>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="dashboard" element={<Dashboard />} />
            <Route path="admin" element={<AdminDashboard />} />
            <Route path="admin/fraud-dashboard" element={<FraudDashboard />} />
            <Route path="carrier" element={<CarrierDashboard />} />
            <Route path="carrier/fraud-dashboard" element={<FraudDashboard />} />
            <Route path="analytics" element={<AnalyticsDashboard />} />
            <Route path="claims" element={<ClaimList />} />
            <Route path="claims/:id" element={<ClaimDetail />} />
            <Route path="claims/upload" element={<UploadClaim />} />
            <Route path="profile" element={<Profile />} />
            <Route path="change-password" element={<ChangePassword />} />
          </Route>

          {/* Catch-all */}
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
