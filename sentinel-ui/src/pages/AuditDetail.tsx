import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, ShieldCheck, AlertTriangle, ShieldX, Clock, Brain } from 'lucide-react';
import { getAuditRecord } from '../services/api';
import type { AuditRecord } from '../types';

export default function AuditDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [record, setRecord] = useState<AuditRecord | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (id) {
      getAuditRecord(id)
        .then((r) => setRecord(r || null))
        .finally(() => setLoading(false));
    }
  }, [id]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full"></div>
      </div>
    );
  }

  if (!record) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500">Audit record not found</p>
        <button onClick={() => navigate('/audit')} className="mt-4 text-blue-600 hover:underline">
          Back to Audit Log
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate('/audit')}
          className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
        >
          <ArrowLeft className="w-5 h-5 text-gray-600" />
        </button>
        <div className="flex-1">
          <h2 className="text-2xl font-bold text-gray-900">Audit Record</h2>
          <p className="text-sm text-gray-500 font-mono">{record.id}</p>
        </div>
        <VerdictBadgeLarge verdict={record.verdict} />
      </div>

      {/* Summary Bar */}
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
          <div>
            <p className="text-xs text-gray-500 uppercase font-medium">Confidence</p>
            <p className="text-2xl font-bold text-gray-900 mt-1">
              {(record.auditDecision.confidence * 100).toFixed(0)}%
            </p>
          </div>
          <div>
            <p className="text-xs text-gray-500 uppercase font-medium">Consistency</p>
            <p className="text-2xl font-bold text-gray-900 mt-1">
              {(record.auditDecision.consistencyScore * 100).toFixed(0)}%
            </p>
          </div>
          <div>
            <p className="text-xs text-gray-500 uppercase font-medium">Latency</p>
            <div className="flex items-center gap-1 mt-1">
              <Clock className="w-4 h-4 text-gray-400" />
              <p className="text-2xl font-bold text-gray-900">{record.latencyMs}ms</p>
            </div>
          </div>
          <div>
            <p className="text-xs text-gray-500 uppercase font-medium">Timestamp</p>
            <p className="text-sm font-medium text-gray-900 mt-2">
              {new Date(record.timestamp).toLocaleString()}
            </p>
          </div>
        </div>
      </div>

      {/* Query */}
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h3 className="text-sm font-semibold text-gray-500 uppercase mb-3">Customer Query</h3>
        <p className="text-gray-900 bg-gray-50 p-4 rounded-lg">{record.query}</p>
      </div>

      {/* LLM Responses Side by Side */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ResponseCard
          provider={record.responseA.provider}
          model={record.responseA.model}
          text={record.responseA.text}
          label="Response A"
          color="blue"
        />
        <ResponseCard
          provider={record.responseB.provider}
          model={record.responseB.model}
          text={record.responseB.text}
          label="Response B"
          color="purple"
        />
      </div>

      {/* Audit Decision */}
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <div className="flex items-center gap-2 mb-4">
          <Brain className="w-5 h-5 text-blue-600" />
          <h3 className="text-sm font-semibold text-gray-500 uppercase">Audit Agent Reasoning</h3>
        </div>
        <p className="text-gray-800 bg-blue-50 p-4 rounded-lg border border-blue-100 leading-relaxed">
          {record.auditDecision.reasoning}
        </p>
      </div>

      {/* Delivered Response */}
      {record.deliveredResponse && (
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <h3 className="text-sm font-semibold text-gray-500 uppercase mb-3">Delivered to Customer</h3>
          <p className="text-gray-900 bg-emerald-50 p-4 rounded-lg border border-emerald-100 leading-relaxed">
            {record.deliveredResponse}
          </p>
        </div>
      )}

      {record.verdict === 'BLOCK' && (
        <div className="bg-red-50 rounded-xl border border-red-200 p-6">
          <h3 className="text-sm font-semibold text-red-700 uppercase mb-3">Response Blocked</h3>
          <p className="text-red-800 text-sm">
            This response was blocked from delivery. The customer received a safe fallback message
            directing them to contact a qualified advisor. This record has been flagged for
            compliance team review.
          </p>
        </div>
      )}
    </div>
  );
}

function ResponseCard({ provider, model, text, label, color }: {
  provider: string;
  model: string;
  text: string;
  label: string;
  color: 'blue' | 'purple';
}) {
  const borderColor = color === 'blue' ? 'border-blue-200' : 'border-purple-200';
  const headerBg = color === 'blue' ? 'bg-blue-50' : 'bg-purple-50';
  const headerText = color === 'blue' ? 'text-blue-700' : 'text-purple-700';

  return (
    <div className={`bg-white rounded-xl border ${borderColor} overflow-hidden`}>
      <div className={`px-4 py-3 ${headerBg} border-b ${borderColor}`}>
        <div className="flex items-center justify-between">
          <span className={`text-sm font-semibold ${headerText}`}>{label}</span>
          <div className="text-right">
            <p className="text-xs font-medium text-gray-600">{provider}</p>
            <p className="text-xs text-gray-400">{model}</p>
          </div>
        </div>
      </div>
      <div className="p-4">
        <p className="text-sm text-gray-800 leading-relaxed">{text}</p>
      </div>
    </div>
  );
}

function VerdictBadgeLarge({ verdict }: { verdict: string }) {
  const config: Record<string, { icon: typeof ShieldCheck; bg: string; text: string; border: string }> = {
    PASS: { icon: ShieldCheck, bg: 'bg-emerald-50', text: 'text-emerald-700', border: 'border-emerald-200' },
    WARN: { icon: AlertTriangle, bg: 'bg-amber-50', text: 'text-amber-700', border: 'border-amber-200' },
    BLOCK: { icon: ShieldX, bg: 'bg-red-50', text: 'text-red-700', border: 'border-red-200' },
  };

  const c = config[verdict] || config.PASS;
  const Icon = c.icon;

  return (
    <span className={`inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-bold border ${c.bg} ${c.text} ${c.border}`}>
      <Icon className="w-5 h-5" />
      {verdict}
    </span>
  );
}
