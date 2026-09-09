package  cribadocervix.personas

import org.springframework.beans.factory.annotation.Autowired
import org.zkoss.zul.Button
import org.zkoss.zul.Checkbox
import org.zkoss.zul.Datebox
import org.zkoss.zul.Intbox
import org.zkoss.zul.Listbox
import org.zkoss.zul.Textbox

import base.FormularioComposer
import  jade.usuario.componentes.BuscadorPersonaComposer
import groovy.util.logging.Slf4j

@Slf4j
class BuscadorPersonasComposer extends FormularioComposer {

    @Autowired
    BuscadorPersonaComposer buscadorPersonaComposer

    Button btnInsertarPersona
    Listbox lbListaPersonas
    Textbox tbNombrePersona
    Textbox tbPrimerApellidoPersona
    Textbox tbSegundoApellidoPersona
    Intbox tbIdPersona
    Datebox dbFechaNacimientoPersona
    Datebox dbFechaNacimientoDesde
    Datebox dbFechaNacimientoHasta
    Checkbox cbBajaPersona
    Listbox lbTipoDocPersona
    Textbox tbDocPersona
    Listbox lsZonaSaludPersona

    def despuesDeComponer() {

        // Aquí inicializamos componentes y listas que se usarán en la página.
        // Se ejecuta cada vez que se carga la página


    }

    def activacion() {
        buscadorPersonaComposer = listaComposersComponentes.find { it instanceof BuscadorPersonaComposer } as BuscadorPersonaComposer
        buscadorPersonaComposer.configurar(this, "DNI")
        btnInsertarPersona.visible = false
        
        if(!vlAnterior.equals("vlPersona")) { //Comprobamos si venimos del detalle del expediente, para decidir si limpiar o no los filtros de búsqueda
            limpiar()
            buscadorPersonaComposer.configurar(this, "DNI")
        }
    }


    void verDetalle(pantalla, parametros) {
        session.id_seleccionado = parametros
        cambiarVlActivo(pantalla)
    }


    void clickBotonVer(parametros) {
        verDetalle("Persona", parametros)
    }
    
    void limpiar() {
        limpiarValorComponente(dbFechaNacimientoDesde)
        limpiarValorComponente(dbFechaNacimientoHasta)
        limpiarValorComponente(tbNombrePersona)
        limpiarValorComponente(tbPrimerApellidoPersona)
        limpiarValorComponente(tbSegundoApellidoPersona)
        limpiarValorComponente(tbIdPersona)
        limpiarValorComponente(cbBajaPersona)
        limpiarValorComponente(tbDocPersona)
        if (!lsZonaSaludPersona.items.empty) {
            lsZonaSaludPersona.setSelectedIndex(0)
        }
        if (!lbTipoDocPersona.items.empty) {
            lbTipoDocPersona.setSelectedIndex(0)
        }
        if (!lbListaPersonas.items.empty) {
            lbListaPersonas.actualizar([])
        }
    }


}
