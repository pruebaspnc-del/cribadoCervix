package  Cribadocervix

import base.BaseService
import  jade.TiposEntidadGlobales
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Slf4j
@Transactional
@Service
class DispensacionesFarmaciaService {

    // Servicios
    @Autowired
    UtilsCribadocervixService utilsCribadocervixService
    @Autowired
    BaseService baseService
    @Autowired
    Environment env
    @Autowired
    Sql groovySql

    static int PENDIENTE_DE_ENVIO = 1
	static int ACCION_ENVIAR_PETICION= 1

    /**
     * Recuperar Listado Farmacias Colaboradoras con la zona de salud
     * @param datos
     * @return
     */
    @Transactional(readOnly = true)
    def recuperarListadoFarmaciasColaboradoras(datos) {
        datos.id_asociacion = utilsCribadocervixService.recuperarVariableGLobalPorNombre("gnIdAsocZonaSFarmacias").VALOR as Integer
        def resultado = [:]
        log.debug("Recuperar Listado Farmacias Colaboradoras")
        def sentencia = """select SUBSTR(ent.descripcion || ' ' || pj.razon_social, 1,55) DESCRIPCION, ent.id, ent.codigo
        from  FUENTES.asociacion_entidad AE,
        FUENTES.entidad ent,
        FUENTES.entidad__persona_juridica epj,
        PERSONA.persona_juridica pj
        where AE.ID_ASOCIACION = :id_asociacion
        and AE.ID_ENTIDAD_1 = :id_zona_salud
        and ae.fecha_alta<=sysdate and ae.fecha_baja>sysdate
        and ae.id_entidad_2 = ent.id
        and ent.fecha_alta <=sysdate and ent.fecha_baja>sysdate
        and ent.id = epj.id_entidad(+)
        and epj.fecha_alta(+)<= sysdate
        and epj.fecha_baja(+)> sysdate
        and epj.id_persona_juridica = pj.id
        and pj.fecha_alta <= sysdate
        and pj.fecha_baja > sysdate
        """
        //resultado = groovySql.rows(sentencia, [id_zona_salud: datos.id_zona_salud, id_asociacion: datos.id_asociacion])
        resultado = groovySql.rows(sentencia, [id_zona_salud: datos.id_zona_salud, id_asociacion: datos.id_asociacion])
        return resultado
    }

    /**
     * Recuperar Listado Zonas Salud Farmacias Colaboradoras con la asociación de farmacias colaboradoras de la zona de salud
     * @return
     */
    @Transactional(readOnly = true)
    def recuperarListadoZonaSaludFarmaciasColaboradoras() {
        def datos = [:]
        datos.zona_salud = TiposEntidadGlobales.ENT_ZonaSalud
        datos.area_salud = TiposEntidadGlobales.ENT_AreaSalud
        datos.id_asociacion = utilsCribadocervixService.recuperarVariableGLobalPorNombre("gnIdAsocZonaSFarmacias").VALOR as Integer
        def resultado = [:]
        log.debug("Recuperar Listado Zonas Salud Farmacias Colaboradoras")
        def sentencia = """
        select distinct e.id, e.descripcion, e.codigo 
        from FUENTES.entidad e, FUENTES.asociacion_entidad ae
        where 
            ae.id_asociacion = :id_asociacion 
            and e.id = ae.id_entidad_1
            and e.id_tipo = :zona_salud
            and ae.fecha_alta<=sysdate 
            and ae.fecha_baja = MAX_FECHA
            and e.fecha_alta <=sysdate 
            and e.fecha_baja = MAX_FECHA
            and e.id_padre in (select id from FUENTES.entidad where id_tipo = :area_salud  and fecha_baja =MAX_FECHA)
        order by e.codigo asc
        """

        resultado = groovySql.rows(sentencia, [zona_salud: datos.zona_salud, area_salud: datos.area_salud, id_asociacion: datos.id_asociacion])
        return resultado

    }

    /**
     * Buscar Dispensaciones Registradas
     * @param parametros
     * @return
     */
    @Transactional(readOnly = true)
    def buscarDispensacionesRegistradas(parametros) {
        def resultado
        def limiteReg = utilsCribadocervixService.getMaxRegistros()
        parametros.put("limite", limiteReg)


        def sentencia = """
        select pd.cod_producto, 
			pde.fec_estado, 
			pe.codigo, pe.descripcion, 
            pd.cod_farmacia, to_char(pd.fec_dispensacion, 'dd/MM/yyyy HH24:MI:SS') fec_dispensacion, 
			p.id_expediente, 
			(SELECT Cribadocervix.F_NOMBRE_FARMACEUTICO(pd.COD_PROFESIONAL) FROM DUAL) profesional
        from Cribadocervix.producto_dispensado pd, Cribadocervix.producto_dispensado__estado pde, Cribadocervix.producto_estado pe, 
            Cribadocervix.estudio e, Cribadocervix.proceso p
        where
            pd.cod_producto = pde.cod_producto
            and e.id = pd.id_estudio
            and p.id = e.id_proceso
            and pde.id_producto_estado = pe.id
            and pde.contador = (select max(contador) from Cribadocervix.producto_dispensado__estado where cod_producto = pde.cod_producto)
            and ROWNUM <= :limite
        """

        // Logica filtro busqueda
        if (parametros.cod_farmacia) {
            sentencia += """    
            and pd.cod_farmacia = :cod_farmacia
            """
        }
        if (parametros.fecha_desde) {
            parametros.fecha_desde = parametros.fecha_desde.format("dd/MM/yyyy")
            sentencia += """
            and pd.fec_dispensacion >= :fecha_desde
            """
        }
        if (parametros.fecha_hasta) {
            parametros.fecha_hasta = parametros.fecha_hasta.format("dd/MM/yyyy")
            sentencia += """
            and pd.fec_dispensacion <= :fecha_hasta
            """
        }
        if (parametros.cod_producto) {
            sentencia += """
            and pd.cod_producto = :cod_producto
            """
        }

        resultado = groovySql.rows(sentencia, parametros)

        return resultado
    }

    /**
     * Recuperar Historial Dispensación
     * @param parametros
     * @return
     */
    @Transactional(readOnly = true)
    def recuperarHistorialDispensacion(parametros) {
        parametros.accion_enviar_peticion = ACCION_ENVIAR_PETICION
        def resultado

        def sentencia = """
        select pd.cod_producto, pde.contador, 
			to_char(pde.fec_estado, 'dd/MM/yyyy HH24:MI:SS') fec_estado,
			pe.codigo,
            pe.descripcion, 
            pd.cod_farmacia, 
            pd.fec_dispensacion, 
			p.id_expediente, 
            (SELECT Cribadocervix.F_NOMBRE_FARMACEUTICO(pd.COD_PROFESIONAL) FROM DUAL) profesional,
            (select trunc(fec_peticion) 
             from Cribadocervix.peticion_muestra pm 
             where pm.cod_producto = pd.cod_producto 
             and trunc(pm.fec_creacion) = trunc(pd.fec_dispensacion) 
             and pm.id_accion= :accion_enviar_peticion) as fec_peticion
        from Cribadocervix.producto_dispensado pd, Cribadocervix.producto_dispensado__estado pde, Cribadocervix.producto_estado pe, 
            Cribadocervix.estudio e, Cribadocervix.proceso p
        where
            pd.cod_producto = pde.cod_producto
            and e.id = pd.id_estudio
            and p.id = e.id_proceso
            and pde.id_producto_estado = pe.id
            and pd.cod_producto = :cod_producto
        order by pde.contador asc
        """

        resultado = groovySql.rows(sentencia, parametros)

        return resultado
    }

    /**
     * Recuperar resumen semanal de Dispensaciones por Farmacia
     * @param parametros
     * @return
     */
    @Transactional(readOnly = true)
    def recuperarResumenDispensacionesPorFarmacia(datos){
        datos.id_asociacion = utilsCribadocervixService.recuperarVariableGLobalPorNombre("gnIdAsocZonaSFarmacias").VALOR as Integer
        def resultados = [:]
        def limiteReg = utilsCribadocervixService.getMaxRegistros()
        datos.put("limite", limiteReg)
		datos.prodCancelado = Constantes.CANCELADO
		datos.prodAnalizando = Constantes.ANALIZANDOSE
		datos.prodResuelto =  Constantes.RESUELTO
		
		
        def sentencia = """
        SELECT e.CODIGO ,pdt.COD_FARMACIA , COUNT(DISTINCT pdet.cod_producto) AS totales,
            (SELECT COUNT(DISTINCT pde.cod_producto) AS anulados
                FROM Cribadocervix.PRODUCTO_DISPENSADO pd, Cribadocervix.PRODUCTO_DISPENSADO__ESTADO pde
                WHERE pd.COD_PRODUCTO = pde.COD_PRODUCTO  AND trunc(pd.FEC_DISPENSACION) >= TO_DATE(:fechaDesde, 'dd/MM/yyyy') AND trunc(pd.FEC_DISPENSACION) < TO_DATE(:fechaHasta, 'dd/MM/yyyy')
                AND pdt.COD_FARMACIA = pd.COD_FARMACIA
                AND pde.ID_PRODUCTO_ESTADO = :prodCancelado
                ) AS anulados,
            (SELECT COUNT(DISTINCT pde.cod_producto) AS anulados
                FROM Cribadocervix.PRODUCTO_DISPENSADO pd, Cribadocervix.PRODUCTO_DISPENSADO__ESTADO pde
                WHERE pd.COD_PRODUCTO = pde.COD_PRODUCTO AND trunc(pd.FEC_DISPENSACION) >= TO_DATE(:fechaDesde, 'dd/MM/yyyy') AND trunc(pd.FEC_DISPENSACION) < TO_DATE(:fechaHasta, 'dd/MM/yyyy')
                AND pdt.COD_FARMACIA = pd.COD_FARMACIA
                AND pde.ID_PRODUCTO_ESTADO = :prodAnalizando AND trunc(pd.FEC_DISPENSACION) >= TO_DATE(:fechaDesde, 'dd/MM/yyyy')  AND trunc(pd.FEC_DISPENSACION) < TO_DATE(:fechaHasta, 'dd/MM/yyyy')
                ) AS en_laboratorio,
            (SELECT COUNT(DISTINCT pde.cod_producto) AS anulados
                FROM Cribadocervix.PRODUCTO_DISPENSADO pd, Cribadocervix.PRODUCTO_DISPENSADO__ESTADO pde
                WHERE pd.COD_PRODUCTO = pde.COD_PRODUCTO AND trunc(pd.FEC_DISPENSACION) >= TO_DATE(:fechaDesde, 'dd/MM/yyyy')  AND trunc(pd.FEC_DISPENSACION) < TO_DATE(:fechaHasta, 'dd/MM/yyyy')
                AND pdt.COD_FARMACIA = pd.COD_FARMACIA
                AND pde.ID_PRODUCTO_ESTADO = :prodResuelto 
                ) AS resueltos
        FROM Cribadocervix.PRODUCTO_DISPENSADO pdt, Cribadocervix.PRODUCTO_DISPENSADO__ESTADO pdet, FUENTES.ASOCIACION_ENTIDAD ae , FUENTES.ENTIDAD e 
        WHERE pdt.COD_PRODUCTO = pdet.COD_PRODUCTO
        AND (pdt.COD_FARMACIA = REGEXP_SUBSTR(e.CODIGO,'[1-9]+') OR (pdt.COD_FARMACIA = e.CODIGO))
        AND ae.id_entidad_2 = e.id
        AND ae.ID_ASOCIACION = :id_asociacion
        AND ae.ID_ENTIDAD_1 = :id_zona_salud 
        AND ae.fecha_alta<=sysdate and ae.fecha_baja>sysdate
        AND e.fecha_alta <=sysdate and e.fecha_baja>sysdate
        AND trunc(pdt.FEC_DISPENSACION) >= TO_DATE(:fechaDesde, 'dd/MM/yyyy')
        AND trunc(pdt.FEC_DISPENSACION) < TO_DATE(:fechaHasta, 'dd/MM/yyyy')
        AND ROWNUM <= :limite
        GROUP BY pdt.COD_FARMACIA, e.CODIGO
        """
        resultados = groovySql.rows(sentencia, datos)
        //Se incluyen en resultados los campos calculados a partir de los resultados de la consulta
        for (int i = 0; i < resultados.size(); i++){
            resultados[i].pendientesE = ((resultados[i].totales - resultados[i].anulados) - resultados[i].en_laboratorio)
            resultados[i].pendientesR = resultados[i].en_laboratorio - resultados[i].resueltos

        }

        return resultados
    }
    /**
     * Modifica un producto de la tabla Cribadocervix.PETICION_MUESTRA para que su estado sea PENDIENTE DE ENVIO
     * @param datos.cod_producto
     * @return
     */
    def reenviarDispensacion(datos) {
        def resultado
        log.debug("Reenviar dispensacion: ${datos}")
        def sentencia = """
				update Cribadocervix.PETICION_MUESTRA
				set ID_ESTADO_PETIC = ${PENDIENTE_DE_ENVIO}
				where cod_producto like :cod_producto
				"""
        //resultado = baseService.modificarEntidad(sentencia, datos)
        resultado = groovySql.execute(sentencia, datos)
        return resultado
    }

    /**
     * Se registra en la tabla Cribadocervix.PETICION_MUESTRA_A
     * @param datos.cod_producto, datos.login
     */
    def auditoriaReenviarDispensacion(datos) {
        def resultado
        log.debug("Auditoria reenviar dispensacion: ${datos}")
        def sentencia = """
			
			insert into Cribadocervix.peticion_muestra_a (cod_producto, fec_reenvio, login)
			values (:cod_producto, sysdate, :login)
	
			"""
        // Ejecutamos el insert
        resultado = groovySql.execute(sentencia, datos)
        return resultado
    }
}
