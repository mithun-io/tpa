import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { forgotPassword, resetPassword } from '../api/auth.service';
import { Mail, KeyRound, Lock, ShieldCheck, ChevronRight, ArrowLeft, Eye, EyeOff, AlertCircle } from 'lucide-react';

/* ─── Step indicator ───────────────────────────────────── */
const Steps = ({ current }) => {
  const steps = ['Email', 'Verify OTP', 'New Password'];
  return (
    <div className="flex items-center gap-2 mb-8">
      {steps.map((label, i) => {
        const done = i < current;
        const active = i === current;
        return (
          <React.Fragment key={label}>
            <div className="flex flex-col items-center gap-1">
              <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold transition-all ${
                done ? 'bg-emerald-500 text-white' : active ? 'bg-blue-600 text-white ring-2 ring-blue-400/40' : 'bg-slate-800 text-slate-500 border border-slate-700'
              }`}>
                {done ? <ShieldCheck size={14} /> : i + 1}
              </div>
              <span className={`text-[10px] font-semibold ${active ? 'text-slate-200' : done ? 'text-emerald-400' : 'text-slate-600'}`}>{label}</span>
            </div>
            {i < steps.length - 1 && (
              <div className={`flex-1 h-px mb-5 transition-colors ${done ? 'bg-emerald-500/40' : 'bg-slate-700'}`} />
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
};

/* ─── Error banner ─────────────────────────────────────── */
const ErrorBanner = ({ msg }) => msg ? (
  <div className="flex items-start gap-2 bg-red-500/10 border border-red-500/30 text-red-300 text-sm p-3 rounded-xl mb-4 animate-[fadeIn_0.2s_ease-out]">
    <AlertCircle size={15} className="shrink-0 mt-0.5" />
    <span>{msg}</span>
  </div>
) : null;

/* ─── Main component ───────────────────────────────────── */
const ForgotPassword = () => {
  const navigate = useNavigate();
  const [step, setStep] = useState(0);          // 0=email, 1=otp, 2=newpass
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPw, setShowPw] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  /* ── Step 0: Send OTP ─────────────────────────────────── */
  const handleSendOtp = async (e) => {
    e.preventDefault();
    setError('');
    if (!email.trim()) { setError('Please enter your email address.'); return; }
    setLoading(true);
    try {
      const res = await forgotPassword(email.trim());
      if (res.success) {
        toast.success('OTP sent to your email!');
        setStep(1);
      } else {
        setError(res.message || 'Failed to send OTP. Check your email and try again.');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to send OTP. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  /* ── Step 1: Verify OTP (move to step 2) ─────────────── */
  const handleVerifyOtp = (e) => {
    e.preventDefault();
    setError('');
    if (otp.length !== 6 || !/^\d+$/.test(otp)) {
      setError('Please enter the 6-digit OTP sent to your email.');
      return;
    }
    setStep(2);
  };

  /* ── Step 2: Reset password ───────────────────────────── */
  const handleReset = async (e) => {
    e.preventDefault();
    setError('');
    if (newPassword.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    setLoading(true);
    try {
      const res = await resetPassword({ email, otp, newPassword });
      if (res.success) {
        toast.success('Password reset successfully! Please log in.');
        navigate('/login');
      } else {
        setError(res.message || 'Password reset failed. OTP may have expired.');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Password reset failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 flex items-center justify-center p-4">
      {/* Background blobs */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -left-40 w-96 h-96 bg-blue-600/10 rounded-full blur-3xl" />
        <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-indigo-600/10 rounded-full blur-3xl" />
      </div>

      <div className="w-full max-w-md relative">
        {/* Card */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl p-8">
          {/* Header */}
          <div className="text-center mb-6">
            <div className="w-12 h-12 bg-blue-600/10 border border-blue-500/20 rounded-2xl flex items-center justify-center mx-auto mb-4">
              <KeyRound size={22} className="text-blue-400" />
            </div>
            <h1 className="text-2xl font-black text-white">Forgot Password</h1>
            <p className="text-sm text-slate-500 mt-1">We'll send a reset OTP to your email</p>
          </div>

          <Steps current={step} />

          <ErrorBanner msg={error} />

          {/* ── STEP 0: Email ─────────────────────────────── */}
          {step === 0 && (
            <form onSubmit={handleSendOtp} className="space-y-4">
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-400">Email Address</label>
                <div className="relative">
                  <Mail size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
                  <input
                    id="fp-email"
                    type="email"
                    value={email}
                    onChange={e => { setEmail(e.target.value); setError(''); }}
                    placeholder="you@example.com"
                    required
                    disabled={loading}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl pl-9 pr-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-60"
                  />
                </div>
              </div>
              <button
                id="fp-send-otp"
                type="submit"
                disabled={loading}
                className="w-full flex items-center justify-center gap-2 py-3 bg-blue-600 hover:bg-blue-500 text-white font-bold rounded-xl transition-all disabled:opacity-60"
              >
                {loading ? <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> : <><Mail size={16} /> Send OTP</>}
              </button>
            </form>
          )}

          {/* ── STEP 1: OTP ───────────────────────────────── */}
          {step === 1 && (
            <form onSubmit={handleVerifyOtp} className="space-y-4">
              <div className="bg-slate-800/50 border border-slate-700 rounded-xl p-3 text-center text-sm text-slate-300 mb-2">
                OTP sent to <span className="text-blue-400 font-semibold">{email}</span>
              </div>
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-400">Enter 6-digit OTP</label>
                <input
                  id="fp-otp"
                  type="text"
                  value={otp}
                  onChange={e => { setOtp(e.target.value.replace(/\D/g, '').slice(0, 6)); setError(''); }}
                  maxLength={6}
                  required
                  placeholder="• • • • • •"
                  className="w-full text-center text-2xl font-mono tracking-[0.4em] bg-slate-950 border border-slate-700 rounded-xl px-4 py-3 text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <button
                id="fp-verify-otp"
                type="submit"
                disabled={otp.length < 6}
                className="w-full flex items-center justify-center gap-2 py-3 bg-blue-600 hover:bg-blue-500 text-white font-bold rounded-xl transition-all disabled:opacity-60"
              >
                <ShieldCheck size={16} /> Verify OTP
              </button>
              <button type="button" onClick={() => setStep(0)} className="w-full text-sm text-slate-500 hover:text-slate-300 transition-colors">
                ← Change email
              </button>
            </form>
          )}

          {/* ── STEP 2: New password ───────────────────────── */}
          {step === 2 && (
            <form onSubmit={handleReset} className="space-y-4">
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-400">New Password</label>
                <div className="relative">
                  <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
                  <input
                    id="fp-new-password"
                    type={showPw ? 'text' : 'password'}
                    value={newPassword}
                    onChange={e => { setNewPassword(e.target.value); setError(''); }}
                    placeholder="Min. 8 characters"
                    required
                    disabled={loading}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl pl-9 pr-10 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-60"
                  />
                  <button type="button" onClick={() => setShowPw(v => !v)} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300">
                    {showPw ? <EyeOff size={15} /> : <Eye size={15} />}
                  </button>
                </div>
              </div>
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-400">Confirm Password</label>
                <div className="relative">
                  <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
                  <input
                    id="fp-confirm-password"
                    type={showConfirm ? 'text' : 'password'}
                    value={confirmPassword}
                    onChange={e => { setConfirmPassword(e.target.value); setError(''); }}
                    placeholder="Repeat password"
                    required
                    disabled={loading}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl pl-9 pr-10 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-60"
                  />
                  <button type="button" onClick={() => setShowConfirm(v => !v)} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300">
                    {showConfirm ? <EyeOff size={15} /> : <Eye size={15} />}
                  </button>
                </div>
              </div>
              {/* Password match indicator */}
              {confirmPassword && (
                <p className={`text-xs font-medium ${newPassword === confirmPassword ? 'text-emerald-400' : 'text-red-400'}`}>
                  {newPassword === confirmPassword ? '✓ Passwords match' : '✗ Passwords do not match'}
                </p>
              )}
              <button
                id="fp-reset"
                type="submit"
                disabled={loading || newPassword !== confirmPassword || newPassword.length < 8}
                className="w-full flex items-center justify-center gap-2 py-3 bg-emerald-600 hover:bg-emerald-500 text-white font-bold rounded-xl transition-all disabled:opacity-60"
              >
                {loading
                  ? <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  : <><ShieldCheck size={16} /> Reset Password</>
                }
              </button>
            </form>
          )}

          {/* Footer */}
          <div className="mt-6 text-center">
            <Link to="/login" className="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-300 transition-colors">
              <ArrowLeft size={14} /> Back to Login
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ForgotPassword;
