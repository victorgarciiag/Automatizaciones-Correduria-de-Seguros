-- Catálogo al día: las automatizaciones ya terminadas pasan a ACTIVA (el panel lo muestra)
UPDATE automatizacion SET estado = 'ACTIVA'
WHERE codigo IN ('recibos-devueltos', 'comparativa-presupuestos', 'alta-poliza', 'clasificador-email');
