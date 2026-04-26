import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { getClaimById, exportClaimReport, getClaimDocuments, getClaimAudits, analyzeClaimAI, generateClaimSummary } from '../api/claim.service';
import axiosInstance from '../api/axios';
import StatusBadge from '../components/StatusBadge';
import Loader from '../components/Loader';
import ErrorMessage from '../components/ErrorMessage';
import { Document, Page, pdfjs } from 'react-pdf';
import {
  ArrowLeft, Download, Bot, FileText, Calendar, DollarSign,
  Activity, User, Building2, Stethoscope, CheckCircle, Clock,
  XCircle, AlertTriangle, ShieldCheck, ShieldX, ShieldAlert,
  Send, History, Columns, Minimize2, ZoomIn, ZoomOut, RefreshCcw, Sparkles
} from 'lucide-react';
import 'react-pdf/dist/Page/AnnotationLayer.css';
import 'react-pdf/dist/Page/TextLayer.css';

// Set up react-pdf worker
pdfjs.GlobalWorkerOptions.workerSrc = new URL(
  'pdfjs-dist/build/pdf.worker.min.mjs',
  import.meta.url,
).toString();

/* ─── Status Timeline ─────────────────────────────────────── */
const STATUS_STEPS = ['PENDING', 'PROCESSING', 'REVIEW', 'APPROVED'];

const StatusTimeline = ({ currentStatus }) => {
  const isRejected = currentStatus === 'REJECTED';
  const currentIdx = isRejected ? STATUS_STEPS.length : STATUS_STEPS.indexOf(currentStatus);

  return (
    <div className="bg-slate-800 rounded-xl border border-slate-700 p-6 shadow-sm">
      <h2 className="text-xs font-bold text-slate-500 uppercase tracking-widest mb-6">Claim Progress</h2>
      <div className="relative flex items-start">
        <div className="absolute top-5 left-5 right-5 h-1 bg-slate-700 rounded-full" />
        <div
          className="absolute top-5 left-5 h-1 bg-blue-500 rounded-full transition-all duration-700"
          style={{ width: isRejected ? '0%' : `${Math.max(0, (currentIdx / (STATUS_STEPS.length - 1)) * 100)}%` }}
        />
        <div className="relative flex justify-between w-full z-10">
          {STATUS_STEPS.map((step, idx) => {
            const done   = !isRejected && idx < currentIdx;
            const active = !isRejected && idx === currentIdx;
            return (
              <div key={step} className="flex flex-col items-center gap-2.5 flex-1">
                <div className={`w-10 h-10 rounded-full flex items-center justify-center border-2 transition-all shadow-sm ${
                  done   ? 'bg-blue-600 border-blue-500 text-white shadow-blue-500/20' :
                  active ? 'bg-slate-800 border-blue-500 text-blue-400 shadow-blue-500/10' :
                           'bg-slate-800 border-slate-600 text-slate-500'
                }`}>
                  {done ? <CheckCircle className="w-5 h-5" /> : <Clock className="w-4 h-4" />}
                </div>
                <span className={`text-xs font-semibold text-center ${done || active ? 'text-blue-400' : 'text-slate-500'}`}>
                  {step.charAt(0) + step.slice(1).toLowerCase()}
                </span>
              </div>
            );
          })}
          <div className="flex flex-col items-center gap-2.5 flex-1">
            <div className={`w-10 h-10 rounded-full flex items-center justify-center border-2 transition-all shadow-sm ${
              isRejected ? 'bg-red-500 border-red-400 text-white shadow-red-500/20' :
              currentStatus === 'APPROVED' ? 'bg-emerald-500 border-emerald-400 text-white shadow-emerald-500/20' :
              'bg-slate-800 border-slate-600 text-slate-500'
            }`}>
              {isRejected ? <XCircle className="w-5 h-5" /> : currentStatus === 'APPROVED' ? <CheckCircle className="w-5 h-5" /> : <Clock className="w-4 h-4" />}
            </div>
            <span className={`text-xs font-semibold text-center ${
              isRejected ? 'text-red-400' : currentStatus === 'APPROVED' ? 'text-emerald-400' : 'text-slate-500'
            }`}>{isRejected ? 'Rejected' : 'Approved'}</span>
          </div>
        </div>
      </div>
    </div>
  );
};

/* ─── Info Row ────────────────────────────────────────────── */
const InfoRow = ({ icon: Icon, iconColor, label, value }) => (
  <div className="flex items-start gap-3">
    <div className="p-2 rounded-lg bg-slate-900/50 border border-slate-700/50">
      <Icon className={`w-4 h-4 ${iconColor}`} />
    </div>
    <div>
      <p className="text-xs text-slate-400 font-medium">{label}</p>
      <p className="font-semibold text-slate-200 text-sm mt-0.5">{value || '—'}</p>
    </div>
  </div>
);

/* ─── AI Chat Panel ───────────────────────────────────────── */
const AiChatPanel = ({ claimId }) => {
  const [messages, setMessages] = useState([
    { role: 'assistant', content: 'Hi, I am your AI Claim Assistant. How can I help you analyze this claim?' }
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef(null);

  const scrollToBottom = () => messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  useEffect(scrollToBottom, [messages]);

  const handleSend = async () => {
    if (!input.trim()) return;
    const userMsg = { role: 'user', content: input };
    setMessages(prev => [...prev, userMsg]);
    setInput('');
    setLoading(true);

    try {
      // Using generic analyze or chat endpoint
      const response = await analyzeClaimAI(claimId, userMsg.content);
      let aiContent = "";
      if (response && response.recommendation) {
         aiContent = response.recommendation;
      } else {
         aiContent = JSON.stringify(response);
      }
      setMessages(prev => [...prev, { role: 'assistant', content: aiContent }]);
    } catch (err) {
      setMessages(prev => [...prev, { role: 'assistant', content: 'Sorry, I encountered an error analysing the claim.' }]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-slate-800 rounded-xl border border-slate-700 shadow-sm flex flex-col h-[600px] sticky top-6">
      <div className="flex items-center gap-3 p-4 border-b border-slate-700">
        <div className="bg-blue-500/10 border border-blue-500/20 p-2 rounded-xl shadow-inner">
          <Bot className="w-5 h-5 text-blue-400" />
        </div>
        <div>
          <h3 className="font-bold text-slate-200 text-base">AI Assistant</h3>
          <p className="text-[10px] text-blue-400 font-medium">Powered by Gemini</p>
        </div>
      </div>
      
      <div className="flex-1 overflow-y-auto p-4 space-y-4 scrollbar-thin scrollbar-thumb-slate-600">
        {messages.map((msg, idx) => (
          <div key={idx} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
            <div className={`max-w-[85%] rounded-2xl p-3 text-sm ${
              msg.role === 'user' 
                ? 'bg-blue-600 text-white rounded-br-none' 
                : 'bg-slate-700 text-slate-200 rounded-bl-none border border-slate-600'
            }`}>
              {msg.content}
            </div>
          </div>
        ))}
        {loading && (
          <div className="flex justify-start">
            <div className="bg-slate-700 text-slate-200 rounded-2xl rounded-bl-none border border-slate-600 p-3 flex gap-1">
              <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce"></span>
              <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce delay-75"></span>
              <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce delay-150"></span>
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      <div className="p-4 border-t border-slate-700 bg-slate-900/50 rounded-b-xl">
        <div className="flex gap-2">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSend()}
            placeholder="Ask about this claim..."
            className="flex-1 bg-slate-950 border border-slate-700 rounded-xl px-4 py-2 text-sm text-slate-200 focus:outline-none focus:border-blue-500"
          />
          <button 
            onClick={handleSend}
            disabled={loading || !input.trim()}
            className="p-2 bg-blue-600 hover:bg-blue-700 rounded-xl text-white disabled:opacity-50 transition-colors"
          >
            <Send className="w-5 h-5" />
          </button>
        </div>
      </div>
    </div>
  );
};

/* ─── Smart Summary Card ──────────────────────────────────── */
const SmartSummaryCard = ({ claim, onRegenerate }) => {
  const [loading, setLoading] = useState(false);

  const handleRegenerate = async () => {
    setLoading(true);
    try {
      await onRegenerate();
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-gradient-to-br from-slate-900 to-slate-800 rounded-xl shadow-lg border border-slate-700/50 p-6 relative overflow-hidden">
      {/* Background glow */}
      <div className="absolute -top-10 -right-10 w-32 h-32 bg-blue-500/10 rounded-full blur-3xl pointer-events-none"></div>
      
      <div className="flex justify-between items-start mb-4 relative z-10">
        <h2 className="text-base font-black text-slate-100 flex items-center gap-2">
          <Sparkles className="w-5 h-5 text-blue-400" /> AI Claim Summary
        </h2>
        <button 
          onClick={handleRegenerate}
          disabled={loading}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-blue-500/10 hover:bg-blue-500/20 text-blue-400 text-xs font-bold transition-all disabled:opacity-50 border border-blue-500/20"
        >
          <RefreshCcw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          {loading ? 'Generating...' : 'Regenerate'}
        </button>
      </div>
      
      <div className="bg-slate-950/40 p-4 rounded-xl border border-slate-700/50">
        {claim.aiSummary ? (
          <p className="text-sm text-slate-300 leading-relaxed font-medium">
            {claim.aiSummary.split('\n').map((line, i) => (
              <React.Fragment key={i}>
                {line}<br/>
              </React.Fragment>
            ))}
          </p>
        ) : (
          <p className="text-sm text-slate-500 italic flex items-center gap-2">
            <Bot className="w-4 h-4" /> No summary generated yet. Click regenerate to create one.
          </p>
        )}
      </div>
    </div>
  );
};

/* ─── Health Score Meter ──────────────────────────────────── */
const HealthScoreMeter = ({ score, riskLevel }) => {
  const isGood = score >= 80;
  const isWarn = score >= 50 && score < 80;
  
  const color = isGood ? '#10B981' : isWarn ? '#F59E0B' : '#EF4444';
  const bgClass = isGood ? 'bg-emerald-500/10 border-emerald-500/20' : isWarn ? 'bg-amber-500/10 border-amber-500/20' : 'bg-red-500/10 border-red-500/20';
  const textClass = isGood ? 'text-emerald-400' : isWarn ? 'text-amber-400' : 'text-red-400';
  const label = isGood ? 'CLEAN' : isWarn ? 'WARNING' : 'HIGH RISK';

  // Calculate SVG circle properties
  const radius = 36;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference - ((score || 0) / 100) * circumference;

  return (
    <div className={`p-6 rounded-xl border flex items-center justify-between shadow-sm ${bgClass}`}>
      <div>
        <h2 className="text-sm font-bold text-slate-300 mb-1 flex items-center gap-2">
          <Activity className={`w-4 h-4 ${textClass}`} /> Claim Health Score
        </h2>
        <p className="text-xs text-slate-400 max-w-[200px]">
          Based on document accuracy, fraud signals, and validation checks.
        </p>
      </div>
      
      <div className="flex items-center gap-6">
        <div className="text-right">
          <p className="text-3xl font-black text-slate-100">{score || 0}<span className="text-lg text-slate-500">/100</span></p>
          <p className={`text-[10px] font-bold uppercase tracking-wider ${textClass}`}>{label}</p>
        </div>
        
        <div className="relative w-24 h-24 flex items-center justify-center">
          {/* Background Circle */}
          <svg className="w-full h-full transform -rotate-90">
            <circle
              cx="48" cy="48" r={radius}
              stroke="currentColor" strokeWidth="8" fill="transparent"
              className="text-slate-800"
            />
            {/* Progress Circle */}
            <circle
              cx="48" cy="48" r={radius}
              stroke={color} strokeWidth="8" fill="transparent"
              strokeDasharray={circumference}
              strokeDashoffset={strokeDashoffset}
              strokeLinecap="round"
              className="transition-all duration-1000 ease-out"
            />
          </svg>
          <div className="absolute flex items-center justify-center">
            <ShieldCheck className={`w-8 h-8 ${textClass}`} />
          </div>
        </div>
      </div>
    </div>
  );
};

/* ─── Document Viewer ─────────────────────────────────────── */
const DocumentViewer = ({ claimId }) => {
  const [docs, setDocs] = useState([]);
  const [activeDocUrl, setActiveDocUrl] = useState(null);
  const [numPages, setNumPages] = useState(null);
  const [pageNumber, setPageNumber] = useState(1);
  const [scale, setScale] = useState(1.0);
  const [isFullscreen, setIsFullscreen] = useState(false);

  useEffect(() => {
    getClaimDocuments(claimId).then(data => setDocs(data || [])).catch(console.error);
  }, [claimId]);

  const loadDocument = async (docId) => {
    try {
      const response = await axiosInstance.get(`/files/download/${docId}`, { responseType: 'blob' });
      const blobUrl = URL.createObjectURL(response.data);
      setActiveDocUrl(blobUrl);
      setPageNumber(1);
    } catch (e) {
      toast.error("Failed to load document");
    }
  };

  if (docs.length === 0) return null;

  return (
    <div className={`bg-slate-800 rounded-xl shadow-sm border border-slate-700 p-6 ${isFullscreen ? 'fixed inset-4 z-50 overflow-auto' : ''}`}>
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
          <FileText className="w-5 h-5 text-indigo-400" /> Attached Documents
        </h2>
        {activeDocUrl && (
          <div className="flex gap-2">
            <button onClick={() => setScale(s => Math.max(0.5, s - 0.2))} className="p-1.5 bg-slate-700 rounded text-slate-300"><ZoomOut className="w-4 h-4" /></button>
            <button onClick={() => setScale(s => Math.min(2.0, s + 0.2))} className="p-1.5 bg-slate-700 rounded text-slate-300"><ZoomIn className="w-4 h-4" /></button>
            <button onClick={() => setIsFullscreen(!isFullscreen)} className="p-1.5 bg-slate-700 rounded text-slate-300"><Columns className="w-4 h-4" /></button>
          </div>
        )}
      </div>

      {!activeDocUrl && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
          {docs.map(doc => {
            const isInvalid = doc.validationStatus === 'INVALID';
            const issues = doc.validationIssues ? JSON.parse(doc.validationIssues) : [];
            const lowConfidence = doc.confidenceScore < 70;
            return (
              <div key={doc.id} className="bg-slate-900 border border-slate-700 rounded-xl p-4 flex flex-col gap-3">
                <div className="flex justify-between items-start">
                  <span className="text-slate-200 font-bold text-sm">{doc.type.replace('_', ' ')}</span>
                  {doc.validationStatus && (
                    <span className={`flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[10px] font-bold uppercase tracking-wider border ${isInvalid ? 'bg-red-500/10 text-red-400 border-red-500/30' : (lowConfidence ? 'bg-amber-500/10 text-amber-400 border-amber-500/30' : 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30')}`}>
                      {isInvalid ? <XCircle className="w-3.5 h-3.5" /> : <CheckCircle className="w-3.5 h-3.5" />}
                      AI: {doc.validationStatus}
                    </span>
                  )}
                </div>
                
                {doc.confidenceScore != null && (
                  <div className="mb-2 bg-slate-950/50 rounded-lg p-3 border border-slate-700/50">
                    <div className="flex justify-between items-center mb-1.5">
                      <span className="text-xs text-slate-400 font-medium">AI Confidence Level</span>
                      <span className={`text-sm font-bold ${lowConfidence ? 'text-amber-400' : 'text-slate-200'}`}>{doc.confidenceScore}%</span>
                    </div>
                    <div className="w-full bg-slate-800 rounded-full h-1.5 overflow-hidden">
                      <div className={`h-1.5 rounded-full ${lowConfidence ? 'bg-amber-400' : 'bg-emerald-400'}`} style={{ width: `${doc.confidenceScore}%` }}></div>
                    </div>
                  </div>
                )}

                {issues.length > 0 && (
                  <div className="space-y-2 mt-2">
                    <p className="text-xs font-bold text-slate-300 flex items-center gap-1.5">
                      <AlertTriangle className="w-3.5 h-3.5 text-amber-400" /> Validation Issues:
                    </p>
                    <ul className="space-y-1.5 ml-1">
                      {issues.filter(iss => !iss.includes('java.') && !iss.includes('Exception')).map((iss, i) => (
                        <li key={i} className="text-xs text-red-300 flex items-start gap-2 bg-red-500/5 p-2 rounded-md border border-red-500/10">
                          <span className="text-red-400 mt-0.5 font-black">•</span> 
                          <span className="leading-relaxed">{iss}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
                
                <button onClick={() => loadDocument(doc.id)} className="mt-auto px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded-lg text-sm font-bold text-white transition-colors">
                  View Document
                </button>
              </div>
            );
          })}
        </div>
      )}

      {activeDocUrl && (
        <div className="bg-slate-900 border border-slate-700 rounded-xl overflow-hidden flex flex-col items-center p-4 min-h-[500px]">
          <div className="flex gap-2 mb-4 w-full justify-center">
            {docs.map(doc => (
              <button key={doc.id} onClick={() => loadDocument(doc.id)} className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 border border-slate-600 rounded text-xs text-slate-300">
                {doc.type}
              </button>
            ))}
            <button onClick={() => setActiveDocUrl(null)} className="px-3 py-1.5 bg-red-500/20 hover:bg-red-500/30 text-red-400 rounded text-xs ml-auto">
              Close Viewer
            </button>
          </div>
          <div className="overflow-auto border border-slate-700 rounded shadow-2xl max-h-[800px] w-full flex justify-center bg-slate-800">
            <Document file={activeDocUrl} onLoadSuccess={({ numPages }) => setNumPages(numPages)} loading={<Loader message="Loading PDF..." />}>
              <Page pageNumber={pageNumber} scale={scale} renderTextLayer={false} renderAnnotationLayer={false} />
            </Document>
          </div>
          {numPages > 1 && (
            <div className="flex gap-4 mt-4 items-center">
              <button disabled={pageNumber <= 1} onClick={() => setPageNumber(p => p - 1)} className="px-3 py-1 bg-slate-700 rounded disabled:opacity-50 text-white text-sm">Prev</button>
              <span className="text-slate-400 text-sm">Page {pageNumber} of {numPages}</span>
              <button disabled={pageNumber >= numPages} onClick={() => setPageNumber(p => p + 1)} className="px-3 py-1 bg-slate-700 rounded disabled:opacity-50 text-white text-sm">Next</button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

/* ─── Audit Timeline ──────────────────────────────────────── */
const AuditTimeline = ({ claimId }) => {
  const [audits, setAudits] = useState([]);
  useEffect(() => {
    getClaimAudits(claimId).then(data => setAudits(data || [])).catch(console.error);
  }, [claimId]);

  if (audits.length === 0) return null;

  return (
    <div className="bg-slate-800 rounded-xl shadow-sm border border-slate-700 p-6">
      <h2 className="text-base font-bold text-slate-100 mb-6 flex items-center gap-2">
        <History className="w-5 h-5 text-purple-400" /> Audit Trail
      </h2>
      <div className="space-y-4 pl-4 border-l-2 border-slate-700">
        {audits.map(audit => (
          <div key={audit.id} className="relative pl-4">
            <div className="absolute -left-[21px] top-1 w-3 h-3 bg-purple-500 rounded-full border-2 border-slate-800"></div>
            <p className="text-xs text-slate-400 font-semibold uppercase tracking-wider">{new Date(audit.changedAt).toLocaleString()}</p>
            <p className="text-sm text-slate-200 mt-0.5">
              <span className="font-bold text-blue-400">{audit.changedBy}</span> changed status from <span className="font-medium bg-slate-700 px-1.5 py-0.5 rounded text-[10px]">{audit.previousStatus || 'NONE'}</span> to <span className="font-medium bg-blue-500/20 text-blue-400 px-1.5 py-0.5 rounded text-[10px]">{audit.newStatus}</span>
            </p>
            {audit.notes && <p className="text-xs text-slate-500 italic mt-1">"{audit.notes}"</p>}
          </div>
        ))}
      </div>
    </div>
  );
};

/* ─── Main ClaimDetail Page ───────────────────────────────── */
const ClaimDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [claim, setClaim]           = useState(null);
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState(null);
  const [downloading, setDownloading] = useState(false);

  const fetchClaimDetails = async () => {
    try {
      setLoading(true); setError(null);
      const data = await getClaimById(id);
      setClaim(data);
    } catch { setError('Failed to fetch claim details.'); }
    finally { setLoading(false); }
  };

  const handleRegenerateSummary = async () => {
    try {
      const res = await generateClaimSummary(id);
      setClaim(prev => ({ ...prev, aiSummary: res.summary }));
      toast.success('AI summary generated');
    } catch (err) {
      toast.error('Failed to generate summary');
    }
  };

  useEffect(() => { if (id) fetchClaimDetails(); }, [id]);

  const handleDownloadPDF = async () => {
    try {
      setDownloading(true);
      toast.loading('Generating PDF…', { id: 'pdf' });
      const blob = await exportClaimReport(id);
      const url = window.URL.createObjectURL(new Blob([blob]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `claim-report-${id}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      toast.success('PDF downloaded', { id: 'pdf' });
    } catch { toast.error('Failed to download PDF', { id: 'pdf' }); }
    finally { setDownloading(false); }
  };

  if (loading) return <Loader fullScreen message="Loading claim details…" />;
  if (error)   return <ErrorMessage message={error} onRetry={fetchClaimDetails} />;
  if (!claim)  return <ErrorMessage message="Claim not found" />;

  const fmt = (date) => date ? new Date(date).toLocaleDateString('en-US', { day: 'numeric', month: 'short', year: 'numeric' }) : '—';

  return (
    <div className="space-y-6 max-w-[1400px] mx-auto pb-10">
      {/* ── Header ── */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <button onClick={() => navigate(-1)} className="p-2.5 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-xl transition-colors text-slate-300 shadow-sm">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-slate-100 tracking-tight flex items-center gap-3">
              Claim #{claim.id}
              <StatusBadge status={claim.status} />
              {claim.riskScore > 0.6 && <span className="bg-red-500/20 text-red-400 border border-red-500/30 px-2 py-0.5 rounded text-[10px] font-bold uppercase animate-pulse flex items-center gap-1"><ShieldAlert className="w-3 h-3"/> High Risk</span>}
            </h1>
            <p className="text-sm text-slate-400 mt-1 font-medium">Submitted on {fmt(claim.createdDate)}</p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <button
            id="export-pdf"
            onClick={handleDownloadPDF}
            disabled={downloading}
            className="flex items-center gap-2 px-4 py-2.5 bg-slate-800 border border-slate-700 rounded-xl text-sm font-medium text-slate-200 hover:bg-slate-700 transition-colors shadow-sm disabled:opacity-50"
          >
            <Download className="w-4 h-4 text-blue-400" />
            {downloading ? 'Generating…' : 'Export PDF'}
          </button>
        </div>
      </div>

      {/* ── Status Timeline ── */}
      <StatusTimeline currentStatus={claim.status} />

      {/* ── Main Grid ── */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left — Details */}
        <div className="lg:col-span-2 space-y-6">
          
          {claim.healthScore != null && (
             <HealthScoreMeter score={claim.healthScore} riskLevel={claim.riskLevel} />
          )}

          <SmartSummaryCard claim={claim} onRegenerate={handleRegenerateSummary} />

          {/* Claim Info */}
          <div className="bg-slate-800 rounded-xl shadow-sm border border-slate-700 p-6">
            <h2 className="text-base font-bold text-slate-100 mb-6 flex items-center gap-2">
              <FileText className="w-5 h-5 text-blue-400" /> Claim Overview
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
              <InfoRow icon={FileText}    iconColor="text-blue-400" label="Policy Number"  value={claim.policyNumber} />
              <InfoRow icon={DollarSign} iconColor="text-emerald-400"  label="Claimed Amount" value={claim.amount != null ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(claim.amount) : null} />
              <InfoRow icon={DollarSign} iconColor="text-cyan-400"   label="Total Bill"     value={claim.totalBillAmount != null ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(claim.totalBillAmount) : null} />
              <InfoRow icon={Activity}    iconColor="text-purple-400" label="Claim Type"     value={claim.claimType} />
              {claim.processedDate && <InfoRow icon={Calendar} iconColor="text-amber-400" label="Processed Date" value={fmt(claim.processedDate)} />}
              {claim.reviewedBy && <InfoRow icon={User} iconColor="text-slate-400" label="Reviewed By" value={claim.reviewedBy} />}
            </div>
          </div>

          {/* Patient & Hospital */}
          <div className="bg-slate-800 rounded-xl shadow-sm border border-slate-700 p-6">
            <h2 className="text-base font-bold text-slate-100 mb-6 flex items-center gap-2">
              <User className="w-5 h-5 text-indigo-400" /> Patient & Hospital Details
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
              <InfoRow icon={User}         iconColor="text-indigo-400"    label="Patient Name"    value={claim.patientName} />
              <InfoRow icon={Building2}    iconColor="text-teal-400"    label="Hospital"        value={claim.hospitalName} />
              <InfoRow icon={Calendar}     iconColor="text-blue-400"  label="Admission Date"  value={fmt(claim.admissionDate)} />
              <InfoRow icon={Calendar}     iconColor="text-amber-400"  label="Discharge Date"  value={fmt(claim.dischargeDate)} />
              <InfoRow icon={Stethoscope}  iconColor="text-pink-400"    label="Diagnosis"       value={claim.diagnosis} />
            </div>
          </div>
          
          {/* Document Viewer */}
          <DocumentViewer claimId={claim.id} />
          
          {/* Audit Timeline */}
          <AuditTimeline claimId={claim.id} />

        </div>

        {/* Right — AI Panel */}
        <div>
          <AiChatPanel claimId={claim.id} />
        </div>
      </div>
    </div>
  );
};

export default ClaimDetail;
