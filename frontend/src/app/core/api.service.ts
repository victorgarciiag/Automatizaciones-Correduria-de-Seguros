import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AltaPoliza, Aseguradora, ClienteDetalle, Comparativa, Cliente, Dashboard, EmailEntrante, Ejecucion, EstadoSiniestro, Pagina,
  PanelAutomatizaciones, Poliza, PolizaDetalle, Recibo, Revision, Siniestro, TotalesRecibos
} from './modelos';

type Filtros = Record<string, string | number | boolean | null | undefined>;

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';

  dashboard(): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${this.base}/dashboard`);
  }

  aseguradoras(): Observable<Aseguradora[]> {
    return this.http.get<Aseguradora[]>(`${this.base}/aseguradoras`);
  }

  clientes(filtros: Filtros): Observable<Pagina<Cliente>> {
    return this.http.get<Pagina<Cliente>>(`${this.base}/clientes`, { params: this.params(filtros) });
  }

  cliente(id: number): Observable<ClienteDetalle> {
    return this.http.get<ClienteDetalle>(`${this.base}/clientes/${id}`);
  }

  polizas(filtros: Filtros): Observable<Pagina<Poliza>> {
    return this.http.get<Pagina<Poliza>>(`${this.base}/polizas`, { params: this.params(filtros) });
  }

  poliza(id: number): Observable<PolizaDetalle> {
    return this.http.get<PolizaDetalle>(`${this.base}/polizas/${id}`);
  }

  recibos(filtros: Filtros): Observable<Pagina<Recibo>> {
    return this.http.get<Pagina<Recibo>>(`${this.base}/recibos`, { params: this.params(filtros) });
  }

  totalesRecibos(): Observable<TotalesRecibos> {
    return this.http.get<TotalesRecibos>(`${this.base}/recibos/totales`);
  }

  cobrarRecibo(id: number): Observable<Recibo> {
    return this.http.post<Recibo>(`${this.base}/recibos/${id}/cobrar`, {});
  }

  reclamarRecibo(id: number): Observable<Recibo> {
    return this.http.post<Recibo>(`${this.base}/recibos/${id}/reclamar`, {});
  }

  siniestros(): Observable<Siniestro[]> {
    return this.http.get<Siniestro[]>(`${this.base}/siniestros`);
  }

  cambiarEstadoSiniestro(id: number, estado: EstadoSiniestro): Observable<Siniestro> {
    return this.http.patch<Siniestro>(`${this.base}/siniestros/${id}/estado`, { estado });
  }

  revisiones(estado: 'PENDIENTE' | 'RESUELTAS'): Observable<Revision[]> {
    return this.http.get<Revision[]>(`${this.base}/revisiones`, { params: { estado } });
  }

  aprobarRevision(id: number, revisor: string, comentario: string, datosFinales: Record<string, unknown>): Observable<Revision> {
    return this.http.post<Revision>(`${this.base}/revisiones/${id}/aprobar`, { revisor, comentario, datosFinales });
  }

  rechazarRevision(id: number, revisor: string, comentario: string): Observable<Revision> {
    return this.http.post<Revision>(`${this.base}/revisiones/${id}/rechazar`, { revisor, comentario });
  }

  panelAutomatizaciones(dias: number): Observable<PanelAutomatizaciones> {
    return this.http.get<PanelAutomatizaciones>(`${this.base}/automatizaciones/panel`, { params: { dias } });
  }

  ejecuciones(filtros: Filtros): Observable<Pagina<Ejecucion>> {
    return this.http.get<Pagina<Ejecucion>>(`${this.base}/automatizaciones/ejecuciones`, { params: this.params(filtros) });
  }

  borrarDatosDemo(): Observable<{ ejecuciones: number; revisiones: number }> {
    return this.http.delete<{ ejecuciones: number; revisiones: number }>(`${this.base}/automatizaciones/demo`);
  }

  comparativas(): Observable<Comparativa[]> {
    return this.http.get<Comparativa[]>(`${this.base}/comparativas`);
  }

  crearComparativa(datos: FormData): Observable<Comparativa> {
    return this.http.post<Comparativa>(`${this.base}/comparativas`, datos);
  }

  aprobarComparativa(id: number, revisor: string, emailAsunto: string, emailHtml: string): Observable<Comparativa> {
    return this.http.post<Comparativa>(`${this.base}/comparativas/${id}/aprobar`, { revisor, emailAsunto, emailHtml });
  }

  rechazarComparativa(id: number, revisor: string, motivo: string): Observable<Comparativa> {
    return this.http.post<Comparativa>(`${this.base}/comparativas/${id}/rechazar`, { revisor, motivo });
  }

  altasPoliza(): Observable<AltaPoliza[]> {
    return this.http.get<AltaPoliza[]>(`${this.base}/altas-poliza`);
  }

  subirPolizas(datos: FormData): Observable<AltaPoliza[]> {
    return this.http.post<AltaPoliza[]>(`${this.base}/altas-poliza`, datos);
  }

  /** Solo pruebas: borra el historial de altas, sus revisiones y las pólizas creadas por la automatización. */
  limpiarAltasPoliza(): Observable<Record<string, number>> {
    return this.http.delete<Record<string, number>>(`${this.base}/altas-poliza`);
  }

  /** Solo pruebas: borra todas las comparativas. */
  limpiarComparativas(): Observable<Record<string, number>> {
    return this.http.delete<Record<string, number>>(`${this.base}/comparativas`);
  }

  emails(): Observable<EmailEntrante[]> {
    return this.http.get<EmailEntrante[]>(`${this.base}/emails`);
  }

  aprobarEmail(id: number, revisor: string, asunto: string, respuesta: string): Observable<EmailEntrante> {
    return this.http.post<EmailEntrante>(`${this.base}/emails/${id}/aprobar`, { revisor, asunto, respuesta });
  }

  descartarEmail(id: number, revisor: string, motivo: string): Observable<EmailEntrante> {
    return this.http.post<EmailEntrante>(`${this.base}/emails/${id}/descartar`, { revisor, motivo });
  }

  /** Solo pruebas: borra todos los emails del buzón. */
  limpiarEmails(): Observable<Record<string, number>> {
    return this.http.delete<Record<string, number>>(`${this.base}/emails`);
  }

  private params(filtros: Filtros): HttpParams {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtros)) {
      if (valor !== null && valor !== undefined && valor !== '') {
        params = params.set(clave, String(valor));
      }
    }
    return params;
  }
}
