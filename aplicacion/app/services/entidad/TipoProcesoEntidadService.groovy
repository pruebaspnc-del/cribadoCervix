package  CRIBADOCERVIX.entidad

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import base.BaseService
import  jade.EntradaService
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.springframework.transaction.annotation.Transactional

@Slf4j
@Transactional
@Service
class TipoProcesoEntidadService {
	
	// Servicios
	@Autowired
	BaseService baseService
	
	@Autowired
	EntradaService entradaService
	
	// Bean que nos permite hacer consultas SQL directas a la base de datos
	@Autowired
	Sql groovySql


	/*
	 *
	 * Métodos básicos de la entidad: validar, recuperar, modificar, insertar y borrar
	 *
	 *
	 */
	
	def validar(datos){
		def errores = [:]
		
		if (!datos.codigo) {
			errores.tipoProceso_codigo = "Debe indicar el código"
		}
		
		if (!datos.descripcion) {
			errores.tipoProceso_descripcion = "La descripción no puede estar vacia"
		}
		
		return errores
	}

    @Transactional(readOnly = true)
	def recuperar(datos, bloquear = false){
		def sentencia = """ 
			select  id, codigo, descripcion
			from CRIBADOCERVIX.proceso_tipo
			where id = :id_tipo_proceso
		"""
		def resultado = baseService.recuperarEntidad(sentencia, datos)
		
		return resultado
	}
	
	def modificar(datos) {
		def resultado
		log.debug("Modificamos tabla PROCESO_TIPO: ${datos}")
		def sentencia = """
				update CRIBADOCERVIX.proceso_tipo
				set codigo = :codigo, descripcion = :descripcion
				where id = :id
				"""
		resultado = baseService.modificarEntidad(sentencia, datos)
		return resultado
	}
	
	def insertar(datos) {
		
		datos.id = entradaService.valorMaximo("CRIBADOCERVIX", "PROCESO_TIPO", "id") + 1
		log.debug("Insertar en PROCESO_TIPO: ${datos}")
		
		def sentencia = """
			insert into CRIBADOCERVIX.proceso_tipo (id, codigo, descripcion)
				values (:id, :codigo, :descripcion)
			"""
		def resultado = baseService.insertarEntidad(sentencia, datos)

		return resultado
	}
		
	def borrar(datos) {
		def sentencia = """
			delete from CRIBADOCERVIX.proceso_tipo
			where id = :id
		"""
		log.debug("Borrar en PROCESO_TIPO: ${datos}")
		def resultado = baseService.borrarEntidad(sentencia, datos)
		return resultado
	}

    @Transactional(readOnly = true)
	def recuperarTodos(){
		
		def sentencia = """
			select id as id_proceso_tipo, codigo, descripcion
			from CRIBADOCERVIX.proceso_tipo
			order by id asc
		"""
		def resultado = groovySql.rows(sentencia)
		
		return resultado
	}
	
	
}
