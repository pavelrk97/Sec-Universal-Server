package com.pavelrk97.secserver.client.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClient;

@Controller
public class HomeController {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final RestClient restClient = RestClient.create();

    @Value("${resource-server.demo-uri}")
    private String demoUri;

    public HomeController(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    @GetMapping("/")
    public String home(OAuth2AuthenticationToken auth, Model model) {
        model.addAttribute("username", auth.getPrincipal().getAttribute("sub"));
        model.addAttribute("provider", auth.getAuthorizedClientRegistrationId());
        return "index";
    }

    @GetMapping("/call-rs")
    @ResponseBody
    public String callResourceServer(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauth2)) {
            return "Not OAuth2 authenticated";
        }
        String registrationId = oauth2.getAuthorizedClientRegistrationId();

        OAuth2AuthorizedClient client = authorizedClientService
                .loadAuthorizedClient(registrationId, oauth2.getName());

        String accessToken = client.getAccessToken().getTokenValue();
        String typeHeader = registrationId.contains("jwt") ? "jwt" : "opaque";

        String body = restClient.get()
                .uri(demoUri)
                .header("Authorization", "Bearer " + accessToken)
                .header("type", typeHeader)
                .retrieve()
                .body(String.class);

        return "RS responded: " + body
                + "  (provider=" + registrationId + ", header type=" + typeHeader + ")";
    }
}
