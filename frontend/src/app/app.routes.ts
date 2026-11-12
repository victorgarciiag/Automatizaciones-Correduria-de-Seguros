import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./paginas/inicio.component').then(m => m.InicioComponent), title: 'Inicio' },
  { path: 'clientes', loadComponent: () => import('./paginas/clientes.component').then(m => m.ClientesComponent), title: 'Clientes' },
  { path: 'clientes/:id', loadComponent: () => import('./paginas/cliente-detalle.component').then(m => m.ClienteDetalleComponent), title: 'Cliente' },
  { path: 'polizas', loadComponent: () => import('./paginas/polizas.component').then(m => m.PolizasComponent), title: 'Pólizas' },
  { path: 'polizas/:id', loadComponent: () => import('./paginas/poliza-detalle.component').then(m => m.PolizaDetalleComponent), title: 'Póliza' },
  { path: 'recibos', loadComponent: () => import('./paginas/recibos.component').then(m => m.RecibosComponent), title: 'Recibos' },
  { path: 'altas-poliza', loadComponent: () => import('./paginas/altas-poliza.component').then(m => m.AltasPolizaComponent), title: 'Altas de pólizas' },
  { path: 'presupuestos', loadComponent: () => import('./paginas/presupuestos.component').then(m => m.PresupuestosComponent), title: 'Presupuestos' },
  { path: 'buzon', loadComponent: () => import('./paginas/buzon.component').then(m => m.BuzonComponent), title: 'Buzón' },
  { path: 'siniestros', loadComponent: () => import('./paginas/siniestros.component').then(m => m.SiniestrosComponent), title: 'Siniestros' },
  { path: 'revision', loadComponent: () => import('./paginas/revision.component').then(m => m.RevisionComponent), title: 'Bandeja de revisión' },
  { path: 'automatizaciones', loadComponent: () => import('./paginas/automatizaciones.component').then(m => m.AutomatizacionesComponent), title: 'Automatizaciones' },
  { path: '**', redirectTo: '' }
];
