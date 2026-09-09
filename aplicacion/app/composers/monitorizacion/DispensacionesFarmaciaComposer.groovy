package  cribadocervix.monitorizacion

import base.FormularioComposer
import  jade.PandoraService
import  cribadocervix.DispensacionesFarmaciaService
import  cribadocervix.UtilsPcacervixService
import groovy.util.logging.Slf4j
import org.zkoss.zk.ui.select.annotation.Listen
import org.zkoss.zul.*

@Slf4j
class DispensacionesFarmaciaComposer extends FormularioComposer {

    // Servicios
    UtilsPcacervixService utilsPcacervixService
    DispensacionesFarmaciaService dispensacionesFarmaciaService
    PandoraService pandoraService

    // Parámetros de búsqueda
    Datebox dbFechaDesdeDisFar
    Datebox dbFechaHastaDisFar
    Textbox tbCodProductoDisFar
    Listbox lbZonaSaludDisFar
    Listbox lbFarmaciaDisFar

    // Pantalla detalle
    Window wndEdicionDispensacionesFarmacia
    Label tituloEdicionDispensacionesFarmacia
    Listbox lbListaDisFar
    Map parametros = [:]
    Listbox lbHistorialDisFar
    Textbox dispensacionFarmacia_cod_producto
    Button btnReenvioPetElec


    def despuesDeComponer() {

    }

    def activacion() {
        // Limpiamos
        limpiar()
        // Recuperamos y rellenamos el select de zonas de salud
        rellenarListbox(lbZonaSaludDisFar, dispensacionesFarmaciaService.recuperarListadoZonaSaludFarmaciasColaboradoras(), "Seleccione...")
        // Dejamos vacio el select de Farmacia
        def datos = [:]
        rellenarListbox(lbFarmaciaDisFar, datos, "Seleccione...")
    }

    @Listen("onSelect=#lbZonaSaludDisFar")
    def actualizarListadoFarmacias(evento) {
        def datos = [:]
        // Comprobamos de que la zona de salud seleccionada no este vacía
        if (evento.target.selectedItem.value == null) {
            // Se deja vacío al no tener zona de salud
            rellenarListbox(lbFarmaciaDisFar, datos, "Seleccione...")

        } else {
            // Recuperamos el id de la zona de salud
            int idZonaSalud = evento.target.selectedItem.value
            datos.id_zona_salud = idZonaSalud
            // Recuperamos las farmacias relacionadas con la zona de salud y las cargamos en el select
            rellenarListbox(lbFarmaciaDisFar, dispensacionesFarmaciaService.recuperarListadoFarmaciasColaboradoras(datos), "Seleccione...", "descripcion", "codigo")
        }
    }

    def transformarCodigoFarmacia(farmacia) {
        // Se limpian todas las letras del principio del String
        def limpieza = farmacia.replaceAll(/[A-Za-z-]/, "")
        // Se limpian los 0 del principio hasta llegar a un número distinto
        def resultado = limpieza.replaceFirst(/^0+/, "")
        return resultado
    }

    @Listen("onClick=#btnBuscarDisFar")
    def buscar() {
        parametros = [:]
        parametros.id_farmacia = leerValorComponente(lbFarmaciaDisFar)
        parametros.cod_producto = leerValorComponente(tbCodProductoDisFar)
        parametros.fecha_desde = leerValorComponente(dbFechaDesdeDisFar)
        parametros.fecha_hasta = leerValorComponente(dbFechaHastaDisFar)
        parametros.zona_basica = leerValorComponente(lbZonaSaludDisFar)
        if (parametros.id_farmacia) {
            // Transformamos el código de la farmacia
            parametros.cod_farmacia = transformarCodigoFarmacia(
                    parametros.id_farmacia)
        }
        // Comprobamos de que se ha introducido por lo menos un filtro de búsqueda
        def permisoBusqueda = false
        parametros.each { it ->
            if (null != it.value && it.value != "") {
                permisoBusqueda = true
            }
        }

        if (permisoBusqueda) {
            // Buscamos y recuperamos las dispensaciones
            def listado = dispensacionesFarmaciaService.buscarDispensacionesRegistradas(parametros)
            if (listado) {
                // En caso de que haya más registros de 1000, se mostrará una ventana de aviso indicándolo
                if (listado.size >= utilsPcacervixService.getMaxRegistros()) {
                    ventanaAviso("La consulta supera el volumen de registros permitido, solo se mostrarán 1000 registros.")
                }
                // Cargamos las dispensaciones al listbox
                lbListaDisFar.actualizar(listado)
            } else {
                // No hay dispensaciones, mostramos una ventana de aviso
                ventanaAviso("No hay resultados de búsqueda.")
                lbListaDisFar.actualizar(listado)
            }
        } else {
            // No ha introducido ningun filtro para la búsqueda
            ventanaAviso("Es necesario introducir algún filtro de búsqueda.")
            return
        }
    }

    @Listen("onDoubleClick=#lbListaDisFar listitem")
    def dobleCLickDispensacionesFarmacia() {
        def itemSelected = lbListaDisFar.selectedItem.value
        log.debug("RegistroSeleccionado: ${itemSelected}")
        Map dispensacionFarmacia = [:]
        dispensacionFarmacia.fec_dispensacion = itemSelected.fec_dispensacion
        dispensacionFarmacia.cod_producto = itemSelected.cod_producto
        dispensacionFarmacia.cod_farmacia = itemSelected.cod_farmacia
        dispensacionFarmacia.profesional = itemSelected.profesional
        dispensacionFarmacia.codigo = itemSelected.codigo
        dispensacionFarmacia.id_expediente = itemSelected.id_expediente

        // Recuperamos el historial de dispensaciones del producto
        def listadoHistorialDispensaciones = dispensacionesFarmaciaService.recuperarHistorialDispensacion(dispensacionFarmacia)
        lbHistorialDisFar.actualizar(listadoHistorialDispensaciones)

        // Formato a la fecha para mostrarla por pantalla
        dispensacionFarmacia.fec_dispensacion = Date.parse("dd/MM/yyyy", dispensacionFarmacia.fec_dispensacion)
        abrirModal(dispensacionFarmacia)
        desactivarComponentesVista("dispensacionFarmacia")
        // Comprobamos si se puede reenviar pet. elec.
        comprobarReenvio(listadoHistorialDispensaciones)
    }

    def abrirModal(Map dispensacionFarmacia) {
        // Hacemos visible la ventana modal
        wndEdicionDispensacionesFarmacia.visible = true
        wndEdicionDispensacionesFarmacia.doModal()
        // Cargamos los valores de la ventana modal
        escribirValoresVista("dispensacionFarmacia", dispensacionFarmacia)
    }

    def comprobarReenvio(listadoHistorialDispensaciones) {
        if (listadoHistorialDispensaciones != null) {
            // Recorremos el listado de historial de dispensaciones
            def codigoDisp = ""
            def fechaPeticion = ""
            // fecha actual menos 60 días
            def diasCaducidad = pandoraService.leerVariableGlobal("gnDiasCaducidadPetGestlab")
            def fechaLimite = new Date() - diasCaducidad.toInteger()

            listadoHistorialDispensaciones.each { it ->
                // Recuperamos el código de la dispensación
                codigoDisp = it.getAt("CODIGO")
                // Transformamos la fecha de String a Date
                //fechaDisp = Date.parse("dd/MM/yyyy", it.getAt("fec_peticion"))
				fechaPeticion = it.getAt("fec_peticion")
                // Comprobamos si el estado es "Enviado"
                if (codigoDisp == "DISPENSADA" && fechaPeticion < fechaLimite) {
                    // Si es "Enviado" se deshabilita el botón de reenvio
                    btnReenvioPetElec.disabled = false
                } else {
                    // Si no es "Enviado" se habilita el botón de reenvio
                    btnReenvioPetElec.disabled = true
                    return
                }
            }
        }
    }

    def clicReenvioPetElec() {
        def datos = [:]
        datos.cod_producto = leerValorComponente(dispensacionFarmacia_cod_producto).toString()
        datos.login = session.loginUnico

        Messagebox.show("¿Confirma que desea reenviar el producto dispensado?",
                "Confirmar Acción", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, [
                onEvent: { e ->
                    if (e.data.intValue() == Messagebox.YES) {
                        dispensacionesFarmaciaService.reenviarDispensacion(datos)
                        dispensacionesFarmaciaService.auditoriaReenviarDispensacion(datos)
                        btnReenvioPetElec.disabled = true
                    }
                }
        ] as org.zkoss.zk.ui.event.EventListener)
    }

    @Listen("onClick=#btnLimpiarFiltrosDisFar")
    def LimpiarFiltros() {
        limpiar()
        // Limpiamos los select
        rellenarListbox(lbZonaSaludDisFar, dispensacionesFarmaciaService.recuperarListadoZonaSaludFarmaciasColaboradoras(), "Seleccione...")
        def datos = [:]
        rellenarListbox(lbFarmaciaDisFar, datos, "Seleccione...")
    }

    void limpiar() {
        // Se limpian todos los componentes
        limpiarValorComponente(dbFechaDesdeDisFar)
        limpiarValorComponente(dbFechaHastaDisFar)
        limpiarValorComponente(tbCodProductoDisFar)
        if (!lbFarmaciaDisFar.getItems().isEmpty()) {
            limpiarValorComponente(lbFarmaciaDisFar)
        }
        if (!lbZonaSaludDisFar.getItems().isEmpty()) {
            limpiarValorComponente(lbZonaSaludDisFar)
        }
        if (!lbListaDisFar.getItems().isEmpty()) {
            limpiarValorComponente(lbListaDisFar)
        }
        parametros = [:]
    }
}
