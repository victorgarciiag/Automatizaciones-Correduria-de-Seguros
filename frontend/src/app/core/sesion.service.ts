import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';

export const EQUIPO = [
  { nombre: 'Carmen Ruiz', puesto: 'Gerente' },
  { nombre: 'Lucía Moreno', puesto: 'Comercial' },
  { nombre: 'Javier Ortega', puesto: 'Emisión' },
  { nombre: 'Rocío Benítez', puesto: 'Administración' },
  { nombre: 'Manuel Pérez', puesto: 'Siniestros auto' },
  { nombre: 'Elena Castro', puesto: 'Siniestros diversos' },
  { nombre: 'Sara Gil', puesto: 'Atención al cliente' }
];

/** Usuario "conectado" (simulado) y contadores que se refrescan solos. */
@Injectable({ providedIn: 'root' })
export class SesionService {
  private readonly api = inject(ApiService);

  readonly usuario = signal(this.leerUsuario());
  readonly revisionesPendientes = signal(0);

  constructor() {
    this.refrescarContadores();
    // Si n8n manda algo a revisión, el contador del menú se actualiza sin recargar.
    setInterval(() => this.refrescarContadores(), 20000);
  }

  cambiarUsuario(nombre: string): void {
    this.usuario.set(nombre);
    try {
      localStorage.setItem('usuario', nombre);
    } catch {
      /* sin almacenamiento: no pasa nada */
    }
  }

  refrescarContadores(): void {
    this.api.revisiones('PENDIENTE').subscribe({
      next: r => this.revisionesPendientes.set(r.length),
      error: () => this.revisionesPendientes.set(0)
    });
  }

  private leerUsuario(): string {
    try {
      return localStorage.getItem('usuario') ?? EQUIPO[2].nombre;
    } catch {
      return EQUIPO[2].nombre;
    }
  }
}
