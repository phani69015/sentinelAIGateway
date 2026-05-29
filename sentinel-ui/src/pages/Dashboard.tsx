import { useEffect, useState } from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip } from 'recharts';
import { ShieldCheck, AlertTriangle, ShieldX, Activity } from 'lucide-react';
import { getAuditStats } from '../services/api';
import type { AuditStats } from '../types';

const COLORS = { PASS: '#10b981', WARN: '#f59e0b', BLOCK: '#ef4444' };

export default function Dashboard() {
  const [stats, setStats] = useState<AuditStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getAuditStats()
      .then(setStats)
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full"></div>
      </div>
    );
  }

  if (!stats) return null;

  const pieData = [
    { name: 'Passed', value: stats.passed, color: COLORS.PASS },
    { name: 'Warned', value: stats.warned, color: COLORS.WARN },
    { name: 'Blocked', value: stats.blocked, color: COLORS.BLOCK },
  ];

  const barData = [
    { name: 'Mon', pass: 89, warn: 23, block: 5 },
    { name: 'Tue', pass: 95, warn: 31, block: 8 },
    { name: 'Wed', pass: 87, warn: 19, block: 4 },
    { name: 'Thu', pass: 102, warn: 27, block: 7 },
    { name: 'Fri', pass: 78, warn: 22, block: 6 },
    { name: 'Sat', pass: 45, warn: 12, block: 3 },
    { name: 'Sun', pass: 38, warn: 9, block: 2 },
  ];

  const passRate = ((stats.passed / stats.total) * 100).toFixed(1);

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-2xl font-bold text-gray-900">Dashboard</h2>
        <p className="text-gray-500 mt-1">Real-time audit pipeline monitoring</p>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <StatCard
          title="Total Queries"
          value={stats.total.toLocaleString()}
          icon={<Activity className="w-5 h-5 text-blue-600" />}
          color="blue"
        />
        <StatCard
          title="Passed"
          value={stats.passed.toLocaleString()}
          subtitle={`${passRate}% pass rate`}
          icon={<ShieldCheck className="w-5 h-5 text-emerald-600" />}
          color="emerald"
        />
        <StatCard
          title="Warned"
          value={stats.warned.toLocaleString()}
          subtitle="Flagged for review"
          icon={<AlertTriangle className="w-5 h-5 text-amber-600" />}
          color="amber"
        />
        <StatCard
          title="Blocked"
          value={stats.blocked.toLocaleString()}
          subtitle="Prevented from delivery"
          icon={<ShieldX className="w-5 h-5 text-red-600" />}
          color="red"
        />
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Pie Chart */}
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Verdict Distribution</h3>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={pieData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={100}
                  dataKey="value"
                  label={({ name, percent }: { name?: string; percent?: number }) => `${name || ''} ${((percent || 0) * 100).toFixed(0)}%`}
                >
                  {pieData.map((entry, index) => (
                    <Cell key={index} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Bar Chart */}
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Weekly Activity</h3>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={barData}>
                <XAxis dataKey="name" />
                <YAxis />
                <Tooltip />
                <Bar dataKey="pass" fill={COLORS.PASS} stackId="a" radius={[0, 0, 0, 0]} />
                <Bar dataKey="warn" fill={COLORS.WARN} stackId="a" />
                <Bar dataKey="block" fill={COLORS.BLOCK} stackId="a" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* System Health */}
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">System Health</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <HealthIndicator name="OpenAI Provider" status="healthy" latency="~1.2s" />
          <HealthIndicator name="Anthropic Provider" status="healthy" latency="~1.5s" />
          <HealthIndicator name="PostgreSQL" status="healthy" latency="~3ms" />
        </div>
      </div>
    </div>
  );
}

function StatCard({ title, value, subtitle, icon, color }: {
  title: string;
  value: string;
  subtitle?: string;
  icon: React.ReactNode;
  color: string;
}) {
  return (
    <div className="bg-white rounded-xl border border-gray-200 p-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-gray-500">{title}</p>
          <p className="text-3xl font-bold text-gray-900 mt-1">{value}</p>
          {subtitle && <p className={`text-sm text-${color}-600 mt-1`}>{subtitle}</p>}
        </div>
        <div className={`w-12 h-12 bg-${color}-50 rounded-lg flex items-center justify-center`}>
          {icon}
        </div>
      </div>
    </div>
  );
}

function HealthIndicator({ name, status, latency }: {
  name: string;
  status: 'healthy' | 'degraded' | 'down';
  latency: string;
}) {
  const statusColors = {
    healthy: 'bg-green-500',
    degraded: 'bg-yellow-500',
    down: 'bg-red-500',
  };

  return (
    <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
      <div className="flex items-center gap-3">
        <span className={`w-3 h-3 rounded-full ${statusColors[status]}`}></span>
        <span className="text-sm font-medium text-gray-700">{name}</span>
      </div>
      <span className="text-sm text-gray-500">{latency}</span>
    </div>
  );
}
