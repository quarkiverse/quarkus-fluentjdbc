# Example usage of FluentJdbc (Runtime)

To start the application run: 
> mvn quarkus:dev

Then open the Swagger UI and run some of the endpoints:
> http://localhost:8080/q/swagger-ui

And check the console for log statements.

## Domain model

The application will startup `fruit`, `farmer` and `fruit_farmer` tables with Liquibase.

`fruit` has a many-to-many relationship with `farmer` via the `fruit_farmer` table.

# Native Compilation (GraalVM)

To compile the project into a native binary use:

> mvn clean package -Pnative
