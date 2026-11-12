import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { EmailEntrante } from '../core/modelos';
import { SesionService } from '../core/sesion.service';
import { CampoPipe, EtiquetaPipe, HaceCuantoPipe } from '../shared/etiquetas.pipe';
import { IconoComponent } from '../shared/icono.component';

type Filtro = 'PENDIENTES' | 'MIOS' | 'TODOS';

@Component({
  selector: 'app-buzon',
  standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule, RouterLink, CampoPipe, EtiquetaPipe, HaceCuantoPipe, IconoComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="cabecera">
      <div>
        <p class="cabecera__antetitulo">Atención al cliente · automatización con IA</p>
        <h1>Buzón</h1>
      </div>
      <div class="buzon__acciones">
        <div class="segmentado">
          <button [class.activo]="filtro() === 'PENDIENTES'" (click)="filtro.set('PENDIENTES')">Por responder</button>
          <button [class.activo]="filtro() === 'MIOS'" (click)="filtro.set('MIOS')">Asignados a mí</button>
          <button [class.activo]="filtro() === 'TODOS'" (click)="filtro.set('TODOS')">Todos</button>
        </div>
        <button class="btn btn--fantasma btn--sm" (click)="limpiar()" title="Borra todos los emails de prueba">
          <app-icono nombre="papelera" [tamano]="14" /> Limpiar pruebas
        </button>
      </div>
    </header>

    <section class="presupuestos">
      <!-- Lista -->
      <div class="presupuestos__lateral">
        @for (e of visibles(); track e.id) {
          <button class="comparativa-item" [class.activa]="seleccionadoId() === e.id" (click)="seleccionar(e)">
            <span class="comparativa-item__fila">
              <span class="buzon__chips">
                <span class="categoria" [attr.data-categoria]="e.categoria">{{ e.categoria | etiqueta }}</span>
                @if (e.urgencia === 'ALTA') { <span class="urgencia" data-urgencia="ALTA">Urgente</span> }
              </span>
              <span class="texto-suave">{{ e.recibidoEn | haceCuanto }}</span>
            </span>
            <strong>{{ e.asunto || '(sin asunto)' }}</strong>
            <small class="buzon__remitente">{{ e.cliente?.nombre ?? e.remitente }}</small>
            @if (e.resumen) { <small class="buzon__resumen-corto">{{ e.resumen }}</small> }
            <span class="comparativa-item__fila">
              <small class="texto-suave">→ {{ e.asignadoA }}</small>
              <span class="insignia" [attr.data-estado]="e.estado">{{ e.estado | etiqueta }}</span>
            </span>
          </button>
        } @empty {
          <div class="estado-vacio">No hay emails {{ filtro() === 'TODOS' ? 'todavía' : 'en esta vista' }}.</div>
        }
      </div>

      <!-- Detalle -->
      <article class="tarjeta">
        @if (seleccionado(); as e) {
          <header class="tarjeta__cabecera">
            <div>
              <p class="cabecera__antetitulo">{{ e.remitente }} · {{ e.recibidoEn | date: 'dd/MM/yyyy HH:mm' }}</p>
              <h2>{{ e.asunto || '(sin asunto)' }}</h2>
            </div>
            <span class="insignia insignia--grande" [attr.data-estado]="e.estado">{{ e.estado | etiqueta }}</span>
          </header>

          <!-- Lo que ha decidido la IA + las reglas -->
          <div class="buzon__clasificacion">
            <div><span class="texto-suave">Categoría</span><span class="categoria" [attr.data-categoria]="e.categoria">{{ e.categoria | etiqueta }}</span></div>
            <div><span class="texto-suave">Urgencia</span><span class="urgencia" [attr.data-urgencia]="e.urgencia">{{ e.urgencia | etiqueta }}</span></div>
            <div><span class="texto-suave">Confianza IA</span><strong>{{ e.confianza != null ? (e.confianza * 100 | number: '1.0-0') + ' %' : '—' }}</strong></div>
            <div><span class="texto-suave">Asignado a</span><strong>{{ e.asignadoA }}</strong></div>
          </div>
          @if (e.resumen) {
            <p class="buzon__resumen"><app-icono nombre="robot" [tamano]="16" /> {{ e.resumen }}</p>
          }

          @if (e.avisos.length) {
            <div class="caja caja--aviso">
              <app-icono nombre="alerta" [tamano]="16" />
              <span>@for (a of e.avisos; track a) { <div>{{ a }}</div> }</span>
            </div>
          }

          <!-- Lo que ha encontrado el backend en la cartera -->
          <h3>Cliente en la cartera</h3>
          @if (e.cliente; as cli) {
            <div class="buzon__ficha">
              <div>
                <a [routerLink]="['/clientes', cli.id]"><strong>{{ cli.nombre }}</strong></a>
                <small class="texto-suave">NIF {{ cli.nif }}@if (cli.telefono) { · {{ cli.telefono }} }</small>
                @if (e.identificadoPor) { <small class="etiqueta">Identificado por {{ identificadoPor(e.identificadoPor) }}</small> }
              </div>
              @if (e.poliza; as p) {
                <div>
                  <a [routerLink]="['/polizas', p.id]"><strong>{{ p.numero }}</strong></a>
                  <small class="texto-suave">{{ p.ramo | etiqueta }} · {{ p.aseguradora }} · {{ p.objetoAsegurado }}</small>
                  <span class="insignia" [attr.data-estado]="p.enVigor ? 'VIGENTE' : 'VENCIDA'">{{ p.enVigor ? 'En vigor' : 'No en vigor' }}</span>
                </div>
              }
            </div>
          } @else {
            <p class="texto-suave">No identificado.</p>
          }

          <h3>Email recibido</h3>
          <pre class="buzon__original">{{ e.texto }}</pre>

          @if (datosExtraidos().length) {
            <h3>Datos extraídos por la IA</h3>
            <div class="buzon__datos">
              @for (d of datosExtraidos(); track d.clave) {
                <span class="texto-suave">{{ d.clave | campo }}</span><strong>{{ d.valor }}</strong>
              }
            </div>
          }

          <!-- Respuesta -->
          <h3>Respuesta</h3>
          @switch (e.estado) {
            @case ('CLASIFICADO') {
              <div class="procesando"><span class="girando"></span> La IA está redactando la respuesta…</div>
            }
            @case ('APROBADO') {
              <div class="procesando"><span class="girando"></span> Aprobada por {{ e.revisor }}. Enviando…</div>
            }
            @case ('ERROR') {
              <div class="caja caja--error"><strong>Error:</strong> {{ e.error }}</div>
            }
            @case ('DESCARTADO') {
              <div class="caja">Descartado por <strong>{{ e.revisor }}</strong>@if (e.error) { · «{{ e.error }}» }</div>
            }
            @case ('RESPONDIDO') {
              <div class="caja"><app-icono nombre="check" [tamano]="16" /> Respondido a {{ e.remitenteEmail }} el {{ e.enviadoEn | date: 'dd/MM/yyyy HH:mm' }} · aprobado por {{ e.revisor }}</div>
              <pre class="buzon__original">{{ e.respuesta }}</pre>
            }
            @case ('PENDIENTE_REVISION') {
              <div class="cabecera-email">
                <span class="texto-suave">Para</span><span>{{ e.remitenteEmail }}</span>
                <span class="texto-suave">Asunto</span><input class="selector" [(ngModel)]="asunto" />
              </div>
              <textarea class="buzon__respuesta" [(ngModel)]="respuesta"></textarea>
              @if (e.error) {
                <div class="caja caja--error" style="margin-top: 12px">{{ e.error }}</div>
              }
              <footer class="revision__acciones">
                <span class="texto-suave">Decides como <strong>{{ sesion.usuario() }}</strong></span>
                <input class="selector" placeholder="Motivo del descarte (opcional)" [(ngModel)]="motivo" />
                <button class="btn btn--peligro-suave" [disabled]="enviando()" (click)="descartar(e)"><app-icono nombre="x" [tamano]="16" /> Descartar</button>
                <button class="btn btn--primario" [disabled]="enviando() || !respuesta.trim()" (click)="aprobar(e)"><app-icono nombre="check" [tamano]="16" /> Aprobar y enviar</button>
              </footer>
            }
          }
        } @else {
          <div class="estado-vacio">
            <app-icono nombre="email" [tamano]="28" />
            <p>Los emails del buzón general llegan aquí ya clasificados, con el cliente identificado<br />y una respuesta sugerida. Tú la revisas antes de que salga.</p>
          </div>
        }
      </article>
    </section>
  `
})
export class BuzonComponent {
  private readonly api = inject(ApiService);
  readonly sesion = inject(SesionService);

  readonly emails = signal<EmailEntrante[]>([]);
  readonly filtro = signal<Filtro>('PENDIENTES');
  readonly seleccionadoId = signal<number | null>(null);
  readonly seleccionado = computed(() => this.emails().find(e => e.id === this.seleccionadoId()) ?? null);
  readonly enviando = signal(false);

  asunto = '';
  respuesta = '';
  motivo = '';

  readonly visibles = computed(() => {
    const lista = this.emails();
    switch (this.filtro()) {
      case 'PENDIENTES':
        return lista.filter(e => e.estado === 'CLASIFICADO' || e.estado === 'PENDIENTE_REVISION' || e.estado === 'ERROR');
      case 'MIOS':
        return lista.filter(e => e.asignadoA === this.sesion.usuario());
      default:
        return lista;
    }
  });

  /** Solo los datos que la IA ha encontrado (los null no se muestran). */
  readonly datosExtraidos = computed(() =>
    Object.entries(this.seleccionado()?.datos ?? {})
      .filter(([, v]) => v !== null && v !== undefined && v !== '')
      .map(([clave, valor]) => ({ clave, valor: String(valor) }))
  );

  constructor() {
    this.cargar();
    // Los emails llegan en cualquier momento desde n8n: se refresca cada 5 s.
    const temporizador = setInterval(() => this.cargar(), 5000);
    inject(DestroyRef).onDestroy(() => clearInterval(temporizador));
  }

  identificadoPor(clave: string): string {
    return ({ NIF: 'NIF', EMAIL: 'email del remitente', POLIZA: 'nº de póliza', MATRICULA: 'matrícula' } as Record<string, string>)[clave] ?? clave;
  }

  cargar(): void {
    this.api.emails().subscribe(lista => {
      const antes = this.seleccionado();
      this.emails.set(lista);
      const ahora = this.seleccionado();
      // Si el seleccionado acaba de recibir el borrador, se carga en el editor (sin pisar lo que estés escribiendo).
      if (ahora && antes?.estado !== ahora.estado) {
        this.prepararEdicion(ahora);
      }
    });
  }

  seleccionar(e: EmailEntrante): void {
    this.seleccionadoId.set(e.id);
    this.prepararEdicion(e);
  }

  limpiar(): void {
    if (!confirm('¿Borrar todos los emails del buzón? Solo para limpiar las pruebas.')) {
      return;
    }
    this.api.limpiarEmails().subscribe(() => {
      this.seleccionadoId.set(null);
      this.cargar();
    });
  }

  aprobar(e: EmailEntrante): void {
    this.enviando.set(true);
    this.api.aprobarEmail(e.id, this.sesion.usuario(), this.asunto, this.respuesta).subscribe({
      next: actualizado => this.terminarAccion(actualizado),
      error: err => {
        this.enviando.set(false);
        this.actualizarLocal({ error: err?.error?.error ?? 'No se pudo aprobar' });
      }
    });
  }

  descartar(e: EmailEntrante): void {
    this.enviando.set(true);
    this.api.descartarEmail(e.id, this.sesion.usuario(), this.motivo).subscribe({
      next: actualizado => this.terminarAccion(actualizado),
      error: () => this.enviando.set(false)
    });
  }

  private terminarAccion(actualizado: EmailEntrante): void {
    this.enviando.set(false);
    this.motivo = '';
    this.emails.update(lista => lista.map(x => (x.id === actualizado.id ? actualizado : x)));
  }

  private prepararEdicion(e: EmailEntrante): void {
    this.asunto = e.respuestaAsunto ?? '';
    this.respuesta = e.respuesta ?? '';
  }

  private actualizarLocal(cambios: Partial<EmailEntrante>): void {
    const id = this.seleccionadoId();
    this.emails.update(lista => lista.map(x => (x.id === id ? { ...x, ...cambios } : x)));
  }
}
