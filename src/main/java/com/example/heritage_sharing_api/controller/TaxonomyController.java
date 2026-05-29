package com.example.heritage_sharing_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/taxonomy")
public class TaxonomyController {

    // Redirect to taxonomy page
    @GetMapping("/admin")
    public RedirectView admin() {
        return new RedirectView("/html/admin/taxonomy.html");
    }

    // Root redirect
    @GetMapping("/")
    public RedirectView index() {
        return new RedirectView("/html/admin/taxonomy.html");
    }
}
