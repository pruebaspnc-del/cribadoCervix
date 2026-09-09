package  CRIBADOCERVIX.entidad

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

import base.BaseService
import base.PermisosEntidadService
import  jade.PersanService
import  CRIBADOCERVIX.AuxiliaresService
import  CRIBADOCERVIX.ExpedienteService
import  CRIBADOCERVIX.CRIBADOCERVIXService
import  CRIBADOCERVIX.UtilsCRIBADOCERVIXService
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.springframework.transaction.annotation.Transactional

@Slf4j
@Transactional
@Service
class ExpedienteEntidadService {

	@Autowired
	Environment env
	// Servicios
	@Autowired
	BaseService baseService
	@Autowired
	CRIBADOCERVIXService CRIBADOCERVIXService
	@Autowired
	ProcesoEntidadService procesoEntidadService
	@Autowired
	EstudioEntidadService estudioEntidadService
	@Autowired
	PermisosEntidadService permisosEntidadService
	@Autowired
	PruebaEntidadService pruebaEntidadService
	@Autowired
	ContactoEntidadService contactoEntidadService
	@Autowired
	ProductoDispensadoEntidadService productoDispensadoEntidadService
	@Autowired
	PersanService persanService
	@Autowired
	AuxiliaresService auxiliaresService
	@Autowired
	ExpedienteService expedienteService
	@Autowired
	UtilsCRIBADOCERVIXService utilsCRIBADOCERVIXService

	// Bean que nos permite hacer consultas SQL directas a la base de datos
	@Autowired
	Sql groovySql

	/*
	 *
	 * Métodos básicos de la entidad: validar, recuperar, modificar, insertar y borrar
	 *
	 *
	 */

    @Transactional(readOnly = true)
	def recuperar(datos, bloquear = false){
		log.debug("recuperar ExpedienteEntidadService datos: ${datos}")
		def sentencia = """
			select expediente.id, expediente.fecha_alta, expediente.fecha_baja, expediente.id_persona
			from CRIBADOCERVIX.expediente
			where id = :id_expediente 
            and fecha_baja = :fecha_baja_exp
		"""
		def resultado = baseService.recuperarEntidad(sentencia, datos, bloquear)

		if (resultado) {
			//Recuperamos el ultimo evento del expediente
			def expediente_historico = expedienteService.recuperarUltimoEventoExpediente(resultado)
			if(expediente_historico) {
				resultado.fecha_evento = expediente_historico.FECHA_EVENTO
				resultado.login_evento_exp = expediente_historico.LOGIN
				resultado.descripcion_evento_exp = expediente_historico.DESCRIPCION
				def tipoEvento = CRIBADOCERVIXService.recuperarTipoEventoExpediente(expediente_historico.CONTADOR_MOTIVO_EVENTO)
				resultado.tipo_evento = tipoEvento[0]
			}
			
			//Recuperamos datos de la persona
			def persona = persanService.buscarPersona(resultado.id_persona.toString().toUpperCase())
			if (persona) {
				resultado.id_persona = persona.id
				resultado.persona_nombre =  persona.nombre
				resultado.persona_apellido1 = persona.apellido1
				resultado.persona_apellido2 = persona.apellido2
				resultado.persona_apellidos_nombre = persona.apellido1 + " " + persona.apellido2 + ", " + persona.nombre
				resultado.persona_fecha_nacimiento = persona.fecha_nacimiento
				resultado.persona_codigo_sexo = persona.codigo_sexo
				
				resultado.persona_descripcion_sexo = utilsCRIBADOCERVIXService.recuperarDescripcionSexoPorCodigo(persona.codigo_sexo)
			}
			
			//Recuperamos la Identificacion de la persona
			resultado.identificacion = persanService.buscarIdentidadPersona(persona.id)
			
			// Recuperamos los procesos del expediente
			resultado.procesos = procesoEntidadService.recuperarProcesosPorIdExpediente(datos.id_expediente)
			log.debug("procesos: ${resultado.procesos}")
			
			resultado.procesos?.each{p ->
				//Recuperamos los estudios de cada proceso
				def estudiosProceso = estudioEntidadService.recuperarListaPorIdProceso(p.id)
				p.estudios = estudiosProceso
				estudiosProceso?.each { e ->
					//Recuperamos las pruebas de cada estudio
					def pruebasEstudio = pruebaEntidadService.recuperarPruebasPorIdEstudio(e)
					e.pruebas = pruebasEstudio
					//Recuperamos los productos dispensados de cada estudio
					def productosDispEstudio = productoDispensadoEntidadService.recuperarPorIdEstudio(e)
					productosDispEstudio?.each { prod ->
					
						//Recuperamos la prueba asociada al producto, en caso de que ya se haya generado
						def idPrueba = pruebaEntidadService.recuperarPruebaPorCodProducto(prod.COD_PRODUCTO)
						if(idPrueba != null) {
							prod.id_prueba = idPrueba[0]
						}else {
							prod.id_prueba = null
						}
					}
					e.productos = productosDispEstudio
				}
			}
				
			
		}

		log.debug("Resultado: ${resultado}")
		return resultado
	}


	def modificar(datos) {
		if(datos.fecha_baja_edicion) {
			datos.fecha_baja_edicion = datos.fecha_baja_edicion.toTimestamp()
		}
		
		def resultado

        def sentencia
        sentencia = """
				UPDATE CRIBADOCERVIX.expediente
				set fecha_baja = :fecha_baja_edicion
				where id = :id
				and fecha_baja = MAX_FECHA """

        resultado = baseService.modificarEntidad(sentencia, datos)

        //Guardamos registro en Historico
        // Recuperamos el contador maximo para el expdiente
        def sentenciaContadorH = """ select max(contador_h) from CRIBADOCERVIX.expediente_h where id = :id """
        def contadorHistorico = groovySql.firstRow(sentenciaContadorH, [id: datos.id])

        datos.contador_h = contadorHistorico[0] + 1

        def sentenciaH = "{call DBA_SGI.HISTORICOS_VERSION.REGISTRAR_VERSION('CRIBADOCERVIX.expediente_h' ,  " +
                " to_date('" +  datos.fecha_alta + "', 'DD/MM/YYYY')" +
                " , ' AND ID = " + datos.id +
                "' , 'ID, ' , '" + datos.id + ", ' " +
                " , " + datos.motivo_evento +
                " , '" + datos.descripcion_motivo +
                "', '" + datos.login +
                "' , null )}"

        groovySql.call(sentenciaH)

		return resultado
	}

	def insertar(datos) {
		def resultado = 0
		def sentencia

		def loginProfesional = utilsCRIBADOCERVIXService.recuperarDatosProfesional(datos)
		datos.login_profesional = loginProfesional.login
		//Comprobamos permisos
		def esquema = env.getProperty("aplicacion.proyecto")
		def permisos = permisosEntidadService.recuperarPermisosEntidad(esquema, "EXPEDIENTE", null)
		permisos.each { privilegio ->
			if (privilegio == "INSERT") {
				def id_expediente = recuperarExpedientePorIdPersona(datos.id_persona)
				if (id_expediente == null) {
					sentencia = "{call CRIBADOCERVIX.P_CREAR_EXPEDIENTE(?,?,?)}"
					
					resultado = groovySql.call(sentencia, [datos.id_persona, datos.login_profesional, datos.id_unidad_funcional])
				}
			}
		}
		return resultado
	}


	/*
	 * Métodos específicos de recuperar por otros campos distintos al id (recuperarPor).
	 * Normalmente devuelven un sólo registro.
	 */
    @Transactional(readOnly = true)
	def recuperarExpedientePorIdPersona(id_persona) {
		//Comprobar si la persona ya tiene un expediente (sólo puede tener uno)
		def sentencia = """
			select id
			from CRIBADOCERVIX.expediente 
			where id_persona = :id_persona
		"""
		def resultado = groovySql.firstRow(sentencia, [id_persona: id_persona])
		log.debug("recuperarExpedientePorIdPersona resultado: ${resultado}")
		return resultado
	}
    
    /**
     * Recuperamos el expediente con mayor fecha baja
     * @param id_persona
     * @return expediente
     */
    @Transactional(readOnly = true)
    def recuperarUltimoExpediente(idPersona){
        log.debug("recuperar ExpedienteEntidadService datos: " +idPersona)
        def sentencia = """
            SELECT ID,FECHA_ALTA,FECHA_BAJA,ID_PERSONA
            FROM CRIBADOCERVIX.EXPEDIENTE EX
            WHERE ID_PERSONA = :id_persona
            AND fecha_baja = (SELECT MAX(FECHA_BAJA) 
                              FROM CRIBADOCERVIX.EXPEDIENTE
                              WHERE ID = EX.ID)
        """
        def resultado = baseService.recuperarEntidad(sentencia, [id_persona: idPersona])
		return resultado
    }

    /**
     * Recuperamos el expediente de la persona(MAX_FECHA) por ID persona
     * @param id_persona
     * @return
     */
    @Transactional(readOnly = true)
    def recuperarExpedienteActivoPorIdPersona(id_persona) {
        def sentencia = """
            select  ID,FECHA_ALTA,FECHA_BAJA,ID_PERSONA
            from CRIBADOCERVIX.expediente 
            where id_persona = :id_persona
            and fecha_baja = MAX_FECHA
        """
        def resultado = groovySql.firstRow(sentencia, [id_persona: id_persona])
        return resultado
    }
    
    
}
