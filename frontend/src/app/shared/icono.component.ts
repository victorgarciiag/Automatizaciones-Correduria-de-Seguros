import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ICONOS } from './iconos';

@Component({
  selector: 'app-icono',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg [attr.width]="tamano()" [attr.height]="tamano()" viewBox="0 0 24 24" fill="none"
         stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path [attr.d]="ruta()" />
    </svg>`,
  styles: [':host { display: inline-flex; flex-shrink: 0; }']
})
export class IconoComponent {
  nombre = input.required<string>();
  tamano = input(18);
  ruta = computed(() => ICONOS[this.nombre()] ?? '');
}
