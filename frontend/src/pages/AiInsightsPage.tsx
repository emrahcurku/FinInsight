import React, { useEffect, useState, useCallback } from 'react';
import { Sparkles, ShieldAlert, CheckCircle2, Info, AlertTriangle, AlertCircle, RefreshCw } from 'lucide-react';
import { aiApi } from '../api/aiApi';
import { AiInsightResponse, AiInsightItem } from '../types/ai.types';
import { Card } from '../components/common/Card';
import { Badge } from '../components/common/Badge';
import { Button } from '../components/common/Button';
import { DateRangeSelector } from '../components/common/DateRangeSelector';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';
import { ErrorState } from '../components/feedback/ErrorState';
import { formatDate } from '../utils/dateFormatter';
import { useDateRange } from '../hooks/useDateRange';
import { extractApiError } from '../utils/errorExtractor';

export const AiInsightsPage: React.FC = () => {
  const { from, to, setFrom, setTo } = useDateRange();
  const [data, setData] = useState<AiInsightResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorInfo, setErrorInfo] = useState<{ message: string; correlationId?: string; isRateLimited?: boolean } | null>(null);

  const fetchInsights = useCallback(async () => {
    setIsLoading(true);
    setErrorInfo(null);
    try {
      const res = await aiApi.getAiInsights(from, to);
      setData(res);
    } catch (err) {
      const parsed = extractApiError(err, 'Failed to generate AI financial insights.');
      const isRateLimited = parsed.status === 429;
      setErrorInfo({
        message: isRateLimited
          ? 'You have reached the AI insight request limit (max 10 requests per minute). Please wait a moment before trying again.'
          : parsed.message,
        correlationId: parsed.correlationId,
        isRateLimited,
      });
    } finally {
      setIsLoading(false);
    }
  }, [from, to]);

  useEffect(() => {
    fetchInsights();
  }, [fetchInsights]);

  const getSeverityIcon = (severity: string) => {
    switch (severity) {
      case 'DANGER':
        return <AlertCircle size={18} style={{ color: '#ef4444' }} />;
      case 'WARNING':
        return <AlertTriangle size={18} style={{ color: '#f59e0b' }} />;
      default:
        return <Info size={18} style={{ color: '#818cf8' }} />;
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div
            style={{
              padding: '10px',
              borderRadius: '12px',
              background: 'linear-gradient(135deg, #9333ea, #6366f1)',
              color: '#ffffff',
              boxShadow: '0 0 20px rgba(168, 85, 247, 0.4)',
            }}
          >
            <Sparkles size={24} />
          </div>
          <div>
            <h1 style={{ fontSize: '1.6rem', fontWeight: 800, color: '#f8fafc', letterSpacing: '-0.02em' }}>
              AI Financial Insights
            </h1>
            <p style={{ fontSize: '0.875rem', color: '#94a3b8' }}>
              Contextual behavioral synthesis and non-advisory budgeting recommendations.
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          <DateRangeSelector from={from} to={to} onFromChange={setFrom} onToChange={setTo} />
          <Button variant="secondary" size="sm" onClick={fetchInsights} leftIcon={<RefreshCw size={14} />}>
            Refresh
          </Button>
        </div>
      </div>

      {isLoading ? (
        <LoadingSpinner message="Synthesizing aggregated financial posture with AI..." />
      ) : errorInfo ? (
        <ErrorState
          title={errorInfo.isRateLimited ? 'Rate Limit Exceeded' : 'Unable to Load AI Insights'}
          message={errorInfo.message}
          correlationId={errorInfo.correlationId}
          onRetry={fetchInsights}
        />
      ) : !data ? null : (
        <>
          {/* Executive Summary Card */}
          <Card
            variant="ai"
            title="Executive Summary"
            subtitle={`Generated on ${formatDate(data.generatedAt)} via ${data.model}`}
            action={
              <Badge variant={data.fallback ? 'warning' : 'purple'}>
                {data.fallback ? 'Rule Engine Fallback' : `Model: ${data.model}`}
              </Badge>
            }
          >
            <p style={{ fontSize: '1.05rem', color: '#f8fafc', lineHeight: 1.6, marginTop: '0.25rem' }}>
              {data.summary}
            </p>
            {data.fallback && (
              <div
                style={{
                  marginTop: '0.75rem',
                  padding: '8px 12px',
                  borderRadius: '8px',
                  backgroundColor: 'rgba(245, 158, 11, 0.1)',
                  border: '1px solid rgba(245, 158, 11, 0.25)',
                  fontSize: '0.8rem',
                  color: '#fbbf24',
                }}
              >
                Note: External AI provider is currently in fallback mode. Deterministic rule-based insights are being served.
              </div>
            )}
          </Card>

          {/* Categorized Insights Grid */}
          <div>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#f8fafc', marginBottom: '1rem' }}>
              Categorized Behavioral Insights
            </h3>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '1.25rem' }}>
              {data.insights.map((item: AiInsightItem, idx: number) => (
                <Card key={idx} style={{ padding: '1.25rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      {getSeverityIcon(item.severity)}
                      <h4 style={{ fontSize: '0.95rem', fontWeight: 600, color: '#f8fafc' }}>
                        {item.title}
                      </h4>
                    </div>
                    <Badge variant={item.severity === 'DANGER' ? 'danger' : item.severity === 'WARNING' ? 'warning' : 'info'}>
                      {item.severity}
                    </Badge>
                  </div>

                  <p style={{ fontSize: '0.85rem', color: '#94a3b8', lineHeight: 1.5, marginBottom: '1rem' }}>
                    {item.description}
                  </p>

                  {item.recommendation && (
                    <div
                      style={{
                        padding: '10px 12px',
                        borderRadius: '8px',
                        backgroundColor: 'rgba(99, 102, 241, 0.08)',
                        border: '1px solid rgba(99, 102, 241, 0.2)',
                        fontSize: '0.8rem',
                        color: '#c7d2fe',
                      }}
                    >
                      <strong style={{ color: '#818cf8' }}>Action Step: </strong>
                      {item.recommendation}
                    </div>
                  )}
                </Card>
              ))}
            </div>
          </div>

          {/* Recommendations Checklist */}
          {data.recommendations && data.recommendations.length > 0 && (
            <Card title="Actionable Budgeting Recommendations" subtitle="Tailored financial habit optimizations">
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {data.recommendations.map((rec: string, idx: number) => (
                  <div
                    key={idx}
                    style={{
                      display: 'flex',
                      alignItems: 'flex-start',
                      gap: '10px',
                      padding: '10px 14px',
                      borderRadius: '10px',
                      backgroundColor: 'rgba(15, 23, 42, 0.6)',
                      border: '1px solid rgba(255, 255, 255, 0.04)',
                      fontSize: '0.875rem',
                      color: '#f8fafc',
                    }}
                  >
                    <CheckCircle2 size={18} style={{ color: '#10b981', flexShrink: 0, marginTop: '2px' }} />
                    <span>{rec}</span>
                  </div>
                ))}
              </div>
            </Card>
          )}

          {/* Mandatory Non-Advisory Legal Disclaimer */}
          <div
            style={{
              padding: '1rem 1.25rem',
              borderRadius: '12px',
              backgroundColor: 'rgba(30, 41, 59, 0.4)',
              border: '1px solid rgba(255, 255, 255, 0.08)',
              display: 'flex',
              alignItems: 'flex-start',
              gap: '12px',
            }}
          >
            <ShieldAlert size={20} style={{ color: '#94a3b8', flexShrink: 0, marginTop: '2px' }} />
            <div>
              <h5 style={{ fontSize: '0.8rem', fontWeight: 600, color: '#f8fafc', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Non-Advisory Financial Disclaimer
              </h5>
              <p style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '2px', lineHeight: 1.4 }}>
                {data.disclaimer}
              </p>
            </div>
          </div>
        </>
      )}
    </div>
  );
};
