package  cribadocervix.personas

import base.FormularioComposer
import  jade.persona.DatosPersonaComposer
import  cribadocervix.Constantes
import  cribadocervix.PersonasService
import  cribadocervix.UtilsPcacervixService
import  cribadocervix.entidad.ExpedienteEntidadService
import groovy.util.logging.Slf4j
import org.zkoss.zk.ui.select.annotation.Listen
import org.zkoss.zul.Messagebox

@Slf4j
class PersonaComposer extends FormularioComposer{
	
	//Servicios
	PersonasService personasService
	ExpedienteEntidadService expedienteEntidadService
    UtilsPcacervixService utilsPcacervixService

	DatosPersonaComposer datosPersonaComposer

    //Variables del profesional
    def descripcionPerfilProfesional
    def idProfesional
    def perfilMatrona = Constantes.SANITARIO_ASISTENCIAL
  
	def parametros
	def id_persona
	def fecha_alta
	def fecha_baja

			 
	def despuesDeComponer() {
        // Aquí inicializamos componentes y listas que se usarán en la página.
        //Y guardamos los parametros de sesion del profesional
        descripcionPerfilProfesional = session.perfil
        idProfesional = session.idProfesional
	}

	def activacion() {

		try {
			datosPersonaComposer = listaComposersComponentes.find { 
                it instanceof DatosPersonaComposer
            }
			if (session.id_seleccionado == null) {
				cambiarVlActivo("BuscadorPersonas")
				return
			}
			this.parametros = session.id_seleccionado
			this.id_persona = parametros.id_persona
			this.fecha_alta = parametros.fecha_alta
			this.fecha_baja = parametros.fecha_baja

			if (id_persona) {
				def parametros1 = [id_persona: id_persona, fecha_alta: fecha_alta, fecha_baja: fecha_baja]
				datosPersonaComposer.mostrarDatosPersona(parametros1)

			}
		} catch (Exception e) {
			ventanaError(e.getMessage())
			cambiarVlActivo("BuscadorPersonas")
			datosPersonaComposer.limpiar()
		}

	}
	
	@Listen("onClick=#btnExpedientePersona")
	def buscarExpediente() {
		def cumpleRequisitos = false
        //Recuperamos el codigo de centro de salud del profesional y de la persona
        def codCSMatrona = utilsPcacervixService.recuperarCodCSPorIdProfesional(idProfesional)
        def codCSPersona = personasService.recuperarCSPorIdPersona(id_persona)
        //Delimitamos el acceso al expediente si la persona no pertenece al mismo centro de salud
        //unicamente para profesionales matronas
        if(descripcionPerfilProfesional == perfilMatrona && codCSMatrona.COD_CS != codCSPersona.COD_CS ){
            ventanaAviso("Los datos no están disponibles porque la persona no pertenece a su centro de salud")
        }else{
            //Comprobamos si tienen expediente activo
            def expediente = expedienteEntidadService.recuperarExpedienteActivoPorIdPersona(id_persona)
            if (expediente) {
                session.id_expediente = expediente.id
                cambiarVlActivo("Expediente")
            } else {
                //Comprobamos si tiene expediente inactivo
                expediente = expedienteEntidadService.recuperarUltimoExpediente(id_persona)
                if (expediente) {
                    Messagebox.show("El expediente de esta persona está dado de baja. ¿Desea visualizarlo?",
                            "Confirmar Acción", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, [
                            onEvent: { e ->
                                if(e.data.intValue() == Messagebox.YES) {
                                    session.id_expediente = expediente.ID
                                    session.fecha_baja_exp = expediente.FECHA_BAJA
                                    cambiarVlActivo("Expediente")
                                }
                            }
                    ] as org.zkoss.zk.ui.event.EventListener)
                }else {
                    cumpleRequisitos = utilsPcacervixService.cumpleRequisitosCervix(id_persona)
                    if (cumpleRequisitos[0] != 0) {
                        Messagebox.show("Esta persona no tiene expediente en cribadocervix. ¿Desea crear un expediente a la persona con ID: " +id_persona+ "?",
                                "Confirmar Acción", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, [
                                onEvent: { e ->
                                    if(e.data.intValue() == Messagebox.YES) {
                                        crearExpediente(id_persona)
                                    }
                                }
                        ] as org.zkoss.zk.ui.event.EventListener)
                    } else {
                        ventanaAviso("Esta persona no tiene Expediente en PCACERVIX y no cumple con los requisitos para crearle uno.")
                    }
                }

            }
        }
		

	}

	def crearExpediente(id_per) {
		def datos = [:]
		datos.id_persona = id_per
		datos.id_profesional = session.idProfesional
		datos.id_unidad_funcional = session.unidad_funcional?.id
		if (datos.id_unidad_funcional == null) {
			def uf = utilsPcacervixService.recuperarUnidadFuncionalPorCodigo(Constantes.COD_UF_COORDINACION)
			datos.id_unidad_funcional = uf.id
		}
		def insertar = expedienteEntidadService.insertar(datos)
		if (insertar == 1) {
			def expediente = expedienteEntidadService.recuperarExpedienteActivoPorIdPersona(id_per)
			session.id_expediente = expediente.id
			cambiarVlActivo("Expediente")
		}
	}

}
