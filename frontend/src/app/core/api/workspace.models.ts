export interface CaseItem {
  id: string;
  title: string;
  kind: string;
  description: string;
  reference: string;
  department: string;
  creator: string;
  status: string;
  version: number;
  createdAt: string;
  updatedAt: string;
}
export interface CaseEvent {
  id: string;
  actor: string;
  action: string;
  note: string;
  createdAt: string;
}
export interface CaseDetail {
  item: CaseItem;
  events: CaseEvent[];
}
export interface DocumentItem {
  id: string;
  caseId: string | null;
  purpose: string;
  filename: string;
  sha256: string;
  sizeBytes: number;
  status: string;
  extractedFields: Record<string, string>;
  errorMessage: string | null;
  createdAt: string;
}
export interface Citation {
  chunkId: string;
  documentId: string;
  filename: string;
  quote: string;
}
export interface PolicyAnswer {
  id: string;
  answer: string;
  abstained: boolean;
  citations: Citation[];
  executionId: string;
  model: string;
}
export interface AmlAlert {
  id: string;
  caseId: string;
  account: string;
  rule: string;
  rationale: string;
  status: string;
  createdAt: string;
}
export interface Simulation {
  id: string;
  transactionCount: number;
  alertCount: number;
}
export interface AuditRow {
  id: string;
  occurredAt: string;
  actorEmail: string | null;
  action: string;
  targetType: string;
  targetId: string;
  outcome: string;
  correlationId: string | null;
}
export interface Execution {
  id: string;
  operation: string;
  promptVersion: string;
  model: string;
  status: string;
  durationMs: number;
  tokenUsage: number | null;
  createdAt: string;
}

export interface SyntheticTransaction {
  id: string;
  simulationId: string;
  account: string;
  amount: number;
  currency: string;
  occurredAt: string;
}
