import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getClaims } from '../api/claim.service';
import StatusBadge from '../components/StatusBadge';
import Loader from '../components/Loader';
import ErrorMessage from '../components/ErrorMessage';
import { Search, Filter, ChevronLeft, ChevronRight, FileText, Eye, Clock, AlertCircle } from 'lucide-react';

const ClaimList = () => {
  const [claims, setClaims] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Pagination & Filtering state
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  
  const [filters, setFilters] = useState({
    status: '',
    minAmount: '',
    maxAmount: '',
    from: '',
    to: '',
    username: ''
  });
  
  const [showFilters, setShowFilters] = useState(false);
  
  const navigate = useNavigate();

  const fetchClaims = async () => {
    try {
      setLoading(true);
      setError(null);
      const params = {
        page,
        size: 10,
        sort: 'createdDate,desc'
      };
      
      if (filters.status) params.status = filters.status;
      if (filters.minAmount) params.minAmount = filters.minAmount;
      if (filters.maxAmount) params.maxAmount = filters.maxAmount;
      if (filters.from) params.from = new Date(filters.from).toISOString();
      if (filters.to) params.to = new Date(filters.to).toISOString();
      if (filters.username) params.username = filters.username;

      const data = await getClaims(params);

      setClaims(data?.content || []);
      setTotalPages(data?.totalPages || 1);
      setTotalElements(data?.totalElements || 0);
    } catch (err) {
      setError('Failed to fetch claims');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchClaims();
  }, [page]); // Removed filters from dep array so we can apply them explicitly

  const handleFilterChange = (e) => {
    setFilters({ ...filters, [e.target.name]: e.target.value });
  };

  const applyFilters = () => {
    setPage(0);
    fetchClaims();
  };

  const clearFilters = () => {
    setFilters({ status: '', minAmount: '', maxAmount: '', from: '', to: '', username: '' });
    setPage(0);
    // Note: useEffect won't trigger if page is already 0, so we call fetch directly after state update (handled by next render loop normally, but better to call it if we need immediate refresh, or just let useEffect trigger if we put filters back in deps. Actually, putting applyFilters is better).
  };

  // Effect for clear filters
  useEffect(() => {
    if (filters.status === '' && filters.minAmount === '' && filters.maxAmount === '' && filters.from === '' && filters.to === '' && filters.username === '') {
      fetchClaims();
    }
  }, [filters]);

  const isDelayed = (createdDate, status) => {
    if (['CARRIER_APPROVED', 'REJECTED', 'PAYMENT_PENDING', 'SETTLED'].includes(status)) return false;
    const created = new Date(createdDate).getTime();
    const now = new Date().getTime();
    return (now - created) > 24 * 60 * 60 * 1000; // > 24 hours
  };

  if (loading && claims.length === 0) return <Loader fullScreen message="Loading claims..." />;
  if (error) return <ErrorMessage message={error} onRetry={fetchClaims} />;

  return (
    <div className="space-y-6 max-w-[1400px] mx-auto pb-10">
      <div className="flex justify-between items-end">
        <div>
          <h1 className="text-2xl font-bold text-slate-100 tracking-tight">All Claims</h1>
          <p className="text-sm text-slate-400 mt-1">View, filter and track submitted claims</p>
        </div>
      </div>

      {/* Advanced Filters */}
      <div className="bg-slate-800 p-4 rounded-xl shadow-sm border border-slate-700">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-sm font-bold text-slate-200 flex items-center gap-2">
            <Filter className="w-4 h-4 text-blue-400" /> Advanced Filters
          </h2>
          <button 
            onClick={() => setShowFilters(!showFilters)}
            className="text-xs text-blue-400 hover:text-blue-300 font-semibold transition-colors"
          >
            {showFilters ? 'Hide Filters' : 'Show Filters'}
          </button>
        </div>
        
        {showFilters && (
          <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-4">
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Status</label>
              <select name="status" value={filters.status} onChange={handleFilterChange} className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-blue-500">
                <option value="">All Statuses</option>
                <option value="SUBMITTED">Submitted</option>
                <option value="AI_VALIDATED">AI Validated</option>
                <option value="UNDER_REVIEW">Under Review</option>
                <option value="ADMIN_APPROVED">Admin Approved</option>
                <option value="CARRIER_APPROVED">Carrier Approved</option>
                <option value="REJECTED">Rejected</option>
                <option value="PAYMENT_PENDING">Payment Pending</option>
                <option value="SETTLED">Settled</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Min Amount ($)</label>
              <input type="number" name="minAmount" value={filters.minAmount} onChange={handleFilterChange} className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-blue-500" placeholder="e.g. 1000" />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Max Amount ($)</label>
              <input type="number" name="maxAmount" value={filters.maxAmount} onChange={handleFilterChange} className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-blue-500" placeholder="e.g. 100000" />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">From Date</label>
              <input type="date" name="from" value={filters.from} onChange={handleFilterChange} className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-blue-500 [color-scheme:dark]" />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">To Date</label>
              <input type="date" name="to" value={filters.to} onChange={handleFilterChange} className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-blue-500 [color-scheme:dark]" />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Username / Email</label>
              <input type="text" name="username" value={filters.username} onChange={handleFilterChange} className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-blue-500" placeholder="Search by email" />
            </div>
          </div>
        )}

        {showFilters && (
          <div className="flex justify-end gap-3 pt-4 border-t border-slate-700">
            <button onClick={clearFilters} className="px-4 py-2 bg-slate-700 hover:bg-slate-600 text-slate-200 rounded-lg text-sm font-semibold transition-colors">Clear</button>
            <button onClick={applyFilters} className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-semibold transition-colors shadow-sm">Apply Filters</button>
          </div>
        )}
      </div>

      {/* Table view */}
      <div className="bg-slate-800 rounded-xl shadow-sm border border-slate-700 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-700/50">
            <thead className="bg-slate-900/50">
              <tr>
                <th scope="col" className="px-6 py-4 text-left text-xs font-semibold text-slate-400 uppercase tracking-wider">Claim Details</th>
                <th scope="col" className="px-6 py-4 text-left text-xs font-semibold text-slate-400 uppercase tracking-wider">Submitter</th>
                <th scope="col" className="px-6 py-4 text-left text-xs font-semibold text-slate-400 uppercase tracking-wider">Amount</th>
                <th scope="col" className="px-6 py-4 text-left text-xs font-semibold text-slate-400 uppercase tracking-wider">Date</th>
                <th scope="col" className="px-6 py-4 text-left text-xs font-semibold text-slate-400 uppercase tracking-wider">Status / SLA</th>
                <th scope="col" className="px-6 py-4 text-right text-xs font-semibold text-slate-400 uppercase tracking-wider">Action</th>
              </tr>
            </thead>
            <tbody className="bg-slate-800 divide-y divide-slate-700/50">
              {claims.map((claim) => (
                <tr 
                  key={claim.id} 
                  onClick={() => navigate(`/claims/${claim.id}`)}
                  className="hover:bg-slate-700/30 cursor-pointer transition-colors group"
                >
                  <td className="px-6 py-4 whitespace-nowrap">
                    <p className="text-sm font-bold text-blue-400">#{claim.id}</p>
                    <p className="text-xs font-medium text-slate-400 mt-1">{claim.policyNumber}</p>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-slate-200">{claim.username || '—'}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-slate-200">
                    {claim.amount != null ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(claim.amount) : '—'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-slate-400">
                    {claim.createdDate ? new Date(claim.createdDate).toLocaleDateString('en-US', { day: 'numeric', month: 'short', year: 'numeric' }) : '—'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex flex-col gap-2 items-start">
                      <StatusBadge status={claim.status} />
                      {isDelayed(claim.createdDate, claim.status) && (
                        <span className="inline-flex items-center gap-1 bg-red-500/10 text-red-400 border border-red-500/20 px-1.5 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider">
                          <AlertCircle className="w-3 h-3" /> SLA Breach &gt;24h
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right">
                    <button className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-slate-700 group-hover:bg-blue-600 text-slate-300 group-hover:text-white rounded-lg text-xs font-medium border border-slate-600 group-hover:border-blue-500 transition-all shadow-sm">
                      <Eye className="w-3.5 h-3.5" /> View
                    </button>
                  </td>
                </tr>
              ))}
              {claims.length === 0 && !loading && (
                <tr>
                  <td colSpan="6" className="px-6 py-16 text-center text-slate-500">
                    <FileText className="w-10 h-10 mx-auto mb-3 text-slate-600" />
                    <p className="font-medium text-slate-400">No claims found matching your criteria.</p>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        
        {/* Pagination */}
        {totalPages > 1 && (
          <div className="px-6 py-4 border-t border-slate-700 flex items-center justify-between bg-slate-800">
            <span className="text-sm text-slate-400">
              Page <strong className="text-slate-200">{page + 1}</strong> of <strong className="text-slate-200">{totalPages}</strong> · {totalElements} claims
            </span>
            <div className="flex gap-2">
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                className="p-2 border border-slate-600 text-slate-300 rounded-lg disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-700 transition-colors"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <button
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page === totalPages - 1}
                className="p-2 border border-slate-600 text-slate-300 rounded-lg disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-700 transition-colors"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ClaimList;
