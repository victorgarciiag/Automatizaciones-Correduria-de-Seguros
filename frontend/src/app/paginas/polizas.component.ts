import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, debounceTime } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiService } from '../core/api.service';
import { Aseguradora, Pagina, Poliza, Ramo } from '../core/modelos';
import { EtiquetaPipe } from '../shared/etiquetas.pipe';
import { IconoComponent } from '../shared/icono.component';
import { PaginadorComponent } from '../shared/paginador.component';

@Component({
  selector: 'app-polizas',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, FormsModule, EtiquetaPipe, IconoComponent, PaginadorComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="cabecera">
      <div>
        <p class="cabecera__antetitulo">Cartera</p>
        <h1>Pólizas</h1>
      </div>
    </header>

    <div class="tarjeta tarjeta--tabla">
      <div class="filtros">
        <label class="buscador">
          <app-icono nombre="buscar" [tamano]="16" />
          <input type="search" placeholder="Nº de póliza, matrícula, cliente o NIF" [ngModel]="q"
                 (ngModelChange)="q = $event; busqueda$.next()" />
        </label>
        <select class="selector" [(ngModel)]="aseguradoraId" (ngModelChange)="cargar(0)">
          <option value="">Todas las aseguradoras</option>
          @for (a of aseguradoras(); track a.id) {
            <option [value]="a.id">{{ a.nombre }}</option>
          }
        </select>
        <select class="selector" [(ngModel)]="estado" (ngModelChange)="cargar(0)">
          <option value="">Cualquier estado</option>
          <option value="VIGENTE">Vigentes</option>
          <option value="ANULADA">Anuladas</option>
        </select>
        <label class="interruptor">
          <input type="checkbox" [(ngModel)]="soloVencen" (ngModelChange)="cargar(0)" />
          Vencen en 30 días
        </label>
      </div>
      <div class="chips">
        <button class="chip" [class.activo]="!ramo" (click)="ramo = ''; cargar(0)">Todos los ramos</button>
        @for (r of ramos; track r) {
          <button class="chip" [class.activo]="ramo === r" [attr.data-ramo]="r" (click)="ramo = r; cargar(0)">{{ r | etiqueta }}</button>
        }
      </div>

      <table class="tabla">
        <thead>
          <tr><th>Póliza</th><th>Tomador</th><th>Ramo</th><th>Objeto asegurado</th><th>Aseguradora</th><th>Vencimiento</th><th class="num">Prima anual</th><th>Estado</th></tr>
        </thead>
        <tbody>
          @for (p of pagina()?.contenido; track p.id) {
            <tr class="fila-enlace" (click)="abrir(p)">
              <td><strong class="mono">{{ p.numero }}</strong><small>{{ p.producto }}</small></td>
              <td>{{ p.clienteNombre }}<small class="mono">{{ p.clienteNif }}</small></td>
              <td><span class="ramo" [attr.data-ramo]="p.ramo">{{ p.ramo | etiqueta }}</span></td>
              <td class="recortar">{{ p.objetoAsegurado }}</td>
              <td><span class="aseguradora"><i [style.background]="p.aseguradoraColor"></i>{{ p.aseguradoraNombre }}</span></td>
              <td>{{ p.fechaVencimiento | date: 'dd/MM/yyyy' }}</td>
              <td class="num">{{ p.primaAnual | currency: 'EUR' }}</td>
              <td><span class="insignia" [attr.data-estado]="p.estado">{{ p.estado | etiqueta }}</span></td>
            </tr>
          } @empty {
            <tr><td colspan="8" class="estado-vacio">Sin resultados</td></tr>
          }
        </tbody>
      </table>
      @if (pagina(); as p) {
        <app-paginador [pagina]="p.pagina" [tamano]="p.tamano" [total]="p.total" (cambiar)="cargar($event)" />
      }
    </div>
  `
})
export class PolizasComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  /** Query param ?vence=30 (desde el inicio). */
  readonly vence = input<string>();

  readonly ramos: Ramo[] = ['AUTO', 'HOGAR', 'COMERCIO', 'SALUD', 'VIDA', 'DECESOS'];
  readonly aseguradoras = signal<Aseguradora[]>([]);
  readonly pagina = signal<Pagina<Poliza> | null>(null);
  readonly busqueda$ = new Subject<void>();

  q = '';
  ramo = '';
  estado = '';
  aseguradoraId = '';
  soloVencen = false;

  constructor() {
    this.busqueda$.pipe(debounceTime(300), takeUntilDestroyed()).subscribe(() => this.cargar(0));
    this.api.aseguradoras().subscribe(a => this.aseguradoras.set(a));
  }

  ngOnInit(): void {
    this.soloVencen = !!this.vence();
    if (this.soloVencen) {
      this.estado = 'VIGENTE';
    }
    this.cargar(0);
  }

  cargar(pagina: number): void {
    this.api.polizas({
      q: this.q, ramo: this.ramo, estado: this.estado, aseguradoraId: this.aseguradoraId,
      venceEnDias: this.soloVencen ? 30 : null, pagina, tamano: 15
    }).subscribe(p => this.pagina.set(p));
  }

  abrir(p: Poliza): void {
    this.router.navigate(['/polizas', p.id]);
  }
}
