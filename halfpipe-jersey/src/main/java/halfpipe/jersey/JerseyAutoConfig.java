package halfpipe.jersey;

import com.fasterxml.jackson.databind.ObjectMapper;
import halfpipe.core.ApplicationProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import javax.inject.Inject;

/**
 * User: spencergibb
 * Date: 4/15/14
 * Time: 7:27 PM
 */
@Configuration
@ConditionalOnClass(ServletContainer.class)
public class JerseyAutoConfig {

    @Inject
    ApplicationProperties applicationProperties;

    @Inject
    ObjectMapper objectMapper;

    @Bean
    public HalfpipeJerseyApplciation jerseyConfig() {
        return new HalfpipeJerseyApplciation();
    }

    @Bean
    public ServletRegistrationBean jerseyServlet() {
        ServletContainer servletContainer = new ServletContainer(jerseyConfig());
        String prefix = applicationProperties.getPrefix();
        ServletRegistrationBean bean = new ServletRegistrationBean(servletContainer, toUrlMapping(prefix));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    private String toUrlMapping(String prefix) {
        StringBuilder url = new StringBuilder(prefix);
        url.append("/*");
        //TODO: adjust if it ends in slash?
        return url.toString();
    }
}
