package  CRIBADOCERVIX.entidad

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import base.BaseService
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.springframework.transaction.annotation.Transactional

@Slf4j
@Transactional
@Service
class ProcesoEntidadService {

	// Servicios
	@Autowired
	BaseService baseService
	
	// Bean que nos permite hacer consultas SQL directas a la base de datos
	@Autowired
	Sql groovySql

    @Transactional(readOnly = true)
	def recuperar(datos, bloquear = false){
		def sentencia = """
			select  ID,ID_EXPEDIENTE,ID_PROCESO_TIPO,FECHA_INICIO,FECHA_FIN,INICIAL,IRREGULAR,ID_RESULTADO_TIPO,ID_PADRE
			from CRIBADOCERVIX.proceso
			where id = :id_proceso
		"""
		def resultado = baseService.recuperarEntidad(sentencia, datos)
		
		return resultado
	}
	
	def modificar(datos) {
		
		if (datos.fecha_inicio) {
			datos.fecha_inicio = datos.fecha_inicio.toTimestamp()
		}
		if (datos.fecha_fin) {
			datos.fecha_fin = datos.fecha_fin.toTimestamp()
		}
		
		def resultado
		log.debug("Modificamos tabla PROCESO: ${datos}")
		def sentencia = """
				update CRIBADOCERVIX.proceso
				set id_padre = :id_padre, fecha_inicio = :fecha_inicio , fecha_fin = :fecha_fin, INICIAL = :inicial, IRREGULAR = :irregular, 
					ID_PROCESO_TIPO = :tipo_proc_exp, ID_RESULTADO_TIPO = :resultado_proc_exp 
				where id = :id
			"""
		
		resultado = baseService.modificarEntidad(sentencia, datos)
		return resultado
	}
	
	def insertar(datos) {
	
		log.debug("Insertar en PROCESO: ${datos}")
		
		def sentencia = """
			insert into CRIBADOCERVIX.proceso (id, ID_EXPEDIENTE, ID_PROCESO_TIPO, FECHA_INICIO, FECHA_FIN,INICIAL,IRREGULAR,ID_RESULTADO_TIPO,ID_PADRE)
				values (CRIBADOCERVIX.S_PROCESO.nextval, :id_expediente, :tipo_proc_exp, :fecha_inicio, :fecha_fin, :inicial, :irregular, :resultado_proc_exp, :id_padre)
			"""
		def resultado = baseService.insertarEntidad(sentencia, datos)

		return resultado
	}
	
	def borrar(datos) {
		
		log.debug("borrar ProcesoEntidadService datos: ${datos}")
		def sentencia = """
			delete from  CRIBADOCERVIX.proceso  
			where id = :id_proceso
			"""
		return baseService.borrarEntidad(sentencia, datos)
	}

    @Transactional(readOnly = true)
	def recuperarProcesosPorIdExpediente(id, bloquear = false){

		def sentencia = """
			select p.*, 
		        pt.codigo as tipo_codigo, 
				pt.descripcion as tipo_descripcion,
		        prt.codigo as resultado_tipo_codigo,
				prt.descripcion as resultado_tipo_descripcion, 
				prt.id_nuevo_proceso_tipo, 
				prt.nuevo_proceso_irregular, 
				prt.cierre_expediente
			from CRIBADOCERVIX.proceso p
			LEFT JOIN CRIBADOCERVIX.proceso_tipo pt ON p.id_proceso_tipo = pt.id
			LEFT JOIN CRIBADOCERVIX.proceso_resultado_tipo prt ON p.ID_RESULTADO_TIPO = prt.id
			where id_expediente = :id 
			order by p.FECHA_INICIO desc
		"""
		
		
		def resultado = groovySql.rows(sentencia, [id: id])
		
		return resultado
		
	}

    @Transactional(readOnly = true)
	def recuperarProceso(datos, bloquear = false){
		def sentencia = """
			select  ID,ID_EXPEDIENTE,ID_PROCESO_TIPO,FECHA_INICIO,FECHA_FIN,INICIAL,IRREGULAR,ID_RESULTADO_TIPO,ID_PADRE
			from CRIBADOCERVIX.proceso
			where id = :id_proceso
		"""
		def resultado = baseService.recuperarEntidad(sentencia, datos)
		
		return resultado
	}
	
	def modificarProceso(datos) {
		
		if (datos.fecha_inicio) {
			datos.fecha_inicio = datos.fecha_inicio.toTimestamp()
		}
		if (datos.fecha_fin) {
			datos.fecha_fin = datos.fecha_fin.toTimestamp()
		}
		
		def resultado
		log.debug("Modificamos tabla PROCESO: ${datos}")
		def sentencia = """
				update CRIBADOCERVIX.proceso
				set id_padre = :id_padre, fecha_inicio = :fecha_inicio , fecha_fin = :fecha_fin, INICIAL = :inicial, IRREGULAR = :irregular, 
					ID_PROCESO_TIPO = :tipo_proc_exp, ID_RESULTADO_TIPO = :resultado_proc_exp 
				where id = :id
			"""
		
		resultado = baseService.modificarEntidad(sentencia, datos)
		return resultado
	}

    @Transactional(readOnly = true)
	def recuperarProcesoPadre(datos) {
		def sentencia = """
			select  ID,ID_EXPEDIENTE,ID_PROCESO_TIPO,FECHA_INICIO,FECHA_FIN,INICIAL,IRREGULAR,ID_RESULTADO_TIPO,ID_PADRE
			from CRIBADOCERVIX.proceso
			where id = :id_padre
		"""
		def resultado = baseService.recuperarEntidad(sentencia, datos)
		
		return resultado
	}

    @Transactional(readOnly = true)
	def recuperarEstudiosProceso(id_proceso) {
		
		def sentencia = """
			select id
			from CRIBADOCERVIX.estudio e
			where e.id_proceso = :id_proceso
		"""
		def resultado = groovySql.rows(sentencia, [id_proceso: id_proceso])
		return resultado
	}

    @Transactional(readOnly = true)
	def recuperarUltimoProcesoPorExpediente(idExpediente){
		def sentencia = """
			select ID, ID_PROCESO_TIPO, ID_PADRE
			from CRIBADOCERVIX.proceso 
			where id = (select max(id)
			from CRIBADOCERVIX.proceso 
			where id_expediente = :id_expediente)

		"""
		return groovySql.firstRow(sentencia, [id_expediente: idExpediente])
	}

    @Transactional(readOnly = true)
    def recuperarUltimoProcesoCerradoPorExpediente(idExpediente){
        def sentencia = """
            select ID, ID_PROCESO_TIPO, ID_PADRE
            from CRIBADOCERVIX.proceso 
            where id = (select max(id)
                        from CRIBADOCERVIX.proceso 
                        where id_expediente = :id_expediente
                        and FECHA_FIN <> max_fecha)

        """
        return groovySql.firstRow(sentencia, [id_expediente: idExpediente])
    }

    @Transactional(readOnly = true)
	def recuperarProcesoActivoPorIdExpediente(idExpediente){
		def sentencia ="""
			select p.id, p.ID_PROCESO_TIPO, 
		        pt.codigo as tipo_codigo, 
				pt.descripcion as tipo_descripcion,
		        prt.codigo as resultado_tipo_codigo,
				prt.descripcion as resultado_tipo_descripcion, 
				prt.id_nuevo_proceso_tipo, 
				prt.nuevo_proceso_irregular, 
				prt.cierre_expediente
			from CRIBADOCERVIX.proceso p
			LEFT JOIN CRIBADOCERVIX.proceso_tipo pt ON p.id_proceso_tipo = pt.id
			LEFT JOIN CRIBADOCERVIX.proceso_resultado_tipo prt ON p.ID_RESULTADO_TIPO = prt.id
			where p.id_expediente = :id_expediente
			AND p.FECHA_FIN = MAX_FECHA
			order by p.FECHA_INICIO DESC
		"""

		return groovySql.firstRow(sentencia, [id_expediente: idExpediente])
	}

}
