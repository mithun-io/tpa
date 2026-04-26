import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { changePassword } from '../api/auth.service';
import { Lock, Eye, EyeOff, ShieldCheck, AlertCircle, ArrowLeft } from 'lucide-react';

const ErrorBanner = ({ msg }) => msg ? (
  <div className="flex items-start gap-2 bg-red-500/10 border border-red-500/30 text-red-300 text-sm p-3 rounded-xl mb-4 animate-[fadeIn_0.2s_ease-out]">
    <AlertCircle size={15} className="shrink-0 mt-0.5" />
    <span>{msg}</span>
  </div>
) : null;

const PasswordField = ({ id, label, value, onChange, placeholder, show, onToggle, disabled }) => (
  <div className="space-y-1">
    <label htmlFor={id} className="text-xs font-semibold text-slate-400">{label}</label>
    <div className="relative">
      <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
      <input
        id={id}
        type={show ? 'text' : 'password'}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        required
        disabled={disabled}
        className="w-full bg-slate-950 border border-slate-700 rounded-xl pl-9 pr-10 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-60"
      />
      <button type="button" onClick={onToggle} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300 transition-colors">
        {show ? <EyeOff size={15} /> : <Eye size={15} />}
      </button>
    </div>
  </div>
);

const ChangePassword = () => {
  const navigate = useNavigate();
  const [form, setForm] = useState({ previousPassword: '', newPassword: '', confirmPassword: '' });
  const [show, setShow] = useState({ prev: false, next: false, confirm: false });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (field) => (e) => {
    setForm(prev => ({ ...prev, [field]: e.target.value }));
    if (error) setError('');
  };

  const toggleShow = (field) => () => setShow(prev => ({ ...prev, [field]: !prev[field] }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!form.previousPassword) { setError('Please enter your current password.'); return; }
    if (form.newPassword.length < 8) { setError('New password must be at least 8 characters.'); return; }
    if (form.newPassword === form.previousPassword) { setError('New password cannot be the same as your current password.'); return; }
    if (form.newPassword !== form.confirmPassword) { setError('New passwords do not match.'); return; }

    setLoading(true);
    try {
      const res = await changePassword({
        previousPassword: form.previousPassword,
        newPassword: form.newPassword,
      });
      if (res.success) {
        toast.success('Password changed successfully!');
        navigate(-1);   // go back
      } else {
        setError(res.message || 'Failed to change password.');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to change password. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const strengthScore = (() => {
    const p = form.newPassword;
    let s = 0;
    if (p.length >= 8) s++;
    if (/[A-Z]/.test(p)) s++;
    if (/[0-9]/.test(p)) s++;
    if (/[^A-Za-z0-9]/.test(p)) s++;
    return s;
  })();
  const strengthLabel = ['', 'Weak', 'Fair', 'Good', 'Strong'][strengthScore];
  const strengthColor = ['', 'bg-red-500', 'bg-amber-500', 'bg-blue-500', 'bg-emerald-500'][strengthScore];

  return (
    <div className="max-w-md mx-auto py-8">
      {/* Header */}
      <div className="flex items-center gap-3 mb-8">
        <button
          onClick={() => navigate(-1)}
          className="w-8 h-8 flex items-center justify-center rounded-lg text-slate-400 hover:bg-slate-800 hover:text-slate-200 transition-colors"
        >
          <ArrowLeft size={18} />
        </button>
        <div>
          <h1 className="text-xl font-black text-white">Change Password</h1>
          <p className="text-sm text-slate-500">Update your account password</p>
        </div>
      </div>

      <div className="bg-slate-800 border border-slate-700 rounded-2xl p-6 shadow-xl">
        <div className="w-12 h-12 bg-blue-600/10 border border-blue-500/20 rounded-2xl flex items-center justify-center mb-6">
          <ShieldCheck size={22} className="text-blue-400" />
        </div>

        <ErrorBanner msg={error} />

        <form onSubmit={handleSubmit} className="space-y-4">
          <PasswordField
            id="cp-current"
            label="Current Password"
            value={form.previousPassword}
            onChange={handleChange('previousPassword')}
            placeholder="Your current password"
            show={show.prev}
            onToggle={toggleShow('prev')}
            disabled={loading}
          />

          <div className="border-t border-slate-700 pt-4">
            <PasswordField
              id="cp-new"
              label="New Password"
              value={form.newPassword}
              onChange={handleChange('newPassword')}
              placeholder="Min. 8 chars, uppercase, number, symbol"
              show={show.next}
              onToggle={toggleShow('next')}
              disabled={loading}
            />

            {/* Strength bar */}
            {form.newPassword && (
              <div className="mt-2 space-y-1">
                <div className="flex gap-1">
                  {[1, 2, 3, 4].map(i => (
                    <div key={i} className={`flex-1 h-1 rounded-full transition-colors ${i <= strengthScore ? strengthColor : 'bg-slate-700'}`} />
                  ))}
                </div>
                <p className={`text-[10px] font-semibold ${strengthScore <= 1 ? 'text-red-400' : strengthScore === 2 ? 'text-amber-400' : strengthScore === 3 ? 'text-blue-400' : 'text-emerald-400'}`}>
                  {strengthLabel}
                </p>
              </div>
            )}
          </div>

          <PasswordField
            id="cp-confirm"
            label="Confirm New Password"
            value={form.confirmPassword}
            onChange={handleChange('confirmPassword')}
            placeholder="Repeat new password"
            show={show.confirm}
            onToggle={toggleShow('confirm')}
            disabled={loading}
          />

          {form.confirmPassword && (
            <p className={`text-xs font-medium ${form.newPassword === form.confirmPassword ? 'text-emerald-400' : 'text-red-400'}`}>
              {form.newPassword === form.confirmPassword ? '✓ Passwords match' : '✗ Passwords do not match'}
            </p>
          )}

          <button
            id="cp-submit"
            type="submit"
            disabled={loading || form.newPassword !== form.confirmPassword || form.newPassword.length < 8}
            className="w-full flex items-center justify-center gap-2 py-3 mt-2 bg-blue-600 hover:bg-blue-500 text-white font-bold rounded-xl transition-all disabled:opacity-60"
          >
            {loading
              ? <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              : <><ShieldCheck size={16} /> Change Password</>
            }
          </button>
        </form>
      </div>
    </div>
  );
};

export default ChangePassword;
