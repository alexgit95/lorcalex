# Technical Decisions

## CORS policy

The project currently uses a single broad CORS policy for API routes.

Allowed origin patterns:
- http://localhost:*
- http://127.0.0.1:*
- https://*

This is an explicit operational decision aligned with current product constraints.

## Recent endpoint limits

The recent collection endpoint accepts only these values:
- 10
- 20
- 25
- 50

Invalid values must fall back to the default API limit.

## Import export compatibility

The backup/export contract follows an N/N-1 compatibility policy.
Any payload contract change requires:
- fixture updates for N and N-1,
- unit test updates,
- integration test updates,
- CI compatibility gate pass.
