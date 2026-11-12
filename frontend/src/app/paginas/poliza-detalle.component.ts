import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { PolizaDetalle } from '../core/modelos';
import { EtiquetaPipe } from '../shared/etiquetas.pipe';
import { IconoComponent } from '../shared/icono.component';

@Component({
  selector: 'app-poliza-detalle',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, DecimalPipe, RouterLink, EtiquetaPipe, IconoComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (d(); as d) {
      <a class="volver" routerLink="/polizas"><app-icono nombre="volver" [tamano]="16" /> Pólizas</a>
      <header class="cabecera">
        <div>
          <p class="cabecera__antetitulo">
            <span class="ramo" [attr.data-ramo]="d.poliza.ramo">{{ d.poliza.ramo | etiqueta }}</span>
            {{ d.poliza.producto }}
          </p>
          <h1 class="mono">{{ d.poliza.numero }}</h1>
        </div>
        <span class="insignia insignia--grande" [attr.data-estado]="d.poliza.enVigor ? 'VIGENTE' : 'ANULADA'">
          {{ d.poliza.enVigor ? 'En vigor' : (d.poliza.estado | etiqueta) }}
        </span>
      </header>

      <section class="rejilla rejilla--3">
        <article class="tarjeta">
          <h2>Póliza</h2>
          <dl class="datos">
            <dt>Aseguradora</dt><dd><span class="aseguradora"><i [style.background]="d.poliza.aseguradoraColor"></i>{{ d.poliza.aseguradoraNombre }}</span></dd>
            <dt>Objeto asegurado</dt><dd>{{ d.poliza.objetoAsegurado }}</dd>
            <dt>Efecto</dt><dd>{{ d.poliza.fechaEfecto | date: 'dd/MM/yyyy' }}</dd>
            <dt>Vencimiento</dt><dd>{{ d.poliza.fechaVencimiento | date: 'dd/MM/yyyy' }}</dd>
          </dl>
        </article>
        <article class="tarjeta">
          <h2>Económico</h2>
          <dl class="datos">
            <dt>Prima anual</dt><dd><strong>{{ d.poliza.primaAnual | currency: 'EUR' }}</strong></dd>
            <dt>Forma de pago</dt><dd>{{ d.poliza.periodicidad | etiqueta }}</dd>
            <dt>Comisión</dt><dd>{{ d.poliza.comisionPct | number: '1.0-2' }} % · {{ d.poliza.primaAnual * d.poliza.comisionPct / 100 | currency: 'EUR' }}</dd>
          </dl>
        </article>
        <article class="tarjeta">
          <h2>Tomador</h2>
          <a class="celda-persona" [routerLink]="['/clientes', d.cliente.id]">
            <span class="avatar avatar--sm">{{ d.cliente.nombre[0] }}</span>
            <div><strong>{{ d.cliente.nombre }}</strong><small>{{ d.cliente.nif }}</small></div>
          </a>
          <dl class="datos">
            <dt>Teléfono</dt><dd>{{ d.cliente.telefono }}</dd>
            <dt>Email</dt><dd>{{ d.cliente.email }}</dd>
          </dl>
        </article>
      </section>

      <section class="rejilla rejilla--2">
        <article class="tarjeta tarjeta--tabla">
          <header class="tarjeta__cabecera"><h2>Recibos</h2></header>
          <table class="tabla">
            <thead><tr><th>Recibo</th><th>Emisión</th><th class="num">Importe</th><th>Estado</th></tr></thead>
            <tbody>
              @for (r of d.recibos; track r.id) {
                <tr>
                  <td class="mono">{{ r.numero }}</td>
                  <td>{{ r.fechaEmision | date: 'dd/MM/yyyy' }}</td>
                  <td class="num">{{ r.importe | currency: 'EUR' }}</td>
                  <td><span class="insignia" [attr.data-estado]="r.estado">{{ r.estado | etiqueta }}</span></td>
                </tr>
              } @empty {
                <tr><td colspan="4" class="estado-vacio">Sin recibos</td></tr>
              }
            </tbody>
          </table>
        </article>
        <article class="tarjeta tarjeta--tabla">
          <header class="tarjeta__cabecera"><h2>Siniestros</h2></header>
          <table class="tabla">
            <thead><tr><th>Nº</th><th>Tipo</th><th>Apertura</th><th>Estado</th></tr></thead>
            <tbody>
              @for (s of d.siniestros; track s.id) {
                <tr>
                  <td class="mono">{{ s.numero }}</td>
                  <td>{{ s.tipo }}</td>
                  <td>{{ s.fechaApertura | date: 'dd/MM/yyyy' }}</td>
                  <td><span class="insignia" [attr.data-estado]="s.estado">{{ s.estado | etiqueta }}</span></td>
                </tr>
              } @empty {
                <tr><td colspan="4" class="estado-vacio">Sin siniestros</td></tr>
              }
            </tbody>
          </table>
        </article>
      </section>
    } @else {
      <div class="cargando">Cargando…</div>
    }
  `
})
export class PolizaDetalleComponent {
  private readonly api = inject(ApiService);
  readonly id = input.required<string>();
  readonly d = signal<PolizaDetalle | null>(null);

  constructor() {
    effect(() => {
      const id = Number(this.id());
      this.api.poliza(id).subscribe(d => this.d.set(d));
    });
  }
}
