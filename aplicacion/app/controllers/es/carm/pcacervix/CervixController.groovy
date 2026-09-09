package es.carm.pcacervix

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

import groovy.util.logging.Slf4j

@Slf4j
@Controller
@ResponseBody
@RequestMapping("/pcacervixWS")
class CervixController {
	
	@Autowired
	PcaCervixService pcacervixService
	
	
	
}	
	
