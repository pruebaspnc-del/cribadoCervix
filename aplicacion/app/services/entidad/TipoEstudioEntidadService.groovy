package  CRIBADOCERVIX.entidad

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import base.BaseService
import  jade.EntradaService
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import  CRIBADOCERVIX.Constantes
import org.springframework.transaction.annotation.Transactional

@Slf4j
@Transactional
@Service
class TipoEstudioEntidadService {
	
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
			errores.codigo = "El código del proceso tipo no puede estar vacio"
		}
		
		if (!datos.descripcion) {
			errores.descripcion = "La descripción del proceso tipo no puede estar vacia"
		}
		
		return errores
	}

    @Transactional(readOnly = true)
	def recuperar(datos, bloquear = false){
		def sentencia = """
			select id as id_estudio_tipo, codigo, descripcion
			from CRIBADOCERVIX.estudio_tipo
			where id = :id_estudio_tipo
		"""
		def resultado = baseService.recuperarEntidad(sentencia, datos, bloquear)
		
		return resultado
	}
	
	def modificar(datos) {
		def resultado
		log.debug("Modificar ESTUDIO_TIPO: ${datos}")
		def sentencia = """
				update CRIBADOCERVIX.estudio_tipo
				set codigo = :codigo, descripcion = :descripcion
				where id = :id_estudio_tipo
				"""
		resultado = baseService.modificarEntidad(sentencia, datos)
		return resultado
	}
	
	def insertar(datos) {
		
		datos.id_requisito_tipo = entradaService.valorMaximo("CRIBADOCERVIX", "ESTUDIO_TIPO", "id") + 1
		log.debug("Insertar en ESTUDIO_TIPO: ${datos}")
		
		def sentencia = """
			insert into CRIBADOCERVIX.estudio_tipo (id, codigo, descripcion)
				values (:id_estudio_tipo, :codigo, :descripcion)
			"""
		def resultado = baseService.insertarEntidad(sentencia, datos)

		return resultado
	}
		
	def borrar(datos) {
		def sentencia = """
			delete from CRIBADOCERVIX.estudio_tipo
			where id = :id_estudio_tipo
		"""
		log.debug("Borrar en ESTUDIO_TIPO: ${datos}")
		def resultado = baseService.borrarEntidad(sentencia, datos)
		return resultado
	}

    @Transactional(readOnly = true)
	def recuperarTipoEstudioPorIdProceso(idProceso) {
		log.debug("Recuperar Tipo de Estudio del Proceso con id: ${idProceso}")
		def resultado 
		
		def sentencia = """
			select et.ID, et.CODIGO, et.DESCRIPCION
			from  CRIBADOCERVIX.ESTUDIO_TIPO et, CRIBADOCERVIX.PROCESO_TIPO__ESTUDIO_TIPO ptet
			where ptet.ID_ESTUDIO_TIPO = et.ID
            and ptet.ID_PROCESO_TIPO = :id_proceso
			order by id asc
			"""
		return resultado =  groovySql.rows(sentencia, [id_proceso: idProceso])
	}

    @Transactional(readOnly = true)
	def recuperarTodos(){
		def sentencia = """
			select id, codigo, descripcion
			from CRIBADOCERVIX.estudio_tipo
			ORDER BY ID ASC
		"""
		def resultado = groovySql.rows(sentencia)
		
		return resultado
	}

    @Transactional(readOnly = true)
	def recuperarEstudiosNoAsociadoProceso(idProceso) {
		def sentencia = """
			select id, codigo, descripcion
			from CRIBADOCERVIX.estudio_tipo
			where id not in (select id_estudio_tipo from CRIBADOCERVIX.PROCESO_TIPO__ESTUDIO_TIPO where id_proceso_tipo = :id_proceso)
		"""
		def resultado = groovySql.rows(sentencia, [id_proceso : idProceso])
		
		return resultado
	}
    
    /**
     * Recuperamos los tipos de estudio según las pruebas con los que trabaja Matrona
     * @return
     */
    @Transactional(readOnly = true)
    def recuperarEstudiosMatrona() {
        
        Map datos = [:]
        datos.id_tipo_est_vph = Constantes.ID_TIPO_ESTUDIO_VPH
        datos.id_tipo_est_cito = Constantes.ID_TIPO_ESTUDIO_CITOLOGIA
        datos.id_tipo_est_cotest_1 = Constantes.ID_TIPO_ESTUDIO_COTEST_1
        datos.id_tipo_est_cotest_2 = Constantes.ID_TIPO_ESTUDIO_COTEST_2
        
        def sentencia = """
            select ID, CODIGO, DESCRIPCION
            from  CRIBADOCERVIX.ESTUDIO_TIPO
            where id in (:id_tipo_est_vph, :id_tipo_est_cito, :id_tipo_est_cotest_1, :id_tipo_est_cotest_2)
            order by ID
        """
        return groovySql.rows(sentencia, datos)
    }

    @Transactional(readOnly = true)
	def recuperarIdPorCodigo(codigoTipoEst){
		def sentencia = """
			select id
			from CRIBADOCERVIX.estudio_tipo
			where CODIGO LIKE :codigo
		"""
		def resultado = groovySql.firstRow(sentencia, [codigo: codigoTipoEst])
		return resultado
	}
	
}
