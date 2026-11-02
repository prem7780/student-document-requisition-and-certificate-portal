# Hibernate Startup Fix (October 23, 2026)

Fixed `backend/src/main/java/com/dccmp/backend/entity/DepartmentDecisionEvent.java`.
The Flyway V3 schema declares `application_id`, `department_id`, `account_id`, `action`, and `decided_at` as NOT NULL. The JPA entity now matches these constraints explicitly.

No changes to Flyway migrations or database data are necessary.

## Apply
Replace your local `backend/src/main/java/com/dccmp/backend/entity/DepartmentDecisionEvent.java` with the fixed file or use this full ZIP. In IntelliJ, Maven > Reload Project; Build > Rebuild Project; run DccmpApplication again.

Hibernate startup was not verified against a running MySQL instance in this environment.
