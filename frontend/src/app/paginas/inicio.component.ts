import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe, PercentPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { Dashboard } from '../core/modelos';
import { SesionService } from '../core/sesion.service';
import { EtiquetaPipe, HaceCuantoPipe } from '../shared/etiquetas.pipe';
import { IconoComponent } from '../shared/icono.component';
import { GraficoEjecucionesComponent } from '../shared/grafico-ejecuciones.component';

@Component({
  selector: 'app-inicio',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, DecimalPipe, PercentPipe, RouterLink, EtiquetaPipe, HaceCuantoPipe,
    IconoComponent, GraficoEjecucionesComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="cabecera">
      <div>
        <p class="cabecera__antetitulo">{{ hoy | date: "EEEE, d 'de' MMMM" }}</p>
        <h1>{{ saludo() }}, {{ nombre() }}</h1>
      </div>
      <a class="btn btn--primario" routerLink="/revision">
        <app-icono nombre="revision" [tamano]="16" />
        Revisar pendientes
      </a>
    </header>

    @if (error()) {
      <div class="estado-vacio estado-vacio--error">No se ha podido conectar con el backend. ¿Está arrancado en el puerto 8080?</div>
    }

    @if (d(); as d) {
      <section class="kpis">
        <a class="kpi" routerLink="/polizas">
          <span class="kpi__icono kpi__icono--azul"><app-icono nombre="polizas" /></span>
          <span class="kpi__etiqueta">Pólizas vigentes</span>
          <strong class="kpi__valor">{{ d.polizasVigentes | number }}</strong>
          <span class="kpi__detalle">{{ d.clientes | number }} clientes</span>
        </a>
        <div class="kpi">
          <span class="kpi__icono kpi__icono--verde"><app-icono nombre="euro" /></span>
          <span class="kpi__etiqueta">Primas en cartera</span>
          <strong class="kpi__valor">{{ d.primasCartera | currency: 'EUR' : 'symbol' : '1.0-0' }}</strong>
          <span class="kpi__detalle">{{ d.vencimientos30d }} renovaciones en 30 días</span>
        </div>
        <a class="kpi" routerLink="/siniestros">
          <span class="kpi__icono kpi__icono--naranja"><app-icono nombre="siniestros" /></span>
          <span class="kpi__etiqueta">Siniestros abiertos</span>
          <strong class="kpi__valor">{{ d.siniestrosAbiertos }}</strong>
          <span class="kpi__detalle">en trámite o pendientes</span>
        </a>
        <a class="kpi" routerLink="/recibos">
          <span class="kpi__icono kpi__icono--rojo"><app-icono nombre="recibos" /></span>
          <span class="kpi__etiqueta">Recibos devueltos</span>
          <strong class="kpi__valor">{{ d.recibosDevueltos }}</strong>
          <span class="kpi__detalle">{{ d.importeDevuelto | currency: 'EUR' }} por cobrar</span>
        </a>
        <a class="kpi kpi--destacado" routerLink="/automatizaciones">
          <span class="kpi__icono"><app-icono nombre="reloj" /></span>
          <span class="kpi__etiqueta">Horas ahorradas (14 días)</span>
          <strong class="kpi__valor">{{ d.automatizaciones.horasAhorradas | number: '1.0-1' }} h</strong>
          <span class="kpi__detalle">≈ {{ d.automatizaciones.ahorroEur | currency: 'EUR' : 'symbol' : '1.0-0' }} · coste IA {{ d.automatizaciones.coste | currency: 'EUR' }}</span>
        </a>
      </section>

      <section class="rejilla rejilla--2-1">
        <article class="tarjeta">
          <header class="tarjeta__cabecera">
            <div>
              <h2>Automatizaciones · últimos 14 días</h2>
              <p class="texto-suave">{{ d.automatizaciones.ejecuciones | number }} ejecuciones · {{ d.automatizaciones.tasaExito / 100 | percent: '1.0-1' }} sin errores</p>
            </div>
            @if (d.automatizaciones.hayDatosDemo) {
              <span class="etiqueta etiqueta--demo">Datos de demostración</span>
            }
          </header>
          <app-grafico-ejecuciones [dias]="d.automatizaciones.porDia" />
        </article>

        <article class="tarjeta">
          <header class="tarjeta__cabecera">
            <h2>Pendiente de revisar</h2>
            <a class="enlace" routerLink="/revision">Ver todo</a>
          </header>
          @for (r of d.revisiones; track r.id) {
            <a class="lista-item" routerLink="/revision">
              <span class="lista-item__icono"><app-icono nombre="robot" [tamano]="16" /></span>
              <span class="lista-item__texto">
                <strong>{{ r.titulo }}</strong>
                <small>{{ r.automatizacionNombre }} · {{ r.creadaEn | haceCuanto }}</small>
              </span>
              @if (r.confianza != null) {
                <span class="confianza" [class.confianza--baja]="r.confianza < 0.7">{{ r.confianza | percent }}</span>
              }
            </a>
          } @empty {
            <div class="estado-vacio">Todo revisado. Nada pendiente.</div>
          }
        </article>
      </section>

      <section class="rejilla rejilla--3">
        <article class="tarjeta">
          <header class="tarjeta__cabecera"><h2>Cartera por ramo</h2></header>
          <div class="barras-h">
            @for (r of d.carteraPorRamo; track r.ramo) {
              <div class="barras-h__fila">
                <span class="barras-h__etiqueta">{{ r.ramo | etiqueta }}</span>
                <span class="barras-h__pista"><span class="barras-h__barra" [attr.data-ramo]="r.ramo" [style.width.%]="(r.polizas / maxRamo()) * 100"></span></span>
                <span class="barras-h__valor">{{ r.polizas }}</span>
              </div>
            }
          </div>
        </article>

        <article class="tarjeta">
          <header class="tarjeta__cabecera">
            <h2>Próximos vencimientos</h2>
            <a class="enlace" [routerLink]="['/polizas']" [queryParams]="{ vence: 30 }">Ver todos</a>
          </header>
          @for (p of d.proximosVencimientos; track p.id) {
            <a class="lista-item" [routerLink]="['/polizas', p.id]">
              <span class="ramo" [attr.data-ramo]="p.ramo">{{ p.ramo | etiqueta }}</span>
              <span class="lista-item__texto">
                <strong>{{ p.clienteNombre }}</strong>
                <small>{{ p.aseguradoraNombre }} · {{ p.primaAnual | currency: 'EUR' }}</small>
              </span>
              <span class="texto-suave">{{ p.fechaVencimiento | date: 'd MMM' }}</span>
            </a>
          }
        </article>

        <article class="tarjeta">
          <header class="tarjeta__cabecera">
            <h2>Siniestros recientes</h2>
            <a class="enlace" routerLink="/siniestros">Tablero</a>
          </header>
          @for (s of d.ultimosSiniestros; track s.id) {
            <a class="lista-item" routerLink="/siniestros">
              <span class="estado" [attr.data-estado]="s.estado"></span>
              <span class="lista-item__texto">
                <strong>{{ s.tipo }}</strong>
                <small>{{ s.clienteNombre }} · {{ s.estado | etiqueta }}</small>
              </span>
              @if (s.origen === 'AUTOMATIZACION') {
                <span class="etiqueta etiqueta--ia" title="Abierto por una automatización"><app-icono nombre="robot" [tamano]="12" /> IA</span>
              }
            </a>
          }
        </article>
      </section>
    } @else if (!error()) {
      <div class="cargando">Cargando…</div>
    }
  `
})
export class InicioComponent {
  private readonly api = inject(ApiService);
  private readonly sesion = inject(SesionService);

  readonly hoy = new Date();
  readonly d = signal<Dashboard | null>(null);
  readonly error = signal(false);
  readonly nombre = computed(() => this.sesion.usuario().split(' ')[0]);
  readonly maxRamo = computed(() => Math.max(1, ...(this.d()?.carteraPorRamo ?? []).map(r => r.polizas)));

  constructor() {
    this.api.dashboard().subscribe({ next: d => this.d.set(d), error: () => this.error.set(true) });
  }

  saludo(): string {
    const h = this.hoy.getHours();
    return h < 14 ? 'Buenos días' : h < 21 ? 'Buenas tardes' : 'Buenas noches';
  }
}
