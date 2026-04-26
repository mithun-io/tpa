import React from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

const ErrorMessage = ({ message = 'Something went wrong', onRetry }) => (
  <div className="flex flex-col items-center justify-center py-20 gap-4">
    <div className="w-14 h-14 rounded-2xl bg-red-500/10 border border-red-500/20 flex items-center justify-center">
      <AlertTriangle className="w-7 h-7 text-red-400" />
    </div>
    <div className="text-center">
      <p className="text-slate-200 font-semibold">{message}</p>
      <p className="text-slate-500 text-sm mt-1">Please try again</p>
    </div>
    {onRetry && (
      <button
        onClick={onRetry}
        className="flex items-center gap-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-sm font-medium border border-slate-700 transition-colors"
      >
        <RefreshCw className="w-4 h-4" /> Retry
      </button>
    )}
  </div>
);

export default ErrorMessage;
