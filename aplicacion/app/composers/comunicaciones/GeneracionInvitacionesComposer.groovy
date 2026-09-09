package  cribadocervix.comunicaciones

import org.zkoss.zk.ui.select.annotation.Listen
import org.zkoss.zul.Button
import org.zkoss.zul.Checkbox
import org.zkoss.zul.Datebox
import org.zkoss.zul.Intbox
import org.zkoss.zul.Label
import org.zkoss.zul.Listbox
import org.zkoss.zul.Textbox
import base.EntidadFormulario
import base.FormularioComposer
import  cribadocervix.PcaCervixService
import  cribadocervix.UtilsPcacervixService
import  cribadocervix.entidad.EmisionInvitacionEntidadService
import groovy.util.logging.Slf4j


@Slf4j
class GeneracionInvitacionesComposer extends FormularioComposer{
	
	UtilsPcacervixService utilsPcacervixService
	PcaCervixService pcaCervixService
	EmisionInvitacionEntidadService emisionInvitacionEntidadService
	
	EntidadFormulario emision_invitacion
	
	ConsultarInvitacionesComposer invocador
	
	Textbox emisionInvitacion_id
	Textbox emisionInvitacion_id_profesional
	Textbox emisionInvitacion_unidad_funcional
	Textbox emisionInvitacion_fecha
	Intbox emisionInvitacion_edad_desde
	Intbox emisionInvitacion_edad_hasta
	Intbox emisionInvitacion_anyo_nacimiento
	Textbox emisionInvitacion_descripcion
	Listbox emisionInvitacion_area_salud
	Listbox emisionInvitacion_zona_salud
	Checkbox emisionInvitacion_reinvitacion
	Datebox emisionInvitacion_fecha_cart_anterior_a
	
	Label tituloVentanaInv
	
	Button btnGuardarInv
	Button btnDescartarInv
	Button btnCerrarVentana
	
	Map parametros = [:]
	
	def id
	def profesional
	def fecha
	def unidadFuncional
	def editar


	def despuesDeComponer() {	
		
		emision_invitacion = new EntidadFormulario("emisionInvitacion","id", this)
		
		// Configuramos la entidad
		emision_invitacion.configurar("pcacervix", "emision_invitacion")
					
		servicioOperaciones = emisionInvitacionEntidadService
		
		establecerBotonGuardar(btnGuardarInv)
		establecerBotonDescartar(btnDescartarInv)
	}
	
	def activacion() {	
				
		Date fechaHoy = new Date()
		profesional = session.profesional
		fecha = fechaHoy.format("dd/MM/yyyy")
		unidadFuncional = session.unidad_funcional?.descripcion
		
		if (invocador) {
			
			escribirValorComponente(tituloVentanaInv, invocador.invitacionActual.titulo)
			rellenarListbox(emisionInvitacion_area_salud, utilsPcacervixService.recuperarAreasSalud(), "Seleccione.." )
			rellenarListbox(emisionInvitacion_zona_salud, utilsPcacervixService.recuperarZonasBasicasSalud(), "Seleccione.." )
            
            def idEmision = invocador.invitacionActual.id
			emision_invitacion.seleccion = idEmision
			btnGuardarInv.visible = true
			btnDescartarInv.visible = true
			btnCerrarVentana.visible = false
		
			if (invocador.invitacionActual.nuevo) {
                log.info("MODAL CREACION EMISION")
				Map m = [:]
				m.id = idEmision
				m.fecha = new Date().clearTime()
				emision_invitacion.insertar(m)
				
				emisionInvitacion_id.value = idEmision
				emisionInvitacion_id_profesional.value = profesional
				emisionInvitacion_unidad_funcional.value = unidadFuncional
				emisionInvitacion_fecha.value = fecha

				if (!emisionInvitacion_zona_salud.getItems().isEmpty()) {
					limpiarValorComponente(emisionInvitacion_zona_salud)
				}
				
				emisionInvitacion_id.disabled = true
				emisionInvitacion_id_profesional.disabled = true
				emisionInvitacion_unidad_funcional.disabled = true
				emisionInvitacion_fecha.disabled = true
				emisionInvitacion_zona_salud.disabled = true
				emisionInvitacion_anyo_nacimiento.disabled = false
				emisionInvitacion_edad_desde.disabled = false
				emisionInvitacion_edad_hasta.disabled = false
				emisionInvitacion_fecha_cart_anterior_a.disabled = false


			} else {
				
				emisionInvitacion_id.disabled = true
				emisionInvitacion_id_profesional.disabled = true
				emisionInvitacion_unidad_funcional.disabled = true
				emisionInvitacion_fecha.disabled = true
				emisionInvitacion_reinvitacion.disabled = false
				emisionInvitacion_fecha_cart_anterior_a.disabled = false
				emisionInvitacion_anyo_nacimiento.disabled = false
				emisionInvitacion_descripcion.disabled = false
				emisionInvitacion_area_salud.disabled = false
				emisionInvitacion_zona_salud.disabled = false
				emisionInvitacion_edad_desde.disabled = false
				emisionInvitacion_edad_hasta.disabled = false
				
				def datosInvitacion = emision_invitacion.recuperar(invocador.invitacionActual)
				
				def zonasSalud = utilsPcacervixService.recuperarZonasIdArea(datosInvitacion.area_salud)
				rellenarListbox(emisionInvitacion_zona_salud, zonasSalud, "Seleccione.." )
				emisionInvitacion_zona_salud.disabled = false
				
				escribirValoresVista("emisionInvitacion", datosInvitacion)
				
				emisionInvitacion_fecha.value = datosInvitacion.fecha.format("dd/MM/yyyy")
				emisionInvitacion_anyo_nacimiento.value = datosInvitacion.anyo_nacimiento
								
				def persona = pcaCervixService.recuperarPersona(datosInvitacion.id_profesional)
				emisionInvitacion_id_profesional.value = persona.nombre_completo
				
				def uni_funcional = utilsPcacervixService.recuperarUnidadFuncional(datosInvitacion.unidad_funcional)
				emisionInvitacion_unidad_funcional.value = uni_funcional.descripcion
	
				editar = true
				
				if (datosInvitacion.num_doc_generados == null)  {
				
					if (!datosInvitacion.anyo_nacimiento.equals(null)) {
						emisionInvitacion_edad_desde.disabled = true
						emisionInvitacion_edad_hasta.disabled = true
					} else if (!datosInvitacion.edad_desde.equals(null) || !datosInvitacion.edad_hasta.equals(null)) {
						emisionInvitacion_anyo_nacimiento.disabled = true
					} else if (!datosInvitacion.fecha_cart_anterior_a == null){
                        emisionInvitacion_fecha_cart_anterior_a.disabled = true
                    }
					btnDescartarInv.disabled = false
					
				} else {
					
					escribirValorComponente(tituloVentanaInv, "Detalle de la Emisión")
					emisionInvitacion_anyo_nacimiento.disabled = true
					emisionInvitacion_descripcion.disabled = true 
					emisionInvitacion_area_salud.disabled = true
					emisionInvitacion_zona_salud.disabled = true
					emisionInvitacion_edad_desde.disabled = true
					emisionInvitacion_edad_hasta.disabled = true
                    emisionInvitacion_fecha_cart_anterior_a.disabled = true
                    emisionInvitacion_reinvitacion.disabled = true

					//Ocultamos los botones guardar/cancelar/limpiar
					btnGuardarInv.disabled = true
					btnGuardarInv.visible = false
					btnDescartarInv.visible = false
					//Mostramos boton cerrar ventana (X)
					btnCerrarVentana.visible = true
				}
			}
		}
		
	}
	
	@Listen("onSelect=#emisionInvitacion_area_salud")
	void rellenarZonasSaludModal() {
		def idArea = leerValorComponente(emisionInvitacion_area_salud.getSelectedItem())
		if (idArea != null) {
			rellenarListbox(emisionInvitacion_zona_salud, utilsPcacervixService.recuperarZonasIdArea(idArea), "Seleccione.." )
			emisionInvitacion_zona_salud.disabled = false
		}else {
			rellenarListbox(emisionInvitacion_zona_salud, utilsPcacervixService.recuperarZonasBasicasSalud(), "Seleccione.." )
			emisionInvitacion_zona_salud.disabled = true
			
		}
	}
	
	@Listen("onChange=#emisionInvitacion_anyo_nacimiento")
	def desactivarDesdeHasta () {
		def nac = leerValorComponente(emisionInvitacion_anyo_nacimiento)
		if (nac.equals(null)) {
			emisionInvitacion_edad_desde.disabled = false
			emisionInvitacion_edad_hasta.disabled = false
		} else {
			emisionInvitacion_edad_desde.disabled = true
			emisionInvitacion_edad_hasta.disabled = true
		}
	}
	
	@Listen("onChange=#emisionInvitacion_edad_hasta; onChange=#emisionInvitacion_edad_desde")
	def desactivarEdad() {
		def desde = leerValorComponente(emisionInvitacion_edad_desde)
		def hasta = leerValorComponente(emisionInvitacion_edad_hasta)
		
		if (desde.equals(null) && hasta.equals(null)) {
			emisionInvitacion_anyo_nacimiento.disabled= false
		} else {
			emisionInvitacion_anyo_nacimiento.disabled = true
		}
		
	}
	
	@Listen("onClick=button#btnGuardarInv")
	void clickGuardarCambiosEmisionInvitacion() {
		
		def idProf = session.idProfesional
		def unFun = session.unidad_funcional?.id
		emisionInvitacion_id_profesional.value = idProf.toString()
		emisionInvitacion_unidad_funcional.value = unFun.toString()
			
	}
	
	void despuesDeGuardar() {	
		if(invocador) {
			parametros = [:]
			def lista = emisionInvitacionEntidadService.buscar(parametros)
			invocador.lbListaInvitaciones.actualizar(lista)	
			// Ocultamos la ventana modal
			contenedor.visible = false
			//Limpiamos el invocador para evitar el error de 'Cambios Pendientes'
			invocador = null
			// Limpiamos el contenido de la ventana para su próxima utilización
			limpiar()
		}
	}

	void limpiar() {
		limpiarValorComponente(emisionInvitacion_edad_desde)
		limpiarValorComponente(emisionInvitacion_edad_hasta)
		limpiarValorComponente(emisionInvitacion_anyo_nacimiento)
		limpiarValorComponente(emisionInvitacion_descripcion)
		limpiarValorComponente(emisionInvitacion_reinvitacion)
		limpiarValorComponente(emisionInvitacion_fecha_cart_anterior_a)

		if (!emisionInvitacion_area_salud.getItems().isEmpty()) {
			limpiarValorComponente(emisionInvitacion_area_salud)			
		}
		
		if (!emisionInvitacion_zona_salud.getItems().isEmpty()) {
			limpiarValorComponente(emisionInvitacion_zona_salud)
			emisionInvitacion_zona_salud.disabled = true
		}
				
		emisionInvitacion_anyo_nacimiento.disabled = false
		emisionInvitacion_edad_desde.disabled = false
		emisionInvitacion_edad_hasta.disabled = false
	}

	@Listen("onClick=#btnDescartarInv, #btnCerrarVentana")
	void clickCerrarVentana() {
		log.debug("click Descartar cambios")
		// Ocultamos la ventana modal
		contenedor.visible = false
		
		emisionInvitacion_anyo_nacimiento.disabled = false
		emisionInvitacion_descripcion.disabled = false
		emisionInvitacion_area_salud.disabled = false
		emisionInvitacion_zona_salud.disabled = false
		emisionInvitacion_edad_desde.disabled = false
		emisionInvitacion_edad_hasta.disabled = false
		btnGuardarInv.disabled = false
		
		descartarOperaciones()
		//Limpiamos el invocador para evitar el error de 'Cambios Pendientes'
		invocador = null
		limpiar()
	}

}
