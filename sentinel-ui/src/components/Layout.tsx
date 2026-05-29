import { Outlet, NavLink } from 'react-router-dom';
import { Shield, LayoutDashboard, MessageSquare, FileText } from 'lucide-react';

export default function Layout() {
  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-colors ${
      isActive
        ? 'bg-blue-50 text-blue-700 border border-blue-200'
        : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
    }`;

  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* Sidebar */}
      <aside className="w-64 bg-white border-r border-gray-200 flex flex-col">
        <div className="p-6 border-b border-gray-200">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-blue-600 rounded-lg flex items-center justify-center">
              <Shield className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="text-lg font-bold text-gray-900">Sentinel AI</h1>
              <p className="text-xs text-gray-500">Audit Gateway</p>
            </div>
          </div>
        </div>

        <nav className="flex-1 p-4 space-y-1">
          <NavLink to="/" className={linkClass} end>
            <LayoutDashboard className="w-5 h-5" />
            Dashboard
          </NavLink>
          <NavLink to="/query" className={linkClass}>
            <MessageSquare className="w-5 h-5" />
            Query Tester
          </NavLink>
          <NavLink to="/audit" className={linkClass}>
            <FileText className="w-5 h-5" />
            Audit Log
          </NavLink>
        </nav>

        <div className="p-4 border-t border-gray-200">
          <div className="bg-gray-50 rounded-lg p-3">
            <p className="text-xs font-medium text-gray-500 uppercase">Status</p>
            <div className="flex items-center gap-2 mt-1">
              <span className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></span>
              <span className="text-sm text-gray-700">System Operational</span>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 overflow-auto">
        <div className="p-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
