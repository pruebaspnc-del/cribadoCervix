/* ****************************************************************************
 * Archivo que guarda el js del menú lateral
 * ************************************************************************* */
let style

//Hace visible y no visible los tooltips de los submenus
$('.menu > ul > li > a').hover(
    function () {
        if ($('.sidebar').hasClass('active')) {
            let alturaTexto = $(this)[0].getBoundingClientRect().top;
            let correccionAltura = 20;

            let texto = $(this).find('span');

            let claseTexto = `
			.visualizar-texto-flotante {
				position: fixed !important;
				top: ${alturaTexto + correccionAltura}px !important;
				left: 80px !important;
				opacity: 1 !important;
				display: block !important;
				visibility: visible !important;
			}
		`;

            if (!style) {
                style = document.createElement('style');
                document.head.append(style);
            }

            style.textContent = claseTexto;
            texto.addClass('visualizar-texto-flotante');
        }
    },

    function () {
        if ($('.sidebar').hasClass('active')) {
            $(this).find('span').removeClass('visualizar-texto-flotante');
        }
    }
);


//Busca la clase "menu" y le asigna función a todos los "li".
$(".menu > ul > li").click(function (e) {
        //Quitar "active" del "li" que ya está activo. "Active" nos marca la sección en la que estamos,
        //en este caso, estará puesto por defecto "Inicio"
        $(this).siblings().removeClass("active");
        //Poner "active" en el "li" pinchado
        $(this).toggleClass("active");

        //"unfolded" es una clase que cambia la dirección de la flecha, haciendo el efecto de que gira
        //Quitar "unfolded" del "li" que estaba activo
        $(this).siblings().removeClass("unfolded");
        //Cerrar sub-menus abiertos
        $(this).siblings().find("ul").slideUp();
        //Y quitarle el "active" a esos sub-menus
        $(this).siblings().find("ul").find("li").removeClass("active");
        //Quitarle el unfolded los li hijos de sus items hermanos (sub-menu-hijo)
        $(this).siblings().find("ul").find("li").removeClass("unfolded");

        //Desplegar o plegar "ul" hijos del "li" activado
        if ($(this).hasClass("active")) {
            //Aquí entra cuando clicamos en un item como "Auxiliares" o "Personas" y está cerrado, para abrirse
            $(this).addClass("unfolded");
            $(this).children("ul").slideDown()
            setTimeout(() => {
                ajustarAlturaMenu(e, $(this));
            }, 350)
        } else {
            //Aquí entra cuando clicamos en un item como "Auxiliares" o "Personas" y está abierto, para cerrarse
            $(this).children("ul").find("ul").slideUp();
            $(this).children("ul").find("ul").children("li").toggleClass("unfolded");
            $(this).children("ul").slideUp();
            $(this).removeClass("unfolded");
            
            //Le quitamos el active y el unfolded a su hijo por si estuviera abierto
            $(this).children("ul").find("li").removeClass("active")
            $(this).children("ul").find("li").removeClass("unfolded")
        }
    }
);

$(".sub-menu > li, .sub-menu-large > li").click(function (e) {
		let clases = $(this)[0].className
		let padre = ($(this).parent())[0].className
		
        if(clases != "active unfolded") { 
			//Quitar "active" del "li" que ya está activo
	        $(this).siblings().removeClass("active");
	        //Poner "active" en el "li" pinchado
	        $(this).toggleClass("active");
	
	        //Quitar "unfolded" del "li" que estaba activo
	        $(this).siblings().removeClass("unfolded");
	        //Cambiar dirección de flecha
	        $(this).toggleClass("unfolded");
	
	        //Cerrar otros submenus abiertos
	        $(this).siblings().find("ul").slideUp();
	
	        //Si el ul tiene sub-menu, se despliega
	        let cantidadhijos = $(this).children("ul").length;
	
	        if (cantidadhijos > 0) {
				//Aquí entra cuando el sub-menú tiene otro sub-menú dentro. Como en "Cartas devueltas" o "Asociaciones"
	            e.stopPropagation();
	            $(this).children("ul").slideToggle();
	
	            $(this).children("ul").click(function (e) {
	                    $(this).slideUp();
	                }
	            );
	        } 
	        
	        if(padre == "sub-menu sub-menu-hijo"){
				e.stopPropagation();
				let padre_this = $(this).parent()
				//Quitamos el active y el unfolded del item clicado en el sub-menu-hijo
				$(this).removeClass("active");
				$(this).removeClass("unfolded");
				//Recogemos el sub-menu-hijo
				$(this).parent("ul").slideUp();
				//Quitamos active y unfolded del item clicado en el sub-menu
				let item_subMenu = padre_this.parent()
				item_subMenu.removeClass("active")
				item_subMenu.removeClass("unfolded")
				//Plegamos el sub-menu				
				item_subMenu.parent("ul").slideUp()
				//Quitamos active y unfolded del item clicado en el menú lateral
				let item_menuLateral =  item_subMenu.parent("ul").parent()
				item_menuLateral.removeClass("active")
				item_menuLateral.removeClass("unfolded")
			}
	        
	        setTimeout(() => {
				desplazarMenu(e, $(this));
	            ajustarAlturaMenu(e, $(this));
	        }, 350)
	        
		} else if ((padre == "sub-menu" || padre == "sub-menu-large") && clases == "active unfolded"){
			//Esta parte gestiona que al clicar, con un sub-menu-hijo abierto, su item padre, no se cierre el sub-menu
			e.stopPropagation();
			$(this).removeClass("active");
			$(this).removeClass("unfolded");
			$(this).children("ul").slideUp();
		}
    }
);


$(".menu-btn").click(function () {
    //"Active" se pone con el menú está plegado.
    $(".sidebar").toggleClass("active");
    if ($(".sidebar.active")) {
        $(".layout-sanidad").toggleClass("layout-sanidad-active");
    }
    ;
});

function desplazarMenu(evento, _subMenu){ 
	let anchoSubMenu = _subMenu[0].getBoundingClientRect().right;
	let subMenu = _subMenu.find("ul").first();
    //Esta variable nos permite dar un poco de margen al sub-menu. Ya que si no lo corregimos, aparecería muy pegado al padre:
    let anchoCorregido = anchoSubMenu - 130; 

    subMenu.animate({left: anchoCorregido + "px"}, 250);
}

function ajustarAlturaMenu(evento, _subMenu) {
    let alturaSubMenu = _subMenu[0].getBoundingClientRect().top;
    let alturaPantalla = $(window).height();
    let subMenu = _subMenu.find("ul").first();
    let subMenuHijos = subMenu.children("li").length
    let tamanyoSubMenu

    if (subMenuHijos > 0) {
        tamanyoSubMenu = subMenu[0].getBoundingClientRect().height;

        if (alturaPantalla < 850) {

            subMenuHijos = (subMenuHijos > 9) ? 9 : subMenuHijos;

            if (alturaSubMenu + tamanyoSubMenu > alturaPantalla) {
                alturaSubMenu = alturaPantalla - tamanyoSubMenu - 15
            }
        }
        ;
    }

    subMenu.animate({top: alturaSubMenu + "px"}, 100);
};