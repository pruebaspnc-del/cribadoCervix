package  CRIBADOCERVIX.componentes

import  CRIBADOCERVIX.Constantes
import org.apache.tomcat.util.bcel.Const
import org.springframework.stereotype.Component
import org.zkoss.zk.ui.event.Event
import org.zkoss.zk.ui.select.annotation.Listen
import org.zkoss.zk.ui.util.Composer
import org.zkoss.zul.Listbox
import org.zkoss.zul.Textbox
import org.zkoss.zul.Window
import org.zkoss.zul.Button
import org.zkoss.zul.Datebox
import org.zkoss.zul.Div
import org.zkoss.zul.Label
import base.EntidadFormulario
import base.FormularioComposer
import  jade.PandoraService
import  jade.PersanService
import  CRIBADOCERVIX.UtilsPcacervixService
import  CRIBADOCERVIX.entidad.ContactoEntidadService
import groovy.util.logging.Slf4j
import org.zkoss.zul.Messagebox

@Slf4j
@Component
class ContactoComposer extends FormularioComposer {

    // Entidad
    EntidadFormulario contacto

    // Servicios
    PandoraService pandoraService
    UtilsPcacervixService utilsPcacervixService
    ContactoEntidadService contactoEntidadService

    //Parametros del fltro en pantalla
    Listbox lbRegistroContacto
    Listbox contacto_id_motivo_contacto
    Listbox contacto_contactado
    Textbox contacto_valor_medio_comunicacion
    Listbox contacto_medio_comunicacion
    Textbox contacto_nombre_profesional
    Datebox contacto_fecha
    Textbox contacto_id_estudio
    Textbox contacto_id_profesional
    Div div_textbox_MedioComun
    Div div_lb_medios_comun
    Label tituloContacto

    //Componentes de pantalla Expediente

    Button borrarContacto
    Button btnDescartarContacto
    Button btnEditarContacto
    Button btnGuardarContacto
    Button btnCerrarVentanaContacto

    Composer invocador
    def idEstudio
    def idPersona
    def motivosContacto
    def mediosComunicacion
    Window wndEdicionContacto


    def despuesDeComponer() {
        contacto = new EntidadFormulario("contacto","contacto_id", this)
        // Configuramos la entidad
        contacto.configurar("pcacervix", "registro_contacto")
        // Recuperamos el id de profesional y su unidad funcional seleccionada del usuario actual
        def idProfesional = session.idProfesional
        def idUnidadFuncional = session.unidadesFuncionales[0].id
        def login = session.loginUnico
        def nombreProfesional = pandoraService.recuperaNombreProfesional(idProfesional)
        // Establecemos los parámetros por defecto para todas las operaciones de entidades
        def parametros = [id_profesional: idProfesional,nombre_profesional: nombreProfesional, id_unidad_funcional: idUnidadFuncional, login: login]
        contacto.parametros = parametros
        // Establecer el servicio que va a gestionar la lista de operaciones
        servicioOperaciones = contactoEntidadService
        establecerBotonGuardar(btnGuardarContacto)
        establecerBotonDescartar(btnDescartarContacto)

    }

    def activacion() {
        //Recuperamos los motivos de contacto
        Map datosContacto = [:]
        datosContacto.id_proyecto = Constantes.ID_PROYECTO
        motivosContacto = utilsPcacervixService.recuperarMotivosContactoPorProyecto(datosContacto)
        rellenarListbox(contacto_id_motivo_contacto, motivosContacto, "Seleccione...")

        if (invocador) {
            idEstudio = invocador.contactosActual.id_estudio
            idPersona = invocador.contactosActual.id_persona
        }
    }

    @Listen("onDoubleClick=#lbRegistroContacto listitem")
    def dobleClickResultado() {
        def registroSeleccionado = lbRegistroContacto.selectedItem.value
        log.debug("Registro Seleccionado: ${registroSeleccionado}")
        Map datos = [:]
        datos.id_estudio = registroSeleccionado.ID_ESTUDIO
        datos.contador = registroSeleccionado.CONTADOR
        def detallesContacto = contacto.recuperar(datos)

        //Recuperamos los Medios Comunicación
        mediosComunicacion = utilsPcacervixService.recuperarMediosComunicacionPersonaConFecActualizada(idPersona)
        //Rellenamos valores de componentes
        escribirValorComponente(tituloContacto, "Detalle del Contacto")
        escribirValoresVista("contacto", detallesContacto)
        if (contacto_id_profesional.value != null && contacto_id_profesional.value != "") {
            escribirValorComponente(contacto_nombre_profesional, pandoraService.recuperaNombreProfesional(contacto_id_profesional.value))
        }

        div_lb_medios_comun.visible = false
        div_textbox_MedioComun.visible = true
        desactivarComponentesVista("contacto")
        //Desactivamos los botones Guardar y Descartar
        btnGuardarContacto.visible = false
        btnDescartarContacto.visible = false
        //Mostramos el boton Editar y Cerrar ventana
        btnEditarContacto.visible = true
        btnCerrarVentanaContacto.visible = true
        // Hacemos visible la ventana modal
        wndEdicionContacto.visible = true
        wndEdicionContacto.doModal()
    }

    @Listen("onClick=#lbRegistroContacto listitem")
    def clickResultado() {
        borrarContacto.disabled = false
    }

    @Listen("onClick=#insertarContacto")
    def clickInsertarContacto() {
        log.debug("Click Nuevo Contacto. Abrimos modal")

        rellenarListbox(contacto_id_motivo_contacto, motivosContacto, "Seleccione...")

        Map datos = [:]
        datos.id = NUEVO_REGISTRO
        datos.id_estudio = idEstudio
        datos.fecha = new Date()
        datos.id_profesional =  contacto.parametros.id_profesional
        datos.nombre_profesional = contacto.parametros.nombre_profesional
        contacto.insertar(datos)

        //Por defecto, no marcamos nada en el listbox 'Contactado', les obligamos a marcar una opción siempre
        escribirValorComponente(contacto_contactado, "Seleccionar..")
        //Recuperamos los Medios Comunicación
        mediosComunicacion = utilsPcacervixService.recuperarMediosComunicacionPersonaConFecActualizada(idPersona)
        contacto_medio_comunicacion.actualizar(mediosComunicacion)

        // Si se llama desde la pantalla de ContactoGestionDocumentos, el motivo de contacto se rellena con el codigo CARTADEV
        if (invocador.contactosActual.pantalla == "ContactoGestionDocumentos") {
            def indexMotivoContacto = motivosContacto.findIndexOf { it.codigo == "CARTADEV" }
            String idMotivoSeleccionado = motivosContacto[indexMotivoContacto].getAt("ID")
            escribirValorComponente(contacto_id_motivo_contacto, idMotivoSeleccionado)
            contacto_id_motivo_contacto.disabled = true
        } else {
            contacto_id_motivo_contacto.disabled = false
        }


        escribirValorComponente(tituloContacto, "Insertar Nuevo Contacto")
        div_textbox_MedioComun.visible = false
        //Mostramos la lista de medios de comunicación
        div_lb_medios_comun.visible= true
        // Ocultamos el boton Editar
        btnEditarContacto.visible = false
        btnCerrarVentanaContacto.visible= false
        //Motramos botonera
        btnGuardarContacto.visible = true
        btnDescartarContacto.visible = true
        btnDescartarContacto.disabled = false
        // Hacemos visible la ventana modal
        wndEdicionContacto.visible = true
        wndEdicionContacto.doModal()

    }

    @Listen("onClick=#borrarContacto")
    def clickBorrarContacto() {
        def registroSeleccionado = lbRegistroContacto.selectedItem.value
        Map datos = [:]
        datos.id_estudio = registroSeleccionado.id_estudio
        datos.contador = registroSeleccionado.contador
        // Mostramos un cuadro de diálogo pidiendo la confimación de la operación al usuario
        Messagebox.show("¿Confirma que desea borrar el contacto?",
                "Confirmar Acción", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, [
                onEvent: { e ->
                    if(e.data.intValue() == Messagebox.YES) {
                        wndEdicionContacto.visible = false
                        contactoEntidadService.borrar(datos)
                        invocador.seleccionTabContactos()
                    }
                }
        ] as org.zkoss.zk.ui.event.EventListener)
    }

    public boolean antesDeGuardar() {
        if (contacto_medio_comunicacion.selectedItem) {
            escribirValorComponente(contacto_valor_medio_comunicacion, contacto_medio_comunicacion.selectedItem.value)
        }
        return true

    }

    void despuesDeGuardar() {
        wndEdicionContacto.visible = false
        invocador.seleccionTabContactos()
    }

    def clickEditarContacto() {
        activarComponentesVista("contacto")
        //Ocultamos el textbox con el numero anterior
        div_textbox_MedioComun.visible = false
        //Mostramos la lista de medios de comunicación
        div_lb_medios_comun.visible= true
        contacto_medio_comunicacion.actualizar(mediosComunicacion)
        //Preseleccionamos el medio de comunicación previo
        def indexMediosCom
        indexMediosCom = leerValorComponente(contacto_medio_comunicacion)?.findIndexOf {
            it.valor == contacto_valor_medio_comunicacion.value
        }
        contacto_medio_comunicacion.selectedIndex = indexMediosCom
        escribirValorComponente(tituloContacto, "Editar Contacto")

        btnEditarContacto.visible = false
        btnGuardarContacto.visible = true
        btnDescartarContacto.visible = true
        btnDescartarContacto.disabled = false
        btnCerrarVentanaContacto.visible = false
        //Desactivamos componenetes fijos
        contacto_id_estudio.disabled = true
        contacto_nombre_profesional.disabled = true
        contacto_fecha.disabled = true
    }

    def descartarCambios() {
        wndEdicionContacto.visible = false
    }

    void limpiar() {
        // Limpiamos todos los componentes de la ventana
        lbRegistroContacto.actualizar([])
        activacion()
    }
}
