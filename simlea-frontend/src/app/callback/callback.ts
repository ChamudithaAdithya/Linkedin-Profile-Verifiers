import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService, LinkedInUserInfo } from '../services/auth.service';

@Component({
  selector: 'app-callback',
  imports: [],
  templateUrl: './callback.html',
  styleUrl: './callback.css',
})
export class Callback implements OnInit {
  userInfo: LinkedInUserInfo | null = null;
  error: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const code = this.route.snapshot.queryParamMap.get('code');
    const state = this.route.snapshot.queryParamMap.get('state') || '';
    if (!code) {
      this.error = 'No authorization code received';
      return;
    }

    this.authService.exchangeCode(code, state).subscribe({
      next: (info) => (this.userInfo = info),
      error: (err) => {
        console.error('Auth error:', err);
        this.error = err.error?.error || 'Failed to authenticate with LinkedIn';
        setTimeout(() => this.router.navigate(['/']), 5000);
      },
    });
  }
}
