import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, debounceTime } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiService } from '../core/api.service';
import { Cliente, Pagina } from '../core/modelos';
import { EtiquetaPipe } from '../shared/etiquetas.pipe';
import { IconoComponent } from '../shared/icono.component';
import { PaginadorComponent } from '../shared/paginador.component';

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [DatePipe, FormsModule, EtiquetaPipe, IconoComponent, PaginadorComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="cabecera">
      <div>
        <p class="cabecera__antetitulo">Cartera</p>
        <h1>Clientes</h1>
      </div>
    </header>

    <div class="tarjeta tarjeta--tabla">
      <div class="filtros">
        <label class="buscador">
          <app-icono nombre="buscar" [tamano]="16" />
          <input type="search" placeholder="Buscar por nombre, NIF, teléfono o email" [ngModel]="q"
                 (ngModelChange)="q = $event; busqueda$.next()" />
        </label>
        <div class="segmentado">
          @for (t of tipos; track t.valor) {
            <button [class.activo]="tipo === t.valor" (click)="tipo = t.valor; cargar(0)">{{ t.texto }}</button>
          }
        </div>
      </div>

      <table class="tabla">
        <thead>
          <tr><th>Cliente</th><th>NIF</th><th>Teléfono</th><th>Localidad</th><th>Cliente desde</th></tr>
        </thead>
        <tbody>
          @for (c of pagina()?.contenido; track c.id) {
            <tr class="fila-enlace" (click)="abrir(c)">
              <td>
                <div class="celda-persona">
                  <span class="avatar avatar--sm" [class.avatar--empresa]="c.tipo === 'EMPRESA'">{{ c.nombre[0] }}</span>
                  <div><strong>{{ c.nombre }}</strong><small>{{ c.email }}</small></div>
                </div>
              </td>
              <td class="mono">{{ c.nif }}</td>
              <td>{{ c.telefono }}</td>
              <td>{{ c.localidad }}</td>
              <td>{{ c.fechaAlta | date: 'MMM yyyy' }}</td>
            </tr>
          } @empty {
            <tr><td colspan="5" class="estado-vacio">Sin resultados</td></tr>
          }
        </tbody>
      </table>

      @if (pagina(); as p) {
        <app-paginador [pagina]="p.pagina" [tamano]="p.tamano" [total]="p.total" (cambiar)="cargar($event)" />
      }
    </div>
  `
})
export class ClientesComponent {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  readonly tipos = [
    { valor: '', texto: 'Todos' },
    { valor: 'PARTICULAR', texto: 'Particulares' },
    { valor: 'EMPRESA', texto: 'Empresas' }
  ];
  readonly pagina = signal<Pagina<Cliente> | null>(null);
  readonly busqueda$ = new Subject<void>();
  q = '';
  tipo = '';

  constructor() {
    this.busqueda$.pipe(debounceTime(300), takeUntilDestroyed()).subscribe(() => this.cargar(0));
    this.cargar(0);
  }

  cargar(pagina: number): void {
    this.api.clientes({ q: this.q, tipo: this.tipo, pagina, tamano: 15 }).subscribe(p => this.pagina.set(p));
  }

  abrir(c: Cliente): void {
    this.router.navigate(['/clientes', c.id]);
  }
}
