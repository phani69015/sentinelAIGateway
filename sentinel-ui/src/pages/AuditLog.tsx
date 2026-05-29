import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldCheck, AlertTriangle, ShieldX, ExternalLink } from 'lucide-react';
import { getAuditRecords } from '../services/api';
import type { AuditRecord } from '../types';

export default function AuditLog() {
  const [records, setRecords] = useState<AuditRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<'ALL' | 'PASS' | 'WARN' | 'BLOCK'>('ALL');
  const navigate = useNavigate();

  useEffect(() => {
    getAuditRecords()
      .then(setRecords)
      .finally(() => setLoading(false));
  }, []);

  const filteredRecords = filter === 'ALL'
    ? records
    : records.filter((r) => r.verdict === filter);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Audit Log</h2>
          <p className="text-gray-500 mt-1">Complete audit trail for compliance review</p>
        </div>
        <div className="text-sm text-gray-500">
          {filteredRecords.length} record{filteredRecords.length !== 1 ? 's' : ''}
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="flex gap-2">
        {(['ALL', 'PASS', 'WARN', 'BLOCK'] as const).map((v) => (
          <button
            key={v}
            onClick={() => setFilter(v)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              filter === v
                ? 'bg-blue-600 text-white'
                : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'
            }`}
          >
            {v === 'ALL' ? 'All' : v}
            {v !== 'ALL' && (
              <span className="ml-2 text-xs opacity-70">
                ({records.filter((r) => r.verdict === v).length})
              </span>
            )}
          </button>
        ))}
      </div>

      {/* Records Table */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase">Verdict</th>
              <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase">Query</th>
              <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase">Confidence</th>
              <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase">Latency</th>
              <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase">Time</th>
              <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {filteredRecords.map((record) => (
              <tr
                key={record.id}
                className="hover:bg-gray-50 cursor-pointer transition-colors"
                onClick={() => navigate(`/audit/${record.id}`)}
              >
                <td className="px-6 py-4">
                  <VerdictBadge verdict={record.verdict} />
                </td>
                <td className="px-6 py-4">
                  <p className="text-sm text-gray-900 truncate max-w-xs">
                    {record.query}
                  </p>
                </td>
                <td className="px-6 py-4">
                  <div className="flex items-center gap-2">
                    <div className="w-16 h-2 bg-gray-200 rounded-full overflow-hidden">
                      <div
                        className={`h-full rounded-full ${confidenceColor(record.auditDecision.confidence)}`}
                        style={{ width: `${record.auditDecision.confidence * 100}%` }}
                      ></div>
                    </div>
                    <span className="text-xs text-gray-500">
                      {(record.auditDecision.confidence * 100).toFixed(0)}%
                    </span>
                  </div>
                </td>
                <td className="px-6 py-4 text-sm text-gray-500">{record.latencyMs}ms</td>
                <td className="px-6 py-4 text-sm text-gray-500">
                  {formatTime(record.timestamp)}
                </td>
                <td className="px-6 py-4">
                  <ExternalLink className="w-4 h-4 text-gray-400" />
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {filteredRecords.length === 0 && (
          <div className="p-12 text-center text-gray-500 text-sm">
            No audit records found for this filter.
          </div>
        )}
      </div>
    </div>
  );
}

function VerdictBadge({ verdict }: { verdict: string }) {
  const config = {
    PASS: { icon: ShieldCheck, bg: 'bg-emerald-50', text: 'text-emerald-700', border: 'border-emerald-200' },
    WARN: { icon: AlertTriangle, bg: 'bg-amber-50', text: 'text-amber-700', border: 'border-amber-200' },
    BLOCK: { icon: ShieldX, bg: 'bg-red-50', text: 'text-red-700', border: 'border-red-200' },
  }[verdict] || { icon: ShieldCheck, bg: 'bg-gray-50', text: 'text-gray-700', border: 'border-gray-200' };

  const Icon = config.icon;

  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border ${config.bg} ${config.text} ${config.border}`}>
      <Icon className="w-3.5 h-3.5" />
      {verdict}
    </span>
  );
}

function confidenceColor(confidence: number) {
  if (confidence >= 0.8) return 'bg-emerald-500';
  if (confidence >= 0.5) return 'bg-amber-500';
  return 'bg-red-500';
}

function formatTime(timestamp: string) {
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now.getTime() - date.getTime();

  if (diff < 60000) return 'Just now';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`;
  return date.toLocaleDateString();
}
