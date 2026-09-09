package  cribadocervix.configuracion

import org.zkoss.zk.ui.event.Event
import org.zkoss.zk.ui.select.annotation.Listen
import org.zkoss.zul.Listbox
import base.BaseComposer
import  cribadocervix.AuxiliaresService
import groovy.util.logging.Slf4j

@Slf4j
class TipoEstudioComposer extends BaseComposer {

	// Servicios
	AuxiliaresService auxiliaresService

	Listbox lbListaEstudioTipo
	Listbox lbResultadosEstudio
	Listbox lbPruebaEstudio
	Listbox lbEstadosEstudio

	// Parámetros de búsqueda
	Map parametros = [:]

	def activacion() {
		buscar()
	}

	def buscar () {
		limpiar()
		lbListaEstudioTipo.actualizar(auxiliaresService.buscarEstudiosTipo())
		def idEstudio =  lbListaEstudioTipo.findResult { 1 }	
		buscarDatosAsociadosAEstudio(idEstudio)
		/* Marcamos la primera fila como seleccionada para saber de que proceso vienen los estudios
		y resultados que aparecen al carga la pagina. Al estar ordenado ascendentemente, siempre marcamos la primera fila. */
		lbListaEstudioTipo.selectedIndex = 0
	}

	@Listen("onClick=#lbListaEstudioTipo listitem")
	void clickTipoEstudio(Event evento) {
		def selected = evento.target
		def idEstudio = selected.value
		buscarDatosAsociadosAEstudio(idEstudio)
	}

	def buscarDatosAsociadosAEstudio(idEstudio) {
		// Buscamos los Resultados asociados al estudio
		// Al venir en orden ascendente, será el primer proceso en bbdd
		def listaResultadoPorEstudio = auxiliaresService.buscarTipoResultadosPorEstudio(idEstudio)
		lbResultadosEstudio.actualizar(listaResultadoPorEstudio)
		// Buscamos las Pruebas asociadas al estudio
		// Al venir en orden ascendente, será el primer proceso en bbdd
		def listaPruebasPorEstudio = auxiliaresService.buscarTipoPruebaPorEstudio(idEstudio)
		lbPruebaEstudio.actualizar(listaPruebasPorEstudio)
		// Buscamos los Estados asociados al estudio
		def listaEstadoPorEstudio = auxiliaresService.buscarTipoEstadoPorEstudio(idEstudio)
		lbEstadosEstudio.actualizar(listaEstadoPorEstudio)
	}

	void limpiar() {
		// Limpiamos todos los componentes de la ventana
		if (!lbListaEstudioTipo.items.empty) {
			limpiarValorComponente(lbListaEstudioTipo)
		}
		if (!lbResultadosEstudio.items.empty) {
			limpiarValorComponente(lbResultadosEstudio)
		}
		if (!lbPruebaEstudio.items.empty) {
			limpiarValorComponente(lbPruebaEstudio)
		}
		if (!lbEstadosEstudio.items.empty) {
			limpiarValorComponente(lbEstadosEstudio)
		}
	}

}
