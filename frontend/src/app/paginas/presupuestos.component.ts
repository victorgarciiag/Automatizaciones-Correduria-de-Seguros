import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ApiService } from '../core/api.service';
import { Comparativa } from '../core/modelos';
import { SesionService } from '../core/sesion.service';
import { EtiquetaPipe, HaceCuantoPipe } from '../shared/etiquetas.pipe';
import { IconoComponent } from '../shared/icono.component';

/** Fila de la tabla de datos extraídos (los campos vienen de la IA, por eso se leen con cuidado). */
interface FilaDatos {
  aseguradora: string;
  precio: number | null;
  franquicia: number | null;
  sustitucion: boolean | null;
}

@Component({
  selector: 'app-presupuestos',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, FormsModule, EtiquetaPipe, HaceCuantoPipe, IconoComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="cabecera">
      <div>
        <p class="cabecera__antetitulo">Comercial · automatización con IA</p>
        <h1>Comparativa de presupuestos</h1>
      </div>
      <button class="btn btn--fantasma btn--sm" (click)="limpiar()" title="Borra todas las comparativas de prueba">
        <app-icono nombre="papelera" [tamano]="14" /> Limpiar pruebas
      </button>
    </header>

    <section class="presupuestos">
      <div class="presupuestos__lateral">
        <!-- Nueva comparativa -->
        <article class="tarjeta">
          <h2>Nueva comparativa</h2>
          <form class="presupuestos__form" (ngSubmit)="crear()">
            <label class="campo"><span>Cliente</span>
              <input name="cliente" [(ngModel)]="cliente" required placeholder="Marta Delgado Vega" />
            </label>
            <label class="campo"><span>Email del cliente</span>
              <input name="email" type="email" [(ngModel)]="email" required placeholder="marta.delgado@correo.example" />
            </label>
            <label class="campo"><span>Vehículo</span>
              <input name="vehiculo" [(ngModel)]="vehiculo" required placeholder="Toyota C-HR Hybrid 2022" />
            </label>
            <label class="subida">
              <app-icono nombre="subir" [tamano]="22" />
              <strong>{{ ficheros().length ? 'Cambiar PDFs' : 'Sube los presupuestos en PDF' }}</strong>
              <small>Uno por aseguradora (máx. 6)</small>
              <input type="file" accept=".pdf,application/pdf" multiple (change)="elegirFicheros($event)" />
            </label>
            @if (ficheros().length) {
              <ul class="ficheros">
                @for (f of ficheros(); track f.name) {
                  <li>📄 {{ f.name }}</li>
                }
              </ul>
            }
            @if (errorAlta()) {
              <div class="caja caja--error">{{ errorAlta() }}</div>
            }
            <button class="btn btn--primario" type="submit" [disabled]="!formularioValido() || creando()">
              <app-icono nombre="automatizaciones" [tamano]="16" />
              {{ creando() ? 'Enviando…' : 'Comparar con IA' }}
            </button>
          </form>
        </article>

        <!-- Historial -->
        @for (c of comparativas(); track c.id) {
          <button class="comparativa-item" [class.activa]="seleccionadaId() === c.id" (click)="seleccionar(c)">
            <span class="comparativa-item__fila">
              <span class="insignia" [attr.data-estado]="c.estado">{{ c.estado | etiqueta }}</span>
              <span class="texto-suave">{{ c.creadaEn | haceCuanto }}</span>
            </span>
            <strong>{{ c.clienteNombre }}</strong>
            <small>{{ c.vehiculo }} · {{ c.numPresupuestos }} presupuestos</small>
          </button>
        } @empty {
          <div class="estado-vacio">Aún no hay comparativas.</div>
        }
      </div>

      <!-- Detalle -->
      <article class="tarjeta">
        @if (seleccionada(); as c) {
          <header class="tarjeta__cabecera">
            <div>
              <p class="cabecera__antetitulo">{{ c.vehiculo }} · {{ c.clienteEmail }}</p>
              <h2>{{ c.clienteNombre }}</h2>
            </div>
            <span class="insignia insignia--grande" [attr.data-estado]="c.estado">{{ c.estado | etiqueta }}</span>
          </header>

          @switch (c.estado) {
            @case ('PROCESANDO') {
              <div class="procesando"><span class="girando"></span> La IA está leyendo los {{ c.numPresupuestos }} presupuestos y redactando el email…</div>
            }
            @case ('APROBADA') {
              <div class="procesando"><span class="girando"></span> Aprobada por {{ c.revisor }}. Enviando el email…</div>
            }
            @case ('ERROR') {
              <div class="caja caja--error"><strong>Error:</strong> {{ c.error }}</div>
            }
            @case ('RECHAZADA') {
              <div class="caja">Rechazada por <strong>{{ c.revisor }}</strong>@if (c.error) { · «{{ c.error }}» }</div>
            }
            @case ('ENVIADA') {
              <div class="caja"><app-icono nombre="check" [tamano]="16" /> Enviada a {{ c.clienteEmail }} el {{ c.enviadaEn | date: 'dd/MM/yyyy HH:mm' }} · aprobada por {{ c.revisor }}</div>
            }
          }

          @if (filas().length) {
            <h3>Datos extraídos por la IA</h3>
            <table class="tabla">
              <thead><tr><th>Aseguradora</th><th class="num">Precio anual</th><th class="num">Franquicia</th><th>Vehículo sustitución</th></tr></thead>
              <tbody>
                @for (f of filas(); track f.aseguradora) {
                  <tr>
                    <td><strong>{{ f.aseguradora }}</strong></td>
                    <td class="num">{{ f.precio != null ? (f.precio | currency: 'EUR') : '—' }}</td>
                    <td class="num">
                      @if (f.franquicia != null) { {{ f.franquicia | currency: 'EUR' }} }
                      @else { <span class="etiqueta etiqueta--aviso">No indicada</span> }
                    </td>
                    <td>{{ f.sustitucion == null ? '—' : (f.sustitucion ? 'Sí' : 'No') }}</td>
                  </tr>
                }
              </tbody>
            </table>
          }

          @if (c.emailHtml) {
            <h3>{{ c.estado === 'PENDIENTE_REVISION' ? 'Borrador del email (revísalo antes de enviar)' : 'Email' }}</h3>
            <div class="cabecera-email">
              <span class="texto-suave">Para</span><span>{{ c.clienteEmail }}</span>
              <span class="texto-suave">Asunto</span>
              @if (c.estado === 'PENDIENTE_REVISION') {
                <input class="selector" [(ngModel)]="asunto" />
              } @else {
                <strong>{{ c.emailAsunto }}</strong>
              }
            </div>

            @if (editando()) {
              <textarea class="editor-html" [(ngModel)]="html"></textarea>
            } @else {
              <iframe class="vista-email" sandbox="" [srcdoc]="vistaPrevia()"></iframe>
            }

            @if (c.estado === 'PENDIENTE_REVISION') {
              @if (c.error) {
                <div class="caja caja--error" style="margin-top: 12px">{{ c.error }}</div>
              }
              <footer class="revision__acciones">
                <button class="btn btn--fantasma btn--sm" (click)="alternarEdicion()">{{ editando() ? 'Ver vista previa' : 'Editar HTML' }}</button>
                <span class="texto-suave">Decides como <strong>{{ sesion.usuario() }}</strong></span>
                <input class="selector" placeholder="Motivo del rechazo (opcional)" [(ngModel)]="motivo" />
                <button class="btn btn--peligro-suave" [disabled]="enviando()" (click)="rechazar(c)"><app-icono nombre="x" [tamano]="16" /> Rechazar</button>
                <button class="btn btn--primario" [disabled]="enviando()" (click)="aprobar(c)"><app-icono nombre="check" [tamano]="16" /> Aprobar y enviar</button>
              </footer>
            }
          }
        } @else {
          <div class="estado-vacio">
            <app-icono nombre="balanza" [tamano]="28" />
            <p>Sube los presupuestos de un cliente y la IA preparará la comparativa.<br />Tú la revisas antes de que salga.</p>
          </div>
        }
      </article>
    </section>
  `
})
export class PresupuestosComponent {
  private readonly api = inject(ApiService);
  private readonly sanitizer = inject(DomSanitizer);
  readonly sesion = inject(SesionService);

  readonly comparativas = signal<Comparativa[]>([]);
  readonly seleccionadaId = signal<number | null>(null);
  readonly seleccionada = computed(() => this.comparativas().find(c => c.id === this.seleccionadaId()) ?? null);
  readonly ficheros = signal<File[]>([]);
  readonly creando = signal(false);
  readonly enviando = signal(false);
  readonly editando = signal(false);
  readonly errorAlta = signal<string | null>(null);

  cliente = '';
  email = '';
  vehiculo = '';
  asunto = '';
  html = '';
  motivo = '';

  readonly filas = computed<FilaDatos[]>(() =>
    (this.seleccionada()?.datos ?? []).map(d => ({
      aseguradora: String(d['aseguradora'] ?? '—'),
      precio: typeof d['precio_anual'] === 'number' ? (d['precio_anual'] as number) : null,
      franquicia: typeof d['franquicia_danos_propios'] === 'number' ? (d['franquicia_danos_propios'] as number) : null,
      sustitucion: typeof d['vehiculo_sustitucion'] === 'boolean' ? (d['vehiculo_sustitucion'] as boolean) : null
    }))
  );

  /** El HTML viene de la IA: se muestra en un iframe aislado (sin scripts) para que sus estilos no afecten a la app. */
  readonly vistaPrevia = computed<SafeHtml>(() =>
    this.sanitizer.bypassSecurityTrustHtml(
      `<div style="font-family: Arial, sans-serif; font-size: 14px; padding: 8px">${this.seleccionada()?.emailHtml ?? ''}</div>`
    )
  );

  constructor() {
    this.cargar();
    // Mientras haya alguna comparativa en marcha, se refresca sola cada 4 s.
    const temporizador = setInterval(() => {
      if (this.comparativas().some(c => c.estado === 'PROCESANDO' || c.estado === 'APROBADA')) {
        this.cargar();
      }
    }, 4000);
    inject(DestroyRef).onDestroy(() => clearInterval(temporizador));
  }

  limpiar(): void {
    if (!confirm('¿Borrar todas las comparativas? Solo para limpiar las pruebas.')) {
      return;
    }
    this.api.limpiarComparativas().subscribe(() => {
      this.seleccionadaId.set(null);
      this.cargar();
    });
  }

  formularioValido(): boolean {
    return !!this.cliente.trim() && /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(this.email) && !!this.vehiculo.trim()
      && this.ficheros().length > 0 && this.ficheros().length <= 6;
  }

  elegirFicheros(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    this.ficheros.set(Array.from(input.files ?? []));
  }

  cargar(): void {
    this.api.comparativas().subscribe(lista => {
      const antes = this.seleccionada();
      this.comparativas.set(lista);
      const ahora = this.seleccionada();
      // Si la seleccionada acaba de pasar a revisión, se cargan el asunto y el HTML en el editor.
      if (ahora && antes?.estado !== ahora.estado) {
        this.prepararEdicion(ahora);
      }
    });
  }

  seleccionar(c: Comparativa): void {
    this.seleccionadaId.set(c.id);
    this.prepararEdicion(c);
  }

  crear(): void {
    if (!this.formularioValido()) {
      return;
    }
    const datos = new FormData();
    datos.append('clienteNombre', this.cliente);
    datos.append('clienteEmail', this.email);
    datos.append('vehiculo', this.vehiculo);
    datos.append('creadaPor', this.sesion.usuario());
    this.ficheros().forEach(f => datos.append('pdfs', f, f.name));

    this.creando.set(true);
    this.errorAlta.set(null);
    this.api.crearComparativa(datos).subscribe({
      next: c => {
        this.creando.set(false);
        this.cliente = this.email = this.vehiculo = '';
        this.ficheros.set([]);
        this.comparativas.update(lista => [c, ...lista]);
        this.seleccionar(c);
      },
      error: e => {
        this.creando.set(false);
        this.errorAlta.set(e?.error?.error ?? 'No se pudo crear la comparativa');
      }
    });
  }

  alternarEdicion(): void {
    if (this.editando()) {
      // Al volver a la vista previa, se aplica el HTML editado.
      this.actualizarLocal({ emailHtml: this.html });
    }
    this.editando.update(v => !v);
  }

  aprobar(c: Comparativa): void {
    this.enviando.set(true);
    this.api.aprobarComparativa(c.id, this.sesion.usuario(), this.asunto, this.html).subscribe({
      next: actualizada => this.terminarAccion(actualizada),
      error: e => {
        this.enviando.set(false);
        this.actualizarLocal({ error: e?.error?.error ?? 'No se pudo aprobar' });
      }
    });
  }

  rechazar(c: Comparativa): void {
    this.enviando.set(true);
    this.api.rechazarComparativa(c.id, this.sesion.usuario(), this.motivo).subscribe({
      next: actualizada => this.terminarAccion(actualizada),
      error: () => this.enviando.set(false)
    });
  }

  private terminarAccion(actualizada: Comparativa): void {
    this.enviando.set(false);
    this.editando.set(false);
    this.motivo = '';
    this.comparativas.update(lista => lista.map(x => (x.id === actualizada.id ? actualizada : x)));
  }

  private prepararEdicion(c: Comparativa): void {
    this.asunto = c.emailAsunto ?? '';
    this.html = c.emailHtml ?? '';
    this.editando.set(false);
  }

  private actualizarLocal(cambios: Partial<Comparativa>): void {
    const id = this.seleccionadaId();
    this.comparativas.update(lista => lista.map(x => (x.id === id ? { ...x, ...cambios } : x)));
  }
}
