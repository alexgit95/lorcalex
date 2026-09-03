## 1. Build provenance

- [x] 1.1 Configure Maven to embed the project version and the `git.commit` build property in the packaged application, defaulting the commit to `unknown`.
- [x] 1.2 Add the `GIT_COMMIT` Docker build argument and pass it to the Maven package command.
- [x] 1.3 Pass `${{ github.sha }}` as `GIT_COMMIT` from every Docker publication action in the GitHub Actions workflow.
- [x] 1.4 Verify that a Maven package without a supplied revision succeeds and records the expected fallback metadata.

## 2. Administration API

- [x] 2.1 Add an authenticated administration endpoint that reads the packaged build metadata and returns the version plus the seven-character commit identifier (or `unknown`).
- [x] 2.2 Add focused MockMvc coverage for the successful metadata response, the fallback revision, and unauthenticated access rejection.

## 3. Administration interface

- [x] 3.1 Add the build identity request to the Administration page data API.
- [x] 3.2 Render `version - commit` in the Administration page header and retain a usable page with an unavailable indicator if that request fails.

## 4. Documentation

- [x] 4.1 Document the Administration build identity display in the Docker/Portainer deployment guide, including the `version - short SHA` format, CI provenance, and the `unknown` fallback for local builds.

## 5. Validation

- [x] 5.1 Run the focused controller tests and the Maven test suite.
- [ ] 5.2 Build the Docker image with a known `GIT_COMMIT` and verify that the running application reports the expected version and short SHA.