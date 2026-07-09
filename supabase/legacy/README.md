# ⚠️ Legacy SQL — DO NOT RUN against the live database

These `.sql` files belong to the **removed** Next.js prototype. They are kept only for
historical reference.

The live database schema is owned entirely by the Spring Boot backend
(`ecommerce-backend`, Hibernate `ddl-auto`). Running any of these dumps against the
production Supabase database would conflict with that schema and can corrupt data.

**Do not execute these in the Supabase SQL editor or anywhere else.** Delete this folder
once you're confident nothing here is needed.
