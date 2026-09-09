package  pcacervix



import base.BaseComposer
import groovy.util.logging.Slf4j
import org.zkoss.zk.ui.select.annotation.Wire

@Slf4j
class InicioComposer extends BaseComposer {
	//	 Servicios
//		PcaCervixService cervixService;

		@Wire('.solo-coordinador')
		List compSoloCoordinador
		@Wire('.resto-perfiles')
		List compRestoPerfiles
		
		// Otros
	
					
		def despuesDeComponer() {
			log.debug("INiCIO COMPOSER--> despues de componer")
			// Aquí inicializamos componentes y listas que se usarán en la página.
			// Se ejecuta cada vez que se carga la página

			
						Date fechaActual = new Date().clearTime()
			log.debug("fecha actual: ${fechaActual}")
			
		}
		
		def activacion() {
			log.debug("procesando activacion inicio")
			quitarComponentesVista()
		}
		
		def desactivacion() {
			log.debug("procesando desactivación inicio")
		}

		void quitarComponentesVista() {
			if (session.perfil != Constantes.COORDINADOR) {
				for (componente in compSoloCoordinador) {
					componente.detach()
				}
			} else {
				for (componente in compRestoPerfiles) {
					componente.detach()
				}
			}
		}
}
