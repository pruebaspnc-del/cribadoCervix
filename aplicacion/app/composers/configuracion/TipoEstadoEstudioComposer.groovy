package  cribadocervix.configuracion

import org.zkoss.zk.ui.event.Event
import org.zkoss.zk.ui.select.annotation.Listen
import org.zkoss.zul.Listbox
import base.BaseComposer
import  cribadocervix.entidad.TipoEstadoEstudioEntidadService
import groovy.util.logging.Slf4j

@Slf4j
class TipoEstadoEstudioComposer extends BaseComposer {

	// Servicios
	TipoEstadoEstudioEntidadService tipoEstadoEstudioEntidadService

	Listbox lbTiposEstado

	def activacion() {
		// Mostramos los requisitos tipo que cumplen con los filtros existentes
		buscar()
	}

	def buscar () {
		limpiar()
		def listaEstadoPorEstudio= tipoEstadoEstudioEntidadService.recuperarTodos()
		lbTiposEstado.actualizar(listaEstadoPorEstudio)
		// Al estar ordenado ascendentemente, siempre marcamos la primera fila.
		lbTiposEstado.selectedIndex = 0
	}

	/* @Listen("onDoubleClick=#lbTiposEstado listitem")
	void clickTipoEstado(Event evento) {
		def selected = evento.target
		def idEstado = selected.value
		def map = [:]
		map.id_tipo_estado = idEstado
		def listaEstadoPorEstudio= tipoEstadoEstudioEntidadService.recuperar(map)
		lbTiposEstado.actualizar(listaEstadoPorEstudio)
	} */

	void limpiar() {
		// Limpiamos todos los componentes de la ventana
		if (!lbTiposEstado.items.empty) {
			limpiarValorComponente(lbTiposEstado)
		}
	}

}
