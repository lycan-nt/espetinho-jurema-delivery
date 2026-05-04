import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { DashboardService } from '../../services/dashboard.service';
import { AuthService } from '../../services/auth.service';
import { Dashboard } from '../../models/dashboard.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, DecimalPipe, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly route = inject(ActivatedRoute);
  readonly authService = inject(AuthService);

  data: Dashboard | null = null;
  loading = true;
  error: string | null = null;
  /** Exibido quando o usuário foi redirecionado por não ter permissão para Gestão */
  semPermissaoGestao = false;

  ngOnInit(): void {
    this.semPermissaoGestao = this.route.snapshot.queryParams['semPermissao'] === 'gestao';
    this.carregar();
  }

  carregar(): void {
    this.loading = true;
    this.error = null;
    this.dashboardService.getDashboard().subscribe({
      next: (d) => {
        this.data = d;
        this.loading = false;
      },
      error: () => {
        this.error = 'Não foi possível carregar o resumo.';
        this.loading = false;
      },
    });
  }
}
