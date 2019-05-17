package ziponia.spring.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.CompositeFilter;
import ziponia.spring.security.social.facebook.FacebookPrincipalExtractor;
import ziponia.spring.security.social.github.GithubPrincipalExtractor;
import ziponia.spring.security.social.kakao.KakaoPrincipalExtractor;

import javax.servlet.Filter;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableOAuth2Client
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Autowired
    private OAuth2ClientContext auth2ClientContext;

    @Autowired
    private FacebookPrincipalExtractor facebookPrincipalExtractor;

    @Autowired
    private KakaoPrincipalExtractor kakaoPrincipalExtractor;

    @Autowired
    private GithubPrincipalExtractor githubPrincipalExtractor;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        /*auth.inMemoryAuthentication()
                .withUser("user") // user 계정을 생성했다. 이부분에 로그인아이디가 된다.
                .password(passwordEncoder().encode("1234")) // passwordEncoder 로 등록 한 인코더로 1234 를 암호화한다.
                .roles("USER"); // 유저에게 USER 라는 역할을 제공한다.*/

        auth
                .userDetailsService(userDetailsService()).passwordEncoder(passwordEncoder());
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/css", "/js", "/h2-console/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/private/**").hasAnyRole("USER")
                .antMatchers("/admin/**").hasAnyRole("ADMIN")
                .anyRequest().permitAll()
        .and()
            .formLogin()
            .loginPage("/login")
            .usernameParameter("username")
            .passwordParameter("password")
            .failureUrl("/login?error=true")
        .and()
                .logout()
                .deleteCookies("JSESSIONID")
                .clearAuthentication(true)
                .invalidateHttpSession(true)
        .and()
            .exceptionHandling()
                // .accessDeniedPage("/access_denied")
                .accessDeniedHandler(customAccessDeniedHandler)
        .and()
            .addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class)
        ;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsServiceImpl();
    }

    private Filter ssoFilter() {
        CompositeFilter filter = new CompositeFilter();
        List<Filter> filters = new ArrayList<>();
        filters.add(ssoFilter(facebook(), "/login/facebook", SocialProvider.FACEBOOK));
        filters.add(ssoFilter(github(), "/login/github", SocialProvider.GITHUB));
        filters.add(ssoFilter(kakao(), "/login/kakao", SocialProvider.KAKAO));
        filter.setFilters(filters);
        return filter;
    }

    private Filter ssoFilter(ClientResources client, String path, SocialProvider provider) {
        OAuth2ClientAuthenticationProcessingFilter filter = new OAuth2ClientAuthenticationProcessingFilter(path);
        OAuth2RestTemplate template = new OAuth2RestTemplate(client.getClient(), auth2ClientContext);
        filter.setRestTemplate(template);
        UserInfoTokenServices tokenServices = new UserInfoTokenServices(
                client.getResource().getUserInfoUri(), client.getClient().getClientId());
        tokenServices.setRestTemplate(template);
        if (provider.equals(SocialProvider.FACEBOOK)) {
            tokenServices.setPrincipalExtractor(facebookPrincipalExtractor);
        }

        if (provider.equals(SocialProvider.GITHUB)) {
            tokenServices.setPrincipalExtractor(githubPrincipalExtractor);
        }

        if (provider.equals(SocialProvider.KAKAO)) {
            tokenServices.setPrincipalExtractor(kakaoPrincipalExtractor);
        }
        filter.setTokenServices(tokenServices);
        return filter;
    }

    @Bean
    @ConfigurationProperties("github")
    public ClientResources github() {
        return new ClientResources();
    }

    @Bean
    @ConfigurationProperties("facebook")
    public ClientResources facebook() {
        return new ClientResources();
    }

    @Bean
    @ConfigurationProperties("kakao")
    public ClientResources kakao() {
        return new ClientResources();
    }

    @Bean
    public OAuth2ClientContext auth2ClientContext() {
        return new DefaultOAuth2ClientContext();
    }

    @Bean
    public FilterRegistrationBean<OAuth2ClientContextFilter> oauth2ClientFilterRegistration(OAuth2ClientContextFilter filter) {
        FilterRegistrationBean<OAuth2ClientContextFilter> registration = new FilterRegistrationBean<OAuth2ClientContextFilter>();
        registration.setFilter(filter);
        registration.setOrder(-100);
        return registration;
    }

    /*@Bean
    public FacebookPrincipalExtractor facebookPrincipalExtractor() {
        return new FacebookPrincipalExtractor();
    }

    @Bean
    public KakaoPrincipalExtractor kakaoPrincipalExtractor() {
        return new KakaoPrincipalExtractor();
    }

    @Bean
    public GithubPrincipalExtractor githubPrincipalExtractor() {
        return new GithubPrincipalExtractor();
    }*/
}
