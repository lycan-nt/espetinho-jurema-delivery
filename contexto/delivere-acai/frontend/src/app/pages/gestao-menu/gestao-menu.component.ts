import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-gestao-menu',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './gestao-menu.component.html',
  styleUrl: './gestao-menu.component.scss',
})
export class GestaoMenuComponent {}
