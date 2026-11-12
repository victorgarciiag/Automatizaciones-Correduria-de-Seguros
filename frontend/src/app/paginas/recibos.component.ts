import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiService } from '../core/api.service';
import { EstadoRecibo, Pagina, Recibo, TotalesRecibos } from '../core/modelos';
import { EtiquetaPipe } from '../shared/etiquetas.pipe';
import { IconoComponent } from '../shared/icono.component';
import { PaginadorComponent } from '../shared/paginador.component';

@Component({
  selector: 'app-recibos',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, FormsModule, RouterLink, EtiquetaPipe, IconoComponent, PaginadorComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="cabecera">
      <div>
        <p class="cabecera__antetitulo">Administración</p>
        <h1>Recibos</h1>
      </div>
    </header>

    @if (totales(); as t) {
      <section class="kpis kpis--3">
        <div class="kpi">
          <span class="kpi__icono kpi__icono--rojo"><app-icono nombre="alerta" /></span>
          <span class="kpi__etiqueta">Recibos devueltos</span>
          <strong class="kpi__valor">{{ t.devueltos }}</strong>
          <span class="kpi__detalle">{{ t.importeDevuelto | currency: 'EUR' }} pendientes de cobro</span>
        </div>
        <div class="kpi">
          <span class="kpi__icono kpi__icono--azul"><app-icono nombre="reloj" /></span>
          <span class="kpi__etiqueta">Emitidos pendientes</span>
          <strong class="kpi__valor">{{ t.pendientes }}</strong>
          <span class="kpi__detalle">en proceso de cobro bancario</span>
        </div>
        <div class="kpi kpi--sugerencia">
          <span class="kpi__icono"><app-icono nombre="automatizaciones" /></span>
          <span class="kpi__etiqueta">Próxima automatización</span>
          <strong class="kpi__valor kpi__valor--texto">Recordatorios escalonados</strong>
          <span class="kpi__detalle">Día 1 amable · día 7 recordatorio · día 15 aviso</span>
        </div>
      </section>
    }

    <div class="tarjeta tarjeta--tabla">
      <div class="filtros">
        <div class="segmentado">
          @for (e of estados; track e.valor) {
            <button [class.activo]="estado === e.valor" (click)="estado = e.valor; cargar(0)">{{ e.texto }}</button>
          }
        </div>
        <label class="buscador">
          <app-icono nombre="buscar" [tamano]="16" />
          <input type="search" placeholder="Nº de recibo, póliza o cliente" [ngModel]="q"
                 (ngModelChange)="q = $event; busqueda$.next()" />
        </label>
      </div>

      <table class="tabla">
        <thead>
          <tr><th>Recibo</th><th>Cliente</th><th>Póliza</th><th>Emisión</th><th class="num">Importe</th><th>Estado</th><th>Reclamaciones</th><th></th></tr>
        </thead>
        <tbody>
          @for (r of pagina()?.contenido; track r.id) {
            <tr>
              <td class="mono">{{ r.numero }}</td>
              <td><a class="enlace" [routerLink]="['/clientes', r.clienteId]">{{ r.clienteNombre }}</a><small>{{ r.clienteTelefono }}</small></td>
              <td><a class="mono enlace" [routerLink]="['/polizas', r.polizaId]">{{ r.polizaNumero }}</a><small>{{ r.aseguradoraNombre }}</small></td>
              <td>{{ r.fechaEmision | date: 'dd/MM/yyyy' }}</td>
              <td class="num">{{ r.importe | currency: 'EUR' }}</td>
              <td><span class="insignia" [attr.data-estado]="r.estado">{{ r.estado | etiqueta }}</span></td>
              <td>
                @if (r.estado === 'DEVUELTO') {
                  <span class="puntos">
                    @for (i of [0, 1, 2]; track i) {
                      <i [class.lleno]="i < r.intentosReclamacion"></i>
                    }
                  </span>
                }
              </td>
              <td class="acciones">
                @if (r.estado === 'DEVUELTO') {
                  <button class="btn btn--fantasma btn--sm" (click)="reclamar(r)">Reclamar</button>
                }
                @if (r.estado === 'DEVUELTO' || r.estado === 'PENDIENTE') {
                  <button class="btn btn--secundario btn--sm" (click)="cobrar(r)">Marcar cobrado</button>
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="8" class="estado-vacio">Sin recibos</td></tr>
          }
        </tbody>
      </table>
      @if (pagina(); as p) {
        <app-paginador [pagina]="p.pagina" [tamano]="p.tamano" [total]="p.total" (cambiar)="cargar($event)" />
      }
    </div>
  `
})
export class RecibosComponent {
  private readonly api = inject(ApiService);

  readonly estados: { valor: EstadoRecibo | ''; texto: string }[] = [
    { valor: 'DEVUELTO', texto: 'Devueltos' },
    { valor: 'PENDIENTE', texto: 'Pendientes' },
    { valor: 'COBRADO', texto: 'Cobrados' },
    { valor: '', texto: 'Todos' }
  ];
  readonly pagina = signal<Pagina<Recibo> | null>(null);
  readonly totales = signal<TotalesRecibos | null>(null);
  readonly busqueda$ = new Subject<void>();
  estado: EstadoRecibo | '' = 'DEVUELTO';
  q = '';

  constructor() {
    this.busqueda$.pipe(debounceTime(300), takeUntilDestroyed()).subscribe(() => this.cargar(0));
    this.cargar(0);
  }

  cargar(pagina: number): void {
    this.api.recibos({ estado: this.estado, q: this.q, pagina, tamano: 15 }).subscribe(p => this.pagina.set(p));
    this.api.totalesRecibos().subscribe(t => this.totales.set(t));
  }

  cobrar(r: Recibo): void {
    this.api.cobrarRecibo(r.id).subscribe(() => this.cargar(this.pagina()?.pagina ?? 0));
  }

  reclamar(r: Recibo): void {
    this.api.reclamarRecibo(r.id).subscribe(() => this.cargar(this.pagina()?.pagina ?? 0));
  }
}
