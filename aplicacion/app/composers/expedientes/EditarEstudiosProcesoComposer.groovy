package  cribadocervix.expedientes

import org.springframework.stereotype.Component
import org.zkoss.zk.ui.select.annotation.Listen
import org.zkoss.zul.Button
import org.zkoss.zul.Datebox
import org.zkoss.zul.Label
import org.zkoss.zul.Listbox
import org.zkoss.zul.Textbox
import base.EntidadFormulario
import base.FormularioComposer

import  jade.PandoraService
import  cribadocervix.ExpedienteService
import  cribadocervix.entidad.EstudioEntidadService
import  cribadocervix.entidad.ExpedienteEntidadService
import  cribadocervix.entidad.ProcesoEntidadService
import  cribadocervix.AuxiliaresService
import groovy.util.logging.Slf4j


@Slf4j
@Component
class EditarEstudiosProcesoComposer extends FormularioComposer{
	
	//Servicios
	ExpedienteService expedienteService
	ProcesoEntidadService procesoEntidadService
	EstudioEntidadService estudioEntidadService
	ExpedienteEntidadService expedienteEntidadService
	PandoraService pandoraService
	EntidadFormulario estudio
	
	ExpedienteComposer invocador
	AuxiliaresService auxiliaresService
	
	//Parametros del filtro de busqueda
	Textbox estudio_id
	Textbox estudio_id_proceso 
	Listbox estudio_id_estudio_tipo
	Listbox estudio_id_estudio_resultado_tipo
	Datebox estudio_fecha_resultado
	Textbox estudio_unidad_funcional 
	Datebox estudio_fecha_inicio
	Label tituloEstudioExp
	
	//Variables
	def idPadre
	def idProceso
	def idExpediente
	
	//Botones
	Button btnGuardarEstudioExp
	Button btnCancelarEstudioExp
				
	def despuesDeComponer() {
		
		estudio = new EntidadFormulario("estudio","id_estudio", this)
		
		// Configuramos la entidad
		estudio.configurar("pcacervix", "estudio")
		// Recuperamos el id de profesional y su unidad funcional seleccionada del usuario actual
		def idProfesional = session.idProfesional
		def idUnidadFuncional = session.unidadesFuncionales[0].id
		def login = session.loginUnico
		// Establecemos los parámetros por defecto para todas las operaciones de entidades
		def parametros = [id_profesional: idProfesional, id_unidad_funcional: idUnidadFuncional, login: login]
		estudio.parametros = parametros
		// Establecer el servicio que va a gestionar la lista de operaciones
		servicioOperaciones = estudioEntidadService
	
		establecerBotonGuardar(btnGuardarEstudioExp)
		establecerBotonDescartar(btnCancelarEstudioExp)
	}
	
	def activacion() {
		
		log.debug("procesando ACTIVACION EditarEstudiosProcesoComposer")
		limpiar()
		
		estudio.activarComponentesVista()
		btnCancelarEstudioExp.disabled = false
		btnGuardarEstudioExp.disabled = true
		
		//Desactivamos el ID
		estudio_id.disabled = true
		
		//Rellenamos los valores de los Desplegables 
		def estudioTipo = auxiliarService.leerTablaAuxiliar("cribadocervix.estudio_tipo")
		rellenarListbox(estudio_id_estudio_tipo, estudioTipo, "Seleccione..." )
		def estudioResultTipo = auxiliarService.leerTablaAuxiliar("cribadocervix.estudio_resultado_tipo")
		rellenarListbox(estudio_id_estudio_resultado_tipo, estudioResultTipo , "Seleccione..." )
//		def unidadesFuncionales =  auxiliarService.leerTablaAuxiliar("pandora.unidad_funcional")
//		rellenarListbox(estudio_unidad_funcional, unidadesFuncionales, "-- Selecciona una Unidad Funcional --")
			
		if (invocador) {
			
			if (invocador.estudioActual.id_estudio == NUEVO_REGISTRO) {
				
				escribirValorComponente(tituloEstudioExp, invocador.estudioActual.titulo)
				//Crear mapa con datos por defecto
				Map m = [:]
				m.id = ""
				m.fecha_inicio = new Date().clearTime()
				m.id_proceso = invocador.estudioActual.id_proceso
				m.fecha_resultado = Date.parse("dd/MM/yyyy", MAX_FECHA)
				m.id_unidad_funcional = estudio.parametros.id_unidad_funcional 
				m.profesional = estudio.parametros.login
				estudio.insertar(m)
				
				//Deshabilitamos el id, que es autogenerico
				estudio_id.disabled = true
				
			}else {
				estudio_id_proceso.disabled = true
				estudio.seleccion= invocador.estudioActual.id_estudio
				def datosEstudio = estudio.recuperar()
				escribirValoresVista("estudio", datosEstudio)
				escribirValorComponente(tituloEstudioExp, invocador.estudioActual.titulo)
				rellenarListbox(estudio_id_estudio_resultado_tipo, auxiliaresService.buscarTipoResultadosPorEstudio(datosEstudio.ID_ESTUDIO_TIPO), "Seleccione...")
	
				if(datosEstudio.ID_ESTUDIO_RESULTADO_TIPO != null) {
					escribirValorComponente(estudio_id_estudio_resultado_tipo, datosEstudio.ID_ESTUDIO_RESULTADO_TIPO)
				}
			}
		}
	}
	
	def desactivacion() {
		log.debug("procesando DESACTIVACION expediente")
	}
	
	@Listen("onClick=button#btnGuardarEstudioExp")
	void clickGuardarCambiosEstudioExp() {
		log.debug("click btnGuardarEstudioExp")
		//Realizamos validaciones previas 
		
		def estudioActual = leerValoresVista("estudio")
		def estudioAnterior = estudioEntidadService.recuperarEstudioAnterior(estudioActual)
			
		if(estudioAnterior) {
			//Si se va a poner resultado al estudio actual, comprobamos que el estudio anterior, si lo hay, tenga Resultado. 
			//No pueden haber dos estudios abiertos al mismo tiempo
			if(null == estudioAnterior.ID_ESTUDIO_RESULTADO_TIPO || estudioAnterior.ID_ESTUDIO_RESULTADO_TIPO == "") {
				ventanaError("El Estudio anterior no tiene Resultado. Asignar antes de continuar")
				descartarOperaciones()
				return
			}
			
			//Si se va a poner resultado al estudio, comprobamos que el estudio anterior, si lo hay, tenga resultado con fecha anterior 
			// o igual a la del estudio actual.
			def fechaResultadoEstudioAnteriorFormat =  estudioAnterior.FECHA_RESULTADO.format("dd/MM/YYYY").toString()
			Date fecResultEstudioAnterior = Date.parse("dd/MM/yyyy", fechaResultadoEstudioAnteriorFormat)
			def fechaIniEstudioAcualFormat =  estudioActual.estudio_fecha_inicio.format("dd/MM/YYYY").toString()
			Date fecIniEstudioActual = Date.parse("dd/MM/yyyy", fechaIniEstudioAcualFormat)
			
			if(fecResultEstudioAnterior > fecIniEstudioActual) {
				ventanaError("La Fecha Resultado del estudio anterior es mayor a la Fecha Inicio del nuevo Estudio. Corregir fechas antes de continuar.")
				descartarOperaciones()
				return
			}
			def fechaResultadoEstudioActual = estudioActual.estudio_fecha_resultado
			//Comprobamos que si se introduce Fecha Resultado también hayan introducido Resultado.
			//Formateamos la fecha a string para compararla con MAX_FECHA
			fechaResultadoEstudioActual = estudioActual.estudio_fecha_resultado.format("dd/MM/yyyy").toString()
			if(fechaResultadoEstudioActual != MAX_FECHA &&  estudioActual.estudio_id_estudio_resultado_tipo == null) {
				ventanaError("No se puede asignar Fecha Resultado al estudio sin un resultado. Asignar Resultado.")
				descartarOperaciones()
				return
			}
			
			//Comprobamos que si se ha introducido un Resultado al estudio, haya Fecha Resultado y la fecha sea correcta (distinta a Fecha Max y mayor que Fecha Inicio)
			if(estudioActual.estudio_id_estudio_resultado_tipo != 0 && null != estudioActual.estudio_id_estudio_resultado_tipo) {
				if( fechaResultadoEstudioActual == MAX_FECHA ) {
					ventanaError("Si se asigna un resultado al estudio, la Fecha Resultado debe ser distinta a la fecha máxima.")
					descartarOperaciones()
					return
				}
				fechaResultadoEstudioActual = Date.parse("dd/MM/yyyy", fechaResultadoEstudioActual)
				if( fecIniEstudioActual >  fechaResultadoEstudioActual) {
					ventanaError("La Fecha Resultado del estudio debe ser mayor o igual a la Fecha de Inicio.")
					descartarOperaciones()
					return
				}
			}
		}
	}
	
	@Listen("onClick=button#btnCancelarEstudioExp")
	void clickDescartarCambiosEstudioExp() {
		
		log.debug("click Descartar cambios")
		// Ocultamos la ventana modal
		contenedor.visible = false
		//Limpiamos el invocador para evitar el error de 'Cambios Pendientes'
		invocador = null
		// Limpiamos el contenido de la ventana para su próxima utilización
		limpiar()
	}
	
	@Listen("onSelect=#estudio_id_estudio_tipo")
	void selectTipoEstudio() {
		def idTipoEstudio = leerValorComponente(estudio_id_estudio_tipo)
		rellenarListbox(estudio_id_estudio_resultado_tipo, auxiliaresService.buscarTipoResultadosPorEstudio(idTipoEstudio), "Seleccione...")
	}
	
	void despuesDeGuardar() {
		// Refrescamos la lista de procesos del expediente
		log.debug("Refrescamos datos de los Estudios")
	
		if(invocador) {
			Map expediente = [:]
			expediente.id_expediente = invocador.estudioActual.id_expediente
			// Recuperamos la info actualizada del expediente
			invocador.activacion()
			// Ocultamos la ventana modal
			contenedor.visible = false
			//Limpiamos el invocador para evitar el error de 'Cambios Pendientes'
			invocador = null
			// Limpiamos el contenido de la ventana para su próxima utilización
			limpiar()
		}
	}
	
	void limpiar() {
		// Limpiamos todos los componentes de la ventana
		limpiarValorComponente(estudio_id)
		limpiarValorComponente(estudio_id_proceso)
		limpiarValorComponente(estudio_fecha_resultado) 
		limpiarValorComponente(estudio_fecha_inicio)
	}

}
