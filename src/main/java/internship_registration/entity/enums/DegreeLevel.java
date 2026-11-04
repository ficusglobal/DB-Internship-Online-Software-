package internship_registration.entity.enums;

import java.util.Locale;

/** Degree level of a student. UG has semesters 1 to 8, PG has semesters 1 to 4. */
public enum DegreeLevel {
    UG("UG", 8),
    PG("PG", 4);

    private final String label;
    private final int maxSemester;

    DegreeLevel(String label, int maxSemester) {
        this.label = label;
        this.maxSemester = maxSemester;
    }

    public String getLabel() {
        return label;
    }

    public int getMaxSemester() {
        return maxSemester;
    }

    /** Parses "UG" / "pg" etc. Throws IllegalArgumentException (HTTP 400) for anything else. */
    public static DegreeLevel parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Degree is required (UG or PG)");
        }
        try {
            return DegreeLevel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Degree must be UG or PG");
        }
    }
}