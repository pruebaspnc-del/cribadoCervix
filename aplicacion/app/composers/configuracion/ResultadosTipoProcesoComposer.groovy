package  cribadocervix.configuracion

import org.zkoss.zk.ui.event.Event
import org.zkoss.zk.ui.select.annotation.Listen
import org.zkoss.zul.Listbox
import base.BaseComposer
import  cribadocervix.AuxiliaresService
import groovy.util.logging.Slf4j

@Slf4j
class ResultadosTipoProcesoComposer extends BaseComposer {

	//Servicios
	AuxiliaresService auxiliaresService

	Listbox lbProcesoTipo
	Listbox lbListaResultadoProcTipo

	def activacion() {
		buscar()
	}

	def buscar () {
		limpiar()
		lbProcesoTipo.actualizar(auxiliaresService.buscarProcesosTipo())
		def idProceso =  lbProcesoTipo.findResult { 1 }

		//buscamos los estudios asociados al proceso
		def listaResultadosPorProceso = auxiliaresService.buscarTipoResultadosPorProceso(idProceso)
		lbListaResultadoProcTipo.actualizar(listaResultadosPorProceso)
		/* Marcamos la primera fila como seleccionada para saber de que proceso vienen los estudios que 
		aparecen al carga la pagina. Al estar ordenado ascendentemente, siempre marcamos la primera fila. */
		lbProcesoTipo.selectedIndex = 0
	}

	@Listen("onClick=#lbProcesoTipo listitem")
	void clickTipoProceso(Event evento) {
		def selected = evento.target
		def idProc = selected.value

		def listaResultadosPorProceso = auxiliaresService.buscarTipoResultadosPorProceso(idProc)
		lbListaResultadoProcTipo.actualizar(listaResultadosPorProceso)
	}

	void limpiar() {
		// Limpiamos todos los componentes de la ventana
		if (!lbProcesoTipo.getItems().isEmpty()) {
			limpiarValorComponente(lbProcesoTipo)
		}
		if (!lbListaResultadoProcTipo.getItems().isEmpty()) {
			limpiarValorComponente(lbListaResultadoProcTipo)
		}
	}

}
