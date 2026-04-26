import React from 'react';

const STATUS_MAP = {
  APPROVED:   { label: 'Approved',    cls: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' },
  REJECTED:   { label: 'Rejected',    cls: 'bg-red-500/10 text-red-400 border-red-500/20' },
  PENDING:    { label: 'Pending',     cls: 'bg-blue-500/10 text-blue-400 border-blue-500/20' },
  PROCESSING: { label: 'Processing',  cls: 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20' },
  REVIEW:     { label: 'Under Review',cls: 'bg-amber-500/10 text-amber-400 border-amber-500/20' },
};

const StatusBadge = ({ status }) => {
  const cfg = STATUS_MAP[status] || { label: status, cls: 'bg-slate-700/50 text-slate-400 border-slate-600' };
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-semibold border ${cfg.cls} whitespace-nowrap`}>
      <span className="w-1.5 h-1.5 rounded-full mr-1.5 bg-current opacity-70" />
      {cfg.label}
    </span>
  );
};

export default StatusBadge;
