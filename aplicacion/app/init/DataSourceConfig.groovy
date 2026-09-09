package cribadocervix;

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy

import javax.sql.DataSource

/**
 * Configuración del DataSource y Groovy SQL para garantizar el correcto manejo de transacciones.
 * 
 * El TransactionAwareDataSourceProxy asegura que groovySql use la misma conexión de base de datos
 * dentro de una transacción Spring (@Transactional), previniendo:
 * - Deadlocks por múltiples conexiones en la misma transacción
 * - Auto-commits prematuros que impiden rollbacks correctos
 * - Inconsistencias de datos cuando falla parte de una operación transaccional
 * 
 * Referencia: https://sadalage.com/post/transactions_using_groovysql/
 */
@Configuration
@Slf4j
class DataSourceConfig {

    /**
     * Crea el bean Groovy SQL usando un DataSource transaction-aware.
     * Todas las operaciones realizadas con este bean participarán correctamente
     * en las transacciones gestionadas por Spring.
     * 
     * @param dataSource El DataSource autoconfigured por Spring Boot
     * @return Instancia de groovy.sql.Sql configurada para transacciones
     */
    @Bean
    Sql groovySql(@Autowired DataSource dataSource) {
        log.info("Configurando Groovy SQL con TransactionAwareDataSourceProxy")
        // Creamos el proxy directamente aquí para evitar dependencias circulares
        def transactionAwareDataSource = new TransactionAwareDataSourceProxy(dataSource)
        return new Sql(transactionAwareDataSource)
    }
}
