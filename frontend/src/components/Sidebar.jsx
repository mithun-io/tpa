import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  LayoutDashboard, FileText, UploadCloud,
  ShieldCheck, LogOut, ChevronRight, BarChart3, User, Truck, ShieldAlert
} from 'lucide-react';

const Sidebar = () => {
  const location = useLocation();
  const { user, logout } = useAuth();
  const isAdmin = user?.userRole === 'FMG_ADMIN';
  const isCarrier = user?.userRole === 'CARRIER_USER';
  const isCustomer = user?.userRole === 'CUSTOMER';

  const navItems = [
    ...(!isCarrier ? [{ name: 'Dashboard', path: '/dashboard', icon: LayoutDashboard }] : []),
    ...(isCarrier ? [
      { name: 'Carrier Portal', path: '/carrier', icon: Truck, admin: false },
      { name: 'Fraud Dashboard', path: '/carrier/fraud-dashboard', icon: ShieldAlert, admin: false }
    ] : []),
    ...(isCustomer ? [
      { name: 'My Claims',    path: '/claims',        icon: FileText },
      { name: 'Upload Claim', path: '/claims/upload', icon: UploadCloud },
    ] : []),
    { name: 'Profile',      path: '/profile',       icon: User },
    ...(isAdmin ? [
      { name: 'Admin Panel', path: '/admin', icon: ShieldCheck, admin: true },
      { name: 'Fraud Dashboard', path: '/admin/fraud-dashboard', icon: ShieldAlert, admin: true },
      { name: 'Analytics', path: '/analytics', icon: BarChart3, admin: true }
    ] : []),
  ];

  const isActive = (path) =>
    path === '/dashboard'
      ? location.pathname === '/dashboard'
      : location.pathname.startsWith(path);

  return (
    <aside className="w-64 flex-shrink-0 bg-slate-950 border-r border-slate-800 flex flex-col">
      {/* Brand */}
      <div className="px-6 py-5 border-b border-slate-800">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-blue-900/40 flex-shrink-0">
            <span className="text-white font-black text-base">T</span>
          </div>
          <div>
            <p className="text-white font-bold text-sm leading-tight">TPA ClaimSys</p>
            <p className="text-slate-500 text-xs">Insurance Platform</p>
          </div>
        </div>
      </div>

      {/* User card */}
      <div className="mx-4 mt-4 px-3 py-3 bg-slate-800/60 rounded-xl border border-slate-700/50 flex items-center gap-3">
        <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center flex-shrink-0">
          <span className="text-white font-bold text-xs">
            {(user?.name || user?.username || 'U')[0].toUpperCase()}
          </span>
        </div>
        <div className="min-w-0">
          <p className="text-slate-200 text-sm font-semibold truncate">
            {user?.name || user?.username}
          </p>
          <p className="text-slate-500 text-xs truncate">{user?.userRole}</p>
        </div>
      </div>

      {/* Nav */}
      <nav className="flex-1 px-3 mt-5 space-y-0.5">
        <p className="text-slate-600 text-[10px] font-semibold uppercase tracking-widest px-3 mb-2">
          Navigation
        </p>
        {navItems.map((item) => {
          const Icon = item.icon;
          const active = isActive(item.path);
          return (
            <Link
              key={item.name}
              to={item.path}
              className={`group flex items-center px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-150 ${
                active
                  ? 'bg-blue-600 text-white shadow-md shadow-blue-900/40'
                  : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
              }`}
            >
              <Icon className={`w-4.5 h-4.5 mr-3 flex-shrink-0 ${active ? 'text-white' : 'text-slate-500 group-hover:text-slate-300'}`} size={18} />
              <span className="flex-1">{item.name}</span>
              {active && <ChevronRight className="w-3.5 h-3.5 text-blue-200" />}
              {item.admin && !active && (
                <span className="text-[10px] bg-indigo-600/30 text-indigo-300 border border-indigo-700/50 px-1.5 py-0.5 rounded-md font-semibold">
                  Admin
                </span>
              )}
            </Link>
          );
        })}
      </nav>

      {/* Logout */}
      <div className="p-3 border-t border-slate-800">
        <button
          id="sidebar-logout"
          onClick={logout}
          className="w-full flex items-center px-3 py-2.5 rounded-xl text-sm font-medium text-slate-500 hover:bg-red-500/10 hover:text-red-400 transition-all duration-150 group"
        >
          <LogOut className="w-4 h-4 mr-3 group-hover:text-red-400" size={18} />
          Sign Out
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;
