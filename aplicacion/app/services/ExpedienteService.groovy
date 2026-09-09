package es.carm.Cribadocervix

import es.carm.jade.FUENTESService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

import base.BaseService
import es.carm.Cribadocervix.entidad.ExpedienteEntidadService
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.springframework.transaction.annotation.Transactional

@Slf4j
@Transactional
@Service
class ExpedienteService {
    
    // Servicios
	@Autowired
	UtilsCribadocervixService utilsCribadocervixService
	ExpedienteEntidadService expedienteEntidadService
	@Autowired
	FUENTESService FUENTESService
	@Autowired
	BaseService baseService
	@Autowired
	Environment env
	
	// Bean que nos permite hacer consultas SQL directas a la base de datos
	@Autowired
	Sql groovySql

	/**
	 * Búsqueda de los expedientes según los parámetros que se pasen
	 * @param parametros Valores de búsqueda
	 * @return
	 */
    @Transactional(readOnly = true)
	def buscar(parametros) {
		def resultado = [:]
		log.debug("Busqueda parametros: ${parametros}")
		//Ejecutamos consulta solo hasta limite de registros (application.properties)
		def sentencia = generaSentenciaBusqueda(parametros)
		resultado = groovySql.rows(sentencia, parametros)
		// Devolvemos el resultado
		return resultado
	}

	/**
	 * METODO QUE GENERA LA SENTENCIA DE BUSQUEDA String SQL*/
	def generaSentenciaBusqueda(parametros) {
		if (parametros.fec_alta_desde) {
			parametros.fec_alta_desde = parametros.fec_alta_desde.toTimestamp()
		}
		if (parametros.fec_alta_hasta) {
			parametros.fec_alta_hasta = parametros.fec_alta_hasta.toTimestamp()
		}
		if (parametros.fec_prueba) {
			parametros.fec_prueba = parametros.fec_prueba.toTimestamp()
		}

		def zona_salud = parametros.zona_salud
		def id_expediente = parametros.id_expediente
		def expedientes_inactivos = parametros.expedientes_inactivos
		def fec_alta_desde = parametros.fec_alta_desde
		def fec_alta_hasta = parametros.fec_alta_hasta
		def id_tipo_proceso = parametros.id_tipo_proceso
		def id_tipo_estado_estudio = parametros.id_tipo_estado_estudio
		def id_tipo_estudio = parametros.id_tipo_estudio
		def tubos_dispen = parametros.tubos_dispensados
		def fec_recep_lab = parametros.fec_recep_lab
		def tipo_prueba = parametros.tipo_prueba
		def fec_prueba = parametros.fec_prueba
        def cod_cs = parametros.cod_cs

        def sentencia = """
				SELECT DISTINCT vez.id_expediente,
                    vez.id_persona,
                    per.nombre||' '||per.apellido1||' '||per.apellido2  nombre_completo,
                    (SELECT valor FROM PERSONA.PERSONA__IDENTIDAD_TIPO pit2 WHERE pit2.id_persona = vez.id_persona AND codigo_identidad_tipo ='DNI') DNI,
                    (SELECT valor FROM PERSONA.PERSONA__IDENTIDAD_TIPO pit2 WHERE pit2.id_persona = vez.id_persona AND codigo_identidad_tipo ='NIE') NIE,
                    (SELECT pt.descripcion FROM Cribadocervix.proceso_tipo pt WHERE pt.id=pro.id_proceso_tipo) tipo_proceso,
                    (SELECT et.descripcion FROM Cribadocervix.estudio_tipo et WHERE et.id=est.id_estudio_tipo) tipo_estudio,
                    (SELECT tee.descripcion FROM Cribadocervix.tipo_estado_estudio tee WHERE tee.id = ee.id_cod_estado) tipo_estado_estudio,
                    pro.fecha_inicio,
                    TO_CHAR(vez.f_baja,'dd/MM/yyyy') fecha_baja,
                    TO_CHAR(vez.f_alta, 'dd/MM/yyyy') fecha_alta,
                    (SELECT descripcion FROM Cribadocervix.prueba_tipo WHERE id =
                        (SELECT p.id_tipo_prueba FROM Cribadocervix.prueba p WHERE p.id = 
                            (SELECT  MAX(p.id) FROM Cribadocervix.prueba p WHERE p.id_estudio = est.id))) tipo_prueba,
                    (SELECT TO_CHAR(MAX(p.fecha),'dd/MM/yyyy') FROM Cribadocervix.prueba p WHERE id = 
                        (SELECT MAX(p.id) FROM Cribadocervix.prueba p WHERE p.id_estudio = est.id)) fec_prueba,
                    (SELECT cod_producto FROM Cribadocervix.prueba_detalle WHERE id_prueba = 
                        (SELECT MAX(p.id) FROM Cribadocervix.prueba p WHERE p.id_estudio = est.id)) cod_producto,
                    (SELECT pi2.login FROM PERSONA.PROFESIONAL_INFORMATICO pi2 WHERE pi2.ID_PROFESIONAL = 
                        (SELECT pru2.id_profesional FROM Cribadocervix.prueba pru2 WHERE pru2.ID = pru.id)) login,    
                    (SELECT MAX(peticion.id) FROM Cribadocervix.peticion_muestra peticion WHERE peticion.cod_producto = 
                        (SELECT codigo.cod_producto FROM Cribadocervix.prueba_detalle codigo WHERE codigo.id_prueba = 
                            (SELECT MAX(p.id) FROM Cribadocervix.prueba p WHERE p.id_estudio = est.id))) peticion_enviada
                FROM Cribadocervix.v_expedientes_zbs vez,
                    PERSONA.persona per,
                    Cribadocervix.proceso pro,
                    Cribadocervix.estudio est LEFT OUTER JOIN Cribadocervix.prueba pru ON pru.id_estudio = est.id,
                    Cribadocervix.estudio_estado ee
            WHERE
                vez.f_baja_pers = max_fecha
                AND per.id = vez.id_persona and per.fecha_baja=MAX_FECHA
                AND pro.fecha_fin = (SELECT MAX(pro2.fecha_fin) FROM Cribadocervix.proceso pro2 WHERE pro2.id_expediente = vez.id_expediente)
                AND pro.id_expediente = vez.id_expediente
                AND est.id_proceso = pro.id
                AND est.id = (SELECT MAX(id) FROM Cribadocervix.estudio WHERE id_proceso = pro.id)
                AND ee.id_estudio = est.id
                AND ee.contador = (SELECT MAX(contador) FROM Cribadocervix.estudio_estado WHERE id_estudio = est.id)
		"""

        // si marca check de expedientes inactivos mostramos tambien registros dados de baja (y con procesos NO activos)
		if (expedientes_inactivos) {
			sentencia = sentencia + " AND vez.f_baja <= max_fecha "
		}
		/*
		 * Expedientes activos. Si recuperamos los exp con procesos activos (AND pro.fecha_fin = MAX_FECHA )
		 * no encontrarems aquellos expedientes que no han sido dados de baja, pero que no tienen un nuevo proceso 
		 * porque cumplirá la fec max de participación en un tiempo y no cumple con los requisitos para un nuevo proceos.
		 */
		else { 
			sentencia = sentencia + " AND vez.f_baja = max_fecha "
		}

        if(tubos_dispen) {
            sentencia += """
                AND est.id LIKE (SELECT DISTINCT(id_estudio) 
                               FROM Cribadocervix.producto_dispensado
                               WHERE id_estudio = est.id
                              )
            """
        }
        if(fec_recep_lab) {
            sentencia += """
                AND est.id LIKE (SELECT DISTINCT(p.id_estudio)
                               FROM Cribadocervix.prueba p, Cribadocervix.prueba_detalle pd
                               WHERE p.id = pd.id_prueba
                               AND p.id_estudio = est.id
                               AND pd.FEC_RECEPCION_LAB IS NOT NULL 
                             )
            """
        }
		if (fec_alta_desde) {
			sentencia = sentencia + " AND TRUNC(vez.f_alta) >= :fec_alta_desde "
		}
		if (fec_alta_hasta) {
			sentencia = sentencia + " AND TRUNC(vez.f_alta) <= :fec_alta_hasta"
	   }
	   
	   if (id_tipo_estudio) {
		   sentencia = sentencia + " AND est.id_estudio_tipo = :id_tipo_estudio"
	   }
		
	   if (id_expediente) {
		   sentencia = sentencia + " AND vez.id_expediente = :id_expediente"
	   }
	  
	   if (id_tipo_proceso != null) {
		   sentencia = sentencia + " AND pro.id_proceso_tipo = :id_tipo_proceso "
	   }
	   
	   if (id_tipo_estado_estudio != null) {
		   sentencia = sentencia + " AND ee.id_cod_estado  = :id_tipo_estado_estudio"
	   }
		   
		if (null != zona_salud) {
			sentencia = sentencia + " AND (FUENTES.F_ID_ZonaSalud_Persona(PER.Id, sysdate) = :zona_salud)"
		}
        if (tipo_prueba) {
            sentencia += """ AND (pru.id = (SELECT MAX(p.id) FROM Cribadocervix.prueba p WHERE p.id_estudio = est.id)
							 AND pru.id_tipo_prueba = :tipo_prueba) """
        }
        if (fec_prueba) {
            sentencia += """ AND (pru.id = (SELECT MAX(p.id) FROM Cribadocervix.prueba p WHERE p.id_estudio = est.id)
							 AND TRUNC(pru.fecha) = :fec_prueba ) """
        }
        if (cod_cs) {
            sentencia += """ AND vez.cod_cs = :cod_cs """
        }

//		if(cancer_prev) {
//		}
		
		def limiteReg = utilsCribadocervixService.getMaxRegistros()
		parametros.put("limite", limiteReg)
		sentencia += " AND ROWNUM <= :limite "
		
		return sentencia
	}

    @Transactional(readOnly = true)
	def recuperarUltimoEventoExpediente(datos) {
		def sentencia = """
			select ID,FECHA_ALTA,CONTADOR_H,FECHA_EVENTO,LOGIN,CONTADOR_MOTIVO_EVENTO,DESCRIPCION
			from Cribadocervix.expediente_h eh
			where eh.id = :id_expediente
            and eh.fecha_alta = :fecha_alta
			and eh.CONTADOR_H = (select max(eh2.CONTADOR_H) 
                                from Cribadocervix.expediente_h eh2 
                                where eh2.id = :id_expediente
                                and eh2.fecha_alta = :fecha_alta)
		"""
	
		def resultado = groovySql.firstRow(sentencia, [id_expediente: datos.id, fecha_alta: datos.fecha_alta])
		return resultado
	}

    @Transactional(readOnly = true)
	def codigoTipoDocumento (id_documento) {
		
		def sentencia = """
			select doc.codigo
			from Cribadocervix.documento_individual di, 
				FUENTES.documento doc
			where di.id_documento= :id_documento and di.id_Documento_Def= doc.id and doc.fecha_baja=MAX_FECHA
		"""
		
		def resultado = groovySql.firstRow(sentencia, [id_documento: id_documento])
		return resultado
	}

	/**
	 * Recupera el historial de vacunación desde Vacusan de una persona mediante id_persona
	 * @param id_persona
	 * @return
	 */
    @Transactional(readOnly = true)
	def recuperarVacunacionPorIdPersona (id_persona) {
		def codigoVPH = FUENTESService.leerVariableGlobal(Constantes.CODIGO_VACUNA_VPH)

		def sentencia = """
			select D.FECHA_DOSIS as FECHA, P.DESCRIPCION as PRODUCTO, P.DESCRIPCION as DESC_VACUNA,
			    D.ORDEN as N_DOSIS, PE.DESCRIPCION as PUESTO, I.DESCRIPCION as INDICACION,
			    D.LOTE as LOTE, MT.DESCRIPCION as INDICACION_PERSONAL
            from VACUSAN.DOSIS D
                left join VACUSAN.EXPEDIENTE E on (d.ID_EXPEDIENTE = e.ID AND e.FECHA_BAJA > d.FECHA_DOSIS AND e.FECHA_ALTA <= d.FECHA_DOSIS)
                left join VACUSAN.PRODUCTO P on (d.ID_PRODUCTO = p.ID AND p.FECHA_BAJA > d.FECHA_DOSIS AND p.FECHA_ALTA <= d.FECHA_DOSIS)
                left join VACUSAN.VACUNA V on (p.ID_VACUNA = v.ID AND v.FECHA_BAJA > d.FECHA_DOSIS AND v.FECHA_ALTA <= d.FECHA_DOSIS)
                left join FUENTES.ENTIDAD PE on (PE.ID = D.ID_PUESTO_VACUNACION AND PE.FECHA_BAJA = MAX_FECHA)
                left join VACUSAN.INDICACION I on (d.ID_INDICACION = i.ID AND d.FECHA_DOSIS > i.FECHA_ALTA AND d.FECHA_DOSIS <= i.FECHA_BAJA)
                left join VACUSAN.MOTIVO_TIPO MT on MT.CONTADOR = D.ID_MOTIVO_TIPO
            where E.ID_PERSONA = :id_persona
            and V.CODIGO = :codigo_VPH
            ORDER BY D.FECHA_DOSIS DESC
		"""
        def resultado = groovySql.rows(sentencia, [id_persona :id_persona, codigo_VPH :codigoVPH])

        return resultado
	}

	/**
	 * Recupera las vacunas asociadas al VPH
	 * @return
	 */
    @Transactional(readOnly = true)
	def recuperarVacunasVPH () {
		def codigoVPH = FUENTESService.leerVariableGlobal(Constantes.CODIGO_VACUNA_VPH)

		def sentencia = """SELECT P.ID AS ID, p.DESCRIPCION AS DESCRIPCION FROM VACUSAN.PRODUCTO p
			JOIN VACUSAN.VACUNA v ON p.ID_VACUNA = v.ID
			WHERE v.CODIGO = :codigo_VPH
			AND p.FECHA_BAJA = MAX_FECHA
		"""
		def resultado = groovySql.rows(sentencia, [codigo_VPH :codigoVPH])

		return resultado
	}

	/**
	 * Recupera vacuna VPH de vacuna por ID
	 * @return
	 */
    @Transactional(readOnly = true)
	def recuperarVacunasVPHPorId (idVacuna) {
		def codigoVPH = FUENTESService.leerVariableGlobal(Constantes.CODIGO_VACUNA_VPH)

		def sentencia = """SELECT P.ID AS ID, p.DESCRIPCION AS DESCRIPCION FROM VACUSAN.PRODUCTO p
			JOIN VACUSAN.VACUNA v ON p.ID_VACUNA = v.ID 
			WHERE v.CODIGO = :codigo_VPH
			AND p.ID = :id
			AND p.FECHA_BAJA = MAX_FECHA
		"""
		def resultado = groovySql.firstRow(sentencia, [id: idVacuna, codigo_VPH :codigoVPH])

		return resultado
	}


}
