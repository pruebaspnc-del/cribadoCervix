package  Cribadocervix

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import  jade.DocumentoWebService
import  jade.EntradaService
import  jade.FUENTESService
import  Cribadocervix.entidad.TipoEstudioEntidadService
import  Cribadocervix.entidad.TipoProcesoEntidadService
import org.springframework.transaction.annotation.Transactional

@Slf4j
@Transactional
@Service
class PersonasService {
    
	
	@Autowired
	TipoProcesoEntidadService tipoProcesoEntidadService
	@Autowired
	TipoEstudioEntidadService tipoEstudioEntidadService
		
    // Servicios comunes
	@Autowired
	FUENTESService FUENTESService
	
	@Autowired
	Environment env
	
	@Autowired
	EntradaService entradaService
	
	@Autowired
	DocumentoWebService documentoWebService
	
	// Bean que nos permite hacer consultas SQL directas a la base de datos
	@Autowired
	Sql groovySql

	
	/**
	 * 
	 * recuperar personas de persan
	 * @return
	 */
    @Transactional(readOnly = true)
	def recuperarPersonas(parametros) {
		def resultado
		
		def sentencia = generarSentenciarecuperarPersonas(parametros);
		
		log.debug("**recuperarPersonas Consulta SQL: " + sentencia)
		resultado = groovySql.rows(sentencia)
		log.debug("Resultado: " + resultado.size())
		
		return resultado
	}

    @Transactional(readOnly = true)
	def recuperarPersonas (parametros, int offset, int maxRows) {
		def resultado
		
		/*Limit y offset gestionado por Groovy SQL, no establece un offset real por lo que recupera datos hasta Offset+limit */
		def sentencia = generarSentenciarecuperarPersonas(parametros);
		log.debug("**recuperarPersonas con offset ${offset} y maxRows ${maxRows} Consulta SQL: " + sentencia )
		resultado = groovySql.rows(sentencia, offset, maxRows)
		
		log.debug("Resultado: " + resultado.size())
		
		return resultado
	}

    @Transactional(readOnly = true)
	def contarPersonas(parametros) {
		
		def sentencia = generarSentenciarecuperarPersonas(parametros);
		
		def sentenciaCount = "SELECT COUNT(*) count FROM ( "+ sentencia + ") a"
		def resultado = groovySql.rows(sentenciaCount)
		log.debug("contarPersonas - Resultado: " + resultado)

		return resultado.first().get("count").intValue()
		
	}
	
	private String generarSentenciarecuperarPersonas(parametros) {
		
		def tipo_doc = ""
		
		if (parametros.tipo_documento) {
			tipo_doc = parametros.tipo_documento
		}
				
		def sentencia = """
						SELECT P.ID,
							P.NOMBRE || ' ' || P.APELLIDO1 || ' ' || P.APELLIDO2 NOMBRE_COMPLETO,
							P.FECHA_NACIMIENTO,
							P.FECHA_ALTA,
							P.FECHA_BAJA FECHA_BAJA_PERSONA,
							PIT.VALOR,
							PPE.ID_ENTIDAD,
							PPE.FECHA_BAJA
						FROM PERSONA.PERSONA__PUESTO_ENTIDAD PPE
						LEFT JOIN PERSONA.PERSONA__IDENTIDAD_TIPO PIT ON PPE.ID_PERSONA = PIT.ID_PERSONA
						"""
		
						sentencia += " AND PIT.CODIGO_IDENTIDAD_TIPO LIKE '" + tipo_doc + "' "
						
						sentencia += """
						LEFT JOIN PERSONA.PERSONA P ON PPE.ID_PERSONA = P.ID
						LEFT JOIN FUENTES.ENTIDAD EN ON PPE.ID_ENTIDAD = EN.ID AND EN.FECHA_BAJA > sysdate 
						WHERE PPE.FECHA_BAJA = (SELECT MAX(FECHA_BAJA) FROM PERSONA.PERSONA__PUESTO_ENTIDAD WHERE ID_PERSONA = P.ID) """
						
		if (parametros.zona_salud) {
			sentencia += " AND EN.ID_PADRE = " + parametros.zona_salud
		}		
						
		if (parametros.id_persona) {
			sentencia += " AND P.ID = " + parametros.id_persona
		}
		
		if (parametros.nombre) {
			sentencia += " AND P.NOMBRE LIKE UPPER('%" + parametros.nombre + "%') "
		}
		
		if (parametros.primer_apellido) {
			sentencia += " AND P.APELLIDO1 LIKE UPPER('%" + parametros.primer_apellido + "%') "
		}
		
		if (parametros.segundo_apellido) {
			sentencia += " AND P.APELLIDO2 LIKE UPPER('%" + parametros.segundo_apellido + "%') "
		}
		
		if (parametros.fecha_nacimiento) {
			Date fechaNac = parametros.fecha_nacimiento
			sentencia += " AND P.FECHA_NACIMIENTO = TO_DATE('" + fechaNac.format("dd/MM/YYYY") + "', 'dd/mm/yyyy')"
		}
		
	    if (parametros.documento) {
			sentencia += " AND PIT.VALOR LIKE UPPER('%" + parametros.documento + "%') "
		}
		
		if (!parametros.baja) {
			sentencia += " AND P.FECHA_BAJA > SYSDATE "
		}
		
		if (parametros.fecha_alta) {
			Date fecha_alta = parametros.fecha_alta
			sentencia += " AND P.FECHA_ALTA >= TO_DATE('" + fecha_alta.format("dd/MM/YYYY") + "', 'dd/mm/yyyy')"
		}
		if (parametros.fecha_baja) {
			Date fecha_baja = parametros.fecha_baja
			sentencia += " AND P.FECHA_BAJA <= TO_DATE('" + fecha_baja.format("dd/MM/YYYY") + "', 'dd/mm/yyyy')"
		}
		
		sentencia += " ORDER BY NOMBRE_COMPLETO ASC"
		
		return sentencia
		
	}

    @Transactional(readOnly = true)
	def recuperarZonasSalud() {
		def sentencia = "SELECT ID_ENTIDAD ID, DESCRIPCION_CARM FROM FUENTES.ZONA_SALUD ORDER BY DESCRIPCION_CARM ASC";
		def resultado = groovySql.rows(sentencia)
		log.debug("Resultado: " + resultado.size())
		return resultado
	}

    /**
     * Metodo que recupera el código de centro de salud de una persona
     * @param id_persona
     * @return cod_cs
     */
    @Transactional(readOnly = true)
    def recuperarCSPorIdPersona(id_persona){
        def sentencia = """SELECT COD_CS FROM Cribadocervix.V_EXPEDIENTES_ZBS WHERE ID_PERSONA = :id_persona
        """
        def resultado = groovySql.firstRow(sentencia, [id_persona:id_persona])
        log.debug("Resultado: " + resultado.size())
        return resultado
    }
	
}
