package es.carm.pcacervix



import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

import base.zk.ZulPage
import groovy.util.logging.Slf4j



@Slf4j
@Controller
class PrincipalCervixController implements ZulPage {

@Autowired
Environment env

@Autowired
ServletContext servletContext



@Autowired
HttpSession session


@RequestMapping(value = "/logoff")
def desconexion (HttpServletRequest request) {
// Realizamos la desconexion del usuario.
// Para ello invalidamos la sesion y redirigimos a la pagina de entrada
request.session.invalidate()

//Redirigimos a entrada/inicio
return "redirect:"+env.getProperty("cas.afterLogoutUrl")
}





}