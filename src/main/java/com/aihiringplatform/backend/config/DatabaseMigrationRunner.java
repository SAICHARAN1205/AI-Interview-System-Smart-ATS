package com.aihiringplatform.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            // Drop the existing constraint safely
            jdbcTemplate.execute("ALTER TABLE applications DROP CONSTRAINT IF EXISTS applications_status_check;");
            
            // Add the updated constraint including INTERVIEW_SCHEDULED and SELECTED
            jdbcTemplate.execute("ALTER TABLE applications ADD CONSTRAINT applications_status_check " +
                    "CHECK (status IN ('APPLIED', 'ATS_REVIEW', 'SHORTLISTED', 'INTERVIEW_SCHEDULED', 'INTERVIEW_COMPLETED', 'REJECTED', 'SELECTED'));");
                    
            System.out.println("✅ Successfully updated applications_status_check constraint!");
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Could not update applications_status_check constraint: " + e.getMessage());
        }
    }
}
