import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-paginador',
  standalone: true,
  imports: [DecimalPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="paginador">
      <span class="texto-suave">{{ desde() | number }}–{{ hasta() | number }} de {{ total() | number }}</span>
      <div class="paginador__botones">
        <button class="btn btn--fantasma btn--sm" [disabled]="pagina() === 0" (click)="cambiar.emit(pagina() - 1)">Anterior</button>
        <button class="btn btn--fantasma btn--sm" [disabled]="hasta() >= total()" (click)="cambiar.emit(pagina() + 1)">Siguiente</button>
      </div>
    </div>`
})
export class PaginadorComponent {
  pagina = input.required<number>();
  tamano = input.required<number>();
  total = input.required<number>();
  cambiar = output<number>();

  desde = computed(() => (this.total() === 0 ? 0 : this.pagina() * this.tamano() + 1));
  hasta = computed(() => Math.min((this.pagina() + 1) * this.tamano(), this.total()));
}
