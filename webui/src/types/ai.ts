import type { Priority } from './task';

export interface AiSubTaskProposalOutputDTO {
  title: string;
  suggestedDeadline?: string;
  rationale?: string;
}

export interface AiTaskBreakdownOutputDTO {
  taskId: string;
  subtasks: AiSubTaskProposalOutputDTO[];
  estimatedDeadline?: string;
  reasoning: string;
  applied: boolean;
}

export interface AiPriorityProposalOutputDTO {
  taskId: string;
  suggestedPriority: Priority;
  reasoning: string;
  confidence: number;
  applied: boolean;
}

export interface AiSummaryOutputDTO {
  taskId: string;
  title: string;
  summary: string;
  keyHighlights: string[];
  suggestedNextStep: string;
}

export interface AiPromptOverrideRequest {
  systemPrompt?: string;
  additionalInstructions?: string;
  provider?: string;
  customFormat?: 'openai' | 'anthropic';
  customBaseUrl?: string;
  customApiKey?: string;
  customModel?: string;
  customHeaders?: Record<string, string>;
}
