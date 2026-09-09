package  pcacervix
import org.zkoss.zk.ui.Executions
import org.zkoss.zul.*

import base.BaseComposer
import groovy.util.logging.Slf4j

@Slf4j
class PrincipalComposer extends BaseComposer {

    // Servicios

				
    def despuesDeComponer() {
	
		log.debug("PRINCIPAL COMPOSER--> despues de componer")
		// Aquí inicializamos componentes y listas que se usarán en la página.
		// Se ejecuta cada vez que se carga la página
		
		
		// Para evitar que se acceda directamente al zul sin pasar por la autenticación, comprobamos que efectivamente
		// el usuario está autorizado
		// Esta comprobación es redundante con los filtros de seguridad (SeguridadFilters en BaseComposer)
		// pero es conveniente tenerla también aquí por si se configuraran mal dichos filtros o por lo que sea no se aplicaran.
		try {

			if (!session.autorizado) {
				Executions.sendRedirect("/entrada/desconexion")
				}
		} catch (e) {
			// En caso de que haya cualquier error, desconectamos la sesion actual
			Executions.sendRedirect("/entrada/desconexion")
		}
		
		log.debug("Perfil: ${session.perfil}")
		// Añadimos los elementos del menú
		/*if (session.perfil == 'TAPAS_GESTOR') {
			session.menu = [Inicio: 'Expedientes', GestionAuxiliares: 'Gestión de Auxiliares', GestionUsuarios: 'Gestión de Usuarios', TramiteTipo: 'Tramite Tipo']
		} else {
			session.menu = [Inicio: 'Expedientes', GestionAuxiliares: 'Gestión de Auxiliares']
		}*/
		/*session.submenu = [SolicitudesTipo: 'Solicitudes Tipo', TramitesTipo: 'Tramites Tipo']*/
		
		// A vlInicio lo ponemos como activo, ya que esto se ejecuta cuando carga la página
		org.zkoss.zk.ui.util.Clients.evalJavaScript("if (window.location.hash == '')  zAu.cmd0.redirect('#inicio');")
		
		
    }

	
	
}
