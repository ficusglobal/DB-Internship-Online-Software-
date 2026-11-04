package internship_registration.entity.enums;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Department (programme) of a student. Each one belongs to a degree level.
 * The CODE (e.g. BSC) is what the API receives and what is stored in students.department;
 * the LABEL (e.g. B.Sc) is what the frontend shows and what is printed on letters.
 */
public enum Department {
    // ---- UG
    BA("B.A", DegreeLevel.UG),
    BSC("B.Sc", DegreeLevel.UG),
    BCOM("B.Com", DegreeLevel.UG),
    BAGRI("B.Agri", DegreeLevel.UG),
    UG_OTHERS("Others", DegreeLevel.UG),
    // ---- PG
    MA("M.A", DegreeLevel.PG),
    MSC("M.Sc", DegreeLevel.PG),
    MCOM("M.Com", DegreeLevel.PG),
    MAGRI("M.Agri", DegreeLevel.PG),
    PG_OTHERS("Others", DegreeLevel.PG);

    private final String label;
    private final DegreeLevel degree;

    Department(String label, DegreeLevel degree) {
        this.label = label;
        this.degree = degree;
    }

    public String getLabel() {
        return label;
    }

    public DegreeLevel getDegree() {
        return degree;
    }

    /** All departments of one degree level, in display order. */
    public static List<Department> forDegree(DegreeLevel degree) {
        List<Department> result = new ArrayList<>();
        for (Department d : values()) {
            if (degree == null || d.degree == degree) {
                result.add(d);
            }
        }
        return result;
    }

    /** Parses a department code such as "BSC". Throws IllegalArgumentException (HTTP 400) when unknown. */
    public static Department parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Department is required");
        }
        try {
            return Department.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown department: " + value
                    + ". Use one of BA, BSC, BCOM, BAGRI, UG_OTHERS, MA, MSC, MCOM, MAGRI, PG_OTHERS");
        }
    }

    /** UG_OTHERS / PG_OTHERS: the student types the department name (students.department_other). */
    public boolean isOthers() {
        return this == UG_OTHERS || this == PG_OTHERS;
    }

    /** Name to show / print: the typed name for "Others", otherwise the label (e.g. B.Sc). */
    public static String display(String storedValue, String departmentOther) {
        if (departmentOther != null && !departmentOther.isBlank()
                && ("UG_OTHERS".equalsIgnoreCase(storedValue) || "PG_OTHERS".equalsIgnoreCase(storedValue))) {
            return departmentOther.trim();
        }
        return labelOf(storedValue);
    }

    /** Label for a stored code; older rows that hold free text are returned unchanged. */
    public static String labelOf(String storedValue) {
        if (storedValue == null) {
            return null;
        }
        try {
            return Department.valueOf(storedValue.trim().toUpperCase(Locale.ROOT)).label;
        } catch (IllegalArgumentException e) {
            return storedValue;
        }
    }
}