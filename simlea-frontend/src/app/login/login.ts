import { Component } from '@angular/core';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  constructor(private authService: AuthService) {}

  loginWithLinkedIn(): void {
    this.authService.getAuthorizationUrl().subscribe({
      next: (res) => (window.location.href = res.url),
      error: () => alert('Failed to get authorization URL'),
    });
  }
}
