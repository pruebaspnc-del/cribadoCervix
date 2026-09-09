package  Cribadocervix

import java.sql.SQLException

import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import groovy.sql.Sql
import groovy.util.logging.Slf4j

@Slf4j(value="slf4j")
@Transactional
@Service
class TratamientoHL7Service {
    
	static GESTLAB = 1
	static PATWIN = 2
	
	Logger log = slf4j
	
	// Bean que nos permite hacer consultas SQL directas a la base de datos
	@Autowired
	Sql groovySql
	
	/**
	 * Tratamiento eventos oru_r01
	 * @param idDetalle Indentificador de tabla específica detalle del evento
	 * @return
	 */
	@Transactional
	def oruR01(int idDetalle) {
		def salida = [:]
		
		try {
			log.debug("Procesando oruR01 ${idDetalle}")
			// 1. Recuperamos información de la tabla específica y la tabla HL7
			// 2. Gestionamos de forma diferente si la muestra/prueba es para VPH o Citología líquida
			// 2.1 VPH --> Debemos guardar los resultados de las observaciones (OBX.5) en función del tipo de prueba (OBR.4.1)
			// 2.2 Citología líquida  --> Debemos guardar los resultados de las observaciones (OBX.5) en función del tipo de prueba (OBR.4.1)

			
			def parametros = [:]
			
			def resultado = groovySql.firstRow("""
				SELECT DECODE(C.SISTEMA_ORIGEN,'APA',2,'GESLAB',1,1) as ORIGEN, 
					D.ID_MUESTRA AS COD_PRODUCTO,
					D.FECHA_RESULTADO AS FECHA_RESULTADO,
					D.RESULTADO AS VALOR_RESULTADO
				FROM FUENTES_EXTERNAS.HL7_ORUR01 D, FUENTES_EXTERNAS.HL7 C
				WHERE D.ID_HL7 = C.ID
				  AND D.ID = :idDetalle
			""", 
				[idDetalle: idDetalle])
	
			if (resultado) {
				// Lanzar PL/SQL con los datos recibidos y parámetros fijos					
				parametros.pnOrigen = resultado.ORIGEN
				parametros.psCodProducto = resultado.COD_PRODUCTO
				parametros.pdFechaResultado = resultado.FECHA_RESULTADO
				parametros.vsValorResultado = resultado.VALOR_RESULTADO
				
				// Llamamos al procedimiento
				groovySql.call '{call Cribadocervix.PROCESA_LABORATORIO.PROCESA_ORU_O01 (?,?,?,?,?)}',
					[idDetalle,parametros.pnOrigen, parametros.psCodProducto, parametros.pdFechaResultado, parametros.vsValorResultado]
										
				salida = [resultado: 1, detalleError: ""]
			} else {
				salida = [resultado: -1, detalleError: "No se han encontrado datos de detalle HL7"]
			}
		} catch (SQLException ex) {
			// Hay que tratar de nuevo (0 es para que se trate de nuevo)
			salida = [resultado: 0, detalleError: ex.message]
		}
		
		return salida;
			
	}
	
	/**
	 * Tratamiento eventos orr_O02
	 * @param idDetalle Indentificador de tabla específica detalle del evento
	 * @return
	 */
	@Transactional
	def orrO02(int idDetalle) {
		def salida = [:]
		
		try {
			log.debug("Procesando orrO02 ${idDetalle}")
			// 1. Recuperamos información de la tabla específica y la tabla HL7
			// 2. Gestionamos de forma diferente si la muestra es para VPH o Citología líquida
			// 2.1 VPH --> Debemos guardar la fecha de entrada en laboratorio
			// 2.2 Citología líquida  --> No contemplado
			
			def parametros = [:]
			
			def resultado = groovySql.firstRow("""
				SELECT DECODE(C.SISTEMA_ORIGEN,'APA',2,'GESLAB',1,1) as ORIGEN, 
					D.ID_MUESTRA AS COD_PRODUCTO,
					D.FECHA_TOMA AS FECHA_MUESTRA,
					D.FECHA_LLEGADA AS FECHA_ENTRADA
				FROM FUENTES_EXTERNAS.HL7_ORRO02 D, FUENTES_EXTERNAS.HL7 C
				WHERE D.ID_HL7 = C.ID
				  AND D.ID = :idDetalle
				""", 
				[idDetalle: idDetalle])
	
			if (resultado) {
				// Lanzar PL/SQL con los datos recibidos y parámetros fijos
				parametros.pnOrigen = resultado.ORIGEN
				parametros.psCodProducto = resultado.COD_PRODUCTO
				parametros.pdFechaMuestra = resultado.FECHA_MUESTRA
				parametros.pdFechaEntrada = resultado.FECHA_ENTRADA
				
				// Llamamos al procedimiento
				groovySql.call '{call Cribadocervix.PROCESA_LABORATORIO.PROCESA_ORR_O02 (?,?,?,?,?)}',
					[idDetalle,parametros.pnOrigen, parametros.psCodProducto, parametros.pdFechaMuestra, parametros.pdFechaEntrada]
			
				salida = [resultado: 1, detalleError: ""]
			} else {
				salida = [resultado: -1, detalleError: "No se han encontrado datos de detalle HL7"]
			}
		} catch (SQLException ex) {
			// Hay que tratar de nuevo (0 es para que se trate de nuevo)
			salida = [resultado: 0, detalleError: ex.message]
		}
		
		return salida;
	}

	
	/**
	 * Tratamiento eventos orm_O01
	 * @param idDetalle Indentificador de tabla específica detalle del evento
	 * @return
	 */
	@Transactional
	def ormO01(int idDetalle) {
		def salida = [:]
		
		try {
			log.debug("Procesando ormO01 ${idDetalle}")
			// 1. Recuperamos información de la tabla específica y la tabla HL7
			// 2. Gestionamos de forma diferente si la muestra es para VPH o Citología líquida
			// 2.1 VPH --> Debemos guardar la fecha de entrada en laboratorio
			// 2.2 Citología líquida  --> No contemplado
			
			def parametros = [:]
			
			def resultado = groovySql.firstRow("""
				SELECT DECODE(C.SISTEMA_ORIGEN,'APA',2,'GESLAB',1,2) as ORIGEN, 
					D.ID_MUESTRA AS COD_PRODUCTO,
					D.FECHA_TOMA AS FECHA_MUESTRA,
					D.FECHA_LLEGADA AS FECHA_ENTRADA
				FROM FUENTES_EXTERNAS.HL7_ORMO01 D, FUENTES_EXTERNAS.HL7 C
				WHERE D.ID_HL7 = C.ID
				  AND D.ID = :idDetalle
				""", 
				[idDetalle: idDetalle])
	
			if (resultado) {
				// Lanzar PL/SQL con los datos recibidos y parámetros fijos
				parametros.pnOrigen = resultado.ORIGEN
				parametros.psCodProducto = resultado.COD_PRODUCTO
				parametros.pdFechaMuestra = resultado.FECHA_MUESTRA
				parametros.pdFechaEntrada = resultado.FECHA_ENTRADA
				
				// Llamamos al procedimiento
				groovySql.call '{call Cribadocervix.PROCESA_LABORATORIO.PROCESA_ORM_O01 (?,?,?,?,?)}',
					[idDetalle,parametros.pnOrigen, parametros.psCodProducto, parametros.pdFechaMuestra, parametros.pdFechaEntrada]
			
				salida = [resultado: 1, detalleError: ""]
			} else {
				salida = [resultado: -1, detalleError: "No se han encontrado datos de detalle HL7"]
			}
		} catch (SQLException ex) {
			// Hay que tratar de nuevo (0 es para que se trate de nuevo)
			salida = [resultado: 0, detalleError: ex.message]
		}
		
		return salida;
	}
}
