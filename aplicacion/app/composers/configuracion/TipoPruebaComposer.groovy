package  cribadocervix.configuracion

import org.zkoss.zk.ui.event.Event
import org.zkoss.zk.ui.select.annotation.Listen
import org.zkoss.zul.Listbox
import base.BaseComposer
import  cribadocervix.entidad.TipoPruebaEntidadService
import groovy.util.logging.Slf4j

@Slf4j
class TipoPruebaComposer extends BaseComposer {

	// Servicios
	TipoPruebaEntidadService tipoPruebaEntidadService

	Listbox lbTipoPrueba

	def activacion() {
		buscar()
	}

	def buscar () {
		limpiar()
		//buscamos las Pruebas asociadas al estudio con id -> idEstudio
		// Al venir en orden ascendente, será el primer proceso en bbdd
		def listaPruebasPorEstudio = tipoPruebaEntidadService.recuperarTodos("ID ASC")
		lbTipoPrueba.actualizar(listaPruebasPorEstudio)
		/* Marcamos la primera fila como seleccionada para saber de que proceso vienen los estudios y resultados
		 que aparecen al carga la pagina. Al estar ordenado ascendentemente, siempre marcamos la primera fila. */
		lbTipoPrueba.selectedIndex = 0
	}

	@Listen("onDoubled=#lbTipoPrueba listitem")
	void clickTipoEstudio(Event evento) {
		def selected = evento.target
		def idPrueba = selected.value
		def map = [:]
		map.id_prueba_tipo = idPrueba
		def listaPruebasPorEstudio = tipoPruebaEntidadService.recuperar(map)
		lbTipoPrueba.actualizar(listaPruebasPorEstudio)
	}

	void limpiar() {
		// Limpiamos todos los componentes de la ventana
		if (!lbTipoPrueba.items.empty) {
			limpiarValorComponente(lbTipoPrueba)
		}
	}

}
