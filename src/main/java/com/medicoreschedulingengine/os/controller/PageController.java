package com.medicoreschedulingengine.os.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * PageController
 *
 * Maps URL paths to Thymeleaf HTML templates.
 * These are the actual pages the user sees in the browser.
 *
 * All templates go in: src/main/resources/templates/
 *
 * The REST API controllers (PatientController, DepartmentController,
 * SimulationController) handle all data — these pages just load
 * the HTML and then call those REST endpoints via fetch() in JavaScript.
 *
 * URL Map:
 *   /              → templates/index.html       (landing / home page)
 *   /dashboard     → templates/dashboard.html   (main dashboard)
 *   /patients      → templates/patients.html    (add & view patients)
 *   /simulation    → templates/simulation.html  (run simulations)
 *   /history       → templates/history.html     (past simulation runs)
 */
@Controller
public class PageController {

    /**
     * Landing page — shown when user opens http://localhost:8080/
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * Main dashboard — overview of all departments and recent results
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    /**
     * Patients page — add new patients, view by department
     */
    @GetMapping("/patients")
    public String patients() {
        return "patients";
    }

    /**
     * Simulation page — select department and run simulation
     */
    @GetMapping("/simulation")
    public String simulation() {
        return "simulation";
    }

    /**
     * History page — view past simulation runs and comparisons
     */
    @GetMapping("/history")
    public String history() {
        return "history";
    }
}