package  CRIBADOCERVIX.entidad

import base.BaseService
import  jade.EntradaService
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Slf4j
@Transactional
@Service
class EstudioResultadoTipoEntidadService {
	
	// Servicios
	@Autowired
	BaseService baseService
	@Autowired
	EntradaService entradaService
	// Bean que nos permite hacer consultas SQL directas a la base de datos
	@Autowired
	Sql groovySql

    @Transactional(readOnly = true)
	def recuperar(datos, bloquear = false){
		def sentencia = """
			select ID,CODIGO,DESCRIPCION,NO_COMPROBAR_FECHA_FIN,ID_PROCESO_RESULTADO_TIPO,ID_NUEVO_ESTUDIO_TIPO,
				PLAZO_NUEVO_ESTUDIO,FECHA_ALTA,FECHA_BAJA,COMPROBAR_VPH,RESULT_VPH
			from CRIBADOCERVIX.ESTUDIO_RESULTADO_TIPO
			where id = :id_resultado_estado
			"""
		def resultado = baseService.recuperarEntidad(sentencia, datos, bloquear)
		
		return resultado
	}

    @Transactional(readOnly = true)
	def recuperarTodos(){
		def sentencia = """
			select ID,CODIGO,DESCRIPCION,NO_COMPROBAR_FECHA_FIN,ID_PROCESO_RESULTADO_TIPO,ID_NUEVO_ESTUDIO_TIPO,
				PLAZO_NUEVO_ESTUDIO,FECHA_ALTA,FECHA_BAJA,COMPROBAR_VPH,RESULT_VPH
			from CRIBADOCERVIX.ESTUDIO_RESULTADO_TIPO
			order by id asc
		"""
		def resultado = groovySql.rows(sentencia)
		return resultado
	}

    @Transactional(readOnly = true)
	def recuperarIdPorCodigo(codigoResultadoEst){
		def sentencia = """
			select id
			from CRIBADOCERVIX.ESTUDIO_RESULTADO_TIPO
			where CODIGO LIKE :codigo
		"""
		def resultado = groovySql.firstRow(sentencia, [codigo: codigoResultadoEst])
		return resultado
	}
	
}
