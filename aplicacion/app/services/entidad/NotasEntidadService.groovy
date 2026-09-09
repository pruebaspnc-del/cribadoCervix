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
class NotasEntidadService {

    // Servicios
    @Autowired
    BaseService baseService
    @Autowired
    FUENTESService FUENTESService
    @Autowired
    Sql groovySql

    def validar(datos) {
        def errores = [:]

        if (datos.id_profesional_actual == null || datos.id_profesional == null) {
            errores.put("profesional", "Error Profesional")
        }
        if (datos.notas == null) {
            errores.put("notas", "Obligatorio indicar nota")
        }
        if (datos.fecha == null) {
            errores.put("fecha", "Obligatorio indicar fecha")
        }


        return errores
    }

    @Transactional(readOnly = true)
    def recuperar(datos, bloquear = false) {
        log.debug("Recuperar Notas del expediente con ID: ${datos.id_expediente}")

        def sentencia = """
			select ID_EXPEDIENTE, CONTADOR, FECHA, NOTAS, ID_PROFESIONAL, PERSONA.F_NOMBRE_PERSONA(ID_PROFESIONAL, sysdate) PROFESIONAL
			from CRIBADOCERVIX.EXPEDIENTE_NOTAS
			where id_expediente = :id_expediente
			"""
        if (datos.contador != null && datos.contador != "") {
            sentencia = sentencia + " and contador = :contador "
        }

        sentencia = sentencia + " order by FECHA desc"
        def resultado = baseService.recuperarEntidad(sentencia, datos)

        return resultado
    }

    def insertar(datos) {
        log.debug("Insertar en EXPEDIENTE_NOTAS: ${datos}")

        // Recuperamos el máximo contador de Notas del expediente
        def contador = recuperarMaxContadorPorExpediente(datos)
        // Si no tiene, ponemos valor 1, sino sumamos uno para el nuevo registro
        if (contador[0] == null) {
            datos.contador = 1
        } else {
            datos.contador = contador[0] + 1
        }

        // Formateamos la fecha con horas
        datos.fecha = datos.fecha.format("dd/MM/yyyy HH:mm:ss")

        def sentencia = """
			insert into CRIBADOCERVIX.EXPEDIENTE_NOTAS (ID_EXPEDIENTE, CONTADOR, FECHA, NOTAS, ID_PROFESIONAL)
			values (:id_expediente, :contador, to_date(:fecha, 'dd/MM/yyyy HH24:mi:ss'), :notas, :id_profesional)
		"""

        def resultado = baseService.insertarEntidad(sentencia, datos)

        return resultado
    }

    def modificar(datos) {
        log.debug("Modificar en EXPEDIENTE_NOTAS: ${datos}")

        datos.fecha = datos.fecha.format("dd/MM/yyyy HH:mm:ss")

        def sentencia = """
		update CRIBADOCERVIX.EXPEDIENTE_NOTAS
		set ID_PROFESIONAL = :id_profesional_actual,
			NOTAS = :notas,
			FECHA = to_date(:fecha, 'dd/MM/yyyy HH24:mi:ss')
		where id_expediente = :id_expediente
		and contador = :contador
		"""

        def resultado = baseService.modificarEntidad(sentencia, datos)
        return resultado
    }

    def borrar(datos) {
        log.debug("borrar NotasENtidadService: ${datos}")
        def sentencia = """
			delete from CRIBADOCERVIX.EXPEDIENTE_NOTAS
			where id_expediente = :id_expediente
			and contador = :contador
		"""

        def resultado = baseService.borrarEntidad(sentencia, datos)
        return resultado
    }

    /**
     * Recupera el contador de la última nota por expediente
     * @param datos datos.id_expediente
     * @return
     */
    @Transactional(readOnly = true)
    def recuperarMaxContadorPorExpediente(datos) {
        def sentencia = """
			select max(CONTADOR)
			from CRIBADOCERVIX.EXPEDIENTE_NOTAS
			where id_expediente = :id_expediente
		"""

        return groovySql.firstRow(sentencia, datos)
    }

    /**
     * Recuperamos las notas relacionadas con un ID Expediente
     * @param datos datos.id_expediente
     * @return
     */
    @Transactional(readOnly = true)
    def recuperarNotasPorIdExpediente(datos) {
        log.debug("Recuperar Notas del expediente con ID: ${datos.id_expediente}")

        def sentencia = """
			select EN.ID_EXPEDIENTE, EN.CONTADOR, EN.FECHA, EN.NOTAS, EN.ID_PROFESIONAL, PERSONA.F_NOMBRE_PERSONA(EN.ID_PROFESIONAL, sysdate) PROFESIONAL
			from CRIBADOCERVIX.EXPEDIENTE_NOTAS EN
			where id_expediente = :id_expediente 
			order by FECHA desc
		"""

        def resultado = groovySql.rows(sentencia, datos)

        return resultado
    }

}
