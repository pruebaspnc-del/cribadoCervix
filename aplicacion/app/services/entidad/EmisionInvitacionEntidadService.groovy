package  CRIBADOCERVIX.entidad

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

import base.BaseService
import base.PermisosEntidadService
import  jade.FUENTESService
import  CRIBADOCERVIX.Constantes
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionAspectSupport

@Slf4j
@Transactional
@Service
class EmisionInvitacionEntidadService {

    // Servicios
    @Autowired
    Environment env
    @Autowired
    BaseService baseService
    @Autowired
    FUENTESService FUENTESService
    @Autowired
    PermisosEntidadService permisosEntidadService
    @Autowired
    DocumentoEntidadService documentoEntidadService

    // Bean que nos permite hacer consultas SQL directas a la base de datos
    @Autowired
    Sql groovySql

    def validar(parametros) {
        def errores = [:]

        if (null == parametros.anyo_nacimiento && (null == parametros.edad_hasta && null == parametros.edad_desde)) {
            errores.anyo_nacimiento = 'Debe indicarse año de nacimiento o ambas edades.'
        }
        if ((parametros.edad_desde == null && parametros.edad_hasta != null) ||
                (parametros.edad_desde != null && parametros.edad_hasta == null)) {
            errores.edad_desde = 'Deben indicarse ambas edades.'
        }
        if (parametros.area_salud == null) {
            errores.area_salud =  'Campo obligatorio.'
        }

        if(parametros.reinvitacion && parametros.fecha_cart_anterior_a == null){
            errores.fecha_cart_anterior_a = 'Debe indicarse la fecha'
        }
        if (!errores.isEmpty()) {
            errores.btnGuardarInv = 'Compruebe campos obligatorios.'
        }

        return errores
    }

    @Transactional(readOnly = true)
    def buscar (parametros, bloquear = false)  {
        def resultado
        def sentencia = """
							SELECT EI.ID, EI.FECHA, A.DESCRIPCION AREA_SALUD,
							Z.DESCRIPCION ZONA_SALUD, EI.AÑO_NACIMIENTO ANYO_NACIMIENTO,
							EI.EDAD_DESDE, EI.EDAD_HASTA, EI.DESCRIPCION, P.LOGIN, EI.NUM_DOC_GENERADOS, EI.FECHA_GENERACION,
							EI.REINVITACION, EI.FECHA_CART_ANTERIOR_A
							FROM CRIBADOCERVIX.EMISION_INVITACION EI
							LEFT JOIN FUENTES.ENTIDAD A ON A.ID = EI.AREA_SALUD AND A.FECHA_BAJA = MAX_FECHA
							LEFT JOIN FUENTES.ENTIDAD Z ON Z.ID = EI.ZONA_SALUD AND Z.FECHA_BAJA = MAX_FECHA
							LEFT JOIN PERSONA.PROFESIONAL_INFORMATICO P ON P.ID_PROFESIONAL = EI.ID_PROFESIONAL
							WHERE 1 = 1
						"""
        if (parametros.fecha_desde)	{
            sentencia += " AND TRUNC(EI.FECHA) >= TRUNC(TO_DATE('"+ parametros.fecha_desde.format(Constantes.fecha_ddMMyyyy) +
                    "', '"+ Constantes.fecha_ddmmyyyy +"')) "
        }
        if (parametros.fecha_hasta)	{
            sentencia += " AND TRUNC(EI.FECHA) <= TRUNC(TO_DATE('"+ parametros.fecha_hasta.format(Constantes.fecha_ddMMyyyy) +
                    "', '"+ Constantes.fecha_ddmmyyyy +"')) "
        }
        if (parametros.area_salud)	{
            sentencia += " AND EI.AREA_SALUD = " + parametros.area_salud
        }
        if (parametros.zona_salud)	{
            sentencia += " AND EI.ZONA_SALUD = " + parametros.zona_salud
        }

        if(parametros.reinvitaciones){
            sentencia += " AND EI.REINVITACION = 1"
        }
        sentencia += " ORDER BY FECHA DESC "
        resultado = groovySql.rows(sentencia)
        log.debug("Buscamos Emisiones: " + sentencia+ " + Resultado = " + resultado)
        return resultado
    }

    def insertar(parametros) {

        if(!parametros.reinvitacion && !parametros.fecha_cart_anterior_a){
            parametros.reinvitacion = 0
            parametros.fecha_cart_anterior_a = null
        }

        def sentencia = """ INSERT INTO CRIBADOCERVIX.EMISION_INVITACION (ID, FECHA, ID_PROFESIONAL,
							UNIDAD_FUNCIONAL, AREA_SALUD, ZONA_SALUD, EDAD_DESDE, EDAD_HASTA,
							DESCRIPCION, AÑO_NACIMIENTO, REINVITACION, FECHA_CART_ANTERIOR_A)
							VALUES (:id, SYSDATE, :id_profesional,
									:unidad_funcional, :area_salud, :zona_salud, :edad_desde,
									:edad_hasta, :descripcion, :anyo_nacimiento, :reinvitacion, :fecha_cart_anterior_a)
						"""
        
        def resultado = baseService.insertarEntidad(sentencia, parametros)
        log.debug("Sentencia INSERT: " + sentencia+ " + Resultado = "+ resultado)
        return resultado
    }

    def modificar(parametros) {
        def sentencia
        def resultado

        sentencia = """ UPDATE CRIBADOCERVIX.EMISION_INVITACION SET AREA_SALUD = :area_salud,
						ZONA_SALUD = :zona_salud, EDAD_DESDE = :edad_desde,
						EDAD_HASTA = :edad_hasta, DESCRIPCION = :descripcion,
						AÑO_NACIMIENTO = :anyo_nacimiento, REINVITACION = :reinvitacion,
						FECHA_CART_ANTERIOR_A = :fecha_cart_anterior_a WHERE ID = :id
					"""
        resultado = baseService.modificarEntidad(sentencia, parametros)
        log.debug("Sentencia UPDATE: " + sentencia+ " + Resultado = " +resultado)

        return resultado
    }

    def recuperar(datos,  bloquear = false) {
        def resultado

        def sentencia = """ 
            SELECT ID, FECHA, ID_PROFESIONAL, UNIDAD_FUNCIONAL,
			AREA_SALUD, ZONA_SALUD, EDAD_DESDE, EDAD_HASTA, DESCRIPCION,
			NUM_DOC_GENERADOS, AÑO_NACIMIENTO as anyo_nacimiento, ID_PROF_GENERA, FECHA_GENERACION, REINVITACION, FECHA_CART_ANTERIOR_A
			FROM CRIBADOCERVIX.EMISION_INVITACION EI 
            WHERE EI.ID = :id
		"""
        log.debug("Recuperacion de Invitacion por ID = " + datos.id)
        resultado = baseService.recuperarEntidad(sentencia, datos)
        return resultado
    }

    def generarCartasInvitacion(parametros) {
        Map resultado = [:]
        resultado.errores = ""
        //Comprobamos permisos
        def esquema = env.getProperty("aplicacion.proyecto")
        def permisos = permisosEntidadService.recuperarPermisosEntidad(esquema, "DOCUMENTO", null)
        if (permisos.find { it.equals("INSERT")}) {
            try {
                def sentencia = "{call CRIBADOCERVIX.GESTION.P_EMISION_INVITACION(?, ?, ?)}"
                resultado.operacion = groovySql.call(sentencia, [parametros.id, parametros.numDoc, parametros.profesional])
            }catch(Exception e){
                log.error("ERROR: " + e)
                resultado.errores += " Error al generar los documentos. Contacte con el departamento de informática. \n"
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
            }
            if(!resultado.errores) { //Actualizamos en EMISION_INVITACION el profesional que ha generado los documentos
                log.info("CARTAS GENERADAS")
                try {
                    resultado.update = profesionalCartas(parametros)
                }catch(Exception e) {
                    log.error("ERROR al actualizar profesional en EMISION_INVITACION")
                    resultado.errores += " No se ha actualizado el profesional. Contacte con departamento de informática. \n"
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
                }
            }
        }else {
            resultado.errores += " No tiene permisos de edición. \n"
        }        
        return resultado
    }

    def profesionalCartas(parametros) {
        def resultado = [:]

        def sentencia = "UPDATE CRIBADOCERVIX.EMISION_INVITACION SET ID_PROF_GENERA = " + parametros.profesional +
                ", FECHA_GENERACION = SYSDATE WHERE ID = " + parametros.id
        resultado = groovySql.execute(sentencia)

        return resultado
    }

    def recuperarEstimacion(idEmision) {
        def sentencia = "SELECT CRIBADOCERVIX.F_ESTIMACION_INVITACION(" + idEmision + ") ESTIMACION FROM DUAL"
        log.debug("estimacion sentencia: " + sentencia)
        def resultado = groovySql.firstRow(sentencia)
        return resultado
    }

    def generarIdInvitaciones() {
        def sentencia = "SELECT CRIBADOCERVIX.S_EMISION.nextval MAX_ID FROM DUAL"
        def resultado = groovySql.firstRow(sentencia)
        return resultado
    }

}
