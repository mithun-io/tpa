import React, { useState, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Lock, Mail, Shield, Truck, User, Eye, EyeOff, ChevronRight, AlertCircle } from 'lucide-react';

/* ─── Tab definitions ───────────────────────────────────── */
const TABS = [
  {
    id: 'CUSTOMER',
    label: 'Customer',
    icon: User,
    accent: '#6366f1',
    gradient: 'from-indigo-600 to-purple-600',
    description: 'Access your insurance claims',
  },
  {
    id: 'FMG_ADMIN',
    label: 'Admin',
    icon: Shield,
    accent: '#ef4444',
    gradient: 'from-red-600 to-rose-600',
    description: 'Manage claims & users',
  },
  {
    id: 'CARRIER_USER',
    label: 'Carrier',
    icon: Truck,
    accent: '#f59e0b',
    gradient: 'from-amber-500 to-orange-500',
    description: 'Carrier portal access',
  },
];

/* ─── Empty form state factory ──────────────────────────── */
const emptyForm = () => ({ email: '', password: '' });

/* ─── Per-tab login form (completely isolated state) ──────── */
const TabLoginForm = ({ tab, onSuccess }) => {
  const [form, setForm]           = useState(emptyForm);
  const [showPw, setShowPw]       = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError]         = useState('');

  const { login, logout } = useAuth();

  const handleChange = useCallback(e => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
    if (error) setError('');          // clear error on typing
  }, [error]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.email.trim() || !form.password) {
      setError('Please enter your email and password.');
      return;
    }
    setSubmitting(true);
    setError('');

    const result = await login(form.email.trim(), form.password);

    if (!result.success) {
      setError(result.message || 'Login failed. Please check your credentials.');
      setSubmitting(false);
      return;
    }

    // Role mismatch guard
    if (result.role !== tab.id) {
      logout();
      setError(`This account is not a ${tab.label} account. Please use the correct login tab.`);
      setSubmitting(false);
      return;
    }

    // Successful: delegate redirect to parent
    onSuccess(result.role);
    setSubmitting(false);
  };

  const inputId = (field) => `${tab.id}-${field}`;

  return (
    <form onSubmit={handleSubmit} className="login-form" noValidate>
      {/* Error banner */}
      {error && (
        <div className="login-error-banner" role="alert">
          <AlertCircle size={16} className="shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Email */}
      <div className="field-group">
        <label htmlFor={inputId('email')} className="field-label">Email Address</label>
        <div className="field-wrap">
          <Mail className="field-icon" size={18} />
          <input
            id={inputId('email')}
            name="email"
            type="email"
            value={form.email}
            onChange={handleChange}
            placeholder="you@example.com"
            className="field-input"
            required
            autoComplete="email"
            disabled={submitting}
          />
        </div>
      </div>

      {/* Password */}
      <div className="field-group">
        <label htmlFor={inputId('password')} className="field-label">Password</label>
        <div className="field-wrap">
          <Lock className="field-icon" size={18} />
          <input
            id={inputId('password')}
            name="password"
            type={showPw ? 'text' : 'password'}
            value={form.password}
            onChange={handleChange}
            placeholder="••••••••"
            className="field-input field-input--padded-right"
            required
            autoComplete="current-password"
            disabled={submitting}
          />
          <button
            type="button"
            onClick={() => setShowPw(v => !v)}
            className="field-eye"
            aria-label={showPw ? 'Hide password' : 'Show password'}
          >
            {showPw ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        </div>
      </div>

      {/* Forgot */}
      <div className="login-forgot">
        <Link to="/forgot-password" className="login-link">Forgot password?</Link>
      </div>

      {/* Submit */}
      <button
        id={`login-submit-${tab.id}`}
        type="submit"
        disabled={submitting}
        className={`login-btn login-btn--${TABS.indexOf(tab)}`}
      >
        {submitting
          ? <span className="login-spinner" />
          : <><span>Sign In as {tab.label}</span><ChevronRight size={18} /></>
        }
      </button>
    </form>
  );
};

/* ─── Root Login Page ───────────────────────────────────── */
const Login = () => {
  const [activeTab, setActiveTab] = useState(0);
  const navigate = useNavigate();
  const tab = TABS[activeTab];

  const handleSuccess = useCallback((role) => {
    if (role === 'CARRIER_USER') navigate('/carrier');
    else if (role === 'FMG_ADMIN')  navigate('/admin');
    else                            navigate('/dashboard');
  }, [navigate]);

  return (
    <div className="login-root">
      {/* Background blobs */}
      <div className="blob blob-1" />
      <div className="blob blob-2" />
      <div className="blob blob-3" />

      <div className="login-card">
        {/* Brand */}
        <div className="login-brand">
          <div className="login-logo"><span>T</span></div>
          <h1 className="login-title">TPA ClaimSys</h1>
          <p className="login-subtitle">Intelligent Insurance Claims Platform</p>
        </div>

        {/* Role Tabs */}
        <div className="login-tabs" role="tablist" aria-label="Login role">
          {TABS.map((t, idx) => {
            const Icon = t.icon;
            return (
              <button
                key={t.id}
                id={`tab-${t.id}`}
                role="tab"
                aria-selected={activeTab === idx}
                aria-controls={`panel-${t.id}`}
                onClick={() => setActiveTab(idx)}
                className={`login-tab ${activeTab === idx ? 'login-tab--active' : ''}`}
                style={activeTab === idx ? { '--tab-accent': t.accent } : {}}
              >
                <Icon size={16} />
                <span>{t.label}</span>
              </button>
            );
          })}
        </div>

        {/* Panel — key={tab.id} forces full remount on tab switch, clearing all state */}
        <div
          id={`panel-${tab.id}`}
          role="tabpanel"
          aria-labelledby={`tab-${tab.id}`}
          className="login-panel"
        >
          <p className="login-role-desc">{tab.description}</p>

          {/* key forces a fresh component (and fresh state) per tab */}
          <TabLoginForm key={tab.id} tab={tab} onSuccess={handleSuccess} />

          {/* Register links */}
          {activeTab === 0 && (
            <p className="login-register-link">
              Don&apos;t have an account?{' '}
              <Link to="/register" className="login-link login-link--bold">Create account</Link>
            </p>
          )}
          {activeTab === 2 && (
            <p className="login-register-link">
              Partner with us?{' '}
              <Link to="/carrier-register" className="login-link login-link--bold">Register as Carrier</Link>
            </p>
          )}
        </div>
      </div>
    </div>
  );
};

export default Login;
