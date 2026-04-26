import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getClaims } from '../api/claim.service';
import { useAuth } from '../context/AuthContext';
import StatusBadge from '../components/StatusBadge';
import Loader from '../components/Loader';
import ErrorMessage from '../components/ErrorMessage';
import {
  FileText, CheckCircle, XCircle, Clock, Plus, ArrowRight,
  ShieldCheck, TrendingUp, AlertCircle,
} from 'lucide-react';

/* ─── Stat Card ───────────────────────────────────────────── */
const StatCard = ({ label, value, icon: Icon, color, bg, onClick }) => (
  <button
    onClick={onClick}
    className={`bg-slate-800 rounded-xl shadow-sm border border-slate-700 p-5 flex items-center gap-4 w-full text-left hover:border-slate-600 hover:shadow-md hover:shadow-black/20 transition-all duration-200 group ${onClick ? 'cursor-pointer' : ''}`}
  >
    <div className={`${bg} p-3 rounded-xl flex-shrink-0 border border-white/5`}>
      <Icon className={`w-6 h-6 ${color}`} />
    </div>
    <div>
      <p className="text-sm text-slate-400 font-medium group-hover:text-slate-300 transition-colors">{label}</p>
      <p className={`text-2xl font-bold ${color}`}>{value}</p>
    </div>
  </button>
);

/* ─── Mini Status Timeline Strip ─────────────────────────── */
const StatusStrip = ({ status }) => {
  const STEPS = ['PENDING', 'PROCESSING', 'REVIEW'];
  const isTerminal = status === 'APPROVED' || status === 'REJECTED';
  const currentIdx = STEPS.indexOf(status);

  return (
    <div className="flex items-center gap-1.5 mt-4">
      {STEPS.map((s, idx) => (
        <div key={s} className="flex items-center gap-1 flex-1">
          <div className={`h-1 rounded-full flex-1 transition-all ${
            isTerminal && status === 'APPROVED' ? 'bg-emerald-500/50' :
            isTerminal && status === 'REJECTED' ? 'bg-red-500/50' :
            idx <= currentIdx ? 'bg-blue-500' : 'bg-slate-700'
          }`} />
        </div>
      ))}
      <div className={`h-1 rounded-full w-4 ${
        status === 'APPROVED' ? 'bg-emerald-500' :
        status === 'REJECTED' ? 'bg-red-500' : 'bg-slate-700'
      }`} />
    </div>
  );
};

/* ─── Claim Row Card ──────────────────────────────────────── */
const ClaimRowCard = ({ claim, onClick }) => (
  <div
    onClick={() => onClick(claim.id)}
    className="bg-slate-800 rounded-xl border border-slate-700 shadow-sm hover:shadow-md hover:shadow-black/20 hover:border-blue-500/30 transition-all duration-200 cursor-pointer p-5 flex flex-col justify-between"
  >
    <div>
      <div className="flex items-start justify-between gap-3 mb-3">
        <div className="min-w-0">
          <p className="text-xs text-blue-400 font-bold mb-1 tracking-wider">#{claim.id} · {claim.policyNumber}</p>
          <p className="font-semibold text-slate-200 truncate">{claim.patientName || 'Claim'}</p>
          <p className="text-xs text-slate-500 truncate mt-0.5">{claim.hospitalName || '—'}</p>
        </div>
        <StatusBadge status={claim.status} />
      </div>
    </div>
    
    <div className="mt-auto">
      <div className="flex items-center justify-between mb-3 pt-4 border-t border-slate-700/50">
        <span className="text-sm font-bold text-slate-200">
          {claim.amount != null ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(claim.amount) : '—'}
        </span>
        <span className="text-xs text-slate-500 font-medium">
          {claim.createdDate ? new Date(claim.createdDate).toLocaleDateString('en-US', { day: 'numeric', month: 'short', year: 'numeric' }) : '—'}
        </span>
      </div>
      <StatusStrip status={claim.status} />
    </div>
  </div>
);

/* ─── Dashboard ───────────────────────────────────────────── */
const Dashboard = () => {
  const { user } = useAuth();
  const navigate  = useNavigate();
  const [claims, setClaims]   = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(null);

  const isAdmin = user?.userRole === 'FMG_ADMIN';

  const fetchDashboardData = async () => {
    try {
      setLoading(true); setError(null);
      const data = await getClaims({ page: 0, size: 12 });
      setClaims(data?.content || []);
    } catch {
      setError('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchDashboardData(); }, []);

  if (loading) return <Loader fullScreen message="Loading dashboard…" />;
  if (error)   return <ErrorMessage message={error} onRetry={fetchDashboardData} />;

  const total    = claims.length;
  const approved = claims.filter(c => c.status === 'APPROVED').length;
  const rejected = claims.filter(c => c.status === 'REJECTED').length;
  const pending  = claims.filter(c => ['PENDING', 'PROCESSING', 'REVIEW'].includes(c.status)).length;

  const stats = [
    { label: 'Total Claims', value: total,    icon: FileText,    color: 'text-blue-400',   bg: 'bg-blue-500/10' },
    { label: 'Approved',     value: approved,  icon: CheckCircle, color: 'text-emerald-400',bg: 'bg-emerald-500/10' },
    { label: 'In Progress',  value: pending,   icon: Clock,       color: 'text-amber-400',  bg: 'bg-amber-500/10' },
    { label: 'Rejected',     value: rejected,  icon: XCircle,     color: 'text-red-400',    bg: 'bg-red-500/10' },
  ];

  const inProgress = claims.filter(c => ['PENDING', 'PROCESSING', 'REVIEW'].includes(c.status));

  return (
    <div className="space-y-6 max-w-[1400px] mx-auto">
      {/* ── Header ── */}
      <div className="flex justify-between items-start flex-wrap gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-100 tracking-tight">
            Welcome, {user?.name || user?.username || 'User'} 👋
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            {isAdmin ? 'Admin overview of current claims' : "Here's a summary of your insurance claims"}
          </p>
        </div>
        <div className="flex gap-3">
          {isAdmin && (
            <button
              onClick={() => navigate('/admin')}
              className="flex items-center gap-2 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 px-4 py-2 rounded-xl text-sm font-medium transition-colors shadow-sm"
            >
              <ShieldCheck className="w-4 h-4 text-blue-400" /> Admin Panel
            </button>
          )}
          {!isAdmin && (
            <button
              onClick={() => navigate('/claims/upload')}
              className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-5 py-2 rounded-xl text-sm font-medium transition-colors shadow-md shadow-blue-900/20"
            >
              <Plus className="w-4 h-4" /> New Claim
            </button>
          )}
        </div>
      </div>

      {/* ── Stats ── */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map(s => (
          <StatCard
            key={s.label}
            {...s}
            onClick={s.label !== 'Total Claims' ? () => navigate('/claims') : undefined}
          />
        ))}
      </div>

      {/* ── In Progress Alert ── */}
      {inProgress.length > 0 && !isAdmin && (
        <div className="bg-blue-900/20 border border-blue-500/20 rounded-2xl p-5 flex items-center gap-4">
          <div className="bg-blue-500/20 p-3 rounded-xl flex-shrink-0 border border-blue-500/30">
            <TrendingUp className="w-6 h-6 text-blue-400" />
          </div>
          <div className="flex-1">
            <p className="font-semibold text-blue-100">
              {inProgress.length} claim{inProgress.length > 1 ? 's' : ''} in progress
            </p>
            <p className="text-sm text-blue-300/80 mt-1">
              Your claims are being processed. We'll update you once reviewed.
            </p>
          </div>
          <button
            onClick={() => navigate('/claims')}
            className="flex items-center gap-1.5 text-blue-400 text-sm font-semibold hover:text-blue-300 transition-colors bg-blue-500/10 px-4 py-2 rounded-lg"
          >
            View <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* ── Recent Claims ── */}
      <div className="bg-slate-800 rounded-2xl shadow-sm border border-slate-700">
        <div className="px-6 py-5 border-b border-slate-700 flex items-center justify-between">
          <h2 className="text-base font-bold text-slate-100">Recent Claims</h2>
          <button
            onClick={() => navigate('/claims')}
            className="text-sm text-blue-400 font-medium hover:text-blue-300 flex items-center gap-1 transition-colors"
          >
            View All <ArrowRight className="w-4 h-4" />
          </button>
        </div>
        <div className="p-6 bg-slate-900/20 rounded-b-2xl">
          {claims.length === 0 ? (
            <div className="text-center py-16">
              <div className="bg-slate-800 w-16 h-16 rounded-2xl flex items-center justify-center mx-auto mb-4 border border-slate-700 shadow-inner">
                <AlertCircle className="w-8 h-8 text-slate-500" />
              </div>
              <p className="text-slate-300 font-medium text-lg">No claims yet</p>
              <p className="text-slate-500 text-sm mt-1 mb-6">Submit your first claim to get started</p>
              {!isAdmin && (
                <button
                  onClick={() => navigate('/claims/upload')}
                  className="inline-flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-6 py-2.5 rounded-xl text-sm font-medium transition-colors shadow-lg shadow-blue-900/20"
                >
                  <Plus className="w-4 h-4" /> Start New Claim
                </button>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
              {claims.slice(0, 6).map(claim => (
                <ClaimRowCard
                  key={claim.id}
                  claim={claim}
                  onClick={(id) => navigate(`/claims/${id}`)}
                />
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
