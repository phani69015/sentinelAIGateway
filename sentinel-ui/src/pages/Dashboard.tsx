import { useEffect, useState } from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, LineChart, Line } from 'recharts';
import { ShieldCheck, AlertTriangle, ShieldX, Activity, Zap, Lock, Brain, Layers } from 'lucide-react';
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

  const trendData = [
    { day: 'W1', hallucinations: 12, compliance: 8, consistency: 95 },
    { day: 'W2', hallucinations: 9, compliance: 6, consistency: 96 },
    { day: 'W3', hallucinations: 7, compliance: 5, consistency: 97 },
    { day: 'W4', hallucinations: 4, compliance: 3, consistency: 98 },
  ];

  const passRate = ((stats.passed / stats.total) * 100).toFixed(1);
  const blockRate = ((stats.blocked / stats.total) * 100).toFixed(1);

  return (
    <div className="space-y-8">
      {/* Product Context Hero */}
      <div className="bg-gradient-to-r from-blue-600 to-indigo-700 rounded-2xl p-8 text-white">
        <div className="flex items-start justify-between">
          <div className="max-w-2xl">
            <div className="flex items-center gap-2 mb-3">
              <div className="px-2.5 py-0.5 bg-white/20 rounded-full text-xs font-medium">
                Level 3: Enterprise-Grade Reliability
              </div>
            </div>
            <h1 className="text-3xl font-bold">Sentinel AI Gateway</h1>
            <p className="mt-3 text-blue-100 leading-relaxed">
              Self-auditing LLM proxy for regulated financial institutions. Every customer query is validated 
              by two independent LLMs and cross-checked by an Audit Agent before delivery — ensuring zero 
              hallucinated data reaches customers while maintaining SEC/FINRA compliance.
            </p>
            <div className="flex items-center gap-6 mt-6">
              <div className="flex items-center gap-2">
                <Brain className="w-4 h-4 text-blue-200" />
                <span className="text-sm text-blue-100">Triple-Agent Architecture</span>
              </div>
              <div className="flex items-center gap-2">
                <Lock className="w-4 h-4 text-blue-200" />
                <span className="text-sm text-blue-100">Immutable Audit Trail</span>
              </div>
              <div className="flex items-center gap-2">
                <Zap className="w-4 h-4 text-blue-200" />
                <span className="text-sm text-blue-100">~2.5s Latency</span>
              </div>
            </div>
          </div>
          <div className="hidden lg:block">
            <PipelineDiagram />
          </div>
        </div>
      </div>

      {/* Architecture Overview */}
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">How It Works</h3>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <ArchStep
            step={1}
            title="Customer Query"
            description="Financial question received via API gateway"
            icon={<Activity className="w-5 h-5 text-blue-600" />}
          />
          <ArchStep
            step={2}
            title="Parallel LLM Calls"
            description="OpenAI + Anthropic answer independently, grounded by knowledge base context"
            icon={<Layers className="w-5 h-5 text-purple-600" />}
          />
          <ArchStep
            step={3}
            title="Audit Agent"
            description="Cross-validates against context, checks consistency, compliance, toxicity"
            icon={<Brain className="w-5 h-5 text-indigo-600" />}
          />
          <ArchStep
            step={4}
            title="Verdict & Deliver"
            description="PASS/WARN/BLOCK decision with full audit trail persisted"
            icon={<ShieldCheck className="w-5 h-5 text-emerald-600" />}
          />
        </div>

        {/* Context Role */}
        <div className="mt-5 bg-gray-50 rounded-lg p-4 border border-gray-100">
          <p className="text-sm text-gray-700">
            <span className="font-semibold">Role of Context:</span> Each query can carry a <code className="text-xs bg-gray-200 px-1.5 py-0.5 rounded">knowledgeBase</code> — verified 
            facts from your product database (rates, fees, terms). LLMs are constrained to this data, and the Audit Agent 
            verifies claims against it. If an LLM invents information not in the context, it's flagged as a hallucination.
          </p>
        </div>
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
          subtitle={`${blockRate}% block rate`}
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
                <Bar dataKey="pass" fill={COLORS.PASS} stackId="a" radius={[0, 0, 0, 0]} name="Passed" />
                <Bar dataKey="warn" fill={COLORS.WARN} stackId="a" name="Warned" />
                <Bar dataKey="block" fill={COLORS.BLOCK} stackId="a" radius={[4, 4, 0, 0]} name="Blocked" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Trend + Compliance */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Risk Trend */}
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-2">Risk Trend (4 Weeks)</h3>
          <p className="text-sm text-gray-500 mb-4">Hallucination and compliance flags trending down</p>
          <div className="h-52">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={trendData}>
                <XAxis dataKey="day" />
                <YAxis />
                <Tooltip />
                <Line type="monotone" dataKey="hallucinations" stroke="#ef4444" strokeWidth={2} name="Hallucination Flags" />
                <Line type="monotone" dataKey="compliance" stroke="#f59e0b" strokeWidth={2} name="Compliance Violations" />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Compliance Rules */}
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-2">SEC/FINRA Compliance Rules</h3>
          <p className="text-sm text-gray-500 mb-4">Active rule checks per query</p>
          <div className="space-y-3">
            <ComplianceRule name="No Guaranteed Returns" severity="Critical" triggered={12} />
            <ComplianceRule name="Suitability Disclaimer" severity="High" triggered={34} />
            <ComplianceRule name="Fair & Balanced Disclosure" severity="Medium" triggered={18} />
            <ComplianceRule name="No Unauthorized Tax Advice" severity="Critical" triggered={8} />
            <ComplianceRule name="Anti-Discrimination" severity="Critical" triggered={2} />
            <ComplianceRule name="Factual Accuracy" severity="High" triggered={27} />
          </div>
        </div>
      </div>

      {/* System Health */}
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">System Health</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <HealthIndicator name="OpenAI GPT-4o" status="healthy" latency="~1.2s" />
          <HealthIndicator name="Anthropic Claude Sonnet" status="healthy" latency="~1.5s" />
          <HealthIndicator name="PostgreSQL (Audit Store)" status="healthy" latency="~3ms" />
        </div>
      </div>

      {/* Tech Stack */}
      <div className="bg-gray-900 rounded-xl p-6 text-white">
        <h3 className="text-lg font-semibold mb-4">Tech Stack</h3>
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-4">
          <TechBadge name="Java 25" detail="Virtual Threads" />
          <TechBadge name="Spring Boot 4" detail="WebMVC" />
          <TechBadge name="PostgreSQL 16" detail="Immutable Audit" />
          <TechBadge name="OpenAI" detail="GPT-4o" />
          <TechBadge name="Anthropic" detail="Claude Sonnet" />
        </div>
      </div>
    </div>
  );
}

function PipelineDiagram() {
  return (
    <div className="flex items-center gap-2 text-xs font-mono bg-white/10 rounded-lg p-4">
      <div className="text-center">
        <div className="bg-white/20 rounded px-2 py-1">Query</div>
      </div>
      <div className="text-blue-200">-&gt;</div>
      <div className="text-center space-y-1">
        <div className="bg-white/20 rounded px-2 py-1">OpenAI</div>
        <div className="bg-white/20 rounded px-2 py-1">Anthropic</div>
      </div>
      <div className="text-blue-200">-&gt;</div>
      <div className="text-center">
        <div className="bg-white/20 rounded px-2 py-1">Audit Agent</div>
      </div>
      <div className="text-blue-200">-&gt;</div>
      <div className="text-center space-y-1">
        <div className="bg-emerald-400/30 rounded px-2 py-1">PASS</div>
        <div className="bg-amber-400/30 rounded px-2 py-1">WARN</div>
        <div className="bg-red-400/30 rounded px-2 py-1">BLOCK</div>
      </div>
    </div>
  );
}

function ArchStep({ step, title, description, icon }: {
  step: number;
  title: string;
  description: string;
  icon: React.ReactNode;
}) {
  return (
    <div className="relative flex flex-col items-center text-center p-4">
      {step < 4 && (
        <div className="hidden md:block absolute top-10 -right-2 text-gray-300 text-xl">
          &rarr;
        </div>
      )}
      <div className="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center mb-3">
        {icon}
      </div>
      <div className="text-xs text-gray-400 font-medium mb-1">Step {step}</div>
      <h4 className="text-sm font-semibold text-gray-900">{title}</h4>
      <p className="text-xs text-gray-500 mt-1">{description}</p>
    </div>
  );
}

function ComplianceRule({ name, severity, triggered }: {
  name: string;
  severity: 'Critical' | 'High' | 'Medium';
  triggered: number;
}) {
  const severityColors = {
    Critical: 'bg-red-100 text-red-700',
    High: 'bg-amber-100 text-amber-700',
    Medium: 'bg-blue-100 text-blue-700',
  };

  return (
    <div className="flex items-center justify-between py-2 border-b border-gray-100 last:border-0">
      <div className="flex items-center gap-3">
        <span className={`px-2 py-0.5 rounded text-xs font-medium ${severityColors[severity]}`}>
          {severity}
        </span>
        <span className="text-sm text-gray-700">{name}</span>
      </div>
      <span className="text-sm text-gray-500">{triggered} triggers</span>
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

function TechBadge({ name, detail }: { name: string; detail: string }) {
  return (
    <div className="bg-gray-800 rounded-lg p-3 text-center">
      <p className="text-sm font-semibold text-white">{name}</p>
      <p className="text-xs text-gray-400 mt-0.5">{detail}</p>
    </div>
  );
}
