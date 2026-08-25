## ADDED Requirements

### Requirement: Configurable OCR total upper bound
The scanner OCR parser MUST validate the N/TOTAL expression using a configurable maximum TOTAL value sourced from application settings, with a documented fallback default.

#### Scenario: Valid value under configured bound
- **WHEN** OCR parses a TOTAL value less than or equal to the configured maximum
- **THEN** the parser SHALL accept the value as valid for downstream card resolution

#### Scenario: Value above configured bound
- **WHEN** OCR parses a TOTAL value above the configured maximum
- **THEN** the parser SHALL reject the parsed code as invalid

#### Scenario: Missing or invalid setting
- **WHEN** OCR bound setting is missing or invalid
- **THEN** the parser SHALL apply the documented fallback maximum value
