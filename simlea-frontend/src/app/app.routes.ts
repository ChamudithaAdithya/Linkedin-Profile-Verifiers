import { Routes } from '@angular/router';
import { Login } from './login/login';
import { Callback } from './callback/callback';
import { Dashboard } from './dashboard/dashboard';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'auth/callback', component: Callback },
  { path: 'dashboard', component: Dashboard },
];
