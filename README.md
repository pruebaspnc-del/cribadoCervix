# Módulo software para la gestión del cribado de cáncer de cérvix por edad

Trabajo de Fin de Grado — Grado en Ingeniería Informática
**Universidad Internacional de La Rioja (UNIR)**
Escuela Superior de Ingeniería y Tecnología

- **Autora:** Paloma Nogales Clavel
- **Director/a:** Judith Guadalupe Ley Flores
- **Fecha:** 08/09/2026

---

## Descripción

Este repositorio contiene el prototipo desarrollado como parte del TFG *"Módulo software para la gestión del cribado de cáncer de cérvix por edad"*. El trabajo aborda el diseño de una solución modular para la gestión del cribado poblacional de cáncer de cérvix, estructurada en cinco módulos funcionales (Captación, Invitaciones, Cribado, Seguimiento y Estadística) y basada en una arquitectura orientada a la interoperabilidad mediante el estándar HL7/FHIR.

El código publicado aquí corresponde al **prototipo de la lógica de negocio central** del sistema: en concreto, el mecanismo de transición de estados del expediente de cribado, definido por las reglas de negocio **RN-01 a RN-07**.

## Alcance del prototipo

> ⚠️ **Importante:** este repositorio tiene un carácter demostrativo y no constituye un sistema desplegable ni una aplicación en producción.

**Qué incluye:**
- La implementación de la máquina de estados del expediente de cribado (reglas RN-01 a RN-07).
- Datos de ejemplo para validar el comportamiento de las transiciones de estado.
- Componentes ejecutables y verificables de forma aislada, sin depender de infraestructura de despliegue real.

**Qué NO incluye:**
- Integración real con el Sistema de Información del Laboratorio (LIS), la Historia Clínica Electrónica (HCE), farmacias u otros sistemas sanitarios.
- Despliegue en infraestructura de producción, ya que ello requeriría certificados institucionales, accesos y acuerdos de interconexión propios de un Servicio de Salud, no disponibles fuera de ese entorno.

Esta decisión de alcance es coherente con los objetivos del TFG, centrado en el análisis de requisitos, el diseño funcional y la definición arquitectónica de la solución, complementados con el prototipado descrito en el capítulo 7 de la memoria.

## Estructura del repositorio

```
cribadoCervix/
└── aplicacion/        # Código del prototipo
```

*(Actualiza este árbol si añades nuevas carpetas o archivos relevantes.)*

## Requisitos

La aplicación **CribadoCervix** está construida sobre la siguiente arquitectura tecnológica:

- **Framework de aplicación:** Spring Boot
- **Lenguaje:** Groovy
- **Base de datos:** Oracle
- **Despliegue:** empaquetado como WAR sobre servidor Tomcat
- **Autenticación:** integración con sistema corporativo CAS
- **Acceso a datos:** Spring JDBC
- **Planificación de tareas:** Quartz
- **Generación documental:** Apache POI / XDocReport
- **Búsqueda:** Apache Lucene
- **Integraciones:** diversos servicios corporativos sanitarios

> Nota: en el prototipo publicado en este repositorio, las integraciones con servicios corporativos sanitarios (LIS, HCE, CAS, etc.) no están operativas, ya que requieren certificados y accesos propios del entorno institucional (ver apartado *Alcance del prototipo*). El repositorio recoge la lógica de negocio ejecutable de forma local.

## Instalación

```bash
git clone https://github.com/pruebaspnc-del/cribadoCervix.git
cd cribadoCervix/aplicacion
# compilar el proyecto con Gradle
./gradlew clean build
```

## Ejecución

```bash
# desplegar el WAR generado (build/libs/) en un servidor Tomcat local, o ejecutar mediante:
./gradlew bootRun
```

## Pruebas

El prototipo incluye pruebas con datos de ejemplo que permiten validar las transiciones de estado del expediente de cribado sin necesidad de conectividad con sistemas externos.

```bash
# comando para ejecutar las pruebas
```

## Relación con la memoria del TFG

Para más contexto sobre el diseño y la justificación de este prototipo, consulta los siguientes apartados de la memoria:

- **Apartado 4.4** — Reglas de negocio (RN-01 a RN-07)
- **Capítulo 6** — Arquitectura propuesta
- **Capítulo 7** — Prototipo y evaluación

## Trabajo futuro

Como se recoge en el capítulo 8 de la memoria, las líneas de trabajo futuro incluyen la implementación completa de la solución, la integración con sistemas sanitarios reales, la validación de la interoperabilidad, la evaluación del rendimiento y la escalabilidad, y la validación con usuarios y profesionales.

## Licencia

Este código se publica con fines académicos como parte de un Trabajo de Fin de Grado. 
No se autoriza su uso, copia o distribución sin permiso expreso de la autora.

## Contacto

Para dudas sobre este trabajo, puedes contactar con la autora a través de paloma.nogales159@comunidadunir.net.
