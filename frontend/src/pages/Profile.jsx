import React, { useState, useEffect } from 'react';
import axiosInstance from '../api/axios';
import Loader from '../components/Loader';
import ErrorMessage from '../components/ErrorMessage';
import { User, Mail, Calendar, Shield, Phone, Activity } from 'lucide-react';

const Profile = () => {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchProfile = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await axiosInstance.get('/users/profile');
      setProfile(response.data);
    } catch (err) {
      setError('Failed to load profile. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
  }, []);

  if (loading) return <Loader fullScreen message="Loading profile..." />;
  if (error) return <ErrorMessage message={error} onRetry={fetchProfile} />;
  if (!profile) return <ErrorMessage message="Profile not found" />;

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="bg-slate-800 rounded-2xl border border-slate-700 overflow-hidden shadow-xl">
        <div className="h-32 bg-gradient-to-r from-blue-600 to-indigo-600 relative">
          <div className="absolute -bottom-12 left-8">
            <div className="w-24 h-24 bg-slate-900 rounded-full border-4 border-slate-800 flex items-center justify-center">
              <User className="w-10 h-10 text-slate-300" />
            </div>
          </div>
        </div>
        
        <div className="pt-16 px-8 pb-8">
          <div className="flex justify-between items-start">
            <div>
              <h1 className="text-2xl font-bold text-white">{profile.username || profile.name || 'User'}</h1>
              <p className="text-slate-400 mt-1 flex items-center gap-2">
                <Shield className="w-4 h-4 text-blue-400" />
                {profile.userRole}
              </p>
            </div>
            <div className="px-3 py-1 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 rounded-full text-xs font-semibold tracking-wide">
              {profile.userStatus || 'ACTIVE'}
            </div>
          </div>

          <div className="mt-8 grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="p-4 bg-slate-900/50 rounded-xl border border-slate-700/50 flex gap-4 items-center">
              <div className="p-3 bg-blue-500/10 rounded-lg text-blue-400">
                <Mail className="w-5 h-5" />
              </div>
              <div>
                <p className="text-xs font-medium text-slate-400">Email Address</p>
                <p className="text-sm font-semibold text-slate-200 mt-0.5">{profile.email}</p>
              </div>
            </div>
            
            <div className="p-4 bg-slate-900/50 rounded-xl border border-slate-700/50 flex gap-4 items-center">
              <div className="p-3 bg-indigo-500/10 rounded-lg text-indigo-400">
                <Phone className="w-5 h-5" />
              </div>
              <div>
                <p className="text-xs font-medium text-slate-400">Mobile Number</p>
                <p className="text-sm font-semibold text-slate-200 mt-0.5">{profile.mobile || 'Not provided'}</p>
              </div>
            </div>
            
            <div className="p-4 bg-slate-900/50 rounded-xl border border-slate-700/50 flex gap-4 items-center">
              <div className="p-3 bg-purple-500/10 rounded-lg text-purple-400">
                <Calendar className="w-5 h-5" />
              </div>
              <div>
                <p className="text-xs font-medium text-slate-400">Member Since</p>
                <p className="text-sm font-semibold text-slate-200 mt-0.5">
                  {profile.createdAt ? new Date(profile.createdAt).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' }) : 'Unknown'}
                </p>
              </div>
            </div>
            
            <div className="p-4 bg-slate-900/50 rounded-xl border border-slate-700/50 flex gap-4 items-center">
              <div className="p-3 bg-teal-500/10 rounded-lg text-teal-400">
                <Activity className="w-5 h-5" />
              </div>
              <div>
                <p className="text-xs font-medium text-slate-400">Account ID</p>
                <p className="text-sm font-semibold text-slate-200 mt-0.5">#{profile.id}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Profile;
