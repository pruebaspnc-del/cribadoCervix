package es.carm.CRIBADOCERVIX

public class Constantes {
	
	
    public static final String MAX_FECHA= "31/12/4312";
	// constantes para el proyecto 
	public static final int ID_PROYECTO= 921;

	//Unidad Funcional
	public static final String COD_UF_COORDINACION = 'CRIBADOCERVIX_SSCC'
	
	// Variables para validación de codigo del Producto de VPH
	public static final int TAM_CODPRODUCTO = 11;
	public static final String PREFIJO_AUTOTOMA = "1873"
	
	// Estados dispensacion:
	public static final int DISPENSADA = 1;
	public static final int ANALIZANDOSE = 2;
	public static final int RESUELTO = 3;
	public static final int CANCELADO = 4;
	
    
    // Variables para validación de codigo del Producto de Citología Líquida
	public static final ArrayList PREFIJO_CIT_LIQ = ["1", "2", "3", "4", "5", "6", "7", "8", "9"]
	public static final int TAM_COD_CIT_LIQ = 8
	
	//Limite de resultados al consultar en buscador expedientes
	public static final int LIMITE_CONSULTAS = 1000;
	
	//Constantes que se envian en PeticionMuestra (citologia liquida)
	public static final String VPH_MUY_ALTO_RIESGO = "MUY ALTO RIESGO"
	public static final String VPH_ALTO_RIESGO = "ALTO RIESGO"
	public static final String VPH_NEGATIVO = "NEGATIVO"
	public static final String VPH_NO_VALIDO = "NO VALIDO"
	
	//Constantes para el ID del tipo de Procesos
	public static final int ID_PROCESO_CRIB_INI_REG = 1
	public static final int ID_PROCESO_CRIB_INI_IRREG = 2
	public static final int ID_PROCESO_CRIB_SUC_REG = 3
	public static final int ID_PROCESO_CRIB_SUC_IRREG = 4
	public static final int ID_PROCESO_SEGUIMIENTO = 5
	
	//Constantes para representar el ID del tipo de Estudios
	public static final int ID_TIPO_ESTUDIO_VPH = 1
	public static final String CODIGO_TIPO_EST_VPH = 'VPH'
	public static final String CODIGO_TIPO_EST_CITO = 'CITOLIQ'
	public static final String CODIGO_TIPO_EST_COTEST_1 = 'CO-TEST_1'
	public static final String CODIGO_TIPO_EST_COTEST_2 = 'CO-TEST_2'

	public static final int ID_TIPO_ESTUDIO_CITOLOGIA = 2
	public static final int ID_TIPO_ESTUDIO_COTEST_1 = 3
	public static final int ID_TIPO_ESTUDIO_GINECOLOGIA = 4
	public static final int ID_TIPO_ESTUDIO_COTEST_2 = 5
	public static final int ID_TIPO_ESTUDIO_ESTUDIO_PILOTO = 6
	
	//Constantes para el ID del tipo de Pruebas
	public static final int ID_TIPO_PRUEBA_VPH = 1
	public static final int ID_TIPO_PRUEBA_CITOLOGIA = 2
	public static final int ID_TIPO_PRUEBA_VPH_PILOTO = 7
	//Constantes CODIGO prueba
	public static final String CODIGO_TIPO_PRUEBA_VPH_AUTOTOMA = 'VPH_AUTOTOMA'
	public static final String CODIGO_TIPO_PRUEBA_CITO = 'CITO'
	
	//Constantes para ID tipo resultado de Estudio
	public static final int ID_RESULTADO_ESTUDIO_NO_PARTICIPA = 1
	public static final int ID_RESULTADO_ESTUDIO_EXCLUIDO = 2
	public static final int ID_RESULTADO_ESTUDIO_VPH_NEG = 3
	public static final int ID_RESULTADO_ESTUDIO_VPH_POS_1 = 4
	public static final int ID_RESULTADO_ESTUDIO_VPH_POS_2 = 5
	public static final int ID_RESULTADO_ESTUDIO_CITO_NEG = 6
	public static final int ID_RESULTADO_ESTUDIO_CITO_INSAT = 7
	public static final int ID_RESULTADO_ESTUDIO_CITO_POS_LEVE = 8
	public static final int ID_RESULTADO_ESTUDIO_CITO_POS_GRAVE = 9
	//Constantes CODIGO tipo resultado Estudio
	public static final String CODIGO_RESULT_EST_NO_PARTICIPA = 'NOPARTICIPA'
	public static final String CODIGO_RESULT_EST_EXCLUIDO = 'EXCLUIDO'
	public static final String CODIGO_RESULT_EST_VPH_NEG = 'VPHNEG'
	public static final String CODIGO_RESULT_EST_VPH_POS_1 = 'VPHPOS1'
	public static final String CODIGO_RESULT_EST_VPH_POS_2 = 'VPHPOS2'
	public static final String CODIGO_RESULT_EST_CITO_NEG = 'CIT_NEG'
	public static final String CODIGO_RESULT_EST_CITO_INSAT= 'CIT_INSATISF'
	public static final String CODIGO_RESULT_EST_CITO_POS_LEVE  = 'CIT_POS_LEVE'
	public static final String CODIGO_RESULT_EST_CITO_POS_GRAVE = 'CIT_POS_GRAVE'
	public static final String CODIGO_RESULT_EST_COT_NEG = 'COT_NEGATIVO'
	public static final String CODIGO_RESULT_EST_COT_POS = 'COT_POSITIVO'
	public static final String CODIGO_RESULT_EST_COT_NO_VALID = 'COT_NOVALIDO'
	public static final String CODIGO_RESULT_EST_COT_NEG_MEDIO = 'COT_NEGATIVO_MED'

	//Constantes para representar el ID de los Tipos de Estado de un estudio
	public static final int ID_ESTADO_PEND_INVIT = 1
	public static final int ID_ESTADO_PEND_PARTIC = 2
	public static final int ID_ESTADO_PEND_RESULT = 3
	public static final int ID_ESTADO_PEND_REP_PRU= 4
	public static final int ID_ESTADO_PEND_CITA = 5
	public static final int ID_ESTADO_PEND_PRU = 6
	public static final int ID_ESTADO_NO_ACUDE_CITA = 7
	public static final int ID_ESTADO_PEND_PROX_INVIT = 8
	public static final int ID_ESTADO_SOLICIT_COTEST = 9
	//Constantes CODIGO Tipo Estado Estudio
	public static final String CODIGO_ESTADO_PEND_REP_PRU= 'PEND_REP_PRU'
	
	//Constantes para el ID Motivo de Contacto esquema CERVIX
	public static final int ID_CONTACTO_RESULT_VPH = 1
	public static final int ID_CONTACTO_RESULT_CITO = 2
	public static final int ID_CONTACTO_RESULT_RECORD_CITA = 3

	//Constantes para el ID Motivo de Contacto esquema PANDORA
	public static final int ID_COMUNICACION_RESULT_VPH = 9
	public static final int ID_COMUNICACION_CITA_CITO = 11
	public static final int ID_COMUNICACION_CITA_COTEST = 16

	// Constantes para formato de fechas
	public static final String fecha_ddMMyyyy= "dd/MM/yyyy"
	public static final String fecha_ddmmyyyy = "dd/mm/yyyy"
    
    // ID perfil conectado (pandora.perfil)
     public static final int ID_PERFIL_GESTOR = 169
    
    //Perfiles aplicacion TEAM
    public static final String GESTOR = "CRIBADOCERVIX_GESTOR"
    public static final String SANITARIO_ASISTENCIAL = "CRIBADOCERVIX_SANITARIO_ASISTENCIAL"
    public static final String ADMINISTRATIVO = "CRIBADOCERVIX_ADMINISTRATIVO"
    public static final String COORDINADOR = "CRIBADOCERVIX_COORDINADOR"

    //Peticion Estado
    public static final int ID_ESTADO_PETI_PTE_ENVIO = 1
    public static final int ID_ESTADO_PETI_PROC_OK = 2
    public static final int ID_ESTADO_PETI_ERROR = 3
    public static final int ID_ESTADO_PETI_PROC_CANCE = 4
    public static final int ID_ESTADO_PETI_PROC_OK_SIN_WS = 5
    public static final int ID_ESTADO_PETI_PROCESANDO = 6
    public static final int ID_ESTADO_PETI_CADUCADO = 7

    //Peticion Acción
    public static final int ID_ACCION_PETI_CREAR = 1
    public static final int ID_ACCION_PETI_CANCELAR = 2
    
    //Sistema Destino (tabla PETICION_MUESTRA)
    public static final int SISTEMA_DESTINO_GESTLAB = 1
    public static final int SISTEMA_DESTINO_PATWIN = 2

    //Codigos de los tipos de documentos
    public static final String CARTA_INVITACION= "CARTA_INVITACION"
    public static final String CARTA_VPH_NEGATIVO= "CARTA_RESULTADO_NEGATIVO_VPH"
    public static final String CARTA_CITOLOGIA_NEGATIVA = "CARTA_CITOLOGIA_NEGATIVA"
    public static final String CARTA_NO_CONTACTADO_POSITIVO_VPH = "NO_CONTACTADO_POSITIVO_VPH"
    public static final String CARTA_NO_CONTACTADO_CITA_COTEST = "NO_CONTACTADO_CITA_COTEST"
    public static final String CARTA_REPETICION_VPH = "CARTA_REPETICION_VPH"
	public static final String CARTA_RESULTADO_CITO_POS_LEVE = "CARTA_RESULTADO_CITO_POS_LEVE"
    
    
    //Variable global
    public static final String RANGO_EDAD_FINAL = "gnRangoEdadFinal"
    public static final String DOC_VPH_POS_NO_CONTACT = "gnIdDocNoContactadoPosVPH"
    public static final String DOC_CITA_COTEST_NO_CONTACT = "gnIdDocNoContacCitaCotest"
    public static final String DOC_ILOCALIZABLE = "gnIdDocIlocalizable"
    public static final String DIAS_CADUCIDAD_PRUEBA = "gnDiasCaducidadPrueba"
    public static final String DIAS_CADUCIDAD_PETICION_VPH = "gnDiasCaducidadPetGestlab"
    public static final String DOC_CITO_POS_LEVE = "gnIdDocCitoPosLeve"
    public static final String DOC_CITO_NEGATIVA = "gnIdDocCitoNegativa"
	public static final String FECHA_LIMITE_CART_INV = "gnFechasLimiteCartaInvitacion"

	//Motivos de contacto en PANDORA
    public static final String MOTIVO_CON_RESULT_VPH = "RESULTVPH" 
    public static final String MOTIVO_CON_RESULT_CITO = "RESULTCITO"
    public static final String MOTIVO_CON_RESULT_COTEST = "RESULTCOTEST"
    public static final String MOTIVO_CON_CITA_COTEST = "CITACOTEST"
    public static final String MOTIVO_CON_RECORD_CITA = "RECORCITA"
    public static final String MOTIVO_CON_OTROS = "OTROS"
    
    //Numero Máximo de contactos permitidos antes de comunicación por carta
    public static final int NUM_MAX_CONTACTOS = 3
    
    //Notificacion Tipo
    public static final int ID_TIPO_NOTIF_CARTA = 1

	//Codigo Vacunas VPH de Vacusan
	public static final String CODIGO_VACUNA_VPH = "gnCodigoVacunaVPH"

	//Parametros de plantillas en Jasper
	static final String ENCABEZADO = "/Encabezado_Imagen.jpg"
	static final String PIE = "/Pie_Imagen.jpg"
	static final String QR_IMAGEN = "/QR_imagen.jpg"
	static final String QR_ENCUESTA_AUTOTOMA = "/QR_encuesta_autotoma.jpg"

	//ID PANDORA DOCUMENTO PLANTILLA
	static final int ID_PANDORA_COD_PANTILLA = 5

	//Descriptivos producto vacunas VPH
	static final String VPH_CERVARIX = "cervarix"
	static final String VPH_GARDASIL = "gardasil"
	static final String VPH_GARDASIL9 = "gardasil 9"

	//ID ZONA SALUD
	static final int ID_ZONAB = 43
	//ID PADRE ZONA SALUD
	static final int ID_PADRE_ZONAB = 22
	//ID PADRE AREA SALUD
	static final int ID_PADRE_AREA = 56

    //INICIO URL INFORMES CITOLOGIAS
    static final String INI_URL_NO_VALIDA ="http://hcuvahl7pat.ad.sms.carm.es"

}
