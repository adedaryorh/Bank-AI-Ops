import { ActivityPage } from './pages/workspace/activity';
import { AmlPage } from './pages/workspace/aml';
import { PoliciesPage } from './pages/workspace/policies';
import { DocumentsPage } from './pages/workspace/documents';
import { CaseDetailPage } from './pages/workspace/case-detail';
import { CasesPage } from './pages/workspace/cases';
import { Routes } from '@angular/router';
import { adminGuard, authGuard, rolesGuard } from './core/auth/auth.guard';
import { AppShell } from './core/layout/app-shell';
import { AdminUsersPage } from './pages/admin-users/admin-users';
import { AuthCallbackPage } from './pages/auth-callback/auth-callback';
import { DashboardPage } from './pages/dashboard/dashboard';
import { ForbiddenPage } from './pages/forbidden/forbidden';

export const routes: Routes = [
  {
    path: 'auth/callback',
    component: AuthCallbackPage,
    title: 'Signing in · BankOps AI',
  },
  {
    path: 'forbidden',
    component: ForbiddenPage,
    title: 'Access denied · BankOps AI',
  },
  {
    path: '',
    component: AppShell,
    canActivate: [authGuard],
    children: [
      {
        path: '',
        pathMatch: 'full',
        component: DashboardPage,
        title: 'Dashboard · BankOps AI',
      },
      { path: 'cases', component: CasesPage, title: 'Cases · BankOps AI' },
      {
        path: 'reviews',
        component: CasesPage,
        data: { reviewQueue: true },
        title: 'Human review queue · BankOps AI',
      },
      { path: 'cases/:id', component: CaseDetailPage, title: 'Case · BankOps AI' },
      { path: 'documents', component: DocumentsPage, title: 'Documents · BankOps AI' },
      { path: 'policies', component: PoliciesPage, title: 'Policy evidence · BankOps AI' },
      {
        path: 'aml',
        component: AmlPage,
        canActivate: [rolesGuard('ADMIN', 'AML_ANALYST', 'SUPERVISOR', 'AUDITOR')],
        title: 'AML simulation · BankOps AI',
      },
      { path: 'activity', component: ActivityPage, title: 'Audit & AI activity · BankOps AI' },
      {
        path: 'admin/users',
        component: AdminUsersPage,
        canActivate: [adminGuard],
        title: 'User administration · BankOps AI',
      },
    ],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
