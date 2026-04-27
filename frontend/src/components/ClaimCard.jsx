import React from 'react';
import { FileText, DollarSign, Calendar, CheckCircle, XCircle } from 'lucide-react';
import StatusBadge from './StatusBadge';
import { useAuth } from '../context/AuthContext';

const ClaimCard = ({ claim, onClick, onApprove, onReject }) => {
  const { user } = useAuth();
  const role = user?.userRole?.replace('ROLE_', '')?.replace('FMG_', '') || '';

  return (
    <div 
      onClick={() => onClick(claim.id)}
      className="bg-slate-800 rounded-xl shadow-sm border border-slate-700 p-5 cursor-pointer hover:shadow-md hover:border-blue-500/30 hover:shadow-black/20 transition-all duration-200 group"
    >
      <div className="flex justify-between items-start mb-4">
        <div>
          <h3 className="font-bold text-slate-200 flex items-center">
            <FileText className="w-4 h-4 mr-2 text-blue-400" />
            {claim.policyNumber}
          </h3>
          <p className="text-sm text-blue-400 mt-1 font-semibold tracking-wider">#{claim.id}</p>
        </div>
        <StatusBadge status={claim.status} />
      </div>
      
      <div className="space-y-2 mt-4 pt-4 border-t border-slate-700/50">
        <div className="flex items-center text-sm text-slate-400 font-medium">
          <DollarSign className="w-4 h-4 mr-2 text-emerald-400" />
          <span>Amount: <strong className="text-slate-200">{claim.amount != null ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(claim.amount) : '—'}</strong></span>
        </div>
        <div className="flex items-center text-sm text-slate-400 font-medium">
          <Calendar className="w-4 h-4 mr-2 text-amber-400" />
          <span>Created: {new Date(claim.createdDate).toLocaleDateString('en-US')}</span>
        </div>
      </div>
      
      {role?.toUpperCase() === 'ADMIN' && (claim.status === 'UNDER_REVIEW' || claim.status === 'AI_VALIDATED') && (
        <div className="flex gap-2 mt-4 pt-4 border-t border-slate-700/50">
          <button 
            onClick={(e) => { e.stopPropagation(); onApprove && onApprove(claim.id); }}
            className="flex-1 flex items-center justify-center gap-1.5 px-3 py-1.5 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 rounded-lg text-xs font-medium border border-emerald-500/20 transition-colors"
          >
            <CheckCircle className="w-3.5 h-3.5" /> Approve
          </button>
          <button 
            onClick={(e) => { e.stopPropagation(); onReject && onReject(claim.id); }}
            className="flex-1 flex items-center justify-center gap-1.5 px-3 py-1.5 bg-red-500/10 hover:bg-red-500/20 text-red-400 rounded-lg text-xs font-medium border border-red-500/20 transition-colors"
          >
            <XCircle className="w-3.5 h-3.5" /> Reject
          </button>
        </div>
      )}
    </div>
  );
};

export default ClaimCard;
