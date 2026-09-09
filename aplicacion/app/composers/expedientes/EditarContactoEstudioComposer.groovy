package  cribadocervix.expedientes


import org.springframework.stereotype.Component
import org.zkoss.zk.ui.select.annotation.Listen
import org.zkoss.zul.Button
import org.zkoss.zul.Datebox
import org.zkoss.zul.Div
import org.zkoss.zul.Label
import org.zkoss.zul.Listbox
import org.zkoss.zul.Messagebox
import org.zkoss.zul.Textbox
import base.EntidadFormulario
import base.FormularioComposer
import  jade.PandoraService
import  jade.PersanService
import  cribadocervix.ExpedienteService
import  cribadocervix.UtilsPcacervixService
import  cribadocervix.entidad.ContactoEntidadService
import  cribadocervix.entidad.DocumentoEntidadService
import  cribadocervix.entidad.EstudioEntidadService
import  cribadocervix.entidad.ExpedienteEntidadService
import  cribadocervix.entidad.ProcesoEntidadService
import  cribadocervix.entidad.VariablesGlobalesEntidadService
import groovy.util.logging.Slf4j
import  cribadocervix.Constantes
import org.zkoss.zk.ui.event.Event


@Slf4j
@Component
class EditarContactoEstudioComposer extends FormularioComposer{

	//Servicios
	ExpedienteService expedienteService
	ExpedienteEntidadService expedienteEntidadService
	ProcesoEntidadService procesoEntidadService
	EstudioEntidadService estudioEntidadService
	ContactoEntidadService contactoEntidadService
    UtilsPcacervixService utilsPcacervixService
    VariablesGlobalesEntidadService variablesGlobalesEntidadService
    DocumentoEntidadService documentoEntidadService
	PandoraService pandoraService
	PersanService persanService
	EntidadFormulario contacto

	ExpedienteComposer invocador

	//Parametros del filtro de busqueda
    Label identifPacienteContacto
	Textbox contacto_id
	Textbox contacto_id_estudio
	Textbox contacto_contador
	Listbox contacto_id_motivo_contacto
	Textbox contacto_valor_medio_comunicacion
	Listbox lbmedios_comunicacion
	Datebox contacto_fecha
	Listbox contacto_contactado
	Textbox contacto_id_profesional
	Textbox contacto_nombre_profesional
	Textbox contacto_observaciones
	Div div_textbox_MedioComun
	Div div_lb_medios_comun

	Label tituloContactoExp

	//Variables
	def idPadre
	def idEstudio
	def idExpediente
    def eventoContCitaCotest

	//Botones
	Button btnGuardarContactoEst
	Button btnDescartarContactoEst
	Button btnCerrarVentanaContacto
	Button btnEditarContacto

	def despuesDeComponer() {

		contacto = new EntidadFormulario("contacto","contacto_id", this)

		// Configuramos la entidad
		contacto.configurar("pcacervix", "registro_contacto")
		// Recuperamos el id de profesional y su unidad funcional seleccionada del usuario actual
		def idProfesional = session.idProfesional
		def idUnidadFuncional = session.unidadesFuncionales[0].id
		def login = session.loginUnico
		// Establecemos los parámetros por defecto para todas las operaciones de entidades
		def parametros = [id_profesional: idProfesional, id_unidad_funcional: idUnidadFuncional, login: login]
		contacto.parametros = parametros
		// Establecer el servicio que va a gestionar la lista de operaciones
		servicioOperaciones = contactoEntidadService

		establecerBotonGuardar(btnGuardarContactoEst)
		establecerBotonDescartar(btnDescartarContactoEst)
	}

	def activacion() {

		log.debug("procesando ACTIVACION EditarContactosEstudioComposer")
		limpiar()

		contacto.activarComponentesVista()
		btnDescartarContactoEst.disabled = false
		btnGuardarContactoEst.disabled = true

		//Desactivamos el ID
//		contacto_id.disabled = true

		//Rellenamos los valores de los Desplegables
        Map datos = [:]
        datos.id_proyecto = Constantes.ID_PROYECTO
		def contactoMotivo = utilsPcacervixService.recuperarMotivosContactoPorProyecto(datos)
		rellenarListbox(contacto_id_motivo_contacto, contactoMotivo, "Seleccione..." )

		if (invocador) {
            idExpediente = invocador.contactoActual.id_expediente

            Date fecNac = Date.parse('dd/MM/yyyy', invocador.contactoActual.fec_nacim)
            def edad = persanService.calcularEdadPersona(fecNac)
            //Pintamos identificación del paciente (solicitado por funcional)
            def nombrePaciente = "Paciente: " + invocador.contactoActual.nombre_paciente + " " + invocador.contactoActual.apellido1_paciente +
            " " + invocador.contactoActual.apellido2_paciente + " (" + edad + " años)"
            escribirValorComponente(identifPacienteContacto, nombrePaciente)

            eventoContCitaCotest = true

			if (invocador.contactoActual.id_contacto == NUEVO_REGISTRO) {
				//Hacemos visibles los botones Guardar y Descartar
				btnGuardarContactoEst.visible = true
				btnDescartarContactoEst.visible = true
				//Ocultamos el boton Cerrar Ventana, puesto que ya lo hace el de Descartar
				btnCerrarVentanaContacto.visible = false
				//Ocultamos el botón Editar
				btnEditarContacto.visible = false
				escribirValorComponente(tituloContactoExp, invocador.contactoActual.titulo)

                //Recuperamos el ultimo proceso del expediente
                def procesoActual = procesoEntidadService.recuperarUltimoProcesoPorExpediente(idExpediente)
                //Recuperamos el ultimo estudio del expediente
                def estudioActual = estudioEntidadService.recuperar([id_estudio:estudioEntidadService.recuperarUltimoEstudioPorIdExpediente(idExpediente).ID])
                def estudioContacto = null
                //Comprobamos si el proceso tiene proceso padre
                if (procesoActual.ID_PADRE != -1) {
                    /* Buscamos su proceso padre, y comprobamos si el ultimo estudio tiene su respectivo registro de contacto */
                    def procesoPadre = procesoEntidadService.recuperarUltimoProcesoCerradoPorExpediente(idExpediente)
                    /*Buscamos los datos del último estudio cerrado del proceso recuperado y lo asiganmos a la variable*/
                    /* NO tenemos en cuenta los resultados de citologia positivo leve (SIN MENOPAUSIA) cuyo resultado se comunica por carta cuando el estudio es de tipo Citologia*/
                    estudioContacto = estudioEntidadService.recuperarUltimoEstudioCerradoPorIdProceso(procesoPadre)
                }
                //Si el estudio del proceso padre esta cerrado y tiene todos los contactos correspondientes, buscamos el estudio correspondiente al proceso Actual
                if (estudioContacto == null) {
                    if((estudioActual.ID_ESTUDIO_TIPO  == Constantes.ID_TIPO_ESTUDIO_COTEST_1 ||
                            estudioActual.ID_ESTUDIO_TIPO  == Constantes.ID_TIPO_ESTUDIO_COTEST_2 ) && estudioActual.ID_ESTUDIO_RESULTADO_TIPO == null){
                        //En el caso de los estudios cotest o citologia que no están cerrados, se asigna su id de estudio para registrar el contacto de CITA
                        estudioContacto = estudioActual
                    }else{
                        //Si no es cotest, se asigan el is del ultimo estudio cerrado del proceso actual
                        estudioContacto = estudioEntidadService.recuperarUltimoEstudioCerradoPorIdProceso(procesoActual)
                    }
                }

                //Declaramos el motivo de contacto para que aparezca automaticamente
                def motivoContacto
                /*Preseleccionamos el motivo de contacto que le corresponde
                 Comprobamos que estudio exista con las condiciones anteriores, sino buscamos el ultimo estudio del expediente*/
                if (estudioContacto && (estudioContacto.ID_ESTUDIO_TIPO  == Constantes.ID_TIPO_ESTUDIO_VPH ||
                                estudioContacto.ID_ESTUDIO_TIPO  == Constantes.ID_TIPO_ESTUDIO_ESTUDIO_PILOTO)) {
                    motivoContacto = utilsPcacervixService.recuperarMotivoContactoPorCodigo(Constantes.MOTIVO_CON_RESULT_VPH)
                }else if (estudioContacto && estudioContacto.ID_ESTUDIO_TIPO  == Constantes.ID_TIPO_ESTUDIO_CITOLOGIA) {
                        motivoContacto = utilsPcacervixService.recuperarMotivoContactoPorCodigo(Constantes.MOTIVO_CON_RESULT_CITO)
                }else if (estudioContacto && (estudioContacto.ID_ESTUDIO_TIPO  == Constantes.ID_TIPO_ESTUDIO_COTEST_1 ||
                                    estudioContacto.ID_ESTUDIO_TIPO  == Constantes.ID_TIPO_ESTUDIO_COTEST_2 )) {
                    //Si el estudio no está resuelto se asigna el motivo contacto de Cita
                    if(estudioContacto.ID_ESTUDIO_RESULTADO_TIPO == null){
                        motivoContacto = utilsPcacervixService.recuperarMotivoContactoPorCodigo(Constantes.MOTIVO_CON_CITA_COTEST)
                    }else{
                        //Si el estudio está resuelto se asigna el motivo contacto de resultado cotest
                        motivoContacto = utilsPcacervixService.recuperarMotivoContactoPorCodigo(Constantes.MOTIVO_CON_RESULT_COTEST)
                    }
                }
                else {
                    //Si no es ninguno de los anteriores, seleccionamos otros Motivos
                    motivoContacto = utilsPcacervixService.recuperarMotivoContactoPorCodigo(Constantes.MOTIVO_CON_OTROS)
                    estudioContacto = estudioEntidadService.recuperarUltimoEstudioPorIdExpediente(idExpediente)
                }
                
                /*Comprobamos si ha cumplido con el maximo de registros de contacto de Comunicacion de Resultado por estudioContacto
                   en cuyo caso, no dejamos insertar mas contactos del tipo Comunicación Resultado */
                Map datosContacto = [:]
                datosContacto.id_estudio = estudioContacto.ID
                datosContacto.motivo_contacto = motivoContacto.ID
                def contadorReg = comprobarNumRegContacto(datosContacto)
                def contactoMotivoOtros = [:]
                if (contadorReg.NUM == Constantes.NUM_MAX_CONTACTOS) {
                    contactoMotivoOtros = utilsPcacervixService.recuperarMotivoContactoPorCodigo(Constantes.MOTIVO_CON_OTROS)
                    ArrayList listMotivos = new ArrayList()
                    listMotivos.add(contactoMotivoOtros)
                    rellenarListbox(contacto_id_motivo_contacto, listMotivos, "Seleccione..." )
                    escribirValorComponente(contacto_id_motivo_contacto, contactoMotivoOtros.ID)
                }else {
                    //Preseleccionamos el motivo
                    escribirValorComponente(contacto_id_motivo_contacto, motivoContacto.ID)
                }
               
				//Crear mapa con datos por defecto
				Map m = [:]
				m.id = ""
				m.fecha = new Date()
				m.id_estudio = estudioContacto.ID
                m.id_motivo_contacto = motivoContacto.ID
				m.id_profesional = contacto.parametros.id_profesional
				contacto.insertar(m)
				
				//Recuperamos los medios de comunicacion de la Persona con su ID
				def idPersona = invocador.contactoActual.id_persona
				def mediosComunicacion = persanService.buscarMediosComunicacionPersona(idPersona)
				lbmedios_comunicacion.actualizar(mediosComunicacion)
				
				def nombreProfesional = pandoraService.recuperaNombreProfesional(session.idProfesional)
				escribirValorComponente(contacto_nombre_profesional, nombreProfesional)
				escribirValorComponente(contacto_contactado, "Seleccionar..")
				
				div_lb_medios_comun.visible = true
				div_textbox_MedioComun.visible = false
                contacto_nombre_profesional.disabled = true
                contacto_id_estudio.disabled = true
				lbmedios_comunicacion.disabled = true
				
			}else {
				btnGuardarContactoEst.visible = false
				btnDescartarContactoEst.visible = false
				btnCerrarVentanaContacto.visible = true
				btnEditarContacto.visible = true
				
				def datosContacto = contacto.recuperar(invocador.contactoActual)
				escribirValoresVista("contacto", datosContacto)
				escribirValorComponente(tituloContactoExp, invocador.contactoActual.titulo)
				def nombreProfesional = pandoraService.recuperaNombreProfesional(datosContacto.ID_PROFESIONAL)
				escribirValorComponente(contacto_nombre_profesional, nombreProfesional)
				contacto_nombre_profesional.disabled = true
				
				if (null != datosContacto.ID_MOTIVO_CONTACTO) {
					escribirValorComponente(contacto_id_motivo_contacto, datosContacto.ID_MOTIVO_CONTACTO)
				}
				div_textbox_MedioComun.visible = true
				div_lb_medios_comun.visible = false
				desactivarComponentesVista("contacto")
			}
		}
	}
	
	def desactivacion() {
	}
	
	
//	@Listen("onSelect=#contacto_id_motivo_contacto")
//	def seleccionMotivoContacto(){
//		activarComponentesVista("contacto")
//		lbmedios_comunicacion.disabled = false
//		def motivo = contacto_id_motivo_contacto.selectedItem.value
//		def proceso = procesoEntidadService.recuperarUltimoProcesoPorExpediente(invocador.contactoActual.id_expediente)
//		def idEstudio
//		
//        //Calculamos el id_estudio al que tienen que ir asociado el contacto, segun el motivo de contacto seleccionado
//		if(motivo == Constantes.ID_CONTACTO_RESULT_VPH) {
//			//Los VPH +16/18 generan un nuevo proceso de Seguimiento con estudio de Gine (id_proceso_tipo = 5), por tanto, recuperamos el ultimo estudio de VPH del proceso anterior al actual (idPadre)
//			// Sino, recuperamos el ultimo estudio del mismo proceso
//			if(proceso.ID_PROCESO_TIPO == Constantes.ID_PROCESO_SEGUIMIENTO) {
//				idEstudio = estudioEntidadService.recuperarUltimoEstudioVPHPorIdProceso(proceso.ID_PADRE)
//			}else
//				idEstudio = estudioEntidadService.recuperarUltimoEstudioVPHPorIdProceso(proceso.ID)
//				
//		}else if(motivo == Constantes.ID_CONTACTO_RESULT_CITO) {
//			// Las citologías +Leve, generan un nuevo proceso de Seguimiento con estudio de Gine (id_proceso_tipo = 5), por tanto, recuperamos el ultimo estudio de Cito del proceso anterior al actual (idPadre)
//			// Sino, recuperamos el ultimo estudio del mismo proceso
//            /* TO-DO: las citos +Leve se comunican por carta, no es necesario asociar contacto para este tipo. Se podría quitar esta condición.
//               Por el momento, la dejamos hasta que esté confirmado que NO la comunican por telefono */ 
//			if(proceso.ID_PROCESO_TIPO == Constantes.ID_PROCESO_SEGUIMIENTO) {
//				idEstudio = estudioEntidadService.recuperarUltimoEstudioCitoPorIdProceso(proceso.ID_PADRE)
//			}else
//				idEstudio = estudioEntidadService.recuperarUltimoEstudioCitoPorIdProceso(proceso.ID)
//				
//		}else // Cualquier otro motivo, se asocia al estudio actual
//			idEstudio = estudioEntidadService.recuperarUltimoEstudioPorIdExpediente(idExpediente)
//		
//		// Para los casos en que, por error, se seleccione un motivo de contacto que debe ir asociado a un estudio que aun no existe en este proceso.
//		// Setteamos idEstudio = 0, y al guardar validamos que no sea = 0, en cuyo caso no permitimos la insercción y obligamos a revisar el Motivo de Contacto
//		// Ejemplo: por error se selecciona Comunicacion Resultado Citologia, para comunicar un VPH +
//		// Para asociar el contacto al estudio, la consulta buscaría el ultimo estudio de Cito finalizado con resultado, no encontraría nada puesto que el estudio finalizado con resultado sería de VPH 
//		if(idEstudio == null) 
//			idEstudio = [0]
//		
//		escribirValorComponente(contacto_id_estudio, idEstudio[0])
//	}
	
	@Listen("onClick=button#btnEditarContacto")
	void clickEditarContactoEstudio() {
		log.debug("click Editar Contacto")
		def titulo = "Editar Contacto"
		escribirValorComponente(tituloContactoExp, titulo)
		btnGuardarContactoEst.visible = true 
		btnDescartarContactoEst.visible = true
		activarComponentesVista("contacto")
		def idPersona = invocador.contactoActual.id_persona
		def mediosComunicacion = persanService.buscarMediosComunicacionPersona(idPersona)
		lbmedios_comunicacion.actualizar(mediosComunicacion)
		def medioComSelected = leerValorComponente(contacto_valor_medio_comunicacion)
		def indexMediosCom
		def listMediosCom = leerValorComponente(lbmedios_comunicacion)
		indexMediosCom = listMediosCom?.findIndexOf { 
			it.valor == medioComSelected
		}
		lbmedios_comunicacion.selectedIndex = indexMediosCom
		// Activamos el listbox para poder elegir un medio de comunicación
		div_lb_medios_comun.visible = true
		div_textbox_MedioComun.visible = false
		//Modificamos la fecha a la actual
		escribirValorComponente(contacto_fecha, new Date())
		btnEditarContacto.visible = false
		contacto_nombre_profesional.disabled = true
        contacto_id_estudio.disabled = true
    }

	public boolean antesDeGuardar() {
        //Se comprueban los campos obligatorios del formulario
        boolean comprobado = false
		if (lbmedios_comunicacion.selectedItem.iterator().size() == 0) {
			ventanaAviso("Debe seleccionar el campo 'Medio de Comunicación'")
            comprobado = false
		}else {
            def valor_medio_comu = lbmedios_comunicacion.selectedItem.value
            contacto_valor_medio_comunicacion.value = valor_medio_comu
            if(contacto_id_motivo_contacto.selectedItem.value == null){
                ventanaAviso("Debe seleccionar el campo 'Motivo de Contacto'")
                comprobado = false
            }else{
                if(contacto_contactado.selectedItem.value == null){
                    ventanaAviso("Debe indicar si se ha contactado")
                    comprobado = false
                }else{
                    if(!eventoContCitaCotest){
                        comprobado = false
                    }else{
                        comprobado = true
                    }
                }
            }
        }
        return comprobado
	}

    def generaDatosNoContactado(){
        //Creamos un mapa para alojar los datos y devolverlos al llamar a este metodo
        Map datos = [:]
        datos.id_estudio = leerValorComponente(contacto_id_estudio)
        datos.motivo_contacto = leerValorComponente(contacto_id_motivo_contacto)
        datos.id_profesional = contacto.parametros.id_profesional
        datos.id_tipo_notif = Constantes.ID_TIPO_NOTIF_CARTA
        datos.id_emision = null
        datos.nombre_variable = null

        //Recuperamos el motivo de contacto para asignar el valor de la variable de documento a generar
        def idMotivo = utilsPcacervixService.recuperarMotivoContactoPorId(datos.motivo_contacto)
        if (idMotivo.CODIGO == Constantes.MOTIVO_CON_RESULT_VPH) {
            def estudio = estudioEntidadService.recuperar(datos)
            if (estudio.ID_ESTUDIO_RESULTADO_TIPO == Constantes.ID_RESULTADO_ESTUDIO_VPH_POS_1 ||
                    estudio.ID_ESTUDIO_RESULTADO_TIPO == Constantes.ID_RESULTADO_ESTUDIO_VPH_POS_2) {
                datos.nombre_variable = Constantes.DOC_ILOCALIZABLE
            }
        }else if (idMotivo.CODIGO == Constantes.MOTIVO_CON_RESULT_CITO) {
            def estudio = estudioEntidadService.recuperar(datos)
            if (estudio.ID_ESTUDIO_RESULTADO_TIPO == Constantes.ID_RESULTADO_ESTUDIO_CITO_INSAT) {
//                    datos.nombre_variable = Constantes.
            }else if (estudio.ID_ESTUDIO_RESULTADO_TIPO == Constantes.ID_RESULTADO_ESTUDIO_CITO_POS_LEVE) {
//                    datos.nombre_variable = Constantes.
            }else if (estudio.ID_ESTUDIO_RESULTADO_TIPO == Constantes.ID_RESULTADO_ESTUDIO_CITO_POS_GRAVE) {
//                    datos.nombre_variable = Constantes.
            }
        }else if (idMotivo.CODIGO == Constantes.MOTIVO_CON_RESULT_COTEST) {
//                    datos.nombre_variable = Constantes.
        }else if(idMotivo.CODIGO == Constantes.MOTIVO_CON_CITA_COTEST || idMotivo.CODIGO == Constantes.MOTIVO_CON_RECORD_CITA){
            datos.nombre_variable = Constantes.DOC_ILOCALIZABLE
        }
        return datos
    }

    @Listen("onClick=button#btnGuardarContactoEst")
    boolean gestionNotificacionNoContactado(Event evento){
        //Gestion de la notificación no contactado
        if(contacto_id_motivo_contacto.selectedItem.value && contacto_contactado.selectedItem.value && lbmedios_comunicacion.selectedItem.iterator().size() > 0){
            //Creamos un mapa para alojar los valores devuletos por el metodo
            Map datos = generaDatosNoContactado()
            //Si existe datos, significa que estamos en unos de siguientes motivos de contacto :
            if (datos && datos.nombre_variable != null) {
                //Se recuperan todos los contactos negativos del estudio
                def contadorReg = comprobarNumRegContacto(datos)
                //Se recuperan los documentos asociados al estudio
                def doc = comprobarExisteDoc(datos)
                //Se comprueba si ha contactado dos veces anteriormente y  si se selecciona que no se ha podido contactar
                if (contadorReg.NUM == Constantes.NUM_MAX_CONTACTOS -1 && contacto_contactado.selectedItem.label == "No") {
                    //Se comprueba que no existan documentos postales previos correspondientes a la notificación
                    if(doc.NUM == 0){
                        //Se inicializa el valor de la variable para que no pase por metodo guardar de las bases antes de confirmar la accion con el evento posterior
                        eventoContCitaCotest = false
                        Messagebox.show("El guardado del tercer contacto como 'No contactado' generará un documento, ¿desea continuar?",
                                "Confirmar Acción", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, [
                                onEvent: { e ->
                                    //Cambiamos valor variable para que guarde a traves de las bases
                                    eventoContCitaCotest = true
                                    if (e.data.intValue() == Messagebox.YES) {
                                        this.guardar()
                                        def resultado = documentoEntidadService.creaDocumento(datos)
                                        if (resultado == 0) {
                                            ventanaAviso("No se ha podido generar el documento postal.Contacte con el servicio de informática.")
                                            return
                                        }
                                        //Se carga de nuevo la pagina de expediente para actualizar los datos de las tabs
                                        invocador.activacion()
                                        //Limpiamos el invocador para evitar el error de 'Cambios Pendientes'
                                        invocador = null
                                        // Limpiamos el contenido de la ventana para su próxima utilización
                                        limpiar()
                                        //Devolvemos el valor de la variables a su estado inicial
                                        eventoContCitaCotest = false
                                    }else{
                                        //Si se descarta crear el documento debemos seguir mostrando la modal de editar contacto
                                        this.clickEditarContactoEstudio()
                                        //Devolvemos el valor de la variables a su estado inicial
                                        eventoContCitaCotest = false
                                    }
                                }
                        ] as org.zkoss.zk.ui.event.EventListener)
                    }else{
                        descartarOperaciones()
                    }
                }else if(contadorReg.NUM == Constantes.NUM_MAX_CONTACTOS && doc.NUM > 0){
                    //Al editar, si hay tres contactos negativos y documento generado no se permite la edicion del contacto registrado
                    //Lanzamos mensaje de aviso al usuario
                    ventanaAviso("No puede editar un contacto que ha generado documento postal. Por favor, inserte un nuevo contacto.")
                    //Descartamos operaciones pendientes
                    clickDescartarCambiosContactoEstudio()
                    //Capturamos y detenemos la propagación del evento guardar
                    evento.stopPropagation()
                }
            }else{
                eventoContCitaCotest = true
                this.guardar()
            }
        }
    }
	
/* 
	Al guardar, si no seleccionan Telefono, guardamos el principal
	@Listen("onClick=button#btnGuardarContactoEst")
	void clickGuardarCambiosContactoExp() {
		def contactoActual = leerValoresVista("contacto")
		//Obligamos a elegir medio de comunicación
		if(lbmedios_comunicacion.selectedItem.iterator().size() == 0) {
			//Comprobamos si se ha seleccionador medio de comunicación. Si no, por defecto eleccionador el telefono Principal
			def idPersona = invocador.contactoActual.id_persona
			def mediosComunicacion = persanService.buscarMediosComunicacionPersona(idPersona)
			mediosComunicacion?.each { it ->
				def medioComu = it.CODIGO_MEDIO_COMUNICACION
				def telefonoPrinc
				if(medioComu.equals("TLFDOM") ) {
					telefonoPrinc = it.valor.value
					contacto_valor_medio_comunicacion.value = telefonoPrinc
				}else{
					telefonoPrinc = it.valor.value
					contacto_valor_medio_comunicacion.value = telefonoPrinc
				}
			}
			
		}else {
			def valor_medio_comu = lbmedios_comunicacion.selectedItem.value
			contacto_valor_medio_comunicacion.value = valor_medio_comu
		}
	}
*/
	
	
	@Listen("onClick=button#btnDescartarContactoEst, #btnCerrarVentanaContacto")
	void clickDescartarCambiosContactoEstudio() {
		
		log.debug("click Descartar cambios")
		// Ocultamos la ventana modal
		contenedor.visible = false
		descartarOperaciones()
		//Limpiamos el invocador para evitar el error de 'Cambios Pendientes'
		invocador = null
		// Limpiamos el contenido de la ventana para su próxima utilización
		limpiar()
	}
	
	void despuesDeGuardar() {
        //Comprobamos si invocador tiene asociados la info de expediente
		if (invocador) {
			// Recuperamos los contactos actualizados del expediente
			//invocador.recuperarContactos(idExpediente)
            //invocador.recuperarNotificaciones(idExpediente)
			invocador.activacion()
			// Ocultamos la ventana modal
			contenedor.visible = false
            //Si hay un evento de confirmación pendiente, evitamos el limpiado hasta que finalice
            if(!eventoContCitaCotest){
                //Limpiamos el invocador para evitar el error de 'Cambios Pendientes'
                invocador = null
                // Limpiamos el contenido de la ventana para su próxima utilización
                limpiar()
            }
		}
	}
    
    def comprobarExisteDoc(datos) {
        def tipoDoc = variablesGlobalesEntidadService.recuperar(datos)
        datos.id_tipo_doc = tipoDoc.VALOR
        def doc = documentoEntidadService.countDocPorIdEstudioTipoDoc(datos)
        return doc
    }
    
    def comprobarNumRegContacto(datos) {
        def contadorRegistrosCont = contactoEntidadService.recuperarNumContactosNegPorEstudio(datos)
        return contadorRegistrosCont
    }
	
	void limpiar() {
		// Limpiamos todos los componentes de la ventana
		if (!contacto_id_motivo_contacto.getItems().isEmpty()) {
			limpiarValorComponente(contacto_id_motivo_contacto)
		}
		limpiarValorComponente(contacto_id_estudio)
		limpiarValorComponente(contacto_fecha)
		limpiarValorComponente(contacto_observaciones)
		if (!lbmedios_comunicacion.getItems().isEmpty()) {
			limpiarValorComponente(lbmedios_comunicacion)
		}
	}

}
