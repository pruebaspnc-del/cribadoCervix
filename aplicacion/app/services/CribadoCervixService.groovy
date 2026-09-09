package  CRIBADOCERVIX

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

import base.BaseService
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.springframework.transaction.annotation.Transactional

@Slf4j
@Transactional
@Service
class CribadoCervixService {
    	
	// Bean que nos permite hacer consultas SQL directas a la base de datos
	@Autowired
	Sql groovySql
	
	@Autowired
	BaseService baseServicio
	
	@Autowired
	Environment env

    @Transactional(readOnly = true)
	def recuperarTipoEventoExpediente(contador) {
		def sentencia = """
		SELECT DESCRIPCION tipo_evento 
		FROM SGI.EVENTO_TIPO 
		WHERE CONTADOR = (SELECT CONTADOR_EVENTO FROM SGI.MOTIVO_EVENTO_TIPO WHERE CONTADOR = :contador) 
		"""
		
		def resultado = groovySql.firstRow(sentencia, [contador: contador])
		return resultado
	}

    @Transactional(readOnly = true)
	def recuperarTipoMotivoEventoBaja() {
		//Renombramos contador_motivo_evento a ID, para cuando rellemos el listbox, podamos identificar cada opción
		def sentencia = """
			SELECT TABLA, CONTADOR_EVENTO, DESCRIPCION_EVENTO, CONTADOR_MOTIVO_EVENTO as id, DESCRIPCION_MOTIVO_EVENTO, SELECCIONABLE,
			CONCAT(DESCRIPCION_EVENTO, CONCAT(' ', DESCRIPCION_MOTIVO_EVENTO)) descripcion 
			FROM SGI.V_tabla_motivo_evento
			WHERE tabla like '%CRIBADOCERVIX%'
			AND contador_evento = 2
			ORDER BY tabla, CONTADOR_EVENTO, DESCRIPCION_MOTIVO_EVENTO
		"""
		
		def resultado = groovySql.rows(sentencia)
		return resultado
	}

    @Transactional(readOnly = true)
	def recuperarPersona(id) {
		def resultado
		def sentencia = """SELECT P.NOMBRE || ' ' || P.APELLIDO1 || ' ' || P.APELLIDO2 NOMBRE_COMPLETO, UF.DESCRIPCION UNIDAD_FUNCIONAL
						FROM PERSONA.PROFESIONAL_INFORMATICO PI2
						LEFT JOIN PERSONA.PERSONA P ON P.ID = PI2.ID_PROFESIONAL 
						LEFT JOIN FUENTES.UF_PROFESIONAL__CARGO UFPC ON UFPC.ID_PROFESIONAL = PI2.ID_PROFESIONAL
						LEFT JOIN FUENTES.UNIDAD_FUNCIONAL UF ON UF.ID = UFPC.ID_UNIDAD_FUNCIONAL
						WHERE PI2.ID_PROFESIONAL = '""" + id + "'"
		log.debug("recuperar persona sentencia: " + sentencia)
		resultado = groovySql.firstRow(sentencia)
		return resultado
	}
    
    /**
     * Despues de guardar en PRUEBA_DETALLE, insertamos registro en PETICION_MUESTRA para que posteriormente se envíe la peticion a laboratoio
     * @return
     */
    def insertarPeticionMuestra(datosPruebas) {
        def resultado
        datosPruebas.each{datos->
            datos.id_accion = Constantes.ID_ACCION_PETI_CREAR

            if(datos.id_tipo_prueba == Constantes.ID_TIPO_PRUEBA_VPH){
                datos.sistema_destino = Constantes.SISTEMA_DESTINO_GESTLAB
            }
            if(datos.id_tipo_prueba == Constantes.ID_TIPO_PRUEBA_CITOLOGIA){
                datos.sistema_destino = Constantes.SISTEMA_DESTINO_PATWIN
            }

            def sentenciaProcedure = """
            {call CRIBADOCERVIX.P_CREA_PETICION_SMS(CRIBADOCERVIX.F_CENTRO_ORIGEN_PETICIONARIO(?), ?, ?, ?, ?)} 
        """

            resultado = groovySql.call(sentenciaProcedure, [datos.ID, datos.COD_PRODUCTO, datos.id_accion,  datos.sistema_destino, datos.ID_MUESTRA])
            //return resultado
        }
        return resultado
    }
    
    /**
     * Compureba si una prueba tiene asociada una peticion a laboratorio en PETICION_MUESTRA
     * @param codProducto, idEstudio
     * @return true/false
     */
    @Transactional(readOnly = true)
    boolean recuperarMuestraPorCodProducto(codProducto, idEstudio){

        def sentencia = """
            SELECT pm.ID
            FROM CRIBADOCERVIX.PETICION_MUESTRA pm,
            CRIBADOCERVIX.PRUEBA_DETALLE pd,
            CRIBADOCERVIX.PRUEBA p
            WHERE pm.ID = pd.ID_MUESTRA
            AND p.ID = pd.ID_PRUEBA
            AND pm.COD_PRODUCTO = :cod_producto
            AND p.ID_ESTUDIO = :idEstudio
        """ 
        def resultado = groovySql.firstRow(sentencia, [cod_producto: codProducto, idEstudio:idEstudio])

        if(resultado)
            return true
        else
            return false
    }
}