import React, { useState, useRef, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  User, Mail, Phone, Lock, Eye, EyeOff,
  Calendar, MapPin, ChevronRight, ArrowLeft,
  CheckCircle, RefreshCw,
} from 'lucide-react';

/* ─── Password strength helper ──────────────────────────────────────── */
const getStrength = (pw) => {
  let score = 0;
  if (pw.length >= 8) score++;
  if (/[A-Z]/.test(pw)) score++;
  if (/[0-9]/.test(pw)) score++;
  if (/[^A-Za-z0-9]/.test(pw)) score++;
  return score; // 0-4
};
const STRENGTH_LABEL = ['', 'Weak', 'Fair', 'Good', 'Strong'];
const STRENGTH_COLOR = ['', '#ef4444', '#f59e0b', '#3b82f6', '#22c55e'];

/* ─── OTP digit input ────────────────────────────────────────────────── */
const OtpInput = ({ value, onChange }) => {
  const inputsRef = useRef([]);

  const handleKey = (idx, e) => {
    if (e.key === 'Backspace') {
      if (value[idx]) {
        const next = value.split('');
        next[idx] = '';
        onChange(next.join(''));
      } else if (idx > 0) {
        inputsRef.current[idx - 1]?.focus();
      }
    }
  };

  const handleChange = (idx, e) => {
    const digit = e.target.value.replace(/\D/g, '').slice(-1);
    const next = value.split('');
    next[idx] = digit;
    const newVal = next.join('').padEnd(6, '');
    onChange(newVal.slice(0, 6));
    if (digit && idx < 5) inputsRef.current[idx + 1]?.focus();
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    onChange(pasted.padEnd(6, ''));
    const nextIdx = Math.min(pasted.length, 5);
    inputsRef.current[nextIdx]?.focus();
  };

  return (
    <div className="otp-inputs">
      {Array.from({ length: 6 }).map((_, idx) => (
        <input
          key={idx}
          ref={(el) => (inputsRef.current[idx] = el)}
          id={`otp-digit-${idx}`}
          type="text"
          inputMode="numeric"
          maxLength={1}
          value={value[idx] || ''}
          onChange={(e) => handleChange(idx, e)}
          onKeyDown={(e) => handleKey(idx, e)}
          onPaste={handlePaste}
          className={`otp-box ${value[idx] ? 'otp-box--filled' : ''}`}
          autoFocus={idx === 0}
          aria-label={`OTP digit ${idx + 1}`}
        />
      ))}
    </div>
  );
};

/* ─── Register page ─────────────────────────────────────────────────── */
const Register = () => {
  const navigate = useNavigate();
  const { register, verifyOtp, resendOtp } = useAuth();

  /* Step 1 form state */
  const [form, setForm] = useState({
    name: '',
    email: '',
    mobile: '',
    password: '',
    gender: '',
    dateOfBirth: '',
    address: '',
  });
  const [showPassword, setShowPassword] = useState(false);

  /* Step 2 OTP state */
  const [step, setStep] = useState(1); // 1 | 2 | 3 (success)
  const [otp, setOtp] = useState('');
  const [countdown, setCountdown] = useState(60);
  const [canResend, setCanResend] = useState(false);

  /* Submitting */
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});

  /* Countdown timer for resend */
  useEffect(() => {
    if (step !== 2) return;
    setCountdown(60);
    setCanResend(false);
    const interval = setInterval(() => {
      setCountdown((c) => {
        if (c <= 1) {
          clearInterval(interval);
          setCanResend(true);
          return 0;
        }
        return c - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [step]);

  const passwordStrength = getStrength(form.password);

  /* ── Validation ─────────────────────────────────────────────────── */
  const validate = () => {
    const errors = {};
    if (!form.name.trim()) errors.name = 'Name is required';
    if (!form.email.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) errors.email = 'Invalid email';
    if (!form.mobile.match(/^[0-9]{10}$/)) errors.mobile = 'Mobile must be 10 digits';
    if (!form.password.match(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/))
      errors.password = 'Min 8 chars, uppercase, lowercase, number & special char';
    if (!form.gender) errors.gender = 'Gender is required';
    if (!form.dateOfBirth) errors.dateOfBirth = 'Date of birth is required';
    if (!form.address.trim()) errors.address = 'Address is required';
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  /* ── Step 1: Submit registration form ──────────────────────────── */
  const handleRegister = async (e) => {
    e.preventDefault();
    if (!validate()) return;
    setIsSubmitting(true);
    const result = await register({ ...form });
    if (result.success) {
      setStep(2);
    }
    setIsSubmitting(false);
  };

  /* ── Step 2: Verify OTP ─────────────────────────────────────────── */
  const handleVerify = async (e) => {
    e.preventDefault();
    if (otp.length < 6) return;
    setIsSubmitting(true);
    const result = await verifyOtp(form.email, otp);
    if (result.success) {
      setStep(3);
    }
    setIsSubmitting(false);
  };

  /* ── Resend OTP ─────────────────────────────────────────────────── */
  const handleResend = async () => {
    if (!canResend) return;
    const result = await resendOtp(form.email);
    if (result.success) {
      setOtp('');
      setStep(2); // triggers timer reset
    }
  };

  /* ── Step 3: Success screen ─────────────────────────────────────── */
  if (step === 3) {
    return (
      <div className="login-root">
        <div className="blob blob-1" />
        <div className="blob blob-2" />
        <div className="register-card register-card--success">
          <div className="success-icon">
            <CheckCircle size={56} color="#22c55e" />
          </div>
          <h2 className="success-title">Account Created!</h2>
          <p className="success-sub">
            Welcome, <strong>{form.name}</strong>! Your account has been verified and is now active.
          </p>
          <button
            id="go-to-login"
            className="login-btn login-btn--0"
            onClick={() => navigate('/login')}
          >
            Go to Login <ChevronRight size={18} />
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="login-root">
      <div className="blob blob-1" />
      <div className="blob blob-2" />
      <div className="blob blob-3" />

      <div className="register-card">
        {/* ── Brand ──────────────────────────────────────────────── */}
        <div className="login-brand">
          <div className="login-logo"><span>T</span></div>
          <h1 className="login-title">Create Account</h1>
          <p className="login-subtitle">TPA ClaimSys — Member Registration</p>
        </div>

        {/* ── Step indicator ──────────────────────────────────────── */}
        <div className="step-indicator">
          {['Details', 'Verify OTP'].map((label, idx) => (
            <React.Fragment key={label}>
              <div className={`step-pill ${step > idx ? 'step-pill--done' : step === idx + 1 ? 'step-pill--active' : ''}`}>
                <span className="step-num">{step > idx + 1 ? '✓' : idx + 1}</span>
                <span className="step-label">{label}</span>
              </div>
              {idx < 1 && <div className={`step-line ${step > 1 ? 'step-line--done' : ''}`} />}
            </React.Fragment>
          ))}
        </div>

        {/* ════════════════════════════════════════════════════════
            STEP 1 — Registration Form
        ════════════════════════════════════════════════════════ */}
        {step === 1 && (
          <form onSubmit={handleRegister} className="register-form" noValidate>
            {/* Name */}
            <div className="field-group">
              <label htmlFor="reg-name" className="field-label">Full Name</label>
              <div className="field-wrap">
                <User className="field-icon" size={18} />
                <input
                  id="reg-name"
                  type="text"
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  placeholder="Jane Smith"
                  className={`field-input ${fieldErrors.name ? 'field-input--error' : ''}`}
                />
              </div>
              {fieldErrors.name && <p className="field-error">{fieldErrors.name}</p>}
            </div>

            {/* Email */}
            <div className="field-group">
              <label htmlFor="reg-email" className="field-label">Email Address</label>
              <div className="field-wrap">
                <Mail className="field-icon" size={18} />
                <input
                  id="reg-email"
                  type="email"
                  value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                  placeholder="you@example.com"
                  className={`field-input ${fieldErrors.email ? 'field-input--error' : ''}`}
                />
              </div>
              {fieldErrors.email && <p className="field-error">{fieldErrors.email}</p>}
            </div>

            {/* Mobile */}
            <div className="field-group">
              <label htmlFor="reg-mobile" className="field-label">Phone Number</label>
              <div className="field-wrap">
                <Phone className="field-icon" size={18} />
                <input
                  id="reg-mobile"
                  type="tel"
                  value={form.mobile}
                  onChange={(e) => setForm({ ...form, mobile: e.target.value.replace(/\D/g, '').slice(0, 10) })}
                  placeholder="(555) 867-5309"
                  className={`field-input ${fieldErrors.mobile ? 'field-input--error' : ''}`}
                />
              </div>
              {fieldErrors.mobile && <p className="field-error">{fieldErrors.mobile}</p>}
            </div>

            {/* Password */}
            <div className="field-group">
              <label htmlFor="reg-password" className="field-label">Password</label>
              <div className="field-wrap">
                <Lock className="field-icon" size={18} />
                <input
                  id="reg-password"
                  type={showPassword ? 'text' : 'password'}
                  value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                  placeholder="Min. 8 chars, uppercase, number, symbol"
                  className={`field-input field-input--padded-right ${fieldErrors.password ? 'field-input--error' : ''}`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="field-eye"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {/* Password strength bar */}
              {form.password && (
                <div className="pw-strength">
                  <div className="pw-bar">
                    {[1, 2, 3, 4].map((s) => (
                      <div
                        key={s}
                        className="pw-segment"
                        style={{ background: passwordStrength >= s ? STRENGTH_COLOR[passwordStrength] : '#334155' }}
                      />
                    ))}
                  </div>
                  <span className="pw-label" style={{ color: STRENGTH_COLOR[passwordStrength] }}>
                    {STRENGTH_LABEL[passwordStrength]}
                  </span>
                </div>
              )}
              {fieldErrors.password && <p className="field-error">{fieldErrors.password}</p>}
            </div>

            {/* Gender + DOB — two columns */}
            <div className="field-row">
              <div className="field-group">
                <label htmlFor="reg-gender" className="field-label">Gender</label>
                <select
                  id="reg-gender"
                  value={form.gender}
                  onChange={(e) => setForm({ ...form, gender: e.target.value })}
                  className={`field-input field-select ${fieldErrors.gender ? 'field-input--error' : ''}`}
                >
                  <option value="">Select</option>
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                </select>
                {fieldErrors.gender && <p className="field-error">{fieldErrors.gender}</p>}
              </div>

              <div className="field-group">
                <label htmlFor="reg-dob" className="field-label">Date of Birth</label>
                <div className="field-wrap">
                  <Calendar className="field-icon" size={18} />
                  <input
                    id="reg-dob"
                    type="date"
                    value={form.dateOfBirth}
                    onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })}
                    max={new Date().toISOString().split('T')[0]}
                    className={`field-input ${fieldErrors.dateOfBirth ? 'field-input--error' : ''}`}
                  />
                </div>
                {fieldErrors.dateOfBirth && <p className="field-error">{fieldErrors.dateOfBirth}</p>}
              </div>
            </div>

            {/* Address */}
            <div className="field-group">
              <label htmlFor="reg-address" className="field-label">Address</label>
              <div className="field-wrap">
                <MapPin className="field-icon" size={18} />
                <input
                  id="reg-address"
                  type="text"
                  value={form.address}
                  onChange={(e) => setForm({ ...form, address: e.target.value })}
                  placeholder="123 Main St, Springfield, IL 62701"
                  className={`field-input ${fieldErrors.address ? 'field-input--error' : ''}`}
                />
              </div>
              {fieldErrors.address && <p className="field-error">{fieldErrors.address}</p>}
            </div>

            <button
              id="register-submit"
              type="submit"
              disabled={isSubmitting}
              className="login-btn login-btn--0"
            >
              {isSubmitting ? (
                <span className="login-spinner" />
              ) : (
                <>Send OTP <ChevronRight size={18} /></>
              )}
            </button>

            <p className="login-register-link">
              Already have an account?{' '}
              <Link to="/login" className="login-link login-link--bold">Sign in</Link>
            </p>
          </form>
        )}

        {/* ════════════════════════════════════════════════════════
            STEP 2 — OTP Verification
        ════════════════════════════════════════════════════════ */}
        {step === 2 && (
          <div className="otp-panel">
            <div className="otp-icon">📬</div>
            <h2 className="otp-title">Check your email</h2>
            <p className="otp-sub">
              We sent a 6-digit OTP to <strong>{form.email}</strong>.<br />
              It expires in <strong>5 minutes</strong>.
            </p>

            <form onSubmit={handleVerify} noValidate>
              <OtpInput value={otp} onChange={setOtp} />

              <button
                id="verify-otp-submit"
                type="submit"
                disabled={isSubmitting || otp.length < 6}
                className="login-btn login-btn--0"
                style={{ marginTop: '1.5rem' }}
              >
                {isSubmitting ? (
                  <span className="login-spinner" />
                ) : (
                  <>Verify OTP <CheckCircle size={18} /></>
                )}
              </button>
            </form>

            {/* Resend */}
            <div className="otp-resend">
              {canResend ? (
                <button
                  id="resend-otp"
                  type="button"
                  onClick={handleResend}
                  className="otp-resend-btn"
                >
                  <RefreshCw size={14} /> Resend OTP
                </button>
              ) : (
                <p className="otp-countdown">
                  Resend in <strong>{countdown}s</strong>
                </p>
              )}
            </div>

            <button
              type="button"
              onClick={() => setStep(1)}
              className="otp-back"
            >
              <ArrowLeft size={14} /> Back to form
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default Register;
