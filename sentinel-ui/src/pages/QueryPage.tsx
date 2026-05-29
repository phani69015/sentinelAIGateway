import { useState } from 'react';
import { Send, Loader2, ShieldCheck, AlertTriangle, ShieldX, Clock, Database, Info, Anchor, CloudOff } from 'lucide-react';
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

  const isGrounded = knowledgeBase.trim().length > 0;

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

      {/* Context Explainer */}
      <div className="bg-blue-50 border border-blue-200 rounded-xl p-5">
        <div className="flex gap-4">
          <div className="flex-shrink-0">
            <Info className="w-5 h-5 text-blue-600 mt-0.5" />
          </div>
          <div>
            <h4 className="text-sm font-semibold text-blue-900 mb-1">How Context Works in the Pipeline</h4>
            <p className="text-sm text-blue-800 leading-relaxed">
              The <strong>Knowledge Base Context</strong> represents your organization's ground truth data — 
              real product rates, fees, terms, and policies sourced from internal databases. When provided, 
              both LLMs are instructed to answer <em>only</em> from this data, and the Audit Agent verifies 
              their responses against it. This is how Sentinel prevents hallucinations: if an LLM invents 
              a rate that doesn't exist in the context, the Hallucination Detector flags it.
            </p>
            <div className="flex flex-wrap gap-4 mt-3">
              <ContextDetail
                label="With Context"
                description="LLMs grounded to verified facts. Hallucinations detectable."
                icon={<Anchor className="w-4 h-4 text-emerald-600" />}
              />
              <ContextDetail
                label="Without Context"
                description="LLMs rely on training data. Only cross-checked against each other."
                icon={<CloudOff className="w-4 h-4 text-amber-600" />}
              />
            </div>
          </div>
        </div>
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

            {/* Knowledge Base with Grounding Indicator */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="block text-sm font-medium text-gray-700">
                  Knowledge Base Context
                </label>
                <GroundingBadge grounded={isGrounded} />
              </div>
              <textarea
                value={knowledgeBase}
                onChange={(e) => setKnowledgeBase(e.target.value)}
                placeholder="Add verified product/company data here (one fact per line)..."
                className="w-full h-28 px-4 py-3 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 resize-none font-mono text-xs"
              />
              <p className="text-xs text-gray-400 mt-1.5">
                This data is injected into the LLM system prompt and used by the Audit Agent to verify factual claims.
                In production, this is populated from your product database, CRM, or rate sheets.
              </p>
            </div>

            {/* Context Flow Diagram */}
            <div className="bg-gray-50 rounded-lg p-3 border border-gray-100">
              <p className="text-xs font-medium text-gray-500 mb-2">Context flow in the pipeline:</p>
              <div className="flex items-center gap-2 text-xs text-gray-600">
                <span className="bg-white border border-gray-200 rounded px-2 py-1 flex items-center gap-1">
                  <Database className="w-3 h-3" /> Knowledge Base
                </span>
                <span className="text-gray-300">&rarr;</span>
                <span className="bg-white border border-gray-200 rounded px-2 py-1">System Prompt</span>
                <span className="text-gray-300">&rarr;</span>
                <span className="bg-white border border-gray-200 rounded px-2 py-1">LLM-A & LLM-B</span>
                <span className="text-gray-300">&rarr;</span>
                <span className="bg-white border border-gray-200 rounded px-2 py-1">Audit Agent verifies</span>
              </div>
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
        <div className="space-y-6">
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

              {/* Grounding Status */}
              <div className={`px-6 py-3 border-b ${isGrounded ? 'bg-emerald-50 border-emerald-100' : 'bg-amber-50 border-amber-100'}`}>
                <div className="flex items-center gap-2">
                  {isGrounded ? (
                    <>
                      <Anchor className="w-4 h-4 text-emerald-600" />
                      <span className="text-xs font-medium text-emerald-700">
                        Grounded Mode — Response verified against provided knowledge base
                      </span>
                    </>
                  ) : (
                    <>
                      <CloudOff className="w-4 h-4 text-amber-600" />
                      <span className="text-xs font-medium text-amber-700">
                        Ungrounded Mode — No knowledge base provided, cross-check only
                      </span>
                    </>
                  )}
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
                  <MetaItem label="Grounding" value={isGrounded ? 'Knowledge Base' : 'Cross-check only'} />
                  <MetaItem label="Context Facts" value={isGrounded ? `${knowledgeBase.split('\n').filter(Boolean).length} facts` : 'None'} />
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

          {/* Context Usage Explainer Card */}
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <h3 className="text-sm font-semibold text-gray-900 mb-3">How Context Reduces Hallucinations</h3>
            <div className="space-y-3">
              <FlowStep
                step={1}
                title="Context injected into system prompt"
                description="Both LLMs receive the knowledge base as ground truth. They are instructed to answer ONLY from this data."
              />
              <FlowStep
                step={2}
                title="LLMs generate responses"
                description="Each LLM answers the query independently, constrained by the provided context."
              />
              <FlowStep
                step={3}
                title="Audit Agent cross-validates"
                description="The Audit Agent checks if claims in the responses actually exist in the knowledge base. Ungrounded claims are flagged as potential hallucinations."
              />
              <FlowStep
                step={4}
                title="Verdict based on evidence"
                description="If a response contains facts not in the knowledge base, or if the two LLMs disagree on a contextual fact, the query is WARNED or BLOCKED."
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function GroundingBadge({ grounded }: { grounded: boolean }) {
  if (grounded) {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">
        <Anchor className="w-3 h-3" />
        Grounded
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-amber-50 text-amber-700 border border-amber-200">
      <CloudOff className="w-3 h-3" />
      Ungrounded
    </span>
  );
}

function ContextDetail({ label, description, icon }: { label: string; description: string; icon: React.ReactNode }) {
  return (
    <div className="flex items-start gap-2">
      {icon}
      <div>
        <p className="text-xs font-semibold text-blue-900">{label}</p>
        <p className="text-xs text-blue-700">{description}</p>
      </div>
    </div>
  );
}

function FlowStep({ step, title, description }: { step: number; title: string; description: string }) {
  return (
    <div className="flex gap-3">
      <div className="flex-shrink-0 w-6 h-6 bg-blue-100 rounded-full flex items-center justify-center">
        <span className="text-xs font-bold text-blue-700">{step}</span>
      </div>
      <div>
        <p className="text-sm font-medium text-gray-900">{title}</p>
        <p className="text-xs text-gray-500 mt-0.5">{description}</p>
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
