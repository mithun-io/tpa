import React, { useState, useEffect } from 'react';
import { getDashboardAnalytics } from '../api/analytics.service';
import Loader from '../components/Loader';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import { TrendingUp, Activity, CheckCircle, DollarSign } from 'lucide-react';

const COLORS = {
  APPROVED: '#10b981', // emerald-500
  REJECTED: '#ef4444', // red-500
  REVIEW: '#f59e0b',   // amber-500
  PROCESSING: '#3b82f6', // blue-500
  PENDING: '#64748b'   // slate-500
};

const StatCard = ({ title, value, icon: Icon, colorClass, bgClass }) => (
  <div className="bg-slate-800 rounded-xl border border-slate-700 p-5 flex items-center gap-4">
    <div className={`p-3 rounded-xl border border-white/5 ${bgClass}`}>
      <Icon className={`w-6 h-6 ${colorClass}`} />
    </div>
    <div>
      <p className="text-sm text-slate-400 font-medium">{title}</p>
      <p className={`text-2xl font-bold ${colorClass}`}>{value}</p>
    </div>
  </div>
);

const AnalyticsDashboard = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const res = await getDashboardAnalytics();
        setData(res);
      } catch (err) {
        console.error('Failed to load analytics', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading) return <Loader fullScreen message="Loading Analytics..." />;
  if (!data) return <div className="text-white">Failed to load data</div>;

  const statusData = Object.entries(data.statusDistribution || {}).map(([name, value]) => ({ name, value }));
  const totalClaims = statusData.reduce((acc, curr) => acc + curr.value, 0);

  return (
    <div className="space-y-6 max-w-[1400px] mx-auto pb-10">
      <div>
        <h1 className="text-2xl font-bold text-slate-100 tracking-tight">Platform Analytics</h1>
        <p className="text-sm text-slate-400 mt-1">Real-time insights and KPIs for the TPA system</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard 
          title="Total Payout (Approved)" 
          value={new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(data.totalApprovedPayout || 0)} 
          icon={DollarSign} 
          colorClass="text-emerald-400" 
          bgClass="bg-emerald-500/10" 
        />
        <StatCard 
          title="Total Claims" 
          value={totalClaims} 
          icon={Activity} 
          colorClass="text-blue-400" 
          bgClass="bg-blue-500/10" 
        />
        <StatCard 
          title="Avg Processing Time" 
          value="~1.2 days" // Placeholder for advanced SLA logic
          icon={TrendingUp} 
          colorClass="text-amber-400" 
          bgClass="bg-amber-500/10" 
        />
        <StatCard 
          title="Approval Rate" 
          value={`${totalClaims > 0 ? Math.round(((data.statusDistribution?.APPROVED || 0) / totalClaims) * 100) : 0}%`} 
          icon={CheckCircle} 
          colorClass="text-indigo-400" 
          bgClass="bg-indigo-500/10" 
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Claims Per Day Chart */}
        <div className="bg-slate-800 rounded-xl border border-slate-700 p-6 shadow-sm">
          <h2 className="text-base font-bold text-slate-100 mb-6">Claims Submitted (Last 30 Days)</h2>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={data.claimsPerDay || []}>
                <XAxis dataKey="date" stroke="#64748b" fontSize={12} tickLine={false} axisLine={false} />
                <YAxis stroke="#64748b" fontSize={12} tickLine={false} axisLine={false} />
                <Tooltip 
                  cursor={{fill: '#334155'}}
                  contentStyle={{ backgroundColor: '#1e293b', borderColor: '#334155', borderRadius: '8px', color: '#f8fafc' }}
                />
                <Bar dataKey="count" fill="#3b82f6" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Status Distribution Pie Chart */}
        <div className="bg-slate-800 rounded-xl border border-slate-700 p-6 shadow-sm">
          <h2 className="text-base font-bold text-slate-100 mb-6">Claim Status Distribution</h2>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={statusData}
                  cx="50%"
                  cy="50%"
                  innerRadius={80}
                  outerRadius={110}
                  paddingAngle={5}
                  dataKey="value"
                  stroke="none"
                >
                  {statusData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[entry.name] || '#94a3b8'} />
                  ))}
                </Pie>
                <Tooltip 
                  contentStyle={{ backgroundColor: '#1e293b', borderColor: '#334155', borderRadius: '8px', color: '#f8fafc' }}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
          
          <div className="flex flex-wrap justify-center gap-4 mt-4">
            {statusData.map(entry => (
              <div key={entry.name} className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full" style={{ backgroundColor: COLORS[entry.name] || '#94a3b8' }} />
                <span className="text-xs font-semibold text-slate-300">{entry.name} ({entry.value})</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AnalyticsDashboard;
