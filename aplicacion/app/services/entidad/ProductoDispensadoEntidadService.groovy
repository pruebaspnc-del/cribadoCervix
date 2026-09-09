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
class ProductoDispensadoEntidadService {
	
	// Servicios
	@Autowired
	BaseService baseService
	@Autowired
	FUENTESService FUENTESService
	
	// Bean que nos permite hacer consultas SQL directas a la base de datos
	@Autowired
	Sql groovySql

    @Transactional(readOnly = true)
	def recuperar(datos, bloquear = false) {
		log.debug("Recuperar Productos Dispensados")
		
		def sentencia = """
			select ID_ESTUDIO, FEC_DISPENSACION, COD_FARMACIA, COD_PROFESIONAL, ID_NOTIFICACION, NO_NOTIFICADO,
				   OBSERVACIONES, TLFCORRECTO, TELEFONO, COD_PRODUCTO
			from  CRIBADOCERVIX.PRODUCTO_DISPENSADO
			order by FEC_DISPENSACION desc
			"""
		
		def resultado =  baseService.recuperarEntidad(sentencia, datos)
		
		return resultado
	}

    @Transactional(readOnly = true)
	def recuperarPorIdEstudio(datos, bloquear = false) {
		log.debug("Recuperar Productos Dispensados del estudio con ID: ${datos.id}")
		
		def sentencia = """
			select pd.ID_ESTUDIO, pd.FEC_DISPENSACION, pd.COD_FARMACIA, pd.COD_PROFESIONAL, pd.ID_NOTIFICACION, pd.NO_NOTIFICADO,
			       pd.OBSERVACIONES, pd.TLFCORRECTO, pd.TELEFONO, pd.COD_PRODUCTO, pde.fec_estado,  prodEs.codigo estado
			from  CRIBADOCERVIX.PRODUCTO_DISPENSADO pd, CRIBADOCERVIX.PRODUCTO_DISPENSADO__ESTADO pde, CRIBADOCERVIX.PRODUCTO_ESTADO prodEs
			where pd.COD_PRODUCTO = pde.COD_PRODUCTO
			AND prodEs.ID = pde.ID_PRODUCTO_ESTADO
			and pde.contador = (select max(contador) from CRIBADOCERVIX.producto_dispensado__estado where cod_producto = pde.cod_producto)
			and id_estudio = :id
			order by pde.FEC_ESTADO desc
			"""
		
		def resultado = groovySql.rows(datos, sentencia)
		
		return resultado
	}

    @Transactional(readOnly = true)
	def recuperarEstadoProductoDispensado(datos) {
		
		def sentencia = """
			
			select prodEs.descripcion,  pde.FEC_ESTADO
			from CRIBADOCERVIX.PRODUCTO_ESTADO prodEs, CRIBADOCERVIX.PRODUCTO_DISPENSADO__ESTADO pde
			where prodEs.ID = pde.ID_PRODUCTO_ESTADO
            and pde.COD_PRODUCTO = :cod_producto
            and pde.contador = (select max(pde3.contador) 
                                from CRIBADOCERVIX.PRODUCTO_DISPENSADO__ESTADO pde3
                                where pde3.COD_PRODUCTO = :cod_producto)

		"""
		
		def resultado = groovySql.firstRow(datos, sentencia)
		
		return resultado
	}
	
}
