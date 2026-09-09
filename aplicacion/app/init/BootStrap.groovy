package  cribadocervix

import java.text.Normalizer
import java.text.Normalizer.Form
import org.quartz.JobDetail
import org.quartz.Trigger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import org.springframework.scheduling.quartz.SchedulerFactoryBean
import org.springframework.stereotype.Component
import org.quartz.*

import groovy.util.logging.Slf4j


@Component
@Slf4j
class BootStrap {
	
	@Autowired
	SchedulerFactoryBean schedulerFactory
	
	@Autowired
	Environment env
	
	void init() {


		// Establecemos que el locale por defecto es el de españa/español
		Locale.setDefault(new Locale("es", "ES"))
		
		// Comprobamos que no haya problemas con la configuración horaria
		assert java.time.ZoneId.systemDefault().toString()== "Europe/Paris" || java.time.ZoneId.systemDefault().toString()== "Europe/Madrid"
		assert java.time.ZoneId.of("Europe/Paris") == java.time.ZoneId.systemDefault() || java.time.ZoneId.of("Europe/Madrid") == java.time.ZoneId.systemDefault()
			
		
		/** Job Envio Peticiones Muestras **/
		// Creamos el jobdetail del job que vamos a ejecutar
		JobDetail jobDetailEnvio = JobBuilder.newJob(PeticionMuestraJob.class).withIdentity("PeticionMuestraJob").build()
		// Creamos el trigger que vamos a usar en el job
		Trigger triggerEnvio = TriggerBuilder.newTrigger().withIdentity("triggerEnvioPeticionMuestra")
		.startNow()
		.withSchedule(SimpleScheduleBuilder.simpleSchedule()
				.withIntervalInSeconds(env.getProperty("aplicacion.tiempo_ejecucion_job").toInteger())
				.repeatForever())
		.build()
		// Añadimos el job al scheduler de quartz

		if (!schedulerFactory.getScheduler().checkExists(JobKey.jobKey("PeticionMuestraJob"))) {
			schedulerFactory.getScheduler().scheduleJob(jobDetailEnvio, triggerEnvio)
		}
		
	}
	
	String cadenaEquivalente(String cadena) {
		
		String cadenaTransformada = Normalizer.normalize((cadena.trim().toUpperCase()), Form.NFD).replaceAll(/\p{InCombiningDiacriticalMarks}+/, '')
		return cadenaTransformada
	}
}
