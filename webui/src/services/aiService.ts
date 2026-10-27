import { apiClient } from './apiClient';
import type { 
  AiTaskBreakdownOutputDTO, 
  AiPriorityProposalOutputDTO, 
  AiSummaryOutputDTO, 
  AiPromptOverrideRequest 
} from '../types/ai';
import type { ApiResponse } from '../types/task';

export const aiService = {
  async breakdownTask(
    taskId: string, 
    apply: boolean = false, 
    override?: AiPromptOverrideRequest
  ): Promise<AiTaskBreakdownOutputDTO> {
    const res = await apiClient.post<ApiResponse<AiTaskBreakdownOutputDTO>>(
      `/v1/tasks/${taskId}/ai-breakdown?apply=${apply}`,
      override || {}
    );
    return res.data.data;
  },

  async prioritizeTask(
    taskId: string, 
    apply: boolean = false, 
    override?: AiPromptOverrideRequest
  ): Promise<AiPriorityProposalOutputDTO> {
    const res = await apiClient.post<ApiResponse<AiPriorityProposalOutputDTO>>(
      `/v1/tasks/${taskId}/ai-prioritize?apply=${apply}`,
      override || {}
    );
    return res.data.data;
  },

  async summarizeTask(
    taskId: string, 
    override?: AiPromptOverrideRequest
  ): Promise<AiSummaryOutputDTO> {
    const res = await apiClient.post<ApiResponse<AiSummaryOutputDTO>>(
      `/v1/tasks/${taskId}/ai-summary`,
      override || {}
    );
    return res.data.data;
  }
};
