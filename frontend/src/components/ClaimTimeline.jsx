import React, { useState, useEffect } from 'react';
import { getClaimTimeline } from '../api/claim.service';
import { 
  CheckCircle, Clock, Bot, ShieldCheck, 
  Hospital, Banknote, AlertCircle, FileText
} from 'lucide-react';

const STATUS_CONFIG = {
  'SUBMITTED': { label: 'Submitted', icon: FileText, color: 'text-slate-400', bg: 'bg-slate-400/10' },
  'AI_VALIDATED': { label: 'AI Validated', icon: Bot, color: 'text-indigo-400', bg: 'bg-indigo-400/10' },
  'UNDER_REVIEW': { label: 'Under Review', icon: Clock, color: 'text-amber-400', bg: 'bg-amber-400/10' },
  'ADMIN_APPROVED': { label: 'Admin Approved', icon: ShieldCheck, color: 'text-blue-400', bg: 'bg-blue-400/10' },
  'CARRIER_APPROVED': { label: 'Carrier Approved', icon: CheckCircle, color: 'text-emerald-400', bg: 'bg-emerald-400/10' },
  'PAYMENT_PENDING': { label: 'Payment Processing', icon: Hospital, color: 'text-orange-400', bg: 'bg-orange-400/10' },
  'SETTLED': { label: 'Payment Completed', icon: Banknote, color: 'text-emerald-500', bg: 'bg-emerald-500/10' },
  'REJECTED': { label: 'Rejected', icon: AlertCircle, color: 'text-red-400', bg: 'bg-red-400/10' }
};

const ClaimTimeline = ({ claimId, currentStatus }) => {
  const [timeline, setTimeline] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchTimeline = async () => {
      try {
        const data = await getClaimTimeline(claimId);
        // Sort ASC for vertical timeline flow
        setTimeline(data.sort((a, b) => new Date(a.changedAt) - new Date(b.changedAt)));
      } catch (err) {
        console.error('Failed to fetch timeline:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchTimeline();
    // Poll for updates if in progress
    const interval = setInterval(fetchTimeline, 10000);
    return () => clearInterval(interval);
  }, [claimId, currentStatus]);

  if (loading && timeline.length === 0) {
    return (
      <div className="animate-pulse space-y-4">
        {[1, 2, 3].map(i => (
          <div key={i} className="flex gap-4">
            <div className="w-8 h-8 bg-slate-700 rounded-full"></div>
            <div className="flex-1 space-y-2">
              <div className="h-2 bg-slate-700 rounded w-1/4"></div>
              <div className="h-2 bg-slate-700 rounded w-1/2"></div>
            </div>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="bg-slate-800 rounded-xl border border-slate-700 p-6 shadow-sm">
      <h2 className="text-base font-bold text-slate-100 mb-8 flex items-center gap-2">
        <Clock className="w-5 h-5 text-blue-400" /> Smart Claim Timeline
      </h2>
      
      <div className="relative space-y-8">
        {/* Continuous Line */}
        <div className="absolute left-4 top-2 bottom-2 w-0.5 bg-slate-700 z-0" />
        
        {timeline.map((step, idx) => {
          const config = STATUS_CONFIG[step.newStatus] || { label: step.newStatus, icon: FileText, color: 'text-slate-400', bg: 'bg-slate-400/10' };
          const Icon = config.icon;
          const isLast = idx === timeline.length - 1;

          return (
            <div key={step.id} className="relative z-10 flex gap-6 group">
              {/* Node Icon */}
              <div className={`w-8 h-8 rounded-full flex items-center justify-center border-2 transition-all duration-500 shadow-lg ${
                isLast ? 'scale-110 ring-4 ring-blue-500/10 border-blue-500 bg-slate-900' : 'border-slate-700 bg-slate-800'
              }`}>
                <Icon className={`w-4 h-4 ${isLast ? 'text-blue-400' : 'text-slate-500'}`} />
              </div>

              {/* Content */}
              <div className="flex-1">
                <div className="flex justify-between items-start">
                  <div>
                    <h3 className={`text-sm font-bold ${isLast ? 'text-slate-100' : 'text-slate-400'}`}>
                      {config.label}
                    </h3>
                    <p className="text-[10px] font-bold text-slate-500 uppercase tracking-widest mt-0.5">
                      {step.changedBy} • {new Date(step.changedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </p>
                  </div>
                  <span className="text-[10px] text-slate-600 font-medium">
                    {new Date(step.changedAt).toLocaleDateString()}
                  </span>
                </div>
                
                {step.notes && (
                  <div className="mt-2 p-2 rounded-lg bg-slate-900/50 border border-slate-700/50">
                    <p className="text-xs text-slate-400 italic">"{step.notes}"</p>
                  </div>
                )}
              </div>
            </div>
          );
        })}

        {/* Current / Next Step indicator if not settled */}
        {!['SETTLED', 'REJECTED'].includes(currentStatus) && (
          <div className="relative z-10 flex gap-6 opacity-50 italic">
            <div className="w-8 h-8 rounded-full flex items-center justify-center border-2 border-dashed border-slate-700 bg-transparent animate-pulse">
              <Clock className="w-4 h-4 text-slate-600" />
            </div>
            <div className="flex-1 pt-1">
              <p className="text-xs text-slate-500 font-medium">Next step processing...</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ClaimTimeline;
