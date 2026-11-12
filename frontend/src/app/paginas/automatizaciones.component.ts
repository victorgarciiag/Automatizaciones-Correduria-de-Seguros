import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe, PercentPipe } from '@angular/common';
import { ApiService } from '../core/api.service';
import { Ejecucion, Pagina, PanelAutomatizaciones } from '../core/modelos';
import { EtiquetaPipe } from '../shared/etiquetas.pipe';
import { IconoComponent } from '../shared/icono.component';
import { GraficoEjecucionesComponent } from '../shared/grafico-ejecuciones.component';
import { PaginadorComponent } from '../shared/paginador.component';

@Component({
  selector: 'app-automatizaciones',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, DecimalPipe, PercentPipe, EtiquetaPipe, IconoComponent,
    GraficoEjecucionesComponent, PaginadorComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="cabecera">
      <div>
        <p class="cabecera__antetitulo">Trazabilidad, coste y ahorro</p>
        <h1>Automatizaciones</h1>
      </div>
      <div class="segmentado">
        @for (r of rangos; track r) {
          <button [class.activo]="dias() === r" (click)="cambiarRango(r)">{{ r }} días</button>
        }
      </div>
    </header>

    @if (panel(); as p) {
      @if (p.hayDatosDemo) {
        <div class="banda-demo">
          <app-icono nombre="alerta" [tamano]="16" />
          <span>Estás viendo <strong>datos de demostración</strong> generados al arrancar. Bórralos antes de medir automatizaciones reales.</span>
          <button class="btn btn--fantasma btn--sm" (click)="borrarDemo()">
            <app-icono nombre="papelera" [tamano]="14" /> Borrar datos demo
          </button>
        </div>
      }

      <section class="kpis">
        <div class="kpi">
          <span class="kpi__icono kpi__icono--azul"><app-icono nombre="automatizaciones" /></span>
          <span class="kpi__etiqueta">Ejecuciones</span>
          <strong class="kpi__valor">{{ p.ejecuciones | number }}</strong>
          <span class="kpi__detalle">{{ p.tasaExito / 100 | percent: '1.0-1' }} sin errores</span>
        </div>
        <div class="kpi kpi--destacado">
          <span class="kpi__icono"><app-icono nombre="reloj" /></span>
          <span class="kpi__etiqueta">Horas ahorradas</span>
          <strong class="kpi__valor">{{ p.horasAhorradas | number: '1.0-1' }} h</strong>
          <span class="kpi__detalle">≈ {{ p.horasAhorradas / 7.5 | number: '1.0-1' }} jornadas de trabajo</span>
        </div>
        <div class="kpi">
          <span class="kpi__icono kpi__icono--verde"><app-icono nombre="euro" /></span>
          <span class="kpi__etiqueta">Ahorro estimado</span>
          <strong class="kpi__valor">{{ p.ahorroEur | currency: 'EUR' : 'symbol' : '1.0-0' }}</strong>
          <span class="kpi__detalle">a 18 €/h de coste de empleado</span>
        </div>
        <div class="kpi">
          <span class="kpi__icono kpi__icono--naranja"><app-icono nombre="robot" /></span>
          <span class="kpi__etiqueta">Coste de IA</span>
          <strong class="kpi__valor">{{ p.coste | currency: 'EUR' }}</strong>
          <span class="kpi__detalle">{{ p.ejecuciones ? (p.coste / p.ejecuciones | currency: 'EUR' : 'symbol' : '1.3-3') : '–' }} por ejecución</span>
        </div>
        <div class="kpi">
          <span class="kpi__icono kpi__icono--morado"><app-icono nombre="flecha" /></span>
          <span class="kpi__etiqueta">Retorno</span>
          <strong class="kpi__valor">{{ retorno() }}</strong>
          <span class="kpi__detalle">euros ahorrados por euro de IA</span>
        </div>
      </section>

      <article class="tarjeta">
        <header class="tarjeta__cabecera">
          <h2>Ejecuciones por día</h2>
          <span class="texto-suave">{{ p.revisionesPendientes }} pendientes de revisión humana</span>
        </header>
        <app-grafico-ejecuciones [dias]="p.porDia" [mostrarEtiquetas]="p.dias <= 31" />
      </article>

      <h2 class="titulo-seccion">Catálogo de automatizaciones</h2>
      <section class="catalogo">
        @for (a of p.automatizaciones; track a.codigo) {
          <article class="automatizacion" [class.automatizacion--seleccionada]="filtro() === a.codigo"
                   (click)="filtrar(filtro() === a.codigo ? '' : a.codigo)">
            <header>
              <span class="automatizacion__orden">{{ a.orden }}</span>
              <span class="insignia" [attr.data-estado]="a.estado">{{ a.estado | etiqueta }}</span>
            </header>
            <strong>{{ a.nombre }}</strong>
            <p>{{ a.descripcion }}</p>
            <footer>
              @if (a.ejecuciones > 0) {
                <span><b>{{ a.ejecuciones }}</b> ejec.</span>
                <span><b>{{ a.horasAhorradas | number: '1.0-1' }} h</b> ahorradas</span>
                <span><b>{{ a.coste | currency: 'EUR' }}</b> coste</span>
              } @else {
                <span class="texto-suave">{{ a.area }} · {{ a.minutosManuales }} min a mano por caso</span>
              }
            </footer>
          </article>
        }
      </section>
    }

    <article class="tarjeta tarjeta--tabla">
      <header class="tarjeta__cabecera">
        <h2>Registro de ejecuciones{{ filtro() ? ' · ' + nombreFiltro() : '' }}</h2>
        @if (filtro()) {
          <button class="btn btn--fantasma btn--sm" (click)="filtrar('')">Quitar filtro</button>
        }
      </header>
      <table class="tabla">
        <thead>
          <tr><th>Fecha</th><th>Automatización</th><th>Resultado</th><th>Detalle</th><th class="num">Duración</th><th class="num">Tokens</th><th class="num">Coste</th><th class="num">Ahorro</th></tr>
        </thead>
        <tbody>
          @for (e of ejecuciones()?.contenido; track e.id) {
            <tr>
              <td>{{ e.inicio | date: 'dd/MM HH:mm:ss' }}</td>
              <td>{{ nombre(e.automatizacion) }}</td>
              <td><span class="insignia" [attr.data-estado]="e.resultado">{{ e.resultado | etiqueta }}</span></td>
              <td class="recortar">{{ e.detalle }}</td>
              <td class="num">{{ e.duracionMs / 1000 | number: '1.1-1' }} s</td>
              <td class="num">{{ e.tokensEntrada + e.tokensSalida | number }}</td>
              <td class="num">{{ e.costeEur | currency: 'EUR' : 'symbol' : '1.4-4' }}</td>
              <td class="num">{{ e.minutosAhorrados | number: '1.0-1' }} min</td>
            </tr>
          } @empty {
            <tr><td colspan="8" class="estado-vacio">Todavía no hay ejecuciones. Cuando n8n registre la primera, aparecerá aquí.</td></tr>
          }
        </tbody>
      </table>
      @if (ejecuciones(); as e) {
        <app-paginador [pagina]="e.pagina" [tamano]="e.tamano" [total]="e.total" (cambiar)="cargarEjecuciones($event)" />
      }
    </article>
  `
})
export class AutomatizacionesComponent {
  private readonly api = inject(ApiService);

  readonly rangos = [7, 30, 90];
  readonly dias = signal(30);
  readonly panel = signal<PanelAutomatizaciones | null>(null);
  readonly ejecuciones = signal<Pagina<Ejecucion> | null>(null);
  readonly filtro = signal('');

  readonly retorno = computed(() => {
    const p = this.panel();
    if (!p || p.coste === 0) return '–';
    return `×${Math.round(p.ahorroEur / p.coste).toLocaleString('es-ES')}`;
  });
  readonly nombreFiltro = computed(() => this.nombre(this.filtro()));

  constructor() {
    this.cargarPanel();
    this.cargarEjecuciones(0);
  }

  cambiarRango(dias: number): void {
    this.dias.set(dias);
    this.cargarPanel();
  }

  filtrar(codigo: string): void {
    this.filtro.set(codigo);
    this.cargarEjecuciones(0);
  }

  cargarPanel(): void {
    this.api.panelAutomatizaciones(this.dias()).subscribe(p => this.panel.set(p));
  }

  cargarEjecuciones(pagina: number): void {
    this.api.ejecuciones({ automatizacion: this.filtro(), pagina, tamano: 12 }).subscribe(e => this.ejecuciones.set(e));
  }

  nombre(codigo: string): string {
    return this.panel()?.automatizaciones.find(a => a.codigo === codigo)?.nombre ?? codigo;
  }

  borrarDemo(): void {
    if (!confirm('¿Borrar las ejecuciones y revisiones de demostración? Los datos de la cartera no se tocan.')) {
      return;
    }
    this.api.borrarDatosDemo().subscribe(() => {
      this.cargarPanel();
      this.cargarEjecuciones(0);
    });
  }
}
