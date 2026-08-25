# operational-api-constraints Specification

## Purpose
TBD - created by archiving change formalize-product-governance-and-compatibility. Update Purpose after archive.
## Requirements
### Requirement: Fixed recent collection limits
The recent collection endpoint MUST accept only the predefined limits 10, 20, 25, and 50.

#### Scenario: Allowed limit
- **WHEN** a request provides limit 10, 20, 25, or 50
- **THEN** the endpoint SHALL use the provided limit value

#### Scenario: Disallowed limit
- **WHEN** a request provides any other limit value
- **THEN** the endpoint SHALL apply the default limit defined by the API contract

### Requirement: Single broad CORS policy
The API surface MUST use one broad, shared CORS policy as an explicit operational choice.

#### Scenario: Cross-origin API request
- **WHEN** a browser request is made to API routes from an origin matching the broad policy
- **THEN** the response SHALL include the configured CORS allow headers for that policy

