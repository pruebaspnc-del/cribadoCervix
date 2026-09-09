package  cribadocervix.configuracion

import org.zkoss.zul.Listbox
import base.BaseComposer
import  cribadocervix.AuxiliaresService
import  cribadocervix.entidad.VariablesGlobalesEntidadService
import groovy.util.logging.Slf4j

@Slf4j
class VariablesGlobalesComposer extends BaseComposer {

	// Servicios
	AuxiliaresService auxiliaresService
    VariablesGlobalesEntidadService variablesGlobalesEntidadService

	Listbox lblistaVariablesGlobales

	def activacion() {
		// Mostramos los requisitos tipo que cumplen con los filtros existentes
		buscar()
	}

	def buscar () {
		limpiar()
		lblistaVariablesGlobales.actualizar(variablesGlobalesEntidadService.recuperarTodos())
	}

	void limpiar() {
		// Limpiamos todos los componentes de la ventana
		if (!lblistaVariablesGlobales.items.empty) {
			limpiarValorComponente(lblistaVariablesGlobales)
		}
	}

}
