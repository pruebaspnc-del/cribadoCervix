package  CRIBADOCERVIX.entidad

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

import base.BaseService
import base.PermisosEntidadService
import  jade.FUENTESService
import  CRIBADOCERVIX.Constantes
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.springframework.transaction.annotation.Transactional

@Slf4j
@Transactional
@Service
class EstudioEntidadService {

    // Servicios
    @Autowired
    BaseService baseService
    @Autowired
    FUENTESService FUENTESService
    @Autowired
    PermisosEntidadService permisosEntidadService

    // Bean que nos permite hacer consultas SQL directas a la base de datos
    @Autowired
    Sql groovySql
    @Autowired
    Environment env

    @Transactional(readOnly = true)
    def recuperar(datos, bloquear = false) {
        log.debug("Recuperar Entidad Estudio, id: ${datos.id_estudio}")

        def sentencia = """
			select ID,	ID_PROCESO,	ID_ESTUDIO_TIPO,FECHA_INICIO, ID_ESTUDIO_RESULTADO_TIPO,
            ID_UNIDAD_FUNCIONAL,PROFESIONAL,FECHA_RESULTADO
			from  CRIBADOCERVIX.estudio
			where id = :id_estudio
			"""
        return baseService.recuperarEntidad(sentencia, datos)
    }

    @Transactional(readOnly = true)
    def recuperarListaPorIdProceso(id_proceso, bloquear = false){
        def sentencia = """
			SELECT e.ID, e.ID_PROCESO, e.ID_ESTUDIO_TIPO, e.FECHA_INICIO, e.ID_ESTUDIO_RESULTADO_TIPO,
				e.ID_UNIDAD_FUNCIONAL, e.PROFESIONAL, e.FECHA_RESULTADO,
				(select et.codigo from CRIBADOCERVIX.estudio_tipo et
                where e.ID_ESTUDIO_TIPO = et.id) codigo_estudio_tipo,
               (select et2.descripcion  from CRIBADOCERVIX.estudio_tipo et2
                where e.ID_ESTUDIO_TIPO = et2.id)  descripcion_estudio_tipo,
				(select ert.codigo from  CRIBADOCERVIX.estudio_resultado_tipo ert
                where e.ID_ESTUDIO_RESULTADO_TIPO = ert.ID) codigo_estudio_resultado_tipo,
				(select ert.descripcion from  CRIBADOCERVIX.estudio_resultado_tipo ert
                where e.ID_ESTUDIO_RESULTADO_TIPO = ert.ID) descrip_estudio_resultado_tipo,
                ( select descripcion
                    from CRIBADOCERVIX.tipo_estado_estudio
                    where codigo = (CRIBADOCERVIX.F_ULTIMO_ESTADO_ESTUDIO(e.id, e.id_estudio_tipo))
                ) descripcion_estado_estudio,
                ( select id
                    from CRIBADOCERVIX.tipo_estado_estudio
                    where codigo = (CRIBADOCERVIX.F_ULTIMO_ESTADO_ESTUDIO(e.id, e.id_estudio_tipo))
                ) id_estado_estudio
			FROM CRIBADOCERVIX.estudio e
			WHERE e.id_proceso = :id
			ORDER BY e.FECHA_INICIO desc
		"""

        def resultado = groovySql.rows(sentencia, [id: id_proceso])

        resultado.each { result->
            if (result.id_unidad_funcional){
                def descUnidFunc = FUENTESService.recuperarDescripcionUnidadFuncional(result.id_unidad_funcional)
                result.descripcion_unidad_funcional = descUnidFunc.descripcion
            }else {
                result.descripcion_unidad_funcional = ' '
            }
        }
        return resultado
    }

    def insertar(datos) {
        log.debug("Insertar en ESTUDIO: ${datos}")
        def resultado = [:]
        resultado.errores = [:]
        Map errores = [:]
        errores.error = ''
        
        /* Encapsulamos las acciones contra bbdd en una transaccion para hacer rollback 
        en caso de que alguna accion falle */
        //Recuperamos el valor del secuencial para idEstudio, que usaremos para insertar el nuevo Estudio y Estado
        def sentenciaSecuencial = """
				select CRIBADOCERVIX.S_ESTUDIO.nextval from dual
			"""
        def idEstudio = groovySql.firstRow(sentenciaSecuencial)
        datos.id_estudio = idEstudio[0]

        def sentencia = """
				insert into CRIBADOCERVIX.estudio (ID, ID_PROCESO, ID_ESTUDIO_TIPO, FECHA_INICIO,
                    ID_ESTUDIO_RESULTADO_TIPO, ID_UNIDAD_FUNCIONAL, PROFESIONAL, FECHA_RESULTADO )
				values (:id_estudio, :id_proceso, :id_estudio_tipo, :fecha_inicio, :id_estudio_resultado_tipo,
                    :id_unidad_funcional, :login, :fecha_resultado)
				"""
        resultado = baseService.insertarEntidad(sentencia, datos)

        //Insertamos Estado al Estudio. Dependiendo del Tipo, se inserta u Estado u otro
        // TO-DO Usar los CODIGOS, no los IDs
        if (datos.id_estudio_tipo == Constantes.ID_TIPO_ESTUDIO_VPH) { //Estudio VPH
            datos.id_cod_estado = Constantes.ID_ESTADO_PEND_INVIT
        }
        else if (datos.id_estudio_tipo == Constantes.ID_TIPO_ESTUDIO_CITOLOGIA) {  //Estudio Citología Líquida
            datos.id_cod_estado = Constantes.ID_ESTADO_PEND_CITA
        }
        else if (datos.id_estudio_tipo == Constantes.ID_TIPO_ESTUDIO_COTEST_1 || //Estudio tipo CoTest 1 y 2
                datos.id_estudio_tipo == Constantes.ID_TIPO_ESTUDIO_COTEST_2 ) {
            datos.id_cod_estado = Constantes.ID_ESTADO_PEND_PROX_INVIT
        }
        else if (datos.id_estudio_tipo == Constantes.ID_TIPO_ESTUDIO_GINECOLOGIA) {  //Estudio Ginecología
            datos.id_cod_estado = Constantes.ID_ESTADO_PEND_CITA
        }

        if (resultado.errores) {
            errores.error += "Error de insercción del Estudio. Repetir operación"

        }else {
            def sentenciaEstado = """
						insert into CRIBADOCERVIX.ESTUDIO_ESTADO (ID_ESTUDIO, CONTADOR, 
                        FEC_INICIO_ESTADO, FEC_FIN_ESTADO, ID_COD_ESTADO)
						values (:id_estudio, 1, SYSDATE, MAX_FECHA, :id_cod_estado)
					"""
            try{
                groovySql.execute(sentenciaEstado, datos)
            }catch(Exception e){
                log.error("Error al asignar el estado al estudio ", e)
                errores.error += "Error al asignar el estado del estudio. Repetir operación"
            }

        }
        if (errores.error) {
            resultado.errores = errores
        }

        return resultado
    }

    def modificar(datos) {
        log.debug("Modificar en ESTUDIO: ${datos}")

        if (datos.fecha_inicio) {
            datos.fecha_inicio = datos.fecha_inicio.toTimestamp()
        }
        if (datos.fecha_resultado) {
            datos.fecha_resultado = datos.fecha_resultado.toTimestamp()
        }

        def sentencia = """
			UPDATE CRIBADOCERVIX.ESTUDIO
			SET ID_PROCESO = :id_proceso, ID_ESTUDIO_TIPO = :id_estudio_tipo, 
				FECHA_INICIO = :fecha_inicio, ID_ESTUDIO_RESULTADO_TIPO = :id_estudio_resultado_tipo, 
				PROFESIONAL = :login, FECHA_RESULTADO = :fecha_resultado
			where id = :id_estudio
		"""
        def resultado = baseService.modificarEntidad(sentencia, datos)
        return resultado
    }

    def borrar(idEstudio) {
        log.debug("borrar EstudioEntidadService con ID: ${idEstudio}")

        Map resultado = [:]

        //Borramos primero el registro en la tabla ESTUDIO_ESTADO
        //Comprobamos permisos
        def esquema = env.getProperty("aplicacion.proyecto")
        def permisos = permisosEntidadService.recuperarPermisosEntidad(esquema, "ESTUDIO_ESTADO", null)
        permisos?.each { privilegio ->
            if (privilegio == "DELETE") {
                def sentenciaEstado = """
						delete from  CRIBADOCERVIX.ESTUDIO_ESTADO  
						where id_estudio = :id_estudio
					"""
                groovySql.execute(sentenciaEstado, [id_estudio: idEstudio])
            }
        }
        def sentencia = """
				delete from  CRIBADOCERVIX.estudio  
				where id = :id_estudio
			"""
        resultado =  baseService.borrarEntidad(sentencia, [id_estudio: idEstudio])

        return resultado
    }

    @Transactional(readOnly = true)
    def recuperarEstudioPorIdProceso(idProceso) {
        log.debug("Recuperar Estudios del Proceso con id: ${idProceso}")

        def sentencia = """
			select ID, ID_PROCESO, ID_ESTUDIO_TIPO, FECHA_INICIO, ID_ESTUDIO_RESULTADO_TIPO,
                ID_UNIDAD_FUNCIONAL, PROFESIONAL, FECHA_RESULTADO
			from  CRIBADOCERVIX.estudio
			where id_proceso = :id_proceso
			order by id asc
		"""
        return baseService.recuperarEntidad(sentencia, [id_proceso: idProceso])
    }

    /**
     * Recuperamos los datos del estudio anterior al enviado como parametro
     * @param datos de estudio actual
     * @return datos de estudio anterior
     */
    @Transactional(readOnly = true)
    def recuperarEstudioAnterior(datos) {
        log.debug("Recuperamos estudio anterior a id: ${datos.estudio_id}")

        def sentencia = """
			select *
            from (select ID, ID_PROCESO, ID_ESTUDIO_TIPO, FECHA_INICIO,
                    ID_ESTUDIO_RESULTADO_TIPO, ID_UNIDAD_FUNCIONAL, PROFESIONAL, FECHA_RESULTADO
			      from  CRIBADOCERVIX.estudio
			      where id_proceso = :estudio_id_proceso
		"""
        if (datos.estudio_id != null && datos.estudio_id != "" ) {
            sentencia = sentencia + " and id < :estudio_id"
        }
        sentencia = sentencia + " order by ID desc) where rownum = 1 "

        return groovySql.firstRow(sentencia, datos)
    }

    @Transactional(readOnly = true)
    def recuperarUltimoEstudioPorIdExpediente(idExpediente) {
        log.debug("Recuperamos estudio anterior a id: ${idExpediente}")

        def sentencia = """
			select max(ID) ID
			from  CRIBADOCERVIX.estudio
			where id_proceso in(select max(id)
                                from CRIBADOCERVIX.proceso
                                where id_expediente = :id_expediente)
		"""
        return groovySql.firstRow(sentencia, [id_expediente: idExpediente])
    }

    @Transactional(readOnly = true)
    def recuperarHistoricoEstadosEstudio(idEstudio) {
        log.debug("Recuperamos estados del estudio con id: ${idEstudio}")

        def sentencia = """
			select ee.ID_ESTUDIO, ee.CONTADOR, ee.FEC_INICIO_ESTADO, ee.FEC_FIN_ESTADO, tee.DESCRIPCION
			from CRIBADOCERVIX.estudio_estado ee, CRIBADOCERVIX.TIPO_ESTADO_ESTUDIO tee
			where ee.ID_COD_ESTADO = tee.id
			and ee.ID_ESTUDIO = :id_estudio
			order by ee.contador desc
		"""
        return groovySql.rows(sentencia, [id_estudio: idEstudio])
    }

    @Transactional(readOnly = true)
    def recuperarTipoEstudioPorIdEstudio(idEstudio) {
        def sentencia = """
			select id_estudio_tipo
			from CRIBADOCERVIX.estudio
			where id = :id_estudio
		"""
        return groovySql.firstRow(sentencia, [id_estudio: idEstudio])
    }


    def actualizarEstadoEstudio(idEstudio, idCodigoEstado) {

        //Recuperamos el ultimo estado del estudio
        def sentenciaUltimoEstado = """
			select max(contador )
			from CRIBADOCERVIX.ESTUDIO_ESTADO
			where ID_ESTUDIO = :id_estudio
		"""
        def contador = groovySql.firstRow(sentenciaUltimoEstado, [id_estudio: idEstudio])

        def sentenciaFinEstado = """
			UPDATE CRIBADOCERVIX.ESTUDIO_ESTADO
			SET FEC_FIN_ESTADO = sysdate
			where ID_ESTUDIO = :id_estudio
			and CONTADOR = :contador
		"""

        def sentenciaNuevoEstado = """
				INSERT INTO CRIBADOCERVIX.ESTUDIO_ESTADO (ID_ESTUDIO,CONTADOR,FEC_INICIO_ESTADO,
                    FEC_FIN_ESTADO,ID_COD_ESTADO)
				VALUES (:id_estudio, :contador, SYSDATE, MAX_FECHA, :id_cod_estado)
			"""

        groovySql.execute(sentenciaFinEstado, [contador: contador[0],id_estudio: idEstudio, id_cod_estado: idCodigoEstado])
        def nuevoContador = contador[0]+1
        groovySql.execute(sentenciaNuevoEstado, [id_estudio: idEstudio, contador: nuevoContador, id_cod_estado: idCodigoEstado])
    }

    /**
     * Recuperamos el ID del último estudio VPH que tenga resultado positivo
     * @param tipoEstudio
     * @param idProceso
     * @return
     */
    @Transactional(readOnly = true)
    def recuperarUltimoEstudioVPHPorIdProceso(idProceso){
        def sentencia = """
			select e.id, ID_ESTUDIO_TIPO
			from CRIBADOCERVIX.estudio e
			where ID_ESTUDIO_TIPO = :id_estudio_tipo
			and ID_PROCESO = :id_proceso
			and FECHA_INICIO = (select max(fecha_inicio)
                                from CRIBADOCERVIX.estudio
                                where ID_PROCESO = e.ID_PROCESO
                                and ID_ESTUDIO_TIPO = :id_estudio_tipo)
			and FECHA_RESULTADO <> MAX_FECHA
			and e.ID_ESTUDIO_RESULTADO_TIPO in (:id_resultado_tipo_1, :id_resultado_tipo_2)
		"""

        def resultado = groovySql.firstRow(sentencia, [id_proceso: idProceso,
            id_estudio_tipo: Constantes.ID_TIPO_ESTUDIO_VPH,
            id_resultado_tipo_1: Constantes.ID_RESULTADO_ESTUDIO_VPH_POS_1,
            id_resultado_tipo_2: Constantes.ID_RESULTADO_ESTUDIO_VPH_POS_2])

        return resultado
    }

    /**
     * Recuperamos último estudio con Resultado, sin contactos satisfactorios o con maximo insatisfactorios establecido
     * @param id Proceso, motivo Resultado VPH, motivo Resultado Cito, motivo Resultado CO-TEST
     * @return resultado
     */
    @Transactional(readOnly = true)
    def recuperarUltimoEstudioCerradoPorIdProceso(datos){
        Map parametros = [:]
        parametros.id_proceso = datos.ID
        parametros.id_documento_def_poslev =  FUENTESService.leerVariableGlobal(Constantes.DOC_CITO_POS_LEVE)
        parametros.id_documento_def_neg =  FUENTESService.leerVariableGlobal(Constantes.DOC_CITO_NEGATIVA)

        def sentencia = """
            SELECT EST.id, EST.ID_ESTUDIO_TIPO, EST.ID_ESTUDIO_RESULTADO_TIPO, EST_E.ID_COD_ESTADO
            FROM CRIBADOCERVIX.ESTUDIO EST,
            CRIBADOCERVIX.ESTUDIO_ESTADO EST_E
            WHERE EST.ID = EST_E.ID_ESTUDIO
            AND EST_E.CONTADOR = (SELECT MAX(EE.CONTADOR) FROM CRIBADOCERVIX.ESTUDIO_ESTADO ee WHERE EE.ID_ESTUDIO = EST.ID)
            AND EST.ID = (SELECT MAX(E.ID)
                            FROM CRIBADOCERVIX.ESTUDIO E
                            WHERE E.ID_PROCESO = :id_proceso
                            and E.FECHA_RESULTADO <> max_fecha)
            AND ( 0 = (SELECT count(ID_ESTUDIO)
                        FROM CRIBADOCERVIX.REGISTRO_CONTACTO
                        WHERE ID_ESTUDIO = EST.ID
                        and ID_MOTIVO_CONTACTO in (select id 
                                                    from FUENTES.motivo_contacto 
                                                    where codigo in(:motivo_contacto_vph, :motivo_contacto_cito, :motivo_contacto_cotest)
                                                   )
                        AND REGISTRO_CONTACTO.CONTACTADO = 1
                        )
                 AND EST.ID NOT IN (SELECT ID_ESTUDIO
                                    FROM CRIBADOCERVIX.REGISTRO_CONTACTO
                                    WHERE ID_ESTUDIO = EST.ID
                                    AND ID_MOTIVO_CONTACTO in (select id 
                                                            from FUENTES.motivo_contacto 
                                                            where codigo in(:motivo_contacto_vph, :motivo_contacto_cito, :motivo_contacto_cotest))
                                    AND CONTACTADO = 0
                                    group by id_estudio
                                    having count(id_estudio) = :num_max_contactos
                                    )
                                                            
            )
            AND (EST.ID NOT IN (SELECT DOC.ID_ESTUDIO
                                FROM CRIBADOCERVIX.DOCUMENTO DOC
                                WHERE DOC.ID = (SELECT ID_DOCUMENTO
                                                FROM CRIBADOCERVIX.DOCUMENTO_INDIVIDUAL
                                                WHERE (ID_DOCUMENTO_DEF = :id_documento_def_poslev OR ID_DOCUMENTO_DEF = :id_documento_def_neg)
                                                AND ID_DOCUMENTO = DOC.ID)
                                AND DOC.ID_ESTUDIO = EST.ID AND EST.ID_ESTUDIO_TIPO = :id_estudio_cito))
        """
        parametros.num_max_contactos = Constantes.NUM_MAX_CONTACTOS
        parametros.motivo_contacto_vph = Constantes.MOTIVO_CON_RESULT_VPH
        parametros.motivo_contacto_cito = Constantes.MOTIVO_CON_RESULT_CITO
        parametros.motivo_contacto_cotest = Constantes.MOTIVO_CON_RESULT_COTEST
        //Se incluye el id de cito para asegurar que los estudios que tengan comunicaciones de cito negativa o positiva leve sean estudios de citologia
        //en la ultima condicion de la sentencia
        parametros.id_estudio_cito = Constantes.ID_TIPO_ESTUDIO_CITOLOGIA

        
        def resultado = groovySql.firstRow(sentencia, parametros)
    
        return resultado
    }

    @Transactional(readOnly = true)
    def recuperarUltimoEstudioCitoPorIdProceso(idProceso) {
        def sentencia = """
			select e.id
			from CRIBADOCERVIX.estudio e
			where ID_ESTUDIO_TIPO = :id_estudio_tipo
			and ID_PROCESO = :id_proceso
			and FECHA_INICIO = (select max(fecha_inicio) 
                                from CRIBADOCERVIX.estudio 
                                where ID_PROCESO = :id_proceso 
                                and ID_ESTUDIO_TIPO = :id_estudio_tipo)
			and FECHA_RESULTADO <> MAX_FECHA
			and e.ID_ESTUDIO_RESULTADO_TIPO in (:id_resultado_tipo_1, :id_resultado_tipo_2)
		"""

        def resultado = groovySql.firstRow(sentencia, [id_proceso: idProceso,
             id_estudio_tipo: Constantes.ID_TIPO_ESTUDIO_CITOLOGIA,
             id_resultado_tipo_1: Constantes.ID_RESULTADO_ESTUDIO_CITO_POS_LEVE,
             id_resultado_tipo_2: Constantes.ID_RESULTADO_ESTUDIO_CITO_POS_GRAVE])
        return resultado
    }

    @Transactional(readOnly = true)
    def recuperarEstudioPorIdMuestra(idMuestra){
        def sentencia = """SELECT E.ID, E.ID_ESTUDIO_TIPO 
                FROM CRIBADOCERVIX.ESTUDIO e 
                LEFT JOIN CRIBADOCERVIX.PRUEBA p ON p.ID_ESTUDIO = e.ID 
                LEFT JOIN CRIBADOCERVIX.PRUEBA_DETALLE pd ON pd.ID_PRUEBA = p.ID 
                WHERE pd.ID_MUESTRA = :id_muestra
        """
        def resultado = groovySql.firstRow(sentencia, [id_muestra:idMuestra])
        return resultado
    }

    def recuperarDatosControlCostest(datos){

        datos.limiteReg = Constantes.LIMITE_CONSULTAS

        if(datos.fecha_desde){
            datos.fecha_desde = datos.fecha_desde.toTimestamp()
        }
        if(datos.fecha_hasta){
            datos.fecha_hasta = datos.fecha_hasta.toTimestamp()
        }

        def sentencia ="""SELECT FECHA_ACTIVACION, ID_EXPEDIENTE, PACIENTE, CONTACTO, FECHA_PRUEBA, PETICION_ENVIADA, RESULTADO_COTEST, CENTRO_SALUD, PROFESIONAL
            FROM CRIBADOCERVIX.V_CONTROL_COTEST
            WHERE FECHA_ACTIVACION >= :fecha_desde AND FECHA_ACTIVACION <= :fecha_hasta"""

        if(datos.cod_cs){
            sentencia += """ AND COD_CS = :cod_cs"""
        }
        if(datos.id_profesional){
            sentencia += """ AND ID_PROFESIONAL = :id_profesional"""
        }
        sentencia += """ AND ROWNUM <= :limiteReg"""

        def resultado = groovySql.rows(sentencia, datos)
        return resultado
    }

}
