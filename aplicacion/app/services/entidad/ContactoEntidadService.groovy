package  CRIBADOCERVIX.entidad

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import base.BaseService
import  jade.FUENTESService
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.springframework.transaction.annotation.Transactional

@Slf4j
@Transactional
@Service
class ContactoEntidadService {

	// Servicios
	@Autowired
	BaseService baseService
	@Autowired
	FUENTESService FUENTESService
	
	// Bean que nos permite hacer consultas SQL directas a la base de datos
	@Autowired
	Sql groovySql

	def validar (datos) {
		def errores = [:]

		if (datos.contactado == null) {
		   errores.put("contactado", "Obligatorio indicar si ha sido contactado o no.")
		}
		if (datos.id_estudio == "0") {
			errores.put("id_estudio", "Motivo de contacto incorrecto.")
		}
		return errores
	}

    @Transactional(readOnly = true)
	def recuperar(datos, bloquear = false) {
		log.debug("Recuperar Contactos del estudio con ID: ${datos.id_estudio}")

		def sentencia = """
			select ID_ESTUDIO, CONTADOR, ID_MOTIVO_CONTACTO, VALOR_MEDIO_COMUNICACION, 
			to_date(to_char(FECHA, 'dd/MM/yyyy HH24:MI:SS'), 'dd/MM/yyyy hh24:mi:ss') FECHA,
			ID_PROFESIONAL, OBSERVACIONES, CONTACTADO
			from  CRIBADOCERVIX.REGISTRO_CONTACTO
			where id_estudio = :id_estudio 
			"""

		if (datos.contador != null && datos.contador != "" ) {
			sentencia = sentencia + " and contador = :contador "
		}

		sentencia = sentencia + " order by FECHA desc"
		def resultado =  baseService.recuperarEntidad(sentencia, datos)

		return resultado
	}

	def insertar(datos) {
		log.debug("Insertar en REGISTRO_CONTACTO: ${datos}")

		//Recuperamos el maximo contador de Contactos del estudio
		def contador = recuperarMaxContadorPorEstudio(datos)
		//Si no tiene, ponemos valor 1, sino sumamos uno para el nuevo registro
		if (contador[0] == null) {
			datos.contador = 1
		}else {
			datos.contador = contador[0] + 1
		}

		//Formateamos la fecha con Horas
		datos.fecha = datos.fecha.format("dd/MM/yyyy HH:mm:ss")

		def sentencia = """
		insert into CRIBADOCERVIX.REGISTRO_CONTACTO (ID_ESTUDIO, CONTADOR, ID_MOTIVO_CONTACTO,
				VALOR_MEDIO_COMUNICACION, FECHA, ID_PROFESIONAL, OBSERVACIONES, CONTACTADO)
			values (:id_estudio, :contador, :id_motivo_contacto, :valor_medio_comunicacion,
				to_date(:fecha, 'dd/MM/yyyy HH24:mi:ss'), :id_profesional, :observaciones, :contactado)
		"""
		def resultado = baseService.insertarEntidad(sentencia, datos)

		return resultado
	}

	def modificar(datos) {
		log.debug("Modificar en REGISTRO_CONTACTO: ${datos}")

		/* No se modifica la fecha, ya que crea problemas entre el contador y la fecha si no se editan en orden logico
		 * datos.fecha = datos.fecha.format("dd/MM/yyyy HH:mm:ss")
		 * FECHA = to_date(:fecha, 'dd/MM/yyyy HH24:mi:ss'),*/

		def sentencia = """
			UPDATE CRIBADOCERVIX.REGISTRO_CONTACTO
			SET ID_PROFESIONAL = :id_profesional,
                FECHA = SYSDATE,
				ID_MOTIVO_CONTACTO = :id_motivo_contacto,
				VALOR_MEDIO_COMUNICACION = :valor_medio_comunicacion,
				OBSERVACIONES = :observaciones,
				CONTACTADO = :contactado
			where id_estudio = :id_estudio
			AND contador = :contador

			"""
		def resultado = baseService.modificarEntidad(sentencia, datos)
		return resultado
	}

	def borrar(datos) {
		log.debug("borrar ContactoEntidadService datos: ${datos}")
		def sentencia = """
			delete from  CRIBADOCERVIX.registro_contacto  
			where id_estudio = :id_estudio
			and contador = :contador
		"""

		def resultado =  baseService.borrarEntidad(sentencia, datos)
		return resultado
	}

    @Transactional(readOnly = true)
	def recuperarMaxContadorPorEstudio(datos) {
		def sentencia = """
			select max(CONTADOR)
			from  CRIBADOCERVIX.REGISTRO_CONTACTO
			where id_estudio = :id_estudio
		"""

		return groovySql.firstRow(sentencia, datos)
	}

    @Transactional(readOnly = true)
	def recuperarContactosPorIdExpediente(idExpediente) {
		log.debug("Recuperar Contactos del expediente con ID: ${idExpediente}")

		def sentencia = """

			select ID_ESTUDIO, CONTADOR,
			( SELECT descripcion 
			  from FUENTES.MOTIVO_CONTACTO
			  where id in (regCont.id_motivo_contacto) ) descripcion_motivo_contacto,
			VALOR_MEDIO_COMUNICACION,
			to_char(regCont.FECHA, 'dd/MM/yyyy HH24:MI:SS') FECHA,
			ID_PROFESIONAL, OBSERVACIONES,
			DECODE(CONTACTADO,1,'Si',0,'No', '') CONTACTADO
			from  CRIBADOCERVIX.REGISTRO_CONTACTO regCont
			where id_estudio IN (select id
								 from CRIBADOCERVIX.estudio
								 where id_proceso IN (select id
													from CRIBADOCERVIX.proceso
													where id_expediente = :id_expediente )
								)
			order by regCont.FECHA desc

		"""
		return groovySql.rows(sentencia, [id_expediente : idExpediente])
	}

    /**
     *  Recuperamos numero de contactos negativos (NO contactados) asociados al estudio y motivo de contacto enviados por parametro
     * @param id Estudio, motivo de contacto
     * @return contador
     */
    @Transactional(readOnly = true)
    def recuperarNumContactosNegPorEstudio(datos){
        def sentencia = """
            SELECT count(1) NUM
            FROM CRIBADOCERVIX.registro_contacto
            WHERE ID_ESTUDIO = :id_estudio
            AND CONTACTADO = 0
            AND ID_MOTIVO_CONTACTO = :motivo_contacto
        """
        /*IN (SELECT ID 
              FROM FUENTES.MOTIVO_CONTACTO
              WHERE CODIGO LIKE ':motivo_contacto') */
        
        def resultado = groovySql.firstRow(sentencia, datos)
		return resultado
    }

	/**
	 * Recupera los contactos de una persona mediante el id_estudio
	 * @param datos
	 * @param bloquear
	 * @return
	 */
    @Transactional(readOnly = true)
	def recuperarContactosporEstudio(datos, bloquear = false) {
		log.debug("Recuperar Contactos del estudio con ID: ${datos.id_estudio}")

		def sentencia = """
            select ID_ESTUDIO, CONTADOR, ID_MOTIVO_CONTACTO,
            ( SELECT descripcion 
              from FUENTES.MOTIVO_CONTACTO
              where id in (regCont.id_motivo_contacto) ) descripcion_motivo_contacto,
            VALOR_MEDIO_COMUNICACION,
            FECHA,
            ID_PROFESIONAL, OBSERVACIONES, 
            DECODE(CONTACTADO,1,'Si',0,'No', '') CONTACTADO
            from  CRIBADOCERVIX.REGISTRO_CONTACTO regCont
            where id_estudio = :id_estudio 
        """
		sentencia = sentencia + " order by FECHA desc"
		def resultado =  groovySql.rows(sentencia, datos)

		return resultado
	}

	/**
	 * Recupera los contactos de una persona mediante el id_proceso
	 * @param datos
	 * @param bloquear
	 * @return
	 */
    @Transactional(readOnly = true)
	def recuperarContactosporProceso(datos, bloquear = false) {
		log.debug("Recuperar Contactos del proceso con ID: ${datos.id_proceso}")

		def sentencia = """
            select ID_ESTUDIO, CONTADOR, ID_MOTIVO_CONTACTO,
            ( SELECT descripcion 
              from FUENTES.MOTIVO_CONTACTO
              where id in (regCont.id_motivo_contacto) ) descripcion_motivo_contacto,
            VALOR_MEDIO_COMUNICACION,
            FECHA,
            ID_PROFESIONAL, OBSERVACIONES, 
            CONTACTADO
            from  CRIBADOCERVIX.REGISTRO_CONTACTO regCont
            join CRIBADOCERVIX.ESTUDIO est on est.id = regCont.id_estudio
            join CRIBADOCERVIX.PROCESO proc on proc.id = est.id_proceso
            where proc.id = :id_proceso
        """
		sentencia = sentencia + " order by FECHA desc"
		def resultado =  groovySql.rows(sentencia, datos)

		return resultado
	}


}
