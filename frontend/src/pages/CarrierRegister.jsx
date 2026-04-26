import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import {
  Building2, FileText, Mail, Phone, Lock, MapPin,
  Hash, CheckCircle2, ChevronRight, Truck, ShieldCheck, KeyRound, Clock
} from 'lucide-react';

/* ─── Helpers ─────────────────────────────────────────── */
const API = (path, method, body) =>
  fetch(path, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  }).then(r => r.json());

/* ─── STEP 1 – Registration Form ─────────────────────── */
const RegisterForm = ({ formData, onChange, onSubmit, submitting }) => (
  <form onSubmit={onSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-4">
    <Field label="Company Name" name="companyName" value={formData.companyName} onChange={onChange} icon={<Building2 size={16} />} placeholder="Acme Insurance" />
    <div className="space-y-1">
      <label className="text-xs font-semibold text-slate-400">Company Type</label>
      <select name="companyType" value={formData.companyType} onChange={onChange} required
        className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:ring-1 focus:ring-amber-500 h-[38px]">
        <option value="INSURANCE">Insurance Provider</option>
        <option value="BANK">Bank</option>
        <option value="PARTNER">Third-Party Partner</option>
      </select>
    </div>
    <Field label="Registration Number" name="registrationNumber" value={formData.registrationNumber} onChange={onChange} icon={<Hash size={16} />} placeholder="REG-12345" />
    <Field label="License Number" name="licenseNumber" value={formData.licenseNumber} onChange={onChange} icon={<FileText size={16} />} placeholder="LIC-9999" />
    <Field label="Official Email" name="email" type="email" value={formData.email} onChange={onChange} icon={<Mail size={16} />} placeholder="contact@company.com" />
    <Field label="Official Phone" name="mobile" value={formData.mobile} onChange={onChange} icon={<Phone size={16} />} placeholder="+1 234 567 890" />
    <Field label="Contact Person Name" name="contactPersonName" value={formData.contactPersonName} onChange={onChange} placeholder="John Doe" />
    <Field label="Contact Person Phone" name="contactPersonPhone" value={formData.contactPersonPhone} onChange={onChange} placeholder="+1 987 654 321" />
    <Field label="Tax ID / GSTIN" name="taxId" value={formData.taxId} onChange={onChange} placeholder="TAX-1234" />
    <Field label="Password" name="password" type="password" value={formData.password} onChange={onChange} icon={<Lock size={16} />} placeholder="••••••••" />
    <div className="space-y-1 md:col-span-2">
      <label className="text-xs font-semibold text-slate-400">Headquarters Address</label>
      <div className="relative">
        <MapPin size={16} className="absolute left-3 top-3 text-slate-500" />
        <input required name="address" value={formData.address} onChange={onChange}
          className="w-full bg-slate-950 border border-slate-800 rounded-lg pl-9 pr-3 py-2 text-sm text-white focus:ring-1 focus:ring-amber-500"
          placeholder="123 Business Avenue, City, State" />
      </div>
    </div>
    <Field label="Website (optional)" name="website" value={formData.website} onChange={onChange} placeholder="https://company.com" />
    <div className="md:col-span-2 mt-4 flex items-center justify-between">
      <Link to="/login" className="text-sm text-slate-400 hover:text-white transition-colors">
        Already have an account? Log in
      </Link>
      <button type="submit" disabled={submitting}
        className="bg-amber-600 hover:bg-amber-500 text-white font-bold py-2.5 px-6 rounded-lg shadow-lg shadow-amber-900/50 flex items-center gap-2 transition-all disabled:opacity-60">
        {submitting ? 'Sending OTP…' : 'Continue'} <ChevronRight size={18} />
      </button>
    </div>
  </form>
);

/* ─── STEP 2 – OTP Verification ───────────────────────── */
const OtpForm = ({ email, onVerify, submitting }) => {
  const [otp, setOtp] = useState('');
  return (
    <div className="max-w-sm mx-auto text-center space-y-6 py-4">
      <div className="w-16 h-16 bg-amber-500/10 rounded-full flex items-center justify-center mx-auto">
        <KeyRound size={32} className="text-amber-400" />
      </div>
      <div>
        <h3 className="text-xl font-bold text-white">Verify your email</h3>
        <p className="text-slate-400 text-sm mt-1">
          We sent a 6-digit OTP to <span className="text-amber-400 font-medium">{email}</span>
        </p>
      </div>
      <form onSubmit={(e) => { e.preventDefault(); onVerify(otp); }} className="space-y-4">
        <input
          value={otp}
          onChange={e => setOtp(e.target.value)}
          maxLength={6}
          required
          placeholder="Enter 6-digit OTP"
          className="w-full text-center text-2xl font-mono tracking-[0.4em] bg-slate-950 border border-slate-700 rounded-xl px-4 py-3 text-white focus:ring-2 focus:ring-amber-500 focus:border-amber-500"
        />
        <button type="submit" disabled={submitting || otp.length < 6}
          className="w-full bg-amber-600 hover:bg-amber-500 text-white font-bold py-3 rounded-lg flex items-center justify-center gap-2 transition-all disabled:opacity-60">
          {submitting ? 'Verifying…' : <><ShieldCheck size={18} /> Verify &amp; Submit Application</>}
        </button>
      </form>
    </div>
  );
};

/* ─── STEP 3 – Pending Admin Approval ─────────────────── */
const PendingScreen = ({ navigate }) => (
  <div className="max-w-sm mx-auto text-center space-y-6 py-8">
    <div className="w-20 h-20 bg-amber-500/10 rounded-full flex items-center justify-center mx-auto animate-pulse">
      <Clock size={40} className="text-amber-400" />
    </div>
    <div className="space-y-2">
      <h3 className="text-2xl font-black text-white">Application Submitted!</h3>
      <p className="text-slate-400 text-sm leading-relaxed">
        Your carrier account is now <span className="text-amber-400 font-semibold">pending admin review</span>.
        Our compliance team will assess your application and notify you by email once a decision has been made.
      </p>
    </div>
    <div className="bg-slate-800/60 border border-slate-700/50 rounded-xl p-4 text-left space-y-3">
      <p className="text-xs font-semibold text-slate-400 uppercase tracking-wide">What happens next?</p>
      {[
        'AI risk assessment has been performed automatically',
        'Admin will review your credentials and AI score',
        'You will receive an approval or rejection email',
        'Login becomes available only after admin approval'
      ].map((step, i) => (
        <div key={i} className="flex items-center gap-3 text-sm text-slate-300">
          <span className="w-5 h-5 rounded-full bg-amber-500/20 text-amber-400 text-xs font-bold flex items-center justify-center shrink-0">{i + 1}</span>
          {step}
        </div>
      ))}
    </div>
    <button onClick={() => navigate('/login')} className="w-full py-3 bg-amber-600 hover:bg-amber-500 text-white font-bold rounded-xl transition-colors">
      Back to Login
    </button>
  </div>
);

/* ─── Reusable Field ──────────────────────────────────── */
const Field = ({ label, name, type = 'text', value, onChange, icon, placeholder }) => (
  <div className="space-y-1">
    <label className="text-xs font-semibold text-slate-400">{label}</label>
    <div className="relative">
      {icon && <span className="absolute left-3 top-3 text-slate-500">{icon}</span>}
      <input
        required={name !== 'website'}
        type={type}
        name={name}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        className={`w-full bg-slate-950 border border-slate-800 rounded-lg ${icon ? 'pl-9' : 'pl-3'} pr-3 py-2 text-sm text-white focus:ring-1 focus:ring-amber-500 focus:border-amber-500`}
      />
    </div>
  </div>
);

/* ─── Main Component ──────────────────────────────────── */
const CarrierRegister = () => {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [submitting, setSubmitting] = useState(false);
  const [formData, setFormData] = useState({
    companyName: '', registrationNumber: '', email: '', mobile: '',
    password: '', address: '', companyType: 'INSURANCE', licenseNumber: '',
    taxId: '', contactPersonName: '', contactPersonPhone: '', website: ''
  });

  const handleChange = e => setFormData(p => ({ ...p, [e.target.name]: e.target.value }));

  const handleRegister = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const data = await API('/api/v1/auth/carrier/register', 'POST', formData);
      if (data.success) {
        toast.success('OTP sent to your email!');
        setStep(2);
      } else {
        toast.error(data.message || 'Registration failed');
      }
    } catch {
      toast.error('Network error. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleVerify = async (otp) => {
    setSubmitting(true);
    try {
      const data = await API('/api/v1/auth/carrier/verify', 'PATCH', { email: formData.email, otp });
      if (data.success) {
        toast.success('Application submitted! Awaiting admin approval.');
        setStep(3);
      } else {
        toast.error(data.message || 'Verification failed');
      }
    } catch {
      toast.error('Verification failed. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const STEPS = [
    { n: 1, label: 'Company Details' },
    { n: 2, label: 'Verify Email' },
    { n: 3, label: 'Pending Approval' },
  ];

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center p-4">
      <div className="w-full max-w-4xl bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl overflow-hidden flex flex-col md:flex-row">

        {/* Left branding */}
        <div className="w-full md:w-1/3 bg-gradient-to-br from-amber-500 to-orange-600 p-8 text-white flex flex-col justify-between flex-shrink-0">
          <div>
            <div className="w-12 h-12 bg-white/20 rounded-xl flex items-center justify-center mb-6">
              <Truck size={24} className="text-white" />
            </div>
            <h1 className="text-3xl font-black tracking-tight mb-2">Carrier Partner Portal</h1>
            <p className="text-amber-100/80 text-sm leading-relaxed">
              Join our exclusive network of insurance providers and streamline claims with our AI-powered platform.
            </p>
          </div>

          {/* Step indicator */}
          <div className="mt-8 hidden md:flex flex-col gap-3">
            {STEPS.map(s => (
              <div key={s.n} className={`flex items-center gap-3 text-sm font-medium ${step >= s.n ? 'text-white' : 'text-amber-200/40'}`}>
                <span className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${step > s.n ? 'bg-white text-amber-600' : step === s.n ? 'bg-white/30' : 'bg-white/10'}`}>
                  {step > s.n ? <CheckCircle2 size={14} /> : s.n}
                </span>
                {s.label}
              </div>
            ))}
          </div>
        </div>

        {/* Right content */}
        <div className="w-full p-8 flex-1 flex flex-col">
          {step === 1 && (
            <>
              <h2 className="text-2xl font-bold text-white mb-6">Create your Carrier account</h2>
              <RegisterForm formData={formData} onChange={handleChange} onSubmit={handleRegister} submitting={submitting} />
            </>
          )}
          {step === 2 && (
            <>
              <h2 className="text-2xl font-bold text-white mb-6">Email Verification</h2>
              <OtpForm email={formData.email} onVerify={handleVerify} submitting={submitting} />
            </>
          )}
          {step === 3 && <PendingScreen navigate={navigate} />}
        </div>
      </div>
    </div>
  );
};

export default CarrierRegister;
