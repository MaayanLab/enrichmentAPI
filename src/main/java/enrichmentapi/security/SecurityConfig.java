package enrichmentapi.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().antMatcher("/**")
                .authorizeRequests()
                .antMatchers("/**")
                .permitAll();
    }

    @Bean
    public FilterRegistrationBean<AuthorizationFilter> filterRegistrationBean() {
        FilterRegistrationBean<AuthorizationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(securityFilter());
        registrationBean.addUrlPatterns("/origin/api/v1/*");
        return registrationBean;
    }

    @Bean
    public AuthorizationFilter securityFilter() {
        return new AuthorizationFilter();
    }
}
