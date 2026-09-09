package  CRIBADOCERVIX.componentes

import org.springframework.stereotype.Component
import org.zkoss.zk.ui.select.annotation.Listen
import org.zkoss.zk.ui.util.Composer
import org.zkoss.zul.Listbox
import org.zkoss.zul.Textbox
import org.zkoss.zul.Window
import org.zkoss.zul.Button
import org.zkoss.zul.Datebox
import org.zkoss.zul.Label
import base.EntidadFormulario
import base.FormularioComposer
import  jade.PandoraService
import  CRIBADOCERVIX.entidad.NotasEntidadService
import groovy.util.logging.Slf4j
import org.zkoss.zul.Messagebox

@Slf4j
@Component
class NotasComposer extends FormularioComposer {

    // Entidad
    EntidadFormulario notas

    // Servicios
    PandoraService pandoraService
    NotasEntidadService notasEntidadService

    //Parametros
    Listbox lbNotas
    Textbox notas_nombre_profesional
    Textbox notas_id_expediente
    Datebox notas_fecha
    Label tituloNota

    // Componentes pantalla
    Button btnGuardarNotas
    Button btnDescartarNotas
    Button btnEditarNotas
    Button btnCerrarVentanaNotas
    Button borrarNotas

    Composer invocador

    def idExpediente
    Map parametros = [:]
    Map notasActual = [:]
    Window wndEdicionNotas

    def despuesDeComponer() {
        notas = new EntidadFormulario("notas", "nota_id", this)
        // Configuramos la entidad
        notas.configurar("pcacervix", "expediente_notas")
        // Recupoeramos el id de profeional y su unidad funcional seleccionada del usuario actual
        def idProfesional = session.idProfesional
        def idUnidadFuncional = session.unidadesFuncionales[0].id
        def login = session.loginUnico
        def nombreProfesional = pandoraService.recuperaNombreProfesional(idProfesional)
        // Establecemos los parámetros por defecto par atodas las operacines de entidades
        def parametros = [id_profesional: idProfesional, nombre_profesional: nombreProfesional, id_unidad_funcional: idUnidadFuncional, login: login]
        notas.parametros = parametros
        // Establecer el servicio que gestiona la lista de operaciones
        servicioOperaciones = notasEntidadService
        establecerBotonGuardar(btnGuardarNotas)
        establecerBotonDescartar(btnDescartarNotas)

    }

    def activacion() {
        if (invocador) {
            idExpediente = invocador.notasActual.id_expediente

        }
    }

    @Listen("onDoubleClick=#lbNotas listitem")
    def dobleClickResultado() {
        def registroSeleccionado = lbNotas.selectedItem.value
        log.debug("Registro Seleccionado: ${registroSeleccionado}")
        idExpediente = registroSeleccionado.ID_EXPEDIENTE
        Map datos = [:]
        datos.id_expediente = registroSeleccionado.ID_EXPEDIENTE
        datos.contador = registroSeleccionado.CONTADOR
        def detallesNota = notas.recuperar(datos)
        detallesNota.id_profesional_actual = session.idProfesional

        // Rellenamos valores de componentes
        escribirValorComponente(tituloNota, "Detalle de la Nota")
        escribirValoresVista("notas", detallesNota)
        escribirValorComponente(notas_nombre_profesional, detallesNota.PROFESIONAL)
        desactivarComponentesVista("notas")
        // Desactivamos los botones Guardar y Descartar
        btnGuardarNotas.visible = false
        btnDescartarNotas.visible = false
        // Mostramos el boton Editar y Cerrar Ventana
        btnEditarNotas.visible = true
        btnCerrarVentanaNotas.visible = true
        // Hacemos visible la ventana modal
        wndEdicionNotas.visible = true
        wndEdicionNotas.doModal()
    }

    @Listen("onClick=#lbNotas listitem")
    def clickResultado() {
        borrarNotas.disabled = false
    }

    @Listen("onClick=#insertarNotas")
    def clickInsertarNotas() {
        log.debug("Click Nueva Nota. Abrimos modal")
        idExpediente = invocador.notasActual.id_expediente

        Map datos = [:]
        datos.id = NUEVO_REGISTRO
        datos.id_expediente = idExpediente
        datos.fecha = new Date()
        datos.id_profesional = notas.parametros.id_profesional
        datos.nombre_profesional = notas.parametros.nombre_profesional


        notas.insertar(datos)

        escribirValorComponente(tituloNota, "Insertar nueva Nota")
        // Ocultamos el boton Editar
        btnEditarNotas.visible = false
        btnCerrarVentanaNotas.visible = false
        // Mostramos botonera
        btnGuardarNotas.visible = true
        btnDescartarNotas.visible = true
        btnDescartarNotas.disabled = false
        // Desactivamos fecha
        notas_fecha.disabled = true
        // Hacemos visible la ventana modal
        wndEdicionNotas.visible = true
        wndEdicionNotas.doModal()
    }

    @Listen("onClick=#borrarNotas")
    def clickBorrarNotas() {
        def registroSeleccionado = lbNotas.selectedItem.value
        Map datos = [:]
        datos.id_expediente = registroSeleccionado.id_expediente
        datos.contador = registroSeleccionado.contador
        // Mostramos un cuadro de diálogo pidiendo la confirmación de la operación al usuario
        Messagebox.show("¿Confirma que desea borrar la nota?",
                "Confirmar Acción", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, [
                onEvent: { e ->
                    if (e.data.intValue() == Messagebox.YES) {
                        wndEdicionNotas.visible = false
                        notasEntidadService.borrar(datos)
                        actualizarResultadosRegistrosNota(datos)
                    }
                }
        ] as org.zkoss.zk.ui.event.EventListener)
    }

    def actualizarResultadosRegistrosNota(datos) {
        def nota = notasEntidadService.recuperarNotasPorIdExpediente(datos)
        nota?.each { not ->
            not.fecha = not.fecha.format("dd/MM/yyyy HH:mm")
        }
        lbNotas.actualizar(nota)
    }

    def clickEditarNotas() {
        activarComponentesVista("notas")
        escribirValorComponente(tituloNota, "Editar Nota")
        escribirValorComponente(notas_nombre_profesional, notas.parametros.nombre_profesional)
        escribirValorComponente(notas_fecha, new Date())

        btnEditarNotas.visible = false
        btnGuardarNotas.visible = true
        btnDescartarNotas.visible = true
        btnDescartarNotas.disabled = false
        btnCerrarVentanaNotas.visible = false
        //Desactivamos componenetes fijos
        notas_nombre_profesional.disabled = true
        notas_fecha.disabled = true
    }

    void despuesDeGuardar() {
        wndEdicionNotas.visible = false
        Map datos = [:]
        datos.id_expediente = idExpediente
        limpiar()
        actualizarResultadosRegistrosNota(datos)
    }

    def descartarCambios() {
        wndEdicionNotas.visible = false
        limpiar()
    }

    void limpiar() {
        limpiarValoresVista("notas")
        activacion()
    }

}



