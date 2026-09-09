package cribadocervix. expedientes

import cribadocervix. UtilsPcacervixService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.zkoss.zk.ui.Executions
import org.zkoss.zk.ui.event.Event
import org.zkoss.zk.ui.select.annotation.Listen
import org.zkoss.zul.Checkbox
import org.zkoss.zul.Datebox
import org.zkoss.zul.Label
import org.zkoss.zul.Listbox
import org.zkoss.zul.Listitem
import org.zkoss.zul.Window
import org.zkoss.zul.Button
import org.zkoss.zul.Menuitem
import org.zkoss.zul.Menupopup
import org.zkoss.zul.Messagebox
import org.zkoss.zul.Filedownload
import base.EntidadFormulario
import base.FormularioComposer
import cribadocervix.jade.PandoraService
import cribadocervix.jade.PersanService
import cribadocervix. Constantes
import cribadocervix. ExpedienteService
import cribadocervix. PcaCervixService
import cribadocervix. entidad.CitaEntidadService
import cribadocervix. entidad.ContactoEntidadService
import cribadocervix. entidad.DocumentoEntidadService
import cribadocervix. entidad.EstudioEntidadService
import cribadocervix. entidad.ExpedienteEntidadService
import cribadocervix. entidad.ProcesoEntidadService
import cribadocervix. entidad.PruebaEntidadService
import groovy.util.logging.Slf4j
import org.zkoss.zk.ui.select.Selectors

@Slf4j
@Component
class ExpedienteComposer  extends FormularioComposer   {

    //Servicios
    @Autowired
    ExpedienteService expedienteService
    ExpedienteEntidadService expedienteEntidadService
    PcaCervixService pcaCervixService
    ProcesoEntidadService procesoEntidadService
    EstudioEntidadService estudioEntidadService
    PruebaEntidadService pruebaEntidadService
    ContactoEntidadService contactoEntidadService
    CitaEntidadService citaEntidadService
    DocumentoEntidadService documentoEntidadService
    UtilsPcacervixService utilsPcacervixService
    PersanService persanService
    PandoraService pandoraService

//    @Wire(".prueba")
//    List componenteOcultar
    EntidadFormulario expediente

    //Campos
    Listbox lbExpedienteProcesos
    Listbox lbIdentPersExpediente
    Listbox lbHistEstadosEstudio
    Listbox lbExpedienteEstudios
    Listbox lbPruebasExp
    Listbox lbExpediente_contacto
    Listbox lbExpediente_cita
    Listbox lbExpediente_notificacion
    Listbox lbExpediente_vacunacion
    Label expediente_fecha_alta
    Label expediente_fecha_baja
    Label expediente_fecha_fin
    Label expediente_id_persona
    Label expediente_persona_nombre
    Label expediente_persona_apellido1
    Label expediente_persona_apellido2
    Label expediente_persona_fecha_nacimiento
    Label expediente_id
    Label expediente_dni_nie
    Label expediente_cipr
    Checkbox cbProcesoSeleccionado
    Datebox fecha_baja_edicion
    Listbox lbMotivoEvento

    Button btnInsertarContacto
	Button btnBorrarContacto
	Button btnEditarFecBajaExp 
	Button btnGuardarFecBajaExp
	Button btnDescartarFecBajaExp
	Button btnInsertarNotificacion
    Button btnDescargarNotificacionExp

    Window wndEditarProcesosExpediente
    Window wndEditarEstudiosProceso
    Window wndEditarPruebasEstudio
    Window wndEditarContactoEstudio
    Window wndEditarCitaEstudio
    //	Window wndVersionadoExpediente
    Window wndIdentif
    Window wndHistoricoEstadosEstudio
    Window wndEditarFecBajaExp

    def parametros = [:]
    Map procesoActual
    Map estudioActual
    Map pruebaActual
    Map contactoActual
    Map citaActual

    String excepcionORA = 'ORA-02292'
    String sqlException = 'SQLException'


    def despuesDeComponer () {
        parametros.id_profesional = session.idProfesional
        parametros.profesional = session.profesional
        parametros.id_unidad_funcional = session.unidad_funcional?.id
        parametros.unidad_funcional = session.unidad_funcional?.descripcion
        parametros.codigo_utramitadora_ea = session.unidad_funcional?.codigo_utramitadora_ea
        parametros.login_unico = session.loginUnico
        parametros.id_expediente = session.id_expediente
        parametros.id_perfil = session.idPerfil
        parametros.perfil = session.perfil
    }

    def activacion() {
       /*
        Visibilidad de componentes según el rol del usuario
        
        if (session.idPerfil != 2) {
            for (componente in componenteOcultar) {
                componente.detach()
            }
        } 
        */
        
        /* Controlamos las versiones
         Si vienen del buscador de expedientes, o vienen del detalle pesona con fecha_baja_exp
         enviamos como parametro de busqueda dicho fecha para recuperar el expediente indicado por dichos parametros
     */
        if ((vlAnterior.equals("vlBuscadorExpedientes") && session.fecha_baja_exp)
                || (vlAnterior.equals("vlPersona") && session.fecha_baja_exp)) {

            parametros.fecha_baja_exp = session.fecha_baja_exp
            parametros.id_expediente = session.id_expediente
            //Reseteamos el valor para las proximas búsquedas
            session.fecha_baja_exp = null

        }else {
            parametros.fecha_baja_exp = Constantes.MAX_FECHA
        }
        log.debug("activacion expediente")
        expediente = new EntidadFormulario("expediente","id_expediente", this)
        // Configuramos la entidad
        expediente.configurar("pcacervix", "expediente")
        //Establecemos el servicio de operaciones que va a gestionar la lista de operaciones
        servicioOperaciones = expedienteEntidadService

        expediente.seleccion = session.id_expediente
        log.debug("Mostrando expediente con id: ${expediente.seleccion}")
        deshabilitar()

        if(expediente.seleccion == NUEVO_REGISTRO) {
            expediente.insertar()
            Date fechaActual = new Date().clearTime()
            Date fechaMax = Date.parse("dd/MM/yyyy", MAX_FECHA)
            escribirValorComponente(expediente_fecha_alta, fechaActual)
            escribirValorComponente(expediente_fecha_baja, fechaMax)
            procesoActual = null

        }else {
            def expedienteRecuperado = expediente.recuperar(parametros)
            if (!expedienteRecuperado) {
                if(null == parametros.id_expediente) {
                    cambiarVlActivo("BuscadorExpedientes")
                }else {
                    ventanaAviso("No hay ningún expediente con id asociado: ${parametros?.id_expediente}")
                    cambiarVlActivo("BuscadorExpedientes")
                }
                return
            }

            expedienteRecuperado.fecha_alta = expedienteRecuperado.fecha_alta.format("dd/MM/yyyy")
            expedienteRecuperado.fecha_baja = expedienteRecuperado.fecha_baja.format("dd/MM/yyyy")
            if(expedienteRecuperado.fecha_baja != MAX_FECHA) { //Si el expediente está dado de baja, deshabilitamos el boton para Editar la Fecha de Baja
                btnEditarFecBajaExp.disabled = true
            }

            if(expedienteRecuperado.hasProperty("fecha_evento")) {
                expedienteRecuperado.fecha_evento = expedienteRecuperado.fecha_evento.format("dd/MM/yyyy")
            }

            //Calculamos Fecha Fin expediente, con Fec.Nacimiento
            Integer edadFin = new Integer(pandoraService.leerVariableGlobal(Constantes.RANGO_EDAD_FINAL))

            Calendar calendar = Calendar.getInstance()
            calendar.setTime(expedienteRecuperado.persona_fecha_nacimiento)
            calendar.add(Calendar.YEAR, edadFin)
            def fecFin = calendar.getTime().format("dd/MM/yyyy")
            escribirValorComponente(expediente_fecha_fin, fecFin)

            expedienteRecuperado.persona_fecha_nacimiento = expedienteRecuperado.persona_fecha_nacimiento.format("dd/MM/yyyy")
            expediente.escribirValoresVista(expedienteRecuperado)

            expedienteRecuperado.procesos?.each { it ->
                it.fecha_inicio = it.fecha_inicio.format("dd/MM/yyyy")
                it.fecha_fin =it.fecha_fin.format("dd/MM/yyyy")
                if (it.inicial == 1) {
                    it.inicial = true
                } else {
                    it.inicial = false
                }
				if (it.irregular == 1) {
                    it.irregular = true
				} else {
                    it.irregular = false
				}
				if(it.estudios.find() != null){ //Si el proceso no tiene estudios(caso de inserccion manual) mostramos menuItem de Inserccion de estudios para poder insertar manualmente el estudio
                    String display ="display:none;"
                    it.noEstudios = display
                }else {
                    it.noEstudios = ""
                }
                it.estudios?.each { est ->
                    //Si el estudio no es de VPH, ocultamos la pestaña Productos Dispensados
                    if(est.ID_ESTUDIO_TIPO != Constantes.ID_TIPO_ESTUDIO_VPH) {
                        est.estudio_vph = false
                    }else {
                        est.estudio_vph = true
                    }
                     //Si es estudio de CITOLOGÍA, GINECOLOGIA o COTEST-1, mostramos columnas Peticion y Accion en la tabla de Pruebas
                    if(est.ID_ESTUDIO_TIPO == Constantes.ID_TIPO_ESTUDIO_CITOLOGIA || est.ID_ESTUDIO_TIPO == Constantes.ID_TIPO_ESTUDIO_GINECOLOGIA || est.ID_ESTUDIO_TIPO == Constantes.ID_TIPO_ESTUDIO_COTEST_1) {
                        est.headerPeticion =  true
                    }else {
                        est.headerPeticion = false
                    }
                    est.fecha_inicio = est.fecha_inicio.format("dd/MM/yyyy")
                    est.fecha_resultado = est.fecha_resultado.format("dd/MM/yyyy")
                    if(est.ID_ESTUDIO_RESULTADO_TIPO != null) {
                        // Si tiene resultado pero fue No Valido, habilitamos boton Insertar Prueba para repetir la prueba
                        if(est.id_estado_estudio == Constantes.ID_ESTADO_PEND_REP_PRU) {
                            //Habilitamos el boton para insertar nueva prueba
                            est.btnPruebaDisabled = false
                            //NO mostramos Resultado y su fecha en la cabecera del panel
                            est.finalizado = true
                            //Mostramos Estado en cabecera
                            est.activo = false
                        }else {
                            //Deshabilitamos el boton de nueva Insercción de Prueba
                            est.btnPruebaDisabled = true
                            //Mostramos Resultado y su fecha en la cabecera del panel
                            est.finalizado = true
                            //NO mostramos Estado en cabecera
                            est.activo = false
                            //Si el estudio está finalizado, modificamos el literal del estado a FINALIZADO
                            est.descripcion_estado_estudio = "Finalizado"
                        }
                    }else {
                      /*Controlamos si el estudio no está en uno de los siguientes estados: pendiente de invitacion (a solicitud de funcionales para el cribado oportunista)
                       * pendiente de participacion, repetición de prueba, pendiente cita (para citologias, estado actual que usamos antes de realizar prueba - 01/10/24)
                        o citado a la espera de realizar prueba (para citologias, estado que utilizaremos cuando nos envien las citas agendadas) */
                        if(est.id_estado_estudio != Constantes.ID_ESTADO_PEND_INVIT && est.id_estado_estudio != Constantes.ID_ESTADO_PEND_PARTIC && 
                            est.id_estado_estudio != Constantes.ID_ESTADO_PEND_REP_PRU && est.id_estado_estudio != Constantes.ID_ESTADO_PEND_PRU && 
                            est.id_estado_estudio != Constantes.ID_ESTADO_PEND_CITA) {
                                //Deshabilitamos el boton de nueva Insercción de Prueba
                                est.btnPruebaDisabled = true
                        }else {
                                //Habilitamos el boton para insertar nueva prueba
                                est.btnPruebaDisabled = false
                        }
                        //NO mostramos Resultado y su fecha en la cabecera del panel
                        est.finalizado = false
                        //Mostramos Estado en cabecera
                        est.activo = true
                    }

                    // Se comprueba si el estudio actual es de tipo ginecologia ya que se debe permitir insertar más de una prueba
                    if (est.ID_ESTUDIO_TIPO == Constantes.ID_TIPO_ESTUDIO_GINECOLOGIA ){
                        //Habilitamos el boton para insertar nueva Prueba aunque tenga una creada
                        est.btnPruebaDisabled = false
                    }

                    est.pruebas?.each { pru ->
                        pru.btnPeticVisib = true
                        if(pru.ID_TIPO_PRUEBA == Constantes.ID_TIPO_PRUEBA_CITOLOGIA || est.ID_ESTUDIO_TIPO == Constantes.ID_TIPO_ESTUDIO_COTEST_1) {
                            //Comprobamos si tiene muestra para mostrar por pantalla que la peticion ha sido enviada (registro en PETICION_MUESTRA)
                            def tieneMuestra = false
                            if (pru.cod_producto) { // Si no tiene cod_producto es porque aun no se ha completado los datos de la prueba (PRUEBA_DETALLE)
                                tieneMuestra = pcaCervixService.recuperarMuestraPorCodProducto(pru.cod_producto, est.ID)
                            }

                            if (tieneMuestra) { //Si tiene peticion enviada, deshabilitamos boton de Envio
                                pru.peticion_enviada = 'ENVIADA'
                                pru.mostrarBotonEnviar = true
                                pru.btnPeticDisab = true
                                if(pru.ID_TIPO_PRUEBA == Constantes.ID_TIPO_PRUEBA_VPH){
                                    pru.btnPeticVisib = false
                                }else{
                                    pru.btnPeticVisib = true
                                }
                            }else{
                                if(pru.ID_TIPO_PRUEBA == Constantes.ID_TIPO_PRUEBA_VPH){
                                    pru.btnPeticVisib = false
                                }else{
                                    pru.btnPeticVisib = true
                                }
                                pru.peticion_enviada = 'NO ENVIADA'
                                pru.mostrarBotonEnviar = true
                                pru.btnPeticDisab = false
                                //Deshabilitamos el boton para insertar nueva Prueba puesto que ya tiene una creada
                                est.btnPruebaDisabled = true
                            }
                        }else {
                            pru.mostrarBotonEnviar = false
                            pru.btnPeticVisib = false
                            pru.peticion_enviada = ''
                            pru.btnPeticDisab = true
                        }
                        pru.fecha = pru.fecha.format("dd/MM/yyyy")
					}
					
					est.productos?.each { prod ->
						prod.fec_dispensacion = prod.fec_dispensacion.format("dd/MM/yyyy")
						prod.fec_estado = prod.fec_estado.format("dd/MM/yyyy")
					}
				}
			}
			lbExpedienteProcesos.actualizar(expedienteRecuperado.procesos)
			// Renderizamos y recorremos el Listbox de Procesos para poder recorrer los elementos tipo Panel y así abrir solo el primero Proceso y su Estudio del expediente (actuales)
			lbExpedienteProcesos.renderAll()
			lbExpedienteProcesos?.getItems()?.each { it ->
				Listbox lbEstudios = Selectors.find(it, ".lbEstudios")[0]
				lbEstudios.renderAll()
				def panelProcesos = Selectors.find(it,".panelProcesos")
				if(lbExpedienteProcesos.items[0] == it) {
					panelProcesos?.each { pnProc ->
						if(panelProcesos.first() == pnProc) {
							pnProc.open= true
							def panelEstudios = Selectors.find(pnProc,".panelEstudios")
							panelEstudios?.each { pnEst ->
								if(panelEstudios[0] == pnEst) {
									pnEst.open = true
								}else {
									pnEst.open = false
								}
							}
						}else {
							pnProc.open = false
						}
					}
				}else {
					panelProcesos?.each { pnProc ->
						pnProc.open= false
						def panelEstudios = Selectors.find(pnProc,".panelEstudios")
						panelEstudios?.each { pnEst ->
							pnEst.open = false
						}
					}
				}
                //Deshabilitamos botones segun permisos
                //PROCESOS
                def permisoProceso = session.permisosTablas. PROCESO
                def menuPopupProc = Selectors.find(it, ".menuProcesos")
                menuPopupProc.each { menuProc ->
                    Menuitem itemInsertProc = Selectors.find(menuProc, ".itemInsertProc")[0]
                    Menuitem itemUpdateProc =  Selectors.find(menuProc, ".itemUpdateProc")[0]
                    Menuitem itemDeleteProc = Selectors.find(menuProc, ".itemDeleteProc")[0]
                    if (permisoProceso){
                        if(permisoProceso.size == 1 && permisoProceso.find { it.PRIVILEGE.equals("SELECT")}) {
                            menuProc.detach()
                        }else {
                            if(!permisoProceso.find { it.PRIVILEGE.equals("INSERT")}) {
                                itemInsertProc.detach()
                            }
                            if(!permisoProceso.find { it.PRIVILEGE.equals("UPDATE")}) {
                                itemUpdateProc.detach()
                            }
                            if(!permisoProceso.find { it.PRIVILEGE.equals("DELETE")}) {
                                itemDeleteProc.detach()
                            }
                        }
                    }else {
                        menuProc.detach()
                    }
                }
               
                //ESTUDIOS
                def permisoEstudio = session.permisosTablas. ESTUDIO
                def menuPopupEst = Selectors.find(lbEstudios, ".menuEstudios")
                menuPopupEst.each { menuEst ->
                    Menuitem itemInsertEst = Selectors.find(menuEst, ".itemInsertarEst")[0]
                    Menuitem itemUpdateEst =  Selectors.find(menuEst, ".itemUpdateEst")[0]
                    Menuitem itemDeleteEst = Selectors.find(menuEst, ".itemDeleteEst")[0]
                    if (permisoEstudio){
                        if(permisoEstudio.size == 1 && permisoEstudio.find { it.PRIVILEGE.equals("SELECT")}) {
                            menuEst.detach()
                        }else {
                            if(!permisoEstudio.find { it.PRIVILEGE.equals("INSERT")}) {
                                itemInsertEst.detach()
                            }
                            if(!permisoEstudio.find { it.PRIVILEGE.equals("UPDATE")}) {
                                itemUpdateEst.detach()
                            }
                            if(!permisoEstudio.find { it.PRIVILEGE.equals("DELETE")}) {
                                itemDeleteEst.detach()
                            }
                        }                        
                    }else {
                        menuEst.detach()
                    }
                }
                
                //PRUEBAS
                def botonesInsertPrueba = Selectors.find(lbEstudios,".insertarPruebaExp")
                botonesInsertPrueba.each { btn ->
                    def permisoPrueba = session.permisosTablas. PRUEBA
                    if (permisoPrueba){
                      if(!permisoPrueba.find { it.PRIVILEGE.equals("INSERT")}) {
                          btn.detach()
                      }
                    }else {
                        btn.detach()
                    }
                }               
            }
			
			//Recuperamos las Distintas Identificaciones de la Persona. Solo mostraremos incialmente DNI o NIE y CIPR. 
			def identificaciones = persanService.buscarIdentidadPersona(expediente_id_persona.value)
			
			identificaciones.findAll { 
				if(it.CODIGO_IDENTIDAD_TIPO.contains("DNI") != null) {
					if(it.CODIGO_IDENTIDAD_TIPO == "DNI") {
						def valorDNI = it.VALOR
						escribirValorComponente(expediente_dni_nie, valorDNI)
					}
				}
				if(it.CODIGO_IDENTIDAD_TIPO.contains("NIE") != null) {
					if(it.CODIGO_IDENTIDAD_TIPO == "NIE") {
						def valorNIE = it.VALOR
						escribirValorComponente(expediente_dni_nie, valorNIE)
					}
				}
				if(it.CODIGO_IDENTIDAD_TIPO.contains("CIPR") != null) {
					if(it.CODIGO_IDENTIDAD_TIPO == "CIPR") {
						def valorCIPR = it.VALOR
						escribirValorComponente(expediente_cipr, valorCIPR)
					}
				}
			}
			
			
			//Recuperamos los Contactos del expediente, asociados a cada estudio
			def contactosEstudio = contactoEntidadService.recuperarContactosPorIdExpediente(expediente.seleccion)
			lbExpediente_contacto.actualizar(contactosEstudio)
            //Deshabilitamos botones Contacto segun permisos
            def permisoContacto = session.permisosTablas. REGISTRO_CONTACTO
            if (permisoContacto){
              if(!permisoContacto.find { it.PRIVILEGE.equals("INSERT")}) {
                  btnInsertarContacto.detach()
              }
              if(!permisoContacto.find { it.PRIVILEGE.equals("DELETE")}) {
                  btnBorrarContacto.detach()
              }
            }else {
                btnInsertarContacto.detach()
                btnBorrarContacto.detach()
            }
			
			//Recuperamos las citas asociadas al expediente
			def citasExpediente = citaEntidadService.recuperarPorIdExpediente(expediente.seleccion)
			citasExpediente?.each { it ->
				if(it.FECHA_CITA != null)
					it.FECHA_CITA = it.FECHA_CITA.format("dd/MM/yyyy")
			}
			lbExpediente_cita.actualizar(citasExpediente)
			
			//Recuperamos los documentos asociados al expediente
			def documentosExpediente = documentoEntidadService.recuperarDocPorIdExpediente(expediente.seleccion)
			documentosExpediente?.each { it ->
				if(it.FECHA != null)
					it.FECHA= it.FECHA.format("dd/MM/yyyy")				
			}
			lbExpediente_notificacion.actualizar(documentosExpediente)
            //Deshabilitamos botones Notificaciones segun permisos
            def permisoDocs = session.permisosTablas. DOCUMENTO
            if (permisoDocs){
                if(!permisoDocs.find { it.PRIVILEGE.equals("INSERT")}) {
                    btnInsertarNotificacion.detach()
                }
            }else {
                btnInsertarNotificacion.detach()
            }

            // Recuperamos las vacunas de la persona
            def vacunasPersona = expedienteService.recuperarVacunacionPorIdPersona(expediente_id_persona.value)
            vacunasPersona?.each { not ->
                not.fecha = not.fecha.format("dd/MM/yyyy")
            }
            lbExpediente_vacunacion.actualizar(vacunasPersona)
		}
	}
	
    @Listen("onClick=.enviarPeticion")
    def clickEnviarPeticion(Event event) {
        def idEstudioActivo = estudioEntidadService.recuperarUltimoEstudioPorIdExpediente(expediente.seleccion).ID
        def tipoEstudioActivo = estudioEntidadService.recuperarTipoEstudioPorIdEstudio(idEstudioActivo).ID_ESTUDIO_TIPO
        List pruebasEstudio = []
        List datosPruebas = []
        Map resultado = [:]
        resultado.errores = ""
        boolean bPuedeInsertar = true
        boolean bCostest

        if(tipoEstudioActivo == Constantes.ID_TIPO_ESTUDIO_COTEST_1 || tipoEstudioActivo == Constantes.ID_TIPO_ESTUDIO_COTEST_2){
            def pruebasRecuperadas = pruebaEntidadService.recuperarPruebasPorIdEstudio([id:idEstudioActivo])
            pruebasRecuperadas.each {it->
                pruebasEstudio.add(it.ID)
            }
            bCostest = true
        }else{
            Listitem itemSeleccionado = event.target.parent.parent
            //def pruebaSeleccionada = itemSeleccionado.value
            pruebasEstudio.add(itemSeleccionado.value)
            bCostest = false
        }

//        Listitem itemSeleccionado = event.target.parent.parent
//        def pruebaSeleccionada = itemSeleccionado.value
        //definir una list con las pruebas seleccionadas para recorrerlo, si se trata de una unica prueba, solo se incluye una
        pruebasEstudio.each { pruebaSeleccionada ->
            Map datos = [:]
            datos.id_prueba = pruebaSeleccionada
            Map datosPrueba = pruebaEntidadService.recuperar(datos)
            datosPruebas.add(datosPrueba)
        }

        datosPruebas.each {datosPrueba ->
            if(datosPrueba) {
                //Si la petición es de Citología, antes de enviarla, validamos campos obligatorios
                if (datosPrueba.id_tipo_prueba == 2) {
                    if (datosPrueba.menopausia == null) {
                        resultado.errores += "* Menopausia \n"
                    }
                    if (datosPrueba.crib_adec_10 == null) {
                        resultado.errores += "* Cribado adecuado 10 años  \n"
                    }
                    if (datosPrueba.pat_cerv_prev == null) {
                        resultado.errores += "* Patología Cervical Previa \n"
                    }
                    if (datosPrueba.vacunacion_vph == null) {
                        resultado.errores += "* Vacunación VPH"
                    }
                    //Mostramos los errores en cada campo
                    if (resultado.errores.isEmpty()) {
                        bPuedeInsertar = true
                    } else {
                        ventanaError("Debe rellenar los campos obligatorios: \n " + resultado.errores)
                        bPuedeInsertar = false
                    }
                }
            }
        }
        if(bPuedeInsertar){
            resultado.insert = pcaCervixService.insertarPeticionMuestra(datosPruebas)

            if(resultado.insert && !bCostest) {
                //Cambiamos el estado del estudio a 'Pendiente de Resultados' si la prueba no es de estudio cotest (si es así el estado se cambio al insertar)
                def idCodEstado = Constantes.ID_ESTADO_PEND_RESULT
                estudioEntidadService.actualizarEstadoEstudio(idEstudioActivo, idCodEstado)
            }
            this.activacion()
        }
    }

    def clickMenuProceso(Event evento) {
        log.debug("Click Boton Menupop")

        def boton = evento.target
        Menupopup menupopup = boton.nextSibling
        menupopup.open(boton)
    }


    def clickInsertarNuevoProceso() {
        log.debug("Click Insertar nuevo Proceso. Redirigimos a ventana emergente de creación")

        Map proc = [:]
        proc.id_proceso = NUEVO_REGISTRO
        proc.id_expediente = expediente.seleccion
        proc.id_persona_expediente = leerValorComponente(expediente_id_persona)
        proc.titulo = "Añadir nuevo proceso al Expediente"

        log.debug("Insertar nuevo proceso a expediente: ${proc}")

        modificarProceso(proc)

    }

    def clickBorrarProceso(Event evento) {
        log.debug("Click Borrar Proceso. Mostramos alerta de confirmacion")

        def itemSelected = evento.target.parent.parent.parent.parent.parent.parent
        def idProcesoSeleccionado = itemSelected.value

        // Mostramos un cuadro de diálogo pidiendo la confimación de la operación al usuario
        Messagebox.show("¿Confirma que desea borrar el proceso con ID: " +idProcesoSeleccionado+ "?",
                "Confirmar Acción", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, [
                    onEvent: { event ->
                        if(event.data.intValue() == Messagebox.YES) {
                            //Capturamos la excepcion para controlar si el proceso tiene dependencias (estudios asociados)
                            try {
                                Map datos=[:]
                                datos.id_proceso = idProcesoSeleccionado
                                procesoEntidadService.borrar(datos)
                                this.activacion()

                            } catch(Exception e) {
                                def mensaje = e.cause.getMessage()
                                if(mensaje && mensaje.contains(excepcionORA)) {
                                    ventanaError("No se puede eliminar el Proceso debido a que tiene datos asociados.")
                                }else if(e.cause && e.cause.toString().contains(sqlException)){
                                    ventanaError("Error al eliminar el proceso: " + mensaje)
                                }
                            }
                        }
                    }
                ] as org.zkoss.zk.ui.event.EventListener)

    }

    def clickEditarProceso(Event evento) {
        log.debug("Click Editar Proceso. Mostramos ventana emergente")

        def itemSelected = evento.target.parent.parent.parent.parent.parent.parent
        def idProcesoSeleccionado = itemSelected.value

        Map proc = [:]
        proc.id_proceso = idProcesoSeleccionado
        proc.id_expediente = expediente.seleccion
        proc.titulo = "Editar Proceso"
        modificarProceso(proc)

    }

    //	@Listen("onDoubleClick= #lbExpedienteProcesos listitem")
    //	def clickDobleProcesos(Event evento) {
    //		log.debug("Doble Click en Proceso. Expandimos")
    //
    //		Panel panel = new Panel()
    //		def itemSelected = evento.target
    //
    //		if (evento.target.parent == lbExpedienteProcesos)  {
    //		   panel = Selectors.find(itemSelected, '.panelProcesos') [0]
    //		   if(panel.open)
    //			  panel.open = false
    //		   else
    //			  panel.open = true
    //		}
    //	}
    //

    def clickMenuEstudio(Event evento) {
        log.debug("Click Boton Menupop")

        def boton = evento.target
        Menupopup menupopup = boton.nextSibling
        menupopup.open(boton)
    }

    def clickInsertarEstudio(Event evento) {
        log.debug("Click Nuevo Estudio")

        def itemSelected = evento.target.parent.parent.parent.parent.parent.parent
        def idProceso = itemSelected.getAttribute("procesoId")

        if(idProceso == null) {
            // Caso en el que manualmente se abre un Proceso y aun no tiene estudio asociado.
            //Se añade boton Insertar Estudio al menuPopUp de Procesos, recuperamos el idProceso de otra forma
            idProceso = evento.target.parent.getId()
        }
        Map estud = [:]
        estud.id_estudio = NUEVO_REGISTRO
        estud.id_expediente = expediente.seleccion
        estud.titulo = "Añadir nuevo Estudio al proceso"
        estud.id_proceso = idProceso
        estud.unidad_funcional = parametros.id_unidad_funcional
        modificarEstudio(estud)

    }

    def clickEditarEstudio(Event evento) {
        log.debug("Click Editar Estudio. Mostramos ventana emergente")

        def itemSelected = evento.target.parent.parent.parent.parent.parent.parent
        //Recogemos el id del estudio que queremos editar, guardado en el value del listItem
        def idEstudioSeleccionado = itemSelected.value
        //Recogemos el id del proceso oculto en un atributo
        def idProceso = itemSelected.getAttribute("procesoId")

        Map estud = [:]
        estud.id_estudio = idEstudioSeleccionado
        estud.id_proceso = idProceso
        estud.id_expediente = expediente.seleccion
        estud.titulo = "Editar Estudio"
        modificarEstudio(estud)

    }


    def clickBorrarEstudio(Event evento) {
        log.debug("Boton Borrar Estudio. Mostramos alerta de confirmacion")

        def itemSelected = evento.target.parent.parent.parent.parent.parent.parent
        //Recogemos el id del estudio ue queremos borrar, guardado en el value del listItem
        def idEstudioSeleccionado = itemSelected.value

        // Mostramos un cuadro de diálogo pidiendo la confimación de la operación al usuario
        Messagebox.show("¿Confirma que desea borrar el Estudio con ID: " +idEstudioSeleccionado+ "?",
                "Confirmar Acción", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, [
                    onEvent: { event ->
                        if(event.data.intValue() == Messagebox.YES) {

                            try {
                                estudioEntidadService.borrar(idEstudioSeleccionado)
                                this.activacion()

                            } catch(Exception e) {
                                def mensaje = e.getMessage()
                                if(mensaje.contains(excepcionORA))
                                    ventanaError("No se puede eliminar el estudio debido a que tiene datos asociados.")
                            }
                        }
                    }
                ] as org.zkoss.zk.ui.event.EventListener)

    }


    def clickVerHistoricoEstados(idEstudio) {
        def historicos = estudioEntidadService.recuperarHistoricoEstadosEstudio(idEstudio)

        historicos?.each {
            it.FEC_INICIO_ESTADO = it.FEC_INICIO_ESTADO.format("dd/MM/yyyy")
            it.FEC_FIN_ESTADO = it.FEC_FIN_ESTADO.format("dd/MM/yyyy")
        }
        lbHistEstadosEstudio.actualizar(historicos)
        // Hacemos visible la ventana modal
        wndHistoricoEstadosEstudio.visible = true
        wndHistoricoEstadosEstudio.doModal()
    }


    //	def clickDobleEstudios(Event evento) {
    //		log.debug("Doble Click en Estudio. Expandimos")
    //		def itemSelected = evento.target
    //		Panel panel = new Panel()
    //
    //		panel = Selectors.find(itemSelected, '.panelEstudios') [0]
    //		if(panel.open)
    //			panel.open = false
    //		else
    //			panel.open = true
    //
    //	}


    @Listen("onClick=#btnVerPersonaExpediente")
    def clickVerPersona() {
        session.id_seleccionado = [id_persona: leerValorComponente(expediente_id_persona)]
        cambiarVlActivo("Persona")
    }

    @Listen("onClick=#btnEditarFecBajaExp")
    def clickEditarFechaBajaExp() {
        //Le ponemos valor por defecto la fecha Actual
        escribirValorComponente(fecha_baja_edicion, new Date())
        // Hacemos visible la ventana modal
        wndEditarFecBajaExp.visible = true
        wndEditarFecBajaExp.doModal()
        establecerBotonGuardar(btnGuardarFecBajaExp)
        establecerBotonDescartar(btnDescartarFecBajaExp)
        def tipoEventos = pcaCervixService.recuperarTipoMotivoEventoBaja()
        rellenarListbox(lbMotivoEvento, tipoEventos, "Seleccione..." )

    }

    def clickGuardarFecBajaEdicion() {
        def nuevaFecBaja = leerValorComponente(fecha_baja_edicion)
        Map datos = [:]
        datos.id = parametros.id_expediente
        datos.fecha_alta = leerValorComponente(expediente_fecha_alta)
        datos.fecha_baja_edicion = nuevaFecBaja
        datos.motivo_evento = leerValorComponente(lbMotivoEvento)
        datos.descripcion_motivo = lbMotivoEvento.selectedItem.getLabel()
        datos.login = parametros.login_unico
        expediente.modificar(datos)
    }

    @Listen("onClick=#btnCerrarVentanaEditFecBaja, #btnDescartarFecBajaExp")
    def clickDescartarFecBajaEdicion() {
        log.debug("click Descartar cambios")
        // Ocultamos la ventana modal
        wndEditarFecBajaExp.visible = false
        descartarOperaciones()
    }

    void despuesDeGuardar() {
        wndEditarFecBajaExp.visible = false
        this.activacion()

    }


    @Listen("onClick=#btnVerIdentifiPers")
    def clickVerIentificaciones() {
        lbIdentPersExpediente.actualizar(persanService.buscarIdentidadPersona(expediente_id_persona.value))
        // Hacemos visible la ventana modal
        wndIdentif.visible = true
        wndIdentif.doModal()
    }


    @Listen("onClick=.insertarPruebaExp")
    def clickInsertarPrueba(Event evento) {
        log.debug("Click Nueva Prueba")
        def tapPanel = evento.target.parent
        def idEstudioPrueba = tapPanel.getAttribute("estudioId")
        def idEstudioTipoPrueba = tapPanel.getAttribute("estudioTipoId")
        def idProcesoPrueba = tapPanel.getAttribute("estudioProcesoId")
        def idProcesoPadrePrueba = procesoEntidadService.recuperar([id_proceso: idProcesoPrueba]).ID_PADRE
        def idEstadoEstudioPrueba = tapPanel.getAttribute("estudioEstadoId")
        def estudioVPHPos = estudioEntidadService.recuperarUltimoEstudioVPHPorIdProceso(idProcesoPrueba)
        def estudioVPHPosAnterior = estudioEntidadService.recuperarUltimoEstudioVPHPorIdProceso(idProcesoPadrePrueba)
        boolean contResultPosVPH = false
        Map prueba = [:]

        //Controlamos si existe prueba pre-seleccionada y se trata de citologia liquida
        // con resultado de estudio VPH positivo y pendiente de registrar prueba o pedir cita
        if (    (idEstudioTipoPrueba == Constantes.ID_TIPO_ESTUDIO_GINECOLOGIA || idEstudioTipoPrueba == Constantes.ID_TIPO_ESTUDIO_CITOLOGIA)
                && (idEstudioTipoPrueba != Constantes.ID_TIPO_ESTUDIO_COTEST_1 || idEstudioTipoPrueba != Constantes.ID_TIPO_ESTUDIO_COTEST_2)
                && (idEstadoEstudioPrueba == Constantes.ID_ESTADO_PEND_PRU
                    || idEstadoEstudioPrueba == Constantes.ID_ESTADO_PEND_CITA
                    || idEstadoEstudioPrueba == Constantes.ID_ESTADO_PEND_RESULT)
                && (estudioVPHPos || estudioVPHPosAnterior)
        ) {

            //Recuperamos los contactos del proceso del proceso actual en el caso de estudios de citologia liquida,
            // o del proceso padre en caso de ser estudio de ginecologia
            // para saber si se ha comunciado el resultado VPH positivo

            def contactosProceso = [:]
            if(estudioVPHPos){
                contactosProceso = contactoEntidadService.recuperarContactosporProceso([id_proceso: idProcesoPrueba])
            }
            if(estudioVPHPosAnterior){
                contactosProceso = contactoEntidadService.recuperarContactosporProceso([id_proceso: idProcesoPadrePrueba])
            }

            //Si existen contactos en el proceso y alguno es de comunicacion VPH , ponemos la variable contResultPosVPH a true
            if(contactosProceso.size() != 0) {
                contactosProceso.each { it ->
                    if (contResultPosVPH == false
                            && it.CONTACTADO == 1
                            && it.ID_MOTIVO_CONTACTO == Constantes.ID_COMUNICACION_RESULT_VPH) {
                        contResultPosVPH = true
                    }
                }
            }

            //Se comprueba si se ha comunicado el positivo en VPH para poder insertar la prueba
            if(contResultPosVPH){
                prueba.id_prueba = NUEVO_REGISTRO
                prueba.id_estudio = idEstudioPrueba
                prueba.id_expediente = expediente.seleccion
                prueba.titulo = "Insertar nueva prueba"
                modalPrueba(prueba)
            }else {
                //Si no se ha comunicado el positivo en VPH, lanza un mensaje al usuario para que lo haga
                ventanaAviso("Debe notificar el resultado de la prueba VPH positiva antes de registrar la prueba.")
            }
        }else{
            //En el resto de estudios deja intertar la nueva prueba sin comprobaciones previas
            prueba.id_prueba = NUEVO_REGISTRO
            prueba.id_estudio = idEstudioPrueba
            prueba.id_expediente = expediente.seleccion
            prueba.titulo = "Insertar nueva prueba"
            modalPrueba(prueba)
        }
    }

    @Listen("onDoubleClick= .lbPruebasExp listitem")
    def clickDoblePruebas(Event evento) {
        log.debug("Doble Click en Prueba. Ventana emergente con Detalle de Pruebas")
        def itemSelected = evento.target
        def idPruebaSeleccionado = itemSelected.value
        def idEstudioPrueba = itemSelected.getAttribute("estudioId")
        def tienePeticion = itemSelected.getAttribute("tienePeticion")

        Map prueba = [:]
        prueba.id_prueba = idPruebaSeleccionado
        prueba.id_estudio = idEstudioPrueba
        prueba.id_expediente = expediente.seleccion
        prueba.titulo = "Detalles de la Prueba"
        if (tienePeticion.equals('NO ENVIADA')) {
            prueba.guardadoPrevioEnvio = true
        }else {
            prueba.guardadoPrevioEnvio =  false
        }
        modalPrueba(prueba)
    }

    @Listen("onClick= .lbPruebasExp listitem")
    void tipoPruebaSelect(Event evento){
        //Dependiendo del tipo de Prueba, habilitamos unos campos u otros
        def panelSelected = evento.target.parent.parent
        def btnBorrarPrueba = Selectors.find(panelSelected, " .borrarPruebaExp")
        btnBorrarPrueba[0].disabled = false

    }

    void clickBorrarPrueba(Event evento){
        log.debug("Boton Borrar Prueba. Mostramos alerta para confirmacion")

        def listbox= evento.target.nextSibling.nextSibling.children
        def itemSelected
        listbox.each { it->
            if(it instanceof Listitem) {
                if(it.selected)
                    itemSelected = it
            }
        }
        //Recogemos el id de la prueba que queremos borrar, guardado en el value del listItem
        Map datos = [:]
        datos.id_prueba = itemSelected.value
        // Mostramos un cuadro de diálogo pidiendo la confimación de la operación al usuario
        Messagebox.show("¿Confirma que desea borrar la Prueba con ID: " + datos.id_prueba + "?",
                "Confirmar Acción", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, [
                    onEvent: { e ->
                        if(e.data.intValue() == Messagebox.YES) {
                            pruebaEntidadService.borrar(datos)
                            //Refrescamos los datos llamando al etodo activacion del propio expediente
                            this.activacion()
                        }
                    }
                ] as org.zkoss.zk.ui.event.EventListener)
    }

    //Este metodo gestiona las url de los informes de citologias, ya que los previos a 27/06/2025 no se visualizan
    //por cambio de repositorio de
    def clickInforme(Event evento){
        //Recuperamos los datos de la prueba asociada al evento del clic del boton del enlace
        def pruebaSelect = evento.getTarget().getAttribute("prueba")
        //Recuperamos la URL
        String urlPdf = pruebaSelect.PDF_RESULT
        //Comprobamos que la url no se null o está vacia
        if(urlPdf != null && !urlPdf.isEmpty()){
            //Comprobamos que el tipo de prueba asociado es citologia y que la URL contiene el texto correspondiente a la URL no valida para mostrar el mensaje de aviso
            if(pruebaSelect.ID_TIPO_PRUEBA == Constantes.ID_TIPO_PRUEBA_CITOLOGIA && urlPdf.contains(Constantes.INI_URL_NO_VALIDA)){
                ventanaAviso("Este informe debe ser visualizado desde Ágora")
            }
            else{
                //En caso contrario se abre la url en una pestaña anexa
                Executions.getCurrent().sendRedirect(urlPdf, "_blank")
            }
        }
    }

    @Listen("onClick=#btnInsertarContacto")
    void clickInsertarContacto() {
        log.debug("Click Nuevo Contacto")

        def idEstudio = estudioEntidadService.recuperarUltimoEstudioPorIdExpediente(expediente.seleccion)

        Map contacto = [:]
        contacto.id_contacto = NUEVO_REGISTRO
        contacto.id_estudio = idEstudio[0]
        contacto.id_expediente = expediente.seleccion
        contacto.id_persona = leerValorComponente(expediente_id_persona)
        contacto.titulo = "Insertar nuevo Contacto"
        modalContacto(contacto)
    }

    @Listen("onClick=#btnBorrarContacto")
    void clickBorrarContacto(Event evento) {
        log.debug("Click Borrar Contacto")
        def listbox = evento.target.nextSibling.nextSibling
        def listItems = listbox.items
        def itemSelected
        listItems?.each { it ->
            if(it.selected)
                itemSelected = it
        }

        def idEstudio = Selectors.find(itemSelected, ".idEstudioContacto")
        Map contacto = [:]
        contacto.contador = itemSelected.value
        contacto.id_estudio = idEstudio[0].value

        //Recuperamos en contacto mediante el id estudio y el contador
        def contactoSelect = contactoEntidadService.recuperar(contacto)
        contactoSelect.motivo_contacto = contactoSelect.ID_MOTIVO_CONTACTO
        //Se recuperan todos los contactos negativos del estudio
        def contadorReg = contactoEntidadService.recuperarNumContactosNegPorEstudio(contactoSelect)

        //Si el motivo contacto puede generar un doc ilocalizable, se incluye el valor de la id de la plantilla al map de datos
        if(contactoSelect.motivo_contacto == Constantes.ID_COMUNICACION_RESULT_VPH ||
                contactoSelect.motivo_contacto == Constantes.ID_COMUNICACION_CITA_CITO ||
                contactoSelect.motivo_contacto == Constantes.ID_COMUNICACION_CITA_COTEST){
            contacto.id_tipo_doc = new Integer(pandoraService.leerVariableGlobal(Constantes.DOC_ILOCALIZABLE))
        }
        //Si el motivo contacto ha generado doc postal de ilocalizable
        // se recuperan los documentos asociados al estudio
        def doc = documentoEntidadService.countDocPorIdEstudioTipoDoc(contacto)

        // Mostramos un cuadro de diálogo pidiendo la confimación de la operación al usuario
        Messagebox.show("¿Confirma que desea borrar el Contacto?",
                "Confirmar Acción", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, [
                    onEvent: { e ->
                        if(e.data.intValue() == Messagebox.YES) {
                            //Se evalua si el tipo de contacto ha llegado al max de notificaciones y generó doc postal
                            //para evitar el borrado
                            if(contadorReg.NUM == Constantes.NUM_MAX_CONTACTOS && doc.NUM > 0){
                                //Al editar, si hay tres contactos negativos y documento generado no se permite la edicion del contacto registrado
                                //Lanzamos mensaje de aviso al usuario
                                ventanaAviso("No puede eliminar un contacto que ha generado documento postal.")
                                //Capturamos y detenemos la propagación del evento de borrado
                                evento.stopPropagation()
                            }else{
                                contactoEntidadService.borrar(contacto)
                            }
                            this.activacion()
                        }
                    }
                ] as org.zkoss.zk.ui.event.EventListener)
    }

    @Listen("onClick=#lbExpediente_contacto listitem")
    void clickItemContacto() {
        btnBorrarContacto.disabled = false
    }
    
    @Listen("onDoubleClick=#lbExpediente_contacto listitem")
    void dobleClickItemContacto(Event evento) {
        def itemSelected = evento.target
        def contador = itemSelected.value
        def idEstudio = Selectors.find(itemSelected, " .idEstudioContacto")
        Map contacto = [:]
        contacto.id_contacto = 1
        contacto.id_estudio = idEstudio[0].value
        contacto.contador = contador
        contacto.id_expediente = expediente.seleccion
        contacto.id_persona = leerValorComponente(expediente_id_persona)
        contacto.titulo = "Detalles del Contacto"
        modalContacto(contacto)
    }

    @Listen("onClick=#lbExpediente_notificacion listitem")
    def clickItemNotificacion() {
        // Habilitamos Descargar Documento
        btnDescargarNotificacionExp.disabled = false
    }

    @Listen("onClick=#btnDescargarNotificacionExp")
    void clickDescargar() {
        def itemSelected = lbExpediente_notificacion.selectedItem.value
        def idDocumento = itemSelected.id

        def parametrosReport = utilsPcacervixService.parametrosPlantillaJasper(session.ruta_jasper)

        def parametrosSQL
        parametrosSQL = ['dfIdDocumento' : + idDocumento]

        def codigoDoc
        codigoDoc= expedienteService.codigoTipoDocumento(idDocumento)
        String codigopdf = codigoDoc.codigo.value.toString().toLowerCase() +".pdf"
        
        def resultado = pandoraService.imprimirReport(codigoDoc.codigo, parametrosSQL, parametrosReport, Constantes.ID_PROYECTO)

        if (resultado) {
            Filedownload.save(resultado, "application/pdf", codigopdf)
            Map datos = [:]
            datos.id_documento = idDocumento
            datos.login = parametros.login_unico
            datos.operacion = 'D' //Descarga
            documentoEntidadService.auditoriaDocumentos(datos)
        } else {
            ventanaError("Error. El documento está vacio")
        }
    }

    /* @Listen("onClick=#btnCrearCitaExp")
     def clickInsertarCita(Event evento) {
     log.debug("Click Nueva Cita")
     def tapPanel = evento.target.parent
     def idEstudioPrueba = tapPanel.getAttribute("estudioId")
     Map cita = [:]
     cita.id_cita = NUEVO_REGISTRO
     cita.id_estudio = idEstudioPrueba
     cita.id_expediente = expediente.seleccion
     cita.titulo = "Insertar nueva cita"
     modalCita(cita)
     } */

    @Listen("onDoubleClick= #lbExpediente_cita listitem")
    def clickDobleCitas(Event evento) {
        log.debug("Doble Click en Cita. Ventana emergente con Detalle de Cita")
        def itemSelected = evento.target
        def idCitaSeleccionado = itemSelected.value
        Map cita = [:]
        cita.id_cita = idCitaSeleccionado
        cita.id_expediente = expediente.seleccion
        cita.titulo = "Detalles de la Cita"

        modalCita(cita)
    }

    /*
    @Listen("onClick=#btnBorrarArchivarExp")
    def clickBorrarArchivarExp() {
        log.debug("Click en boton Borrar/Archvar expediente. Mostramos ventana emergente")

        Messagebox.show("¿Desea Archivar el registro actual sin borrarlo?"+
                "\n Si elige SI, podrá consultarse cambiando la fecha de referencia."+
                "\n Si elige NO, se borrarán los datos, sin posibilidad de consulta",
                "Archivar / Borrar", Messagebox.YES | Messagebox.NO | Messagebox.CANCEL, Messagebox.QUESTION,
                new org.zkoss.zk.ui.event.EventListener(){
                    void onEvent(Event e){
                        if(Messagebox.ON_YES.equals(e.getName())){
                            log.debug("Archivamos Expediente")
                            //						archivarExpediente()
                        }
                        else if(Messagebox.ON_NO.equals(e.getName())){
                            log.debug("Borramos Expediente")
                            //						hayCambiosPendientesFormulario(true)
                            //						borrarExpediente()
                        }
                    }
                })
    }
    */

    @Listen("onClick=#btnInsertarNotificacion")
    def clickInsertarNotificacion() {

    }

    /*
    @Listen("onClick=#btnVersionarExp")
    def clickVersionarExp() {
        log.debug("Click en boton Versionar expediente")

        Messagebox.show("Se va a versionar el expediente con ID: ${expediente.seleccion}. ¿Desea confirmar?",
                "Versionar Expediente", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION,
                new org.zkoss.zk.ui.event.EventListener(){
                    public void onEvent(Event e){
                        if(Messagebox.ON_YES.equals(e.getName())){
                            log.debug("Versionando Expedient con ID:" + expediente.seleccion)
                            //						versionarExpediente(expediente.seleccion)
                        }
                        else if(Messagebox.ON_NO.equals(e.getName())){
                            return
                        }
                    }
                })

    }
     */

    //	void archivarExpediente() {
    //		Map datos = [:]
    //		datos.fecha_baja = new Date()
    //		datos.id_expediente = expediente.seleccion
    //		datos.fecha_alta = expediente_fecha_alta
    //		datos.fecha_baja = expediente_fecha_baja
    //		expedienteService.darDeBajaExp(datos)
    //		expediente.modificar(datos)
    //	}

    void borrarExpediente() {
        Map datos = [:]
        datos.id_expediente = expediente.seleccion
        datos.fecha_alta = expediente_fecha_alta
        expediente.borrar(datos)
    }

    def validacionBorrarProceso(id_proceso) {
        //Comprobamos si tiene estudios asociados
        def estudiosProc = estudioEntidadService.recuperarEstudioPorIdProceso(id_proceso)
        boolean tieneEstudios = false
        estudiosProc?.each {
            tieneEstudios = true
        }
        return tieneEstudios
    }

    void modificarProceso(proc) {
        procesoActual = proc
        log.debug("modificarProceso: ${procesoActual}")

        // Establecemos el composer llamante
        wndEditarProcesosExpediente.getAttribute("\$composer").invocador= this
        wndEditarProcesosExpediente.getAttribute("\$composer").activacion()
        // Hacemos visible la ventana modal
        wndEditarProcesosExpediente.visible = true
        wndEditarProcesosExpediente.doModal()
    }

    void modificarEstudio(estud) {
        estudioActual = estud
        log.debug("modificarEstudio: ${estudioActual}")

        // Establecemos el composer llamante
        wndEditarEstudiosProceso.getAttribute("\$composer").invocador= this
        wndEditarEstudiosProceso.getAttribute("\$composer").activacion()
        // Hacemos visible la ventana modal
        wndEditarEstudiosProceso.visible = true
        wndEditarEstudiosProceso.doModal()
    }

    void modalPrueba(pru) {
        pru.nombre_paciente = expediente_persona_nombre.value
        pru.apellido1_paciente = expediente_persona_apellido1.value
        pru.apellido2_paciente = expediente_persona_apellido2.value
        pru.fec_nacim = expediente_persona_fecha_nacimiento.value
        pruebaActual = pru
        log.debug("modificarPrueba: ${pruebaActual}")

        // Establecemos el composer llamante
        wndEditarPruebasEstudio.getAttribute("\$composer").invocador= this
        wndEditarPruebasEstudio.getAttribute("\$composer").activacion()
        // Hacemos visible la ventana modal
        wndEditarPruebasEstudio.visible = true
        wndEditarPruebasEstudio.doModal()
        //		wndEditarPruebasEstudio.setTitle(pru.titulo)
    }

    void modalContacto(contacto) {
        contacto.nombre_paciente = expediente_persona_nombre.value
        contacto.apellido1_paciente = expediente_persona_apellido1.value
        contacto.apellido2_paciente = expediente_persona_apellido2.value
        contacto.fec_nacim = expediente_persona_fecha_nacimiento.value
        contactoActual = contacto
        log.debug("modificarContacto: ${contactoActual}")

        // Establecemos el composer llamante
        wndEditarContactoEstudio.getAttribute("\$composer").invocador= this
        wndEditarContactoEstudio.getAttribute("\$composer").activacion()
        // Hacemos visible la ventana modal
        wndEditarContactoEstudio.visible = true
        wndEditarContactoEstudio.doModal()
    }

    void modalCita(cita) {
        citaActual = cita
        log.debug("modificarCita: ${citaActual}")

        // Establecemos el composer llamante
        wndEditarCitaEstudio.getAttribute("\$composer").invocador= this
        wndEditarCitaEstudio.getAttribute("\$composer").activacion()
        // Hacemos visible la ventana modal
        wndEditarCitaEstudio.visible = true
        wndEditarCitaEstudio.doModal()
    }

    void recuperarContactos(expediente) {
        def contactosExpediente = contactoEntidadService.recuperarContactosPorIdExpediente(expediente)
        lbExpediente_contacto.actualizar(contactosExpediente)
    }

    void recuperarCitas(expediente) {
        def citasExpediente = citaEntidadService.recuperarPorIdExpediente(expediente)
        lbExpediente_cita.actualizar(citasExpediente)
    }
    
    void recuperarNotificaciones(expediente) {
        def notificacionesExpediente = documentoEntidadService.recuperarDocPorIdExpediente(expediente)
        lbExpediente_notificacion.actualizar(notificacionesExpediente)
    }

    //	def versionarExpediente(idExpediente) {
    //
    //		// Establecemos el composer llamante
    //		wndVersionadoExpediente.getAttribute("\$composer").invocador= this
    //		wndVersionadoExpediente.getAttribute("\$composer").activacion()
    //		// Hacemos visible la ventana modal
    //		wndVersionadoExpediente.visible = true
    //		wndVersionadoExpediente.doModal()
    //	}

    

    void limpiar() {
        if(!lbExpedienteProcesos.getItems().isEmpty())
            limpiarValorComponente(lbExpedienteProcesos)
        if(!lbExpedienteEstudios.getItems().isEmpty())
            limpiarValorComponente(lbExpedienteEstudios)
        if(!lbPruebasExp.getItems().isEmpty())
            limpiarValorComponente(lbPruebasExp)
        if(!lbExpediente_contacto.getItems().isEmpty())
            limpiarValorComponente(lbExpediente_contacto)
        if(!lbExpediente_cita.getItems().isEmpty())
            limpiarValorComponente(lbExpediente_cita)
        limpiarValorComponente(expediente_fecha_alta)
        limpiarValorComponente(expediente_fecha_baja)
        limpiarValorComponente(expediente_id_persona)
        limpiarValorComponente(expediente_id)
        limpiarValorComponente(cbProcesoSeleccionado)
        if(!lbExpediente_vacunacion.getItems().isEmpty()) {
            limpiarValorComponente(lbExpediente_vacunacion)
        }
    }

    void deshabilitar() {
    }


}
