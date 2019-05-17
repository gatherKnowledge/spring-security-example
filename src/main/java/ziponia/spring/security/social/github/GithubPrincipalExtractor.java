package ziponia.spring.security.social.github;

import org.springframework.stereotype.Component;
import ziponia.spring.security.SocialProvider;
import ziponia.spring.security.social.BasePrincipalExtractor;

import java.util.Map;

@Component
public class GithubPrincipalExtractor extends BasePrincipalExtractor {

    @Override
    public Object extractPrincipal(Map<String, Object> map) {
        String id = map.get("id").toString();
        this.saveSocialUser(id, SocialProvider.GITHUB);
        return this.createPrincipal(map);
    }
}
