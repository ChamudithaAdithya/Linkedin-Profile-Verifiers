import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EnrichService, ResolvedProfile } from '../services/enrich.service';

@Component({
  selector: 'app-dashboard',
  imports: [FormsModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  name = '';
  company = '';
  location = '';
  isLoading = false;
  hasSearched = false;

  searchResults: ResolvedProfile[] = [];
  selectedProfile: ResolvedProfile | null = null;

  constructor(private enrichService: EnrichService) {}

  onSearch(): void {
    if (!this.name.trim() && !this.company.trim() && !this.location.trim()) return;

    this.isLoading = true;
    this.hasSearched = true;
    this.selectedProfile = null;

    this.enrichService.search({
      name: this.name.trim() || undefined,
      company: this.company.trim() || undefined,
      location: this.location.trim() || undefined,
    }).subscribe({
      next: (res) => {
        this.searchResults = res.results;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  selectProfile(profile: ResolvedProfile): void {
    this.selectedProfile = profile;
  }

  confidenceDisplay(score: number): string {
    return Math.round(score) + '%';
  }

  confidenceColor(score: number): string {
    if (score >= 70) return '#2e7d32';
    if (score >= 50) return '#f57c00';
    return '#c62828';
  }

  openUrl(url: string | undefined): void {
    if (url) window.open(url, '_blank');
  }

  avatarUrl(url: string | undefined): string {
    return url || 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 96 96"><rect fill="#e0e0e0" width="96" height="96" rx="48"/><text x="48" y="54" text-anchor="middle" font-size="32" fill="#9e9e9e" font-family="sans-serif">?</text></svg>');
  }
}
