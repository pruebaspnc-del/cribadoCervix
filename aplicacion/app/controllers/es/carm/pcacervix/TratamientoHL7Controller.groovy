package es.carm.pcacervix

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

import groovy.util.logging.Slf4j

@Slf4j
@RestController
@RequestMapping("/tratamientohl7")
class TratamientoHL7Controller {
	
	@Autowired
	TratamientoHL7Service tratamientoHL7Service
	
	
	@PostMapping("/orur01")
	def oruR01 (@RequestBody datosJson) {
		
		def salida = [:]
		
		try {
			
			// Se realiza llamada a servicio correspondiente	
			salida = tratamientoHL7Service.oruR01((int)datosJson.idDetalleEvento)
			
		} catch (Exception ex) {
			// Hay que tratar de nuevo (0 es para que se trate de nuevo)
			salida = [resultado: 0, detalleError: ex.message]
		}
		
		return salida

	}
		
	
	@PostMapping("/orro02")
	def orrO02 (@RequestBody datosJson) {
		
		def salida = [:]
		
		try {
			
			// Se realiza llamada a servicio correspondiente	
			salida = tratamientoHL7Service.orrO02((int)datosJson.idDetalleEvento)
			
		} catch (Exception ex) {
			// Hay que tratar de nuevo (0 es para que se trate de nuevo)
			salida = [resultado: 0, detalleError: ex.message]
		}
		
		return salida
		

	}
	
	@PostMapping("/ormo01")
	def ormO01 (@RequestBody datosJson) {
		
		def salida = [:]
		
		try {
			
			// Se realiza llamada a servicio correspondiente
			salida = tratamientoHL7Service.ormO01((int)datosJson.idDetalleEvento)
			
		} catch (Exception ex) {
			// Hay que tratar de nuevo (0 es para que se trate de nuevo)
			salida = [resultado: 0, detalleError: ex.message]
		}
		
		return salida
		

	}
	
}	
	
