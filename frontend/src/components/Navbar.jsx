import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Bell, Settings, Check, Clock } from 'lucide-react';
import { getNotifications, markNotificationsAsRead } from '../api/notification.service';

const Navbar = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState([]);
  const [showDropdown, setShowDropdown] = useState(false);
  const dropdownRef = useRef(null);

  const unreadCount = notifications.filter(n => !n.read).length;

  useEffect(() => {
    const fetchNotifs = async () => {
      try {
        const data = await getNotifications();
        setNotifications(data);
      } catch (err) {
        console.error('Failed to load notifications');
      }
    };
    if (user) {
      fetchNotifs();
      // Optional: Polling every 30s
      const interval = setInterval(fetchNotifs, 30000);
      return () => clearInterval(interval);
    }
  }, [user]);

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleOpenDropdown = () => {
    setShowDropdown(!showDropdown);
    if (!showDropdown && unreadCount > 0) {
      markNotificationsAsRead().then(() => {
        setNotifications(prev => prev.map(n => ({ ...n, read: true })));
      });
    }
  };

  const handleNotifClick = (notif) => {
    setShowDropdown(false);
    if (notif.targetUrl) {
      navigate(notif.targetUrl);
    }
  };

  return (
    <header className="h-14 flex-shrink-0 bg-slate-950 border-b border-slate-800 flex items-center px-6 gap-4 relative z-50">
      {/* Left */}
      <div className="flex-1 min-w-0">
        <p className="text-slate-400 text-sm">
          Welcome back,{' '}
          <span className="text-white font-semibold">{user?.name || user?.username || 'User'}</span>
        </p>
      </div>

      {/* Right */}
      <div className="flex items-center gap-2 flex-shrink-0">
        <span className="hidden sm:inline-flex items-center text-[11px] font-semibold text-blue-300 bg-blue-500/10 border border-blue-500/20 px-2.5 py-1 rounded-full">
          {user?.userRole || 'CUSTOMER'}
        </span>
        
        <div className="relative" ref={dropdownRef}>
          <button 
            onClick={handleOpenDropdown}
            className="relative w-8 h-8 flex items-center justify-center rounded-lg text-slate-400 hover:bg-slate-800 hover:text-slate-200 transition-colors"
          >
            <Bell size={17} />
            {unreadCount > 0 && (
              <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-red-500 border border-slate-950 rounded-full animate-pulse" />
            )}
          </button>

          {/* Dropdown */}
          {showDropdown && (
            <div className="absolute right-0 mt-2 w-80 bg-slate-800 border border-slate-700 rounded-xl shadow-2xl overflow-hidden animate-[slideIn_0.2s_ease-out]">
              <div className="p-3 border-b border-slate-700 flex justify-between items-center bg-slate-900/50">
                <span className="text-sm font-bold text-slate-200">Notifications</span>
                {unreadCount > 0 && (
                  <span className="text-[10px] font-bold px-2 py-0.5 bg-blue-500/20 text-blue-400 rounded-full">
                    {unreadCount} New
                  </span>
                )}
              </div>
              <div className="max-h-80 overflow-y-auto scrollbar-thin scrollbar-thumb-slate-600">
                {notifications.length === 0 ? (
                  <div className="p-6 text-center text-slate-500 text-sm">No notifications</div>
                ) : (
                  notifications.map((n) => (
                    <div 
                      key={n.id} 
                      onClick={() => handleNotifClick(n)}
                      className={`p-4 border-b border-slate-700/50 hover:bg-slate-700/50 cursor-pointer transition-colors ${!n.read ? 'bg-blue-500/5' : ''}`}
                    >
                      <div className="flex gap-3">
                        <div className={`mt-0.5 p-1.5 rounded-full ${n.title.toLowerCase().includes('reject') ? 'bg-red-500/10 text-red-400' : n.title.toLowerCase().includes('approve') ? 'bg-emerald-500/10 text-emerald-400' : 'bg-blue-500/10 text-blue-400'}`}>
                          {n.title.toLowerCase().includes('approve') ? <Check className="w-3.5 h-3.5" /> : <Bell className="w-3.5 h-3.5" />}
                        </div>
                        <div>
                          <p className={`text-sm ${!n.read ? 'font-bold text-slate-200' : 'font-medium text-slate-300'}`}>{n.title}</p>
                          <p className="text-xs text-slate-400 mt-1 line-clamp-2">{n.message}</p>
                          <div className="flex items-center gap-1 mt-2 text-[10px] text-slate-500 font-medium">
                            <Clock className="w-3 h-3" />
                            {new Date(n.createdAt).toLocaleString()}
                          </div>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </div>

        <button
          id="navbar-settings"
          onClick={() => navigate('/change-password')}
          title="Change Password"
          className="w-8 h-8 flex items-center justify-center rounded-lg text-slate-400 hover:bg-slate-800 hover:text-slate-200 transition-colors"
        >
          <Settings size={17} />
        </button>
      </div>
    </header>
  );
};

export default Navbar;
