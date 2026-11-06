/** API DTOs for the BankOps AI backend (JSON, RFC 9457 errors). */

export interface CurrentUser {
  readonly id: string;
  readonly subject: string;
  readonly email: string;
  readonly displayName: string;
  readonly department: string | null;
  readonly departmentName: string | null;
  readonly roles: readonly string[];
}

export interface DashboardMetric {
  readonly key: string;
  readonly label: string;
  readonly value: number | null;
  readonly available: boolean;
  readonly reason: string | null;
}

export interface DashboardWidget {
  readonly key: string;
  readonly label: string;
  readonly module: string;
  readonly available: boolean;
  readonly reason: string | null;
}

export interface AuditEntry {
  readonly id: string;
  readonly occurredAt: string;
  readonly action: string;
  readonly actorEmail: string;
  readonly outcome: string;
}

export interface DashboardSummary {
  readonly generatedAt: string;
  readonly user: CurrentUser;
  readonly metrics: readonly DashboardMetric[];
  readonly widgets: readonly DashboardWidget[];
  readonly recentAudit: readonly AuditEntry[];
  readonly notice: string | null;
}

export interface AdminUser {
  readonly id: string;
  readonly subject: string;
  readonly email: string;
  readonly displayName: string;
  readonly department: string | null;
  readonly roles: readonly string[];
  readonly createdAt: string;
  readonly lastSeenAt: string | null;
}

export interface PageResponse<T> {
  readonly content: readonly T[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}

/** RFC 9457 problem details, exactly as produced by the backend. */
export interface ProblemDetails {
  readonly type?: string;
  readonly title?: string;
  readonly status?: number;
  readonly detail?: string;
  readonly correlationId?: string;
}
