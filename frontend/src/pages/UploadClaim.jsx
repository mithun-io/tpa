import React, { useState } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { createClaim, uploadMultipleDocuments, validateClaimAI } from '../api/claim.service';
import Loader from '../components/Loader';
import { UploadCloud, File, X, CheckCircle, Bot, ShieldCheck, ShieldAlert, ShieldX, AlertTriangle, XCircle } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

// ─── AI Pre-Validation Side Panel ──────────────────────────────────────────────
const AiPreValidationPanel = ({ result, validating }) => {
  if (validating) {
    return (
      <div className="bg-slate-800 rounded-xl border border-slate-700 p-6 flex flex-col items-center justify-center min-h-[300px] transition-all duration-300">
        <div className="animate-spin rounded-full h-10 w-10 border-4 border-slate-700 border-t-blue-500 mb-4" />
        <p className="text-blue-400 font-medium text-sm">Running AI pre-validation...</p>
      </div>
    );
  }

  if (!result) {
    return (
      <div className="bg-slate-800/50 rounded-xl border border-slate-700 border-dashed p-6 flex flex-col items-center justify-center min-h-[300px] text-center transition-all duration-300">
        <div className="bg-blue-500/10 border border-blue-500/20 p-3 rounded-xl mb-4 shadow-inner">
          <Bot className="w-7 h-7 text-blue-400" />
        </div>
        <p className="text-slate-300 font-bold text-sm mb-1">AI Pre-Validation</p>
        <p className="text-slate-500 text-xs mt-1 leading-relaxed">Fill in the form and click<br />"Validate with AI" for instant feedback</p>
      </div>
    );
  }

  const verdictConfig = {
    APPROVED: { icon: ShieldCheck, color: 'text-emerald-400', bg: 'bg-emerald-500/10', border: 'border-emerald-500/20', label: 'Looks Good' },
    REVIEW:   { icon: ShieldAlert, color: 'text-amber-400',   bg: 'bg-amber-500/10',   border: 'border-amber-500/20',   label: 'Needs Review' },
    REJECTED: { icon: ShieldX,    color: 'text-red-400',      bg: 'bg-red-500/10',     border: 'border-red-500/20',     label: 'Issues Found' },
  };
  const vc = verdictConfig[result.verdict] || verdictConfig.REVIEW;
  const VerdictIcon = vc.icon;
  const riskPct = Math.round((result.riskScore ?? 0.5) * 100);
  const riskBarColor = riskPct < 30 ? 'bg-emerald-500' : riskPct < 60 ? 'bg-amber-500' : 'bg-red-500';

  return (
    <div className="bg-slate-800 rounded-xl border border-slate-700 shadow-sm overflow-hidden transition-all duration-500 sticky top-6">
      <div className={`p-4 ${vc.bg} border-b ${vc.border} flex items-center gap-3`}>
        <VerdictIcon className={`w-6 h-6 ${vc.color}`} />
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider opacity-70 mb-0.5">AI Verdict</p>
          <p className={`font-bold text-base ${vc.color}`}>{vc.label}</p>
        </div>
        <span className={`ml-auto text-xs font-bold px-2.5 py-1 rounded-full ${vc.bg} ${vc.color} border ${vc.border}`}>
          {result.verdict}
        </span>
      </div>

      <div className="p-5 space-y-5">
        <div className="grid grid-cols-2 gap-3">
          <div className="bg-slate-900/50 border border-slate-700/50 rounded-xl p-3 text-center">
            <p className="text-slate-500 text-xs font-semibold uppercase tracking-wider mb-1">Confidence</p>
            <p className="text-slate-200 font-bold text-xl">{Math.round((result.confidence ?? 0.5) * 100)}%</p>
          </div>
          <div className="bg-slate-900/50 border border-slate-700/50 rounded-xl p-3 text-center">
            <p className="text-slate-500 text-xs font-semibold uppercase tracking-wider mb-1">Risk Score</p>
            <p className="text-slate-200 font-bold text-xl">{riskPct}/100</p>
          </div>
        </div>

        <div>
          <div className="flex justify-between text-[10px] font-bold text-slate-500 uppercase tracking-widest mb-2">
            <span>Low Risk</span><span>High Risk</span>
          </div>
          <div className="w-full bg-slate-700 rounded-full h-2">
            <div className={`${riskBarColor} h-2 rounded-full transition-all duration-700`} style={{ width: `${riskPct}%` }} />
          </div>
        </div>

        {result.validations && (
          <div className="pt-2 border-t border-slate-700/50 mt-4">
            <p className="text-[10px] font-bold text-slate-500 uppercase tracking-widest mb-3">Validation Checks</p>
            <div className="space-y-2">
              {[
                { key: 'policyActive', label: 'Policy Active' },
                { key: 'documentsComplete', label: 'Documents Complete' },
                { key: 'withinLimit', label: 'Within Claim Limit' },
              ].map(({ key, label }) => (
                <div key={key} className="flex items-center gap-2.5 text-sm">
                  {result.validations[key]
                    ? <CheckCircle className="w-4 h-4 text-emerald-500 flex-shrink-0" />
                    : <X className="w-4 h-4 text-red-400 flex-shrink-0" />}
                  <span className={result.validations[key] ? 'text-slate-300 font-medium' : 'text-slate-500 line-through'}>{label}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {result.flags && result.flags.length > 0 && (
          <div className="bg-amber-500/10 border border-amber-500/20 rounded-xl p-4">
            <div className="flex items-center gap-2 mb-2">
              <AlertTriangle className="w-4 h-4 text-amber-500" />
              <p className="text-xs font-bold text-amber-400 uppercase tracking-widest">Flags Detected</p>
            </div>
            <ul className="space-y-1.5 mt-2">
              {result.flags.map((flag, i) => (
                <li key={i} className="text-amber-200/80 text-xs flex items-start gap-2">
                  <span className="mt-0.5 text-amber-500">•</span>{flag}
                </li>
              ))}
            </ul>
          </div>
        )}

        {result.recommendation && (
          <div className="bg-blue-500/10 border border-blue-500/20 rounded-xl p-4">
            <p className="text-[10px] font-bold text-blue-400 uppercase tracking-widest mb-1.5">Recommendation</p>
            <p className="text-blue-100 text-sm leading-relaxed">{result.recommendation}</p>
          </div>
        )}
      </div>
    </div>
  );
};

// ─── Main Upload Claim Page ────────────────────────────────────────────────────
const UploadClaim = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  // Role check: Admin shouldn't access this
  if (user?.userRole === 'FMG_ADMIN') {
    return <Navigate to="/dashboard" replace />;
  }

  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const [validating, setValidating] = useState(false);
  const [createdClaimId, setCreatedClaimId] = useState(null);
  const [preValidationResult, setPreValidationResult] = useState(null);
  const [validationResults, setValidationResults] = useState(null);

  const [formData, setFormData] = useState({
    policyNumber: '', claimFormPresent: true, combinedDocumentPresent: true,
    claimFormPatientName: '', claimFormHospitalName: '',
    claimFormAdmissionDate: '', claimFormDischargeDate: '',
    claimedAmount: '', totalBillAmount: '', policyId: '',
    carrierName: '', policyName: '', claimType: 'REIMBURSEMENT',
    diagnosis: '', billNumber: '', billDate: ''
  });

  const [selectedFiles, setSelectedFiles] = useState([]);

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  };

  const handleValidateWithAI = async () => {
    if (!formData.policyNumber || !formData.claimedAmount) {
      toast.error('Please fill in Policy Number and Claimed Amount first');
      return;
    }
    try {
      setValidating(true);
      setPreValidationResult(null);
      const result = await validateClaimAI({
        policyNumber: formData.policyNumber,
        amount: parseFloat(formData.claimedAmount) || 0,
        hospitalName: formData.claimFormHospitalName,
        diagnosis: formData.diagnosis,
        patientName: formData.claimFormPatientName,
        admissionDate: formData.claimFormAdmissionDate,
        dischargeDate: formData.claimFormDischargeDate,
      });
      setPreValidationResult(result);
    } catch (err) {
      toast.error('AI pre-validation failed. Please try again.');
    } finally {
      setValidating(false);
    }
  };

  const handleCreateClaim = async (e) => {
    e.preventDefault();
    try {
      setLoading(true);
      const payload = {
        ...formData,
        claimedAmount: parseFloat(formData.claimedAmount),
        totalBillAmount: parseFloat(formData.totalBillAmount)
      };
      const response = await createClaim(payload);
      if (response && response.id) {
        setCreatedClaimId(response.id);
        setStep(2);
        toast.success('Claim created successfully. Now upload documents.');
      }
    } catch (error) {
      toast.error('Failed to create claim');
    } finally {
      setLoading(false);
    }
  };

  const handleFileChange = (e) => {
    const files = Array.from(e.target.files || []);
    setSelectedFiles(prev => [...prev, ...files]);
  };

  const handleRemoveFile = (index) => {
    setSelectedFiles(prev => prev.filter((_, i) => i !== index));
  };

  const handleFileUpload = async (e) => {
    e.preventDefault();
    
    if (selectedFiles.length === 0) {
      toast.error('Please select at least one file');
      return;
    }

    const hasPdf = selectedFiles.some(file => file.type === "application/pdf");
    if (!hasPdf) {
      toast.error("Please upload at least one PDF document");
      return;
    }
    
    if (validationResults) {
      navigate(`/claims/${createdClaimId}`);
      return;
    }

    try {
      setLoading(true);
      const response = await uploadMultipleDocuments(createdClaimId, selectedFiles);
      
      setValidationResults(response.documents.map(doc => ({
        type: doc.fileType === 'PDF' ? 'PDF Document' : 'Image Attachment',
        ...doc
      })));
      
      toast.success('Files uploaded and AI validation complete!');
    } catch (error) {
      toast.error('Failed to upload files');
    } finally {
      setLoading(false);
    }
  };

  const renderMultiFileUploader = () => (
    <div className="mt-6">
      <label className="block text-sm font-bold text-slate-300 mb-2.5">
        Upload Documents (At least one PDF mandatory)
      </label>
      <div className={`flex justify-center px-6 pt-8 pb-8 border-2 border-dashed rounded-xl transition-all duration-200 border-slate-700 bg-slate-900/50 hover:bg-slate-800 hover:border-slate-600`}>
        <div className="space-y-4 text-center w-full">
          <div className="w-14 h-14 bg-slate-800 border border-slate-700 rounded-full flex items-center justify-center mx-auto mb-2">
            <UploadCloud className="h-6 w-6 text-blue-400" />
          </div>
          <div className="flex text-sm text-slate-400 justify-center">
            <label className="relative cursor-pointer bg-transparent font-semibold text-blue-400 hover:text-blue-300 focus-within:outline-none transition-colors">
              <span>Select files</span>
              <input 
                type="file" 
                className="sr-only" 
                multiple 
                accept=".pdf,.jpg,.jpeg,.png"
                onChange={handleFileChange} 
              />
            </label>
            <p className="pl-1.5">to upload</p>
          </div>
          <p className="text-xs text-slate-500 font-medium tracking-wide">PDF (Mandatory), PNG, JPG up to 10MB</p>

          {selectedFiles.length > 0 && (
            <div className="mt-6 text-left space-y-2">
              <p className="text-[10px] font-bold text-slate-500 uppercase tracking-widest px-1">Selected Files ({selectedFiles.length})</p>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                {selectedFiles.map((file, idx) => (
                  <div key={idx} className="flex items-center justify-between bg-slate-800/80 border border-slate-700 p-2.5 rounded-lg group">
                    <div className="flex items-center min-w-0">
                      {file.type === 'application/pdf' ? (
                        <File className="w-4 h-4 text-red-400 flex-shrink-0 mr-2" />
                      ) : (
                        <CheckCircle className="w-4 h-4 text-blue-400 flex-shrink-0 mr-2" />
                      )}
                      <span className="text-xs text-slate-300 truncate font-medium">{file.name}</span>
                    </div>
                    <button 
                      type="button" 
                      onClick={() => handleRemoveFile(idx)}
                      className="text-slate-500 hover:text-red-400 p-1 rounded-md transition-colors"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );

  const inputClass = "w-full px-4 py-2.5 bg-slate-900 border border-slate-700 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none text-sm text-slate-200 placeholder-slate-500 transition-shadow";
  const labelClass = "block text-xs font-bold text-slate-400 uppercase tracking-wider mb-2";

  return (
    <div className="space-y-6 max-w-[1400px] mx-auto">
      <div className="flex justify-between items-end flex-wrap gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-100 tracking-tight">
            {step === 1 ? 'Create New Claim' : 'Upload Documents'}
          </h1>
          <p className="text-sm text-slate-400 mt-1">Submit your claim details and documentation</p>
        </div>
        <div className="text-xs font-bold uppercase tracking-widest text-blue-400 bg-blue-500/10 px-4 py-2 rounded-xl shadow-inner border border-blue-500/20">
          Step {step} of 2
        </div>
      </div>

      {step === 1 && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
          {/* ── Claim Form ── */}
          <div className="lg:col-span-2 bg-slate-800 rounded-2xl shadow-sm border border-slate-700 overflow-hidden">
            <form onSubmit={handleCreateClaim} className="p-8">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-7">
                <div>
                  <label className={labelClass}>Policy Number <span className="text-red-400">*</span></label>
                  <input required type="text" name="policyNumber" value={formData.policyNumber} onChange={handleInputChange} className={inputClass} placeholder="POL-US-2024-123456" />
                </div>
                <div>
                  <label className={labelClass}>Patient Name <span className="text-red-400">*</span></label>
                  <input required type="text" name="claimFormPatientName" value={formData.claimFormPatientName} onChange={handleInputChange} className={inputClass} placeholder="e.g. Jane Smith" />
                </div>
                <div className="md:col-span-2">
                  <label className={labelClass}>Hospital Name <span className="text-red-400">*</span></label>
                  <input required type="text" name="claimFormHospitalName" value={formData.claimFormHospitalName} onChange={handleInputChange} className={inputClass} placeholder="e.g. Mayo Clinic, Johns Hopkins" />
                </div>
                <div>
                  <label className={labelClass}>Admission Date <span className="text-red-400">*</span></label>
                  <input required type="date" name="claimFormAdmissionDate" value={formData.claimFormAdmissionDate} onChange={handleInputChange} className={inputClass} style={{colorScheme: 'dark'}} />
                </div>
                <div>
                  <label className={labelClass}>Discharge Date <span className="text-red-400">*</span></label>
                  <input required type="date" name="claimFormDischargeDate" value={formData.claimFormDischargeDate} onChange={handleInputChange} className={inputClass} style={{colorScheme: 'dark'}} />
                </div>
                <div>
                  <label className={labelClass}>Claimed Amount ($) <span className="text-red-400">*</span></label>
                  <input required type="number" step="0.01" name="claimedAmount" value={formData.claimedAmount} onChange={handleInputChange} className={inputClass} placeholder="12500.00" />
                </div>
                <div>
                  <label className={labelClass}>Total Bill Amount ($) <span className="text-red-400">*</span></label>
                  <input required type="number" step="0.01" name="totalBillAmount" value={formData.totalBillAmount} onChange={handleInputChange} className={inputClass} placeholder="15750.00" />
                </div>
                <div className="md:col-span-2">
                  <label className={labelClass}>Diagnosis / Reason</label>
                  <input type="text" name="diagnosis" value={formData.diagnosis} onChange={handleInputChange} className={inputClass} placeholder="e.g. Appendectomy, Hip Replacement, etc." />
                </div>
              </div>

              <div className="mt-10 pt-6 border-t border-slate-700 flex flex-col sm:flex-row justify-end gap-4">
                <button type="button" onClick={handleValidateWithAI} disabled={validating || loading}
                  className="flex items-center justify-center gap-2 px-6 py-3 rounded-xl border border-blue-500/30 text-blue-400 bg-blue-500/10 hover:bg-blue-500/20 font-bold text-sm transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed">
                  {validating
                    ? <><span className="animate-spin rounded-full h-4 w-4 border-2 border-blue-500/30 border-t-blue-400" />Validating...</>
                    : <><Bot className="w-5 h-5" />Validate with AI</>}
                </button>
                <button type="submit" disabled={loading || validating}
                  className="flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-8 py-3 rounded-xl font-bold text-sm transition-all disabled:opacity-50 shadow-lg shadow-blue-900/30">
                  {loading ? <Loader message="" /> : 'Next Step: Upload Documents →'}
                </button>
              </div>
            </form>
          </div>

          {/* ── AI Side Panel ── */}
          <div className="lg:col-span-1">
            <AiPreValidationPanel result={preValidationResult} validating={validating} />
          </div>
        </div>
      )}

      {step === 2 && (
        <div className="bg-slate-800 rounded-2xl shadow-sm border border-slate-700 overflow-hidden max-w-4xl mx-auto">
          <form onSubmit={handleFileUpload} className="p-10">
            <div className="bg-blue-500/10 p-5 rounded-xl mb-8 border border-blue-500/20 shadow-inner flex gap-4 items-start">
              <div className="bg-blue-500/20 p-2 rounded-lg">
                <CheckCircle className="w-5 h-5 text-blue-400" />
              </div>
              <div>
                <p className="text-blue-100 font-medium">
                  Claim <strong className="text-blue-400 font-bold">#{createdClaimId}</strong> created successfully.
                </p>
                <p className="text-blue-300/80 text-sm mt-1">Please upload the required supporting documents to proceed.</p>
              </div>
            </div>
            
            <div className="space-y-2">
              {renderMultiFileUploader()}
            </div>
            
            {validationResults && (
              <div className="mt-8 space-y-4">
                <h3 className="text-slate-200 font-bold flex items-center gap-2">
                  <Bot className="w-5 h-5 text-blue-400" /> AI Document Validation Results
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {validationResults.map((doc, idx) => {
                    const isInvalid = doc.validationStatus === 'INVALID';
                    const issues = doc.validationIssues ? JSON.parse(doc.validationIssues) : [];
                    return (
                      <div key={idx} className={`p-4 rounded-xl border ${isInvalid ? 'bg-red-500/10 border-red-500/20' : 'bg-emerald-500/10 border-emerald-500/20'}`}>
                        <div className="flex justify-between items-start mb-2">
                          <p className="font-bold text-slate-200 text-sm">{doc.type}</p>
                          <span className={`flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[10px] font-bold uppercase tracking-wider ${isInvalid ? 'bg-red-500/20 text-red-400 border border-red-500/30' : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'}`}>
                            {isInvalid ? <XCircle className="w-3.5 h-3.5" /> : <CheckCircle className="w-3.5 h-3.5" />}
                            {doc.validationStatus || 'UNKNOWN'}
                          </span>
                        </div>
                        
                        <div className="mb-4 bg-slate-950/50 rounded-lg p-3 border border-slate-700/50">
                           <div className="flex justify-between items-center mb-1.5">
                             <span className="text-xs text-slate-400 font-medium">AI Confidence Level</span>
                             <span className="text-sm font-bold text-slate-200">{doc.confidenceScore}%</span>
                           </div>
                           <div className="w-full bg-slate-800 rounded-full h-1.5 overflow-hidden">
                             <div className={`h-1.5 rounded-full ${doc.confidenceScore < 70 ? 'bg-amber-400' : 'bg-emerald-400'}`} style={{ width: `${doc.confidenceScore}%` }}></div>
                           </div>
                        </div>

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
                        {!isInvalid && (
                          <p className="text-xs text-emerald-300 flex items-center gap-1 mt-2">
                            <CheckCircle className="w-3.5 h-3.5" /> All checks passed
                          </p>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            <div className="mt-10 pt-8 border-t border-slate-700 flex justify-end space-x-4">
              <button type="button" onClick={() => navigate(`/claims/${createdClaimId}`)}
                className="bg-transparent border border-slate-600 hover:bg-slate-700 text-slate-300 px-6 py-3 rounded-xl font-bold text-sm transition-all">
                Skip for now
              </button>
              <button type="submit" disabled={loading || selectedFiles.length === 0}
                className="bg-blue-600 hover:bg-blue-700 text-white px-8 py-3 rounded-xl font-bold text-sm transition-all disabled:opacity-50 flex items-center shadow-lg shadow-blue-900/30">
                {loading ? <><span className="animate-spin rounded-full h-4 w-4 border-2 border-white/30 border-t-white mr-2" /> Uploading...</> : (validationResults ? 'Finish' : 'Upload & Validate')}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};

export default UploadClaim;
