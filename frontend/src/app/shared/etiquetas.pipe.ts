import { Pipe, PipeTransform } from '@angular/core';

const ETIQUETAS: Record<string, string> = {
  // Ramos
  AUTO: 'Auto', HOGAR: 'Hogar', COMERCIO: 'Comercio', VIDA: 'Vida', DECESOS: 'Decesos', SALUD: 'Salud',
  // Estados de póliza
  VIGENTE: 'Vigente', ANULADA: 'Anulada', VENCIDA: 'Vencida',
  // Recibos
  PENDIENTE: 'Pendiente', COBRADO: 'Cobrado', DEVUELTO: 'Devuelto', ANULADO: 'Anulado',
  // Siniestros
  ABIERTO: 'Abierto', DOCUMENTACION_PENDIENTE: 'Falta documentación', EN_TRAMITE: 'En trámite',
  CERRADO: 'Cerrado', RECHAZADO: 'Rechazado',
  // Canales
  TELEFONO: 'Teléfono', WHATSAPP: 'WhatsApp', TELEGRAM: 'Telegram', EMAIL: 'Email', PRESENCIAL: 'Presencial',
  MANUAL: 'Manual', AUTOMATIZACION: 'Automático',
  // Comparativas
  CREADA: 'Dada de alta', PROCESANDO: 'Procesando', PENDIENTE_REVISION: 'Pendiente de revisión', ENVIADA: 'Enviada',
  // Revisiones
  APROBADA: 'Aprobada', RECHAZADA: 'Rechazada',
  // Automatizaciones
  PLANIFICADA: 'Planificada', EN_DESARROLLO: 'En desarrollo', ACTIVA: 'Activa', PAUSADA: 'Pausada',
  OK: 'Correcta', REVISION: 'A revisión', ERROR: 'Error',
  // Buzón
  CLASIFICADO: 'Redactando respuesta', APROBADO: 'Enviando', RESPONDIDO: 'Respondido', DESCARTADO: 'Descartado',
  SINIESTRO: 'Siniestro', PRESUPUESTO: 'Presupuesto', RECIBOS: 'Recibos', MODIFICACION: 'Modificación',
  BAJA: 'Baja', DOCUMENTACION: 'Documentación', OTRO: 'Otro', ALTA: 'Alta', MEDIA: 'Media',
  // Periodicidad y tipo
  ANUAL: 'Anual', SEMESTRAL: 'Semestral', TRIMESTRAL: 'Trimestral', MENSUAL: 'Mensual',
  PARTICULAR: 'Particular', EMPRESA: 'Empresa'
};

@Pipe({ name: 'etiqueta', standalone: true })
export class EtiquetaPipe implements PipeTransform {
  transform(valor: string | null | undefined): string {
    return valor ? (ETIQUETAS[valor] ?? valor) : '';
  }
}

const ACENTOS: Record<string, string> = {
  telefono: 'teléfono', numero: 'número', descripcion: 'descripción', matricula: 'matrícula',
  vehiculo: 'vehículo', recomendacion: 'recomendación', direccion: 'dirección', categorias: 'categorías',
  poliza: 'póliza', periodicidad: 'periodicidad', asignar: 'asignar', ocurrencia: 'ocurrencia'
};

/** "fechaEfecto" → "Fecha efecto" (para mostrar los datos que propone la IA). */
@Pipe({ name: 'campo', standalone: true })
export class CampoPipe implements PipeTransform {
  transform(clave: string): string {
    const texto = clave.replace(/([a-z])([A-Z])/g, '$1 $2').replace(/_/g, ' ').toLowerCase()
      .split(' ').map(p => ACENTOS[p] ?? p).join(' ');
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }
}

@Pipe({ name: 'haceCuanto', standalone: true })
export class HaceCuantoPipe implements PipeTransform {
  transform(fecha: string | null | undefined): string {
    if (!fecha) {
      return '';
    }
    const minutos = Math.round((Date.now() - new Date(fecha).getTime()) / 60000);
    if (minutos < 1) return 'ahora mismo';
    if (minutos < 60) return `hace ${minutos} min`;
    const horas = Math.round(minutos / 60);
    if (horas < 24) return `hace ${horas} h`;
    const dias = Math.round(horas / 24);
    return dias === 1 ? 'ayer' : `hace ${dias} días`;
  }
}
