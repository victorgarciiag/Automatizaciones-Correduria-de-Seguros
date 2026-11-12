export type Ramo = 'AUTO' | 'HOGAR' | 'COMERCIO' | 'VIDA' | 'DECESOS' | 'SALUD';
export type EstadoPoliza = 'VIGENTE' | 'ANULADA' | 'VENCIDA';
export type EstadoRecibo = 'PENDIENTE' | 'COBRADO' | 'DEVUELTO' | 'ANULADO';
export type EstadoSiniestro = 'ABIERTO' | 'DOCUMENTACION_PENDIENTE' | 'EN_TRAMITE' | 'CERRADO' | 'RECHAZADO';
export type EstadoRevision = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA';
export type EstadoAutomatizacion = 'PLANIFICADA' | 'EN_DESARROLLO' | 'ACTIVA' | 'PAUSADA';
export type ResultadoEjecucion = 'OK' | 'REVISION' | 'ERROR';

export interface Pagina<T> {
  contenido: T[];
  total: number;
  pagina: number;
  tamano: number;
}

export interface Aseguradora {
  id: number;
  codigo: string;
  nombre: string;
  color: string;
}

export interface Cliente {
  id: number;
  tipo: 'PARTICULAR' | 'EMPRESA';
  nombre: string;
  nif: string;
  telefono: string;
  email: string;
  direccion: string;
  localidad: string;
  fechaAlta: string;
}

export interface ClienteDetalle {
  cliente: Cliente;
  polizas: Poliza[];
  primaTotal: number;
  polizasVigentes: number;
  siniestros: Siniestro[];
}

export interface Poliza {
  id: number;
  numero: string;
  clienteId: number;
  clienteNombre: string;
  clienteNif: string;
  aseguradoraId: number;
  aseguradoraNombre: string;
  aseguradoraColor: string;
  ramo: Ramo;
  producto: string;
  objetoAsegurado: string;
  matricula?: string;
  fechaEfecto: string;
  fechaVencimiento: string;
  primaAnual: number;
  periodicidad: string;
  comisionPct: number;
  estado: EstadoPoliza;
  enVigor: boolean;
}

export interface PolizaDetalle {
  poliza: Poliza;
  cliente: Cliente;
  recibos: Recibo[];
  siniestros: Siniestro[];
}

export interface Recibo {
  id: number;
  numero: string;
  polizaId: number;
  polizaNumero: string;
  ramo: Ramo;
  clienteId: number;
  clienteNombre: string;
  clienteTelefono: string;
  aseguradoraNombre: string;
  importe: number;
  fechaEmision: string;
  fechaCobro?: string;
  estado: EstadoRecibo;
  intentosReclamacion: number;
}

export interface TotalesRecibos {
  pendientes: number;
  devueltos: number;
  importeDevuelto: number;
}

export interface Siniestro {
  id: number;
  numero: string;
  polizaId: number;
  polizaNumero: string;
  ramo: Ramo;
  objetoAsegurado: string;
  clienteId: number;
  clienteNombre: string;
  clienteTelefono: string;
  aseguradoraNombre: string;
  tipo: string;
  descripcion: string;
  fechaOcurrencia: string;
  fechaApertura: string;
  estado: EstadoSiniestro;
  canal: string;
  origen: 'MANUAL' | 'AUTOMATIZACION';
  tramitador: string;
  documentosPendientes: string[];
  importeEstimado?: number;
}

export interface Revision {
  id: number;
  automatizacion: string;
  automatizacionNombre: string;
  titulo: string;
  motivo?: string;
  datosPropuestos: Record<string, unknown>;
  datosFinales?: Record<string, unknown>;
  confianza?: number;
  estado: EstadoRevision;
  revisor?: string;
  comentario?: string;
  creadaEn: string;
  resueltaEn?: string;
  callbackEstado?: string;
  demo: boolean;
}

export interface Ejecucion {
  id: number;
  automatizacion: string;
  inicio: string;
  duracionMs: number;
  resultado: ResultadoEjecucion;
  tokensEntrada: number;
  tokensSalida: number;
  costeEur: number;
  minutosAhorrados: number;
  referencia?: string;
  detalle?: string;
  n8nExecutionId?: string;
  demo: boolean;
}

export interface EstadisticasAutomatizacion {
  codigo: string;
  nombre: string;
  descripcion: string;
  area: string;
  estado: EstadoAutomatizacion;
  minutosManuales: number;
  orden: number;
  ejecuciones: number;
  ok: number;
  revision: number;
  error: number;
  tasaExito: number;
  coste: number;
  horasAhorradas: number;
  costeMedio: number;
  ultimaEjecucion?: string;
}

export interface DiaEjecuciones {
  fecha: string;
  ok: number;
  revision: number;
  error: number;
  horasAhorradas: number;
}

export interface PanelAutomatizaciones {
  dias: number;
  ejecuciones: number;
  tasaExito: number;
  coste: number;
  horasAhorradas: number;
  ahorroEur: number;
  revisionesPendientes: number;
  hayDatosDemo: boolean;
  porDia: DiaEjecuciones[];
  automatizaciones: EstadisticasAutomatizacion[];
}

export interface Dashboard {
  clientes: number;
  polizasVigentes: number;
  primasCartera: number;
  siniestrosAbiertos: number;
  recibosDevueltos: number;
  importeDevuelto: number;
  revisionesPendientes: number;
  vencimientos30d: number;
  carteraPorRamo: { ramo: Ramo; polizas: number; primas: number }[];
  proximosVencimientos: Poliza[];
  ultimosSiniestros: Siniestro[];
  revisiones: Revision[];
  automatizaciones: PanelAutomatizaciones;
}

export type EstadoComparativa = 'PROCESANDO' | 'PENDIENTE_REVISION' | 'APROBADA' | 'ENVIADA' | 'RECHAZADA' | 'ERROR';

export interface Comparativa {
  id: number;
  clienteNombre: string;
  clienteEmail: string;
  vehiculo: string;
  numPresupuestos: number;
  estado: EstadoComparativa;
  datos?: Record<string, unknown>[];
  emailAsunto?: string;
  emailHtml?: string;
  error?: string;
  creadaPor?: string;
  revisor?: string;
  creadaEn: string;
  borradorEn?: string;
  resueltaEn?: string;
  enviadaEn?: string;
}

export type EstadoAltaPoliza = 'PROCESANDO' | 'CREADA' | 'REVISION' | 'ERROR';

export interface AltaPoliza {
  id: number;
  fichero: string;
  estado: EstadoAltaPoliza;
  polizaNumero?: string;
  motivos: string[];
  creadaPor?: string;
  creadaEn: string;
  resueltaEn?: string;
}

export type CategoriaEmail = 'SINIESTRO' | 'PRESUPUESTO' | 'RECIBOS' | 'MODIFICACION' | 'BAJA' | 'DOCUMENTACION' | 'OTRO';
export type UrgenciaEmail = 'ALTA' | 'MEDIA' | 'BAJA';
export type EstadoEmail = 'CLASIFICADO' | 'PENDIENTE_REVISION' | 'APROBADO' | 'RESPONDIDO' | 'DESCARTADO' | 'ERROR';

export interface EmailEntrante {
  id: number;
  remitente: string;
  remitenteEmail?: string;
  asunto?: string;
  texto?: string;
  recibidoEn: string;
  categoria: CategoriaEmail;
  urgencia: UrgenciaEmail;
  confianza?: number;
  resumen?: string;
  datos?: Record<string, unknown>;
  cliente?: { id: number; nombre: string; nif: string; telefono?: string; email?: string };
  poliza?: { id: number; numero: string; ramo: Ramo; producto: string; aseguradora: string; objetoAsegurado: string; enVigor: boolean; fechaVencimiento: string };
  identificadoPor?: string;
  avisos: string[];
  asignadoA: string;
  estado: EstadoEmail;
  respuestaAsunto?: string;
  respuesta?: string;
  revisor?: string;
  error?: string;
  creadoEn: string;
  borradorEn?: string;
  resueltoEn?: string;
  enviadoEn?: string;
}
