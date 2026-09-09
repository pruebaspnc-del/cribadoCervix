package  cribadocervix.expedientes

import  jade.PandoraService
import  jade.PersanService
import  cribadocervix.DispensacionesFarmaciaService
import groovy.sql.GroovyRowResult
import org.springframework.stereotype.Component
import org.zkoss.zk.ui.Components
import org.zkoss.zk.ui.event.Event
import org.zkoss.zk.ui.select.annotation.Listen
import org.zkoss.zul.Button
import org.zkoss.zul.Checkbox
import org.zkoss.zul.Datebox
import org.zkoss.zul.Div
import org.zkoss.zul.Intbox
import org.zkoss.zul.Label
import org.zkoss.zul.Listbox
import org.zkoss.zul.Listitem
import org.zkoss.zul.Textbox
import org.zkoss.zul.Messagebox
import base.EntidadFormulario
import base.FormularioComposer

import  cribadocervix.Constantes
import  cribadocervix.entidad.EstudioEntidadService
import  cribadocervix.entidad.PruebaEntidadService
import  cribadocervix.entidad.TipoPruebaEntidadService
import groovy.util.logging.Slf4j

import java.text.SimpleDateFormat


@Slf4j
@Component
class PruebaComposer extends FormularioComposer{

	//Entidades
	EstudioEntidadService estudioEntidadService
	PruebaEntidadService pruebaEntidadService
    TipoPruebaEntidadService tipoPruebaEntidadService
	EntidadFormulario prueba
	PersanService persanService
    PandoraService pandoraService
    DispensacionesFarmaciaService dispensacionesFarmaciaService
	
	ExpedienteComposer invocador
	
	//Parametros del filtro de busqueda
	Textbox prueba_id
    Div divIdPrueba
	Textbox prueba_cod_producto
	Listbox prueba_id_tipo_prueba
	Textbox prueba_valor_resultado
    Textbox prueba_centro_extraccion
	Datebox prueba_fec_resultado
	Datebox prueba_fec_muestra
	Listbox prueba_fm
	Checkbox prueba_embarazo
	Intbox prueba_embarazo_num
	Listbox prueba_menopausia
	Intbox prueba_menop_edad_ini
	Datebox prueba_fec_ult_regla
	Checkbox prueba_antecedentes_patologicos
	Div divAntecedentes
	Textbox prueba_antec_patol
	Div divCirugGinPrev
	Checkbox prueba_cirugia_gine_previa
	Textbox prueba_cirugias_gine_prev
	Listbox prueba_crib_adec_10
	Div divResultNeg
	Listbox prueba_crib_adec_10_negat
	Listbox prueba_pat_cerv_prev
	Div divLesionCervical
	Listbox prueba_pat_cerv_prev_vph
	Div divLesionTiempo
	Listbox prueba_pat_cerv_prev_20 
	Div divTipoLesion
	Listbox prueba_pat_cerv_prev_tipocin
	Listbox prueba_vacunacion_vph
	Div divTipoVacuna
	Listbox prueba_id_tipo_vacuna
	Listbox prueba_dosis_recibidas
	Intbox prueba_edad_prim_dosis
	Datebox prueba_fec_ult_dosis
	Checkbox prueba_ter_hormonal
	Textbox prueba_ter_horm_tipo
	Intbox prueba_ter_horm_duracion
	Checkbox prueba_anovul_orales
	Checkbox prueba_quimioterapia
	Checkbox prueba_radioterapia
	Checkbox prueba_gestante
	Checkbox prueba_puerperio
	Checkbox prueba_diu
	Textbox prueba_otros_tratam
	Textbox prueba_observaciones
	Checkbox prueba_hallazgos_gine
	Checkbox prueba_hall_gine_vulvitis
	Checkbox prueba_hall_gine_hipo_atro
	Checkbox prueba_hall_gine_dolor
	Checkbox prueba_hall_gine_eritroplasia
	Checkbox prueba_hall_gine_leucorrea
	Checkbox prueba_hall_gine_colpitis
	Checkbox prueba_otros
	Textbox prueba_hall_gine_otros
	Checkbox prueba_colposcopia
	Textbox prueba_hallazgos_colpos
	Listbox lbAnalitos_prueba
    Textbox prueba_resultadoVph
    Textbox prueba_genotiposVph
    Textbox prueba_fMuestraVph
    Textbox prueba_fResultadoVph
    Textbox prueba_resultadoCito
    Textbox prueba_genotiposCito
    Textbox prueba_fMuestraCito
    Textbox prueba_fResultadoCito
    Textbox prueba_fReenvio

	
	Label tituloPruebaExp
	Label identifPaciente
	Div divDatosPaciente
	Div divDatosGinec 
	Div divAnalitos
	Div divTerapiaHorm
	Div divHallazGinecoDetalles
	Div datosInsercion
	Div datosVista
	Div divHallazColposDetalles
    Div divDatosCribIni
    Div divCentroExtraccion
    Div divReenvioPetElecAr
	
	//Variables
	int indice
	//Botones
	Button btnGuardarPruebaEst
    Button btnGuardarPruebaEstCostest
	Button btnDescartarPruebaEst
	Button btnCerrarVentana
    Button btnReenvioPetElecAr

    //formato fechas
    SimpleDateFormat formato = new SimpleDateFormat(Constantes.fecha_ddMMyyyy)

	def despuesDeComponer() {
		
		prueba = new EntidadFormulario("prueba","id_prueba", this)
		
		// Configuramos la entidad
		prueba.configurar("pcacervix", "prueba")
		// Recuperamos el id de profesional y su unidad funcional seleccionada del usuario actual
		def idProfesional = session.idProfesional
		def idUnidadFuncional = session.unidadesFuncionales[0].id
		def login = session.loginUnico
		// Establecemos los parámetros por defecto para todas las operaciones de entidades
		def parametros = [id_profesional: idProfesional, id_unidad_funcional: idUnidadFuncional, login: login]
		prueba.parametros = parametros
		// Establecer el servicio que va a gestionar la lista de operaciones
		servicioOperaciones = pruebaEntidadService
	
		establecerBotonGuardar(btnGuardarPruebaEst)
		establecerBotonDescartar(btnDescartarPruebaEst)
	}
	
	def activacion() {
       log.debug("procesando ACTIVACION EditarPruebasEstudioComposer")

		limpiar()
		prueba.activarComponentesVista()
		btnDescartarPruebaEst.disabled = false
		btnGuardarPruebaEst.disabled = true
		//Desactivamos el ID y los campos de Historial de Cribado
		prueba_id.disabled = true
        //No mostramos los campos de cribado previo si no es cotest
        divDatosCribIni.visible = false
        btnGuardarPruebaEstCostest.visible = false


		if (invocador) {
            //Rellenamos los valores de los Desplegables
			//Recuperamos el tipo de Pruebas asociadas al estudio
			def idEstudio = invocador.pruebaActual.id_estudio
			def tipoEstudio = estudioEntidadService.recuperarTipoEstudioPorIdEstudio(idEstudio)
			def pruebaTipo = tipoPruebaEntidadService.recuperarPruebasPorTipoEstudio(tipoEstudio[0])

            //Si no es COTEST, el selecctor de prueba se rellena con los recuperados por tipo de estudio
            rellenarListbox(prueba_id_tipo_prueba, pruebaTipo, "Seleccione..." )

			//Recuperamos las vacunas que corresponden con el VPH
			def pruebaVacuna = invocador.expedienteService.recuperarVacunasVPH()
			pruebaVacuna.each {pv->
				if(pv.DESCRIPCION.toLowerCase() == Constantes.VPH_CERVARIX){
					pv.DESCRIPCION = "Bivalente (" + pv.DESCRIPCION + ")"
				}
				if (pv.DESCRIPCION.toLowerCase() == Constantes.VPH_GARDASIL){
					pv.DESCRIPCION = "Tetravalente (" + pv.DESCRIPCION + ")"
				}
				if(pv.DESCRIPCION.toLowerCase() == Constantes.VPH_GARDASIL9){
					pv.DESCRIPCION = "Nonavalente (" + pv.DESCRIPCION + ")"
				}
			}
			//Añadimos un registro más correspondiente a "Desconocido" a la lista de vacunas recuperadas
			def registroDesconocido = new GroovyRowResult(ID:null, DESCRIPCION:"Desconocido")
			pruebaVacuna.add(registroDesconocido)

			rellenarListboxTipoVacuna(prueba_id_tipo_vacuna, pruebaVacuna, "Seleccione...")
			//Recuperamos las pruebas ya registradas en el estudio
			def pruebasEstudio = pruebaEntidadService.recuperarPruebasPorIdEstudio([id:idEstudio])

            //Pintamos identificación del paciente, nombre y edad (solicitado por funcional)
			Date fecNac = Date.parse('dd/MM/yyyy', invocador.pruebaActual.fec_nacim)
			def edad = persanService.calcularEdadPersona(fecNac)
            def nombrePaciente = "Paciente: " + invocador.pruebaActual.nombre_paciente + " " + invocador.pruebaActual.apellido1_paciente +
            " " + invocador.pruebaActual.apellido2_paciente + " (" + edad + " años)"
            escribirValorComponente(identifPaciente, nombrePaciente)

            //Recuperamos el id tipo del proceso activo a traves del id de expediente
            def proceso = invocador.procesoEntidadService.recuperarProcesoActivoPorIdExpediente(invocador.pruebaActual.id_expediente)
            def idProcesoTipo = 0 //valor por defecto
            if(proceso){
                idProcesoTipo = proceso.id_proceso_tipo
            }
			if (invocador.pruebaActual.id_prueba == NUEVO_REGISTRO) {

				//Mostramos los datos que deben insertar al registrar una nueva prueba
				datosInsercion.visible = true
				//Se ocultan los datos que se generan automaticamente, solo se muestran en la vista del detalle. Así no sobrecargamos la vista
				datosVista.visible = false
				//Hacemos visibles los botones Guardar y Descartar(Cancelar)
				btnGuardarPruebaEst.visible = true
				btnDescartarPruebaEst.visible = true
				//Ocultamos el boton Cerrar Ventana, puesto que ya lo hace el de Descartar(Cancelar)
				btnCerrarVentana.visible = false
				
				escribirValorComponente(tituloPruebaExp, invocador.pruebaActual.titulo)
				
				//Crear mapa con datos por defecto
				Map m = [:]
				m.id = ""
				m.fecha = new Date().clearTime()
				m.id_estudio = invocador.pruebaActual.id_estudio
				m.id_profesional = prueba.parametros.id_profesional
				prueba.insertar(m)
				
				/*  Deshabilitamos todos los campos excepto Tipo de Prueba, para que primero seleccionen este, 
				 * ya que dependiendo del tipo de prueba, se habilitaran unos campos u otros. */
				desactivarComponentesVista("prueba")
				prueba_id_tipo_prueba.disabled = false
                // Preseleccionamos el tipo de prueba cuando solo recuperamos 1 tipo
                if(pruebaTipo.size == 1) {
                    prueba_id_tipo_prueba.selectedIndex = 1
                    prueba_cod_producto.disabled = false
                }else{
					//Comprobamos que el tipo de estudio es Ginecologia dentro de
                    // un proceso tipo de seguimiento sin ninguna prueba previa registrada para preseleccionar
                    //la prueba de citologia
					if(tipoEstudio.ID_ESTUDIO_TIPO == Constantes.ID_TIPO_ESTUDIO_GINECOLOGIA
							&& idProcesoTipo == Constantes.ID_PROCESO_SEGUIMIENTO
							&& pruebasEstudio.size() == 0){
						indice = 1
						while(pruebaTipo[indice].ID == Constantes.ID_TIPO_PRUEBA_CITOLOGIA){
							indice++
						}
						prueba_id_tipo_prueba.selectedIndex = indice
						prueba_cod_producto.disabled = false
					}
                    //Damos valor por defecto a la Fecha de Toma de la Muestra, día actual (modificable)
                    Date fechaActual = new Date().clearTime()
                    escribirValorComponente(prueba_fec_muestra, fechaActual)
				}
                //Ocultamos campos que no son comunes para todas las pruebas. Se haran visibles al seleccionar el tipo de prueba correspondiente
                //Si es del tipo Citologia, mostramos todos los campos del detalle
                if(prueba_id_tipo_prueba.selectedItem.value == Constantes.ID_TIPO_PRUEBA_CITOLOGIA){
                    activarComponentesVista("prueba")
                    //Deshabilitamos los campos que se generan automaticamente
                    prueba_id.disabled= true
                    prueba_fec_resultado.disabled= true
                    prueba_valor_resultado.disabled = true
                    prueba_centro_extraccion.disabled = true

                    //Mostramos los campos a rellenar en este tipo de prueba
                    divDatosPaciente.visible = true
                    divDatosGinec.visible = true
                    //Deshabilitamos los campos que solo se activan al marcar ciertos campos
                    prueba_menop_edad_ini.disabled = true
                    prueba_embarazo_num.disabled= true
                    prueba_hall_gine_otros.disabled = true
                }//COSTEST- Se controla los componenetes que deben ser visisbles
                else if(tipoEstudio.ID_ESTUDIO_TIPO == Constantes.ID_TIPO_ESTUDIO_COTEST_1
                        && idProcesoTipo == Constantes.ID_PROCESO_SEGUIMIENTO
                        && pruebasEstudio.size() == 0){
                    //Limpiamos los valores de la lista de tipos de prueba y añadimos la de cotest-1
                    //para que sólo aparezca COTEST
                    prueba_id_tipo_prueba.items.clear()
                    prueba_id_tipo_prueba.appendItem(Constantes.CODIGO_TIPO_EST_COTEST_1, null)
                    prueba_id_tipo_prueba.setSelectedIndex(0)

                    activarComponentesVista("prueba")
                    //Deshabilitamos los campos que se generan automaticamente
                    divIdPrueba.visible = false
                    divDatosCribIni.visible = true
                    divIdPrueba.class = "col order-3"
                    prueba_fec_resultado.disabled= true
                    prueba_valor_resultado.disabled = true
                    prueba_centro_extraccion.disabled = true

                    //Mostramos los campos a rellenar en este tipo de prueba
                    divDatosPaciente.visible = true
                    divDatosGinec.visible = true

                    //Deshabilitamos los campos que solo se activan al marcar ciertos campos
                    prueba_menop_edad_ini.disabled = true
                    prueba_embarazo_num.disabled= true
                    prueba_hall_gine_otros.disabled = true

                    mostrarCribadoPrevioCotest(proceso.id)
                    btnGuardarPruebaEst.visible = false
                    btnGuardarPruebaEstCostest.visible = true
                    btnGuardarPruebaEstCostest.setClass("btn btn-big btn-success")
                }
                else {
					//Si la prueba no es citologia se ocultan los campos del detalle
                    divDatosPaciente.visible = false
					divDatosGinec.visible = false
                }
				divAnalitos.visible = false
			}else {
                if(tipoEstudio.ID_ESTUDIO_TIPO == Constantes.ID_TIPO_ESTUDIO_COTEST_1
                        && idProcesoTipo == Constantes.ID_PROCESO_SEGUIMIENTO){
                    divIdPrueba.visible = true
                    divIdPrueba.class = "col order-1"
                    btnGuardarPruebaEstCostest.visible = false
                    mostrarCribadoPrevioCotest(proceso.id)
                }
                // Comprobamos si aun no se ha enviado la peticion, en cuyo caso activaremos el boton enviar peticion.
               if (invocador.pruebaActual.guardadoPrevioEnvio) {
                   btnGuardarPruebaEst.visible = true
                   btnDescartarPruebaEst.visible = true
                   //Desactivamos los componentes que no pueden modificarse
                   prueba_cod_producto.disabled = true
                   prueba_id_tipo_prueba.disabled = true
                   prueba_fec_muestra.disabled = true
                   //Ocultamos datos de resultado
                   datosVista.visible = false
                   divAnalitos.visible = false
               }else{
                    desactivarComponentesVista("prueba")
                    //Ocultamos los botones guardar y Descartar(Cancelar)
                    btnGuardarPruebaEst.visible = false
                    btnDescartarPruebaEst.visible = false
                    //Mostramos el boton cerrar ventana. (Solo para las vistas de Detalle)
                    btnCerrarVentana.visible = true
                    //Mostramos todos los datos
                    datosVista.visible = true
                    divAnalitos.visible = true
                }
				datosInsercion.visible = true
				
				prueba.seleccion= invocador.pruebaActual.id_prueba
				def map = [:]
				map.id_prueba = prueba.seleccion
				def datosPrueba = prueba.recuperar(map)
				//Recuperamos los Analitos
				def analitos = pruebaEntidadService.recuperarAnalitosPorIdPrueba(prueba.seleccion)
				lbAnalitos_prueba.actualizar(analitos)
				
				escribirValoresVista("prueba", datosPrueba)
				escribirValorComponente(prueba_id_tipo_prueba, datosPrueba.id_tipo_prueba)
				escribirValorComponente(tituloPruebaExp, invocador.pruebaActual.titulo)
				prueba_id.disabled = true

				if (null != datosPrueba.ID_TIPO_PRUEBA) {
					escribirValorComponente(prueba_id_tipo_prueba, datosPrueba.ID_TIPO_PRUEBA)
					//Mostramos solo los componentes asociados a la prueba
					if(datosPrueba.ID_TIPO_PRUEBA == Constantes.ID_TIPO_PRUEBA_CITOLOGIA) {
						divDatosPaciente.visible = true
						divDatosGinec.visible = true
                        //Hacemos no visible el centro de extraccion cuando es citologia
                        divCentroExtraccion.visible = false
                        if(tipoEstudio.ID_ESTUDIO_TIPO == Constantes.ID_TIPO_ESTUDIO_COTEST_1
                                && idProcesoTipo == Constantes.ID_PROCESO_SEGUIMIENTO){
                            divDatosCribIni.visible = true
                        }

						// Tipo 1 -> VPH / Tipo 7 -> VPH Estudio Piloto / Tipo 2 -> Citol Liq
						//Rellenamos valores de la vista dependiendo de lo que nos viene de bbdd
                        if(datosPrueba.VACUNACION_VPH != null && datosPrueba.VACUNACION_VPH == 1){
                            divTipoVacuna.visible = true
                        }
						if(datosPrueba.EMBARAZO_NUM != null) {
							prueba_embarazo.checked = true
						}
						if(datosPrueba.ANTEC_PATOL != null && !datosPrueba.ANTEC_PATOL.equals("")) {
							prueba_antecedentes_patologicos.checked= true
							divAntecedentes.visible = true
						}
						if(datosPrueba.CIRUGIAS_GINE_PREV != null && !datosPrueba.CIRUGIAS_GINE_PREV.equals("")) {
							prueba_cirugia_gine_previa.checked= true
							divCirugGinPrev.visible = true
						}
						if(datosPrueba.TER_HORM_TIPO != null && !datosPrueba.TER_HORM_TIPO.equals("")) {
							prueba_ter_hormonal.checked = true
							divTerapiaHorm.visible = true
						}
						if(datosPrueba.CRIB_ADEC_10 == 1)
							divResultNeg.visible = true
							
						if(datosPrueba.PAT_CERV_PREV == 1)
							divLesionCervical.visible = true
						
						if(datosPrueba.PAT_CERV_PREV_VPH == 1)
							divLesionTiempo.visible = true
						
						if(datosPrueba.PAT_CERV_PREV_20 == 1)
							divTipoLesion.visible = true
						
						if(datosPrueba.hall_gine_vulvitis || datosPrueba.hall_gine_hipo_atro || datosPrueba.hall_gine_dolor ||
							datosPrueba.hall_gine_eritroplasia || datosPrueba.hall_gine_leucorrea || datosPrueba.hall_gine_colpitis
                            || (!datosPrueba.hall_gine_otros.equals("") && datosPrueba.HALL_GINE_OTROS != null)) {
							divHallazGinecoDetalles.visible = true
							prueba_hallazgos_gine.checked = true
						}
												
						if(datosPrueba.HALL_GINE_OTROS != null && (!datosPrueba.HALL_GINE_OTROS.equals("") && datosPrueba.HALL_GINE_OTROS != null))
							prueba_otros.checked = true
						
						if(datosPrueba.HALLAZGOS_COLPOS != null & !datosPrueba.HALLAZGOS_COLPOS.equals("")) {
							prueba_colposcopia.checked = true
							divHallazColposDetalles.visible = true
						}
						
					}else{
						//Se ocultan los campos de datos paciente y datos gine
                        //y se visualiza el centro de extraccion
                        // si la prueba no es citologia liquida
						divDatosPaciente.visible = false
						divDatosGinec.visible = false
                        divCentroExtraccion.visible = true
                        //Si la prueba es de VPH y estamos en un perfil distinto al de matrona, se visualiza los campos
                        //de reenvío de petición electrónica
                        if(session.perfil != Constantes.SANITARIO_ASISTENCIAL){
                            //Recuperamos el valor de la variable global de los días para que la peticion caduque
                            int diasCaducidad = Integer.parseInt(pandoraService.leerVariableGlobal(Constantes.DIAS_CADUCIDAD_PETICION_VPH))
                            //Recuperamos los días transcurridos desde el envio de la última peticion, siempre que sea una prueba VPH, no Cotest, sin resultado en
                            //estudio y que previamente fue enviada ok
                            def diasDesdePet = pruebaEntidadService.recuperarPruebasVPHARCaducadas(datosPrueba.COD_PRODUCTO)?.diasDesdePet
                            //Si se recuperan los dias porque cumple criterios y estos días son mayores al max para su caducidad
                            //se muestra el boton de reenvío
                            if(diasDesdePet && (diasDesdePet > diasCaducidad)){
                                prueba_fReenvio.value = diasDesdePet
                                divReenvioPetElecAr.visible = true
                            }
                        }
					}
				}
			}
		}
	}
	
	@Listen("onClick=button#btnDescartarPruebaEst, #btnCerrarVentana")
	void clickDescartarCambiosPruebaEstudio() {
		log.debug("click Descartar cambios")
		// Ocultamos la ventana modal
		contenedor.visible = false
		descartarOperaciones()
		//Limpiamos el invocador para evitar el error de 'Cambios Pendientes'
		invocador = null
		// Limpiamos el contenido de la ventana para su próxima utilización
		limpiar()
	}

    @Listen("onClick=button#btnGuardarPruebaEstCostest")
    def clickGuardarPruebasCostest(){
        def resultado

        def valoresFormulario = [:]
        valoresFormulario << prueba.leerValoresVista()
        valoresFormulario.id_estudio = invocador.pruebaActual.id_estudio
        valoresFormulario.id_tipo_estudio = estudioEntidadService.recuperarTipoEstudioPorIdEstudio(prueba.parametros.id_estudio).ID_ESTUDIO_TIPO
        valoresFormulario.baseServicio = pruebaEntidadService
        valoresFormulario.baseEntidad = "pruebaEntidadService"
        valoresFormulario.id_profesional = session.idProfesional

        List idsPruebasCotest = [Constantes.ID_TIPO_PRUEBA_VPH, Constantes.ID_TIPO_PRUEBA_CITOLOGIA]
        prueba_id_tipo_prueba.selectedItem.value = idsPruebasCotest[1]
        def bNoduplicado = antesDeGuardar()
        if(bNoduplicado){
            resultado = [:]
            for(int i = 0; i< idsPruebasCotest.size(); i++){
                valoresFormulario.id_tipo_prueba = idsPruebasCotest[i]
                prueba_id_tipo_prueba.selectedItem.value = idsPruebasCotest[i]
                resultado = pruebaEntidadService.insertar(valoresFormulario)
                if(resultado.errores){
                    resultado.errores.each { k, v ->
                        def componente = catalogoComponentes."prueba_${k}"?.componente
                        if(Components.isRealVisible(componente)){
                            ventanaError(v)
                        }
                        log.debug("componente")
                    }
                    return
                }
            }
            descartarOperaciones()
            despuesDeGuardar()
        }else{
            resultado = [errores: 'El código de producto ya fue usado anteriormente']
        }
        return resultado
    }

	@Listen("onSelect=#prueba_id_tipo_prueba")
	void tipoPruebaChange(Event evento){
		//Dependiendo del tipo de Prueba, habilitamos unos campos u otros
		def lb = evento.target
		def idTipoPrueba
		lb.children.each { item ->
			if(item.selected)
				idTipoPrueba = item.value
		}
		//Damos valor por defecto a la Fecha de Toma de la Muestra, día actual (modificable)
		Date fechaActual = new Date().clearTime()
		escribirValorComponente(prueba_fec_muestra, fechaActual)
		//Ocultamos la tabla de Analitos puesto que esta info nos la enviar de Laboratorio
		divAnalitos.visible = false
		
		//Dependiendo del Tipo de Prueba, activamos ciertos campos
		// Id == 1 -> VPH  / 2 -> Citología Líquida
		if(idTipoPrueba == Constantes.ID_TIPO_PRUEBA_VPH) {
			prueba_cod_producto.disabled = false
			prueba_fec_muestra.disabled= false
			//Ocultamos los campos que no pertenecen a este tipo de prueba
			divDatosPaciente.visible = false
			divDatosGinec.visible = false
		}
		else if(idTipoPrueba == Constantes.ID_TIPO_PRUEBA_CITOLOGIA) {
			activarComponentesVista("prueba")
			//Deshabilitamos los campos que se generan automaticamente
			prueba_id.disabled= true
			prueba_fec_resultado.disabled= true
			prueba_valor_resultado.disabled = true
            prueba_centro_extraccion.disabled = true

			//Mostramos los campos a rellenar en este tipo de prueba
			divDatosPaciente.visible = true
			divDatosGinec.visible = true
			//Deshabilitamos los campos que solo se activan al marcar ciertos campos
			prueba_menop_edad_ini.disabled = true
			prueba_embarazo_num.disabled= true
			prueba_hall_gine_otros.disabled = true
		}
		else {
			//Ocultamos los campos que no pertenecen a este tipo de prueba
			divDatosPaciente.visible = false
			divDatosGinec.visible = false
		}
	}

	@Listen("onCheck=#prueba_embarazo")
	def checkEmbarazos() {
		if(prueba_embarazo.checked) {
			prueba_embarazo_num.disabled = false
		}else {
			prueba_embarazo_num.disabled = true
			limpiarValorComponente(prueba_embarazo_num)
		}
	}

	@Listen("onSelect=#prueba_menopausia")
	def selectMenopausia() {
		if(leerValorComponente(prueba_menopausia).equals("1")) {
			prueba_menop_edad_ini.disabled = false
		}else {
			prueba_menop_edad_ini.disabled = true
			limpiarValorComponente(prueba_menop_edad_ini)
		}
	}

	@Listen("onCheck=#prueba_antecedentes_patologicos") 
	def checkAntecPatologicos() {
		if(prueba_antecedentes_patologicos.checked)
			divAntecedentes.visible = true
		else {
			divAntecedentes.visible = false
			limpiarValorComponente(prueba_antec_patol)
		}
	}

	@Listen("onCheck=#prueba_cirugia_gine_previa")
	def checkCirugPrev() {
		if(prueba_cirugia_gine_previa.checked)
			divCirugGinPrev.visible = true
		else {
			divCirugGinPrev.visible = false
			limpiarValorComponente(prueba_cirugias_gine_prev)
		}
	}

	@Listen("onCheck=#prueba_ter_hormonal")
	def checkTerapiaHormonal() {
		if(prueba_ter_hormonal.checked) {
			divTerapiaHorm.visible = true
		}else {
			divTerapiaHorm.visible = false
			limpiarValorComponente(prueba_ter_horm_tipo)
			limpiarValorComponente(prueba_ter_horm_duracion)
		}
	}
	
	@Listen("onSelect=#prueba_crib_adec_10")
	def clickCribadoAdecuado() {
		if(leerValorComponente(prueba_crib_adec_10) == "1")
			divResultNeg.visible = true
		else{
			divResultNeg.visible = false
			limpiarValorComponente(prueba_crib_adec_10_negat)
		}
	}
	
	@Listen("onSelect=#prueba_pat_cerv_prev")
	def clickLesionCervical() {
		if(leerValorComponente(prueba_pat_cerv_prev) == "1")
			divLesionCervical.visible = true
		else {
			divLesionCervical.visible = false
			limpiarValorComponente(prueba_pat_cerv_prev_vph)
			limpiarValorComponente(prueba_pat_cerv_prev_20)
			limpiarValorComponente(prueba_pat_cerv_prev_tipocin)
		}
	}
	
	@Listen("onSelect=#prueba_pat_cerv_prev_vph")
	def clickLesionCervicalVPH() {
		if(leerValorComponente(prueba_pat_cerv_prev_vph) == "1") {
			divLesionTiempo.visible = true
		}else{			
			divLesionTiempo.visible = false
			divTipoLesion.visible= false //ocultamos los hijos, por si rectifican
			limpiarValorComponente(prueba_pat_cerv_prev_20)
			limpiarValorComponente(prueba_pat_cerv_prev_tipocin)
		}
	}
	
	@Listen("onSelect=#prueba_pat_cerv_prev_20")
	def clickLesionTiempo() {
		if(leerValorComponente(prueba_pat_cerv_prev_20) == "1")
			divTipoLesion.visible = true
		else {
			divTipoLesion.visible = false
			limpiarValorComponente(prueba_pat_cerv_prev_tipocin)
		}
	}

	@Listen("onSelect=#prueba_vacunacion_vph")
	def clickVacunacionVph() {
		if(leerValorComponente(prueba_vacunacion_vph) == "1")
			divTipoVacuna.visible = true
		else {
			divTipoVacuna.visible = false
			limpiarValorComponente(prueba_id_tipo_vacuna)
			limpiarValorComponente(prueba_dosis_recibidas)
			limpiarValorComponente(prueba_edad_prim_dosis)
			limpiarValorComponente(prueba_fec_ult_dosis)
		}
	}

	@Listen("onCheck=#prueba_hallazgos_gine")
	def checkHallazgosGinecologicos() {
		if(prueba_hallazgos_gine.checked)
			divHallazGinecoDetalles.visible = true
		else {
			divHallazGinecoDetalles.visible = false
			limpiarValorComponente(prueba_hall_gine_vulvitis)
			limpiarValorComponente(prueba_hall_gine_hipo_atro) 
			limpiarValorComponente(prueba_hall_gine_dolor)
			limpiarValorComponente(prueba_hall_gine_eritroplasia)
			limpiarValorComponente(prueba_hall_gine_leucorrea)
			limpiarValorComponente(prueba_hall_gine_colpitis)
			limpiarValorComponente(prueba_otros)
			limpiarValorComponente(prueba_hall_gine_otros)
		}

	}
	
	@Listen("onCheck=#prueba_otros")
	def checkOtrosHallazgosGinecologicos() {
		if(prueba_otros.checked)
			prueba_hall_gine_otros.disabled = false
		else {
			prueba_hall_gine_otros.disabled = true
			limpiarValorComponente(prueba_hall_gine_otros)
		}
	}

	@Listen("onCheck=#prueba_colposcopia")
	def checkColposcopia() {
		if(prueba_colposcopia.checked) {
			divHallazColposDetalles.visible = true
		}else {
			divHallazColposDetalles.visible = false
			limpiarValorComponente(prueba_hallazgos_colpos)
		}
	}

	//Metodo que rellena el listbox con las vacunas para el germen del VPH y que no tiene en cuenta el ID para poder incluir
	//el registro de "Desconocido" con el ID = null
	def rellenarListboxTipoVacuna(Listbox listbox, datos, String textoSeleccion, String campoDescripcion="descripcion", String campoId="id"){
		// Quitamos los items que pudiera haber previamente en el listbox
		listbox.items.clear()

		// Utilizamos el builder de ZK para añadir los listitems programáticamente sin tener en cuenta que el
		//ID sea distinto de null para poder agregar datos a los resultado de la consulta
		listbox.append {
			listitem(value: null, label: textoSeleccion)
			datos.each { e ->
				assert e."${campoDescripcion}" != null
				listitem(value: e."${campoId}", label: e."${campoDescripcion}")
			}
		}
		// Después de rellenar el listbox, ponemos cono seleccionado el item del texto de selección
		listbox.selectedIndex = 0
	}

	void move(Listbox src, Listbox dst) {
		Listitem s = src.getSelectedItem();
		if (s == null) {
			Messagebox.show("Select an item first");
		} else {
			s.setParent(dst);
		}
	}

    //Metodo que comprueba si la id de producto esta duplicada al hacer una nueva inserción
	boolean antesDeGuardar(){
		//Recuperamos el id de la prueba
		def idPruebaActual = invocador.pruebaActual.id_prueba
		//Declaramos una variable booleana que será lo que retornemos en este metodo
		boolean bConfirmarGuardar = true
		//Recogemos el tipo de prueba
		def tipoPrueba = prueba_id_tipo_prueba.selectedItem.value
		//Recogemos el codigo de producto  del valor leido del formulario
		def codProdInsertado = leerValorComponente(prueba_cod_producto)
		//comprobamos si es una insercion nueva o no
		if(idPruebaActual == NUEVO_REGISTRO){
            def tipoEstudio = estudioEntidadService.recuperarTipoEstudioPorIdEstudio(invocador.pruebaActual.id_estudio)
			boolean bEnUso = pruebaEntidadService.existeCodigoProducto(codProdInsertado,tipoPrueba, tipoEstudio)
			//Si hay coincidenicia, la variable booleana será falsa y mostrará un mensaje de error
			if(bEnUso) {
				ventanaError("Este código de producto ha sido usado en otra prueba aún no resuelta.")
				bConfirmarGuardar = false
			}
		}
		//devolvemos el valor de la variable booelana
		return bConfirmarGuardar
	}
	
	void despuesDeGuardar() {
        if(invocador) {
			Map expediente = [:]
			expediente.id_expediente = invocador.pruebaActual.id_expediente
			// Ocultamos la ventana modal
			contenedor.visible = false
            // Recuperamos la info actualizada del expediente
            invocador.activacion()
			//Limpiamos el invocador para evitar el error de 'Cambios Pendientes'
			invocador = null
			// Limpiamos el contenido de la ventana para su próxima utilización
			limpiar()
		}
	}

    def mostrarCribadoPrevioCotest(id_proceso){
        def datosCribadoPrevVPH = pruebaEntidadService.recuperarDatosCribadoPrevCotest(id_proceso, Constantes.ID_TIPO_PRUEBA_VPH)
        if(datosCribadoPrevVPH){
            escribirValorComponente(prueba_resultadoVph, datosCribadoPrevVPH.resultado)
            escribirValorComponente(prueba_fMuestraVph,datosCribadoPrevVPH.fecha_toma)
            escribirValorComponente(prueba_fResultadoVph,datosCribadoPrevVPH.fecha_resultado)
            escribirValorComponente(prueba_genotiposVph,datosCribadoPrevVPH.genotipos)
        }
        def datosCribadoPrevCito = pruebaEntidadService.recuperarDatosCribadoPrevCotest(id_proceso, Constantes.ID_TIPO_PRUEBA_CITOLOGIA)
        if(datosCribadoPrevCito){
            escribirValorComponente(prueba_resultadoCito, datosCribadoPrevCito.resultado)
            escribirValorComponente(prueba_fMuestraCito,datosCribadoPrevCito.fecha_toma)
            escribirValorComponente(prueba_fResultadoCito,datosCribadoPrevCito.fecha_resultado)
            escribirValorComponente(prueba_genotiposCito,datosCribadoPrevCito.genotipos)
        }
        prueba_resultadoVph.disabled = true
        prueba_genotiposVph.disabled = true
        prueba_fMuestraVph.disabled = true
        prueba_fResultadoVph.disabled = true
        prueba_resultadoCito.disabled = true
        prueba_genotiposCito.disabled = true
        prueba_fMuestraCito.disabled = true
        prueba_fResultadoCito.disabled = true
    }

    def clicReenvioPetElecAr() {
        def datos = [:]
        datos.cod_producto = leerValorComponente(prueba_cod_producto).toString()
        datos.login = session.loginUnico
        //Pedimos confirmacion al usuario para reenvio de peticion, si confirma
        //se hace una actualizacion de la tabla Peticion muestra para que el job la reenvie
        // con fecha de hoy, y se inserta un registro en la tabla de auditoria
        Messagebox.show("¿Confirma que desea reenviar la petición electrónica?",
                "Confirmar Acción", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, [
                onEvent: { e ->
                    if (e.data.intValue() == Messagebox.YES) {
                        //Por limitaciones de tiempo, se usan los metodos de esta servicio en lugar
                        //de refactorizar a uno más generico
                        dispensacionesFarmaciaService.reenviarDispensacion(datos)
                        dispensacionesFarmaciaService.auditoriaReenviarDispensacion(datos)
                        //Asignamos el valor cero a los días
                        prueba_fReenvio.value = 0
                        //Deshabilitamos el boton
                        btnReenvioPetElecAr.disabled = true
                    }
                }
        ] as org.zkoss.zk.ui.event.EventListener)
    }
	
	void limpiar() {
		// Limpiamos todos los componentes de la ventana
		limpiarValorComponente(prueba_id)
		if(!prueba_id_tipo_prueba.getItems().isEmpty())
			limpiarValorComponente(prueba_id_tipo_prueba)
		limpiarValorComponente(prueba_cod_producto)
		limpiarValorComponente(prueba_fec_resultado)
		limpiarValorComponente(prueba_valor_resultado)
		limpiarValorComponente(prueba_centro_extraccion)
		limpiarValorComponente(prueba_fec_muestra)
		if(!prueba_fm.getItems().isEmpty())
			limpiarValorComponente(prueba_fm)
		limpiarValorComponente(prueba_fec_ult_regla)
		limpiarValorComponente(prueba_embarazo)
		limpiarValorComponente(prueba_embarazo_num)
		limpiarValorComponente(prueba_menopausia)
		limpiarValorComponente(prueba_menop_edad_ini)
		limpiarValorComponente(prueba_antecedentes_patologicos)
		limpiarValorComponente(prueba_antec_patol)
		limpiarValorComponente(prueba_cirugia_gine_previa)
		limpiarValorComponente(prueba_cirugias_gine_prev)
		limpiarValorComponente(prueba_ter_hormonal)
		limpiarValorComponente(prueba_ter_horm_tipo)
		limpiarValorComponente(prueba_ter_horm_duracion)
		if(!prueba_crib_adec_10.getItems().isEmpty())
			limpiarValorComponente(prueba_crib_adec_10)
		if(!prueba_crib_adec_10_negat.getItems().isEmpty())
			limpiarValorComponente(prueba_crib_adec_10_negat)
		if(!prueba_pat_cerv_prev.getItems().isEmpty())
			limpiarValorComponente(prueba_pat_cerv_prev)
		if(!prueba_pat_cerv_prev_vph.getItems().isEmpty())
			limpiarValorComponente(prueba_pat_cerv_prev_vph)
		if(!prueba_pat_cerv_prev_20.getItems().isEmpty())
			limpiarValorComponente(prueba_pat_cerv_prev_20)
		if(!prueba_pat_cerv_prev_tipocin.getItems().isEmpty())
			limpiarValorComponente(prueba_pat_cerv_prev_tipocin)
		if(!prueba_vacunacion_vph.getItems().isEmpty())
			limpiarValorComponente(prueba_vacunacion_vph)
		if(!prueba_id_tipo_vacuna.getItems().isEmpty())
			limpiarValorComponente(prueba_id_tipo_vacuna)
		if(!prueba_dosis_recibidas.getItems().isEmpty())
			limpiarValorComponente(prueba_dosis_recibidas)
		limpiarValorComponente(prueba_edad_prim_dosis)
		limpiarValorComponente(prueba_fec_ult_dosis)
		limpiarValorComponente(prueba_anovul_orales)
		limpiarValorComponente(prueba_quimioterapia)
		limpiarValorComponente(prueba_radioterapia)
		limpiarValorComponente(prueba_gestante)
		limpiarValorComponente(prueba_puerperio)
		limpiarValorComponente(prueba_diu)
		limpiarValorComponente(prueba_otros_tratam)
		limpiarValorComponente(prueba_observaciones)
		limpiarValorComponente(prueba_hallazgos_gine)
		limpiarValorComponente(prueba_hall_gine_vulvitis)
		limpiarValorComponente(prueba_hall_gine_colpitis)
		limpiarValorComponente(prueba_hall_gine_leucorrea)
		limpiarValorComponente(prueba_hall_gine_eritroplasia)
		limpiarValorComponente(prueba_hall_gine_hipo_atro)
		limpiarValorComponente(prueba_hall_gine_dolor)
		limpiarValorComponente(prueba_hall_gine_otros)
		limpiarValorComponente(prueba_hallazgos_colpos)
        limpiarValorComponente(prueba_fReenvio)
		divAntecedentes.visible= false
		divCirugGinPrev.visible = false
		divTerapiaHorm.visible = false
		divResultNeg.visible= false
		divLesionCervical.visible = false
		divTipoVacuna.visible = false
		divLesionTiempo.visible = false
		divHallazGinecoDetalles.visible = false
		divHallazColposDetalles.visible = false
        divReenvioPetElecAr.visible = false
	}

}
