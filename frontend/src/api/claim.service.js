import axiosInstance from './axios';

export const getClaims = async (params = {}) => {
  const response = await axiosInstance.get('/claims/search', { params });
  return response.data;
};

export const getClaimById = async (id) => {
  const response = await axiosInstance.get(`/claims/${id}`);
  return response.data;
};

export const createClaim = async (claimData) => {
  const response = await axiosInstance.post('/claims', claimData);
  return response.data;
};

export const uploadClaimDocument = async (claimId, documentType, file) => {
  const formData = new FormData();
  formData.append('claimId', claimId);
  formData.append('documentType', documentType);
  formData.append('file', file);

  const response = await axiosInstance.post('/files/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

export const uploadMultipleDocuments = async (claimId, files) => {
  const formData = new FormData();
  formData.append('claimId', claimId);
  files.forEach(file => {
    formData.append('files', file);
  });

  const response = await axiosInstance.post('/files/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

export const getClaimDocuments = async (claimId) => {
  const response = await axiosInstance.get(`/files/claim/${claimId}`);
  return response.data;
};

export const getClaimAudits = async (claimId) => {
  const response = await axiosInstance.get(`/claims/${claimId}/audits`);
  return response.data;
};

export const analyzeClaimAI = async (claimId, prompt) => {
  const response = await axiosInstance.post(`/ai/analyze/${claimId}`, { prompt });
  return response.data;
};

export const validateClaimAI = async (payload) => {
  const response = await axiosInstance.post('/ai/validate-claim', payload);
  return response.data;
};

export const validateDocumentAI = async (file, documentType) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('documentType', documentType);
  const response = await axiosInstance.post('/ai/validate-document', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return response.data;
};

export const exportClaimReport = async (id) => {
  const response = await axiosInstance.get(`/claims/${id}/export`, {
    responseType: 'blob', // Important for downloading files
  });
  return response.data;
};

export const generateClaimSummary = async (claimId) => {
  const response = await axiosInstance.post(`/ai/claims/${claimId}/generate-summary`);
  return response.data;
};

export const getAdminFraudDashboard = async () => {
  const response = await axiosInstance.get('/fraud/admin/dashboard');
  return response.data;
};

export const getCarrierFraudDashboard = async () => {
  const response = await axiosInstance.get('/fraud/carrier/dashboard');
  return response.data;
};

export const markClaimAsSafe = async (claimId) => {
  const response = await axiosInstance.patch(`/fraud/admin/claims/${claimId}/safe`);
  return response.data;
};

// ── Payment APIs ──────────────────────────────────────────────────────────────

export const createPaymentOrder = async (claimId, amount) => {
  const response = await axiosInstance.post('/payments/create-order', { claimId, amount });
  return response.data;
};

export const verifyPayment = async (payload) => {
  const response = await axiosInstance.post('/payments/verify', payload);
  return response.data;
};

export const getPaymentForClaim = async (claimId) => {
  const response = await axiosInstance.get(`/payments/claim/${claimId}`);
  return response.data;
};

// ── Timeline APIs ─────────────────────────────────────────────────────────────

export const getClaimTimeline = async (claimId) => {
  const response = await axiosInstance.get(`/claims/${claimId}/timeline`);
  return response.data;
};
