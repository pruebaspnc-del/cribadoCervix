package  CRIBADOCERVIX.componentes

import base.EntidadFormulario
import base.FormularioComposer
import  jade.PandoraService
import  CRIBADOCERVIX.Constantes
import  CRIBADOCERVIX.UtilsPcacervixService
import  CRIBADOCERVIX.entidad.DocumentoEntidadService
import  CRIBADOCERVIX.entidad.EstudioEntidadService
import groovy.util.logging.Slf4j
import org.zkoss.zk.ui.select.annotation.Listen
import org.zkoss.zk.ui.select.annotation.Wire
import org.zkoss.zk.ui.util.Composer
import org.zkoss.zul.*

@Slf4j
class DocumentosComposer extends FormularioComposer {

    // Entidad
    EntidadFormulario documento

    // Servicios
    PandoraService pandoraService
    EstudioEntidadService estudioEntidadService
    DocumentoEntidadService documentoEntidadService
    UtilsPcacervixService utilsPcacervixService

    // Parametros pantalla
    Listbox lbDocumentos

    Listbox documento_id_documento_def
    Label tituloDocumento
    Datebox documento_fecha
    Textbox documento_id_emision
    Textbox documento_nombre_profesional
    Textbox documento_id_estudio
    Textbox documento_id
    Textbox documento_id_expediente
    Textbox documento_id_persona
    Listbox documento_id_motivo
    Datebox documento_fecha_registro
    Datebox documento_fecha_devolucion
    Textbox documento_nombre_profesional_registro
    Textbox documento_id_profesional_registro
    Textbox documento_id_profesional
    Checkbox documento_resuelto
    Checkbox documento_dom_correcto
    Datebox documento_fec_resolucion
    Textbox documento_nombre_profesional_resolucion
    Textbox documento_prof_resolucion

    @Wire(".nuevoDocumento")
    List nuevoDocumento

    @Wire(".detalleDocumento")
    List detalleDocumento

    // Variables
    def tiposDocumento
    def idDocumento

    // Botones
    Button btnGuardarDocumento
    Button btnDescartarDocumento
    Button btnEditarDocumento
    Button btnCerrarVentanaDocumento
    Button btnBorrarDocu
    Button btnDescargarDocumento

    Window wndEdicionDocumento
    Composer invocador
    def idExpediente
    def idEstudio
    def idPersona
    def fechaBajaExp
    def idDocumentoGestionar

    def despuesDeComponer() {
        documento = new EntidadFormulario("documento", "documento_id", this)

        // Configuramos la entidad
        documento.configurar("pcacervix", "documento")
        // Recuperamos el id de profesional y su unidad funcional seleccionada del usuario actual
        def idProfesional = session.idProfesional
        def idUnidadFuncional = session.unidadesFuncionales[0].id
        def login = session.loginUnico
        def nombreProfesional = pandoraService.recuperaNombreProfesional(idProfesional)
        // Establecemos los parámetros por defecto para todas las operaciones de entidades
        def parametros = [id_profesional: idProfesional, nombre_profesional: nombreProfesional, id_unidad_funcional: idUnidadFuncional, login: login]
        documento.parametros = parametros
        // Establecer el servicio que va a gestionar la lista de operaciones
        servicioOperaciones = documentoEntidadService
        establecerBotonGuardar(btnGuardarDocumento)
        establecerBotonDescartar(btnDescartarDocumento)
    }

    def activacion() {
        // Recuperamos los documentos de pandora
        tiposDocumento = utilsPcacervixService.recuperarTiposDocumentoXML()
        rellenarListbox(documento_id_documento_def, tiposDocumento, "Seleccione...")
        def listaMotivosDevolucion = utilsPcacervixService.recuperarDevolucionesTipo()
        rellenarListbox(documento_id_motivo, listaMotivosDevolucion, "Seleccione...", "descripcion", "codigo")

        if (invocador) {
            idExpediente = invocador.documentosActual.id_expediente
            idEstudio = invocador.documentosActual.id_estudio
            idPersona = invocador.documentosActual.id_persona
            fechaBajaExp = invocador.documentosActual.fecha_baja_exp
            idDocumentoGestionar = invocador.documentosActual.id_documento_gestionar
        }
    }

    @Listen("onClick=#lbDocumentos listitem")
    def clickResultado() {
        def registroSeleccionado = lbDocumentos.selectedItem.value
        // Comprobamos que pueda borrar
        if (registroSeleccionado == idDocumentoGestionar) {
            btnBorrarDocu.disabled = false
        } else {
            btnBorrarDocu.disabled = true
        }

        // Habilitamos Descargar Documento
        btnDescargarDocumento.disabled = false

    }

    @Listen("onClick=#btnDescargarDocumento")
    def clickDescargar() {
        def registroSeleccionado = lbDocumentos.selectedItem.value
        Map datos = [:]
        datos.id = registroSeleccionado


        imprimirDocumentoJasper(datos)


    }


    def imprimirDocumentoJasper(datos) {
        def parametrosReport = utilsPcacervixService.parametrosPlantillaJasper(session.ruta_jasper)
        //Recuperamos eldocumento por parametro
        def documentoRecuperado = documentoEntidadService.recuperar(datos)

        if (documentoRecuperado) {
            def parametrosSQL
            parametrosSQL = ['dfIdDocumento': +documentoRecuperado.getAt("ID")]
            datos.id_documento = documentoRecuperado.getAt("ID")

            // Generamos PDF
            def docPDF = pandoraService.imprimirReport(documentoRecuperado.getAt("tipo_comunicacion"), parametrosSQL, parametrosReport, Constantes.ID_PROYECTO)
            byte[] out = docPDF
            String nombrePDF = documentoRecuperado.getAt("tipo_comunicacion") + ".pdf"
            Filedownload.save(out, "application/pdf", nombrePDF)
            //Guardamos registro de descargas en auditoria
            datos.operacion = 'D'
            datos.login = session.loginUnico
            documentoEntidadService.auditoriaDocumentos(datos)
        }
    }

    @Listen("onClick=#btnBorrarDocu")
    def clickBorrarDocumento() {
        def registroSeleccionado = lbDocumentos.selectedItem.value
        Map datos = [:]
        datos.id_expediente = idExpediente
        datos.id_persona = idPersona
        datos.id_estudio = idEstudio
        datos.id_documento = registroSeleccionado

        // Comprobamos que pueda borrar
        if (registroSeleccionado == idDocumentoGestionar) {
            Messagebox.show("¿Confirma que desea borrar el documento con ID: ${datos.id_documento}?",
                    "Confirmar Acción", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, [
                    onEvent: { e ->
                        if (e.data.intValue() == Messagebox.YES) {
                            wndEdicionDocumento.visible = false
                            def resultado = documentoEntidadService.borrar(datos)

                            if (resultado.errores) {
                                ventanaAviso(resultado.errores)
                                return
                            }

                            actualizarDocumentos(datos.id_expediente)
                        }
                    }
            ] as org.zkoss.zk.ui.event.EventListener)
            // }
        } else {
            ventanaAviso("El documento con ID: ${datos.id_documento} no se puede borrar.")
        }

    }

    @Listen("onDoubleClick=#lbDocumentos listitem")
    def dobleClickResultado() {
        def registroSeleccionado = lbDocumentos.selectedItem.value
        log.debug("RegistroSeleccionado: ${registroSeleccionado}")

        // Comprobamos que tenga permiso para gestionarlo
        if (registroSeleccionado == idDocumentoGestionar) {
            // Se puede
            Map datos = [:]
            datos.id = registroSeleccionado
            documento.seleccion = datos.id
            def detallesDocumento = documento.recuperar(datos)
            idDocumento = detallesDocumento.ID

            // Rellenamops los valores de componentes
            escribirValorComponente(tituloDocumento, "Detalle Documento")
            escribirValoresVista("documento", detallesDocumento)

            if (documento_id_profesional.value != null && documento_id_profesional.value != "") {
                escribirValorComponente(documento_nombre_profesional, pandoraService.recuperaNombreProfesional(documento_id_profesional.value))
            }

            // escribirValorComponente(documento_nombre_profesional, documento.parametros.nombre_profesional)
            desactivarComponentesVista("documento")

            // Escribir nombre profesional de datos y gestión devolución
            if (documento_id_profesional_registro.value != null && documento_id_profesional_registro.value != "") {
                escribirValorComponente(documento_nombre_profesional_registro, pandoraService.recuperaNombreProfesional(documento_id_profesional_registro.value))
            }

            if (documento_prof_resolucion.value != null && documento_prof_resolucion.value != "") {
                escribirValorComponente(documento_nombre_profesional_resolucion, pandoraService.recuperaNombreProfesional(documento_prof_resolucion.value))
            }

            // Desactivamos los botones Guardar y Descartar
            btnGuardarDocumento.visible = false
            btnDescartarDocumento.visible = false
            // Mostramos el boton Editar y Cerrar ventana
            btnEditarDocumento.visible = true
            btnCerrarVentanaDocumento.visible = true

            for (componente in nuevoDocumento) {
                componente.visible = false
            }

            for (componente in detalleDocumento) {
                componente.visible = true
            }
            // Hacemos visible la ventana modal
            wndEdicionDocumento.visible = true
            wndEdicionDocumento.doModal()
        } else {
            // No se puede
            Messagebox.show("No tienes permiso para gestionar este documento.")
        }


    }

    @Listen("onClick=#insertarDocumento")
    def clickInsertarDocumento() {
        log.debug("Click Nuevo Documento. Abrimos modal")
        // Nuevo Documento
        Map datos = [:]
        datos.id = NUEVO_REGISTRO
        datos.fecha = new Date()
        datos.id_expediente = idExpediente
        datos.id_persona = idPersona
        datos.id_estudio = estudioEntidadService.recuperarUltimoEstudioPorIdExpediente(idExpediente).id

        documento.insertar(datos)

        escribirValorComponente(tituloDocumento, "Nuevo Documento")
        documento_id.disabled = true
        documento_fecha.disabled = true
        //documento_id_documento.disabled = true
        documento_nombre_profesional.disabled = true
        documento_id_estudio.disabled = true
        documento_id_expediente.disabled = true
        documento_id_persona.disabled = true
        for (componente in detalleDocumento) {
            componente.visible = false
        }
        for (componente in nuevoDocumento) {
            componente.visible = true
        }
        // Mostramos botones Guardar y Descartar
        btnGuardarDocumento.visible = true
        btnDescartarDocumento.visible = true
        btnDescartarDocumento.disabled = false
        // Ocultamos boton Cerrar Ventana
        btnCerrarVentanaDocumento.visible = false
        // Ocultamos boton Editar
        btnEditarDocumento.visible = false
        // Hacemos visible la ventana modal
        wndEdicionDocumento.visible = true
        wndEdicionDocumento.doModal()
    }

    def clickEditarDocumento() {
        activarComponentesVista("documento")

        // Hacemos visibles los botones guardar y descartar
        btnGuardarDocumento.visible = true
        btnDescartarDocumento.visible = true
        // Hacemos invisibles los botones editar, descartar y cerrar ventana
        btnEditarDocumento.visible = false
        btnDescartarDocumento.disabled = false
        btnCerrarVentanaDocumento.visible = false

        // Desactivamos campos que no se pueden modificar
        documento_id.disabled = true
        documento_fecha.disabled = true
        documento_nombre_profesional.disabled = true
        documento_id_documento_def.disabled = true
        documento_id_estudio.disabled = true
        documento_id_emision.disabled = true
        documento_fecha_registro.disabled = true
        documento_nombre_profesional_registro.disabled = true
        documento_nombre_profesional_resolucion.disabled = true
        documento_fec_resolucion.disabled = true

        // Desactivamos campos en caso de que el documento este marcado como devuelto
        if (documento_fecha_devolucion.value) {

            // Desactivamos campos
            documento_fecha_devolucion.disabled = true
            documento_id_motivo.disabled = true


        } else {
            // Preparamos Profesional y fecha registro
            def fechaActual = new Date()
            escribirValorComponente(documento_fecha_registro, fechaActual)

            // Rellenamos nombre profesional registro
            escribirValorComponente(documento_id_profesional_registro, documento.parametros.id_profesional)
            escribirValorComponente(documento_nombre_profesional_registro, documento.parametros.nombre_profesional)

            // Desactivamos campos de Gestión devolución
            documento_resuelto.disabled = true
            documento_dom_correcto.disabled = true
        }

    }

    def clickGestionado() {
        if (leerValorComponente(documento_resuelto) == true ) {
            // Preparamos campos gestion devolucion
            def fechaActual = new Date()
            escribirValorComponente(documento_fec_resolucion, fechaActual)
            escribirValorComponente(documento_prof_resolucion, documento.parametros.id_profesional)
            escribirValorComponente(documento_nombre_profesional_resolucion, documento.parametros.nombre_profesional)
        } else {
            // Limpiamos los campos gestion devolucion
            def fechaActual = null
            escribirValorComponente(documento_fec_resolucion, fechaActual)
            escribirValorComponente(documento_prof_resolucion, null)
            escribirValorComponente(documento_nombre_profesional_resolucion, null)
        }
    }


    def actualizarDocumentos(idExpediente) {
        if (idExpediente) {
            def documentosRecuperados = documentoEntidadService.recuperarDocPorIdExpediente(idExpediente)
            lbDocumentos.actualizar(documentosRecuperados)
        }
    }

    def descartarCambios() {
        // Ocultamos la ventana modal
        wndEdicionDocumento.visible = false
        // limpiamos
        limpiar()
    }

    void despuesDeGuardar() {
        wndEdicionDocumento.visible = false
        Map datos = [:]
        datos.id_expediente = idExpediente
        datos.id_persona = idPersona
        datos.id_estudio = idEstudio
        actualizarDocumentos(datos.id_expediente)
    }

    void limpiar() {
        limpiarValoresVista("documento")
        activacion()
    }
}
