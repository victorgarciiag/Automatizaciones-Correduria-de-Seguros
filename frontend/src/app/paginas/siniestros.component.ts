import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { EstadoSiniestro, Siniestro } from '../core/modelos';
import { EtiquetaPipe, HaceCuantoPipe } from '../shared/etiquetas.pipe';
import { IconoComponent } from '../shared/icono.component';

interface Columna {
  estado: EstadoSiniestro;
  titulo: string;
  ayuda: string;
}

@Component({
  selector: 'app-siniestros',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink, EtiquetaPipe, HaceCuantoPipe, IconoComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="cabecera">
      <div>
        <p class="cabecera__antetitulo">Siniestros</p>
        <h1>Tablero de siniestros</h1>
      </div>
      <div class="filtros filtros--compacto">
        <div class="segmentado">
          @for (t of tramitadores; track t) {
            <button [class.activo]="tramitador() === t" (click)="tramitador.set(t)">{{ t || 'Todos' }}</button>
          }
        </div>
        <label class="interruptor">
          <input type="checkbox" [checked]="soloIa()" (change)="soloIa.set(!soloIa())" />
          Solo abiertos por IA
        </label>
      </div>
    </header>

    <p class="texto-suave ayuda-tablero">Arrastra una tarjeta a otra columna para cambiar su estado.</p>

    <section class="kanban">
      @for (col of columnas; track col.estado) {
        <div class="kanban__columna" [class.sobre]="columnaSobre() === col.estado"
             (dragover)="$event.preventDefault(); columnaSobre.set(col.estado)"
             (dragleave)="columnaSobre.set(null)"
             (drop)="soltar(col.estado)">
          <header class="kanban__cabecera">
            <span class="estado" [attr.data-estado]="col.estado"></span>
            <strong>{{ col.titulo }}</strong>
            <span class="kanban__total">{{ porEstado()[col.estado].length }}</span>
          </header>
          <p class="kanban__ayuda">{{ col.ayuda }}</p>
          @for (s of porEstado()[col.estado]; track s.id) {
            <article class="kanban__tarjeta" draggable="true" (dragstart)="arrastrando = s" (click)="seleccionado.set(s)"
                     [class.seleccionada]="seleccionado()?.id === s.id">
              <div class="kanban__fila">
                <span class="mono texto-suave">{{ s.numero }}</span>
                @if (s.origen === 'AUTOMATIZACION') {
                  <span class="etiqueta etiqueta--ia"><app-icono nombre="robot" [tamano]="12" /> IA</span>
                }
              </div>
              <strong>{{ s.tipo }}</strong>
              <span class="texto-suave">{{ s.clienteNombre }}</span>
              <div class="kanban__fila kanban__pie">
                <span class="ramo" [attr.data-ramo]="s.ramo">{{ s.ramo | etiqueta }}</span>
                @if (s.documentosPendientes.length) {
                  <span class="etiqueta etiqueta--aviso">{{ s.documentosPendientes.length }} doc.</span>
                }
                <span class="texto-suave kanban__tiempo">{{ s.fechaApertura | haceCuanto }}</span>
              </div>
            </article>
          } @empty {
            <div class="kanban__vacio">Sin siniestros</div>
          }
        </div>
      }
    </section>

    @if (seleccionado(); as s) {
      <div class="velo" (click)="seleccionado.set(null)"></div>
      <aside class="panel-lateral">
        <header class="panel-lateral__cabecera">
          <div>
            <span class="mono texto-suave">{{ s.numero }}</span>
            <h2>{{ s.tipo }}</h2>
          </div>
          <button class="btn btn--icono" (click)="seleccionado.set(null)" aria-label="Cerrar"><app-icono nombre="x" /></button>
        </header>
        <div class="panel-lateral__cuerpo">
          <div class="chips">
            <span class="insignia" [attr.data-estado]="s.estado">{{ s.estado | etiqueta }}</span>
            <span class="etiqueta">{{ s.canal | etiqueta }}</span>
            @if (s.origen === 'AUTOMATIZACION') {
              <span class="etiqueta etiqueta--ia"><app-icono nombre="robot" [tamano]="12" /> Abierto por IA</span>
            }
          </div>
          <p class="descripcion">{{ s.descripcion }}</p>

          @if (s.documentosPendientes.length) {
            <div class="caja caja--aviso">
              <strong>Documentación pendiente</strong>
              <ul>
                @for (doc of s.documentosPendientes; track doc) {
                  <li>{{ doc }}</li>
                }
              </ul>
            </div>
          }

          <dl class="datos">
            <dt>Cliente</dt><dd><a class="enlace" [routerLink]="['/clientes', s.clienteId]">{{ s.clienteNombre }}</a></dd>
            <dt>Teléfono</dt><dd>{{ s.clienteTelefono }}</dd>
            <dt>Póliza</dt><dd><a class="enlace mono" [routerLink]="['/polizas', s.polizaId]">{{ s.polizaNumero }}</a></dd>
            <dt>Aseguradora</dt><dd>{{ s.aseguradoraNombre }}</dd>
            <dt>Asegurado</dt><dd>{{ s.objetoAsegurado }}</dd>
            <dt>Ocurrencia</dt><dd>{{ s.fechaOcurrencia | date: 'dd/MM/yyyy' }}</dd>
            <dt>Apertura</dt><dd>{{ s.fechaApertura | date: 'dd/MM/yyyy HH:mm' }}</dd>
            <dt>Tramitador</dt><dd>{{ s.tramitador }}</dd>
            @if (s.importeEstimado) {
              <dt>Importe estimado</dt><dd>{{ s.importeEstimado | currency: 'EUR' }}</dd>
            }
          </dl>

          <h3>Cambiar estado</h3>
          <div class="botonera">
            @for (col of columnas; track col.estado) {
              <button class="btn btn--fantasma btn--sm" [disabled]="s.estado === col.estado" (click)="cambiar(s, col.estado)">{{ col.titulo }}</button>
            }
            <button class="btn btn--fantasma btn--sm btn--peligro" [disabled]="s.estado === 'RECHAZADO'" (click)="cambiar(s, 'RECHAZADO')">Rechazado</button>
          </div>
        </div>
      </aside>
    }
  `
})
export class SiniestrosComponent {
  private readonly api = inject(ApiService);

  readonly columnas: Columna[] = [
    { estado: 'ABIERTO', titulo: 'Abiertos', ayuda: 'Recién declarados' },
    { estado: 'DOCUMENTACION_PENDIENTE', titulo: 'Falta documentación', ayuda: 'Esperando al cliente' },
    { estado: 'EN_TRAMITE', titulo: 'En trámite', ayuda: 'En la aseguradora' },
    { estado: 'CERRADO', titulo: 'Cerrados', ayuda: 'Últimos 30 días' }
  ];
  readonly tramitadores = ['', 'Manuel Pérez', 'Elena Castro'];

  readonly siniestros = signal<Siniestro[]>([]);
  readonly seleccionado = signal<Siniestro | null>(null);
  readonly tramitador = signal('');
  readonly soloIa = signal(false);
  readonly columnaSobre = signal<EstadoSiniestro | null>(null);
  arrastrando: Siniestro | null = null;

  readonly porEstado = computed(() => {
    const grupos: Record<EstadoSiniestro, Siniestro[]> = {
      ABIERTO: [], DOCUMENTACION_PENDIENTE: [], EN_TRAMITE: [], CERRADO: [], RECHAZADO: []
    };
    for (const s of this.siniestros()) {
      if (this.tramitador() && s.tramitador !== this.tramitador()) continue;
      if (this.soloIa() && s.origen !== 'AUTOMATIZACION') continue;
      // Los rechazados se muestran junto a los cerrados
      grupos[s.estado === 'RECHAZADO' ? 'CERRADO' : s.estado].push(s);
    }
    return grupos;
  });

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.api.siniestros().subscribe(s => this.siniestros.set(s));
  }

  soltar(estado: EstadoSiniestro): void {
    this.columnaSobre.set(null);
    if (this.arrastrando && this.arrastrando.estado !== estado) {
      this.cambiar(this.arrastrando, estado);
    }
    this.arrastrando = null;
  }

  cambiar(s: Siniestro, estado: EstadoSiniestro): void {
    this.api.cambiarEstadoSiniestro(s.id, estado).subscribe(actualizado => {
      this.siniestros.update(lista => lista.map(x => (x.id === actualizado.id ? actualizado : x)));
      if (this.seleccionado()?.id === actualizado.id) {
        this.seleccionado.set(actualizado);
      }
    });
  }
}
