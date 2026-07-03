import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ResolvedProfile {
  id: string;
  fullName: string;
  headline: string;
  company: string;
  location: string;
  linkedinUrl: string;
  githubUrl: string;
  websiteUrl: string;
  photoUrl: string;
  email: string;
  confidence: number;
  sourceIds: string;
}

export interface SearchResponse {
  results: ResolvedProfile[];
  total: number;
}

export interface SearchFilters {
  name?: string;
  company?: string;
  location?: string;
}

@Injectable({
  providedIn: 'root',
})
export class EnrichService {
  private apiUrl = 'http://localhost:8080/api/enrich';

  constructor(private http: HttpClient) {}

  search(filters: SearchFilters): Observable<SearchResponse> {
    let params = new HttpParams();
    if (filters.name) params = params.set('name', filters.name);
    if (filters.company) params = params.set('company', filters.company);
    if (filters.location) params = params.set('location', filters.location);
    return this.http.get<SearchResponse>(`${this.apiUrl}/search`, { params });
  }

  getProfile(id: string): Observable<ResolvedProfile> {
    return this.http.get<ResolvedProfile>(`${this.apiUrl}/profile/${id}`);
  }

  refreshProfile(id: string): Observable<ResolvedProfile> {
    return this.http.post<ResolvedProfile>(`${this.apiUrl}/refresh/${id}`, {});
  }
}
