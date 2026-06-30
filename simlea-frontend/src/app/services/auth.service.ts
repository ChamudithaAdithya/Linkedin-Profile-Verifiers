import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LinkedInUserInfo {
  sub: string;
  name: string;
  given_name: string;
  family_name: string;
  picture: string;
  email: string;
  email_verified: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth/linkedin';

  constructor(private http: HttpClient) {}

  getAuthorizationUrl(): Observable<{ url: string }> {
    return this.http.get<{ url: string }>(`${this.apiUrl}/url`);
  }

  exchangeCode(code: string, state: string): Observable<LinkedInUserInfo> {
    return this.http.post<LinkedInUserInfo>(`${this.apiUrl}/callback`, { code, state });
  }
}
