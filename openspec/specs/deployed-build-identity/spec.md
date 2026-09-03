# deployed-build-identity Specification

## Purpose
Enables administrators to identify the version and source commit of the active application deployment. This specification defines how build identity is captured during artifact creation, exposed through an authenticated administration endpoint, and displayed in the Administration page header for operational visibility and debugging.

## Requirements

### Requirement: Build artifact records deployment identity
The system SHALL embed the Maven project version and the complete source commit identifier supplied at build time in the deployable application artifact.

#### Scenario: CI build receives a source revision
- **WHEN** the Docker publication workflow builds an application image for a Git commit
- **THEN** the resulting application artifact SHALL record the POM version and the complete commit identifier for that commit

#### Scenario: Build occurs without a supplied source revision
- **WHEN** an application artifact is built without a source commit identifier
- **THEN** the build SHALL complete and the artifact SHALL record `unknown` as its commit identifier

### Requirement: Administrators can retrieve active build identity
The system SHALL provide an authenticated administration endpoint that returns the active artifact's POM version and a seven-character commit identifier suitable for display.

#### Scenario: Authenticated administrator requests build identity
- **WHEN** an authenticated administrator requests the build identity endpoint
- **THEN** the response SHALL contain the active artifact version and the first seven characters of its recorded commit identifier

#### Scenario: Build identity uses the fallback revision
- **WHEN** the active artifact was built without a supplied source revision
- **THEN** the endpoint SHALL return `unknown` as its commit identifier

#### Scenario: Unauthenticated user requests build identity
- **WHEN** an unauthenticated user requests the build identity endpoint
- **THEN** the system SHALL reject the request according to the application's existing administration authentication rules

### Requirement: Administration header displays active build identity
The system SHALL display the active build identity in the Administration page header using the format `version - commit`.

#### Scenario: Administrator opens the Administration page
- **WHEN** an authenticated administrator opens the Administration page and build identity is available
- **THEN** the page header SHALL display the version, a hyphen separator, and the seven-character commit identifier

#### Scenario: Build identity request fails
- **WHEN** the Administration page cannot retrieve build identity
- **THEN** the page SHALL remain usable and SHALL display an unavailable build identity indicator in the header
