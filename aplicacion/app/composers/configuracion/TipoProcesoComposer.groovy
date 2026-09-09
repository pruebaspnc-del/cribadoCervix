package  cribadocervix.configuracion

import org.springframework.beans.factory.annotation.Autowired
import org.zkoss.zk.ui.event.Event
import org.zkoss.zk.ui.select.annotation.Listen
import org.zkoss.zul.Listbox
import org.zkoss.zul.Textbox
import org.zkoss.zul.Messagebox
import org.zkoss.zul.Window
import org.zkoss.zul.Button
import org.zkoss.zul.Label
import base.EntidadFormulario
import base.FormularioComposer
import  cribadocervix.AuxiliaresService
import  cribadocervix.entidad.TipoEstudioEntidadService
import  cribadocervix.entidad.TipoProcesoEntidadService
import groovy.util.logging.Slf4j

@Slf4j
class TipoProcesoComposer extends FormularioComposer {

	// Servicios
	AuxiliaresService auxiliaresService
	TipoProcesoEntidadService tipoProcesoEntidadService
	@Autowired
	TipoEstudioEntidadService tipoEstudioEntidadService

	//Entidad
	EntidadFormulario tipoProceso

	Listbox lbListaProcesoTipo
	Textbox tbBuscarProcesoTipo
	Listbox lbListaEstudioProcTipo
	Listbox lbListaResultadoProcTip
	Listbox lbListaEstudios
	Label tituloTipoProc
	Textbox tipoProceso_id
	Textbox tipoProceso_codigo
	Textbox tipoProceso_descripcion
	Textbox id_proc_seleccionado

	Window wndEdicionTipoProceso
	Window wndAsociarEstudio

	Button btnGuardarTipoProc
	Button btnDescartarTipoProc
	Button btnEditarTipoProc
	Button btnCerrarVentanaTipoProc
	Button btnBorrarProceso
	Button btnInsertarProceso
	Button btnGuardarEstudioAsociado
	Button btnDescartarEstudioAsociado
	Button btnDesasociarEstudio

	Map tipoProcesoActual = [:]
	String tipoProcesoString = 'tipoProceso'

	def despuesDeComponer () {
		tipoProceso = new EntidadFormulario(tipoProcesoString,"id", this)

		// Configuramos la entidad
		tipoProceso.configurar("pcacervix", "proceso_tipo")
		// Recuperamos el id de profesional y su unidad funcional seleccionada del usuario actual
		def idProfesional = session.idProfesional
		def idUnidadFuncional = session.unidadesFuncionales[0].id
		def login = session.loginUnico
		// Establecemos los parámetros por defecto para todas las operaciones de entidades
		def parametros = [id_profesional: idProfesional, id_unidad_funcional: idUnidadFuncional, login: login]
		tipoProceso.parametros = parametros
		// Establecer el servicio que va a gestionar la lista de operaciones
		servicioOperaciones = tipoProcesoEntidadService
		// Configuramos bototnes de la modal
		establecerBotonGuardar(btnGuardarTipoProc)
		establecerBotonDescartar(btnDescartarTipoProc)
		establecerBotonDescartar(btnDescartarEstudioAsociado)
		establecerBotonGuardar(btnGuardarEstudioAsociado)
	}

	def activacion() {
		btnBorrarProceso.disabled = true
		btnDesasociarEstudio.disabled = true
		buscar()
	}

	@Listen("onClick=#btnBuscarProcesos")
	def buscar () {
		limpiarTodas()
		def tiposProceso = tipoProcesoEntidadService.recuperarTodos()
		lbListaProcesoTipo.actualizar(tiposProceso)
		def idProceso =  lbListaProcesoTipo.findResult { 1 }

		//buscamos los Estudios asociados al proceso
		def listaEstudiosPorProceso = tipoEstudioEntidadService.recuperarTipoEstudioPorIdProceso(idProceso);
		lbListaEstudioProcTipo.actualizar(listaEstudiosPorProceso)

		//buscamos los Resultados asociados al proceso
		def listaResultadosPorProceso = auxiliaresService.buscarTipoResultadosPorProceso(idProceso);
		lbListaResultadoProcTip.actualizar(listaResultadosPorProceso)

		/* Marcamos la primera fila como seleccionada para saber de que proceso vienen los estudios 
		que aparecen al carga la pagina. Al estar ordenado ascendentemente, siempre marcamos la primera fila. */  
		lbListaProcesoTipo.selectedIndex = 0;
	}

	@Listen("onClick=#lbListaProcesoTipo listitem")
	void clickTipoProceso(Event evento) {
		def selected = evento.target
		def idProc = selected.value

		def listaEstudiosPorProceso = tipoEstudioEntidadService.recuperarTipoEstudioPorIdProceso(idProc)
		lbListaEstudioProcTipo.actualizar(listaEstudiosPorProceso)
		def listaResultadosPorProceso = auxiliaresService.buscarTipoResultadosPorProceso(idProc	)
		lbListaResultadoProcTip.actualizar(listaResultadosPorProceso)

		if (tipoProceso.puedeBorrar) {
			btnBorrarProceso.disabled = false
		}
	}

	@Listen("onClick=#btnInsertarProceso")
	void nuevoProcesoTipo() {
		// Configuramos bototnes de la modal
//		establecerBotonGuardar(btnGuardarTipoProc)
//		establecerBotonDescartar(btnDescartarTipoProc)

		Map tipoProc = [:]
		tipoProc.id = ""
		tipoProc.titulo = "Nuevo Tipo de Proceso"
		tipoProceso.insertar(tipoProc)
		//Mostramos botones guardar y cancelar
		btnDescartarTipoProc.visible = true
		btnGuardarTipoProc.visible = true
		//Ocultamos botones Editar y Cerrar ventana
		btnCerrarVentanaTipoProc.visible = false
		btnEditarTipoProc.visible = false
		escribirValorComponente(tituloTipoProc, tipoProc.titulo)
		activarComponentesVista(tipoProcesoString)
		abrirModal(tipoProc)
	}

	@Listen("onDoubleClick=#lbListaProcesoTipo listitem")
	void dobleClickTipoProceso(Event evento) {
		def selected = evento.target
		def idProc = selected.value

		Map tipoPro = [:]
		tipoPro.id_tipo_proceso = idProc 
		tipoPro.titulo = "Detalles del Tipo de Proceso"
		tipoProcesoActual = tipoProceso.recuperar(tipoPro)
		//Ocultamos botones guardar y cancelar
		btnDescartarTipoProc.visible = false
		btnGuardarTipoProc.visible = false
		//Mostramos botones Editar y Cerrar ventana
		btnCerrarVentanaTipoProc.visible = true
		btnEditarTipoProc.visible = true

		escribirValorComponente(tituloTipoProc, tipoPro.titulo)
		abrirModal(tipoProcesoActual)
		desactivarComponentesVista(tipoProcesoString)
	}

	def clickEditarTipoProceso() {
		activarComponentesVista(tipoProcesoString)
		//Ocultamos bototnes Editar y Cerrar ventana
		btnCerrarVentanaTipoProc.visible = false
		btnEditarTipoProc.visible = false
		//Mostramos botones guardar y cancelar
		btnDescartarTipoProc.visible = true
		btnDescartarTipoProc.disabled = false
		btnGuardarTipoProc.visible = true
		//Cambiamos el titulo de la modal
		Map titulo = [:]
		titulo.titulo = "Edición del Tipo de Proceso"
		escribirValorComponente(tituloTipoProc, titulo.titulo)
		// Configuramos botones de la modal
//		establecerBotonGuardar(btnGuardarTipoProc)
//		establecerBotonDescartar(btnDescartarTipoProc)
	}

	def abrirModal(Map tipoProceso) {
		// Hacemos visible la ventana modal
		wndEdicionTipoProceso.visible = true
		wndEdicionTipoProceso.doModal()
		escribirValoresVista(tipoProcesoString, tipoProceso)
	}

	@Listen("onClick=#btnBorrarProceso")
	void clickBorrarProceso() {
		if (!tipoProceso.puedeBorrar) {
			return
		}
	   log.debug("Click Borrar Proceso")
	   def itemSelected = lbListaProcesoTipo.selectedItem

	   Map procesoTipo = [:]
	   procesoTipo.id = itemSelected.value
	   tipoProcesoActual.id = itemSelected.value
	   // Mostramos un cuadro de diálogo pidiendo la confimación de la operación al usuario
	   Messagebox.show("¿Confirma que desea borrar el Proceso con ID "+ itemSelected.value + "?",
		   "Confirmar Acción", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, [
		   onEvent: { e ->
			   if (e.data.intValue() == Messagebox.YES) {
				   log.debug("borrar Proceso tipo: ${procesoTipo.id}")
				   tipoProcesoEntidadService.borrar(procesoTipo)
				   this.activacion()
			   }
		   }
	   ] as org.zkoss.zk.ui.event.EventListener)
	}

	void despuesDeGuardar() {
		//Cerramos modal
		wndEdicionTipoProceso.visible = false
		wndAsociarEstudio.visible = false
		// Refrescamos la lista de tipos de procesos
		activacion()
		// Limpiamos el contenido de la ventana para su próxima utilización
		limpiarModal()
	}

	@Listen("onClick=#btnAsociarEstudio")
	void asociarTipoEstudioAProceso() {
		log.debug("Asociar nuevo Estudio a Proceso")
		def idProcesoSelected = leerValorComponente(lbListaProcesoTipo.selectedItem)
		//Recuperamos los estudios asociados al proceso enviado por parametro
		def listaEstudiosTipo = tipoEstudioEntidadService.recuperarEstudiosNoAsociadoProceso(idProcesoSelected)
		//Abrimos modal
		wndAsociarEstudio.doModal()
		wndAsociarEstudio.visible = true
		rellenarListbox(lbListaEstudios, listaEstudiosTipo, "Seleccione...")
		escribirValorComponente(id_proc_seleccionado, leerValorComponente(lbListaProcesoTipo.selectedItem))
	}

//	@Listen("onSelect=#lbListaEstudios")
//	def seleccionTpoEstudioModal() {
//		btnGuardarEstudioAsociado.class = "btn-success btn"
//	}
	
	@Listen("onClick=#btnGuardarEstudioAsociado")
	void clickGuardarEstudioAsociado() {
		Map datos = [:]
		datos.id_estudio_tipo = leerValorComponente(lbListaEstudios.selectedItem)
		datos.id_proceso_tipo = leerValorComponente(id_proc_seleccionado)
		auxiliaresService.asociarEstudioTipoProcesoTipo(datos)
	}

	@Listen("onClick=#lbListaEstudioProcTipo listitem")
	void clickTipoEstudio() {
		if (tipoProceso.puedeBorrar) {
			btnDesasociarEstudio.disabled = false
		}
	}

	@Listen("onClick=#btnDesasociarEstudio")
	void clickDesasociarEstudio() {
		if (!tipoProceso.puedeBorrar) {
			return
		}
	   def itemSelected = lbListaEstudioProcTipo.selectedItem

	   Map datos = [:]
	   datos.id_estudio_tipo = itemSelected.value
	   datos.id_proceso_tipo = leerValorComponente(lbListaProcesoTipo.selectedItem)
	   // Mostramos un cuadro de diálogo pidiendo la confimación de la operación al usuario
	   Messagebox.show("¿Confirma que desea desasociar el Estudio con ID "+ itemSelected.value + "?",
		   "Confirmar Acción", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, [
		   onEvent: { e ->
			   if (e.data.intValue() == Messagebox.YES) {
				   log.debug("Desasociar Estudio tipo: ${datos.id}")
				   auxiliaresService.eliminarAsociacionEstudioTipoProcesoTipo(datos)
				   this.activacion()
			   }
		   }
	   ] as org.zkoss.zk.ui.event.EventListener)
	}

	@Listen("onClick=#btnDescartarTipoProc, #btnDescartarEstudioAsociado")
	void clickDescartarCambios() {
		log.debug("click Descartar cambios")
		// Ocultamos la ventana modal
		wndEdicionTipoProceso.visible = false
		wndAsociarEstudio.visible = false
		descartarOperaciones()
		limpiarModal()
	}	

	void limpiarTodas() {
		// Limpiamos todos los componentes de la ventana
		if (!lbListaProcesoTipo.items.empty) {
			limpiarValorComponente(lbListaProcesoTipo)
		}
		if (!lbListaEstudioProcTipo.items.empty) {
			limpiarValorComponente(lbListaEstudioProcTipo)
		}
	}
	void limpiarAsociados() {
		// Limpiamos los componentes asociados al proceso de la ventana
		if (!lbListaEstudioProcTipo.items.empty) {
			limpiarValorComponente(lbListaEstudioProcTipo)
		}
		limpiarValorComponente(lbListaEstudioProcTipo)
	}
	void limpiarModal() {
		//Modal tipo Proceso
		limpiarValorComponente(tipoProceso_id)
		limpiarValorComponente(tipoProceso_codigo)
		limpiarValorComponente(tipoProceso_descripcion)
		//Modal asociar estudio
		if (!lbListaEstudios.items.empty) {
			limpiarValorComponente(lbListaEstudios)
		}
	}

}
