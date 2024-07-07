import com.example.apitest.config.AuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthenticationInterceptor authenticationInterceptor;

    @Autowired
    public WebConfig(AuthenticationInterceptor authenticationInterceptor) {
        this.authenticationInterceptor = authenticationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 모든 URL에 인증 인터셉터를 등록합니다.
        registry.addInterceptor(authenticationInterceptor)
                .excludePathPatterns("/public/**"); // "/public/**" 패턴을 제외하고 모든 URL에 인증 인터셉터를 적용합니다.
    }
}
