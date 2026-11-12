import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ClienteDetalle } from '../core/modelos';
import { EtiquetaPipe } from '../shared/etiquetas.pipe';
import { IconoComponent } from '../shared/icono.component';

@Component({
  selector: 'app-cliente-detalle',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink, EtiquetaPipe, IconoComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (d(); as d) {
      <a class="volver" routerLink="/clientes"><app-icono nombre="volver" [tamano]="16" /> Clientes</a>
      <header class="cabecera">
        <div class="celda-persona celda-persona--grande">
          <span class="avatar avatar--lg" [class.avatar--empresa]="d.cliente.tipo === 'EMPRESA'">{{ d.cliente.nombre[0] }}</span>
          <div>
            <p class="cabecera__antetitulo">{{ d.cliente.tipo | etiqueta }} · {{ d.cliente.nif }}</p>
            <h1>{{ d.cliente.nombre }}</h1>
          </div>
        </div>
      </header>

      <section class="rejilla rejilla--1-2">
        <article class="tarjeta">
          <h2>Datos de contacto</h2>
          <dl class="datos">
            <dt>Teléfono</dt><dd>{{ d.cliente.telefono }}</dd>
            <dt>Email</dt><dd>{{ d.cliente.email }}</dd>
            <dt>Dirección</dt><dd>{{ d.cliente.direccion }}</dd>
            <dt>Localidad</dt><dd>{{ d.cliente.localidad }}</dd>
            <dt>Cliente desde</dt><dd>{{ d.cliente.fechaAlta | date: 'longDate' }}</dd>
          </dl>
          <div class="mini-kpis">
            <div><strong>{{ d.polizasVigentes }}</strong><small>pólizas vigentes</small></div>
            <div><strong>{{ d.primaTotal | currency: 'EUR' : 'symbol' : '1.0-0' }}</strong><small>prima anual</small></div>
            <div><strong>{{ d.siniestros.length }}</strong><small>siniestros</small></div>
          </div>
        </article>

        <article class="tarjeta tarjeta--tabla">
          <header class="tarjeta__cabecera"><h2>Pólizas</h2></header>
          <table class="tabla">
            <thead><tr><th>Póliza</th><th>Ramo</th><th>Aseguradora</th><th>Vence</th><th class="num">Prima</th><th>Estado</th></tr></thead>
            <tbody>
              @for (p of d.polizas; track p.id) {
                <tr class="fila-enlace" [routerLink]="['/polizas', p.id]">
                  <td><strong class="mono">{{ p.numero }}</strong><small>{{ p.producto }}</small></td>
                  <td><span class="ramo" [attr.data-ramo]="p.ramo">{{ p.ramo | etiqueta }}</span></td>
                  <td><span class="aseguradora"><i [style.background]="p.aseguradoraColor"></i>{{ p.aseguradoraNombre }}</span></td>
                  <td>{{ p.fechaVencimiento | date: 'dd/MM/yyyy' }}</td>
                  <td class="num">{{ p.primaAnual | currency: 'EUR' }}</td>
                  <td><span class="insignia" [attr.data-estado]="p.estado">{{ p.estado | etiqueta }}</span></td>
                </tr>
              }
            </tbody>
          </table>
        </article>
      </section>

      @if (d.siniestros.length) {
        <article class="tarjeta tarjeta--tabla">
          <header class="tarjeta__cabecera"><h2>Siniestros</h2></header>
          <table class="tabla">
            <thead><tr><th>Nº</th><th>Tipo</th><th>Apertura</th><th>Canal</th><th>Estado</th></tr></thead>
            <tbody>
              @for (s of d.siniestros; track s.id) {
                <tr>
                  <td class="mono">{{ s.numero }}</td>
                  <td>{{ s.tipo }}</td>
                  <td>{{ s.fechaApertura | date: 'dd/MM/yyyy' }}</td>
                  <td>{{ s.canal | etiqueta }}</td>
                  <td><span class="insignia" [attr.data-estado]="s.estado">{{ s.estado | etiqueta }}</span></td>
                </tr>
              }
            </tbody>
          </table>
        </article>
      }
    } @else {
      <div class="cargando">Cargando…</div>
    }
  `
})
export class ClienteDetalleComponent {
  private readonly api = inject(ApiService);
  readonly id = input.required<string>();
  readonly d = signal<ClienteDetalle | null>(null);

  constructor() {
    effect(() => {
      const id = Number(this.id());
      this.api.cliente(id).subscribe(d => this.d.set(d));
    });
  }
}
