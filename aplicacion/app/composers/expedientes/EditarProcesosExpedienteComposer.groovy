package  cribadocervix.expedientes

import org.springframework.stereotype.Component
import org.zkoss.zk.ui.select.annotation.Listen
import org.zkoss.zul.Button
import org.zkoss.zul.Checkbox
import org.zkoss.zul.Datebox
import org.zkoss.zul.Label
import org.zkoss.zul.Listbox
import org.zkoss.zul.Textbox
import base.EntidadFormulario
import base.FormularioComposer

import  cribadocervix.ExpedienteService
import  cribadocervix.entidad.ExpedienteEntidadService
import  cribadocervix.entidad.ProcesoEntidadService
import groovy.util.logging.Slf4j


@Slf4j
@Component
class EditarProcesosExpedienteComposer extends FormularioComposer{
	
	//Servicios
	ExpedienteService expedienteService
	
	ProcesoEntidadService procesoEntidadService
	ExpedienteEntidadService expedienteEntidadService
	EntidadFormulario proceso
	
	ExpedienteComposer invocador
	
	//Parametros del filtro de busqueda
	Textbox proceso_id
	Textbox proceso_id_padre 
	Listbox proceso_tipo_proc_exp
	Listbox proceso_resultado_proc_exp
	Checkbox proceso_inicial
	Checkbox proceso_irregular 
	Datebox proceso_fecha_inicio
	Datebox proceso_fecha_fin
	Label tituloProcesoExp
	
	//Variables
	def idPadre
	def idExpediente
	
	//Botones
	Button btnGuardarProcesoExp
	Button btnDescartarProcesoExp
				
	def despuesDeComponer() {
		
		proceso = new EntidadFormulario("proceso","id_proceso", this)
		
		// Configuramos la entidad
		proceso.configurar("pcacervix", "proceso")
		
//		proceso.servicio = procesoEntidadService
		// Establecer el servicio que va a gestionar la lista de operaciones
		servicioOperaciones = procesoEntidadService
	
		establecerBotonGuardar(btnGuardarProcesoExp)
		establecerBotonDescartar(btnDescartarProcesoExp)
	}
	
	def activacion() {
		
		log.debug("procesando ACTIVACION EditarProcesosExpedienteComposer")
		limpiar()
		
		proceso.activarComponentesVista()
		btnDescartarProcesoExp.disabled = false
		btnGuardarProcesoExp.disabled = true
		
		//Desactivamos el ID
		proceso_id.disabled = true
		
		//Rellenamos los valores de los Desplegables 
		def resultadoProc = auxiliarService.leerTablaAuxiliar("cribadocervix.proceso_tipo")
		rellenarListbox(proceso_tipo_proc_exp, resultadoProc, "-- Seleccione.." )
		def resultadoEst = auxiliarService.leerTablaAuxiliar("cribadocervix.proceso_resultado_tipo")
		rellenarListbox(proceso_resultado_proc_exp, resultadoEst , "-- Seleccione.." )
		
		if (invocador) {
			def expActual = invocador.procesoActual.id_expediente
			idPadre = procesoEntidadService.recuperarUltimoProcesoPorExpediente(expActual)
			
			if (invocador.procesoActual.id_proceso == NUEVO_REGISTRO) {
				escribirValorComponente(tituloProcesoExp, invocador.procesoActual.titulo)
				
				//Crear mapa con datos por defecto
				Map m = [:]
				m.id = ""
				m.fecha_inicio = new Date().clearTime()
				m.fecha_fin = Date.parse("dd/MM/yyyy", MAX_FECHA)
				m.id_padre = idPadre.id
				m.id_expediente = invocador.procesoActual.id_expediente
				proceso.insertar(m)
				
				//Deshabilitamos el id, que es autogenerico
				proceso_id.disabled = true
				
			}else {
				proceso.seleccion= invocador.procesoActual.id_proceso
				def datosProceso = proceso.recuperar()
				idPadre = datosProceso.id_padre
				escribirValoresVista("proceso", datosProceso)
				escribirValorComponente(tituloProcesoExp, invocador.procesoActual.titulo)
				
				if(null != datosProceso.ID_PROCESO_TIPO) {
					resultadoProc.each { it ->
						if(it.id == datosProceso.ID_PROCESO_TIPO) {
							def idTipoProc = (int)it.id
							escribirValorComponente(proceso_tipo_proc_exp, idTipoProc)
						}
					}
				}
				
				if(null != datosProceso.ID_RESULTADO_TIPO) {
					resultadoEst.each { it ->
						if(it.id == datosProceso.ID_RESULTADO_TIPO) {
							def idResultProc = (int)it.id
							escribirValorComponente(proceso_resultado_proc_exp, idResultProc)
							
						}
					}
				}
			}
		}
	}
	
	def desactivacion() {
		log.debug("procesando DESACTIVACION expediente")
	}
	
	@Listen("onClick=button#btnGuardarProcesoExp")
	void clickGuardarCambiosProcesoExp() {
		log.debug("click btnGuardarProcesoExp")
		//Realizamos validaciones previas 
		
		def procesoActual = leerValoresVista("proceso")
		
		def datos = [:]
		if (invocador.procesoActual.id_proceso == NUEVO_REGISTRO) {
			
			datos.id_proceso = idPadre.id
		} else { 
			datos.id_proceso = idPadre
		}
			
		def procesoPadre = procesoEntidadService.recuperar(datos)		
		if(procesoPadre) {
			//Comprobamos que el proceso anterior tenga Resultado. No puedes haber dos procesos abiertos a la vez
			if(null == procesoPadre.ID_RESULTADO_TIPO || procesoPadre.ID_RESULTADO_TIPO == "") {
				ventanaError("El proceso Padre no tiene Resultado. Asignar antes de continuar.")
				descartarOperaciones()
				return
			}
			
			//Comprobamos que el proceso esté cerrado con fecha anterior o igual a la que se va a crear el nuevo Proceso.
			def fechaFinProcPadreFormat =  procesoPadre.fecha_fin.format("dd/MM/YYYY").toString()
			Date fecFinProcPadre = Date.parse("dd/MM/yyyy", fechaFinProcPadreFormat)
			def fechaIniProcAcualFormat =  procesoActual.proceso_fecha_inicio.format("dd/MM/YYYY").toString()
			Date fecIniProcActual = Date.parse("dd/MM/yyyy", fechaIniProcAcualFormat)
			
			if(fecFinProcPadre > fecIniProcActual) {
				ventanaError("La Fecha Fin del proceso Padre es mayor a la Fecha Inicio del nuevo Proceso. Corregir fechas antes de insertar nuevo proceso.")
				descartarOperaciones()
				return
			}
			
			//Comprobamos que si se ha introducido Fecha Fin hayan introducido Resultado.
			def fechaFinProcActual = procesoActual.proceso_fecha_fin.format("dd/MM/yyyy").toString()
			if(fechaFinProcActual != MAX_FECHA &&  procesoActual.proceso_resultado_proc_exp == null) {
				ventanaError("Proceso con Fecha Fin sin Resultado. Asignar resultado.")
				descartarOperaciones()
				return
			}
			
			//Comprobamos que si se ha introducido un Resultado al proceso, la fecha Fin sea correcta (distinta a Fecha Max y mayor que Fecha Inicio)
			if(procesoActual.proceso_resultado_proc_exp != 0 && null != procesoActual.proceso_resultado_proc_exp) {
				if( fechaFinProcActual == MAX_FECHA ) {
					ventanaError("Si se asigna un Resultado al proceso, la Fecha Fin debe ser distinta a la fecha máxima.")
					descartarOperaciones()
					return
				}
				fechaFinProcActual = Date.parse("dd/MM/yyyy", fechaFinProcActual)
				fechaIniProcAcualFormat = Date.parse("dd/MM/yyyy", fechaIniProcAcualFormat)
				if( fechaIniProcAcualFormat >=  fechaFinProcActual) {
					ventanaError("La Fecha Fin debe ser mayor a la Fecha de Inicio.")
					descartarOperaciones()
					return
				}
			}
		}
	}
	
	@Listen("onClick=button#btnDescartarProcesoExp")
	void clickDescartarCambiosProcesoExp() {
		log.debug("click btnDescartarProcesoExp")
		// Ocultamos la ventana modal
		contenedor.visible = false
		//Limpiamos el invocador para evitar el error de 'Cambios Pendientes'
		invocador = null
		// Limpiamos el contenido de la ventana para su próxima utilización
		limpiar()
	}
	
	void despuesDeGuardar() {
		// Refrescamos la lista de procesos del expediente
		log.debug("Refrescar procesos")
	
		if(invocador) {
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
		limpiarValorComponente(proceso_id_padre)
//		limpiarValorComponente(proceso_tipo_proc_exp)
//		limpiarValorComponente(proceso_resultado_proc_exp) 
		limpiarValorComponente(proceso_inicial) 
		limpiarValorComponente(proceso_irregular)
		limpiarValorComponente(proceso_fecha_inicio) 
		limpiarValorComponente(proceso_fecha_fin)
	}

}
