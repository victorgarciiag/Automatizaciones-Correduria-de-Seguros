import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { IconoComponent } from './shared/icono.component';
import { EQUIPO, SesionService } from './core/sesion.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconoComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="app">
      <aside class="lateral">
        <div class="marca">
          <span class="marca__logo"><app-icono nombre="escudo" [tamano]="20" /></span>
          <div>
            <strong>Correduría de Seguros</strong>
            <small>Backoffice</small>
          </div>
        </div>

        <nav class="menu">
          <span class="menu__grupo">Gestión</span>
          @for (item of menuGestion; track item.ruta) {
            <a class="menu__item" [routerLink]="item.ruta" routerLinkActive="activo" [routerLinkActiveOptions]="{ exact: item.ruta === '/' }">
              <app-icono [nombre]="item.icono" />
              <span>{{ item.texto }}</span>
            </a>
          }
          <span class="menu__grupo">Inteligencia</span>
          <a class="menu__item" routerLink="/revision" routerLinkActive="activo">
            <app-icono nombre="revision" />
            <span>Bandeja de revisión</span>
            @if (sesion.revisionesPendientes() > 0) {
              <span class="menu__contador">{{ sesion.revisionesPendientes() }}</span>
            }
          </a>
          <a class="menu__item" routerLink="/automatizaciones" routerLinkActive="activo">
            <app-icono nombre="automatizaciones" />
            <span>Automatizaciones</span>
          </a>
        </nav>

        <div class="lateral__pie">
          <label class="usuario">
            <span class="avatar">{{ iniciales(sesion.usuario()) }}</span>
            <span class="usuario__datos">
              <small>Conectado como</small>
              <select (change)="sesion.cambiarUsuario($any($event.target).value)">
                @for (p of equipo; track p.nombre) {
                  <option [value]="p.nombre" [selected]="p.nombre === sesion.usuario()">{{ p.nombre }}</option>
                }
              </select>
            </span>
          </label>
          <p class="aviso-ficticio">Empresa y datos ficticios · demo de automatización</p>
        </div>
      </aside>

      <main class="contenido">
        <router-outlet />
      </main>
    </div>
  `
})
export class AppComponent {
  readonly sesion = inject(SesionService);
  readonly equipo = EQUIPO;
  readonly menuGestion = [
    { ruta: '/', texto: 'Inicio', icono: 'inicio' },
    { ruta: '/clientes', texto: 'Clientes', icono: 'clientes' },
    { ruta: '/polizas', texto: 'Pólizas', icono: 'polizas' },
    { ruta: '/recibos', texto: 'Recibos', icono: 'recibos' },
    { ruta: '/buzon', texto: 'Buzón', icono: 'email' },
    { ruta: '/siniestros', texto: 'Siniestros', icono: 'siniestros' },
    { ruta: '/presupuestos', texto: 'Presupuestos', icono: 'balanza' },
    { ruta: '/altas-poliza', texto: 'Altas de pólizas', icono: 'subir' }
  ];

  iniciales(nombre: string): string {
    return nombre.split(' ').slice(0, 2).map(p => p[0]).join('');
  }
}
