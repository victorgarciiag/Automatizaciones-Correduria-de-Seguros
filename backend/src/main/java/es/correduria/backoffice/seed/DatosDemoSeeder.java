package es.correduria.backoffice.seed;

import es.correduria.backoffice.dominio.*;
import es.correduria.backoffice.repositorio.*;
import es.correduria.backoffice.servicio.SiniestroService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Genera una cartera ficticia coherente (siempre la misma, semilla fija) la primera vez que arranca.
 * Todos los nombres, NIF, teléfonos y matrículas son inventados.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatosDemoSeeder implements ApplicationRunner {

    private static final String LETRAS_NIF = "TRWAGMYFPDXBNJZSQVHLCKE";
    private static final String CONSONANTES = "BCDFGHJKLMNPRSTVWXYZ";

    private final AseguradoraRepository aseguradoras;
    private final ClienteRepository clientes;
    private final PolizaRepository polizas;
    private final ReciboRepository recibos;
    private final SiniestroRepository siniestros;
    private final RevisionRepository revisiones;
    private final EjecucionRepository ejecuciones;
    private final AutomatizacionRepository automatizaciones;

    @Value("${app.seed.cartera}")
    private boolean seedCartera;

    @Value("${app.seed.ejecuciones-demo}")
    private boolean seedEjecucionesDemo;

    private final Random rnd = new Random(42);
    private final LocalDate hoy = LocalDate.now();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (seedCartera && clientes.count() == 0) {
            log.info("Generando cartera ficticia...");
            var listaAseguradoras = crearAseguradoras();
            var listaClientes = crearClientes();
            var listaPolizas = crearPolizas(listaClientes, listaAseguradoras);
            crearRecibos(listaPolizas);
            crearSiniestros(listaPolizas);
            log.info("Cartera generada: {} clientes, {} pólizas, {} recibos, {} siniestros",
                    clientes.count(), polizas.count(), recibos.count(), siniestros.count());
        }
        if (seedEjecucionesDemo && ejecuciones.count() == 0 && polizas.count() > 0) {
            log.info("Generando ejecuciones y revisiones de DEMOSTRACIÓN (demo=true)...");
            crearEjecucionesDemo();
            crearRevisionesDemo();
        }
    }

    // ------------------------------------------------------------------ aseguradoras

    private List<Aseguradora> crearAseguradoras() {
        String[][] datos = {
                {"ATL", "Atlántica Seguros", "#0e7490"},
                {"MSU", "Mutua del Sur", "#b45309"},
                {"IBV", "Iberia Vida", "#7c3aed"},
                {"LEV", "Levante Generales", "#be123c"},
                {"FAR", "Faro Seguros", "#0369a1"},
                {"SIE", "Sierra Mutual", "#15803d"},
                {"COS", "Costa Previsión", "#c2410c"},
        };
        List<Aseguradora> lista = new ArrayList<>();
        for (String[] d : datos) {
            var a = new Aseguradora();
            a.setCodigo(d[0]);
            a.setNombre(d[1]);
            a.setColor(d[2]);
            a.setEmailSiniestros("siniestros@" + d[1].toLowerCase().replace(" ", "").replace("á", "a") + ".example");
            a.setTelefono("900 " + (100 + rnd.nextInt(900)) + " " + (100 + rnd.nextInt(900)));
            lista.add(a);
        }
        return aseguradoras.saveAll(lista);
    }

    // ------------------------------------------------------------------ clientes

    private static final String[] NOMBRES = {"Antonio", "Manuel", "José", "Francisco", "David", "Juan", "Javier",
            "Daniel", "Carlos", "Alejandro", "Miguel", "Rafael", "Pablo", "Sergio", "Fernando", "Jorge", "Alberto",
            "Álvaro", "Adrián", "Raúl", "María", "Carmen", "Ana", "Isabel", "Laura", "Lucía", "Cristina", "Marta",
            "Pilar", "Rocío", "Elena", "Paula", "Sara", "Rosario", "Inmaculada", "Macarena", "Nuria", "Beatriz",
            "Patricia", "Irene"};
    private static final String[] APELLIDOS = {"García", "Rodríguez", "González", "Fernández", "López", "Martínez",
            "Sánchez", "Pérez", "Gómez", "Martín", "Jiménez", "Ruiz", "Hernández", "Díaz", "Moreno", "Muñoz",
            "Álvarez", "Romero", "Alonso", "Gutiérrez", "Navarro", "Torres", "Domínguez", "Vázquez", "Ramos",
            "Gil", "Ramírez", "Serrano", "Blanco", "Molina", "Morales", "Ortega", "Delgado", "Castro", "Ortiz",
            "Rubio", "Marín", "Sanz", "Núñez", "Iglesias", "Medina", "Garrido", "Cortés", "Castillo", "Santos",
            "Lozano", "Guerrero", "Cano", "Prieto", "Méndez", "Benítez", "Mateos", "Caballero", "Vega", "Reyes"};
    private static final String[] NEGOCIOS = {"Bar", "Taberna", "Ferretería", "Panadería", "Taller", "Peluquería",
            "Farmacia", "Frutería", "Bodega", "Librería", "Clínica Dental", "Academia", "Restaurante", "Carpintería",
            "Autoescuela", "Floristería", "Gestoría", "Óptica", "Lavandería", "Cafetería"};
    private static final String[] NOMBRES_NEGOCIO = {"El Rincón", "La Plaza", "San Miguel", "Los Arcos",
            "El Puerto", "La Albariza", "Santa Ana", "El Faro", "La Alameda", "Los Naranjos", "La Bahía",
            "El Mirador", "San Dionisio", "La Viña", "El Molino", "La Marisma", "Las Salinas", "El Castillo"};
    private static final String[][] LOCALIDADES = {
            {"Jerez de la Frontera", "11401"}, {"Jerez de la Frontera", "11402"}, {"Jerez de la Frontera", "11403"},
            {"Jerez de la Frontera", "11404"}, {"Jerez de la Frontera", "11405"}, {"El Puerto de Santa María", "11500"},
            {"Cádiz", "11001"}, {"Sanlúcar de Barrameda", "11540"}, {"Chipiona", "11550"}, {"Rota", "11520"},
            {"Arcos de la Frontera", "11630"}, {"Chiclana de la Frontera", "11130"}, {"Lebrija", "41740"}};
    private static final String[] CALLES = {"C/ Larga", "C/ Porvenir", "Av. Álvaro Domecq", "C/ Corredera",
            "C/ Lancería", "Av. de Europa", "C/ Arcos", "C/ Sevilla", "C/ Medina", "Plaza del Arenal",
            "C/ Bizcocheros", "Av. de la Libertad", "C/ San Francisco", "C/ Ancha", "C/ Real", "Av. del Mar",
            "C/ Cristóbal Colón", "C/ Rosario", "C/ Palma", "C/ Doña Blanca"};

    private List<Cliente> crearClientes() {
        List<Cliente> lista = new ArrayList<>();
        Set<String> nifs = new HashSet<>();
        for (int i = 0; i < 300; i++) {
            var c = new Cliente();
            boolean empresa = i % 5 == 0;
            String[] loc = LOCALIDADES[rnd.nextInt(LOCALIDADES.length)];
            if (empresa) {
                c.setTipo(Cliente.Tipo.EMPRESA);
                String negocio = elegir(NEGOCIOS) + " " + elegir(NOMBRES_NEGOCIO);
                c.setNombre(negocio + " S.L.");
                c.setNif(cifUnico(nifs));
                c.setEmail(slug(negocio) + "@empresa.example");
            } else {
                c.setTipo(Cliente.Tipo.PARTICULAR);
                String nombre = elegir(NOMBRES);
                String ap1 = elegir(APELLIDOS);
                String ap2 = elegir(APELLIDOS);
                c.setNombre(nombre + " " + ap1 + " " + ap2);
                c.setNif(nifUnico(nifs));
                c.setEmail(slug(nombre + "." + ap1) + (rnd.nextInt(90) + 10) + "@correo.example");
            }
            if (!empresa) {
                c.setTelefono("6" + String.format("%02d %03d %03d", rnd.nextInt(100), rnd.nextInt(1000), rnd.nextInt(1000)));
            } else {
                c.setTelefono("956 " + String.format("%03d %03d", rnd.nextInt(1000), rnd.nextInt(1000)));
            }
            c.setDireccion(elegir(CALLES) + ", " + (rnd.nextInt(120) + 1) + " · " + loc[1]);
            c.setLocalidad(loc[0]);
            c.setFechaAlta(hoy.minusDays(rnd.nextInt(365 * 15) + 30));
            lista.add(c);
        }
        return clientes.saveAll(lista);
    }

    // ------------------------------------------------------------------ pólizas

    private static final Map<Poliza.Ramo, String[]> PRODUCTOS = Map.of(
            Poliza.Ramo.AUTO, new String[]{"Auto Todo Riesgo con franquicia 300 €", "Auto Terceros Ampliado",
                    "Auto Terceros Básico", "Moto Terceros + Robo", "Furgoneta Profesional Terceros Ampliado"},
            Poliza.Ramo.HOGAR, new String[]{"Hogar Completo", "Hogar Básico Continente", "Hogar Alquiler Propietario",
                    "Hogar Premium Joyas y Arte"},
            Poliza.Ramo.COMERCIO, new String[]{"Multirriesgo Comercio", "Multirriesgo Hostelería",
                    "Responsabilidad Civil Profesional", "Multirriesgo Taller"},
            Poliza.Ramo.VIDA, new String[]{"Vida Riesgo 100.000 €", "Vida Hipoteca", "Vida Riesgo 60.000 €"},
            Poliza.Ramo.DECESOS, new String[]{"Decesos Familiar", "Decesos Individual Prima Nivelada"},
            Poliza.Ramo.SALUD, new String[]{"Salud Cuadro Médico", "Salud Reembolso", "Salud Dental Plus"});

    private static final Map<Poliza.Ramo, int[]> RANGO_PRIMAS = Map.of(
            Poliza.Ramo.AUTO, new int[]{260, 950}, Poliza.Ramo.HOGAR, new int[]{140, 480},
            Poliza.Ramo.COMERCIO, new int[]{380, 2400}, Poliza.Ramo.VIDA, new int[]{110, 620},
            Poliza.Ramo.DECESOS, new int[]{85, 320}, Poliza.Ramo.SALUD, new int[]{480, 1900});

    private static final String[] COCHES = {"Seat León", "Seat Ibiza", "Volkswagen Golf", "Renault Clio",
            "Peugeot 208", "Toyota Corolla", "Dacia Sandero", "Kia Ceed", "Hyundai Tucson", "Citroën C4",
            "Opel Corsa", "Ford Focus", "Toyota C-HR", "Nissan Qashqai", "Seat Arona", "MG ZS"};
    private static final String[] FURGONETAS = {"Renault Kangoo", "Citroën Berlingo", "Ford Transit Connect",
            "Peugeot Partner", "Fiat Doblò"};
    private static final String[] MOTOS = {"Honda PCX 125", "Yamaha NMAX", "Kymco Agility", "Piaggio Liberty"};

    private List<Poliza> crearPolizas(List<Cliente> listaClientes, List<Aseguradora> listaAseguradoras) {
        List<Poliza> lista = new ArrayList<>();
        int secuencia = 4100;
        for (Cliente c : listaClientes) {
            int numPolizas = c.getTipo() == Cliente.Tipo.EMPRESA ? 1 + rnd.nextInt(3) : 1 + pesos(55, 30, 15);
            Set<Poliza.Ramo> usados = EnumSet.noneOf(Poliza.Ramo.class);
            for (int i = 0; i < numPolizas; i++) {
                Poliza.Ramo ramo = ramoPara(c, usados);
                usados.add(ramo);
                var a = listaAseguradoras.get(rnd.nextInt(listaAseguradoras.size()));
                var p = new Poliza();
                p.setCliente(c);
                p.setAseguradora(a);
                p.setRamo(ramo);
                p.setNumero("%s-%s-%06d".formatted(a.getCodigo(), ramo.name().substring(0, 2), ++secuencia * 7 % 999983));
                rellenarObjeto(p, c);
                LocalDate vencimiento = hoy.plusDays(rnd.nextInt(365) + 1);
                p.setFechaVencimiento(vencimiento);
                p.setFechaEfecto(vencimiento.minusYears(1));
                int[] rango = RANGO_PRIMAS.get(ramo);
                p.setPrimaAnual(BigDecimal.valueOf(rango[0] + rnd.nextInt(rango[1] - rango[0]))
                        .add(BigDecimal.valueOf(rnd.nextInt(100), 2)));
                p.setPeriodicidad(periodicidad(ramo));
                p.setComisionPct(BigDecimal.valueOf(ramo == Poliza.Ramo.AUTO ? 10 + rnd.nextInt(6)
                        : ramo == Poliza.Ramo.SALUD ? 8 + rnd.nextInt(4) : 15 + rnd.nextInt(11)));
                p.setEstado(rnd.nextInt(100) < 4 ? Poliza.Estado.ANULADA : Poliza.Estado.VIGENTE);
                lista.add(p);
            }
        }
        return polizas.saveAll(lista);
    }

    private Poliza.Ramo ramoPara(Cliente c, Set<Poliza.Ramo> usados) {
        for (int intento = 0; intento < 10; intento++) {
            Poliza.Ramo ramo;
            if (c.getTipo() == Cliente.Tipo.EMPRESA) {
                ramo = rnd.nextInt(100) < 65 ? Poliza.Ramo.COMERCIO : Poliza.Ramo.AUTO;
            } else {
                int r = rnd.nextInt(100);
                ramo = r < 46 ? Poliza.Ramo.AUTO : r < 76 ? Poliza.Ramo.HOGAR : r < 88 ? Poliza.Ramo.SALUD
                        : r < 94 ? Poliza.Ramo.VIDA : Poliza.Ramo.DECESOS;
            }
            if (!usados.contains(ramo) || ramo == Poliza.Ramo.AUTO) {
                return ramo;
            }
        }
        return Poliza.Ramo.HOGAR;
    }

    private void rellenarObjeto(Poliza p, Cliente c) {
        String[] productos = PRODUCTOS.get(p.getRamo());
        switch (p.getRamo()) {
            case AUTO -> {
                String matricula = matricula();
                p.setMatricula(matricula);
                if (c.getTipo() == Cliente.Tipo.EMPRESA) {
                    p.setProducto(productos[4]);
                    p.setObjetoAsegurado(elegir(FURGONETAS) + " · " + formatoMatricula(matricula));
                } else if (rnd.nextInt(100) < 12) {
                    p.setProducto(productos[3]);
                    p.setObjetoAsegurado(elegir(MOTOS) + " · " + formatoMatricula(matricula));
                } else {
                    p.setProducto(productos[rnd.nextInt(3)]);
                    p.setObjetoAsegurado(elegir(COCHES) + " · " + formatoMatricula(matricula));
                }
            }
            case HOGAR, COMERCIO -> {
                p.setProducto(elegir(productos));
                p.setObjetoAsegurado(c.getDireccion() + ", " + c.getLocalidad());
            }
            default -> {
                p.setProducto(elegir(productos));
                p.setObjetoAsegurado(c.getNombre());
            }
        }
    }

    private Poliza.Periodicidad periodicidad(Poliza.Ramo ramo) {
        int r = rnd.nextInt(100);
        if (ramo == Poliza.Ramo.SALUD || ramo == Poliza.Ramo.DECESOS) {
            return r < 60 ? Poliza.Periodicidad.MENSUAL : Poliza.Periodicidad.TRIMESTRAL;
        }
        return r < 70 ? Poliza.Periodicidad.ANUAL : r < 85 ? Poliza.Periodicidad.SEMESTRAL : Poliza.Periodicidad.TRIMESTRAL;
    }

    // ------------------------------------------------------------------ recibos

    private void crearRecibos(List<Poliza> listaPolizas) {
        List<Recibo> lista = new ArrayList<>();
        int secuencia = 0;
        for (Poliza p : listaPolizas) {
            int fraccion = switch (p.getPeriodicidad()) {
                case ANUAL -> 1;
                case SEMESTRAL -> 2;
                case TRIMESTRAL -> 4;
                case MENSUAL -> 12;
            };
            BigDecimal importe = p.getPrimaAnual().divide(BigDecimal.valueOf(fraccion), 2, RoundingMode.HALF_UP);
            for (int i = 0; i < fraccion; i++) {
                LocalDate emision = p.getFechaEfecto().plusMonths(12L / fraccion * i);
                if (emision.isAfter(hoy)) {
                    break;
                }
                var r = new Recibo();
                r.setPoliza(p);
                r.setNumero("R-%d-%07d".formatted(emision.getYear(), ++secuencia));
                r.setImporte(importe);
                r.setFechaEmision(emision);
                long antiguedad = hoy.toEpochDay() - emision.toEpochDay();
                if (p.getEstado() == Poliza.Estado.ANULADA) {
                    r.setEstado(Recibo.Estado.ANULADO);
                } else if (antiguedad <= 4) {
                    r.setEstado(Recibo.Estado.PENDIENTE);
                } else if (antiguedad <= 60 && rnd.nextInt(100) < 22) {
                    r.setEstado(Recibo.Estado.DEVUELTO);
                    r.setIntentosReclamacion(rnd.nextInt(3));
                } else {
                    r.setEstado(Recibo.Estado.COBRADO);
                    r.setFechaCobro(emision.plusDays(rnd.nextInt(4)));
                }
                lista.add(r);
            }
        }
        recibos.saveAll(lista);
    }

    // ------------------------------------------------------------------ siniestros

    private static final Map<Poliza.Ramo, String[][]> SINIESTROS = Map.of(
            Poliza.Ramo.AUTO, new String[][]{
                    {"Colisión con otro vehículo", "Golpe por alcance en una rotonda. El otro conductor reconoce la culpa y se rellena parte amistoso.", "Parte amistoso;Fotos de los daños"},
                    {"Rotura de lunas", "Impacto de una piedra en la autovía. Grieta en el parabrisas.", "Fotos del parabrisas"},
                    {"Golpe en aparcamiento", "El vehículo aparece con la aleta trasera rayada y abollada en un parking. No hay contrario identificado.", "Fotos de los daños;Ticket del parking"},
                    {"Robo de elementos", "Rotura de la ventanilla y robo de objetos del interior durante la noche.", "Denuncia;Fotos de los daños"},
                    {"Asistencia en carretera", "Avería de batería. Se solicita grúa al domicilio.", ""}},
            Poliza.Ramo.HOGAR, new String[][]{
                    {"Daños por agua", "Fuga en la tubería del baño que ha afectado al techo del vecino de abajo.", "Fotos de los daños;Factura del fontanero"},
                    {"Rotura de cristales", "Rotura del cristal de la puerta de la terraza por el viento.", "Fotos;Presupuesto de reparación"},
                    {"Daños eléctricos", "Subida de tensión que ha estropeado la nevera y el router.", "Informe del técnico;Facturas de los aparatos"},
                    {"Robo en vivienda", "Robo con forzamiento de la puerta principal mientras los propietarios estaban fuera.", "Denuncia;Listado de objetos robados"}},
            Poliza.Ramo.COMERCIO, new String[][]{
                    {"Daños por agua en local", "Filtración desde la planta superior que ha dañado el falso techo y mercancía.", "Fotos;Inventario de mercancía dañada"},
                    {"Rotura de escaparate", "Rotura de la luna del escaparate durante la noche.", "Denuncia;Fotos"},
                    {"Avería de maquinaria", "Avería de la cámara frigorífica con pérdida de género.", "Informe del técnico;Valoración del género"},
                    {"Responsabilidad civil", "Un cliente resbala en el local y reclama daños.", "Escrito de reclamación;Declaración del responsable"}},
            Poliza.Ramo.SALUD, new String[][]{
                    {"Solicitud de autorización", "Solicitud de autorización para una prueba diagnóstica.", "Volante médico"}});

    private void crearSiniestros(List<Poliza> listaPolizas) {
        var candidatas = listaPolizas.stream()
                .filter(p -> p.getEstado() == Poliza.Estado.VIGENTE && SINIESTROS.containsKey(p.getRamo()))
                .toList();
        Siniestro.Estado[] estados = Siniestro.Estado.values();
        Siniestro.Canal[] canales = {Siniestro.Canal.TELEFONO, Siniestro.Canal.WHATSAPP, Siniestro.Canal.WHATSAPP,
                Siniestro.Canal.EMAIL, Siniestro.Canal.PRESENCIAL};
        for (int i = 0; i < 42; i++) {
            Poliza p = candidatas.get(rnd.nextInt(candidatas.size()));
            String[] plantilla = elegir(SINIESTROS.get(p.getRamo()));
            int dias = rnd.nextInt(90);
            LocalDateTime apertura = hoy.minusDays(dias).atTime(LocalTime.of(9 + rnd.nextInt(9), rnd.nextInt(60)));
            Siniestro.Estado estado = dias > 45 ? (rnd.nextInt(10) < 8 ? Siniestro.Estado.CERRADO : Siniestro.Estado.RECHAZADO)
                    : dias > 20 ? estados[2 + rnd.nextInt(2)]
                    : estados[rnd.nextInt(3)];

            var s = new Siniestro();
            s.setNumero("TMP-" + i);
            s.setPoliza(p);
            s.setTipo(plantilla[0]);
            s.setDescripcion(plantilla[1]);
            s.setFechaOcurrencia(apertura.toLocalDate().minusDays(rnd.nextInt(3)));
            s.setFechaApertura(apertura);
            s.setEstado(estado);
            s.setCanal(elegir(canales));
            s.setOrigen(Siniestro.Origen.MANUAL);
            s.setTramitador(SiniestroService.tramitadorPorRamo(p.getRamo()));
            if (estado == Siniestro.Estado.DOCUMENTACION_PENDIENTE && !plantilla[2].isBlank()) {
                s.setDocumentosPendientes(plantilla[2]);
            } else if (estado == Siniestro.Estado.DOCUMENTACION_PENDIENTE) {
                s.setEstado(Siniestro.Estado.ABIERTO);
            }
            s.setImporteEstimado(BigDecimal.valueOf(120 + rnd.nextInt(3200)));
            s = siniestros.save(s);
            s.setNumero(SiniestroService.numero(s));
        }
    }

    // ------------------------------------------------------------------ demo: ejecuciones y revisiones

    private void crearEjecucionesDemo() {
        Map<String, Integer> pesoPorAutomatizacion = new LinkedHashMap<>();
        pesoPorAutomatizacion.put("comparativa-presupuestos", 6);
        pesoPorAutomatizacion.put("alta-poliza", 4);
        pesoPorAutomatizacion.put("apertura-siniestro", 4);
        pesoPorAutomatizacion.put("clasificador-email", 14);
        Map<String, Integer> minutos = new HashMap<>();
        automatizaciones.findAll().forEach(a -> minutos.put(a.getCodigo(), a.getMinutosManuales()));

        List<Ejecucion> lista = new ArrayList<>();
        for (int d = 29; d >= 0; d--) {
            LocalDate dia = hoy.minusDays(d);
            boolean finDeSemana = dia.getDayOfWeek().getValue() >= 6;
            double rampa = 0.4 + 0.6 * (29 - d) / 29.0; // el uso crece con el tiempo
            for (var entrada : pesoPorAutomatizacion.entrySet()) {
                int n = (int) Math.round(entrada.getValue() * rampa * (finDeSemana ? 0.2 : 1) * (0.7 + rnd.nextDouble() * 0.6));
                for (int i = 0; i < n; i++) {
                    lista.add(ejecucionDemo(entrada.getKey(), dia, minutos.get(entrada.getKey())));
                }
            }
        }
        ejecuciones.saveAll(lista);
    }

    private Ejecucion ejecucionDemo(String automatizacion, LocalDate dia, int minutosManuales) {
        int r = rnd.nextInt(100);
        var resultado = r < 81 ? Ejecucion.Resultado.OK : r < 95 ? Ejecucion.Resultado.REVISION : Ejecucion.Resultado.ERROR;
        boolean pesada = automatizacion.equals("comparativa-presupuestos") || automatizacion.equals("alta-poliza");
        int tokensEntrada = pesada ? 6000 + rnd.nextInt(9000) : 800 + rnd.nextInt(2500);
        int tokensSalida = pesada ? 700 + rnd.nextInt(900) : 150 + rnd.nextInt(400);
        // Coste orientativo con tarifas tipo Sonnet (≈3 $/M entrada, 15 $/M salida) pasado a euros.
        double coste = (tokensEntrada * 3.0 + tokensSalida * 15.0) / 1_000_000 * 0.92;
        double ahorro = switch (resultado) {
            case OK -> minutosManuales;
            case REVISION -> minutosManuales * 0.6;
            case ERROR -> 0;
        };

        var e = new Ejecucion();
        e.setAutomatizacion(automatizacion);
        e.setInicio(dia.atTime(LocalTime.of(8 + rnd.nextInt(11), rnd.nextInt(60), rnd.nextInt(60))));
        e.setDuracionMs(pesada ? 6000 + rnd.nextInt(14000) : 1500 + rnd.nextInt(4000));
        e.setResultado(resultado);
        e.setTokensEntrada(resultado == Ejecucion.Resultado.ERROR ? tokensEntrada / 3 : tokensEntrada);
        e.setTokensSalida(resultado == Ejecucion.Resultado.ERROR ? 0 : tokensSalida);
        e.setCosteEur(BigDecimal.valueOf(coste).setScale(5, RoundingMode.HALF_UP));
        e.setMinutosAhorrados(BigDecimal.valueOf(ahorro).setScale(1, RoundingMode.HALF_UP));
        e.setDetalle(switch (resultado) {
            case OK -> "Procesado correctamente";
            case REVISION -> "Enviado a revisión: confianza baja en algún campo";
            case ERROR -> elegir(new String[]{"PDF escaneado ilegible", "Timeout llamando a la API de Claude",
                    "El adjunto no es un documento válido"});
        });
        e.setReferencia("DEMO-" + Integer.toHexString(rnd.nextInt(0xFFFFF)).toUpperCase());
        e.setDemo(true);
        return e;
    }

    private void crearRevisionesDemo() {
        var auto = polizas.findAll().stream()
                .filter(p -> p.getRamo() == Poliza.Ramo.AUTO && p.getEstado() == Poliza.Estado.VIGENTE).findFirst().orElseThrow();
        var hogar = polizas.findAll().stream()
                .filter(p -> p.getRamo() == Poliza.Ramo.HOGAR && p.getEstado() == Poliza.Estado.VIGENTE).findFirst().orElseThrow();
        var clienteAuto = auto.getCliente();
        String matricula = formatoMatricula(auto.getMatricula());
        String matriculaOida = matricula.substring(0, 6) + (matricula.charAt(6) == 'B' ? 'P' : 'B') + matricula.substring(7);

        List<Revision> lista = new ArrayList<>();
        lista.add(revisionDemo("apertura-siniestro", "Siniestro por Telegram · golpe en aparcamiento",
                "La matrícula del audio (" + matriculaOida + ") no coincide exactamente con la de la póliza (" + matricula + ")",
                datos("cliente", clienteAuto.getNombre(), "telefono", clienteAuto.getTelefono(),
                        "numeroPoliza", auto.getNumero(), "matricula", matriculaOida,
                        "fechaOcurrencia", hoy.minusDays(1).toString(), "tipo", "Golpe en aparcamiento",
                        "descripcion", "Al volver al coche en el parking del centro comercial tenía la puerta trasera derecha abollada. No hay nota del otro conductor.",
                        "documentosPendientes", "Fotos de los daños; Ticket del parking"),
                "0.64", 35));
        lista.add(revisionDemo("alta-poliza", "Nueva póliza de hogar · Atlántica Seguros",
                "El NIF del PDF no corresponde a ningún cliente: posible cliente nuevo",
                datos("tomador", "Marta Delgado Vega", "nif", nifAleatorio(), "numeroPoliza", "ATL-HO-" + (100000 + rnd.nextInt(899999)),
                        "ramo", "HOGAR", "producto", "Hogar Completo", "direccion", "C/ Porvenir, 18 · 11402 Jerez de la Frontera",
                        "fechaEfecto", hoy.plusDays(3).toString(), "primaAnual", "312.40", "periodicidad", "ANUAL"),
                "0.71", 80));
        lista.add(revisionDemo("comparativa-presupuestos", "Comparativa auto · 3 aseguradoras para " + hogar.getCliente().getNombre(),
                "Faro Seguros no indica la franquicia en el PDF",
                datos("cliente", hogar.getCliente().getNombre(), "vehiculo", "Toyota C-HR Hybrid 2022",
                        "Atlántica Seguros", "612,30 € · Todo riesgo · franquicia 300 €",
                        "Mutua del Sur", "548,90 € · Todo riesgo · franquicia 400 €",
                        "Faro Seguros", "579,00 € · Todo riesgo · franquicia no indicada",
                        "recomendacion", "Mutua del Sur (más barata, pero franquicia mayor)"),
                "0.82", 140));
        lista.add(revisionDemo("clasificador-email", "Email sin categoría clara · «Consulta sobre mi póliza y un recibo»",
                "El email mezcla una consulta de cobertura y un recibo devuelto",
                datos("remitente", hogar.getCliente().getEmail(), "asunto", "Consulta sobre mi póliza y un recibo",
                        "categoriasPosibles", "CONSULTA_COBERTURA, RECIBO", "asignarA", "Sara Gil",
                        "borradorRespuesta", "Hola, gracias por escribirnos. Revisamos tu recibo y la consulta sobre la cobertura y te respondemos hoy mismo."),
                "0.55", 210));

        for (int i = 0; i < 6; i++) {
            var r = revisionDemo(i % 2 == 0 ? "alta-poliza" : "apertura-siniestro",
                    i % 2 == 0 ? "Alta de póliza auto · " + elegir(NOMBRES) + " " + elegir(APELLIDOS)
                            : "Siniestro por Telegram · " + elegir(new String[]{"rotura de lunas", "daños por agua", "colisión"}),
                    "Campo con confianza baja", datos("nota", "Revisión de ejemplo ya resuelta"),
                    "0.7" + i, 60 * 24 * (i + 1));
            r.setEstado(i == 3 ? Revision.Estado.RECHAZADA : Revision.Estado.APROBADA);
            r.setRevisor(i % 2 == 0 ? "Javier Ortega" : "Manuel Pérez");
            r.setResueltaEn(r.getCreadaEn().plusMinutes(12 + rnd.nextInt(90)));
            r.setDatosFinales(r.getEstado() == Revision.Estado.APROBADA ? r.getDatosPropuestos() : null);
            r.setComentario(i == 3 ? "Documento duplicado" : null);
            lista.add(r);
        }
        revisiones.saveAll(lista);
    }

    private Revision revisionDemo(String automatizacion, String titulo, String motivo, Map<String, Object> datos,
                                  String confianza, int minutosAtras) {
        var r = new Revision();
        r.setAutomatizacion(automatizacion);
        r.setTitulo(titulo);
        r.setMotivo(motivo);
        r.setDatosPropuestos(datos);
        r.setConfianza(new BigDecimal(confianza));
        r.setEstado(Revision.Estado.PENDIENTE);
        r.setCreadaEn(LocalDateTime.now().minusMinutes(minutosAtras));
        r.setDemo(true);
        return r;
    }

    // ------------------------------------------------------------------ utilidades

    private static Map<String, Object> datos(String... claveValor) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < claveValor.length; i += 2) {
            m.put(claveValor[i], claveValor[i + 1]);
        }
        return m;
    }

    private <T> T elegir(T[] opciones) {
        return opciones[rnd.nextInt(opciones.length)];
    }

    private int pesos(int... pesos) {
        int total = Arrays.stream(pesos).sum();
        int r = rnd.nextInt(total);
        for (int i = 0; i < pesos.length; i++) {
            r -= pesos[i];
            if (r < 0) {
                return i;
            }
        }
        return 0;
    }

    /** NIF ficticio en el rango 90.000.000-99.999.999 con letra de control válida. */
    private String nifAleatorio() {
        int numero = 90_000_000 + rnd.nextInt(10_000_000);
        return String.valueOf(numero) + LETRAS_NIF.charAt(numero % 23);
    }

    private String nifUnico(Set<String> usados) {
        String nif;
        do {
            nif = nifAleatorio();
        } while (!usados.add(nif));
        return nif;
    }

    private String cifUnico(Set<String> usados) {
        String cif;
        do {
            cif = "B" + (11_000_000 + rnd.nextInt(1_000_000)) + "X";
        } while (!usados.add(cif));
        return cif;
    }

    private String matricula() {
        StringBuilder sb = new StringBuilder(String.format("%04d", rnd.nextInt(10000)));
        sb.append(rnd.nextBoolean() ? 'K' : 'L');
        for (int i = 0; i < 2; i++) {
            sb.append(CONSONANTES.charAt(rnd.nextInt(CONSONANTES.length())));
        }
        return sb.toString();
    }

    private static String formatoMatricula(String m) {
        return m.substring(0, 4) + " " + m.substring(4);
    }

    private static String slug(String texto) {
        return java.text.Normalizer.normalize(texto.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^a-z0-9.]+", "");
    }
}
