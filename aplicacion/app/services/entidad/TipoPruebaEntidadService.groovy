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
class TipoPruebaEntidadService {
	
	// Servicios
	@Autowired
	BaseService baseService
	@Autowired
	EntradaService entradaService
	// Bean que nos permite hacer consultas SQL directas a la base de datos
	@Autowired
	Sql groovySql

	/*Métodos básicos de la entidad: validar, recuperar, modificar, insertar y borrar*/
    @Transactional(readOnly = true)
	def recuperar(datos, bloquear = false){
		def sentencia = """ 
			select  id, codigo, descripcion
			from CRIBADOCERVIX.prueba_tipo
			where id = :id_prueba_tipo
		"""
		def resultado = baseService.recuperarEntidad(sentencia, datos)
		return resultado
	}
	
	def modificar(datos) {
		def resultado
		log.debug("Modificamos tabla PRUEBA_TIPO: ${datos}")
		def sentencia = """
				update CRIBADOCERVIX.prueba_tipo
				set codigo = :codigo, descripcion = :descripcion
				where id = :id_prueba_tipo
				"""
		resultado = baseService.modificarEntidad(sentencia, datos)
		return resultado
	}
	
	def insertar(datos) {
		datos.id_prueba_tipo = entradaService.valorMaximo("CRIBADOCERVIX", "PRUEBA_TIPO", "id") + 1
		log.debug("Insertar en PRUEBA_TIPO: ${datos}")
		
		def sentencia = """
			insert into CRIBADOCERVIX.prueba_tipo (id, codigo, descripcion)
				values (:id_prueba_tipo, :codigo, :descripcion)
			"""
		def resultado = baseService.insertarEntidad(sentencia, datos)
		return resultado
	}
		
	def borrar(datos) {
		def sentencia = """
			delete from CRIBADOCERVIX.PRUEBA_TIPO
			where id = :id_prueba_tipo
		"""
		log.debug("Borrar en PRUEBA_TIPO: ${datos}")
		def resultado = baseService.borrarEntidad(sentencia, datos)
		return resultado
	}
	
    /**
     * RECUPERAMOS TODOS LOS TIPOS DE PRUEBA 
     * @param order, para definir el orden que queramos dependiendo de la pantalla donde estemos
     * @return
     */
    @Transactional(readOnly = true)
	def recuperarTodos(String order){
		def sentencia = """
			select id, codigo, descripcion
			from CRIBADOCERVIX.PRUEBA_TIPO
		"""
        sentencia += "order by " + order
        
		def resultado = groovySql.rows(sentencia)
		return resultado
	}
  
    
    /**
     * Recuperamos los tipos de prueba con los que trabaja Matrona
     * @return
     */
    @Transactional(readOnly = true)
    def recuperarPruebasMatrona() {
        Map datos = [:]
        datos.id_tipo_pru_vph = Constantes.ID_TIPO_PRUEBA_VPH
        datos.id_tipo_pru_cito = Constantes.ID_TIPO_PRUEBA_CITOLOGIA
        def sentencia = """
            select ID, CODIGO, DESCRIPCION
            from  CRIBADOCERVIX.PRUEBA_TIPO
            where id in (:id_tipo_pru_vph, :id_tipo_pru_cito)
            order by ID
        """

        return groovySql.rows(sentencia, datos)
    }

    @Transactional(readOnly = true)
    def recuperarPruebasPorTipoEstudio(tipoEstudio) {
        def sentencia = """
            select ID, CODIGO, DESCRIPCION
            from  CRIBADOCERVIX.prueba_tipo
            where id in (select ID_PRUEBA_TIPO
                        from CRIBADOCERVIX.ESTUDIO_TIPO__PRUEBA_TIPO
                        where ID_ESTUDIO_TIPO = :id_tipo_estudio)
            order by ID
        """
    
        return groovySql.rows(sentencia, [id_tipo_estudio: tipoEstudio])
    }

    @Transactional(readOnly = true)
	def recuperarIdPorCodigo(codigoTipoPru){
		def sentencia = """ 
			select  id
			from CRIBADOCERVIX.prueba_tipo
			where codigo like :codigo
		"""
		def resultado = groovySql.firstRow(sentencia, [codigo: codigoTipoPru])
		return resultado
	}
	
}
