import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { AltaPoliza } from '../core/modelos';
import { SesionService } from '../core/sesion.service';
import { EtiquetaPipe, HaceCuantoPipe } from '../shared/etiquetas.pipe';
import { IconoComponent } from '../shared/icono.component';

@Component({
  selector: 'app-altas-poliza',
  standalone: true,
  imports: [DatePipe, RouterLink, EtiquetaPipe, HaceCuantoPipe, IconoComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="cabecera">
      <div>
        <p class="cabecera__antetitulo">Emisión · automatización con IA</p>
        <h1>Altas de pólizas desde PDF</h1>
      </div>
      <button class="btn btn--fantasma btn--sm" (click)="limpiar()" title="Borra el historial, sus revisiones y las pólizas creadas en las pruebas">
        <app-icono nombre="papelera" [tamano]="14" /> Limpiar pruebas
      </button>
    </header>

    <section class="presupuestos">
      <article class="tarjeta">
        <h2>Subir pólizas</h2>
        <div class="presupuestos__form">
          <label class="subida">
            <app-icono nombre="subir" [tamano]="22" />
            <strong>{{ ficheros().length ? 'Cambiar PDFs' : 'Sube los PDFs de las pólizas' }}</strong>
            <small>Uno por póliza (máx. 10). La IA extrae los datos y los valida.</small>
            <input type="file" accept=".pdf,application/pdf" multiple (change)="elegirFicheros($event)" />
          </label>
          @if (ficheros().length) {
            <ul class="ficheros">
              @for (f of ficheros(); track f.name) {
                <li>📄 {{ f.name }}</li>
              }
            </ul>
          }
          @if (error()) {
            <div class="caja caja--error">{{ error() }}</div>
          }
          <button class="btn btn--primario" [disabled]="!ficheros().length || subiendo()" (click)="subir()">
            <app-icono nombre="automatizaciones" [tamano]="16" />
            {{ subiendo() ? 'Enviando…' : 'Procesar con IA' }}
          </button>
          <p class="texto-suave" style="margin: 0; font-size: 12.5px">
            Si los datos cuadran, la póliza se da de alta sola. Si algo no cuadra, irá a la
            <a class="enlace" routerLink="/revision">Bandeja de revisión</a>.
          </p>
        </div>
      </article>

      <article class="tarjeta tarjeta--tabla">
        <header class="tarjeta__cabecera"><h2>Últimos documentos</h2></header>
        <table class="tabla">
          <thead><tr><th>Documento</th><th>Estado</th><th>Póliza</th><th>Motivos</th><th>Subido</th></tr></thead>
          <tbody>
            @for (a of altas(); track a.id) {
              <tr>
                <td><strong>{{ a.fichero }}</strong><small>{{ a.creadaPor }}</small></td>
                <td>
                  @if (a.estado === 'PROCESANDO') {
                    <span class="procesando" style="padding: 0; justify-content: flex-start"><span class="girando"></span> Procesando…</span>
                  } @else {
                    <span class="insignia" [attr.data-estado]="a.estado">{{ a.estado | etiqueta }}</span>
                  }
                </td>
                <td class="mono">{{ a.polizaNumero ?? '—' }}</td>
                <td>
                  @for (m of a.motivos; track m) {
                    <small>• {{ m }}</small>
                  } @empty {
                    <span class="texto-suave">—</span>
                  }
                </td>
                <td>{{ a.creadaEn | haceCuanto }}<small>{{ a.creadaEn | date: 'dd/MM HH:mm' }}</small></td>
              </tr>
            } @empty {
              <tr><td colspan="5" class="estado-vacio">Aún no se ha subido ninguna póliza.</td></tr>
            }
          </tbody>
        </table>
      </article>
    </section>
  `
})
export class AltasPolizaComponent {
  private readonly api = inject(ApiService);
  private readonly sesion = inject(SesionService);

  readonly altas = signal<AltaPoliza[]>([]);
  readonly ficheros = signal<File[]>([]);
  readonly subiendo = signal(false);
  readonly error = signal<string | null>(null);

  constructor() {
    this.cargar();
    // Mientras haya documentos procesándose, se refresca sola cada 4 s.
    const temporizador = setInterval(() => {
      if (this.altas().some(a => a.estado === 'PROCESANDO')) {
        this.cargar();
        this.sesion.refrescarContadores();
      }
    }, 4000);
    inject(DestroyRef).onDestroy(() => clearInterval(temporizador));
  }

  cargar(): void {
    this.api.altasPoliza().subscribe(lista => this.altas.set(lista));
  }

  limpiar(): void {
    if (!confirm('¿Borrar el historial de altas, sus revisiones y las pólizas que se dieron de alta en las pruebas?')) {
      return;
    }
    this.api.limpiarAltasPoliza().subscribe(() => {
      this.cargar();
      this.sesion.refrescarContadores();
    });
  }

  elegirFicheros(evento: Event): void {
    this.ficheros.set(Array.from((evento.target as HTMLInputElement).files ?? []));
  }

  subir(): void {
    const datos = new FormData();
    this.ficheros().forEach(f => datos.append('pdfs', f, f.name));
    datos.append('creadaPor', this.sesion.usuario());

    this.subiendo.set(true);
    this.error.set(null);
    this.api.subirPolizas(datos).subscribe({
      next: nuevas => {
        this.subiendo.set(false);
        this.ficheros.set([]);
        this.altas.update(lista => [...nuevas, ...lista]);
      },
      error: e => {
        this.subiendo.set(false);
        this.error.set(e?.error?.error ?? 'No se pudieron subir los PDFs');
      }
    });
  }
}
