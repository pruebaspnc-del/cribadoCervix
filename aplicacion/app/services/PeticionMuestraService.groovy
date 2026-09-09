package  Cribadocervix

import base.RESTClient
import base.security.jwt.JwtSanidadService
import  Cribadocervix.entidad.EstudioEntidadService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException

import base.BaseService
import base.SslUtilsService
import  jade.IntereaService
import  jade.FUENTESService
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.zkoss.zk.ui.Sessions
import wslite.json.JSONObject

@Slf4j
@Transactional
@Service
class PeticionMuestraService {

	// Servicios comunes
	@Autowired
	BaseService baseService

	@Autowired
	FUENTESService FUENTESService

	@Autowired
	SslUtilsService sslUtilsService

	@Autowired
	IntereaService intereaService

	@Autowired
	ExpedienteService expedienteService

    @Autowired
    EstudioEntidadService estudioEntidadService


	//@Autowired
	//RestTemplate restTemplate;

	// Bean que nos permite hacer consultas SQL directas a la base de datos
	@Autowired
	Sql groovySql

	@Autowired
	JwtSanidadService jwtSanidadService



	static int PENDIENTE_DE_ENVIO = 1
	static int PROCESADO_OK_CON_WS = 2
	static int ERROR_ENVIO = 3
	static int PROCESADO_POR_CANCELACION = 4
	static int PROCESADO_OK_SIN_WS = 5
	static int PROCESANDO = 6
	static int CADUCADO = 7

	static int CREAR = 1
	static int CANCELAR = 2

	static int DESTINO_VPH = 1
	static int DESTINO_CITO_LIQ = 2

	static String RESPUESTA_OK = "<COD_RESPUESTA>OK</COD_RESPUESTA>"

	static String DIAS_CADUCIDAD_PRUEBA = "gnDiasCaducidadPrueba"

	static String PRIORIDAD_NORMAL = "Normal"

	static String ACCION_CREAR = "CREAR"
	static String ACCION_CANCELAR = "CANCELAR"

	/**
	 * Procediento principal llamado por Job de gestión de muestras
	 * @return
	 */
	def procesarMuestrasJob(){
		procesarMuestrasCancelacion()
		procesarMuestrasCreacion()
	}

	/**
	 * Procediento principal que gestiona las muestras a cancelar
	 * @return
	 */
	def procesarMuestrasCancelacion(){

		def sentencia = """
			SELECT ID,ID_ACCION FROM Cribadocervix.PETICION_MUESTRA WHERE ID_ACCION = ${CANCELAR} AND ID_ESTADO_PETIC IN (${PENDIENTE_DE_ENVIO},${ERROR_ENVIO})             
            ORDER BY FEC_CREACION ASC
		"""

		def muestras = groovySql.rows(sentencia, [:])

		muestras.each { r ->
			procesarMuestra(recuperarMuestra(r.ID, r.ID_ACCION))
		}


	}

	/**
	 * Procediento principal que gestiona las muestras a crear
	 * @return
	 */
	def procesarMuestrasCreacion(){

		def sentencia = """
			SELECT ID,ID_ACCION FROM Cribadocervix.PETICION_MUESTRA WHERE ID_ACCION = ${CREAR} AND ID_ESTADO_PETIC IN (${PENDIENTE_DE_ENVIO},${ERROR_ENVIO}) 
			ORDER BY FEC_CREACION ASC
		"""

		def muestras = groovySql.rows(sentencia, [:])

		muestras.each { r ->
			procesarMuestra(recuperarMuestra(r.ID, r.ID_ACCION))
		}

	}


	/**
	 * Gestión de una muestra de cancelación
	 * @param muestra
	 * @return
	 */
	def procesarMuestra(muestra){

		try {
			def realizarLlamadaWS = false;

			if (muestra.ID_ESTADO_PETIC != PROCESANDO) {

				log.debug("Inicio procesado de muestra: ${muestra.ID}-${muestra.ID_ACCION}")
				iniciarProcesadoMuestra(muestra.ID,muestra.ID_ACCION)

				if (muestra.ID_ACCION == CANCELAR) { // Si la muestra es de cancelación hay que tratar la muestra de creación asociada
					def muestraCreacion = recuperarMuestra(muestra.ID,CREAR)

					if (muestraCreacion) {
						if (muestraCreacion.ID_ESTADO_PETIC == PENDIENTE_DE_ENVIO || muestraCreacion.ID_ESTADO_PETIC == ERROR_ENVIO) {

							muestraCreacion.ID_ESTADO_PETIC = PROCESADO_POR_CANCELACION
							muestraCreacion.ERROR = ""
							muestra.ID_ESTADO_PETIC = PROCESADO_OK_SIN_WS
							muestra.ERROR = ""

							actualizarMuestra(muestraCreacion)
							actualizarMuestra(muestra)

						} else if (muestraCreacion.ID_ESTADO_PETIC == PROCESADO_OK_CON_WS || muestraCreacion.ID_ESTADO_PETIC == PROCESADO_POR_CANCELACION) {
							realizarLlamadaWS = true;
						}
					} else { // No hay muestra de creación
						realizarLlamadaWS = true;
					}
				} else { // La muestra no es de cancelación
					realizarLlamadaWS = true;
				}

				if (realizarLlamadaWS) {

					// LLamada WS
					def respuestaSW = llamadaSW(muestra)

					if (respuestaSW.correcta) {
						muestra.ID_ESTADO_PETIC = PROCESADO_OK_CON_WS
						muestra.ERROR = ""
					} else {
						BigDecimal dcp = new BigDecimal(FUENTESService.leerVariableGlobal(DIAS_CADUCIDAD_PRUEBA))
						if (muestra.DIAS_PETICION >= dcp) { // Si se supera los DIAS_CADUCIDAD_PRUEBA dias la peticion de muestra caduca si es erronea
							muestra.ID_ESTADO_PETIC = CADUCADO
						} else {
							muestra.ID_ESTADO_PETIC = ERROR_ENVIO
						}
						muestra.ERROR = respuestaSW.error
					}

					actualizarMuestra(muestra)
				}

			} else {
				log.debug("Muestra procesandose: ${muestra.ID}-${muestra.ID_ACCION}")
			}
		} catch (Exception ex) {
			log.error("Error no controlado: ${muestra.ID}-${muestra.ID_ACCION} --> ${ex.message}" )
		}
	}



	/**
	 * Obtención de registro de muestra de BBDD
	 * @param idMuestra
	 * @param idAccion
	 * @return
	 */
	@Transactional
	def recuperarMuestra(idMuestra,idAccion){
		def resultado = groovySql.firstRow("SELECT ID,FEC_PETICION,ID_ESTADO_PETIC,ID_ACCION,ERROR,FEC_CREACION,COD_PRODUCTO,SISTEMA_DESTINO, FLOOR(SYSDATE - FEC_CREACION)  AS DIAS_PETICION, COD_CENTRO_PETICIONARIO FROM Cribadocervix.PETICION_MUESTRA WHERE ID = :id AND ID_ACCION = :idAccion", [id: idMuestra, idAccion: idAccion])
		return resultado
	}

	/**
	 * Bloqueo en BBDD de registro de muestra tratado
	 * @param idMuestra
	 * @return
	 */
	@Transactional
	def iniciarProcesadoMuestra(idMuestra,idAccion){
		groovySql.execute("UPDATE Cribadocervix.PETICION_MUESTRA SET FEC_PETICION = SYSDATE, ID_ESTADO_PETIC = ${PROCESANDO} WHERE ID = :id AND ID_ACCION = :idAccion", [id: idMuestra, idAccion: idAccion])
	}

	/**
	 * Actualización en BBDD de registro de muestra tratado
	 * @param registroMuestra
	 * @return
	 */
	@Transactional
	def actualizarMuestra(registroMuestra){
		groovySql.execute("UPDATE Cribadocervix.PETICION_MUESTRA SET FEC_PETICION = SYSDATE, ID_ESTADO_PETIC = :idEstado, ERROR = SUBSTR(:error,1,4000) WHERE ID = :id AND ID_ACCION = :idAccion",
			[idEstado:registroMuestra.ID_ESTADO_PETIC,error:registroMuestra.ERROR,id: registroMuestra.ID, idAccion:registroMuestra.ID_ACCION])
	}


	/**
	 * Obtención de registro con datos de la persona asociada a la muestra de muestra de BBDD
	 * @param idMuestra
	 * @param idAccion
	 * @return
	 */
	@Transactional
	def recuperarPersonaMuestra(idMuestra,idAccion){
		// 03/04/2024; jms30x; Issue #37. Todos los campos deben ser texto!!
		def resultado = groovySql.firstRow(
			"""
			SELECT P.NOMBRE, P.APELLIDO1, P.APELLIDO2, TO_CHAR(P.FECHA_NACIMIENTO,'YYYYMMDD') AS FECHA_NACIMIENTO, DECODE(P.CODIGO_SEXO,1,'M',6,'F', '') AS SEXO, PIT.VALOR AS CIPA,
			FUENTES.F_CODIGO_CENTRO_SALUD_PERSONA(p.id,sysdate) as COD_CS
			from Cribadocervix.PETICION_MUESTRA M,  PERSONA.PERSONA P, PERSONA.PERSONA__IDENTIDAD_TIPO PIT
			WHERE M.ID = :id AND M.ID_ACCION = :idAccion
			  AND P.ID = Cribadocervix.F_PERSONA_PRODUCTO(m.cod_producto)
			  and p.fecha_baja = MAX_FECHA
			  and pit.id_persona(+)= p.id and pit.CODIGO_IDENTIDAD_TIPO(+)= 'CIPR'
			"""
			,
			[id: idMuestra, idAccion:idAccion])
		return resultado
	}


	/**
	 * Obtención de registro con datos de la persona asociada a la muestra de muestra de BBDD
	 * @param idMuestra
	 * @return
	 */
	@Transactional
	def recuperarPruebaDetalle(idMuestra){
		// 03/04/2024; jms30x; Issue #37. Todos los campos deben ser texto!!
		def resultado = groovySql.firstRow(
			"""
			SELECT to_char(FEC_MUESTRA, 'YYYYMMDDHH24MISS') as FEC_MUESTRA, 
				DECODE(FM,  1,'REGULAR', 
							0, 'IRREGULAR',
							NULL) AS FM,
				to_char(FEC_ULT_REGLA, 'YYYYMMDD') AS FEC_ULT_REGLA,
				DECODE(MENOPAUSIA, 1,'SI', 
								   0, 'NO', 
								   2, 'DESCONOCIDO', 
								   NULL) AS MENOPAUSIA,
				to_char(MENOP_EDAD_INI) AS MENOP_EDAD_INI,
				to_char(EMBARAZO_NUM) AS EMBARAZO_NUM,
				ANTEC_PATOL, CIRUGIAS_GINE_PREV, TER_HORM_TIPO, TER_HORM_DURACION,
				DECODE(CRIB_ADEC_10, 1,'SI',
									 0, 'NO',
									 NULL) AS CRIB_ADEC_10,
				DECODE(CRIB_ADEC_10_NEGAT,  1,'SI', 
											0, 'NO',
											NULL) AS CRIB_ADEC_10_NEGAT,
				DECODE(PAT_CERV_PREV, 1,'SI',
									  0, 'NO',
									  NULL) AS PAT_CERV_PREV,
				DECODE(PAT_CERV_PREV_VPH, 1,'SI',
									 	  0, 'NO',
										  2, 'DESCONOCIDO',
									      NULL) AS PAT_CERV_PREV_VPH,
				DECODE(PAT_CERV_PREV_20, 1,'SI',
									 	  0, 'NO',
										  2, 'DESCONOCIDO',
									      NULL) AS PAT_CERV_PREV_20,
				DECODE(PAT_CERV_PREV_TIPOCIN, 1,'SI',
									 	  0, 'NO',
										  2, 'DESCONOCIDO',
									      NULL) AS PAT_CERV_PREV_TIPOCIN,
				DECODE(ANOVUL_ORALES, 1,'SI',
									 0, 'NO',
									 NULL) AS ANOVUL_ORALES,
				DECODE(QUIMIOTERAPIA, 1,'SI',
									 0, 'NO',
									 NULL) AS QUIMIOTERAPIA,
				DECODE(RADIOTERAPIA, 1,'SI',
									 0, 'NO',
									 NULL) AS RADIOTERAPIA,
				DECODE(GESTANTE, 1,'SI',
									 0, 'NO',
									 NULL) AS GESTANTE,
				DECODE(PUERPERIO, 1,'SI',
									 0, 'NO',
									 NULL) AS PUERPERIO,
				DECODE(DIU, 1,'SI',
									 0, 'NO',
									 NULL) AS DIU,
				OTROS_TRATAM, OBSERVACIONES, 
				DECODE(HALL_GINE_VULVITIS, 1,'SI',
										 0, 'NO',
										 NULL) AS HALL_GINE_VULVITIS,
				DECODE(HALL_GINE_HIPO_ATRO, 1,'SI',
										 0, 'NO',
										 NULL) AS HALL_GINE_HIPO_ATRO,
				DECODE(HALL_GINE_DOLOR, 1,'SI',
										 0, 'NO',
										 NULL) AS HALL_GINE_DOLOR,
				DECODE(HALL_GINE_ERITROPLASIA, 1,'SI',
										 0, 'NO',
										 NULL) AS HALL_GINE_ERITROPLASIA,
				DECODE(HALL_GINE_LEUCORREA, 1,'SI',
										 0, 'NO',
										 NULL) AS HALL_GINE_LEUCORREA,
				DECODE(HALL_GINE_COLPITIS, 1,'SI',
										 0, 'NO',
										 NULL) AS HALL_GINE_COLPITIS,
				HALL_GINE_OTROS, HALLAZGOS_COLPOS, 
				DECODE(VACUNACION_VPH, 1, 'SI',
									0, 'NO',
									2, 'DESCONOCIDO',
									NULL ) AS VACUNACION_VPH,			
				ID_TIPO_VACUNA,
				to_char(DOSIS_RECIBIDAS) AS DOSIS_RECIBIDAS, 
				to_char(EDAD_PRIM_DOSIS) AS EDAD_PRIM_DOSIS,
				to_char(FEC_ULT_DOSIS, 'YYYYMMDDHH24MISS')  AS FEC_ULT_DOSIS
			from Cribadocervix.PRUEBA_DETALLE
			WHERE ID_MUESTRA = :idMuestra
			"""
			,
			[idMuestra: idMuestra])
		return resultado
	}

	def recuperarResultadoEstudioVPH(idMuestra){
		def resultado
		//Recuperamos el id Estudio de la muestra con la que estamos generando la petición de citología
		def idTipoPrueba = Constantes.ID_TIPO_PRUEBA_CITOLOGIA
		def idEstudio = groovySql.firstRow( """ 
			select id_estudio
			from Cribadocervix.prueba p, Cribadocervix.prueba_detalle pd
			where p.id = pd.id_prueba
			and p.ID_TIPO_PRUEBA = :id_tipo_prueba
			and pd.id_muestra = :idMuestra
		""",
		[idMuestra: idMuestra, id_tipo_prueba: idTipoPrueba])

		//Comprobamos el tipo de estudio
		def tipoEstudio = groovySql.firstRow( """ 
			select ID_ESTUDIO_TIPO from Cribadocervix.estudio where id = :id_estudio
			""",
		[id_estudio: idEstudio[0]])

		//Tipo de estudio que queremos recuperar (VPH)
		def idEstudioTipoAnterior= Constantes.ID_TIPO_ESTUDIO_VPH

		//Si el tipo estudio es Citologia buscamos el estudio VPH anterior del mismo proceso
		if(tipoEstudio[0] != null && tipoEstudio[0] == Constantes.ID_TIPO_ESTUDIO_CITOLOGIA) {
            //Si el estudio es citologia se recupera el resultado del estudio vph del mismo proceso
			resultado = groovySql.firstRow( """
				select TO_CHAR(ID_ESTUDIO_RESULTADO_TIPO) ID_ESTUDIO_RESULTADO_TIPO
				from Cribadocervix.estudio
				where id = (select max(id)
							from Cribadocervix.estudio
							where id_proceso = (select id_proceso
												from Cribadocervix.estudio
												where id = :id_estudio)
							and id < :id_estudio
				 			and ID_ESTUDIO_TIPO = :id_tipo_estudio)
				""",
				[id_estudio: idEstudio[0], id_tipo_estudio: idEstudioTipoAnterior])
		}else if(tipoEstudio[0] != null && tipoEstudio[0] == Constantes.ID_TIPO_ESTUDIO_GINECOLOGIA){
            //Si el estudio es ginecologia, recuperamos el resultado del estudio vph del proceso padre
            resultado = groovySql.firstRow( """
				select TO_CHAR(e1.ID_ESTUDIO_RESULTADO_TIPO) ID_ESTUDIO_RESULTADO_TIPO
				from Cribadocervix.estudio e1
				where e1.id = (select id
							from Cribadocervix.estudio
							where id_proceso = (select p.id_padre
												from Cribadocervix.proceso p,
												Cribadocervix.estudio e2
												where e2.id_proceso = p.id
												AND e2.id = :id_estudio))
							and id < :id_estudio
				 			and ID_ESTUDIO_TIPO = :id_tipo_estudio
				""",
                    [id_estudio: idEstudio[0], id_tipo_estudio: idEstudioTipoAnterior])
        }else{
            resultado = [:]
        }
		return resultado
	}

	/**
	 * Función que recupera los genotipos positivos asociados a la muestra de VPH
	 * @param muestra
	 * @return
	 */
    @Transactional(readOnly = true)
	def recuperarGenotiposPositivosVPH(idMuestra){
		def resultado = [:]
        def tipoPrueba = Constantes.ID_TIPO_PRUEBA_VPH
		//Sentencia que recupera los genotipos de VPH positivo asociados a una muestra
		def sentencia = """ SELECT SUBSTR(ta.DESCRIPCION, -2) AS GENOTIPOS
		FROM Cribadocervix.TIPO_ANALITO ta
		JOIN Cribadocervix.PRUEBA__ANALITO pa ON ta.CODIGO = pa.CODIGO
		JOIN Cribadocervix.PRUEBA_DETALLE pd ON pd.ID_PRUEBA = pa.ID_PRUEBA
		WHERE ta.ID_TIPO_PRUEBA = :tipo_prueba 
		AND pd.ID_MUESTRA = :id_muestra
		"""
		resultado = groovySql.rows(sentencia,[tipo_prueba:tipoPrueba,id_muestra:idMuestra])

		return resultado
	}

    /**
     * Metodo que recupera los datos de cribado anterior si se trata de pruebas Cotest
     * @param muestra
     * @return
     */
    @Transactional(readOnly = true)
    def recuperarCribadoAnteriorCostest(idMuestra){
        def datos = [:]
        datos.id_muestra = idMuestra
        def resultado
        def resultadoCribados = []
        def genotipos
        datos.tipo_cotest1 = Constantes.ID_TIPO_ESTUDIO_COTEST_1
        datos.tipo_cotest2 = Constantes.ID_TIPO_ESTUDIO_COTEST_2
        def tiposPruebas = [Constantes.ID_TIPO_PRUEBA_VPH,Constantes.ID_TIPO_PRUEBA_CITOLOGIA]

        tiposPruebas.each {tipoPruebas->
            datos.tipo_prueba = tipoPruebas

            //Sentencia que recupera los genotipos de VPH positivo asociados a una muestra
            def sentenciaPrueba = """ SELECT DISTINCT TO_CHAR(vcpc.RESULTADO) AS RESULTADO, TO_CHAR(vcpc.FECHA_TOMA,'DD/MM/YYYY') AS FECHA_TOMA,
                                    TO_CHAR(vcpc.FECHA_RESULTADO,'DD/MM/YYYY') AS FECHA_RESULTADO, vcpc.TIPO_PRUEBA
            FROM Cribadocervix.V_CRIB_PREVIO_COTEST vcpc 
            LEFT JOIN Cribadocervix.PROCESO pr ON pr.ID = vcpc.PROCESO
            LEFT JOIN Cribadocervix.ESTUDIO e ON e.ID_PROCESO = pr.ID 
            LEFT JOIN Cribadocervix.PRUEBA p ON e.ID = p.ID_ESTUDIO
            LEFT JOIN Cribadocervix.PRUEBA_DETALLE pd ON pd.ID_PRUEBA = p.ID
            WHERE e.ID_ESTUDIO_TIPO in(:tipo_cotest1,:tipo_cotest2)
            AND pd.ID_MUESTRA = :id_muestra
            AND vcpc.TIPO_PRUEBA = :tipo_prueba
		"""
            resultado = groovySql.firstRow(sentenciaPrueba,datos)

            def sentenciaGenotipos = """SELECT vcpc.GENOTIPO
            FROM Cribadocervix.V_CRIB_PREVIO_COTEST vcpc 
            LEFT JOIN Cribadocervix.PROCESO pr ON pr.ID = vcpc.PROCESO
            LEFT JOIN Cribadocervix.ESTUDIO e ON e.ID_PROCESO = pr.ID 
            LEFT JOIN Cribadocervix.PRUEBA p ON e.ID = p.ID_ESTUDIO
            LEFT JOIN Cribadocervix.PRUEBA_DETALLE pd ON pd.ID_PRUEBA = p.ID
            WHERE e.ID_ESTUDIO_TIPO in(:tipo_cotest1,:tipo_cotest2)
            AND pd.ID_MUESTRA = :id_muestra
            AND vcpc.TIPO_PRUEBA = :tipo_prueba
            AND vcpc.GENOTIPO IS NOT NULL
        """
            genotipos = groovySql.rows(sentenciaGenotipos, datos)
            genotipos = genotipos.collect{it.get(genotipos.get(0).keySet()[0])}.join(', ')

            if(resultado) {
                if (genotipos) {
                    resultado.GENOTIPOS = genotipos
                } else {
                    resultado.GENOTIPOS = ""
                }
            }
            resultadoCribados.add(resultado)
        }
        return resultadoCribados
    }

	/**
	 * Función que recupera la respuesta de servicio WEB llamado para realizar petición de análisis de la muestra
	 * @param muestra
	 * @return
	 */
	def llamadaSW(muestra) {
		def respuestaSW = [:]
		def response

		respuestaSW.correcta = false
		respuestaSW.error = ""

		try {

			def listaRoles


			HttpHeaders headers = new HttpHeaders()
			headers.add("Authorization", jwtSanidadService.generarToken("Cribadocervix", listaRoles))

			def personaMuestra = recuperarPersonaMuestra(muestra.ID,muestra.ID_ACCION)
			def pruebaDetalle = recuperarPruebaDetalle(muestra.ID)
            def tipoEstudioMuestra = estudioEntidadService.recuperarEstudioPorIdMuestra(muestra.ID)

			if (!personaMuestra) {
				respuestaSW.error = "Imposible recuperar datos de la persona asociada a la muestra."

			} else if (muestra.SISTEMA_DESTINO == DESTINO_VPH) {
				String url = baseService.leerParametro("smspeticervix.url") + "/" + baseService.leerParametro("smspeticervix.gestlab")
				def path = "/" + baseService.leerParametro("smspeticervix.gestlab")

				def params = [:]
				params.idPeticionSistemaOrigen = muestra.ID * 10 + muestra.ID_ACCION
				params.idCodigoEtiquetaMuestra = muestra.COD_PRODUCTO
				params.pacienteCIPA = personaMuestra.CIPA
				params.pacienteNombre = personaMuestra.NOMBRE
				params.pacienteApellido1 = personaMuestra.APELLIDO1
				params.pacienteApellido2 = personaMuestra.APELLIDO2
				params.pacienteFechaNacimiento = personaMuestra.FECHA_NACIMIENTO
				params.pacienteSexo = personaMuestra.SEXO
				params.pacientecs=personaMuestra.COD_CS
				params.peticionarioCodCentro = muestra.COD_CENTRO_PETICIONARIO 

				params.accion = muestra.ID_ACCION==1?ACCION_CREAR:ACCION_CANCELAR
				params.prioridad = PRIORIDAD_NORMAL
                if(tipoEstudioMuestra != null){
                    if ((tipoEstudioMuestra.ID_ESTUDIO_TIPO == Constantes.ID_TIPO_ESTUDIO_COTEST_1 || tipoEstudioMuestra.ID_ESTUDIO_TIPO == Constantes.ID_TIPO_ESTUDIO_COTEST_2)){
                        params.codPrueba = baseService.leerParametro("smspeticervix.codPruebaVPHCotest")
                    }else {
                        params.codPrueba = baseService.leerParametro("smspeticervix.codPruebaVPH")
                    }
                }else{
                    params.codPrueba = baseService.leerParametro("smspeticervix.codPruebaVPH")
                }
				
				params.descPrueba = baseService.leerParametro("smspeticervix.descPruebaVPH")

				params.fechaTomaMuestra = pruebaDetalle?pruebaDetalle.FEC_MUESTRA:''

				RESTClient client = new RESTClient(url)
				client.setHeaders(headers)
				response = client.postConHeaders(params, JSONObject.class)

				if (response.error) {
					respuestaSW.error = response.error
				} else if (response.respuesta) {
					if (((String)response.respuesta).indexOf(RESPUESTA_OK) >= 0) {
						respuestaSW.correcta = true;
					} else {
						respuestaSW.error = response.respuesta
					}
				}

			} else if (muestra.SISTEMA_DESTINO == DESTINO_CITO_LIQ) {
				String url = baseService.leerParametro("smspeticervix.url") + "/" + baseService.leerParametro("smspeticervix.patwin")
				def path = "/" + baseService.leerParametro("smspeticervix.patwin")
				//Comprobamos el resultado del VPH anterior a la Citología. Hay que enviar ese dato como un parametro mas en la petición para que puedan diferenciar un VPH+ vs VPH+(Muy Alto Riesgo)
				//Esto es porque las citologías positivas que vienen de un VPH+ Muy Alto Riesgo no serán comunicadas por la matrona, lo comunican en Ginecología. Por tanto, no nos deben enviar cita de comunicacion de resultado para estos casos.
				//Con este parametro podrán diferenciarlo y obviar estas citas.

                //Declaramos variables String para alojar los resultados del VPH previo si el estudio es Citologia Liquida
                String resultadoVPH = ''
                String resultadoGenoPosVPH = ''
                def resultadoPruebaVPH

                //Declaramos variables String para alojar los resultados del Cribado previo si el estudio es Costest
                String resultadoVPHCribPrev = ''
                String resultadoCitoCribPrev = ''
                String fechaTomaVPHCribPrev = ''
                String fechaTomaCitoCribPrev = ''
                String fechaResulVPHCribPrev = ''
                String fechaResulCitoCribPrev = ''
                String genoVPHPrev = ''
                String genoCitoPrev = ''

                if(tipoEstudioMuestra.ID_ESTUDIO_TIPO == Constantes.ID_TIPO_ESTUDIO_COTEST_1 || tipoEstudioMuestra.ID_ESTUDIO_TIPO == Constantes.ID_TIPO_ESTUDIO_COTEST_2){
                    def datosCribPrev = recuperarCribadoAnteriorCostest(muestra.ID)
                    datosCribPrev.each {datoCribPrev ->
                        if(datoCribPrev.TIPO_PRUEBA == Constantes.ID_TIPO_PRUEBA_VPH){
                            resultadoVPHCribPrev = datoCribPrev.RESULTADO
                            fechaTomaVPHCribPrev = datoCribPrev.FECHA_TOMA
                            fechaResulVPHCribPrev = datoCribPrev.FECHA_RESULTADO
                            genoVPHPrev = datoCribPrev.GENOTIPOS
                        }
                        if(datoCribPrev.TIPO_PRUEBA == Constantes.ID_TIPO_PRUEBA_CITOLOGIA){
                            resultadoCitoCribPrev = datoCribPrev.RESULTADO
                            fechaTomaCitoCribPrev = datoCribPrev.FECHA_TOMA
                            fechaResulCitoCribPrev = datoCribPrev.FECHA_RESULTADO
                            genoCitoPrev = datoCribPrev.GENOTIPOS

                        }
                    }
                }else if (tipoEstudioMuestra.ID_ESTUDIO_TIPO == Constantes.ID_TIPO_ESTUDIO_CITOLOGIA ||
                        tipoEstudioMuestra.ID_ESTUDIO_TIPO == Constantes.ID_TIPO_ESTUDIO_GINECOLOGIA){
                    resultadoPruebaVPH = recuperarResultadoEstudioVPH(muestra.ID)

                    if (resultadoPruebaVPH.ID_ESTUDIO_RESULTADO_TIPO.equals(Constantes.ID_RESULTADO_ESTUDIO_VPH_POS_2.toString())) {
                        resultadoVPH = Constantes.VPH_MUY_ALTO_RIESGO
                    }
                    else if (resultadoPruebaVPH.ID_ESTUDIO_RESULTADO_TIPO.equals(Constantes.ID_RESULTADO_ESTUDIO_VPH_POS_1.toString())){
                        resultadoVPH = Constantes.VPH_ALTO_RIESGO
                    }
                    else if (resultadoPruebaVPH.ID_ESTUDIO_RESULTADO_TIPO.equals(Constantes.ID_RESULTADO_ESTUDIO_VPH_NEG.toString())) {
                        resultadoVPH = Constantes.VPH_NEGATIVO
                    }
                    else {
                        resultadoVPH = Constantes.VPH_NO_VALIDO
                    }
                    //Recuperamos los genotipos de la muestra si el estudio es Citologia Liquida
                    def genoPosVPH = recuperarGenotiposPositivosVPH(muestra.ID)
                    //Se comprueba si el resultado recuperado es nulo o no tiene valores, ya que en esta
                    //caso la variable anterior debe ser un String vacio
                    if(genoPosVPH != null || genoPosVPH.size() != 0){
                        resultadoGenoPosVPH = genoPosVPH.collect{it.get(genoPosVPH.get(0).keySet()[0])}.join(", ")
                    }
                }

				//Recuperamos la descripcion de la vacuna VPH administrada a la paciente a través del tipo de vacuna
				def vacunaVPHPaciente
				def idVacuna = pruebaDetalle.ID_TIPO_VACUNA
				if(pruebaDetalle.ID_TIPO_VACUNA != null){
					vacunaVPHPaciente = expedienteService.recuperarVacunasVPHPorId(idVacuna).DESCRIPCION
						//Añadimos el prefijo según el tipo de vacuna VPH
						if(vacunaVPHPaciente.toLowerCase() == Constantes.VPH_CERVARIX){
							vacunaVPHPaciente = "Bivalente (" + vacunaVPHPaciente + ")"
						}
						if (vacunaVPHPaciente.toLowerCase() == Constantes.VPH_GARDASIL){
							vacunaVPHPaciente = "Tetravalente (" + vacunaVPHPaciente + ")"
						}
						if(vacunaVPHPaciente.toLowerCase() == Constantes.VPH_GARDASIL9){
							vacunaVPHPaciente = "Nonavalente (" + vacunaVPHPaciente + ")"
						}
				}else{
					vacunaVPHPaciente = "Sin especificar"
				}

				def params = [:]

				params.idPeticionSistemaOrigen = muestra.ID * 10 + muestra.ID_ACCION
				params.idCodigoEtiquetaMuestra = muestra.COD_PRODUCTO
				params.pacienteCIPA = personaMuestra.CIPA
				params.pacienteNombre = personaMuestra.NOMBRE
				params.pacienteApellido1 = personaMuestra.APELLIDO1
				params.pacienteApellido2 = personaMuestra.APELLIDO2
				params.pacienteFechaNacimiento = personaMuestra.FECHA_NACIMIENTO
				params.pacienteSexo = personaMuestra.SEXO

				// Es necesario obtenerlo de la tabla de peticiones o de la ubicación de la matrona.
				params.peticionarioCodCentro = muestra.COD_CENTRO_PETICIONARIO // Ejemplo "08015410" CS MURCIA/FLORIDABLANCA

				params.accion = muestra.ID_ACCION==1?ACCION_CREAR:ACCION_CANCELAR
				params.prioridad = PRIORIDAD_NORMAL
				params.codPrueba = baseService.leerParametro("smspeticervix.codPruebaCITOLI")
				params.descPrueba = baseService.leerParametro("smspeticervix.descPruebaCITOLI")

				params.fechaTomaMuestra = pruebaDetalle?pruebaDetalle.FEC_MUESTRA:''


				params.comentariosPrueba = new java.util.LinkedHashMap()
				if (pruebaDetalle != null && !pruebaDetalle.equals("")) {
					// Añadimos los detalles de la prueba que han sido rellenados al mandar la petición
					//Incluimos los obligatorios (Menopausia, Cribado Adecuado, Patologia Previa)
					//junto a los resultados VPH y los datos de vacunación
					params.comentariosPrueba = params.comentariosPrueba + [
						"Resultado VPH": (resultadoPruebaVPH?resultadoVPH:''),
						"Genotipos VPH": resultadoGenoPosVPH]

					//Vacunacion
					if (pruebaDetalle.VACUNACION_VPH.equals("SI")){
						params.comentariosPrueba = params.comentariosPrueba + [
								"Vacunación VPH": (pruebaDetalle?pruebaDetalle.VACUNACION_VPH:''),
								"Tipo de Vacuna recibida": vacunaVPHPaciente,
								"Dosis recibidas": (pruebaDetalle.DOSIS_RECIBIDAS?pruebaDetalle.DOSIS_RECIBIDAS:'Sin especificar'),
								"Edad primera dosis":(pruebaDetalle.EDAD_PRIM_DOSIS?pruebaDetalle.EDAD_PRIM_DOSIS:'Sin especificar'),
								"Fecha última dosis":(pruebaDetalle.FEC_ULT_DOSIS?pruebaDetalle.FEC_ULT_DOSIS:'Sin especificar')]
					}else{
						params.comentariosPrueba = params.comentariosPrueba + [
								"Vacunación VPH": (pruebaDetalle?pruebaDetalle.VACUNACION_VPH:''),
								"Tipo de Vacuna recibida": 'Sin especificar',
								"Dosis recibidas": 'Sin especificar',
								"Edad primera dosis": 'Sin especificar',
								"Fecha última dosis":'Sin especificar']
					}
					//Menopausia
					params.comentariosPrueba = params.comentariosPrueba + [
						"Menopausia": (pruebaDetalle?pruebaDetalle.MENOPAUSIA:'')
					]
					if (pruebaDetalle.MENOPAUSIA.equals("SI")) {
						params.comentariosPrueba = params.comentariosPrueba + [
						"Menopausia Edad Inicio": (pruebaDetalle?pruebaDetalle.MENOP_EDAD_INI:'')]
					}
					//Cribado Adecuado
					params.comentariosPrueba = params.comentariosPrueba + [
						"Cribado adecuado en los últimos 10 años": (pruebaDetalle?pruebaDetalle.CRIB_ADEC_10:'')]
					if (pruebaDetalle.CRIB_ADEC_10.equals("SI")) {
						params.comentariosPrueba = params.comentariosPrueba + [
							"¿Los resultados fueron negativos?": (pruebaDetalle?pruebaDetalle.CRIB_ADEC_10_NEGAT:'')]
					}
					//Patologia Previa
					params.comentariosPrueba = params.comentariosPrueba + [
							"Patología Cervical Previa": (pruebaDetalle?pruebaDetalle.PAT_CERV_PREV:'')]
					if (pruebaDetalle.PAT_CERV_PREV.equals("SI")) {
					params.comentariosPrueba = params.comentariosPrueba + [
							"¿Ha tenido alguna lesión cervical causada por el VPH?": (pruebaDetalle?pruebaDetalle.PAT_CERV_PREV_VPH:'')]
					}
					if (pruebaDetalle.PAT_CERV_PREV_VPH.equals("SI")) {
					params.comentariosPrueba = params.comentariosPrueba + [
							"¿Fue hace menos de 20 años?": (pruebaDetalle?pruebaDetalle.PAT_CERV_PREV_20:'')]
					}
					if (pruebaDetalle.PAT_CERV_PREV_20.equals("SI")) {
					params.comentariosPrueba = params.comentariosPrueba + [
							"¿Recuerda si fue CIN II o más?(CIN III, Ca escamoso infiltrante, AIS o ADC)": (pruebaDetalle?pruebaDetalle.PAT_CERV_PREV_TIPOCIN:'')]
					}

					//Incluimos los opcionales
					if (pruebaDetalle.EMBARAZO_NUM != null) {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Embarazo":"SI",
							"Número de embarazos": (pruebaDetalle?pruebaDetalle.EMBARAZO_NUM:'')]
					}else {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Embarazo":"Sin especificar"]
					}
					if (pruebaDetalle.FM != null) {
						params.comentariosPrueba = params.comentariosPrueba + [
							"F.M.": (pruebaDetalle?pruebaDetalle.FM:'')]
					}else {
						params.comentariosPrueba = params.comentariosPrueba + [
							"F.M.":"Sin especificar"]
					}
					if (pruebaDetalle.FEC_ULT_REGLA != null) {
						params.comentariosPrueba = params.comentariosPrueba + [
							"FUR (Fecha última regla)": (pruebaDetalle?pruebaDetalle.FEC_ULT_REGLA:'')]
					}else {
						params.comentariosPrueba = params.comentariosPrueba + [
							"FUR (Fecha última regla)": "Sin especificar"]
					}
					if (pruebaDetalle.ANTEC_PATOL.equals("SI")) {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Antecedentes patológicos": (pruebaDetalle?pruebaDetalle.ANTEC_PATOL:'')]
					}else {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Antecedentes patológicos": "Sin especificar"]
					}
					if (pruebaDetalle.CIRUGIAS_GINE_PREV.equals("SI")) {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Cirugías Ginecológicas Previas": (pruebaDetalle?pruebaDetalle.CIRUGIAS_GINE_PREV:'')]
					}else {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Cirugías Ginecológicas Previas": "Sin especificar"]
					}
					if (pruebaDetalle.TER_HORM_TIPO.equals("SI")) {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Terapia Hormonal": "SI",
							"Tipo": (pruebaDetalle?pruebaDetalle.TER_HORM_TIPO:''),
							"Duración(meses)": (pruebaDetalle?pruebaDetalle.TER_HORM_DURACION:'')]
					}else {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Terapia Hormonal": "Sin especificar"]
					}
					if (pruebaDetalle.ANOVUL_ORALES.equals("SI")) {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Anovulatorios": (pruebaDetalle?pruebaDetalle.ANOVUL_ORALES:'')]
					}else {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Anovulatorios": "Sin especificar"]
					}
					if (pruebaDetalle.QUIMIOTERAPIA.equals("SI")) {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Quimioterapia": (pruebaDetalle?pruebaDetalle.QUIMIOTERAPIA:'')]
					}else {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Quimioterapia": "Sin especificar"]
					}
					if (pruebaDetalle.RADIOTERAPIA.equals("SI")) {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Radioterapia": (pruebaDetalle?pruebaDetalle.RADIOTERAPIA:'')]
					}else {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Radioterapia": "Sin especificar"]
					}
					if (pruebaDetalle.GESTANTE.equals("SI")) {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Gestante": (pruebaDetalle?pruebaDetalle.GESTANTE:'')]
					}else {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Gestante": "Sin especificar"]
					}
					if (pruebaDetalle.PUERPERIO.equals("SI")) {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Puerperio": (pruebaDetalle?pruebaDetalle.PUERPERIO:'')]
					}else {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Puerperio": "Sin especificar"]
					}
					if (pruebaDetalle.DIU.equals("SI")) {
						params.comentariosPrueba = params.comentariosPrueba + [
							"DIU": (pruebaDetalle?pruebaDetalle.DIU:'')]
					}else {
						params.comentariosPrueba = params.comentariosPrueba + [
							"DIU": "sin especificar"]
					}
					if (pruebaDetalle.OTROS_TRATAM != null)
						params.comentariosPrueba = params.comentariosPrueba + [
							"Otros tratamientos": (pruebaDetalle?pruebaDetalle.OTROS_TRATAM:'')]
					if (pruebaDetalle.OBSERVACIONES != null)
						params.comentariosPrueba = params.comentariosPrueba + [
							"Observaciones": (pruebaDetalle?pruebaDetalle.OBSERVACIONES:'')]


					//Hallazgos Ginecológicos
					if (pruebaDetalle.HALL_GINE_VULVITIS.equals("SI") || pruebaDetalle.HALL_GINE_HIPO_ATRO.equals("SI") || pruebaDetalle.HALL_GINE_DOLOR.equals("SI") || pruebaDetalle.HALL_GINE_ERITROPLASIA.equals("SI") || pruebaDetalle.HALL_GINE_LEUCORREA.equals("SI") || pruebaDetalle.HALL_GINE_COLPITIS.equals("SI") || pruebaDetalle.HALL_GINE_OTROS != null) {

						String hallazgos = ""
						if (pruebaDetalle.HALL_GINE_VULVITIS.equals("SI")) {
							hallazgos = hallazgos + ", Vulvitis"
						}
						if (pruebaDetalle.HALL_GINE_HIPO_ATRO.equals("SI")) {
							hallazgos = hallazgos + ", Hipotrofia-Atrofia"
						}
						if (pruebaDetalle.HALL_GINE_DOLOR.equals("SI")) {
							hallazgos = hallazgos + ", Dolor"
						}
						if (pruebaDetalle.HALL_GINE_ERITROPLASIA.equals("SI")) {
							hallazgos = hallazgos + ", Eritroplasia"
						}
						if (pruebaDetalle.HALL_GINE_LEUCORREA.equals("SI")) {
							hallazgos = hallazgos + ", Leucorrea"
						}
						if (pruebaDetalle.HALL_GINE_COLPITIS.equals("SI")) {
							hallazgos = hallazgos + ", Colpitis"
						}
						if (pruebaDetalle.HALL_GINE_OTROS != null && !pruebaDetalle.HALL_GINE_OTROS.equals("")) {
							String otros = ", Otros: " +pruebaDetalle.HALL_GINE_OTROS
							hallazgos = hallazgos + otros
						}

						params.comentariosPrueba = params.comentariosPrueba + [
							"Hallazgos Ginecológicos": hallazgos.substring(2)]

					}else {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Hallazgos Ginecológicos": "Sin especificar"]
					}

					//Hallazgos Colposcopicos
					if (pruebaDetalle.HALLAZGOS_COLPOS != null) {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Hallazgos Colposcópicos": (pruebaDetalle?pruebaDetalle.HALLAZGOS_COLPOS:'')]
					}else {
						params.comentariosPrueba = params.comentariosPrueba + [
							"Hallazgos Colposcópicos": "Sin especificar"]
					}
                    //Cribado Previo Cotest
                    if(tipoEstudioMuestra.ID_ESTUDIO_TIPO == Constantes.ID_TIPO_ESTUDIO_COTEST_1 || tipoEstudioMuestra.ID_ESTUDIO_TIPO == Constantes.ID_TIPO_ESTUDIO_COTEST_2){
                        params.comentariosPrueba = params.comentariosPrueba + [
                                "Resultado VPH Previo": resultadoVPHCribPrev,
                                "Fecha toma VPH Previo" : fechaTomaVPHCribPrev,
                                "Fecha resultado VPH Previo" : fechaResulVPHCribPrev,
                                "Genotipos VPH Previo": genoVPHPrev,
                                "Resultado Citología Previa": resultadoCitoCribPrev,
                                "Fecha toma Citología Previa" : fechaTomaCitoCribPrev,
                                "Fecha resultado Citología Previa" : fechaResulCitoCribPrev,
                                "Genotipos Citología Previa": genoCitoPrev,
								"Cotest" : "SI"
                                ]
					}else{
                        params.comentariosPrueba = params.comentariosPrueba + ["Cotest": "NO"]
                    }
				}

				RESTClient client = new RESTClient(url)
				client.setHeaders(headers)
				response = client.postConHeaders(params, JSONObject.class)

				if (response.error) {
					respuestaSW.error = response.error
				} else if (response.respuesta) {
					if (((String)response.respuesta).indexOf(RESPUESTA_OK) >= 0) {
						respuestaSW.correcta = true;
					} else {
						respuestaSW.error = response.respuesta
					}
				}

			}
		} catch (HttpClientErrorException e) {
			respuestaSW.error = e.getResponseBodyAsString()
		}catch (Exception ex) {
			respuestaSW.error = ex.getMessage();
		}

		return respuestaSW;
	}
}
