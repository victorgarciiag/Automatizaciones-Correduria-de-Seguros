import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { DiaEjecuciones } from '../core/modelos';

/** Barras apiladas por día (correctas / a revisión / error), dibujadas en SVG. */
@Component({
  selector: 'app-grafico-ejecuciones',
  standalone: true,
  imports: [DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="grafico">
      <div class="grafico__barras">
        @for (d of barras(); track d.fecha) {
          <div class="grafico__col" [title]="(d.fecha | date: 'EEE d MMM') + ' · ' + d.total + ' ejecuciones · ' + d.horasAhorradas + ' h ahorradas'">
            <div class="grafico__pila" [style.height.%]="d.alto">
              <span class="seg seg--error" [style.flex-grow]="d.error"></span>
              <span class="seg seg--revision" [style.flex-grow]="d.revision"></span>
              <span class="seg seg--ok" [style.flex-grow]="d.ok"></span>
            </div>
            @if (mostrarEtiquetas()) {
              <span class="grafico__etiqueta">{{ d.fecha | date: 'd' }}</span>
            }
          </div>
        }
      </div>
      <div class="grafico__leyenda">
        <span><i class="punto punto--ok"></i>Correctas</span>
        <span><i class="punto punto--revision"></i>A revisión</span>
        <span><i class="punto punto--error"></i>Error</span>
      </div>
    </div>`
})
export class GraficoEjecucionesComponent {
  dias = input.required<DiaEjecuciones[]>();
  mostrarEtiquetas = input(true);

  barras = computed(() => {
    const max = Math.max(1, ...this.dias().map(d => d.ok + d.revision + d.error));
    return this.dias().map(d => {
      const total = d.ok + d.revision + d.error;
      return { ...d, total, alto: total === 0 ? 0 : Math.max(3, (total / max) * 100) };
    });
  });
}
