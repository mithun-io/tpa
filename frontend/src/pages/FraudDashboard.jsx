import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { useAuth } from '../context/AuthContext';
import { getAdminFraudDashboard, getCarrierFraudDashboard, markClaimAsSafe } from '../api/claim.service';
import Loader from '../components/Loader';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip as RechartsTooltip, Legend } from 'recharts';
import { ShieldAlert, ShieldCheck, AlertTriangle, Eye, CheckCircle, RefreshCcw, Activity } from 'lucide-react';

const FraudDashboard = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState(null);
  const [lastRefreshed, setLastRefreshed] = useState(new Date());
  const [errorMsg, setErrorMsg] = useState(null);

  const isAdmin = user?.userRole === 'FMG_ADMIN';

  const fetchData = async () => {
    try {
      const response = isAdmin ? await getAdminFraudDashboard() : await getCarrierFraudDashboard();
      setData(response);
      setLastRefreshed(new Date());
      setErrorMsg(null);
    } catch (error) {
      console.error('Error fetching fraud dashboard:', error);
      setErrorMsg(prev => {
        if (!prev) toast.error('Failed to load fraud detection data');
        return "Unable to load fraud data. Please try again.";
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    // Real-time polling every 20 seconds
    const interval = setInterval(fetchData, 20000);
    return () => clearInterval(interval);
  }, [user]);

  const handleMarkSafe = async (claimId) => {
    try {
      await markClaimAsSafe(claimId);
      toast.success('Claim marked as safe');
      fetchData();
    } catch (error) {
      toast.error('Failed to mark claim as safe');
    }
  };

  if (loading && !data && !errorMsg) return <Loader />;
  if (errorMsg && !data) return (
    <div className="bg-red-500/10 border border-red-500/20 rounded-xl p-10 text-center max-w-lg mx-auto mt-10">
      <AlertTriangle className="w-12 h-12 text-red-500 mx-auto mb-4" />
      <h2 className="text-xl font-bold text-slate-100 mb-2">Failed to Load</h2>
      <p className="text-slate-400 text-sm">{errorMsg}</p>
      <button 
        onClick={() => { setLoading(true); setErrorMsg(null); fetchData(); }} 
        className="mt-6 px-5 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-lg transition-colors font-medium text-sm"
      >
        Retry
      </button>
    </div>
  );
  if (!data) return null;

  const { stats, claims } = data;

  const chartData = [
    { name: 'High Risk', value: stats.highRisk, color: '#EF4444' },
    { name: 'Medium Risk', value: stats.mediumRisk, color: '#F59E0B' },
    { name: 'Safe (Low Risk)', value: stats.lowRisk, color: '#10B981' }
  ].filter(d => d.value > 0);

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-black text-slate-100 flex items-center gap-2">
            <ShieldAlert className="w-8 h-8 text-red-500" />
            Live Fraud Detection Engine
          </h1>
          <p className="text-slate-400 mt-1">Real-time AI monitoring and risk assessment</p>
        </div>
        <div className="flex items-center gap-4">
          {stats.highRisk > 0 && (
            <div className="bg-red-500/10 border border-red-500/20 text-red-400 px-4 py-2 rounded-lg font-bold flex items-center gap-2 animate-pulse">
              <AlertTriangle className="w-5 h-5" />
              {stats.highRisk} High-Risk Claims Detected
            </div>
          )}
          <div className="text-xs text-slate-500 flex items-center gap-1">
            <RefreshCcw className="w-3 h-3" /> Last updated: {lastRefreshed.toLocaleTimeString()}
          </div>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-slate-900 border border-slate-700 p-5 rounded-xl">
          <p className="text-slate-400 text-sm font-medium mb-1">Total Monitored</p>
          <p className="text-3xl font-black text-slate-200">{stats.totalClaims}</p>
        </div>
        <div className="bg-slate-900 border border-red-500/30 p-5 rounded-xl shadow-[0_0_15px_rgba(239,68,68,0.1)] relative overflow-hidden">
          <div className="absolute top-0 left-0 w-1 h-full bg-red-500"></div>
          <p className="text-red-400/80 text-sm font-bold uppercase tracking-wider mb-1">High Risk</p>
          <p className="text-3xl font-black text-red-500">{stats.highRisk}</p>
        </div>
        <div className="bg-slate-900 border border-amber-500/30 p-5 rounded-xl relative overflow-hidden">
          <div className="absolute top-0 left-0 w-1 h-full bg-amber-500"></div>
          <p className="text-amber-400/80 text-sm font-bold uppercase tracking-wider mb-1">Medium Risk</p>
          <p className="text-3xl font-black text-amber-500">{stats.mediumRisk}</p>
        </div>
        <div className="bg-slate-900 border border-emerald-500/30 p-5 rounded-xl relative overflow-hidden">
          <div className="absolute top-0 left-0 w-1 h-full bg-emerald-500"></div>
          <p className="text-emerald-400/80 text-sm font-bold uppercase tracking-wider mb-1">Safe</p>
          <p className="text-3xl font-black text-emerald-500">{stats.lowRisk}</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Risk Distribution Chart */}
        <div className="bg-slate-900 border border-slate-700 rounded-xl p-5 lg:col-span-1">
          <h2 className="text-lg font-bold text-slate-200 mb-4 flex items-center gap-2">
            <Activity className="w-5 h-5 text-blue-400" /> Risk Distribution
          </h2>
          {chartData.length > 0 ? (
            <div className="h-[250px]">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={chartData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={80}
                    paddingAngle={5}
                    dataKey="value"
                  >
                    {chartData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <RechartsTooltip 
                    contentStyle={{ backgroundColor: '#1E293B', borderColor: '#334155', color: '#F8FAFC' }}
                    itemStyle={{ color: '#E2E8F0' }}
                  />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <div className="h-[250px] flex items-center justify-center text-slate-500">No data available</div>
          )}
        </div>

        {/* Flagged Claims Table */}
        <div className="bg-slate-900 border border-slate-700 rounded-xl p-5 lg:col-span-2 flex flex-col">
          <h2 className="text-lg font-bold text-slate-200 mb-4 flex items-center gap-2">
            <ShieldAlert className="w-5 h-5 text-red-400" /> Flagged Claims Review
          </h2>
          <div className="overflow-x-auto flex-1">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-700 text-xs uppercase tracking-wider text-slate-400">
                  <th className="p-3 font-bold">Claim / Policy</th>
                  <th className="p-3 font-bold">Risk Level</th>
                  <th className="p-3 font-bold">Reasons</th>
                  <th className="p-3 font-bold text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {claims.filter(c => c.riskLevel !== 'LOW').length > 0 ? (
                  claims.filter(c => c.riskLevel !== 'LOW').sort((a,b) => b.riskScore - a.riskScore).map(claim => (
                    <tr key={claim.claimId} className="border-b border-slate-800/50 hover:bg-slate-800/50 transition-colors">
                      <td className="p-3">
                        <div className="font-bold text-slate-200">#{claim.claimId}</div>
                        <div className="text-xs text-slate-400">{claim.policyNumber}</div>
                      </td>
                      <td className="p-3">
                        <span className={`px-2.5 py-1 rounded-md text-[10px] font-bold tracking-wider uppercase border ${
                          claim.riskLevel === 'HIGH' ? 'bg-red-500/20 text-red-400 border-red-500/30' : 'bg-amber-500/20 text-amber-400 border-amber-500/30'
                        }`}>
                          {claim.riskLevel} ({Math.round(claim.riskScore)})
                        </span>
                      </td>
                      <td className="p-3">
                        <ul className="text-xs text-slate-300 space-y-1">
                          {claim.reasons.map((r, i) => (
                            <li key={i} className="flex items-start gap-1">
                              <span className="text-red-400 mt-0.5">•</span> {r}
                            </li>
                          ))}
                        </ul>
                      </td>
                      <td className="p-3 text-right">
                        <div className="flex justify-end gap-2">
                          <button
                            onClick={() => navigate(`/claims/${claim.claimId}`)}
                            className="p-2 bg-blue-500/10 hover:bg-blue-500/20 text-blue-400 rounded-lg transition-colors"
                            title="View Claim"
                          >
                            <Eye className="w-4 h-4" />
                          </button>
                          {isAdmin && (
                            <button
                              onClick={() => handleMarkSafe(claim.claimId)}
                              className="p-2 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 rounded-lg transition-colors"
                              title="Mark as Safe"
                            >
                              <ShieldCheck className="w-4 h-4" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="4" className="p-8 text-center text-slate-500">
                      <CheckCircle className="w-8 h-8 text-emerald-500/50 mx-auto mb-2" />
                      No high or medium risk claims detected.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};

export default FraudDashboard;
