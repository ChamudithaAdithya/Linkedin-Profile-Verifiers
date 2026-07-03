import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AccountStatus {
  valid: boolean;
  profileName: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class LinkedinAccountService {
  private apiUrl = 'http://localhost:8080/api/linkedin/account';

  constructor(private http: HttpClient) {}

  getStatus(): Observable<AccountStatus> {
    return this.http.get<AccountStatus>(`${this.apiUrl}/status`);
  }

  submitCookies(cookiesJson: string): Observable<{ valid: boolean; profileName?: string; error?: string }> {
    return this.http.post<{ valid: boolean; profileName?: string; error?: string }>(
      `${this.apiUrl}/cookies`,
      { cookies: cookiesJson }
    );
  }
}
