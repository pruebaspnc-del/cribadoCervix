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
class TipoEstadoEstudioEntidadService {
	
	// Servicios
	@Autowired
	BaseService baseService
	
	@Autowired
	EntradaService entradaService
	
	// Bean que nos permite hacer consultas SQL directas a la base de datos
	@Autowired
	Sql groovySql


	/*
	 * Métodos básicos de la entidad: validar, recuperar, modificar, insertar y borrar
	 *
	 */
	
	def validar(datos){
		def errores = [:]
		
		if (!datos.codigo) {
			errores.codigo = "El código no puede estar vacio"
		}
		
		if (!datos.descripcion) {
			errores.descripcion = "La descripción no puede estar vacia"
		}
		
		return errores
	}

    @Transactional(readOnly = true)
	def recuperar(datos, bloquear = false){
		def sentencia = """
			select id, codigo, descripcion
			from CRIBADOCERVIX.TIPO_ESTADO_ESTUDIO
			where id = :id_tipo_estado
			"""
		def resultado = baseService.recuperarEntidad(sentencia, datos, bloquear)
		
		return resultado
	}
	
	def modificar(datos) {
		def resultado
		log.debug("Modificar TIPO_ESTADO_ESTUDIO: ${datos}")
		def sentencia = """
				update CRIBADOCERVIX.TIPO_ESTADO_ESTUDIO
				set codigo = :codigo, descripcion = :descripcion
				where id = :id_tipo_estado
				"""
		//resultado = baseService.modificarEntidad(sentencia, datos)
		resultado = groovySql.execute(sentencia, datos)
		return resultado
	}
	
	def insertar(datos) {
		
		datos.id_tipo_estado = entradaService.valorMaximo("CRIBADOCERVIX", "TIPO_ESTADO_ESTUDIO", "id") + 1
		log.debug("Insertar en ESTUDIO_TIPO: ${datos}")
		
		def sentencia = """
			insert into CRIBADOCERVIX.TIPO_ESTADO_ESTUDIO (id, codigo, descripcion)
				values (:id_tipo_estado, :codigo, :descripcion)
			"""
		def resultado = baseService.insertarEntidad(sentencia, datos)

		return resultado
	}
		
	def borrar(datos) {
		def sentencia = """
			delete from CRIBADOCERVIX.TIPO_ESTADO_ESTUDIO
			where id = :id_tipo_estado
		"""
		log.debug("Borrar en TIPO_ESTADO_ESTUDIO: ${datos}")
		def resultado = baseService.borrarEntidad(sentencia, datos)
		return resultado
	}
    @Transactional(readOnly = true)
	def recuperarTodos(){
		def sentencia = """
			select id, codigo, descripcion
			from CRIBADOCERVIX.TIPO_ESTADO_ESTUDIO
			order by id asc
		"""
		def resultado = groovySql.rows(sentencia)
		
		return resultado
	}

    @Transactional(readOnly = true)
    def recuperarEstadosPorEstudio(idTipoEstudio){
        log.debug("Recuperamos estados asociados al estudio con id: ${idTipoEstudio}")

        def sentencia = """
            select id, codigo, descripcion 
            from CRIBADOCERVIX.TIPO_ESTADO_ESTUDIO
            where id in ( select EST_TIPO__ESTADO.ID_ESTADO 
                          from CRIBADOCERVIX.EST_TIPO__ESTADO
                          where EST_TIPO__ESTADO.ID_EST_TIPO = :id_tipo_estudio)
            order by id
        """
        return groovySql.rows(sentencia, [id_tipo_estudio: idTipoEstudio])
    }

    @Transactional(readOnly = true)
	def recuperarIdPorCodigo(codigoResultadoEst){
		def sentencia = """
			select id
			from CRIBADOCERVIX.TIPO_ESTADO_ESTUDIO
			where CODIGO LIKE :codigo
		"""
		def resultado = groovySql.firstRow(sentencia, [codigo: codigoResultadoEst])
		return resultado
	}
	
}
