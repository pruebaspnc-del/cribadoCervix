package  CRIBADOCERVIX.entidad

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import  CRIBADOCERVIX.Constantes
import base.BaseService
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionAspectSupport

import java.time.format.DateTimeFormatter


@Slf4j
@Transactional
@Service
class DocumentoEntidadService {

	// Servicios
	@Autowired
	BaseService baseService
	@Autowired
    DocumentoDevolucionEntidadService documentoDevolucionEntidadService
	@Autowired
	Sql groovySql

    def validar(datos) {
        def errores = [:]
        if (datos.id_documento_def == null) {
            errores.id_documento_def = "Campo obligatorio"
        }
        if (datos.fecha_registro != '' && datos.fecha_registro != null) {
            if (documentoDevolucionEntidadService.comprobarDocumentoDevolucion(datos.id) == false) {
                if (datos.fecha_devolucion == null) {
                    errores.fecha_devolucion = "Campo obligatorio"
                }
                if (datos.id_motivo == null) {
                    errores.id_motivo = "Campo obligatorio"
                }
            }
        }
        // No se puede marcar domicilio correcto sin estar gestionado
        if (datos.resuelto == false && datos.dom_correcto == true) {
            errores.resuelto = "Campo obligatorio"
        }
        return errores
    }

    @Transactional(readOnly = true)
	def recuperar(datos, bloquear = false) {
		log.debug("Recuperar Documento")
		def sentencia = """
			SELECT 
                D.ID, DI.FECHA, DI.ID_PROFESIONAL, D.ID_NOTIFICACION_TIPO, 
                DI.ID_DOCUMENTO_DEF, D.ID_EXPEDIENTE, D.ID_ESTUDIO, D.ID_EMISION,
                (SELECT DD.FECHA FROM CRIBADOCERVIX.DOCUMENTO_DEVOLUCION DD  WHERE DD.ID_DOCUMENTO = D.ID) FECHA_DEVOLUCION,
                (SELECT DD.FECHA_REGISTRO FROM CRIBADOCERVIX.DOCUMENTO_DEVOLUCION DD  WHERE DD.ID_DOCUMENTO = D.ID) FECHA_REGISTRO,
                (SELECT DD.ID_PROFESIONAL FROM CRIBADOCERVIX.DOCUMENTO_DEVOLUCION DD  WHERE DD.ID_DOCUMENTO = D.ID) ID_PROFESIONAL_REGISTRO,
                (SELECT DD.DOM_CORRECTO FROM CRIBADOCERVIX.DOCUMENTO_DEVOLUCION DD  WHERE DD.ID_DOCUMENTO = D.ID) DOM_CORRECTO,
                (SELECT DD.RESUELTO FROM CRIBADOCERVIX.DOCUMENTO_DEVOLUCION DD  WHERE DD.ID_DOCUMENTO = D.ID) RESUELTO,
                (SELECT DD.PROF_RESOLUCION FROM CRIBADOCERVIX.DOCUMENTO_DEVOLUCION DD  WHERE DD.ID_DOCUMENTO = D.ID) PROF_RESOLUCION,
                (SELECT DD.FEC_RESOLUCION FROM CRIBADOCERVIX.DOCUMENTO_DEVOLUCION DD  WHERE DD.ID_DOCUMENTO = D.ID) FEC_RESOLUCION,
                (SELECT DD.CODIGO_TIPO FROM CRIBADOCERVIX.DOCUMENTO_DEVOLUCION DD  WHERE DD.ID_DOCUMENTO = D.ID) ID_MOTIVO,
                (select codigo from FUENTES.DOCUMENTO where id = di.ID_DOCUMENTO_DEF and fecha_baja = MAX_FECHA) tipo_comunicacion
            FROM CRIBADOCERVIX.DOCUMENTO D, CRIBADOCERVIX.DOCUMENTO_INDIVIDUAL DI
            WHERE D.ID = :id
            AND DI.ID_DOCUMENTO = D.ID
		"""

		def resultado =  baseService.recuperarEntidad(sentencia, datos)
		return resultado
	}

    def insertar(datos) {
        log.debug("Insertar en Documentos: ${datos}")
        datos.id_notificacion_tipo = Constantes.ID_TIPO_NOTIF_CARTA

        def sentencia = "{call CRIBADOCERVIX.GESTION.P_CREA_DOCUMENTO(:id_estudio,null,:id_notificacion_tipo,:id_documento_def,:id_profesional)}"
        def resultado = baseService.insertarEntidad(sentencia, datos)
        return resultado
    }

    def modificar(datos){
        //Solo se modifica datos de DOCUMENTO_DEVOLUCION, por tanto llamamos a la entidad
        def resultado = documentoDevolucionEntidadService.modificar(datos)
        return resultado
    }

    def borrar(datos) {
        log.debug("Borrar DocumentoEntidadSerivice datos: ${datos}")
        Map resultado = [:]
        resultado.errores = [:]

        def sentenciaDocIndividual = """
                DELETE FROM CRIBADOCERVIX.DOCUMENTO_INDIVIDUAL DI 
                WHERE DI.ID_DOCUMENTO = :id_documento         
            """
        try {
            groovySql.execute(sentenciaDocIndividual, [id_documento: datos.id_documento])
        } catch (Exception e) {
            log.error("Error de borrar en DOCUMENTOS ", e)
            resultado.errores = "No se ha podido borrar el documento."
            return resultado
        }

        def sentenciaDocDevolucion = """
                DELETE FROM CRIBADOCERVIX.DOCUMENTO_DEVOLUCION DD 
                WHERE DD.ID_DOCUMENTO = :id_documento
            """
        try {
            groovySql.execute(sentenciaDocDevolucion, [id_documento: datos.id_documento])
        } catch (Exception e) {
            log.error("Error de borrar en DOCUMENTOS ", e)
            resultado.errores = "No se ha podido borrar el documento."
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
            return resultado
        }

        def sentencia = """
                DELETE FROM CRIBADOCERVIX.DOCUMENTO D
                WHERE D.ID = :id_documento
                AND D.ID_EXPEDIENTE = :id_expediente
            """
        try {
            baseService.borrarEntidad(sentencia, [id_documento: datos.id_documento, id_expediente: datos.id_expediente])
        } catch (Exception e) {
            log.error("Error de borrar en DOCUMENTOS ", e)
            resultado.errores = "No se ha podido borrar el documento."
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
            return resultado
        }

        return resultado
    }

    @Transactional(readOnly = true)
	def recuperarDocPorIdExpediente(idExpediente) {
		log.debug("Recuperar Documentos del expediente con ID: ${idExpediente}")

		def sentencia = """
            select ID, 
                ID_EXPEDIENTE, 
                ID_ESTUDIO,
                (select descripcion from CRIBADOCERVIX.NOTIFICACION_TIPO where id = documento.id_notificacion_tipo) tipo_notificacion,
                CASE  WHEN EXISTS (
                    SELECT 1 
                    FROM CRIBADOCERVIX.documento_devolucion  dd
                    WHERE dd.id_documento = ID
                ) THEN 'Devuelto'
                ELSE 'No devuelto'
                END AS devuelto,
                ID_EMISION, 
                di.fecha,
                (select descripcion from FUENTES.DOCUMENTO where id = di.ID_DOCUMENTO_DEF) tipo_comunicacion
            from  CRIBADOCERVIX.DOCUMENTO documento, CRIBADOCERVIX.documento_individual di
            where  documento.id = di.ID_DOCUMENTO
            and documento.id_expediente = :id_expediente
            order by di.fecha desc
		"""
		def resultado =  groovySql.rows(sentencia, [id_expediente: idExpediente])
		return resultado
	}
    
    def actualizarDatosUltimaDescargaDoc(datos) {
        def sentencia = """
                UPDATE CRIBADOCERVIX.DOCUMENTO_INDIVIDUAL
                SET DESCARGADO = 1,
                    FEC_ULTIMA_DESCARGA = SYSDATE,
                    LOGIN_DESCARGA = :login
                WHERE ID_DOCUMENTO = :id_documento
            """
        def resultado = groovySql.execute(sentencia, datos)
        return resultado
   }
   /**
    * Recupera todos los documentos asociados a una emision
    * @return
    */
    @Transactional(readOnly = true)
   def recuperarDocumentosPorEmision(idEmision) {
       
       def sentenciaDocumentos = """
            SELECT ID, ID_EXPEDIENTE,ID_ESTUDIO,ID_NOTIFICACION_TIPO,ID_EMISION
            FROM CRIBADOCERVIX.DOCUMENTO
            WHERE ID_EMISION = :id_emision
        """
       def resultado = groovySql.rows(sentenciaDocumentos, [id_emision: idEmision])
       return resultado
   }
   
   def auditoriaDocumentos(parametros) {
       if(!parametros.id_doc_def) {
           parametros.id_doc_def = null
       }
       if(!parametros.fecha_desde) {
           parametros.fecha_desde = null
       }
       if(!parametros.fecha_hasta) {
           parametros.fecha_hasta = null
       }
       if(!parametros.id_emision) {
           parametros.id_emision = null
       }
       if(!parametros.operacion) {
           parametros.operacion = null
       }
       def sentencia = "{call CRIBADOCERVIX.GESTION.P_AUDITORIA_DOCUMENTOS(?,?,?,?,SYSDATE,?,?,?)}"
       
       def resultado = groovySql.call(sentencia, [parametros.id_documento, parametros.id_emision, parametros.operacion, 
           parametros.login, parametros.id_doc_def, parametros.fecha_desde, parametros.fecha_hasta])
       return resultado
   }

   /**
    * Llama al proceso GESTION.P_CREA_DOCUMENTO
    * @param datos
    * @return
    */
   def creaDocumento(datos) {
       def sentencia = """
           {call CRIBADOCERVIX.GESTION.P_CREA_DOCUMENTO(?, ?, ?, ?, ?)}
       """

       def resultado = groovySql.call(sentencia, [datos.id_estudio, datos.id_emision, datos.id_tipo_notif, 
                           datos.id_tipo_doc, datos.id_profesional])
       return resultado
   }
   
   /**
    * Devuelve la cantidad de documentos asociados al idEstudio y tipo de Doc enviados por parametro
    * @return count
    */
    @Transactional(readOnly = true)
   def countDocPorIdEstudioTipoDoc(datos) {
       String sentencia = """
           SELECT count(1) as NUM
           from CRIBADOCERVIX.documento d, CRIBADOCERVIX.documento_individual di
           where d.id = di.ID_DOCUMENTO 
           and di.ID_DOCUMENTO_DEF = :id_tipo_doc
           and d.ID_ESTUDIO = :id_estudio
       """
       
       def resultado = groovySql.firstRow(sentencia, datos)
       return resultado
   }
   
   /**
    * Recuperación de documentos por tipo y fechas
    * @params fecha desde, fecha hasta
    * @return
    */
    @Transactional(readOnly = true)
   def recuperarDocsPorFechasYTipo(datos) {
       datos.fecha_desde = datos.fecha_desde.format("dd/MM/yyyy")
       datos.fecha_hasta = datos.fecha_hasta.format("dd/MM/yyyy")
       
       String sentencia = """ 
           SELECT d.ID
           FROM CRIBADOCERVIX.documento d, CRIBADOCERVIX.documento_individual di
           WHERE d.id = di.id_documento
           AND di.ID_DOCUMENTO_DEF = :id_doc_def
           AND d.ID_NOTIFICACION_TIPO = :tipo_notif
           AND trunc(di.FECHA) BETWEEN :fecha_desde and :fecha_hasta
       """
       def resultado = groovySql.rows(sentencia, datos)
       return resultado
   }

    /**
     * Recuperación del historico de descargas de documentos (no invitaciones) con profesional asociado
     * @return
     */
    @Transactional(readOnly = true)
    def recuperarHistDocsDescargados(datos){

        String sentencia
        boolean entradaDatos = false
        def resultados

        sentencia = """SELECT hd.*, d.DESCRIPCION, 
            (p.NOMBRE || ' ' || p.APELLIDO1 ||' ' || p.APELLIDO2) AS PROFESIONAL 
            FROM CRIBADOCERVIX.HISTORICO_DESCARGAS hd 
            JOIN FUENTES.DOCUMENTO d ON (hd.ID_DOCUMENTO_DEF = d.ID)
            JOIN PERSONA.PROFESIONAL_INFORMATICO pi2 ON (hd.LOGIN = pi2.LOGIN)
            JOIN PERSONA.PERSONA p ON (pi2.ID_PROFESIONAL = p.ID)
            WHERE hd.ID_EMISION IS NULL
            AND hd.FECHA_DESDE IS NOT NULL
            AND hd.FECHA_HASTA IS NOT NULL
            AND d.FECHA_BAJA = MAX_FECHA
            AND p.FECHA_BAJA = MAX_FECHA """

        if(datos.tipoDoc != null){
            sentencia += """AND d.ID = :tipoDoc """
            entradaDatos = true
        }

        if(datos.profDes != null){
            sentencia += """AND pi2.ID_PROFESIONAL = :profDes """
            entradaDatos = true
        }

        sentencia += """ ORDER BY hd.FECHA DESC """
        if(entradaDatos) {
            resultados = groovySql.rows(sentencia, datos)
        }else {
            resultados = groovySql.rows(sentencia)
        }
        return resultados
    }

    /**
     * Metodo que recupera los profesionales que han realizado descargas masivas de documentos
     * @return
     */
    @Transactional(readOnly = true)
    def recuperarProfInformaticosHistDoscDescargados(){
        String sentencia = """SELECT DISTINCT (p.NOMBRE ||' '|| p.APELLIDO1 ||' '|| p.APELLIDO2) AS DESCRIPCION, p.ID, hd.LOGIN 
            FROM CRIBADOCERVIX.HISTORICO_DESCARGAS hd
            JOIN PERSONA.PROFESIONAL_INFORMATICO pi2 ON (hd.LOGIN = pi2.LOGIN)
            JOIN PERSONA.PERSONA p ON (pi2.ID_PROFESIONAL = p.ID)
            WHERE p.FECHA_BAJA = MAX_FECHA
        """

        def resultado = groovySql.rows(sentencia)
        return resultado
    }

    /**
     * Recupera la fecha de un documento de la tabla CRIBADOCERVIX.DOCUMENTO_INDIVIDUAL
     * @param id_documento
     * @return
     */
    @Transactional(readOnly = true)
    def recuperarFechaDocumento(id_documento) {
        def sentencia = """
            select DI.FECHA
            from CRIBADOCERVIX.DOCUMENTO_INDIVIDUAL DI
            left join CRIBADOCERVIX.DOCUMENTO D on D.ID = DI.ID_DOCUMENTO
            where DI.ID_DOCUMENTO = :id_documento
		"""

        def resultado = groovySql.firstRow(sentencia, [id_documento: id_documento])
        return resultado
    }

}
