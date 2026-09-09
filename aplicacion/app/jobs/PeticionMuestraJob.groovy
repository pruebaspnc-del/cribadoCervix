package cribadocervix

import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.springframework.beans.factory.annotation.Autowired
import groovy.util.logging.Slf4j

@Slf4j
// El job no es concurrente ya que no debe de empezar uno nuevo hasta que no haya
// terminado el anterior
@DisallowConcurrentExecution
class PeticionMuestraJob implements Job {

	@Autowired
	PeticionMuestraService peticionMuestrasService
	    
	void execute(JobExecutionContext context) throws JobExecutionException {
        // Procesamos los mensajes pendientes de enviar. El job no se ejecuta en desarrollo local.
		if (!System.properties['os.name'].toLowerCase().contains('windows')) {
			try {
				peticionMuestrasService.procesarMuestrasJob()
			} catch (Exception ex) {
				log.error("Error al ejecutar JOB: peticionMuestrasService.procesarMuestrasJob")
				log.error(ex.getMessage())
				ex.printStackTrace()
			}
		}
	}

}
