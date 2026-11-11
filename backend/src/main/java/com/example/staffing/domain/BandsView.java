package com.example.staffing.domain;

import java.util.List;

/**
 * The three ordered scales as the screens read them, so a form offers the same names the server
 * accepts and no vocabulary is copied into the browser.
 */
public record BandsView(
    List<String> jobLevels, List<String> proficiencies, List<String> travel) {}
