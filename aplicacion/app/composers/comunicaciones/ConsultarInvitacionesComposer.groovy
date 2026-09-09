package  CRIBADOCERVIX.comunicaciones

import org.springframework.beans.factory.annotation.Autowired
import org.zkoss.zk.ui.event.Event
import org.zkoss.zk.ui.select.annotation.Listen
import org.zkoss.zul.Button
import org.zkoss.zul.Checkbox
import org.zkoss.zul.Datebox
import org.zkoss.zul.Intbox
import org.zkoss.zul.Listbox
import org.zkoss.zul.Listitem
import org.zkoss.zul.Window
import org.zkoss.zul.Filedownload

import base.FormularioComposer

import  jade.PandoraService
import  CRIBADOCERVIX.Constantes
import  CRIBADOCERVIX.ExpedienteService
import  CRIBADOCERVIX.PcaCervixService
import  CRIBADOCERVIX.UtilsPcacervixService
import  CRIBADOCERVIX.entidad.DocumentoEntidadService
import  CRIBADOCERVIX.entidad.EmisionInvitacionEntidadService
import groovy.util.logging.Slf4j


@Slf4j
class ConsultarInvitacionesComposer extends FormularioComposer{

	//Servicios
	UtilsPcacervixService utilsPcacervixService
	PcaCervixService pcaCervixService
	EmisionInvitacionEntidadService emisionInvitacionEntidadService
    @Autowired
    DocumentoEntidadService documentoEntidadService
    @Autowired
    ExpedienteService expedienteService
    @Autowired
    PandoraService pandoraService
	//Lista
	Listbox lbListaInvitaciones
	
	//Parametros del filtro de busqueda
	Datebox dbFecDesdeInv
	Datebox dbFecHastaInv
	Listbox lbAreaSaludInv
	Listbox lbZonaSaludInv
	Checkbox cbFechaLimite
    Checkbox cbBuscarReinvitaciones
    
	Intbox inv_num_doc
	
	Window wndCrearEmision
	Window wndGenDocs
		
	Map invitacionActual
	
	Button btnEstimacionInv
	Button btnGenerarInv
	Button btnDescargarInv
	Button btnGenerarDocs
	
	Map parametros = [:]
	
	def lista
	def total
	def idEmision
	Map seleccionado

	def despuesDeComponer() {	
	}
	
	def activacion() {		
		parametros = [:]
		total = 0
		//limpiarFiltros()
		rellenarListbox(lbAreaSaludInv, utilsPcacervixService.recuperarAreasSalud(), "Seleccione..." )
		lbZonaSaludInv.disabled = true
	}

	@Listen("onClick=#btnBorrarFiltrosInv")
	def clickBorrarFiltros() {
		limpiarFiltros()
	}

	@Listen("onClick=#btnBuscarEmisiones")
	def clickBuscarEmisiones() {
		limpiarValorComponente(lbListaInvitaciones)
		parametros.fecha_desde = leerValorComponente(dbFecDesdeInv)
		parametros.fecha_hasta = leerValorComponente(dbFecHastaInv)
		parametros.area_salud = leerValorComponente(lbAreaSaludInv)
        parametros.reinvitaciones = leerValorComponente(cbBuscarReinvitaciones)
		if (lbZonaSaludInv.getItems().isEmpty()) {
			parametros.zona_salud = null
		} else {
			parametros.zona_salud = leerValorComponente(lbZonaSaludInv)
		}		
		//Obligamos a poner filtros de busqueda
		def permisoBusqueda = false
		parametros.each { it->
            if(null != it.value && it.value != "" && it.key != 'reinvitaciones') {
                permisoBusqueda = true
            }
		}
        if(cbBuscarReinvitaciones.isChecked()){
            permisoBusqueda = true
        }
		if(permisoBusqueda) {
            lista = emisionInvitacionEntidadService.buscar(parametros)
			lbListaInvitaciones.actualizar(lista)
            if(lista.size() == 0){
                ventanaAviso("No hay resultados de búsqueda")
            }
		}else {
			ventanaAviso("Introduzca algún filtro de búsqueda.")
		}
	}
	
	@Listen("onSelect=#lbAreaSaludInv")
	void rellenarZonasSalud() {
		def idArea = leerValorComponente(lbAreaSaludInv.getSelectedItem())
		if (idArea != null) {
			def zonasSalud = utilsPcacervixService.recuperarZonasIdArea(idArea)
			rellenarListbox(lbZonaSaludInv, zonasSalud, "Seleccione..." )
			lbZonaSaludInv.disabled = false
		}else {
			rellenarListbox(lbAreaSaludInv, utilsPcacervixService.recuperarAreasSalud(), "Seleccione..." )
			if(!lbZonaSaludInv.getItems().isEmpty()) {
				lbZonaSaludInv.getItems().removeAll(lbZonaSaludInv.getItems())
				lbZonaSaludInv.disabled = true
			}
		}	
	}	

	@Listen("onClick=#btnCrearEmision")
	def crearEmision() {
		log.debug("Nueva Emisión")
		Map id = emisionInvitacionEntidadService.generarIdInvitaciones()
		Map invitacion = [:]
		invitacion.nuevo = true
		invitacion.id = id.MAX_ID + 1
		invitacion.titulo = "Nueva Emisión"
		modalCrearEmision(invitacion)
	}

	@Listen("onDoubleClick=#lbListaInvitaciones listitem")
	def dobleClickInv (Event evento) {
		log.debug("Doble Click. Ventana emergente detalle")
		def itemSelected = evento.target
	    def idInvSeleccionada = itemSelected.value
		Map invitacion = [:]
		invitacion.nuevo = false
		invitacion.id = idInvSeleccionada
		invitacion.titulo = "Editar Emisión"
		modalCrearEmision(invitacion)
	}

	void limpiarFiltros() {
		limpiarValorComponente(dbFecDesdeInv)
		limpiarValorComponente(dbFecHastaInv)
		if (!lbAreaSaludInv.getItems().isEmpty())
			limpiarValorComponente(lbAreaSaludInv)
		if (!lbZonaSaludInv.getItems().isEmpty()) {
			limpiarValorComponente(lbZonaSaludInv);
			lbZonaSaludInv.disabled = true
		}
		parametros = [:]
		lbListaInvitaciones.actualizar([])
        cbBuscarReinvitaciones.checked = false
		btnEstimacionInv.disabled = true
		btnGenerarInv.disabled = true
		btnDescargarInv.disabled= true
	}

	@Listen("onSelect=#lbListaInvitaciones")
	void clickInvitacion(Event evento) {
		// Recuperamos la fila del listbox (listitem) sobre la que hemos hecho click
		Listitem fila = evento.target.getSelectedItem()
		Map datos = [:]
		datos.id = fila.value
		seleccionado = emisionInvitacionEntidadService.recuperar(datos)
		idEmision = seleccionado.id		
		
		if (seleccionado.num_doc_generados == null) {
			btnEstimacionInv.disabled = false
			btnGenerarInv.disabled = false
			btnDescargarInv.disabled= true
		} else {
			btnEstimacionInv.disabled = true
			btnGenerarInv.disabled = true
			btnDescargarInv.disabled= false
		}
	}

	void modalCrearEmision(inv) {
		invitacionActual = inv
		// Establecemos el composer llamante
		wndCrearEmision.getAttribute("\$composer").invocador = this
		wndCrearEmision.getAttribute("\$composer").activacion()
		// Hacemos visible la ventana modal
		wndCrearEmision.visible = true
		wndCrearEmision.doModal()
		//wndCrearEmision.setTitle(inv.titulo)		
	}

	@Listen("onClick=#btnEstimacionInv")
	def clickEstimacion() {
		total = emisionInvitacionEntidadService.recuperarEstimacion(idEmision)
		ventanaInformacion("Estimación de personas a invitar: " + total.estimacion)	
        log.info("Estimacion. Total estimado:" +total.estimacion)
	}

	def clickGenerarDocs() {
		def num_doc = leerValorComponente(inv_num_doc)
		if (total.estimacion >= num_doc) {
			Map generaCartas = [:]
			generaCartas.profesional = session.idProfesional
			generaCartas.id = idEmision
			generaCartas.numDoc = num_doc
			
			def cartas = emisionInvitacionEntidadService.generarCartasInvitacion(generaCartas)
            
			if (cartas.errores) {
				log.info("ERROR en Cartas Generadas:" + cartas.errores)
				ventanaError(cartas.errores)
			}else {
				log.info("CARTAS GENERADAS")
				ventanaInformacion("Cartas generadas. Disponibles para descargar.")
				btnEstimacionInv.disabled = true
				btnGenerarInv.disabled = true
				btnDescargarInv.disabled= false
				wndGenDocs.visible = false
				//Recargamos los datos actualizados
				this.activacion()
			}
		} else {
			ventanaAviso("No puede superar el número total de documentos: " + total.estimacion)
		}
	}

	@Listen("onClick=#btnDescargarInv")
	def clickDescargar() {
        Map datos = [:]
		datos.id_emision = idEmision
		datos.login = session.loginUnico

		def parametrosReport = utilsPcacervixService.parametrosPlantillaJasper(session.ruta_jasper)
		//Comprobamos datos opcionales a enviar a la plantilla, segun filtros seleccionados por pantalla
		// Check fechas limite
		boolean incluirFechaLimite = leerValorComponente(cbFechaLimite)
		if(incluirFechaLimite){
			parametrosReport.gnFechaLimite = pandoraService.leerVariableGlobal(Constantes.FECHA_LIMITE_CART_INV)
		}else{
			parametrosReport.gnFechaLimite = null
		}

        ArrayList<ByteArrayOutputStream> documentosPDF = new  ArrayList<ByteArrayOutputStream>()
        //Recuperamos todos los documentos de la emision recibida por parametro
        def documentosEmision = documentoEntidadService.recuperarDocumentosPorEmision(datos.id_emision)

        if(documentosEmision) {
            documentosEmision.each { documento ->
                def parametrosSQL
                parametrosSQL = ['dfIdDocumento' : + documento.ID]
                datos.id_documento = documento.ID

                //Generamos PDFs y los unimos en uno solo.
                def docPDF = pandoraService.imprimirReport(Constantes.CARTA_INVITACION, parametrosSQL, parametrosReport, Constantes.ID_PROYECTO)
                if (docPDF) {
                    documentosPDF = documentosPDF.plus(docPDF)
                    //Actualizamos en DOCUMENTO_INDIVIDUAL quien se descarga el documento
                    documentoEntidadService.actualizarDatosUltimaDescargaDoc(datos)
                }
            }
            log.info("Combinamos PDFs")
            byte[] out = utilsPcacervixService.combinePDFs(documentosPDF)
            String nombrePDF = Constantes.CARTA_INVITACION +".pdf"
            Filedownload.save(out, "application/pdf", nombrePDF)
            //Guardamos registro de descargas en auditoria
            documentoEntidadService.auditoriaDocumentos(datos)
        }

	}

	@Listen("onClick=#btnGenerarInv")
	def clickGenerar() {
		total = emisionInvitacionEntidadService.recuperarEstimacion(idEmision)
		inv_num_doc.value = total.estimacion
        log.info("Estimacion antes de Generación. Total estimado:" +total.estimacion)
		// Hacemos visible la ventana modal
		wndGenDocs.visible = true
		wndGenDocs.doModal()

		//Si el numero de documentos a generar es 0, deshabilitamos el boton Generar.
		if(total.estimacion == 0) {
			btnGenerarDocs.disabled = true
		}else {
			btnGenerarDocs.disabled=false
		}
	}

}
