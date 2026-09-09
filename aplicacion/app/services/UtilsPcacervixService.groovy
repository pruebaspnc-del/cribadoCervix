package  Cribadocervix

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

import com.lowagie.text.Document
import com.lowagie.text.pdf.PdfCopy
import com.lowagie.text.pdf.PdfReader

import base.BaseService
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.springframework.transaction.annotation.Transactional

@Slf4j
@Transactional
@Service
class UtilsCribadocervixService {

	@Autowired
	Sql groovySql

	@Autowired
	Environment env
    
    @Autowired
    BaseService baseService

    int getMaxRegistros(){
        try{
            return baseService.leerParametro('numero_maximo_resultados_busqueda').toInteger() 
        } catch(Exception e){
            return 1000
        }
    }
	
	class ListLimitExcedeedException extends Exception{
		int value
		ListLimitExcedeedException(String mensaje, int resultadoCuenta = 0) {
			super(mensaje)
			this.value = resultadoCuenta
		}
	
		int getValue() {
			return value
		}
	}
	
	//TODO: El sitio de este método sería FUENTES SERVICE
    @Transactional(readOnly = true)
	def recuperarDescripcionSexoPorCodigo( codigoSexo) {
		def resultado
		
		def sentencia = """
			select codigo, descripcion
			from FUENTES.sexo
			Where codigo = :codigoSexo
		"""
		resultado = groovySql.firstRow(sentencia, [codigoSexo: codigoSexo])
		def descripcion = resultado.descripcion
		
		return descripcion
	}
    
    def cumpleRequisitosCervix(id_persona) {
        def sentencia = "SELECT Cribadocervix.F_CUMPLE_REQ_PARTICIPACION(:id, SYSDATE) cumple_requisitos FROM DUAL"
        def resultado = groovySql.firstRow(sentencia, [id: id_persona])
        return resultado
        
    }
	
	//TODO: El sitio de este método sería PersanService
    @Transactional(readOnly = true)
	def recuperarMediosComunicacionPersonaConFecActualizada (idPersona) {
		def resultado
		
		def sentencia = """
			SELECT PMC.ID_PERSONA,
			(SELECT orden FROM FUENTES.MEDIO_COMUNICACION WHERE CODIGO = PMC.CODIGO_MEDIO_COMUNICACION) ORDEN,
			PMC.CONTADOR,
			PMC.CODIGO_MEDIO_COMUNICACION,
			(SELECT DESCRIPCION FROM FUENTES.MEDIO_COMUNICACION MEC WHERE MEC.ambito_profesional in (0, 1) AND CODIGO = PMC.CODIGO_MEDIO_COMUNICACION) DESCRIPCION_MEDIO_COMUN,
			PMC.VALOR,
			(SELECT TO_CHAR(fecha_actualizacion, 'dd/mm/yyyy') || ' ' || login 
			FROM PERSONA.PERSONA__MEDIO_COMUNICACION_a MCA 
			WHERE MCA.ID_PERSONA = PMC.ID_PERSONA 
			AND MCA.contador = PMC.contador 
			AND MCA.contador_a = (SELECT MAX(contador_a) 
			                    FROM PERSONA.PERSONA__MEDIO_COMUNICACION_a PMCA2 
			                    WHERE PMCA2.ID_PERSONA = PMC.ID_PERSONA 
			                    AND PMCA2.contador = PMC.contador ) ) FEC_ACTUALIZADA
			from PERSONA.PERSONA__MEDIO_COMUNICACION PMC
			where PMC.CODIGO_MEDIO_COMUNICACION IN (SELECT CODIGO 
			    			FROM FUENTES.MEDIO_COMUNICACION WHERE AMBITO_PROFESIONAL in (0, 1))
			AND ID_PERSONA = :idPersona
			order by orden asc
 
		"""
		 
		
		return resultado = groovySql.rows(sentencia, [idPersona: idPersona])
	}

	/*
	 * TODO : Este metodo debería incorporarse a FUENTESService
	 */
    @Transactional(readOnly = true)
	def recuperarZonasSaludPorIDPersona(idPersona) {
		def resultado
		def sentencia = """
			FUENTES.F_Nombre_ZonaSalud_Persona(:id_persona, sysdate)

		"""
		return resultado = groovySql.rows(sentencia, [id_persona: idPersona])
	}

    @Transactional(readOnly = true)
	def recuperarZonasBasicasSalud() {
		def resultado		
		def sentencia = """
						SELECT ZONAB.ID, ZONAB.CODIGO, ZONAB.DESCRIPCION
						FROM FUENTES.ENTIDAD ZONAB
						WHERE ZONAB.ID_TIPO = 43 AND ZONAB.FECHA_BAJA = MAX_FECHA
						AND ZONAB.ID_PADRE IN (SELECT ID FROM FUENTES.ENTIDAD WHERE ID_TIPO = 22)
						ORDER BY ZONAB.DESCRIPCION ASC
						"""
		resultado = groovySql.rows(sentencia)
		return resultado	
	}

    @Transactional(readOnly = true)
	def recuperarAreasSalud() {
		def resultado		
		def sentencia = """
						SELECT AREA.ID, AREA.CODIGO, CONCAT(CONCAT(AREA.CODIGO,'-'), AREA.DESCRIPCION) DESCRIPCION
						FROM FUENTES.ENTIDAD AREA
						WHERE AREA.ID_TIPO = 22 AND AREA.FECHA_BAJA = MAX_FECHA
						AND AREA.ID_PADRE IN (SELECT ID FROM FUENTES.ENTIDAD WHERE ID_TIPO = 56)
						ORDER BY AREA.CODIGO ASC
						"""
		resultado = groovySql.rows(sentencia)
		return resultado
	}

    @Transactional(readOnly = true)
	def recuperarZonasPorId(datos) {
		def sentencia = """
						SELECT ZONAB.ID, ZONAB.CODIGO, ZONAB.DESCRIPCION
						FROM FUENTES.ENTIDAD ZONAB
						WHERE ZONAB.ID_TIPO ="""+ Constantes.ID_ZONAB.toString() + """ AND ZONAB.FECHA_BAJA = MAX_FECHA
						AND ZONAB.ID = :id
						ORDER BY ZONAB.DESCRIPCION ASC
						"""
		log.debug(sentencia)
		def resultado = groovySql.firstRow(sentencia,datos)
		return resultado
	}

    @Transactional(readOnly = true)
	def recuperarZonasIdArea(id) {
		def sentencia = """
						SELECT ZONAB.ID, ZONAB.CODIGO, ZONAB.DESCRIPCION
						FROM FUENTES.ENTIDAD ZONAB
						WHERE ZONAB.ID_TIPO = 43 AND ZONAB.FECHA_BAJA = MAX_FECHA
						AND ZONAB.ID_PADRE = """ + id + """
						ORDER BY ZONAB.DESCRIPCION ASC
						"""
		log.debug(sentencia)
		def resultado = groovySql.rows(sentencia)
		return resultado
	}

    @Transactional(readOnly = true)
	def recuperarUnidadFuncional(id) {
		def resultado
		def sentencia = "SELECT UF.DESCRIPCION, UF.CODIGO, UF.ID FROM FUENTES.UNIDAD_FUNCIONAL UF WHERE UF.ID = " + id
		resultado = groovySql.firstRow(sentencia)
		return resultado
	}
/**
 * Recupera datos de Unidad Funcional
 * @param codigo
 * @return id, codigo, descripcion
 */
    @Transactional(readOnly = true)
	def recuperarUnidadFuncionalPorCodigo(String codigo) {
		def sentencia = "SELECT UF.DESCRIPCION, UF.CODIGO, UF.ID FROM FUENTES.UNIDAD_FUNCIONAL UF WHERE UF.CODIGO = :codigo"
		def resultado = groovySql.firstRow(sentencia, [codigo: codigo])
		return resultado
	}

	/**
	 *
	 * @param datos id o login
	 */
    @Transactional(readOnly = true)
	def recuperarDatosProfesional(datos){
		def sentencia = """
			select id_profesional, login from PERSONA.PROFESIONAL_INFORMATICO 
			where 
		"""
		if(datos.login){
			sentencia += " login=:login"
		}
		if(datos.id_profesional){
			sentencia += " id_profesional=:id_profesional"
		}

		def resultado = groovySql.firstRow(sentencia, datos)
		return resultado
	}
    /**
     * Recupera los MOTIVOS de contacto en FUENTES
     * @return motivos de contacto
     */
    @Transactional(readOnly = true)
	def recuperarMotivosContactoPorProyecto(datos) {
        def sentencia = """
            SELECT ID,CODIGO,DESCRIPCION
            FROM FUENTES.MOTIVO_CONTACTO
            WHERE id in (SELECT ID_MOTIVO_CONTACTO 
                         FROM FUENTES.PROYECTO__MOTIVO_CONTACTO
                         WHERE ID_PROYECTO = :id_proyecto)
            ORDER BY descripcion asc
        """
        def resultado = groovySql.rows(sentencia, datos)
		return resultado
    } 
    
    /**
     * Recupera MOTIVO_CONTACTO en FUENTES por ID
     * @return motivo de contacto
     */
    @Transactional(readOnly = true)
    def recuperarMotivoContactoPorId(idMotivo) {
        def sentencia = """
            SELECT ID,CODIGO,DESCRIPCION
            FROM FUENTES.MOTIVO_CONTACTO
            WHERE ID = :id_motivo
        """
        def resultado = groovySql.firstRow(sentencia, [id_motivo: idMotivo])
		return resultado
    }
    
    /**
     * Recupera MOTIVO_CONTACTO en FUENTES
     * @return motivos de contacto
     */
    @Transactional(readOnly = true)
    def recuperarMotivoContactoPorCodigo(codigoMotivo) {
        def sentencia = """
            SELECT ID,CODIGO,DESCRIPCION
            FROM FUENTES.MOTIVO_CONTACTO
            WHERE CODIGO = :codigo_motivo
        """
        def resultado = groovySql.firstRow(sentencia, [codigo_motivo: codigoMotivo])
		return resultado
    }

    /**
     * Recuperación de plantillas de cartas, distintas a Carta de Invitacion
     * @return
     */
    @Transactional(readOnly = true)
	def recuperarPlantillas(datos) {
		def resultado
		def sentencia = """ 
            SELECT D.ID, D.CODIGO, D.DESCRIPCION 
            FROM FUENTES.DOCUMENTO D 
            WHERE D.ID_PROYECTO = (SELECT P.ID 
                                   FROM FUENTES.PROYECTO P 
                                   WHERE P.ID = :id_proyecto) 
            AND D.CODIGO <> :codigo_doc

        """
		resultado = groovySql.rows(sentencia, datos)
		return resultado		
	}

    @Transactional(readOnly = true)
    def recuperarPlantillaPorId(idDocumento) {
        def resultado
        def sentencia = """
            SELECT D.ID, D.CODIGO, D.DESCRIPCION 
            FROM FUENTES.DOCUMENTO D 
            WHERE ID = :id_doc
        """
        resultado = groovySql.firstRow(sentencia, [id_doc: idDocumento])
        return resultado
    }

    @Transactional(readOnly = true)
	def recuperarVariableGLobalPorNombre(nombre_variable){
		def resultado
		def sentencia = "SELECT NOMBRE_VARIABLE, VALOR, COMENTARIO FROM Cribadocervix.VARIABLE_GLOBAL WHERE NOMBRE_VARIABLE = :nombre "
		resultado = groovySql.firstRow(sentencia, [nombre: nombre_variable])
		return resultado
	}
    
    def combinePDFs(pdfList) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()

        Document combinedDocument = new Document()
        PdfCopy pdfCopy = new PdfCopy(combinedDocument, byteArrayOutputStream)
        combinedDocument.open()

        pdfList.each { pdfBytes ->
            PdfReader reader = new PdfReader(pdfBytes)
            int totalPages = reader.getNumberOfPages()
            (1..totalPages).each { page ->
                def pagePdf = pdfCopy.getImportedPage(reader, page)
                pdfCopy.addPage(pagePdf)
            }
            reader.close()
        }

        combinedDocument.close()
        return byteArrayOutputStream.toByteArray()
    }

	/**
	 * Recuperamos los motivos de devolucion de la tabla FUENTES.DEVOLUCION_TIPO
	 */
    @Transactional(readOnly = true)
	def recuperarDevolucionesTipo() {
		def sentencia = """
			SELECT
				codigo,
				descripcion
			FROM
				FUENTES.DEVOLUCION_TIPO
			ORDER BY
				codigo ASC
		"""
		def resultado = groovySql.rows(sentencia)
		return resultado
	}

	/**
	 * Se recuperan los diferentes tipos de documentos que existen para la gestion de cartas
	 * @return
	 */
    @Transactional(readOnly = true)
	def recuperarTipoDocumento() {
		def codProyecto = Constantes.ID_PROYECTO
		def sentencia = """
			SELECT
				DOC.ID,
				DOC.DESCRIPCION
			FROM
				FUENTES.DOCUMENTO DOC, FUENTES.DOCUMENTO__PLANTILLA DOCP, FUENTES.PLANTILLA PL
			WHERE
				DOC.ID_PROYECTO = :codProyecto
				AND DOC.FECHA_BAJA > sysdate
				AND DOC.ID = DOCP.ID_DOCUMENTO
				AND DOCP.CODIGO_PLANTILLA = PL.CODIGO
				AND PL.EXTENSION IN ('odt', 'doc', 'xml')
				AND UPPER(DOC.DESCRIPCION) NOT LIKE '%LISTADO%'
			ORDER BY DOC.DESCRIPCION ASC
		"""

		def resultado = groovySql.rows(sentencia, [codProyecto :codProyecto])

		return resultado
	}

	/**
	 * Recupera la descripcion de los documentos del proyecto que sean XML, y que no esten de baja
	 * @return
	 */
    @Transactional(readOnly = true)
	def recuperarTiposDocumentoXML() {
		def id_proyecto = Constantes.ID_PROYECTO
		def sentencia = """
			SELECT D.DESCRIPCION, D.ID
			FROM FUENTES.DOCUMENTO D,
			FUENTES.DOCUMENTO__PLANTILLA DP
			WHERE D.ID_PROYECTO = :id_proyecto
			AND D.FECHA_BAJA = MAX_FECHA 
			AND  D.ID = DP.ID_DOCUMENTO
			AND D.PERMITE_CREACION_INDIVIDUAL = 1
			AND DP.CODIGO_PLANTILLA = 5
		"""

		def resultado = groovySql.rows(sentencia, [id_proyecto: id_proyecto])
		return resultado
	}

	/**
	 * Rellena los parametros habituales de las plantillas con Jasper Report
	 */
	static def parametrosPlantillaJasper(rutaJasperSession) {
		def parametrosReport = [:]
		parametrosReport.ruta_jasper = rutaJasperSession
		parametrosReport.Encabezado_Imagen = rutaJasperSession + Constantes.ENCABEZADO
		parametrosReport.Pie_Imagen = rutaJasperSession + Constantes.PIE
		parametrosReport.QR_imagen = rutaJasperSession + Constantes.QR_IMAGEN
		parametrosReport.QR_encuesta_autotoma = rutaJasperSession + Constantes.QR_ENCUESTA_AUTOTOMA
		return parametrosReport
	}

    /**
     * Recupera el codigo de centro de salud asociado a un profesional
     * @param idProfesional
     * @return
     */
    def recuperarCodCSPorIdProfesional(idProfesional) {
        def sentencia = """
            SELECT id_entidad
            FROM FUENTES.puesto__profesional pp, FUENTES.entidad__puesto ep
            WHERE pp.id_profesional= :idProfesional AND pp.fecha_baja=MAX_FECHA
                AND pp.id_puesto= ep.id_puesto AND ep.fecha_baja= MAX_FECHA
        """
        def resultado = groovySql.firstRow(sentencia, [idProfesional: idProfesional])

        if (resultado) {
            def idEntidad = resultado.id_entidad
            sentencia = """SELECT FUENTES.f_codigo_entidad(:idEntidad, sysdate) AS cod_cs FROM DUAL"""
            def resultCs = groovySql.firstRow(sentencia, [idEntidad: idEntidad])
            resultado.cod_cs = resultCs.cod_cs
        }

        return resultado

    }

    /**
     * Recupera el id, codigo y descripcion de los centros de salud asociados a los
     * profesionales con perfil matrona y el id_profesional y nombre de estos
     * @return
     */
    def recuperaCodCsAsociadosPerfilMatronas(){
        String perfil_matrona = Constantes.SANITARIO_ASISTENCIAL
        def sentencia ="""SELECT DISTINCT (e.ID) as ID, e.DESCRIPCION, 
            e.CODIGO 
            FROM FUENTES.ENTIDAD e
            JOIN FUENTES.ENTIDAD__PUESTO ep ON e.ID = ep.ID_ENTIDAD 
            JOIN FUENTES.PUESTO__PROFESIONAL pp ON ep.ID_PUESTO = pp.ID_PUESTO
            JOIN FUENTES.PERFIL__PROFESIONAL pp2 ON PP.ID_PROFESIONAL = PP2.ID_PROFESIONAL 
            WHERE e.CODIGO = (SELECT FUENTES.f_codigo_entidad(ep.ID_ENTIDAD, sysdate) AS cod_cs FROM DUAL)
            AND ep.FECHA_BAJA = MAX_FECHA
            AND e.FECHA_BAJA = MAX_FECHA
            AND PP2.ID_PERFIL = (SELECT ID 
                                        FROM FUENTES.PERFIL p 
                                        WHERE p.DESCRIPCION = :perfil_matrona)"""

        def resultado = groovySql.rows(sentencia, [perfil_matrona: perfil_matrona])
        return resultado
    }

    /**
     * Recupera el id, codigo y descripcion de los centros de salud asociados
     * a un area de salud
     * @return
     */
    def recuperaCodCsAsociadosAreaSalud(idAreaSalud){

        def sentencia ="""SELECT DISTINCT (eCs.ID) as ID, eCs.DESCRIPCION, 
            eCs.CODIGO 
            FROM FUENTES.ENTIDAD eCs
            LEFT JOIN FUENTES.ENTIDAD__PUESTO ep ON eCs.ID = ep.ID_ENTIDAD 
            LEFT JOIN FUENTES.ENTIDAD eZbs on eCs.ID_PADRE = eZbs.ID
            LEFT JOIN FUENTES.ENTIDAD eAs on eZbs.ID_PADRE = eAs.ID
            WHERE eZbs.FECHA_BAJA = MAX_FECHA
            AND eAs.FECHA_BAJA = MAX_FECHA
            AND eCs.FECHA_BAJA = MAX_FECHA
            AND eCs.CODIGO = (SELECT FUENTES.f_codigo_entidad(ep.ID_ENTIDAD, sysdate) AS cod_cs FROM DUAL)
            AND eAs.ID = :idAreaSalud
            """

        def resultado = groovySql.rows(sentencia, [idAreaSalud: idAreaSalud])
        return resultado
    }

    /**
     * Recupera el id de persona y el nombre de los
     * profesionales con perfil matrona asociados a un centro de salud. Si no
     * se pasa el parametro de id Centro de salud, recupera todos los profesionales con perfil
     * matrona asociados que tengan asociado un centro de salud a su puesto
     *  @param idCs
     * @return
     */
    def recuperaMatronaPorIdCentroSalud(idCs){
        String perfil_matrona = Constantes.SANITARIO_ASISTENCIAL
        def resultado

        def sentencia = """SELECT P.ID, p.NOMBRE||' '|| p.APELLIDO1||' '|| p.APELLIDO2 AS NOMBRE
                                FROM PERSONA.PERSONA p 
                                LEFT JOIN FUENTES.PUESTO__PROFESIONAL pp ON pp.ID_PROFESIONAL = p.ID
                                LEFT JOIN FUENTES.PERFIL__PROFESIONAL pp2 ON PP.ID_PROFESIONAL = PP2.ID_PROFESIONAL 
                                LEFT JOIN FUENTES.ENTIDAD__PUESTO ep ON ep.ID_PUESTO = pp.ID_PUESTO
                                LEFT JOIN FUENTES.ENTIDAD e ON e.ID = ep.ID_ENTIDAD 
                                WHERE pp.FECHA_BAJA = MAX_FECHA
                                AND e.FECHA_BAJA = MAX_FECHA 
                                AND ep.FECHA_BAJA = MAX_FECHA
                                AND PP2.ID_PERFIL = (SELECT ID 
                                        FROM FUENTES.PERFIL p 
                                        WHERE p.DESCRIPCION = :perfil_matrona)"""

        if(idCs){
            sentencia += """ AND e.CODIGO = (SELECT FUENTES.f_codigo_entidad(:id_entidad, sysdate) AS cod_cs FROM DUAL)"""
            resultado = groovySql.rows(sentencia, [perfil_matrona: perfil_matrona, id_entidad: idCs])
        }else{
            resultado = groovySql.rows(sentencia, [perfil_matrona: perfil_matrona])
        }

        return resultado
    }

    /**
     * Recupera las descripciones de los resultados VPH positivos
     * @return
     */
    def recuperaResultadoTipoVphPositivo(){
        int idVphPosAlto = Constantes.ID_RESULTADO_ESTUDIO_VPH_POS_1
        int  idVphPosMuyAlto = Constantes.ID_RESULTADO_ESTUDIO_VPH_POS_2
        def resultado

        def sentencia = """
        SELECT ID, CODIGO, DESCRIPCION 
        FROM Cribadocervix.ESTUDIO_RESULTADO_TIPO ert
        WHERE ID IN (:idVphPosAlto,:idVphPosMuyAlto)
        """
        resultado = groovySql.rows(sentencia, [idVphPosAlto:idVphPosAlto,idVphPosMuyAlto:idVphPosMuyAlto])

        return resultado
    }


}
