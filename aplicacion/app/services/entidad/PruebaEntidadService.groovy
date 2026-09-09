package  pcacervix.entidad

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

import base.BaseService
import base.PermisosEntidadService
import  jade.FUENTESService
import  pcacervix.Constantes
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionAspectSupport

import java.sql.Timestamp

@Slf4j
@Transactional
@Service
//@RestController("CERVIXestudioEntidadService")
class PruebaEntidadService {
	@Autowired
	Environment env
	// Servicios
	@Autowired
	BaseService baseService
	@Autowired
	FUENTESService FUENTESService
	@Autowired
	EstudioEntidadService estudioEntidadService
	@Autowired
	PermisosEntidadService permisosEntidadService
	
	// Bean que nos permite hacer consultas SQL directas a la base de datos
	@Autowired
	Sql groovySql
	
	
	def validar (datos) {
		def errores = [:]
		
		if(datos.cod_producto != null && datos.cod_producto != "") {
			def tamanyo = datos.cod_producto.length()
			//Prueba VPH
			if(datos.id_tipo_prueba == 1 && datos.id_tipo_estudio != Constantes.ID_TIPO_ESTUDIO_COTEST_1) {
				// Tamaño 11 digitos, cuatro primeros digitos debe ser 1873
				if ( tamanyo != Constantes.TAM_CODPRODUCTO) {
					errores.cod_producto = "El código del producto no cumple con los requisitos de tamaño 11 dígitos (los 4 primeros 1873)."
				}else {
					def primerosDigVPH = datos.cod_producto.substring(0,4)
					if(primerosDigVPH != Constantes.PREFIJO_AUTOTOMA) {
						errores.cod_producto = "El código del producto no cumple con los requisitos. Los 4 primeros dígitos deben ser: 1873."
					 }
				}	   
			}
			// Prueba Citología Líquida
			if(datos.id_tipo_prueba == 2 || (datos.id_tipo_prueba == 1 && datos.id_tipo_estudio == Constantes.ID_TIPO_ESTUDIO_COTEST_1)) {
				//Comprobamos tamaño, 13 digitos
				if (tamanyo != Constantes.TAM_COD_CIT_LIQ) {
					errores.cod_producto =  "El código del producto no cumple con el tamaño requerido, 8 dígitos."
				}else {
					def primerDigCito = datos.cod_producto.substring(0,1)
					//Comprobamos prefijo
					def coincide = false
					Constantes.PREFIJO_CIT_LIQ.each {
						if(primerDigCito == it)
							coincide = true
					}
					if (!coincide)
						errores.cod_producto = "El código del producto no cumple con los requisitos. El primer dígito corresponde al area de salud. Del 1-9."
				}
                if (datos.embarazo && datos.embarazo_num == null) {
                    errores.embarazo_num = "Debe rellenar este campo"
                }
                if (datos.menopausia == "1" && datos.menop_edad_ini == null) {
                    errores.menop_edad_ini = "Debe indicar la edad"
                }
                if (datos.antecedentes_patologicos && datos.antec_patol.equals("")) {
                   errores.antec_patol = "Debe rellenar este campo"
                }
                if (datos.cirugia_gine_previa && datos.cirugias_gine_prev.equals("")) {
                   errores.cirugias_gine_prev = "Debe rellenar este campo"
                }
                if (datos.ter_hormonal && datos.ter_horm_tipo.equals("")) {
                   errores.ter_horm_tipo = "Debe rellenar Tipo"
                }
                if (datos.crib_adec_10 == "1" && datos.crib_adec_10_negat == null) {
                    errores.crib_adec_10_negat = "Campo obligatorio"
                }
                if (datos.pat_cerv_prev == "1" && datos.pat_cerv_prev_vph == null ) {
                    errores.pat_cerv_prev_vph = "Campo obligatorio"
                }
                if (datos.pat_cerv_prev_vph == "1" && datos.pat_cerv_prev_20 == null ) {
                    errores.pat_cerv_prev_20 = "Campo obligatorio"
                }
                if (datos.pat_cerv_prev_20 == "1" && datos.pat_cerv_prev_tipocin == null ) {
                    errores.pat_cerv_prev_tipocin = "Campo obligatorio"
                }
                if (datos.hallazgos_gine && (!datos.hall_gine_vulvitis && !datos.hall_gine_hipo_atro && !datos.hall_gine_dolor &&
                    !datos.hall_gine_eritroplasia && !datos.hall_gine_leucorrea && !datos.hall_gine_colpitis && !datos.otros)){
                     errores.hallazgos_gine = "Debe seleccionar alguno de los Hallazgos Ginecológicos."
                }
                if (datos.otros && datos.hall_gine_otros.equals("")) {
                     errores.hall_gine_otros = "Debe rellenar descripción del campo Otros Hallazgos Ginecológicos"
                }
                if (datos.colposcopia && datos.hallazgos_colpos.equals("")) {
                     errores.hallazgos_colpos = "Debe rellenar descripción de Hallazgos Colposcópicos"
                }
				if(datos.edad_prim_dosis != null){
					if(datos.edad_prim_dosis < 0 || datos.edad_prim_dosis >= 100){
						errores.edad_prim_dosis = "Número no válido. Debe contener sólo dos dígitos y ser positivo."
					}
				}
            }
          }else {
			errores.cod_producto = "El código del producto es obligatorio."
		}
       
		if(!errores.isEmpty()) {
			errores.btnGuardarPruebaEst = "Compruebe campos obligatorios"
		}

		return errores
	}

    @Transactional(readOnly = true)
	def recuperar(datos, bloquear = false) {
		log.debug("Recuperar Entidad Prueba, id: ${datos.id_prueba}")
		
		def sentencia = """
			select p.ID, (select pt.descripcion from pcacervix.prueba_tipo pt where pt.id = p.ID_TIPO_PRUEBA) descripcion, 
                ID_TIPO_PRUEBA, p.FECHA, p.ID_ESTUDIO, p.FEC_RESULTADO, p.VALOR_RESULTADO, 
                (SELECT DESCRIPCION FROM FUENTES.ENTIDAD WHERE CODIGO = p.COD_PROCEDENCIA AND FECHA_BAJA > SYSDATE) CENTRO_EXTRACCION, 
                p.ID_PROFESIONAL, pd.ID_MUESTRA, pd.COD_PRODUCTO, pd.AUTOTOMA, pd.FEC_MUESTRA, pd.FEC_RECEPCION_LAB, 
                pd.FEC_ULT_REGLA,pd.MENOPAUSIA, pd.CIRUGIAS_GINE_PREV, pd.HALLAZGOS_COLPOS, pd.FM, pd.MENOP_EDAD_INI,
                pd.EMBARAZO_NUM,pd.ANTEC_PATOL, pd.TER_HORM_TIPO,pd.TER_HORM_DURACION,pd.CRIB_ADEC_10,pd.CRIB_ADEC_10_NEGAT, 
                pd.PAT_CERV_PREV,pd.PAT_CERV_PREV_VPH,pd.PAT_CERV_PREV_20, pd.PAT_CERV_PREV_TIPOCIN, pd.ANOVUL_ORALES,pd.QUIMIOTERAPIA,
                pd.RADIOTERAPIA,pd.GESTANTE,pd.PUERPERIO,pd.DIU, pd.OTROS_TRATAM,pd.OBSERVACIONES,pd.HALL_GINE_VULVITIS,pd.HALL_GINE_HIPO_ATRO,
                pd.HALL_GINE_DOLOR, pd.HALL_GINE_ERITROPLASIA,pd.HALL_GINE_LEUCORREA,pd.HALL_GINE_COLPITIS,pd.HALL_GINE_OTROS, pd.VACUNACION_VPH,
                pd.ID_TIPO_VACUNA,pd.DOSIS_RECIBIDAS,pd.EDAD_PRIM_DOSIS,pd.FEC_ULT_DOSIS 
			from pcacervix.prueba p 
			left join pcacervix.prueba_detalle pd on p.id = pd.id_prueba 
			where p.id = :id_prueba
			"""
		def resultado =  baseService.recuperarEntidad(sentencia, datos)
		
		return resultado
	}

	def insertar(datos) {
		log.debug("Insertar en PRUEBA: ${datos}")
		Map resultado = [:]
		resultado.errores = [:]
        Map errores = [:]
        errores.error = ''

        //Damos formato de fecha a todas las fechas del formulario
		
		if (datos.fec_ult_regla) {
			datos.fec_ult_regla = datos.fec_ult_regla.toTimestamp()
		}

        if (datos.fec_ult_dosis) {
            datos.fec_ult_dosis = datos.fec_ult_dosis.toTimestamp()
        }

        if (datos.fec_muestra) {
            datos.fec_muestra = datos.fec_muestra.toTimestamp()
        }else{
            //Si la prueba es VPH y no es autotoma, se incluye la fecha de muestra el día actual
            datos.fec_muestra = new Date().toTimestamp()
        }

        //recuperamos el valor del secuencial para idPrueba, que usaremos mas adelante
        def sentenciaSecuencial = """
				select  pcacervix.S_PRUEBA.nextval from dual
			"""
        def idPrueba = groovySql.firstRow(sentenciaSecuencial)
        datos.id = idPrueba[0]

        //La Fecha que registramos en la tabla Prueba es la del día Actual en el que se registra
        def sentencia = """
				insert into pcacervix.prueba (ID, ID_TIPO_PRUEBA, FECHA, ID_ESTUDIO, FEC_RESULTADO, VALOR_RESULTADO, ID_PROFESIONAL)
					values (:id, :id_tipo_prueba, SYSDATE, :id_estudio, :fec_resultado, :valor_resultado, :id_profesional)
			"""

        try{
            resultado = baseService.insertarEntidad(sentencia, datos)
        }catch(Exception e){
            //Añadimos trazas en el log para encontrar el error facilmente
            log.error("Error de insercion en PRUEBA ", e)
            return
        }

        if(resultado.errores) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
        }else{
            //Insertamos detalles en PRUEBA_DETALLE (siempre que el tipo de Prueba sea 1 o 2 (VPH o Cit. Liq.)
            // La Fec_Muestra que registramos en PRUEBA_DETALLE es la que inserta la matrona por pantalla a traves del Campo Fecha Toma Muestra
            if(datos.id_tipo_prueba == Constantes.ID_TIPO_PRUEBA_VPH || datos.id_tipo_prueba == Constantes.ID_TIPO_PRUEBA_CITOLOGIA) {
                if(datos.id_tipo_prueba == Constantes.ID_TIPO_PRUEBA_VPH) {
                    //Si se inserta a traves de la aplicación una prueba de tipo VPH, el campo AUTOTOMA en BBDD lo ponemos a 0, así identificamos en los procedimientos de bbdd si ha sido introducida manualmente o por PL-SQL
                    datos.autotoma = 0
                }
                if(datos.id_tipo_prueba == Constantes.ID_TIPO_PRUEBA_CITOLOGIA) {
                    //Si el tipo de estudio es Cit Liq, el valor lo ponemos a null, es la forma de diferenciar en la tabla PRUEBA_DETALLE cuando una prueba es citología.
                    datos.autotoma = null
                }

                sentencia = """
						insert into pcacervix.prueba_detalle (ID_PRUEBA,ID_MUESTRA,COD_PRODUCTO,AUTOTOMA,FEC_MUESTRA,FEC_ULT_REGLA,MENOPAUSIA,CIRUGIAS_GINE_PREV,HALLAZGOS_COLPOS,
								 FM,MENOP_EDAD_INI,EMBARAZO_NUM,ANTEC_PATOL,TER_HORM_TIPO,TER_HORM_DURACION,CRIB_ADEC_10,CRIB_ADEC_10_NEGAT,PAT_CERV_PREV,PAT_CERV_PREV_VPH,
								 PAT_CERV_PREV_20,PAT_CERV_PREV_TIPOCIN,ANOVUL_ORALES,QUIMIOTERAPIA,RADIOTERAPIA,GESTANTE,PUERPERIO,DIU,OTROS_TRATAM,OBSERVACIONES,HALL_GINE_VULVITIS,HALL_GINE_HIPO_ATRO,
								 HALL_GINE_DOLOR,HALL_GINE_ERITROPLASIA,HALL_GINE_LEUCORREA,HALL_GINE_COLPITIS,HALL_GINE_OTROS,VACUNACION_VPH,ID_TIPO_VACUNA,DOSIS_RECIBIDAS,EDAD_PRIM_DOSIS,FEC_ULT_DOSIS)
						values (:id, pcacervix.S_MUESTRA.nextval, :cod_producto, :autotoma, :fec_muestra, :fec_ult_regla, :menopausia, :cirugias_gine_prev, :hallazgos_colpos,
								:fm, :menop_edad_ini, :embarazo_num, :antec_patol, :ter_horm_tipo, :ter_horm_duracion, :crib_adec_10, :crib_adec_10_negat, :pat_cerv_prev, 
								:pat_cerv_prev_vph, :pat_cerv_prev_20, :pat_cerv_prev_tipocin, :anovul_orales, :quimioterapia, :radioterapia, :gestante, :puerperio, :diu, :otros_tratam,
								:observaciones, :hall_gine_vulvitis, :hall_gine_hipo_atro, :hall_gine_dolor, :hall_gine_eritroplasia, :hall_gine_leucorrea, :hall_gine_colpitis, :hall_gine_otros, 
								:vacunacion_vph, :id_tipo_vacuna, :dosis_recibidas, :edad_prim_dosis, :fec_ult_dosis
								)

					"""
            }

            try{
                //Se intenta ejecutar la segunda operación
                groovySql.execute(sentencia, datos)
                //Si los datos son distinto de citologia, se procede a actualizar el estado del estudio
                if(datos.id_tipo_prueba != Constantes.ID_TIPO_PRUEBA_CITOLOGIA) {
                    def codigoEstado = Constantes.ID_ESTADO_PEND_RESULT
                    try{
                        //Se intenta actualizar el estado
                        estudioEntidadService.actualizarEstadoEstudio(datos.id_estudio, codigoEstado)
                    }catch(Exception e){
                        //Añadimos trazas en el log para encontrar el error facilmente
                        log.error("Error al actualizar el estado del estudio ", e)
                        //Si falla la actualización del estado se hace rollback de todas las operaciones realizadas y se guarda el error para luego mostrarlo
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
                        errores.error += 'Error al actualizar el estado del estudio. Repetir operación'
                    }
                }
            }catch(Exception e){
                //Añadimos trazas en el log para encontrar el error facilmente
                log.error("Error de insercion en prueba_detalle ", e)
                //Si falla la inserción del detalle se hace rollback de todas las operaciones realizadas y se guarda el error para luego mostrarlo
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
                errores.error += 'Error al insertar el detalle de la prueba'
            }
        }
        if(errores.error){
            resultado.errores = errores
        }
		return resultado
	}

	def modificar(datos) {
        log.debug("Editamos PruebaEntidadService datos: ${datos}")
        def resultado

        def sentencia
        sentencia = """
                UPDATE pcacervix.prueba_detalle
                set FEC_ULT_REGLA = :fec_ult_regla,
                    MENOPAUSIA = :menopausia,
                    CIRUGIAS_GINE_PREV = :cirugias_gine_prev,
                    HALLAZGOS_COLPOS = :hallazgos_colpos,
                    FM = :fm,
                    MENOP_EDAD_INI = :menop_edad_ini,
                    EMBARAZO_NUM = :embarazo_num,
                    ANTEC_PATOL = :antec_patol,
                    TER_HORM_TIPO = :ter_horm_tipo,
                    TER_HORM_DURACION = :ter_horm_duracion,
                    CRIB_ADEC_10 = :crib_adec_10,
                    CRIB_ADEC_10_NEGAT = :crib_adec_10_negat,
                    PAT_CERV_PREV = :pat_cerv_prev,
                    PAT_CERV_PREV_VPH = :pat_cerv_prev_vph,
                    PAT_CERV_PREV_20 = :pat_cerv_prev_20,
                    PAT_CERV_PREV_TIPOCIN = :pat_cerv_prev_tipocin,
                    ANOVUL_ORALES = :anovul_orales,
                    QUIMIOTERAPIA = :quimioterapia,
                    RADIOTERAPIA = :radioterapia,
                    GESTANTE = :gestante,
                    PUERPERIO = :puerperio,
                    DIU = :diu,
                    OTROS_TRATAM = :otros_tratam,
                    OBSERVACIONES = :observaciones,
                    HALL_GINE_VULVITIS = :hall_gine_vulvitis,
                    HALL_GINE_HIPO_ATRO = :hall_gine_hipo_atro,
                    HALL_GINE_DOLOR = :hall_gine_dolor,
                    HALL_GINE_ERITROPLASIA = :hall_gine_eritroplasia,
                    HALL_GINE_LEUCORREA = :hall_gine_leucorrea,
                    HALL_GINE_COLPITIS = :hall_gine_colpitis,
                    HALL_GINE_OTROS = :hall_gine_otros,
                    VACUNACION_VPH = :vacunacion_vph,
                    ID_TIPO_VACUNA = :id_tipo_vacuna,
                    DOSIS_RECIBIDAS = :dosis_recibidas,
                    EDAD_PRIM_DOSIS = :edad_prim_dosis,
                    FEC_ULT_DOSIS = :fec_ult_dosis
                where id_prueba = :id_prueba
            """

        resultado = baseService.modificarEntidad(sentencia, datos)

        return resultado
    }

	def borrar(datos) {
		log.debug("borrar PruebaEntidadService datos: ${datos}")
		Map resultado = [:]
		resultado.errores = [:]


        def sentenciaDetalles = """
				delete from pcacervix.prueba_detalle
				where id_prueba = :id_prueba
			"""
        groovySql.execute(sentenciaDetalles, datos)

        def sentencia = """
				delete from  pcacervix.prueba  
				where id = :id_prueba
				"""

        resultado =  baseService.borrarEntidad(sentencia, datos)

        if(resultado){
            log.debug("Datos eliminados en PRUEBA y  PRUEBA_DETALLE")
            return resultado
        }else {
            def errores = [:]
            errores.id_prueba = "Error al eliminar la Prueba. Repetir operación"
            resultado.errores = errores
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
        }

		return resultado
	}

    @Transactional(readOnly = true)
	def recuperarPruebasPorIdEstudio(datos) {
		log.debug("Recuperar Entidad Prueba, con id Estudio: ${datos.id}")

		def sentencia = """
			select DISTINCT(p.ID), (select pt.descripcion from pcacervix.prueba_tipo pt where pt.id = p.ID_TIPO_PRUEBA) descripcion, 
            p.ID_TIPO_PRUEBA, p.FECHA, p.ID_ESTUDIO, p.FEC_RESULTADO, p.VALOR_RESULTADO,
            (SELECT pcacervix.F_INFORME_RESULTADO(p.id) FROM DUAL) PDF_RESULT,
			CASE --Si existe url de pdf devuelve 1 sino 0
				WHEN (SELECT COUNT(p1.ID_ORU_RESULT) FROM PCACERVIX.PRUEBA p1 where p1.id = p.id) >= 1
						THEN 1
				ELSE 0	
			END AS EXISTE_PDF,
            (select cod_producto from pcacervix.prueba_detalle where id_prueba = p.id) cod_producto
			from  pcacervix.prueba p
			where p.id_estudio = :id
			order by p.id DESC
			"""
		return groovySql.rows(sentencia, datos)
	}

    @Transactional(readOnly = true)
	def recuperarAnalitosPorIdPrueba(idPrueba) {
		def sentencia = """
			select CODIGO
			from pcacervix.PRUEBA__ANALITO
			where id_prueba = :id_prueba
		"""
		
		return groovySql.rows(sentencia, [id_prueba: idPrueba])
	}

    @Transactional(readOnly = true)
	def recuperarPruebaPorCodProducto(codProducto) {
		def sentencia = """
			select distinct(pd.id_prueba)
		    from pcacervix.PRODUCTO_DISPENSADO__ESTADO pde, pcacervix.prueba_detalle pd
		    where pd.COD_PRODUCTO = :cod_producto
		"""
		
		return groovySql.firstRow(sentencia, [cod_producto: codProducto])
	}

	def existeCodigoProducto(cod_producto,id_tipo_prueba, tipo_estudio){

		def sentencia = """SELECT pd.ROWID, pd.id_prueba 
		FROM PCACERVIX.PRUEBA_DETALLE pd
		JOIN PCACERVIX.PRUEBA p ON P.ID = pd.ID_PRUEBA 
		WHERE p.ID_TIPO_PRUEBA = :id_tipo_prueba AND pd.COD_PRODUCTO = :cod_producto"""

        //Si la prueba es de citología o vph en estudio cotest, se tiene que cumplir que además no esté resuelta
        //permitiendo incluir codigos de producto duplicados si la prueba está resuelta
        if(id_tipo_prueba == Constantes.ID_TIPO_PRUEBA_CITOLOGIA ||
                (id_tipo_prueba == Constantes.ID_TIPO_PRUEBA_VPH && (tipo_estudio == Constantes.ID_TIPO_ESTUDIO_COTEST_1 ||
                                                                        tipo_estudio == Constantes.ID_TIPO_ESTUDIO_COTEST_2))){
            sentencia += """ AND FEC_RESULTADO IS NULL AND VALOR_RESULTADO IS NULL"""
        }

		def resultado = groovySql.rows(sentencia, [cod_producto: cod_producto, id_tipo_prueba:id_tipo_prueba])
		if(resultado){
			return true
		}else{
			return false
		}

	}

    @Transactional(readOnly = true)
    def recuperarDatosCribadoPrevCotest(id_proceso, tipo_prueba){

        def sentenciaPrueba =""" SELECT DISTINCT RESULTADO, 
                                    TO_CHAR(FECHA_TOMA,'DD/MM/YYYY') AS FECHA_TOMA,
                                    TO_CHAR(FECHA_RESULTADO,'DD/MM/YYYY') AS FECHA_RESULTADO
                                    FROM PCACERVIX.V_CRIB_PREVIO_COTEST 
                                    WHERE PROCESO = :id_proceso
                                    AND TIPO_PRUEBA = :tipo_prueba"""
        def resultado = groovySql.firstRow(sentenciaPrueba, [id_proceso:id_proceso, tipo_prueba:tipo_prueba])

        def sentenciaGenotipos =""" SELECT GENOTIPO
                                    FROM PCACERVIX.V_CRIB_PREVIO_COTEST 
                                    WHERE PROCESO = :id_proceso
                                    AND TIPO_PRUEBA = :tipo_prueba
                                    AND GENOTIPO IS NOT NULL"""

        def genotipos = groovySql.rows(sentenciaGenotipos, [id_proceso:id_proceso, tipo_prueba:tipo_prueba])
        genotipos = genotipos.collect{it.get(genotipos.get(0).keySet()[0])}.join(', ')

        if(resultado) {
            if (genotipos) {
                resultado.GENOTIPOS = genotipos
            } else {
                resultado.GENOTIPOS = " "
            }
        }
        return resultado
    }

    @Transactional(readOnly = true)
    def recuperarDatosControlCito(datos){

        datos.limiteReg = Constantes.LIMITE_CONSULTAS

        if(datos.fecha_desde){
            datos.fecha_desde = datos.fecha_desde.toTimestamp()
        }
        if(datos.fecha_hasta){
            datos.fecha_hasta = datos.fecha_hasta.toTimestamp()
        }

        def sentencia = """SELECT ID_EXPEDIENTE, FECHA_INICIO_EST,PERSONA, FECHA_MUESTRA,PETICION_ENVIADA, RESULTADO_CITO,PDF_RESULT,EXISTE_PDF,
        FECHA_ITCAP, CENTRO_SALUD, PROFESIONAL FROM PCACERVIX.V_CONTROL_CITO WHERE
        """
        if(datos.fecha_desde && datos.fecha_hasta){
            sentencia +=""" FECHA_INICIO_EST >= :fecha_desde AND FECHA_INICIO_EST <= :fecha_hasta AND"""
        }
        if(datos.id_area_salud){
            sentencia += """ ID_AREA_SALUD = :id_area_salud AND"""
        }
        //Se usa el parámetro codigo en vez de ID para localizar el centro de salud
        if(datos.cod_cs){
            sentencia += """ COD_CS = :cod_cs AND"""
        }
        if(datos.id_profesional){
            sentencia += """ ID_PROFESIONAL = :id_profesional AND"""
        }
        if(datos.result_vph_prev){
            sentencia += """ RESULT_VPH_PREV = :result_vph_prev AND"""
        }

        sentencia += """ ROWNUM <= :limiteReg"""

        def resultado = groovySql.rows(sentencia, datos)
        return resultado

    }

    def recuperarPruebasVPHARCaducadas(cod_producto){
        def datos = [:]
        datos.cod_producto = cod_producto
        datos.id_tipo_prueba = Constantes.ID_TIPO_PRUEBA_VPH
        datos.id_tipo_estudio = Constantes.ID_TIPO_ESTUDIO_VPH
        datos.id_est_petic = Constantes.ID_ESTADO_PETI_PROC_OK

        def sentencia ="""select trunc(sysdate - pm.fec_peticion) as diasDesdePet
        from pcacervix.peticion_muestra pm
        left join pcacervix.prueba_detalle pd on pm.id = pd.id_muestra
        left join pcacervix.prueba p on p.id = pd.id_prueba
        left join pcacervix.estudio e on e.id = p.id_estudio
        where pm.cod_producto = :cod_producto
        and pm.id_estado_petic = :id_est_petic
        and pd.fec_recepcion_lab is null
        and p.valor_resultado is null
        and p.id_tipo_prueba = :id_tipo_prueba
        and pd.autotoma = 0
        and e.id_estudio_tipo = :id_tipo_estudio
        and e.id_estudio_resultado_tipo is NULL
        """
        def resultado = groovySql.firstRow(sentencia, datos)
        return resultado

    }

}
