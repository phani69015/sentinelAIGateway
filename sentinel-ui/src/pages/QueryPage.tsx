import { useState } from 'react';
import { Send, Loader2, ShieldCheck, AlertTriangle, ShieldX, Clock } from 'lucide-react';
import { submitQuery } from '../services/api';
import type { QueryResponse } from '../types';

const SAMPLE_QUERIES = [
  'What is the interest rate on your high-yield savings account?',
  'Should I invest in index funds or individual stocks for retirement?',
  'I want a guaranteed 10% annual return with zero risk. What do you recommend?',
  'Can you explain what compound interest is and how it works?',
  'Tell me exactly which tax deductions to claim this year',
  'What fees are associated with your checking account?',
];

export default function QueryPage() {
  const [query, setQuery] = useState('');
  const [knowledgeBase, setKnowledgeBase] = useState(
    'Premium Savings: 4.25% APY on balances over $10,000\nFDIC insured up to $250,000\n6 free withdrawals per month\nNo monthly fee with minimum balance'
  );
  const [strictMode, setStrictMode] = useState(false);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<QueryResponse | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!query.trim()) return;

    setLoading(true);
    setResult(null);

    try {
      const response = await submitQuery({
        query,
        context: {
          customerSegment: 'retail',
          productCategory: 'general',
          knowledgeBase: knowledgeBase.split('\n').filter(Boolean),
        },
        options: { strictMode },
      });
      setResult(response);
    } catch (err) {
      console.error('Query failed:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-2xl font-bold text-gray-900">Query Tester</h2>
        <p className="text-gray-500 mt-1">Submit queries to test the Sentinel AI pipeline</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Input Panel */}
        <div className="space-y-6">
          <form onSubmit={handleSubmit} className="bg-white rounded-xl border border-gray-200 p-6 space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Customer Query</label>
              <textarea
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Ask a financial question..."
                className="w-full h-32 px-4 py-3 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 resize-none"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Knowledge Base Context <span className="text-gray-400">(optional)</span>
              </label>
              <textarea
                value={knowledgeBase}
                onChange={(e) => setKnowledgeBase(e.target.value)}
                placeholder="One fact per line..."
                className="w-full h-24 px-4 py-3 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 resize-none font-mono text-xs"
              />
            </div>

            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="strictMode"
                checked={strictMode}
                onChange={(e) => setStrictMode(e.target.checked)}
                className="w-4 h-4 text-blue-600 rounded border-gray-300"
              />
              <label htmlFor="strictMode" className="text-sm text-gray-700">
                Strict Mode <span className="text-gray-400">(BLOCK on any discrepancy)</span>
              </label>
            </div>

            <button
              type="submit"
              disabled={loading || !query.trim()}
              className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {loading ? (
                <>
                  <Loader2 className="w-5 h-5 animate-spin" />
                  Processing...
                </>
              ) : (
                <>
                  <Send className="w-5 h-5" />
                  Submit Query
                </>
              )}
            </button>
          </form>

          {/* Sample Queries */}
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <h3 className="text-sm font-semibold text-gray-700 mb-3">Sample Queries</h3>
            <div className="space-y-2">
              {SAMPLE_QUERIES.map((sq, i) => (
                <button
                  key={i}
                  onClick={() => setQuery(sq)}
                  className="w-full text-left px-3 py-2 text-sm text-gray-600 hover:bg-gray-50 rounded-lg transition-colors border border-transparent hover:border-gray-200"
                >
                  {sq}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Results Panel */}
        <div>
          {result ? (
            <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
              {/* Verdict Banner */}
              <div className={`px-6 py-4 ${verdictBg(result.verdict)}`}>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    {verdictIcon(result.verdict)}
                    <div>
                      <p className="font-bold text-lg">{result.verdict}</p>
                      <p className="text-sm opacity-80">
                        Confidence: {(result.confidence * 100).toFixed(0)}%
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-1 text-sm opacity-80">
                    <Clock className="w-4 h-4" />
                    {result.metadata.latencyMs}ms
                  </div>
                </div>
              </div>

              {/* Response */}
              <div className="p-6 space-y-4">
                <div>
                  <h4 className="text-sm font-semibold text-gray-500 uppercase mb-2">Delivered Response</h4>
                  <p className="text-sm text-gray-800 leading-relaxed bg-gray-50 p-4 rounded-lg">
                    {result.response}
                  </p>
                </div>

                {/* Metadata */}
                <div className="grid grid-cols-2 gap-4 pt-4 border-t border-gray-100">
                  <MetaItem label="Audit ID" value={result.auditId.slice(0, 8) + '...'} />
                  <MetaItem label="Responders Agreed" value={result.metadata.respondersAgreed ? 'Yes' : 'No'} />
                  <MetaItem label="Compliance Checks" value={String(result.metadata.complianceChecks)} />
                  <MetaItem label="Violations Found" value={String(result.metadata.violationsFound)} />
                </div>
              </div>
            </div>
          ) : (
            <div className="bg-white rounded-xl border border-gray-200 p-12 flex flex-col items-center justify-center text-center">
              <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
                <Send className="w-8 h-8 text-gray-400" />
              </div>
              <p className="text-gray-500 text-sm">Submit a query to see the Sentinel AI pipeline in action</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function MetaItem({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs text-gray-500">{label}</p>
      <p className="text-sm font-medium text-gray-900">{value}</p>
    </div>
  );
}

function verdictBg(verdict: string) {
  switch (verdict) {
    case 'PASS': return 'bg-emerald-50 text-emerald-800';
    case 'WARN': return 'bg-amber-50 text-amber-800';
    case 'BLOCK': return 'bg-red-50 text-red-800';
    default: return 'bg-gray-50 text-gray-800';
  }
}

function verdictIcon(verdict: string) {
  switch (verdict) {
    case 'PASS': return <ShieldCheck className="w-8 h-8 text-emerald-600" />;
    case 'WARN': return <AlertTriangle className="w-8 h-8 text-amber-600" />;
    case 'BLOCK': return <ShieldX className="w-8 h-8 text-red-600" />;
    default: return null;
  }
}
