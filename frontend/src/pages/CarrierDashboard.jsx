import React, { useState, useEffect, useCallback } from 'react';
import {
  Truck, CheckCircle, XCircle, ShieldCheck, ShieldAlert,
  Flag, MessageSquare, Brain, RefreshCw, X, AlertTriangle,
  ChevronRight, Activity, FileSearch, Loader2
} from 'lucide-react';
import toast from 'react-hot-toast';

/* ─── API helper ─────────────────────────────────────── */
const token = () => localStorage.getItem('token');
const apiFetch = (url, method = 'GET', body) =>
  fetch(url, {
    method,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token()}`
    },
    body: body ? JSON.stringify(body) : undefined
  }).then(r => r.json());

/* ─── Status Badge ───────────────────────────────────── */
const STATUS_COLORS = {
  PENDING:    'bg-amber-500/10 text-amber-400 border-amber-500/20',
  PROCESSING: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  APPROVED:   'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
  REJECTED:   'bg-red-500/10 text-red-400 border-red-500/20',
  REVIEW:     'bg-purple-500/10 text-purple-400 border-purple-500/20',
};
const StatusBadge = ({ status }) => (
  <span className={`px-2 py-0.5 rounded border text-[11px] font-bold ${STATUS_COLORS[status] || 'bg-slate-700 text-slate-400 border-slate-600'}`}>
    {status}
  </span>
);

/* ─── Risk Score Gauge ───────────────────────────────── */
const RiskGauge = ({ score }) => {
  const pct = Math.round((score ?? 0) * 100);
  const color = pct > 70 ? 'bg-red-500' : pct > 40 ? 'bg-amber-500' : 'bg-emerald-500';
  return (
    <div className="space-y-1">
      <div className="flex justify-between text-xs">
        <span className="text-slate-400">Risk Score</span>
        <span className={`font-bold ${pct > 70 ? 'text-red-400' : pct > 40 ? 'text-amber-400' : 'text-emerald-400'}`}>{pct}%</span>
      </div>
      <div className="h-1.5 bg-slate-700 rounded-full overflow-hidden">
        <div className={`h-full rounded-full transition-all ${color}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
};

/* ─── Remark Modal ───────────────────────────────────── */
const RemarkModal = ({ claimId, onClose, onSuccess }) => {
  const [remark, setRemark] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const QUICK = ['Policy expired', 'Coverage limit exceeded', 'Duplicate claim', 'Billing mismatch detected'];

  const submit = async () => {
    if (!remark.trim()) return;
    setSubmitting(true);
    const data = await apiFetch(`/api/v1/carrier/claims/${claimId}/remark`, 'PATCH', { remark });
    if (data.success) { toast.success('Remark added'); onSuccess(); onClose(); }
    else toast.error(data.message);
    setSubmitting(false);
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/70 flex items-center justify-center p-4">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-md shadow-2xl">
        <div className="flex items-center justify-between p-5 border-b border-slate-700">
          <h3 className="font-bold text-white flex items-center gap-2"><MessageSquare size={18} className="text-amber-400" /> Add Remark</h3>
          <button onClick={onClose} className="text-slate-500 hover:text-white"><X size={20} /></button>
        </div>
        <div className="p-5 space-y-4">
          <div className="flex flex-wrap gap-2">
            {QUICK.map(q => (
              <button key={q} onClick={() => setRemark(q)}
                className="text-xs bg-slate-800 hover:bg-amber-500/20 hover:text-amber-300 text-slate-400 border border-slate-700 px-3 py-1.5 rounded-lg transition-colors">
                {q}
              </button>
            ))}
          </div>
          <textarea
            value={remark}
            onChange={e => setRemark(e.target.value)}
            rows={4}
            placeholder="Enter your remark…"
            className="w-full bg-slate-950 border border-slate-700 rounded-xl p-3 text-sm text-white resize-none focus:ring-1 focus:ring-amber-500"
          />
          <div className="flex justify-end gap-3">
            <button onClick={onClose} className="px-4 py-2 text-sm text-slate-400 hover:text-white">Cancel</button>
            <button onClick={submit} disabled={submitting || !remark.trim()}
              className="px-5 py-2 bg-amber-600 hover:bg-amber-500 text-white font-bold rounded-lg text-sm disabled:opacity-50">
              {submitting ? 'Saving…' : 'Save Remark'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

/* ─── AI Analysis Panel ──────────────────────────────── */
const AiPanel = ({ claimId, onClose }) => {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [prompt, setPrompt] = useState('Analyze this claim for fraud risk, billing mismatch, and policy coverage issues.');

  const analyze = async () => {
    setLoading(true);
    const data = await apiFetch(`/api/v1/carrier/claims/${claimId}/ai-analyze`, 'POST', { prompt });
    if (data.success) setResult(data.data);
    else toast.error(data.message || 'AI analysis failed');
    setLoading(false);
  };

  const flagColor = (score) => {
    const pct = Math.round((score ?? 0) * 100);
    return pct > 70 ? 'text-red-400' : pct > 40 ? 'text-amber-400' : 'text-emerald-400';
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/70 flex items-end md:items-center justify-center p-4">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-5 border-b border-slate-700 sticky top-0 bg-slate-900">
          <h3 className="font-bold text-white flex items-center gap-2"><Brain size={18} className="text-purple-400" /> AI Risk Analysis</h3>
          <button onClick={onClose} className="text-slate-500 hover:text-white"><X size={20} /></button>
        </div>
        <div className="p-5 space-y-4">
          <div className="space-y-2">
            <label className="text-xs text-slate-400 font-semibold">Analysis Prompt</label>
            <textarea value={prompt} onChange={e => setPrompt(e.target.value)} rows={2}
              className="w-full bg-slate-950 border border-slate-700 rounded-xl p-3 text-sm text-white resize-none focus:ring-1 focus:ring-purple-500" />
          </div>
          <button onClick={analyze} disabled={loading}
            className="w-full bg-purple-600 hover:bg-purple-500 text-white font-bold py-2.5 rounded-xl flex items-center justify-center gap-2 disabled:opacity-60 transition-colors">
            {loading ? <><Loader2 size={16} className="animate-spin" /> Analyzing…</> : <><Brain size={16} /> Run AI Analysis</>}
          </button>

          {result && (
            <div className="space-y-4 pt-2">
              {/* Risk gauge */}
              <div className="bg-slate-950/60 p-4 rounded-xl border border-slate-700/50 space-y-3">
                <RiskGauge score={result.riskScore} />
                <div className="flex justify-between text-xs">
                  <span className="text-slate-400">Verdict</span>
                  <span className={`font-bold ${result.verdict === 'APPROVED' ? 'text-emerald-400' : result.verdict === 'REJECTED' ? 'text-red-400' : 'text-amber-400'}`}>{result.verdict}</span>
                </div>
                <div className="flex justify-between text-xs">
                  <span className="text-slate-400">Confidence</span>
                  <span className={`font-bold ${flagColor(result.confidence)}`}>{Math.round((result.confidence ?? 0) * 100)}%</span>
                </div>
              </div>

              {/* Flags */}
              {result.flags?.length > 0 && (
                <div className="space-y-2">
                  <p className="text-xs font-semibold text-slate-400 uppercase tracking-wide">Risk Flags</p>
                  {result.flags.map((f, i) => (
                    <div key={i} className="flex items-start gap-2 text-sm text-amber-300 bg-amber-500/5 border border-amber-500/20 rounded-lg p-2.5">
                      <AlertTriangle size={14} className="mt-0.5 shrink-0" /> {f}
                    </div>
                  ))}
                </div>
              )}

              {/* Recommendation */}
              {result.recommendation && (
                <div className="bg-blue-500/5 border border-blue-500/20 rounded-xl p-4">
                  <p className="text-xs font-semibold text-blue-400 mb-1">Recommendation</p>
                  <p className="text-sm text-slate-200">{result.recommendation}</p>
                </div>
              )}

              {/* Validations */}
              {result.validations && (
                <div className="grid grid-cols-3 gap-2">
                  {[['Policy Active', result.validations.policyActive], ['Documents', result.validations.documentsComplete], ['Within Limit', result.validations.withinLimit]].map(([k, v]) => (
                    <div key={k} className={`rounded-lg p-2.5 border text-center text-xs font-bold ${v ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400' : 'bg-red-500/10 border-red-500/20 text-red-400'}`}>
                      {v ? '✓' : '✗'} {k}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

/* ─── Policy Status Modal ────────────────────────────── */
const PolicyModal = ({ result, onClose }) => (
  <div className="fixed inset-0 z-50 bg-black/70 flex items-center justify-center p-4">
    <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-sm shadow-2xl">
      <div className="flex items-center justify-between p-5 border-b border-slate-700">
        <h3 className="font-bold text-white flex items-center gap-2"><ShieldCheck size={18} className="text-blue-400" /> Policy Status</h3>
        <button onClick={onClose} className="text-slate-500 hover:text-white"><X size={20} /></button>
      </div>
      <div className="p-6 space-y-4 text-center">
        <div className={`w-16 h-16 rounded-full flex items-center justify-center mx-auto ${result.status === 'VALID' ? 'bg-emerald-500/10' : 'bg-red-500/10'}`}>
          {result.status === 'VALID'
            ? <CheckCircle size={32} className="text-emerald-400" />
            : <XCircle size={32} className="text-red-400" />}
        </div>
        <div>
          <p className={`text-2xl font-black ${result.status === 'VALID' ? 'text-emerald-400' : 'text-red-400'}`}>{result.status}</p>
          <p className="text-slate-400 text-sm mt-1 font-mono">{result.policyNumber}</p>
        </div>
        <p className="text-slate-300 text-sm bg-slate-800 rounded-lg p-3">{result.reason}</p>
        <button onClick={onClose} className="w-full py-2.5 bg-slate-800 hover:bg-slate-700 text-white rounded-xl font-semibold transition-colors">Close</button>
      </div>
    </div>
  </div>
);

/* ─── Main Dashboard ─────────────────────────────────── */
const CarrierDashboard = () => {
  const [claims, setClaims] = useState([]);
  const [loading, setLoading] = useState(true);
  const [remarkTarget, setRemarkTarget] = useState(null);
  const [aiTarget, setAiTarget] = useState(null);
  const [policyResult, setPolicyResult] = useState(null);
  const [actionLoading, setActionLoading] = useState(null);

  const fetchClaims = useCallback(async () => {
    setLoading(true);
    const data = await apiFetch('/api/v1/carrier/claims');
    if (data.success) setClaims(data.data ?? []);
    else toast.error('Failed to load assigned claims');
    setLoading(false);
  }, []);

  useEffect(() => { fetchClaims(); }, [fetchClaims]);

  const action = async (claimId, path, method = 'PATCH', body) => {
    setActionLoading(`${claimId}-${path}`);
    const data = await apiFetch(`/api/v1/carrier/claims/${claimId}/${path}`, method, body);
    if (data.success) { toast.success(data.message); fetchClaims(); }
    else toast.error(data.message || 'Action failed');
    setActionLoading(null);
  };

  const getPolicyStatus = async (claimId) => {
    const data = await apiFetch(`/api/v1/carrier/claims/${claimId}/policy-status`, 'GET');
    if (data.success) setPolicyResult(data.data);
    else toast.error(data.message || 'Failed to get policy status');
  };

  const isLoading = (id, path) => actionLoading === `${id}-${path}`;

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="relative bg-gradient-to-r from-slate-900 to-slate-800 border border-amber-900/30 p-6 rounded-2xl shadow-xl overflow-hidden">
        <div className="absolute top-0 right-0 w-80 h-80 bg-amber-600/10 rounded-full blur-3xl -translate-y-1/2 translate-x-1/3 pointer-events-none" />
        <div className="relative z-10 flex items-center justify-between flex-wrap gap-4">
          <div>
            <h1 className="text-2xl font-black text-white flex items-center gap-3">
              <span className="p-2 bg-amber-500/10 rounded-xl"><Truck className="text-amber-500" size={24} /></span>
              Carrier Portal
            </h1>
            <p className="text-slate-400 mt-1 text-sm">Validate, analyze, and manage your assigned insurance claims.</p>
          </div>
          <button onClick={fetchClaims} className="flex items-center gap-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-300 rounded-xl text-sm font-medium transition-colors">
            <RefreshCw size={15} /> Refresh
          </button>
        </div>

        {/* Stats row */}
        <div className="relative z-10 mt-5 grid grid-cols-2 md:grid-cols-4 gap-3">
          {[
            { label: 'Assigned', value: claims.length, color: 'text-amber-400' },
            { label: 'Approved', value: claims.filter(c => c.status === 'APPROVED').length, color: 'text-emerald-400' },
            { label: 'Rejected', value: claims.filter(c => c.status === 'REJECTED').length, color: 'text-red-400' },
            { label: 'Pending', value: claims.filter(c => c.status === 'PENDING').length, color: 'text-blue-400' },
          ].map(s => (
            <div key={s.label} className="bg-slate-800/60 border border-slate-700/50 rounded-xl p-3">
              <p className="text-xs text-slate-500">{s.label}</p>
              <p className={`text-2xl font-black ${s.color}`}>{s.value}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Claims Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-800 flex items-center justify-between">
          <h2 className="text-base font-bold text-white flex items-center gap-2">
            <Activity size={16} className="text-amber-400" /> Assigned Claims
          </h2>
          <span className="text-xs text-slate-500">{claims.length} claim{claims.length !== 1 ? 's' : ''}</span>
        </div>

        {loading ? (
          <div className="p-12 flex flex-col items-center gap-3 text-slate-500">
            <Loader2 size={32} className="animate-spin text-amber-500/50" />
            <p>Loading claims…</p>
          </div>
        ) : claims.length === 0 ? (
          <div className="p-12 flex flex-col items-center gap-3 text-slate-500">
            <FileSearch size={48} className="text-slate-700" />
            <p>No claims assigned to your carrier yet.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left text-slate-300">
              <thead className="text-[11px] uppercase text-slate-500 bg-slate-800/50 border-b border-slate-800">
                <tr>
                  {['ID', 'Policy No.', 'Patient', 'Amount', 'Status', 'Risk', 'Actions'].map(h => (
                    <th key={h} className="px-5 py-3.5 font-semibold">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/50">
                {claims.map(claim => (
                  <tr key={claim.id} className="hover:bg-slate-800/30 transition-colors group">
                    <td className="px-5 py-3.5">
                      <span className="font-mono text-xs text-slate-400">#{claim.id}</span>
                    </td>
                    <td className="px-5 py-3.5 font-medium text-slate-200 text-xs font-mono">{claim.policyNumber}</td>
                    <td className="px-5 py-3.5 text-xs">{claim.patientName || '—'}</td>
                    <td className="px-5 py-3.5 font-mono text-xs">${(claim.amount ?? 0).toFixed(2)}</td>
                    <td className="px-5 py-3.5"><StatusBadge status={claim.status} /></td>
                    <td className="px-5 py-3.5 w-28">
                      <RiskGauge score={claim.riskScore} />
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-1.5 flex-wrap">
                        {/* Validate */}
                        <ActionBtn
                          icon={<ShieldCheck size={14} />}
                          title="Validate Policy"
                          color="blue"
                          loading={isLoading(claim.id, 'validate')}
                          onClick={() => action(claim.id, 'validate')} />
                        {/* Approve */}
                        <ActionBtn
                          icon={<CheckCircle size={14} />}
                          title="Approve"
                          color="emerald"
                          loading={isLoading(claim.id, 'approve')}
                          onClick={() => action(claim.id, 'approve')} />
                        {/* Reject */}
                        <ActionBtn
                          icon={<XCircle size={14} />}
                          title="Reject"
                          color="red"
                          loading={isLoading(claim.id, 'reject')}
                          onClick={() => action(claim.id, 'reject')} />
                        {/* Flag */}
                        <ActionBtn
                          icon={<Flag size={14} />}
                          title="Flag Suspicious"
                          color="amber"
                          loading={isLoading(claim.id, 'flag')}
                          onClick={() => action(claim.id, 'flag')} />
                        {/* Remark */}
                        <ActionBtn
                          icon={<MessageSquare size={14} />}
                          title="Add Remark"
                          color="purple"
                          onClick={() => setRemarkTarget(claim.id)} />
                        {/* Policy Status */}
                        <ActionBtn
                          icon={<ChevronRight size={14} />}
                          title="Policy Status"
                          color="slate"
                          onClick={() => getPolicyStatus(claim.id)} />
                        {/* AI */}
                        <ActionBtn
                          icon={<Brain size={14} />}
                          title="AI Analysis"
                          color="violet"
                          onClick={() => setAiTarget(claim.id)} />
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modals */}
      {remarkTarget && (
        <RemarkModal claimId={remarkTarget} onClose={() => setRemarkTarget(null)} onSuccess={fetchClaims} />
      )}
      {aiTarget && (
        <AiPanel claimId={aiTarget} onClose={() => setAiTarget(null)} />
      )}
      {policyResult && (
        <PolicyModal result={policyResult} onClose={() => setPolicyResult(null)} />
      )}
    </div>
  );
};

/* ─── Tiny ActionBtn ─────────────────────────────────── */
const COLOR_MAP = {
  blue:    'bg-blue-500/10 text-blue-400 hover:bg-blue-500 hover:text-white border-blue-500/20',
  emerald: 'bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500 hover:text-white border-emerald-500/20',
  red:     'bg-red-500/10 text-red-400 hover:bg-red-500 hover:text-white border-red-500/20',
  amber:   'bg-amber-500/10 text-amber-400 hover:bg-amber-500 hover:text-white border-amber-500/20',
  purple:  'bg-purple-500/10 text-purple-400 hover:bg-purple-500 hover:text-white border-purple-500/20',
  violet:  'bg-violet-500/10 text-violet-400 hover:bg-violet-500 hover:text-white border-violet-500/20',
  slate:   'bg-slate-700/50 text-slate-400 hover:bg-slate-600 hover:text-white border-slate-600',
};
const ActionBtn = ({ icon, title, color, onClick, loading }) => (
  <button
    title={title}
    onClick={onClick}
    disabled={loading}
    className={`p-1.5 rounded border text-[11px] transition-all ${COLOR_MAP[color]} disabled:opacity-50`}>
    {loading ? <Loader2 size={14} className="animate-spin" /> : icon}
  </button>
);

export default CarrierDashboard;
