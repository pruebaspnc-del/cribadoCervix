package  cribadocervix.expedientes

import org.springframework.stereotype.Component
import org.zkoss.zk.ui.select.annotation.Listen
import org.zkoss.zul.Button
import org.zkoss.zul.Label
import base.EntidadFormulario
import base.FormularioComposer

import  cribadocervix.entidad.CitaEntidadService
import groovy.util.logging.Slf4j

@Slf4j
@Component
class EditarCitaEstudioComposer extends FormularioComposer{
	
	//Servicios
	CitaEntidadService citaEntidadService
	EntidadFormulario cita
	
	ExpedienteComposer invocador
	
	//Componentes vista
	Label tituloCitaExp
	
	//Botones
	Button btnGuardarCitaEst
	Button btnDescartarCitaEst
	Button btnCerrarVentanaCita
				
	def despuesDeComponer() {
		cita = new EntidadFormulario("cita","id_cita", this)
		// Configuramos la entidad
		cita.configurar("pcacervix", "cita")
		// Recuperamos el id de profesional y su unidad funcional seleccionada del usuario actual
		def idProfesional = session.idProfesional
		def idUnidadFuncional = session.unidadesFuncionales[0].id
		def login = session.loginUnico
		// Establecemos los parámetros por defecto para todas las operaciones de entidades
		def parametros = [id_profesional: idProfesional, id_unidad_funcional: idUnidadFuncional, login: login]
		cita.parametros = parametros
		// Establecer el servicio que va a gestionar la lista de operaciones
		servicioOperaciones = citaEntidadService
	
		establecerBotonGuardar(btnGuardarCitaEst)
		establecerBotonDescartar(btnDescartarCitaEst)
	}
	
	def activacion() {
		limpiar()
		
		if(invocador) {
			cita.activarComponentesVista()
			btnDescartarCitaEst.disabled = false
			btnGuardarCitaEst.disabled = false
			escribirValorComponente(tituloCitaExp, invocador.citaActual.titulo)
			
			if (invocador.citaActual.id_cita == NUEVO_REGISTRO) {
				//Hacemos visibles los botones Guardar y Descartar
				btnGuardarCitaEst.visible = true
				btnDescartarCitaEst.visible = true
				//Ocultamos el boton Cerrar Ventana, puesto que ya lo hace el de Descartar
				btnCerrarVentanaCita.visible = false
				//Crear mapa con datos por defecto
				Map m = [:]
				m.id = ""
				m.fecha = new Date()
				m.profesional = cita.parametros.login
				cita.insertar(m)
			}
			else {
				btnCerrarVentanaCita.visible = true
				def datos =[:]
				datos.id_cita = invocador.citaActual.id_cita
				cita.seleccion= invocador.citaActual.id_cita
				def datosCita = cita.recuperar(datos)
				if(datosCita) {
					if(datosCita.no_asiste == 0)
						datosCita.no_asiste = "NO"
					if(datosCita.no_asiste == 1)
						datosCita.no_asiste = "SI"
					if(datosCita.no_asiste == null)
						datosCita.no_asiste = "Pendiente"
				}
				escribirValoresVista("cita", datosCita)
				desactivarComponentesVista("cita")
				btnDescartarCitaEst.visible = false
				btnGuardarCitaEst.visible = false
			}
		}
	}
	
	def desactivacion() {
		log.debug("procesando DESACTIVACION Cita")
	}
	
	@Listen("onClick=button#btnDescartarCitaEst, #btnCerrarVentanaCita")
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
	
	void despuesDeGuardar() {
		// Refrescamos la lista de procesos del expediente
		log.debug("Refrescamos datos de los Contactos")
	
		if(invocador) {
			// Recuperamos las citas y actualizados el expediente
			invocador.recuperarCitas(invocador.citaActual.id_expediente)
			// Ocultamos la ventana modal
			contenedor.visible = false
			//Limpiamos el invocador para evitar el error de 'Cambios Pendientes'
			invocador = null
			// Limpiamos el contenido de la ventana para su próxima utilización
			limpiar()
		}
	}
	
	void limpiar() {
		// Limpiamos todos los componentes de la ventana
	}

}
