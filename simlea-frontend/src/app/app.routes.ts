import { Routes } from '@angular/router';
import { Login } from './login/login';
import { Callback } from './callback/callback';

export const routes: Routes = [
  { path: '', component: Login },
  { path: 'auth/callback', component: Callback },
];
