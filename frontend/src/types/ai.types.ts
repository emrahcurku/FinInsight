import { InsightSeverity } from './dashboard.types';

export interface AiInsightItem {
  type: string;
  severity: InsightSeverity;
  title: string;
  description: string;
  recommendation: string;
}

export interface AiInsightResponse {
  generatedAt: string;
  summary: string;
  insights: AiInsightItem[];
  recommendations: string[];
  model: string;
  fallback: boolean;
  disclaimer: string;
  correlationId?: string;
}
