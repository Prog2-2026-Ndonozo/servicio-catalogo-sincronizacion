import com.ndonozo.serviciocatalogosincronizacion.config.CatedraProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CatedraProperties.class)
public class RestClientConfig {
}