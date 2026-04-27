import React, { useState, useEffect, useCallback } from 'react';
import { Truck, CheckCircle, XCircle, ShieldCheck, Flag, MessageSquare, Brain,
  RefreshCw, X, AlertTriangle, Activity, FileSearch, Loader2, User, Calendar,
  DollarSign, FileText, ChevronDown, ChevronUp, ShieldAlert, ShieldX } from 'lucide-react';
import toast from 'react-hot-toast';

const token = () => localStorage.getItem('token');
const api = (url, method = 'GET', body) =>
  fetch(url, { method, headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token()}` },
    body: body ? JSON.stringify(body) : undefined }).then(r => r.json());

const STATUS_COLORS = {
  PENDING:'bg-amber-500/10 text-amber-400 border-amber-500/20',
  PROCESSING:'bg-blue-500/10 text-blue-400 border-blue-500/20',
  APPROVED:'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
  REJECTED:'bg-red-500/10 text-red-400 border-red-500/20',
  REVIEW:'bg-purple-500/10 text-purple-400 border-purple-500/20',
};
const Badge = ({ v, cls }) => <span className={`px-2 py-0.5 rounded border text-[11px] font-bold ${cls}`}>{v}</span>;
const StatusBadge = ({ s }) => <Badge v={s} cls={STATUS_COLORS[s] || 'bg-slate-700 text-slate-400 border-slate-600'} />;
const RiskBar = ({ score }) => {
  const pct = Math.min(100, Math.max(0, Math.round(score ?? 0)));
  const c = pct > 70 ? 'bg-red-500' : pct > 40 ? 'bg-amber-500' : 'bg-emerald-500';
  const t = pct > 70 ? 'text-red-400' : pct > 40 ? 'text-amber-400' : 'text-emerald-400';
  return (
    <div className="space-y-1">
      <div className="flex justify-between text-xs"><span className="text-slate-500">Risk</span><span className={`font-bold ${t}`}>{pct}%</span></div>
      <div className="h-1.5 bg-slate-700 rounded-full overflow-hidden"><div className={`h-full ${c} rounded-full`} style={{ width: `${pct}%` }} /></div>
    </div>
  );
};

const InfoCard = ({ label, value, icon, cls = 'text-slate-200' }) => (
  <div className="bg-slate-900/60 border border-slate-700/50 rounded-xl p-3">
    <div className="flex items-center gap-1.5 text-slate-500 text-[10px] uppercase tracking-wider font-semibold mb-1.5">{icon}{label}</div>
    <p className={`text-xs font-medium break-words ${cls}`}>{value || '—'}</p>
  </div>
);

const fmt = d => d ? new Date(d).toLocaleDateString('en-US', { year:'numeric', month:'short', day:'numeric' }) : '—';
const money = v => v != null ? `$${Number(v).toFixed(2)}` : '—';

const PatientPanel = ({ c, onClose }) => (
  <tr><td colSpan={7} className="px-0 py-0">
    <div className="bg-slate-800/90 border-y border-amber-500/20 px-5 py-5">
      <div className="flex items-center justify-between mb-4">
        <h3 className="font-bold text-white flex items-center gap-2"><User size={15} className="text-amber-400"/>Claim #{c.claimId} — Full Details</h3>
        <button onClick={onClose} className="text-slate-500 hover:text-white"><X size={15}/></button>
      </div>
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 mb-4">
        <InfoCard label="Patient Name"     value={c.patient?.name}      icon={<User size={12}/>} cls="text-amber-300 font-bold"/>
        <InfoCard label="Email"            value={c.patient?.email}     icon={<FileText size={12}/>}/>
        <InfoCard label="Phone"            value={c.patient?.mobile}    icon={<FileText size={12}/>}/>
        <InfoCard label="Date of Birth"    value={fmt(c.patient?.dateOfBirth)} icon={<Calendar size={12}/>}/>
        <InfoCard label="Gender"           value={c.patient?.gender}    icon={<User size={12}/>}/>
        <InfoCard label="Address"          value={c.patient?.address}   icon={<FileText size={12}/>}/>
        <InfoCard label="Hospital"         value={c.hospitalName}       icon={<ShieldCheck size={12}/>}/>
        <InfoCard label="Diagnosis"        value={c.diagnosis}          icon={<FileText size={12}/>}/>
        <InfoCard label="Admission"        value={fmt(c.admissionDate)} icon={<Calendar size={12}/>}/>
        <InfoCard label="Discharge"        value={fmt(c.dischargeDate)} icon={<Calendar size={12}/>}/>
        <InfoCard label="Claimed Amount"   value={money(c.amount)}      icon={<DollarSign size={12}/>} cls="text-emerald-400 font-bold"/>
        <InfoCard label="Total Bill"       value={money(c.totalBillAmount)} icon={<DollarSign size={12}/>}/>
        <InfoCard label="Policy No."       value={c.policyNumber}       icon={<FileText size={12}/>}/>
        <InfoCard label="Claim Type"       value={c.claimType}          icon={<FileText size={12}/>}/>
        <InfoCard label="Policy Status"    value={c.policy?.status}     icon={<ShieldCheck size={12}/>}
          cls={c.policy?.status === 'VALID' ? 'text-emerald-400 font-bold' : 'text-red-400 font-bold'}/>
        <InfoCard label="Policy Reason"    value={c.policy?.reason}     icon={<ShieldAlert size={12}/>}/>
      </div>
      {/* Fraud section */}
      <div className="bg-slate-900/50 border border-slate-700/50 rounded-xl p-4 mb-3">
        <p className="text-[10px] font-bold text-slate-500 uppercase tracking-wider mb-3">Fraud / Risk Assessment</p>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          <InfoCard label="Risk Level"   value={c.fraud?.riskLevel}  icon={<AlertTriangle size={12}/>}
            cls={c.fraud?.riskLevel==='HIGH'?'text-red-400 font-bold':c.fraud?.riskLevel==='MEDIUM'?'text-amber-400 font-bold':'text-emerald-400 font-bold'}/>
          <InfoCard label="Health Score" value={c.fraud?.healthScore != null ? `${c.fraud.healthScore}/100` : '—'} icon={<Activity size={12}/>}/>
          <InfoCard label="Risk Flags"   value={c.fraud?.riskFlags || 'None detected'} icon={<Flag size={12}/>}/>
          <InfoCard label="AI Summary"   value={c.fraud?.aiSummary || 'Not analyzed'} icon={<Brain size={12}/>}/>
        </div>
        <div className="mt-3"><RiskBar score={c.fraud?.riskScore}/></div>
      </div>
      {c.reviewNotes && (
        <div className="bg-blue-500/5 border border-blue-500/20 rounded-xl p-3">
          <p className="text-[10px] text-blue-400 font-semibold uppercase mb-1">Review Notes</p>
          <p className="text-slate-300 text-xs whitespace-pre-wrap">{c.reviewNotes}</p>
        </div>
      )}
    </div>
  </td></tr>
);

const RemarkModal = ({ claimId, onClose, onSuccess }) => {
  const [remark, setRemark] = useState('');
  const [saving, setSaving] = useState(false);
  const QUICK = ['Policy expired','Coverage limit exceeded','Out-of-network provider','Deductible not met','Duplicate claim','Billing code mismatch'];
  const save = async () => {
    if (!remark.trim()) return;
    setSaving(true);
    const d = await api(`/api/v1/carrier/claims/${claimId}/remark`, 'PATCH', { remark });
    if (d.success) { toast.success('Remark saved'); onSuccess(); onClose(); }
    else toast.error(d.message || 'Failed');
    setSaving(false);
  };
  return (
    <div className="fixed inset-0 z-50 bg-black/70 flex items-center justify-center p-4">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-md shadow-2xl">
        <div className="flex items-center justify-between p-5 border-b border-slate-700">
          <h3 className="font-bold text-white flex items-center gap-2"><MessageSquare size={16} className="text-amber-400"/>Add Remark</h3>
          <button onClick={onClose} className="text-slate-500 hover:text-white"><X size={18}/></button>
        </div>
        <div className="p-5 space-y-4">
          <div className="flex flex-wrap gap-2">
            {QUICK.map(q => <button key={q} onClick={() => setRemark(q)} className="text-xs bg-slate-800 hover:bg-amber-500/20 hover:text-amber-300 text-slate-400 border border-slate-700 px-2.5 py-1.5 rounded-lg transition-colors">{q}</button>)}
          </div>
          <textarea value={remark} onChange={e => setRemark(e.target.value)} rows={4} placeholder="Enter your remark…"
            className="w-full bg-slate-950 border border-slate-700 rounded-xl p-3 text-sm text-white resize-none focus:ring-1 focus:ring-amber-500"/>
          <div className="flex justify-end gap-3">
            <button onClick={onClose} className="px-4 py-2 text-sm text-slate-400 hover:text-white">Cancel</button>
            <button onClick={save} disabled={saving || !remark.trim()} className="px-5 py-2 bg-amber-600 hover:bg-amber-500 text-white font-bold rounded-lg text-sm disabled:opacity-50">
              {saving ? 'Saving…' : 'Save'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

const AiModal = ({ claimId, onClose }) => {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [prompt, setPrompt] = useState('Analyze this claim for fraud risk, billing mismatch, and policy coverage issues.');
  const run = async () => {
    setLoading(true);
    const d = await api(`/api/v1/carrier/claims/${claimId}/ai-analyze`, 'POST', { prompt });
    if (d.success) setResult(d.data); else toast.error(d.message || 'AI failed');
    setLoading(false);
  };
  const rPct = result ? Math.round((result.riskScore ?? 0) * 100) : 0;
  const rc = rPct > 70 ? 'text-red-400' : rPct > 40 ? 'text-amber-400' : 'text-emerald-400';
  const rb = rPct > 70 ? 'bg-red-500' : rPct > 40 ? 'bg-amber-500' : 'bg-emerald-500';
  const VIcon = result?.verdict === 'APPROVED' ? ShieldCheck : result?.verdict === 'REJECTED' ? ShieldX : ShieldAlert;
  const vc = result?.verdict === 'APPROVED' ? 'text-emerald-400' : result?.verdict === 'REJECTED' ? 'text-red-400' : 'text-amber-400';
  return (
    <div className="fixed inset-0 z-50 bg-black/70 flex items-end md:items-center justify-center p-4">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-5 border-b border-slate-700 sticky top-0 bg-slate-900 z-10">
          <h3 className="font-bold text-white flex items-center gap-2"><Brain size={16} className="text-purple-400"/>AI Risk Analysis</h3>
          <button onClick={onClose} className="text-slate-500 hover:text-white"><X size={18}/></button>
        </div>
        <div className="p-5 space-y-4">
          <div><label className="text-xs text-slate-400 font-semibold block mb-1">Analysis Prompt</label>
            <textarea value={prompt} onChange={e => setPrompt(e.target.value)} rows={2}
              className="w-full bg-slate-950 border border-slate-700 rounded-xl p-3 text-sm text-white resize-none focus:ring-1 focus:ring-purple-500"/>
          </div>
          <button onClick={run} disabled={loading}
            className="w-full bg-purple-600 hover:bg-purple-500 text-white font-bold py-2.5 rounded-xl flex items-center justify-center gap-2 disabled:opacity-60">
            {loading ? <><Loader2 size={15} className="animate-spin"/>Analyzing…</> : <><Brain size={15}/>Run AI Analysis</>}
          </button>
          {result && (
            <div className="space-y-4 pt-2">
              <div className="bg-slate-950/60 border border-slate-700/50 rounded-xl p-4 space-y-3">
                <div className="flex items-center gap-2">
                  <VIcon size={20} className={vc}/>
                  <span className={`font-bold text-lg ${vc}`}>{result.verdict}</span>
                  <span className="ml-auto text-xs text-slate-500">Confidence: <b className={`${vc}`}>{Math.round((result.confidence??0)*100)}%</b></span>
                </div>
                <div>
                  <div className="flex justify-between text-xs mb-1"><span className="text-slate-400">AI Risk Score</span><span className={`font-bold ${rc}`}>{rPct}%</span></div>
                  <div className="h-2 bg-slate-700 rounded-full overflow-hidden"><div className={`h-full rounded-full ${rb}`} style={{ width:`${rPct}%` }}/></div>
                </div>
              </div>
              {result.flags?.length > 0 && (
                <div className="bg-amber-500/5 border border-amber-500/20 rounded-xl p-4">
                  <p className="text-xs font-bold text-amber-400 uppercase mb-2">⚠ Flags</p>
                  <ul className="space-y-1">{result.flags.map((f,i) => <li key={i} className="text-amber-200/80 text-xs flex gap-2"><span className="text-amber-500">•</span>{f}</li>)}</ul>
                </div>
              )}
              {result.recommendation && (
                <div className="bg-blue-500/5 border border-blue-500/20 rounded-xl p-4">
                  <p className="text-xs font-bold text-blue-400 mb-1">Recommendation</p>
                  <p className="text-sm text-slate-200">{result.recommendation}</p>
                </div>
              )}
              {result.validations && (
                <div className="grid grid-cols-3 gap-2">
                  {[['Policy Active', result.validations.policyActive],['Documents', result.validations.documentsComplete],['Within Limit', result.validations.withinLimit]].map(([k,v]) => (
                    <div key={k} className={`rounded-lg p-2.5 border text-center text-xs font-bold ${v?'bg-emerald-500/10 border-emerald-500/20 text-emerald-400':'bg-red-500/10 border-red-500/20 text-red-400'}`}>
                      {v?'✓':'✗'} {k}
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

const Btn = ({ icon, label, color, loading, onClick, disabled }) => {
  const MAP = { blue:'bg-blue-600 hover:bg-blue-500', emerald:'bg-emerald-600 hover:bg-emerald-500', red:'bg-red-600 hover:bg-red-500', amber:'bg-amber-600 hover:bg-amber-500', purple:'bg-purple-600 hover:bg-purple-500', violet:'bg-violet-600 hover:bg-violet-500' };
  return (
    <button onClick={onClick} disabled={loading || disabled}
      className={`flex items-center justify-center gap-1.5 px-3 py-1.5 ${MAP[color]} text-white text-xs font-semibold rounded-lg transition-all disabled:opacity-50 disabled:cursor-not-allowed min-w-[80px]`}>
      {loading ? <Loader2 size={13} className="animate-spin"/> : icon} {loading ? '…' : label}
    </button>
  );
};

export default function CarrierDashboard() {
  const [claims, setClaims] = useState([]);
  const [loading, setLoading] = useState(true);
  const [expanded, setExpanded] = useState(null);
  const [remarkId, setRemarkId] = useState(null);
  const [aiId, setAiId] = useState(null);
  const [actionLoading, setActionLoading] = useState(null);

  const fetch_ = useCallback(async () => {
    setLoading(true);
    const d = await api('/api/v1/carrier/claims');
    if (d.success) setClaims(d.data ?? []);
    else toast.error('Failed to load claims');
    setLoading(false);
  }, []);

  useEffect(() => { fetch_(); }, [fetch_]);

  const act = async (claimId, path, method = 'PATCH', body) => {
    setActionLoading(`${claimId}-${path}`);
    const d = await api(`/api/v1/carrier/claims/${claimId}/${path}`, method, body);
    if (d.success) { toast.success(d.message || 'Done'); fetch_(); }
    else toast.error(d.message || 'Action failed');
    setActionLoading(null);
  };

  const isAct = (id, p) => actionLoading === `${id}-${p}`;

  const stats = [
    { label: 'Assigned', value: claims.length, color: 'text-amber-400' },
    { label: 'Approved', value: claims.filter(c => c.status === 'APPROVED').length, color: 'text-emerald-400' },
    { label: 'Rejected', value: claims.filter(c => c.status === 'REJECTED').length, color: 'text-red-400' },
    { label: 'Pending',  value: claims.filter(c => c.status === 'PENDING' || c.status === 'REVIEW').length, color: 'text-blue-400' },
  ];

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="relative bg-gradient-to-r from-slate-900 to-slate-800 border border-amber-900/30 p-6 rounded-2xl shadow-xl overflow-hidden">
        <div className="absolute top-0 right-0 w-80 h-80 bg-amber-600/10 rounded-full blur-3xl -translate-y-1/2 translate-x-1/3 pointer-events-none"/>
        <div className="relative z-10 flex items-center justify-between flex-wrap gap-4">
          <div>
            <h1 className="text-2xl font-black text-white flex items-center gap-3">
              <span className="p-2 bg-amber-500/10 rounded-xl"><Truck className="text-amber-500" size={24}/></span>
              Carrier Portal
            </h1>
            <p className="text-slate-400 mt-1 text-sm">Validate, analyze, and manage your assigned insurance claims.</p>
          </div>
          <button onClick={fetch_} className="flex items-center gap-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-300 rounded-xl text-sm font-medium transition-colors">
            <RefreshCw size={14}/> Refresh
          </button>
        </div>
        <div className="relative z-10 mt-5 grid grid-cols-2 md:grid-cols-4 gap-3">
          {stats.map(s => (
            <div key={s.label} className="bg-slate-800/60 border border-slate-700/50 rounded-xl p-3">
              <p className="text-xs text-slate-500">{s.label}</p>
              <p className={`text-2xl font-black ${s.color}`}>{s.value}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-800 flex items-center justify-between">
          <h2 className="text-base font-bold text-white flex items-center gap-2"><Activity size={15} className="text-amber-400"/>Assigned Claims</h2>
          <span className="text-xs text-slate-500">{claims.length} claim{claims.length !== 1 ? 's' : ''}</span>
        </div>

        {loading ? (
          <div className="p-12 flex flex-col items-center gap-3 text-slate-500">
            <Loader2 size={32} className="animate-spin text-amber-500/50"/><p>Loading claims…</p>
          </div>
        ) : claims.length === 0 ? (
          <div className="p-12 flex flex-col items-center gap-3 text-slate-500">
            <FileSearch size={48} className="text-slate-700"/><p>No claims assigned to your carrier yet.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left text-slate-300">
              <thead className="text-[11px] uppercase text-slate-500 bg-slate-800/50 border-b border-slate-800">
                <tr>
                  {['','Claim','Policy No.','Patient / Hospital','Amount','Status','Risk','Actions'].map(h => (
                    <th key={h} className="px-4 py-3.5 font-semibold whitespace-nowrap">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/50">
                {claims.map(c => {
                  const open = expanded === c.claimId;
                  const isFinal = c.status === 'APPROVED' || c.status === 'REJECTED';
                  return (
                    <React.Fragment key={c.claimId}>
                      <tr className="hover:bg-slate-800/30 transition-colors">
                        <td className="px-3 py-3">
                          <button onClick={() => setExpanded(open ? null : c.claimId)}
                            className="p-1 rounded hover:bg-slate-700 text-slate-500 hover:text-amber-400 transition-colors"
                            title={open ? 'Hide details' : 'View patient details'}>
                            {open ? <ChevronUp size={14}/> : <ChevronDown size={14}/>}
                          </button>
                        </td>
                        <td className="px-4 py-3"><span className="font-mono text-xs text-slate-400">#{c.claimId}</span></td>
                        <td className="px-4 py-3 font-mono text-xs text-slate-200">{c.policyNumber}</td>
                        <td className="px-4 py-3">
                          <div className="text-xs font-semibold text-slate-200">{c.patient?.name || '—'}</div>
                          <div className="text-[11px] text-slate-500 mt-0.5">{c.hospitalName || '—'}</div>
                        </td>
                        <td className="px-4 py-3 font-mono text-xs text-emerald-400 font-bold">{money(c.amount)}</td>
                        <td className="px-4 py-3"><StatusBadge s={c.status}/></td>
                        <td className="px-4 py-3 w-28"><RiskBar score={c.fraud?.riskScore}/></td>
                        <td className="px-4 py-3">
                          <div className="flex flex-wrap items-center gap-1.5">
                            <Btn icon={<ShieldCheck size={13}/>} label="Validate" color="blue" loading={isAct(c.claimId,'validate')} onClick={() => act(c.claimId,'validate')}/>
                            {!isFinal && <Btn icon={<CheckCircle size={13}/>} label="Approve" color="emerald" loading={isAct(c.claimId,'approve')} onClick={() => act(c.claimId,'approve')}/>}
                            {!isFinal && <Btn icon={<XCircle size={13}/>} label="Reject" color="red" loading={isAct(c.claimId,'reject')} onClick={() => act(c.claimId,'reject')}/>}
                            <Btn icon={<Flag size={13}/>} label="Flag" color="amber" loading={isAct(c.claimId,'flag')} onClick={() => act(c.claimId,'flag')}/>
                            <Btn icon={<MessageSquare size={13}/>} label="Remark" color="purple" onClick={() => setRemarkId(c.claimId)}/>
                            <Btn icon={<Brain size={13}/>} label="AI" color="violet" onClick={() => setAiId(c.claimId)}/>
                          </div>
                        </td>
                      </tr>
                      {open && <PatientPanel c={c} onClose={() => setExpanded(null)}/>}
                    </React.Fragment>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {remarkId && <RemarkModal claimId={remarkId} onClose={() => setRemarkId(null)} onSuccess={fetch_}/>}
      {aiId     && <AiModal    claimId={aiId}    onClose={() => setAiId(null)}/>}
    </div>
  );
}
