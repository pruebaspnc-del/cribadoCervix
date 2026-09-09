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
class VariablesGlobalesEntidadService {
	
	// Servicios
	@Autowired
	BaseService baseService
	
	@Autowired
	EntradaService entradaService
	
	// Bean que nos permite hacer consultas SQL directas a la base de datos
	@Autowired
	Sql groovySql

    @Transactional(readOnly = true)
	def recuperar(datos){
		def sentencia = """ 
			SELECT NOMBRE_VARIABLE, VALOR, COMENTARIO
			FROM CRIBADOCERVIX.VARIABLE_GLOBAL
            WHERE NOMBRE_VARIABLE = :nombre_variable
			ORDER BY NOMBRE_VARIABLE
		"""
		def resultado = baseService.recuperarEntidad(sentencia, datos)
		
		return resultado
	}

    @Transactional(readOnly = true)
	def recuperarTodos() {
		def sentencia = """
			SELECT NOMBRE_VARIABLE, VALOR, COMENTARIO
			FROM CRIBADOCERVIX.VARIABLE_GLOBAL
			ORDER BY NOMBRE_VARIABLE
		"""
		def resultado = groovySql.rows(sentencia)
		
		return resultado
	}

    @Transactional(readOnly = true)
    def recuperarVariablesCartasInvRepeticion(datos, bloquear = false){
        def sentencia = """
            SELECT NOMBRE_VARIABLE, VALOR
            FROM CRIBADOCERVIX.VARIABLE_GLOBAL
            WHERE NOMBRE_VARIABLE in (:nombre_1, :nombre_2, :nombre_3)
            ORDER BY NOMBRE_VARIABLE
        """
        def resultado = groovySql.rows(sentencia, datos)
        
        return resultado
    }
	
	def insertar(datos) {
			
		def sentencia = """
			insert into CRIBADOCERVIX.VARIABLE_GLOBAL (NOMBRE_VARIABLE, VALOR, COMENTARIO)
				values (:nombre_variable, :valor, :comentario)
			"""
		def resultado = baseService.insertarEntidad(sentencia, datos)

		return resultado
		
	}
	
	def modificar(datos) {
		def resultado = [:]
		def sentencia = """
				update CRIBADOCERVIX.VARIABLE_GLOBAL
				set valor = :valor, comentario = :comentario
				where nombre_variable = :nombre_variable
				"""
		resultado = baseService.modificarEntidad(sentencia, datos)
		return resultado
	}
	
	def borrar(nombre_variable) {
		def sentencia = """
			delete from CRIBADOCERVIX.VARIABLE_GLOBAL
			where nombre_variable = :nombre_variable
		"""
		def resultado = baseService.borrarEntidad(sentencia, [nombre_variable : nombre_variable])
		return resultado
	}
	
	boolean existeNombreVariable(nombre_variable) {
		def existe = false
		def sentencia = """
            SELECT COUNT(1)
            FROM CRIBADOCERVIX.variable_global 
            WHERE nombre_variable = :nombre_variable
        """
		def resultado = groovySql.firstRow(sentencia, [nombre_variable : nombre_variable])
		
		if (!resultado.isEmpty()) {
			existe = true
		}
		return existe		
	}
	
}
