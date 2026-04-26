import React from 'react';
import { FileText, DollarSign, Calendar } from 'lucide-react';
import StatusBadge from './StatusBadge';

const ClaimCard = ({ claim, onClick }) => {
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
    </div>
  );
};

export default ClaimCard;
