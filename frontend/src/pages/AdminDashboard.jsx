import React, { useState, useEffect, useCallback } from 'react';
import toast from 'react-hot-toast';
import { useAuth } from '../context/AuthContext';
import axiosInstance from '../api/axios';
import { getAdminClaims, approveClaim, rejectClaim, getClaimAiSummary, getUsers, blockUser, unblockUser, getSystemMonitoring, getCarriers, approveCarrier, rejectCarrier, assignCarrierToClaim } from '../api/admin.service';
import StatusBadge from '../components/StatusBadge';
import Loader from '../components/Loader';
import {
  CheckCircle, XCircle, Clock, FileText, Search, Filter,
  ChevronLeft, ChevronRight, Bot, X, ShieldCheck, ShieldX,
  ShieldAlert, AlertTriangle, RefreshCw, Eye, Users, Shield,
  Lock, Unlock, Activity, Terminal, Truck, Brain, Loader2, Bell,
  CreditCard
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';

/* ─── Reason Modal ────────────────────────────────────────── */
const ActionModal = ({ isOpen, title, content, placeholder, onConfirm, onCancel, loading, actionType }) => {
  const [reason, setReason] = useState('');
  if (!isOpen) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/80 backdrop-blur-sm">
      <div className="bg-slate-800 border border-slate-700 rounded-2xl shadow-2xl w-full max-w-md p-6 animate-[cardIn_0.2s_ease-out]">
        <h3 className="text-lg font-bold text-slate-200 mb-2">{title}</h3>
        {content && <p className="text-sm text-slate-400 mb-4">{content}</p>}
        {placeholder && (
          <textarea
            id="modal-reason"
            value={reason}
            onChange={e => setReason(e.target.value)}
            placeholder={placeholder}
            rows={4}
            className="w-full bg-slate-900 border border-slate-700 rounded-xl p-3 text-sm text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none placeholder:text-slate-500"
          />
        )}
        <div className="flex gap-3 mt-4 justify-end">
          <button onClick={onCancel} className="px-4 py-2 rounded-lg border border-slate-700 text-slate-300 hover:bg-slate-700 text-sm font-medium transition-colors">
            Cancel
          </button>
          <button
            onClick={() => { onConfirm(reason); setReason(''); }}
            disabled={(placeholder && !reason.trim()) || loading}
            className={`px-4 py-2 rounded-lg text-white text-sm font-medium disabled:opacity-50 transition-colors flex items-center justify-center min-w-[100px] ${
              actionType === 'danger' ? 'bg-red-600 hover:bg-red-700' :
              actionType === 'success' ? 'bg-emerald-600 hover:bg-emerald-700' :
              'bg-blue-600 hover:bg-blue-700'
            }`}
          >
            {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Confirm'}
          </button>
        </div>
      </div>
    </div>
  );
};

/* ─── AI Summary Drawer ───────────────────────────────────── */
const AiDrawer = ({ claim, result, loading, onClose }) => {
  const verdictColor = {
    APPROVED: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20',
    REJECTED: 'text-red-400 bg-red-500/10 border-red-500/20',
    REVIEW:   'text-amber-400 bg-amber-500/10 border-amber-500/20',
  };
  const vc = verdictColor[result?.verdict] || verdictColor.REVIEW;

  return (
    <div className="fixed inset-0 z-40 flex">
      <div className="flex-1 bg-slate-900/60 backdrop-blur-sm" onClick={onClose} />
      <div className="w-full max-w-sm bg-slate-800 border-l border-slate-700 shadow-2xl flex flex-col animate-[slideIn_0.3s_ease-out]">
        <div className="flex items-center justify-between p-5 border-b border-slate-700 bg-slate-800/50">
          <div className="flex items-center gap-3">
            <div className="bg-blue-500/10 border border-blue-500/20 p-2 rounded-lg">
              <Bot className="w-5 h-5 text-blue-400" />
            </div>
            <div>
              <p className="text-xs text-blue-400 font-medium">AI Claim Analysis</p>
              <p className="font-bold text-slate-200 text-sm">Claim #{claim?.id}</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1 hover:bg-slate-700 rounded-lg transition-colors">
            <X className="w-5 h-5 text-slate-400" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-5 scrollbar-thin scrollbar-thumb-slate-700">
          {loading ? (
            <div className="flex flex-col items-center justify-center h-48 gap-3">
              <div className="w-10 h-10 border-4 border-slate-700 border-t-blue-500 rounded-full animate-spin" />
              <p className="text-sm text-blue-400 font-medium">Running AI analysis…</p>
            </div>
          ) : result ? (
            <div className="space-y-4">
              <div className={`flex items-center gap-3 p-3 rounded-xl border ${vc}`}>
                {result.verdict === 'APPROVED' ? <ShieldCheck className="w-5 h-5" /> :
                 result.verdict === 'REJECTED' ? <ShieldX className="w-5 h-5" /> :
                 <ShieldAlert className="w-5 h-5" />}
                <div>
                  <p className="text-xs opacity-70">AI Verdict</p>
                  <p className="font-bold">{result.verdict}</p>
                </div>
                <span className="ml-auto text-sm font-bold">
                  {Math.round((result.confidence ?? 0) * 100)}% conf.
                </span>
              </div>
              <div className="bg-slate-900/50 border border-slate-700/50 rounded-xl p-4">
                <div className="flex justify-between text-xs text-slate-400 mb-2">
                  <span>Risk Score</span>
                  <span className="font-bold text-slate-200">{Math.round((result.riskScore ?? 0) * 100)}/100</span>
                </div>
                <div className="w-full bg-slate-700 rounded-full h-2">
                  <div
                    className={`h-2 rounded-full transition-all duration-700 ${
                      result.riskScore < 0.3 ? 'bg-emerald-500' :
                      result.riskScore < 0.6 ? 'bg-amber-500' : 'bg-red-500'
                    }`}
                    style={{ width: `${Math.round((result.riskScore ?? 0) * 100)}%` }}
                  />
                </div>
              </div>
              {result.flags?.length > 0 && (
                <div className="bg-amber-500/10 border border-amber-500/20 rounded-xl p-4">
                  <div className="flex items-center gap-2 mb-2">
                    <AlertTriangle className="w-4 h-4 text-amber-500" />
                    <p className="text-xs font-semibold text-amber-400">Flags Detected</p>
                  </div>
                  <ul className="space-y-1">
                    {result.flags.map((f, i) => (
                      <li key={i} className="text-amber-200/80 text-xs flex gap-1.5">
                        <span className="text-amber-500 mt-0.5">•</span>{f}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
              {result.recommendation && (
                <div className="bg-blue-500/10 border border-blue-500/20 rounded-xl p-4">
                  <p className="text-xs font-semibold text-blue-400 mb-1">Recommendation</p>
                  <p className="text-blue-200 text-sm">{result.recommendation}</p>
                </div>
              )}
            </div>
          ) : (
            <p className="text-slate-500 text-sm text-center mt-12">No analysis data</p>
          )}
        </div>
      </div>
    </div>
  );
};

/* ─── Main Admin Dashboard ────────────────────────────────── */
const AdminDashboard = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const userRoleStr = (user?.userRole || user?.role || '').toUpperCase();
  const isAdmin = userRoleStr.includes('ADMIN');

  console.log('AdminDashboard Render -> user:', user, 'isAdmin:', isAdmin, 'userRoleStr:', userRoleStr);

  const [activeTab, setActiveTab] = useState('CLAIMS'); // 'CLAIMS' | 'USERS' | 'MONITORING'

  // Claims State
  const [claims, setClaims]           = useState([]);
  const [cLoading, setCLoading]       = useState(true);
  const [cError, setCError]           = useState(null);
  const [cPage, setCPage]             = useState(0);
  const [cTotalPages, setCTotalPages] = useState(1);
  const [cTotal, setCTotal]           = useState(0);
  const [statusFilter, setStatusFilter] = useState('');
  const [cSearch, setCSearch]         = useState('');

  // Users State
  const [users, setUsers]             = useState([]);
  const [uLoading, setULoading]       = useState(false); // lazy: only load when tab is active
  const [uError, setUError]           = useState(null);
  const [uPage, setUPage]             = useState(0);
  const [uTotalPages, setUTotalPages] = useState(1);
  const [uTotal, setUTotal]           = useState(0);
  const [uSearch, setUSearch]         = useState('');

  // Monitoring State
  const [monitoring, setMonitoring]   = useState(null);
  const [mLoading, setMLoading]       = useState(false); // lazy: only load when tab is active
  const [mError, setMError]           = useState(null);

  // Action states
  const [actionLoading, setActionLoading] = useState(false);
  const [approveModal, setApproveModal]   = useState(null); // claimId
  const [rejectModal, setRejectModal]     = useState(null); // claimId
  const [blockModal, setBlockModal]       = useState(null); // userId
  const [unblockModal, setUnblockModal]   = useState(null); // userId

  // AI Drawer
  const [aiClaim, setAiClaim]   = useState(null);
  const [aiResult, setAiResult] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);

  // Carriers
  const [carriers, setCarriers]     = useState([]);
  const [carrierLoading, setCarrierLoading] = useState(false);
  const [carrierStatusFilter, setCarrierStatusFilter] = useState('');
  const [carrierActionLoading, setCarrierActionLoading] = useState(null);

  // Notifications
  const [notifications, setNotifications]   = useState([]);
  const [notifOpen, setNotifOpen]           = useState(false);
  const [notifLoading, setNotifLoading]     = useState(false);
  const unreadCount = notifications.filter(n => !n.read).length;

  // Carrier Approved Notification
  const [carrierApprovedCount, setCarrierApprovedCount] = useState(0);
  const [showCarrierPopup, setShowCarrierPopup] = useState(false);

  useEffect(() => {
    if (isAdmin) {
      getAdminClaims({ status: 'CARRIER_APPROVED' })
        .then(data => {
          if (data && data.totalElements > 0) {
            setCarrierApprovedCount(data.totalElements);
            setShowCarrierPopup(true);
          }
        })
        .catch(err => console.error('Failed to fetch carrier approved claims', err));
    }
  }, [isAdmin]);

  const fetchNotifications = useCallback(async () => {
    try {
      const res = await axiosInstance.get('/notifications');
      const data = res.data;
      setNotifications(prev => {
        const prevUnread = prev.filter(n => !n.read).length;
        const newUnread  = (data || []).filter(n => !n.read).length;
        if (newUnread > prevUnread) {
          toast(`🔔 New notification: ${(data || []).find(n => !n.read)?.title || 'Claim update'}`,
            { icon: '🔔', style: { background: '#1e293b', color: '#f1f5f9', border: '1px solid #334155' } });
        }
        return data || [];
      });
    } catch (_) {}
  }, []);

  // Poll every 10 seconds for new notifications
  useEffect(() => {
    fetchNotifications();
    const id = setInterval(fetchNotifications, 10000);
    return () => clearInterval(id);
  }, [fetchNotifications]);

  const markAllRead = async () => {
    await axiosInstance.post('/notifications/mark-read');
    setNotifications(prev => prev.map(n => ({ ...n, read: true })));
  };

  const markOneRead = async (id) => {
    await axiosInstance.patch(`/notifications/${id}/read`);
    setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true } : n));
  };

  // Assign carrier to claim
  const [assignModal, setAssignModal]   = useState(null); // claimId
  const [assignCarrierId, setAssignCarrierId] = useState('');

  const fetchClaims = useCallback(async () => {
    try {
      setCLoading(true);
      setCError(null);
      // Use dedicated admin endpoint: GET /api/v1/admin/claims
      const params = { page: cPage, size: 10 };
      if (statusFilter) params.status = statusFilter;
      const data = await getAdminClaims(params);
      setClaims(data?.content || []);
      setCTotalPages(data?.totalPages || 1);
      setCTotal(data?.totalElements || 0);
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message || 'Failed to load claims';
      setCError(msg);
    } finally {
      setCLoading(false);
    }
  }, [cPage, statusFilter]);

  const fetchUsers = useCallback(async () => {
    try {
      setULoading(true);
      setUError(null);
      const data = await getUsers({ page: uPage, size: 10, search: uSearch || undefined });
      setUsers(data?.content || []);
      setUTotalPages(data?.totalPages || 1);
      setUTotal(data?.totalElements || 0);
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message || 'Failed to load users';
      setUError(msg);
    } finally {
      setULoading(false);
    }
  }, [uPage, uSearch]);

  const fetchMonitoring = useCallback(async () => {
    try {
      setMLoading(true);
      setMError(null);
      const data = await getSystemMonitoring();
      setMonitoring(data);
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message || 'Failed to load monitoring data';
      setMError(msg);
    } finally {
      setMLoading(false);
    }
  }, []);

  // Only load the active tab's data — avoid redundant API calls on mount
  useEffect(() => {
    if (activeTab === 'CLAIMS') fetchClaims();
    else if (activeTab === 'USERS') fetchUsers();
    else if (activeTab === 'MONITORING') fetchMonitoring();
    // CARRIERS is loaded via its own tab button click
  }, [activeTab, fetchClaims, fetchUsers, fetchMonitoring]);


  const handleApprove = async (reason) => {
    setActionLoading(true);
    try {
      await approveClaim(approveModal, reason);
      toast.success(`Claim #${approveModal} approved successfully`);
      setApproveModal(null);
      await fetchClaims();   // force refetch
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message || 'Failed to approve claim';
      toast.error(msg);
    }
    finally { setActionLoading(false); }
  };

  const handleReject = async (reason) => {
    setActionLoading(true);
    try {
      await rejectClaim(rejectModal, reason);
      toast.success(`Claim #${rejectModal} rejected`);
      setRejectModal(null);
      await fetchClaims();   // force refetch
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message || 'Failed to reject claim';
      toast.error(msg);
    }
    finally { setActionLoading(false); }
  };

  const handleBlock = async () => {
    setActionLoading(true);
    try {
      await blockUser(blockModal);
      toast.success(`User blocked successfully`);
      setBlockModal(null);
      fetchUsers();
    } catch { toast.error('Failed to block user'); }
    finally { setActionLoading(false); }
  };

  const handleUnblock = async () => {
    setActionLoading(true);
    try {
      await unblockUser(unblockModal);
      toast.success(`User unblocked successfully`);
      setUnblockModal(null);
      fetchUsers();
    } catch { toast.error('Failed to unblock user'); }
    finally { setActionLoading(false); }
  };

  const handleAiSummary = async (claim) => {
    setAiClaim(claim);
    setAiResult(null);
    setAiLoading(true);
    try {
      const result = await getClaimAiSummary(claim.id);
      // Unwrap ApiResponse wrapper: result = { success, message, data, httpStatus }
      setAiResult(result?.data ?? result);
    } catch (err) {
      const msg = err?.response?.data?.message || 'AI analysis failed';
      toast.error(msg);
    }
    finally { setAiLoading(false); }
  };

  const STATUS_OPTS = ['', 'SUBMITTED', 'AI_VALIDATED', 'UNDER_REVIEW', 'ADMIN_APPROVED', 'CARRIER_APPROVED', 'REJECTED', 'PAYMENT_PENDING', 'SETTLED'];

  const fetchCarriers = useCallback(async () => {
    setCarrierLoading(true);
    try {
      const result = await getCarriers();
      // Unwrap if wrapped: { success, data: [...] } or plain array
      const list = Array.isArray(result) ? result : (result?.data ?? result?.content ?? []);
      setCarriers(Array.isArray(list) ? list : []);
    } catch { toast.error('Failed to load carriers'); }
    finally { setCarrierLoading(false); }
  }, []);

  // Load carriers on mount (declared AFTER fetchCarriers — no TDZ)
  useEffect(() => { fetchCarriers(); }, [fetchCarriers]);

  const handleAssignCarrier = async () => {
    if (!assignCarrierId) { toast.error('Select a carrier first'); return; }
    setActionLoading(true);
    try {
      await assignCarrierToClaim(assignModal, Number(assignCarrierId));
      toast.success('Carrier assigned to claim!');
      setAssignModal(null);
      setAssignCarrierId('');
      await fetchClaims();
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message || 'Assignment failed';
      toast.error(msg);
    }
    finally { setActionLoading(false); }
  };

  const handleApproveCarrier = async (id) => {
    setCarrierActionLoading(`approve-${id}`);
    try {
      const result = await approveCarrier(id);
      const name = result?.companyName || result?.data?.companyName || `Carrier #${id}`;
      toast.success(`✅ ${name} has been approved! An email notification has been sent.`, { duration: 4000 });
      await fetchCarriers();
    } catch (err) {
      // Global axios interceptor already shows a toast — only log here
      console.error('Approve carrier error:', err?.response?.data || err?.message);
    } finally {
      setCarrierActionLoading(null);
    }
  };

  const handleRejectCarrier = async (id) => {
    setCarrierActionLoading(`reject-${id}`);
    try {
      const result = await rejectCarrier(id);
      const name = result?.companyName || result?.data?.companyName || `Carrier #${id}`;
      toast.success(`🚫 ${name} has been rejected.`, { duration: 4000 });
      await fetchCarriers();
    } catch (err) {
      console.error('Reject carrier error:', err?.response?.data || err?.message);
    } finally {
      setCarrierActionLoading(null);
    }
  };

  return (
    <div className="space-y-6 max-w-[1400px] mx-auto pb-10">
      {/* Carrier Approved Notification Modal */}
      {showCarrierPopup && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-slate-900/80 backdrop-blur-sm">
          <div className="bg-slate-800 border border-amber-500/50 rounded-2xl shadow-2xl w-full max-w-md p-6 animate-[cardIn_0.3s_ease-out]">
            <div className="flex items-center gap-3 mb-4">
              <div className="bg-amber-500/20 p-3 rounded-full">
                <CheckCircle className="w-6 h-6 text-amber-500" />
              </div>
              <h3 className="text-xl font-bold text-slate-100">Action Required</h3>
            </div>
            <p className="text-slate-300 text-sm mb-6">
              Carrier has approved <strong className="text-amber-400">{carrierApprovedCount}</strong> claim(s). Ready for final approval/payment.
            </p>
            <div className="flex justify-end gap-3">
              <button onClick={() => setShowCarrierPopup(false)} className="px-5 py-2.5 bg-amber-600 hover:bg-amber-700 text-white font-bold rounded-lg transition-colors">
                Got it
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-100 tracking-tight">Admin Dashboard</h1>
          <p className="text-sm text-slate-400 mt-1">Manage users and review insurance claims</p>
        </div>
        <div className="flex items-center gap-3">
          {/* Notification Bell */}
          <div className="relative">
            <button
              onClick={() => { setNotifOpen(o => !o); if (!notifOpen) fetchNotifications(); }}
              className="relative flex items-center justify-center w-10 h-10 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-lg transition-all"
              title="Notifications"
            >
              <Bell className="w-4 h-4 text-slate-300" />
              {unreadCount > 0 && (
                <span className="absolute -top-1.5 -right-1.5 min-w-[18px] h-[18px] bg-red-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center px-1 shadow">
                  {unreadCount > 99 ? '99+' : unreadCount}
                </span>
              )}
            </button>

            {/* Notification Panel */}
            {notifOpen && (
              <div className="absolute right-0 top-12 w-96 bg-slate-900 border border-slate-700 rounded-2xl shadow-2xl z-50 overflow-hidden">
                <div className="flex items-center justify-between px-4 py-3 border-b border-slate-700">
                  <h3 className="font-bold text-white flex items-center gap-2">
                    <Bell className="w-4 h-4 text-amber-400" /> Notifications
                    {unreadCount > 0 && <span className="bg-red-500 text-white text-[10px] font-bold px-1.5 py-0.5 rounded-full">{unreadCount}</span>}
                  </h3>
                  <div className="flex items-center gap-2">
                    {unreadCount > 0 && (
                      <button onClick={markAllRead} className="text-[11px] text-blue-400 hover:text-blue-300 font-medium">Mark all read</button>
                    )}
                    <button onClick={() => setNotifOpen(false)} className="text-slate-500 hover:text-white"><X className="w-4 h-4" /></button>
                  </div>
                </div>
                <div className="max-h-96 overflow-y-auto divide-y divide-slate-800">
                  {notifications.length === 0 ? (
                    <div className="py-10 text-center text-slate-500 text-sm">No notifications yet</div>
                  ) : notifications.map(n => (
                    <div
                      key={n.id}
                      onClick={() => markOneRead(n.id)}
                      className={`px-4 py-3 cursor-pointer transition-colors hover:bg-slate-800/60 ${!n.read ? 'bg-blue-500/5 border-l-2 border-blue-500' : ''}`}
                    >
                      <div className="flex items-start justify-between gap-2">
                        <p className={`text-xs font-semibold ${!n.read ? 'text-white' : 'text-slate-400'}`}>{n.title}</p>
                        {!n.read && <span className="w-2 h-2 rounded-full bg-blue-500 shrink-0 mt-1" />}
                      </div>
                      <p className="text-[11px] text-slate-400 mt-0.5 leading-relaxed">{n.message}</p>
                      <p className="text-[10px] text-slate-600 mt-1">
                        {n.createdAt ? new Date(n.createdAt).toLocaleString('en-US', { month:'short', day:'numeric', hour:'2-digit', minute:'2-digit' }) : ''}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          <button
            onClick={() => {
              if (activeTab === 'CLAIMS') fetchClaims();
              else if (activeTab === 'USERS') fetchUsers();
              else if (activeTab === 'MONITORING') fetchMonitoring();
              else if (activeTab === 'CARRIERS') fetchCarriers();
            }}
            className="flex items-center gap-2 text-sm text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-lg px-4 py-2 transition-all shadow-sm"
          >
            <RefreshCw className="w-4 h-4" /> Refresh Data
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex space-x-1 bg-slate-800/50 border border-slate-700 p-1 rounded-xl w-max">
        <button
          onClick={() => setActiveTab('CLAIMS')}
          className={`flex items-center gap-2 px-6 py-2.5 rounded-lg text-sm font-semibold transition-all ${
            activeTab === 'CLAIMS' ? 'bg-blue-600 text-white shadow-md' : 'text-slate-400 hover:text-slate-200 hover:bg-slate-700/50'
          }`}
        >
          <FileText className="w-4 h-4" /> Claims Management
        </button>
        <button
          onClick={() => setActiveTab('USERS')}
          className={`flex items-center gap-2 px-6 py-2.5 rounded-lg text-sm font-semibold transition-all ${
            activeTab === 'USERS' ? 'bg-blue-600 text-white shadow-md' : 'text-slate-400 hover:text-slate-200 hover:bg-slate-700/50'
          }`}
        >
          <Users className="w-4 h-4" /> User Management
        </button>
        <button
          onClick={() => setActiveTab('MONITORING')}
          className={`flex items-center gap-2 px-6 py-2.5 rounded-lg text-sm font-semibold transition-all ${
            activeTab === 'MONITORING' ? 'bg-blue-600 text-white shadow-md' : 'text-slate-400 hover:text-slate-200 hover:bg-slate-700/50'
          }`}
        >
          <Activity className="w-4 h-4" /> System Monitoring
        </button>
        <button
          onClick={() => { setActiveTab('CARRIERS'); fetchCarriers(); }}
          className={`flex items-center gap-2 px-6 py-2.5 rounded-lg text-sm font-semibold transition-all ${
            activeTab === 'CARRIERS' ? 'bg-amber-600 text-white shadow-md' : 'text-slate-400 hover:text-slate-200 hover:bg-slate-700/50'
          }`}
        >
          <Truck className="w-4 h-4" /> Carriers
        </button>
      </div>

      {activeTab === 'CLAIMS' && (
        <div className="space-y-6 animate-[fadeIn_0.3s_ease-out]">
          <div className="bg-slate-800 rounded-xl shadow-sm border border-slate-700 p-4 flex flex-wrap gap-3 items-center">
            <div className="relative flex-1 min-w-[200px]">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
              <input
                type="text"
                value={cSearch}
                onChange={e => { setCSearch(e.target.value); setCPage(0); }}
                placeholder="Search by username…"
                className="pl-9 pr-3 py-2 w-full bg-slate-900 border border-slate-700 rounded-lg text-sm text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 placeholder:text-slate-500 transition-shadow"
              />
            </div>
            <div className="flex items-center gap-2 relative">
              <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" />
              <select
                value={statusFilter}
                onChange={e => { setStatusFilter(e.target.value); setCPage(0); }}
                className="w-40 appearance-none bg-slate-900 border border-slate-700 rounded-lg py-2 pl-9 pr-8 text-sm text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow"
              >
                {STATUS_OPTS.map(s => <option key={s} value={s}>{s || 'All Statuses'}</option>)}
              </select>
            </div>
          </div>

          <div className="bg-slate-800 rounded-xl shadow-sm border border-slate-700 overflow-hidden">
            {cLoading ? (
              <div className="flex justify-center py-16"><Loader message="Loading claims…" /></div>
            ) : cError ? (
              <div className="flex flex-col items-center gap-3 py-16 text-center">
                <AlertTriangle className="w-10 h-10 text-amber-500/60" />
                <p className="text-slate-300 font-semibold">Failed to load claims</p>
                <p className="text-slate-500 text-sm max-w-sm">{cError}</p>
                <button onClick={fetchClaims} className="mt-2 flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white text-sm font-semibold rounded-lg transition-colors">
                  <RefreshCw className="w-4 h-4" /> Retry
                </button>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-700/50">
                  <thead className="bg-slate-900/50">
                    <tr>
                      {['Claim ID', 'Policy No', 'Amount', 'Date', 'Status', 'Actions'].map(h => (
                        <th key={h} className="px-6 py-4 text-left text-xs font-semibold text-slate-400 uppercase tracking-wider whitespace-nowrap">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-700/50 bg-slate-800">
                    {claims.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="text-center py-16 text-slate-500">
                          <FileText className="w-10 h-10 mx-auto mb-3 text-slate-600" />
                          <p className="font-medium text-slate-400">No claims found</p>
                        </td>
                      </tr>
                    ) : claims.map(claim => (
                      <tr key={claim.id} className="hover:bg-slate-700/30 transition-colors group">
                        <td className="px-6 py-4 text-sm font-bold text-blue-400">#{claim.id}</td>
                        <td className="px-6 py-4 text-sm text-slate-200 font-medium">{claim.policyNumber}</td>
                        <td className="px-6 py-4 text-sm font-semibold text-slate-200">{claim.amount != null ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(claim.amount) : '—'}</td>
                        <td className="px-6 py-4 text-sm text-slate-400 whitespace-nowrap">
                          {claim.createdDate ? new Date(claim.createdDate).toLocaleDateString('en-US', { day: 'numeric', month: 'short', year: 'numeric' }) : '—'}
                        </td>
                        <td className="px-6 py-4"><StatusBadge status={claim.status} /></td>
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-2 flex-wrap">
                            <button onClick={() => navigate(`/claims/${claim.id}`)} className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-700 hover:bg-slate-600 text-slate-200 rounded-lg text-xs font-medium border border-slate-600 transition-colors">
                              <Eye className="w-3.5 h-3.5" /> View
                            </button>
                            <button onClick={() => handleAiSummary(claim)} className="flex items-center gap-1.5 px-3 py-1.5 bg-blue-500/10 hover:bg-blue-500/20 text-blue-400 rounded-lg text-xs font-medium border border-blue-500/20 transition-colors">
                              <Bot className="w-3.5 h-3.5" /> Analyze
                            </button>
                            <button
                              onClick={() => {
                                setAssignModal(claim.id);
                                setAssignCarrierId('');
                                // Refresh carrier list so dropdown is always current
                                if (carriers.length === 0) fetchCarriers();
                              }}
                              className="flex items-center gap-1.5 px-3 py-1.5 bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 rounded-lg text-xs font-medium border border-amber-500/20 transition-colors">
                              <Truck className="w-3.5 h-3.5" /> Assign Carrier
                            </button>
                            {isAdmin && (claim.status === 'SUBMITTED' || claim.status === 'UNDER_REVIEW' || claim.status === 'AI_VALIDATED') && (
                              <>
                                <button onClick={() => setApproveModal(claim.id)} className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 rounded-lg text-xs font-medium border border-emerald-500/20 transition-colors">
                                  <CheckCircle className="w-3.5 h-3.5" /> Approve
                                </button>
                                <button onClick={() => setRejectModal(claim.id)} className="flex items-center gap-1.5 px-3 py-1.5 bg-red-500/10 hover:bg-red-500/20 text-red-400 rounded-lg text-xs font-medium border border-red-500/20 transition-colors">
                                  <XCircle className="w-3.5 h-3.5" /> Reject
                                </button>
                              </>
                            )}
                            {isAdmin && claim.status === 'CARRIER_APPROVED' && (
                              <button 
                                onClick={() => navigate(`/claims/${claim.id}`)} 
                                className="flex items-center gap-1.5 px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-bold transition-all shadow-sm shadow-blue-600/20"
                              >
                                <CreditCard className="w-3.5 h-3.5" /> Release Payment
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            {!cLoading && !cError && cTotalPages > 1 && (
              <div className="px-6 py-4 border-t border-slate-700 flex items-center justify-between bg-slate-800">
                <span className="text-sm text-slate-400">Page <strong className="text-slate-200">{cPage + 1}</strong> of <strong className="text-slate-200">{cTotalPages}</strong> · {cTotal} claims</span>
                <div className="flex gap-2">
                  <button onClick={() => setCPage(p => Math.max(0, p - 1))} disabled={cPage === 0} className="p-2 border border-slate-600 text-slate-300 rounded-lg disabled:opacity-40 hover:bg-slate-700 transition-colors"><ChevronLeft className="w-4 h-4" /></button>
                  <button onClick={() => setCPage(p => Math.min(cTotalPages - 1, p + 1))} disabled={cPage === cTotalPages - 1} className="p-2 border border-slate-600 text-slate-300 rounded-lg disabled:opacity-40 hover:bg-slate-700 transition-colors"><ChevronRight className="w-4 h-4" /></button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {activeTab === 'USERS' && (
        <div className="space-y-6 animate-[fadeIn_0.3s_ease-out]">
          <div className="bg-slate-800 rounded-xl shadow-sm border border-slate-700 p-4">
            <div className="relative max-w-md">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
              <input
                type="text"
                value={uSearch}
                onChange={e => { setUSearch(e.target.value); setUPage(0); }}
                placeholder="Search by name or email…"
                className="pl-9 pr-3 py-2 w-full bg-slate-900 border border-slate-700 rounded-lg text-sm text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 placeholder:text-slate-500 transition-shadow"
              />
            </div>
          </div>

          <div className="bg-slate-800 rounded-xl shadow-sm border border-slate-700 overflow-hidden">
            {uLoading ? (
              <div className="flex justify-center py-16"><Loader message="Loading users…" /></div>
            ) : uError ? (
              <div className="flex flex-col items-center gap-3 py-16 text-center">
                <AlertTriangle className="w-10 h-10 text-amber-500/60" />
                <p className="text-slate-300 font-semibold">Failed to load users</p>
                <p className="text-slate-500 text-sm max-w-sm">{uError}</p>
                <button onClick={fetchUsers} className="mt-2 flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white text-sm font-semibold rounded-lg transition-colors">
                  <RefreshCw className="w-4 h-4" /> Retry
                </button>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-700/50">
                  <thead className="bg-slate-900/50">
                    <tr>
                      {['Name', 'Email', 'Role', 'Status', 'Joined Date', 'Actions'].map(h => (
                        <th key={h} className="px-6 py-4 text-left text-xs font-semibold text-slate-400 uppercase tracking-wider whitespace-nowrap">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-700/50 bg-slate-800">
                    {users.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="text-center py-16 text-slate-500">
                          <Users className="w-10 h-10 mx-auto mb-3 text-slate-600" />
                          <p className="font-medium text-slate-400">No users found</p>
                        </td>
                      </tr>
                    ) : users.map(user => (
                      <tr key={user.id} className="hover:bg-slate-700/30 transition-colors group">
                        <td className="px-6 py-4 text-sm font-bold text-slate-200">{user.username}</td>
                        <td className="px-6 py-4 text-sm text-slate-400 font-medium">{user.email}</td>
                        <td className="px-6 py-4 text-sm">
                          <span className="bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 px-2 py-1 rounded text-[10px] font-bold uppercase tracking-wider">
                            {(user.userRole || '').replace('ROLE_', '')}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold border ${
                            user.userStatus === 'ACTIVE' 
                              ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' 
                              : 'bg-red-500/10 text-red-400 border-red-500/20'
                          }`}>
                            {user.userStatus === 'ACTIVE' ? <CheckCircle className="w-3.5 h-3.5" /> : <XCircle className="w-3.5 h-3.5" />}
                            {user.userStatus}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-sm text-slate-400 whitespace-nowrap">
                          {user.createdAt ? new Date(user.createdAt).toLocaleDateString('en-US') : '—'}
                        </td>
                        <td className="px-6 py-4">
                          {user.userRole !== 'FMG_ADMIN' && (
                            <div className="flex items-center gap-2">
                              {user.userStatus === 'ACTIVE' ? (
                                <button onClick={() => setBlockModal(user.id)} className="flex items-center gap-1.5 px-3 py-1.5 bg-red-500/10 hover:bg-red-500/20 text-red-400 rounded-lg text-xs font-medium border border-red-500/20 transition-colors">
                                  <Lock className="w-3.5 h-3.5" /> Block
                                </button>
                              ) : (
                                <button onClick={() => setUnblockModal(user.id)} className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 rounded-lg text-xs font-medium border border-emerald-500/20 transition-colors">
                                  <Unlock className="w-3.5 h-3.5" /> Unblock
                                </button>
                              )}
                            </div>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            {uTotalPages > 1 && (
              <div className="px-6 py-4 border-t border-slate-700 flex items-center justify-between bg-slate-800">
                <span className="text-sm text-slate-400">Page <strong className="text-slate-200">{uPage + 1}</strong> of <strong className="text-slate-200">{uTotalPages}</strong> · {uTotal} users</span>
                <div className="flex gap-2">
                  <button onClick={() => setUPage(p => Math.max(0, p - 1))} disabled={uPage === 0} className="p-2 border border-slate-600 text-slate-300 rounded-lg disabled:opacity-40 hover:bg-slate-700 transition-colors"><ChevronLeft className="w-4 h-4" /></button>
                  <button onClick={() => setUPage(p => Math.min(uTotalPages - 1, p + 1))} disabled={uPage === uTotalPages - 1} className="p-2 border border-slate-600 text-slate-300 rounded-lg disabled:opacity-40 hover:bg-slate-700 transition-colors"><ChevronRight className="w-4 h-4" /></button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {activeTab === 'MONITORING' && (
        <div className="space-y-6 animate-[fadeIn_0.3s_ease-out]">
          {mLoading ? (
            <div className="flex justify-center py-16"><Loader message="Loading system data…" /></div>
          ) : mError ? (
            <div className="flex flex-col items-center gap-3 py-16 text-center">
              <AlertTriangle className="w-10 h-10 text-amber-500/60" />
              <p className="text-slate-300 font-semibold">Failed to load monitoring data</p>
              <p className="text-slate-500 text-sm max-w-sm">{mError}</p>
              <button onClick={fetchMonitoring} className="mt-2 flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white text-sm font-semibold rounded-lg transition-colors">
                <RefreshCw className="w-4 h-4" /> Retry
              </button>
            </div>
          ) : monitoring ? (
            <>
              {/* Kafka Status */}
              <div className="bg-slate-800 rounded-xl shadow-sm border border-slate-700 p-6">
                <h2 className="text-lg font-bold text-slate-100 flex items-center gap-2 mb-4">
                  <Activity className="w-5 h-5 text-blue-400" /> Kafka Broker Status
                </h2>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div className="bg-slate-900/50 p-4 rounded-lg border border-slate-700/50">
                    <p className="text-xs text-slate-400 mb-1">Status</p>
                    <div className="flex items-center gap-2">
                      <div className={`w-2 h-2 rounded-full ${monitoring.kafka.status === 'ONLINE' ? 'bg-emerald-500 animate-pulse' : 'bg-red-500'}`} />
                      <span className={`font-bold ${monitoring.kafka.status === 'ONLINE' ? 'text-emerald-400' : 'text-red-400'}`}>{monitoring.kafka.status}</span>
                    </div>
                  </div>
                  <div className="bg-slate-900/50 p-4 rounded-lg border border-slate-700/50">
                    <p className="text-xs text-slate-400 mb-1">Brokers</p>
                    <p className="text-sm font-semibold text-slate-200">{monitoring.kafka.brokers}</p>
                  </div>
                  <div className="bg-slate-900/50 p-4 rounded-lg border border-slate-700/50">
                    <p className="text-xs text-slate-400 mb-1">Active Topics</p>
                    <div className="flex gap-2 mt-1 flex-wrap">
                      {monitoring.kafka.topics.map(t => (
                        <span key={t} className="px-2 py-1 bg-blue-500/10 text-blue-400 rounded border border-blue-500/20 text-[10px] font-bold">{t}</span>
                      ))}
                    </div>
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Failed Claims */}
                <div className="bg-slate-800 rounded-xl shadow-sm border border-slate-700 flex flex-col">
                  <div className="p-5 border-b border-slate-700 bg-slate-800/50">
                    <h2 className="text-sm font-bold text-slate-100 flex items-center gap-2">
                      <ShieldAlert className="w-4 h-4 text-red-400" /> Recent Failed / Rejected Claims
                    </h2>
                  </div>
                  <div className="p-5 flex-1 overflow-y-auto max-h-[400px] scrollbar-thin scrollbar-thumb-slate-700">
                    {monitoring.failedClaims?.length === 0 ? (
                      <p className="text-slate-500 text-sm">No failed claims found.</p>
                    ) : (
                      <ul className="space-y-3">
                        {monitoring.failedClaims.map(claim => (
                          <li key={claim.id} className="bg-slate-900/50 p-3 rounded-lg border border-slate-700/50 flex justify-between items-start cursor-pointer hover:border-slate-600 transition-colors" onClick={() => navigate(`/claims/${claim.id}`)}>
                            <div>
                              <p className="text-xs font-bold text-blue-400">#{claim.id} <span className="text-slate-500 font-medium ml-1">· {claim.policyNumber}</span></p>
                              <p className="text-sm text-slate-200 mt-0.5">{claim.patientName || 'Unknown Patient'}</p>
                            </div>
                            <span className="text-xs font-semibold text-red-400 bg-red-500/10 px-2 py-1 rounded border border-red-500/20">REJECTED</span>
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                </div>

                {/* Error Logs */}
                <div className="bg-slate-800 rounded-xl shadow-sm border border-slate-700 flex flex-col">
                  <div className="p-5 border-b border-slate-700 bg-slate-800/50">
                    <h2 className="text-sm font-bold text-slate-100 flex items-center gap-2">
                      <Terminal className="w-4 h-4 text-slate-400" /> Recent System Logs
                    </h2>
                  </div>
                  <div className="p-5 flex-1 bg-[#0d1117] overflow-y-auto max-h-[400px] scrollbar-thin scrollbar-thumb-slate-700 rounded-b-xl font-mono text-xs">
                    {monitoring.errorLogs?.length === 0 ? (
                      <p className="text-slate-500">No error logs available.</p>
                    ) : (
                      <ul className="space-y-2">
                        {monitoring.errorLogs.map((log, idx) => (
                          <li key={idx} className="flex gap-3 text-slate-300">
                            <span className="text-slate-500 shrink-0">{new Date(log.timestamp).toLocaleTimeString([], { hour12: false })}</span>
                            <span className={`shrink-0 w-10 ${log.level === 'ERROR' ? 'text-red-400' : 'text-amber-400'}`}>[{log.level}]</span>
                            <span className="break-all">{log.message}</span>
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                </div>
              </div>
            </>
          ) : (
            <div className="text-center py-16 text-slate-500">Failed to load monitoring data.</div>
          )}
        </div>
      )}

      {/* Modals */}
      <ActionModal
        isOpen={!!approveModal}
        title={`Approve Claim #${approveModal}`}
        placeholder="Enter approval notes (optional)…"
        onConfirm={handleApprove}
        onCancel={() => setApproveModal(null)}
        loading={actionLoading}
        actionType="success"
      />
      <ActionModal
        isOpen={!!rejectModal}
        title={`Reject Claim #${rejectModal}`}
        placeholder="Enter rejection reason (required)…"
        onConfirm={handleReject}
        onCancel={() => setRejectModal(null)}
        loading={actionLoading}
        actionType="danger"
      />
      <ActionModal
        isOpen={!!blockModal}
        title="Block User"
        content="Are you sure you want to block this user? They will no longer be able to log in or submit claims."
        onConfirm={handleBlock}
        onCancel={() => setBlockModal(null)}
        loading={actionLoading}
        actionType="danger"
      />
      <ActionModal
        isOpen={!!unblockModal}
        title="Unblock User"
        content="Are you sure you want to unblock this user? Their account access will be restored."
        onConfirm={handleUnblock}
        onCancel={() => setUnblockModal(null)}
        loading={actionLoading}
        actionType="success"
      />

      {/* AI Drawer */}
      {aiClaim && (
        <AiDrawer claim={aiClaim} result={aiResult} loading={aiLoading} onClose={() => { setAiClaim(null); setAiResult(null); }} />
      )}

      {/* Assign Carrier Modal */}
      {assignModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/80 backdrop-blur-sm">
          <div className="bg-slate-800 border border-slate-700 rounded-2xl shadow-2xl w-full max-w-sm p-6">
            <h3 className="text-lg font-bold text-slate-200 mb-1">Assign Carrier to Claim #{assignModal}</h3>
            <p className="text-sm text-slate-400 mb-4">Only active carriers are shown.</p>
            <select
              value={assignCarrierId}
              onChange={e => setAssignCarrierId(e.target.value)}
              className="w-full bg-slate-900 border border-slate-700 rounded-xl p-3 text-sm text-slate-200 focus:outline-none focus:ring-2 focus:ring-amber-500 mb-4"
            >
              <option value="">— Select a Carrier —</option>
              {carriers.filter(c => c.userStatus === 'ACTIVE').map(c => (
                <option key={c.id} value={c.id}>{c.companyName} ({c.registrationNumber})</option>
              ))}
            </select>
            <div className="flex gap-3 justify-end">
              <button onClick={() => setAssignModal(null)} className="px-4 py-2 rounded-lg border border-slate-700 text-slate-300 hover:bg-slate-700 text-sm font-medium transition-colors">Cancel</button>
              <button
                onClick={handleAssignCarrier}
                disabled={!assignCarrierId || actionLoading}
                className="px-5 py-2 rounded-lg bg-amber-600 hover:bg-amber-500 text-white text-sm font-bold disabled:opacity-50 transition-colors">
                {actionLoading ? 'Assigning…' : 'Assign'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── CARRIERS PANEL ─────────────────────────────────────── */}
      {activeTab === 'CARRIERS' && (
        <div className="space-y-5 animate-[fadeIn_0.3s_ease-out]">
          <div className="bg-slate-800 rounded-xl border border-slate-700 p-4 flex items-center gap-4 flex-wrap">
            <div className="flex items-center gap-2 text-slate-300">
              <Truck className="w-4 h-4 text-amber-400" />
              <span className="text-sm font-semibold">Filter by Status:</span>
            </div>
            {['', 'PENDING', 'ACTIVE', 'BLOCKED'].map(s => (
              <button key={s}
                onClick={() => setCarrierStatusFilter(s)}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold border transition-all ${
                  carrierStatusFilter === s
                    ? 'bg-amber-600 text-white border-amber-700'
                    : 'bg-slate-700/50 text-slate-400 border-slate-600 hover:bg-slate-700 hover:text-slate-200'
                }`}>
                {s || 'All'}
              </button>
            ))}
            <button onClick={fetchCarriers} className="ml-auto flex items-center gap-2 text-xs text-slate-400 hover:text-white bg-slate-700 hover:bg-slate-600 border border-slate-600 px-3 py-1.5 rounded-lg transition-all">
              <RefreshCw className="w-3.5 h-3.5" /> Refresh
            </button>
          </div>

          <div className="bg-slate-800 rounded-xl border border-slate-700 overflow-hidden">
            <div className="px-5 py-3.5 border-b border-slate-700 flex items-center justify-between">
              <h3 className="text-sm font-bold text-slate-200 flex items-center gap-2">
                <Truck className="w-4 h-4 text-amber-400" /> Insurance Carriers
              </h3>
              <span className="text-xs text-slate-500">{carriers.length} total</span>
            </div>

            {carrierLoading ? (
              <div className="p-12 text-center text-slate-500"><Loader message="Loading carriers…" /></div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm text-left text-slate-300">
                  <thead className="text-[11px] uppercase text-slate-500 bg-slate-900/50 border-b border-slate-700">
                    <tr>
                      {['Company', 'Email', 'Type', 'License No.', 'Risk Score', 'AI Status', 'Status', 'Actions'].map(h => (
                        <th key={h} className="px-4 py-3 font-semibold">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-700/50">
                    {carriers
                      .filter(c => !carrierStatusFilter || c.userStatus === carrierStatusFilter)
                      .map(carrier => {
                        const risk = carrier.aiRiskScore ?? 50;
                        const riskColor = risk > 70 ? 'text-red-400' : risk > 40 ? 'text-amber-400' : 'text-emerald-400';
                        const statusColor = carrier.userStatus === 'ACTIVE' ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                          : carrier.userStatus === 'BLOCKED' ? 'bg-red-500/10 text-red-400 border-red-500/20'
                          : 'bg-amber-500/10 text-amber-400 border-amber-500/20';
                        return (
                          <tr key={carrier.id} className="hover:bg-slate-700/20 transition-colors">
                            <td className="px-4 py-3.5">
                              <div className="font-semibold text-slate-200">{carrier.companyName}</div>
                              <div className="text-xs text-slate-500 mt-0.5">{carrier.registrationNumber}</div>
                            </td>
                            <td className="px-4 py-3.5 text-xs text-slate-400">{carrier.email}</td>
                            <td className="px-4 py-3.5">
                              <span className="px-2 py-0.5 bg-slate-700 text-slate-300 rounded text-[11px]">{carrier.companyType}</span>
                            </td>
                            <td className="px-4 py-3.5 font-mono text-xs">{carrier.licenseNumber}</td>
                            <td className="px-4 py-3.5">
                              <div className="space-y-1">
                                <div className="flex justify-between text-[11px]">
                                  <span className="text-slate-500">Risk</span>
                                  <span className={`font-bold ${riskColor}`}>{Math.round(risk)}%</span>
                                </div>
                                <div className="h-1.5 bg-slate-700 rounded-full overflow-hidden w-20">
                                  <div className={`h-full rounded-full ${risk > 70 ? 'bg-red-500' : risk > 40 ? 'bg-amber-500' : 'bg-emerald-500'}`} style={{ width: `${risk}%` }} />
                                </div>
                              </div>
                            </td>
                            <td className="px-4 py-3.5">
                              <div className="text-xs">
                                <div className={`font-bold ${riskColor}`}>{carrier.aiRiskStatus || '—'}</div>
                                <div className="text-slate-500 mt-0.5">{carrier.aiRecommendation || '—'}</div>
                              </div>
                            </td>
                            <td className="px-4 py-3.5">
                              <span className={`px-2 py-0.5 rounded border text-[11px] font-bold ${statusColor}`}>
                                {carrier.userStatus}
                              </span>
                            </td>
                            <td className="px-4 py-3.5">
                              <div className="flex items-center gap-2">
                                {isAdmin && (carrier.status === "PENDING" || carrier.userStatus === "PENDING") && (
                                  <button
                                    onClick={() => handleApproveCarrier(carrier.id)}
                                    disabled={carrierActionLoading === `approve-${carrier.id}` || carrierActionLoading === `reject-${carrier.id}`}
                                    className="flex items-center justify-center gap-1 min-w-[90px] px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold rounded-lg transition-all disabled:opacity-60 disabled:cursor-not-allowed">
                                    {carrierActionLoading === `approve-${carrier.id}`
                                      ? <><Loader2 className="w-3.5 h-3.5 animate-spin" /> Approving…</>
                                      : <><CheckCircle className="w-3.5 h-3.5" /> Approve</>}
                                  </button>
                                )}
                                {isAdmin && carrier.userStatus !== 'BLOCKED' && (
                                  <button
                                    onClick={() => handleRejectCarrier(carrier.id)}
                                    disabled={carrierActionLoading === `reject-${carrier.id}` || carrierActionLoading === `approve-${carrier.id}`}
                                    className="flex items-center justify-center gap-1 min-w-[82px] px-3 py-1.5 bg-red-600 hover:bg-red-500 text-white text-xs font-semibold rounded-lg transition-all disabled:opacity-60 disabled:cursor-not-allowed">
                                    {carrierActionLoading === `reject-${carrier.id}`
                                      ? <><Loader2 className="w-3.5 h-3.5 animate-spin" /> Rejecting…</>
                                      : <><XCircle className="w-3.5 h-3.5" /> Reject</>}
                                  </button>
                                )}
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                  </tbody>
                </table>
                {carriers.filter(c => !carrierStatusFilter || c.userStatus === carrierStatusFilter).length === 0 && (
                  <div className="p-10 text-center text-slate-500">No carriers found.</div>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminDashboard;
