import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe, PercentPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { Revision } from '../core/modelos';
import { SesionService } from '../core/sesion.service';
import { CampoPipe, EtiquetaPipe, HaceCuantoPipe } from '../shared/etiquetas.pipe';
import { IconoComponent } from '../shared/icono.component';

interface Campo {
  clave: string;
  valor: string;
  original: string;
  json: boolean;
}

@Component({
  selector: 'app-revision',
  standalone: true,
  imports: [DatePipe, PercentPipe, FormsModule, CampoPipe, EtiquetaPipe, HaceCuantoPipe, IconoComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="cabecera">
      <div>
        <p class="cabecera__antetitulo">La IA propone, la persona decide</p>
        <h1>Bandeja de revisión</h1>
      </div>
      <div class="segmentado">
        <button [class.activo]="vista() === 'PENDIENTE'" (click)="cambiarVista('PENDIENTE')">Pendientes</button>
        <button [class.activo]="vista() === 'RESUELTAS'" (click)="cambiarVista('RESUELTAS')">Resueltas</button>
      </div>
    </header>

    <section class="revision">
      <div class="revision__lista">
        @for (r of revisiones(); track r.id) {
          <button class="revision__item" [class.activa]="seleccionada()?.id === r.id" (click)="seleccionar(r)">
            <span class="revision__cabecera">
              <span class="etiqueta etiqueta--ia"><app-icono nombre="robot" [tamano]="12" /> {{ r.automatizacionNombre }}</span>
              <span class="texto-suave">{{ (r.resueltaEn ?? r.creadaEn) | haceCuanto }}</span>
            </span>
            <strong>{{ r.titulo }}</strong>
            @if (r.estado === 'PENDIENTE') {
              @if (r.motivo) {
                <small class="revision__motivo">{{ r.motivo }}</small>
              }
            } @else {
              <small><span class="insignia" [attr.data-estado]="r.estado">{{ r.estado | etiqueta }}</span> por {{ r.revisor }}</small>
            }
          </button>
        } @empty {
          <div class="estado-vacio">
            <app-icono nombre="check" [tamano]="28" />
            <p>{{ vista() === 'PENDIENTE' ? 'Nada pendiente de revisar.' : 'Aún no hay revisiones resueltas.' }}</p>
          </div>
        }
      </div>

      <div class="revision__detalle tarjeta">
        @if (seleccionada(); as r) {
          <header class="tarjeta__cabecera">
            <div>
              <p class="cabecera__antetitulo">{{ r.automatizacionNombre }} · {{ r.creadaEn | date: 'dd/MM/yyyy HH:mm' }}</p>
              <h2>{{ r.titulo }}</h2>
            </div>
            @if (r.demo) {
              <span class="etiqueta etiqueta--demo">Ejemplo</span>
            }
          </header>

          @if (r.confianza != null) {
            <div class="medidor">
              <span>Confianza de la IA</span>
              <span class="medidor__pista"><span class="medidor__barra" [class.baja]="r.confianza < 0.7" [style.width.%]="r.confianza * 100"></span></span>
              <strong>{{ r.confianza | percent }}</strong>
            </div>
          }

          @if (r.motivo) {
            <div class="caja caja--aviso">
              <app-icono nombre="alerta" [tamano]="16" />
              <span><strong>¿Por qué necesita revisión?</strong> {{ r.motivo }}</span>
            </div>
          }

          <h3>{{ r.estado === 'PENDIENTE' ? 'Datos propuestos (puedes corregirlos)' : 'Datos' }}</h3>
          <div class="formulario">
            @for (c of campos(); track c.clave) {
              <label class="campo" [class.campo--editado]="c.valor !== c.original">
                <span>{{ c.clave | campo }}</span>
                @if (c.json || c.valor.length > 70) {
                  <textarea rows="3" [(ngModel)]="c.valor" [disabled]="r.estado !== 'PENDIENTE'"></textarea>
                } @else {
                  <input [(ngModel)]="c.valor" [disabled]="r.estado !== 'PENDIENTE'" />
                }
              </label>
            }
          </div>

          @if (r.estado === 'PENDIENTE') {
            <label class="campo">
              <span>Comentario (opcional)</span>
              <input [(ngModel)]="comentario" placeholder="Ej.: matrícula corregida tras llamar al cliente" />
            </label>
            @if (error()) {
              <div class="caja caja--error">{{ error() }}</div>
            }
            <footer class="revision__acciones">
              <span class="texto-suave">Decides como <strong>{{ sesion.usuario() }}</strong></span>
              <button class="btn btn--peligro-suave" [disabled]="enviando()" (click)="rechazar(r)"><app-icono nombre="x" [tamano]="16" /> Rechazar</button>
              <button class="btn btn--primario" [disabled]="enviando()" (click)="aprobar(r)"><app-icono nombre="check" [tamano]="16" /> Aprobar</button>
            </footer>
          } @else {
            <div class="caja">
              <span class="insignia" [attr.data-estado]="r.estado">{{ r.estado | etiqueta }}</span>
              por <strong>{{ r.revisor }}</strong> el {{ r.resueltaEn | date: 'dd/MM/yyyy HH:mm' }}
              @if (r.comentario) { · «{{ r.comentario }}» }
              @if (r.callbackEstado) { <br /><small class="texto-suave">Aviso a la automatización: {{ r.callbackEstado }}</small> }
            </div>
          }
        } @else {
          <div class="estado-vacio">Selecciona un elemento de la lista</div>
        }
      </div>
    </section>
  `
})
export class RevisionComponent {
  private readonly api = inject(ApiService);
  readonly sesion = inject(SesionService);

  readonly vista = signal<'PENDIENTE' | 'RESUELTAS'>('PENDIENTE');
  readonly revisiones = signal<Revision[]>([]);
  readonly seleccionada = signal<Revision | null>(null);
  readonly campos = signal<Campo[]>([]);
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);
  comentario = '';

  constructor() {
    this.cargar();
  }

  cambiarVista(vista: 'PENDIENTE' | 'RESUELTAS'): void {
    this.vista.set(vista);
    this.cargar();
  }

  cargar(): void {
    this.api.revisiones(this.vista()).subscribe(lista => {
      this.revisiones.set(lista);
      const actual = lista.find(r => r.id === this.seleccionada()?.id) ?? lista[0] ?? null;
      if (actual) {
        this.seleccionar(actual);
      } else {
        this.seleccionada.set(null);
      }
    });
  }

  seleccionar(r: Revision): void {
    this.seleccionada.set(r);
    this.comentario = '';
    this.error.set(null);
    const datos = r.datosFinales ?? r.datosPropuestos;
    this.campos.set(Object.entries(datos).map(([clave, v]) => {
      const json = typeof v === 'object' && v !== null;
      const texto = json ? JSON.stringify(v, null, 2) : String(v ?? '');
      return { clave, valor: texto, original: texto, json };
    }));
  }

  aprobar(r: Revision): void {
    const datos: Record<string, unknown> = {};
    try {
      for (const c of this.campos()) {
        datos[c.clave] = c.json ? JSON.parse(c.valor) : c.valor;
      }
    } catch {
      this.error.set('Hay un campo con JSON no válido.');
      return;
    }
    this.enviar(this.api.aprobarRevision(r.id, this.sesion.usuario(), this.comentario, datos));
  }

  rechazar(r: Revision): void {
    this.enviar(this.api.rechazarRevision(r.id, this.sesion.usuario(), this.comentario));
  }

  private enviar(peticion: ReturnType<ApiService['aprobarRevision']>): void {
    this.enviando.set(true);
    peticion.subscribe({
      next: () => {
        this.enviando.set(false);
        this.seleccionada.set(null);
        this.sesion.refrescarContadores();
        this.cargar();
      },
      error: e => {
        this.enviando.set(false);
        this.error.set(e?.error?.error ?? 'No se pudo guardar la decisión');
      }
    });
  }
}
