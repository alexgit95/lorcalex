# pricing-admin-manual-import-simulation Specification

## Purpose
Temporary admin capability to apply pricing updates from a manually pasted provider-shaped JSON payload, without any outbound provider call or budget consumption.

## Requirements
### Requirement: Manual pricing payload simulation
The system SHALL allow an admin to apply pricing updates from a manually pasted provider-shaped JSON payload, without calling the external provider or consuming any daily or per-minute call budget.

#### Scenario: Admin submits a valid provider-shaped payload
- **WHEN** an admin submits a JSON payload matching the provider episode-cards response shape (a `data` array, a raw array, or a single card object)
- **THEN** the system SHALL apply the same deterministic card mapping and price extraction rules used by the real pricing sync
- **AND** SHALL update matched local cards' pricing metadata
- **AND** SHALL report the number of rows received, updated, and unresolved

#### Scenario: Admin submits invalid or empty JSON
- **WHEN** the submitted payload is empty or is not valid JSON
- **THEN** the system SHALL return an explicit failure result
- **AND** SHALL NOT modify any card pricing data

#### Scenario: Simulation never consumes provider call budget
- **WHEN** a manual pricing payload simulation is executed
- **THEN** the system SHALL NOT perform any outbound provider HTTP request
- **AND** SHALL NOT decrement daily or per-minute call budget counters
