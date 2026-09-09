package  cribadocervix.expedientes

import org.zkoss.zk.ui.select.annotation.Listen
import org.zkoss.zul.Checkbox
import org.zkoss.zul.Datebox
import org.zkoss.zul.Listbox
import org.zkoss.zul.Listheader
import org.zkoss.zul.Paging
import org.zkoss.zul.Textbox
import base.FormularioComposer
import  cribadocervix.ExpedienteService
import  cribadocervix.UtilsPcacervixService
import  cribadocervix.entidad.EstudioEntidadService
import  cribadocervix.entidad.TipoEstadoEstudioEntidadService
import  cribadocervix.entidad.TipoEstudioEntidadService
import  cribadocervix.entidad.TipoProcesoEntidadService
import  cribadocervix.entidad.TipoPruebaEntidadService
import  cribadocervix.Constantes


//@Slf4j
class BuscadorExpedientesComposer extends FormularioComposer{

	//Servicios
	UtilsPcacervixService utilsPcacervixService
	ExpedienteService expedienteService
	//Entidades
	TipoProcesoEntidadService tipoProcesoEntidadService
	TipoEstadoEstudioEntidadService tipoEstadoEstudioEntidadService
	EstudioEntidadService estudioEntidadService
	TipoEstudioEntidadService tipoEstudioEntidadService
    TipoPruebaEntidadService tipoPruebaEntidadService
	
	Listbox lbZonaSaludBusExp
	Listbox lbTipoProcesosBusExp
	Listbox lbTipoEstadoEstudioBusExp
	Listbox lbListaExpedientes
	Listheader lhListaExpedientesNombre
	Listbox lbTipoEstudioExp
    Listbox lbTipoPruebaBusExp
    def dvExpZBS

    Paging pagExpedientes
    //Parametros del filtro de busqueda

	Textbox tbIdExpBusExp
	Checkbox cbCancerPrevBusExp
	Checkbox cbExpInactivosBusExp
    Checkbox cbTubosDispenBusExp 
    Checkbox cbExpRecepLabBusExp
	Datebox dbFecAltaDesdeBusExp
	Datebox dbFecAltaHastaBusExp
    Datebox dbFecPruebaBusExp
    
    Listheader headerTipoPrueba
    Listheader headerFechaPrueba
	Listheader headerTipoProceso
    Listheader headerTipoEstudio
    Listheader headerPeticionEnviada
    Listheader headerCodProducto

    def descripcionPerfilProfesional
    def idProfesional
    def perfilMatrona = Constantes.SANITARIO_ASISTENCIAL

    Map parametros = [:]

    def despuesDeComponer() {
        // Aquí inicializamos componentes y listas que se usarán en la página.
        //Y guardamos los parametros de sesion del profesional
        descripcionPerfilProfesional = session.perfil
        idProfesional = session.idProfesional
        servicioOperaciones = expedienteService
        escribirValorDesplegables(descripcionPerfilProfesional)
    }

    def activacion() {
        if(!vlAnterior.equals("vlExpediente")) { //Comprobamos si venimos del detalle del expediente, para decidir si limpiar o no los filtros de búsqueda
            limpiar()
        }
        //Comprobamos si hay valores en el campo estado para mostrar o no los checks correspondientes cuando volvemos del detalle de expediente
        seleccionTipoEstadoEstudio()
    }

    def escribirValorDesplegables(def perfil) {
        rellenarListbox(lbTipoProcesosBusExp, auxiliarService.leerTablaAuxiliar("cribadocervix.proceso_tipo", "id asc"), "Seleccione..." )
        rellenarListbox(lbTipoEstadoEstudioBusExp, auxiliarService.leerTablaAuxiliar("cribadocervix.tipo_estado_estudio", "id asc"), "Seleccione..." )
        rellenarListbox(lbTipoEstudioExp, auxiliarService.leerTablaAuxiliar("cribadocervix.ESTUDIO_TIPO", "id asc"), "Seleccione...")
        //Ocultar el filtro de Zona Básica de Salud para perfil matrona
        if(perfil == perfilMatrona){
            dvExpZBS.visible = false
        }else{
            rellenarListbox(lbZonaSaludBusExp, utilsPcacervixService.recuperarZonasBasicasSalud(), "Seleccione...")
        }
        rellenarListbox(lbTipoPruebaBusExp, auxiliarService.leerTablaAuxiliar("cribadocervix.prueba_tipo"), "Seleccione...")
    }

    @Listen("onClick=#btnInsertarExpediente")
    def clickNuevoExpediente() {
        log.debug("Click clickNuevoExpediente")
        Map datos= [:]
        datos.id_expediente = NUEVO_REGISTRO
        datos.fecha_baja = null
		ver(datos) //nuevo_Resgistro=-1 constante que indica que vamos a insertar algo nuevo.  
	}
	
	@Listen("onClick=#btnBorrarFiltroBusExp")
	def clickBorrarFiltros() {
		log.debug("Click Borrar Filtros")
		limpiar()
	}
	

	@Listen("onClick=#btnBuscadorExpedientes")
	def clickBuscarExpedientes() {
		//Limpiamos la búsqueda anterior
		limpiarValorComponente(lbListaExpedientes)

        // Filtrar accesos datos matronas

        if (descripcionPerfilProfesional == perfilMatrona) {
            parametros.cod_cs = utilsPcacervixService.recuperarCodCSPorIdProfesional(session.idProfesional).cod_cs
        }

        parametros.zona_salud = leerValorComponente(lbZonaSaludBusExp)
        parametros.id_expediente = leerValorComponente(tbIdExpBusExp)
        parametros.cancer_prev = leerValorComponente(cbCancerPrevBusExp)
        parametros.expedientes_inactivos = leerValorComponente(cbExpInactivosBusExp)
        parametros.fec_alta_desde = leerValorComponente(dbFecAltaDesdeBusExp)
        parametros.fec_alta_hasta = leerValorComponente(dbFecAltaHastaBusExp)
        parametros.id_tipo_proceso = leerValorComponente(lbTipoProcesosBusExp)
        parametros.id_tipo_estado_estudio = leerValorComponente(lbTipoEstadoEstudioBusExp)
        parametros.id_tipo_estudio = leerValorComponente(lbTipoEstudioExp)
        parametros.tubos_dispensados = leerValorComponente(cbTubosDispenBusExp)
        parametros.fec_recep_lab = leerValorComponente(cbExpRecepLabBusExp)
        parametros.tipo_prueba = leerValorComponente(lbTipoPruebaBusExp)
        parametros.fec_prueba = leerValorComponente(dbFecPruebaBusExp)

        log.debug("Click buscar, parametros:${parametros}")

        //Comprobamos que se han introducido parámetros de búsqueda, para NO hacer una búsqueda masiva.
        def permisoBusqueda = false
        parametros.each { it->
            // comprobamos que it.value == true para evitar los checkbox con valor false por defecto
            //TODO: controlar los checkbox
            if(null != it.value && it.value != "" && it.value) {
                permisoBusqueda = true
            }
        }
        if(permisoBusqueda) {
            def lista = expedienteService.buscar(parametros)
            if (lista) {
                /*Indicamos si se deben mostrar ciertas columnas en la tabla de resultados
                Si indican el tipo de prueba, no mostramos la columna porque es readuntante, pero si mostramos la fecha
                y viceversa
                 */
                if (parametros.tipo_prueba == null && parametros.fec_prueba == null) {
                    headerFechaPrueba.visible = true
                    headerTipoPrueba.visible = true
                }else if (parametros.tipo_prueba != null && parametros.fec_prueba == null){
                    headerFechaPrueba.visible = true
                    headerTipoPrueba.visible = false
                }else if (parametros.tipo_prueba == null && parametros.fec_prueba != null) {
                    headerFechaPrueba.visible = false
                    headerTipoPrueba.visible = true
                }else if (parametros.tipo_prueba != null && parametros.fec_prueba != null) {
                    headerFechaPrueba.visible = false
                    headerTipoPrueba.visible = false
                }
                if (parametros.id_tipo_proceso) {
                    headerTipoProceso.visible = false
                }else {
                    headerTipoProceso.visible = true
                }
                if(parametros.id_tipo_estudio) {
                    headerTipoEstudio.visible = false
                }else {
                    headerTipoEstudio.visible = true
                }

                if (lista.size >= utilsPcacervixService.getMaxRegistros()) {
                    ventanaAviso("La consulta supera el volumen de registros permitido, solo se mostrarán 1000 registros.")
                }
                lista?.each { it ->
                    if(it.DNI == null) {
                        it.dni_nie = it.NIE
                    }else if(it.NIE == null){
                        it.dni_nie = it.DNI
                    }else if(it.DNI && it.NIE){
                        it.dni_nie = it.DNI
                    }else {
                        it.dni_nie = null
                    }
                }
                lbListaExpedientes.actualizar(lista)
            }else {
                if(descripcionPerfilProfesional == perfilMatrona && (parametros.id_expediente != null && parametros.id_expediente != "")){
                    ventanaAviso("Los datos de la paciente buscada no corresponden a este centro de salud/consultorio.")
                }else{
                    ventanaAviso("No hay resultados de búsqueda.")
                }
            }
        }else {
            ventanaAviso("Es necesario introducir algún filtro de búsqueda.")
            return
        }
    }


    @Listen("onChange=#tbIdExpBusExp")
    def activacionCheckInactivos() {
        if (tbIdExpBusExp.value.isEmpty()) {
            cbExpInactivosBusExp.disabled = true
            cbExpInactivosBusExp.checked = false
        }else {
            cbExpInactivosBusExp.disabled = false
        }
    }

    /**
     * Rellenamos los desplegables en funcion del tipo de proceso que eligen. Si no eligen ninguno, se muestran todos los tipos
     * @param evento
     */
    @Listen("onSelect=#lbTipoProcesosBusExp")
    void seleccionTipoProceso(){
        def idTipoProceso = leerValorComponente(lbTipoProcesosBusExp.selectedItem)
        rellenarListbox(lbTipoEstadoEstudioBusExp, auxiliarService.leerTablaAuxiliar("cribadocervix.tipo_estado_estudio", "id asc"), "Seleccione..." )
        if(idTipoProceso != null) {
            def estudioProceso = tipoEstudioEntidadService.recuperarTipoEstudioPorIdProceso(idTipoProceso)
            rellenarListbox(lbTipoEstudioExp, estudioProceso, "Seleccione...")
            seleccionTipoEstadoEstudio()
        }
        else {
            rellenarListbox(lbTipoEstudioExp, auxiliarService.leerTablaAuxiliar("cribadocervix.ESTUDIO_TIPO", "id asc"), "Seleccione...")
            seleccionTipoEstadoEstudio()
        }

    }

    @Listen("onSelect=#lbTipoEstudioExp")
    void seleccionTipoEstudio(){
        def idTipoEstudio = leerValorComponente(lbTipoEstudioExp.selectedItem)
        def estadosEstudio = tipoEstadoEstudioEntidadService.recuperarEstadosPorEstudio(idTipoEstudio)
        rellenarListbox(lbTipoEstadoEstudioBusExp, estadosEstudio , "Seleccione..." )
        seleccionTipoEstadoEstudio()
        def pruebasEstudio = tipoPruebaEntidadService.recuperarPruebasPorTipoEstudio(idTipoEstudio)
        rellenarListbox(lbTipoPruebaBusExp, pruebasEstudio, "Seleccione...")
    }

    @Listen("onSelect=#lbTipoEstadoEstudioBusExp")
    void seleccionTipoEstadoEstudio(){
        def idTipoEstado = leerValorComponente(lbTipoEstadoEstudioBusExp.selectedItem)
        if(idTipoEstado == Constantes.ID_ESTADO_PEND_PARTIC ) {
            cbTubosDispenBusExp.visible = true
        }else {
            cbTubosDispenBusExp.visible = false
            limpiarValorComponente(cbTubosDispenBusExp)
        }
        if(idTipoEstado == Constantes.ID_ESTADO_PEND_RESULT) {
            cbExpRecepLabBusExp.visible = true
        }else {
            cbExpRecepLabBusExp.visible = false
            limpiarValorComponente(cbExpRecepLabBusExp)
        }
    }

    @Listen("onDoubleClick=#lbListaExpedientes listitem")
    def clickDobleExpediente() {
        def itemSelected = lbListaExpedientes.selectedItem.value
        ver(itemSelected)
    }

    def ver(datos) {
        session.id_expediente = datos.id_expediente
        session.fecha_baja_exp = datos.fecha_baja
        cambiarVlActivo("Expediente")
    }

    void limpiar() {
        // Limpiamos todos los componentes de la ventana
        limpiarValorComponente(tbIdExpBusExp)
        limpiarValorComponente(dbFecAltaDesdeBusExp)
        limpiarValorComponente(dbFecAltaHastaBusExp)
        limpiarValorComponente(cbExpInactivosBusExp)
        limpiarValorComponente(cbCancerPrevBusExp)
        cbCancerPrevBusExp.disabled = true
        limpiarValorComponente(cbExpRecepLabBusExp)
        cbExpRecepLabBusExp.visible = false
        limpiarValorComponente(cbTubosDispenBusExp)
        cbTubosDispenBusExp.visible = false
        if(!lbZonaSaludBusExp.getItems().isEmpty()) {
            limpiarValorComponente(lbZonaSaludBusExp)
        }
        if(!lbTipoProcesosBusExp.getItems().isEmpty()) {
            limpiarValorComponente(lbTipoProcesosBusExp)
        }
        if(!lbTipoEstadoEstudioBusExp.getItems().isEmpty()) {
            limpiarValorComponente(lbTipoEstadoEstudioBusExp)
        }
        if(!lbListaExpedientes.getItems().isEmpty()) {
            limpiarValorComponente(lbListaExpedientes)
        }
        if(!lbTipoEstudioExp.getItems().isEmpty()) {
            limpiarValorComponente(lbTipoEstudioExp)
        }
        if (!lbTipoPruebaBusExp.getItems().isEmpty()) {
            limpiarValorComponente(lbTipoPruebaBusExp)
        }
        limpiarValorComponente(dbFecPruebaBusExp)
        escribirValorDesplegables()

    }

}