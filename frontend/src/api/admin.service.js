import axiosInstance from './axios';

/* ── Claims ─────────────────────────────────────────── */
/**
 * Admin-specific claim listing via /admin/claims (role-gated, all claims).
 */
export const getAdminClaims = async (params) => {
  const response = await axiosInstance.get('/admin/claims', { params });
  return response.data; // Spring Page<ClaimResponse>
};

export const approveClaim = async (id, reason = 'Approved by admin') => {
  const response = await axiosInstance.put(
    `/admin/claims/${id}/approve`,
    null,
    { params: { reason } }
  );
  return response.data;
};

export const rejectClaim = async (id, reason) => {
  const response = await axiosInstance.put(
    `/admin/claims/${id}/reject`,
    null,
    { params: { reason } }
  );
  return response.data;
};

export const getClaimAiSummary = async (id) => {
  // Returns AiAnalysisResponse directly (not wrapped in ApiResponse)
  const response = await axiosInstance.get(`/admin/claims/${id}/ai-summary`);
  return response.data;
};

/* ── Users ──────────────────────────────────────────── */
export const getAllCustomers = async () => {
  const response = await axiosInstance.get('/admin/customers');
  return response.data;
};

export const getUsers = async (params) => {
  const response = await axiosInstance.get('/admin/users', { params });
  return response.data; // Spring Page<UserResponse>
};

export const blockUser = async (id) => {
  const response = await axiosInstance.patch(`/admin/users/${id}/block`);
  return response.data;
};

export const unblockUser = async (id) => {
  const response = await axiosInstance.patch(`/admin/users/${id}/unblock`);
  return response.data;
};

/* ── System ─────────────────────────────────────────── */
export const getSystemMonitoring = async () => {
  const response = await axiosInstance.get('/admin/monitoring');
  return response.data; // plain Map<String,Object>
};

/* ── Carriers ───────────────────────────────────────── */
export const getCarriers = async () => {
  const response = await axiosInstance.get('/admin/carriers');
  return response.data; // List<CarrierResponse>
};

export const approveCarrier = async (id) => {
  const response = await axiosInstance.patch(`/admin/carriers/${id}/approve`);
  return response.data;
};

export const rejectCarrier = async (id) => {
  const response = await axiosInstance.patch(`/admin/carriers/${id}/reject`);
  return response.data;
};

export const assignCarrierToClaim = async (claimId, carrierId) => {
  const response = await axiosInstance.patch(
    `/admin/claims/${claimId}/assign-carrier`,
    { carrierId }
  );
  return response.data;
};
