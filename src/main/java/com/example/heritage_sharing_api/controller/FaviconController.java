package com.example.heritage_sharing_api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class FaviconController {

    @GetMapping("/favicon.ico")
    public RedirectView favicon() {
        RedirectView redirectView = new RedirectView("/static/favicon.svg");
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }
}
